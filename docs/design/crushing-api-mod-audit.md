# 粉碎 / 研磨统一接口：整合包语义审计

审计依据：仓库 manifest/mods.json 中全部 135 个模组条目；重点按机器行为、配方语义和当前安装版本判断，不只按模组名称检索。

## 结论与边界

统一 API 覆盖“固体物品经粉碎、磨碎或研磨而变成粉末/碎料或其他加工产物”的机器配方。统一的是注册接口，不是把所有加工都当成粉碎；切割、压制、砂纸抛光、洗涤、附魔拆解、烹饪等语义不同的工艺不应混为一类。

当前接入的适配器：

| machine | 模组/机器 | 支持的配方能力 |
|---|---|---|
| create:crushing | Create 粉碎轮 | 多个物品结果、独立概率结果 |
| create:milling | Create 磨石 | 多个物品结果、独立概率结果 |
| mekanism:crushing | Mekanism 粉碎机 | 单个确定物品结果；其他形式显式报错 |
| create_enchantment_industry:grinding | Create: Enchantment Industry 机械磨石 | 最多 4 个物品结果；可选一个流体输入或一个流体结果 |

统一入口为 global.DimensionWorksGrinding.register(event, recipe)。global.DimensionWorksCrushing 保留为兼容别名。未来接入其他 mod 机器时，只需注册适配器，不改变调用方的数据模型。

Create 的粉碎轮/磨石通过 KubeJS Create 注册；Mekanism 粉碎机通过 KubeJS Mekanism 注册；CEI Grinding 使用它自己的 1.20.1 配方类型 JSON（不是 Create:crushing，也不是 CEI disenchanting）。CEI 1.20.1 GrindingRecipe 的源代码约束是一个物品输入、最多四个物品输出，以及一个流体输入或一个流体输出；两种流体方向不可同时存在。

## 逐项语义判断

### 直接接入

- create：Crushing Wheels 和 Millstone 是直接匹配的物料粉碎/研磨机器。
- kubejs_create：为 Create 的 crushing/milling 提供配方注册接口，不需要为每个 Create 附属重复实现一套 Create 基础配方适配器。
- mekanism：Crusher 是直接匹配的粉碎机。
- kubejs_mekanism：提供 Mekanism crushing 配方 builder；当前适配器限制为一个确定结果，避免静默丢弃概率。
- create_enchantment_industry：含独立的 create_enchantment_industry:grinding 机器配方类型和 Mechanical Grindstone，故作为独立适配器接入。disenchanting 是附魔拆解/经验转换，并不是物料粉碎，故不混用。
- alexscaves + dimensionworks_cave_factory：洞穴维度脚本中原有的 Create 粉碎铀碎片与磨石制薄荷粉配方已迁移到共享接口。Cave Factory 六种自定义机器是极性冶炼、生物反应、衰变、渊压萃取、暗相谐振、糖晶析出，没有独立粉碎机。
- ae2：专门核对了本包安装的 AE2 15.4.10 对应的 Forge 1.20.1 源代码。该版本的 AERecipeTypes 未注册 grinder recipe type，misc blockentity 列表也没有独立 Grindstone/Grinder 实现，因此没有可用的配方接口需要适配。旧版本/旧教程中的 Quartz Grindstone 描述不能直接套用到本包版本；不凭历史版本添加虚构的 ae2:grinder 配方类型。

### Create 附属：逐项审查

| 模组 | 判定 |
|---|---|
| laowu / Create: Meowchanics | 含机械滚轮/铁砧压扁等特殊加工，但这是压制/滚压，不是粉碎配方。若其配方直接使用 Create:crushing 或 Create:milling，则基础 Create 适配器已能覆盖，不需要附属专用适配器。 |
| create_dragons_plus / Create: Dragons Plus | 风扇处理、冻结、染色与表面打磨/抛光；未发现需要单独注册的粉碎配方类型。砂纸表面处理不等于把固体研成粉。 |
| create_enchantment_industry | Mechanical Grindstone 的 grinding 类型独立适配；disenchanting 仍单独归类。 |
| sliceanddice / Create Slice & Dice | Slicer 自动执行切菜板配方，属于切片/切割，而非粉碎。 |
| create_diesel_generators | 燃料精炼、发酵、成形和浇铸；不是粉碎类。 |
| create_integrated_farming | 种植、收割和农作物集成；不是粉碎类。 |
| creategearsandtavern | 酒馆、酒液及相关 Create 自动化；没有同语义的独立粉碎机。 |
| create_connected、copycats | 物流、动力与结构方块扩展；没有同语义的独立粉碎机。 |
| create_additional_logistics、create-mobile-packages-dimensionworks | 物流、包裹与物品输送；不是粉碎。 |
| create_liquid_fuel | 液体燃料处理；不是固体粉碎。 |
| create_power_loader、create-ultimine、createfastschematiccannon | 区块加载、挖掘辅助、蓝图炮；不提供本类配方机器。 |
| create_cyber_goggles | 客户端信息显示；不提供配方机器。 |
| sophisticated_backpacks_create_integration | 背包与 Create 物流集成；不是粉碎机器。 |

