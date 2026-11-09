# KubeJS

存放 DimensionWorks 的 KubeJS 脚本、配方、标签和数据。

建议结构：

- `startup_scripts/`
- `server_scripts/`
- `client_scripts/`
- `data/`
- `assets/`

## ME 动力路线配方

`server_scripts/me_memory.js` 提供 20 种正常卡配方、16 种四卡逐级升级配方和五代驱动器配方。DDR1 使用工程处理器与强化合金；后续驱动器用一台前代驱动器、四个对应电路和四个合金升级。升级回调保留原驱动器的内存卡 NBT 并删除旧 `StoredSu`；保留的旧代卡须另行升级至同代才生效。普通 DDR5 卡不再使用反物质。

`data/dimensionworks/recipes/advanced_speed_controller.json` 保留 5×5 动力合成图案，将原四个粉碎轮和四个电子管分别换为四个工程处理器、四个强化合金，使两条路线共用中后期工业材料。实时 SU/RPM 规则见 [`../docs/design/me-live-stress-network.md`](../docs/design/me-live-stress-network.md)。

## 能量板工业制造

`startup_scripts/energy_plate.js` 保留顺序装配中间物 `kubejs:incomplete_energy_plate`，新增物品需完整重启。成品直接使用 Mekanism 的 `mekanism:energy_tablet`，不再注册重复的自定义能量板。

`data/dimensionworks/recipes/energy_plate.json` 提供一次循环、固定产出 1 个 `mekanism:energy_tablet` 的 Create 顺序装配：1 块铜板经注液器消耗 250 mB 熔融金，再由机械手依次消耗 1 红石粉、1 灌注合金和 1 个 AE2 OMNI Cells 复杂链路处理器（`ae2omnicells:complex_link_processor`），最后机械压力机定型。处理器接入下界合金碎片电路板与印制硅的 AE 压印加工链。`server_scripts/me_memory.js` 只删除 Mekanism 原生能量板配方，保留这条新配方；能量立方和感应储能元件继续退役。

工艺成本、物流、绕过边界与验证要求见 [能量板工业制造](../docs/design/energy-plate-production.md)。

## 天境装备规则

`server_scripts/aether_equipment_lock.js` 禁用天境装备的制作、修复和正常获取流程，保留流程功能物。
配套的 `defaultconfigs/aether-client.toml` 与 `defaultconfigs/aether-common.toml` 隐藏天境饰品按钮，并启用 Curios 标准菜单。

## 暮色森林拆解台

`twilight_uncrafting_table.js` 移除拆解台配方，从暮色森林创造栏及 JEI 隐藏物品，清理玩家物品栏，并阻止放置和使用。已放置的拆解台在所在区块加载后移除；未加载的区块不会提前修改。箱子等容器内的存量不会主动扫描，进入玩家物品栏时才清理。启动脚本变更需要完整重启。

## 车万女仆妖精生成

`server_scripts/touhou_little_maid_fairy_natural_spawn.js` 通过生成检查与实体加入世界两层事件，禁止车万女仆的女仆妖精（`touhou_little_maid:fairy`）生成，包括自然生成、刷怪蛋、刷怪笼、命令和模组直接添加；存档中的妖精重新加载时也会被拦截。已加载的妖精不会因脚本热重载立即消失，需重新加载所在区块或重启服务端。此规则不扫描每 tick 的实体列表。

## 齿轮之心

`startup_scripts/gear_heart.js` 注册两个最高稀有度的齿轮之心和七个独立贴图的修复碎片；`server_scripts/gear_heart_recipes.js` 提供七个固定 ID 配方。
四张齿轮之心贴图均为 32x32；所有状态都不启用附魔光效。

| 物品 | `CustomModelData` | 状态 |
| --- | --- | --- |
| `kubejs:gear_heart` | `0` | 常态外观，携带七条未修复工业瓶颈 |
| `kubejs:gear_heart` | `1` | 破损外观，携带同样的七条未修复工业瓶颈 |
| `kubejs:gear_heart_enchanted` | `0` | 全链释限，不显示工业瓶颈 |

