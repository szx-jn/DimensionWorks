---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: ME 接口
  icon: interface
  position: 210
categories:
- devices
item_ids:
- ae2:interface
- ae2:cable_interface
---

# ME 接口

<Row gap="20">
<BlockImage id="interface" scale="8" />
<GameScene zoom="8" background="transparent">
  <ImportStructure src="../assets/blocks/cable_interface.snbt" />
</GameScene>
</Row>

ME 接口相当于一个能够自动补充和回收物品的箱子与流体储罐。它会根据设定的目标库存，从[网络存储](../ae2-mechanics/import-export-storage.md)中提取资源，或者把多余资源送回网络。

它最多可以缓存 9 种不同的物品或流体，还能在一次操作中处理多个槽位。即使不接入网络，接口本身也可以作为一个容量有限的多资源容器使用。

## 接口的工作原理

可以把接口理解为同时具有<ItemLink id="import_bus" />、<ItemLink id="export_bus" />和多个<ItemLink id="level_emitter" />的组合。内部按目标库存自动输入、输出和调整物品数量。

<GameScene zoom="3" interactive={true}>
  <ImportStructure src="../assets/assemblies/interface_internals.snbt" />

  <BoxAnnotation color="#dddddd" min="1.3 0.3 1.3" max="9.7 1 1.7">
        一组标准发信器，负责判断各槽位的目标库存数量
        <GameScene zoom="4" background="transparent">
        <ImportStructure src="../assets/blocks/level_emitter.snbt" />
        </GameScene>
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="1.3 4 1.3" max="9.7 4.7 1.7">
        一组标准发信器，负责判断各槽位的目标库存数量
        <GameScene zoom="4" background="transparent">
        <ImportStructure src="../assets/blocks/level_emitter.snbt" />
        </GameScene>
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="1.3 1.3 1.3" max="9.7 2 1.7">
        输入总线负责将多余的物品送回 ME 网络
        <GameScene zoom="4" background="transparent">
        <ImportStructure src="../assets/blocks/import_bus.snbt" />
        </GameScene>
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="1.3 3 1.3" max="9.7 3.7 1.7">
        输出总线负责从 ME 网络补充物品
        <GameScene zoom="4" background="transparent">
        <ImportStructure src="../assets/blocks/export_bus.snbt" />
        </GameScene>
  </BoxAnnotation>

  <BoxAnnotation color="#dddddd" min="1 2 1" max="10 3 2">
        9 个独立的内部缓存槽
  </BoxAnnotation>

  <IsometricCamera yaw="195" pitch="15" />
</GameScene>

## 特殊交互

ME 接口与其他[设备](../ae2-mechanics/devices.md)还有一些特殊的配合方式。

当接口没有设置目标库存时，将<ItemLink id="storage_bus" />连接到它，可以让存储总线所在的网络访问接口所连接的[网络存储](../ae2-mechanics/import-export-storage.md)，就像把另一套网络当作一个大型仓库。为接口配置目标库存后，这种特殊行为会停止。

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/interface_storage.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

样板供应器可以通过没有设置目标库存的接口，把整批加工材料直接送入[子网络](../ae2-mechanics/subnetworks.md)的[存储系统](../ae2-mechanics/import-export-storage.md)。这时材料会绕过接口本身的缓冲区，避免因缓存空间不足而只传输部分材料。

<GameScene zoom="6" background="transparent">
<ImportStructure src="../assets/assemblies/provider_interface_storage.snbt" />

<BoxAnnotation color="#dddddd" min="2.7 0 1" max="3 1 2">
        扁平接口（不能使用完整方块形态）
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="1 0 0" max="1.3 1 4">
        外部存储容器
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="0 0 0" max="1 1 4">
        样板供应器的输出目标（可以连接多台机器，或机器的多个输入面）
  </BoxAnnotation>

<IsometricCamera yaw="185" pitch="30" />
</GameScene>

## 两种安装形式

ME 接口有完整方块和[扁平附属部件](../ae2-mechanics/cable-subparts.md)两种形式：

* **完整方块接口**：各面可以输入和输出物品，并像其他完整的 AE2 设备一样，在多个方向提供网络连接。
* **扁平接口**：安装在线缆表面，节省空间。它从指定面交换资源，但不会通过这个面把外部设备连接到网络。

两种形态可以通过合成栏互相转换。

## 库存设置

界面上方的配置槽用于设定每种物品或流体应保留的数量。将物品放入配置槽或从 JEI 拖入图标后，可以点击扳手设置目标数量。

如果想配置流体本身，可以手持装有该流体的桶或储罐右键配置槽，而不是把容器物品当作目标。

## 升级卡

ME 接口支持以下[升级卡](upgrade_cards.md)：

* <ItemLink id="fuzzy_card" />：允许按需忽略部分耐久或 NBT 差异。
* <ItemLink id="crafting_card" />：库存不足时，可以向[自动合成系统](../ae2-mechanics/autocrafting.md)发出合成请求，补充目标物品。

## 优先级

可以通过界面右上角的扳手调整接口优先级。优先级较高的接口通常会更早获得所需材料。

## 合成配方

<Recipe id="network/blocks/interfaces_interface" />

<RecipeFor id="cable_interface" />
