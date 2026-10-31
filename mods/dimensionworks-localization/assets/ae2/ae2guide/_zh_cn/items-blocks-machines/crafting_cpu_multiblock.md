---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 自动合成 CPU 多方块结构
  icon: 1k_crafting_storage
  position: 210
categories:
- devices
item_ids:
- ae2:1k_crafting_storage
- ae2:4k_crafting_storage
- ae2:16k_crafting_storage
- ae2:64k_crafting_storage
- ae2:256k_crafting_storage
- ae2:crafting_accelerator
- ae2:crafting_monitor
- ae2:crafting_unit
---

# 自动合成 CPU

<GameScene zoom="4" background="transparent">
  <ImportStructure src="../assets/assemblies/crafting_cpus.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

<Row>
  <BlockImage id="1k_crafting_storage" scale="4" />
  <BlockImage id="crafting_accelerator" scale="4" />
  <BlockImage id="crafting_monitor" scale="4" />
  <BlockImage id="crafting_unit" scale="4" />
</Row>

合成 CPU 负责分配和执行[自动合成](../ae2-mechanics/autocrafting.md)任务，并暂存生产过程中需要的材料和中间产物。CPU 的容量与协处理器数量会影响可处理的任务规模和材料投送速度。

每个合成 CPU **同一时间只能处理一项合成请求**。如果想同时合成计算处理器和大量石头，通常至少需要两套独立 CPU。

合成任务既可以由玩家在终端中发起，也可以由安装了<ItemLink id="crafting_card" />的<ItemLink id="export_bus" />等设备自动发起。

右键合成 CPU 可以打开任务状态界面，查看正在执行的任务及其进度。

## 设置

可以决定 CPU 只接受玩家手动请求、只接受自动化请求，或者两种请求都接受。

## 多方块结构

合成 CPU 必须组成**没有空隙的完整长方体**。最小结构只需要一个合成存储组件；更大的 CPU 可以加入合成单元、协处理器和显示器。

## 合成单元

<BlockImage id="crafting_unit" scale="4" />

合成单元为可选组件，主要用来补齐多方块结构的空位，也可以用于合成其他 CPU 部件。

<RecipeFor id="crafting_unit" />

## 合成存储

<Row>
  <BlockImage id="1k_crafting_storage" scale="4" />
  <BlockImage id="4k_crafting_storage" scale="4" />
  <BlockImage id="16k_crafting_storage" scale="4" />
  <BlockImage id="64k_crafting_storage" scale="4" />
  <BlockImage id="256k_crafting_storage" scale="4" />
</Row>

**每个 CPU 至少需要一个合成存储组件。**

合成存储有 1K、4K、16K、64K 和 256K 五种容量。它们用于缓存合成任务所需的材料和中间产物。任务越复杂、需要暂存的资源越多，通常越需要较大的合成存储空间。

<Column>
  <Row>
    <RecipeFor id="1k_crafting_storage" />
    <RecipeFor id="4k_crafting_storage" />
    <RecipeFor id="16k_crafting_storage" />
  </Row>
  <Row>
    <RecipeFor id="64k_crafting_storage" />
    <RecipeFor id="256k_crafting_storage" />
  </Row>
</Column>

## 合成协处理器

<BlockImage id="crafting_accelerator" scale="4" />

协处理器是可选组件，能够提高合成 CPU 同时向多个<ItemLink id="pattern_provider" />分发原料的能力。

如果一个样板供应器周围连接了多个<ItemLink id="molecular_assembler" />，增加协处理器通常可以更好地利用并行生产能力。

<RecipeFor id="crafting_accelerator" />

## 合成监视器

<BlockImage id="crafting_monitor" scale="4" />

合成监视器也是可选组件，用来直接显示 CPU 当前处理的任务。可以使用<ItemLink id="color_applicator" />调整显示颜色。

<RecipeFor id="crafting_monitor" />
