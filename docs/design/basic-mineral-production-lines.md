# 基础矿物增殖：三路线验收矩阵

适用版本：Minecraft 1.20.1 Forge；Create 6.0.8；Mekanism 10.4.16.80。

## 设计约束

当前保留的每一种目标资源都需要 **1 个有限样本**；每条配方消耗 1 个样本并输出 2 个目标资源。基础压制线额外输出 1 个通用矿物残渣；残渣回收线需要目标样本、残渣、辅料、流体与热源，不能脱离初始样本独立启动。钻石、绿宝石、远古残骸和铀等高价值资源的自增配方已暂时移除，待产业链设计完成后再重新规划。

三种风格：
1. **机械压制（Create Basin / Mechanical Press）**：固体载体与矿物样本压制，附带 1 个矿物残渣。
2. **高端重构（Mekanism Combiner）**：通过已纳入整合包清单的 KubeJS Mekanism UNOFFICIAL 扩展注册 combining 配方；主输入为目标样本，副输入按表消耗指定数量的高端载体，产出 2 个目标。
3. **热化学回收（Create Mixer）**：矿物样本与多份残渣、辅料和流体混合；当前保留目标要求加热；高价值资源配方暂不注册，待完整产业链设计完成后再加入。

## 当前保留目标配方矩阵

输出约定：机械压制线 = 2 个目标 + 1 个残渣；Mekanism 线 = 2 个目标；残渣回收线 = 2 个目标。下表中的成本均为每次配方消耗量，不含输出。

| 目标物品 | 机械压制线：样本 + 固体载体 | Mekanism 合成线：样本 + 高端载体 | 残渣回收线：样本 + 残渣 + 辅料 + 流体 | 热要求 |
|---|---|---|---|---|
| minecraft:raw_iron | 1× minecraft:raw_iron；2× minecraft:cobblestone；1× minecraft:gravel | 1× minecraft:raw_iron；2× create:andesite_alloy | 1× minecraft:raw_iron；2× kubejs:mineral_residue；1× minecraft:iron_nugget；1× minecraft:gravel；250 mB minecraft:water | 加热 |
| minecraft:raw_copper | 1× minecraft:raw_copper；2× minecraft:gravel；1× minecraft:calcite | 1× minecraft:raw_copper；2× minecraft:flint | 1× minecraft:raw_copper；2× kubejs:mineral_residue；1× minecraft:copper_ingot；1× minecraft:gravel；250 mB minecraft:water | 加热 |
| create:raw_zinc | 1× create:raw_zinc；2× minecraft:cobblestone；1× minecraft:quartz | 1× create:raw_zinc；2× create:brass_ingot | 1× create:raw_zinc；3× kubejs:mineral_residue；1× create:zinc_ingot；1× minecraft:gravel；500 mB minecraft:water | 加热 |
| minecraft:raw_gold | 1× minecraft:raw_gold；2× minecraft:netherrack；1× minecraft:quartz | 1× minecraft:raw_gold；3× minecraft:blaze_powder | 1× minecraft:raw_gold；3× kubejs:mineral_residue；1× minecraft:gold_nugget；1× minecraft:netherrack；500 mB minecraft:water | 加热 |
| minecraft:coal | 1× minecraft:coal；2× minecraft:charcoal；1× minecraft:gravel | 1× minecraft:coal；2× minecraft:charcoal | 1× minecraft:coal；2× kubejs:mineral_residue；1× minecraft:charcoal；1× minecraft:gravel；250 mB minecraft:water | 加热 |
| minecraft:redstone | 1× minecraft:redstone；1× minecraft:clay_ball；1× minecraft:iron_nugget；1× minecraft:flint | 1× minecraft:redstone；2× minecraft:glowstone_dust | 1× minecraft:redstone；3× kubejs:mineral_residue；1× minecraft:clay_ball；1× minecraft:flint；250 mB minecraft:water | 加热 |
| minecraft:lapis_lazuli | 1× minecraft:lapis_lazuli；1× minecraft:clay_ball；1× minecraft:bone_meal；1× minecraft:gravel | 1× minecraft:lapis_lazuli；2× minecraft:clay_ball | 1× minecraft:lapis_lazuli；3× kubejs:mineral_residue；1× minecraft:clay_ball；1× minecraft:bone_meal；1× minecraft:gravel；500 mB minecraft:water | 加热 |
| minecraft:quartz | 1× minecraft:quartz；2× minecraft:netherrack；1× minecraft:soul_sand | 1× minecraft:quartz；2× minecraft:soul_sand | 1× minecraft:quartz；2× kubejs:mineral_residue；1× minecraft:netherrack；1× minecraft:soul_sand；250 mB minecraft:water | 加热 |
| minecraft:amethyst_shard | 1× minecraft:amethyst_shard；2× minecraft:calcite；2× minecraft:quartz | 1× minecraft:amethyst_shard；2× minecraft:glowstone_dust | 1× minecraft:amethyst_shard；3× kubejs:mineral_residue；1× minecraft:calcite；1× minecraft:quartz；1× minecraft:glowstone_dust；500 mB minecraft:water | 加热 |
| mekanism:raw_tin | 1× mekanism:raw_tin；2× minecraft:cobblestone；1× minecraft:gravel；1× minecraft:copper_ingot | 1× mekanism:raw_tin；2× minecraft:deepslate | 1× mekanism:raw_tin；2× kubejs:mineral_residue；1× minecraft:copper_ingot；1× minecraft:cobblestone；500 mB minecraft:water | 加热 |
| mekanism:raw_lead | 1× mekanism:raw_lead；2× minecraft:gravel；1× minecraft:flint；1× minecraft:iron_ingot | 1× mekanism:raw_lead；2× minecraft:flint | 1× mekanism:raw_lead；2× kubejs:mineral_residue；1× minecraft:iron_ingot；1× minecraft:gravel；1× minecraft:flint；500 mB minecraft:water | 加热 |
| mekanism:raw_osmium | 1× mekanism:raw_osmium；2× minecraft:deepslate；1× minecraft:iron_ingot；1× minecraft:quartz | 1× mekanism:raw_osmium；2× create:brass_ingot | 1× mekanism:raw_osmium；3× kubejs:mineral_residue；1× minecraft:iron_ingot；1× minecraft:deepslate；1× minecraft:quartz；500 mB minecraft:water | 加热 |
| mekanism:fluorite_gem | 1× mekanism:fluorite_gem；2× minecraft:basalt；1× minecraft:calcite；1× minecraft:glowstone_dust | 1× mekanism:fluorite_gem；2× minecraft:lapis_lazuli | 1× mekanism:fluorite_gem；3× kubejs:mineral_residue；1× minecraft:calcite；1× minecraft:glowstone_dust；500 mB minecraft:water | 加热 |

