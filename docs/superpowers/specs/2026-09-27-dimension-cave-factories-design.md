# 洞穴维度工厂设计

日期：2026-09-27

## 目标

为 Alex's Caves 的六个洞穴维度增加统一骨架下的专属工厂。工厂属于后期生产层，保留 Create、Mekanism 与 AE2 原有基础科技树，通过效果流体、稳定基质、模块与少量高级接点形成跨维度工业网络。

首个实现里程碑交付共享结构、稳定器、模块协议、五类 Create 机器兼容层与渊海陷窟端到端样板。其余五维注册稳定 ID 并进入创造预览，但暂不提供生存配方。

## 独立 Mod

- modId：`dimensionworks_cave_factory`
- 归档名：`dimensionworks_cave_factory`
- group：`dev.szx.dimensionworks`
- 初始版本：`0.1.0`
- Java 17、Minecraft 1.20.1、Forge 47.4.16
- 编译期使用 Gradle 复合构建依赖相邻的 `dimensionworks-rpm-limit` 工程。
- 强制依赖 Create、`dimensionworks_rpm_limit`、Alex's Caves 与 Dimensions of Alex's Caves。
- 不依赖 KubeJS 编译期 API；配方、标签和映射由整合包中的 KubeJS 数据提供。

## 多结构

每台签名机和通用稳定器共享同一种结构语言。首版使用 3x3x3 完整结构：

- 中心是维专属控制器 BlockEntity。
- 六个面心位置是通用端口，用扳手配置为动能输入、物品输入、物品输出、流体输入、流体输出或停用。
- 其余 20 个位置是共享工厂机壳，不创建 BlockEntity、不独立 tick。
- 每台结构最多一个动能端口。
- 任意机壳或端口右键打开控制器 GUI；手持 DimensionWorks 模块潜行右键打开模块页面。
- 方块损坏、结构变化或端口配置只在变化时通知控制器，不进行每 tick 全结构扫描。

控制器 GUI 保存：

- 4 个物品输入槽
- 4 个物品输出槽
- 1 个来源残渣输出槽
- 1 个空封装输入槽
- 1 个满封装输出槽
- 1 个数值模块槽
- 1 个机制模块槽

输入物品缓存为 4 组，输出物品缓存为 4 组。输入流体腔默认 `4000 mB`，输出流体腔默认 `8000 mB`。压力缓冲模块按 COMMON 配置的数值百分比提高适用腔体容量，首版默认 25%。

## 动力学与超速

- 所有签名机与稳定器使用 512 RPM 饱和点和 64 SU/RPM 应力影响。
- 玩家 RPM 上限和应力修正通过 `RpmLimitManager` 读取。
- 超速倍数使用 `OverspeedCurve` 的全局断点表。
- 机器值从理论转速计算，避免过载时应力抖动。
- 加工入口按确定性批次重复，不重复整套 tick 或动画状态。
- 护目镜与 Jade 显示有效转速、饱和点、超速批次数和应力状态。
- 数值模块不得修改 RPM 或应力倍率。

## 相位

每台签名机有两个固定相位：

| 机器 | 相位 A | 相位 B |
| --- | --- | --- |
| 极性冶炼塔 | 赤相 | 青相 |
| 原生质生物反应器 | 孕育 | 演化 |
| 同位素衰变炉 | 稳定 | 衰变 |
| 渊压萃取塔 | 增压 | 泄压 |
| 暗相谐振器 | 暗相 | 显相 |
| 糖晶析出器 | 溶解 | 结晶 |

规则：

- 每完成 8 个成功批次自动推进到另一相位。
- 红石通电时锁住当前相位，但机器继续加工。
- 两个相位共享主输入但允许不同物品、副产或流体输出。
- 相位进度保存在控制器 NBT。
- 维度不匹配或动力不足时相位冻结。

## 配方

配方使用新数据驱动类型：

