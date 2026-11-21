---
navigation:
    parent: epp_intro/epp_intro-index.md
    title: ME 标签存储总线
    icon: extendedae:tag_storage_bus
categories:
- extended devices
item_ids:
- extendedae:tag_storage_bus
---

# ME 标签存储总线

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../structure/cable_tag_storage_bus.snbt"></ImportStructure>
</GameScene>

ME 标签存储总线是一种<ItemLink id="ae2:storage_bus" />，可以按物品标签或流体标签过滤，并支持一些基本逻辑运算符。

示例：

- 仅接受粗矿

`c:raw_materials/*`

- 接受所有锭和宝石

`c:ingots/* | c:gems/*`
