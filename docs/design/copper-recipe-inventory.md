# 铜与衍生物配方清单

基线：DimensionWorks **0.2.63** 正式服务端包；结合当前仓库 KubeJS 脚本和数据覆盖。氧化铜只是 `minecraft:raw_copper` 的显示名调整，默认烧制、高炉烧制配方均保留。粗铜块同步显示为氧化铜块，不引入其他现实铜化合物。

## 范围与证据

- 扫描正式服务端包的 116 个数据来源（114 个模组 Jar、Forge 数据 Jar 和原版数据 Jar），读取 14883 条原始配方定义，并按配方 ID 合并覆盖。
- 范围包括氧化铜、铜矿与铜锭、铜粉/碎片/晶体/结晶块、铜建筑与零件、熔融铜、Mek 铜浆液，以及黄铜、青铜相关形态；列出直接消耗或产出这些材料的配方。不会沿机器配方继续递归，把所有后续机器都算成铜衍生物。
- 展开原包物品标签，核对 Forge 模组依赖条件；通过隔离的脚本接口采集器展开当前 KubeJS 配方注册、删除和输入替换。采集器不是 Minecraft/KubeJS 运行时，不能替代 JEI 和实际服务器最终注册表验证。
- “静态启用”表示依赖与已解析条件成立；模组自定义配置条件单列为“配置条件待核对”。被脚本删除和依赖不满足的配方在末尾另列，不计入主清单。
- Mekanism 气体输入的 amount 记录配方 JSON 标称量；部分机器按加工刻消耗气体，不能将这里的气体数值直接当作整次加工总耗量。序列装配的结果概率按各结果权重归一化。
- CSV 保留完整配方 ID、物料数量、概率、工艺参数与来源；工作台有序配方的输入按图案中的实际数量合计。“/”表示可选原料，“+”表示同时投入。
- 动态注册、世界交互、熔融金属露天结晶等不一定属于配方管理器，单独说明；此清单不声称已获取运行时最终注册表。

## 汇总

主清单共 **707** 条，其中配置条件待核对 **21** 条。完整结构化表见 [CSV](copper-recipe-inventory.csv)。

| 配方命名空间 | 条数 |
|---|---:|
| `ae2` | 15 |
| `alexscaves` | 2 |
| `appliedcreate` | 6 |
| `brewinandchewin` | 4 |
| `caves_delight` | 13 |
| `create` | 271 |
| `create_connected` | 11 |
| `create_dragons_plus` | 4 |
| `create_enchantment_industry` | 2 |
| `create_integrated_farming` | 1 |
| `create_mobile_packages` | 1 |
| `create_power_loader` | 1 |
| `createadditionallogistics` | 21 |
| `createdieselgenerators` | 14 |
| `dimensionworks` | 10 |
| `dimensionworks_cave_factory` | 4 |
| `farmersdelight` | 55 |
| `kaleidoscope_cookery` | 4 |
| `kaleidoscope_dim_wine` | 1 |
| `kaleidoscope_tavern` | 1 |
| `kubejs` | 1 |
| `l2complements` | 30 |
| `laowu` | 9 |
| `megacells` | 7 |
| `mekanism` | 93 |
| `mekanismdelight` | 2 |
| `minecraft` | 110 |
| `netmusic` | 4 |
| `sliceanddice` | 1 |
| `sophisticatedbackpacks` | 3 |
| `supplementaries` | 6 |

## 氧化铜与铜加工主干

| 配方 ID | 工艺 | 输入 → 输出 | 参数 |
|---|---|---|---|
| `create:crushing/raw_copper` | `create:crushing` | #forge:raw_materials/copper → 粉碎铜矿石 (create:crushed_raw_copper) + 经验颗粒 (create:experience_nugget) [75.0%] | {"processingTime":400} |
| `create:crushing/raw_copper_block` | `create:crushing` | #forge:storage_blocks/raw_copper → 9 × 粉碎铜矿石 (create:crushed_raw_copper) + 9 × 经验颗粒 (create:experience_nugget) [75.0%] | {"processingTime":400} |
| `create:mixing/brass_ingot` | `create:mixing` | #forge:ingots/copper + #forge:ingots/zinc → 2 × 黄铜锭 (create:brass_ingot) | {"heatRequirement":"heated"} |
| `create:pressing/copper_ingot` | `create:pressing` | #forge:ingots/copper → 铜板 (create:copper_sheet) |  |
| `create:splashing/crushed_raw_copper` | `create:splashing` | 粉碎铜矿石 (create:crushed_raw_copper) → 9 × 铜粒 (create:copper_nugget) + 黏土球 (minecraft:clay_ball) [50.0%] |  |
| `create_dragons_plus:freezing/molten_brass_to_ingots` | `create_dragons_plus:freezing` | dimensionworks:molten_brass_bucket → 铁桶 (minecraft:bucket) + 黄铜锭 (create:brass_ingot) |  |
| `create_dragons_plus:freezing/molten_bronze_to_ingots` | `create_dragons_plus:freezing` | dimensionworks:molten_bronze_bucket → 铁桶 (minecraft:bucket) + 青铜锭 (mekanism:ingot_bronze) |  |
| `create_dragons_plus:freezing/molten_copper_to_ingots` | `create_dragons_plus:freezing` | dimensionworks:molten_copper_bucket → 铁桶 (minecraft:bucket) + 铜锭 (minecraft:copper_ingot) |  |
| `dimensionworks:copper_processing/dust_from_ingot` | `create:crushing` | 铜锭 (minecraft:copper_ingot) → 铜粉 (mekanism:dust_copper) + 2 × 铜粉 (mekanism:dust_copper) [80.0%] + 铜粉 (mekanism:dust_copper) [60.0%] |  |
| `dimensionworks:copper_processing/reduction_with_carbon_monoxide` | `create:mixing` | 氧化铜 (minecraft:raw_copper) + 250 mB dimensionworks:carbon_monoxide → 铜锭 (minecraft:copper_ingot) + 250 mB dimensionworks:carbon_dioxide | {"heatRequirement":"heated","excludeSuperheated":true} |
| `dimensionworks:copper_processing/veridium_from_dust` | `create:mixing` | 铜粉 (mekanism:dust_copper) + 铜粉 (mekanism:dust_copper) + 铜粉 (mekanism:dust_copper) + 铜粉 (mekanism:dust_copper) + 圆石 (minecraft:cobblestone) → 2 × 辉绿岩 (create:veridium) | {"heatRequirement":"heated"} |
| `dimensionworks:molten_metals/brass_from_molten_metals` | `create:mixing` | 1000 mB dimensionworks:molten_copper + 1000 mB dimensionworks:molten_zinc → 2000 mB dimensionworks:molten_brass | {"heatRequirement":"heated"} |
| `dimensionworks:molten_metals/bronze_from_molten_metals` | `create:mixing` | 3000 mB dimensionworks:molten_copper + 1000 mB dimensionworks:molten_tin → 4000 mB dimensionworks:molten_bronze | {"heatRequirement":"heated"} |
| `dimensionworks:molten_metals/molten_brass_from_ingot` | `create:mixing` | 黄铜锭 (create:brass_ingot) → 1000 mB dimensionworks:molten_brass | {"heatRequirement":"heated"} |
| `dimensionworks:molten_metals/molten_bronze_from_ingot` | `create:mixing` | 青铜锭 (mekanism:ingot_bronze) → 1000 mB dimensionworks:molten_bronze | {"heatRequirement":"heated"} |
| `dimensionworks:molten_metals/molten_copper_from_ingot` | `create:mixing` | 铜锭 (minecraft:copper_ingot) → 1000 mB dimensionworks:molten_copper | {"heatRequirement":"heated"} |
| `dimensionworks:copper_processing/oxide_from_veridium` | `mekanism:purifying` | 辉绿岩 (create:veridium) + 1 mB 液态氧 (mekanism:oxygen) → 2 × 氧化铜 (minecraft:raw_copper) |  |
| `mekanism:processing/copper/ore/deepslate_from_raw` | `mekanism:combining` | #forge:cobblestone/deepslate + 20 × #forge:raw_materials/copper → 深层铜矿石 (minecraft:deepslate_copper_ore) |  |
| `mekanism:processing/copper/ore/from_raw` | `mekanism:combining` | #forge:cobblestone/normal + 20 × #forge:raw_materials/copper → 铜矿石 (minecraft:copper_ore) |  |
| `mekanism:processing/copper/dirty_dust/from_clump` | `mekanism:crushing` | #mekanism:clumps/copper → 污浊铜粉 (mekanism:dirty_dust_copper) |  |
| `mekanism:processing/copper/dust/from_ingot` | `mekanism:crushing` | #forge:ingots/copper → 铜粉 (mekanism:dust_copper) |  |
| `mekanism:processing/copper/crystal/from_slurry` | `mekanism:crystallizing` | 200 mB 纯净铜浆液 (mekanism:clean_copper) → 铜晶体 (mekanism:crystal_copper) |  |
| `mekanism:processing/copper/slurry/dirty/from_ore` | `mekanism:dissolution` | 1 mB 液态硫酸 (mekanism:sulfuric_acid) + #forge:ores/copper → 1000 mB 污浊铜浆液 (mekanism:dirty_copper) |  |
| `mekanism:processing/copper/slurry/dirty/from_raw_block` | `mekanism:dissolution` | 2 mB 液态硫酸 (mekanism:sulfuric_acid) + #forge:storage_blocks/raw_copper → 6000 mB 污浊铜浆液 (mekanism:dirty_copper) |  |
| `mekanism:processing/copper/slurry/dirty/from_raw_ore` | `mekanism:dissolution` | 1 mB 液态硫酸 (mekanism:sulfuric_acid) + 3 × #forge:raw_materials/copper → 2000 mB 污浊铜浆液 (mekanism:dirty_copper) |  |
| `mekanism:processing/copper/dust/from_dirty_dust` | `mekanism:enriching` | #mekanism:dirty_dusts/copper → 铜粉 (mekanism:dust_copper) |  |
| `mekanism:processing/copper/dust/from_ore` | `mekanism:enriching` | #forge:ores/copper → 2 × 铜粉 (mekanism:dust_copper) |  |
| `mekanism:processing/copper/dust/from_raw_block` | `mekanism:enriching` | #forge:storage_blocks/raw_copper → 12 × 铜粉 (mekanism:dust_copper) |  |
| `mekanism:processing/copper/dust/from_raw_ore` | `mekanism:enriching` | 3 × #forge:raw_materials/copper → 4 × 铜粉 (mekanism:dust_copper) |  |
| `mekanism:processing/copper/shard/from_crystal` | `mekanism:injecting` | 1 mB 液态氯化氢 (mekanism:hydrogen_chloride) + #mekanism:crystals/copper → 铜碎片 (mekanism:shard_copper) |  |
| `mekanism:processing/copper/shard/from_ore` | `mekanism:injecting` | 1 mB 液态氯化氢 (mekanism:hydrogen_chloride) + #forge:ores/copper → 4 × 铜碎片 (mekanism:shard_copper) |  |
| `mekanism:processing/copper/shard/from_raw_block` | `mekanism:injecting` | 2 mB 液态氯化氢 (mekanism:hydrogen_chloride) + #forge:storage_blocks/raw_copper → 24 × 铜碎片 (mekanism:shard_copper) |  |
| `mekanism:processing/copper/shard/from_raw_ore` | `mekanism:injecting` | 1 mB 液态氯化氢 (mekanism:hydrogen_chloride) + 3 × #forge:raw_materials/copper → 8 × 铜碎片 (mekanism:shard_copper) |  |
| `mekanism:processing/bronze/ingot/from_infusing` | `mekanism:metallurgic_infusing` | 10 mB #mekanism:tin + 3 × #forge:ingots/copper → 4 × 青铜锭 (mekanism:ingot_bronze) |  |
| `mekanism:processing/copper/slurry/clean` | `mekanism:washing` | 5 mB #minecraft:water + 1 mB 污浊铜浆液 (mekanism:dirty_copper) → 1 mB 纯净铜浆液 (mekanism:clean_copper) |  |
| `mekanism:processing/copper/ingot/from_dust_blasting` | `minecraft:blasting` | #forge:dusts/copper → 铜锭 (minecraft:copper_ingot) | {"cookingtime":100,"experience":0.35} |
| `mekanism:processing/copper/ingot/from_dust_smelting` | `minecraft:smelting` | #forge:dusts/copper → 铜锭 (minecraft:copper_ingot) | {"cookingtime":200,"experience":0.35} |
| `minecraft:copper_ingot_from_blasting_raw_copper` | `minecraft:blasting` | 氧化铜 (minecraft:raw_copper) → 铜锭 (minecraft:copper_ingot) | {"cookingtime":100,"experience":0.7} |
| `minecraft:copper_ingot_from_smelting_raw_copper` | `minecraft:smelting` | 氧化铜 (minecraft:raw_copper) → 铜锭 (minecraft:copper_ingot) | {"cookingtime":200,"experience":0.7} |

## 关键材料标签

| 标签 | 当前数据成员 |
|---|---|
| `#forge:dusts/bronze` | 青铜粉 (mekanism:dust_bronze) |
| `#forge:dusts/copper` | 铜粉 (mekanism:dust_copper) |
| `#forge:ingots/brass` | 黄铜锭 (create:brass_ingot) |
| `#forge:ingots/bronze` | 青铜锭 (mekanism:ingot_bronze) |
| `#forge:ingots/copper` | 铜锭 (minecraft:copper_ingot) |
| `#forge:nuggets/brass` | 黄铜粒 (create:brass_nugget) |
| `#forge:nuggets/bronze` | 青铜粒 (mekanism:nugget_bronze) |
| `#forge:nuggets/copper` | 铜粒 (create:copper_nugget) |
| `#forge:ores/copper` | 铜矿石 (minecraft:copper_ore) / 深层铜矿石 (minecraft:deepslate_copper_ore) |
| `#forge:plates/brass` | 黄铜板 (create:brass_sheet) |
| `#forge:plates/copper` | 铜板 (create:copper_sheet) |
| `#forge:raw_materials/copper` | 氧化铜 (minecraft:raw_copper) |
| `#forge:storage_blocks/brass` | 黄铜块 (create:brass_block) |
| `#forge:storage_blocks/bronze` | 青铜块 (mekanism:block_bronze) |
| `#forge:storage_blocks/copper` | 铜块 (minecraft:copper_block) |
| `#forge:storage_blocks/raw_copper` | 氧化铜块 (minecraft:raw_copper_block) |
| `#mekanism:clumps/copper` | 铜碎块 (mekanism:clump_copper) |
| `#mekanism:crystals/copper` | 铜晶体 (mekanism:crystal_copper) |
| `#mekanism:dirty_dusts/copper` | 污浊铜粉 (mekanism:dirty_dust_copper) |
| `#mekanism:shards/copper` | 铜碎片 (mekanism:shard_copper) |

## 完整配方表

### ae2

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `ae2:matter_cannon/nuggets/copper` | `ae2:matter_cannon` | #forge:nuggets/copper → 作为物质炮弹药使用 | 重量=63.546；静态启用 |
| `ae2:network/blocks/crank` | `minecraft:crafting_shaped` | 4 × #forge:rods/wooden + #forge:ingots/copper → 木质曲柄 (ae2:crank) | ；静态启用 |
| `ae2:network/blocks/crystal_processing_charger` | `minecraft:crafting_shaped` | 5 × #forge:ingots/iron + 2 × #forge:ingots/copper → 充能器 (ae2:charger) | ；静态启用 |
| `ae2:network/blocks/energy_energy_acceptor` | `minecraft:crafting_shaped` | 4 × #forge:ingots/iron + 4 × 石英玻璃 (ae2:quartz_glass) + #forge:ingots/copper → 能源接收器 (ae2:energy_acceptor) | ；静态启用 |
| `ae2:network/blocks/energy_vibration_chamber` | `minecraft:crafting_shaped` | 2 × #forge:ingots/copper + #forge:gems/fluix + 4 × #forge:ingots/iron + 熔炉 (minecraft:furnace) + 能源接收器 (ae2:energy_acceptor) → 谐振仓 (ae2:vibration_chamber) | ；静态启用 |
| `ae2:network/blocks/inscribers` | `minecraft:crafting_shaped` | 5 × #forge:ingots/iron + 2 × 黏性活塞 (minecraft:sticky_piston) + #forge:ingots/copper → 压印器 (ae2:inscriber) | ；静态启用 |
| `ae2:network/blocks/storage_chest` | `minecraft:crafting_shaped` | 2 × #forge:glass + ME终端 (ae2:terminal) + 2 × 福鲁伊克斯色ME玻璃线缆 (ae2:fluix_glass_cable) + 2 × #forge:ingots/iron + #forge:ingots/copper → ME箱子 (ae2:chest) | ；静态启用 |
| `ae2:network/cells/fluid_cell_housing` | `minecraft:crafting_shaped` | 2 × 石英玻璃 (ae2:quartz_glass) + 3 × #forge:dusts/redstone + 3 × #forge:ingots/copper → ME流体元件外壳 (ae2:fluid_cell_housing) | ；静态启用 |
| `ae2:network/cells/fluid_storage_cell_16k` | `minecraft:crafting_shaped` | 2 × 石英玻璃 (ae2:quartz_glass) + 3 × #forge:dusts/redstone + 16k ME存储组件 (ae2:cell_component_16k) + 3 × #forge:ingots/copper → 16k ME流体存储元件 (ae2:fluid_storage_cell_16k) | ；静态启用 |
| `ae2:network/cells/fluid_storage_cell_1k` | `minecraft:crafting_shaped` | 2 × 石英玻璃 (ae2:quartz_glass) + 3 × #forge:dusts/redstone + 1k ME存储组件 (ae2:cell_component_1k) + 3 × #forge:ingots/copper → 1k ME流体存储元件 (ae2:fluid_storage_cell_1k) | ；静态启用 |
| `ae2:network/cells/fluid_storage_cell_256k` | `minecraft:crafting_shaped` | 2 × 石英玻璃 (ae2:quartz_glass) + 3 × #forge:dusts/redstone + 256k ME存储组件 (ae2:cell_component_256k) + 3 × #forge:ingots/copper → 256k ME流体存储元件 (ae2:fluid_storage_cell_256k) | ；静态启用 |
| `ae2:network/cells/fluid_storage_cell_4k` | `minecraft:crafting_shaped` | 2 × 石英玻璃 (ae2:quartz_glass) + 3 × #forge:dusts/redstone + 4k ME存储组件 (ae2:cell_component_4k) + 3 × #forge:ingots/copper → 4k ME流体存储元件 (ae2:fluid_storage_cell_4k) | ；静态启用 |
| `ae2:network/cells/fluid_storage_cell_64k` | `minecraft:crafting_shaped` | 2 × 石英玻璃 (ae2:quartz_glass) + 3 × #forge:dusts/redstone + 64k ME存储组件 (ae2:cell_component_64k) + 3 × #forge:ingots/copper → 64k ME流体存储元件 (ae2:fluid_storage_cell_64k) | ；静态启用 |
| `ae2:network/crystal_resonance_generator` | `minecraft:crafting_shaped` | 4 × #forge:ingots/copper + 福鲁伊克斯块 (ae2:fluix_block) + 充能赛特斯石英水晶 (ae2:charged_certus_quartz_crystal) + 3 × #forge:ingots/iron → 晶共振生成器 (ae2:crystal_resonance_generator) | ；静态启用 |
| `ae2:network/parts/cable_anchor` | `minecraft:crafting_shapeless` | #ae2:metal_ingots + #ae2:knife → 4 × 线缆锚 (ae2:cable_anchor) | ；静态启用 |

### alexscaves

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `alexscaves:copper_valve` | `minecraft:crafting_shaped` | 4 × 铜锭 (minecraft:copper_ingot) + 废金属块 (alexscaves:scrap_metal) → 铜阀门 (alexscaves:copper_valve) | ；静态启用 |
| `alexscaves:armortrim/polarity` | `minecraft:smithing_trim` | #minecraft:trim_materials + #minecraft:trimmable_armor + alexscaves:polarity_armor_trim_smithing_template → 保留原装备并应用模板纹饰（可选铜锭饰材） | ；静态启用 |

### appliedcreate

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `appliedcreate:inscriber/advanced_stress_circuit_board` | `ae2:inscriber` | {"middle":{"item":"create:brass_ingot"}} → 黄铜应力电路板 (appliedcreate:advanced_stress_circuit_board) | ；静态启用 |
| `appliedcreate:brass_pattern_provider` | `create:mechanical_crafting` | 7 × 黄铜锭 (create:brass_ingot) + ME样板供应器 (ae2:pattern_provider) + 黄铜机壳 (create:brass_casing) → 黄铜样板供应器 (appliedcreate:brass_pattern_provider) | ；静态启用 |
| `appliedcreate:me_gearbox` | `create:mechanical_crafting` | 4 × 黄铜锭 (create:brass_ingot) + 2 × 齿轮 (create:cogwheel) + 2 × 福鲁伊克斯色ME玻璃线缆 (ae2:fluix_glass_cable) + 传动杆 (create:shaft) → ME齿轮箱 (appliedcreate:me_gearbox) | ；静态启用 |
| `appliedcreate:brass_pattern_provider_from_part` | `minecraft:crafting_shapeless` | 黄铜样板供应器 (appliedcreate:brass_pattern_provider_part) → 黄铜样板供应器 (appliedcreate:brass_pattern_provider) | ；静态启用 |
| `appliedcreate:brass_pattern_provider_part` | `minecraft:crafting_shapeless` | 黄铜样板供应器 (appliedcreate:brass_pattern_provider) → 黄铜样板供应器 (appliedcreate:brass_pattern_provider_part) | ；静态启用 |
| `appliedcreate:brass_pattern_provider_upgrade` | `minecraft:crafting_shapeless` | 黄铜样板供应器 (appliedcreate:brass_pattern_provider) + 安山合金 (create:andesite_alloy) → 黄铜样板供应器升级 (appliedcreate:brass_pattern_provider_upgrade) | ；静态启用 |

### brewinandchewin

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `brewinandchewin:cutting/flaxen_cheese_wheel` | `farmersdelight:cutting` | 金黄奶酪轮 (brewinandchewin:flaxen_cheese_wheel) → 4 × 金黄奶酪块 (brewinandchewin:flaxen_cheese_wedge) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `brewinandchewin:cutting/pizza` | `farmersdelight:cutting` | 比萨 (brewinandchewin:pizza) → 4 × 披萨切片 (brewinandchewin:pizza_slice) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `brewinandchewin:cutting/quiche` | `farmersdelight:cutting` | 咸派 (brewinandchewin:quiche) → 4 × 咸派切片 (brewinandchewin:quiche_slice) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `brewinandchewin:cutting/scarlet_cheese_wheel` | `farmersdelight:cutting` | 绯红奶酪轮 (brewinandchewin:scarlet_cheese_wheel) → 4 × 绯红奶酪块 (brewinandchewin:scarlet_cheese_wedge) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |

### caves_delight

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `caves_delight:cutting/blue_sweetish_fish_slice` | `farmersdelight:cutting` | 蓝色软糖鱼 (alexscaves:sweetish_fish_blue) → 2 × 蓝色甜味鱼片 (caves_delight:blue_sweetish_fish_slice) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `caves_delight:cutting/cut_slam` | `farmersdelight:cutting` | 猛汉午餐肉 (alexscaves:slam) → 2 × 生史莱姆肉片 (caves_delight:cut_slam) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `caves_delight:cutting/green_sweetish_fish_slice` | `farmersdelight:cutting` | 绿色软糖鱼 (alexscaves:sweetish_fish_green) → 2 × 绿色甜味鱼片 (caves_delight:green_sweetish_fish_slice) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `caves_delight:cutting/pink_sweetish_fish_slice` | `farmersdelight:cutting` | 粉红色软糖鱼 (alexscaves:sweetish_fish_pink) → 2 × 粉色甜味鱼片 (caves_delight:pink_sweetish_fish_slice) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `caves_delight:cutting/raw_bioluminessence_pasta` | `farmersdelight:cutting` | 生物荧光质 (alexscaves:bioluminesscence) → 生荧光意面 (caves_delight:raw_bioluminessence_pasta) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `caves_delight:cutting/raw_lanternfish_slice` | `farmersdelight:cutting` | 灯笼鱼 (alexscaves:lanternfish) → 2 × 生灯笼鱼片 (caves_delight:raw_lanternfish_slice) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `caves_delight:cutting/raw_radgill_slice` | `farmersdelight:cutting` | 辐鳃鱼 (alexscaves:radgill) → 2 × 生辐鳃鱼片 (caves_delight:raw_radgill_slice) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `caves_delight:cutting/raw_trilocaris_slice` | `farmersdelight:cutting` | 生三叶虾尾 (alexscaves:trilocaris_tail) → 2 × 生三叶虾片 (caves_delight:raw_trilocaris_slice) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `caves_delight:cutting/raw_tripodfish_slice` | `farmersdelight:cutting` | 三脚鱼 (alexscaves:tripodfish) → 2 × 生三脚鱼片 (caves_delight:raw_tripodfish_slice) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `caves_delight:cutting/red_sweetish_fish_slice` | `farmersdelight:cutting` | 红色软糖鱼 (alexscaves:sweetish_fish_red) → 2 × 红色甜味鱼片 (caves_delight:red_sweetish_fish_slice) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `caves_delight:cutting/sea_pig_roll_slice` | `farmersdelight:cutting` | 海猪肉卷 (caves_delight:sea_pig_roll) → 3 × 海猪肉卷切片 (caves_delight:sea_pig_roll_slice) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `caves_delight:cutting/trilocaris_slice` | `farmersdelight:cutting` | 熟三叶虾尾 (alexscaves:cooked_trilocaris_tail) → 2 × 熟三叶虾片 (caves_delight:cooked_trilocaris_slice) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `caves_delight:cutting/yellow_sweetish_fish_slice` | `farmersdelight:cutting` | 黄色软糖鱼 (alexscaves:sweetish_fish_yellow) → 2 × 黄色甜味鱼片 (caves_delight:yellow_sweetish_fish_slice) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |

