---
navigation:
  parent: example-setups/example-setups-index.md
  title: 投入水中自动加工
  icon: fluix_crystal
---

# 投入水中自动加工

有些 AE2 配方需要将几种物品一起投入水中，例如制作<ItemLink id="fluix_crystal" />，或将<ItemLink id="quartz_block" />与<ItemLink id="charged_certus_quartz_crystal" />加工为赛特斯石英母岩。

这类配方可以使用<ItemLink id="formation_plane" />、<ItemLink id="annihilation_plane" />与两个[物流子网](pipe-subnet.md)自动完成。

本例使用<ItemLink id="pattern_provider" />，因此需要与[自动合成系统](../ae2-mechanics/autocrafting.md)连接；为它提供充能赛特斯石英水晶时，可以搭配[充电器自动化](charger-automation.md)。

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/throw_in_water.snbt" />

<BoxAnnotation color="#dddddd" min="2 0 1" max="3 1 2">
        （1）样板供应器：安装水中加工的处理样板，保持默认设置。

        ![福鲁伊克斯水晶处理样板](../assets/diagrams/fluix_pattern_small.png)![有瑕母岩处理样板](../assets/diagrams/flawed_budding_pattern_small.png)
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="1.7 0 1" max="2 1 2">
        （2）ME 接口：保持默认设置。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="1 .7 1" max="2 1 2">
        （3）成型面板：设置为“以掉落物形式输出物品”。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="1 2 1" max="2 2.3 2">
        （4）破坏面板：无需额外配置，负责收回加工产物。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="2 1 1" max="3 1.3 2">
        （5）存储总线：只允许加工完成的目标产物进入。
        <Row><ItemImage id="fluix_crystal" scale="2" /><BlockImage id="flawless_budding_quartz" scale="2" /></Row>
  </BoxAnnotation>

<DiamondAnnotation pos="3.9 0.5 1.5" color="#00ff00">
        连接主网络与充电器自动化装置
        <GameScene zoom="3" background="transparent">
          <ImportStructure src="../assets/assemblies/charger_automation.snbt" />
          <IsometricCamera yaw="195" pitch="30" />
        </GameScene>
    </DiamondAnnotation>

  <IsometricCamera yaw="180" pitch="0" />
</GameScene>

## 配置与样板

* <ItemLink id="pattern_provider" />（1）：安装相关<ItemLink id="processing_pattern" />。制作<ItemLink id="fluix_crystal" />时，可以参考 JEI 中的原始水中加工配方：

  ![福鲁伊克斯水晶处理样板](../assets/diagrams/fluix_pattern.png)

* 制作<ItemLink id="flawed_budding_quartz" />时，建议以<ItemLink id="quartz_block" />作为输入，从而避免一个样板的输入物与另一个样板的输出物相同，影响存储总线筛选。

  ![有瑕赛特斯石英母岩处理样板](../assets/diagrams/flawed_budding_pattern.png)

* <ItemLink id="interface" />（2）：保持默认设置。
* <ItemLink id="formation_plane" />（3）：设置为向世界中投出物品实体，而不是放置方块。
* <ItemLink id="annihilation_plane" />（4）：无需额外设置。
* <ItemLink id="storage_bus" />（5）：只允许目标加工产物进入。

## 工作流程

1. <ItemLink id="pattern_provider" />把配方原料送入绿色子网的<ItemLink id="interface" />。
2. 接口没有设置常备库存，因此尝试将原料送入子网的[网络存储](../ae2-mechanics/import-export-storage.md)。
3. 绿色子网唯一的存储目标是<ItemLink id="formation_plane" />，它将原料投入水中。
4. 橙色子网的<ItemLink id="annihilation_plane" />尝试回收水中的物品，但其唯一存储目标<ItemLink id="storage_bus" />只允许最终产物进入，因此原料不会立刻被取走。
5. 原料在水中完成加工，变成目标产物。
6. 破坏面板能够回收新生成的产物，并通过存储总线将其送回样板供应器。
7. 样板供应器将产物返回主网络，自动合成任务完成。