`kubejs:gear_heart` 本身就是带工业瓶颈的物品，不需要手动设置 `CustomModelData`；物品说明会直接列出当前七条工业瓶颈，已破除项在原位置改为绿色效果。
`kubejs:gear_heart_enchanted` 的旧 `CustomModelData=2` 受损物品会在装备后自动迁移为
破损态 `kubejs:gear_heart`。`kubejs:gear_heart_repair_fragment_1` 至 `_7` 对应七个固定配方，
右键已装备的破损齿轮之心必须按 1 到 7 的顺序破除七条工业瓶颈。

`data/curios/` 新增专属 `gear_heart` 槽，只接受齿轮之心；装备后由 RPM Limit 模组拦截
手动取下并设为死亡保留。未完全修复时提供方块与实体交互距离 +2、急迫 II、夜视 I，造成伤害降低 10%；全链状态改为交互距离 +4、急迫 III、夜视 I，造成伤害提高 50%，不叠加原惩罚。伤害按小数倍率结算，不取整。

初始上限为 32 RPM，七次修复依次使用固定档位 512、1,024、2,048、4,096、5,120、8,192、10,240 RPM。第三项立即移除矿物处理次数额度，第四项立即移除 AE2 自动合成并发额度；其他未修复项继续独立生效。物品说明显示当前连续修复阶段及其转速上限，已有存档由 RPM Limit 自动按进度恢复。
工业额度运行效果与破除位写入同一模组，详见 `mods/dimensionworks-rpm-limit/README.md`。

贴图生成流程见 `scripts/README.md`。

## 洞穴维度工厂数据

`data/dimensionworks_cave_factory/recipes/survival/` 提供渊海端生存获得链；`recipes/cave_processing/`、`recipes/fluid_stabilization/` 和 `recipes/module_recipe_rule/` 分别承载签名机加工、稳定化与机制模块映射。
`data/ae2/recipes/network/cells/` 把渊压基质接入 128³ 空间存储元件配方。

## 洞穴基础产物自增殖

`server_scripts/cave_self_propagation.js` 为六维共 18 项基础产物各提供一条 Create 主链和一条残渣备链，不依赖洞穴工厂签名机。

- 主链：`dimensionworks_cave_factory:self_propagation/<dimension>/<target>_primary`
- 备链：`dimensionworks_cave_factory:self_propagation/<dimension>/<target>_recovery`
- 中间物：`dimensionworks_cave_factory:self_propagation/support/<dimension>/<name>`

Create 的 Mixer、Press、Crushing Wheels、Saw、Deployer、Spout、Fan 等加工配方每个条目只消耗一个物品，所以 `"2x item"` 的写法会由脚本中的 `expandIngredients` 展开成重复条目。调整配比时只需改这个前缀。

目标组标签位于 `data/dimensionworks_cave_factory/tags/items/self_propagation/` 与同结构的 `tags/fluids/self_propagation/`，供任务、JEI 说明与后续模块映射引用。

## 基础矿物与常规金属三路线

本包将原版粗铜（`minecraft:raw_copper`）显示为“氧化铜”，粗铜块显示为“氧化铜块”；英文分别为 Copper Oxide 和 Block of Copper Oxide。语言覆盖位于 `assets/minecraft/lang/`，仅调整显示名称，物品 ID、纹理、标签、默认熔炉和高炉烧制均保留。不新增其他现实铜化合物。铜、黄铜、青铜及相关加工/用途配方盘点见 [铜配方清单](../docs/design/copper-recipe-inventory.md)。

server_scripts/basic_mineral_propagation.js 为基础矿物目标配置 Create 固体压制与 Mekanism 合成路线。压制线要求对应目标的有限样本作为输入。

