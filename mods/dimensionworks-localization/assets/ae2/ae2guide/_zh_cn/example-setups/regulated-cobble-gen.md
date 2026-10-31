---
navigation:
  parent: example-setups/example-setups-index.md
  title: 自动限量圆石生产
  icon: minecraft:cobblestone
---

# 自动限量圆石生产

将<ItemLink id="annihilation_plane" />对准普通圆石生成器，就能不断挖掘圆石。但如果不加控制，圆石最终会占满 ME 网络的存储空间。

破坏面板的工作方式与<ItemLink id="import_bus" />类似，不能直接把它当作<ItemLink id="export_bus" />用红石控制。因此，这里使用<ItemLink id="level_emitter" />配合<ItemLink id="toggle_bus" />断开或接通专用[子网络](../ae2-mechanics/subnetworks.md)。

切换总线会让它所连接的网络重新计算。为了避免频繁重启主网络，应该把切换总线放在子网一侧，而不是主网一侧。子网通过<ItemLink id="quartz_fiber" />共享能源。

<GameScene zoom="4" interactive={true}>
  <ImportStructure src="../assets/assemblies/regulated_cobble_gen.snbt" />

<BoxAnnotation color="#dddddd" min="3 2 2" max="7 2.3 3">
        （1）破坏面板：无需界面配置，可以附魔效率和耐久来降低工作成本。
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="2 2 2" max="2.3 3 3">
        （2）存储总线：保持默认配置，将圆石送回主网络。
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="2.3 2.3 2" max="2.7 2.7 2.3">
        （3）切换总线：必须放在子网一侧，避免每次切换都触发主网络重启。
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="2.3 3 2.3" max="2.7 3.3 2.7">
        （4）标准发信器：设定圆石目标库存，配置为“低于阈值时输出信号”。
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="1 2 3" max="2 3 2">
        （5）ME 接口：保持默认配置。
  </BoxAnnotation>

<DiamondAnnotation pos="0 2.5 1.5" color="#00ff00">
        连接主网络
    </DiamondAnnotation>

<DiamondAnnotation pos="5 1.5 3.5" color="#00ff00">
        水中的楼梯阻挡水流，避免错误生成黑曜石。
    </DiamondAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 配置

* <ItemLink id="annihilation_plane" />（1）：无需界面配置，可以按需要附魔。
* <ItemLink id="storage_bus" />（2）：保持默认配置。
* <ItemLink id="toggle_bus" />（3）：安装在石英纤维的**子网一侧**，不要装在主网。
* <ItemLink id="level_emitter" />（4）：选择“库存低于阈值时输出信号”。
* <ItemLink id="interface" />（5）：保持默认设置。

## 工作流程

1. 圆石生成器在水与熔岩的作用下产生圆石。
2. <ItemLink id="annihilation_plane" />自动破坏圆石。
3. <ItemLink id="storage_bus" />通过<ItemLink id="interface" />将圆石送入主网络。
4. 当圆石存量达到设定上限时，<ItemLink id="level_emitter" />停止输出红石信号，关闭<ItemLink id="toggle_bus" />。
5. 子网因此断开供能，破坏面板停止工作；库存下降后再恢复。

