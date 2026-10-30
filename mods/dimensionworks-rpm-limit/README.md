# DimensionWorks RPM Limit API

## Default
New players start at **32 RPM**. The hard maximum is **10240 RPM**.
The mod declares Create as a `BEFORE` dependency and its Mixin configuration is required, so the Create kinetic hooks are applied before Create's content is loaded.

## 齿轮之心

RPM Limit 模组承载 KubeJS 齿轮之心的工业额度运行时。物品由 `kubejs` 注册，Java 只按装备者 UUID 聚合当前加载且活跃的系统，不扫描卸载区块。

- Curios 专属槽 `gear_heart` 只接受 `kubejs:gear_heart` 与 `kubejs:gear_heart_enchanted`；不可手动取下，死亡保留。
- 玩家每次进入服务器后，登录事件会延后一 tick 检查 `gear_heart` 槽位；空槽自动补发
  一个 `CustomModelData=1`、修复位掩码为 0 的破损齿轮之心，因此七条额度诅咒全部未修复。
  已有物品的槽位不会被替换。
- 基础增益仍为交互距离 +2、急迫 II、幸运 I。
- 旧 `CustomModelData=2` 祝福物品会在装备时迁移为受破损态，`DimensionWorksGearHeartRepaired` 按原位保留。
- 修复必须按 1 到 7 顺序使用碎片；第七项完成后物品替换为 `kubejs:gear_heart_enchanted`，进入“全链释限”。
- 所有额度按 `min(齿轮之心额度, 原模组能力)` 应用；修复或最终释限只能移除齿轮之心额度。
- 无所有者系统进入共享后备桶，每个仍受诅咒的装备者都要承担共享用量。

### 七阶段

| 阶段 | 诅咒 | 单项修复 |
| --- | --- | --- |
| 1 | 装备者网络 32 RPM | 恢复 512 RPM |
| 2 | 全部活跃 Create 网络合计 4,096 SU | 立即移除自有 Create 容量额度 |
| 3 | Mek 矿物处理 8 次/秒 | 64 次/秒 |
| 4 | AE2 自动合成 1 个 | 80 个 |
| 5 | `gear_heart:cross_process` 16 件/秒 | 128 件/秒 |
| 6 | Mek/AE2 活跃设备各 8 台 | 各 64 台 |
| 7 | `gear_heart:final_production` 16 件/秒 | 128 件/秒 |

Create 容量、Mek 处理和 AE2 提交都在真实输出或提交边界先做完整预检；固定间隔令牌桶按玩家与额度类型隔离，同 tick 的多类请求原子提交，拒绝时不消耗输入或令牌。Create、Mek 和 AE2 白名单最终产物在空间模拟通过后才申请第七项额度。

Mek 直接机械供能接受正反两个方向：有效速度按 `abs(RPM)` 收口，Mekanism 机器应力按 `abs(RPM) × 256` 计算，零转速不供能。该行为由 `dimensionworks-mek-stress` 的 `MachinePowerMath` 与输出 Mixin 覆盖。

### 配置（COMMON，`[gearHeart]`）

| 键 | 默认值 |
| --- | ---: |
| `rotationCursedRpm` | 32 |
| `rotationRepairedRpm` | 512 |
| `createCapacityCursedSu` | 4096 |
| `mekOreOperationsCursedPerSecond` | 8 |
| `mekOreOperationsRepairedPerSecond` | 64 |
| `aeCraftingJobsCursed` | 1 |
| `aeCraftingJobsRepaired` | 80 |
| `crossProcessItemsCursedPerSecond` | 16 |
| `crossProcessItemsRepairedPerSecond` | 128 |
| `activeMachinesCursed` | 8 |
| `activeMachinesRepaired` | 64 |
| `finalProductionItemsCursedPerSecond` | 16 |
| `finalProductionItemsRepairedPerSecond` | 128 |

### 运行边界与验收状态

随机攻击、产出失败、冻结和夜晚饥饿配置已删除；不再取消玩家操作或吞入输入。构建、单元测试、Mixin 合同、JSON、KubeJS 与打包预检均通过；尚未实际进入游戏验证双人隔离、任务图、JEI 配方和正反向供能，因此这些项目仍标记为未运行时验证。

## Create 全局转速上限

Create 自身还会用 `kinetics.maxRotationSpeed` 钳制所有动力方块，默认只有 256 RPM。
本模组会在每个存档的 `create-server.toml` 加载时检查这个值；低于 10240 时直接修正运行时配置值，
因此已有存档被 Forge 回落到默认值、或新世界配置缺少该键时，都不会再把实际转速卡在 256。
随包仍提供 `defaultconfigs/create-server.toml`，让新建世界第一次加载前就使用正确值。


## Create 动力网络性能优化

> **状态：实验开发中。** 在自动构建和实际服务器压力测试完成前，不应视为已验证的稳定性能补丁，也不应作为正式版本发布。

从 0.4.6 起，本 Mod 通过 Mixin 为 Create 6.0.8 的动力网络维护可失效的增量统计缓存：

- 普通成员转速变化时，尽可能只修正该成员的应力贡献。
- 网络结构变化、区块实体移除或无法确认缓存有效时，丢弃缓存并回退到完整核算。
- 一次发电机转速更新中，应力与容量变动合并为一次网络刷新。
- 容量缓存保存的是应用齿轮之心额度前的自然容量；玩家额度仍在返回边界应用。
- 不重写 Create 的转速传播图，也不修改或分发 Create 源码。

该改造主要减少重复全网计算和重复同步，不会让必要的网络成员同步变成常数时间。实际收益需用相同工厂布局及 Spark/Forge profiler 对比确认。

超速倍率在 `KineticNetwork#getActualStressOf` 的实际应力边界动态应用，`network.members` 只保存速度无关的基础 impact，因此速度断点、玩家 RPM 上限和附属发电机变速后不会继续沿用旧应力系数。批处理状态会在服务器 tick 末清理，单个网络刷新异常会被隔离并记录。

### 所有权与额度边界

- 无所有者或无法沿 source 链解析所有者的普通 Create 方块回退到保守的默认 RPM，不回退到最大 RPM。
- 已经由其他自研适配器收口转速的虚拟成员通过 `IPreclampedKineticSpeed` 跳过二次限速。
- Gear Heart 的 Create 容量按每个 source 的实际容量与所有者分别分桶；混合所有者网络不会整体归给第一个活跃成员。
- 速度控制器只在原存档缺少所有者时补写发送者，不覆盖既有所有者。

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

齿轮、大齿轮和速度控制器仍使用 Create 原生倍率计算，但每次 Create 写入动能方块转速时，都会
按该方块所有者的个人上限封顶。因此齿轮链的每一级存储转速、理论转速及消费者实际转速都不会
超过其所有者上限；齿轮倍率不能继续叠加出超限转速。修改个人上限时，已加载的自有动能方块会
立即重新收口，未加载区块在加载时收口。不同玩家混合的网络按每台方块的直接所有者分别限速。
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

Values are clamped to 1..10240. This API changes the player's stored limit immediately and refreshes all currently loaded machines owned by that player, including their stored network speeds.

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