铜改用 `server_scripts/copper_processing.js`：氧化铜 + 250 mB 一氧化碳在普通加热工作盆中搅拌得到 1 铜锭 + 250 mB 二氧化碳；铜锭经 Create 粉碎轮产出 1 个保底铜粉、2 个各 80% 概率铜粉、1 个 60% 概率铜粉（期望 3.2 个）；4 铜粉 + 1 圆石加热搅拌得到 2 辉绿岩；每个辉绿岩经 Mekanism 提纯室耗氧得到 2 氧化铜。旧的铜双路线增殖和原生铜碎块提纯配方移除。默认烧制、高炉烧制及其他未指定铜加工保留。

煤炭自增殖配方已移除，仅规划木炭化学合成来源；现已实现木炭制气，后续制煤工序待定。

## 余烬面粉与红石

`server_scripts/cinder_flour_recipes.js` 将 Create 原版下界岩粉碎配方改为固定产出 1 个余烬面粉，另有 2 个各以 85% 概率、2 个各以 89% 概率额外产出（逐个物品独立判定）；4 个余烬面粉可在工作盆压制成下界岩。余烬面粉经注液器注入 250 mB Create: Diesel Generators 原油，可制成 1 个红石粉。旧的红石矿物自增殖线已移除。

完整矿物形态清单、路线分类和验收步骤见 `../docs/design/basic-mineral-production-lines.md`。

## 稳定硫供应

`server_scripts/sulfur_processing.js` 新增普通加热工作盆搅拌：4 个余烬面粉 → 1 个 `kubejs:crude_sulfur`（粗硫）；Create 磨石将 1 个粗硫固定磨成 1 个 `mekanism:dust_sulfur`。粗硫由 `startup_scripts/mineral_processing_items.js` 注册，复用原版粗金纹理；更新后需要完整重启以加载新物品。

下界岩粉碎与压块回流沿用原配方。优先回流母料后，每加工一个下界岩期望剩余 0.48 个余烬面粉，即长期平均约 8.33 次粉碎净产一份硫；概率波动需要库存缓冲。余烬面粉同时供红石与硫使用。

下界岩压块回流的四份面粉已展开为四个独立输入条目，修正旧 `4x` 写法导致实际扣料不足的风险。

移除 Mekanism `reaction/coal_gasification/` 下的 `coals`、`dusts_coals`、`blocks_coals` 三条原生煤炭/木炭制硫配方，保留火药制硫及其他硫化学。`forge:dusts/sulfur` 合并 Mekanism 与 Alex’s Caves 硫粉，硫化氢和铀精矿配方改用该标签；旧洞穴配方的硫粉输出和任务物品保持兼容。

工艺设定、产能与验证边界见 [稳定硫供应设计](../docs/design/sulfur-production.md)。

## 烈焰蛋糕红石成本

`server_scripts/blaze_cake_redstone_cost.js` 统一替换所有产出 `create:blaze_cake_base` 的配方，要求鸡蛋、糖、灰烬粉（Create Cinder Flour）和 1 份红石粉经机械压制合成底料。原版将底料注入 250 mB 熔岩制成烈焰蛋糕的配方保留不变；这样附属模组若提供其他底料配方，也不会绕过红石成本。

## 熔融金属与特殊材料

`startup_scripts/molten_metals.js` 通过可复用的 `MoltenMetalFluidDefinition` 注册 21 种熔融金属、合金与特殊材料流体，使用 `dimensionworks:` 命名空间并提供中英文名称。该脚本只注册流体，不自动添加熔炼、浇注或材料加工配方；这些配方应在后续服务端脚本中按各机器的实际能力分别实现。

`server_scripts/molten_metal_recipes.js` 将对应锭熔成 1,000 mB（1 B）流体，并定义黄铜、青铜熔融混合配方。21 种熔融流体的冷却配方位于 `data/create_dragons_plus/recipes/freezing/`：使用 Create: Connected 冷冻触媒和 Create: Dragons Plus 鼓风冷冻类型处理 1,000 mB 熔融流体桶，输出空桶与 1 个对应锭，保持 1 个锭与 1 B 流体一一对应。

## Create 染色触媒兼容

