---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 赛特斯石英母岩夸尔
  icon: flawless_budding_quartz
  position: 010
categories:
- misc ingredients blocks
item_ids:
- ae2:flawless_budding_quartz
- ae2:flawed_budding_quartz
- ae2:chipped_budding_quartz
- ae2:damaged_budding_quartz
- ae2:small_quartz_bud
- ae2:medium_quartz_bud
- ae2:large_quartz_bud
- ae2:quartz_cluster
---

# 赛特斯石英母岩

详情也可阅读[赛特斯石英生长机制](../ae2-mechanics/certus-growth.md)。

<GameScene zoom="4" background="transparent">
  <ImportStructure src="../assets/assemblies/budding_blocks.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

赛特斯石英芽会在母岩表面生长，方式类似原版紫水晶母岩。母岩通常可以在[陨石](../ae2-mechanics/meteorites.md)中找到。

赛特斯石英母岩分为四个等级：**无瑕、有瑕、开裂、损坏**。使用 Jade 等信息提示模组或 F3 调试界面，可以查看具体方块类型。

有瑕、开裂和损坏的母岩每当石英芽进入下一生长阶段时，都有概率降低一个品质等级，最终变为普通<ItemLink id="quartz_block" />。

**无瑕母岩不会因为石英芽生长而退化**，因此是最理想的持续晶体来源。

普通挖掘母岩会降低品质；使用精准采集时，除无瑕母岩以外的其他等级可以保持原样。**即使使用精准采集，无瑕母岩也无法原样取得。**若需要移动无瑕母岩，可以研究[空间存储](../ae2-mechanics/spatial-io.md)。

## 修复母岩

把较低品质的母岩或普通<ItemLink id="quartz_block" />与一个或多个<ItemLink id="charged_certus_quartz_crystal" />一起投入水中，可以把它们提升为相应品质的母岩。

无瑕母岩无法通过这种方式合成，只能在世界中寻找。

<Row>
  <RecipeFor id="damaged_budding_quartz" />
  <RecipeFor id="chipped_budding_quartz" />
  <RecipeFor id="flawed_budding_quartz" />
</Row>
