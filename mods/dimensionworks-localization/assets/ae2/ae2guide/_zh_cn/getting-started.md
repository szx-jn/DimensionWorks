---
navigation:
  title: 入门教程（1.20+）
  position: 10
---

<div class="notification is-info">
  以下说明适用于 Minecraft 1.20 及更新版本的应用能源 2（AE2）。
</div>

# 入门教程

## 寻找最初的材料

<GameScene zoom="4" background="transparent">
  <ImportStructure src="assets/assemblies/meteor_interior.snbt" />
</GameScene>

要开始使用 AE2，首先需要找到[陨石](ae2-mechanics/meteorites.md)。陨石比较常见，通常会在地面留下明显的撞击坑。你也可以制作<ItemLink id="meteorite_compass" />，让它指向最近的<ItemLink id="mysterious_cube" />。

找到陨石后，挖到中央区域。里面通常有赛特斯石英晶簇、石英芽、[赛特斯石英母岩](items-blocks-machines/budding_certus.md)，以及藏着压印模板的神秘方块。

收集赛特斯石英晶簇和石英块。母岩也能开采，但普通开采会使其品质下降一个等级。**不要轻易挖走无瑕母岩**：即使使用精准采集，它也会变成有瑕母岩，无法重新恢复到无瑕状态。

最后挖掘陨石中心的神秘方块，取得四种压印模板。

## 培育赛特斯石英

<GameScene zoom="4" background="transparent">
<ImportStructure src="assets/assemblies/budding_certus_1.snbt" />
</GameScene>

赛特斯石英芽会在[赛特斯石英母岩](items-blocks-machines/budding_certus.md)上生长，与紫水晶的生长方式类似。

尚未成熟的石英芽被破坏时，会掉落一个<ItemLink id="certus_quartz_dust" />，不受时运影响。完全成熟的晶簇则会掉落四个<ItemLink id="certus_quartz_crystal" />，时运可以提高产量。

赛特斯石英母岩有四个品质等级：**无瑕、有瑕、开裂、损坏**。

<GameScene zoom="4" background="transparent">
<ImportStructure src="assets/assemblies/budding_blocks.snbt" />
<IsometricCamera yaw="195" pitch="30" />
</GameScene>

每次石英芽进入下一生长阶段，母岩都有概率降低一个等级，最终变成普通赛特斯石英块。将有损伤的母岩（或普通赛特斯石英块）与一颗或多颗<ItemLink id="charged_certus_quartz_crystal" />一起投入水中，就可以获得新的母岩。

<RecipeFor id="damaged_budding_quartz" />

无瑕母岩不会退化，可以无限培育晶体。但无瑕母岩无法合成，也不能直接挖掘搬走；[空间存储](ae2-mechanics/spatial-io.md)是少数能够移动它的手段之一。

石英芽自然生长较慢。把<ItemLink id="growth_accelerator" />放在母岩附近，可以显著提高生长速度，因此建议尽早制作几个。

<GameScene zoom="4" background="transparent">
<ImportStructure src="assets/assemblies/budding_certus_2.snbt" />
<IsometricCamera yaw="195" pitch="30" />
</GameScene>

如果暂时没有足够的资源制作<ItemLink id="energy_acceptor" />或<ItemLink id="vibration_chamber" />，也可以使用<ItemLink id="crank" />手动为设备供能。

想要自动收获晶体，可以阅读[简易赛特斯石英农场](example-setups/simple-certus-farm.md)。

## 制作福鲁伊克斯水晶

福鲁伊克斯是另一种基础材料。将充能赛特斯石英水晶、红石粉和下界石英一起投入水中，就能获得福鲁伊克斯水晶。你还可以设计装置将这一过程自动化。

需要充能水晶时，使用<ItemLink id="charger" />加工<ItemLink id="certus_quartz_crystal" />，获得<ItemLink id="charged_certus_quartz_crystal" />。

## 用压印器制作处理器

