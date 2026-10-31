---
navigation:
    parent: epp_intro/epp_intro-index.md
    title: ME 设备升级组件
    icon: expatternprovider:pattern_provider_upgrade
categories:
- extended items
item_ids:
- expatternprovider:pattern_provider_upgrade
- expatternprovider:interface_upgrade
- expatternprovider:io_bus_upgrade
- expatternprovider:pattern_terminal_upgrade
- expatternprovider:drive_upgrade
---

# ME 设备升级组件

使用升级组件，可以直接把普通 ME 设备升级为扩展版本，无需先破坏或拆卸设备。

<Row>
<ItemImage id="expatternprovider:pattern_provider_upgrade" scale="4"></ItemImage>
<ItemImage id="expatternprovider:interface_upgrade" scale="4"></ItemImage>
<ItemImage id="expatternprovider:io_bus_upgrade" scale="4"></ItemImage>
<ItemImage id="expatternprovider:pattern_terminal_upgrade" scale="4"></ItemImage>
<ItemImage id="expatternprovider:drive_upgrade" scale="4"></ItemImage>
</Row>

手持相应升级组件，**潜行并右键点击**支持的设备，即可原地完成升级。设备原有的设置、样板和存储内容都会保留。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../structure/upgrade_show_1.snbt"></ImportStructure>
  <BoxAnnotation color="#ffffff" min="1 0 0" max="4 1 1">
        普通样板供应器。使用样板供应器升级组件即可升级。
        <ItemImage id="expatternprovider:pattern_provider_upgrade" scale="2"></ItemImage>
  </BoxAnnotation>
</GameScene>
<GameScene zoom="6" background="transparent">
  <ImportStructure src="../structure/upgrade_show_2.snbt"></ImportStructure>
  <BoxAnnotation color="#ffffff" min="1 0 0" max="4 1 1">
        扩展样板供应器会保留普通版本的全部设置和样板库存。
  </BoxAnnotation>
</GameScene>

## 可升级设备列表

|                                      升级                                      |                           普通设备                            |                                  扩展设备                                  |
|:---------------------------------------------------------------------------------:|:------------------------------------------------------------------:|:---------------------------------------------------------------------------------:|
| <ItemImage id="expatternprovider:pattern_provider_upgrade" scale="3"></ItemImage> |    <ItemImage id="ae2:pattern_provider" scale="3"></ItemImage>     |   <ItemImage id="expatternprovider:ex_pattern_provider" scale="3"></ItemImage>    |
| <ItemImage id="expatternprovider:pattern_provider_upgrade" scale="3"></ItemImage> | <ItemImage id="ae2:cable_pattern_provider" scale="3"></ItemImage>  | <ItemImage id="expatternprovider:ex_pattern_provider_part" scale="3"></ItemImage> |
|    <ItemImage id="expatternprovider:interface_upgrade" scale="3"></ItemImage>     |        <ItemImage id="ae2:interface" scale="3"></ItemImage>        |       <ItemImage id="expatternprovider:ex_interface" scale="3"></ItemImage>       |
|    <ItemImage id="expatternprovider:interface_upgrade" scale="3"></ItemImage>     |     <ItemImage id="ae2:cable_interface" scale="3"></ItemImage>     |    <ItemImage id="expatternprovider:ex_interface_part" scale="3"></ItemImage>     |
|      <ItemImage id="expatternprovider:io_bus_upgrade" scale="3"></ItemImage>      |       <ItemImage id="ae2:import_bus" scale="3"></ItemImage>        |    <ItemImage id="expatternprovider:ex_import_bus_part" scale="3"></ItemImage>    |
|      <ItemImage id="expatternprovider:io_bus_upgrade" scale="3"></ItemImage>      |       <ItemImage id="ae2:export_bus" scale="3"></ItemImage>        |    <ItemImage id="expatternprovider:ex_export_bus_part" scale="3"></ItemImage>    |
| <ItemImage id="expatternprovider:pattern_terminal_upgrade" scale="3"></ItemImage> | <ItemImage id="ae2:pattern_access_terminal" scale="3"></ItemImage> |  <ItemImage id="expatternprovider:ex_pattern_access_part" scale="3"></ItemImage>  |
|      <ItemImage id="expatternprovider:drive_upgrade" scale="3"></ItemImage>       |          <ItemImage id="ae2:drive" scale="3"></ItemImage>          |         <ItemImage id="expatternprovider:ex_drive" scale="3"></ItemImage>         |
