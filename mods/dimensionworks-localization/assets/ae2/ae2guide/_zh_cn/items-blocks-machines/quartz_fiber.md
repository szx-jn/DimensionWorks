---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 石英纤维
  icon: quartz_fiber
  position: 110
categories:
- network infrastructure
item_ids:
- ae2:quartz_fiber
---

# 石英纤维

<GameScene zoom="8" background="transparent">
<ImportStructure src="../assets/assemblies/quartz_fiber.snbt" />
<IsometricCamera yaw="195" pitch="30" />
</GameScene>

石英纤维能够在相互独立的 [ME 网络](../ae2-mechanics/me-network-connections.md)之间传递能源，但**不会让它们合并为同一个网络**。这对建立[子网络](../ae2-mechanics/subnetworks.md)非常有用：子网可以共享主网的能源，而不必单独布置能源接收器。

它还可以阻断线缆之间的直接连接。不过，如果只是想隔开线缆，使用不同颜色的线缆或<ItemLink id="cable_anchor" />通常更加简单、便宜。

石英纤维属于[线缆附属部件](../ae2-mechanics/cable-subparts.md)。

## 合成配方

<RecipeFor id="quartz_fiber" />
