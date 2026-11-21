---
navigation:
    parent: epp_intro/epp_intro-index.md
    title: ME 阈值输出总线
    icon: extendedae:threshold_export_bus
categories:
- extended devices
item_ids:
- extendedae:threshold_export_bus
---

# ME 阈值输出总线

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../structure/cable_threshold_export_bus.snbt"></ImportStructure>
</GameScene>

当 ME 网络中某种物品的数量高于或低于设定阈值时，ME 阈值输出总线会运行。

## 示例

![GUI](../pic/thr_bus_gui1.png)

铜的阈值设为 128，因此网络中铜的数量超过 128 时，总线会输出铜。

![GUI](../pic/thr_bus_gui2.png)

阈值与上例相同，但模式设为“低于”。网络中铜的数量低于 128 时，总线会输出铜。
