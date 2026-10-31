---
navigation:
  parent: example-setups/example-setups-index.md
  title: 自动紫水晶农场
  icon: minecraft:amethyst_shard
---

# 自动紫水晶农场

[晶体催生器](../items-blocks-machines/growth_accelerator.md)既可以加速赛特斯石英生长，也可以用于紫水晶农场。

不过，普通紫水晶芽在被破坏时不会掉落物品。若直接使用<ItemLink id="annihilation_plane" />，便无法通过掉落物筛选成熟晶簇。解决方法是先给第一个破坏面板附魔**精准采集**，让不同生长阶段的紫水晶芽变成可以筛选的方块物品。

第一个破坏面板将成熟的<ItemLink id="minecraft:amethyst_cluster" />采集下来，<ItemLink id="formation_plane" />再把成熟晶簇放到第二个破坏面板前。第二个面板不带精准采集，使用时运将晶簇转化为<ItemLink id="minecraft:amethyst_shard" />。

注意：紫水晶簇的朝向会影响能否正确放置，成型面板应面向合适的方向。

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/amethyst_farm.snbt" />

  <BoxAnnotation color="#dddddd" min="2.7 1 1" max="3 2 2">
        (1)破坏面板 1：必须附魔精准采集，用于收获完整的成熟紫水晶簇。
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="2 1 1" max="2.3 2 2">
        (2)成型面板：只允许放置成熟紫水晶簇。
        <ItemImage id="minecraft:amethyst_cluster" scale="2" />
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="1.3 0.7 1" max="2 1 2">
        (3)破坏面板 2：可以附魔时运，用于将晶簇加工为紫水晶碎片。
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="1 0 1" max="1.3 1 2">
        (4)存储总线 1：只允许紫水晶碎片。
        <ItemImage id="minecraft:amethyst_shard" scale="2" />
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="0 0 .7" max="1 1 1">
        (5)存储总线 2：只允许紫水晶碎片，优先级高于主网络存储。
        <ItemImage id="minecraft:amethyst_shard" scale="2" />
  </BoxAnnotation>

<DiamondAnnotation pos="0 0.5 0.5" color="#00ff00">
        连接主网络
    </DiamondAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 配置

* 第一个<ItemLink id="annihilation_plane" />（1）：必须附魔精准采集。
* <ItemLink id="formation_plane" />（2）：筛选<ItemLink id="minecraft:amethyst_cluster" />。
* 第二个<ItemLink id="annihilation_plane" />（3）：可以附魔时运，不要附魔精准采集。
* 第一个<ItemLink id="storage_bus" />（4）：筛选<ItemLink id="minecraft:amethyst_shard" />。
* 第二个<ItemLink id="storage_bus" />（5）：同样筛选<ItemLink id="minecraft:amethyst_shard" />，并将[优先级](../ae2-mechanics/import-export-storage.md#storage-priority)设为高于主网络。

## 工作流程

1. 第一个破坏面板采集成熟紫水晶簇。因为面板附有精准采集，晶芽和晶簇都会作为物品掉落，子网就可以依靠筛选规则只接受成熟晶簇。
2. 成型面板重新放置成熟晶簇，确保放置方向正确。
3. 第二个破坏面板破坏晶簇，得到紫水晶碎片；附魔时运可提高产量。
4. 第一个存储总线将碎片放入缓冲容器。
5. 第二个存储总线让主网络访问这些碎片。高优先级保证它们优先进入缓冲容器，而不是散落在其他存储设备中。