### create

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `create:crushing/copper_ore` | `create:crushing` | 铜矿石 (minecraft:copper_ore) → 5 × 粉碎铜矿石 (create:crushed_raw_copper) + 粉碎铜矿石 (create:crushed_raw_copper) [25.0%] + 经验颗粒 (create:experience_nugget) [75.0%] + 圆石 (minecraft:cobblestone) [12.5%] | {"processingTime":250}；静态启用 |
| `create:crushing/deepslate_copper_ore` | `create:crushing` | 深层铜矿石 (minecraft:deepslate_copper_ore) → 7 × 粉碎铜矿石 (create:crushed_raw_copper) + 粉碎铜矿石 (create:crushed_raw_copper) [25.0%] + 经验颗粒 (create:experience_nugget) [75.0%] + 深板岩圆石 (minecraft:cobbled_deepslate) [12.5%] | {"processingTime":350}；静态启用 |
| `create:crushing/raw_copper` | `create:crushing` | #forge:raw_materials/copper → 粉碎铜矿石 (create:crushed_raw_copper) + 经验颗粒 (create:experience_nugget) [75.0%] | {"processingTime":400}；静态启用 |
| `create:crushing/raw_copper_block` | `create:crushing` | #forge:storage_blocks/raw_copper → 9 × 粉碎铜矿石 (create:crushed_raw_copper) + 9 × 经验颗粒 (create:experience_nugget) [75.0%] | {"processingTime":400}；静态启用 |
| `create:crushing/tuff` | `create:crushing` | 凝灰岩 (minecraft:tuff) → 燧石 (minecraft:flint) [25.0%] + 金粒 (minecraft:gold_nugget) [10.0%] + 铜粒 (create:copper_nugget) [10.0%] + 锌粒 (create:zinc_nugget) [10.0%] + 铁粒 (minecraft:iron_nugget) [10.0%] | {"processingTime":350}；静态启用 |
| `create:crushing/tuff_recycling` | `create:crushing` | #create:stone_types/tuff → 燧石 (minecraft:flint) [25.0%] + 金粒 (minecraft:gold_nugget) [10.0%] + 铜粒 (create:copper_nugget) [10.0%] + 锌粒 (create:zinc_nugget) [10.0%] + 铁粒 (minecraft:iron_nugget) [10.0%] | {"processingTime":350}；静态启用 |
| `create:crushing/veridium` | `create:crushing` | 辉绿岩 (create:veridium) → 粉碎铜矿石 (create:crushed_raw_copper) [80.0%] + 铜粒 (create:copper_nugget) [80.0%] | {"processingTime":250}；静态启用 |
| `create:crushing/veridium_recycling` | `create:crushing` | #create:stone_types/veridium → 粉碎铜矿石 (create:crushed_raw_copper) [80.0%] + 铜粒 (create:copper_nugget) [80.0%] | {"processingTime":250}；静态启用 |
| `create:deploying/copper_block_from_deoxidising` | `create:deploying` | 斑驳的铜块 (minecraft:exposed_copper) + #minecraft:axes → 铜块 (minecraft:copper_block) | ；静态启用 |
| `create:deploying/copper_block_from_removing_wax` | `create:deploying` | 涂蜡的铜块 (minecraft:waxed_copper_block) + #minecraft:axes → 铜块 (minecraft:copper_block) | ；静态启用 |
| `create:deploying/copper_shingle_slab_from_deoxidising` | `create:deploying` | 斑驳的铜砖瓦台阶 (create:exposed_copper_shingle_slab) + #minecraft:axes → 铜砖瓦台阶 (create:copper_shingle_slab) | ；静态启用 |
| `create:deploying/copper_shingle_slab_from_removing_wax` | `create:deploying` | 涂蜡铜砖瓦台阶 (create:waxed_copper_shingle_slab) + #minecraft:axes → 铜砖瓦台阶 (create:copper_shingle_slab) | ；静态启用 |
| `create:deploying/copper_shingle_stairs_from_deoxidising` | `create:deploying` | 斑驳的铜砖瓦楼梯 (create:exposed_copper_shingle_stairs) + #minecraft:axes → 铜砖瓦楼梯 (create:copper_shingle_stairs) | ；静态启用 |
| `create:deploying/copper_shingle_stairs_from_removing_wax` | `create:deploying` | 涂蜡铜砖瓦楼梯 (create:waxed_copper_shingle_stairs) + #minecraft:axes → 铜砖瓦楼梯 (create:copper_shingle_stairs) | ；静态启用 |
| `create:deploying/copper_shingles_from_deoxidising` | `create:deploying` | 斑驳的铜砖瓦 (create:exposed_copper_shingles) + #minecraft:axes → 铜砖瓦 (create:copper_shingles) | ；静态启用 |
| `create:deploying/copper_shingles_from_removing_wax` | `create:deploying` | 涂蜡铜砖瓦 (create:waxed_copper_shingles) + #minecraft:axes → 铜砖瓦 (create:copper_shingles) | ；静态启用 |
| `create:deploying/copper_tile_slab_from_deoxidising` | `create:deploying` | 斑驳的铜瓦台阶 (create:exposed_copper_tile_slab) + #minecraft:axes → 铜瓦台阶 (create:copper_tile_slab) | ；静态启用 |
| `create:deploying/copper_tile_slab_from_removing_wax` | `create:deploying` | 涂蜡铜瓦台阶 (create:waxed_copper_tile_slab) + #minecraft:axes → 铜瓦台阶 (create:copper_tile_slab) | ；静态启用 |
| `create:deploying/copper_tile_stairs_from_deoxidising` | `create:deploying` | 斑驳的铜瓦楼梯 (create:exposed_copper_tile_stairs) + #minecraft:axes → 铜瓦楼梯 (create:copper_tile_stairs) | ；静态启用 |
| `create:deploying/copper_tile_stairs_from_removing_wax` | `create:deploying` | 涂蜡铜瓦楼梯 (create:waxed_copper_tile_stairs) + #minecraft:axes → 铜瓦楼梯 (create:copper_tile_stairs) | ；静态启用 |
| `create:deploying/copper_tiles_from_deoxidising` | `create:deploying` | 斑驳的铜瓦 (create:exposed_copper_tiles) + #minecraft:axes → 铜瓦 (create:copper_tiles) | ；静态启用 |
| `create:deploying/copper_tiles_from_removing_wax` | `create:deploying` | 涂蜡铜瓦 (create:waxed_copper_tiles) + #minecraft:axes → 铜瓦 (create:copper_tiles) | ；静态启用 |
| `create:deploying/cut_copper_from_deoxidising` | `create:deploying` | 斑驳的切制铜块 (minecraft:exposed_cut_copper) + #minecraft:axes → 切制铜块 (minecraft:cut_copper) | ；静态启用 |
| `create:deploying/cut_copper_from_removing_wax` | `create:deploying` | 涂蜡的切制铜块 (minecraft:waxed_cut_copper) + #minecraft:axes → 切制铜块 (minecraft:cut_copper) | ；静态启用 |
| `create:deploying/cut_copper_slab_from_deoxidising` | `create:deploying` | 斑驳的切制铜台阶 (minecraft:exposed_cut_copper_slab) + #minecraft:axes → 切制铜台阶 (minecraft:cut_copper_slab) | ；静态启用 |
| `create:deploying/cut_copper_slab_from_removing_wax` | `create:deploying` | 涂蜡的切制铜台阶 (minecraft:waxed_cut_copper_slab) + #minecraft:axes → 切制铜台阶 (minecraft:cut_copper_slab) | ；静态启用 |
| `create:deploying/cut_copper_stairs_from_deoxidising` | `create:deploying` | 斑驳的切制铜楼梯 (minecraft:exposed_cut_copper_stairs) + #minecraft:axes → 切制铜楼梯 (minecraft:cut_copper_stairs) | ；静态启用 |
| `create:deploying/cut_copper_stairs_from_removing_wax` | `create:deploying` | 涂蜡的切制铜楼梯 (minecraft:waxed_cut_copper_stairs) + #minecraft:axes → 切制铜楼梯 (minecraft:cut_copper_stairs) | ；静态启用 |
| `create:deploying/exposed_copper_from_deoxidising` | `create:deploying` | 锈蚀的铜块 (minecraft:weathered_copper) + #minecraft:axes → 斑驳的铜块 (minecraft:exposed_copper) | ；静态启用 |
| `create:deploying/exposed_copper_from_removing_wax` | `create:deploying` | 涂蜡的斑驳铜块 (minecraft:waxed_exposed_copper) + #minecraft:axes → 斑驳的铜块 (minecraft:exposed_copper) | ；静态启用 |
| `create:deploying/exposed_copper_shingle_slab_from_deoxidising` | `create:deploying` | 锈蚀的铜砖瓦台阶 (create:weathered_copper_shingle_slab) + #minecraft:axes → 斑驳的铜砖瓦台阶 (create:exposed_copper_shingle_slab) | ；静态启用 |
| `create:deploying/exposed_copper_shingle_slab_from_removing_wax` | `create:deploying` | 斑驳的涂蜡铜砖瓦台阶 (create:waxed_exposed_copper_shingle_slab) + #minecraft:axes → 斑驳的铜砖瓦台阶 (create:exposed_copper_shingle_slab) | ；静态启用 |
| `create:deploying/exposed_copper_shingle_stairs_from_deoxidising` | `create:deploying` | 锈蚀的铜砖瓦楼梯 (create:weathered_copper_shingle_stairs) + #minecraft:axes → 斑驳的铜砖瓦楼梯 (create:exposed_copper_shingle_stairs) | ；静态启用 |
| `create:deploying/exposed_copper_shingle_stairs_from_removing_wax` | `create:deploying` | 斑驳的涂蜡铜砖瓦楼梯 (create:waxed_exposed_copper_shingle_stairs) + #minecraft:axes → 斑驳的铜砖瓦楼梯 (create:exposed_copper_shingle_stairs) | ；静态启用 |
| `create:deploying/exposed_copper_shingles_from_deoxidising` | `create:deploying` | 锈蚀的铜砖瓦 (create:weathered_copper_shingles) + #minecraft:axes → 斑驳的铜砖瓦 (create:exposed_copper_shingles) | ；静态启用 |
| `create:deploying/exposed_copper_shingles_from_removing_wax` | `create:deploying` | 斑驳的涂蜡铜砖瓦 (create:waxed_exposed_copper_shingles) + #minecraft:axes → 斑驳的铜砖瓦 (create:exposed_copper_shingles) | ；静态启用 |
| `create:deploying/exposed_copper_tile_slab_from_deoxidising` | `create:deploying` | 锈蚀的铜瓦台阶 (create:weathered_copper_tile_slab) + #minecraft:axes → 斑驳的铜瓦台阶 (create:exposed_copper_tile_slab) | ；静态启用 |
| `create:deploying/exposed_copper_tile_slab_from_removing_wax` | `create:deploying` | 斑驳的涂蜡铜瓦台阶 (create:waxed_exposed_copper_tile_slab) + #minecraft:axes → 斑驳的铜瓦台阶 (create:exposed_copper_tile_slab) | ；静态启用 |
| `create:deploying/exposed_copper_tile_stairs_from_deoxidising` | `create:deploying` | 锈蚀的铜瓦楼梯 (create:weathered_copper_tile_stairs) + #minecraft:axes → 斑驳的铜瓦楼梯 (create:exposed_copper_tile_stairs) | ；静态启用 |
| `create:deploying/exposed_copper_tile_stairs_from_removing_wax` | `create:deploying` | 斑驳的涂蜡铜瓦楼梯 (create:waxed_exposed_copper_tile_stairs) + #minecraft:axes → 斑驳的铜瓦楼梯 (create:exposed_copper_tile_stairs) | ；静态启用 |
| `create:deploying/exposed_copper_tiles_from_deoxidising` | `create:deploying` | 锈蚀的铜瓦 (create:weathered_copper_tiles) + #minecraft:axes → 斑驳的铜瓦 (create:exposed_copper_tiles) | ；静态启用 |
| `create:deploying/exposed_copper_tiles_from_removing_wax` | `create:deploying` | 斑驳的涂蜡铜瓦 (create:waxed_exposed_copper_tiles) + #minecraft:axes → 斑驳的铜瓦 (create:exposed_copper_tiles) | ；静态启用 |
| `create:deploying/exposed_cut_copper_from_deoxidising` | `create:deploying` | 锈蚀的切制铜块 (minecraft:weathered_cut_copper) + #minecraft:axes → 斑驳的切制铜块 (minecraft:exposed_cut_copper) | ；静态启用 |
| `create:deploying/exposed_cut_copper_from_removing_wax` | `create:deploying` | 涂蜡的斑驳切制铜块 (minecraft:waxed_exposed_cut_copper) + #minecraft:axes → 斑驳的切制铜块 (minecraft:exposed_cut_copper) | ；静态启用 |
| `create:deploying/exposed_cut_copper_slab_from_deoxidising` | `create:deploying` | 锈蚀的切制铜台阶 (minecraft:weathered_cut_copper_slab) + #minecraft:axes → 斑驳的切制铜台阶 (minecraft:exposed_cut_copper_slab) | ；静态启用 |
| `create:deploying/exposed_cut_copper_slab_from_removing_wax` | `create:deploying` | 涂蜡的斑驳切制铜台阶 (minecraft:waxed_exposed_cut_copper_slab) + #minecraft:axes → 斑驳的切制铜台阶 (minecraft:exposed_cut_copper_slab) | ；静态启用 |
| `create:deploying/exposed_cut_copper_stairs_from_deoxidising` | `create:deploying` | 锈蚀的切制铜楼梯 (minecraft:weathered_cut_copper_stairs) + #minecraft:axes → 斑驳的切制铜楼梯 (minecraft:exposed_cut_copper_stairs) | ；静态启用 |
| `create:deploying/exposed_cut_copper_stairs_from_removing_wax` | `create:deploying` | 涂蜡的斑驳切制铜楼梯 (minecraft:waxed_exposed_cut_copper_stairs) + #minecraft:axes → 斑驳的切制铜楼梯 (minecraft:exposed_cut_copper_stairs) | ；静态启用 |
| `create:deploying/oxidized_copper_from_removing_wax` | `create:deploying` | 涂蜡的氧化铜块 (minecraft:waxed_oxidized_copper) + #minecraft:axes → 氧化的铜块 (minecraft:oxidized_copper) | ；静态启用 |
| `create:deploying/oxidized_copper_shingle_slab_from_removing_wax` | `create:deploying` | 氧化的涂蜡铜砖瓦台阶 (create:waxed_oxidized_copper_shingle_slab) + #minecraft:axes → 氧化的铜砖瓦台阶 (create:oxidized_copper_shingle_slab) | ；静态启用 |
| `create:deploying/oxidized_copper_shingle_stairs_from_removing_wax` | `create:deploying` | 氧化的涂蜡铜砖瓦楼梯 (create:waxed_oxidized_copper_shingle_stairs) + #minecraft:axes → 氧化的铜砖瓦楼梯 (create:oxidized_copper_shingle_stairs) | ；静态启用 |
| `create:deploying/oxidized_copper_shingles_from_removing_wax` | `create:deploying` | 氧化的涂蜡铜砖瓦 (create:waxed_oxidized_copper_shingles) + #minecraft:axes → 氧化的铜砖瓦 (create:oxidized_copper_shingles) | ；静态启用 |
| `create:deploying/oxidized_copper_tile_slab_from_removing_wax` | `create:deploying` | 氧化的涂蜡铜瓦台阶 (create:waxed_oxidized_copper_tile_slab) + #minecraft:axes → 氧化的铜瓦台阶 (create:oxidized_copper_tile_slab) | ；静态启用 |
| `create:deploying/oxidized_copper_tile_stairs_from_removing_wax` | `create:deploying` | 氧化的涂蜡铜瓦楼梯 (create:waxed_oxidized_copper_tile_stairs) + #minecraft:axes → 氧化的铜瓦楼梯 (create:oxidized_copper_tile_stairs) | ；静态启用 |
| `create:deploying/oxidized_copper_tiles_from_removing_wax` | `create:deploying` | 氧化的涂蜡铜瓦 (create:waxed_oxidized_copper_tiles) + #minecraft:axes → 氧化的铜瓦 (create:oxidized_copper_tiles) | ；静态启用 |
| `create:deploying/oxidized_cut_copper_from_removing_wax` | `create:deploying` | 涂蜡的氧化切制铜块 (minecraft:waxed_oxidized_cut_copper) + #minecraft:axes → 氧化的切制铜块 (minecraft:oxidized_cut_copper) | ；静态启用 |
| `create:deploying/oxidized_cut_copper_slab_from_removing_wax` | `create:deploying` | 涂蜡的氧化切制铜台阶 (minecraft:waxed_oxidized_cut_copper_slab) + #minecraft:axes → 氧化的切制铜台阶 (minecraft:oxidized_cut_copper_slab) | ；静态启用 |
| `create:deploying/oxidized_cut_copper_stairs_from_removing_wax` | `create:deploying` | 涂蜡的氧化切制铜楼梯 (minecraft:waxed_oxidized_cut_copper_stairs) + #minecraft:axes → 氧化的切制铜楼梯 (minecraft:oxidized_cut_copper_stairs) | ；静态启用 |
| `create:deploying/waxed_copper_block_from_adding_wax` | `create:deploying` | 铜块 (minecraft:copper_block) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡的铜块 (minecraft:waxed_copper_block) | ；静态启用 |
| `create:deploying/waxed_copper_shingle_slab_from_adding_wax` | `create:deploying` | 铜砖瓦台阶 (create:copper_shingle_slab) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡铜砖瓦台阶 (create:waxed_copper_shingle_slab) | ；静态启用 |
| `create:deploying/waxed_copper_shingle_stairs_from_adding_wax` | `create:deploying` | 铜砖瓦楼梯 (create:copper_shingle_stairs) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡铜砖瓦楼梯 (create:waxed_copper_shingle_stairs) | ；静态启用 |
| `create:deploying/waxed_copper_shingles_from_adding_wax` | `create:deploying` | 铜砖瓦 (create:copper_shingles) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡铜砖瓦 (create:waxed_copper_shingles) | ；静态启用 |
| `create:deploying/waxed_copper_tile_slab_from_adding_wax` | `create:deploying` | 铜瓦台阶 (create:copper_tile_slab) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡铜瓦台阶 (create:waxed_copper_tile_slab) | ；静态启用 |
| `create:deploying/waxed_copper_tile_stairs_from_adding_wax` | `create:deploying` | 铜瓦楼梯 (create:copper_tile_stairs) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡铜瓦楼梯 (create:waxed_copper_tile_stairs) | ；静态启用 |
| `create:deploying/waxed_copper_tiles_from_adding_wax` | `create:deploying` | 铜瓦 (create:copper_tiles) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡铜瓦 (create:waxed_copper_tiles) | ；静态启用 |
| `create:deploying/waxed_cut_copper_from_adding_wax` | `create:deploying` | 切制铜块 (minecraft:cut_copper) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡的切制铜块 (minecraft:waxed_cut_copper) | ；静态启用 |
| `create:deploying/waxed_cut_copper_slab_from_adding_wax` | `create:deploying` | 切制铜台阶 (minecraft:cut_copper_slab) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡的切制铜台阶 (minecraft:waxed_cut_copper_slab) | ；静态启用 |
| `create:deploying/waxed_cut_copper_stairs_from_adding_wax` | `create:deploying` | 切制铜楼梯 (minecraft:cut_copper_stairs) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡的切制铜楼梯 (minecraft:waxed_cut_copper_stairs) | ；静态启用 |
| `create:deploying/waxed_exposed_copper_from_adding_wax` | `create:deploying` | 斑驳的铜块 (minecraft:exposed_copper) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡的斑驳铜块 (minecraft:waxed_exposed_copper) | ；静态启用 |
| `create:deploying/waxed_exposed_copper_shingle_slab_from_adding_wax` | `create:deploying` | 斑驳的铜砖瓦台阶 (create:exposed_copper_shingle_slab) + 蜜脾块 (minecraft:honeycomb_block) → 斑驳的涂蜡铜砖瓦台阶 (create:waxed_exposed_copper_shingle_slab) | ；静态启用 |
| `create:deploying/waxed_exposed_copper_shingle_stairs_from_adding_wax` | `create:deploying` | 斑驳的铜砖瓦楼梯 (create:exposed_copper_shingle_stairs) + 蜜脾块 (minecraft:honeycomb_block) → 斑驳的涂蜡铜砖瓦楼梯 (create:waxed_exposed_copper_shingle_stairs) | ；静态启用 |
| `create:deploying/waxed_exposed_copper_shingles_from_adding_wax` | `create:deploying` | 斑驳的铜砖瓦 (create:exposed_copper_shingles) + 蜜脾块 (minecraft:honeycomb_block) → 斑驳的涂蜡铜砖瓦 (create:waxed_exposed_copper_shingles) | ；静态启用 |
| `create:deploying/waxed_exposed_copper_tile_slab_from_adding_wax` | `create:deploying` | 斑驳的铜瓦台阶 (create:exposed_copper_tile_slab) + 蜜脾块 (minecraft:honeycomb_block) → 斑驳的涂蜡铜瓦台阶 (create:waxed_exposed_copper_tile_slab) | ；静态启用 |
| `create:deploying/waxed_exposed_copper_tile_stairs_from_adding_wax` | `create:deploying` | 斑驳的铜瓦楼梯 (create:exposed_copper_tile_stairs) + 蜜脾块 (minecraft:honeycomb_block) → 斑驳的涂蜡铜瓦楼梯 (create:waxed_exposed_copper_tile_stairs) | ；静态启用 |
| `create:deploying/waxed_exposed_copper_tiles_from_adding_wax` | `create:deploying` | 斑驳的铜瓦 (create:exposed_copper_tiles) + 蜜脾块 (minecraft:honeycomb_block) → 斑驳的涂蜡铜瓦 (create:waxed_exposed_copper_tiles) | ；静态启用 |
| `create:deploying/waxed_exposed_cut_copper_from_adding_wax` | `create:deploying` | 斑驳的切制铜块 (minecraft:exposed_cut_copper) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡的斑驳切制铜块 (minecraft:waxed_exposed_cut_copper) | ；静态启用 |
| `create:deploying/waxed_exposed_cut_copper_slab_from_adding_wax` | `create:deploying` | 斑驳的切制铜台阶 (minecraft:exposed_cut_copper_slab) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡的斑驳切制铜台阶 (minecraft:waxed_exposed_cut_copper_slab) | ；静态启用 |
| `create:deploying/waxed_exposed_cut_copper_stairs_from_adding_wax` | `create:deploying` | 斑驳的切制铜楼梯 (minecraft:exposed_cut_copper_stairs) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡的斑驳切制铜楼梯 (minecraft:waxed_exposed_cut_copper_stairs) | ；静态启用 |
| `create:deploying/waxed_oxidized_copper_from_adding_wax` | `create:deploying` | 氧化的铜块 (minecraft:oxidized_copper) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡的氧化铜块 (minecraft:waxed_oxidized_copper) | ；静态启用 |
| `create:deploying/waxed_oxidized_copper_shingle_slab_from_adding_wax` | `create:deploying` | 氧化的铜砖瓦台阶 (create:oxidized_copper_shingle_slab) + 蜜脾块 (minecraft:honeycomb_block) → 氧化的涂蜡铜砖瓦台阶 (create:waxed_oxidized_copper_shingle_slab) | ；静态启用 |
| `create:deploying/waxed_oxidized_copper_shingle_stairs_from_adding_wax` | `create:deploying` | 氧化的铜砖瓦楼梯 (create:oxidized_copper_shingle_stairs) + 蜜脾块 (minecraft:honeycomb_block) → 氧化的涂蜡铜砖瓦楼梯 (create:waxed_oxidized_copper_shingle_stairs) | ；静态启用 |
| `create:deploying/waxed_oxidized_copper_shingles_from_adding_wax` | `create:deploying` | 氧化的铜砖瓦 (create:oxidized_copper_shingles) + 蜜脾块 (minecraft:honeycomb_block) → 氧化的涂蜡铜砖瓦 (create:waxed_oxidized_copper_shingles) | ；静态启用 |
| `create:deploying/waxed_oxidized_copper_tile_slab_from_adding_wax` | `create:deploying` | 氧化的铜瓦台阶 (create:oxidized_copper_tile_slab) + 蜜脾块 (minecraft:honeycomb_block) → 氧化的涂蜡铜瓦台阶 (create:waxed_oxidized_copper_tile_slab) | ；静态启用 |
| `create:deploying/waxed_oxidized_copper_tile_stairs_from_adding_wax` | `create:deploying` | 氧化的铜瓦楼梯 (create:oxidized_copper_tile_stairs) + 蜜脾块 (minecraft:honeycomb_block) → 氧化的涂蜡铜瓦楼梯 (create:waxed_oxidized_copper_tile_stairs) | ；静态启用 |
| `create:deploying/waxed_oxidized_copper_tiles_from_adding_wax` | `create:deploying` | 氧化的铜瓦 (create:oxidized_copper_tiles) + 蜜脾块 (minecraft:honeycomb_block) → 氧化的涂蜡铜瓦 (create:waxed_oxidized_copper_tiles) | ；静态启用 |
| `create:deploying/waxed_oxidized_cut_copper_from_adding_wax` | `create:deploying` | 氧化的切制铜块 (minecraft:oxidized_cut_copper) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡的氧化切制铜块 (minecraft:waxed_oxidized_cut_copper) | ；静态启用 |
| `create:deploying/waxed_oxidized_cut_copper_slab_from_adding_wax` | `create:deploying` | 氧化的切制铜台阶 (minecraft:oxidized_cut_copper_slab) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡的氧化切制铜台阶 (minecraft:waxed_oxidized_cut_copper_slab) | ；静态启用 |
| `create:deploying/waxed_oxidized_cut_copper_stairs_from_adding_wax` | `create:deploying` | 氧化的切制铜楼梯 (minecraft:oxidized_cut_copper_stairs) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡的氧化切制铜楼梯 (minecraft:waxed_oxidized_cut_copper_stairs) | ；静态启用 |
| `create:deploying/waxed_weathered_copper_from_adding_wax` | `create:deploying` | 锈蚀的铜块 (minecraft:weathered_copper) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡的锈蚀铜块 (minecraft:waxed_weathered_copper) | ；静态启用 |
| `create:deploying/waxed_weathered_copper_shingle_slab_from_adding_wax` | `create:deploying` | 锈蚀的铜砖瓦台阶 (create:weathered_copper_shingle_slab) + 蜜脾块 (minecraft:honeycomb_block) → 锈蚀的涂蜡铜砖瓦台阶 (create:waxed_weathered_copper_shingle_slab) | ；静态启用 |
| `create:deploying/waxed_weathered_copper_shingle_stairs_from_adding_wax` | `create:deploying` | 锈蚀的铜砖瓦楼梯 (create:weathered_copper_shingle_stairs) + 蜜脾块 (minecraft:honeycomb_block) → 锈蚀的涂蜡铜砖瓦楼梯 (create:waxed_weathered_copper_shingle_stairs) | ；静态启用 |
| `create:deploying/waxed_weathered_copper_shingles_from_adding_wax` | `create:deploying` | 锈蚀的铜砖瓦 (create:weathered_copper_shingles) + 蜜脾块 (minecraft:honeycomb_block) → 锈蚀的涂蜡铜砖瓦 (create:waxed_weathered_copper_shingles) | ；静态启用 |
| `create:deploying/waxed_weathered_copper_tile_slab_from_adding_wax` | `create:deploying` | 锈蚀的铜瓦台阶 (create:weathered_copper_tile_slab) + 蜜脾块 (minecraft:honeycomb_block) → 锈蚀的涂蜡铜瓦台阶 (create:waxed_weathered_copper_tile_slab) | ；静态启用 |
| `create:deploying/waxed_weathered_copper_tile_stairs_from_adding_wax` | `create:deploying` | 锈蚀的铜瓦楼梯 (create:weathered_copper_tile_stairs) + 蜜脾块 (minecraft:honeycomb_block) → 锈蚀的涂蜡铜瓦楼梯 (create:waxed_weathered_copper_tile_stairs) | ；静态启用 |
| `create:deploying/waxed_weathered_copper_tiles_from_adding_wax` | `create:deploying` | 锈蚀的铜瓦 (create:weathered_copper_tiles) + 蜜脾块 (minecraft:honeycomb_block) → 锈蚀的涂蜡铜瓦 (create:waxed_weathered_copper_tiles) | ；静态启用 |
| `create:deploying/waxed_weathered_cut_copper_from_adding_wax` | `create:deploying` | 锈蚀的切制铜块 (minecraft:weathered_cut_copper) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡的锈蚀切制铜块 (minecraft:waxed_weathered_cut_copper) | ；静态启用 |
| `create:deploying/waxed_weathered_cut_copper_slab_from_adding_wax` | `create:deploying` | 锈蚀的切制铜台阶 (minecraft:weathered_cut_copper_slab) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡的锈蚀切制铜台阶 (minecraft:waxed_weathered_cut_copper_slab) | ；静态启用 |
| `create:deploying/waxed_weathered_cut_copper_stairs_from_adding_wax` | `create:deploying` | 锈蚀的切制铜楼梯 (minecraft:weathered_cut_copper_stairs) + 蜜脾块 (minecraft:honeycomb_block) → 涂蜡的锈蚀切制铜楼梯 (minecraft:waxed_weathered_cut_copper_stairs) | ；静态启用 |
| `create:deploying/weathered_copper_from_deoxidising` | `create:deploying` | 氧化的铜块 (minecraft:oxidized_copper) + #minecraft:axes → 锈蚀的铜块 (minecraft:weathered_copper) | ；静态启用 |
| `create:deploying/weathered_copper_from_removing_wax` | `create:deploying` | 涂蜡的锈蚀铜块 (minecraft:waxed_weathered_copper) + #minecraft:axes → 锈蚀的铜块 (minecraft:weathered_copper) | ；静态启用 |
| `create:deploying/weathered_copper_shingle_slab_from_deoxidising` | `create:deploying` | 氧化的铜砖瓦台阶 (create:oxidized_copper_shingle_slab) + #minecraft:axes → 锈蚀的铜砖瓦台阶 (create:weathered_copper_shingle_slab) | ；静态启用 |
| `create:deploying/weathered_copper_shingle_slab_from_removing_wax` | `create:deploying` | 锈蚀的涂蜡铜砖瓦台阶 (create:waxed_weathered_copper_shingle_slab) + #minecraft:axes → 锈蚀的铜砖瓦台阶 (create:weathered_copper_shingle_slab) | ；静态启用 |
| `create:deploying/weathered_copper_shingle_stairs_from_deoxidising` | `create:deploying` | 氧化的铜砖瓦楼梯 (create:oxidized_copper_shingle_stairs) + #minecraft:axes → 锈蚀的铜砖瓦楼梯 (create:weathered_copper_shingle_stairs) | ；静态启用 |
| `create:deploying/weathered_copper_shingle_stairs_from_removing_wax` | `create:deploying` | 锈蚀的涂蜡铜砖瓦楼梯 (create:waxed_weathered_copper_shingle_stairs) + #minecraft:axes → 锈蚀的铜砖瓦楼梯 (create:weathered_copper_shingle_stairs) | ；静态启用 |
| `create:deploying/weathered_copper_shingles_from_deoxidising` | `create:deploying` | 氧化的铜砖瓦 (create:oxidized_copper_shingles) + #minecraft:axes → 锈蚀的铜砖瓦 (create:weathered_copper_shingles) | ；静态启用 |
| `create:deploying/weathered_copper_shingles_from_removing_wax` | `create:deploying` | 锈蚀的涂蜡铜砖瓦 (create:waxed_weathered_copper_shingles) + #minecraft:axes → 锈蚀的铜砖瓦 (create:weathered_copper_shingles) | ；静态启用 |
| `create:deploying/weathered_copper_tile_slab_from_deoxidising` | `create:deploying` | 氧化的铜瓦台阶 (create:oxidized_copper_tile_slab) + #minecraft:axes → 锈蚀的铜瓦台阶 (create:weathered_copper_tile_slab) | ；静态启用 |
| `create:deploying/weathered_copper_tile_slab_from_removing_wax` | `create:deploying` | 锈蚀的涂蜡铜瓦台阶 (create:waxed_weathered_copper_tile_slab) + #minecraft:axes → 锈蚀的铜瓦台阶 (create:weathered_copper_tile_slab) | ；静态启用 |
| `create:deploying/weathered_copper_tile_stairs_from_deoxidising` | `create:deploying` | 氧化的铜瓦楼梯 (create:oxidized_copper_tile_stairs) + #minecraft:axes → 锈蚀的铜瓦楼梯 (create:weathered_copper_tile_stairs) | ；静态启用 |
| `create:deploying/weathered_copper_tile_stairs_from_removing_wax` | `create:deploying` | 锈蚀的涂蜡铜瓦楼梯 (create:waxed_weathered_copper_tile_stairs) + #minecraft:axes → 锈蚀的铜瓦楼梯 (create:weathered_copper_tile_stairs) | ；静态启用 |
| `create:deploying/weathered_copper_tiles_from_deoxidising` | `create:deploying` | 氧化的铜瓦 (create:oxidized_copper_tiles) + #minecraft:axes → 锈蚀的铜瓦 (create:weathered_copper_tiles) | ；静态启用 |
| `create:deploying/weathered_copper_tiles_from_removing_wax` | `create:deploying` | 锈蚀的涂蜡铜瓦 (create:waxed_weathered_copper_tiles) + #minecraft:axes → 锈蚀的铜瓦 (create:weathered_copper_tiles) | ；静态启用 |
| `create:deploying/weathered_cut_copper_from_deoxidising` | `create:deploying` | 氧化的切制铜块 (minecraft:oxidized_cut_copper) + #minecraft:axes → 锈蚀的切制铜块 (minecraft:weathered_cut_copper) | ；静态启用 |
| `create:deploying/weathered_cut_copper_from_removing_wax` | `create:deploying` | 涂蜡的锈蚀切制铜块 (minecraft:waxed_weathered_cut_copper) + #minecraft:axes → 锈蚀的切制铜块 (minecraft:weathered_cut_copper) | ；静态启用 |
| `create:deploying/weathered_cut_copper_slab_from_deoxidising` | `create:deploying` | 氧化的切制铜台阶 (minecraft:oxidized_cut_copper_slab) + #minecraft:axes → 锈蚀的切制铜台阶 (minecraft:weathered_cut_copper_slab) | ；静态启用 |
| `create:deploying/weathered_cut_copper_slab_from_removing_wax` | `create:deploying` | 涂蜡的锈蚀切制铜台阶 (minecraft:waxed_weathered_cut_copper_slab) + #minecraft:axes → 锈蚀的切制铜台阶 (minecraft:weathered_cut_copper_slab) | ；静态启用 |
| `create:deploying/weathered_cut_copper_stairs_from_deoxidising` | `create:deploying` | 氧化的切制铜楼梯 (minecraft:oxidized_cut_copper_stairs) + #minecraft:axes → 锈蚀的切制铜楼梯 (minecraft:weathered_cut_copper_stairs) | ；静态启用 |
| `create:deploying/weathered_cut_copper_stairs_from_removing_wax` | `create:deploying` | 涂蜡的锈蚀切制铜楼梯 (minecraft:waxed_weathered_cut_copper_stairs) + #minecraft:axes → 锈蚀的切制铜楼梯 (minecraft:weathered_cut_copper_stairs) | ；静态启用 |
| `create:item_application/brass_casing_from_log` | `create:item_application` | #forge:stripped_logs + #forge:ingots/brass → 黄铜机壳 (create:brass_casing) | ；静态启用 |
| `create:item_application/brass_casing_from_wood` | `create:item_application` | #forge:stripped_wood + #forge:ingots/brass → 黄铜机壳 (create:brass_casing) | ；静态启用 |
| `create:item_application/copper_casing_from_log` | `create:item_application` | #forge:stripped_logs + #forge:ingots/copper → 铜机壳 (create:copper_casing) | ；静态启用 |
| `create:item_application/copper_casing_from_wood` | `create:item_application` | #forge:stripped_wood + #forge:ingots/copper → 铜机壳 (create:copper_casing) | ；静态启用 |
| `create:item_application/railway_casing` | `create:item_application` | 黄铜机壳 (create:brass_casing) + #forge:plates/obsidian → 列车机壳 (create:railway_casing) | ；静态启用 |
| `create:mechanical_crafting/extendo_grip` | `create:mechanical_crafting` | #forge:ingots/brass + 精密构件 (create:precision_mechanism) + 6 × #forge:rods/wooden + 黄铜手部零件 (create:brass_hand) → 伸缩机械手 (create:extendo_grip) | ；静态启用 |
| `create:mechanical_crafting/potato_cannon` | `create:mechanical_crafting` | 安山合金 (create:andesite_alloy) + 精密构件 (create:precision_mechanism) + 3 × 流体管道 (create:fluid_pipe) + 2 × #forge:ingots/copper → 土豆加农炮 (create:potato_cannon) | ；静态启用 |
| `create:mechanical_crafting/wand_of_symmetry` | `create:mechanical_crafting` | 3 × #forge:glass + #forge:ender_pearls + 精密构件 (create:precision_mechanism) + #forge:ingots/brass + #forge:obsidian → 对称之杖 (create:wand_of_symmetry) | ；静态启用 |
| `create:mixing/brass_ingot` | `create:mixing` | #forge:ingots/copper + #forge:ingots/zinc → 2 × 黄铜锭 (create:brass_ingot) | {"heatRequirement":"heated"}；静态启用 |
| `create:pressing/brass_ingot` | `create:pressing` | #forge:ingots/brass → 黄铜板 (create:brass_sheet) | ；静态启用 |
| `create:pressing/copper_ingot` | `create:pressing` | #forge:ingots/copper → 铜板 (create:copper_sheet) | ；静态启用 |
| `create:splashing/crushed_raw_copper` | `create:splashing` | 粉碎铜矿石 (create:crushed_raw_copper) → 9 × 铜粒 (create:copper_nugget) + 黏土球 (minecraft:clay_ball) [50.0%] | ；静态启用 |
| `create:blasting/copper_ingot_from_crushed` | `minecraft:blasting` | 粉碎铜矿石 (create:crushed_raw_copper) → 铜锭 (minecraft:copper_ingot) | {"cookingtime":100,"experience":0.1}；静态启用 |
| `create:copper_shingle_slab` | `minecraft:crafting_shaped` | 3 × 铜砖瓦 (create:copper_shingles) → 6 × 铜砖瓦台阶 (create:copper_shingle_slab) | ；静态启用 |
| `create:copper_shingle_stairs` | `minecraft:crafting_shaped` | 6 × 铜砖瓦 (create:copper_shingles) → 4 × 铜砖瓦楼梯 (create:copper_shingle_stairs) | ；静态启用 |
| `create:copper_tile_slab` | `minecraft:crafting_shaped` | 3 × 铜瓦 (create:copper_tiles) → 6 × 铜瓦台阶 (create:copper_tile_slab) | ；静态启用 |
| `create:copper_tile_stairs` | `minecraft:crafting_shaped` | 6 × 铜瓦 (create:copper_tiles) → 4 × 铜瓦楼梯 (create:copper_tile_stairs) | ；静态启用 |
| `create:crafting/appliances/copper_backtank` | `minecraft:crafting_shaped` | 2 × 安山合金 (create:andesite_alloy) + 传动杆 (create:shaft) + 3 × #forge:ingots/copper + #forge:storage_blocks/copper → 铜背罐 (create:copper_backtank) | ；静态启用 |
| `create:crafting/appliances/copper_diving_boots` | `minecraft:crafting_shaped` | 4 × #forge:ingots/copper + 2 × 安山合金 (create:andesite_alloy) → 铜潜水靴 (create:copper_diving_boots) | ；静态启用 |
| `create:crafting/appliances/copper_diving_helmet` | `minecraft:crafting_shaped` | 5 × #forge:ingots/copper + #forge:glass → 铜潜水头盔 (create:copper_diving_helmet) | ；静态启用 |
| `create:crafting/curiosities/peculiar_bell` | `minecraft:crafting_shaped` | #forge:storage_blocks/brass + #forge:plates/brass → 奇异钟 (create:peculiar_bell) | ；静态启用 |
| `create:crafting/kinetics/attribute_filter` | `minecraft:crafting_shaped` | 2 × #forge:nuggets/brass + #minecraft:wool → 属性过滤器 (create:attribute_filter) | ；静态启用 |
| `create:crafting/kinetics/brass_hand` | `minecraft:crafting_shaped` | 安山合金 (create:andesite_alloy) + 4 × #forge:plates/brass → 黄铜手部零件 (create:brass_hand) | ；静态启用 |
| `create:crafting/kinetics/clockwork_bearing` | `minecraft:crafting_shaped` | #minecraft:wooden_slabs + 黄铜机壳 (create:brass_casing) + 电子管 (create:electron_tube) → 时钟轴承 (create:clockwork_bearing) | ；静态启用 |
| `create:crafting/kinetics/copper_valve_handle` | `minecraft:crafting_shaped` | 3 × #forge:plates/copper + 安山合金 (create:andesite_alloy) → 铜阀门手轮 (create:copper_valve_handle) | ；静态启用 |
| `create:crafting/kinetics/crafter_slot_cover` | `minecraft:crafting_shaped` | 3 × #forge:nuggets/brass → 合成槽盖板 (create:crafter_slot_cover) | ；静态启用 |
| `create:crafting/kinetics/deployer` | `minecraft:crafting_shaped` | 电子管 (create:electron_tube) + 安山机壳 (create:andesite_casing) + 黄铜手部零件 (create:brass_hand) → 机械手 (create:deployer) | ；静态启用 |
| `create:crafting/kinetics/elevator_pulley` | `minecraft:crafting_shaped` | 黄铜机壳 (create:brass_casing) + 干海带块 (minecraft:dried_kelp_block) + #forge:plates/iron → 升降机滑轮 (create:elevator_pulley) | ；静态启用 |
| `create:crafting/kinetics/fluid_pipe` | `minecraft:crafting_shaped` | 2 × #forge:plates/copper + #forge:ingots/copper → 4 × 流体管道 (create:fluid_pipe) | ；静态启用 |
| `create:crafting/kinetics/fluid_pipe_vertical` | `minecraft:crafting_shaped` | 2 × #forge:plates/copper + #forge:ingots/copper → 4 × 流体管道 (create:fluid_pipe) | ；静态启用 |
| `create:crafting/kinetics/fluid_tank` | `minecraft:crafting_shaped` | 2 × #forge:plates/copper + #forge:barrels/wooden → 流体储罐 (create:fluid_tank) | ；静态启用 |
| `create:crafting/kinetics/flywheel` | `minecraft:crafting_shaped` | 8 × #forge:ingots/brass + 传动杆 (create:shaft) → 飞轮 (create:flywheel) | ；静态启用 |
| `create:crafting/kinetics/hose_pulley` | `minecraft:crafting_shaped` | 铜机壳 (create:copper_casing) + 干海带块 (minecraft:dried_kelp_block) + #forge:plates/copper → 软管滑轮 (create:hose_pulley) | ；静态启用 |
| `create:crafting/kinetics/item_drain` | `minecraft:crafting_shaped` | 铁栏杆 (minecraft:iron_bars) + 铜机壳 (create:copper_casing) → 分液池 (create:item_drain) | ；静态启用 |
| `create:crafting/kinetics/mechanical_arm` | `minecraft:crafting_shaped` | 3 × #forge:plates/brass + 安山合金 (create:andesite_alloy) + 精密构件 (create:precision_mechanism) + 黄铜机壳 (create:brass_casing) → 动力臂 (create:mechanical_arm) | ；静态启用 |
| `create:crafting/kinetics/mechanical_crafter` | `minecraft:crafting_shaped` | 电子管 (create:electron_tube) + 黄铜机壳 (create:brass_casing) + 工作台 (minecraft:crafting_table) → 3 × 动力合成器 (create:mechanical_crafter) | ；静态启用 |
| `create:crafting/kinetics/rotation_speed_controller` | `minecraft:crafting_shaped` | 精密构件 (create:precision_mechanism) + 黄铜机壳 (create:brass_casing) → 转速控制器 (create:rotation_speed_controller) | ；静态启用 |
| `create:crafting/kinetics/smart_chute` | `minecraft:crafting_shaped` | #forge:plates/brass + 溜槽 (create:chute) + 电子管 (create:electron_tube) → 智能溜槽 (create:smart_chute) | ；静态启用 |
| `create:crafting/kinetics/smart_fluid_pipe` | `minecraft:crafting_shaped` | #forge:plates/brass + 流体管道 (create:fluid_pipe) + 电子管 (create:electron_tube) → 智能流体管道 (create:smart_fluid_pipe) | ；静态启用 |
| `create:crafting/kinetics/spout` | `minecraft:crafting_shaped` | 铜机壳 (create:copper_casing) + 干海带 (minecraft:dried_kelp) → 注液器 (create:spout) | ；静态启用 |
| `create:crafting/kinetics/steam_engine` | `minecraft:crafting_shaped` | #forge:plates/gold + 安山合金 (create:andesite_alloy) + #forge:storage_blocks/copper → 蒸汽引擎 (create:steam_engine) | ；静态启用 |
| `create:crafting/kinetics/steam_whistle` | `minecraft:crafting_shaped` | #forge:plates/gold + #forge:ingots/copper → 蒸汽笛 (create:steam_whistle) | ；静态启用 |
| `create:crafting/logistics/brass_funnel` | `minecraft:crafting_shaped` | 电子管 (create:electron_tube) + #forge:ingots/brass + 干海带 (minecraft:dried_kelp) → 2 × 黄铜漏斗 (create:brass_funnel) | ；静态启用 |
| `create:crafting/logistics/brass_tunnel` | `minecraft:crafting_shaped` | 电子管 (create:electron_tube) + 2 × #forge:ingots/brass + 2 × 干海带 (minecraft:dried_kelp) → 2 × 黄铜隧道 (create:brass_tunnel) | ；静态启用 |
| `create:crafting/logistics/content_observer` | `minecraft:crafting_shaped` | 电子管 (create:electron_tube) + 黄铜机壳 (create:brass_casing) + 侦测器 (minecraft:observer) → 智能侦测器 (create:content_observer) | ；静态启用 |
| `create:crafting/logistics/display_link` | `minecraft:crafting_shaped` | 发信线圈 (create:transmitter) + 黄铜机壳 (create:brass_casing) → 显示链接器 (create:display_link) | ；静态启用 |
| `create:crafting/logistics/pulse_extender` | `minecraft:crafting_shaped` | 2 × 红石火把 (minecraft:redstone_torch) + #forge:dusts/redstone + #forge:plates/brass + 3 × #forge:stone → 脉冲延长器 (create:pulse_extender) | ；静态启用 |
| `create:crafting/logistics/pulse_repeater` | `minecraft:crafting_shaped` | #forge:dusts/redstone + #forge:plates/brass + 红石火把 (minecraft:redstone_torch) + 3 × #forge:stone → 脉冲中继器 (create:pulse_repeater) | ；静态启用 |
| `create:crafting/logistics/pulse_timer` | `minecraft:crafting_shaped` | 紫水晶碎片 (minecraft:amethyst_shard) + #forge:plates/brass + 红石火把 (minecraft:redstone_torch) + 3 × #forge:stone → 脉冲计时器 (create:pulse_timer) | ；静态启用 |
| `create:crafting/logistics/stockpile_switch` | `minecraft:crafting_shaped` | 电子管 (create:electron_tube) + 黄铜机壳 (create:brass_casing) + 红石比较器 (minecraft:comparator) → 存量转信器 (create:stockpile_switch) | ；静态启用 |
| `create:crafting/materials/brass_block_from_compacting` | `minecraft:crafting_shaped` | 9 × #forge:ingots/brass → 黄铜块 (create:brass_block) | ；静态启用 |
| `create:crafting/materials/brass_ingot_from_compacting` | `minecraft:crafting_shaped` | 9 × #forge:nuggets/brass → 黄铜锭 (create:brass_ingot) | ；静态启用 |
| `create:crafting/materials/copper_ingot` | `minecraft:crafting_shaped` | 9 × #forge:nuggets/copper → 铜锭 (minecraft:copper_ingot) | ；静态启用 |
| `create:crafting/materials/transmitter` | `minecraft:crafting_shaped` | 避雷针 (minecraft:lightning_rod) + 3 × #forge:plates/copper + #forge:dusts/redstone → 发信线圈 (create:transmitter) | ；静态启用 |
| `create:exposed_copper_shingle_slab` | `minecraft:crafting_shaped` | 3 × 斑驳的铜砖瓦 (create:exposed_copper_shingles) → 6 × 斑驳的铜砖瓦台阶 (create:exposed_copper_shingle_slab) | ；静态启用 |
| `create:exposed_copper_shingle_stairs` | `minecraft:crafting_shaped` | 6 × 斑驳的铜砖瓦 (create:exposed_copper_shingles) → 4 × 斑驳的铜砖瓦楼梯 (create:exposed_copper_shingle_stairs) | ；静态启用 |
| `create:exposed_copper_tile_slab` | `minecraft:crafting_shaped` | 3 × 斑驳的铜瓦 (create:exposed_copper_tiles) → 6 × 斑驳的铜瓦台阶 (create:exposed_copper_tile_slab) | ；静态启用 |
| `create:exposed_copper_tile_stairs` | `minecraft:crafting_shaped` | 6 × 斑驳的铜瓦 (create:exposed_copper_tiles) → 4 × 斑驳的铜瓦楼梯 (create:exposed_copper_tile_stairs) | ；静态启用 |
| `create:oxidized_copper_shingle_slab` | `minecraft:crafting_shaped` | 3 × 氧化的铜砖瓦 (create:oxidized_copper_shingles) → 6 × 氧化的铜砖瓦台阶 (create:oxidized_copper_shingle_slab) | ；静态启用 |
| `create:oxidized_copper_shingle_stairs` | `minecraft:crafting_shaped` | 6 × 氧化的铜砖瓦 (create:oxidized_copper_shingles) → 4 × 氧化的铜砖瓦楼梯 (create:oxidized_copper_shingle_stairs) | ；静态启用 |
| `create:oxidized_copper_tile_slab` | `minecraft:crafting_shaped` | 3 × 氧化的铜瓦 (create:oxidized_copper_tiles) → 6 × 氧化的铜瓦台阶 (create:oxidized_copper_tile_slab) | ；静态启用 |
| `create:oxidized_copper_tile_stairs` | `minecraft:crafting_shaped` | 6 × 氧化的铜瓦 (create:oxidized_copper_tiles) → 4 × 氧化的铜瓦楼梯 (create:oxidized_copper_tile_stairs) | ；静态启用 |
| `create:weathered_copper_shingle_slab` | `minecraft:crafting_shaped` | 3 × 锈蚀的铜砖瓦 (create:weathered_copper_shingles) → 6 × 锈蚀的铜砖瓦台阶 (create:weathered_copper_shingle_slab) | ；静态启用 |
| `create:weathered_copper_shingle_stairs` | `minecraft:crafting_shaped` | 6 × 锈蚀的铜砖瓦 (create:weathered_copper_shingles) → 4 × 锈蚀的铜砖瓦楼梯 (create:weathered_copper_shingle_stairs) | ；静态启用 |
| `create:weathered_copper_tile_slab` | `minecraft:crafting_shaped` | 3 × 锈蚀的铜瓦 (create:weathered_copper_tiles) → 6 × 锈蚀的铜瓦台阶 (create:weathered_copper_tile_slab) | ；静态启用 |
| `create:weathered_copper_tile_stairs` | `minecraft:crafting_shaped` | 6 × 锈蚀的铜瓦 (create:weathered_copper_tiles) → 4 × 锈蚀的铜瓦楼梯 (create:weathered_copper_tile_stairs) | ；静态启用 |
| `create:crafting/copper/waxed_copper_shingle_slab_from_honeycomb` | `minecraft:crafting_shapeless` | 铜砖瓦台阶 (create:copper_shingle_slab) + 蜜脾 (minecraft:honeycomb) → 涂蜡铜砖瓦台阶 (create:waxed_copper_shingle_slab) | ；静态启用 |
| `create:crafting/copper/waxed_copper_shingle_stairs_from_honeycomb` | `minecraft:crafting_shapeless` | 铜砖瓦楼梯 (create:copper_shingle_stairs) + 蜜脾 (minecraft:honeycomb) → 涂蜡铜砖瓦楼梯 (create:waxed_copper_shingle_stairs) | ；静态启用 |
| `create:crafting/copper/waxed_copper_shingles_from_honeycomb` | `minecraft:crafting_shapeless` | 铜砖瓦 (create:copper_shingles) + 蜜脾 (minecraft:honeycomb) → 涂蜡铜砖瓦 (create:waxed_copper_shingles) | ；静态启用 |
| `create:crafting/copper/waxed_copper_tile_slab_from_honeycomb` | `minecraft:crafting_shapeless` | 铜瓦台阶 (create:copper_tile_slab) + 蜜脾 (minecraft:honeycomb) → 涂蜡铜瓦台阶 (create:waxed_copper_tile_slab) | ；静态启用 |
| `create:crafting/copper/waxed_copper_tile_stairs_from_honeycomb` | `minecraft:crafting_shapeless` | 铜瓦楼梯 (create:copper_tile_stairs) + 蜜脾 (minecraft:honeycomb) → 涂蜡铜瓦楼梯 (create:waxed_copper_tile_stairs) | ；静态启用 |
| `create:crafting/copper/waxed_copper_tiles_from_honeycomb` | `minecraft:crafting_shapeless` | 铜瓦 (create:copper_tiles) + 蜜脾 (minecraft:honeycomb) → 涂蜡铜瓦 (create:waxed_copper_tiles) | ；静态启用 |
| `create:crafting/copper/waxed_exposed_copper_shingle_slab_from_honeycomb` | `minecraft:crafting_shapeless` | 斑驳的铜砖瓦台阶 (create:exposed_copper_shingle_slab) + 蜜脾 (minecraft:honeycomb) → 斑驳的涂蜡铜砖瓦台阶 (create:waxed_exposed_copper_shingle_slab) | ；静态启用 |
| `create:crafting/copper/waxed_exposed_copper_shingle_stairs_from_honeycomb` | `minecraft:crafting_shapeless` | 斑驳的铜砖瓦楼梯 (create:exposed_copper_shingle_stairs) + 蜜脾 (minecraft:honeycomb) → 斑驳的涂蜡铜砖瓦楼梯 (create:waxed_exposed_copper_shingle_stairs) | ；静态启用 |
| `create:crafting/copper/waxed_exposed_copper_shingles_from_honeycomb` | `minecraft:crafting_shapeless` | 斑驳的铜砖瓦 (create:exposed_copper_shingles) + 蜜脾 (minecraft:honeycomb) → 斑驳的涂蜡铜砖瓦 (create:waxed_exposed_copper_shingles) | ；静态启用 |
| `create:crafting/copper/waxed_exposed_copper_tile_slab_from_honeycomb` | `minecraft:crafting_shapeless` | 斑驳的铜瓦台阶 (create:exposed_copper_tile_slab) + 蜜脾 (minecraft:honeycomb) → 斑驳的涂蜡铜瓦台阶 (create:waxed_exposed_copper_tile_slab) | ；静态启用 |
| `create:crafting/copper/waxed_exposed_copper_tile_stairs_from_honeycomb` | `minecraft:crafting_shapeless` | 斑驳的铜瓦楼梯 (create:exposed_copper_tile_stairs) + 蜜脾 (minecraft:honeycomb) → 斑驳的涂蜡铜瓦楼梯 (create:waxed_exposed_copper_tile_stairs) | ；静态启用 |
| `create:crafting/copper/waxed_exposed_copper_tiles_from_honeycomb` | `minecraft:crafting_shapeless` | 斑驳的铜瓦 (create:exposed_copper_tiles) + 蜜脾 (minecraft:honeycomb) → 斑驳的涂蜡铜瓦 (create:waxed_exposed_copper_tiles) | ；静态启用 |
| `create:crafting/copper/waxed_oxidized_copper_shingle_slab_from_honeycomb` | `minecraft:crafting_shapeless` | 氧化的铜砖瓦台阶 (create:oxidized_copper_shingle_slab) + 蜜脾 (minecraft:honeycomb) → 氧化的涂蜡铜砖瓦台阶 (create:waxed_oxidized_copper_shingle_slab) | ；静态启用 |
| `create:crafting/copper/waxed_oxidized_copper_shingle_stairs_from_honeycomb` | `minecraft:crafting_shapeless` | 氧化的铜砖瓦楼梯 (create:oxidized_copper_shingle_stairs) + 蜜脾 (minecraft:honeycomb) → 氧化的涂蜡铜砖瓦楼梯 (create:waxed_oxidized_copper_shingle_stairs) | ；静态启用 |
| `create:crafting/copper/waxed_oxidized_copper_shingles_from_honeycomb` | `minecraft:crafting_shapeless` | 氧化的铜砖瓦 (create:oxidized_copper_shingles) + 蜜脾 (minecraft:honeycomb) → 氧化的涂蜡铜砖瓦 (create:waxed_oxidized_copper_shingles) | ；静态启用 |
| `create:crafting/copper/waxed_oxidized_copper_tile_slab_from_honeycomb` | `minecraft:crafting_shapeless` | 氧化的铜瓦台阶 (create:oxidized_copper_tile_slab) + 蜜脾 (minecraft:honeycomb) → 氧化的涂蜡铜瓦台阶 (create:waxed_oxidized_copper_tile_slab) | ；静态启用 |
| `create:crafting/copper/waxed_oxidized_copper_tile_stairs_from_honeycomb` | `minecraft:crafting_shapeless` | 氧化的铜瓦楼梯 (create:oxidized_copper_tile_stairs) + 蜜脾 (minecraft:honeycomb) → 氧化的涂蜡铜瓦楼梯 (create:waxed_oxidized_copper_tile_stairs) | ；静态启用 |
| `create:crafting/copper/waxed_oxidized_copper_tiles_from_honeycomb` | `minecraft:crafting_shapeless` | 氧化的铜瓦 (create:oxidized_copper_tiles) + 蜜脾 (minecraft:honeycomb) → 氧化的涂蜡铜瓦 (create:waxed_oxidized_copper_tiles) | ；静态启用 |
| `create:crafting/copper/waxed_weathered_copper_shingle_slab_from_honeycomb` | `minecraft:crafting_shapeless` | 锈蚀的铜砖瓦台阶 (create:weathered_copper_shingle_slab) + 蜜脾 (minecraft:honeycomb) → 锈蚀的涂蜡铜砖瓦台阶 (create:waxed_weathered_copper_shingle_slab) | ；静态启用 |
| `create:crafting/copper/waxed_weathered_copper_shingle_stairs_from_honeycomb` | `minecraft:crafting_shapeless` | 锈蚀的铜砖瓦楼梯 (create:weathered_copper_shingle_stairs) + 蜜脾 (minecraft:honeycomb) → 锈蚀的涂蜡铜砖瓦楼梯 (create:waxed_weathered_copper_shingle_stairs) | ；静态启用 |
| `create:crafting/copper/waxed_weathered_copper_shingles_from_honeycomb` | `minecraft:crafting_shapeless` | 锈蚀的铜砖瓦 (create:weathered_copper_shingles) + 蜜脾 (minecraft:honeycomb) → 锈蚀的涂蜡铜砖瓦 (create:waxed_weathered_copper_shingles) | ；静态启用 |
| `create:crafting/copper/waxed_weathered_copper_tile_slab_from_honeycomb` | `minecraft:crafting_shapeless` | 锈蚀的铜瓦台阶 (create:weathered_copper_tile_slab) + 蜜脾 (minecraft:honeycomb) → 锈蚀的涂蜡铜瓦台阶 (create:waxed_weathered_copper_tile_slab) | ；静态启用 |
| `create:crafting/copper/waxed_weathered_copper_tile_stairs_from_honeycomb` | `minecraft:crafting_shapeless` | 锈蚀的铜瓦楼梯 (create:weathered_copper_tile_stairs) + 蜜脾 (minecraft:honeycomb) → 锈蚀的涂蜡铜瓦楼梯 (create:waxed_weathered_copper_tile_stairs) | ；静态启用 |
| `create:crafting/copper/waxed_weathered_copper_tiles_from_honeycomb` | `minecraft:crafting_shapeless` | 锈蚀的铜瓦 (create:weathered_copper_tiles) + 蜜脾 (minecraft:honeycomb) → 锈蚀的涂蜡铜瓦 (create:waxed_weathered_copper_tiles) | ；静态启用 |
| `create:crafting/kinetics/andesite_door` | `minecraft:crafting_shapeless` | #minecraft:wooden_doors + 安山机壳 (create:andesite_casing) → 安山门 (create:andesite_door) | ；静态启用 |
| `create:crafting/kinetics/black_valve_handle_from_other_valve_handle` | `minecraft:crafting_shapeless` | #forge:dyes/black + #create:valve_handles → 黑色阀门手轮 (create:black_valve_handle) | ；静态启用 |
| `create:crafting/kinetics/blue_valve_handle_from_other_valve_handle` | `minecraft:crafting_shapeless` | #forge:dyes/blue + #create:valve_handles → 蓝色阀门手轮 (create:blue_valve_handle) | ；静态启用 |
| `create:crafting/kinetics/brass_door` | `minecraft:crafting_shapeless` | #minecraft:wooden_doors + 黄铜机壳 (create:brass_casing) → 黄铜门 (create:brass_door) | ；静态启用 |
| `create:crafting/kinetics/brown_valve_handle_from_other_valve_handle` | `minecraft:crafting_shapeless` | #forge:dyes/brown + #create:valve_handles → 棕色阀门手轮 (create:brown_valve_handle) | ；静态启用 |
| `create:crafting/kinetics/copper_door` | `minecraft:crafting_shapeless` | #minecraft:wooden_doors + 铜机壳 (create:copper_casing) → 铜门 (create:copper_door) | ；静态启用 |
| `create:crafting/kinetics/copper_valve_handle_from_others` | `minecraft:crafting_shapeless` | #create:valve_handles → 铜阀门手轮 (create:copper_valve_handle) | ；静态启用 |
| `create:crafting/kinetics/cyan_valve_handle_from_other_valve_handle` | `minecraft:crafting_shapeless` | #forge:dyes/cyan + #create:valve_handles → 青色阀门手轮 (create:cyan_valve_handle) | ；静态启用 |
| `create:crafting/kinetics/framed_glass_door` | `minecraft:crafting_shapeless` | #minecraft:wooden_doors + 边框玻璃 (create:framed_glass) → 边框玻璃门 (create:framed_glass_door) | ；静态启用 |
| `create:crafting/kinetics/gray_valve_handle_from_other_valve_handle` | `minecraft:crafting_shapeless` | #forge:dyes/gray + #create:valve_handles → 灰色阀门手轮 (create:gray_valve_handle) | ；静态启用 |
| `create:crafting/kinetics/green_valve_handle_from_other_valve_handle` | `minecraft:crafting_shapeless` | #forge:dyes/green + #create:valve_handles → 绿色阀门手轮 (create:green_valve_handle) | ；静态启用 |
| `create:crafting/kinetics/light_blue_valve_handle_from_other_valve_handle` | `minecraft:crafting_shapeless` | #forge:dyes/light_blue + #create:valve_handles → 淡蓝色阀门手轮 (create:light_blue_valve_handle) | ；静态启用 |
| `create:crafting/kinetics/light_gray_valve_handle_from_other_valve_handle` | `minecraft:crafting_shapeless` | #forge:dyes/light_gray + #create:valve_handles → 淡灰色阀门手轮 (create:light_gray_valve_handle) | ；静态启用 |
| `create:crafting/kinetics/lime_valve_handle_from_other_valve_handle` | `minecraft:crafting_shapeless` | #forge:dyes/lime + #create:valve_handles → 黄绿色阀门手轮 (create:lime_valve_handle) | ；静态启用 |
| `create:crafting/kinetics/magenta_valve_handle_from_other_valve_handle` | `minecraft:crafting_shapeless` | #forge:dyes/magenta + #create:valve_handles → 品红色阀门手轮 (create:magenta_valve_handle) | ；静态启用 |
| `create:crafting/kinetics/orange_valve_handle_from_other_valve_handle` | `minecraft:crafting_shapeless` | #forge:dyes/orange + #create:valve_handles → 橙色阀门手轮 (create:orange_valve_handle) | ；静态启用 |
| `create:crafting/kinetics/pink_valve_handle_from_other_valve_handle` | `minecraft:crafting_shapeless` | #forge:dyes/pink + #create:valve_handles → 粉红色阀门手轮 (create:pink_valve_handle) | ；静态启用 |
| `create:crafting/kinetics/placard` | `minecraft:crafting_shapeless` | 物品展示框 (minecraft:item_frame) + #forge:plates/brass → 置物板 (create:placard) | ；静态启用 |
| `create:crafting/kinetics/portable_fluid_interface` | `minecraft:crafting_shapeless` | 铜机壳 (create:copper_casing) + 溜槽 (create:chute) → 移动式流体接口 (create:portable_fluid_interface) | ；静态启用 |
| `create:crafting/kinetics/purple_valve_handle_from_other_valve_handle` | `minecraft:crafting_shapeless` | #forge:dyes/purple + #create:valve_handles → 紫色阀门手轮 (create:purple_valve_handle) | ；静态启用 |
| `create:crafting/kinetics/red_valve_handle_from_other_valve_handle` | `minecraft:crafting_shapeless` | #forge:dyes/red + #create:valve_handles → 红色阀门手轮 (create:red_valve_handle) | ；静态启用 |
| `create:crafting/kinetics/sequenced_gearshift` | `minecraft:crafting_shapeless` | 黄铜机壳 (create:brass_casing) + 齿轮 (create:cogwheel) + 电子管 (create:electron_tube) → 可编程齿轮箱 (create:sequenced_gearshift) | ；静态启用 |
| `create:crafting/kinetics/train_door` | `minecraft:crafting_shapeless` | #minecraft:wooden_doors + 列车机壳 (create:railway_casing) → 列车门 (create:train_door) | ；静态启用 |
| `create:crafting/kinetics/white_valve_handle_from_other_valve_handle` | `minecraft:crafting_shapeless` | #forge:dyes/white + #create:valve_handles → 白色阀门手轮 (create:white_valve_handle) | ；静态启用 |
| `create:crafting/kinetics/yellow_valve_handle_from_other_valve_handle` | `minecraft:crafting_shapeless` | #forge:dyes/yellow + #create:valve_handles → 黄色阀门手轮 (create:yellow_valve_handle) | ；静态启用 |
| `create:crafting/logistics/brass_table_cloth_clear` | `minecraft:crafting_shapeless` | 黄铜桌面 (create:brass_table_cloth) → 黄铜桌面 (create:brass_table_cloth) | ；静态启用 |
| `create:crafting/logistics/copper_table_cloth_clear` | `minecraft:crafting_shapeless` | 铜桌面 (create:copper_table_cloth) → 铜桌面 (create:copper_table_cloth) | ；静态启用 |
| `create:crafting/materials/brass_ingot_from_decompacting` | `minecraft:crafting_shapeless` | #forge:storage_blocks/brass → 9 × 黄铜锭 (create:brass_ingot) | ；静态启用 |
| `create:crafting/materials/brass_nugget_from_decompacting` | `minecraft:crafting_shapeless` | #forge:ingots/brass → 9 × 黄铜粒 (create:brass_nugget) | ；静态启用 |
| `create:crafting/materials/copper_nugget` | `minecraft:crafting_shapeless` | #forge:ingots/copper → 9 × 铜粒 (create:copper_nugget) | ；静态启用 |
| `create:smelting/copper_ingot_from_crushed` | `minecraft:smelting` | 粉碎铜矿石 (create:crushed_raw_copper) → 铜锭 (minecraft:copper_ingot) | {"cookingtime":200,"experience":0.1}；静态启用 |
| `create:crafting/appliances/netherite_backtank` | `minecraft:smithing_transform` | #forge:ingots/netherite + 铜背罐 (create:copper_backtank) + 锻造模板 (minecraft:netherite_upgrade_smithing_template) → 下界合金背罐 (create:netherite_backtank) | ；静态启用 |
| `create:crafting/appliances/netherite_backtank_from_netherite` | `minecraft:smithing_transform` | 铜背罐 (create:copper_backtank) + 下界合金胸甲 (minecraft:netherite_chestplate) + 锻造模板 (minecraft:netherite_upgrade_smithing_template) → 下界合金背罐 (create:netherite_backtank) | ；静态启用 |
| `create:crafting/appliances/netherite_diving_boots` | `minecraft:smithing_transform` | #forge:ingots/netherite + 铜潜水靴 (create:copper_diving_boots) + 锻造模板 (minecraft:netherite_upgrade_smithing_template) → 下界合金潜水靴 (create:netherite_diving_boots) | ；静态启用 |
| `create:crafting/appliances/netherite_diving_boots_from_netherite` | `minecraft:smithing_transform` | 铜潜水靴 (create:copper_diving_boots) + 下界合金靴子 (minecraft:netherite_boots) + 锻造模板 (minecraft:netherite_upgrade_smithing_template) → 下界合金潜水靴 (create:netherite_diving_boots) | ；静态启用 |
| `create:crafting/appliances/netherite_diving_helmet` | `minecraft:smithing_transform` | #forge:ingots/netherite + 铜潜水头盔 (create:copper_diving_helmet) + 锻造模板 (minecraft:netherite_upgrade_smithing_template) → 下界合金潜水头盔 (create:netherite_diving_helmet) | ；静态启用 |
| `create:crafting/appliances/netherite_diving_helmet_from_netherite` | `minecraft:smithing_transform` | 铜潜水头盔 (create:copper_diving_helmet) + 下界合金头盔 (minecraft:netherite_helmet) + 锻造模板 (minecraft:netherite_upgrade_smithing_template) → 下界合金潜水头盔 (create:netherite_diving_helmet) | ；静态启用 |
| `create:brass_bars_from_ingots_brass_stonecutting` | `minecraft:stonecutting` | #forge:ingots/brass → 黄铜栏杆 (create:brass_bars) | ；静态启用 |
| `create:brass_ladder_from_ingots_brass_stonecutting` | `minecraft:stonecutting` | #forge:ingots/brass → 黄铜梯子 (create:brass_ladder) | ；静态启用 |
| `create:brass_scaffolding_from_ingots_brass_stonecutting` | `minecraft:stonecutting` | #forge:ingots/brass → 黄铜脚手架 (create:brass_scaffolding) | ；静态启用 |
| `create:brass_table_cloth_from_ingots_brass_stonecutting` | `minecraft:stonecutting` | #forge:ingots/brass → 黄铜桌面 (create:brass_table_cloth) | ；静态启用 |
| `create:copper_bars_from_ingots_copper_stonecutting` | `minecraft:stonecutting` | #forge:ingots/copper → 铜栏杆 (create:copper_bars) | ；静态启用 |
| `create:copper_ladder_from_ingots_copper_stonecutting` | `minecraft:stonecutting` | #forge:ingots/copper → 铜梯子 (create:copper_ladder) | ；静态启用 |
| `create:copper_scaffolding_from_ingots_copper_stonecutting` | `minecraft:stonecutting` | #forge:ingots/copper → 铜脚手架 (create:copper_scaffolding) | ；静态启用 |
| `create:copper_shingle_slab_from_copper_shingles_stonecutting` | `minecraft:stonecutting` | 铜砖瓦 (create:copper_shingles) → 铜砖瓦台阶 (create:copper_shingle_slab) | ；静态启用 |
| `create:copper_shingle_stairs_from_copper_shingles_stonecutting` | `minecraft:stonecutting` | 铜砖瓦 (create:copper_shingles) → 铜砖瓦楼梯 (create:copper_shingle_stairs) | ；静态启用 |
| `create:copper_shingles_from_ingots_copper_stonecutting` | `minecraft:stonecutting` | #forge:ingots/copper → 铜砖瓦 (create:copper_shingles) | ；静态启用 |
| `create:copper_table_cloth_from_ingots_copper_stonecutting` | `minecraft:stonecutting` | #forge:ingots/copper → 铜桌面 (create:copper_table_cloth) | ；静态启用 |
| `create:copper_tile_slab_from_copper_tiles_stonecutting` | `minecraft:stonecutting` | 铜瓦 (create:copper_tiles) → 铜瓦台阶 (create:copper_tile_slab) | ；静态启用 |
| `create:copper_tile_stairs_from_copper_tiles_stonecutting` | `minecraft:stonecutting` | 铜瓦 (create:copper_tiles) → 铜瓦楼梯 (create:copper_tile_stairs) | ；静态启用 |
| `create:copper_tiles_from_ingots_copper_stonecutting` | `minecraft:stonecutting` | #forge:ingots/copper → 铜瓦 (create:copper_tiles) | ；静态启用 |
| `create:exposed_copper_shingle_slab_from_exposed_copper_shingles_stonecutting` | `minecraft:stonecutting` | 斑驳的铜砖瓦 (create:exposed_copper_shingles) → 斑驳的铜砖瓦台阶 (create:exposed_copper_shingle_slab) | ；静态启用 |
| `create:exposed_copper_shingle_stairs_from_exposed_copper_shingles_stonecutting` | `minecraft:stonecutting` | 斑驳的铜砖瓦 (create:exposed_copper_shingles) → 斑驳的铜砖瓦楼梯 (create:exposed_copper_shingle_stairs) | ；静态启用 |
| `create:exposed_copper_tile_slab_from_exposed_copper_tiles_stonecutting` | `minecraft:stonecutting` | 斑驳的铜瓦 (create:exposed_copper_tiles) → 斑驳的铜瓦台阶 (create:exposed_copper_tile_slab) | ；静态启用 |
| `create:exposed_copper_tile_stairs_from_exposed_copper_tiles_stonecutting` | `minecraft:stonecutting` | 斑驳的铜瓦 (create:exposed_copper_tiles) → 斑驳的铜瓦楼梯 (create:exposed_copper_tile_stairs) | ；静态启用 |
| `create:oxidized_copper_shingle_slab_from_oxidized_copper_shingles_stonecutting` | `minecraft:stonecutting` | 氧化的铜砖瓦 (create:oxidized_copper_shingles) → 氧化的铜砖瓦台阶 (create:oxidized_copper_shingle_slab) | ；静态启用 |
| `create:oxidized_copper_shingle_stairs_from_oxidized_copper_shingles_stonecutting` | `minecraft:stonecutting` | 氧化的铜砖瓦 (create:oxidized_copper_shingles) → 氧化的铜砖瓦楼梯 (create:oxidized_copper_shingle_stairs) | ；静态启用 |
| `create:oxidized_copper_tile_slab_from_oxidized_copper_tiles_stonecutting` | `minecraft:stonecutting` | 氧化的铜瓦 (create:oxidized_copper_tiles) → 氧化的铜瓦台阶 (create:oxidized_copper_tile_slab) | ；静态启用 |
| `create:oxidized_copper_tile_stairs_from_oxidized_copper_tiles_stonecutting` | `minecraft:stonecutting` | 氧化的铜瓦 (create:oxidized_copper_tiles) → 氧化的铜瓦楼梯 (create:oxidized_copper_tile_stairs) | ；静态启用 |
| `create:weathered_copper_shingle_slab_from_weathered_copper_shingles_stonecutting` | `minecraft:stonecutting` | 锈蚀的铜砖瓦 (create:weathered_copper_shingles) → 锈蚀的铜砖瓦台阶 (create:weathered_copper_shingle_slab) | ；静态启用 |
| `create:weathered_copper_shingle_stairs_from_weathered_copper_shingles_stonecutting` | `minecraft:stonecutting` | 锈蚀的铜砖瓦 (create:weathered_copper_shingles) → 锈蚀的铜砖瓦楼梯 (create:weathered_copper_shingle_stairs) | ；静态启用 |
| `create:weathered_copper_tile_slab_from_weathered_copper_tiles_stonecutting` | `minecraft:stonecutting` | 锈蚀的铜瓦 (create:weathered_copper_tiles) → 锈蚀的铜瓦台阶 (create:weathered_copper_tile_slab) | ；静态启用 |
| `create:weathered_copper_tile_stairs_from_weathered_copper_tiles_stonecutting` | `minecraft:stonecutting` | 锈蚀的铜瓦 (create:weathered_copper_tiles) → 锈蚀的铜瓦楼梯 (create:weathered_copper_tile_stairs) | ；静态启用 |

