---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 破坏面板
  icon: annihilation_plane
  position: 210
categories:
- devices
item_ids:
- ae2:annihilation_plane
---

# 破坏面板

<GameScene zoom="8" background="transparent">
<ImportStructure src="../assets/blocks/annihilation_plane.snbt" />
</GameScene>

破坏面板能够破坏正前方的方块，并拾取与面板表面接触的掉落物，再将它们存入[ME 网络存储](../ae2-mechanics/import-export-storage.md)。你可以把它理解成同时具备挖掘和<ItemLink id="import_bus" />收集能力的设备。

破坏面板只能操作与自身直接接触的方块或物品，不会自动扫描大范围区域。

它可以接受部分工具附魔，例如时运、精准采集和效率。合理利用这些附魔，可以制作[自动矿物采集装置](../example-setups/ore-fortuner.md)。

破坏面板属于[线缆附属部件](../ae2-mechanics/cable-subparts.md)。

**注意：**部分领地保护或区块认领规则可能会阻止破坏面板挖掘方块。

## 如何过滤目标

破坏面板只有在网络能够接收相应掉落物时，才会尝试破坏目标。因此，要限制它能破坏什么，需要限制其所连接网络能够存储的物品种类。

最简单的方式是建立独立[子网络](../ae2-mechanics/subnetworks.md)，通过<ItemLink id="storage_bus" />或在[元件工作台](cell_workbench.md)设置过筛选规则的[存储元件](../items-blocks-machines/storage_cells.md)，只允许接收所需材料。

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/annihilation_filtering.snbt" />

  <DiamondAnnotation pos="1 0.5 0.5" color="#00ff00">
    只允许目标方块的掉落物进入网络
  </DiamondAnnotation>

  <DiamondAnnotation pos=".5 0.5 2.5" color="#00ff00">
    通过存储规则限制面板会采集的物品
  </DiamondAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

**过滤依据是最终掉落物，而不是方块的注册名。** 例如，想限制<ItemLink id="minecraft:amethyst_cluster" />的采集，必须考虑成熟度与精准采集附魔对掉落物的影响。没有掉落物的未成熟晶芽，可能不会按预期被阻止破坏。

## 合成配方

<RecipeFor id="annihilation_plane" />
