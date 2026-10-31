---
navigation:
  parent: ae2-mechanics/ae2-mechanics-index.md
  title: 赛特斯石英生长
  icon: quartz_cluster
---

# 赛特斯石英生长

## 基础培育方式

<GameScene zoom="6" background="transparent">
<ImportStructure src="../assets/assemblies/budding_certus_1.snbt" />
</GameScene>

赛特斯石英芽会从[赛特斯石英母岩](../items-blocks-machines/budding_certus.md)上长出，生长过程与紫水晶类似。

破坏尚未成熟的石英芽，只会获得一个<ItemLink id="certus_quartz_dust" />，且不受时运附魔影响。破坏完全成熟的晶簇，则能获得四个<ItemLink id="certus_quartz_crystal" />，时运附魔还能增加产量。

赛特斯石英母岩分为四个等级：无瑕、有瑕、开裂和损坏。

<GameScene zoom="4" background="transparent">
  <ImportStructure src="../assets/assemblies/budding_blocks.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

每当石英芽进入下一生长阶段，母岩都有概率退化一个等级，最终变成普通赛特斯石英块。将受损母岩或普通石英块与一颗或多颗<ItemLink id="charged_certus_quartz_crystal" />一起投入水中，可以重新制作母岩。

<RecipeFor id="damaged_budding_quartz" />

无瑕的母岩不会退化，能够无限培育石英晶体。它无法合成，也不能通过普通挖掘搬走，即使使用精准采集也会退化；但[空间存储](../ae2-mechanics/spatial-io.md)可以用于移动它。

自然生长速度很慢。在母岩附近放置<ItemLink id="growth_accelerator" />可以显著加快生长，因此建议尽早准备几个。

<GameScene zoom="4" background="transparent">
<ImportStructure src="../assets/assemblies/budding_certus_2.snbt" />
<IsometricCamera yaw="195" pitch="30" />
</GameScene>

如果尚未建立稳定能源系统，可以先制作<ItemLink id="crank" />，手动为设备提供动力；之后再考虑<ItemLink id="energy_acceptor" />或<ItemLink id="vibration_chamber" />。

自动收获方案请参阅[简易赛特斯石英农场](../example-setups/simple-certus-farm.md)。
