---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: ME 输入总线
  icon: import_bus
  position: 220
categories:
- devices
item_ids:
- ae2:import_bus
---

# ME 输入总线

<GameScene zoom="8" background="transparent">
<ImportStructure src="../assets/blocks/import_bus.snbt" />
</GameScene>

输入总线从相邻的外部容器中抽取物品、流体或其他受支持的资源，并将它们送入[ME 网络存储](../ae2-mechanics/import-export-storage.md)。

为了降低空闲时的运算负载，长时间没有成功输入资源的总线会降低检查频率；一旦再次找到可输入物品，就会恢复正常运行速度。

输入总线属于[线缆附属部件](../ae2-mechanics/cable-subparts.md)。

## 过滤规则

默认情况下，输入总线会尽可能抽取全部资源。在过滤槽内指定物品后，它就只会抽取符合条件的物品。

可以将 JEI 中的物品拖入过滤槽，即使背包里没有该物品也能设置。如果需要过滤的是**流体本身**，可以手持装有该流体的桶或储罐右键过滤槽。

## 升级卡

输入总线支持以下[升级卡](upgrade_cards.md)：

* <ItemLink id="capacity_card" />：增加可设置的过滤槽。
* <ItemLink id="speed_card" />：增加每次操作搬运的数量。
* <ItemLink id="fuzzy_card" />：允许忽略部分物品耐久或 NBT 差异。
* <ItemLink id="inverter_card" />：将白名单切换为黑名单。
* <ItemLink id="redstone_card" />：增加红石控制方式，可以根据有无信号或脉冲运行。

## 传输速度

| 加速卡数量 | 每次操作搬运的物品数 |
| :--- | ---: |
| 0 | 1 |
| 1 | 8 |
| 2 | 32 |
| 3 | 64 |
| 4 | 96 |

## 合成配方

<RecipeFor id="import_bus" />
