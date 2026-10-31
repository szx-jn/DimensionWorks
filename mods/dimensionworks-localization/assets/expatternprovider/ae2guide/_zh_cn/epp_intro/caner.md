---
navigation:
    parent: epp_intro/epp_intro-index.md
    title: ME 罐装机
    icon: expatternprovider:caner
categories:
- extended devices
item_ids:
- expatternprovider:caner
---

# ME 罐装机

<BlockImage id="expatternprovider:caner" scale="8"></BlockImage>

ME 罐装机可以在不同容器之间**灌装或抽取资源**。除流体以外，安装对应附属模组后，还能处理 Mekanism 化学品、Botania 魔力以及能量等资源。

第一个槽位用于放置待处理的容器，第二个槽位用于提供灌装材料。机器需要能源才能工作，每次操作消耗 **80 AE**。

![罐装机界面](../pic/caner_gui.png)

默认只支持流体。若要处理其他资源，需要安装对应的扩展支持。

### 相关附属模组

* Applied Flux
* Applied Mekanistics
* Applied Botanics

## 自动化罐装

只有顶部和底部能连接到 ME 网络并接收能源。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../structure/caner_example.snbt"></ImportStructure>
</GameScene>

可以使用<ItemLink id="ae2:pattern_provider" />将材料和容器送入罐装机，建立自动化灌装产线。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../structure/caner_auto.snbt"></ImportStructure>
</GameScene>

处理样板只需要包含待灌装的资源和目标容器。例如：

将水装入水桶：

![灌装水桶样板](../pic/fill_water.png)

给能量容器充能（需要 Applied Flux）：

![充能样板](../pic/fill_energy.png)

## 反向操作

罐装机还可以把容器中的资源抽出。此时需要在处理样板中交换输入与输出的配置。
