---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: 振动室
  icon: vibration_chamber
  position: 110
categories:
- network infrastructure
item_ids:
- ae2:vibration_chamber
---

# 振动室

<BlockImage id="vibration_chamber" p:active="true" scale="8" />

振动室可以在不使用<ItemLink id="energy_acceptor" />的情况下，直接为 ME 网络生产少量[AE 能量](../ae2-mechanics/energy.md)。

在原版默认设置下，没有升级卡的振动室约产生 **40 AE/t**。但维度工坊对 AE2 供能规则进行了改造，实际可用性与产出应以整合包内的配置和配方为准。

当网络已有充足能源时，振动室会降低燃料消耗，但通常不会完全停止。

## 设置

可以在界面中查看能源状态，也可以按配置切换能源显示单位。

## 升级

振动室支持以下[升级卡](upgrade_cards.md)：

* <ItemLink id="energy_card" />：提高燃料使用效率，每张卡通常增加 50%，最高可达到基础效率的 250%。
* <ItemLink id="speed_card" />：提高燃料消耗速度和发电功率，每张卡通常增加 50%，最高可达到基础输出的 250%。

## 配置

振动室的详细参数可以在 AE2 配置文件中调整，包括：

* 基础燃料效率：决定无升级时能从燃料获得多少 AE 能量。
* 每刻最低输出：即使网络暂时不缺电，也可能继续缓慢消耗燃料。
* 每刻最高输出：限制无升级时的最大功率。

## 合成配方

<RecipeFor id="vibration_chamber" />
