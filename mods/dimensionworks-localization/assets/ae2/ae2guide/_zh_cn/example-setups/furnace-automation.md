---
navigation:
  parent: example-setups/example-setups-index.md
  title: 熔炉自动化
  icon: minecraft:furnace
---

# 熔炉自动化

这个方案使用<ItemLink id="pattern_provider" />，适合已经建立[自动合成系统](../ae2-mechanics/autocrafting.md)的玩家。独立熔炉的简单自动化通常只需要漏斗和箱子。

熔炉需要从**顶部输入待烧炼物品**、从**侧面输入燃料**，再从底部取出产物，因此比[充电器自动化](../example-setups/charger-automation.md)稍复杂。

如果分别给<ItemLink id="pattern_provider" />、<ItemLink id="export_bus" />和<ItemLink id="import_bus" />供能，就可能占用 3 个[频道](../ae2-mechanics/channels.md)。

下面的方案通过子网把材料分流到不同的熔炉槽位，只需占用主网的一个频道。

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/furnace_automation.snbt" />

<BoxAnnotation color="#dddddd" min="1 0 0" max="2 1 1">
        （1）样板供应器：使用赛特斯石英扳手调整朝向，安装烧炼用处理样板。

        ![铁铁模式](../assets/diagrams/furnace_pattern_small.png)
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="1 1 0" max="2 1.3 1">
        （2）ME 接口：保持默认配置。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="1 1 0" max="1.3 2 1">
        （3）存储总线 1：仅允许煤炭等燃料进入。
        <ItemImage id="minecraft:coal" scale="2" />
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="0 2 0" max="1 2.3 1">
        （4）存储总线 2：安装反转卡，将煤炭排除在允许输入的材料外。
        <Row><ItemImage id="minecraft:coal" scale="2" /><ItemImage id="inverter_card" scale="2" /></Row>
  </BoxAnnotation>

<DiamondAnnotation pos="4 0.5 0.5" color="#00ff00">
        连接主网络
    </DiamondAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 配置

* <ItemLink id="pattern_provider" />（1）：安装对应的<ItemLink id="processing_pattern" />，并用<ItemLink id="certus_quartz_wrench" />调整朝向。

  ![熔炉处理样板](../assets/diagrams/furnace_pattern.png)

* <ItemLink id="interface" />（2）：保持默认设置。
* 第一个<ItemLink id="storage_bus" />（3）：筛选煤炭或准备使用的其他燃料。
* 第二个<ItemLink id="storage_bus" />（4）：安装<ItemLink id="inverter_card" />，将煤炭设为黑名单，让待烧炼物品通过。

## 工作流程

1. <ItemLink id="pattern_provider" />将配方材料推向<ItemLink id="interface" />。接口的连接结构允许材料经存储总线进入子网，而无需实际存放在接口库存中。
2. 接口没有设置常备库存，因此材料会被交给子网的[网络存储](../ae2-mechanics/import-export-storage.md)。
3. 子网中的两个<ItemLink id="storage_bus" />分别把**燃料送入侧面燃料槽**、把**原料送入顶部输入槽**。
4. 熔炉完成烧炼。
5. 底部漏斗取走产物，将其放入样板供应器的返回槽，最终送回主网络。

