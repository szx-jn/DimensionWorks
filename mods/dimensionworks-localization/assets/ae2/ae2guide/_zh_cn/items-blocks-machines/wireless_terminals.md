---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 无线终端
  icon: wireless_crafting_terminal
  position: 410
categories:
- tools
item_ids:
- ae2:wireless_terminal
- ae2:wireless_crafting_terminal
---

# 无线终端

<Row>
  <ItemImage id="wireless_terminal" scale="4" />
  <ItemImage id="wireless_crafting_terminal" scale="4" />
</Row>

无线终端是普通有线[终端](terminals.md)的便携版本，界面与功能基本相同。不过，无线终端的扩展槽用于安装[升级卡](upgrade_cards.md)，而不是<ItemLink id="view_cell" />。

要绑定网络，把无线终端放入<ItemLink id="wireless_access_point" />界面右上角的绑定槽，再取出即可。

使用时必须处于至少一个有效无线接入点的覆盖范围内。使用<ItemLink id="charger" />可以为终端充能。

## 普通无线终端

<ItemImage id="wireless_terminal" scale="4" />

只要位于<ItemLink id="wireless_access_point" />的覆盖范围内，就能远程浏览和操作[网络存储](../ae2-mechanics/import-export-storage.md)，并发起[自动合成](../ae2-mechanics/autocrafting.md)请求。

界面操作详见[终端](terminals.md)。

### 升级

* <ItemLink id="energy_card" />：提高内部电池容量。

### 合成配方

<RecipeFor id="wireless_terminal" />

## 无线合成终端

<ItemImage id="wireless_crafting_terminal" scale="4" />

无线合成终端具备普通无线终端的全部功能，另外还有合成网格，可以从[网络存储](../ae2-mechanics/import-export-storage.md)中补齐合成材料。

使用合成输出槽时要注意：合成结果可能立即消耗网络中的材料。操作详见[终端](terminals.md)。

### 升级

* <ItemLink id="energy_card" />：提高内部电池容量。

### 合成配方

<RecipeFor id="wireless_crafting_terminal" />
