---
navigation:
  parent: ae2-mechanics/ae2-mechanics-index.md
  title: 能源
  icon: energy_cell
---

# 能源

ME 网络必须有能源才能工作。网络中的设备共享同一套能源储备：<ItemLink id="vibration_chamber" />、<ItemLink id="energy_acceptor" />以及<ItemLink id="controller" />都可以向网络供能。

使用<ItemLink id="network_tool" />右键网络中的设备，或者打开控制器界面，可以查看网络的能源信息。网络中的能源统一分配，因此设备的实际位置通常不会限制它能够从网络获得的能量。

## 如何向网络供能

<Row>
  <BlockImage id="energy_acceptor" scale="4" />

  <GameScene zoom="4" background="transparent">
  <ImportStructure src="../assets/blocks/cable_energy_acceptor.snbt" />
  </GameScene>

  <BlockImage id="controller" p:state="online" scale="4" />

  <BlockImage id="vibration_chamber" p:active="true" scale="4" />
</Row>

<ItemLink id="energy_acceptor" />用于接入外部能源；<ItemLink id="controller" />也有能源接口，但控制器的表面通常更适合用来连接和分配[频道](../ae2-mechanics/channels.md)。<ItemLink id="vibration_chamber" />则能自行产生 AE 能量，适合前期使用。

**维度工坊已经修改了 AE2 的能源兼容与转换规则。** 不要直接套用原版 AE2 的 FE／AE 换算公式，实际供能方式以整合包当前版本的规则为准。

## 能源储备

<Row>
  <BlockImage id="energy_cell" scale="4" p:fullness="4" />

  <BlockImage id="dense_energy_cell" scale="4" p:fullness="4" />

  <BlockImage id="creative_energy_cell" scale="4" />
</Row>

网络单次能够处理的能源消耗，受到已有储能容量的限制。例如，如果一个小型网络的能源缓冲很少，一次性存入大量物品就可能耗尽能源，使设备短暂离线。

**增加能源元件通常能够解决这种问题。**

每条线缆或设备本身都具有少量能源缓冲，控制器的内部缓冲也比普通设备更大。<ItemLink id="energy_cell" />能扩充常规网络的能源储备；<ItemLink id="dense_energy_cell" />容量更高，适合瞬时能耗较大的[空间存储](spatial-io.md)等用途。

<ItemLink id="creative_energy_cell" />是创造模式下用于测试的无限能源设备。
