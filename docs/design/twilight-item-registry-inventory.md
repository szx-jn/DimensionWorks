# 暮色森林完整注册物品清单

日期：2026-11-14。范围：整合包固定版本 Twilight Forest **4.3.2508**（Minecraft 1.20.1 Forge），共 **562 个不同物品 ID**。包含方块物品、装备、工具、材料、食物、特殊道具和刷怪蛋；不把方块状态、模型变体或没有物品注册的技术方块算成物品。

[完整 CSV](twilight-item-registry-inventory.csv) 保留序号、完整 ID、中英文名称、注册来源、中文语言键和模型路径，可用于后续筛选合成材料。

## 范围与静态核对依据

- 版本来源：[manifest/mods.json](../../manifest/mods.json)，CurseForge 文件 ID `5468648`。
- 核对产物：`twilightforest-1.20.1-4.3.2508-universal.jar`，SHA-256：`0bdc89263616d1b35c32ef82c5e9c14cbd20368e2fe8b468c72a28320be7a778`。
- 用本地 JDK `javap -c -p` 检查固定 JAR 的 `TFItems`、`TFBlocks`、`TFEntities` 和 `TFEntityNames` 注册路径；不以移动源码分支或物品模型目录代替注册表。
- `TFItems` 显式注册 204 项，其中包含部分特殊方块物品；`TFBlocks` 的五类方块物品辅助注册方法另注册 305 项。两组 ID 没有重复。
- `TFEntities` 的刷怪蛋注册要求两个颜色参数均非零；符合条件的 53 项计入清单，`buildNoEgg` 及零颜色的实体不生成物品条目。
- 中文名称优先采用 [整合包汉化](../../mods/dimensionworks-localization/assets/twilightforest/lang/zh_cn.json)，其余取固定 JAR 自带 `zh_cn.json`；英文取其 `en_us.json`。空心原木物品按 `HollowLogItem` 使用的竖直原木方块语言键取名。
- 562 项 ID 唯一，均能对应固定 JAR 中同 ID 的物品模型，并具有中英文名称。这是静态注册路径与资源核对，尚未通过游戏运行时导出注册表。

清单只说明物品存在，不表示每项都有生存获取方式或适合用于配方。包含创造模式和特殊用途物品；附属模组、整合包专属物品及原版物品不属于本表范围。旧 [物品模型清单](twilight-item-model-inventory.csv) 继续作为模型调查资料，不能用于计算实际物品总数。

目前只盘点全部物品，不选择弹跳封带的暮色材料，不确定合成数量或工艺。其玩法范围见 [弹力打包工具规格](twilight-easter-egg-toys.md)。

## 注册来源汇总

| 注册来源 | 数量 | 包含内容 |
| --- | ---: | --- |
| TFItems 显式物品注册 | 204 | 材料、食物、装备、工具、特殊道具，以及奖杯、空心原木等部分特殊方块物品 |
| TFBlocks 方块物品注册 | 305 | 经方块辅助方法同时注册的方块物品 |
| TFEntities 刷怪蛋注册 | 53 | 实际满足注册条件的刷怪蛋 |
| 合计 | **562** | 每个完整物品 ID 只列一次 |

## 完整列表

以下各组按物品 ID 排序；序号与 CSV 一致。

### TFItems 显式物品注册（204 项）

