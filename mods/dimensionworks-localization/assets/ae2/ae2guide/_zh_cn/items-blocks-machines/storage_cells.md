---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 存储元件
  icon: item_storage_cell_1k
  position: 410
categories:
- tools
item_ids:
- ae2:item_cell_housing
- ae2:fluid_cell_housing
- ae2:cell_component_1k
- ae2:cell_component_4k
- ae2:cell_component_16k
- ae2:cell_component_64k
- ae2:cell_component_256k
- ae2:item_storage_cell_1k
- ae2:item_storage_cell_4k
- ae2:item_storage_cell_16k
- ae2:item_storage_cell_64k
- ae2:item_storage_cell_256k
- ae2:fluid_storage_cell_1k
- ae2:fluid_storage_cell_4k
- ae2:fluid_storage_cell_16k
- ae2:fluid_storage_cell_64k
- ae2:fluid_storage_cell_256k
---

# 存储元件

<Column>
  <Row>
    <ItemImage id="item_storage_cell_1k" scale="4" />

    <ItemImage id="item_storage_cell_4k" scale="4" />

    <ItemImage id="item_storage_cell_16k" scale="4" />

    <ItemImage id="item_storage_cell_64k" scale="4" />

    <ItemImage id="item_storage_cell_256k" scale="4" />
  </Row>

  <Row>
    <ItemImage id="fluid_storage_cell_1k" scale="4" />

    <ItemImage id="fluid_storage_cell_4k" scale="4" />

    <ItemImage id="fluid_storage_cell_16k" scale="4" />

    <ItemImage id="fluid_storage_cell_64k" scale="4" />

    <ItemImage id="fluid_storage_cell_256k" scale="4" />
  </Row>
</Column>

存储槽是存储在 Applied Energistics。他们进入<ItemLink id="drive" />
或<ItemLink id="chest" />.

见见[字节和类型](../ae2-mechanics/bytes-and-types.md)说明其能力的字节和类型。

如果单元格空,可按左转右键单击,用手边的单元格来移动存储组件,则可以从房屋中移除存储部件。

## 不同物品类型数量下的容量

由于每种物品类型都要占用额外存储字节，只存放一种物品时，实际容量通常接近存满 63 种物品时的两倍。

| 存储元件                                     | 仅存 1 种物品时的容量 | 存满 63 种物品时的容量 |
| ---------------------------------------- | ----------------------------------------: | ------------------------------------------: |
| <ItemLink id="item_storage_cell_1k" />   |                                     8,128 |                                       4,160 |
| <ItemLink id="item_storage_cell_4k" />   |                                    32,512 |                                      16,640 |
| <ItemLink id="item_storage_cell_16k" />  |                                   130,048 |                                      66,560 |
| <ItemLink id="item_storage_cell_64k" />  |                                   520,192 |                                     266,240 |
| <ItemLink id="item_storage_cell_256k" /> |                                 2,080,768 |                                   1,064,960 |


## 存储元件分区

通过<ItemLink id="cell_workbench" />设置分区，可以限制存储元件允许存入的物品或流体种类。其作用类似<ItemLink id="storage_bus" />的过滤规则。

即使背包中没有目标物品，也可以直接从 JEI 拖入图标来设置分区。

## 升级卡

存储元件可以通过<ItemLink id="cell_workbench" />安装[升级卡](upgrade_cards.md)：

* <ItemLink id="fuzzy_card" />：对物品存储元件启用模糊匹配，忽略部分耐久或 NBT 差异；流体元件不支持此卡。
* <ItemLink id="inverter_card" />：将允许列表改成排除列表。
* <ItemLink id="equal_distribution_card" />：为各种类型平均分配存储容量，防止一种物品占满元件。
* <ItemLink id="void_card" />：当存储已满时销毁继续存入的多余资源。**请谨慎使用**，避免误删重要材料。
* <ItemLink id="energy_card" />：可以安装在便携式元件中，提高其内部能源容量。

## 染色

