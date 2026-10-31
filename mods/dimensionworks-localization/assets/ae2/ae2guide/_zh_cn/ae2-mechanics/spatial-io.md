---
navigation:
  parent: ae2-mechanics/ae2-mechanics-index.md
  title: 空间输入输出
  icon: spatial_storage_cell_2
---

# 空间输入输出（空间 IO）

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/spatial_storage_1x1x1.snbt" />

  <BoxAnnotation color="#33dd33" min="1 1 1" max="2 2 2">
    将要移动的空间区域
  </BoxAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

空间 IO 可以把世界中的一片区域**整体保存到空间存储元件中，再在需要时恢复**。这能用来移动<ItemLink id="flawless_budding_quartz" />、快速更换房间布局，甚至搬迁特殊建筑。

它更准确的工作方式是“交换”：把指定区域内的方块与空间存储维度中同样大小的区域互换，而非单纯复制。

如果能够进入空间存储维度，你还可以把它用作特殊的独立空间。不过，设计稳定的进出方式比较复杂，需要额外规划。

## 多方块结构

整个空间 IO 系统需要安装在同一个[ME 网络](me-network-connections.md)上。同一个网络只能有一套空间 IO 结构，因此通常建议通过[子网络](subnetworks.md)将它独立出来。

## 空间输入输出端口

<BlockImage id="spatial_io_port" p:powered="true" scale="4" />

<ItemLink id="spatial_io_port" />用于安装[空间存储元件](../items-blocks-machines/spatial_cells.md)，并控制空间 IO 的启动。它的界面会显示：

* 网络当前与最大[储能](energy.md)；
* 执行本次操作需要的能量。空间 IO 可能瞬间消耗大量能量，建议安装足够的[能源元件](../items-blocks-machines/energy_cells.md)；
* 空间塔阵列的效率；
* 当前选定区域的尺寸。

放入空间存储元件后，向端口输入一次红石脉冲，就会执行区域交换。下一次使用同一张元件时，之前保存的区域会被换回来，原本位置上的内容则会进入空间存储维度。

**注意：实体也可能随区域一起移动，包括玩家。** 如果没有准备好返回途径，玩家可能被困在空间存储维度里。

## 空间塔

<BlockImage id="spatial_pylon" p:powered_on="true" scale="4" />

<ItemLink id="spatial_pylon" />负责界定空间 IO 的作用范围。空间塔形成的边界必须围住要移动的区域，并满足以下条件：

* 最小外框尺寸为 3×3×3，对应内部 1×1×1 的空间；
* 所有空间塔必须位于区域外侧一格厚的外框范围内；
* 所有空间塔必须处于同一 ME 网络；
* 每条连续空间塔结构至少有 2 个方块长。

例如，若要移动一个 3×3×3 的内部空间，空间塔应位于其外部 5×5×5 的框架上。

<GameScene zoom="4" interactive={true}>
<ImportStructure src="../assets/assemblies/spatial_storage_3x3x3_pylon_demonstration.snbt" />

<BoxAnnotation color="#33dd33" min="1 1 1" max="4 4 4">
    将要移动的空间区域
</BoxAnnotation>

<BoxAnnotation color="#3333ff" min="5 5 0" max="0 0 5">
</BoxAnnotation>

<IsometricCamera yaw="195" pitch="30" />
</GameScene>

下面是更合理、更易维护的空间塔布置方式：

<GameScene zoom="4" interactive={true}>
<ImportStructure src="../assets/assemblies/better_spatial_storage_3x3x3.snbt" />

<BoxAnnotation color="#33dd33" min="1 1 1" max="4 4 4">
    将要移动的空间区域
</BoxAnnotation>

<BoxAnnotation color="#3333ff" min="5 5 0" max="0 0 5">
</BoxAnnotation>

<IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 效率与能源消耗

空间塔外框的完整程度会影响效率。效率过低时，完成一次空间 IO 可能需要数十亿 AE，因此应尽量合理布置空间塔并提前储存能源。

## 空间元件的尺寸

一张[空间存储元件](../items-blocks-machines/spatial_cells.md)首次使用后，会与具体的 X、Y、Z 尺寸（例如 3×4×2）绑定。**使用过的元件不能重新调整尺寸，也不能简单地清空后另存一片不同大小的区域。**

存储元件名称中的尺寸只表示最大边长，例如 16³ 元件可保存任何不超过 16×16×16 的区域。

尺寸还区分方向：2×2×3 与 3×2×2 不是同一种布局。元件尺寸与空间 IO 端口显示的区域尺寸不一致时，系统不会执行操作。
