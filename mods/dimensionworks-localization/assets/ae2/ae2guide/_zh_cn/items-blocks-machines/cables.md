---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 电缆
  icon: fluix_glass_cable
  position: 110
categories:
- network infrastructure
item_ids:
- ae2:white_glass_cable
- ae2:orange_glass_cable
- ae2:magenta_glass_cable
- ae2:light_blue_glass_cable
- ae2:yellow_glass_cable
- ae2:lime_glass_cable
- ae2:pink_glass_cable
- ae2:gray_glass_cable
- ae2:light_gray_glass_cable
- ae2:cyan_glass_cable
- ae2:purple_glass_cable
- ae2:blue_glass_cable
- ae2:brown_glass_cable
- ae2:green_glass_cable
- ae2:red_glass_cable
- ae2:black_glass_cable
- ae2:fluix_glass_cable
- ae2:white_covered_cable
- ae2:orange_covered_cable
- ae2:magenta_covered_cable
- ae2:light_blue_covered_cable
- ae2:yellow_covered_cable
- ae2:lime_covered_cable
- ae2:pink_covered_cable
- ae2:gray_covered_cable
- ae2:light_gray_covered_cable
- ae2:cyan_covered_cable
- ae2:purple_covered_cable
- ae2:blue_covered_cable
- ae2:brown_covered_cable
- ae2:green_covered_cable
- ae2:red_covered_cable
- ae2:black_covered_cable
- ae2:fluix_covered_cable
- ae2:white_covered_dense_cable
- ae2:orange_covered_dense_cable
- ae2:magenta_covered_dense_cable
- ae2:light_blue_covered_dense_cable
- ae2:yellow_covered_dense_cable
- ae2:lime_covered_dense_cable
- ae2:pink_covered_dense_cable
- ae2:gray_covered_dense_cable
- ae2:light_gray_covered_dense_cable
- ae2:cyan_covered_dense_cable
- ae2:purple_covered_dense_cable
- ae2:blue_covered_dense_cable
- ae2:brown_covered_dense_cable
- ae2:green_covered_dense_cable
- ae2:red_covered_dense_cable
- ae2:black_covered_dense_cable
- ae2:fluix_covered_dense_cable
- ae2:white_smart_cable
- ae2:orange_smart_cable
- ae2:magenta_smart_cable
- ae2:light_blue_smart_cable
- ae2:yellow_smart_cable
- ae2:lime_smart_cable
- ae2:pink_smart_cable
- ae2:gray_smart_cable
- ae2:light_gray_smart_cable
- ae2:cyan_smart_cable
- ae2:purple_smart_cable
- ae2:blue_smart_cable
- ae2:brown_smart_cable
- ae2:green_smart_cable
- ae2:red_smart_cable
- ae2:black_smart_cable
- ae2:fluix_smart_cable
- ae2:white_smart_dense_cable
- ae2:orange_smart_dense_cable
- ae2:magenta_smart_dense_cable
- ae2:light_blue_smart_dense_cable
- ae2:yellow_smart_dense_cable
- ae2:lime_smart_dense_cable
- ae2:pink_smart_dense_cable
- ae2:gray_smart_dense_cable
- ae2:light_gray_smart_dense_cable
- ae2:cyan_smart_dense_cable
- ae2:purple_smart_dense_cable
- ae2:blue_smart_dense_cable
- ae2:brown_smart_dense_cable
- ae2:green_smart_dense_cable
- ae2:red_smart_dense_cable
- ae2:black_smart_dense_cable
- ae2:fluix_smart_dense_cable
---

# ME 线缆

<GameScene zoom="3" background="transparent">
  <ImportStructure src="../assets/assemblies/cables.snbt" />
  <IsometricCamera yaw="180" pitch="30" />
</GameScene>

ME 网络中，相邻设备可以直接连接。需要跨越较远距离时，则可以使用线缆将设备组成同一个网络。

不同颜色的线缆可以用来隔离连接，方便管理[频道](../ae2-mechanics/channels.md)。线缆还会影响终端显示的颜色，因此可以用不同颜色区分网络区域。

**注意：线缆染色不会增加频道容量。**

## 初学者建议

刚接触 AE2 时，建议优先使用智能线缆与致密智能线缆。它们可以直接显示频道使用情况，方便查找设备没有频道、布线拥堵等问题。

## 线缆不是物品管道

**普通 ME 线缆不存储也不主动运输物品、流体或外部能源。** 它们负责把 AE2 [设备](../ae2-mechanics/devices.md)连接成网络；实际的物品输入输出，需要使用相应总线或接口来完成。

## 玻璃线缆

<GameScene zoom="6" background="transparent">
<ImportStructure src="../assets/assemblies/fluix_glass_cable.snbt" />
<IsometricCamera yaw="195" pitch="30" />
</GameScene>

<ItemLink id="fluix_glass_cable" />是最基本的 ME 线缆，能够传递 AE 能量和最多 **8 个[频道](../ae2-mechanics/channels.md)**。

玻璃线缆有未染色的福鲁伊克斯版本，以及 16 种染色版本。通常可以用同类线缆和染料合成其他颜色，也可以使用支持的染色工具直接上色；将染色线缆与水桶合成可以去掉颜色。

用羊毛覆盖玻璃线缆，可以制作<ItemLink id="fluix_covered_cable" />；制作<ItemLink id="fluix_smart_cable" />后，还能直观查看频道使用情况。

<RecipeFor id="fluix_glass_cable" />
<RecipeFor id="blue_glass_cable" />

## 包层线缆

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/fluix_covered_cable.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

包层线缆与<ItemLink id="fluix_glass_cable" />功能基本相同，但外观更加完整，适合建筑装饰。它同样支持染色。

将四段<ItemLink id="fluix_covered_cable" />与红石、荧石等材料组合，可以制作<ItemLink id="fluix_covered_dense_cable" />。

<Recipe id="network/cables/covered_fluix" />
<RecipeFor id="blue_covered_cable" />

## 致密线缆

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/fluix_covered_dense_cable.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

致密线缆最多能承载 **32 个频道**，比普通线缆的 8 个频道更适合作为主干。

但致密线缆的表面不能像普通线缆一样安装所有总线和面板。要连接这些设备，应先通过<ItemLink id="fluix_glass_cable" />或<ItemLink id="fluix_smart_cable" />分出普通线缆支路。

频道会根据连接路径进行分配，具体规则可参见[频道系统](../ae2-mechanics/channels.md)。

<Recipe id="network/cables/dense_covered_fluix" />
<RecipeFor id="blue_covered_dense_cable" />

## 智能线缆

<Row>
<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/fluix_smart_cable.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>
<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/fluix_smart_dense_cable.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>
</Row>

智能线缆在普通包层线缆的基础上增加了频道显示条，可以直观查看频道使用量。

普通智能线缆最多承载 8 个频道；致密智能线缆最多承载 32 个频道，每条亮色纹路代表一定数量的频道。

在连接<ItemLink id="controller" />的网络中，智能线缆显示的是经过该线路的频道。无控制器网络的显示方式有所不同，通常表示整个网络已经使用的频道数量。

它们也可以像<ItemLink id="fluix_glass_cable" />一样进行染色。

<Recipe id="network/cables/smart_fluix" />
<Recipe id="network/cables/dense_smart_fluix" />
<RecipeFor id="blue_smart_cable" />
