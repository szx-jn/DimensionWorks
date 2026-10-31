---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 充电器
  icon: charger
  position: 310
categories:
- machines
item_ids:
- ae2:charger
---

# 充电器

<BlockImage id="charger" scale="8" />

充电器可以为支持充能的 AE2 工具与<ItemLink id="certus_quartz_crystal" />充电。

能源可以从设备顶部或底部输入，也可以根据整合包供能规则连接 [ME 线缆](cables.md)或其他能源设备。物品通常可以从不同侧面输入和输出；充能完成前的水晶不会被当作加工结果错误取出。

使用<ItemLink id="certus_quartz_wrench" />调整设备朝向，有助于布置自动化物流。

充电器可以将<ItemLink id="certus_quartz_crystal" />加工成<ItemLink id="charged_certus_quartz_crystal" />，也能将普通指南针<ItemLink id="minecraft:compass" />加工成<ItemLink id="meteorite_compass" />。

前期没有自动供能时，可以在充电器顶部或底部安装<ItemLink id="crank" />，通过右键手动充能。

充电器也能为部分使用 AE 能量的工具补充能量。

## 简易自动化示例

利用充电器可调整朝向的特点，可以配合漏斗建立简单的半自动充能装置：

<GameScene zoom="4" background="transparent">
  <ImportStructure src="../assets/assemblies/charger_hopper.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 合成配方

<RecipeFor id="charger" />
