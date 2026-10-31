---
navigation:
  parent: example-setups/example-setups-index.md
  title: 半自动赛特斯石英农场
  icon: certus_quartz_crystal
  position: 115
---

# 半自动赛特斯石英农场

[基础农场](simple-certus-farm.md)若要长期全自动运行，通常需要<ItemLink id="flawless_budding_quartz" />，这意味着你得在[陨石](../ae2-mechanics/meteorites.md)附近建造农场，或者使用[空间存储](../ae2-mechanics/spatial-io.md)搬运无瑕母岩。

如果使用会退化的赛特斯石英母岩，可以搭建这套半自动系统。它会自动收割成熟晶簇，并在旧母岩耗尽后放置新的<ItemLink id="flawed_budding_quartz" />。

玩家只需要定期向输入容器补充新母岩，并取出退化后的<ItemLink id="quartz_block" />，就可以继续运行。若希望连这些步骤也自动完成，请参考[高级农场](advanced-certus-farm.md)。

农场由**三个独立子网**组成，图中部分设备彼此遮挡，建议旋转场景从各个方向观察。

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/semiauto_certus_farm.snbt" />

  <BoxAnnotation color="#ddaaaa" min="3.7 2 1" max="4 3 2">
        (1)（1）破坏面板 1：可附魔时运，专门收获成熟晶簇。
  </BoxAnnotation>

  <BoxAnnotation color="#ddaaaa" min="2 2 1" max="2.3 3 2">
        (2)（2）存储总线 1：只允许赛特斯石英水晶。
        <ItemImage id="certus_quartz_crystal" scale="2" />
  </BoxAnnotation>

  <DiamondAnnotation pos="3 2.5 1.5" color="#ff0000">
    晶簇收获子网
  </DiamondAnnotation>

  <BoxAnnotation color="#aaddaa" min="3.7 1 1" max="4 2 2">
        (3)（3）破坏面板 2：必须附魔精准采集，用于收回退化的赛特斯石英块。
  </BoxAnnotation>

  <BoxAnnotation color="#aaddaa" min="2 1 1" max="2.3 2 2">
        (4)（4）存储总线 2：只允许赛特斯石英块。
        <BlockImage id="quartz_block" scale="2" />
  </BoxAnnotation>

  <DiamondAnnotation pos="3 1.5 1.5" color="#00ff00">
    母岩回收子网
  </DiamondAnnotation>

  <BoxAnnotation color="#ffddaa" min="4 0.7 1" max="5 1 2">
        (5)（5）成型面板：保持默认设置，用于放置新的母岩。
  </BoxAnnotation>

  <BoxAnnotation color="#ffddaa" min="2 0 1" max="2.3 1 2">
        (6)（6）输入总线：从补货容器提取母岩。
  </BoxAnnotation>

  <DiamondAnnotation pos="3 0.5 1.5" color="#ddcc00">
    母岩放置子网
  </DiamondAnnotation>

  <BoxAnnotation color="#aaaadd" min="0.7 2 1" max="1 3 2">
        (7)（7）存储总线 3：筛选赛特斯石英水晶，优先级高于主网存储。
        <ItemImage id="certus_quartz_crystal" scale="2" />
  </BoxAnnotation>

    <DiamondAnnotation pos="1.5 0.5 1.5" color="#00ff00">
        在此手动放入新的赛特斯石英母岩
        <BlockImage id="flawed_budding_quartz" scale="2" />
    </DiamondAnnotation>

    <DiamondAnnotation pos="1.5 1.5 1.5" color="#00ff00">
        从这里手动取出退化后的赛特斯石英块。
        <BlockImage id="quartz_block" scale="2" />
    </DiamondAnnotation>

<DiamondAnnotation pos="0.5 0.5 0" color="#00ff00">
        连接主网络
    </DiamondAnnotation>

  <IsometricCamera yaw="165" pitch="5" />
</GameScene>

## 配置

### 晶簇收获子网

* <ItemLink id="annihilation_plane" />（1）：可附魔时运。
* <ItemLink id="storage_bus" />（2）：只允许<ItemLink id="certus_quartz_crystal" />，避免收获未成熟晶芽。

### 母岩回收子网

* <ItemLink id="annihilation_plane" />（3）：必须附魔精准采集。
* <ItemLink id="storage_bus" />（4）：只允许<ItemLink id="quartz_block" />。

### 母岩放置子网

* <ItemLink id="formation_plane" />（5）：保持默认设置。
* <ItemLink id="import_bus" />（6）：从补货容器提取新母岩。

### 主网络

* <ItemLink id="storage_bus" />（7）：筛选<ItemLink id="certus_quartz_crystal" />，且[优先级](../ae2-mechanics/import-export-storage.md#storage-priority)高于主仓库。

## 工作流程

1. 晶簇收获子网只接收成熟晶簇产生的赛特斯石英水晶，将水晶送入专用容器。
2. 母岩逐渐退化为普通赛特斯石英块时，附魔精准采集的第二个破坏面板将其取出，并送入回收容器。
3. 定期取出回收的石英块，与<ItemLink id="charged_certus_quartz_crystal" />投入水中，制作新的母岩，再放入补货容器。
4. 母岩放置子网从补货容器提取<ItemLink id="flawed_budding_quartz" />，由成型面板自动放回农场。
5. 高优先级存储总线把收获的水晶提供给主网，同时保证晶体优先留在专用缓冲容器。

