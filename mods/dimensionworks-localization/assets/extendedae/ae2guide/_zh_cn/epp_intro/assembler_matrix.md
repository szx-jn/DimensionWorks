---
navigation:
    parent: epp_intro/epp_intro-index.md
    title: 装配矩阵
    icon: extendedae:assembler_matrix_frame
categories:
- extended devices
item_ids:
- extendedae:assembler_matrix_frame
- extendedae:assembler_matrix_wall
- extendedae:assembler_matrix_glass
- extendedae:assembler_matrix_pattern
- extendedae:assembler_matrix_crafter
- extendedae:assembler_matrix_speed
---

# 装配矩阵

<Row>
<BlockImage id="extendedae:assembler_matrix_frame" p:formed="true" p:powered="true" scale="5"></BlockImage>
<BlockImage id="extendedae:assembler_matrix_wall" scale="5"></BlockImage>
<BlockImage id="extendedae:assembler_matrix_glass" scale="5"></BlockImage>
</Row>
<Row>
<BlockImage id="extendedae:assembler_matrix_pattern" scale="5"></BlockImage>
<BlockImage id="extendedae:assembler_matrix_crafter" scale="5"></BlockImage>
<BlockImage id="extendedae:assembler_matrix_speed" scale="5"></BlockImage>
</Row>

装配矩阵是一种多方块结构，由<ItemLink id="ae2:molecular_assembler" />和<ItemLink id="ae2:pattern_provider" />组合而成。只要 ME 网络中有足够的<ItemLink id="ae2:crafting_accelerator" />，它就能同时处理许多合成任务，并节省频道。

## 结构

<GameScene zoom="3" background="transparent" interactive={true}>
  <ImportStructure src="../structure/assembler_matrix.snbt"></ImportStructure>
</GameScene>

它是一个长方体棱柱，每条棱的长度为 3 到 7 格。
- 棱由装配矩阵框架组成。
- 表面由装配矩阵墙或玻璃组成。
- 内部由装配矩阵样板核心、合成核心或速度核心组成。

有效的装配矩阵至少要有一个样板核心和一个合成核心。结构必须完全填满，不能中空。正确成型并通电后，装配矩阵框架上的线条会变成蓝色。

## 装配矩阵核心

装配矩阵核心有三种。

- 装配矩阵样板核心

装配矩阵只会从样板核心读取样板。每个样板核心为装配矩阵提供 36 个样板槽位。

- 装配矩阵合成核心

装配矩阵会将收到的合成任务分配给合成核心。每个合成核心可以同时处理 8 个合成任务。

- 装配矩阵速度核心

它相当于装配矩阵的<ItemLink id="ae2:speed_card" />。装入 5 个速度核心即可使装配矩阵达到全速；超过 5 个不会进一步提升速度。

## GUI

右键点击已成型且在线的装配矩阵即可打开 GUI。

![GUI](../pic/assembler_matrix.png)

你可以在其中添加或搜索样板，并查看正在处理的合成任务数量。
