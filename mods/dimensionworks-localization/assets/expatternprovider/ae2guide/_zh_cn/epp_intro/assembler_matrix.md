---
navigation:
    parent: epp_intro/epp_intro-index.md
    title: 装配矩阵
    icon: expatternprovider:assembler_matrix_frame
categories:
- extended devices
item_ids:
- expatternprovider:assembler_matrix_frame
- expatternprovider:assembler_matrix_wall
- expatternprovider:assembler_matrix_glass
- expatternprovider:assembler_matrix_pattern
- expatternprovider:assembler_matrix_crafter
- expatternprovider:assembler_matrix_speed
---

# 装配矩阵

<Row>
<BlockImage id="expatternprovider:assembler_matrix_frame" p:formed="true" p:powered="true" scale="5"></BlockImage>
<BlockImage id="expatternprovider:assembler_matrix_wall" scale="5"></BlockImage>
<BlockImage id="expatternprovider:assembler_matrix_glass" scale="5"></BlockImage>
</Row>
<Row>
<BlockImage id="expatternprovider:assembler_matrix_pattern" scale="5"></BlockImage>
<BlockImage id="expatternprovider:assembler_matrix_crafter" scale="5"></BlockImage>
<BlockImage id="expatternprovider:assembler_matrix_speed" scale="5"></BlockImage>
</Row>

装配矩阵是一种多方块自动合成结构，结合了<ItemLink id="ae2:molecular_assembler" />和<ItemLink id="ae2:pattern_provider" />的功能，可以并行执行大量合成任务，并减少网络中的设备与频道开销。

为了让 ME 网络同时处理多个任务，还需要配置足够的<ItemLink id="ae2:crafting_accelerator" />等自动合成资源。

## 结构搭建

<GameScene zoom="3" background="transparent" interactive={true}>
  <ImportStructure src="../structure/assembler_matrix.snbt"></ImportStructure>
</GameScene>

装配矩阵必须是边长在 **3～7 格**之间的长方体：

* 边框由装配矩阵框架组成。
* 各个面由矩阵墙或矩阵玻璃组成。
* 内部填充样板核心、合成核心和速度核心等部件，不能留有空洞。

有效结构至少需要一个**样板核心**和一个**合成核心**。正确组装并通电后，框架上的线条会亮起蓝光。

## 三种矩阵核心

**样板核心**：提供自动合成样板槽位。每个核心可以安装 36 张样板，装配矩阵只会读取核心中的样板。

**合成核心**：负责执行自动合成任务。每个核心最多可同时处理 8 项任务。

**速度核心**：类似于<ItemLink id="ae2:speed_card" />，能够加快装配矩阵的工作速度。安装 **5 个速度核心**即可达到最高速度，再增加也不会更快。

## 操作界面

右键点击已经成功形成且通电的装配矩阵，可以打开管理界面。

![装配矩阵界面](../pic/assembler_matrix.png)

在界面中可以添加或搜索样板，并查看当前正在执行的合成任务数量。
