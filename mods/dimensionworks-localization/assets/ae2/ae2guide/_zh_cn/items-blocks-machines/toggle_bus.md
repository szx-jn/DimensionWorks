---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 切换总线
  icon: toggle_bus
  position: 110
categories:
- network infrastructure
item_ids:
- ae2:toggle_bus
- ae2:inverted_toggle_bus
---

# 切换总线

<GameScene zoom="8" background="transparent">
<ImportStructure src="../assets/assemblies/toggle_bus.snbt" />
<IsometricCamera yaw="195" pitch="30" />
</GameScene>

切换总线与<ItemLink id="fluix_glass_cable" />等普通线缆类似，但能够通过红石信号控制连接状态。它可用来按需断开或接通[ME 网络](../ae2-mechanics/me-network-connections.md)的某个分支。

普通切换总线收到红石信号时接通网络；<ItemLink id="inverted_toggle_bus" />的逻辑相反，收到红石信号时断开。

**注意：**频繁切换会触发网络重新连接和计算，规模较大的网络应谨慎使用。

它们属于[线缆附属部件](../ae2-mechanics/cable-subparts.md)。

## 合成配方

<RecipeFor id="toggle_bus" />

<RecipeFor id="inverted_toggle_bus" />
