---
navigation:
    parent: epp_intro/epp_intro-index.md
    title: ME 精确输出总线
    icon: expatternprovider:precise_export_bus
categories:
- extended devices
item_ids:
- expatternprovider:precise_export_bus
---

# ME 精确输出总线

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../structure/cable_precise_export_bus.snbt"></ImportStructure>
</GameScene>

ME 精确输出总线会按照设定的**固定数量**输出物品或流体。只有网络中存量足够、目标容器也能够一次接收全部数量时，才会执行输出。

## 示例

![每次输出数量](../pic/pre_bus_gui1.png)

如图设置为每次输出 3 个圆石。若 ME 网络中的圆石不足 3 个，总线就会暂停输出。

![目标容器剩余空间](../pic/pre_bus_gui2.png)

如果目标箱子只剩 2 个圆石的空位，同样不会输出。这种方式可以避免只传入部分材料。
