# MEK 机械 SU 统一与应力守恒设计

## 目标

把 DimensionWorks 中合格的 Mekanism 加工机器从 FE/J 改用 SU 作为唯一工作能量，并修复 ME Gearbox EXPORT 与 Create 网络之间的应力无中生有。

本阶段只覆盖 `StressRules.isEligible` 判定为合格加工机器的方块。能量立方、通用线缆、充电台、发电机、多方块结构和物流结构不在范围内。

## 核心规则

- 换算固定为 `2.5 FE = 1 SU`。消耗量向上取整，避免低于原 FE 成本。
- 合格 MEK 机器拒绝一切外部 FE 输入，内部处理能量提取被映射为 ME Network 中的 SU 消耗。
- 机器只能从相邻 Create 动力网络获得机械能；ME Gearbox IMPORT 仍向网络输入 SU，ME Gearbox EXPORT 从网络输出 SU。
- `stress_output_card`、AE2 输出总线驱动逻辑、机器 AE 路由和相关 Jade 总线显示全部删除。
- ME Gearbox EXPORT 不再按配置上限盲目扣费。它先计算下游 Create 网络在满配置 RPM 下的实际应力负载，上报该需求，再由库存与带宽系数 `q` 决定实际供能和实际转速。
- 齿轮箱向 Create 宣告的每 RPM 容量必须等于 `实际扣除 SU / 实际输出 RPM`，因此下游不能消费高于 ME 库存允许值的应力。

## 网络现金流

```text
fullLoad      = Σ(member stressPerRpm * configuredRpm)
requestedSu   = ceil(fullLoad)
q             = min(stockQ, bandwidthQ)
outputRpm     = round(configuredRpm * q)
chargedSu     = min(requestedSu, availableSu)
perRpmCapacity = chargedSu > 0 && outputRpm > 0 ? chargedSu / outputRpm : 0
```

机器侧的 `DirectStressRegistry` 不再使用固定 `rpm * 8`。它向动力网络报告上一完整 tick 内合格 MEK 机器实际请求的 FE 所换算出的 SU；空闲机器不占应力。

## Jade

- 合格 MEK 加工机器的 Mekanism Jade 能量元素必须移除，不再显示 FE/J。
- 本 Mod 的 MEK Jade 条目显示 Create 路线、实际 RPM、SU/t 成本和状态。
- 非加工 MEK 设备的 Jade 能量显示保持不变。

## Memory Drive UI

- 使用 AE2 的 `ae2:textures/guis/drive.png` 直接绘制背景，不复制第三方贴图。
- 每页 4 列 × 5 行，共 20 个标准 18×18 槽位，沿用 AE2 原版的紧凑布局。
- DDR5 共 4 页；低等级按 `ceil(slotsPerDrive / 20)` 动态显示页数。
- 玩家背包沿用 AE2 驱动器背景的 176×199 标准布局，所有控件必须留在背景框内。

## 配置与兼容

- 新增 `mekanismPerSu` 配置，默认 `2.5`。
- Mod 版本提升到 `0.2.2`，同步 `build.gradle`、`mods.toml` 与 `manifest/mods.json`。
- 已存在的 `stress_output_card` 物品不迁移、不补偿，注册移除后由 Forge 将未知物品视作空物品。

## 验收

- 纯逻辑测试覆盖 FE→SU 向上取整、齿轮箱供能上限、每 RPM 容量和动态分页。
- Jade 净化测试覆盖只删除 `mekData` 中的 `energy` 条目。
- `gradle test --no-daemon` 与 `gradle build --no-daemon` 通过。
- 构建产物中不再包含 `stress_output_card` 资源、配方或 mixin 注册。
