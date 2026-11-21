---
navigation:
  parent: ae2wtlib/ae2wtlib-index.md
  title: 无线合成终端
  icon: ae2:wireless_crafting_terminal
  position: 100
categories:
- ae2wtlib
item_ids:
  - ae2:wireless_crafting_terminal
---

# 无线合成终端

<ItemImage id="ae2:wireless_crafting_terminal" scale="3" />

<ItemLink id="ae2:wireless_crafting_terminal" />是<ItemLink id="ae2:crafting_terminal" />的[无线](wireless_terminals.md)版本。AE2WTLib 为其添加了从 ME 网络[自动补货](restock.md)的功能，并支持使用“选取方块”功能从 ME 网络取出物品。装上<ItemLink id="ae2wtlib:magnet_card" />后，它还可以作为磁铁使用。

它可以与其他[无线终端](wireless_terminals.md)组合成<ItemLink id="ae2wtlib:wireless_universal_terminal" />。

AE2WTLib 增强了 Minecraft 的 `Pick Block` 功能：如果物品栏中没有对应物品，就会尝试从网络中取出。（如果当前格已满，也会尝试存入物品。）

## 界面

参见[终端](ae2:items-blocks-machines/terminals.md)。

## 升级

<ItemLink id="ae2:wireless_crafting_terminal" />支持以下[升级](ae2:items-blocks-machines/upgrade_cards.md)：

* <ItemLink id="ae2:energy_card" />：增加电池容量。
* <ItemLink id="ae2wtlib:quantum_bridge_card" />：使终端拥有无限范围（包括跨维度），并能自动从 ME 网络充能。
* <ItemLink id="ae2wtlib:magnet_card" />：添加磁铁功能。

## 配方

<RecipeFor id="ae2:wireless_crafting_terminal" />
