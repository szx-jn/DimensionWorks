---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: ME 输入输出端口
  icon: io_port
  position: 210
categories:
- devices
item_ids:
- ae2:io_port
---

# ME 输入输出端口

<BlockImage id="io_port" p:powered="true" scale="8" />

ME 输入输出端口可以快速把物品从[存储元件](../items-blocks-machines/storage_cells.md)转移到[ME 网络存储](../ae2-mechanics/import-export-storage.md)，或反过来将网络中的物品装入元件。

使用<ItemLink id="certus_quartz_wrench" />可以调整设备朝向。

## 设置

* 可以设定元件在**已清空、已装满或处理完毕**时，自动移到输出槽。
* 安装<ItemLink id="redstone_card" />后，还可以按不同红石信号条件控制运行。
* 界面中央的箭头用于切换传输方向：从存储元件转入网络，或从网络写入存储元件。

## 升级

输入输出端口支持以下[升级卡](upgrade_cards.md)：

* <ItemLink id="speed_card" />：提高每次操作搬运的物品数量。
* <ItemLink id="redstone_card" />：根据有无红石信号，或每次红石脉冲，控制传输行为。

## 合成配方

<RecipeFor id="io_port" />
