---
navigation:
    parent: epp_intro/epp_intro-index.md
    title: ME 精确输出总线
    icon: extendedae:precise_export_bus
categories:
- extended devices
item_ids:
- extendedae:precise_export_bus
---

# ME 精确输出总线

<GameScene zoom="8" background="transparent">
  <ImportStructure src="../structure/cable_precise_export_bus.snbt"></ImportStructure>
</GameScene>

ME 精确输出总线会按指定数量输出物品或流体。只有容器能够完整接收全部输出时，它才会进行输出。

## 示例

![GUI](../pic/pre_bus_gui1.png)

此设置每次操作输出 3 个圆石。当网络中的圆石少于 3 个时，输出就会停止。

![GUI](../pic/pre_bus_gui2.png)

如果目标容器无法容纳全部输出，输出也会停止。箱子只能再容纳 2 个圆石，因此输出总线会停止工作。