### create_connected

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `create_connected:crafting/kinetics/brass_chute` | `minecraft:crafting_shaped` | 2 × 黄铜板 (create:brass_sheet) + 黄铜锭 (create:brass_ingot) → 4 × 黄铜溜槽 (create_connected:brass_chute) | ；配置条件待核对 |
| `create_connected:crafting/kinetics/brass_gearbox` | `minecraft:crafting_shaped` | 4 × 齿轮 (create:cogwheel) + 转速控制器 (create:rotation_speed_controller) → 黄铜十字齿轮箱 (create_connected:brass_gearbox) | ；配置条件待核对 |
| `create_connected:crafting/kinetics/dashboard` | `minecraft:crafting_shaped` | 翻牌显示器 (create:display_board) + 黄铜机壳 (create:brass_casing) → 仪表板 (create_connected:dashboard) | ；配置条件待核对 |
| `create_connected:crafting/kinetics/empty_fan_catalyst` | `minecraft:crafting_shaped` | 4 × 黄铜锭 (create:brass_ingot) + 4 × 铁栏杆 (minecraft:iron_bars) → 空鼓风机触媒 (create_connected:empty_fan_catalyst) | ；配置条件待核对 |
| `create_connected:crafting/kinetics/fluid_vessel` | `minecraft:crafting_shaped` | 2 × 铜板 (create:copper_sheet) + #forge:barrels/wooden → 横向流体储罐 (create_connected:fluid_vessel) | ；配置条件待核对 |
| `create_connected:crafting/kinetics/inventory_access_port` | `minecraft:crafting_shaped` | 黄铜机壳 (create:brass_casing) + 溜槽 (create:chute) + 电子管 (create:electron_tube) → 2 × 容器存取端口 (create_connected:inventory_access_port) | ；配置条件待核对 |
| `create_connected:crafting/kinetics/kinetic_battery` | `minecraft:crafting_shaped` | 精密构件 (create:precision_mechanism) + 黄铜机壳 (create:brass_casing) + 2 × 铁板 (create:iron_sheet) + 红石粉 (minecraft:redstone) → 8 × 动力电池 (create_connected:kinetic_battery) | ；配置条件待核对 |
| `create_connected:crafting/kinetics/kinetic_bridge` | `minecraft:crafting_shaped` | 2 × 黄铜机壳 (create:brass_casing) + 2 × 传动杆 (create:shaft) + 离合器 (create:clutch) → 动力桥接器 (create_connected:kinetic_bridge) | ；配置条件待核对 |
| `create_connected:crafting/kinetics/sequenced_pulse_generator` | `minecraft:crafting_shaped` | 2 × 电子管 (create:electron_tube) + 控制芯片 (create_connected:control_chip) + 黄铜板 (create:brass_sheet) + 红石火把 (minecraft:redstone_torch) + 3 × #forge:stone → 可编程脉冲生成器 (create_connected:sequenced_pulse_generator) | ；配置条件待核对 |
| `create_connected:crafting/kinetics/brass_gearbox_from_conversion` | `minecraft:crafting_shapeless` | 黄铜竖直十字齿轮箱 (create_connected:vertical_brass_gearbox) → 黄铜十字齿轮箱 (create_connected:brass_gearbox) | ；配置条件待核对 |
| `create_connected:crafting/kinetics/vertical_brass_gearbox_from_conversion` | `minecraft:crafting_shapeless` | 黄铜十字齿轮箱 (create_connected:brass_gearbox) → 黄铜竖直十字齿轮箱 (create_connected:vertical_brass_gearbox) | ；配置条件待核对 |