便携式存储元件可以像皮革盔甲一样使用染料更改颜色。

## 存储外壳

存储元件由外壳和内部存储组件组成。可通过以下配方组装完整元件，或只制作外壳，再与已有组件组合。

<Row>
  <Recipe id="network/cells/item_storage_cell_1k" />

  <Recipe id="network/cells/item_storage_cell_1k_storage" />
</Row>

外壳的合成配方如下：

<Row>
  <RecipeFor id="item_cell_housing" />

  <RecipeFor id="fluid_cell_housing" />
</Row>

## 存储组件

存储组件决定元件的容量。每提升一个等级，容量通常增长为之前的 4 倍，同时需要更多材料。

<Column>
  <Row>
    <RecipeFor id="cell_component_1k" />

    <RecipeFor id="cell_component_4k" />

    <RecipeFor id="cell_component_16k" />
  </Row>

  <Row>
    <RecipeFor id="cell_component_64k" />

    <RecipeFor id="cell_component_256k" />
  </Row>
</Column>

## 物品存储元件

普通物品存储元件最多可容纳 63 种物品，有 1K、4K、16K、64K 和 256K 等容量等级。

<Column>
  <Row>
    <Recipe id="network/cells/item_storage_cell_1k_storage" />

    <Recipe id="network/cells/item_storage_cell_4k_storage" />

    <Recipe id="network/cells/item_storage_cell_16k_storage" />
  </Row>

  <Row>
    <Recipe id="network/cells/item_storage_cell_64k_storage" />

    <Recipe id="network/cells/item_storage_cell_256k_storage" />
  </Row>
</Column>

### 便携式物品存储元件

便携式元件可以随身使用，功能类似迷你<ItemLink id="chest" />。它们需要能量维持工作，可以使用<ItemLink id="charger" />充能。

它们的容量分配方式与普通存储元件略有不同，高容量型号的字节容量会更多地用于存储物品类型。安装<ItemLink id="energy_card" />可提高内部电池容量。

<Column>
  <Row>
    <RecipeFor id="portable_item_cell_1k" />

    <RecipeFor id="portable_item_cell_4k" />

    <RecipeFor id="portable_item_cell_16k" />
  </Row>

  <Row>
    <RecipeFor id="portable_item_cell_64k" />

    <RecipeFor id="portable_item_cell_256k" />
  </Row>
</Column>

## 流体存储元件

流体存储元件最多可以保存 5 种不同流体，也提供各个标准容量等级。

<Column>
  <Row>
    <Recipe id="network/cells/fluid_storage_cell_1k_storage" />

    <Recipe id="network/cells/fluid_storage_cell_4k_storage" />

    <Recipe id="network/cells/fluid_storage_cell_16k_storage" />
  </Row>

  <Row>
    <Recipe id="network/cells/fluid_storage_cell_64k_storage" />

    <Recipe id="network/cells/fluid_storage_cell_256k_storage" />
  </Row>
</Column>

### 便携式流体存储元件

便携式流体元件可以随身访问流体存储，类似小型<ItemLink id="chest" />，可以通过<ItemLink id="charger" />补充能量，并通过<ItemLink id="energy_card" />扩充内部储能。

<Column>
  <Row>
    <RecipeFor id="portable_fluid_cell_1k" />

    <RecipeFor id="portable_fluid_cell_4k" />

    <RecipeFor id="portable_fluid_cell_16k" />
  </Row>

  <Row>
    <RecipeFor id="portable_fluid_cell_64k" />

    <RecipeFor id="portable_fluid_cell_256k" />
  </Row>
</Column>

## 创造模式存储元件

<Row>
  <ItemImage id="creative_item_cell" scale="2" />

  <ItemImage id="creative_fluid_cell" scale="2" />
</Row>

创造模式物品元件和流体元件**不是普通意义上的无限容量仓库**。它们更像无限资源的输入源与销毁端，只会对通过[分区设置](cell_workbench.md)允许的资源生效。