- `dimensionworks_cave_factory:cave_processing`：签名机配方，包含机器 ID、相位、物品/流体输入、物品/流体输出、处理时长、可选必需模块和可选来源残渣。
- `dimensionworks_cave_factory:fluid_stabilization`：通用稳定器配方，默认处理 `1000 mB` 效果流体并输出对应稳定基质。
- `fluid_stabilization` 配方只需指定流体与结果；未显式写 `amount` 时使用 COMMON 配置的 `stabilizationInput`，默认 `1000 mB`。
- `dimensionworks_cave_factory:module_recipe_rule`：显式列出机制模块可增强的 Create recipe ID 与数值模块行为映射。

所有具体配方、标签和映射由 `kubejs/data/dimensionworks_cave_factory/` 管理。Java 只实现引擎和安全校验。

## 输出与故障

输出判定是原子的：

- 物品输出、残渣输出和流体输出必须全部容纳，整轮才成功。
- 任一输出不足时，本轮输入全部消耗，只产出对应来源残渣。
- 启动前缺少输入、动力、维度或结构时只停机，不消耗材料。
- 过载、网络未加载或维度不匹配不写残渣。
- 带液控制器拒绝扳手移动；方块被破坏时内部效果流体结算为来源残渣。
- Forlorn 的相位超存模块可在后续里程碑缓存确定性溢出批次，首版只注册预览。

## 模块

模块为独立物品、不可堆叠。每台兼容机器有一个数值槽和一个机制槽。

兼容规则：

- 插入前查询机器能力与模块映射，不兼容模块不能占槽。
- 数值模块只在适用字段存在时接受。
- 机制模块只在存在显式 recipe ID 映射时接受。
- 机器和模块归首个放置者所有；非所有者可查看但不能改配置或插拔。
- 拆机、扳手移动或 contraption 装配前弹出模块；模块优先返还所有者，否则在方块位置掉落。
- 首版唯一禁配是 `biomass_yield_amplifier + biomass_cultivator`。

数值模块采用确定性计数，不引入随机数。默认增益范围为 10% 到 25%，通过每第 N 次成功操作执行一次节约、增产、容量或副产效果。

## 效果流体与封装

六种流体分别是：

| 流体 ID | 中文名 |
| --- | --- |
| `polarized_flux` | 极性磁流质 |
| `primordial_protoplasm` | 原生质原浆 |
| `radionuclide_slurry` | 核素浆液 |
| `abyssal_brine` | 渊压卤液 |
| `umbral_condensate` | 暗相凝液 |
| `supersaturated_syrup` | 过饱和糖液 |

封装规则：

- `fluid_ampoule`，最大堆叠 16，单件容量 `250 mB`。
- 普通桶不提供装填配方。
- 封装保存 `origin_dimension` 与可选的 `expires_at_epoch_ms`。
- 首次由支持的转运路径确认离维时，整叠写入同一到期时间。
- 拆分复制同一时间戳；只有同时间戳堆可合并。
- 返回原生维度不暂停、不重置。
- 默认稳定窗口 60 秒，使用现实时间戳。
- 懒结算发生在玩家跨维、机器读取、灌装、打开菜单、转运和受支持 API 调用时。
- 过期返还空封装并生成一份来源残渣。
- 第三方未知储罐不提供全局拦截；工厂只保证受支持路径。

## 稳定器

`fluid_stabilizer` 是可在任意维度运行的通用多方块设备：

- 使用 Create 动能、512 RPM、64 SU/RPM。
- 接受六种效果流体。
- 默认消耗 `1000 mB` 流体输出一个稳定基质。
- 支持数值和机制模块的兼容映射。
- `pressure_buffer` 提高流体容量。
- `phase_converter` 提供渊海固液相转换配方。
- 稳定矩阵不参与离维倒计时。

## 六维内容

