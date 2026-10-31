---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: ME 输出总线
  icon: export_bus
  position: 220
categories:
- devices
item_ids:
- ae2:export_bus
---

# ME 输出总线

<GameScene zoom="8" background="transparent">
<ImportStructure src="../assets/blocks/export_bus.snbt" />
</GameScene>

输出总线从[ME 网络存储](../ae2-mechanics/import-export-storage.md)提取物品、流体或其他受支持的资源，并将它们送入相邻的外部容器。

长时间没有成功输出资源时，总线会自动降低检查频率；一旦可以重新输出，就会恢复正常运行速度。

输出总线属于[线缆附属部件](../ae2-mechanics/cable-subparts.md)。

## 过滤规则

默认情况下，输出总线**不会主动输出任何物品**。你需要在过滤槽指定需要输出的物品或流体。

可以直接从 JEI 中拖入物品设置过滤条件。要过滤流体本身，可以手持装有目标流体的容器右键过滤槽。

## 升级卡

输出总线支持以下[升级卡](upgrade_cards.md)：

* <ItemLink id="capacity_card" />：增加过滤槽数量，并允许设置输出顺序。
* <ItemLink id="speed_card" />：增加每次操作搬运的数量。
* <ItemLink id="fuzzy_card" />：允许忽略部分耐久或 NBT 差异。
* <ItemLink id="crafting_card" />：网络内缺少指定物品时，可以向[自动合成系统](../ae2-mechanics/autocrafting.md)发起请求。
* <ItemLink id="redstone_card" />：增加红石控制方式，例如有信号时运行或响应脉冲。

## 传输速度

| 加速卡数量 | 每次操作搬运的物品数 |
| :--- | ---: |
| 0 | 1 |
| 1 | 8 |
| 2 | 32 |
| 3 | 64 |
| 4 | 96 |

## 合成配方

<RecipeFor id="export_bus" />
