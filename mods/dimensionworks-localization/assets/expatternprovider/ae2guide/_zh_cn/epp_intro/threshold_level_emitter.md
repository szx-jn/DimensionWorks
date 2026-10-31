---
navigation:
    parent: epp_intro/epp_intro-index.md
    title: ME 双阈值标准发信器
    icon: expatternprovider:threshold_level_emitter
categories:
- extended devices
item_ids:
- expatternprovider:threshold_level_emitter
---

# ME 双阈值标准发信器

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../structure/cable_threshold_level_emitter.snbt"></ImportStructure>
</GameScene>

这是一种具有**上下限回差**的标准发信器，工作方式类似红石的 RS 锁存器。它不会在库存量刚越过某个临界值时反复开关，而是分别使用“开启阈值”和“关闭阈值”。

例如，将下限设为 100、上限设为 150：

1. 网络最初为空时，发信器处于关闭状态。
2. 当物品数量增加到超过 150 时，发信器输出红石信号。
3. 之后即使数量下降到 150 以下，信号也会继续保持。
4. 只有当数量降到 100 以下时，发信器才会关闭。

这种控制方式特别适合避免自动化生产线在目标库存附近频繁启停。