### create_dragons_plus

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `create_dragons_plus:freezing/molten_brass_to_ingots` | `create_dragons_plus:freezing` | dimensionworks:molten_brass_bucket → 铁桶 (minecraft:bucket) + 黄铜锭 (create:brass_ingot) | ；静态启用 |
| `create_dragons_plus:freezing/molten_bronze_to_ingots` | `create_dragons_plus:freezing` | dimensionworks:molten_bronze_bucket → 铁桶 (minecraft:bucket) + 青铜锭 (mekanism:ingot_bronze) | ；静态启用 |
| `create_dragons_plus:freezing/molten_copper_to_ingots` | `create_dragons_plus:freezing` | dimensionworks:molten_copper_bucket → 铁桶 (minecraft:bucket) + 铜锭 (minecraft:copper_ingot) | ；静态启用 |
| `create_dragons_plus:crafting/fluid_hatch` | `minecraft:crafting_shapeless` | #forge:ingots/copper + 分液池 (create:item_drain) → 流体舱口 (create_dragons_plus:fluid_hatch) | ；配置条件待核对 |

### create_enchantment_industry

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `create_enchantment_industry:crafting/disenchanter` | `minecraft:crafting_shaped` | #create:sandpaper + 铜机壳 (create:copper_casing) → 祛魔池 (create_enchantment_industry:disenchanter) | ；静态启用 |
| `create_enchantment_industry:crafting/printer` | `minecraft:crafting_shaped` | 铜机壳 (create:copper_casing) + 干海带 (minecraft:dried_kelp) + #forge:plates/iron → 打印机 (create_enchantment_industry:printer) | ；静态启用 |

### create_integrated_farming

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `create_integrated_farming:crafting/vacuum_harvester` | `minecraft:crafting_shapeless` | 动力收割机 (create:mechanical_harvester) + 鼓风机 (create:encased_fan) + 黄铜机壳 (create:brass_casing) + 溜槽 (create:chute) → 真空收割机 (create_integrated_farming:vacuum_harvester) | ；静态启用 |

### create_mobile_packages

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `create_mobile_packages:mobile_packager` | `minecraft:crafting_shaped` | 2 × 黄铜板 (create:brass_sheet) + 打包机 (create:packager) + 纸板 (create:cardboard) + 2 × 木棍 (minecraft:stick) → 手持式打包工具 (create_mobile_packages:mobile_packager) | ；静态启用 |

### create_power_loader

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `create_power_loader:crafting/empty_brass_chunk_loader` | `create:mechanical_crafting` | 9 × #forge:glass + 重生锚 (minecraft:respawn_anchor) + 6 × 黄铜机壳 (create:brass_casing) + 3 × 精密构件 (create:precision_mechanism) + 传动杆 (create:shaft) → 空的黄铜区块加载器 (create_power_loader:empty_brass_chunk_loader) | ；静态启用 |