| 序号 | 中文名称 | 英文名称 | 物品 ID |
| ---: | --- | --- | --- |
| 1 | 旗帜图案 | Banner Pattern | `twilightforest:alpha_yeti_banner_pattern` |
| 2 | 雪怪首领毛皮 | Alpha Yeti Fur | `twilightforest:alpha_yeti_fur` |
| 3 | 雪怪首领奖杯 | Alpha Yeti Trophy | `twilightforest:alpha_yeti_trophy` |
| 4 | 极地靴子 | Arctic Boots | `twilightforest:arctic_boots` |
| 5 | 极地皮衣 | Arctic Jacket | `twilightforest:arctic_chestplate` |
| 6 | 极地毛皮 | Arctic Fur | `twilightforest:arctic_fur` |
| 7 | 极地风帽 | Arctic Hood | `twilightforest:arctic_helmet` |
| 8 | 极地护腿 | Arctic Leggings | `twilightforest:arctic_leggings` |
| 9 | 装甲碎片 | Armor Shard | `twilightforest:armor_shard` |
| 10 | 装甲碎片堆 | Armor Shard Cluster | `twilightforest:armor_shard_cluster` |
| 11 | 链锤 | Block and Chain | `twilightforest:block_and_chain` |
| 12 | 蛀虫精华 | Borer Essence | `twilightforest:borer_essence` |
| 13 | 易碎药水瓶 | Brittle Potion Flask | `twilightforest:brittle_potion_flask` |
| 14 | 苍穹木船 | Canopy Boat | `twilightforest:canopy_boat` |
| 15 | 苍穹木运输船 | Canopy Chest Boat | `twilightforest:canopy_chest_boat` |
| 16 | 悬挂式苍穹木告示牌 | Canopy Hanging Sign | `twilightforest:canopy_hanging_sign` |
| 17 | 苍穹木告示牌 | Canopy Sign | `twilightforest:canopy_sign` |
| 18 | 砷铅铁矿石 | Carminite | `twilightforest:carminite` |
| 19 | 保管符咒 I | Charm of Keeping I | `twilightforest:charm_of_keeping_1` |
| 20 | 保管符咒 II | Charm of Keeping II | `twilightforest:charm_of_keeping_2` |
| 21 | 保管符咒 III | Charm of Keeping III | `twilightforest:charm_of_keeping_3` |
| 22 | 生命符咒 I | Charm of Life I | `twilightforest:charm_of_life_1` |
| 23 | 生命符咒 II | Charm of Life II | `twilightforest:charm_of_life_2` |
| 24 | 蝉 | Cicada | `twilightforest:cicada` |
| 25 | 牛头人肉排 | Meef Steak | `twilightforest:cooked_meef` |
| 26 | 鹿肉排 | Venison Steak | `twilightforest:cooked_venison` |
| 27 | 苦力怕头颅烛台 | Creeper Skull Candle | `twilightforest:creeper_skull_candle` |
| 28 | 瓦解之号角 | Crumble Horn | `twilightforest:crumble_horn` |
| 29 | 湮灭立方 | Cube of Annihilation | `twilightforest:cube_of_annihilation` |
| 30 | 立方护符 | Talisman of the Cube | `twilightforest:cube_talisman` |
| 31 | 黑木船 | Darkwood Boat | `twilightforest:dark_boat` |
| 32 | 黑木运输船 | Darkwood Chest Boat | `twilightforest:dark_chest_boat` |
| 33 | 悬挂式黑木告示牌 | Darkwood Hanging Sign | `twilightforest:dark_hanging_sign` |
| 34 | 黑木告示牌 | Darkwood Sign | `twilightforest:dark_sign` |
| 35 | 钻石米诺陶战斧 | Diamond Minotaur Axe | `twilightforest:diamond_minotaur_axe` |
| 36 | 末影弓 | Ender Bow | `twilightforest:ender_bow` |
| 37 | 试验物品115 | Experiment 115 | `twilightforest:experiment_115` |
| 38 | 落叶 | Fallen Leaves | `twilightforest:fallen_leaves` |
| 39 | 炽热的血液 | Fiery Blood | `twilightforest:fiery_blood` |
| 40 | 炽铁靴子 | Fiery Boots | `twilightforest:fiery_boots` |
| 41 | 炽铁胸甲 | Fiery Chestplate | `twilightforest:fiery_chestplate` |
| 42 | 炽铁头盔 | Fiery Helmet | `twilightforest:fiery_helmet` |
| 43 | 炽铁锭 | Fiery Ingot | `twilightforest:fiery_ingot` |
| 44 | 炽铁护腿 | Fiery Leggings | `twilightforest:fiery_leggings` |
| 45 | 炽铁镐 | Fiery Pickaxe | `twilightforest:fiery_pickaxe` |
| 46 | 炽铁剑 | Fiery Sword | `twilightforest:fiery_sword` |
| 47 | 炽热之泪 | Fiery Tears | `twilightforest:fiery_tears` |
| 48 | 魔法地图 | Magic Map | `twilightforest:filled_magic_map` |
| 49 | 迷宫地图 | Maze Map | `twilightforest:filled_maze_map` |
| 50 | 迷宫/矿石地图 | Maze/Ore Map | `twilightforest:filled_ore_map` |
| 51 | 萤火虫 | Firefly | `twilightforest:firefly` |
| 52 | 护盾权杖 | Scepter of Fortification | `twilightforest:fortification_scepter` |
| 53 | 巨人镐 | Giant's Pickaxe | `twilightforest:giant_pickaxe` |
| 54 | 巨人剑 | Giant's Sword | `twilightforest:giant_sword` |
| 55 | 玻璃剑 | Glass Sword | `twilightforest:glass_sword` |
| 56 | 金米诺陶战斧 | Golden Minotaur Axe | `twilightforest:gold_minotaur_axe` |
| 57 | 大型药水瓶 | Greater Potion Flask | `twilightforest:greater_potion_flask` |
| 58 | 空心金合欢原木 | Hollow Acacia Log | `twilightforest:hollow_acacia_log` |
| 59 | 空心白桦原木 | Hollow Birch Log | `twilightforest:hollow_birch_log` |
| 60 | 空心苍穹木原木 | Hollow Canopy Tree Log | `twilightforest:hollow_canopy_log` |
| 61 | 空心樱桃原木 | Hollow Cherry Log | `twilightforest:hollow_cherry_log` |
| 62 | 空心绯红菌柄 | Hollow Crimson Stem | `twilightforest:hollow_crimson_stem` |
| 63 | 空心黑木原木 | Hollow Darkwood Log | `twilightforest:hollow_dark_log` |
| 64 | 空心深色橡木原木 | Hollow Dark Oak Log | `twilightforest:hollow_dark_oak_log` |
| 65 | 空心丛林原木 | Hollow Jungle Log | `twilightforest:hollow_jungle_log` |
| 66 | 空心红树原木 | Hollow Mangrove Log | `twilightforest:hollow_mangrove_log` |
| 67 | 空心矿石原木 | Hollow Minewood Log | `twilightforest:hollow_mining_log` |
| 68 | 空心橡木原木 | Hollow Oak Log | `twilightforest:hollow_oak_log` |
| 69 | 空心分类原木 | Hollow Sortingwood Log | `twilightforest:hollow_sorting_log` |
| 70 | 空心云杉原木 | Hollow Spruce Log | `twilightforest:hollow_spruce_log` |
| 71 | 空心时光原木 | Hollow Timewood Log | `twilightforest:hollow_time_log` |
| 72 | 空心变化原木 | Hollow Transwood Log | `twilightforest:hollow_transformation_log` |
| 73 | 空心暮色橡木原木 | Hollow Twilight Oak Log | `twilightforest:hollow_twilight_oak_log` |
| 74 | 空心红树原木 | Hollow Mangrove Log | `twilightforest:hollow_vangrove_log` |
| 75 | 空心诡异菌柄 | Hollow Warped Stem | `twilightforest:hollow_warped_stem` |
| 76 | 巨型睡莲 | Huge Lily Pad | `twilightforest:huge_lily_pad` |
| 77 | 巨型荷花 | Huge Water Lily | `twilightforest:huge_water_lily` |
| 78 | 旗帜图案 | Banner Pattern | `twilightforest:hydra_banner_pattern` |
| 79 | 九头蛇肉排 | Hydra Chop | `twilightforest:hydra_chop` |
| 80 | 九头蛇奖杯 | Hydra Trophy | `twilightforest:hydra_trophy` |
| 81 | 冰霜炸弹 | Ice Bomb | `twilightforest:ice_bomb` |
| 82 | 寒冰弓 | Ice Bow | `twilightforest:ice_bow` |
| 83 | 寒冰剑 | Ice Sword | `twilightforest:ice_sword` |
| 84 | 铁木斧 | Ironwood Axe | `twilightforest:ironwood_axe` |
| 85 | 铁木靴子 | Ironwood Boots | `twilightforest:ironwood_boots` |
| 86 | 铁木胸甲 | Ironwood Chestplate | `twilightforest:ironwood_chestplate` |
| 87 | 铁木头盔 | Ironwood Helmet | `twilightforest:ironwood_helmet` |
| 88 | 铁木锄 | Ironwood Hoe | `twilightforest:ironwood_hoe` |
| 89 | 铁木锭 | Ironwood Ingot | `twilightforest:ironwood_ingot` |
| 90 | 铁木护腿 | Ironwood Leggings | `twilightforest:ironwood_leggings` |
| 91 | 铁木镐 | Ironwood Pickaxe | `twilightforest:ironwood_pickaxe` |
| 92 | 铁木锹 | Ironwood Shovel | `twilightforest:ironwood_shovel` |
| 93 | 铁木剑 | Ironwood Sword | `twilightforest:ironwood_sword` |
| 94 | 旗帜图案 | Banner Pattern | `twilightforest:knight_phantom_banner_pattern` |
| 95 | 幻影骑士奖杯 | Knight Phantom Trophy | `twilightforest:knight_phantom_trophy` |
| 96 | 骑士斧 | Knightmetal Axe | `twilightforest:knightmetal_axe` |
| 97 | 骑士靴子 | Knightmetal Boots | `twilightforest:knightmetal_boots` |
| 98 | 骑士胸甲 | Knightmetal Chestplate | `twilightforest:knightmetal_chestplate` |
| 99 | 骑士头盔 | Knightmetal Helmet | `twilightforest:knightmetal_helmet` |
| 100 | 骑士金属锭 | Knightmetal Ingot | `twilightforest:knightmetal_ingot` |
| 101 | 骑士护腿 | Knightmetal Greaves | `twilightforest:knightmetal_leggings` |
| 102 | 骑士镐 | Knightmetal Pickaxe | `twilightforest:knightmetal_pickaxe` |
| 103 | 骑士金属环 | Knightmetal Loop | `twilightforest:knightmetal_ring` |
| 104 | 骑士盾牌 | Knightmetal Shield | `twilightforest:knightmetal_shield` |
| 105 | 骑士剑 | Knightmetal Sword | `twilightforest:knightmetal_sword` |
| 106 | 余烬之灯 | Lamp of Cinders | `twilightforest:lamp_of_cinders` |
| 107 | 旗帜图案 | Banner Pattern | `twilightforest:lich_banner_pattern` |
| 108 | 巫妖奖杯 | Lich Trophy | `twilightforest:lich_trophy` |
| 109 | 吸血权杖 | Scepter of Life Draining | `twilightforest:lifedrain_scepter` |
| 110 | 活根 | Liveroot | `twilightforest:liveroot` |
| 111 | 魔豆 | Magic Beans | `twilightforest:magic_beans` |
| 112 | 空白魔法地图 | Blank Magic Map | `twilightforest:magic_map` |
| 113 | 魔法地图核心 | Magic Map Focus | `twilightforest:magic_map_focus` |
| 114 | 魔法画作 | Magic Painting | `twilightforest:magic_painting` |
| 115 | 红树木船 | Mangrove Boat | `twilightforest:mangrove_boat` |
| 116 | 红树木运输船 | Mangrove Chest Boat | `twilightforest:mangrove_chest_boat` |
| 117 | 悬挂式红树木告示牌 | Mangrove Hanging Sign | `twilightforest:mangrove_hanging_sign` |
| 118 | 红树木告示牌 | Mangrove Sign | `twilightforest:mangrove_sign` |
| 119 | 空白迷宫地图 | Blank Maze Map | `twilightforest:maze_map` |
| 120 | 迷宫地图核心 | Maze Map Focus | `twilightforest:maze_map_focus` |
| 121 | 迷宫薄饼 | Maze Wafer | `twilightforest:maze_wafer` |
| 122 | 迷宫破坏者 | Mazebreaker | `twilightforest:mazebreaker_pickaxe` |
| 123 | 牛头人沙拉酱肉 | Meef Stroganoff | `twilightforest:meef_stroganoff` |
| 124 | 矿石木船 | Minewood Boat | `twilightforest:mining_boat` |
| 125 | 矿石木运输船 | Minewood Chest Boat | `twilightforest:mining_chest_boat` |
| 126 | 悬挂式矿石木告示牌 | Minewood Hanging Sign | `twilightforest:mining_hanging_sign` |
| 127 | 矿石木告示牌 | Minewood Sign | `twilightforest:mining_sign` |
| 128 | 旗帜图案 | Banner Pattern | `twilightforest:minoshroom_banner_pattern` |
| 129 | 米诺菇奖杯 | Minoshroom Trophy | `twilightforest:minoshroom_trophy` |
| 130 | 月相罗盘 | Moon Dial | `twilightforest:moon_dial` |
| 131 | 月光蠕虫 | Moonworm | `twilightforest:moonworm` |
| 132 | 月光蠕虫女王 | Moonworm Queen | `twilightforest:moonworm_queen` |
| 133 | 音乐唱片 | Music Disc | `twilightforest:music_disc_findings` |
| 134 | 音乐唱片 | Music Disc | `twilightforest:music_disc_home` |
| 135 | 音乐唱片 | Music Disc | `twilightforest:music_disc_maker` |
| 136 | 音乐唱片 | Music Disc | `twilightforest:music_disc_motion` |
| 137 | 音乐唱片 | Music Disc | `twilightforest:music_disc_radiance` |
| 138 | 音乐唱片 | Music Disc | `twilightforest:music_disc_steps` |
| 139 | 音乐唱片 | Music Disc | `twilightforest:music_disc_superstitious` |
| 140 | 音乐唱片 | Music Disc | `twilightforest:music_disc_thread` |
| 141 | 音乐唱片 | Music Disc | `twilightforest:music_disc_wayfarer` |
| 142 | 旗帜图案 | Banner Pattern | `twilightforest:naga_banner_pattern` |
| 143 | 娜迦胸甲 | Naga Scale Tunic | `twilightforest:naga_chestplate` |
| 144 | 娜迦护腿 | Naga Scale Leggings | `twilightforest:naga_leggings` |
| 145 | 娜迦鳞片 | Naga Scale | `twilightforest:naga_scale` |
| 146 | 娜迦奖杯 | Naga Trophy | `twilightforest:naga_trophy` |
| 147 | 矿石磁铁 | Ore Magnet | `twilightforest:ore_magnet` |
| 148 | 空白迷宫/矿石地图 | Blank Maze/Ore Map | `twilightforest:ore_map` |
| 149 | 矿石感应仪 | Ore Meter | `twilightforest:ore_meter` |
| 150 | 孔雀羽扇 | Peacock Feather Fan | `twilightforest:peacock_feather_fan` |
| 151 | 幻影胸甲 | Phantom Chestplate | `twilightforest:phantom_chestplate` |
| 152 | 幻影头盔 | Phantom Helmet | `twilightforest:phantom_helmet` |
| 153 | 猪灵头颅烛台 | Piglin Skull Candle | `twilightforest:piglin_skull_candle` |
| 154 | 头颅烛台 | Player Skull Candle | `twilightforest:player_skull_candle` |
| 155 | 旗帜图案 | Banner Pattern | `twilightforest:quest_ram_banner_pattern` |
| 156 | 谜题羊奖杯 | Quest Ram Trophy | `twilightforest:quest_ram_trophy` |
| 157 | 渡鸦羽毛 | Raven Feather | `twilightforest:raven_feather` |
| 158 | 粗铁木 | Raw Ironwood | `twilightforest:raw_ironwood` |
| 159 | 生牛头人肉 | Raw Meef | `twilightforest:raw_meef` |
| 160 | 生鹿肉 | Raw Venison | `twilightforest:raw_venison` |
| 161 | 追踪弓 | Seeker Bow | `twilightforest:seeker_bow` |
| 162 | 骷髅头烛台 | Skeleton Skull Candle | `twilightforest:skeleton_skull_candle` |
| 163 | 旗帜图案 | Banner Pattern | `twilightforest:snow_queen_banner_pattern` |
| 164 | 冰雪女王奖杯 | Snow Queen Trophy | `twilightforest:snow_queen_trophy` |
| 165 | 分类木船 | Sortingwood Boat | `twilightforest:sorting_boat` |
| 166 | 分类木运输船 | Sortingwood Chest Boat | `twilightforest:sorting_chest_boat` |
| 167 | 悬挂式分类木告示牌 | Sortingwood Hanging Sign | `twilightforest:sorting_hanging_sign` |
| 168 | 分类木告示牌 | Sortingwood Sign | `twilightforest:sorting_sign` |
| 169 | 钢叶斧 | Steeleaf Axe | `twilightforest:steeleaf_axe` |
| 170 | 钢叶靴子 | Steeleaf Boots | `twilightforest:steeleaf_boots` |
| 171 | 钢叶胸甲 | Steeleaf Chestplate | `twilightforest:steeleaf_chestplate` |
| 172 | 钢叶头盔 | Steeleaf Helmet | `twilightforest:steeleaf_helmet` |
| 173 | 钢叶锄 | Steeleaf Hoe | `twilightforest:steeleaf_hoe` |
| 174 | 钢叶 | Steeleaf | `twilightforest:steeleaf_ingot` |
| 175 | 钢叶护腿 | Steeleaf Leggings | `twilightforest:steeleaf_leggings` |
| 176 | 钢叶镐 | Steeleaf Pickaxe | `twilightforest:steeleaf_pickaxe` |
| 177 | 钢叶锹 | Steeleaf Shovel | `twilightforest:steeleaf_shovel` |
| 178 | 钢叶剑 | Steeleaf Sword | `twilightforest:steeleaf_sword` |
| 179 | 时光木船 | Timewood Boat | `twilightforest:time_boat` |
| 180 | 时光木运输船 | Timewood Chest Boat | `twilightforest:time_chest_boat` |
| 181 | 悬挂式时光木告示牌 | Timewood Hanging Sign | `twilightforest:time_hanging_sign` |
| 182 | 时光木告示牌 | Timewood Sign | `twilightforest:time_sign` |
| 183 | 火炬浆果 | Torchberries | `twilightforest:torchberries` |
| 184 | 塔钥匙 | Tower Key | `twilightforest:tower_key` |
| 185 | 变化木船 | Transwood Boat | `twilightforest:transformation_boat` |
| 186 | 变化木运输船 | Transwood Chest Boat | `twilightforest:transformation_chest_boat` |
| 187 | 悬挂式变化木告示牌 | Transwood Hanging Sign | `twilightforest:transformation_hanging_sign` |
| 188 | 转换粉 | Transformation Powder | `twilightforest:transformation_powder` |
| 189 | 变化木告示牌 | Transwood Sign | `twilightforest:transformation_sign` |
| 190 | 三发弓 | Tri-Bow | `twilightforest:triple_bow` |
| 191 | 暮色橡木船 | Twilight Oak Boat | `twilightforest:twilight_oak_boat` |
| 192 | 暮色橡木运输船 | Twilight Oak Chest Boat | `twilightforest:twilight_oak_chest_boat` |
| 193 | 悬挂式暮色橡木告示牌 | Twilight Oak Hanging Sign | `twilightforest:twilight_oak_hanging_sign` |
| 194 | 暮色橡木告示牌 | Twilight Oak Sign | `twilightforest:twilight_oak_sign` |
| 195 | 黄昏权杖 | Scepter of Twilight | `twilightforest:twilight_scepter` |
| 196 | 旗帜图案 | Banner Pattern | `twilightforest:ur_ghast_banner_pattern` |
| 197 | 暮初恶魂奖杯 | Ur-Ghast Trophy | `twilightforest:ur_ghast_trophy` |
| 198 | 凋灵骷髅头烛台 | Wither Skeleton Skull Candle | `twilightforest:wither_skeleton_skull_candle` |
| 199 | 雪怪靴子 | Yeti Boots | `twilightforest:yeti_boots` |
| 200 | 雪怪皮衣 | Yeti Jacket | `twilightforest:yeti_chestplate` |
| 201 | 雪怪角盔 | Yeti Horned Helm | `twilightforest:yeti_helmet` |
| 202 | 雪怪护腿 | Yeti Leggings | `twilightforest:yeti_leggings` |
| 203 | 僵尸权杖 | Zombie Scepter | `twilightforest:zombie_scepter` |
| 204 | 僵尸头颅烛台 | Zombie Skull Candle | `twilightforest:zombie_skull_candle` |

