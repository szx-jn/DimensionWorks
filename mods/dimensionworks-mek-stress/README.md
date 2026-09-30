# DimensionWorks ME Mechanical Power

把 Create 机械动力、Mekanism 机器和 AE2 内存网络连接成两条明确边界：

- AE 侧库存由 Memory Drive 与 Memory Card 提供。Gearbox IMPORT 把 Create 应力存入网络，Gearbox EXPORT 从网络扣除 SU 后输出应力。
- Create 侧动力只能从 ME Gearbox EXPORT 取出。合格 Mekanism 加工机器必须位于该齿轮箱供能的 Create 网络内。
- 合格 Mekanism 机器不再读取或换算 FE。只要连接的 EXPORT 齿轮箱正在输出，机器就持续按 `当前有效 RPM × 8` 占用网络 SU；空闲机器也占用。机器拒绝一切 FE 输入。

本 Mod 不修改 Applied Create、AE2、Mekanism、Create 的源码。Applied Create 原有的应力存储 Cell、组件、外壳和创造 Cell 全部退役，不能继续挂载、读写或供应 `StressKey`。

KubeJS 会把 `memory_drive_ddr1..5` 和 DDR1～5 × 六形态共 30 个 `memory_card_ddrN_*` 加入创造标签；`stress_output_card` 及其配方、资源、总线逻辑和 Jade 显示已全部删除。

## Memory 参数

| DDR | 每 Drive 槽位 | 基础单卡容量 `C_d` | 基础驱动带宽 `B_d` |
|---|---:|---:|---:|
| DDR1 | 4 | 8,192 SU | 8,192 RPM |
| DDR2 | 8 | 9,216 SU | 16,384 RPM |
| DDR3 | 16 | 10,240 SU | 32,768 RPM |
| DDR4 | 32 | 12,288 SU | 65,536 RPM |
| DDR5 | 64 | 16,384 SU | 131,072 RPM |

每种卡用二维二进制指数 `(a,v)` 描述：劣质 `(-2,-2)`、经济 `(-1,-1)`、均衡 `(0,0)`、高速 `(-1,+1)`、高存储 `(+1,-1)`、最终 `(+2,+2)`。单卡容量 `C(d,t)=C_d×2^a`；驱动器有效带宽 `B=max(0,B_d+Σ round(B_d×(2^v-1)/S_d))`。空驱动器保留基础速度，均衡满盘保持原标定。

每个逻辑 AE 网络最多统计两台 Drive。第三台会使网络容量、带宽和输出归零，但 Drive 内已有 Card 与 SU 不会丢失；移除多余 Drive 后自动恢复。

## 应力守恒

```text
fullLoad       = Create 网络在有效 RPM 下的完整应力负载
requestedSu    = ceil(fullLoad)
stockQ         = storedSu / requestedSu
rpmQ           = bandwidthRpm / requestedRpm，低于 0.6 时归零
q              = min(stockQ, rpmQ)
outputRpm      = round(effectiveRpm * q)
chargedSu      = min(requestedSu, 当前可用 SU)
perRpmCapacity = chargedSu / outputRpm
```

ME Gearbox EXPORT 只有在负载、有效 RPM、实际输出 RPM、实际扣费和 `q > 0` 全部有效时才运行；任一条件缺失都会同时清除输出速度与宣告容量。没有 SU、没有 RPM 带宽、没有下游机器负载或 RPM 覆盖率低于 60% 时，全网机械动力停止。

RPM 需求以每台 EXPORT 齿轮箱为一个占用单位，同一 AE 网络内求和；多台下游机器只放大该齿轮箱的 SU 应力需求，不重复占用 RPM。机器所在齿轮箱暂时停转时，机器仍保留其 Create 应力需求，避免“机器等齿轮箱、齿轮箱等负载”的死锁。

ME Gearbox EXPORT 向 Create 网络宣告的容量严格等于本次实际扣除的 SU。下游机器在 `outputRpm` 下最多只能消费 `chargedSu`；超出部分触发 Create 原有应力过载，不能无中生有。

## Jade

- 合格 Mekanism 加工机器的 Mekanism FE/J 元素被移除。
- 本 Mod 的 MEK Jade 条目显示当前有效 RPM、`RPM × 8` 的 SU/t 占用和当前工作倍率。
- 非加工 MEK 设备的 Jade 能量显示保持不变。

## 工程接口

- `memory_drive_ddr1..5`：使用 AE2 驱动器背景，每页 4 列 × 5 行共 20 槽，按 DDR 等级只接受同等级 Memory Card。
- `memory_card_ddrN_{economy,balanced,high_speed,high_storage,defective,final}`：只影响所在驱动器的容量与有效带宽。
- 正常卡配方每次固定产出 4 张；每张独立有 25% 概率变为同 DDR 劣质卡，自动合成假玩家跳过该判定。
- 经济型、均衡型、高速型、高存储型可合成；劣质型只作副产物，最终型仅注册，毕业配方待定。
- `appliedcreate:me_gearbox`：IMPORT 输入应力到 AE，EXPORT 从 AE 输出应力并执行 SU 守恒。
- `/dw memory`：读取附近已连接 Drive 所在逻辑网格的状态快照。

所有平衡值位于 COMMON 配置 `dimensionworks_mek_stress-common.toml`。槽数、结构规则、两台 Drive 上限、公式和齿轮箱守恒边界不可配置。

## 构建

```sh
gradle build --no-daemon
```
