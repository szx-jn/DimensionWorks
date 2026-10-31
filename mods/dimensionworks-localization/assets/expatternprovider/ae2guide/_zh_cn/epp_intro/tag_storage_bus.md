---
navigation:
    parent: epp_intro/epp_intro-index.md
    title: ME 标签存储总线
    icon: expatternprovider:tag_storage_bus
categories:
- extended devices
item_ids:
- expatternprovider:tag_storage_bus
---

# ME 标签存储总线

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../structure/cable_tag_storage_bus.snbt"></ImportStructure>
</GameScene>

ME 标签存储总线在<ItemLink id="ae2:storage_bus" />基础上增加了**按物品或流体标签筛选**的能力，还支持简单的逻辑组合。

例如，只允许原材料：

`forge:raw_materials/*`

同时允许所有金属锭和宝石：

`forge:ingots/* | forge:gems/*`