### TFBlocks 方块物品注册（305 项）

| 序号 | 中文名称 | 英文名称 | 物品 ID |
| ---: | --- | --- | --- |
| 205 | 金合欢木栏杆 | Acacia Banister | `twilightforest:acacia_banister` |
| 206 | 雪怪首领Boss刷怪笼 | Alpha Yeti Boss Spawner | `twilightforest:alpha_yeti_boss_spawner` |
| 207 | 反建器 | Antibuilder | `twilightforest:antibuilder` |
| 208 | 极地毛皮块 | Block of Arctic Fur | `twilightforest:arctic_fur_block` |
| 209 | 极光方块 | Aurora Block | `twilightforest:aurora_block` |
| 210 | 极光柱 | Aurora Pillar | `twilightforest:aurora_pillar` |
| 211 | 极光台阶 | Aurora Slab | `twilightforest:aurora_slab` |
| 212 | 极光玻璃 | Auroralized Glass | `twilightforest:auroralized_glass` |
| 213 | 竹栏杆 | Bamboo Banister | `twilightforest:bamboo_banister` |
| 214 | 豆茎叶 | Beanstalk Leaves | `twilightforest:beanstalk_leaves` |
| 215 | 白桦木栏杆 | Birch Banister | `twilightforest:birch_banister` |
| 216 | 蓝色城堡门 | Blue Castle Door | `twilightforest:blue_castle_door` |
| 217 | 蓝色城堡符文砖 | Blue Castle Rune Brick | `twilightforest:blue_castle_rune_brick` |
| 218 | 蓝色力场 | Blue Force Field | `twilightforest:blue_force_field` |
| 219 | 粗纹城堡砖柱 | Bold Castle Brick Pillar | `twilightforest:bold_castle_brick_pillar` |
| 220 | 粗纹城堡砖楼梯 | Bold Castle Brick Stairs | `twilightforest:bold_castle_brick_stairs` |
| 221 | 粗纹城堡砖瓦 | Bold Castle Brick Tile | `twilightforest:bold_castle_brick_tile` |
| 222 | 石巨柱 | Bold Stone Pillar | `twilightforest:bold_stone_pillar` |
| 223 | 荆棘 | Thorns | `twilightforest:brown_thorns` |
| 224 | 烧焦荆棘 | Burnt Thorns | `twilightforest:burnt_thorns` |
| 225 | 烛台 | Candelabra | `twilightforest:candelabra` |
| 226 | 苍穹木栏杆 | Canopy Banister | `twilightforest:canopy_banister` |
| 227 | 苍穹木书架 | Canopy Bookshelf | `twilightforest:canopy_bookshelf` |
| 228 | 苍穹木按钮 | Canopy Button | `twilightforest:canopy_button` |
| 229 | 苍穹木箱子 | Canopy Chest | `twilightforest:canopy_chest` |
| 230 | 苍穹木门 | Canopy Door | `twilightforest:canopy_door` |
| 231 | 苍穹木栅栏 | Canopy Fence | `twilightforest:canopy_fence` |
| 232 | 苍穹木栅栏门 | Canopy Fence Gate | `twilightforest:canopy_fence_gate` |
| 233 | 苍穹树叶 | Canopy Tree Leaves | `twilightforest:canopy_leaves` |
| 234 | 苍穹原木 | Canopy Tree Log | `twilightforest:canopy_log` |
| 235 | 苍穹木板 | Canopy Planks | `twilightforest:canopy_planks` |
| 236 | 苍穹木压力板 | Canopy Pressure Plate | `twilightforest:canopy_pressure_plate` |
| 237 | 苍穹树苗 | Canopy Tree Sapling | `twilightforest:canopy_sapling` |
| 238 | 苍穹木台阶 | Canopy Slab | `twilightforest:canopy_slab` |
| 239 | 苍穹木楼梯 | Canopy Stairs | `twilightforest:canopy_stairs` |
| 240 | 苍穹木活板门 | Canopy Trapdoor | `twilightforest:canopy_trapdoor` |
| 241 | 苍穹木 | Canopy Tree Wood | `twilightforest:canopy_wood` |
| 242 | 砷铅铁块 | Block of Carminite | `twilightforest:carminite_block` |
| 243 | 砷铅铁建造器 | Carminite Builder | `twilightforest:carminite_builder` |
| 244 | 砷铅铁反应堆 | Carminite Reactor | `twilightforest:carminite_reactor` |
| 245 | 城堡砖 | Castle Brick | `twilightforest:castle_brick` |
| 246 | 城堡砖楼梯 | Castle Brick Stairs | `twilightforest:castle_brick_stairs` |
| 247 | 城堡顶瓦 | Castle Roof Tile | `twilightforest:castle_roof_tile` |
| 248 | 樱花木栏杆 | Cherry Banister | `twilightforest:cherry_banister` |
| 249 | 蝉罐子 | Cicada Jar | `twilightforest:cicada_jar` |
| 250 | 余烬熔炉 | Cinder Furnace | `twilightforest:cinder_furnace` |
| 251 | 余烬原木 | Cinder Log | `twilightforest:cinder_log` |
| 252 | 余烬木 | Cinder Wood | `twilightforest:cinder_wood` |
| 253 | 三叶草 | Clover Patch | `twilightforest:clover_patch` |
| 254 | 裂纹城堡砖 | Cracked Castle Brick | `twilightforest:cracked_castle_brick` |
| 255 | 裂纹城堡砖楼梯 | Cracked Castle Brick Stairs | `twilightforest:cracked_castle_brick_stairs` |
| 256 | 裂纹死岩 | Cracked Deadrock | `twilightforest:cracked_deadrock` |
| 257 | 裂纹蚀刻娜迦石 | Cracked Etched Nagastone | `twilightforest:cracked_etched_nagastone` |
| 258 | 裂纹迷宫石砖 | Cracked Mazestone Brick | `twilightforest:cracked_mazestone` |
| 259 | 裂纹娜迦石柱 | Cracked Nagastone Pillar | `twilightforest:cracked_nagastone_pillar` |
| 260 | 裂纹娜迦石楼梯(左) | Cracked Nagastone Stairs (Left) | `twilightforest:cracked_nagastone_stairs_left` |
| 261 | 裂纹娜迦石楼梯(右) | Cracked Nagastone Stairs (Right) | `twilightforest:cracked_nagastone_stairs_right` |
| 262 | 破裂塔木木板 | Cracked Towerwood Planks | `twilightforest:cracked_towerwood` |
| 263 | 裂纹地下砖 | Cracked Underbrick | `twilightforest:cracked_underbrick` |
| 264 | 绯红木栏杆 | Crimson Banister | `twilightforest:crimson_banister` |
| 265 | 錾制迷宫石 | Cut Mazestone | `twilightforest:cut_mazestone` |
| 266 | 黑木栏杆 | Darkwood Banister | `twilightforest:dark_banister` |
| 267 | 黑木按钮 | Darkwood Button | `twilightforest:dark_button` |
| 268 | 黑木箱子 | Darkwood Chest | `twilightforest:dark_chest` |
| 269 | 黑木门 | Darkwood Door | `twilightforest:dark_door` |
| 270 | 黑木栅栏 | Darkwood Fence | `twilightforest:dark_fence` |
| 271 | 黑木栅栏门 | Darkwood Fence Gate | `twilightforest:dark_fence_gate` |
| 272 | 黑树树叶 | Darkwood Leaves | `twilightforest:dark_leaves` |
| 273 | 黑木原木 | Darkwood Log | `twilightforest:dark_log` |
| 274 | 深色橡木栏杆 | Dark Oak Banister | `twilightforest:dark_oak_banister` |
| 275 | 黑木木板 | Darkwood Planks | `twilightforest:dark_planks` |
| 276 | 黑木压力板 | Darkwood Pressure Plate | `twilightforest:dark_pressure_plate` |
| 277 | 黑木台阶 | Darkwood Slab | `twilightforest:dark_slab` |
| 278 | 黑木楼梯 | Darkwood Stairs | `twilightforest:dark_stairs` |
| 279 | 黑木活板门 | Darkwood Trapdoor | `twilightforest:dark_trapdoor` |
| 280 | 黑木 | Darkwood Wood | `twilightforest:dark_wood` |
| 281 | 黑树树苗 | Darkwood Sapling | `twilightforest:darkwood_sapling` |
| 282 | 死岩 | Deadrock | `twilightforest:deadrock` |
| 283 | 死灵书柜 | Death Tome Spawner | `twilightforest:death_tome_spawner` |
| 284 | 装饰迷宫石砖 | Decorative Mazestone Brick | `twilightforest:decorative_mazestone` |
| 285 | 空苍穹书架 | Empty Canopy Bookshelf | `twilightforest:empty_canopy_bookshelf` |
| 286 | 黑纹城堡砖柱 | Encased Castle Brick Pillar | `twilightforest:encased_castle_brick_pillar` |
| 287 | 黑纹城堡砖楼梯 | Encased Castle Brick Stairs | `twilightforest:encased_castle_brick_stairs` |
| 288 | 黑纹城堡砖瓦 | Encased Castle Brick Tile | `twilightforest:encased_castle_brick_tile` |
| 289 | 封装射焰方块 | Encased Fire Jet | `twilightforest:encased_fire_jet` |
| 290 | 封装烟雾方块 | Encased Smoker | `twilightforest:encased_smoker` |
| 291 | 封装塔木木板 | Encased Towerwood Planks | `twilightforest:encased_towerwood` |
| 292 | 蚀刻娜迦石 | Etched Nagastone | `twilightforest:etched_nagastone` |
| 293 | 暮色蕨 | Fiddlehead Fern | `twilightforest:fiddlehead` |
| 294 | 炽铁块 | Block of Fiery Metal | `twilightforest:fiery_block` |
| 295 | 高原首领Boss刷怪笼 | Final Boss Boss Spawner | `twilightforest:final_boss_boss_spawner` |
| 296 | 射焰方块 | Fire Jet | `twilightforest:fire_jet` |
| 297 | 萤火虫罐子 | Firefly Jar | `twilightforest:firefly_jar` |
| 298 | 萤火粒子罐 | Firefly Particle Spawner | `twilightforest:firefly_particle_spawner` |
| 299 | 浮云 | Fluffy Cloud | `twilightforest:fluffy_cloud` |
| 300 | 恶魂陷阱 | Ghast Trap | `twilightforest:ghast_trap` |
| 301 | 巨型圆石 | Giant Cobblestone | `twilightforest:giant_cobblestone` |
| 302 | 巨型树叶 | Giant Leaves | `twilightforest:giant_leaves` |
| 303 | 巨型原木 | Giant Log | `twilightforest:giant_log` |
| 304 | 巨型黑曜石 | Giant Obsidian | `twilightforest:giant_obsidian` |
| 305 | 绿色力场 | Green Force Field | `twilightforest:green_force_field` |
| 306 | 翠绿荆棘 | Green Thorns | `twilightforest:green_thorns` |
| 307 | 树篱 | Hedge | `twilightforest:hedge` |
| 308 | 高大暮色橡树树苗 | Robust Twilight Oak Sapling | `twilightforest:hollow_oak_sapling` |
| 309 | 巨型荧光蘑菇块 | Huge Mushgloom Block | `twilightforest:huge_mushgloom` |
| 310 | 巨型荧光蘑菇柄 | Huge Mushgloom Stem | `twilightforest:huge_mushgloom_stem` |
| 311 | 巨茎 | Huge Stalk | `twilightforest:huge_stalk` |
| 312 | 九头蛇Boss刷怪笼 | Hydra Boss Spawner | `twilightforest:hydra_boss_spawner` |
| 313 | 虫蛀塔木木板 | Infested Towerwood Planks | `twilightforest:infested_towerwood` |
| 314 | 铁梯子 | Iron Ladder | `twilightforest:iron_ladder` |
| 315 | 铁木块 | Block of Ironwood | `twilightforest:ironwood_block` |
| 316 | 丛林木栏杆 | Jungle Banister | `twilightforest:jungle_banister` |
| 317 | 遗物匣 | Keepsake Casket | `twilightforest:keepsake_casket` |
| 318 | 幻影骑士Boss刷怪笼 | Knight Phantom Boss Spawner | `twilightforest:knight_phantom_boss_spawner` |
| 319 | 骑士金属块 | Block of Knightmetal | `twilightforest:knightmetal_block` |
| 320 | 巫妖Boss刷怪笼 | Lich Boss Spawner | `twilightforest:lich_boss_spawner` |
| 321 | 巫妖怪塔微缩雕塑 | Miniature Lich Tower | `twilightforest:lich_tower_miniature_structure` |
| 322 | 活根 | Liveroots | `twilightforest:liveroot_block` |
| 323 | 锁定的消失方块 | Locked Vanishing Block | `twilightforest:locked_vanishing_block` |
| 324 | 红树木栏杆 | Mangrove Banister | `twilightforest:mangrove_banister` |
| 325 | 红树木按钮 | Mangrove Button | `twilightforest:mangrove_button` |
| 326 | 红树木箱子 | Mangrove Chest | `twilightforest:mangrove_chest` |
| 327 | 红树木门 | Mangrove Door | `twilightforest:mangrove_door` |
| 328 | 红树木栅栏 | Mangrove Fence | `twilightforest:mangrove_fence` |
| 329 | 红树木栅栏门 | Mangrove Fence Gate | `twilightforest:mangrove_fence_gate` |
| 330 | 红树树叶 | Mangrove Leaves | `twilightforest:mangrove_leaves` |
| 331 | 红树原木 | Mangrove Log | `twilightforest:mangrove_log` |
| 332 | 红树木板 | Mangrove Planks | `twilightforest:mangrove_planks` |
| 333 | 红树木压力板 | Mangrove Pressure Plate | `twilightforest:mangrove_pressure_plate` |
| 334 | 红树根 | Mangrove Roots | `twilightforest:mangrove_root` |
| 335 | 红树树苗 | Mangrove Sapling | `twilightforest:mangrove_sapling` |
| 336 | 红树木台阶 | Mangrove Slab | `twilightforest:mangrove_slab` |
| 337 | 红树木楼梯 | Mangrove Stairs | `twilightforest:mangrove_stairs` |
| 338 | 红树木活板门 | Mangrove Trapdoor | `twilightforest:mangrove_trapdoor` |
| 339 | 红树木 | Mangrove Wood | `twilightforest:mangrove_wood` |
| 340 | 鬼臼果 | Mayapple | `twilightforest:mayapple` |
| 341 | 迷宫石 | Mazestone | `twilightforest:mazestone` |
| 342 | 迷宫石沿 | Mazestone Border | `twilightforest:mazestone_border` |
| 343 | 迷宫石砖 | Mazestone Brick | `twilightforest:mazestone_brick` |
| 344 | 迷宫镶石 | Mazestone Mosaic | `twilightforest:mazestone_mosaic` |
| 345 | 矿石木栏杆 | Minewood Banister | `twilightforest:mining_banister` |
| 346 | 矿石木按钮 | Minewood Button | `twilightforest:mining_button` |
| 347 | 矿石木箱子 | Minewood Chest | `twilightforest:mining_chest` |
| 348 | 矿石木门 | Minewood Door | `twilightforest:mining_door` |
| 349 | 矿石木栅栏 | Minewood Fence | `twilightforest:mining_fence` |
| 350 | 矿石木栅栏门 | Minewood Fence Gate | `twilightforest:mining_fence_gate` |
| 351 | 矿石树叶 | Miner's Leaves | `twilightforest:mining_leaves` |
| 352 | 矿石原木 | Minewood Log | `twilightforest:mining_log` |
| 353 | 矿石树核心 | Minewood Core | `twilightforest:mining_log_core` |
| 354 | 矿石木板 | Minewood Planks | `twilightforest:mining_planks` |
| 355 | 矿石木压力板 | Minewood Pressure Plate | `twilightforest:mining_pressure_plate` |
| 356 | 矿石树苗 | Miner's Tree Sapling | `twilightforest:mining_sapling` |
| 357 | 矿石木台阶 | Minewood Slab | `twilightforest:mining_slab` |
| 358 | 矿石木楼梯 | Minewood Stairs | `twilightforest:mining_stairs` |
| 359 | 矿石木活板门 | Minewood Trapdoor | `twilightforest:mining_trapdoor` |
| 360 | 矿石木 | Minewood Wood | `twilightforest:mining_wood` |
| 361 | 米诺菇Boss刷怪笼 | Minoshroom Boss Spawner | `twilightforest:minoshroom_boss_spawner` |
| 362 | 苔藓块 | Moss Patch | `twilightforest:moss_patch` |
| 363 | 生苔城堡砖 | Mossy Castle Brick | `twilightforest:mossy_castle_brick` |
| 364 | 生苔城堡砖楼梯 | Mossy Castle Brick Stairs | `twilightforest:mossy_castle_brick_stairs` |
| 365 | 蚀刻娜迦苔石 | Mossy Etched Nagastone | `twilightforest:mossy_etched_nagastone` |
| 366 | 生苔迷宫石砖 | Mossy Mazestone Brick | `twilightforest:mossy_mazestone` |
| 367 | 娜迦苔石柱 | Mossy Nagastone Pillar | `twilightforest:mossy_nagastone_pillar` |
| 368 | 娜迦苔石楼梯（左） | Mossy Nagastone Stairs (Left) | `twilightforest:mossy_nagastone_stairs_left` |
| 369 | 娜迦苔石楼梯（右） | Mossy Nagastone Stairs (Right) | `twilightforest:mossy_nagastone_stairs_right` |
| 370 | 塔木板 | Mossy Towerwood Planks | `twilightforest:mossy_towerwood` |
| 371 | 生苔地下砖 | Mossy Underbrick | `twilightforest:mossy_underbrick` |
| 372 | 荧光蘑菇 | Mushgloom | `twilightforest:mushgloom` |
| 373 | 娜迦Boss刷怪笼 | Naga Boss Spawner | `twilightforest:naga_boss_spawner` |
| 374 | 娜迦庭院微缩雕塑 | Miniature Naga Courtyard | `twilightforest:naga_courtyard_miniature_structure` |
| 375 | 娜迦石 | Nagastone | `twilightforest:nagastone` |
| 376 | 娜迦石首 | Nagastone Head | `twilightforest:nagastone_head` |
| 377 | 娜迦石柱 | Nagastone Pillar | `twilightforest:nagastone_pillar` |
| 378 | 娜迦石楼梯（左） | Nagastone Stairs (Left) | `twilightforest:nagastone_stairs_left` |
| 379 | 娜迦石楼梯（右） | Nagastone Stairs (Right) | `twilightforest:nagastone_stairs_right` |
| 380 | 橡木栏杆 | Oak Banister | `twilightforest:oak_banister` |
| 381 | 橙色力场 | Orange Force Field | `twilightforest:orange_force_field` |
| 382 | 品红色城堡门 | Magenta Castle Door | `twilightforest:pink_castle_door` |
| 383 | 品红色城堡符文砖 | Magenta Castle Rune Brick | `twilightforest:pink_castle_rune_brick` |
| 384 | 品红色力场 | Magenta Force Field | `twilightforest:pink_force_field` |
| 385 | 彩虹橡树树叶 | Rainbow Oak Leaves | `twilightforest:rainbow_oak_leaves` |
| 386 | 彩虹橡树树苗 | Rainbow Oak Sapling | `twilightforest:rainbow_oak_sapling` |
| 387 | 雨云 | Rainy Cloud | `twilightforest:rainy_cloud` |
| 388 | 重现方块 | Reappearing Block | `twilightforest:reappearing_block` |
| 389 | 红线 | Red Thread | `twilightforest:red_thread` |
| 390 | 根 | Roots | `twilightforest:root` |
| 391 | 根须 | Root Strands | `twilightforest:root_strand` |
| 392 | 滑动陷阱 | Slide Trap | `twilightforest:slider` |
| 393 | 烟雾方块 | Smoker | `twilightforest:smoker` |
| 394 | 冰雪女王Boss刷怪笼 | Snow Queen Boss Spawner | `twilightforest:snow_queen_boss_spawner` |
| 395 | 雪云 | Snowy Cloud | `twilightforest:snowy_cloud` |
| 396 | 分类木栏杆 | Sortingwood Banister | `twilightforest:sorting_banister` |
| 397 | 分类木按钮 | Sortingwood Button | `twilightforest:sorting_button` |
| 398 | 分类木箱子 | Sortingwood Chest | `twilightforest:sorting_chest` |
| 399 | 分类木门 | Sortingwood Door | `twilightforest:sorting_door` |
| 400 | 分类木栅栏 | Sortingwood Fence | `twilightforest:sorting_fence` |
| 401 | 分类木栅栏门 | Sortingwood Fence Gate | `twilightforest:sorting_fence_gate` |
| 402 | 分类树叶 | Sorting Leaves | `twilightforest:sorting_leaves` |
| 403 | 分类原木 | Sortingwood Log | `twilightforest:sorting_log` |
| 404 | 分类树引擎 | Sortingwood Engine | `twilightforest:sorting_log_core` |
| 405 | 分类木板 | Sortingwood Planks | `twilightforest:sorting_planks` |
| 406 | 分类木压力板 | Sortingwood Pressure Plate | `twilightforest:sorting_pressure_plate` |
| 407 | 分类树苗 | Sorting Tree Sapling | `twilightforest:sorting_sapling` |
| 408 | 分类木台阶 | Sortingwood Slab | `twilightforest:sorting_slab` |
| 409 | 分类木楼梯 | Sortingwood Stairs | `twilightforest:sorting_stairs` |
| 410 | 分类木活板门 | Sortingwood Trapdoor | `twilightforest:sorting_trapdoor` |
| 411 | 分类木 | Sortingwood Wood | `twilightforest:sorting_wood` |
| 412 | 螺旋石砖 | Spiral Bricks | `twilightforest:spiral_bricks` |
| 413 | 云杉木栏杆 | Spruce Banister | `twilightforest:spruce_banister` |
| 414 | 钢叶块 | Block of Steeleaf | `twilightforest:steeleaf_block` |
| 415 | 去皮苍穹原木 | Stripped Canopy Tree Log | `twilightforest:stripped_canopy_log` |
| 416 | 去皮苍穹木 | Stripped Canopy Tree Wood | `twilightforest:stripped_canopy_wood` |
| 417 | 去皮黑木原木 | Stripped Darkwood Log | `twilightforest:stripped_dark_log` |
| 418 | 去皮黑木 | Stripped Darkwood Wood | `twilightforest:stripped_dark_wood` |
| 419 | 去皮红树原木 | Stripped Mangrove Log | `twilightforest:stripped_mangrove_log` |
| 420 | 去皮红树木 | Stripped Mangrove Wood | `twilightforest:stripped_mangrove_wood` |
| 421 | 去皮矿石原木 | Stripped Minewood Log | `twilightforest:stripped_mining_log` |
| 422 | 去皮矿石木 | Stripped Minewood Wood | `twilightforest:stripped_mining_wood` |
| 423 | 去皮分类原木 | Stripped Sortingwood Log | `twilightforest:stripped_sorting_log` |
| 424 | 去皮分类木 | Stripped Sortingwood Wood | `twilightforest:stripped_sorting_wood` |
| 425 | 去皮时光原木 | Stripped Timewood Log | `twilightforest:stripped_time_log` |
| 426 | 去皮时光木 | Stripped Timewood Wood | `twilightforest:stripped_time_wood` |
| 427 | 去皮变化原木 | Stripped Transwood Log | `twilightforest:stripped_transformation_log` |
| 428 | 去皮变化木 | Stripped Transwood Wood | `twilightforest:stripped_transformation_wood` |
| 429 | 去皮暮色橡木原木 | Stripped Twilight Oak Log | `twilightforest:stripped_twilight_oak_log` |
| 430 | 去皮暮色橡木 | Stripped Twilight Oak Wood | `twilightforest:stripped_twilight_oak_wood` |
| 431 | 要塞护盾 | Stronghold Shield | `twilightforest:stronghold_shield` |
| 432 | 厚重城堡砖 | Thick Castle Brick | `twilightforest:thick_castle_brick` |
| 433 | 荆棘叶 | Thorn Leaves | `twilightforest:thorn_leaves` |
| 434 | 荆棘玫瑰 | Thorn Rose | `twilightforest:thorn_rose` |
| 435 | 时光木栏杆 | Timewood Banister | `twilightforest:time_banister` |
| 436 | 时光木按钮 | Timewood Button | `twilightforest:time_button` |
| 437 | 时光木箱子 | Timewood Chest | `twilightforest:time_chest` |
| 438 | 时光木门 | Timewood Door | `twilightforest:time_door` |
| 439 | 时光木栅栏 | Timewood Fence | `twilightforest:time_fence` |
| 440 | 时光木栅栏门 | Timewood Fence Gate | `twilightforest:time_fence_gate` |
| 441 | 时光树叶 | Timewood Leaves | `twilightforest:time_leaves` |
| 442 | 时光原木 | Timewood Log | `twilightforest:time_log` |
| 443 | 时光树时钟 | Timewood Clock | `twilightforest:time_log_core` |
| 444 | 时光木板 | Timewood Planks | `twilightforest:time_planks` |
| 445 | 时光木压力板 | Timewood Pressure Plate | `twilightforest:time_pressure_plate` |
| 446 | 时光树苗 | Tree of Time Sapling | `twilightforest:time_sapling` |
| 447 | 时光木台阶 | Timewood Slab | `twilightforest:time_slab` |
| 448 | 时光木楼梯 | Timewood Stairs | `twilightforest:time_stairs` |
| 449 | 时光木活板门 | Timewood Trapdoor | `twilightforest:time_trapdoor` |
| 450 | 时光木 | Timewood Wood | `twilightforest:time_wood` |
| 451 | 火炬浆果植物 | Torchberry Plant | `twilightforest:torchberry_plant` |
| 452 | 塔木木板 | Towerwood Planks | `twilightforest:towerwood` |
| 453 | 变化木栏杆 | Transwood Banister | `twilightforest:transformation_banister` |
| 454 | 变化木按钮 | Transwood Button | `twilightforest:transformation_button` |
| 455 | 变化木箱子 | Transwood Chest | `twilightforest:transformation_chest` |
| 456 | 变化木门 | Transwood Door | `twilightforest:transformation_door` |
| 457 | 变化木栅栏 | Transwood Fence | `twilightforest:transformation_fence` |
| 458 | 变化木栅栏门 | Transwood Fence Gate | `twilightforest:transformation_fence_gate` |
| 459 | 变化树叶 | Leaves of Transformation | `twilightforest:transformation_leaves` |
| 460 | 变化原木 | Transwood Log | `twilightforest:transformation_log` |
| 461 | 变化树心脏 | Heart of Transformation | `twilightforest:transformation_log_core` |
| 462 | 变化木板 | Transwood Planks | `twilightforest:transformation_planks` |
| 463 | 变化木压力板 | Transwood Pressure Plate | `twilightforest:transformation_pressure_plate` |
| 464 | 变化树苗 | Tree of Transformation Sapling | `twilightforest:transformation_sapling` |
| 465 | 变化木台阶 | Transwood Slab | `twilightforest:transformation_slab` |
| 466 | 变化木楼梯 | Transwood Stairs | `twilightforest:transformation_stairs` |
| 467 | 变化木活板门 | Transwood Trapdoor | `twilightforest:transformation_trapdoor` |
| 468 | 变化木 | Transwood Wood | `twilightforest:transformation_wood` |
| 469 | 巨魔莓 | Trollber | `twilightforest:trollber` |
| 470 | 巨魔岩石 | Trollsteinn | `twilightforest:trollsteinn` |
| 471 | 巨魔藤蔓 | Trollvidr | `twilightforest:trollvidr` |
| 472 | 奖杯基座 | Trophy Pedestal | `twilightforest:trophy_pedestal` |
| 473 | 暮色橡木栏杆 | Twilight Oak Banister | `twilightforest:twilight_oak_banister` |
| 474 | 暮色橡木按钮 | Twilight Oak Button | `twilightforest:twilight_oak_button` |
| 475 | 暮色橡木箱子 | Twilight Oak Chest | `twilightforest:twilight_oak_chest` |
| 476 | 暮色橡木门 | Twilight Oak Door | `twilightforest:twilight_oak_door` |
| 477 | 暮色橡木栅栏 | Twilight Oak Fence | `twilightforest:twilight_oak_fence` |
| 478 | 暮色橡木栅栏门 | Twilight Oak Fence Gate | `twilightforest:twilight_oak_fence_gate` |
| 479 | 暮色橡树树叶 | Twilight Oak Leaves | `twilightforest:twilight_oak_leaves` |
| 480 | 暮色橡木原木 | Twilight Oak Log | `twilightforest:twilight_oak_log` |
| 481 | 暮色橡木木板 | Twilight Oak Planks | `twilightforest:twilight_oak_planks` |
| 482 | 暮色橡木压力板 | Twilight Oak Pressure Plate | `twilightforest:twilight_oak_pressure_plate` |
| 483 | 暮色橡树树苗 | Sickly Twilight Oak Sapling | `twilightforest:twilight_oak_sapling` |
| 484 | 暮色橡木台阶 | Twilight Oak Slab | `twilightforest:twilight_oak_slab` |
| 485 | 暮色橡木楼梯 | Twilight Oak Stairs | `twilightforest:twilight_oak_stairs` |
| 486 | 暮色橡木活板门 | Twilight Oak Trapdoor | `twilightforest:twilight_oak_trapdoor` |
| 487 | 暮色橡木 | Twilight Oak Wood | `twilightforest:twilight_oak_wood` |
| 488 | 暮色森林传送门微缩雕塑 | Miniature Twilight Forest Portal | `twilightforest:twilight_portal_miniature_structure` |
| 489 | 螺纹石 | Twisted Stone | `twilightforest:twisted_stone` |
| 490 | 螺纹石柱 | Twisted Stone Pillar | `twilightforest:twisted_stone_pillar` |
| 491 | 肥沃泥土 | Uberous Soil | `twilightforest:uberous_soil` |
| 492 | 拆解台 | Uncrafting Table | `twilightforest:uncrafting_table` |
| 493 | 地下砖 | Underbrick | `twilightforest:underbrick` |
| 494 | 抛光地下砖 | Underbrick Floor | `twilightforest:underbrick_floor` |
| 495 | 未熟的巨魔莓 | Unripe Trollber | `twilightforest:unripe_trollber` |
| 496 | 暮初恶魂Boss刷怪笼 | Ur-Ghast Boss Spawner | `twilightforest:ur_ghast_boss_spawner` |
| 497 | 红树木栏杆 | Mangrove Banister | `twilightforest:vangrove_banister` |
| 498 | 消失方块 | Vanishing Block | `twilightforest:vanishing_block` |
| 499 | 紫色城堡门 | Violet Castle Door | `twilightforest:violet_castle_door` |
| 500 | 紫色城堡符文砖 | Violet Castle Rune Brick | `twilightforest:violet_castle_rune_brick` |
| 501 | 紫色力场 | Violet Force Field | `twilightforest:violet_force_field` |
| 502 | 诡异木栏杆 | Warped Banister | `twilightforest:warped_banister` |
| 503 | 风化死岩 | Weathered Deadrock | `twilightforest:weathered_deadrock` |
| 504 | 薄云 | Wispy Cloud | `twilightforest:wispy_cloud` |
| 505 | 磨损城堡砖 | Worn Castle Brick | `twilightforest:worn_castle_brick` |
| 506 | 磨损城堡砖楼梯 | Worn Castle Brick Stairs | `twilightforest:worn_castle_brick_stairs` |
| 507 | 锻铁栅栏 | Wrought Iron Fence | `twilightforest:wrought_iron_fence` |
| 508 | 黄色城堡门 | Yellow Castle Door | `twilightforest:yellow_castle_door` |
| 509 | 黄色城堡符文砖 | Yellow Castle Rune Brick | `twilightforest:yellow_castle_rune_brick` |

