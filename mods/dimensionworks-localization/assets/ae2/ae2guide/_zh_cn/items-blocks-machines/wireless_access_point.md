---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 无线接入点
  icon: wireless_access_point
  position: 210
categories:
- devices
item_ids:
- ae2:wireless_booster
- ae2:wireless_access_point
---

# 无线接入点

<BlockImage id="wireless_access_point" p:state="has_channel" scale="8" />

无线接入点允许<ItemLink id="wireless_terminal" />在一定距离内访问 ME 网络。访问距离和能源消耗取决于安装的<ItemLink id="wireless_booster" />数量。

一个网络可以安装多个无线接入点。你可以给不同位置的接入点配置不同数量的无线增幅器，在覆盖范围和能耗之间取得平衡。

无线接入点需要占用一个[频道](../ae2-mechanics/channels.md)，也可以用于绑定[无线终端](wireless_terminals.md)。

## 无线增幅器

<ItemImage id="wireless_booster" scale="2" />

无线增幅器用于扩大接入点的有效范围。

## 合成配方

<RecipeFor id="wireless_access_point" />

<RecipeFor id="wireless_booster" />
