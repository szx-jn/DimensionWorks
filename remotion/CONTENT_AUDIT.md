# 开场取材：整包内容与魔改核对

清单基线为本次修改时的 `manifest/mods.json`：142 个条目，双端 110、客户端 31、服务端 1。版本和环境取自清单；装有某个 Mod，不等于原模组全部玩法在本包开放。

核对深度：全量清单的名称、固定版本、来源与环境；全部 11 个自研模块 README；主要 KubeJS 注册、服务端配方、客户端隐藏规则；相关默认配置、任务章节与既有 fork 文档。重点追踪齿轮之心效果、ME 实时应力、机械供能与维度访问拦截的源码。没有逐个审计第三方 Mod 完整源码，没有运行游戏验证全部 Mod 功能。

## 工业与资源

| 系统 | 当前实现及边界 | 开场处理 |
| --- | --- | --- |
| Create / Mekanism | 机械动力桥接到合格加工机；旧 FE/J 供能和储能路线退役，保留加工与化学机制 | 轴驱动原本使用 FE 的机器，不宣称所有设备可任意双向换能 |
| 应用机动 | Applied Create 经自研模块改为实时 IMPORT 输入、EXPORT 占额；DDR 驱动器和卡调整转速及输入损耗，旧库存停用 | 展示输入、网络、输出，不凭空发电或无限储能 |
| AE2 扩展 | Applied Mekanistics、ExtendedAE、MEGA Cells、OMNI Cells、无线终端、ECO AE 等在清单中 | 作为存储与调度生态；不把附属的所有原生功能都说成魔改 |
| 龙之进化 | 配方收束为反应堆路线；机械输入注入器、稳定器输出机械动力 | 不展示龙套装备和聚合合成作为当前本包主线 |
| 熔融金属 | 21 种材料，90 mB/锭；普通/超级加热分档；黄铜、青铜流体混合及冷冻配方 | 铜、铁、金作代表；不声称所有材料同温熔化 |
| 空气结晶 | 用户确认的后续方向，异常物质、结晶流体与特殊剥离尚未实现 | 独立机制预告；同温同种金属空气接触对照，结晶后仍流动，后续化工停滞 |
| 核心化学品 | 12 种新增 Mek 化学品及相应配方；Create 工作盆输出 Mek 气体，露天损失、封闭保留 | 不把已有气体损失等同于未来金属结晶；gas 注册类型不等于现实常温气态 |
| 基础矿物 | 铁：粗铁磨石研磨→铁粉；铁粉机械压制→活化料；活化料加另一份铁粉经结合器→粗铁。其余矿物线仍属临时方案 | 不沿用旧设计中的粗铁加安山合金成本，不表现为野外矿脉自行生长 |
| 六洞穴产物 | 18 项 Create 自增殖主链与残渣备链；五维残渣生存来源受后续签名机启用限制 | 不把预览内容描述为完整可玩生态 |
| 燃料与流体 | 烈焰蛋糕底料必需红石；柴油、生物柴油、汽油、乙醇接燃烧室；软管无限流体阈值 2048 | 不解释成新增自然地貌 |
| 工业平台 | 安装但四类关键物品移除配方并在 JEI 隐藏 | 不当作默认开局工具 |

依据：`mods/dimensionworks-mek-stress/README.md`、`docs/design/me-live-stress-network.md`，以及 `kubejs/server_scripts/` 的 me_memory、basic_mineral_propagation、cave_self_propagation、chemistry_gas_recipes、molten_metal_recipes、blaze_cake_redstone_cost、industrial_platform_policy 与 crushing_api。实际铁配方以脚本为准，部分旧 README 的成本描述已落后，不作为分镜依据。

## 心脏、维度与生活

