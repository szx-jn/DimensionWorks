---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 能源接收器
  icon: energy_acceptor
  position: 110
categories:
- network infrastructure
item_ids:
- ae2:energy_acceptor
---

# 能源接收器

<Row gap="20">
<BlockImage id="energy_acceptor" scale="8" />

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../assets/blocks/cable_energy_acceptor.snbt" />
</GameScene>
</Row>

能源接收器负责将外部能源转换成 ME 网络使用的 [AE 能量](../ae2-mechanics/energy.md)。

**维度工坊对 AE2 的能源输入规则进行了修改。** 实际可用的供能方式和转换倍率应以本整合包的配置及能源改造为准，不应直接套用原版 AE2 的 FE 转 AE 换算值。

虽然<ItemLink id="controller" />也具有部分供能接口，但专门使用能源接收器接入供能系统，通常更容易维护。

## 安装形式

能源接收器有**完整方块**和可贴附在线缆上的[平面附属部件](../ae2-mechanics/cable-subparts.md)两种形式，可以在合成栏中相互转换，以适应不同的布线空间。

## 合成配方

<RecipeFor id="energy_acceptor" />

<RecipeFor id="cable_energy_acceptor" />
