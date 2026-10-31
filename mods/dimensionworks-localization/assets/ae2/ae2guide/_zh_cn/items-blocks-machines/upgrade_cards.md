---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 升级卡
  icon: speed_card
  position: 410
categories:
- tools
item_ids:
- ae2:basic_card
- ae2:advanced_card
- ae2:redstone_card
- ae2:capacity_card
- ae2:void_card
- ae2:fuzzy_card
- ae2:speed_card
- ae2:inverter_card
- ae2:crafting_card
- ae2:equal_distribution_card
- ae2:energy_card
---

# 升级卡

<Row>
  <ItemImage id="redstone_card" scale="2" />

  <ItemImage id="capacity_card" scale="2" />

  <ItemImage id="void_card" scale="2" />

  <ItemImage id="fuzzy_card" scale="2" />

  <ItemImage id="speed_card" scale="2" />

  <ItemImage id="inverter_card" scale="2" />

  <ItemImage id="crafting_card" scale="2" />

  <ItemImage id="equal_distribution_card" scale="2" />

  <ItemImage id="energy_card" scale="2" />
</Row>

升级卡能改变 AE2 [设备](../ae2-mechanics/devices.md)与机器的工作方式，例如提高处理速度、增加过滤条件或提供红石控制。

## 升级卡基础材料

<Row>
  <ItemImage id="basic_card" scale="2" />

  <ItemImage id="advanced_card" scale="2" />
</Row>

合成各类升级卡之前，需要先制作基础卡或高级卡作为材料。

<Row>
  <RecipeFor id="basic_card" />

  <RecipeFor id="advanced_card" />
</Row>

## 红石卡

<ItemImage id="redstone_card" scale="2" />

红石卡为支持的设备添加红石控制功能，可在界面中切换“有信号运行”“无信号运行”或“脉冲触发”等条件。

<RecipeFor id="redstone_card" />

## 容量卡

<ItemImage id="capacity_card" scale="2" />

容量卡可以增加输入总线、输出总线、存储总线和成型面板等设备的过滤槽。

<RecipeFor id="capacity_card" />

## 虚空卡

<ItemImage id="void_card" scale="2" />

虚空卡可以通过<ItemLink id="cell_workbench" />安装在[存储元件](storage_cells.md)中。当元件已满时，继续送入的多余资源会被直接销毁。若搭配平均分配卡，当某一类资源分配的空间耗尽时，也会开始销毁该类资源。**使用前应设置好[分区](cell_workbench.md)，避免误删重要物品。**

<RecipeFor id="void_card" />

## 模糊卡

<ItemImage id="fuzzy_card" scale="2" />

模糊卡让支持过滤的设备可以忽略物品的部分耐久和 NBT 差异。例如，可以接收所有铁镐，而不必逐把设置不同耐久的铁镐。

下表以铁镐为例说明不同耐久匹配阈值的效果。✅ 表示匹配，星号表示不匹配。

| 25%                    | 损坏 10%镐 | 损坏 30%的镐 | 损坏 80%的镐 | 完好镐 |
| ---------------------- | ------------------- | ------------------- | ------------------- | ------------------- |
| 接近损坏的镐  | ✅                   | \*\*\*\*            | \*\*\*\*            | \*\*\*\*            |
| 完好的镐 | \*\*\*\*            | ✅                   | ✅                   | ✅                   |

| 50%                    | 损坏 10%镐 | 损坏 30%的镐 | 损坏 80%的镐 | 完好镐 |
| ---------------------- | ------------------- | ------------------- | ------------------- | ------------------- |
| 接近损坏的镐  | ✅                   | ✅                   | \*\*\*\*            | \*\*\*\*            |
| 完好的镐 | \*\*\*\*            | \*\*\*\*            | ✅                   | ✅                   |

| 75%                    | 损坏 10%镐 | 损坏 30%的镐 | 损坏 80%的镐 | 完好镐 |
| ---------------------- | ------------------- | ------------------- | ------------------- | ------------------- |
| 接近损坏的镐  | ✅                   | ✅                   | \*\*\*\*            | \*\*\*\*            |
| 完好的镐 | \*\*\*\*            |                                          | ✅                   | ✅                   |

| 99%                    | 损坏 10%镐 | 损坏 30%的镐 | 损坏 80%的镐 | 完好镐 |
| ---------------------- | ------------------- | ------------------- | ------------------- | ------------------- |
| 接近损坏的镐  | ✅                   | ✅                   | ✅                   | \*\*\*\*            |
| 完好的镐 | \*\*\*\*            | \*\*\*\*            | \*\*\*\*            | ✅                   |

| 忽略                 | 损坏 10%镐 | 损坏 30%的镐 | 损坏 80%的镐 | 完好镐 |
| ---------------------- | ------------------- | ------------------- | ------------------- | ------------------- |
| 接近损坏的镐  | ✅                   | ✅                   | ✅                   | **✅**               |
| 完好的镐 | **✅**               | **✅**               | **✅**               | ✅                   |

<RecipeFor id="fuzzy_card" />

## 加速卡

<ItemImage id="speed_card" scale="2" />

加速卡可以提高设备的工作速度，例如增加输入、输出总线每次传输的物品数量，或加快分子装配室的加工速度。

<RecipeFor id="speed_card" />

## 反转卡

<ItemImage id="inverter_card" scale="2" />

反转卡可将支持的设备的过滤规则从白名单切换为黑名单，或者反过来。

<RecipeFor id="inverter_card" />

## 合成卡

<ItemImage id="crafting_card" scale="2" />

合成卡允许设备向[自动合成系统](../ae2-mechanics/autocrafting.md)发出生产请求，以补充网络内不足的目标物品。

<RecipeFor id="crafting_card" />

## 平均分配卡

<ItemImage id="equal_distribution_card" scale="2" />

平均分配卡可通过<ItemLink id="cell_workbench" />安装在[存储元件](storage_cells.md)中。它把存储空间平均分配给不同类型，避免一种物品独占整张元件的容量。

<RecipeFor id="equal_distribution_card" />

## 能源卡

<ItemImage id="energy_card" scale="2" />

能源卡可增加部分工具与便携终端的内部储能，还可以提高<ItemLink id="vibration_chamber" />的运行效率。

<RecipeFor id="energy_card" />