| 维度 | 控制器 | 主流体 | 基质 |
| --- | --- | --- | --- |
| 磁场 | `polarity_smelter` | `polarized_flux` | `polarity_matrix` |
| 原始 | `primordial_bioreactor` | `primordial_protoplasm` | `genesis_matrix` |
| 毒化 | `isotope_decay_furnace` | `radionuclide_slurry` | `decay_matrix` |
| 渊海 | `abyssal_pressure_tower` | `abyssal_brine` | `pressure_matrix` |
| 异寂 | `umbral_resonator` | `umbral_condensate` | `umbral_matrix` |
| 糖果 | `candy_crystallizer` | `supersaturated_syrup` | `crystal_matrix` |

主要投入：

- 磁场：钕材料、充能方铅岩、富铁黏液。
- 原始：南洋杉树脂、石灰岩、星树果。
- 毒化：铀材料、酸蚀辐射石、硫粉。
- 渊海：渊海石、生物荧光质、海雪。
- 异寂：秽岩、荆棘木、纯粹黑暗。
- 糖果：冰糖、明胶、紫色苏打。

以上 18 项基础产物都有不依赖签名机的 Create 自增殖循环（一主一备），详见 [`2026-09-27-cave-self-propagation-design.md`](2026-09-27-cave-self-propagation-design.md)。

模块：

| 维度 | 数值模块 | 机制模块 |
| --- | --- | --- |
| 磁场 | `flux_conservator` | `polarity_inverter` |
| 原始 | `biomass_yield_amplifier` | `biomass_cultivator` |
| 毒化 | `isotope_economizer` | `fission_reclaimer` |
| 渊海 | `pressure_buffer` | `phase_converter` |
| 异寂 | `umbral_capacity_core` | `phase_overbuffer` |
| 糖果 | `sugar_economizer` | `crystal_compressor` |

残渣：

`polarity_residue`、`genesis_residue`、`decay_residue`、`pressure_residue`、`umbral_residue`、`crystal_residue`。

## 首个实现里程碑

1. 建立新 Mod、复合构建和元数据。
2. 注册全部六维控制器、流体、基质、模块、残渣、机壳、端口、稳定器和空封装。
3. 实现相位、模块规则、封装时间戳、稳定化换算等纯逻辑。
4. 实现 3x3x3 控制器、端口能力、GUI、NBT、所有权和破坏处理。
5. 实现通用稳定器与渊海增压/泄压配方。
6. 为五类 Create 机器提供模块宿主；首版只启用 `pressure_buffer` 与 `phase_converter`。
7. 将两颗 `pressure_matrix` 加入 AE2 128³空间存储元件配方。
8. 提供 Abyssal Ponder、JEI 信息、Jade 状态、中英文资源和 README。
9. 添加 JUnit、GameTest、JSON 检查与真实客户端/服务端验收说明。

其余五维不带生存配方，控制/流体/基质/模块/残渣进入创造预览并在说明中标为未启用。

## 测试

- 纯逻辑：相位轮转、红石锁定、模块禁配、确定性计数、封装时间戳、拆分/合并、返维计时、残渣转换、稳定化比例。
- GameTest：3x3x3 组机、端口能力、原子输出、模块弹出、所有者权限、带液禁止拆除、配方重载缓存失效。
- 人工：512 RPM、超速并行、红石锁相、五类机器兼容、跨维封装、异地稳定、AE2 配方、双客户端权限。
- 性能：50 台运行、200 台加载，机壳无 BlockEntity，端口无独立 tick。

## 范围边界

- 不实现中转端口、端口号、路由或端口 GUI。
- 不把工厂代码写入 `dimensionworks_transfer` 或 `dimensionworks_rpm_limit`。
- 不硬编码基础 Mod 配方；仅通过 KubeJS 覆盖明确列出的高级接点。
- 不使用随机数决定模块增产或封装过期。
- 不承诺未知第三方储罐遵守离维时间戳。
- 不新增任何自制生物、实体模型或动画；涉及生物的内容只引用模组自带生物并做机制改动。
