# 粉碎 / 研磨统一接口：整合包语义审计

审计基准：`manifest/mods.json` 中的 135 个已配置模组；Minecraft 1.20.1 Forge；Create 6.0.8；Mekanism 10.4.16.80；KubeJS Create 2001.3.0-build.8；KubeJS Mekanism UNOFFICIAL 2001.1.5.1。

## 结论

统一接口覆盖的是“固体输入经粉碎、磨碎、研磨后产出物品/流体”的工艺语义，不是所有名字里带有 Processing、Grinding、Saw 或 Machine 的功能。当前接入三个底层家族：

| 接口 machine | 所属模组与机器 | 底层配方类型 | 能力 |
|---|---|---|---|
| \`create:crushing\` | Create 粉碎轮 | Create Crushing | 多个物品产物、概率产物 |
| \`create:milling\` | Create 磨石 | Create Milling | 多个物品产物、概率产物 |
| \`mekanism:crushing\` | Mekanism 粉碎机 | Mekanism Crushing | 当前 KubeJS 适配器只接受单个确定产物 |
| \`create_enchantment_industry:grinding\` | Create: Enchantment Industry 机械磨石 | CEI Grinding | 最多 4 个物品产物，并可选一个流体输入或一个流体输出 |

统一入口为 \`global.DimensionWorksGrinding.register(event, recipe)\`。旧入口 \`global.DimensionWorksCrushing\` 保留为兼容别名。底层适配器不能宣称不存在的能力：例如 Mekanism 粉碎适配器若收到概率产物或多产物，会直接报错。

Create 的 crushing/milling 通过 KubeJS Create 注册；Mekanism crushing 通过 KubeJS Mekanism 注册；CEI Grinding 由于包内 KubeJS 集成没有该专用 builder，使用其 Forge 1.20.1 配方类型的自定义 JSON 结构。CEI 的流体输入/输出与物品产物使用各自字段，不会伪装成普通物品栈。

## 逐项语义审查

### 直接接入

- **\`create\`**：有 Crushing Wheels 的 crushing 和 Millstone 的 milling，均是本接口核心类别。
- **\`kubejs_create\`**：提供 Create crushing/milling 的配方注册能力；不需要再为每个 Create 附属写一套重复适配器。
- **\`mekanism\`**：Crusher 是直接匹配的粉碎机。
- **\`kubejs_mekanism\`**：提供 \`event.recipes.mekanism.crushing(...)\`；概率产物能力按当前 builder 的实际能力限制。
- **\`create_enchantment_industry\`**：包内版本有独立 \`create_enchantment_industry:grinding\` 配方类型和机械磨石，因此单独接入。该 recipe type 最多接收一个物品输入，可输出最多四个物品结果，并允许一个流体输入或一个流体输出。
- **\`alexscaves\` + \`dimensionworks_cave_factory\` 配方脚本**：原洞穴维度脚本中，铀碎片使用 Create crushing、薄荷粉使用 Create milling；这两处现在改用统一 API，避免脚本中出现第二套直接调用方式。Cave Factory 本身的六类自定义工厂是极性冶炼、生物反应、衰变、渊压萃取、暗相谐振和糖晶析出，并没有独立的固体粉碎机。

### Create 附属：逐项判断

| 已安装模组 | 语义与决定 |
|---|---|
| \`laowu\` / Create: Meowchanics | 有多种 Create 加工，并有机械滚轮与铁砧压扁猫的机制。滚压/压制不是粉碎；若某配方实际使用 Create 的 crushing/milling 类型，直接走本接口对应适配器即可，无需单独附属适配器。Permeation Tank 是独立材料处理，不假定它等于粉碎。 |
| \`create_dragons_plus\` | 风扇批量染色、冻结、末地能量处理、砂纸打磨/抛光；属于风扇处理或表面处理，不是把固体研磨成粉。 |
| \`create_enchantment_industry\` | 机械磨石的 \`grinding\` 是相同类别，已接入；其 \`disenchanting\` 则是附魔拆解/经验转换，不等价于固体粉碎，因此不共用同一个 recipe type。 |
| \`sliceanddice\` | Slicer 自动执行切菜板配方，语义是切片/切割，不是粉碎或研磨；不混入该 API。 |
| \`create_diesel_generators\` | 油料精炼、发酵、压模和浇铸；没有同类粉碎机器。 |
| \`create_integrated_farming\` | 收割、种植及农作物集成；没有同类粉碎机器。 |
| \`creategearsandtavern\` | 酒馆、酒液与 Create 自动化；没有同类粉碎机器。 |
| \`create_connected\`、\`copycats\` | 物流、动力与结构方块增强；没有同类粉碎机器。 |
| \`create_additional_logistics\`、\`create-mobile-packages-dimensionworks\` | 包装与物品物流；不是材料粉碎。 |
| \`create_liquid_fuel\` | 液体燃料功能；不是固体粉碎。 |
| \`create_power_loader\`、\`create-ultimine\`、\`createfastschematiccannon\` | 区块加载、挖掘辅助、蓝图炮；不是材料加工配方类型。 |
| \`create_cyber_goggles\` | 客户端信息显示；无配方机器。 |
| \`sophisticated_backpacks_create_integration\` | 背包与 Create 物流集成；不是粉碎机器。 |

### 其他语义相邻项

- **\`mekanismdelight\`、\`farmersdelight\`、\`kaleidoscope_cookery\`、\`kaleidoscopecookery_automation\`、\`kaleidoscope_tavern\`、\`brewinandchewin\`、\`aethersdelight\`、\`caves_delight\`、\`maidtavern\`**：餐食、切菜、饮品或烹饪自动化。切菜板/切片与粉碎语义不同。
- **\`dimensionworks_rpm_limit\`**：已有代码对 Create Millstone / Mechanical Saw 等机器做转速相关处理；它不是新配方机器。Create 原生 milling/crushing recipe 仍由 Create 本身执行；新增独立机器适配器不应假设自动继承该模组的转速限制。
- **\`alexscaves\`**：包含本整合包自定义的铀碎片、薄荷粉配方，但执行机器是 Create crushing/milling，因此改用对应接口，不额外添加一个“Alex's Caves 粉碎机”。
- **Minecraft 原版砂轮**：主要处理附魔与耐久，不是通用材料粉碎配方类型，因此不纳入。

## 全量清单覆盖记录

审计对象是 \`manifest/mods.json\` 的 135 个条目；除上面逐项列明的候选与相邻项外，其他 105 个模组未发现独立的固体粉碎/研磨配方机器，不需要额外 adapter。为便于复核，下面保留这 105 个已审查的模组 ID：

\`ae2\`, \`ae2ct\`, \`ae2omnicells\`, \`ae2wtlib\`, \`alex_caves_dimensions\`, \`appleskin\`, \`appliedcreate\`, \`appmek\`, \`architectury\`, \`balm\`, \`betterf3\`, \`brewinandchewin\`, \`carryon\`, \`caves_delight\`, \`chat_heads\`, \`citadel\`, \`cloth_config\`, \`colorwheel\`, \`colorwheel_patcher\`, \`controlling\`, \`creativecore\`, \`curios\`, \`customskinloader\`, \`dimensionworks_drop_lifetime\`, \`dimensionworks_ftbchunks_no_forceload\`, \`dimensionworks_localization\`, \`dimensionworks_maid_ae_crank\`, \`dimensionworks_mek_stress\`, \`dimensionworks_transfer\`, \`dimensionworks_version_gate\`, \`dimensionworks_wrench_compat\`, \`drippyloadingscreen\`, \`embeddium\`, \`enhanced_boss_bars\`, \`exptternprovider\`, \`fancy_entity_renderer\`, \`fancymenu\`, \`ferritecore\`, \`ftb-chunks\`, \`ftb-essentials\`, \`ftb-library\`, \`ftb-quests\`, \`ftb-teams\`, \`ftb-ultimine\`, \`ftb-xmod-compat\`, \`glodium\`, \`guideme\`, \`ias\`, \`iceberg\`, \`imblocker\`, \`inventoryhud\`, \`inventoryprofilesnext\`, \`iris_shader_folder\`, \`itemborders\`, \`itemphysiclite\`, \`jade\`, \`jadeaddons\`, \`jecharacters\`, \`jeed\`, \`jei\`, \`kaleidoscope_compat\`, \`kaleidoscope_contraption\`, \`kaleidoscope_dim_wine\`, \`kaleidoscope_end\`, \`kaleidoscope_nether\`, \`kaleidoscope_world_liquor\`, \`konkrete\`, \`kotlinforforge\`, \`kubejs\`, \`l2complements\`, \`l2library\`, \`libipn\`, \`luckperms\`, \`maid_storage_manager\`, \`maidusehandcrank\`, \`megacells\`, \`melody\`, \`meplacementtool\`, \`minemenu\`, \`modernfix\`, \`modernui\`, \`mousetweaks\`, \`netmusic\`, \`nochatreports\`, \`oculus\`, \`overloadedarmorbar\`, \`patchouli\`, \`prism\`, \`rhino\`, \`searchables\`, \`shulkerboxtooltip\`, \`smsn\`, \`sodiumextras\`, \`sophisticatedbackpacks\`, \`sophisticatedcore\`, \`titanium\`, \`touhou_little_maid\`, \`traderefresh\`, \`trashslot\`, \`trueuuid\`, \`witherstormmod\`, \`xaerominimap\`, \`xaeroworldmap\`.

> 注：模组的“内容有粉末物品”与“新增粉碎机器/配方类型”是两回事。统一 API 负责为指定机器类型注册配方；它不自动重写其他模组的所有原生配方，也不把切割、压制、洗涤等工艺错误解释为粉碎。
