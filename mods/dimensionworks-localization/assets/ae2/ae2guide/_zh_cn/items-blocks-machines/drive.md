---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: ME 驱动器
  icon: drive
  position: 210
categories:
- devices
item_ids:
- ae2:drive
---

# ME 驱动器

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../assets/blocks/drive.snbt" />
</GameScene>

ME 驱动器是用于安装[存储元件](storage_cells.md)的[设备](../ae2-mechanics/devices.md)。每台驱动器有 **10 个存储元件槽位**，元件中的物品会成为[网络存储](../ae2-mechanics/import-export-storage.md)的一部分。

驱动器中的元件可以通过漏斗或 ME 物流设备自动装入和取出。使用<ItemLink id="certus_quartz_wrench" />还能调整驱动器朝向。

## 指示灯颜色

| 颜色 | 存储元件状态 |
| :--- | :--- |
| 绿色 | 元件为空 |
| 蓝色 | 元件内有物品 |
| 橙色 | [物品类型](../ae2-mechanics/bytes-and-types.md)已满，不能存入新的种类 |
| 红色 | [字节容量](../ae2-mechanics/bytes-and-types.md)已满，不能再存入物品 |
| 黑色 | 没有供电，或驱动器没有取得[频道](../ae2-mechanics/channels.md) |

## 存储优先级

你可以通过界面右上角的扳手设置优先级。ME 网络存入物品时，会先使用优先级较高的存储设备；优先级相同的设备中，已存有同类物品或通过[元件工作台](cell_workbench.md)设有相应筛选规则的元件通常会被优先使用。

从网络取出物品时，系统通常优先从低优先级的设备中取出，因此合理设置优先级还能让物品逐渐向主要仓库集中。

## 合成配方

<RecipeFor id="drive" />