### 其他功能相邻的模组

- mekanismdelight、farmersdelight、kaleidoscope_cookery、kaleidoscopecookery_automation、kaleidoscope_tavern、brewinandchewin、aethersdelight、caves_delight、maidtavern：涉及食物、切菜、饮品和烹饪。切菜板操作属于切割，不是物料粉碎。
- dimensionworks_rpm_limit：现有代码会跟踪 Create Millstone / Mechanical Saw 等机器的处理与转速，但这个模组是限制逻辑，并未引入新的粉碎配方类型。新适配器仍需单独确认是否受到该模组的运行时限制，不能凭 API 注册成功就认定机器行为一致。
- alexscaves：个别粉末/碎片配方由 Create crushing/milling 执行，现已统一从 Create 适配器注册；不额外虚构 Alex's Caves 专属粉碎机。
- 原版砂轮：其用途是修复耐久与拆除附魔，不会把材料磨成粉，因此排除。
- AE2：按当前 1.20.1 安装版本核对，没有可注册的 Quartz Grindstone 配方类型；旧版 AE2 文档不作为当前版本适配依据。

## 其余 100 个模组条目

以下 ID 逐项与 manifest 对照过，没有识别到需要新增“固体粉碎/研磨”配方适配器的独立机器或配方类型；它们主要属于库、UI、地图、存储、权限、内容维度、自动化辅助等类别：

ae2ct, ae2omnicells, ae2wtlib, aether, appleskin, appliedcreate, appmek, architectury, balm, betterf3, carryon, chat_heads, citadel, cloth_config, colorwheel, colorwheel_patcher, controlling, creativecore, curios, customskinloader, dimensionworks_drop_lifetime, dimensionworks_ftbchunks_no_forceload, dimensionworks_localization, dimensionworks_maid_ae_crank, dimensionworks_mek_stress, dimensionworks_transfer, dimensionworks_version_gate, dimensionworks_wrench_compat, drippyloadingscreen, embeddium, enhanced_boss_bars, expatternprovider, fancy_entity_renderer, fancymenu, ferritecore, ftb-chunks, ftb-essentials, ftb-library, ftb-quests, ftb-teams, ftb-ultimine, ftb-xmod-compat, glodium, guideme, ias, iceberg, imblocker, inventoryhud, inventoryprofilesnext, iris_shader_folder, itemborders, itemphysiclite, jade, jadeaddons, jecharacters, jeed, jei, kaleidoscope_compat, kaleidoscope_contraption, kaleidoscope_dim_wine, kaleidoscope_end, kaleidoscope_nether, kaleidoscope_world_liquor, konkrete, kotlinforforge, kubejs, l2complements, l2library, libipn, luckperms, maid_storage_manager, maidusehandcrank, megacells, melody, meplacementtool, minemenu, modernfix, modernui, mousetweaks, netmusic, nochatreports, oculus, overloadedarmorbar, patchouli, prism, rhino, searchables, shulkerboxtooltip, smsn, sodiumextras, sophisticatedbackpacks, sophisticatedcore, titanium, touhou_little_maid, traderefresh, trashslot, trueuuid, witherstormmod, xaerominimap, xaeroworldmap

## 静态测试与限制

测试应验证适配器映射、概率结果、CEI 流体字段、未知机器/不支持能力报错、现有 Create crushing/milling 调用是否绕过共享 API，以及清单语义审计文件是否覆盖关键候选项。静态测试无法替代游戏内配方解码、JEI 显示、物品扣料和实际机器运行检查。
