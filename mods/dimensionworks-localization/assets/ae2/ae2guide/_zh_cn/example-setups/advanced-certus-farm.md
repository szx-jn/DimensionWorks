---
navigation:
  parent: example-setups/example-setups-index.md
  title: 高级全自动赛特斯石英农场
  icon: certus_quartz_crystal
  position: 120
---

# 高级全自动赛特斯石英农场

这套装置基于[半自动赛特斯石英农场](semiauto-certus-farm.md)，并将母岩的回收、修复和补充全部接入 ME 网络。

不同于半自动方案需要人工补充新母岩、取走退化的石英块，高级农场结合[充电器自动化](charger-automation.md)与[投入水中自动加工](throw-in-water-automation.md)，使用退化的石英块与充能赛特斯石英水晶制作新的母岩，再自动放回原位。

**完整装置较为紧凑，部分组件会遮挡其他部件。请旋转三维场景，从各个方向查看线路。**

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/advanced_certus_farm.snbt" />

  <BoxAnnotation color="#ddaaaa" min="3.7 2 1" max="4 3 2">
        （1）破坏面板 1：可附魔时运，用于收割成熟晶簇。
  </BoxAnnotation>

  <BoxAnnotation color="#ddaaaa" min="2 2 1.7" max="3 3 2">
        （2）存储总线 1：筛选赛特斯石英水晶。
        <ItemImage id="certus_quartz_crystal" scale="2" />
  </BoxAnnotation>

  <DiamondAnnotation pos="3 2.5 1.5" color="#ff0000">
    晶簇收获子网
  </DiamondAnnotation>

  <BoxAnnotation color="#aaddaa" min="3.7 1 1" max="4 2 2">
        （3）破坏面板 2：必须附魔精准采集，取回退化的母岩。
  </BoxAnnotation>

  <BoxAnnotation color="#aaddaa" min="2 1 1.7" max="3 2 2">
        （4）存储总线 2：筛选赛特斯石英块。
        <BlockImage id="quartz_block" scale="2" />
  </BoxAnnotation>

  <DiamondAnnotation pos="3 1.5 1.5" color="#00ff00">
    母岩回收子网
  </DiamondAnnotation>

  <BoxAnnotation color="#ffddaa" min="4 0.7 1" max="5 1 2">
        （5）成型面板：默认配置，负责放置补充的母岩。
  </BoxAnnotation>

  <BoxAnnotation color="#ffddaa" min="2 0.7 2" max="3 1 3">
        （6）输入总线：筛选有瑕赛特斯石英母岩。
        <BlockImage id="flawed_budding_quartz" scale="2" />
  </BoxAnnotation>

  <DiamondAnnotation pos="3 0.5 1.5" color="#ddcc00">
    母岩放置子网
  </DiamondAnnotation>

  <BoxAnnotation color="#aaaadd" min="1.7 2 2" max="2 3 3">
        （7）存储总线 3：筛选赛特斯石英水晶，优先级高于主网。
        <ItemImage id="certus_quartz_crystal" scale="2" />
  </BoxAnnotation>

  <BoxAnnotation color="#aaaadd" min="2 1 2" max="3 2 3">
        （8）ME 接口：始终保持至少 1 个有瑕赛特斯石英母岩，安装合成卡以自动补充。
        <Row><BlockImage id="flawed_budding_quartz" scale="2" /><ItemImage id="crafting_card" scale="2" /></Row>
  </BoxAnnotation>

<DiamondAnnotation pos="1.5 0.5 0" color="#00ff00">
        连接主网络、充电器自动化和投入水中自动加工装置
        <Row>
        <GameScene zoom="3" background="transparent">
          <ImportStructure src="../assets/assemblies/charger_automation.snbt" />
          <IsometricCamera yaw="195" pitch="30" />
        </GameScene>
        <GameScene zoom="3" background="transparent">
          <ImportStructure src="../assets/assemblies/throw_in_water.snbt" />
          <IsometricCamera yaw="195" pitch="30" />
        </GameScene>
        </Row>
    </DiamondAnnotation>

  <IsometricCamera yaw="165" pitch="5" />
</GameScene>

## 配置

### 晶簇收获子网

* 第一个<ItemLink id="annihilation_plane" />（1）：可附魔时运。
* 第一个<ItemLink id="storage_bus" />（2）：只允许<ItemLink id="certus_quartz_crystal" />。

### 母岩回收子网

* 第二个<ItemLink id="annihilation_plane" />（3）：必须附魔精准采集。
* 第二个<ItemLink id="storage_bus" />（4）：只允许<ItemLink id="quartz_block" />。

### 母岩放置子网

* <ItemLink id="formation_plane" />（5）：保持默认设置。
* <ItemLink id="import_bus" />（6）：只允许<ItemLink id="flawed_budding_quartz" />。

### 主网络

* 第三个<ItemLink id="storage_bus" />（7）：筛选<ItemLink id="certus_quartz_crystal" />，并将[存储优先级](../ae2-mechanics/import-export-storage.md#storage-priority)设为高于主仓库。
* <ItemLink id="interface" />（8）：配置为保留 1 个<ItemLink id="flawed_budding_quartz" />，安装<ItemLink id="crafting_card" />。

## 工作流程

1. **收获晶簇：**晶簇收获子网与[基础农场](simple-certus-farm.md)相同，只回收成熟晶簇掉落的水晶。
2. **回收母岩：**母岩退化为普通<ItemLink id="quartz_block" />后，带精准采集的第二个破坏面板将其取出；存储总线把石英块提供给 ME 接口，供[投入水中自动加工](throw-in-water-automation.md)制作新的母岩。
3. **补充母岩：**主网的<ItemLink id="interface" />在库存不足时，通过<ItemLink id="crafting_card" />申请自动合成新母岩。放置子网经<ItemLink id="import_bus" />提取成品，再由<ItemLink id="formation_plane" />放回农场。
4. **回收晶体：**主网通过高优先级<ItemLink id="storage_bus" />访问水晶缓冲容器；水晶优先留在专用容器中，供[充电器自动化](charger-automation.md)使用。

