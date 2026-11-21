---
navigation:
  parent: ae2wtlib/ae2wtlib-index.md
  title: AE2WTLib 无线终端
  icon: ae2wtlib:wireless_universal_terminal
  position: 10
categories:
- ae2wtlib
item_ids:
  - ae2wtlib:wireless_pattern_encoding_terminal
  - ae2wtlib:wireless_pattern_access_terminal
---

# 无线终端

<ItemGrid>
  <ItemIcon id="ae2wtlib:wireless_universal_terminal" />
  <ItemIcon id="ae2:wireless_crafting_terminal" />
  <ItemIcon id="ae2wtlib:wireless_pattern_encoding_terminal" />
  <ItemIcon id="ae2wtlib:wireless_pattern_access_terminal" />
</ItemGrid>

除<ItemLink id="ae2:energy_card" />外，所有 AE2WTLib 无线终端都可以安装<ItemLink id="ae2wtlib:quantum_bridge_card" />，并可组合成<ItemLink id="ae2wtlib:wireless_universal_terminal" />。

与 AE2 的<ItemLink id="ae2:wireless_terminal" />一样，可以通过按键绑定打开，也可以放入饰品栏（前提是安装了实现 Curios API 的模组）。

无线终端也可以像普通<ItemLink id="ae2:wireless_terminal" />一样，通过<ItemLink id="ae2:wireless_access_point" />连接网络。

## 无线通用终端

<ItemImage id="ae2wtlib:wireless_universal_terminal" scale="3" />

<ItemLink id="ae2wtlib:wireless_universal_terminal" />可以将多个无线终端组合为一个物品。

## 无线合成终端

<ItemImage id="ae2:wireless_crafting_terminal" scale="3" />

<ItemLink id="ae2:wireless_crafting_terminal" />是合成终端的无线版本。与 AE2 原版相比，它还具有一些[额外功能](wireless_crafting_terminal.md)。

## 无线样板编码终端

<ItemImage id="ae2wtlib:wireless_pattern_encoding_terminal" scale="3" />

这是<ItemLink id="ae2:pattern_encoding_terminal" />的无线版本。

<RecipeFor id="ae2wtlib:wireless_pattern_encoding_terminal" />

## 无线样板访问终端

<ItemImage id="ae2wtlib:wireless_pattern_access_terminal" scale="3" />

这是<ItemLink id="ae2:pattern_access_terminal" />的无线版本。

<RecipeFor id="ae2wtlib:wireless_pattern_access_terminal" />

## 附属模组的终端

大多数其他附属模组提供的无线终端也可以与<ItemLink id="ae2wtlib:wireless_universal_terminal" />配合使用。

## 无法装入通用终端的终端

<ItemLink id="ae2:wireless_terminal" />无法装入<ItemLink id="ae2wtlib:wireless_universal_terminal" />，因为它相比<ItemLink id="ae2:wireless_crafting_terminal" />没有额外用途。它也无法使用<ItemLink id="ae2wtlib:quantum_bridge_card" />。
