---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 视图元件
  icon: view_cell
  position: 410
categories:
- tools
item_ids:
- ae2:view_cell
---

# 视图元件

<ItemImage id="view_cell" scale="2" />

视图元件用于筛选[终端](terminals.md)中显示的物品。先通过<ItemLink id="cell_workbench" />配置允许显示的物品，再将视图元件放入终端。

例如，如果你只想在建筑材料终端中看到石头和木板，就可以把这两种材料添加到视图元件。这样终端只会显示符合条件的物品。

安装多张视图元件时，允许显示的物品会取并集。例如，一张允许橡木木板，另一张允许圆石，那么终端会同时显示两者。

## 合成配方

<Recipe id="network/cells/view_cell_storage" />

<Recipe id="network/cells/view_cell" />
