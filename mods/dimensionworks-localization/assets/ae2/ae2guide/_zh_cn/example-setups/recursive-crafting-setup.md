---
navigation:
  parent: example-setups/example-setups-index.md
  title: 递归合成
  icon: minecraft:netherite_upgrade_smithing_template
---

# 递归合成装置

如[自动合成](../ae2-mechanics/autocrafting.md)一节所述，自动合成规划算法无法处理主要产物同时也是输入材料的配方。例如，它无法复制<ItemLink id="minecraft:netherite_upgrade_smithing_template" />。

一种解决办法是利用<ItemLink id="level_emitter" />伪装成[样板](../items-blocks-machines/patterns.md)的能力。

然后可以用它启动一个持续执行合成的小型装置。这里以复制<ItemLink id="minecraft:netherite_upgrade_smithing_template" />为例。

<RecipeFor id="minecraft:netherite_upgrade_smithing_template" />

***

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/recursive_recipe_setup.snbt" />

  <BoxAnnotation color="#dddddd" min="1 0 0" max="2 1 1">
        (1) 接口：设置为储备额外所需材料：钻石和下界岩。
        <Row><ItemImage id="minecraft:diamond" scale="2" /> <ItemImage id="minecraft:netherrack" scale="2" /></Row>
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="2.3 1 0.3" max="2.7 1.3 0.7">
        (2) 数量发信器：配置为“下界合金锻造模板”，并设为“发出红石信号以合成物品”。
        <Row><ItemImage id="minecraft:netherite_upgrade_smithing_template" scale="2" /> <ItemImage id="crafting_card" scale="2" /></Row>
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="2 0 0" max="2.3 1 1">
        (3) 输入总线 #1：过滤接口储备的材料。装有红石卡，红石模式设为“有信号时启用”。
        <Row>
        <ItemImage id="minecraft:diamond" scale="2" />
        <ItemImage id="minecraft:netherrack" scale="2" />
        <ItemImage id="redstone_card" scale="2" />
        </Row>
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="3 1 1" max="4 1.3 2">
        (4) 存储总线 #1：优先级高于另一个存储总线。非常重要。
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="3 0 1" max="4 1 2">
        (5) 分子装配室：放入用于复制锻造模板的样板。

        ![样板](../assets/diagrams/smithing_template_pattern_small.png)

        初次搭建时，还要手动在其中放入一个锻造模板。
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="2.7 0 1" max="3 1 2">
        (6) 输入总线 #2：保持默认配置。
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="1 0 1" max="2 1 1.3">
        (7) 存储总线 #2：过滤“下界合金锻造模板”。优先级低于另一个存储总线。
        <ItemImage id="minecraft:netherite_upgrade_smithing_template" scale="2" />
  </BoxAnnotation>

<DiamondAnnotation pos="0 0.5 0.5" color="#00ff00">
        接入主网络
    </DiamondAnnotation>

  <IsometricCamera yaw="15" pitch="30" />
</GameScene>

## 配置

* <ItemLink id="interface" /> (1) 设置为储备额外所需材料：钻石和下界岩。
* <ItemLink id="level_emitter" /> (2) 配置为“下界合金锻造模板”，并设为“发出红石信号以合成物品”。
* 第一个<ItemLink id="import_bus" /> (3) 过滤接口储备的材料，装有红石卡，红石模式设为“有信号时启用”。
* 第一个<ItemLink id="storage_bus" /> (4) 的[优先级](../ae2-mechanics/import-export-storage.md#storage-priority)设为高于第二个存储总线。
* <ItemLink id="molecular_assembler" /> (5) 放有复制锻造模板的样板，并已手动放入一个锻造模板。

  ![样板](../assets/diagrams/smithing_template_pattern.png)

* 第二个<ItemLink id="import_bus" /> (6) 保持默认配置。
* 第二个<ItemLink id="storage_bus" /> (7) 过滤“下界合金锻造模板”，其[优先级](../ae2-mechanics/import-export-storage.md#storage-priority)低于第一个存储总线。

## 工作原理

1. 由于插入了<ItemLink id="crafting_card" />并设置为“发出红石信号以合成物品”，<ItemLink id="level_emitter" />会伪装成一个[样板](../items-blocks-machines/patterns.md)。因此，“下界合金锻造模板”会作为有效的[自动合成](../ae2-mechanics/autocrafting.md)项目显示在[终端](../items-blocks-machines/terminals.md)中。
2. 玩家或系统发出该物品的合成请求时，数量发信器就会启动。
3. 数量发信器激活第一个<ItemLink id="import_bus" />，使其取出<ItemLink id="interface" />中储备的材料。
4. 网络中只有装在装配室上的存储总线能够存入这些材料。
5. <ItemLink id="molecular_assembler" />（其中已放入一个锻造模板）收到材料后进行合成，产出两个锻造模板。
6. 第二个<ItemLink id="import_bus" />取出一个锻造模板。
7. 第一个存储总线优先级较高，因此该锻造模板会回到装配室。
8. 第二个<ItemLink id="import_bus" />再取出一个锻造模板。
9. 装配室无法再接收锻造模板，因此第二个模板会进入优先级较低的存储总线，并存入接口。
10. 接口没有设置储备锻造模板，因此会将其送入网络。