### createadditionallogistics

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `createadditionallogistics:splashing/flexible_shaft` | `create:splashing` | #createadditionallogistics:flexible_shafts → 柔性惰性传动杆 (createadditionallogistics:flexible_shaft) | ；静态启用 |
| `createadditionallogistics:crafting/kinetics/flexible_shaft` | `minecraft:crafting_shaped` | 2 × #createadditionallogistics:basic_shafts + 2 × 齿轮 (create:cogwheel) + 黄铜机壳 (create:brass_casing) → 4 × 柔性惰性传动杆 (createadditionallogistics:flexible_shaft) | ；静态启用 |
| `createadditionallogistics:crafting/logistics/cash_register` | `minecraft:crafting_shaped` | #forge:glass + 黄铜板 (create:brass_sheet) + 仓储链接站 (create:stock_link) + 书 (minecraft:book) + #forge:chests → 收银机 (createadditionallogistics:cash_register) | ；静态启用 |
| `createadditionallogistics:crafting/logistics/package_accelerator` | `minecraft:crafting_shaped` | 强力胶 (create:super_glue) + 精密构件 (create:precision_mechanism) + 纸板块 (create:cardboard_block) + 黄铜机壳 (create:brass_casing) + 齿轮 (create:cogwheel) → 打包机加速器 (createadditionallogistics:package_accelerator) | ；静态启用 |
| `createadditionallogistics:crafting/kinetics/black_flexible_shaft_from_other_flexible_shaft` | `minecraft:crafting_shapeless` | #forge:dyes/black + #createadditionallogistics:flexible_shafts → 黑色柔性惰性传动杆 (createadditionallogistics:black_flexible_shaft) | ；静态启用 |
| `createadditionallogistics:crafting/kinetics/blue_flexible_shaft_from_other_flexible_shaft` | `minecraft:crafting_shapeless` | #forge:dyes/blue + #createadditionallogistics:flexible_shafts → 蓝色柔性惰性传动杆 (createadditionallogistics:blue_flexible_shaft) | ；静态启用 |
| `createadditionallogistics:crafting/kinetics/brown_flexible_shaft_from_other_flexible_shaft` | `minecraft:crafting_shapeless` | #forge:dyes/brown + #createadditionallogistics:flexible_shafts → 棕色柔性惰性传动杆 (createadditionallogistics:brown_flexible_shaft) | ；静态启用 |
| `createadditionallogistics:crafting/kinetics/cyan_flexible_shaft_from_other_flexible_shaft` | `minecraft:crafting_shapeless` | #forge:dyes/cyan + #createadditionallogistics:flexible_shafts → 青色柔性惰性传动杆 (createadditionallogistics:cyan_flexible_shaft) | ；静态启用 |
| `createadditionallogistics:crafting/kinetics/flexible_shaft_remove_dye` | `minecraft:crafting_shapeless` | #createadditionallogistics:flexible_shafts → 柔性惰性传动杆 (createadditionallogistics:flexible_shaft) | ；静态启用 |
| `createadditionallogistics:crafting/kinetics/gray_flexible_shaft_from_other_flexible_shaft` | `minecraft:crafting_shapeless` | #forge:dyes/gray + #createadditionallogistics:flexible_shafts → 灰色柔性惰性传动杆 (createadditionallogistics:gray_flexible_shaft) | ；静态启用 |
| `createadditionallogistics:crafting/kinetics/green_flexible_shaft_from_other_flexible_shaft` | `minecraft:crafting_shapeless` | #forge:dyes/green + #createadditionallogistics:flexible_shafts → 绿色柔性惰性传动杆 (createadditionallogistics:green_flexible_shaft) | ；静态启用 |
| `createadditionallogistics:crafting/kinetics/light_blue_flexible_shaft_from_other_flexible_shaft` | `minecraft:crafting_shapeless` | #forge:dyes/light_blue + #createadditionallogistics:flexible_shafts → 浅蓝色柔性惰性传动杆 (createadditionallogistics:light_blue_flexible_shaft) | ；静态启用 |
| `createadditionallogistics:crafting/kinetics/light_gray_flexible_shaft_from_other_flexible_shaft` | `minecraft:crafting_shapeless` | #forge:dyes/light_gray + #createadditionallogistics:flexible_shafts → 浅灰色柔性惰性传动杆 (createadditionallogistics:light_gray_flexible_shaft) | ；静态启用 |
| `createadditionallogistics:crafting/kinetics/lime_flexible_shaft_from_other_flexible_shaft` | `minecraft:crafting_shapeless` | #forge:dyes/lime + #createadditionallogistics:flexible_shafts → 黄绿色柔性惰性传动杆 (createadditionallogistics:lime_flexible_shaft) | ；静态启用 |
| `createadditionallogistics:crafting/kinetics/magenta_flexible_shaft_from_other_flexible_shaft` | `minecraft:crafting_shapeless` | #forge:dyes/magenta + #createadditionallogistics:flexible_shafts → 品红色柔性惰性传动杆 (createadditionallogistics:magenta_flexible_shaft) | ；静态启用 |
| `createadditionallogistics:crafting/kinetics/orange_flexible_shaft_from_other_flexible_shaft` | `minecraft:crafting_shapeless` | #forge:dyes/orange + #createadditionallogistics:flexible_shafts → 橙色柔性惰性传动杆 (createadditionallogistics:orange_flexible_shaft) | ；静态启用 |
| `createadditionallogistics:crafting/kinetics/pink_flexible_shaft_from_other_flexible_shaft` | `minecraft:crafting_shapeless` | #forge:dyes/pink + #createadditionallogistics:flexible_shafts → 粉红色柔性惰性传动杆 (createadditionallogistics:pink_flexible_shaft) | ；静态启用 |
| `createadditionallogistics:crafting/kinetics/purple_flexible_shaft_from_other_flexible_shaft` | `minecraft:crafting_shapeless` | #forge:dyes/purple + #createadditionallogistics:flexible_shafts → 紫色柔性惰性传动杆 (createadditionallogistics:purple_flexible_shaft) | ；静态启用 |
| `createadditionallogistics:crafting/kinetics/red_flexible_shaft_from_other_flexible_shaft` | `minecraft:crafting_shapeless` | #forge:dyes/red + #createadditionallogistics:flexible_shafts → 红色柔性惰性传动杆 (createadditionallogistics:red_flexible_shaft) | ；静态启用 |
| `createadditionallogistics:crafting/kinetics/white_flexible_shaft_from_other_flexible_shaft` | `minecraft:crafting_shapeless` | #forge:dyes/white + #createadditionallogistics:flexible_shafts → 白色柔性惰性传动杆 (createadditionallogistics:white_flexible_shaft) | ；静态启用 |
| `createadditionallogistics:crafting/kinetics/yellow_flexible_shaft_from_other_flexible_shaft` | `minecraft:crafting_shapeless` | #forge:dyes/yellow + #createadditionallogistics:flexible_shafts → 黄色柔性惰性传动杆 (createadditionallogistics:yellow_flexible_shaft) | ；静态启用 |

### createdieselgenerators

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `createdieselgenerators:crushing/wood_chip_casings` | `create:crushing` | #create:casing / 轴向底盘 (create:linear_chassis) / 轴向底盘二号 (create:secondary_linear_chassis) / 径向底盘 (create:radial_chassis) → 31 × 木屑 (createdieselgenerators:wood_chip) + 木屑 (createdieselgenerators:wood_chip) [50.0%] | ；静态启用 |
| `createdieselgenerators:crushing/wood_chip_doors` | `create:crushing` | #minecraft:wooden_doors → 15 × 木屑 (createdieselgenerators:wood_chip) + 木屑 (createdieselgenerators:wood_chip) [50.0%] | ；静态启用 |
| `createdieselgenerators:mechanical_crafting/chemcial_sprayer` | `create:mechanical_crafting` | 2 × 海带手柄 (createdieselgenerators:kelp_handle) + 精密构件 (create:precision_mechanism) + #forge:storage_blocks/copper + 干海带 (minecraft:dried_kelp) + 流体管道 (create:fluid_pipe) → 化学喷雾器 (createdieselgenerators:chemical_sprayer) | ；静态启用 |
| `createdieselgenerators:hammering/brass_sheet` | `createdieselgenerators:hammering` | #forge:ingots/brass → 黄铜板 (create:brass_sheet) | ；静态启用 |
| `createdieselgenerators:hammering/copper_sheet` | `createdieselgenerators:hammering` | #forge:ingots/copper → 铜板 (create:copper_sheet) | ；静态启用 |
| `createdieselgenerators:crafting/burner` | `minecraft:crafting_shaped` | 2 × 打火石 (minecraft:flint_and_steel) + #forge:ingots/brass + 传动杆 (create:shaft) + 2 × 安山合金 (create:andesite_alloy) + 空的烈焰人燃烧室 (create:empty_blaze_burner) → 可控燃烧室 (createdieselgenerators:burner) | ；静态启用 |
| `createdieselgenerators:crafting/chemical_turret` | `minecraft:crafting_shaped` | 精密构件 (create:precision_mechanism) + 3 × #forge:plates/copper + 化学喷雾器 (createdieselgenerators:chemical_sprayer) + 齿轮 (create:cogwheel) + 铜机壳 (create:copper_casing) → 动力化学喷雾器 (createdieselgenerators:chemical_turret) | ；静态启用 |
| `createdieselgenerators:crafting/chemical_turret_lighter` | `minecraft:crafting_shaped` | 精密构件 (create:precision_mechanism) + 3 × #forge:plates/copper + 装有打火机的化学喷雾器 (createdieselgenerators:chemical_sprayer_lighter) + 齿轮 (create:cogwheel) + 铜机壳 (create:copper_casing) → 动力化学喷雾器 (createdieselgenerators:chemical_turret) NBT={"BlockEntityTag": {"LighterUpgrade": true}} | ；静态启用 |
| `createdieselgenerators:crafting/diesel_engine` | `minecraft:crafting_shaped` | 打火石 (minecraft:flint_and_steel) + 2 × 引擎活塞 (createdieselgenerators:engine_piston) + #forge:storage_blocks/brass + 2 × 磨制黑石台阶 (minecraft:polished_blackstone_slab) + 流体储罐 (create:fluid_tank) → 柴油引擎 (createdieselgenerators:diesel_engine) | ；静态启用 |
| `createdieselgenerators:crafting/entity_filter` | `minecraft:crafting_shaped` | 2 × #forge:nuggets/copper + #minecraft:wool → 实体过滤器 (createdieselgenerators:entity_filter) | ；静态启用 |
| `createdieselgenerators:crafting/huge_diesel_engine` | `minecraft:crafting_shaped` | 2 × 安山合金 (create:andesite_alloy) + 打火石 (minecraft:flint_and_steel) + 2 × #forge:plates/brass + 蒸汽引擎 (create:steam_engine) + 2 × 流体管道 (create:fluid_pipe) + #forge:storage_blocks/brass → 大型柴油引擎 (createdieselgenerators:huge_diesel_engine) | ；静态启用 |
| `createdieselgenerators:crafting/large_diesel_engine` | `minecraft:crafting_shaped` | 安山合金 (create:andesite_alloy) + 2 × #forge:plates/brass + 柴油引擎 (createdieselgenerators:diesel_engine) + 磨制黑石台阶 (minecraft:polished_blackstone_slab) → 模块柴油引擎 (createdieselgenerators:large_diesel_engine) | ；静态启用 |
| `createdieselgenerators:crafting/lighter` | `minecraft:crafting_shaped` | 5 × #forge:plates/brass + 打火石 (minecraft:flint_and_steel) + 线 (minecraft:string) + 安山合金 (create:andesite_alloy) → 打火机 (createdieselgenerators:lighter) | ；静态启用 |
| `createdieselgenerators:crafting/pumpjack_hole` | `minecraft:crafting_shaped` | 2 × 流体管道 (create:fluid_pipe) + 2 × 锁链 (minecraft:chain) + 铜机壳 (create:copper_casing) → 抽油机油泵 (createdieselgenerators:pumpjack_hole) | ；静态启用 |

### dimensionworks

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `dimensionworks:copper_processing/dust_from_ingot` | `create:crushing` | 铜锭 (minecraft:copper_ingot) → 铜粉 (mekanism:dust_copper) + 2 × 铜粉 (mekanism:dust_copper) [80.0%] + 铜粉 (mekanism:dust_copper) [60.0%] | ；KubeJS脚本定义 |
| `dimensionworks:advanced_speed_controller` | `create:mechanical_crafting` | 4 × 黄铜机壳 (create:brass_casing) + 8 × 精密构件 (create:precision_mechanism) + 4 × 坚固板 (create:sturdy_sheet) + 4 × 工程处理器 (ae2:engineering_processor) + 4 × 强化合金 (mekanism:alloy_reinforced) + 转速控制器 (create:rotation_speed_controller) → 高级转速控制器 (dimensionworks_rpm_limit:advanced_speed_controller) | ；静态启用 |
| `dimensionworks:copper_processing/reduction_with_carbon_monoxide` | `create:mixing` | 氧化铜 (minecraft:raw_copper) + 250 mB dimensionworks:carbon_monoxide → 铜锭 (minecraft:copper_ingot) + 250 mB dimensionworks:carbon_dioxide | {"heatRequirement":"heated","excludeSuperheated":true}；KubeJS脚本定义 |
| `dimensionworks:copper_processing/veridium_from_dust` | `create:mixing` | 铜粉 (mekanism:dust_copper) + 铜粉 (mekanism:dust_copper) + 铜粉 (mekanism:dust_copper) + 铜粉 (mekanism:dust_copper) + 圆石 (minecraft:cobblestone) → 2 × 辉绿岩 (create:veridium) | {"heatRequirement":"heated"}；KubeJS脚本定义 |
| `dimensionworks:molten_metals/brass_from_molten_metals` | `create:mixing` | 1000 mB dimensionworks:molten_copper + 1000 mB dimensionworks:molten_zinc → 2000 mB dimensionworks:molten_brass | {"heatRequirement":"heated"}；KubeJS脚本定义 |
| `dimensionworks:molten_metals/bronze_from_molten_metals` | `create:mixing` | 3000 mB dimensionworks:molten_copper + 1000 mB dimensionworks:molten_tin → 4000 mB dimensionworks:molten_bronze | {"heatRequirement":"heated"}；KubeJS脚本定义 |
| `dimensionworks:molten_metals/molten_brass_from_ingot` | `create:mixing` | 黄铜锭 (create:brass_ingot) → 1000 mB dimensionworks:molten_brass | {"heatRequirement":"heated"}；KubeJS脚本定义 |
| `dimensionworks:molten_metals/molten_bronze_from_ingot` | `create:mixing` | 青铜锭 (mekanism:ingot_bronze) → 1000 mB dimensionworks:molten_bronze | {"heatRequirement":"heated"}；KubeJS脚本定义 |
| `dimensionworks:molten_metals/molten_copper_from_ingot` | `create:mixing` | 铜锭 (minecraft:copper_ingot) → 1000 mB dimensionworks:molten_copper | {"heatRequirement":"heated"}；KubeJS脚本定义 |
| `dimensionworks:copper_processing/oxide_from_veridium` | `mekanism:purifying` | 辉绿岩 (create:veridium) + 1 mB 液态氧 (mekanism:oxygen) → 2 × 氧化铜 (minecraft:raw_copper) | ；KubeJS脚本定义 |

### dimensionworks_cave_factory

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `dimensionworks_cave_factory:self_propagation/basic_minerals/tin/mechanical_growth` | `create:compacting` | 粗锡 (mekanism:raw_tin) + 圆石 (minecraft:cobblestone) + 圆石 (minecraft:cobblestone) + 沙砾 (minecraft:gravel) + 铜锭 (minecraft:copper_ingot) → 2 × 粗锡 (mekanism:raw_tin) | ；KubeJS脚本定义 |
| `dimensionworks_cave_factory:self_propagation/basic_minerals/osmium/mekanism_combining` | `mekanism:combining` | 粗锇 (mekanism:raw_osmium) + 2 × 黄铜锭 (create:brass_ingot) → 2 × 粗锇 (mekanism:raw_osmium) | ；KubeJS脚本定义 |
| `dimensionworks_cave_factory:self_propagation/basic_minerals/zinc/mekanism_combining` | `mekanism:combining` | 粗锌 (create:raw_zinc) + 2 × 黄铜锭 (create:brass_ingot) → 2 × 粗锌 (create:raw_zinc) | ；KubeJS脚本定义 |
| `dimensionworks_cave_factory:survival/factory_casing` | `minecraft:crafting_shaped` | 4 × 工业铁块 (create:industrial_iron_block) + 4 × 黄铜机壳 (create:brass_casing) + 传动杆 (create:shaft) → 8 × 工厂机壳 (dimensionworks_cave_factory:factory_casing) | ；静态启用 |

### farmersdelight

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `farmersdelight:cutting/allium` | `farmersdelight:cutting` | 绒球葱 (minecraft:allium) → 2 × 品红色染料 (minecraft:magenta_dye) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/apple_pie` | `farmersdelight:cutting` | 苹果派 (farmersdelight:apple_pie) → 4 × 苹果派切片 (farmersdelight:apple_pie_slice) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/azure_bluet` | `farmersdelight:cutting` | 蓝花美耳草 (minecraft:azure_bluet) → 2 × 淡灰色染料 (minecraft:light_gray_dye) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/beef` | `farmersdelight:cutting` | 生牛肉 (minecraft:beef) → 2 × 牛肉馅 (farmersdelight:minced_beef) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/blue_berry` | `farmersdelight:cutting` | 蓝莓 (aether:blue_berry) → 蓝色染料 (minecraft:blue_dye) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `farmersdelight:cutting/blue_orchid` | `farmersdelight:cutting` | 兰花 (minecraft:blue_orchid) → 2 × 淡蓝色染料 (minecraft:light_blue_dye) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/brown_mushroom_colony` | `farmersdelight:cutting` | 棕色蘑菇菌落 (farmersdelight:brown_mushroom_colony) → 5 × 棕色蘑菇 (minecraft:brown_mushroom) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/cabbage` | `farmersdelight:cutting` | 卷心菜 (farmersdelight:cabbage) → 2 × 卷心菜叶 (farmersdelight:cabbage_leaf) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/cake` | `farmersdelight:cutting` | 蛋糕 (minecraft:cake) → 7 × 蛋糕切片 (farmersdelight:cake_slice) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/chicken` | `farmersdelight:cutting` | 生鸡肉 (minecraft:chicken) → 2 × 生鸡肉丁 (farmersdelight:chicken_cuts) + 骨粉 (minecraft:bone_meal) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/chocolate_pie` | `farmersdelight:cutting` | 巧克力派 (farmersdelight:chocolate_pie) → 4 × 巧克力派切片 (farmersdelight:chocolate_pie_slice) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/cod` | `farmersdelight:cutting` | 生鳕鱼 (minecraft:cod) → 2 × 生鳕鱼片 (farmersdelight:cod_slice) + 骨粉 (minecraft:bone_meal) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/cooked_chicken` | `farmersdelight:cutting` | 熟鸡肉 (minecraft:cooked_chicken) → 2 × 熟鸡肉丁 (farmersdelight:cooked_chicken_cuts) + 骨粉 (minecraft:bone_meal) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/cooked_cod` | `farmersdelight:cutting` | 熟鳕鱼 (minecraft:cooked_cod) → 2 × 熟鳕鱼片 (farmersdelight:cooked_cod_slice) + 骨粉 (minecraft:bone_meal) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/cooked_moa` | `farmersdelight:cutting` | 熟莫亚肉 (aethersdelight:cooked_moa) → 2 × 熟莫亚肉片 (aethersdelight:cooked_moa_cuts) + 骨头 (minecraft:bone) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `farmersdelight:cutting/cooked_mutton` | `farmersdelight:cutting` | 熟羊肉 (minecraft:cooked_mutton) → 2 × 熟羊排 (farmersdelight:cooked_mutton_chops) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/cooked_salmon` | `farmersdelight:cutting` | 熟鲑鱼 (minecraft:cooked_salmon) → 2 × 熟鲑鱼片 (farmersdelight:cooked_salmon_slice) + 骨粉 (minecraft:bone_meal) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/cornflower` | `farmersdelight:cutting` | 矢车菊 (minecraft:cornflower) → 2 × 蓝色染料 (minecraft:blue_dye) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/dandelion` | `farmersdelight:cutting` | 蒲公英 (minecraft:dandelion) → 2 × 黄色染料 (minecraft:yellow_dye) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/enchanted_berry` | `farmersdelight:cutting` | 神饯蓝莓 (aether:enchanted_berry) → 黄色染料 (minecraft:yellow_dye) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `farmersdelight:cutting/ginger` | `farmersdelight:cutting` | 生姜 (aethersdelight:ginger) → 2 × 姜香料 (aethersdelight:ginger_spice) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `farmersdelight:cutting/ham` | `farmersdelight:cutting` | 火腿 (farmersdelight:ham) → 2 × 生猪排 (minecraft:porkchop) + 骨头 (minecraft:bone) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/ink_sac` | `farmersdelight:cutting` | 墨囊 (minecraft:ink_sac) → 2 × 黑色染料 (minecraft:black_dye) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/kelp_roll` | `farmersdelight:cutting` | 海带寿司卷 (farmersdelight:kelp_roll) → 3 × 海带寿司 (farmersdelight:kelp_roll_slice) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/lily_of_the_valley` | `farmersdelight:cutting` | 铃兰 (minecraft:lily_of_the_valley) → 2 × 白色染料 (minecraft:white_dye) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/melon` | `farmersdelight:cutting` | 西瓜 (minecraft:melon) → 9 × 西瓜片 (minecraft:melon_slice) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/mutton` | `farmersdelight:cutting` | 生羊肉 (minecraft:mutton) → 2 × 生羊排 (farmersdelight:mutton_chops) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/orange_tulip` | `farmersdelight:cutting` | 橙色郁金香 (minecraft:orange_tulip) → 2 × 橙色染料 (minecraft:orange_dye) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/oxeye_daisy` | `farmersdelight:cutting` | 滨菊 (minecraft:oxeye_daisy) → 2 × 淡灰色染料 (minecraft:light_gray_dye) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/peppermint_leaf` | `farmersdelight:cutting` | 薄荷叶 (aethersdelight:peppermint_leaf) → 绿色染料 (minecraft:green_dye) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `farmersdelight:cutting/pink_tulip` | `farmersdelight:cutting` | 粉红色郁金香 (minecraft:pink_tulip) → 2 × 粉红色染料 (minecraft:pink_dye) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/poppy` | `farmersdelight:cutting` | 虞美人 (minecraft:poppy) → 2 × 红色染料 (minecraft:red_dye) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/porkchop` | `farmersdelight:cutting` | 生猪排 (minecraft:porkchop) → 2 × 生培根 (farmersdelight:bacon) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/pumpkin` | `farmersdelight:cutting` | 南瓜 (minecraft:pumpkin) → 4 × 南瓜片 (farmersdelight:pumpkin_slice) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/pumpkin_pie` | `farmersdelight:cutting` | 南瓜派 (minecraft:pumpkin_pie) → 4 × 南瓜派切片 (farmersdelight:pumpkin_pie_slice) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/purple_flower` | `farmersdelight:cutting` | 幻紫花 (aether:purple_flower) → 2 × 紫色染料 (minecraft:purple_dye) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `farmersdelight:cutting/raw_moa` | `farmersdelight:cutting` | 生莫亚肉 (aethersdelight:raw_moa) → 2 × 生莫亚肉片 (aethersdelight:raw_moa_cuts) + 骨头 (minecraft:bone) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `farmersdelight:cutting/red_mushroom_colony` | `farmersdelight:cutting` | 红色蘑菇菌落 (farmersdelight:red_mushroom_colony) → 5 × 红色蘑菇 (minecraft:red_mushroom) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/red_tulip` | `farmersdelight:cutting` | 红色郁金香 (minecraft:red_tulip) → 2 × 红色染料 (minecraft:red_dye) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/rice_panicle` | `farmersdelight:cutting` | 稻米穗 (farmersdelight:rice_panicle) → 稻米 (farmersdelight:rice) + 草秆 (farmersdelight:straw) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/salmon` | `farmersdelight:cutting` | 生鲑鱼 (minecraft:salmon) → 2 × 生鲑鱼片 (farmersdelight:salmon_slice) + 骨粉 (minecraft:bone_meal) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/smoked_ham` | `farmersdelight:cutting` | 烟熏火腿 (farmersdelight:smoked_ham) → 2 × 熟猪排 (minecraft:cooked_porkchop) + 骨头 (minecraft:bone) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/sweet_berry_cheesecake` | `farmersdelight:cutting` | 甜浆果芝士派 (farmersdelight:sweet_berry_cheesecake) → 4 × 甜浆果芝士派切片 (farmersdelight:sweet_berry_cheesecake_slice) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/tag_dough` | `farmersdelight:cutting` | #forge:dough → 生意面 (farmersdelight:raw_pasta) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/torchflower` | `farmersdelight:cutting` | 火把花 (minecraft:torchflower) → 2 × 橙色染料 (minecraft:orange_dye) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/white_flower` | `farmersdelight:cutting` | 白玫瑰 (aether:white_flower) → 2 × 白色染料 (minecraft:white_dye) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `farmersdelight:cutting/white_tulip` | `farmersdelight:cutting` | 白色郁金香 (minecraft:white_tulip) → 2 × 淡灰色染料 (minecraft:light_gray_dye) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/wild_beetroots` | `farmersdelight:cutting` | 海甜菜 (farmersdelight:wild_beetroots) → 甜菜种子 (minecraft:beetroot_seeds) + 红色染料 (minecraft:red_dye) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/wild_cabbages` | `farmersdelight:cutting` | 野生卷心菜 (farmersdelight:wild_cabbages) → 卷心菜种子 (farmersdelight:cabbage_seeds) + 2 × 黄色染料 (minecraft:yellow_dye) [50.0%] | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/wild_carrots` | `farmersdelight:cutting` | 野生胡萝卜 (farmersdelight:wild_carrots) → 胡萝卜 (minecraft:carrot) + 2 × 淡灰色染料 (minecraft:light_gray_dye) [50.0%] | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/wild_onions` | `farmersdelight:cutting` | 野生洋葱 (farmersdelight:wild_onions) → 洋葱 (farmersdelight:onion) + 2 × 品红色染料 (minecraft:magenta_dye) + 黄绿色染料 (minecraft:lime_dye) [10.0%] | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/wild_potatoes` | `farmersdelight:cutting` | 野生马铃薯 (farmersdelight:wild_potatoes) → 马铃薯 (minecraft:potato) + 2 × 紫色染料 (minecraft:purple_dye) [50.0%] | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/wild_rice` | `farmersdelight:cutting` | 野生稻米 (farmersdelight:wild_rice) → 稻米 (farmersdelight:rice) + 草秆 (farmersdelight:straw) [50.0%] | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/wild_tomatoes` | `farmersdelight:cutting` | 番茄灌木 (farmersdelight:wild_tomatoes) → 番茄种子 (farmersdelight:tomato_seeds) + 番茄 (farmersdelight:tomato) [20.0%] + 绿色染料 (minecraft:green_dye) [10.0%] | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |
| `farmersdelight:cutting/wither_rose` | `farmersdelight:cutting` | 凋灵玫瑰 (minecraft:wither_rose) → 2 × 黑色染料 (minecraft:black_dye) | {"tool":[{"type":"farmersdelight:tool_action","action":"knife_dig"},{"tag":"forge:tools/knives"}]}；静态启用 |

### kaleidoscope_cookery

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `kaleidoscope_cookery:bamboo_tray/wetting/copper_block_to_oxidized_copper` | `kaleidoscope_cookery:bamboo_tray` | 铜块 (minecraft:copper_block) → 氧化的铜块 (minecraft:oxidized_copper) | {"duration":900}；静态启用 |
| `kaleidoscope_cookery:millstone/raw_copper_from_copper_ore` | `kaleidoscope_cookery:millstone` | 铜矿石 (minecraft:copper_ore) → 5 × 氧化铜 (minecraft:raw_copper) | ；静态启用 |
| `kaleidoscope_cookery:millstone/raw_copper_from_deepslate_copper_ore` | `kaleidoscope_cookery:millstone` | 深层铜矿石 (minecraft:deepslate_copper_ore) → 5 × 氧化铜 (minecraft:raw_copper) | ；静态启用 |
| `kaleidoscope_cookery:teapot` | `minecraft:crafting_shaped` | 6 × #forge:ingots/copper + 铁桶 (minecraft:bucket) → 茶壶 (kaleidoscope_cookery:teapot) | ；静态启用 |

