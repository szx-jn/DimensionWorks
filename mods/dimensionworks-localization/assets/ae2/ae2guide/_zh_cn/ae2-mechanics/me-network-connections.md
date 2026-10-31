---
navigation:
  parent: ae2-mechanics/ae2-mechanics-index.md
  title: 网络连接
  icon: fluix_glass_cable
---

# 网络连接

## 什么是 ME 网络？

ME 网络由[设备](../ae2-mechanics/devices.md)和能够传递[频道](../ae2-mechanics/channels.md)的连接结构组成，例如[线缆](../items-blocks-machines/cables.md)、<ItemLink id="charger" />、<ItemLink id="interface" />和<ItemLink id="drive" />。

只要组件能够通过符合规则的连接路径互相连通，它们就属于同一个网络。理论上，哪怕只有一段线缆，也已经构成一个网络。

## 设备的位置重要吗？

对于网络里的大多数[设备](../ae2-mechanics/devices.md)，**是否连接到正确的网络**往往比它们的具体位置更重要。例如，<ItemLink id="interface" />可以与[网络存储](../ae2-mechanics/import-export-storage.md)交换物品，<ItemLink id="level_emitter" />能查询网络库存，<ItemLink id="drive" />则提供存储空间。

如果这些设备连接到同一个网络，通常不需要紧挨着放置。

## 如何判断网络是否连通？

使用<ItemLink id="network_tool" />检查网络组件。工具列出的内容如果缺少某台设备，或意外包含另一片设备，就说明布线存在问题。

下面是两个互不连接的网络：

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/2_networks_1.snbt" />

  <BoxAnnotation color="#915dcd" min="0 0 0" max="1 2 2">
    网络 1
  </BoxAnnotation>

<BoxAnnotation color="#915dcd" min="2 0 0" max="3 2 2">
    网络 2
  </BoxAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

下面仍是两个独立网络，因为<ItemLink id="quartz_fiber" />只传递[能源](../ae2-mechanics/energy.md)，并不会把两端合并成一个网络：

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/2_networks_2.snbt" />

  <BoxAnnotation color="#915dcd" min="0 0 0" max="1 2 2">
    网络 1
  </BoxAnnotation>

  <BoxAnnotation color="#915dcd" min="1.3 0 0" max="3 2 2">
    网络 2
  </BoxAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

但[量子网络桥](../items-blocks-machines/quantum_bridge.md)会像无线的[致密线缆](../items-blocks-machines/cables.md#dense-cable)一样传输频道，因此桥的两端属于同一个网络：

<GameScene zoom="4" background="transparent">
  <ImportStructure src="../assets/assemblies/actually_1_network.snbt" />

  <BoxAnnotation color="#915dcd" min="0 0 0" max="7 3 3">
    同一个网络
  </BoxAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

线缆的颜色也会影响连接：不同染色线缆之间通常不会直接互连，但未染色的福鲁伊克斯线缆可以连接不同颜色的线缆。因此下图实际上仍属于一个网络。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/actually_1_network_2.snbt" />

  <BoxAnnotation color="#915dcd" min="0 0 0" max="4 2 2">
    同一个网络
  </BoxAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 避免意外连接

某些完整方块设备本身也能传递网络连接。例如，下图中的<ItemLink id="pattern_provider" />与<ItemLink id="inscriber" />可能在不经意间连通本来应该分开的网络。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/pattern_provider_network_connection_1.snbt" />

  <BoxAnnotation color="#915dcd" min="0 0 0" max="4 2 2">
    同一个网络
  </BoxAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

使用<ItemLink id="certus_quartz_wrench" />可以调整样板供应器的朝向。定向样板供应器不会通过其输出面连接网络，因此适合把两条网络隔开，构建[子网络](../ae2-mechanics/subnetworks.md)。

<Row gap="40">
<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/pattern_provider_network_connection_2.snbt" />

  <BoxAnnotation color="#915dcd" min="0 0 0" max="2 2 2">
    网络 1
  </BoxAnnotation>

  <BoxAnnotation color="#915dcd" min="2 0 0" max="4 2 2">
    网络 2
  </BoxAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/pattern_provider_directional_connection.snbt" />

  <BoxAnnotation color="#ee3333" min="1 .3 .3" max="1.3 .7 .7">
    注意：输出方向不再传递网络连接
  </BoxAnnotation>

  <IsometricCamera yaw="255" pitch="30" />
</GameScene>
</Row>

不少[线缆附属部件](../ae2-mechanics/cable-subparts.md)也有特殊的连接规则。例如<ItemLink id="import_bus" />、<ItemLink id="storage_bus" />以及<ItemLink id="cable_interface" />通常只在它们安装的网络侧建立连接，不会直接把面向的外部设备并入网络。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/subpart_no_connection.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>
