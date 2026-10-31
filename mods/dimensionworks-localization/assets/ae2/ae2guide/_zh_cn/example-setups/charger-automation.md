---
navigation:
  parent: example-setups/example-setups-index.md
  title: 充电器自动化
  icon: charger
---

# 充电器自动化

这个示例会用到<ItemLink id="pattern_provider" />，因此适合已经拥有[自动合成系统](../ae2-mechanics/autocrafting.md)的玩家。如果只想独立使用<ItemLink id="charger" />，漏斗和箱子就能满足简单的自动化需求。

基本思路是：让<ItemLink id="pattern_provider" />向充电器发送未充能材料，再由[管道子网](pipe-subnet.md)把成品送回样板供应器。

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/charger_automation.snbt" />

<BoxAnnotation color="#dddddd" min="1 0 0" max="2 1 1">
        （1）样板供应器：安装充电用处理样板，保持默认设置，同时为充电器提供网络连接与能源。

        ![充电模式](../assets/diagrams/charger_pattern_small.png)
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="0 1 0" max="1 1.3 1">
        （2）输入总线：使用默认设置，负责抽取充电成品。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="1 1 0" max="2 1.3 1">
        （3）存储总线：使用默认设置，将成品送回样板供应器。
  </BoxAnnotation>

<DiamondAnnotation pos="4 0.5 0.5" color="#00ff00">
        连接主网络
    </DiamondAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 配置

* <ItemLink id="pattern_provider" />（1）：安装对应的<ItemLink id="processing_pattern" />，保持默认配置。它通过[ME 线缆](../items-blocks-machines/cables.md)为充电器提供[能源](../ae2-mechanics/energy.md)。

  ![充电处理样板](../assets/diagrams/charger_pattern.png)

* <ItemLink id="import_bus" />（2）：使用默认设置。
* <ItemLink id="storage_bus" />（3）：使用默认设置。

## 工作流程

1. <ItemLink id="pattern_provider" />把未充能的材料送入<ItemLink id="charger" />。
2. 充电器消耗能源，将材料加工为充能物品。
3. 绿色子网中的<ItemLink id="import_bus" />把成品取出，并尝试存入子网的[网络存储](../ae2-mechanics/import-export-storage.md)。
4. 子网上的<ItemLink id="storage_bus" />连接到主网络的样板供应器，因此成品会返回主网络，完成这次加工任务。