### kaleidoscope_dim_wine

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `kaleidoscope_dim_wine:integration/kaleidoscope_twilight/barrel/druids_secret_brew` | `kaleidoscope_tavern:barrel` | twilightforest:liveroot + twilightforest:armor_shard + #c:nuggets/copper → {"count":1,"id":"kaleidoscope_dim_wine:druids_secret_brew"} | ；静态启用 |

### kaleidoscope_tavern

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `kaleidoscope_tavern:shaker/brass_heart` | `kaleidoscope_tavern:shaker` | #kaleidoscope_tavern:cocktail_ingredient_gold + #kaleidoscope_tavern:cocktail_ingredient_gold + #kaleidoscope_tavern:cocktail_ingredient_gold → 黄铜心脏 (kaleidoscope_tavern:brass_heart) | ；静态启用 |

### kubejs

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `kubejs:gear_heart_repair_fragment_2` | `create:compacting` | 铁板 (create:iron_sheet) + 黄铜板 (create:brass_sheet) → kubejs:gear_heart_repair_fragment_2 | ；KubeJS脚本定义 |

### l2complements

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `l2complements:craft/material_swap_smithing_template` | `minecraft:crafting_shaped` | 7 × 铜锭 (minecraft:copper_ingot) + 闪长岩 (minecraft:diorite) → 材料替换锻造模版 (l2complements:material_swap_smithing_template) | ；静态启用 |
| `l2complements:craft/sculkium_ingot` | `minecraft:crafting_shapeless` | 回响碎片 (minecraft:echo_shard) + 强化骨片 (l2complements:warden_bone_shard) + 强化骨片 (l2complements:warden_bone_shard) + 铜锭 (minecraft:copper_ingot) → 2 × 幽匿锭 (l2complements:sculkium_ingot) | ；静态启用 |
| `l2complements:generated_tools/poseidite/upgrade/poseidite_boots` | `minecraft:smithing_transform` | 海神块 (l2complements:poseidite_block) + #forge:armors/boots + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 海神靴子 (l2complements:poseidite_boots) | ；静态启用 |
| `l2complements:generated_tools/poseidite/upgrade/poseidite_chestplate` | `minecraft:smithing_transform` | 海神块 (l2complements:poseidite_block) + #forge:armors/chestplates + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 海神胸甲 (l2complements:poseidite_chestplate) | ；静态启用 |
| `l2complements:generated_tools/poseidite/upgrade/poseidite_helmet` | `minecraft:smithing_transform` | 海神块 (l2complements:poseidite_block) + #forge:armors/helmets + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 海神头盔 (l2complements:poseidite_helmet) | ；静态启用 |
| `l2complements:generated_tools/poseidite/upgrade/poseidite_sword` | `minecraft:smithing_transform` | 海神块 (l2complements:poseidite_block) + #minecraft:swords + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 海神剑 (l2complements:poseidite_sword) | ；静态启用 |
| `l2complements:generated_tools/sculkium/upgrade/sculkium_boots` | `minecraft:smithing_transform` | 幽匿块 (l2complements:sculkium_block) + #forge:armors/boots + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 幽匿靴子 (l2complements:sculkium_boots) | ；静态启用 |
| `l2complements:generated_tools/sculkium/upgrade/sculkium_chestplate` | `minecraft:smithing_transform` | 幽匿块 (l2complements:sculkium_block) + #forge:armors/chestplates + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 幽匿胸甲 (l2complements:sculkium_chestplate) | ；静态启用 |
| `l2complements:generated_tools/sculkium/upgrade/sculkium_helmet` | `minecraft:smithing_transform` | 幽匿块 (l2complements:sculkium_block) + #forge:armors/helmets + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 幽匿头盔 (l2complements:sculkium_helmet) | ；静态启用 |
| `l2complements:generated_tools/sculkium/upgrade/sculkium_sword` | `minecraft:smithing_transform` | 幽匿块 (l2complements:sculkium_block) + #minecraft:swords + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 幽匿剑 (l2complements:sculkium_sword) | ；静态启用 |
| `l2complements:generated_tools/shulkerate/upgrade/shulkerate_boots` | `minecraft:smithing_transform` | 潜影块 (l2complements:shulkerate_block) + #forge:armors/boots + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 潜影靴子 (l2complements:shulkerate_boots) | ；静态启用 |
| `l2complements:generated_tools/shulkerate/upgrade/shulkerate_chestplate` | `minecraft:smithing_transform` | 潜影块 (l2complements:shulkerate_block) + #forge:armors/chestplates + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 潜影胸甲 (l2complements:shulkerate_chestplate) | ；静态启用 |
| `l2complements:generated_tools/shulkerate/upgrade/shulkerate_helmet` | `minecraft:smithing_transform` | 潜影块 (l2complements:shulkerate_block) + #forge:armors/helmets + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 潜影头盔 (l2complements:shulkerate_helmet) | ；静态启用 |
| `l2complements:generated_tools/shulkerate/upgrade/shulkerate_sword` | `minecraft:smithing_transform` | 潜影块 (l2complements:shulkerate_block) + #minecraft:swords + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 潜影剑 (l2complements:shulkerate_sword) | ；静态启用 |
| `l2complements:generated_tools/totemic_gold/upgrade/totemic_gold_boots` | `minecraft:smithing_transform` | 生命块 (l2complements:totemic_gold_block) + #forge:armors/boots + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 生命靴子 (l2complements:totemic_gold_boots) | ；静态启用 |
| `l2complements:generated_tools/totemic_gold/upgrade/totemic_gold_chestplate` | `minecraft:smithing_transform` | 生命块 (l2complements:totemic_gold_block) + #forge:armors/chestplates + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 生命胸甲 (l2complements:totemic_gold_chestplate) | ；静态启用 |
| `l2complements:generated_tools/totemic_gold/upgrade/totemic_gold_helmet` | `minecraft:smithing_transform` | 生命块 (l2complements:totemic_gold_block) + #forge:armors/helmets + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 生命头盔 (l2complements:totemic_gold_helmet) | ；静态启用 |
| `l2complements:generated_tools/totemic_gold/upgrade/totemic_gold_sword` | `minecraft:smithing_transform` | 生命块 (l2complements:totemic_gold_block) + #minecraft:swords + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 生命剑 (l2complements:totemic_gold_sword) | ；静态启用 |
| `l2complements:vanilla/upgrade/diamond_boots` | `minecraft:smithing_transform` | 钻石块 (minecraft:diamond_block) + #forge:armors/boots + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 钻石靴子 (minecraft:diamond_boots) | ；静态启用 |
| `l2complements:vanilla/upgrade/diamond_chestplate` | `minecraft:smithing_transform` | 钻石块 (minecraft:diamond_block) + #forge:armors/chestplates + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 钻石胸甲 (minecraft:diamond_chestplate) | ；静态启用 |
| `l2complements:vanilla/upgrade/diamond_helmet` | `minecraft:smithing_transform` | 钻石块 (minecraft:diamond_block) + #forge:armors/helmets + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 钻石头盔 (minecraft:diamond_helmet) | ；静态启用 |
| `l2complements:vanilla/upgrade/diamond_sword` | `minecraft:smithing_transform` | 钻石块 (minecraft:diamond_block) + #minecraft:swords + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 钻石剑 (minecraft:diamond_sword) | ；静态启用 |
| `l2complements:vanilla/upgrade/golden_boots` | `minecraft:smithing_transform` | 金块 (minecraft:gold_block) + #forge:armors/boots + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 金靴子 (minecraft:golden_boots) | ；静态启用 |
| `l2complements:vanilla/upgrade/golden_chestplate` | `minecraft:smithing_transform` | 金块 (minecraft:gold_block) + #forge:armors/chestplates + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 金胸甲 (minecraft:golden_chestplate) | ；静态启用 |
| `l2complements:vanilla/upgrade/golden_helmet` | `minecraft:smithing_transform` | 金块 (minecraft:gold_block) + #forge:armors/helmets + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 金头盔 (minecraft:golden_helmet) | ；静态启用 |
| `l2complements:vanilla/upgrade/golden_sword` | `minecraft:smithing_transform` | 金块 (minecraft:gold_block) + #minecraft:swords + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 金剑 (minecraft:golden_sword) | ；静态启用 |
| `l2complements:vanilla/upgrade/iron_boots` | `minecraft:smithing_transform` | 铁块 (minecraft:iron_block) + #forge:armors/boots + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 铁靴子 (minecraft:iron_boots) | ；静态启用 |
| `l2complements:vanilla/upgrade/iron_chestplate` | `minecraft:smithing_transform` | 铁块 (minecraft:iron_block) + #forge:armors/chestplates + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 铁胸甲 (minecraft:iron_chestplate) | ；静态启用 |
| `l2complements:vanilla/upgrade/iron_helmet` | `minecraft:smithing_transform` | 铁块 (minecraft:iron_block) + #forge:armors/helmets + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 铁头盔 (minecraft:iron_helmet) | ；静态启用 |
| `l2complements:vanilla/upgrade/iron_sword` | `minecraft:smithing_transform` | 铁块 (minecraft:iron_block) + #minecraft:swords + 材料替换锻造模版 (l2complements:material_swap_smithing_template) → 铁剑 (minecraft:iron_sword) | ；静态启用 |

### laowu

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `laowu:intermediate_breeding_box_deploying` | `create:deploying` | 初级繁育箱 (laowu:basic_breeding_box) + #create:table_cloths → 中级繁育箱 (laowu:intermediate_breeding_box) | ；静态启用 |
| `laowu:infiltration_tank_mechanical_crafting` | `create:mechanical_crafting` | 8 × #forge:ingots/brass + 2 × 猫锭 (laowu:cat_ingot) + 4 × 玻璃 (minecraft:glass) + 2 × 分液池 (create:item_drain) → 渗透罐 (laowu:infiltration_tank) | ；静态启用 |
| `laowu:diving_suit_sequenced_assembly` | `create:sequenced_assembly` | 猫构件 (laowu:cat_component) → 潜水套装 (laowu:diving_suit) [100.0%] | {"loops":1,"sequence":["create:deploying: 未完成的潜水套装 (laowu:incomplete_diving_suit) + 铜潜水头盔 (create:copper_diving_helmet) → 未完成的潜水套装 (laowu:incomplete_diving_suit)","create:deploying: 未完成的潜水套装 (laowu:incomplete_diving_suit) + 动力泵 (create:mechanical_pump) → 未完成的潜水套装 (laowu:incomplete_diving_suit)"]}；静态启用 |
| `laowu:golden_cat_can_sequenced_assembly` | `create:sequenced_assembly` | 猫罐头 (laowu:cat_can) → 金色猫罐头 (laowu:golden_cat_can) [100.0%] | {"loops":1,"sequence":["create:deploying: 猫罐头 (laowu:cat_can) + 金板 (create:golden_sheet) → 猫罐头 (laowu:cat_can)","create:deploying: 猫罐头 (laowu:cat_can) + 黄铜粒 (create:brass_nugget) → 猫罐头 (laowu:cat_can)"]}；静态启用 |
| `laowu:cat_carrier` | `minecraft:crafting_shaped` | 3 × 铁锭 (minecraft:iron_ingot) + 2 × 玻璃板 (minecraft:glass_pane) + 铁栏杆 (minecraft:iron_bars) + 3 × 黄铜锭 (create:brass_ingot) → 航空箱 (laowu:cat_carrier) | ；静态启用 |
| `laowu:cat_engine` | `minecraft:crafting_shaped` | 2 × 猫板 (laowu:cat_sheet) + 黄铜块 (create:brass_block) + 2 × 磨制黑石台阶 (minecraft:polished_blackstone_slab) + 流体储罐 (create:fluid_tank) → 猫咪引擎 (laowu:cat_engine) | ；静态启用 |
| `laowu:cat_pouch` | `minecraft:crafting_shaped` | 线 (minecraft:string) + 3 × 皮革 (minecraft:leather) + 黄铜锭 (create:brass_ingot) → 猫袋 (laowu:cat_pouch) | ；静态启用 |
| `laowu:cat_shell` | `minecraft:crafting_shaped` | 2 × 猫板 (laowu:cat_sheet) + 黄铜板 (create:brass_sheet) → 猫弹壳 (laowu:cat_shell) | ；静态启用 |
| `laowu:hissing_collector` | `minecraft:crafting_shaped` | #forge:ingots/copper + #minecraft:wool + 3 × 安山合金 (create:andesite_alloy) → 哈气收集网 (laowu:hissing_collector) | ；静态启用 |

### megacells

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `megacells:cells/mega_fluid_cell_housing` | `minecraft:crafting_shaped` | 2 × 聚能石英玻璃 (ae2:quartz_vibrant_glass) + 3 × 陨石粉 (ae2:sky_dust) + 3 × #forge:ingots/copper → MEGA 流体元件外壳 (megacells:mega_fluid_cell_housing) | ；静态启用 |
| `megacells:cells/standard/fluid_storage_cell_16m` | `minecraft:crafting_shaped` | 2 × 聚能石英玻璃 (ae2:quartz_vibrant_glass) + 3 × 陨石粉 (ae2:sky_dust) + 16M MEGA 存储组件 (megacells:cell_component_16m) + 3 × #forge:ingots/copper → 16M MEGA 流体存储元件 (megacells:fluid_storage_cell_16m) | ；静态启用 |
| `megacells:cells/standard/fluid_storage_cell_1m` | `minecraft:crafting_shaped` | 2 × 聚能石英玻璃 (ae2:quartz_vibrant_glass) + 3 × 陨石粉 (ae2:sky_dust) + 1M MEGA 存储组件 (megacells:cell_component_1m) + 3 × #forge:ingots/copper → 1M MEGA 流体存储元件 (megacells:fluid_storage_cell_1m) | ；静态启用 |
| `megacells:cells/standard/fluid_storage_cell_256m` | `minecraft:crafting_shaped` | 2 × 聚能石英玻璃 (ae2:quartz_vibrant_glass) + 3 × 陨石粉 (ae2:sky_dust) + 256M MEGA 存储组件 (megacells:cell_component_256m) + 3 × #forge:ingots/copper → 256M MEGA 流体存储元件 (megacells:fluid_storage_cell_256m) | ；静态启用 |
| `megacells:cells/standard/fluid_storage_cell_4m` | `minecraft:crafting_shaped` | 2 × 聚能石英玻璃 (ae2:quartz_vibrant_glass) + 3 × 陨石粉 (ae2:sky_dust) + 4M MEGA 存储组件 (megacells:cell_component_4m) + 3 × #forge:ingots/copper → 4M MEGA 流体存储元件 (megacells:fluid_storage_cell_4m) | ；静态启用 |
| `megacells:cells/standard/fluid_storage_cell_64m` | `minecraft:crafting_shaped` | 2 × 聚能石英玻璃 (ae2:quartz_vibrant_glass) + 3 × 陨石粉 (ae2:sky_dust) + 64M MEGA 存储组件 (megacells:cell_component_64m) + 3 × #forge:ingots/copper → 64M MEGA 流体存储元件 (megacells:fluid_storage_cell_64m) | ；静态启用 |
| `megacells:network/cell_dock` | `minecraft:crafting_shaped` | 2 × #forge:ingots/iron + #forge:ingots/copper + #ae2:glass_cable → ME 元件坞 (megacells:cell_dock) | ；静态启用 |