陨石中的神秘方块能提供四种压印模板。把它们放入<ItemLink id="inscriber" />，便可生产 ME 网络所需的处理器。

<ItemGrid>
  <ItemIcon id="silicon_press" />

  <ItemIcon id="logic_processor_press" />

  <ItemIcon id="calculation_processor_press" />

  <ItemIcon id="engineering_processor_press" />
</ItemGrid>

压印器的不同面对应不同槽位：顶部和底部负责上下材料槽，侧面负责中间材料槽；加工产物可从侧面取出。漏斗能够完成最基本的自动输入输出，<ItemLink id="certus_quartz_wrench" />则可调整设备朝向。

建议先分别制作几种处理器，供接下来的 ME 网络使用。之后可以再尝试把处理器生产完全自动化。

## ME 网络与物品存储

### 什么是 ME 存储？

ME 是“物质—能源”（Matter Energy）的缩写。与传统箱子不同，AE2 把物品保存在专用的存储元件中，通过网络中的终端统一访问。你不再需要翻找一整面箱子，就能检索和提取想要的材料。

### 初学者需要知道什么？

ME 存储依赖[存储元件](items-blocks-machines/storage_cells.md)。不同容量等级的元件能够容纳不同数量的物品和类型。要使用它们，需要把元件放入<ItemLink id="chest" />或<ItemLink id="drive" />。

<ItemLink id="chest" />是很好的入门设备：它自带简单终端，能向其中的元件存取物品。注意，物品实际保存在存储元件里，而不是箱子本体中。

熟悉基本操作后，就可以开始搭建真正的[ME 网络](ae2-mechanics/me-network-connections.md)。

## 搭建第一个 ME 网络

接下来先制作一个最简单的存储网络：它暂时不负责自动生产，只用来集中存放并搜索物品。

<GameScene zoom="6" interactive={true}>
<ImportStructure src="assets/assemblies/tiny_me_system.snbt" />

</GameScene>

所需材料：

* 1 个<ItemLink id="drive" />
* 1 个<ItemLink id="terminal" />或<ItemLink id="crafting_terminal" />
* 1 个<ItemLink id="energy_acceptor" />
* 少量[普通 ME 线缆](items-blocks-machines/cables.md)，例如玻璃线缆、包层线缆或智能线缆
* 数张[存储元件](items-blocks-machines/storage_cells.md)。起步可以选择 1K 和 4K 的组合，之后再研究[存储元件分区](items-blocks-machines/cell_workbench.md)

按照下面的顺序搭建：

1. 放置 ME 驱动器。
2. 在附近放置能源接收器。能源接收器有完整方块和扁平部件两种形态，可以在合成栏中相互转换；完整方块可以直接贴着驱动器放置，扁平部件需要安装在线缆或方块表面。
3. 按整合包的能源规则，为能源接收器接入合适的能源。
4. 使用 ME 线缆连接驱动器，并在线缆上安装终端。
5. 将存储元件插入驱动器。
6. 打开终端，尝试存入、搜索、取出物品。

这样就完成了一个基础的 ME 存储系统。它还有很多扩展空间。

### 扩展自动化功能

有了存储网络后，可以先尝试让熔炉自动处理矿石：使用<ItemLink id="export_bus" />把待加工的物品从 ME 网络送入熔炉，再用<ItemLink id="import_bus" />把熔炉产物收回网络。

**输出总线**负责将网络中的物品送入外部容器；**输入总线**则负责从外部容器抽取物品并存入网络。

### 了解频道限制

初期网络规模较小时，你可能暂时感觉不到限制。但当需要频道的[设备](ae2-mechanics/devices.md)越来越多时，就要学习如何分配[频道](ae2-mechanics/channels.md)。

普通 ME 线缆通常最多承载 8 个频道。当设备数量超过这一限制时，需要规划不同分支，或引入<ItemLink id="controller" />扩展频道容量。

[智能线缆](items-blocks-machines/cables.md)能够直观显示正在使用的频道数量，特别适合学习和排查网络连接。
