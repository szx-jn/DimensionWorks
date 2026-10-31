---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 压印器
  icon: inscriber
  position: 310
categories:
- machines
item_ids:
- ae2:inscriber
---

# 压印器

<BlockImage id="inscriber" scale="8" />

压印器用于制作[处理器](processors.md)、使用[压印模板](presses.md)加工电路，以及粉碎部分材料。

压印器的不同侧面对应不同输入槽位，可以通过<ItemLink id="certus_quartz_wrench" />调整朝向，方便与漏斗、ME 总线和其他物流设备搭配。

设备能够使用 AE 能量，也能根据整合包当前的能源改造接受相应的供能方式。不要直接套用原版 FE 与 AE 的换算值。

压印器可以调整输入缓冲区的最大堆叠数。并行放置多台压印器时，较小的缓冲区能让材料分配更加均衡，而不是让第一台机器先堆满原料。

制作处理器通常需要以下四种压印模板：

<Row>
  <ItemImage id="silicon_press" scale="4" />
  <ItemImage id="logic_processor_press" scale="4" />
  <ItemImage id="calculation_processor_press" scale="4" />
  <ItemImage id="engineering_processor_press" scale="4" />
</Row>

此外还可以使用<ItemImage id="name_press" scale="4" />给部分物品或方块添加名称；自定义名称也有助于在<ItemLink id="pattern_access_terminal" />中区分自动合成样板。

## 配置模式

* **按面分配槽位**：不同方向对应指定的材料槽和成品槽，便于自动化。
* **自由输入模式**：输入物品可以由内部过滤规则分配到合适的槽位，但顶部和底部通常不能作为输出。
* **自动输出**：加工完成后，可将产物推入相邻容器。
* **缓冲区大小**：手动使用时可以适当调大；多台机器并行时建议调小。

## 界面与输入输出方向

下图展示按面分配槽位时，各个方向的功能：

![压印器界面](../assets/diagrams/inscriber_gui.png) ![压印器各侧面](../assets/diagrams/inscriber_sides.png)

* **A：顶部输入**。可以向顶部材料槽输入，也可以从该槽取出。
* **B：中部输入**。从左、右、前、后等侧面输入中间材料；不能直接从该槽自动取出。
* **C：底部输入**。可以向底部材料槽输入，也可以从该槽取出。
* **D：成品输出**。从侧面抽取完成的产物；不能向成品槽输入材料。

## 简易自动化

利用侧面分配和可旋转的特点，可以用漏斗建立简单的半自动生产线：

<GameScene zoom="4" background="transparent">
  <ImportStructure src="../assets/assemblies/inscriber_hopper_automation.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 升级卡

压印器支持[升级卡](upgrade_cards.md)，例如<ItemLink id="speed_card" />，用于提高加工速度。

## 合成配方

<RecipeFor id="inscriber" />
