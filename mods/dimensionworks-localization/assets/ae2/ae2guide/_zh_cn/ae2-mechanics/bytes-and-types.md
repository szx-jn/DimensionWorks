---
navigation:
  parent: ae2-mechanics/ae2-mechanics-index.md
  title: 字节与物品类型
  icon: creative_item_cell
---

# 字节与物品类型

<Row>
    <ItemImage id="item_storage_cell_1k" scale="4" />
    <ItemImage id="item_storage_cell_4k" scale="4" />
    <ItemImage id="item_storage_cell_16k" scale="4" />
    <ItemImage id="item_storage_cell_64k" scale="4" />
    <ItemImage id="item_storage_cell_256k" scale="4" />
</Row>

[存储元件](../items-blocks-machines/storage_cells.md)有两个重要限制：**字节容量**和**物品类型数量**。

字节决定物品的总存储容量，而类型数量决定可以存放多少种不同物品。例如，4096 个圆石只算一种物品类型，但 16 把耐久度和附魔都不相同的剑，可能占用 16 种类型。

每增加一种物品类型，会先消耗一部分字节作为类型开销；之后还会根据物品数量消耗存储空间。普通物品平均每 8 个占用 1 字节，流体则通常每 8 桶占用 1 字节。

类型上限是为了控制存储元件的 NBT 数据规模。过多的不同物品会增加服务器与客户端同步数据、网络搜索和排序的负担。

因此，建议提前筛选怪物农场产出的随机附魔装备，否则不同耐久度和附魔的装备会迅速占满类型额度。

<ItemLink id="drive" />能安装多张存储元件，但每张普通存储元件的类型上限仍为 63 种。更大容量的元件能存储更多**数量**，不一定能存放更多**种类**。

## 存储元件费用

| 存储元件                                     |   字节 | 类型 | 每个类型字节数 | 赛特斯石英 | 红石 | 金锭 | 荧石 |
| ---------------------------------------- | ------: | ----: | -------------: | -----: | -------: | ---: | --------: |
| <ItemLink id="item_storage_cell_1k" />   |   1,024 |    63 |              8 |      4 |        5 |    1 |         0 |
| <ItemLink id="item_storage_cell_4k" />   |   4,096 |    63 |             32 |  14.25 |       20 |    3 |         0 |
| <ItemLink id="item_storage_cell_16k" />  |  16,384 |    63 |            128 |     45 |       61 |    9 |         4 |
| <ItemLink id="item_storage_cell_64k" />  |  65,536 |    63 |            512 | 137.25 |      184 |   27 |        16 |
| <ItemLink id="item_storage_cell_256k" /> | 262,144 |    63 |           2048 |    414 |      553 |   81 |        48 |

## 不同类型数量下的容量

由于每种物品类型都需要额外的存储开销，只存放一种物品时，实际容量通常比存满 63 种不同物品时更大。

| 存储元件                                     | 只存 1 种物品时的容量 | 存满 63 种物品时的容量 |
| ---------------------------------------- | ----------------------------------------: | ------------------------------------------: |
| <ItemLink id="item_storage_cell_1k" />   |                                     8,128 |                                       4,160 |
| <ItemLink id="item_storage_cell_4k" />   |                                    32,512 |                                      16,640 |
| <ItemLink id="item_storage_cell_16k" />  |                                   130,048 |                                      66,560 |
| <ItemLink id="item_storage_cell_64k" />  |                                   520,192 |                                     266,240 |
| <ItemLink id="item_storage_cell_256k" /> |                                 2,080,768 |                                   1,064,960 |

![1 型的存储元件](../assets/diagrams/1_type_cell.png)

![63 种类型的存储元件](../assets/diagrams/63_type_cell.png)