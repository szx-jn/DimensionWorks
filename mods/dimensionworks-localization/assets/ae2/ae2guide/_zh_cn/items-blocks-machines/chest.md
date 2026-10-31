---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: ME 箱子
  icon: chest
  position: 210
categories:
- devices
item_ids:
- ae2:chest
---

# ME 箱子

<GameScene zoom="8" background="transparent">
<ImportStructure src="../assets/blocks/chest.snbt" />
</GameScene>

ME 箱子把<ItemLink id="terminal" />、<ItemLink id="drive" />和<ItemLink id="energy_acceptor" />的部分功能整合在一个方块中。它只支持安装一张[存储元件](../items-blocks-machines/storage_cells.md)，因此不适合承担大型仓库的全部存储需求，但非常适合用作独立的小型存储装置。

ME 箱子顶部自带终端，只能直接查看和访问其中安装的元件；与之不同，普通 ME 网络中的[终端](../ae2-mechanics/devices.md)可以访问网络内的全部[存储设备](../ae2-mechanics/import-export-storage.md)，包括已接入网络的 ME 箱子。

ME 箱子的不同面具有不同功能：点击顶部可打开内置终端；点击其他面可打开存储元件槽和优先级设置。物品能够通过相应面输入或输出，而存储元件的自动装卸只能通过元件槽所在面完成。

可以使用<ItemLink id="certus_quartz_wrench" />调整 ME 箱子的朝向。

ME 箱子拥有少量 AE 能量缓冲。如果没有连接[能源元件](../items-blocks-machines/energy_cells.md)等稳定能源，大量快速存取物品可能暂时耗尽缓冲。

使用<ItemLink id="color_applicator" />可以更改箱子的颜色。

<GameScene zoom="6" background="transparent">
<ImportStructure src="../assets/assemblies/chest_color.snbt" />
<IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 设置

ME 箱子支持与<ItemLink id="terminal" />或<ItemLink id="crafting_terminal" />类似的设置，但不能使用<ItemLink id="view_cell" />。

## 存储元件指示灯

箱子上显示元件状态的指示灯含义如下：

| 颜色 | 状态 |
| :--- | :--- |
| 绿色 | 元件为空 |
| 蓝色 | 元件中存有物品 |
| 橙色 | [物品类型](../ae2-mechanics/bytes-and-types.md)达到上限，无法存入新的类型 |
| 红色 | [存储字节](../ae2-mechanics/bytes-and-types.md)已满，无法继续存入物品 |
| 黑色 | 没有供电，或设备未取得[频道](../ae2-mechanics/channels.md) |

## 存储优先级

点击元件槽右上角的扳手可以设置优先级。

ME 网络存入物品时，会优先选择优先级更高的存储设备。同一优先级下，已经存有该物品的设备通常会优先接收同类物品；通过[元件工作台](cell_workbench.md)设置了筛选的元件也会按其规则参与分配。

取出物品时通常会优先从较低优先级的存储设备提取。因此，你可以利用优先级让物品从低优先级存储流向高优先级存储。

## 合成配方

<RecipeFor id="chest" />
