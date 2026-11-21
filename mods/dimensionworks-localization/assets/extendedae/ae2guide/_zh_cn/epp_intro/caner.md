---
navigation:
    parent: epp_intro/epp_intro-index.md
    title: ME 灌装机
    icon: extendedae:caner
categories:
- extended devices
item_ids:
- extendedae:caner
---

# ME 灌装机

<BlockImage id="extendedae:caner" scale="8"></BlockImage>

ME 灌装机用于“灌装”材料，包括流体、Mekanism 气体、植物魔法魔力，甚至能量！

第一个槽位放置填充材料，第二个槽位放置需要填充的物品。

它需要能量才能运行，每次操作消耗 80 AE。

![GUI](../pic/caner_gui.png)

默认情况下，它只能灌装流体；安装相应的附属模组后，才能灌装其他类型的材料。

### 支持的附属模组
- Applied Flux
- Applied Mekanistics
- Applied Botanics Addon

## 使用 ME 灌装机自动合成

只有顶部和底部可以接入能量并连接网络。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../structure/caner_example.snbt"></ImportStructure>
</GameScene>

这是一个简单的 ME 灌装机装置。ME 灌装机从<ItemLink id="ae2:pattern_provider" />收到材料后，会自动弹出灌装完成的物品。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../structure/caner_auto.snbt"></ImportStructure>
</GameScene>

样板中只能包含填充材料和需要灌装的容器。以下是一些示例：

灌装水桶：

![P1](../pic/fill_water.png)

强化能量板（需要 Applied Flux）：

![P1](../pic/fill_energy.png)

## 抽空

ME 灌装机也可以在空置模式下从容器中抽出材料。你需要在样板中调换输入和输出。
