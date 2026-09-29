# DimensionWorks ME Mechanical Power

把 Create 机械动力、Mekanism 机器和 AE2 内存网络连接成两条明确边界：

- AE 侧库存由 Memory Drive 与 Memory Card 提供。Gearbox IMPORT 把 Create 应力存入网络，Gearbox EXPORT 从网络扣除 SU 后输出应力。
- Create 侧动力只能从 ME Gearbox EXPORT 取出。合格 Mekanism 加工机器必须位于该齿轮箱供能的 Create 网络内。
- 机器内部 FE 不再提供可用能量。FE 请求只作为加工工作量记录，并按 `2.5 FE = 1 SU` 向上取整扣除网络 SU。

本 Mod 不修改 Applied Create、AE2、Mekanism、Create 的源码。Applied Create 原有的应力存储 Cell、组件、外壳和创造 Cell 全部退役，不能继续挂载、读写或供应 `StressKey`。

KubeJS 会把 `memory_drive_ddr1..5` 和 `memory_card_ddr1..5` 加入创造标签；`stress_output_card` 及其配方、资源、总线逻辑和 Jade 显示已全部删除。

## Memory 参数

| DDR | 每 Drive 槽位 | 单卡 SU | 单 Drive 带宽 |
|---|---:|---:|---:|
| DDR1 | 4 | 8,192 | 8,192 RPM |
| DDR2 | 8 | 9,216 | 16,384 RPM |
| DDR3 | 16 | 10,240 | 32,768 RPM |
| DDR4 | 32 | 12,288 | 65,536 RPM |
| DDR5 | 64 | 16,384 | 131,072 RPM |

每个逻辑 AE 网络最多统计两台 Drive。第三台会使网络容量、带宽和输出归零，但 Drive 内已有 Card 与 SU 不会丢失；移除多余 Drive 后自动恢复。

## 应力守恒

```text
fullLoad      = Σ Create member stressPerRpm * configuredRpm
requestedSu   = ceil(fullLoad)
q             = min(stockQ, bandwidthQ)
outputRpm     = round(configuredRpm * q)
chargedSu     = min(requestedSu, availableSu)
perRpmCapacity = chargedSu / outputRpm
```

ME Gearbox EXPORT 向 Create 网络宣告的容量严格等于本次实际扣除的 SU。下游机器在 `outputRpm` 下最多只能消费 `chargedSu`；超出部分触发 Create 原有应力过载，不能无中生有。

合格 Mekanism 机器实际加工时才会记录 FE 工作量。空闲机器不占 SU，也不产生 Create 应力；每 tick 的请求汇总换算后写入 Memory 网络账本。

## 工程接口

- `memory_drive_ddr1..5`：使用 AE2 驱动器背景，每页 4 列 × 5 行共 20 槽，按 DDR 等级只接受同等级 Memory Card。
- `memory_card_ddr1..5`：只影响网络 SU 容量，不影响 Drive 带宽。
- `appliedcreate:me_gearbox`：IMPORT 输入应力到 AE，EXPORT 从 AE 输出应力并执行 SU 守恒。
- `/dw memory`：读取附近已连接 Drive 所在逻辑网格的状态快照。

所有平衡值位于 COMMON 配置 `dimensionworks_mek_stress-common.toml`。槽数、结构规则、两台 Drive 上限、公式和齿轮箱守恒边界不可配置。

## 构建

```sh
gradle build --no-daemon
```
