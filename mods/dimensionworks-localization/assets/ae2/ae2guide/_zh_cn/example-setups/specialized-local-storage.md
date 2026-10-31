---
navigation:
  parent: example-setups/example-setups-index.md
  title: 农场专用本地存储
  icon: drive
---

# 农场专用本地存储

通过[ME 接口的特殊连接方式](../items-blocks-machines/interface.md#special-interactions)，可以让[子网络](../ae2-mechanics/subnetworks.md)中的存储空间对主网络可见，但子网本身无法浏览主网络存储。这种结构只占用主网 **1 个[频道](../ae2-mechanics/channels.md)**。

这特别适合农场：让产物优先进入本地专用仓库，避免大量圆石、作物等物品占满主仓库。

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/local_storage.snbt" />

<BoxAnnotation color="#dddddd" min="4 0 0" max="5 2 1">
        （1）物品输入方式：这里使用 ME 接口接收农场产物。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="3 0 0" max="4 1 1">
        （2）驱动器：放入已筛选农场产物的存储元件；可安装均分卡和溢出销毁卡。
        <Row><ItemImage id="item_storage_cell_4k" scale="2" /><ItemImage id="equal_distribution_card" scale="2" /><ItemImage id="void_card" scale="2" /></Row>
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="3 1 0" max="4 2 0.3">
        （3）子网合成终端：只能浏览子网驱动器中的内容。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="2 0 0" max="2.3 1 1">
        （4）接口 2：保持默认配置，作为主网与子网的存储桥接点。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="1.7 0 0" max="2 1 1">
        （5）存储总线：筛选农场产物，并将优先级设为高于主仓库。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="1 1 0" max="2 2 0.3">
        主网络合成终端：既能查看主网络存储，也能访问通过接口暴露的子网存储。
  </BoxAnnotation>

<DiamondAnnotation pos="0 0.5 0.5" color="#00ff00">
        连接主网络
    </DiamondAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 配置

* 第一个<ItemLink id="interface" />（1）：接收农场产物，并将它们送入子网络。
* <ItemLink id="drive" />（2）：放入通过[元件工作台](../items-blocks-machines/cell_workbench.md)设置了农场产物筛选的[存储元件](../items-blocks-machines/storage_cells.md)。可根据需要安装<ItemLink id="equal_distribution_card" />和<ItemLink id="void_card" />。
* 第二个<ItemLink id="interface" />（4）：保持默认配置。
* <ItemLink id="storage_bus" />（5）：筛选农场产物，把[存储优先级](../ae2-mechanics/import-export-storage.md#storage-priority)设为高于主仓库。

## 工作流程

1. 子网中的<ItemLink id="drive" />负责保存农场产物；子网合成终端只看到这些本地物品。
2. 主网中的<ItemLink id="storage_bus" />通过子网的<ItemLink id="interface" />访问驱动器。因为优先级较高，主网收到符合筛选的物品时，会优先将它们存入子网元件。
3. 子网元件装满时，符合筛选的物品不会自动溢出到主仓库。对于持续产出单一物品的农场，可以安装<ItemLink id="void_card" />处理多余产物，但要注意这会销毁资源。
4. 当农场产物类型较多时，<ItemLink id="equal_distribution_card" />可以防止某一种物品占满全部存储容量。

