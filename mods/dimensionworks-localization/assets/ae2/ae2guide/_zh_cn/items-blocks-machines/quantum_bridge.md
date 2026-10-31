---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 量子网络桥
  icon: quantum_ring
  position: 110
categories:
- network infrastructure
item_ids:
- ae2:quantum_link
- ae2:quantum_ring
---

# 量子网络桥

![量子网络桥的构造](../assets/diagrams/quantum_bridge_demonstration.png)

量子网络桥可以把相隔很远的[ME 网络](../ae2-mechanics/me-network-connections.md)连接起来，甚至支持**跨维度连接**。

量子桥最多可以传输 **32 个频道**，相当于一条无线的[致密线缆](cables.md#dense-cable)。

<GameScene zoom="4" background="transparent">
  <ImportStructure src="../assets/assemblies/quantum_bridge_internal_structure_1.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

<GameScene zoom="4" background="transparent">
  <ImportStructure src="../assets/assemblies/quantum_bridge_internal_structure_2.snbt" />

  <BoxAnnotation color="#33dd33" min="1 1 1" max="6 2 3">
        两座量子桥之间的虚拟连接线
  </BoxAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

**注意：两端都必须保持区块加载。** 如果相距较远，可以在两侧使用<ItemLink id="spatial_anchor" />或其他区块加载方式，否则连接可能中断。

# 量子环

<BlockImage id="quantum_ring" scale="8" />

用 8 个<ItemLink id="quantum_ring" />围绕一个<ItemLink id="quantum_link" />即可组成量子网络桥。

只有正上、正下、左、右的四个<ItemLink id="quantum_ring" />能连接 ME 线缆；四个角落无法直接连接线缆。

## 合成配方

<RecipeFor id="quantum_ring" />

# 量子连接仓

<BlockImage id="quantum_link" scale="8" />

<ItemLink id="quantum_ring" />围绕量子连接仓组成完整结构后，量子连接仓才会成为 ME 网络的一部分。它本身不能直接连接线缆。

连接仓只能安装一个<ItemLink id="quantum_entangled_singularity" />，但支持通过自动化物流输入和输出。

## 用途

<RecipeFor id="quantum_link" />
