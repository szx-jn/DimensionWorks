---
navigation:
  parent: example-setups/example-setups-index.md
  title: 处理器自动化
  icon: inscriber
---

# 处理器生产自动化

AE2 的逻辑、计算、工程三种[处理器](../items-blocks-machines/processors.md)都需要先制作印制硅与对应的印制电路，再与红石一起进行最后一次压印。把这一过程自动化，可以避免玩家反复手动操作多台压印器。

这套生产线可以使用任何支持筛选的物流管道和容器；下面的示例仅使用 AE2 自带的[物流子网](pipe-subnet.md)完成分流、加工和回收。

![处理器生产流程](../assets/diagrams/processor_flow_diagram.png)

本例将<ItemLink id="pattern_provider" />连接到主网的[自动合成系统](../ae2-mechanics/autocrafting.md)。如果暂时没有自动合成系统，也可以让漏斗或箱子代替样板供应器，直接输入原料。

压印器可以从不同的方位接收材料。使用多个独立子网，可以让它们自动把印制电路、硅和红石送到正确的槽位。

<GameScene zoom="4" interactive={true}>
  <ImportStructure src="../assets/assemblies/processor_automation.snbt" />

  <BoxAnnotation color="#dddddd" min="5 1 0" max="6 2 1" thickness=".05">
        （1）样板供应器：安装逻辑、计算、工程处理器的处理样板。

        <Row>
            ![逻辑模式](../assets/diagrams/logic_pattern_small.png)
            ![计算模式](../assets/diagrams/calculation_pattern_small.png)
            ![工程模式](../assets/diagrams/engineering_pattern_small.png)
        </Row>
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="4.7 2 0" max="5 3 1" thickness=".05">
        （2）存储总线 1：保持默认配置。
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="4 1 0" max="4.3 2 1" thickness=".05">
        （3）输出总线 1：只输出硅，安装两张加速卡。
        <Row><ItemImage id="silicon" scale="2" /><ItemImage id="speed_card" scale="2" /></Row>
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="4 4 0" max="4.3 3 1" thickness=".05">
        （4）输出总线 2：只输出金锭，安装两张加速卡。
        <Row><ItemImage id="minecraft:gold_ingot" scale="2" /><ItemImage id="speed_card" scale="2" /></Row>
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="4 5 0" max="4.3 4 1" thickness=".05">
        （5）输出总线 3：只输出赛特斯石英水晶，安装两张加速卡。
        <Row><ItemImage id="certus_quartz_crystal" scale="2" /><ItemImage id="speed_card" scale="2" /></Row>
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="4 6 0" max="4.3 5 1" thickness=".05">
        （6）输出总线 4：只输出钻石，安装两张加速卡。
        <Row><ItemImage id="minecraft:diamond" scale="2" /><ItemImage id="speed_card" scale="2" /></Row>
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="2.3 3 0" max="2 2 1" thickness=".05">
        （7）输出总线 5：只输出红石粉，安装两张加速卡。
        <Row><ItemImage id="minecraft:redstone" scale="2" /><ItemImage id="speed_card" scale="2" /></Row>
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="4 1 0" max="3 2 1" thickness=".05">
        （8）压印器 1：安装硅压印模板和四张加速卡。
        <Row><ItemImage id="silicon_press" scale="2" /><ItemImage id="speed_card" scale="2" /></Row>
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="4 3 0" max="3 4 1" thickness=".05">
        （9）压印器 2：安装逻辑压印模板和四张加速卡。
        <Row><ItemImage id="logic_processor_press" scale="2" /><ItemImage id="speed_card" scale="2" /></Row>
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="4 4 0" max="3 5 1" thickness=".05">
        （10）压印器 3：安装计算压印模板和四张加速卡。
        <Row><ItemImage id="calculation_processor_press" scale="2" /><ItemImage id="speed_card" scale="2" /></Row>
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="4 5 0" max="3 6 1" thickness=".05">
        （11）压印器 4：安装工程压印模板和四张加速卡。
        <Row><ItemImage id="engineering_processor_press" scale="2" /><ItemImage id="speed_card" scale="2" /></Row>
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="2 2 0" max="1 3 1" thickness=".05">
        （12）压印器 5：用于合并印制材料与红石，安装四张加速卡。
        <ItemImage id="speed_card" scale="2" />
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="2.7 2 0" max="3 1 1" thickness=".05">
        （13）输入总线 1：保持默认设置，安装两张加速卡。
        <ItemImage id="speed_card" scale="2" />
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="2.7 4 0" max="3 3 1" thickness=".05">
        （14）输入总线 2：保持默认设置，安装两张加速卡。
        <ItemImage id="speed_card" scale="2" />
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="2.7 5 0" max="3 4 1" thickness=".05">
        （15）输入总线 3：保持默认设置，安装两张加速卡。
        <ItemImage id="speed_card" scale="2" />
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="2.7 6 0" max="3 5 1" thickness=".05">
        （16）输入总线 4：保持默认设置，安装两张加速卡。
        <ItemImage id="speed_card" scale="2" />
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="2 3 0" max="1 3.3 1" thickness=".05">
        （17）存储总线 2：保持默认配置。
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="2 1.7 0" max="1 2 1" thickness=".05">
        （18）存储总线 3：保持默认配置。
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="1 2 0" max="0.7 3 1" thickness=".05">
        （19）输入总线 5：保持默认设置，安装两张加速卡。
        <ItemImage id="speed_card" scale="2" />
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="5 0.7 0" max="6 1 1" thickness=".05">
        （20）存储总线 4：保持默认配置。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="3.3 2.7 0.3" max="3.7 3 0.7" thickness=".05">
        石英纤维为压印器所在子网提供能源，同时避免与主网络直接合并。
  </BoxAnnotation>

