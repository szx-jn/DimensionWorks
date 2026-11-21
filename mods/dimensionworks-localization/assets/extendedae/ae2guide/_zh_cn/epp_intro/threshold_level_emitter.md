---
navigation:
    parent: epp_intro/epp_intro-index.md
    title: ME 阈值数量发信器
    icon: extendedae:threshold_level_emitter
categories:
- extended devices
item_ids:
- extendedae:threshold_level_emitter
---

# ME 阈值数量发信器

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../structure/cable_threshold_level_emitter.snbt"></ImportStructure>
</GameScene>

它的工作方式类似复位-置位锁存器。当网络中的某种物品数量低于下限阈值时，它会关闭红石信号；当数量高于上限阈值时，则会打开信号。

例如，下限阈值设为 100，上限阈值设为 150。

起初网络为空，因此发信器未激活。

物品数量超过 150 后，发信器发送红石信号。

当物品数量降到 150 以下时，发信器仍会继续发送信号。

最后，当数量低于 100 时，发信器关闭信号。
