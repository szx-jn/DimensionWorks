---
navigation:
  parent: ae2-mechanics/ae2-mechanics-index.md
  title: 自动合成
  icon: pattern_provider
---

# 自动合成

## 工作原理

<GameScene zoom="4" interactive={true}>
  <ImportStructure src="../assets/assemblies/autocraft_setup_greebles.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

自动合成是 AE2 的核心能力之一。只要告诉 ME 网络你需要什么，它就能计算材料需求，自动制作中间产物，再生产最终物品。除了普通工作台配方，还能与熔炉、其他模组的机器及流体加工配方配合。

一套自动合成系统主要由三部分组成：**发起合成请求的设备、合成 CPU 和<ItemLink id="pattern_provider" />。**

当你发起合成请求时，ME 网络会按照以下步骤工作：

1. 你在终端中请求制作物品，或者某个安装了合成卡的接口、输出总线自动发出请求。
2. 网络根据[样板](../items-blocks-machines/patterns.md)计算材料和中间加工步骤，并分配合成 CPU。
3. <ItemLink id="pattern_provider" />将样板对应的材料送入相邻机器。普通合成通常交给<ItemLink id="molecular_assembler" />，处理样板则可以控制熔炉或其他工业设备。
4. 加工结束后，必须通过输入总线、接口或样板供应器等方式，**让成品重新进入 ME 网络**。只把成品放入连接了<ItemLink id="storage_bus" />的外部箱子，并不总能让系统确认加工任务完成。
5. 如果产物是另一份配方的中间材料，合成 CPU 会暂存它并继续下一步操作，直到最终产物完成。

**注意：**如果希望自动合成特定材料，不要让手动合成操作与仓库中已有材料的取用相互冲突。可以先在终端里确认任务预览，检查是否存在意外的材料消耗。

## 样板类型

<ItemImage id="crafting_pattern" scale="4" />

使用<ItemLink id="pattern_encoding_terminal" />将配方写入空白样板。不同样板适合不同加工方式：

* <ItemLink id="crafting_pattern" />：编码普通工作台合成配方。它可以放入<ItemLink id="molecular_assembler" />，也可以由<ItemLink id="pattern_provider" />把材料和合成信息交给相邻的分子装配室。

* <ItemLink id="smithing_table_pattern" />：记录锻造台配方，可以使用对应的自动合成设备处理。

* <ItemLink id="stonecutting_pattern" />：记录切石机配方，与普通合成、锻造样板配合使用。

* <ItemLink id="processing_pattern" />：记录其他机器的加工流程。你只需要告诉网络“投入哪些材料，最终会返回哪些产物”，具体加工可以由机械动力、Mekanism 或自行设计的复杂产线完成。

处理样板特别灵活。例如，设定“投入 8 个圆石，产出 8 个石头”，样板供应器就会按批次投放材料；多台安装相同样板的供应器也能够并行工作。

## 使用标准发信器控制生产

<ItemLink id="level_emitter" />安装合成卡后，可以在库存低于阈值时请求生产指定物品。这种方式无需按传统处理样板投入原料，非常适合控制自动刷石机、农场或循环加工流水线。

## 合成 CPU

<GameScene zoom="4" background="transparent">
  <ImportStructure src="../assets/assemblies/crafting_cpus.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

合成 CPU 负责处理自动合成请求，并暂存加工过程中的中间产物。它是多方块结构，必须形成完整的长方体，且至少包含一个合成存储组件。

CPU 可使用以下部件：

* **必需**：[合成存储组件](../items-blocks-machines/crafting_cpu_multiblock.md)。不同容量等级（1K、4K、16K、64K、256K）决定能同时缓存多少中间材料。
* **可选**：<ItemLink id="crafting_accelerator" />，提高可同时分发的材料批次数量，使一个样板供应器能够更充分地利用周围的多个分子装配室。
* **可选**：<ItemLink id="crafting_monitor" />，显示 CPU 当前处理的任务。还可以用<ItemLink id="color_applicator" />更改颜色。
* **可选**：<ItemLink id="crafting_unit" />，用于补齐结构形状。

每个合成 CPU 同一时间只能处理一个合成请求。如果要同时运行两个独立任务，就需要至少两个 CPU。它们既可以接受玩家手动发起的任务，也能处理自动维持库存的任务。

## 样板供应器

<Row>
<BlockImage id="pattern_provider" scale="4" />

<BlockImage id="pattern_provider" p:push_direction="up" scale="4" />

<GameScene zoom="4" background="transparent">
  <ImportStructure src="../assets/blocks/cable_pattern_provider.snbt" />
</GameScene>
</Row>

<ItemLink id="pattern_provider" />负责向相邻容器或机器输出样板规定的原料，并接收加工后的产物。对于一些设备，可以直接把产物推回供应器，而不必额外安装<ItemLink id="import_bus" />。

样板供应器通常不会长期存放待加工原料，而是从[合成存储](../items-blocks-machines/crafting_cpu_multiblock.md#crafting-storage)中按需取出。因此，不能把它当作普通物品箱使用。

样板供应器会尽量一次性发送完整配方所需的全部材料，避免机器只收到半份原料。它还可以与[子网络](../ae2-mechanics/subnetworks.md)组合使用，让材料直接送入子网的[存储系统](../ae2-mechanics/import-export-storage.md)。

多个样板供应器可以安装相同样板，并行执行相同配方。

### 三种安装形式

* **普通方块形态**：可以从多个面向机器推送材料，也能从各面接收返回的产物。
* **定向形态**：使用<ItemLink id="certus_quartz_wrench" />调整输出面，只从指定方向推出原料，不会通过该输出面连接 ME 网络。
* **扁平形态**：属于[线缆附属部件](../ae2-mechanics/cable-subparts.md)，占用空间小，适合紧凑布局。

普通形态与扁平形态可以通过合成栏互相转换。

### 运行设置

* **阻塞模式**：目标机器还有尚未处理的原料时，暂停发送下一批。
* **锁定合成**：在满足特定红石条件或收到加工结果前保持锁定，避免多个任务相互干扰。
* **样板管理终端**：可以选择是否在<ItemLink id="pattern_access_terminal" />中显示该供应器。
* **优先级**：若多张样板都能制作同种物品，优先考虑较高优先级的供应器；如果材料不足，再考虑其他配方。

## 分子装配室

<BlockImage id="molecular_assembler" scale="4" />

<ItemLink id="molecular_assembler" />负责执行工作台、锻造台或切石机类的自动合成。它可以识别<ItemLink id="crafting_pattern" />、<ItemLink id="smithing_table_pattern" />和<ItemLink id="stonecutting_pattern" />，从相邻的<ItemLink id="pattern_provider" />接收材料和配方信息，并将成品输出回网络。

一个样板供应器周围可以连接多台分子装配室，实现并行制作。

<GameScene zoom="4" background="transparent">
<ImportStructure src="../assets/assemblies/assembler_tower.snbt" />
<IsometricCamera yaw="195" pitch="30" />
</GameScene>