### mekanism

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `mekanism:combining/wax/waxed_copper_block` | `mekanism:combining` | 蜜脾 (minecraft:honeycomb) + 铜块 (minecraft:copper_block) → 涂蜡的铜块 (minecraft:waxed_copper_block) | ；静态启用 |
| `mekanism:combining/wax/waxed_cut_copper` | `mekanism:combining` | 蜜脾 (minecraft:honeycomb) + 切制铜块 (minecraft:cut_copper) → 涂蜡的切制铜块 (minecraft:waxed_cut_copper) | ；静态启用 |
| `mekanism:combining/wax/waxed_cut_copper_slab` | `mekanism:combining` | 蜜脾 (minecraft:honeycomb) + 切制铜台阶 (minecraft:cut_copper_slab) → 涂蜡的切制铜台阶 (minecraft:waxed_cut_copper_slab) | ；静态启用 |
| `mekanism:combining/wax/waxed_cut_copper_stairs` | `mekanism:combining` | 蜜脾 (minecraft:honeycomb) + 切制铜楼梯 (minecraft:cut_copper_stairs) → 涂蜡的切制铜楼梯 (minecraft:waxed_cut_copper_stairs) | ；静态启用 |
| `mekanism:combining/wax/waxed_exposed_copper` | `mekanism:combining` | 蜜脾 (minecraft:honeycomb) + 斑驳的铜块 (minecraft:exposed_copper) → 涂蜡的斑驳铜块 (minecraft:waxed_exposed_copper) | ；静态启用 |
| `mekanism:combining/wax/waxed_exposed_cut_copper` | `mekanism:combining` | 蜜脾 (minecraft:honeycomb) + 斑驳的切制铜块 (minecraft:exposed_cut_copper) → 涂蜡的斑驳切制铜块 (minecraft:waxed_exposed_cut_copper) | ；静态启用 |
| `mekanism:combining/wax/waxed_exposed_cut_copper_slab` | `mekanism:combining` | 蜜脾 (minecraft:honeycomb) + 斑驳的切制铜台阶 (minecraft:exposed_cut_copper_slab) → 涂蜡的斑驳切制铜台阶 (minecraft:waxed_exposed_cut_copper_slab) | ；静态启用 |
| `mekanism:combining/wax/waxed_exposed_cut_copper_stairs` | `mekanism:combining` | 蜜脾 (minecraft:honeycomb) + 斑驳的切制铜楼梯 (minecraft:exposed_cut_copper_stairs) → 涂蜡的斑驳切制铜楼梯 (minecraft:waxed_exposed_cut_copper_stairs) | ；静态启用 |
| `mekanism:combining/wax/waxed_oxidized_copper` | `mekanism:combining` | 蜜脾 (minecraft:honeycomb) + 氧化的铜块 (minecraft:oxidized_copper) → 涂蜡的氧化铜块 (minecraft:waxed_oxidized_copper) | ；静态启用 |
| `mekanism:combining/wax/waxed_oxidized_cut_copper` | `mekanism:combining` | 蜜脾 (minecraft:honeycomb) + 氧化的切制铜块 (minecraft:oxidized_cut_copper) → 涂蜡的氧化切制铜块 (minecraft:waxed_oxidized_cut_copper) | ；静态启用 |
| `mekanism:combining/wax/waxed_oxidized_cut_copper_slab` | `mekanism:combining` | 蜜脾 (minecraft:honeycomb) + 氧化的切制铜台阶 (minecraft:oxidized_cut_copper_slab) → 涂蜡的氧化切制铜台阶 (minecraft:waxed_oxidized_cut_copper_slab) | ；静态启用 |
| `mekanism:combining/wax/waxed_oxidized_cut_copper_stairs` | `mekanism:combining` | 蜜脾 (minecraft:honeycomb) + 氧化的切制铜楼梯 (minecraft:oxidized_cut_copper_stairs) → 涂蜡的氧化切制铜楼梯 (minecraft:waxed_oxidized_cut_copper_stairs) | ；静态启用 |
| `mekanism:combining/wax/waxed_weathered_copper` | `mekanism:combining` | 蜜脾 (minecraft:honeycomb) + 锈蚀的铜块 (minecraft:weathered_copper) → 涂蜡的锈蚀铜块 (minecraft:waxed_weathered_copper) | ；静态启用 |
| `mekanism:combining/wax/waxed_weathered_cut_copper` | `mekanism:combining` | 蜜脾 (minecraft:honeycomb) + 锈蚀的切制铜块 (minecraft:weathered_cut_copper) → 涂蜡的锈蚀切制铜块 (minecraft:waxed_weathered_cut_copper) | ；静态启用 |
| `mekanism:combining/wax/waxed_weathered_cut_copper_slab` | `mekanism:combining` | 蜜脾 (minecraft:honeycomb) + 锈蚀的切制铜台阶 (minecraft:weathered_cut_copper_slab) → 涂蜡的锈蚀切制铜台阶 (minecraft:waxed_weathered_cut_copper_slab) | ；静态启用 |
| `mekanism:combining/wax/waxed_weathered_cut_copper_stairs` | `mekanism:combining` | 蜜脾 (minecraft:honeycomb) + 锈蚀的切制铜楼梯 (minecraft:weathered_cut_copper_stairs) → 涂蜡的锈蚀切制铜楼梯 (minecraft:waxed_weathered_cut_copper_stairs) | ；静态启用 |
| `mekanism:processing/copper/ore/deepslate_from_raw` | `mekanism:combining` | #forge:cobblestone/deepslate + 20 × #forge:raw_materials/copper → 深层铜矿石 (minecraft:deepslate_copper_ore) | ；静态启用 |
| `mekanism:processing/copper/ore/from_raw` | `mekanism:combining` | #forge:cobblestone/normal + 20 × #forge:raw_materials/copper → 铜矿石 (minecraft:copper_ore) | ；静态启用 |
| `mekanism:crushing/dewax/copper_block` | `mekanism:crushing` | 涂蜡的铜块 (minecraft:waxed_copper_block) → 铜块 (minecraft:copper_block) | ；静态启用 |
| `mekanism:crushing/dewax/cut_copper` | `mekanism:crushing` | 涂蜡的切制铜块 (minecraft:waxed_cut_copper) → 切制铜块 (minecraft:cut_copper) | ；静态启用 |
| `mekanism:crushing/dewax/cut_copper_slab` | `mekanism:crushing` | 涂蜡的切制铜台阶 (minecraft:waxed_cut_copper_slab) → 切制铜台阶 (minecraft:cut_copper_slab) | ；静态启用 |
| `mekanism:crushing/dewax/cut_copper_stairs` | `mekanism:crushing` | 涂蜡的切制铜楼梯 (minecraft:waxed_cut_copper_stairs) → 切制铜楼梯 (minecraft:cut_copper_stairs) | ；静态启用 |
| `mekanism:crushing/dewax/exposed_copper` | `mekanism:crushing` | 涂蜡的斑驳铜块 (minecraft:waxed_exposed_copper) → 斑驳的铜块 (minecraft:exposed_copper) | ；静态启用 |
| `mekanism:crushing/dewax/exposed_cut_copper` | `mekanism:crushing` | 涂蜡的斑驳切制铜块 (minecraft:waxed_exposed_cut_copper) → 斑驳的切制铜块 (minecraft:exposed_cut_copper) | ；静态启用 |
| `mekanism:crushing/dewax/exposed_cut_copper_slab` | `mekanism:crushing` | 涂蜡的斑驳切制铜台阶 (minecraft:waxed_exposed_cut_copper_slab) → 斑驳的切制铜台阶 (minecraft:exposed_cut_copper_slab) | ；静态启用 |
| `mekanism:crushing/dewax/exposed_cut_copper_stairs` | `mekanism:crushing` | 涂蜡的斑驳切制铜楼梯 (minecraft:waxed_exposed_cut_copper_stairs) → 斑驳的切制铜楼梯 (minecraft:exposed_cut_copper_stairs) | ；静态启用 |
| `mekanism:crushing/dewax/oxidized_copper` | `mekanism:crushing` | 涂蜡的氧化铜块 (minecraft:waxed_oxidized_copper) → 氧化的铜块 (minecraft:oxidized_copper) | ；静态启用 |
| `mekanism:crushing/dewax/oxidized_cut_copper` | `mekanism:crushing` | 涂蜡的氧化切制铜块 (minecraft:waxed_oxidized_cut_copper) → 氧化的切制铜块 (minecraft:oxidized_cut_copper) | ；静态启用 |
| `mekanism:crushing/dewax/oxidized_cut_copper_slab` | `mekanism:crushing` | 涂蜡的氧化切制铜台阶 (minecraft:waxed_oxidized_cut_copper_slab) → 氧化的切制铜台阶 (minecraft:oxidized_cut_copper_slab) | ；静态启用 |
| `mekanism:crushing/dewax/oxidized_cut_copper_stairs` | `mekanism:crushing` | 涂蜡的氧化切制铜楼梯 (minecraft:waxed_oxidized_cut_copper_stairs) → 氧化的切制铜楼梯 (minecraft:oxidized_cut_copper_stairs) | ；静态启用 |
| `mekanism:crushing/dewax/weathered_copper` | `mekanism:crushing` | 涂蜡的锈蚀铜块 (minecraft:waxed_weathered_copper) → 锈蚀的铜块 (minecraft:weathered_copper) | ；静态启用 |
| `mekanism:crushing/dewax/weathered_cut_copper` | `mekanism:crushing` | 涂蜡的锈蚀切制铜块 (minecraft:waxed_weathered_cut_copper) → 锈蚀的切制铜块 (minecraft:weathered_cut_copper) | ；静态启用 |
| `mekanism:crushing/dewax/weathered_cut_copper_slab` | `mekanism:crushing` | 涂蜡的锈蚀切制铜台阶 (minecraft:waxed_weathered_cut_copper_slab) → 锈蚀的切制铜台阶 (minecraft:weathered_cut_copper_slab) | ；静态启用 |
| `mekanism:crushing/dewax/weathered_cut_copper_stairs` | `mekanism:crushing` | 涂蜡的锈蚀切制铜楼梯 (minecraft:waxed_weathered_cut_copper_stairs) → 锈蚀的切制铜楼梯 (minecraft:weathered_cut_copper_stairs) | ；静态启用 |
| `mekanism:processing/bronze/dust/from_ingot` | `mekanism:crushing` | #forge:ingots/bronze → 青铜粉 (mekanism:dust_bronze) | ；静态启用 |
| `mekanism:processing/copper/dirty_dust/from_clump` | `mekanism:crushing` | #mekanism:clumps/copper → 污浊铜粉 (mekanism:dirty_dust_copper) | ；静态启用 |
| `mekanism:processing/copper/dust/from_ingot` | `mekanism:crushing` | #forge:ingots/copper → 铜粉 (mekanism:dust_copper) | ；静态启用 |
| `mekanism:processing/copper/crystal/from_slurry` | `mekanism:crystallizing` | 200 mB 纯净铜浆液 (mekanism:clean_copper) → 铜晶体 (mekanism:crystal_copper) | ；静态启用 |
| `mekanism:processing/copper/slurry/dirty/from_ore` | `mekanism:dissolution` | 1 mB 液态硫酸 (mekanism:sulfuric_acid) + #forge:ores/copper → 1000 mB 污浊铜浆液 (mekanism:dirty_copper) | ；静态启用 |
| `mekanism:processing/copper/slurry/dirty/from_raw_block` | `mekanism:dissolution` | 2 mB 液态硫酸 (mekanism:sulfuric_acid) + #forge:storage_blocks/raw_copper → 6000 mB 污浊铜浆液 (mekanism:dirty_copper) | ；静态启用 |
| `mekanism:processing/copper/slurry/dirty/from_raw_ore` | `mekanism:dissolution` | 1 mB 液态硫酸 (mekanism:sulfuric_acid) + 3 × #forge:raw_materials/copper → 2000 mB 污浊铜浆液 (mekanism:dirty_copper) | ；静态启用 |
| `mekanism:enriching/deoxidizing/copper_block` | `mekanism:enriching` | 斑驳的铜块 (minecraft:exposed_copper) → 铜块 (minecraft:copper_block) | ；静态启用 |
| `mekanism:enriching/deoxidizing/cut_copper` | `mekanism:enriching` | 斑驳的切制铜块 (minecraft:exposed_cut_copper) → 切制铜块 (minecraft:cut_copper) | ；静态启用 |
| `mekanism:enriching/deoxidizing/cut_copper_slab` | `mekanism:enriching` | 斑驳的切制铜台阶 (minecraft:exposed_cut_copper_slab) → 切制铜台阶 (minecraft:cut_copper_slab) | ；静态启用 |
| `mekanism:enriching/deoxidizing/cut_copper_stairs` | `mekanism:enriching` | 斑驳的切制铜楼梯 (minecraft:exposed_cut_copper_stairs) → 切制铜楼梯 (minecraft:cut_copper_stairs) | ；静态启用 |
| `mekanism:enriching/deoxidizing/exposed_copper` | `mekanism:enriching` | 锈蚀的铜块 (minecraft:weathered_copper) → 斑驳的铜块 (minecraft:exposed_copper) | ；静态启用 |
| `mekanism:enriching/deoxidizing/exposed_cut_copper` | `mekanism:enriching` | 锈蚀的切制铜块 (minecraft:weathered_cut_copper) → 斑驳的切制铜块 (minecraft:exposed_cut_copper) | ；静态启用 |
| `mekanism:enriching/deoxidizing/exposed_cut_copper_slab` | `mekanism:enriching` | 锈蚀的切制铜台阶 (minecraft:weathered_cut_copper_slab) → 斑驳的切制铜台阶 (minecraft:exposed_cut_copper_slab) | ；静态启用 |
| `mekanism:enriching/deoxidizing/exposed_cut_copper_stairs` | `mekanism:enriching` | 锈蚀的切制铜楼梯 (minecraft:weathered_cut_copper_stairs) → 斑驳的切制铜楼梯 (minecraft:exposed_cut_copper_stairs) | ；静态启用 |
| `mekanism:enriching/deoxidizing/weathered_copper` | `mekanism:enriching` | 氧化的铜块 (minecraft:oxidized_copper) → 锈蚀的铜块 (minecraft:weathered_copper) | ；静态启用 |
| `mekanism:enriching/deoxidizing/weathered_cut_copper` | `mekanism:enriching` | 氧化的切制铜块 (minecraft:oxidized_cut_copper) → 锈蚀的切制铜块 (minecraft:weathered_cut_copper) | ；静态启用 |
| `mekanism:enriching/deoxidizing/weathered_cut_copper_slab` | `mekanism:enriching` | 氧化的切制铜台阶 (minecraft:oxidized_cut_copper_slab) → 锈蚀的切制铜台阶 (minecraft:weathered_cut_copper_slab) | ；静态启用 |
| `mekanism:enriching/deoxidizing/weathered_cut_copper_stairs` | `mekanism:enriching` | 氧化的切制铜楼梯 (minecraft:oxidized_cut_copper_stairs) → 锈蚀的切制铜楼梯 (minecraft:weathered_cut_copper_stairs) | ；静态启用 |
| `mekanism:processing/copper/dust/from_dirty_dust` | `mekanism:enriching` | #mekanism:dirty_dusts/copper → 铜粉 (mekanism:dust_copper) | ；静态启用 |
| `mekanism:processing/copper/dust/from_ore` | `mekanism:enriching` | #forge:ores/copper → 2 × 铜粉 (mekanism:dust_copper) | ；静态启用 |
| `mekanism:processing/copper/dust/from_raw_block` | `mekanism:enriching` | #forge:storage_blocks/raw_copper → 12 × 铜粉 (mekanism:dust_copper) | ；静态启用 |
| `mekanism:processing/copper/dust/from_raw_ore` | `mekanism:enriching` | 3 × #forge:raw_materials/copper → 4 × 铜粉 (mekanism:dust_copper) | ；静态启用 |
| `mekanism:injecting/oxidizing/exposed_copper` | `mekanism:injecting` | 1 mB 液态氧 (mekanism:oxygen) + 铜块 (minecraft:copper_block) → 斑驳的铜块 (minecraft:exposed_copper) | ；静态启用 |
| `mekanism:injecting/oxidizing/exposed_cut_copper` | `mekanism:injecting` | 1 mB 液态氧 (mekanism:oxygen) + 切制铜块 (minecraft:cut_copper) → 斑驳的切制铜块 (minecraft:exposed_cut_copper) | ；静态启用 |
| `mekanism:injecting/oxidizing/exposed_cut_copper_slab` | `mekanism:injecting` | 1 mB 液态氧 (mekanism:oxygen) + 切制铜台阶 (minecraft:cut_copper_slab) → 斑驳的切制铜台阶 (minecraft:exposed_cut_copper_slab) | ；静态启用 |
| `mekanism:injecting/oxidizing/exposed_cut_copper_stairs` | `mekanism:injecting` | 1 mB 液态氧 (mekanism:oxygen) + 切制铜楼梯 (minecraft:cut_copper_stairs) → 斑驳的切制铜楼梯 (minecraft:exposed_cut_copper_stairs) | ；静态启用 |
| `mekanism:injecting/oxidizing/oxidized_copper` | `mekanism:injecting` | 1 mB 液态氧 (mekanism:oxygen) + 锈蚀的铜块 (minecraft:weathered_copper) → 氧化的铜块 (minecraft:oxidized_copper) | ；静态启用 |
| `mekanism:injecting/oxidizing/oxidized_cut_copper` | `mekanism:injecting` | 1 mB 液态氧 (mekanism:oxygen) + 锈蚀的切制铜块 (minecraft:weathered_cut_copper) → 氧化的切制铜块 (minecraft:oxidized_cut_copper) | ；静态启用 |
| `mekanism:injecting/oxidizing/oxidized_cut_copper_slab` | `mekanism:injecting` | 1 mB 液态氧 (mekanism:oxygen) + 锈蚀的切制铜台阶 (minecraft:weathered_cut_copper_slab) → 氧化的切制铜台阶 (minecraft:oxidized_cut_copper_slab) | ；静态启用 |
| `mekanism:injecting/oxidizing/oxidized_cut_copper_stairs` | `mekanism:injecting` | 1 mB 液态氧 (mekanism:oxygen) + 锈蚀的切制铜楼梯 (minecraft:weathered_cut_copper_stairs) → 氧化的切制铜楼梯 (minecraft:oxidized_cut_copper_stairs) | ；静态启用 |
| `mekanism:injecting/oxidizing/weathered_copper` | `mekanism:injecting` | 1 mB 液态氧 (mekanism:oxygen) + 斑驳的铜块 (minecraft:exposed_copper) → 锈蚀的铜块 (minecraft:weathered_copper) | ；静态启用 |
| `mekanism:injecting/oxidizing/weathered_cut_copper` | `mekanism:injecting` | 1 mB 液态氧 (mekanism:oxygen) + 斑驳的切制铜块 (minecraft:exposed_cut_copper) → 锈蚀的切制铜块 (minecraft:weathered_cut_copper) | ；静态启用 |
| `mekanism:injecting/oxidizing/weathered_cut_copper_slab` | `mekanism:injecting` | 1 mB 液态氧 (mekanism:oxygen) + 斑驳的切制铜台阶 (minecraft:exposed_cut_copper_slab) → 锈蚀的切制铜台阶 (minecraft:weathered_cut_copper_slab) | ；静态启用 |
| `mekanism:injecting/oxidizing/weathered_cut_copper_stairs` | `mekanism:injecting` | 1 mB 液态氧 (mekanism:oxygen) + 斑驳的切制铜楼梯 (minecraft:exposed_cut_copper_stairs) → 锈蚀的切制铜楼梯 (minecraft:weathered_cut_copper_stairs) | ；静态启用 |
| `mekanism:processing/copper/shard/from_crystal` | `mekanism:injecting` | 1 mB 液态氯化氢 (mekanism:hydrogen_chloride) + #mekanism:crystals/copper → 铜碎片 (mekanism:shard_copper) | ；静态启用 |
| `mekanism:processing/copper/shard/from_ore` | `mekanism:injecting` | 1 mB 液态氯化氢 (mekanism:hydrogen_chloride) + #forge:ores/copper → 4 × 铜碎片 (mekanism:shard_copper) | ；静态启用 |
| `mekanism:processing/copper/shard/from_raw_block` | `mekanism:injecting` | 2 mB 液态氯化氢 (mekanism:hydrogen_chloride) + #forge:storage_blocks/raw_copper → 24 × 铜碎片 (mekanism:shard_copper) | ；静态启用 |
| `mekanism:processing/copper/shard/from_raw_ore` | `mekanism:injecting` | 1 mB 液态氯化氢 (mekanism:hydrogen_chloride) + 3 × #forge:raw_materials/copper → 8 × 铜碎片 (mekanism:shard_copper) | ；静态启用 |
| `mekanism:flamethrower` | `mekanism:mek_data` | 4 × #forge:ingots/tin + 基础化学品储罐 (mekanism:basic_chemical_tank) + 打火石 (minecraft:flint_and_steel) + 2 × #forge:ingots/bronze + #forge:circuits/advanced → 火焰喷射器 (mekanism:flamethrower) | ；静态启用 |
| `mekanism:free_runners_armored` | `mekanism:mek_data` | #forge:storage_blocks/steel + 2 × #forge:dusts/diamond + 自由助跑器 (mekanism:free_runners) + 2 × #forge:ingots/bronze → 装甲自由助跑器 (mekanism:free_runners_armored) | ；静态启用 |
| `mekanism:jetpack_armored` | `mekanism:mek_data` | 2 × #forge:dusts/diamond + 2 × #forge:ingots/bronze + #forge:storage_blocks/steel + 喷气背包 (mekanism:jetpack) → 装甲喷气背包 (mekanism:jetpack_armored) | ；静态启用 |
| `mekanism:processing/bronze/dust/from_infusing` | `mekanism:metallurgic_infusing` | 10 mB #mekanism:tin + 3 × #forge:dusts/copper → 4 × 青铜粉 (mekanism:dust_bronze) | ；静态启用 |
| `mekanism:processing/bronze/ingot/from_infusing` | `mekanism:metallurgic_infusing` | 10 mB #mekanism:tin + 3 × #forge:ingots/copper → 4 × 青铜锭 (mekanism:ingot_bronze) | ；静态启用 |
| `mekanism:processing/copper/slurry/clean` | `mekanism:washing` | 5 mB #minecraft:water + 1 mB 污浊铜浆液 (mekanism:dirty_copper) → 1 mB 纯净铜浆液 (mekanism:clean_copper) | ；静态启用 |
| `mekanism:processing/bronze/ingot/from_dust_blasting` | `minecraft:blasting` | #forge:dusts/bronze → 青铜锭 (mekanism:ingot_bronze) | {"cookingtime":100,"experience":0.35}；静态启用 |
| `mekanism:processing/copper/ingot/from_dust_blasting` | `minecraft:blasting` | #forge:dusts/copper → 铜锭 (minecraft:copper_ingot) | {"cookingtime":100,"experience":0.35}；静态启用 |
| `mekanism:module_base` | `minecraft:crafting_shaped` | 4 × #forge:nuggets/bronze + 4 × #forge:ingots/tin + 高密度聚乙烯片 (mekanism:hdpe_sheet) → 2 × 模块基板 (mekanism:module_base) | ；静态启用 |
| `mekanism:processing/bronze/ingot/from_nuggets` | `minecraft:crafting_shaped` | 8 × #forge:nuggets/bronze + 青铜粒 (mekanism:nugget_bronze) → 青铜锭 (mekanism:ingot_bronze) | ；静态启用 |
| `mekanism:solar_neutron_activator` | `minecraft:crafting_shaped` | 2 × #mekanism:alloys/reinforced + 高密度聚乙烯片 (mekanism:hdpe_sheet) + 2 × #forge:circuits/elite + 钢质机壳 (mekanism:steel_casing) + 3 × #forge:ingots/bronze → 太阳能中子活化器 (mekanism:solar_neutron_activator) | ；静态启用 |
| `mekanism:storage_blocks/bronze` | `minecraft:crafting_shaped` | 8 × #forge:ingots/bronze + 青铜锭 (mekanism:ingot_bronze) → 青铜块 (mekanism:block_bronze) | ；静态启用 |
| `mekanism:supercharged_coil` | `minecraft:crafting_shaped` | 3 × #forge:ingots/copper + 2 × #forge:circuits/ultimate + 激光 (mekanism:laser) + 3 × 钋球 (mekanism:pellet_polonium) → 增压线圈 (mekanism:supercharged_coil) | ；静态启用 |
| `mekanism:superheating_element` | `minecraft:crafting_shaped` | 4 × #mekanism:alloys/basic + 4 × #forge:ingots/copper + 钢质机壳 (mekanism:steel_casing) → 过热元件 (mekanism:superheating_element) | ；静态启用 |
| `mekanism:thermal_evaporation/block` | `minecraft:crafting_shaped` | 4 × #forge:ingots/steel + #forge:ingots/copper → 4 × 热力蒸馏方块 (mekanism:thermal_evaporation_block) | ；静态启用 |
| `mekanism:transmitter/thermodynamic_conductor/basic` | `minecraft:crafting_shaped` | 2 × #forge:ingots/steel + #forge:ingots/copper → 8 × 基础热导线缆 (mekanism:basic_thermodynamic_conductor) | ；静态启用 |
| `mekanism:nuggets/bronze` | `minecraft:crafting_shapeless` | #forge:ingots/bronze → 9 × 青铜粒 (mekanism:nugget_bronze) | ；静态启用 |
| `mekanism:processing/bronze/ingot/from_block` | `minecraft:crafting_shapeless` | #forge:storage_blocks/bronze → 9 × 青铜锭 (mekanism:ingot_bronze) | ；静态启用 |
| `mekanism:processing/bronze/ingot/from_dust_smelting` | `minecraft:smelting` | #forge:dusts/bronze → 青铜锭 (mekanism:ingot_bronze) | {"cookingtime":200,"experience":0.35}；静态启用 |
| `mekanism:processing/copper/ingot/from_dust_smelting` | `minecraft:smelting` | #forge:dusts/copper → 铜锭 (minecraft:copper_ingot) | {"cookingtime":200,"experience":0.35}；静态启用 |

### mekanismdelight

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `mekanismdelight:cutting/robit` | `farmersdelight:cutting` | 机器人 (mekanism:robit) → 2 × 原机器仆人 (mekanismdelight:formerly_robit) | {"tool":{"tag":"forge:tools/knives"}}；静态启用 |
| `mekanismdelight:bronze_knife` | `minecraft:crafting_shaped` | #forge:ingots/bronze + 木棍 (minecraft:stick) → 青铜刀 (mekanismdelight:bronze_knife) | ；静态启用 |

