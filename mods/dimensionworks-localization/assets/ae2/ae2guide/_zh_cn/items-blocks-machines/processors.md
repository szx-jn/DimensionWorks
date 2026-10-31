---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 处理器
  icon: logic_processor
  position: 010
categories:
- misc ingredients blocks
item_ids:
- ae2:logic_processor
- ae2:calculation_processor
- ae2:engineering_processor
- ae2:printed_silicon
- ae2:printed_logic_processor
- ae2:printed_calculation_processor
- ae2:printed_engineering_processor
- ae2:silicon
---

# 处理器

<Row>
  <ItemImage id="logic_processor" scale="4" />

  <ItemImage id="calculation_processor" scale="4" />

  <ItemImage id="engineering_processor" scale="4" />
</Row>

处理器是 AE2 [设备](../ae2-mechanics/devices.md)和机器的重要材料。主要有三种：使用金锭的**逻辑处理器**、使用<ItemLink id="certus_quartz_crystal" />的**计算处理器**，以及使用钻石的**工程处理器**。

制作处理器需要相应的[压印模板](presses.md)和<ItemLink id="inscriber" />。整个过程包括多个加工步骤，建议尽早建立自动化生产线。

## 生产步骤

<Column gap="5">
  1. 准备硅、红石、金锭、<ItemLink id="certus_quartz_crystal" />和钻石等原料。

  <RecipeFor id="silicon" />

  <br />

  2. 使用压印模板制作印制硅和三种印制电路。

  <Row>
    <RecipeFor id="printed_silicon" />

    <RecipeFor id="printed_logic_processor" />
  </Row>

  <Row>
    <RecipeFor id="printed_calculation_processor" />

    <RecipeFor id="printed_engineering_processor" />
  </Row>

  <br />

  3. 将印制部件与红石等原料再次压印，得到最终处理器。

  <Row>
    <RecipeFor id="logic_processor" />

    <RecipeFor id="calculation_processor" />
  </Row>

  <RecipeFor id="engineering_processor" />
</Column>
