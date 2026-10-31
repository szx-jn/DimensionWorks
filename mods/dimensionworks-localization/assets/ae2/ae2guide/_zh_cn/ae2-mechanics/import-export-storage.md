---
navigation:
  parent: ae2-mechanics/ae2-mechanics-index.md
  title: 网络存储与输入输出
---

# 网络存储与输入输出

**理解 ME 网络如何与外部世界交换物品，是搭建自动化生产线的基础。**

“网络存储”是指 ME 网络实际管理的全部物品与资源，既包括[存储元件](../items-blocks-machines/storage_cells.md)，也可以包括通过<ItemLink id="storage_bus" />接入的外部箱子或储罐。许多 AE2 [设备](../ae2-mechanics/devices.md)都围绕网络存储工作。

* <ItemLink id="import_bus" />：从外部容器抽取资源，存入网络。
* <ItemLink id="export_bus" />：从网络提取资源，送到外部容器。
* <ItemLink id="interface" />：根据配置从网络存储提取物品，也能将接收到的物品重新送回网络。
* [终端](../items-blocks-machines/terminals.md)：玩家可直接向网络存入物品、取出物品，以及给合成格补充材料。
* <ItemLink id="storage_bus" />：将外部容器视为网络存储的一部分；它本身并不主动搬运物品。

<GameScene zoom="4" interactive={true}>
  <ImportStructure src="../assets/assemblies/import_export_storage.snbt" />

  <BoxAnnotation color="#dddddd" min="8 1 1" max="9 1.3 2">
    输入总线从它所指向的容器中抽取物品，并将物品存入网络
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="8 2 1" max="9 3 1.3">
    从终端向网络存入物品，也属于输入操作
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="7 0 1" max="8 1 2">
    接口会根据配置从外部接收物品，再把接收到的物品存入 ME 网络
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="6 0 1" max="7 1 2">
    样板供应器能够接收加工后的产物，并将其送回自动合成网络
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="4 1 1" max="5 2 2">
    ME 驱动器中的存储元件为网络提供存储空间
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="3 1 1" max="4 1.3 2">
    存储总线把所连接的外部容器加入网络存储
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="1 1 1" max="2 1.3 2">
    输出总线把网络中的物品送入它所指向的容器
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="1 2 1" max="2 3 1.3">
    从终端中取出物品，也属于输出操作
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="0 1 1" max="1 2 2">
    接口会根据设定的库存目标，从 ME 网络取出并缓存物品
  </BoxAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

设计自动化时，尤其要区分**将物品送回 ME 网络**与**只把物品放进外部容器**这两件事。

## 存储优先级

部分存储设备可以通过界面右上角的扳手调整优先级。

向网络存入物品时，系统优先使用优先级较高的存储设备。若优先级相同，通常会先选择已经存有同类物品的设备；具有分区白名单的存储元件，也可能优先接收匹配的物品。

从网络取出物品时，系统通常优先从较低优先级的存储设备中提取。因此，你可以借助优先级把物品逐渐集中到更高优先级的仓库。
