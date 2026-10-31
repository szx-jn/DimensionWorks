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

能源元件可以增加 ME 网络的[能源](../ae2-mechanics/energy.md)缓冲容量，在短时间大量存取物品、供电暂时中断或执行[空间存储](../ae2-mechanics/spatial-io.md)等高耗能操作时尤其有用。

## 电量指示灯

<Row>
<BlockImage id="energy_cell" scale="4" p:fullness="0" />
<BlockImage id="energy_cell" scale="4" p:fullness="1" />
<BlockImage id="energy_cell" scale="4" p:fullness="2" />
<BlockImage id="energy_cell" scale="4" p:fullness="3" />
<BlockImage id="energy_cell" scale="4" p:fullness="4" />
</Row>

能源元件侧面的指示灯会根据充能比例变化：

* 0 格：低于 25%；
* 1 格：25%～50%；
* 2 格：50%～75%；
* 3 格：75%～99%；
* 4 格：99% 以上。

## 三种能源元件

* <ItemLink id="energy_cell" />：最多存储 **200K AE**，通常足以应付一般网络的短时能耗波动。
* <ItemLink id="dense_energy_cell" />：最多存储 **1.6M AE**，适合更大型的网络和空间 IO 等需要瞬时大量供能的系统。
* <ItemLink id="creative_energy_cell" />：创造模式专用，提供无限能量，主要用于测试。

实际外部供能与换算方式遵循维度工坊当前版本的能源规则。

## 合成配方

<Row>
  <RecipeFor id="energy_cell" />
  <RecipeFor id="dense_energy_cell" />
</Row>
