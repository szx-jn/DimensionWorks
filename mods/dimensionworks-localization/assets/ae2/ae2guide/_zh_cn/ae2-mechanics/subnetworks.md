---
navigation:
  parent: ae2-mechanics/ae2-mechanics-index.md
  title: 子网络
---

# 子网络

<GameScene zoom="4" interactive={true}>
<ImportStructure src="../assets/assemblies/subnet_demonstration.snbt" />

<DiamondAnnotation pos="6.5 2.5 0.5" color="#00ff00">
    物品管道子网
</DiamondAnnotation>

<DiamondAnnotation pos="5.5 2.5 0.5" color="#00ff00">
    流体管道子网
</DiamondAnnotation>

<DiamondAnnotation pos="4.5 2.5 0.5" color="#00ff00">
    带过滤功能的破坏面板
</DiamondAnnotation>

<DiamondAnnotation pos="3.5 2.5 0.5" color="#00ff00">
    成型面板子网
</DiamondAnnotation>

<DiamondAnnotation pos="2.5 2.5 0.5" color="#00ff00">
    通过接口和存储总线，让主网络访问局部存储
</DiamondAnnotation>

<DiamondAnnotation pos="1.5 1.5 0.5" color="#00ff00">
    将完成充能的物品送回样板供应器
</DiamondAnnotation>

<IsometricCamera yaw="195" pitch="30" />
</GameScene>

**子网络（子网）**是与主 ME 网络相互独立、专门执行某些工作的较小网络。它们通常不需要单独的控制器，主要有两类用途：

* **限制设备可访问的存储范围。** 例如，把输入总线和存储总线放在一个管道子网中，让物品从一个容器转移到另一个容器，而不是进入主网仓库。
* **节省主网络频道。** 你可以让一条子网同时服务多台机器，而主网只需通过少量接口与之交互。

注意：不同颜色的线缆可以阻止彼此直接连接，但**给线缆染色本身并不会创建子网**，还需要合理分隔网络结构。

常见应用包括：

* 使用输入总线与存储总线，在两个容器之间转运物品或流体，模拟物品管道、流体管道。
* 使用破坏面板与存储总线，让破坏的方块只进入指定容器，实现受控收集。
* 使用接口与成型面板，把主网送来的材料自动放置到世界中。
* 与主网的<ItemLink id="level_emitter" />配合，让小型农场按库存需求启动和停止。
* 将农场产物存入独立的本地仓库，再通过存储总线按需向主网开放。

<ItemLink id="quartz_fiber" />能够在网络之间传递能源，而不让两边合并。这样可以让主网给子网供能，无需为每个子网单独建立能源接收系统。
