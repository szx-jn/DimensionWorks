---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 伪装板
  icon: facade
  icon_nbt: '{item: "minecraft:stone"}'
  position: 110
categories:
- network infrastructure
item_ids:
- ae2:facade
---

# 伪装板

伪装板可以遮住 ME 线缆，使线路融入建筑墙面。它支持许多不同方块的外观，非常适合隐藏机房中的布线。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/facades_1.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

伪装板可以安装在线缆的侧面。已有的[线缆附属部件](../ae2-mechanics/cable-subparts.md)和连接位置可能会限制伪装板的摆放。

<GameScene zoom="6"  interactive={true}>
  <ImportStructure src="../assets/assemblies/facades_2.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

合理使用伪装板，不仅能隐藏线缆，还可以通过不同方块纹理做出更自然的墙面设计。

<GameScene zoom="4" interactive={true}>
  <ImportStructure src="../assets/assemblies/facades_3.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 临时隐藏伪装板

手持<a href="network_tool.md">网络工具</a>时，伪装板会临时隐藏。你可以直接与伪装板后面的设备交互，而不用拆除伪装板。

## 合成方法

制作时，将四个想要模仿纹理的方块围绕一个<ItemLink id="cable_anchor" />摆放。

![伪装板配方示意图](../assets/diagrams/facade_recipe.png)
