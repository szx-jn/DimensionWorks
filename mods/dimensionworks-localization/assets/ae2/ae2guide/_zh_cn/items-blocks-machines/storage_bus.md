---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: ME 存储总线
  icon: storage_bus
  position: 220
categories:
- devices
item_ids:
- ae2:storage_bus
---

# ME 存储总线

<GameScene zoom="8" background="transparent">
<ImportStructure src="../assets/blocks/storage_bus.snbt" />
</GameScene>

想继续使用原来的箱子或抽屉，而不是把所有物品搬进 ME 存储元件？**存储总线**正是为此设计的。

将存储总线贴在外部容器上，可以把其中的资源加入[ME 网络存储](../ae2-mechanics/import-export-storage.md)。网络中的[设备](../ae2-mechanics/devices.md)便可以查看、存入或取出容器里的物品。

存储总线还可以在[子网络](../ae2-mechanics/subnetworks.md)中充当资源的来源或目标，实现[物品与流体管道](../example-setups/pipe-subnet.md)等特殊物流布局。

它属于[线缆附属部件](../ae2-mechanics/cable-subparts.md)。

## 过滤

默认情况下，存储总线允许网络访问相邻容器中的所有资源。把物品拖入过滤槽后，就只会允许对应的物品。

可以直接从 JEI 中拖入物品图标。若要筛选流体本身，可以手持装有流体的桶或储罐右键过滤槽。

## 优先级

可以在界面右上角点击扳手调整优先级：

* 存入物品时，优先使用优先级较高的存储。
* 优先级相同时，通常优先存入已经存有同种物品、或分区规则允许该物品的存储设备。
* 取出物品时，通常优先使用优先级较低的存储设备。

因此，优先级也可以用来逐步清空旧仓库，向高优先级存储设备转移物品。

## 设置

* 从容器现有内容自动生成过滤规则。
* 决定网络是否显示无法从容器中直接取出的物品，例如部分<ItemLink id="inscriber" />内部槽位。
* 设置过滤规则作用于存入、取出，还是两个方向。
* 在双向、仅存入和仅取出模式之间切换。

## 升级卡

支持以下[升级卡](upgrade_cards.md)：

* <ItemLink id="capacity_card" />：增加过滤槽数量。
* <ItemLink id="fuzzy_card" />：允许忽略部分耐久或 NBT 差异。
* <ItemLink id="inverter_card" />：将白名单切换为黑名单。
* <ItemLink id="void_card" />：目标容器已满时销毁多余的输入资源，适合防止农场堵塞，但可能造成物品损失。

## 合成配方

<RecipeFor id="storage_bus" />
