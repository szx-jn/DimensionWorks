# DimensionWorks RPM Limit API

## Default
New players start at **32 RPM**. The hard maximum is **10240 RPM**.
The mod declares Create as a `BEFORE` dependency and its Mixin configuration is required, so the Create kinetic hooks are applied before Create's content is loaded.

## Create 全局转速上限

Create 自身还会用 `kinetics.maxRotationSpeed` 钳制所有动力方块，默认只有 256 RPM。
本模组会在每个存档的 `create-server.toml` 加载时检查这个值；低于 10240 时直接修正运行时配置值，
因此已有存档被 Forge 回落到默认值、或新世界配置缺少该键时，都不会再把实际转速卡在 256。
随包仍提供 `defaultconfigs/create-server.toml`，让新建世界第一次加载前就使用正确值。

## Command
Requires permission level 2:
```
/dw rpm <player> <limit>
```

This changes the target player's RPM limit for Create kinetic networks they own.

## 每台机械上限

玩家放下动力方块时，方块会记录直接拥有者，并同步保存该玩家当时的 RPM 上限。上限变化后，
模组会立即扫描所有已加载维度中该玩家直接放置的动能方块，更新机器上限并同步给客户端；
未加载区块会在下次加载时读取玩家最新上限。

齿轮、大齿轮和速度控制器仍使用 Create 原生倍率计算；所有对外读取得到的理论转速、机器加工
速度和位移统一在 `KineticBlockEntity#getSpeed()` 处按拥有者上限封顶。Create 的
`getTheoreticalSpeed()`、`Source`、`Network` 和 `RotationPropagator` 全部保持原样，因此齿轮链
可以正常传递和重算，消费者实际获得的 RPM 仍不超过该玩家上限。
应力容量与应力消耗也按同一个有效转速缩放，避免只看原始网络速度时把限速后的机器误判为过载。

## KubeJS
Server-side KubeJS can call the public Java API:
```js
const RpmLimitApi = Java.loadClass("dev.szx.dimensionworks.rpmlimit.RpmLimitApi")

// ServerPlayer -> limit
RpmLimitApi.setLimit(player, 64)
const limit = RpmLimitApi.getLimit(player)

// UUID string -> read limit
const limit2 = RpmLimitApi.getLimit(player.uuid.toString())
```

Values are clamped to 1..10240. This API changes the player's stored limit immediately and refreshes all currently loaded machines owned by that player. Create's raw kinetic network is left untouched.

## 超速并行（Overspeed Batch）

转速超过机器自身的饱和点后，同一次操作会按倍数并行处理多份原料与产出。
512→10240 RPM 的空转区间因此变成真实产能，但应力会随倍数同步放大，想超速多产就必须多堆发电机。

比例 `r = 当前 RPM ÷ 该机器饱和点`，阶跃取值，全局封顶 15×：

| r | 1 | 2 | 4 | 8 | 10 | 12 | 14 | 16 | 18 | 20 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 倍数 | 1× | 2× | 2.5× | 3.5× | 5× | 7× | 8.5× | 10× | 11.5× | 15× |

换算到实际转速：

- 搅拌机、机械压力机（饱和点 512）：512 / 1024 / 2048 / 4096 / 5120 / 6144 / 7168 / 8192 / 9216 / 10240 RPM 对应上表。
- 风箱（256）：断点转速减半，2560 RPM 起达 5×，5120 RPM 到顶 15×。
- 动力锯（3072）：10240 RPM 时只到 2×。
- 磨石（8192）：10240 RPM 时仍是 1×。

小数倍数用确定性余量累积实现：2.5× 表现为交替 2 份、3 份，长期平均 2.5，不使用随机数，
也不额外同步任何字段。工作批次只在机器原本完成一次配方/加工的位置重复执行对应入口，不重跑完整的 `tick()`；
原料、产出、概率产物、流体仍走 Create 原生逻辑，原料不足时自然降为可用份数。

渲染侧使用另一条路径：Flywheel 与非 Flywheel 动画都会把视觉转速限制在该机器自身饱和点。
因此超出饱和点后动画保持原来的最高速度，只有加工倍率和应力继续增长。

纯传动与物流设备（轴、齿轮、传送带、溜槽、机械臂、活塞、绳索、火车、显示器）、钻头与动力泵本轮不参与。
风箱只重复物品处理（`AirCurrent#tickAffectedHandlers`），不重复推动玩家与生物的 `tickAffectedEntities`。

### 护目镜

倍数大于 1 时，护目镜在原有信息后追加一行“超速并行 ×N”；倍数为 1 时不显示，保持原版观感。
倍数由客户端用同一张表从自己看到的转速推算，不需要额外网络包。

### 配置（COMMON，`[overspeed]`）

| 键 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 超速并行总开关 |
| `cap` | `15` | 全局倍数上限 |
| `stressScaling` | `true` | 应力是否随倍数放大 |
| `stepTable` | `1:1,2:2,4:2.5,8:3.5,10:5,12:7,14:8.5,16:10,18:11.5,20:15` | 断点表，解析失败时保留旧表并记日志 |
| `defaultSaturationRpm` | `512` | 白名单机器未登记饱和点时的回退值 |
| `classWhitelist` | `[]` | 额外参与批处理的方块实体类名（全限定名或简单名） |

### 附属机器

附属机器默认不参与，保持 1×。确认某台附属加工机适合批量后，把它的类名加进 `classWhitelist` 即可，
饱和点用 `defaultSaturationRpm` 或按需在后续版本登记。Create: Diesel Generators 的盆盖继承盆式加工链，
会自动跟随搅拌机一类机器生效。

### 已知取舍

- 置物台冲压走 `BeltPressingCallbacks`，会在一轮动画内聚合多份原料和产出；原版回调无论输入堆多大都只消耗 1 个。地面物品另走 `applyInWorld()`，两条路径都已按倍率处理。
- 动力锯受单个输入槽限制，原版在饱和转速附近已经达到每 tick 一个堆栈的处理上限；超速不会额外复制输入。
- 倍数使用玩家上限后的有效转速，但直接读取原始理论转速再封顶，避免过载时掉回 1× 造成应力抖动。
- 视觉转速上限只影响渲染器，不参与配方、应力或网络速度计算。
