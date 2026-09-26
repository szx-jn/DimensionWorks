# 超速并行批处理设计

## 目标

给 `dimensionworks-rpm-limit` 增加一条超速增益机制：转速超过机器自身饱和点后，同一次操作按倍数并行处理多份原料与产出，
把现在 512→10240 RPM 的空转区间变成真实产能。应力随并行倍数同步放大，因此超速多产必须付出更多发电能力。

## 曲线

比例 `r = |当前 RPM| ÷ 该机器饱和点`，阶跃取值（取已达成断点值），全局封顶 15×：

| r | 1 | 2 | 4 | 8 | 10 | 12 | 14 | 16 | 18 | 20 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 倍数 | 1× | 2× | 2.5× | 3.5× | 5× | 7× | 8.5× | 10× | 11.5× | 15× |

换算到实际转速：

- 搅拌机、机械压力机（饱和点 512）：512 / 1024 / 2048 / 4096 / 5120 / 6144 / 7168 / 8192 / 9216 / 10240 RPM。
- 风箱（256）：断点转速减半，2560 RPM 起达 5×，5120 RPM 到顶。
- 动力锯（3072）：10240 RPM 时只到 2×。
- 磨石（8192）：10240 RPM 时仍是 1×。

表以 `ratio:multiplier` 对写进 COMMON 配置，解析失败时保留上一张表并记日志。

### 饱和点来源

饱和点不做猜测，全部从 Create 6.0.8 自身代码读出：

| 机器 | 饱和点 | 依据 |
| --- | --- | --- |
| 机械搅拌机 | 512 | `MechanicalMixerBlockEntity` 里 `512.0f / speed` 的钳位常数 |
| 机械压力机 | 512 | 同上，压力机走盆式加工路径 |
| 风箱 | 256 | 风箱气流处理的速度基准 |
| 动力锯 | 3072 | `SawBlockEntity` 的 `|speed| / 24` 进度式 |
| 磨石 | 8192 | `getProcessingSpeed()` 在 `|speed| / 16` 上钳位到 512/刻 |

## 小数倍数的确定性

2.5× 表现的是一组交替的 2 份、3 份，而不是每刻固定 2.5 份。实现方式是取「楼层差分」而不是存小数余量：

```
batches(m, t) = floor(m * (t + 1)) - floor(m * t)
```

其中 `t` 是 `level.getGameTime()`。任意时刻累计的批次数恰好是 `floor(m * (t + 1))`，所以长期平均值严格等于 `m`，
不需要随机数，不需要存档字段，也不需要任何同步字段。

## 实现路线

工作倍率与动画状态机彻底分离。

工作侧只在 Create 原本完成一次操作的位置重复入口：

- 搅拌机和盆式压力机：在 `BasinOperatingBlockEntity#applyBasinRecipe` 返回后重复配方应用。
- 磨石：在 `MillstoneBlockEntity#process` 返回后重复加工。
- 世界物品压力机：在 `PressingBehaviour#applyInWorld` 返回后重复扫描与压制。
- 风箱：只重复 `AirCurrent#tickAffectedHandlers`，不重复 `tickAffectedEntities`。

整段 `tick()` 不再重复执行，所以 `runningTicks`、压头位移、搅拌头状态机等动画状态始终每个游戏 tick 只推进一次。
重复入口使用 `ThreadLocal` 防重入，原料不足时 Create 原生逻辑会自行跳过。

渲染侧在 Flywheel 的 `RotatingInstance#setup`、非 Flywheel 的 `KineticBlockEntityRenderer#getAngleForBe` 以及搅拌头旋转入口读取
`OverspeedBonus#animationSpeed`。视觉转速在机器自身饱和点处封顶，只有加工倍率与应力继续随实际转速增长。

风箱是唯一例外：它的加工发生在 `AirCurrent` 里，而 `AirCurrent` 同时负责推动玩家与生物。因此风箱只重复
`AirCurrent#tickAffectedHandlers`（物品处理），不重复 `tickAffectedEntities`（实体推动），避免把击退乘上倍数。

## 应力

`KineticBlockEntity#calculateStressApplied` 的返回值乘以当前倍数。倍数先读取 `getTheoreticalSpeed()` 原始网络速度，再按玩家上限封顶，不直接读取 `getSpeed()`：
Create 对过载网络返回 0 RPM，如果应力随 `getSpeed()` 走，机器过载的一瞬间应力会掉回 1×，从而立刻解除过载并在下一刻
再次触发，形成抖动。用理论转速后应力是网络的纯函数，过载状态稳定。

## 护目镜

倍数为纯函数，客户端用同一张表从自己看到的转速推算，因此不需要新网络包。倍数大于 1 时在 Create 原有信息后追加一行
「超速并行 ×N」（`create.gui.stressometer.overspeed_batch`），倍数为 1 时不显示。

## 配置

COMMON，`[overspeed]`：

| 键 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 总开关 |
| `cap` | `15` | 全局倍数上限 |
| `stressScaling` | `true` | 应力是否随倍数放大 |
| `stepTable` | 默认曲线 | 断点表 |
| `defaultSaturationRpm` | `512` | 白名单机器未登记饱和点时的回退值 |
| `classWhitelist` | `[]` | 额外参与批处理的方块实体类名 |

## 范围与取舍

- 纯传动与物流设备（轴、齿轮、传送带、溜槽、机械臂、活塞、绳索、火车、显示器）不参与。
- 钻头与动力泵本轮不参与。
- 附属机器默认 1×，需要实测后加进 `classWhitelist`。Create: Diesel Generators 的盆盖继承盆式加工链，会自动生效。
- 动力锯受单个输入槽限制，原版在饱和转速附近已经达到每 tick 一个堆栈的处理上限；超速不会额外复制输入。
- 视觉上限只改渲染器，不影响配方、应力或网络速度。
- 单次操作最多重复 15 次，且只在原料足够时进入循环。
