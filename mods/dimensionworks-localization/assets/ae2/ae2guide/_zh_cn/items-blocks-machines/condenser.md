---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 物质浓缩器
  icon: condenser
  position: 310
categories:
- machines
item_ids:
- ae2:condenser
---

# 物质浓缩器

<BlockImage id="condenser" scale="8" />

物质浓缩器既可以当作垃圾处理器，也能把多余的物品或流体转化为<ItemLink id="matter_ball" />和[奇点](singularities.md)。它能够处理 ME 存储系统支持的大多数资源类型。

## 运行模式

* **垃圾处理模式：**销毁所有输入的物品和流体，不产生输出。
* **物质球模式：**使用放入上方槽位的存储组件，将输入资源压缩为<ItemLink id="matter_ball" />。每个物质球需要 256 单位物品或流体；安装<ItemLink id="cell_component_1k" />通常就足够。
* **奇点模式：**将大量输入资源压缩为[奇点](singularities.md)。每个奇点需要约 256,000 单位资源，通常需要<ItemLink id="cell_component_64k" />等更大容量的存储组件。

在后两种模式中，如果内部物质存储空间或输出缓冲已满，浓缩器会暂停接收新的物品。设计自动化产线时要考虑这一点。

## 合成配方

<RecipeFor id="condenser" />