`data/create_dragons_plus/tags/blocks/fan_processing_catalysts/coloring/` 为 Create: Connected 的 16 色染色触媒补充 Create: Dragons Plus 所需的方块标签。Create: Connected 1.2.3 只为 Garnished 生成了染色触媒兼容标签，漏掉 Dragons Plus 使用的标签；补齐后，鼓风机可通过对应触媒执行批量染色。标签采用可扩展合并，不替换其他数据包内容。

## 高中化学核心气体

- `startup_scripts/chemistry_gases.js` 注册 Mekanism 原版未提供的核心化学品气体，使用独立的 `dimensionworks:` 命名空间，避免覆盖 Mekanism 原版气体。
- `server_scripts/chemistry_gas_recipes.js` 添加对应的 Mekanism 化学氧化室、化学灌注器、加压反应室与太阳中子活化器配方。
- 当前新增：二氧化碳、一氧化碳、氮气、氨气、一氧化氮、二氧化氮、一氧化二氮、硫化氢、甲烷、乙炔、臭氧、硝酸；氢气、氧气、氯气、二氧化硫、三氧化硫、氯化氢、乙烯等复用 Mekanism 原生化学品。
- 这些 Mekanism 气体是游戏内化学品储罐类型；注册为 gas 不代表该物质在常温常压下必然是气态。颜色用于区分，不代表真实颜色。
- 获取方式为整合包生产线的简化模型，并非每条配方都逐原子复刻实验室制备；继续扩充时应优先保持化学反应方向、元素守恒和工业流程合理性。


## Create 工作盆 Mekanism 气体输入与输出

- 工作盆拥有独立的进气、出气缓冲，各 10,000 mB、每个缓冲只存一种气体。管道灌入进气缓冲、抽取出气缓冲；制气盆的 CO 需要通过管道送入反应盆。
- Create 配方以内部物品 `dimensionworks_mek_stress:gas_output` 的 NBT `Gas`（注册 ID）和 `Amount`（mB）指定气体产出，可与普通物品同时产出，标记不会掉落。可选的 `InputGas` 与 `InputAmount` 指定进气消耗；`InputOnly: true` 表示只消耗进气、不占用出气缓冲。气体不足、气体类型不匹配或产物无空间时不能加工。可选 `ExcludeSuperheated: true` 限制配方不能在超级加热时执行，匹配与实际执行都检查。
- `server_scripts/chemistry_gas_recipes.js` 提供木炭熔炼配方：工作盆普通加热每消耗 1 个木炭产出 125 mB 一氧化碳，超级加热每消耗 1 个木炭产出 250 mB 二氧化碳，由 Mekanism 气体管道抽取。木炭化学氧化室产一氧化碳入口已移除。煤炭增殖及煤炭制二氧化碳入口已移除；后续制煤工序待定。
- 工作盆无盖或开盖时，每 20 游戏刻合计流失 200 mB，按进气、出气缓冲存量比例分摊；总量不足时清空。只有盆正上方关闭的柴油动力工作盆盖（`createdieselgenerators:basin_lid`）能阻止流失；屋顶、普通方块和搅拌器不视作密封盖。所有盆内 Mekanism 气体均遵循此规则，与气体种类无关，液体不受影响。

其他气体配方可复用同一输出标记，并按需设置进气要求；进气缓冲只通过配方消耗，出气缓冲可由管道抽空。切换产出气体前需抽空出气缓冲。

## Create 烈焰人燃烧室柴油动力燃料

`data/dimensionworks/blaze_burner_fuel/` 为 Create: Liquid Fuel 注册 Create: Diesel Generators 的四种有效发动机燃料：柴油、生物柴油、汽油和乙醇。柴油、生物柴油与乙醇提供普通加热；汽油提供超级加热。

燃料定义使用 Create: Liquid Fuel 2.2.0 的 `fluid`、`burnTime`、`superHeat` 和 `amountConsumedPerTick` 字段。当前平衡值分别为柴油 32、生物柴油 24、汽油 24、乙醇 16，均每 tick 消耗 1 mB；这些是整合包自定义燃烧参数，不是柴油动力引擎 SU 参数的直接换算。

