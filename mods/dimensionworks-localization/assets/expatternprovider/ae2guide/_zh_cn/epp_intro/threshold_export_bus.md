---
navigation:
    parent: epp_intro/epp_intro-index.md
    title: ME 阈值输出总线
    icon: expatternprovider:threshold_export_bus
categories:
- extended devices
item_ids:
- expatternprovider:threshold_export_bus
---

# ME 阈值输出总线

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../structure/cable_threshold_export_bus.snbt"></ImportStructure>
</GameScene>

ME 阈值输出总线会根据 **ME 网络中某种资源的库存量**与设定阈值的比较结果，决定是否向外输出。

## 示例

![高于阈值时输出](../pic/thr_bus_gui1.png)

将铜的阈值设为 128，并选择“高于”（ABOVE）模式：只有当网络中铜的数量超过 128 时，才会向外输出。

![低于阈值时输出](../pic/thr_bus_gui2.png)

如果改为“低于”（BELOW）模式，只有网络中铜的数量不足 128 时，才会执行输出。
