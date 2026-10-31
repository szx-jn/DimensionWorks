---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 网络工具
  icon: network_tool
  position: 410
categories:
- tools
item_ids:
- ae2:network_tool
---

# 网络工具

<ItemImage id="network_tool" scale="4" />

网络工具是[石英扳手](wrench.md)的进阶版本，除了拆卸 AE2 组件，还可以查看网络诊断信息、携带[升级卡](upgrade_cards.md)。

它保留了扳手拆除方块与[线缆附属部件](../ae2-mechanics/cable-subparts.md)的功能，但不能用来旋转设备。

网络工具拥有 **9 个升级卡槽位**。只要工具位于背包中，就可以随时取用其中保存的升级卡。

右键点击 ME 网络中的设备，可以打开诊断界面，类似于右键点击<ItemLink id="controller" />，其中包括：

* 网络已使用的频道数量。
* AE 和 FE 等能源显示单位的切换选项（具体取决于整合包配置）。
* 网络当前的[能源](../ae2-mechanics/energy.md)存量和最大容量。
* 网络的能源输入和消耗情况。
* 网络中的全部[设备](../ae2-mechanics/devices.md)及组件。

如果两段线缆没有合并成一个网络，这个界面也有助于区分它们是否属于不同[子网络](../ae2-mechanics/subnetworks.md)。

## 临时隐藏伪装板

手持网络工具时，<a href="facades.md">伪装板</a>会临时隐藏，方便你直接操作后方的设备，无需先拆掉装饰外壳。

## 合成配方

<RecipeFor id="network_tool" />