## 配方 ID 规范

所有配方统一使用命名空间 dimensionworks_cave_factory，根路径为：

self_propagation/basic_minerals/<目标路径>/<路线>

当前保留的每个目标应有以下三个唯一 ID：
- mechanical_growth
- mekanism_combining
- residue_recovery

对应脚本：kubejs/server_scripts/basic_mineral_propagation.js。

## 验收步骤

1. 新建世界启动客户端与专用服务端，确认启动日志没有脚本解析错误、无效配方、未知物品 ID 或配方重复 ID。
2. 对每种目标分别使用 JEI 搜索三个配方，确认成本、流体量、热等级和输出与上表一致；重点检查 Mekanism Combiner 的副输入数量确实按配方扣除。
3. 使用实际机械验证：Create 压制线可稳定输出目标与残渣；Mekanism Combiner 可按配置耗材产出 2 个目标；Mixer 在正确热等级下可用残渣回收。
4. 用漏斗/传送带与自动化输入连续运行至少 10 分钟；确认各项物料数量按表扣除，没有复制辅料、残渣或目标物品的配方循环漏洞。
5. 同时检查客户端和专用服务端的配方同步、JEI 显示与 KubeJS 日志。

## 范围说明

纳入：铁、铜、金、煤、红石、青金石、下界石英、紫水晶碎片，以及 Create 锌、Mekanism 锡、铅、锇和萤石。

暂不纳入：钻石、绿宝石、远古残骸、铀，以及洞穴维度专属稀有资源与其他维度进度门槛资源。高价值资源在完整产业链方案确定前不提供自增配方。

## 当前验证边界

仓库静态测试会检查目标清单、每目标三条路线、残渣物品注册/本地化/贴图、成本升级规则，以及脚本语法。真正的游戏内配方解码、JEI 显示、机器连续运行和客户端/专用服务端启动仍必须按上述步骤实测后才能标记为最终平衡验收通过。
