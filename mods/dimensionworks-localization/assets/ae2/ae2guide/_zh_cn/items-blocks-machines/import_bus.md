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

输入总线可以从面向的箱子、储罐等外部容器中抽取物品或流体，并送入[ME 网络存储](../ae2-mechanics/import-export-storage.md)。

为降低性能开销，如果长时间没有成功导入物品，总线会进入低频检查状态；一旦检测到可导入资源，就会恢复正常速度，最多每秒执行约 4 次操作。

它属于[线缆附属部件](../ae2-mechanics/cable-subparts.md)。

## 筛选

默认情况下，输入总线会尝试导入能访问到的所有资源。你也可以向筛选槽中添加物品，使其只导入白名单中的资源。

即使背包没有目标物品，也可以直接从 JEI 中拖入图标设置筛选条件。要筛选流体，可手持装有对应流体的桶或储罐右键配置槽，设置流体本身而不是容器物品。

## 升级

输入总线支持以下[升级卡](upgrade_cards.md)：

* <ItemLink id="capacity_card" />：增加筛选槽数量。
* <ItemLink id="speed_card" />：增加每次操作移动的物品数量。
* <ItemLink id="fuzzy_card" />：匹配时忽略部分耐久度或 NBT 差异。
* <ItemLink id="inverter_card" />：将筛选条件从白名单改为黑名单。
* <ItemLink id="redstone_card" />：按红石信号或脉冲控制运行。

## 传输速度

| 加速卡数量 | 每次操作移动的物品数 |
| :--- | ---: |
| 0 | 1 |
| 1 | 8 |
| 2 | 32 |
| 3 | 64 |
| 4 | 96 |

## 合成配方

<RecipeFor id="import_bus" />