原油与植物油未加入：当前柴油动力版本没有为它们注册对应的发动机 FuelType，避免将原料油误作可直接驱动发动机的成品燃料。这些数据只影响 Create 烈焰人燃烧室，不会改变柴油动力引擎自身的燃料倍率或燃烧器规则。

## 配方加载兼容

`startup_scripts/mekanism_schema_compat.js` 对当前 KubeJS Mekanism 插件无法正确解析的 rotary、washing、reaction、separating、evaporating 类型使用原始 JSON schema，保留 Mekanism 自身对配方的解析和校验。其余正常工作的类型（包括 crushing、combining）保留插件的类型化接口；上述五种类型的新配方使用 `event.custom`。

部分上游集成配方缺少依赖条件：ComputerCraft 网络监视器、暮色森林酒类配方、已不存在的驴肉配方及未注册的熔岩渔网配方在对应依赖或物品不存在时跳过加载，保留原材料与产出定义。AE2 128³ 空间存储元件配方删除未使用的图案键，修复配方被拒绝加载的问题。Cultural Delights 0.16.7 内置的四个原版汤品覆盖 JSON 为 0 字节，KubeJS 以 1.20.1 原版合法配方重新提供同 ID 文件，恢复甜菜根汤、蘑菇煲和两种兔肉煲并消除 EOF 解析报错。

## 下界岩自增期望

`server_scripts/cinder_flour_recipes.js` 的每个下界岩产出 1 个保底余烬面粉、2 个各有 85% 概率的面粉和 2 个各有 89% 概率的面粉，期望共 4.48 个。4 个面粉压回 1 个下界岩后，期望净余 0.48 个面粉，等价于净增 0.12 个下界岩（12%）。概率按每个物品独立计算；循环需保留库存缓冲，不能保证每轮都盈利。

## 森罗物语配方接口

清单加入 KubeJS Kaleidoscope 1.1.0，提供上游森罗配方联动接口；本次保留附属原生配方，未新增自定义配方脚本。附属清单、依赖与兼容性边界见 [食物附属扩展](../docs/design/food-addon-expansion.md)。

## 魔法皮蛋

`server_scripts/magic_century_egg.js` 移除原皮蛋及其后续皮蛋粥配方，并从创造栏、JEI 和玩家物品栏清除原皮蛋。新链先在工作盆中用 1 个鸡蛋、100 mB 水、100 mB 熔融铁和 100 mB `mekanism:spent_nuclear_waste` 搅拌；用尽的核废料通过工作盆进气缓冲纯消耗。产物随后熔炼、洗涤，再进入两段各 7 步的 Create 序列组装：第一段 8 轮、每轮冲压 7 次，产出紧压皮蛋芯；第二段 8 轮、每轮冲压 3 次、切割 1 次、注入 1 B 熔融下界合金，并依次装配暮色森林任意原木和 `ae2omnicells:quantum_omni_cell_component_1k`。总冲压次数仍为 80 次，JEI 不会出现超长的 14 步单配方。

成品 `kubejs:magic_century_egg` 使用稳定的 Epic 品质名称和简短双行说明。食物属性直接提供伤害吸收 X、漂浮 II、跳跃提升 V、速度 V、抗火 V；食用回调再为其余有益效果添加 V 级增益，所有效果持续 3 分钟。首次食用启动 3 分钟死亡倒计时；倒计时期间再次食用会立即触发死亡和烟花，不会刷新倒计时。死亡时造成一次 `9,999,999,999,999` 点 `genericKill` 伤害，并在死亡位置播放三轮各 5 秒、覆盖 9×9×9 范围的浓密烟花粒子。实际饥饿值取原版上限 20 点。

## AE2 能源接入

`startup_scripts/ae2_energy_acceptor.js` 从 AE2 创造栏移除能源接收器与能源接收线缆；`server_scripts/ae2_energy_acceptor.js` 删除其合成配方，并将使用能源接收器作为材料的配方改为应用机动的 `kinetic_energy_acceptor`。`client_scripts/ae2_energy_acceptor.js` 同步隐藏 JEI 条目。ME 网络保留应用机动应力接收器作为能源输入端。