| 系统 | 当前实现及边界 | 开场处理 |
| --- | --- | --- |
| 齿轮之心 | 本次调整为交互距离 +2、急迫 II、虚弱 I、夜视 I；移除幸运 I。所有心脏状态保留基础效果 | 苏醒段 14 秒明确显示四项；虚弱用负面配色 |
| 七类工业额度 | RPM、Create 容量、Mek 处理、AE 并发、跨工序、活跃机器、最终产物；不是七种药水效果 | 保留力量与代价，不展示七碎片解法，不加入随机毁物 |
| 天境及六洞穴维度 | Cave Factory 访问开关默认拦截；洞穴钥匙配方移除并禁止使用，天境装备也受获取/使用限制 | 不把安装等同于开放，不呈现玩家醒来即可访问的完整六维科技线 |
| 洞穴工厂 | 六维骨架、相位、封装与稳定化；只有渊海具有完整生存控制器，其他五维以预览为主 | 不新增自然工厂、遗迹或未知建造者 |
| 中转维度 | 一次性钥匙永久解锁；每玩家 3×3 区块地块、屏障、虚空、恒定光照、和平规则 | 确有实现，但本轮不新增段落、不混作普通主世界地形 |
| Mobile Packages fork | 固定 GitHub Release；包裹物流、玩家打包；本地 fork README 记录原版包裹渲染、0.5 格碰撞箱与移动限制 | 可供后续物流篇取材，不擅自加入故事因果；README 不等于游戏内实测 |
| 女仆生态 | 女仆 AE2 曲柄自研任务，以及存储、酒馆、曲柄附属 | 不虚构“女仆建造了主角苏醒地点”等设定 |
| 农业与烹饪 | 农夫乐事、森罗系列、Create 农业、Slice & Dice 与烹饪自动化 fork | 支撑生活/生产内容，不假定每个原生食谱都有魔改 |
| 凋零风暴与任务 | 已安装且有 FTB 任务章节；没有依据说明它就是天地异变原因 | 不擅自指定灾变来源或终局使命 |

依据：`GearHeartEffects`、`GearHeartForgeEvents`、`CaveFactoryConfig`、`CFForgeEvents`、Transfer / Maid AE Crank README；KubeJS 的 gear_heart、dimension_key_use、cave_dimension_access_lock、aether_equipment_lock；现有 FTB 任务章节与两个固定 Release fork 的文档。

## 兼容、客户端与维护

- FTB Chunks 自身强加载路径禁用，领地保护保留；其他 Mod 的区块票并未全局禁用。
- 掉落寿命按领地/稀有度调整；扳手连锁兼容 AE2/Mek；背包与传送带拾取竞争有防重复修复。这些属于运行规则和修复，不编造灾变因果。
- 版本门禁、汉化、更新器是维护功能。Jade、JEI、地图、字体输入、菜单、音效、皮肤、渲染与性能 Mod 是客户端和操作支持；不据此虚构世界事件。
- 11 个自研模块：RPM Limit、Mek Stress、Cave Factory、Transfer、Maid AE Crank、Drop Lifetime、FTB Chunks No Forceload、Wrench Compat、Item Pickup Guard、Version Gate、Localization。

## 本轮时间与视觉约束

0–10 天地异变；10–21 心脏改写；21–27 史蒂夫遭遇齿轮；27–34 三万年（7 秒）；34–48 苏醒与实际效果（14 秒）；48–61 动力与应用机动；61–79 熔融反应；79–84 回到世界；84–90 片名。总长 2700 帧。

史蒂夫采用原创方块人物插画，使用棕发、像素面部、青色上衣、蓝裤与鞋区分四肢；没有复制第三方皮肤或模型。自然场景不增加工业设施；机制插画抽离背景；心脏与结晶只用声音呼应，不确认共同成因。

## 全量清单快照

以下逐项记录名称、版本、来源与环境。它用于后续取材定位，不代表每个 Mod 的全部机制已运行验收；未来更新以清单为准。

