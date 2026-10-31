---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 样板供应器
  icon: pattern_provider
  position: 210
categories:
- devices
item_ids:
- ae2:pattern_provider
- ae2:cable_pattern_provider
---

# 样板供应器

<Row gap="20">
<BlockImage id="pattern_provider" scale="8" />
<BlockImage id="pattern_provider" p:push_direction="up" scale="8" />
<GameScene zoom="8" background="transparent">
  <ImportStructure src="../assets/blocks/cable_pattern_provider.snbt" />
</GameScene>
</Row>

样板供应器是[自动合成](../ae2-mechanics/autocrafting.md)系统向世界输出原料、接收成品的主要设备。它会根据安装的[样板](patterns.md)将一整批原料送入相邻的机器或容器。加工完成后，可以让机器将产物直接送回样板供应器，也可以使用<ItemLink id="import_bus" />将产物送回 ME 网络。

样板供应器的原料来自合成 CPU 的[合成存储](crafting_cpu_multiblock.md#crafting-storage)，**它自身并不是普通存储箱**。需要先让它把材料送入相邻容器，再由其他设备分配。

供应器会尽量将完整批次的原料一次性送出，不会主动发送半份配方。结合[子网络](../ae2-mechanics/subnetworks.md)和未配置缓存的接口，可以直接向子网的[存储系统](../ae2-mechanics/import-export-storage.md)投料，避免在接口中途缓存不完整的配方。

例如，下面的布局可以将燃料和待熔炼物分别送入熔炉对应的输入位置：

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/furnace_automation.snbt" />

<BoxAnnotation color="#dddddd" min="1 0 0" max="2 1 1">
        （1）样板供应器：使用赛特斯石英扳手设置输出方向，并安装对应处理样板

        ![熔炼样板](../assets/diagrams/furnace_pattern_small.png)
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="1 1 0" max="2 1.3 1">
        （2）接口：保持默认设置，作为子网连接。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="1 1 0" max="1.3 2 1">
        （3）存储总线 1：只接收煤炭。
        <ItemImage id="minecraft:coal" scale="2" />
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="0 2 0" max="1 2.3 1">
        （4）存储总线 2：安装反转卡并排除煤炭。
        <Row><ItemImage id="minecraft:coal" scale="2" /><ItemImage id="inverter_card" scale="2" /></Row>
  </BoxAnnotation>

<DiamondAnnotation pos="4 0.5 0.5" color="#00ff00">
        转到主网络
    </DiamondAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

下面的示例展示了如何把同一批材料分配给多台机器：

<GameScene zoom="6" background="transparent">
<ImportStructure src="../assets/assemblies/provider_interface_storage.snbt" />

<BoxAnnotation color="#dddddd" min="2.7 0 1" max="3 1 2">
        扁平接口（不能使用完整方块接口）
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="1 0 0" max="1.3 1 4">
        外部容器
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="0 0 0" max="1 1 4">
        样板供应器的目标机器或输入位置
  </BoxAnnotation>

<IsometricCamera yaw="185" pitch="30" />
</GameScene>

多个供应器可以安装相同的样板并行工作。普通方块形态的供应器还会尝试向相邻的各个有效面输出原料，以同时利用多台加工设备。

## 三种形态

样板供应器分为普通、定向和[扁平线缆附属部件](../ae2-mechanics/cable-subparts.md)三种形态：

* **普通形态**：可以向多个方向输出材料，也能从不同方向接收产物，并在各个可连接面传递网络。
* **定向形态**：使用<ItemLink id="certus_quartz_wrench" />设置指定输出面。它不会通过该输出面传递网络连接，适合用来分隔[子网](../ae2-mechanics/subnetworks.md)。
* **扁平形态**：安装在[线缆附属部件](../ae2-mechanics/cable-subparts.md)的位置，节省空间，具有定向输出的特性。

普通与扁平形态可以通过合成栏相互转换。

## 设置与优先级

* **阻塞模式**：目标机器中还有未处理的原料时，不再投放下一批。
* **锁定合成**：通过红石状态或等待产物返回的条件锁定供应器，避免工序混乱。
* **可见性**：选择是否在<ItemLink id="pattern_access_terminal" />中显示该供应器。
* **优先级**：多张[样板](patterns.md)可以生成相同物品时，会优先使用较高优先级的供应器；如果该配方原料不足，系统可以考虑其他配方。

## 常见误解：样板供应器不是物品管道

部分玩家会把样板供应器连接到一段普通[线缆](cables.md)，再在远端安装<ItemLink id="export_bus" />，希望输出总线自动把样板原料送进机器。

**这种结构不会按预期工作。** 普通 ME 线缆只负责连接网络，不是运输物品的管道。样板供应器需要直接对着可接受材料的机器或容器输出。

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../assets/assemblies/provider_misconception_1.snbt" />

  <BoxAnnotation color="#dddddd" min="1 0 3" max="2 1 4">
        错误连接：无法执行这批自动合成
  </BoxAnnotation>

  <IsometricCamera yaw="95" pitch="5" />
</GameScene>

在错误布线中，样板供应器的输出面没有相邻容器，因此无法把原料推出。它仅通过线缆与输出总线连接，不会自动改变输出总线的过滤设置。

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../assets/assemblies/provider_misconception_2.snbt" />

  <BoxAnnotation color="#dddddd" min="1 0 3" max="2 1 4">
        错误连接：无法执行这批自动合成
  </BoxAnnotation>

  <IsometricCamera yaw="95" pitch="5" />
</GameScene>

正确做法是将样板供应器的输出面对准实际处理原料的机器或中间容器，例如：

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../assets/assemblies/provider_misconception_3.snbt" />

  <BoxAnnotation color="#dddddd" min="1 0 3" max="2 1 4">
        错误连接：无法执行这批自动合成
  </BoxAnnotation>

  <IsometricCamera yaw="95" pitch="5" />
</GameScene>

## 合成配方

<RecipeFor id="pattern_provider" />

<RecipeFor id="cable_pattern_provider" />
