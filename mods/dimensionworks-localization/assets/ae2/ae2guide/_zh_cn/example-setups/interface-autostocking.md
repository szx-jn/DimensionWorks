---
navigation:
  parent: example-setups/example-setups-index.md
  title: 通过接口自动补货
  icon: interface
---

# 通过接口自动补货

如果希望网络中始终备有一定数量的不同物品，可以结合<ItemLink id="interface" />与<ItemLink id="crafting_card" />，让[自动合成系统](../ae2-mechanics/autocrafting.md)在库存不足时补充材料。这种方案适合维护**种类较多、每种数量较少**的常备库存。

示例使用 4 个<ItemLink id="interface" />和 4 个<ItemLink id="storage_bus" />，合计占用 8 个[频道](../ae2-mechanics/channels.md)，可以通过一条普通[ME 线缆](../items-blocks-machines/cables.md)连接主网络。

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/interface_autostocking.snbt" />

<BoxAnnotation color="#dddddd" min="0 0 0" max="2 1 1">
        （1）ME 接口：设置希望保留的物品及数量，安装合成卡。
        <ItemImage id="crafting_card" scale="2" />
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="0 1 0" max="2 1.3 1">
        （2）存储总线：将输入／输出模式设为“仅提取”。
  </BoxAnnotation>

<DiamondAnnotation pos="4 0.5 0.5" color="#00ff00">
        连接主网络
    </DiamondAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 配置

* <ItemLink id="interface" />（1）：将希望保留的物品拖入配置槽（可以从 JEI 拖入），再点击槽位上方的扳手设置数量。安装<ItemLink id="crafting_card" />。
* <ItemLink id="storage_bus" />（2）：将“输入／输出模式”设为“仅提取”。

## 工作流程

1. 当<ItemLink id="interface" />无法从[网络存储](../ae2-mechanics/import-export-storage.md)取出足够的指定物品时，已安装的<ItemLink id="crafting_card" />会向网络提交[自动合成](../ae2-mechanics/autocrafting.md)请求。
2. <ItemLink id="storage_bus" />允许网络访问接口中保存的物品。
3. 合成产物进入网络后，接口会再次补满到指定库存量。

