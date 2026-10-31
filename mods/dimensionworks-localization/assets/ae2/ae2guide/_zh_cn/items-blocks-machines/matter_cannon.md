---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 物质炮
  icon: matter_cannon
  position: 410
categories:
- tools
item_ids:
- ae2:matter_cannon
---

# 物质炮

<ItemImage id="matter_cannon" scale="4" />

物质炮是一种便携式远程武器，可以发射<ItemLink id="matter_ball" />或其他小型物品作为弹药。

伤害取决于弹药的类型和重量。例如，较重的金属粒可能造成 **10 点伤害**，而较轻的物质球约造成 **2 点伤害**。每次射击消耗 **1600 AE**。

如果启用了配置项 `matterCannonBlockDamage`，物质炮还可以击碎部分方块，效果取决于弹药造成的伤害与方块硬度。

使用<ItemLink id="charger" />可以给物质炮充能。

物质炮的弹药库存与[存储元件](storage_cells.md)类似。将物质炮放入<ItemLink id="chest" />的元件槽，就能方便地装填弹药。

## 升级

物质炮可以通过<ItemLink id="cell_workbench" />安装以下[升级卡](upgrade_cards.md)：

* <ItemLink id="fuzzy_card" />：匹配弹药时忽略部分耐久度或 NBT 差异。
* <ItemLink id="inverter_card" />：将筛选规则由白名单切换为黑名单。
* <ItemLink id="speed_card" />：提高每次射击的能量消耗与威力。
* <ItemLink id="void_card" />：弹药库存已满时销毁额外输入的弹药，注意会损失资源。
* <ItemLink id="energy_card" />：提高内部储能容量。

## 合成配方

<RecipeFor id="matter_cannon" />
