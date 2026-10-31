---
navigation:
  parent: example-setups/example-setups-index.md
  title: 通过标准发信器自动补货
  icon: level_emitter
---

# 通过标准发信器自动补货

如果你需要为**某一种大量消耗的材料**维持最低库存，可以组合<ItemLink id="export_bus" />、<ItemLink id="level_emitter" />和<ItemLink id="crafting_card" />，让 ME [自动合成系统](../ae2-mechanics/autocrafting.md)在库存不足时启动生产。

示例中，标准发信器根据库存发出红石信号，输出总线再申请合成缺少的材料。

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/level_emitter_autostocking.snbt" />

  <BoxAnnotation color="#dddddd" min="1 1 0" max="2 1.3 1">
        （1）输出总线：筛选目标物品，并安装红石卡和合成卡。红石模式设为“收到信号时运行”，合成卡设为“不使用已有库存”。
        <Row><ItemImage id="redstone_card" scale="2" /><ItemImage id="crafting_card" scale="2" /></Row>
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="0.7 1 0" max="1 2 1">
        （2）标准发信器：设置目标物品、最低库存数量，并选择“低于阈值时输出信号”。
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="1 0 0" max="2 1 1">
        （3）ME 接口：保持默认配置，用于接收补货物品。
  </BoxAnnotation>

<DiamondAnnotation pos="4 0.5 0.5" color="#00ff00">
        连接主网络
    </DiamondAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 配置

* <ItemLink id="export_bus" />（1）：设置目标物品，并安装<ItemLink id="redstone_card" />和<ItemLink id="crafting_card" />。红石模式选择“有信号时运行”，合成卡选择“不使用已有库存”。
* <ItemLink id="level_emitter" />（2）：设置目标物品和库存阈值，选择“库存低于阈值时输出信号”。
* <ItemLink id="interface" />（3）：保持默认配置。

## 工作流程

1. 当[网络存储](../ae2-mechanics/import-export-storage.md)中的目标物品少于阈值时，<ItemLink id="level_emitter" />输出红石信号。
2. <ItemLink id="export_bus" />收到信号后，通过<ItemLink id="crafting_card" />请求网络[自动合成](../ae2-mechanics/autocrafting.md)目标物品，并将产物输出。
3. <ItemLink id="interface" />将收到的物品重新提供给网络存储。

