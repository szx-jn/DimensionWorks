# DimensionWorks ME Mechanical Power

把 Create 机械动力、Mekanism 机器和 AE2 内存网络连接成两条明确边界：

- AE 侧库存由 Memory Drive 与 Memory Card 提供。Gearbox IMPORT 把 Create 应力存入网络，Gearbox EXPORT 从网络扣除 SU 后输出应力。
- Create 侧动力可由相邻动力网络中的任意有效动力源提供；ME Gearbox EXPORT 是其中一种源，普通传动杆、齿轮组和其他有源 Create 网络也能直接接入。
- 机器能量采用二元供能：每次能量接口读取时动态检查当前 Create 应力路线；路线存在、当前 RPM 有效且网络未过载时虚拟返回满能量，否则返回零。虚拟能量不写入 Mekanism 的真实 `MachineEnergyContainer`，并在序列化时输出空能量 NBT；世界保存不会将机械动力伪装成可持久化的 FE。单台机器在正常服务端 tick 中复用该 tick 已计算的路线。充能器也属于合格机器，按基础档 `1024 RPM` 接入。RPM 低于机器目标时保留满能量契约，但按 `min(1, 有效 RPM / 目标 RPM)` 降低工作速度。RPM 高于目标时，生产倍率按 `有效 RPM / 目标 RPM` 提升，并在 Mekanism 输出边界统一作用于物品、气体、流体、复合输出和随机副产物；每个逻辑产物先按该 tick 的实际操作次数求和，再乘倍率并向下取整，避免概率副产物被重复放大。
- 合格 Mekanism 机器会检查主机与全部 Mekanism 包围方块的外表面；任意相邻 `KineticBlockEntity` 只要其轴朝向其中一面就可以接通。机器作为虚拟成员加入实际的 `KineticNetwork.members`，因此它按 Create 已传播的网络转速幅值计算 `abs(RPM) × 8` 应力并直接计入网络总量与过载判定，不再只是显示值；空闲机器也保留零速虚拟成员。机器拒绝一切 FE 输入。
- MEK 直接动力与 ME Gearbox EXPORT 在读取 Create 已传播的网络转速后，先按 `abs(RPM)` 取速度幅值，再经过玩家的有效 RPM 上限，最后受 `directMaxRpm` 收口。顺时针与逆时针的等幅转速获得相同的有效 RPM 和应力计费，零转速仍无动力。原始转向只用于 Create 网络传播；MEK 消费、Jade 显示与虚拟成员负载都使用收口后的非负有效转速，因此玩家上限降低后，MEK 不会继续按旧的高速运行。
- 四档目标 RPM 分别为基础 `1024`、高级 `2048`、精英 `4096`、终极 `8192`。每台机器向所在 Create 网络施加 `abs(当前 RPM) × 8` 的应力需求，不区分正转和反转；网络应力不足时由 Create 规则判定过载。

本 Mod 不分发或替换 Applied Create、AE2、Mekanism、Create 的源码；兼容通过自研 Mod 与运行时 Mixin 完成。Applied Create 原有的应力存储 Cell、组件、外壳和创造 Cell 全部退役，不能继续挂载、读写或供应 `StressKey`。
Mekanism 的能量立方、能量平板、感应 Cell 与感应 Provider 由 KubeJS 退役：移除配方、移出创造栏、隐藏 JEI、阻止拾取和使用，并清除玩家库存。为避免配置器、碎星者、激光器等非储能配方失去材料，残留配方中的 `mekanism:energy_tablet` 输入会替换为物品标签 `#mekanism:alloys/infused`。感应外壳与感应端口不属于储能元件，继续保留；没有 Cell 与 Provider 后无法组成可储能的感应矩阵。

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

## 输入应力扣除

同一 ME 网络中所有已安装内存卡的扣除贡献相加，并统一封顶为 100%。每张卡的贡献为：劣质型 20%、经济型 2%、均衡型 1.5%、高速型 1.75%、高存储型 0.75%、最终型 0%。网络有效输入应力为 `原始输入总应力 × (1 - min(总扣除比例, 100%))`；因此即使扣除比例之和超过 100%，有效应力也不会为负。计算使用整数基点并跨分次输入累计小数余量，避免不同齿轮箱分开输入时因逐次舍入产生偏差。

每个逻辑 AE 网络最多统计两台 Drive。第三台会使网络容量、带宽和输出归零，但 Drive 内已有 Card 与 SU 不会丢失；移除多余 Drive 后自动恢复。

## 应力守恒

```text
idleLoad       = effectiveRpm * 8
fullLoad       = max(idleLoad, Create 网络完整应力负载)
requestedSu    = ceil(fullLoad)
rpmQ           = min(1, bandwidthRpm / requestedRpm)
requiredSu     = ceil(requestedSu * rpmQ)
stockQ         = storedSu >= requiredSu ? 1 : 0
q              = min(stockQ, rpmQ)
outputRpm      = round(effectiveRpm * q)
chargedSu      = requiredSu
perRpmCapacity = chargedSu / outputRpm
```

