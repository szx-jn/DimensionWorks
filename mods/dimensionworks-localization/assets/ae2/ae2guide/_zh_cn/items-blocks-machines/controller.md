---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: ME 控制器
  icon: controller
  position: 110
categories:
- network infrastructure
item_ids:
- ae2:controller
---

# ME 控制器

<BlockImage id="controller" p:state="online" scale="8" />

ME 控制器是大型[ME 网络](../ae2-mechanics/me-network-connections.md)的核心组件。

没有控制器时，网络处于**临时网络（Ad-hoc）**模式，最多只能供 **8 个需要频道的[设备](../ae2-mechanics/devices.md)** 工作。

同一个 ME 网络不能存在两个彼此独立的控制器结构。

每个控制器方块的每一侧最多可提供 **32 个[频道](../ae2-mechanics/channels.md)**。

控制器工作时每格通常消耗 6 AE/t，每个控制器方块能够储存 8000 AE。因此，大型网络还需要配置充足的[能源](../ae2-mechanics/energy.md)供应和缓冲。

多个控制器方块可以组合成多方块结构，不必拘泥于单个方块。

<GameScene zoom="2" background="transparent">
  <ImportStructure src="../assets/assemblies/controllers.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 多方块结构规则

1. 同一[ME 网络](../ae2-mechanics/me-network-connections.md)中的控制器方块必须连接在同一个结构中；否则会显示红色错误状态。
2. 整个控制器结构必须在 **7×7×7** 的尺寸范围内。
3. 控制器结构不能出现过宽的连续实体截面：每条轴线上允许的相邻控制器组合受结构规则限制，违规时结构会变红并停止工作。

<GameScene zoom="2" background="transparent">
  <ImportStructure src="../assets/assemblies/controller_rules.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

符合规则并通电后，控制器会亮起，并显示循环变化的颜色。

右键点击控制器可以打开网络诊断界面，其功能与<ItemLink id="network_tool" />提供的诊断信息类似。

## 合成配方

<RecipeFor id="controller" />
