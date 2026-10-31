---
navigation:
  parent: example-setups/example-setups-index.md
  title: 物品与流体物流子网
  icon: storage_bus
---

# 物品与流体物流子网

ME 网络不仅能存储物品，还能利用[设备](../ae2-mechanics/devices.md)构建类似管道的自动物流系统。只需使用少量输入总线、输出总线和存储总线，就能把物品或流体从一个容器送往另一个容器，例如把加工产物送回<ItemLink id="pattern_provider" />。

下面介绍几种常用结构。

## 输入总线 → 存储总线

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/import_storage_pipe.snbt" />

<BoxAnnotation color="#dddddd" min="3.7 0 0" max="4 1 1">
        (1)输入总线:可设置筛选条件。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="1 0 0" max="1.3 1 1">
        (2)存储总线:可设置筛选条件。这个(和您想要成为目标容器的其他存储总线)
        必须是唯一存储在网络上的存储。
  </BoxAnnotation>

<DiamondAnnotation pos="4.5 0.5 0.5" color="#00ff00">
        来源容器
    </DiamondAnnotation>

<DiamondAnnotation pos="0.5 0.5 0.5" color="#00ff00">
        目标容器
    </DiamondAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

<ItemLink id="import_bus" />（1）从来源容器抽取物品或流体，然后尝试存入子网的[网络存储](../ae2-mechanics/import-export-storage.md)。

因为子网唯一的存储目标是<ItemLink id="storage_bus" />（2）连接的目标容器，资源就会直接转移过去。可利用<ItemLink id="quartz_fiber" />从主网络共享能源，但不会把两个网络合并。

两种总线都可以设置筛选条件，默认则处理所有可访问的资源。这种结构也支持多个输入端和多个目标容器。

## 存储总线 → 输出总线

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/storage_export_pipe.snbt" />

<BoxAnnotation color="#dddddd" min="3.7 0 0" max="4 1 1">
        (1)存储总线:可设置筛选条件。此存储总线（以及其他指向原料容器的存储总线）必须是子网内唯一的存储方式。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="1 0 0" max="1.3 1 1">
        (2)输出总线:必须设置筛选条件。
  </BoxAnnotation>

<DiamondAnnotation pos="4.5 0.5 0.5" color="#00ff00">
        来源容器
    </DiamondAnnotation>

<DiamondAnnotation pos="0.5 0.5 0.5" color="#00ff00">
        目标容器
    </DiamondAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

<ItemLink id="storage_bus" />（1）把来源容器中的物品视作子网的存储资源，<ItemLink id="export_bus" />（2）再将符合条件的资源送往目标容器。

**输出总线必须配置筛选物品**，否则不会主动输出。这种结构同样支持多个源容器和多个输出端口。

## 无效结构：输入总线 → 输出总线

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/import_export_pipe.snbt" />

<BoxAnnotation color="#dd3333" min="3.7 0 0" max="4 1 1">
        输入总线：网络没有存储目标，无法导入物品。
  </BoxAnnotation>

<BoxAnnotation color="#dd3333" min="1 0 0" max="1.3 1 1">
        (2)输出总线：网络没有可提取的存储资源，无法输出。
  </BoxAnnotation>

<DiamondAnnotation pos="4.5 0.5 0.5" color="#ff0000">
        来源容器
    </DiamondAnnotation>

<DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
        目标容器
    </DiamondAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

如果子网只有输入总线与输出总线，而没有任何存储设备，那么两者都无法工作：输入总线没有存储目标，输出总线也没有资源来源。

因此，要让物流子网正常运转，至少需要一个充当存储连接点的<ItemLink id="storage_bus" />或其他存储设备。

## 同一个方块面同时输入与输出

有些机器可以在同一侧接收原料并输出成品，例如<ItemLink id="charger" />。可以将上述两类物流子网组合使用：一条负责送入原料，另一条负责取出结果。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/import_storage_export_pipe.snbt" />

<BoxAnnotation color="#dddddd" min="4 1 1" max="5 1.3 2">
        (1)输入总线:可设置筛选条件。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="2 1 1" max="3 1.3 2">
        (2)存储总线:可设置筛选条件。这个(和您想要推动和拉动的物品的其他存储总线)
        必须是唯一存储在网络上的存储。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="2 0 1" max="3 1 2">
        (3)需要同时输入原料、输出产物的机器，这里以充电器为例。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="0 1 1" max="1 1.3 2">
        (4)输出总线:必须设置筛选条件。
  </BoxAnnotation>

<DiamondAnnotation pos="4.5 0.5 1.5" color="#00ff00">
        来源容器
    </DiamondAnnotation>

<DiamondAnnotation pos="0.5 0.5 1.5" color="#00ff00">
        目标容器
    </DiamondAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 使用 ME 接口作为中转站

<ItemLink id="interface" />也能充当子网与外部容器之间的中转点。

没有设置常备库存的接口会尝试把外部输入的物品送入网络存储，因此可以替代“输入总线 → 存储总线”方案中的输入端。

反过来，在接口中设置需要保持的物品数量，它就会从网络存储中补充物品，借助存储总线可以将这些物品提供给其他设备。

<GameScene zoom="6" background="transparent">
<ImportStructure src="../assets/assemblies/interface_pipes.snbt" />

<BoxAnnotation color="#dddddd" min="3.7 0 0" max="4 1 1">
        ME 接口
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="1 0 0" max="1.3 1 1">
        存储总线
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="3.7 0 2" max="4 1 3">
        存储总线
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="0 1 2" max="1 1.3 3">
        存储总线
  </BoxAnnotation>

<IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 一对多与多对多传输

子网并不要求只有一个<ItemLink id="import_bus" />、<ItemLink id="export_bus" />或<ItemLink id="storage_bus" />。按照实际需求增加多个输入和输出端点，可以搭建更复杂的物流分配网络。

<GameScene zoom="3" background="transparent">
<ImportStructure src="../assets/assemblies/many_to_many_pipe.snbt" />

<IsometricCamera yaw="185" pitch="30" />
</GameScene>

## 一份样板原料分送到多个位置

有时<ItemLink id="pattern_provider" />需要将一份配方原料送到不同机器，或同一台机器的不同输入面。样板供应器在开始加工时会直接向相邻容器**推出**材料，不会先把材料保存在自身库存中。

因此，这里需要一台相邻的<ItemLink id="interface" />接收样板供应器的原料，再通过存储总线把材料送往多个加工位置。

注意接口和样板供应器的实际连接方式：如果要阻止两个 ME 网络意外合并，应使用合适朝向的**平面附属部件形式**，而不是随意连接两个完整方块设备。

<GameScene zoom="6" background="transparent">
<ImportStructure src="../assets/assemblies/provider_interface_storage.snbt" />

<BoxAnnotation color="#dddddd" min="2.7 0 1" max="3 1 2">
        ME 接口：必须使用平面附属部件形式，不能使用完整方块。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="1 0 0" max="1.3 1 4">
        存储容器
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="0 0 0" max="1 1 4">
        需要接收样板原料的位置，可以是多台机器或同一台机器的不同面。
  </BoxAnnotation>

<IsometricCamera yaw="185" pitch="30" />
</GameScene>

