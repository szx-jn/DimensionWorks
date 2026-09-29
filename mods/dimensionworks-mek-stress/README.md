# DimensionWorks ME Mechanical Power

把 Create 机械动力、Mekanism 机器和 AE2 网络连接成两套独立工程路线：

- ME 数字路线：Memory Drive 提供实时 SU 库存，Memory Drive 自身提供网络 RPM 带宽；所有 AE 驱动机器按统一拥塞系数 `q` 调度。
- Create 直连路线：机器直接从相邻 Create 动力轴读取实际 RPM，不占用 ME 库存，也不经过 `q` 调度。

本 Mod 不修改 Applied Create、AE2、Mekanism、Create 的源码。Applied Create 原有的应力存储 Cell、组件、外壳和创造 Cell 全部退役，不能继续挂载、读写或供应 `StressKey`。

## Memory 参数

| DDR | 每 Drive 槽位 | 单卡 SU | 单 Drive 带宽 |
|---|---:|---:|---:|
| DDR1 | 4 | 8,192 | 8,192 RPM |
| DDR2 | 8 | 9,216 | 16,384 RPM |
| DDR3 | 16 | 10,240 | 32,768 RPM |
| DDR4 | 32 | 12,288 | 65,536 RPM |
| DDR5 | 64 | 16,384 | 131,072 RPM |

每个逻辑 AE 网络最多统计两台 Drive。第三台会使网络容量、带宽和输出归零，但 Drive 内已有 Card 与 SU 不会丢失；移除多余 Drive 后自动恢复。

## 调度公式

```text
D_RPM = Σ effectiveRpm + Σ gearboxExportRpm
D_SU  = Σ 8 * effectiveRpm + Σ gearboxExportSuPerTick
C     = Σ driveCardCount * configuredCardCapacity
B     = Σ driveBandwidth
q_su  = D_SU == 0 ? 1 : min(1, storedSu / D_SU)
q_bw  = D_RPM == 0 ? 1 : min(1, B / D_RPM)
q     = min(q_su, q_bw)

AE actualRpm     = effectiveRpm * q
AE efficiency    = q^1.25
AE production    = (effectiveRpm / tierRpm) * q^2.25
Create direct    = min(adjacentRpm, configuredDirectMaxRpm) / tierRpm
```

空闲机器计入带宽需求，但只有实际加工 tick 才扣除 `8 * effectiveRpm * q` SU。Gearbox IMPORT 向网络输入 SU；Gearbox EXPORT 与输出总线共用同一个 `q`。

## 工程接口

- `memory_drive_ddr1..5`：每页显示 10 槽，按 DDR 等级只接受同等级 Memory Card。
- `memory_card_ddr1..5`：只影响网络 SU 容量，不影响 Drive 带宽。
- `stress_output_card`：只可安装于 AE2 ME 输出总线，最多一张。装卡后总线停止普通物品导出，并驱动相邻的合格 Mekanism 机器。
- `/dw memory`：读取附近已连接 Drive 所在逻辑网格的状态快照。

所有平衡值位于 COMMON 配置 `dimensionworks_mek_stress-common.toml`。槽数、结构规则、两台 Drive 上限、公式和两条路线的基本结构不可配置。

## 构建

```sh
gradle build --no-daemon
```
