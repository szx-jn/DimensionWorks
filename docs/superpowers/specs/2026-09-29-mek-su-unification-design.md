# MEK 机械 SU/RPM 严格供能设计

## 目标

把 DimensionWorks 中合格的 Mekanism 加工机器从 FE/J 改为只接受 ME Gearbox EXPORT 提供的机械动力，并让 ME Memory 网络中的 SU 库存和 RPM 带宽同时参与供能判断。

本阶段只覆盖 `StressRules.isEligible` 判定为合格加工机器的方块。能量立方、通用线缆、充电台、发电机、多方块结构和物流结构不在范围内。

## 核心规则

- 合格 MEK 机器不再读取、换算或接收 FE。内部配方系统在有有效机械动力时看到满能量，无有效机械动力时看到零能量。
- 机器从相邻 Create 动力网络中的 ME Gearbox EXPORT 取动力。只要齿轮箱实际输出 RPM 大于零，机器就按 `当前有效 RPM × 8` 占用网络 SU；空闲机器也持续占用。
- 齿轮箱不依赖下游机器决定是否启动。空载时使用 `有效 RPM × 8` 的基础负载；接入机器后，SU 需求取基础负载与下游实际完整应力负载的较大值。
- 机器只读取齿轮箱实际输出的有效 RPM 与已扣除 SU：供能有效时工作，任一缺失时工作倍率、加工与 Jade 运行状态均为零。
- 单台标准机器仍对应每 RPM `8` SU；多台机器只放大齿轮箱的 SU 负载，不重复累计 RPM。
- ME Gearbox IMPORT 仍把 Create 应力存入 AE；ME Gearbox EXPORT 从 AE 输出应力，并执行严格 SU 守恒。
- Applied Create 原有的 `stress_output_card`、AE 输出总线路由和相关 Jade 总线显示保持退役。

## 网络计算

```text
idleLoad       = effectiveRpm * 8
fullLoad       = max(idleLoad, Create 网络完整应力负载)
requestedSu    = ceil(fullLoad)
stockQ         = storedSu / requestedSu
rpmQ           = bandwidthRpm / requestedRpm，低于 0.6 时归零
q              = min(stockQ, rpmQ)
outputRpm      = round(effectiveRpm * q)
chargedSu      = min(requestedSu, 当前可用 SU)
perRpmCapacity = chargedSu / outputRpm
```

- `effectiveRpm` 先经过玩家 RPM 上限，再经过 `directMaxRpm`。
- `requestedRpm` 以每台 EXPORT 齿轮箱为一个占用单位；同一 AE 网络内的齿轮箱需求求和。多台下游机器只增加 SU 应力，不重复占用 RPM。
- 齿轮箱空载时的固定贡献是每 RPM `8` SU 应力；有下游负载时取较大值。实际通过网络扣除的 SU 仍按实际输出 RPM 等比例计算。
- 库存 SU、RPM 带宽、SU 需求或 RPM 需求任一项为零或为负，`q` 固定为零。零需求也不视为可用动力。
- 齿轮箱在有效 RPM、实际输出 RPM、实际扣费和 `q > 0` 全部有效时运行，不要求存在下游负载；否则立即清除输出速度与宣告容量。
- 齿轮箱向 Create 宣告的每 RPM 容量必须等于实际扣除的 SU 除以实际输出 RPM。下游不能消费高于本次 ME 库存允许值的应力。

## 路由与缓存

- `MachinePowerManager` 保存机器到相邻动力源和 EXPORT 齿轮箱的路由缓存。
- 实时网络可用时，机器直接使用实时路由；齿轮箱停转导致 Create 网络暂时断开时，只要源方块与齿轮箱仍然存在，就继续使用缓存路由保留应力需求。
- `MemoryGridService` 每 10 tick 扫描同一 AE 网格内全部 EXPORT 齿轮箱并重建需求缓存；齿轮箱每 tick 仍会做最终 `q`、扣费和输出门控。
- Create `KineticNetwork.members` 已经携带机器附加应力和成本倍率。网络存在时直接汇总成员系数；只有缓存路由未出现在当前网络时才补充机器负载，避免重复计费。

## FE 契约

- `getEnergy()`：有机械动力返回 `getMaxEnergy()`，否则返回 0。
- `isEmpty()`：无机械动力时为 `true`。
- `insert()`：合格机器的任何 FE 插入都返回未接受量，不修改实体 FE。
- `extract()`：仅 `AutomationType.INTERNAL` 且机械动力有效时返回请求量；不修改实体 FE，SU 已由齿轮箱在网络层扣除。
- 删除 `ProcessingEnergyRegistry`、`SuConversion`、`mekanismPerSu` 配置及其测试。

## Jade

- 合格 MEK 加工机器的 Mekanism Jade 能量元素必须移除，不再显示 FE/J。
- 本 Mod 的 MEK Jade 条目显示 Create 路线、当前有效 RPM、`RPM × 8` 的 SU/t 占用和工作倍率。
- 非加工 MEK 设备的 Jade 能量显示保持不变。

## 验收

- 纯逻辑测试覆盖每 RPM 固定 8 SU、`availableQ` 对 SU/RPM 任一项缺失返回 0、零需求不产生运行状态。
- 契约测试覆盖 `getEnergy`、`isEmpty`、`extract`、`insert` 注入点，以及齿轮箱只有在负载、RPM、扣费全部有效时才进入运行状态。
- 资源测试断言旧 FE 换算类和两份配置中的 `mekanismPerSu` 已删除。
- `gradle test --no-daemon` 与 `gradle build --no-daemon` 通过。
