---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 存储监视器
  icon: storage_monitor
  position: 210
categories:
- devices
item_ids:
- ae2:storage_monitor
- ae2:conversion_monitor
---

# 存储监视器

<GameScene zoom="8" background="transparent">
<ImportStructure src="../assets/assemblies/monitors.snbt" />
<IsometricCamera yaw="195" pitch="30" />
</GameScene>

监视器可以在不打开终端界面的情况下，直接显示并操作某种物品或流体。

监视器会继承所连接[线缆](cables.md)的颜色。如果安装在地板或天花板上，可以使用<ItemLink id="certus_quartz_wrench" />调整显示方向。

监视器是[线缆附属部件](../ae2-mechanics/cable-subparts.md)。

## 存储监视器

存储监视器显示指定物品或流体的图标，以及 ME 网络中对应资源的数量，适合安装在农场或工厂旁边，实时查看库存。

**存储监视器不占用[频道](../ae2-mechanics/channels.md)。**

操作方式：

* 手持物品右键监视器，或使用流体容器设置需要显示的资源。
* 空手右键清除当前监视内容。
* 空手潜行并右键锁定或解锁监视器。

### 合成配方

<RecipeFor id="storage_monitor" />

## 转换监视器

转换监视器除了显示数量，还可以向 ME 网络中存入或取出指定物品。

当网络中没有指定物品，但存在对应[自动合成](../ae2-mechanics/autocrafting.md)配方时，可以直接发起合成请求。

**转换监视器需要占用一个[频道](../ae2-mechanics/channels.md)。**

额外操作：

* 左键：取出一组已设置的物品；若库存不足且支持自动合成，则可以请求合成。
* 手持物品右键：将该物品存入网络。
* 空手右键：把背包中符合设置的物品全部存入网络。

### 合成配方

<RecipeFor id="conversion_monitor" />
