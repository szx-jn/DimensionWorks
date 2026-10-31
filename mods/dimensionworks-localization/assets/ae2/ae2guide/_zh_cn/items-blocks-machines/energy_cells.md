---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 能源电池
  icon: energy_cell
  position: 110
categories:
- network infrastructure
item_ids:
- ae2:energy_cell
- ae2:dense_energy_cell
- ae2:creative_energy_cell
---

# 能源元件

<Row gap="20">
  <BlockImage id="energy_cell" scale="8" p:fullness="4" />
  <BlockImage id="dense_energy_cell" scale="8" p:fullness="4" />
  <BlockImage id="creative_energy_cell" scale="8" />
</Row>

能源元件可增加 ME 网络的[能源](../ae2-mechanics/energy.md)缓冲容量，防止短时间内大量存取物品时出现断电，也能在电源暂时中断时维持网络运行。

足够的储能还可以承担[空间存储](../ae2-mechanics/spatial-io.md)等操作产生的瞬时大额能耗。

## 储能指示灯

<Row>
<BlockImage id="energy_cell" scale="4" p:fullness="0" />
<BlockImage id="energy_cell" scale="4" p:fullness="1" />
<BlockImage id="energy_cell" scale="4" p:fullness="2" />
<BlockImage id="energy_cell" scale="4" p:fullness="3" />
<BlockImage id="energy_cell" scale="4" p:fullness="4" />
</Row>

能源元件侧面的指示灯会根据储能比例变化：

* **0 格：**低于 25%。
* **1 格：**25%～50%。
* **2 格：**50%～75%。
* **3 格：**75%～99%。
* **4 格：**高于 99%。

## 元件类型

* <ItemLink id="energy_cell" />：容量约 **200 kAE**，适合小型网络和瞬时能耗缓冲。
* <ItemLink id="dense_energy_cell" />：容量约 **1.6 MAE**，适合大型网络、断电缓冲和[空间存储](../ae2-mechanics/spatial-io.md)等高能耗操作。
* <ItemLink id="creative_energy_cell" />：创造模式专用，可以提供无限 AE 能量，用于测试。

## 合成配方

<Row>
  <RecipeFor id="energy_cell" />
  <RecipeFor id="dense_energy_cell" />
</Row>