| ID / 名称 | 固定版本 | 来源 | 环境 |
| --- | --- | --- | --- |
| `ae2` · Applied Energistics 2 | 15.4.10 | modrinth | 双端 |
| `ae2ct` · AE2: Crafting Tree | 1.20.1-1.1.1 | curseforge | 双端 |
| `ae2omnicells` · AE2 OMNI Cells | 1.1.6-1.20.1-forge | modrinth | 双端 |
| `ae2wtlib` · Applied Energistics 2 Wireless Terminals | 15.3.3-forge | modrinth | 双端 |
| `ldlib` · LDLib | 1.20.1-1.0.52.a-forge | curseforge | 双端 |
| `neoecoae` · Neo ECO AE Extension | 20.4.2 | curseforge | 双端 |
| `aether` · The Aether | 1.20.1-1.5.2-neoforge | modrinth | 双端 |
| `aethersdelight` · Aether's Delight | 0.1.3.1 | modrinth | 双端 |
| `alex_caves_dimensions` · Dimensions of Alex's Caves | 1.0.4 | modrinth | 双端 |
| `alexscaves` · Alex's Caves | 2.0.2 | modrinth | 双端 |
| `appleskin` · AppleSkin | 2.5.1+mc1.20.1 | modrinth | 双端 |
| `appliedcreate` · Applied Create | 1.1.5 | modrinth | 双端 |
| `appmek` · Applied Mekanistics | 1.4.3 | modrinth | 双端 |
| `architectury` · Architectury API | 9.2.14+forge | modrinth | 双端 |
| `balm` · Balm | 7.3.43+forge-1.20.1 | modrinth | 双端 |
| `betterf3` · BetterF3 | 7.0.2 | modrinth | 客户端 |
| `brandonscore` · Brandon's Core | 1.20.1-3.2.1.302 | modrinth | 双端 |
| `brewinandchewin` · Brewin' And Chewin' | 1.20.1-3.2.1 | modrinth | 双端 |
| `carryon` · Carry On | 2.1.2.7 | modrinth | 双端 |
| `caves_delight` · Caves Delight | 1.1.0 | modrinth | 双端 |
| `chat_heads` · Chat Heads | 0.15.1 | modrinth | 客户端 |
| `citadel` · Citadel | 2.6.3 | modrinth | 双端 |
| `cloth_config` · Cloth Config API | 11.1.136+forge | modrinth | 双端 |
| `codechickenlib` · CodeChicken Lib | 1.20.1-4.4.0.528 | modrinth | 双端 |
| `colorwheel` · Colorwheel | 1.2.1+mc1.20.1 | modrinth | 客户端 |
| `colorwheel_patcher` · Colorwheel Patcher | 1.0.4+mc1.20.1 | modrinth | 客户端 |
| `controlling` · Controlling | 12.0.2 | modrinth | 客户端 |
| `copycats` · Create: Copycats+ | 3.0.10+mc.1.20.1-forge | modrinth | 双端 |
| `create` · Create | mc1.20.1-6.0.8 | modrinth | 双端 |
| `create_additional_logistics` · Create: Additional Logistics | 1.4.5 | modrinth | 双端 |
| `create_connected` · Create: Connected | 1.2.3-mc1.20.1 | modrinth | 双端 |
| `create_cyber_goggles` · Create: Cyber Goggles | 1.20.1-7.2.3-Forge | modrinth | 客户端 |
| `create_diesel_generators` · Create: Diesel Generators | 1.20.1-1.3.12 | modrinth | 双端 |
| `create_dragons_plus` · Create: Dragons Plus | 1.11.9 | curseforge | 双端 |
| `create_enchantment_industry` · Create: Enchantment Industry | 1.4.1 | modrinth | 双端 |
| `create_integrated_farming` · Create: Integrated Farming [Unofficial] | 1.4.3.2 | curseforge | 双端 |
| `create_liquid_fuel` · Create: Liquid Fuel | 2.2.0-1.20.1 | modrinth | 双端 |
| `create_power_loader` · Create: Power Loader | 2.0.3-mc1.20.1 | modrinth | 双端 |
| `create-mobile-packages-dimensionworks` · Create Mobile Packages — DimensionWorks | 0.6.1-dimensionworks.10 | github-release | 双端 |
| `create-ultimine` · Create Ultimine | 1.20.1-forge-1.3.1 | modrinth | 双端 |
| `createfastschematiccannon` · Create: FastSchematicCannon | 1.20.1-1.4.1-6.0.x | modrinth | 双端 |
| `creategearsandtavern` · Create: Gears and Tavern | 1.1.8 | modrinth | 双端 |
| `creativecore` · CreativeCore | 2.12.40 | modrinth | 双端 |
| `curios` · Curios API | 5.14.1+1.20.1 | modrinth | 双端 |
| `customskinloader` · CustomSkinLoader | 14.28-ForgeV2 | modrinth | 客户端 |
| `dimensionworks_cave_factory` · DimensionWorks Cave Factory | 0.1.1 | in-repo | 双端 |
| `dimensionworks_drop_lifetime` · DimensionWorks Drop Lifetime | 0.1.1 | in-repo | 双端 |
| `dimensionworks_ftbchunks_no_forceload` · DimensionWorks FTB Chunks Force-load Disable | 0.1.2 | in-repo | 双端 |
| `dimensionworks_localization` · DimensionWorks 简体中文汉化 | 0.1.3 | in-repo | 客户端 |
| `dimensionworks_maid_ae_crank` · DimensionWorks Maid AE2 Crank | 0.1.0 | in-repo | 双端 |
| `dimensionworks_mek_stress` · DimensionWorks ME Mechanical Power | 0.3.13 | in-repo | 双端 |
| `dimensionworks_rpm_limit` · DimensionWorks RPM Limit | 0.4.13 | in-repo | 双端 |
| `dimensionworks_transfer` · DimensionWorks Transfer | 0.2.0 | in-repo | 双端 |
| `dimensionworks_version_gate` · DimensionWorks Version Gate | 0.1.0 | in-repo | 双端 |
| `dimensionworks_wrench_compat` · DimensionWorks Wrench Compatibility | 0.1.0 | in-repo | 双端 |
| `draconicevolution` · Draconic Evolution | 3.1.2.621 | modrinth | 双端 |
| `drippyloadingscreen` · Drippy Loading Screen | 3.1.5-1.20.1-forge | modrinth | 客户端 |
| `embeddium` · Embeddium | 0.3.31+mc1.20.1 | modrinth | 客户端 |
| `enhanced_boss_bars` · Enhanced Boss Bars | 1.20.1-1.0.0 | modrinth | 客户端 |
| `expatternprovider` · ExtendedAE | 1.20-1.4.14-forge | github-release | 双端 |
| `fancy_entity_renderer` · Fancy Entity Renderer | 0.4.6 | modrinth | 客户端 |
| `fancymenu` · FancyMenu | 3.9.12-1.20.1-forge | modrinth | 双端 |
| `farmersdelight` · Farmer's Delight | 1.20.1-1.3.4 | modrinth | 双端 |
| `ferritecore` · FerriteCore | 6.0.1 | modrinth | 双端 |
| `ftb-chunks` · FTB Chunks | 2001.3.8 | curseforge | 双端 |
| `ftb-essentials` · FTB Essentials | 2001.2.4 | curseforge | 双端 |
| `ftb-library` · FTB Library | 2001.2.13 | curseforge | 双端 |
| `ftb-quests` · FTB Quests | 2001.4.22 | curseforge | 双端 |
| `ftb-teams` · FTB Teams | 2001.3.2 | curseforge | 双端 |
| `ftb-ultimine` · FTB Ultimine | 2001.1.8 | curseforge | 双端 |
| `ftb-xmod-compat` · FTB XMod Compat | 2.1.3 | curseforge | 双端 |
| `glodium` · Glodium | 1.20-1.5-forge | modrinth | 双端 |
| `guideme` · GuideME | 20.1.15 | modrinth | 双端 |
| `ias` · In-Game Account Switcher | 9.0.0 | modrinth | 客户端 |
| `iceberg` · Iceberg | 1.1.25 | modrinth | 双端 |
| `imblocker` · IMBlocker | 5.6.1 | modrinth | 客户端 |
| `inventoryhud` · InventoryHUD+ | 3.4.26 | modrinth | 客户端 |
| `inventoryprofilesnext` · Inventory Profiles Next | forge-1.20.1-1.10.20 | modrinth | 客户端 |
| `iris_shader_folder` · Iris/Oculus Shader Folder | 1.3.1-forge | modrinth | 客户端 |
| `itemborders` · Item Borders | 1.2.2 | modrinth | 客户端 |
| `itemphysiclite` · ItemPhysic Lite | 1.6.6 | modrinth | 客户端 |
| `jade` · Jade | 11.13.2+forge | modrinth | 双端 |
| `jadeaddons` · Jade Addons | 5.5.1+forge | modrinth | 双端 |
| `jecharacters` · JustEnoughCharacters | 4.6.3 | modrinth | 客户端 |
| `jeed` · Just Enough Effect Descriptions (JEED) | 1.20-2.2.5 | modrinth | 客户端 |
| `jei` · Just Enough Items (JEI) | 15.20.0.130 | modrinth | 双端 |
| `kaleidoscope_compat` · Kaleidoscope Compat | 2.6.5-forge+mc1.20.1-Patch | modrinth | 双端 |
| `kaleidoscope_contraption` · Kaleidoscope Contraption | 1.0.3 | modrinth | 双端 |
| `kaleidoscope_cookery` · Kaleidoscope Cookery | 1.5.1-forge+mc1.20.1 | modrinth | 双端 |
| `kaleidoscope_dim_wine` · Kaleidoscope: Dimensions Wine | 1.1.8-mc1.20.1-forge | modrinth | 双端 |
| `kaleidoscope_end` · Kaleidoscope End | 1.0.14-forge+mc1.20.1 | modrinth | 双端 |
| `kaleidoscope_nether` · Kaleidoscope Nether | 1.1.8-forge+mc1.20.1 | modrinth | 双端 |
| `kaleidoscope_tavern` · Kaleidoscope Tavern | 1.2.0-forge+mc1.20.1 | modrinth | 双端 |
| `kaleidoscope_world_liquor` · Kaleidoscope World Liquor | 1.1.11-forge+mc1.20.1-fix | modrinth | 双端 |
| `kaleidoscopecookery_automation` · Kaleidoscope Cookery Automation - DimensionWorks | 1.0.1-dimensionworks.1 | github-release | 双端 |
| `konkrete` · Konkrete | 1.8.0-1.20-1.20.1-forge | modrinth | 双端 |
| `kotlinforforge` · Kotlin for Forge | 4.12.0 | modrinth | 双端 |
| `kubejs` · KubeJS | 2001.6.5-build.26+forge | modrinth | 双端 |
| `kubejs_create` · KubeJS Create | 2001.3.0-build.8+forge | modrinth | 双端 |
| `kubejs_mekanism` · KubeJS Mekanism UNOFFICIAL | 2001.1.5.1-build.2 | curseforge | 双端 |
| `l2complements` · L2 Complements | 2.6.1 | modrinth | 双端 |
| `l2library` · L2 Library | 2.5.3 | modrinth | 双端 |
| `laowu` · Create: Meowchanics | 2.2.1 | curseforge | 双端 |
| `libipn` · libIPN | forge-1.20-4.0.2 | modrinth | 客户端 |
| `luckperms` · LuckPerms | 5.4.102-forge | modrinth | 服务端 |
| `maid_storage_manager` · Maid Storage Manager | 1.15.6 | modrinth | 双端 |
| `maidtavern` · Maid Tavern | 1.2.0-forge+mc1.20.1 | modrinth | 双端 |
| `maidusehandcrank` · MaidUseHandCrank | 1.6.1 | modrinth | 双端 |
| `megacells` · MEGA Cells | forge-2.4.6 | modrinth | 双端 |
| `mekanism` · Mekanism | 10.4.16.80 | modrinth | 双端 |
| `mekanismdelight` · MekanismDelight | 1.1.3 | modrinth | 双端 |
| `melody` · Melody | 1.0.3-1.20.1-1.20.4-forge | modrinth | 双端 |
| `meplacementtool` · ME Placement Tool | 2.1.4-forge1.20.1 | modrinth | 双端 |
| `minemenu` · Mine Menu | 1.12.3 | modrinth | 客户端 |
| `modernfix` · ModernFix | 5.27.8+mc1.20.1 | modrinth | 双端 |
| `modernui` · Modern UI | 3.12.0.1 | modrinth | 客户端 |
| `mousetweaks` · Mouse Tweaks | 1.20.1-2.25.1-forge | modrinth | 客户端 |
| `netmusic` · Net Music | 1.5.2-forge+mc1.20.1 | modrinth | 双端 |
| `nochatreports` · No Chat Reports | Forge-1.20.1-v2.2.2 | modrinth | 双端 |
| `oculus` · Mekalus (Oculus) | 1.20.1-1.8.0.1 | curseforge | 客户端 |
| `overloadedarmorbar` · Overloaded Armor Bar | 1.20.1-1 | curseforge | 客户端 |
| `patchouli` · Patchouli | 1.20.1-85-forge | modrinth | 双端 |
| `prism` · Prism | 1.0.5 | modrinth | 客户端 |
| `rhino` · Rhino | 2001.2.3-build.10+forge | modrinth | 双端 |
| `searchables` · Searchables | 1.0.3 | modrinth | 客户端 |
| `shulkerboxtooltip` · Shulker Box Tooltip | 4.0.4+1.20.1-forge | modrinth | 客户端 |
| `sliceanddice` · Create Slice & Dice | 3.6.0 | modrinth | 双端 |
| `smsn` · Save My Shaky Network | 1.4.0-1.20.1 | curseforge | 双端 |
| `sodiumextras` · Sodium Extras | forge-1.20.1-1.0.7 | modrinth | 客户端 |
| `sophisticated_backpacks_create_integration` · Sophisticated Backpacks Create Integration | 1.20.1-0.1.10.167 | modrinth | 双端 |
| `sophisticatedbackpacks` · Sophisticated Backpacks | 1.20.1-3.26.3.2157 | modrinth | 双端 |
| `sophisticatedcore` · Sophisticated Core | 1.20.1-1.5.1.2335 | modrinth | 双端 |
| `titanium` · Titanium | 1.20.1-3.8.32 | modrinth | 双端 |
| `touhou_little_maid` · Touhou Little Maid | 1.5.3-forge+mc1.20.1 | modrinth | 双端 |
| `traderefresh` · Trade Refresh | 2.5.1 | modrinth | 双端 |
| `trashslot` · TrashSlot | 15.1.4+forge-1.20.1 | curseforge | 双端 |
| `trueuuid` · TrueUUID | 1.0.9 | modrinth | 双端 |
| `witherstormmod` · Cracker's Wither Storm Mod | 4.2.1 | modrinth | 双端 |
| `xaerominimap` · Xaero's Minimap | forge-1.20.1-25.3.10 | modrinth | 双端 |
| `xaeroworldmap` · Xaero's World Map | forge-1.20.1-1.40.16 | modrinth | 双端 |
| `dimensionworks_item_pickup_guard` · DimensionWorks Item Pickup Guard | 0.1.0 | in-repo | 双端 |
| `industrial_platform` · Industrial Platform | 1.20.1-2.0.1 | curseforge | 双端 |
