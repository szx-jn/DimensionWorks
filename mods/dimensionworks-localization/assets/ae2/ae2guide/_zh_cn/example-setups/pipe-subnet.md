---
navigation:
  parent: example-setups/example-setups-index.md
  title: 物品与流体管道子网
  icon: storage_bus
---

# 物品与流体管道子网

AE2 的[设备](../ae2-mechanics/devices.md)可以组成小型独立网络，充当物品管道或流体管道。这样既能传送原料，也能把加工完成的物品送回<ItemLink id="pattern_provider" />。

常见方案有以下两种。

## 输入总线 → 存储总线

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/import_storage_pipe.snbt" />

<BoxAnnotation color="#dddddd" min="3.7 0 0" max="4 1 1">
        (1)输入总线:可设置筛选条件。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="1 0 0" max="1.3 1 1">
        （2）存储总线：可设置过滤规则。该子网中只能保留需要作为输出目标的存储设备。
  </BoxAnnotation>

<DiamondAnnotation pos="4.5 0.5 0.5" color="#00ff00">
        来源容器
    </DiamondAnnotation>

<DiamondAnnotation pos="0.5 0.5 0.5" color="#00ff00">
        目标容器
    </DiamondAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

<ItemLink id="import_bus" />从来源容器抽取资源并尝试存入[网络存储](../ae2-mechanics/import-export-storage.md)。由于这个子网的唯一存储入口是<ItemLink id="storage_bus" />，资源只能进入存储总线所连接的目标容器。

可以使用<ItemLink id="quartz_fiber" />从主网共享能源，而不让两个网络直接合并。

输入总线和存储总线都能设置过滤规则。也可以使用多个输入总线或多个目标存储总线，搭建一对多传输系统。

## 存储总线 → 输出总线

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/storage_export_pipe.snbt" />

<BoxAnnotation color="#dddddd" min="3.7 0 0" max="4 1 1">
        （1）存储总线：可设置过滤规则。该子网应只包含需要作为物品来源的存储设备。
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

<ItemLink id="export_bus" />从[网络存储](../ae2-mechanics/import-export-storage.md)提取资源。这个子网唯一的存储来源是<ItemLink id="storage_bus" />连接的容器，因此输出总线会从该容器取出指定物品。

输出总线**必须设置过滤规则**才能工作。也可以用<ItemLink id="quartz_fiber" />共享能源，并根据需要增加更多来源或输出端。

## 错误示例：输入总线 → 输出总线

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

仅把输入总线与输出总线相连，**不会形成有效管道**。输入总线需要一个存储目标才能存入物品，输出总线也需要一个存储来源才能提取物品；如果网络中完全没有存储设备，两端都无法运行。

## 对同一台机器输入并回收产物

像<ItemLink id="charger" />这样通过同一个面接受输入与输出的机器，可以组合上述两种管道子网，让原料自动进入，成品自动离开。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/import_storage_export_pipe.snbt" />

<BoxAnnotation color="#dddddd" min="4 1 1" max="5 1.3 2">
        (1)输入总线:可设置筛选条件。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="2 1 1" max="3 1.3 2">
        （2）存储总线：负责访问机器内部库存，是子网中的唯一资源存储入口。
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

## 使用 ME 接口

除了输入、输出总线，<ItemLink id="interface" />也能与[网络存储](../ae2-mechanics/import-export-storage.md)交换物品。

接口未配置目标库存时，可以接收物品并将其送回网络；设置库存目标后，则可以主动从网络提取并准备指定物品。将接口与存储总线配合，就能灵活设计双向资源交换流程。

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

## 一对多、多对一与多对多

并不要求每个管道子网只有一台<ItemLink id="import_bus" />、<ItemLink id="export_bus" />或<ItemLink id="storage_bus" />。根据需求可以组合多个来源和目标，利用过滤和优先级控制流向。

<GameScene zoom="3" background="transparent">
<ImportStructure src="../assets/assemblies/many_to_many_pipe.snbt" />

<IsometricCamera yaw="185" pitch="30" />
</GameScene>

## 把一份样板的材料分发到多个位置

样板供应器<ItemLink id="pattern_provider" />通常直接向相邻容器输出材料，并不长期存储原料。因此，不能把它直接当作普通箱子连接到管道子网。

可以使用<ItemLink id="interface" />作为中间连接点，让样板供应器把整批原料送入子网，再由多个存储总线把不同原料分配到不同机器或一台机器的不同输入面。

要确保定向样板供应器和扁平接口没有意外把两个 ME 网络合并。

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
