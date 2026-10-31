---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 晶体催生器
  icon: growth_accelerator
  position: 310
categories:
- machines
item_ids:
- ae2:growth_accelerator
---

# 晶体催生器

<BlockImage id="growth_accelerator" p:powered="true" scale="8"/>

将晶体催生器放在赛特斯石英母岩或石英芽附近，可以显著提高[赛特斯石英晶体的生长速度](../ae2-mechanics/certus-growth.md)。

它还能够加快部分其他植物的生长。

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/growth_accelerator.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

前期没有稳定的供电系统时，可以将<ItemLink id="crank" />安装在催生器顶部或底部，再通过右键手动转动。

催生器的线缆接口位于带有福鲁伊克斯纹理的端面，只有这些面可以连接能源和网络。

<GameScene zoom="6" background="transparent">
<ImportStructure src="../assets/assemblies/accelerator_connections.snbt" />
<IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 合成配方

<RecipeFor id="growth_accelerator" />