<DiamondAnnotation pos="7 1.5 0.5" color="#00ff00">
        连接主网络
    </DiamondAnnotation>

  <IsometricCamera yaw="185" pitch="5" />
</GameScene>

## 配置

* <ItemLink id="pattern_provider" />（1）：保持默认设置，安装三种处理器的<ItemLink id="processing_pattern" />。

  ![逻辑处理器样板](../assets/diagrams/logic_pattern.png)
  ![计算处理器样板](../assets/diagrams/calculation_pattern.png)
  ![工程处理器样板](../assets/diagrams/engineering_pattern.png)

* <ItemLink id="storage_bus" />（2、17、18、20）：保持默认设置。
* <ItemLink id="export_bus" />（3～7）：分别筛选硅、金锭、赛特斯石英水晶、钻石和红石粉，各安装两张<ItemLink id="speed_card" />。

  <Row>
    <ItemImage id="silicon" scale="2" />
    <ItemImage id="minecraft:gold_ingot" scale="2" />
    <ItemImage id="certus_quartz_crystal" scale="2" />
    <ItemImage id="minecraft:diamond" scale="2" />
    <ItemImage id="minecraft:redstone" scale="2" />
  </Row>

* <ItemLink id="import_bus" />（13～16、19）：保持默认设置，各安装两张<ItemLink id="speed_card" />。
* <ItemLink id="inscriber" />（8～12）：前四台分别安装对应[压印模板](../items-blocks-machines/presses.md)；第五台用于处理器最终压印。建议所有压印器都安装四张<ItemLink id="speed_card" />。

  <Row>
    <ItemImage id="silicon_press" scale="2" />
    <ItemImage id="logic_processor_press" scale="2" />
    <ItemImage id="calculation_processor_press" scale="2" />
    <ItemImage id="engineering_processor_press" scale="2" />
  </Row>

## 工作流程

1. <ItemLink id="pattern_provider" />将所需原料发送到输入容器。
2. 第一条[物流子网](pipe-subnet.md)（橙色）把硅、红石粉，以及对应的处理器原料分别送至压印器。
3. 前四台<ItemLink id="inscriber" />生产<ItemLink id="printed_silicon" />、<ItemLink id="printed_logic_processor" />、<ItemLink id="printed_calculation_processor" />或<ItemLink id="printed_engineering_processor" />。
4. 第二、第三条物流子网（绿色）把印制材料转运到第五台压印器。
5. 第五台压印器把印制电路、印制硅与红石压印成最终[处理器](../items-blocks-machines/processors.md)。
6. 最后一条物流子网（紫色）收集处理器，将它们送回样板供应器与主网络，自动合成任务随之完成。
