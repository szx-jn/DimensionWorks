---
navigation:
  parent: example-setups/example-setups-index.md
  title: 基础赛特斯石英农场
  icon: certus_quartz_crystal
  position: 110
---

# 基础赛特斯石英农场

根据[赛特斯石英生长机制](../ae2-mechanics/certus-growth.md)，可以使用<ItemLink id="annihilation_plane" />和<ItemLink id="storage_bus" />自动收获<ItemLink id="certus_quartz_crystal" />。

用<ItemLink id="growth_accelerator" />加速母岩上的石英芽生长，并让破坏面板只破坏成熟的<ItemLink id="quartz_cluster" />。由于不成熟的石英芽只会掉落<ItemLink id="certus_quartz_dust" />，筛选赛特斯石英水晶即可避免误收获未成熟晶芽。

如果使用<ItemLink id="flawless_budding_quartz" />，农场可以长期自动运作。若使用其他品质的母岩，母岩会逐渐退化，需要手动更换或改用[半自动农场](semiauto-certus-farm.md)及[高级农场](advanced-certus-farm.md)。

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/simple_certus_farm.snbt" />

  <BoxAnnotation color="#dddddd" min="3.7 1 1" max="4 2 2">
        （1）破坏面板：无需界面设置，可以附魔时运提高成熟晶簇的掉落量。
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="3 1 1" max="3.3 2 2">
        （2）存储总线 1：只允许赛特斯石英水晶进入。
        <ItemImage id="certus_quartz_crystal" scale="2" />
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="3 1 .7" max="2 2 1">
        （3）存储总线 2：同样筛选赛特斯石英水晶，但优先级高于主网络中的存储设备。
        <ItemImage id="certus_quartz_crystal" scale="2" />
  </BoxAnnotation>

<DiamondAnnotation pos="1 0.5 0.5" color="#00ff00">
        连接主网络
    </DiamondAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 配置

* <ItemLink id="annihilation_plane" />（1）：无需打开界面，可附魔时运。
* 第一个<ItemLink id="storage_bus" />（2）：仅允许<ItemLink id="certus_quartz_crystal" />。
* 第二个<ItemLink id="storage_bus" />（3）：同样筛选<ItemLink id="certus_quartz_crystal" />，且[存储优先级](../ae2-mechanics/import-export-storage.md#storage-priority)高于主网络。

## 工作流程

1. <ItemLink id="annihilation_plane" />尝试破坏前方的晶芽或晶簇。由于子网上唯一的存储目标仅接收<ItemLink id="certus_quartz_crystal" />，只有成熟的<ItemLink id="quartz_cluster" />能被有效收获。
2. 第一个<ItemLink id="storage_bus" />把水晶存入中转容器。
3. 第二个<ItemLink id="storage_bus" />将容器中的水晶提供给主网络。由于优先级较高，收获的晶体会优先进入专用容器，而不是散落在其他仓库。

