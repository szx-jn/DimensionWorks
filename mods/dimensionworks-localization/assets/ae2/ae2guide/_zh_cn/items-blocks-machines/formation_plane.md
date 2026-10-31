---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 成型面板
  icon: formation_plane
  position: 210
categories:
- devices
item_ids:
- ae2:formation_plane
---

# 成型面板

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../assets/blocks/formation_plane.snbt" />
</GameScene>

成型面板能够把物品从 ME 网络中**放置到世界中或作为掉落物抛出**。它类似一个只允许输入的<ItemLink id="storage_bus" />：当其他[设备](../ae2-mechanics/devices.md)尝试将物品存入[网络存储](../ae2-mechanics/import-export-storage.md)时，成型面板会把物品放到自己前方。

例如，<ItemLink id="import_bus" />或<ItemLink id="interface" />向网络输送材料时，若成型面板被设置为高优先级存储目标，这些材料就可以被它直接放置或丢弃。

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/formation_plane_demonstration.snbt" />
  <IsometricCamera yaw="255" pitch="30" />
</GameScene>

成型面板常用于[管道子网](../example-setups/pipe-subnet.md)等小型独立网络：你可以用它代替部分存储总线，让物品直接落到地面或自动放置方块。

成型面板属于[线缆附属部件](../ae2-mechanics/cable-subparts.md)。

**注意：**服务器的领地保护规则可能会阻止成型面板放置方块。

## 过滤

默认情况下，面板可以放置任何受支持的材料。把物品拖入过滤槽后，它就只会接受符合条件的物品。

你也可以从 JEI 拖入筛选用物品。如果要筛选流体本身，可以手持装有该流体的桶或容器右键过滤槽。

## 优先级

在界面中点击扳手调整优先级。网络向存储设备分配物品时，优先使用优先级更高的目标。

## 工作模式

面板可以配置为**放置方块**，也可以配置为**直接投出掉落物**。

## 升级卡

支持以下[升级卡](upgrade_cards.md)：

* <ItemLink id="capacity_card" />：增加过滤槽。
* <ItemLink id="fuzzy_card" />：按需忽略部分耐久或 NBT 差异。
* <ItemLink id="inverter_card" />：把白名单切换为黑名单。

## 合成配方

<RecipeFor id="formation_plane" />