### minecraft

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `minecraft:copper_ingot_from_blasting_copper_ore` | `minecraft:blasting` | 铜矿石 (minecraft:copper_ore) → 铜锭 (minecraft:copper_ingot) | {"cookingtime":100,"experience":0.7}；静态启用 |
| `minecraft:copper_ingot_from_blasting_deepslate_copper_ore` | `minecraft:blasting` | 深层铜矿石 (minecraft:deepslate_copper_ore) → 铜锭 (minecraft:copper_ingot) | {"cookingtime":100,"experience":0.7}；静态启用 |
| `minecraft:copper_ingot_from_blasting_raw_copper` | `minecraft:blasting` | 氧化铜 (minecraft:raw_copper) → 铜锭 (minecraft:copper_ingot) | {"cookingtime":100,"experience":0.7}；静态启用 |
| `minecraft:brush` | `minecraft:crafting_shaped` | 羽毛 (minecraft:feather) + #forge:ingots/copper + #forge:rods/wooden → 刷子 (minecraft:brush) | ；静态启用 |
| `minecraft:copper_block` | `minecraft:crafting_shaped` | 9 × 铜锭 (minecraft:copper_ingot) → 铜块 (minecraft:copper_block) | ；静态启用 |
| `minecraft:cut_copper` | `minecraft:crafting_shaped` | 4 × 铜块 (minecraft:copper_block) → 4 × 切制铜块 (minecraft:cut_copper) | ；静态启用 |
| `minecraft:cut_copper_slab` | `minecraft:crafting_shaped` | 3 × 切制铜块 (minecraft:cut_copper) → 6 × 切制铜台阶 (minecraft:cut_copper_slab) | ；静态启用 |
| `minecraft:cut_copper_stairs` | `minecraft:crafting_shaped` | 6 × 切制铜块 (minecraft:cut_copper) → 4 × 切制铜楼梯 (minecraft:cut_copper_stairs) | ；静态启用 |
| `minecraft:exposed_cut_copper` | `minecraft:crafting_shaped` | 4 × 斑驳的铜块 (minecraft:exposed_copper) → 4 × 斑驳的切制铜块 (minecraft:exposed_cut_copper) | ；静态启用 |
| `minecraft:exposed_cut_copper_slab` | `minecraft:crafting_shaped` | 3 × 斑驳的切制铜块 (minecraft:exposed_cut_copper) → 6 × 斑驳的切制铜台阶 (minecraft:exposed_cut_copper_slab) | ；静态启用 |
| `minecraft:exposed_cut_copper_stairs` | `minecraft:crafting_shaped` | 6 × 斑驳的切制铜块 (minecraft:exposed_cut_copper) → 4 × 斑驳的切制铜楼梯 (minecraft:exposed_cut_copper_stairs) | ；静态启用 |
| `minecraft:lightning_rod` | `minecraft:crafting_shaped` | 3 × #forge:ingots/copper → 避雷针 (minecraft:lightning_rod) | ；静态启用 |
| `minecraft:oxidized_cut_copper` | `minecraft:crafting_shaped` | 4 × 氧化的铜块 (minecraft:oxidized_copper) → 4 × 氧化的切制铜块 (minecraft:oxidized_cut_copper) | ；静态启用 |
| `minecraft:oxidized_cut_copper_slab` | `minecraft:crafting_shaped` | 3 × 氧化的切制铜块 (minecraft:oxidized_cut_copper) → 6 × 氧化的切制铜台阶 (minecraft:oxidized_cut_copper_slab) | ；静态启用 |
| `minecraft:oxidized_cut_copper_stairs` | `minecraft:crafting_shaped` | 6 × 氧化的切制铜块 (minecraft:oxidized_cut_copper) → 4 × 氧化的切制铜楼梯 (minecraft:oxidized_cut_copper_stairs) | ；静态启用 |
| `minecraft:raw_copper_block` | `minecraft:crafting_shaped` | 9 × 氧化铜 (minecraft:raw_copper) → 氧化铜块 (minecraft:raw_copper_block) | ；静态启用 |
| `minecraft:spyglass` | `minecraft:crafting_shaped` | #forge:gems/amethyst + 2 × #forge:ingots/copper → 望远镜 (minecraft:spyglass) | ；静态启用 |
| `minecraft:waxed_cut_copper` | `minecraft:crafting_shaped` | 4 × 涂蜡的铜块 (minecraft:waxed_copper_block) → 4 × 涂蜡的切制铜块 (minecraft:waxed_cut_copper) | ；静态启用 |
| `minecraft:waxed_cut_copper_slab` | `minecraft:crafting_shaped` | 3 × 涂蜡的切制铜块 (minecraft:waxed_cut_copper) → 6 × 涂蜡的切制铜台阶 (minecraft:waxed_cut_copper_slab) | ；静态启用 |
| `minecraft:waxed_cut_copper_stairs` | `minecraft:crafting_shaped` | 6 × 涂蜡的切制铜块 (minecraft:waxed_cut_copper) → 4 × 涂蜡的切制铜楼梯 (minecraft:waxed_cut_copper_stairs) | ；静态启用 |
| `minecraft:waxed_exposed_cut_copper` | `minecraft:crafting_shaped` | 4 × 涂蜡的斑驳铜块 (minecraft:waxed_exposed_copper) → 4 × 涂蜡的斑驳切制铜块 (minecraft:waxed_exposed_cut_copper) | ；静态启用 |
| `minecraft:waxed_exposed_cut_copper_slab` | `minecraft:crafting_shaped` | 3 × 涂蜡的斑驳切制铜块 (minecraft:waxed_exposed_cut_copper) → 6 × 涂蜡的斑驳切制铜台阶 (minecraft:waxed_exposed_cut_copper_slab) | ；静态启用 |
| `minecraft:waxed_exposed_cut_copper_stairs` | `minecraft:crafting_shaped` | 6 × 涂蜡的斑驳切制铜块 (minecraft:waxed_exposed_cut_copper) → 4 × 涂蜡的斑驳切制铜楼梯 (minecraft:waxed_exposed_cut_copper_stairs) | ；静态启用 |
| `minecraft:waxed_oxidized_cut_copper` | `minecraft:crafting_shaped` | 4 × 涂蜡的氧化铜块 (minecraft:waxed_oxidized_copper) → 4 × 涂蜡的氧化切制铜块 (minecraft:waxed_oxidized_cut_copper) | ；静态启用 |
| `minecraft:waxed_oxidized_cut_copper_slab` | `minecraft:crafting_shaped` | 3 × 涂蜡的氧化切制铜块 (minecraft:waxed_oxidized_cut_copper) → 6 × 涂蜡的氧化切制铜台阶 (minecraft:waxed_oxidized_cut_copper_slab) | ；静态启用 |
| `minecraft:waxed_oxidized_cut_copper_stairs` | `minecraft:crafting_shaped` | 6 × 涂蜡的氧化切制铜块 (minecraft:waxed_oxidized_cut_copper) → 4 × 涂蜡的氧化切制铜楼梯 (minecraft:waxed_oxidized_cut_copper_stairs) | ；静态启用 |
| `minecraft:waxed_weathered_cut_copper` | `minecraft:crafting_shaped` | 4 × 涂蜡的锈蚀铜块 (minecraft:waxed_weathered_copper) → 4 × 涂蜡的锈蚀切制铜块 (minecraft:waxed_weathered_cut_copper) | ；静态启用 |
| `minecraft:waxed_weathered_cut_copper_slab` | `minecraft:crafting_shaped` | 3 × 涂蜡的锈蚀切制铜块 (minecraft:waxed_weathered_cut_copper) → 6 × 涂蜡的锈蚀切制铜台阶 (minecraft:waxed_weathered_cut_copper_slab) | ；静态启用 |
| `minecraft:waxed_weathered_cut_copper_stairs` | `minecraft:crafting_shaped` | 6 × 涂蜡的锈蚀切制铜块 (minecraft:waxed_weathered_cut_copper) → 4 × 涂蜡的锈蚀切制铜楼梯 (minecraft:waxed_weathered_cut_copper_stairs) | ；静态启用 |
| `minecraft:weathered_cut_copper` | `minecraft:crafting_shaped` | 4 × 锈蚀的铜块 (minecraft:weathered_copper) → 4 × 锈蚀的切制铜块 (minecraft:weathered_cut_copper) | ；静态启用 |
| `minecraft:weathered_cut_copper_slab` | `minecraft:crafting_shaped` | 3 × 锈蚀的切制铜块 (minecraft:weathered_cut_copper) → 6 × 锈蚀的切制铜台阶 (minecraft:weathered_cut_copper_slab) | ；静态启用 |
| `minecraft:weathered_cut_copper_stairs` | `minecraft:crafting_shaped` | 6 × 锈蚀的切制铜块 (minecraft:weathered_cut_copper) → 4 × 锈蚀的切制铜楼梯 (minecraft:weathered_cut_copper_stairs) | ；静态启用 |
| `minecraft:copper_ingot` | `minecraft:crafting_shapeless` | 铜块 (minecraft:copper_block) → 9 × 铜锭 (minecraft:copper_ingot) | ；静态启用 |
| `minecraft:copper_ingot_from_waxed_copper_block` | `minecraft:crafting_shapeless` | 涂蜡的铜块 (minecraft:waxed_copper_block) → 9 × 铜锭 (minecraft:copper_ingot) | ；静态启用 |
| `minecraft:raw_copper` | `minecraft:crafting_shapeless` | 氧化铜块 (minecraft:raw_copper_block) → 9 × 氧化铜 (minecraft:raw_copper) | ；静态启用 |
| `minecraft:waxed_copper_block_from_honeycomb` | `minecraft:crafting_shapeless` | 铜块 (minecraft:copper_block) + 蜜脾 (minecraft:honeycomb) → 涂蜡的铜块 (minecraft:waxed_copper_block) | ；静态启用 |
| `minecraft:waxed_cut_copper_from_honeycomb` | `minecraft:crafting_shapeless` | 切制铜块 (minecraft:cut_copper) + 蜜脾 (minecraft:honeycomb) → 涂蜡的切制铜块 (minecraft:waxed_cut_copper) | ；静态启用 |
| `minecraft:waxed_cut_copper_slab_from_honeycomb` | `minecraft:crafting_shapeless` | 切制铜台阶 (minecraft:cut_copper_slab) + 蜜脾 (minecraft:honeycomb) → 涂蜡的切制铜台阶 (minecraft:waxed_cut_copper_slab) | ；静态启用 |
| `minecraft:waxed_cut_copper_stairs_from_honeycomb` | `minecraft:crafting_shapeless` | 切制铜楼梯 (minecraft:cut_copper_stairs) + 蜜脾 (minecraft:honeycomb) → 涂蜡的切制铜楼梯 (minecraft:waxed_cut_copper_stairs) | ；静态启用 |
| `minecraft:waxed_exposed_copper_from_honeycomb` | `minecraft:crafting_shapeless` | 斑驳的铜块 (minecraft:exposed_copper) + 蜜脾 (minecraft:honeycomb) → 涂蜡的斑驳铜块 (minecraft:waxed_exposed_copper) | ；静态启用 |
| `minecraft:waxed_exposed_cut_copper_from_honeycomb` | `minecraft:crafting_shapeless` | 斑驳的切制铜块 (minecraft:exposed_cut_copper) + 蜜脾 (minecraft:honeycomb) → 涂蜡的斑驳切制铜块 (minecraft:waxed_exposed_cut_copper) | ；静态启用 |
| `minecraft:waxed_exposed_cut_copper_slab_from_honeycomb` | `minecraft:crafting_shapeless` | 斑驳的切制铜台阶 (minecraft:exposed_cut_copper_slab) + 蜜脾 (minecraft:honeycomb) → 涂蜡的斑驳切制铜台阶 (minecraft:waxed_exposed_cut_copper_slab) | ；静态启用 |
| `minecraft:waxed_exposed_cut_copper_stairs_from_honeycomb` | `minecraft:crafting_shapeless` | 斑驳的切制铜楼梯 (minecraft:exposed_cut_copper_stairs) + 蜜脾 (minecraft:honeycomb) → 涂蜡的斑驳切制铜楼梯 (minecraft:waxed_exposed_cut_copper_stairs) | ；静态启用 |
| `minecraft:waxed_oxidized_copper_from_honeycomb` | `minecraft:crafting_shapeless` | 氧化的铜块 (minecraft:oxidized_copper) + 蜜脾 (minecraft:honeycomb) → 涂蜡的氧化铜块 (minecraft:waxed_oxidized_copper) | ；静态启用 |
| `minecraft:waxed_oxidized_cut_copper_from_honeycomb` | `minecraft:crafting_shapeless` | 氧化的切制铜块 (minecraft:oxidized_cut_copper) + 蜜脾 (minecraft:honeycomb) → 涂蜡的氧化切制铜块 (minecraft:waxed_oxidized_cut_copper) | ；静态启用 |
| `minecraft:waxed_oxidized_cut_copper_slab_from_honeycomb` | `minecraft:crafting_shapeless` | 氧化的切制铜台阶 (minecraft:oxidized_cut_copper_slab) + 蜜脾 (minecraft:honeycomb) → 涂蜡的氧化切制铜台阶 (minecraft:waxed_oxidized_cut_copper_slab) | ；静态启用 |
| `minecraft:waxed_oxidized_cut_copper_stairs_from_honeycomb` | `minecraft:crafting_shapeless` | 氧化的切制铜楼梯 (minecraft:oxidized_cut_copper_stairs) + 蜜脾 (minecraft:honeycomb) → 涂蜡的氧化切制铜楼梯 (minecraft:waxed_oxidized_cut_copper_stairs) | ；静态启用 |
| `minecraft:waxed_weathered_copper_from_honeycomb` | `minecraft:crafting_shapeless` | 锈蚀的铜块 (minecraft:weathered_copper) + 蜜脾 (minecraft:honeycomb) → 涂蜡的锈蚀铜块 (minecraft:waxed_weathered_copper) | ；静态启用 |
| `minecraft:waxed_weathered_cut_copper_from_honeycomb` | `minecraft:crafting_shapeless` | 锈蚀的切制铜块 (minecraft:weathered_cut_copper) + 蜜脾 (minecraft:honeycomb) → 涂蜡的锈蚀切制铜块 (minecraft:waxed_weathered_cut_copper) | ；静态启用 |
| `minecraft:waxed_weathered_cut_copper_slab_from_honeycomb` | `minecraft:crafting_shapeless` | 锈蚀的切制铜台阶 (minecraft:weathered_cut_copper_slab) + 蜜脾 (minecraft:honeycomb) → 涂蜡的锈蚀切制铜台阶 (minecraft:waxed_weathered_cut_copper_slab) | ；静态启用 |
| `minecraft:waxed_weathered_cut_copper_stairs_from_honeycomb` | `minecraft:crafting_shapeless` | 锈蚀的切制铜楼梯 (minecraft:weathered_cut_copper_stairs) + 蜜脾 (minecraft:honeycomb) → 涂蜡的锈蚀切制铜楼梯 (minecraft:waxed_weathered_cut_copper_stairs) | ；静态启用 |
| `minecraft:copper_ingot_from_smelting_copper_ore` | `minecraft:smelting` | 铜矿石 (minecraft:copper_ore) → 铜锭 (minecraft:copper_ingot) | {"cookingtime":200,"experience":0.7}；静态启用 |
| `minecraft:copper_ingot_from_smelting_deepslate_copper_ore` | `minecraft:smelting` | 深层铜矿石 (minecraft:deepslate_copper_ore) → 铜锭 (minecraft:copper_ingot) | {"cookingtime":200,"experience":0.7}；静态启用 |
| `minecraft:copper_ingot_from_smelting_raw_copper` | `minecraft:smelting` | 氧化铜 (minecraft:raw_copper) → 铜锭 (minecraft:copper_ingot) | {"cookingtime":200,"experience":0.7}；静态启用 |
| `minecraft:coast_armor_trim_smithing_template_smithing_trim` | `minecraft:smithing_trim` | #minecraft:trim_materials + #minecraft:trimmable_armor + 锻造模板 (minecraft:coast_armor_trim_smithing_template) → 保留原装备并应用模板纹饰（可选铜锭饰材） | ；静态启用 |
| `minecraft:dune_armor_trim_smithing_template_smithing_trim` | `minecraft:smithing_trim` | #minecraft:trim_materials + #minecraft:trimmable_armor + 锻造模板 (minecraft:dune_armor_trim_smithing_template) → 保留原装备并应用模板纹饰（可选铜锭饰材） | ；静态启用 |
| `minecraft:eye_armor_trim_smithing_template_smithing_trim` | `minecraft:smithing_trim` | #minecraft:trim_materials + #minecraft:trimmable_armor + 锻造模板 (minecraft:eye_armor_trim_smithing_template) → 保留原装备并应用模板纹饰（可选铜锭饰材） | ；静态启用 |
| `minecraft:host_armor_trim_smithing_template_smithing_trim` | `minecraft:smithing_trim` | #minecraft:trim_materials + #minecraft:trimmable_armor + 锻造模板 (minecraft:host_armor_trim_smithing_template) → 保留原装备并应用模板纹饰（可选铜锭饰材） | ；静态启用 |
| `minecraft:raiser_armor_trim_smithing_template_smithing_trim` | `minecraft:smithing_trim` | #minecraft:trim_materials + #minecraft:trimmable_armor + 锻造模板 (minecraft:raiser_armor_trim_smithing_template) → 保留原装备并应用模板纹饰（可选铜锭饰材） | ；静态启用 |
| `minecraft:rib_armor_trim_smithing_template_smithing_trim` | `minecraft:smithing_trim` | #minecraft:trim_materials + #minecraft:trimmable_armor + 锻造模板 (minecraft:rib_armor_trim_smithing_template) → 保留原装备并应用模板纹饰（可选铜锭饰材） | ；静态启用 |
| `minecraft:sentry_armor_trim_smithing_template_smithing_trim` | `minecraft:smithing_trim` | #minecraft:trim_materials + #minecraft:trimmable_armor + 锻造模板 (minecraft:sentry_armor_trim_smithing_template) → 保留原装备并应用模板纹饰（可选铜锭饰材） | ；静态启用 |
| `minecraft:shaper_armor_trim_smithing_template_smithing_trim` | `minecraft:smithing_trim` | #minecraft:trim_materials + #minecraft:trimmable_armor + 锻造模板 (minecraft:shaper_armor_trim_smithing_template) → 保留原装备并应用模板纹饰（可选铜锭饰材） | ；静态启用 |
| `minecraft:silence_armor_trim_smithing_template_smithing_trim` | `minecraft:smithing_trim` | #minecraft:trim_materials + #minecraft:trimmable_armor + 锻造模板 (minecraft:silence_armor_trim_smithing_template) → 保留原装备并应用模板纹饰（可选铜锭饰材） | ；静态启用 |
| `minecraft:snout_armor_trim_smithing_template_smithing_trim` | `minecraft:smithing_trim` | #minecraft:trim_materials + #minecraft:trimmable_armor + 锻造模板 (minecraft:snout_armor_trim_smithing_template) → 保留原装备并应用模板纹饰（可选铜锭饰材） | ；静态启用 |
| `minecraft:spire_armor_trim_smithing_template_smithing_trim` | `minecraft:smithing_trim` | #minecraft:trim_materials + #minecraft:trimmable_armor + 锻造模板 (minecraft:spire_armor_trim_smithing_template) → 保留原装备并应用模板纹饰（可选铜锭饰材） | ；静态启用 |
| `minecraft:tide_armor_trim_smithing_template_smithing_trim` | `minecraft:smithing_trim` | #minecraft:trim_materials + #minecraft:trimmable_armor + 锻造模板 (minecraft:tide_armor_trim_smithing_template) → 保留原装备并应用模板纹饰（可选铜锭饰材） | ；静态启用 |
| `minecraft:vex_armor_trim_smithing_template_smithing_trim` | `minecraft:smithing_trim` | #minecraft:trim_materials + #minecraft:trimmable_armor + 锻造模板 (minecraft:vex_armor_trim_smithing_template) → 保留原装备并应用模板纹饰（可选铜锭饰材） | ；静态启用 |
| `minecraft:ward_armor_trim_smithing_template_smithing_trim` | `minecraft:smithing_trim` | #minecraft:trim_materials + #minecraft:trimmable_armor + 锻造模板 (minecraft:ward_armor_trim_smithing_template) → 保留原装备并应用模板纹饰（可选铜锭饰材） | ；静态启用 |
| `minecraft:wayfinder_armor_trim_smithing_template_smithing_trim` | `minecraft:smithing_trim` | #minecraft:trim_materials + #minecraft:trimmable_armor + 锻造模板 (minecraft:wayfinder_armor_trim_smithing_template) → 保留原装备并应用模板纹饰（可选铜锭饰材） | ；静态启用 |
| `minecraft:wild_armor_trim_smithing_template_smithing_trim` | `minecraft:smithing_trim` | #minecraft:trim_materials + #minecraft:trimmable_armor + 锻造模板 (minecraft:wild_armor_trim_smithing_template) → 保留原装备并应用模板纹饰（可选铜锭饰材） | ；静态启用 |
| `minecraft:cut_copper_from_copper_block_stonecutting` | `minecraft:stonecutting` | 铜块 (minecraft:copper_block) → 切制铜块 (minecraft:cut_copper) | ；静态启用 |
| `minecraft:cut_copper_slab_from_copper_block_stonecutting` | `minecraft:stonecutting` | 铜块 (minecraft:copper_block) → 切制铜台阶 (minecraft:cut_copper_slab) | ；静态启用 |
| `minecraft:cut_copper_slab_from_cut_copper_stonecutting` | `minecraft:stonecutting` | 切制铜块 (minecraft:cut_copper) → 切制铜台阶 (minecraft:cut_copper_slab) | ；静态启用 |
| `minecraft:cut_copper_stairs_from_copper_block_stonecutting` | `minecraft:stonecutting` | 铜块 (minecraft:copper_block) → 切制铜楼梯 (minecraft:cut_copper_stairs) | ；静态启用 |
| `minecraft:cut_copper_stairs_from_cut_copper_stonecutting` | `minecraft:stonecutting` | 切制铜块 (minecraft:cut_copper) → 切制铜楼梯 (minecraft:cut_copper_stairs) | ；静态启用 |
| `minecraft:exposed_cut_copper_from_exposed_copper_stonecutting` | `minecraft:stonecutting` | 斑驳的铜块 (minecraft:exposed_copper) → 斑驳的切制铜块 (minecraft:exposed_cut_copper) | ；静态启用 |
| `minecraft:exposed_cut_copper_slab_from_exposed_copper_stonecutting` | `minecraft:stonecutting` | 斑驳的铜块 (minecraft:exposed_copper) → 斑驳的切制铜台阶 (minecraft:exposed_cut_copper_slab) | ；静态启用 |
| `minecraft:exposed_cut_copper_slab_from_exposed_cut_copper_stonecutting` | `minecraft:stonecutting` | 斑驳的切制铜块 (minecraft:exposed_cut_copper) → 斑驳的切制铜台阶 (minecraft:exposed_cut_copper_slab) | ；静态启用 |
| `minecraft:exposed_cut_copper_stairs_from_exposed_copper_stonecutting` | `minecraft:stonecutting` | 斑驳的铜块 (minecraft:exposed_copper) → 斑驳的切制铜楼梯 (minecraft:exposed_cut_copper_stairs) | ；静态启用 |
| `minecraft:exposed_cut_copper_stairs_from_exposed_cut_copper_stonecutting` | `minecraft:stonecutting` | 斑驳的切制铜块 (minecraft:exposed_cut_copper) → 斑驳的切制铜楼梯 (minecraft:exposed_cut_copper_stairs) | ；静态启用 |
| `minecraft:oxidized_cut_copper_from_oxidized_copper_stonecutting` | `minecraft:stonecutting` | 氧化的铜块 (minecraft:oxidized_copper) → 氧化的切制铜块 (minecraft:oxidized_cut_copper) | ；静态启用 |
| `minecraft:oxidized_cut_copper_slab_from_oxidized_copper_stonecutting` | `minecraft:stonecutting` | 氧化的铜块 (minecraft:oxidized_copper) → 氧化的切制铜台阶 (minecraft:oxidized_cut_copper_slab) | ；静态启用 |
| `minecraft:oxidized_cut_copper_slab_from_oxidized_cut_copper_stonecutting` | `minecraft:stonecutting` | 氧化的切制铜块 (minecraft:oxidized_cut_copper) → 氧化的切制铜台阶 (minecraft:oxidized_cut_copper_slab) | ；静态启用 |
| `minecraft:oxidized_cut_copper_stairs_from_oxidized_copper_stonecutting` | `minecraft:stonecutting` | 氧化的铜块 (minecraft:oxidized_copper) → 氧化的切制铜楼梯 (minecraft:oxidized_cut_copper_stairs) | ；静态启用 |
| `minecraft:oxidized_cut_copper_stairs_from_oxidized_cut_copper_stonecutting` | `minecraft:stonecutting` | 氧化的切制铜块 (minecraft:oxidized_cut_copper) → 氧化的切制铜楼梯 (minecraft:oxidized_cut_copper_stairs) | ；静态启用 |
| `minecraft:waxed_cut_copper_from_waxed_copper_block_stonecutting` | `minecraft:stonecutting` | 涂蜡的铜块 (minecraft:waxed_copper_block) → 涂蜡的切制铜块 (minecraft:waxed_cut_copper) | ；静态启用 |
| `minecraft:waxed_cut_copper_slab_from_waxed_copper_block_stonecutting` | `minecraft:stonecutting` | 涂蜡的铜块 (minecraft:waxed_copper_block) → 涂蜡的切制铜台阶 (minecraft:waxed_cut_copper_slab) | ；静态启用 |
| `minecraft:waxed_cut_copper_slab_from_waxed_cut_copper_stonecutting` | `minecraft:stonecutting` | 涂蜡的切制铜块 (minecraft:waxed_cut_copper) → 涂蜡的切制铜台阶 (minecraft:waxed_cut_copper_slab) | ；静态启用 |
| `minecraft:waxed_cut_copper_stairs_from_waxed_copper_block_stonecutting` | `minecraft:stonecutting` | 涂蜡的铜块 (minecraft:waxed_copper_block) → 涂蜡的切制铜楼梯 (minecraft:waxed_cut_copper_stairs) | ；静态启用 |
| `minecraft:waxed_cut_copper_stairs_from_waxed_cut_copper_stonecutting` | `minecraft:stonecutting` | 涂蜡的切制铜块 (minecraft:waxed_cut_copper) → 涂蜡的切制铜楼梯 (minecraft:waxed_cut_copper_stairs) | ；静态启用 |
| `minecraft:waxed_exposed_cut_copper_from_waxed_exposed_copper_stonecutting` | `minecraft:stonecutting` | 涂蜡的斑驳铜块 (minecraft:waxed_exposed_copper) → 涂蜡的斑驳切制铜块 (minecraft:waxed_exposed_cut_copper) | ；静态启用 |
| `minecraft:waxed_exposed_cut_copper_slab_from_waxed_exposed_copper_stonecutting` | `minecraft:stonecutting` | 涂蜡的斑驳铜块 (minecraft:waxed_exposed_copper) → 涂蜡的斑驳切制铜台阶 (minecraft:waxed_exposed_cut_copper_slab) | ；静态启用 |
| `minecraft:waxed_exposed_cut_copper_slab_from_waxed_exposed_cut_copper_stonecutting` | `minecraft:stonecutting` | 涂蜡的斑驳切制铜块 (minecraft:waxed_exposed_cut_copper) → 涂蜡的斑驳切制铜台阶 (minecraft:waxed_exposed_cut_copper_slab) | ；静态启用 |
| `minecraft:waxed_exposed_cut_copper_stairs_from_waxed_exposed_copper_stonecutting` | `minecraft:stonecutting` | 涂蜡的斑驳铜块 (minecraft:waxed_exposed_copper) → 涂蜡的斑驳切制铜楼梯 (minecraft:waxed_exposed_cut_copper_stairs) | ；静态启用 |
| `minecraft:waxed_exposed_cut_copper_stairs_from_waxed_exposed_cut_copper_stonecutting` | `minecraft:stonecutting` | 涂蜡的斑驳切制铜块 (minecraft:waxed_exposed_cut_copper) → 涂蜡的斑驳切制铜楼梯 (minecraft:waxed_exposed_cut_copper_stairs) | ；静态启用 |
| `minecraft:waxed_oxidized_cut_copper_from_waxed_oxidized_copper_stonecutting` | `minecraft:stonecutting` | 涂蜡的氧化铜块 (minecraft:waxed_oxidized_copper) → 涂蜡的氧化切制铜块 (minecraft:waxed_oxidized_cut_copper) | ；静态启用 |
| `minecraft:waxed_oxidized_cut_copper_slab_from_waxed_oxidized_copper_stonecutting` | `minecraft:stonecutting` | 涂蜡的氧化铜块 (minecraft:waxed_oxidized_copper) → 涂蜡的氧化切制铜台阶 (minecraft:waxed_oxidized_cut_copper_slab) | ；静态启用 |
| `minecraft:waxed_oxidized_cut_copper_slab_from_waxed_oxidized_cut_copper_stonecutting` | `minecraft:stonecutting` | 涂蜡的氧化切制铜块 (minecraft:waxed_oxidized_cut_copper) → 涂蜡的氧化切制铜台阶 (minecraft:waxed_oxidized_cut_copper_slab) | ；静态启用 |
| `minecraft:waxed_oxidized_cut_copper_stairs_from_waxed_oxidized_copper_stonecutting` | `minecraft:stonecutting` | 涂蜡的氧化铜块 (minecraft:waxed_oxidized_copper) → 涂蜡的氧化切制铜楼梯 (minecraft:waxed_oxidized_cut_copper_stairs) | ；静态启用 |
| `minecraft:waxed_oxidized_cut_copper_stairs_from_waxed_oxidized_cut_copper_stonecutting` | `minecraft:stonecutting` | 涂蜡的氧化切制铜块 (minecraft:waxed_oxidized_cut_copper) → 涂蜡的氧化切制铜楼梯 (minecraft:waxed_oxidized_cut_copper_stairs) | ；静态启用 |
| `minecraft:waxed_weathered_cut_copper_from_waxed_weathered_copper_stonecutting` | `minecraft:stonecutting` | 涂蜡的锈蚀铜块 (minecraft:waxed_weathered_copper) → 涂蜡的锈蚀切制铜块 (minecraft:waxed_weathered_cut_copper) | ；静态启用 |
| `minecraft:waxed_weathered_cut_copper_slab_from_waxed_weathered_copper_stonecutting` | `minecraft:stonecutting` | 涂蜡的锈蚀铜块 (minecraft:waxed_weathered_copper) → 涂蜡的锈蚀切制铜台阶 (minecraft:waxed_weathered_cut_copper_slab) | ；静态启用 |
| `minecraft:waxed_weathered_cut_copper_slab_from_waxed_weathered_cut_copper_stonecutting` | `minecraft:stonecutting` | 涂蜡的锈蚀切制铜块 (minecraft:waxed_weathered_cut_copper) → 涂蜡的锈蚀切制铜台阶 (minecraft:waxed_weathered_cut_copper_slab) | ；静态启用 |
| `minecraft:waxed_weathered_cut_copper_stairs_from_waxed_weathered_copper_stonecutting` | `minecraft:stonecutting` | 涂蜡的锈蚀铜块 (minecraft:waxed_weathered_copper) → 涂蜡的锈蚀切制铜楼梯 (minecraft:waxed_weathered_cut_copper_stairs) | ；静态启用 |
| `minecraft:waxed_weathered_cut_copper_stairs_from_waxed_weathered_cut_copper_stonecutting` | `minecraft:stonecutting` | 涂蜡的锈蚀切制铜块 (minecraft:waxed_weathered_cut_copper) → 涂蜡的锈蚀切制铜楼梯 (minecraft:waxed_weathered_cut_copper_stairs) | ；静态启用 |
| `minecraft:weathered_cut_copper_from_weathered_copper_stonecutting` | `minecraft:stonecutting` | 锈蚀的铜块 (minecraft:weathered_copper) → 锈蚀的切制铜块 (minecraft:weathered_cut_copper) | ；静态启用 |
| `minecraft:weathered_cut_copper_slab_from_weathered_copper_stonecutting` | `minecraft:stonecutting` | 锈蚀的铜块 (minecraft:weathered_copper) → 锈蚀的切制铜台阶 (minecraft:weathered_cut_copper_slab) | ；静态启用 |
| `minecraft:weathered_cut_copper_slab_from_weathered_cut_copper_stonecutting` | `minecraft:stonecutting` | 锈蚀的切制铜块 (minecraft:weathered_cut_copper) → 锈蚀的切制铜台阶 (minecraft:weathered_cut_copper_slab) | ；静态启用 |
| `minecraft:weathered_cut_copper_stairs_from_weathered_copper_stonecutting` | `minecraft:stonecutting` | 锈蚀的铜块 (minecraft:weathered_copper) → 锈蚀的切制铜楼梯 (minecraft:weathered_cut_copper_stairs) | ；静态启用 |
| `minecraft:weathered_cut_copper_stairs_from_weathered_cut_copper_stonecutting` | `minecraft:stonecutting` | 锈蚀的切制铜块 (minecraft:weathered_cut_copper) → 锈蚀的切制铜楼梯 (minecraft:weathered_cut_copper_stairs) | ；静态启用 |

### netmusic

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `netmusic:big_megaphone` | `minecraft:crafting_shaped` | 4 × #forge:ingots/copper + 音符盒 (minecraft:note_block) → 大喇叭 (netmusic:big_megaphone) | ；静态启用 |
| `netmusic:cd_burner` | `minecraft:crafting_shaped` | 避雷针 (minecraft:lightning_rod) + 2 × #forge:nuggets/iron + #forge:dusts/redstone + 3 × #forge:ingots/copper → 唱片刻录机 (netmusic:cd_burner) | ；静态启用 |
| `netmusic:computer` | `minecraft:crafting_shaped` | 3 × 玻璃板 (minecraft:glass_pane) + 2 × #forge:ingots/copper + 时钟 (minecraft:clock) + 2 × #forge:ingots/iron + #forge:dusts/redstone → 电脑 (netmusic:computer) | ；静态启用 |
| `netmusic:music_player` | `minecraft:crafting_shaped` | #forge:ingots/copper + 避雷针 (minecraft:lightning_rod) + 音符盒 (minecraft:note_block) + 3 × #minecraft:planks → 唱片机 (netmusic:music_player) | ；静态启用 |

### sliceanddice

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `sliceanddice:sprinkler` | `minecraft:crafting_shaped` | 4 × #forge:plates/copper + 流体管道 (create:fluid_pipe) + 铁栏杆 (minecraft:iron_bars) → 3 × 洒水器 (sliceanddice:sprinkler) | ；静态启用 |

### sophisticatedbackpacks

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `sophisticatedbackpacks:stack_upgrade_starter_tier` | `minecraft:crafting_shaped` | 8 × #forge:storage_blocks/copper + 空白升级 (sophisticatedbackpacks:upgrade_base) → 堆叠升级青春版 (sophisticatedbackpacks:stack_upgrade_starter_tier) | ；配置条件待核对 |
| `sophisticatedbackpacks:copper_backpack` | `sophisticatedbackpacks:backpack_upgrade` | 8 × #forge:ingots/copper + 背包 (sophisticatedbackpacks:backpack) → 铜制背包 (sophisticatedbackpacks:copper_backpack) | ；配置条件待核对 |
| `sophisticatedbackpacks:iron_backpack_from_copper` | `sophisticatedbackpacks:backpack_upgrade` | 4 × #forge:ingots/iron + 铜制背包 (sophisticatedbackpacks:copper_backpack) → 铁制背包 (sophisticatedbackpacks:iron_backpack) | ；配置条件待核对 |

### supplementaries

| 配方 ID | 工艺 | 输入 → 输出 | 参数 / 状态 |
|---|---|---|---|
| `supplementaries:altimeter` | `minecraft:crafting_shaped` | 4 × 铜锭 (minecraft:copper_ingot) + 红石粉 (minecraft:redstone) → 海拔计 (supplementaries:altimeter) | ；配置条件待核对 |
| `supplementaries:cog_block` | `minecraft:crafting_shaped` | 8 × 铜锭 (minecraft:copper_ingot) + 红石块 (minecraft:redstone_block) → 6 × 齿轮方块 (supplementaries:cog_block) | ；配置条件待核对 |
| `supplementaries:fire_pit` | `minecraft:crafting_shaped` | 5 × 铜锭 (minecraft:copper_ingot) + 荧光液瓶 (supplementaries:lumisene_bottle) → 篝火 (supplementaries:fire_pit) | ；配置条件待核对 |
| `supplementaries:fire_pit_2` | `minecraft:crafting_shaped` | 5 × 铜锭 (minecraft:copper_ingot) + #minecraft:coal → 篝火 (supplementaries:fire_pit) | ；配置条件待核对 |
| `supplementaries:turn_table` | `minecraft:crafting_shaped` | 3 × #minecraft:planks + 4 × 圆石 (minecraft:cobblestone) + 铜锭 (minecraft:copper_ingot) + 红石粉 (minecraft:redstone) → 转台 (supplementaries:turn_table) | ；配置条件待核对 |
| `supplementaries:wrench` | `minecraft:crafting_shaped` | 4 × 铜锭 (minecraft:copper_ingot) → 扳手 (supplementaries:wrench) | ；配置条件待核对 |

## 未计入主表的原生定义

| 配方 ID | 原因 |
|---|---|
| `appliedcreate:andesite_stress_cell_housing` | KubeJS移除：kubejs/server_scripts/me_memory.js |
| `appliedcreate:brass_stress_cell_housing` | KubeJS移除：kubejs/server_scripts/me_memory.js |
| `appliedcreate:circuit_cutter/advanced_stress_circuit_board` | 缺少依赖或标签条件不成立 |
| `appliedcreate:stress_storage_cell_16m` | KubeJS移除：kubejs/server_scripts/me_memory.js |
| `appliedcreate:stress_storage_cell_1m` | KubeJS移除：kubejs/server_scripts/me_memory.js |
| `appliedcreate:stress_storage_cell_256m` | KubeJS移除：kubejs/server_scripts/me_memory.js |
| `appliedcreate:stress_storage_cell_4m` | KubeJS移除：kubejs/server_scripts/me_memory.js |
| `appliedcreate:stress_storage_cell_64m` | KubeJS移除：kubejs/server_scripts/me_memory.js |
| `create:crushing/compat/elementaryores/ore_copper_end` | 缺少依赖或标签条件不成立 |
| `create:crushing/compat/elementaryores/ore_copper_nether` | 缺少依赖或标签条件不成立 |
| `create:cutting/compat/immersiveengineering/wire_copper` | 缺少依赖或标签条件不成立 |
| `createdieselgenerators:compat/createaddition/copper_rod` | 缺少依赖或标签条件不成立 |
| `createdieselgenerators:compat/createaddition/copper_wire` | 缺少依赖或标签条件不成立 |
| `createdieselgenerators:compat/immersiveengineering/copper_wire` | 缺少依赖或标签条件不成立 |
| `createdieselgenerators:compat/immersiveengineering/herbicide` | 缺少依赖或标签条件不成立 |
| `mekanism:processing/copper/clump/from_ore` | KubeJS移除：kubejs/server_scripts/copper_processing.js |
| `mekanism:processing/copper/clump/from_raw_block` | KubeJS移除：kubejs/server_scripts/copper_processing.js |
| `mekanism:processing/copper/clump/from_raw_ore` | KubeJS移除：kubejs/server_scripts/copper_processing.js |
| `mekanism:processing/copper/clump/from_shard` | KubeJS移除：kubejs/server_scripts/copper_processing.js |
| `supplementaries:sconce_nether_brass` | 缺少依赖或标签条件不成立 |

## 非配方管理器机制

- 熔融铜、黄铜、青铜流体方块在露天连续暴露 30 秒后变为对应结晶熔液，遮挡天空会重置计时。2026-11-14 起敞口工作盆也可结晶；结晶熔液在工作盆按压缩机转速消耗 600 mB 析出三枚结晶，剩余流体恢复为普通熔液。详见 `MoltenCrystallizationEvents` 与 Pressure Crystals 说明。
- 铜建筑方块的自然氧化、用斧刮除氧化层、蜜脾涂蜡和斧去蜡是世界交互；工作台中已有的涂蜡配方列在主表。
- 普通烧制与高炉烧制只改变名称说明：氧化铜 → 铜锭；不增加酸浸、电解或其他铜化合物路线。
