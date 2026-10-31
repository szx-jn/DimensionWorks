---
navigation:
  parent: example-setups/example-setups-index.md
  title: 自动时运处理矿石
  icon: minecraft:raw_iron
---

# 自动时运处理矿石

<ItemLink id="annihilation_plane" />可以附魔，包括“时运”。因此，可以组合<ItemLink id="formation_plane" />与<ItemLink id="annihilation_plane" />：先自动放置矿石，再用带时运的破坏面板挖掘它们，从而提高产量。

注意：<ItemLink id="import_bus" />进入一段时间的空闲状态后，重新检测到物品时会逐步提高传输速度，因此产线刚启动时可能比较慢。

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/ore_fortuner.snbt" />

  <BoxAnnotation color="#dddddd" min="2.7 0 2" max="3 1 3">
        （1）输入总线：安装若干加速卡，提高产物回收速度。
        <ItemImage id="speed_card" scale="2" />
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="0 0 2" max="2 1 2.3">
        （2）成型面板：默认配置，用于自动放置矿石。
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="0 0 0.7" max="2 1 1">
        （3）破坏面板：无需额外配置，但必须附魔“时运”。
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="2.7 0 0" max="3 1 1">
        （4）存储总线：默认配置，负责连接产物缓冲容器。
  </BoxAnnotation>

<DiamondAnnotation pos="3.5 0.5 2.5" color="#00ff00">
        矿石输入
    </DiamondAnnotation>

<DiamondAnnotation pos="3.5 0.5 0.5" color="#00ff00">
        产物输出
    </DiamondAnnotation>

<DiamondAnnotation pos="4 0.5 1.5" color="#00ff00">
        连接主网络
    </DiamondAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 配置

* <ItemLink id="import_bus" />（1）：安装足够的<ItemLink id="speed_card" />，确保输入速度跟得上成型面板的放置速度。
* <ItemLink id="formation_plane" />（2）：保持默认配置。
* <ItemLink id="annihilation_plane" />（3）：无需打开界面配置，但要附上“时运”。
* <ItemLink id="storage_bus" />（4）：保持默认配置。

## 工作流程

1. 绿色子网中的<ItemLink id="import_bus" />从矿石输入容器抽取矿石。
2. 绿色子网中的<ItemLink id="formation_plane" />在指定位置放置矿石。
3. 橙色子网中的<ItemLink id="annihilation_plane" />破坏矿石，应用时运效果。
4. 橙色子网通过<ItemLink id="storage_bus" />把掉落物放入产物容器，再送回主网络。

