---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 空间存储元件
  icon: spatial_storage_cell_128
  position: 410
categories:
- tools
item_ids:
- ae2:spatial_storage_cell_2
- ae2:spatial_storage_cell_16
- ae2:spatial_storage_cell_128
- ae2:spatial_cell_component_2
- ae2:spatial_cell_component_16
- ae2:spatial_cell_component_128
---

# 空间存储元件

<Row>
  <ItemImage id="spatial_storage_cell_2" scale="4" />

  <ItemImage id="spatial_storage_cell_16" scale="4" />

  <ItemImage id="spatial_storage_cell_128" scale="4" />
</Row>

空间存储元件用于保存一定体积的[世界空间](../ae2-mechanics/spatial-io.md)，需要放入<ItemLink id="spatial_io_port" />使用。

与普通[物品存储元件](../items-blocks-machines/storage_cells.md)不同，空间存储元件不能自由重复配置或更换空间规格。

**重要：空间存储元件一旦写入过空间数据，就不能随意重新配置或用于不同规格的空间区域。** 如需保存新的、更大范围的区域，请制作新的空间存储元件。

## 合成配方

<Row>
  <Recipe id="network/cells/spatial_storage_cell_2_cubed_storage" />

  <Recipe id="network/cells/spatial_storage_cell_16_cubed_storage" />

  <Recipe id="network/cells/spatial_storage_cell_128_cubed_storage" />
</Row>

## 存储外壳

可以通过组合空间存储组件与存储外壳来制作元件，也可以使用其他等价配方。

<Row>
  <Recipe id="network/cells/spatial_storage_cell_2_cubed" />

  <Recipe id="network/cells/spatial_storage_cell_2_cubed_storage" />
</Row>

外壳的制作配方：

<RecipeFor id="item_cell_housing" />

## 空间存储组件

空间存储组件是空间存储元件的核心。不同等级的组件可存储的空间体积按比例增加。

<Row>
  <RecipeFor id="spatial_cell_component_2" />

  <RecipeFor id="spatial_cell_component_16" />

  <RecipeFor id="spatial_cell_component_128" />
</Row>
