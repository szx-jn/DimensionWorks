---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: ME 标准发信器
  icon: level_emitter
  position: 220
categories:
- devices
item_ids:
- ae2:level_emitter
- ae2:energy_level_emitter
---

# ME 标准发信器

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../assets/blocks/level_emitter.snbt" />
</GameScene>

标准发信器根据[ME 网络存储](../ae2-mechanics/import-export-storage.md)中某种物品或流体的数量输出红石信号。另有一种**能源标准发信器**，可以监测网络的[能源](../ae2-mechanics/energy.md)水平。

可以从 JEI 拖入目标物品设置检测对象。若需要检测某种流体，可以手持装有该流体的容器右键设置槽。

标准发信器属于[线缆附属部件](../ae2-mechanics/cable-subparts.md)。与多数其他[设备](../ae2-mechanics/devices.md)不同，**标准发信器不占用[频道](../ae2-mechanics/channels.md)**。

## 基本设置

* **大于或等于**：库存达到设定数量后输出红石信号。
* **小于**：库存低于设定数量时输出红石信号。
* 安装<ItemLink id="crafting_card" />后，可切换为自动合成相关的控制模式。

## 升级

标准发信器支持以下[升级卡](upgrade_cards.md)：

* <ItemLink id="fuzzy_card" />：比较物品时可选择忽略部分耐久或 NBT 差异。
* <ItemLink id="crafting_card" />：启用自动合成控制功能。

## 自动合成控制

安装<ItemLink id="crafting_card" />后，可以选择两种常见用途：

**合成期间输出信号：**当[自动合成系统](../ae2-mechanics/autocrafting.md)正在制作指定物品时输出红石信号。这适合仅在生产任务运行时启用的机器或产线。

**通过红石信号请求合成：**将红石状态作为某种物品的生产请求。它特别适合刷石机、农场等无需从 ME 网络主动投入原料的生产线。

此时系统会生成一个虚拟的[样板](patterns.md)来代表这种生产方式，因此不应再为同一种物品同时放置会发生冲突的实际样板。可以将其理解为：发信器被触发后，外部生产线开始工作，直到所需产物重新进入网络。

<ItemLink id="pattern_provider" />可以配合其他处理样板完成更复杂的任务。

## 合成配方

<RecipeFor id="level_emitter" />

<RecipeFor id="energy_level_emitter" />