### TFEntities 刷怪蛋注册（53 项）

| 序号 | 中文名称 | 英文名称 | 物品 ID |
| ---: | --- | --- | --- |
| 510 | 信徒刷怪蛋 | Adherent Spawn Egg | `twilightforest:adherent_spawn_egg` |
| 511 | 雪怪首领刷怪蛋 | Alpha Yeti Spawn Egg | `twilightforest:alpha_yeti_spawn_egg` |
| 512 | 武装巨人刷怪蛋 | Armored Giant Spawn Egg | `twilightforest:armored_giant_spawn_egg` |
| 513 | 大角羊刷怪蛋 | Bighorn Sheep Spawn Egg | `twilightforest:bighorn_sheep_spawn_egg` |
| 514 | 链锤哥布林刷怪蛋 | Block and Chain Goblin Spawn Egg | `twilightforest:blockchain_goblin_spawn_egg` |
| 515 | 野猪刷怪蛋 | Boar Spawn Egg | `twilightforest:boar_spawn_egg` |
| 516 | 砷铅铁寄生虫刷怪蛋 | Carminite Broodling Spawn Egg | `twilightforest:carminite_broodling_spawn_egg` |
| 517 | 砷铅铁守卫刷怪蛋 | Carminite Ghastguard Spawn Egg | `twilightforest:carminite_ghastguard_spawn_egg` |
| 518 | 砷铅铁恶灵刷怪蛋 | Carminite Ghastling Spawn Egg | `twilightforest:carminite_ghastling_spawn_egg` |
| 519 | 砷铅铁傀儡刷怪蛋 | Carminite Golem Spawn Egg | `twilightforest:carminite_golem_spawn_egg` |
| 520 | 死灵书刷怪蛋 | Death Tome Spawn Egg | `twilightforest:death_tome_spawn_egg` |
| 521 | 鹿刷怪蛋 | Deer Spawn Egg | `twilightforest:deer_spawn_egg` |
| 522 | 侏儒兔刷怪蛋 | Dwarf Rabbit Spawn Egg | `twilightforest:dwarf_rabbit_spawn_egg` |
| 523 | 喷火甲虫刷怪蛋 | Fire Beetle Spawn Egg | `twilightforest:fire_beetle_spawn_egg` |
| 524 | 巨人矿工刷怪蛋 | Giant Miner Spawn Egg | `twilightforest:giant_miner_spawn_egg` |
| 525 | 噩兆方块刷怪蛋 | Harbinger Cube Spawn Egg | `twilightforest:harbinger_cube_spawn_egg` |
| 526 | 树篱蜘蛛刷怪蛋 | Hedge Spider Spawn Egg | `twilightforest:hedge_spider_spawn_egg` |
| 527 | 寄居蟹刷怪蛋 | Helmet Crab Spawn Egg | `twilightforest:helmet_crab_spawn_egg` |
| 528 | 恶狼刷怪蛋 | Hostile Wolf Spawn Egg | `twilightforest:hostile_wolf_spawn_egg` |
| 529 | 九头蛇刷怪蛋 | Hydra Spawn Egg | `twilightforest:hydra_spawn_egg` |
| 530 | 冰晶刷怪蛋 | Ice Crystal Spawn Egg | `twilightforest:ice_crystal_spawn_egg` |
| 531 | 国王蜘蛛刷怪蛋 | King Spider Spawn Egg | `twilightforest:king_spider_spawn_egg` |
| 532 | 幻影骑士刷怪蛋 | Knight Phantom Spawn Egg | `twilightforest:knight_phantom_spawn_egg` |
| 533 | 狗头人刷怪蛋 | Kobold Spawn Egg | `twilightforest:kobold_spawn_egg` |
| 534 | 巫妖刷怪蛋 | Lich Spawn Egg | `twilightforest:lich_spawn_egg` |
| 535 | 哥布林骑士刷怪蛋 | Goblin Knight Spawn Egg | `twilightforest:lower_goblin_knight_spawn_egg` |
| 536 | 迷宫史莱姆刷怪蛋 | Maze Slime Spawn Egg | `twilightforest:maze_slime_spawn_egg` |
| 537 | 米诺菇刷怪蛋 | Minoshroom Spawn Egg | `twilightforest:minoshroom_spawn_egg` |
| 538 | 牛头人刷怪蛋 | Minotaur Spawn Egg | `twilightforest:minotaur_spawn_egg` |
| 539 | 迷雾狼刷怪蛋 | Mist Wolf Spawn Egg | `twilightforest:mist_wolf_spawn_egg` |
| 540 | 蚊群刷怪蛋 | Mosquito Swarm Spawn Egg | `twilightforest:mosquito_swarm_spawn_egg` |
| 541 | 娜迦刷怪蛋 | Naga Spawn Egg | `twilightforest:naga_spawn_egg` |
| 542 | 企鹅刷怪蛋 | Penguin Spawn Egg | `twilightforest:penguin_spawn_egg` |
| 543 | 巨钳甲虫刷怪蛋 | Pinch Beetle Spawn Egg | `twilightforest:pinch_beetle_spawn_egg` |
| 544 | 谜题羊刷怪蛋 | Quest Ram Spawn Egg | `twilightforest:quest_ram_spawn_egg` |
| 545 | 乌鸦刷怪蛋 | Raven Spawn Egg | `twilightforest:raven_spawn_egg` |
| 546 | 红帽工兵刷怪蛋 | Redcap Sapper Spawn Egg | `twilightforest:redcap_sapper_spawn_egg` |
| 547 | 小红帽刷怪蛋 | Redcap Spawn Egg | `twilightforest:redcap_spawn_egg` |
| 548 | 骷髅德鲁伊刷怪蛋 | Skeleton Druid Spawn Egg | `twilightforest:skeleton_druid_spawn_egg` |
| 549 | 黏液甲虫刷怪蛋 | Slime Beetle Spawn Egg | `twilightforest:slime_beetle_spawn_egg` |
| 550 | 冰雪守卫刷怪蛋 | Snow Guardian Spawn Egg | `twilightforest:snow_guardian_spawn_egg` |
| 551 | 冰雪女王刷怪蛋 | Snow Queen Spawn Egg | `twilightforest:snow_queen_spawn_egg` |
| 552 | 松鼠刷怪蛋 | Squirrel Spawn Egg | `twilightforest:squirrel_spawn_egg` |
| 553 | 冰精灵刷怪蛋 | Stable Ice Core Spawn Egg | `twilightforest:stable_ice_core_spawn_egg` |
| 554 | 集群蜘蛛刷怪蛋 | Swarm Spider Spawn Egg | `twilightforest:swarm_spider_spawn_egg` |
| 555 | 小鸟刷怪蛋 | Tiny Bird Spawn Egg | `twilightforest:tiny_bird_spawn_egg` |
| 556 | 塔木蛀虫刷怪蛋 | Towerwood Borer Spawn Egg | `twilightforest:towerwood_borer_spawn_egg` |
| 557 | 巨魔刷怪蛋 | Troll Spawn Egg | `twilightforest:troll_spawn_egg` |
| 558 | 破碎冰精灵刷怪蛋 | Unstable Ice Core Spawn Egg | `twilightforest:unstable_ice_core_spawn_egg` |
| 559 | 暮初恶魂刷怪蛋 | Ur-Ghast Spawn Egg | `twilightforest:ur_ghast_spawn_egg` |
| 560 | 寒冬狼刷怪蛋 | Winter Wolf Spawn Egg | `twilightforest:winter_wolf_spawn_egg` |
| 561 | 游魂刷怪蛋 | Wraith Spawn Egg | `twilightforest:wraith_spawn_egg` |
| 562 | 雪怪刷怪蛋 | Yeti Spawn Egg | `twilightforest:yeti_spawn_egg` |
