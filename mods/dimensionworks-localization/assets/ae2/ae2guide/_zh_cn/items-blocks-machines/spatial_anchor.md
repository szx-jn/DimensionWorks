---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 空间锚
  icon: spatial_anchor
  position: 110
categories:
- network infrastructure
item_ids:
- ae2:spatial_anchor
---

# 空间锚

<BlockImage id="spatial_anchor" p:powered="true" scale="8"/>

ME 网络中的[设备](../ae2-mechanics/devices.md)需要所在区块处于加载状态才能正常工作。如果网络跨越多个区块，而其中一部分未加载，就可能出现连接或自动化问题。

空间锚可以持续加载其所属 ME 网络经过的区块。即使只是一条线缆经过区块边界，也可能让相邻区块纳入加载范围。

它的区块加载效果可以沿[量子桥](quantum_bridge.md)的网络连接传播，但不会自动跨维度传播，因此跨维系统通常需要在两侧分别安排空间锚。

默认情况下，加载的区块还可能获得随机刻更新。此行为可在 AE2 配置中调整。

使用<ItemLink id="certus_quartz_wrench" />可以调整空间锚的朝向。

## 设置

* 可以查看当前正在加载的区块范围及其能源开销。
* 可以显示已加载区块的全息轮廓，方便检查加载范围。

## 能源消耗

空间锚的能源消耗随着加载区块数量增加而上升：

`e = 80 + (x × (x + 1)) / 2`

其中 `x` 为加载的区块数量。实际能源换算还会受到整合包能源规则的影响。

## 合成配方

<RecipeFor id="spatial_anchor" />
