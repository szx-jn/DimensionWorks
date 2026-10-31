---
navigation:
    parent: epp_intro/epp_intro-index.md
    title: ME 无线连接器
    icon: expatternprovider:wireless_connect
categories:
- extended devices
item_ids:
- expatternprovider:wireless_connect
- expatternprovider:wireless_tool
---

# ME 无线连接器

<Row gap="20">
<BlockImage id="expatternprovider:wireless_connect" scale="6"></BlockImage>
<ItemImage id="expatternprovider:wireless_tool" scale="6"></ItemImage>
</Row>

ME 无线连接器可以在一定距离内无线连接两处 ME 网络，其用途类似<ItemLink id="ae2:quantum_link" />，但**不能跨维度连接**。

普通无线连接器只支持一对一配对。如需同时连接多个设备，可使用<ItemLink id="expatternprovider:wireless_hub" />。

## 配对无线连接器

手持无线设置工具，依次右键点击需要配对的两个连接器，即可建立连接。

潜行并右键使用设置工具，可以清除工具中保存的配对信息。

连接成功后，连接器的外观会发生变化。

未连接状态：

<GameScene zoom="5" background="transparent">
  <ImportStructure src="../structure/wireless_connector_off.snbt"></ImportStructure>
</GameScene>

已连接状态：

<GameScene zoom="5" background="transparent">
  <ImportStructure src="../structure/wireless_connector_on.snbt"></ImportStructure>
</GameScene>

## 颜色匹配

无线连接器可以像 ME 线缆一样染色，只有颜色匹配的组件才能相互连接。

使用<ItemLink id="ae2:color_applicator" />调整颜色，就能区分多条无线网络。

<GameScene zoom="3" background="transparent" interactive={true}>
  <ImportStructure src="../structure/wireless_connector_setup.snbt"></ImportStructure>
</GameScene>

## 能源消耗

无线连接的距离越远，耗能越高，而且**耗能增长并非线性**。两端距离过远时，能源成本可能急剧上升。

可以安装<ItemLink id="ae2:energy_card" />降低耗能，每张能源卡减少约 10% 的能量开销。