空载 EXPORT 齿轮箱的基础负载仍为 `有效 RPM × 8`；Mekanism 机器本身按 `有效 RPM × 256` 计费。接入下游机器时取“基础负载”与“实际完整应力负载”的较大值，因此单台标准机器仍保持原有 `RPM × 256` 标定，多台机器会继续按总负载增长。

ME Gearbox EXPORT 只有在有效 RPM、实际输出 RPM、实际扣费和 `q > 0` 全部有效时才运行；任一条件缺失都会同时清除输出速度与宣告容量。`storedSu < requiredSu` 时整台齿轮箱无能量输出，不允许用库存比例赊速；RPM 带宽不足时则按比例输出转速。没有下游机器时，齿轮箱仍按基础负载运行。

RPM 需求以每台 EXPORT 齿轮箱为一个占用单位，同一 AE 网络内求和；下游机器只放大该齿轮箱的 SU 应力需求，不重复占用 RPM。齿轮箱不需要等待下游机器出现才开始运行，因此不会形成“机器等齿轮箱、齿轮箱等负载”的死锁。

ME Gearbox EXPORT 向 Create 网络宣告的容量严格等于本次实际扣除的 SU。下游机器在 `outputRpm` 下最多只能消费 `chargedSu`；超出部分触发 Create 原有应力过载，不能无中生有。

## Jade

- 所有 Mekanism 方块的 Mekanism FE/J 元素，以及 Jade 通用 FE/J provider 对 Mekanism 方块的显示，都被移除。
- 本 Mod 的 MEK Jade 条目显示当前有效 RPM、`RPM × 256` 的 SU/t 占用和当前工作倍率。
- MEK Jade 的“生产倍率”显示端到端吞吐倍率：低于目标时小于 `1x`，达到目标为 `1x`，高于目标时按比例大于 `1x`；配方内部仍不会在低转速时重复放大产出，避免二次取产。
- 非加工 MEK 设备仍不会出现本 Mod 的机械动力条目。

## 工程接口

- `memory_drive_ddr1..5`：使用 AE2 驱动器背景，每页 4 列 × 5 行共 20 槽，按 DDR 等级只接受同等级 Memory Card。
- `memory_card_ddrN_{economy,balanced,high_speed,high_storage,defective,final}`：影响所在驱动器容量、有效带宽，并向所在 ME 网络贡献输入应力扣除比例。
- 正常卡配方每次固定产出 4 张；每张独立有 25% 概率变为同 DDR 劣质卡，自动合成假玩家跳过该判定。
- 快速移动合成时，只有成功从玩家背包扣除的正常卡才会转换为劣质卡；无法扣除的部分不会额外生成副产物。
- 经济型、均衡型、高速型、高存储型可合成；劣质型只作副产物，最终型仅注册，毕业配方待定。
- `appliedcreate:me_gearbox`：IMPORT 输入应力到 AE，EXPORT 从 AE 输出应力并执行 SU 守恒。
- `/dw memory`：读取附近已连接 Drive 所在逻辑网格的状态快照。

容量、带宽、机器转速等平衡值位于 COMMON 配置 `dimensionworks_mek_stress-common.toml`；内存卡输入应力扣除比例由卡型固定定义。槽数、结构规则、两台 Drive 上限、100% 扣除上限和齿轮箱守恒边界不可配置。

## Create 动力网络统计缓存

0.3.6 起，虚拟 Mekanism 机器参与同一套可失效的应力总账。网络成员变化时缓存立即失效；重算仍会检查虚拟成员有效性，避免区块卸载或设备移除后继续计入过期负载。发电机更新的批处理和增量统计由依赖的 `dimensionworks-rpm-limit` 提供。

兼容性运行边界：

- Mekanism 调度器只在连续、同转速的 tick 上累积小数工作量；停机、区块重载或变速后不会补执行历史 tick。
- AE2 Memory Drive 与 ME Gearbox 只在节点 `isActive()`（供电、启动且满足频道）时参与容量、带宽和导出。
- Mek 虚拟成员在 `setRemoved` 时解除；官方映射 refmap 已验证为 `m_7651_()`。
- 能量容器机械供能契约只在服务端生效，客户端继续显示 Mekanism 原始能量状态。
- 同一台机器的服务端 tick 内复用一次动力路线（包括无路线结果），避免能量读取重复扫描多方块脚印与 Create 网络；路线检查只读取已加载区块，绝不因寻找相邻轴系而同步加载新区块；能量 NBT 序列化直接写空标签，不会触发虚拟能量路线查找。
- 多台 Mek 机器引起的变化按网络标记，服务器 tick 末每张网络最多全量刷新一次。
- 超速缩放后的物品、流体和化学品在容量检查阶段申请第七项最终产物额度；额度不足时不开始该批操作。

## 构建

```sh
gradle build --no-daemon
```
