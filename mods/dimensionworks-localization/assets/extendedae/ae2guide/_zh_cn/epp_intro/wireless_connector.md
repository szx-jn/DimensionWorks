---
navigation:
    parent: epp_intro/epp_intro-index.md
    title: ME 无线连接器
    icon: extendedae:wireless_connect
categories:
- extended devices
item_ids:
- extendedae:wireless_connect
- extendedae:wireless_tool
---

# ME 无线连接器

<Row gap="20">
<BlockImage id="extendedae:wireless_connect" scale="6"></BlockImage>
<ItemImage id="extendedae:wireless_tool" scale="6"></ItemImage>
</Row>

ME 无线连接器可以像<ItemLink id="ae2:quantum_link" />一样连接两个网络，但连接范围有限，且无法跨维度连接。它只支持一对一连接；如果要建立多对多连接，请使用<ItemLink id="extendedae:wireless_hub" />。

## 连接无线连接器

使用 ME 无线设置工具依次点击要连接的两个无线连接器。

潜行并点击，可清除 ME 无线设置工具当前保存的设置。

成功建立连接后，无线连接器会改变纹理。

未连接的 ME 无线连接器

<GameScene zoom="5" background="transparent">
  <ImportStructure src="../structure/wireless_connector_off.snbt"></ImportStructure>
</GameScene>

已连接的 ME 无线连接器

<GameScene zoom="5" background="transparent">
  <ImportStructure src="../structure/wireless_connector_on.snbt"></ImportStructure>
</GameScene>

## 颜色

无线连接器可以像线缆一样染色，并且只能连接颜色相同的线缆或连接器。

需要使用<ItemLink id="ae2:color_applicator" />为连接器染色。

因此，你可以按如下方式布置无线连接器：

<GameScene zoom="3" background="transparent" interactive={true}>
  <ImportStructure src="../structure/wireless_connector_setup.snbt"></ImportStructure>
</GameScene>

## 能量消耗

ME 无线连接器之间的距离越远，能量消耗越大。能耗与距离并非线性关系，因此相距过远时，能量消耗可能会非常高。

可以使用<ItemLink id="ae2:energy_card" />节省能量；每张卡可将能量消耗降低 10%。
