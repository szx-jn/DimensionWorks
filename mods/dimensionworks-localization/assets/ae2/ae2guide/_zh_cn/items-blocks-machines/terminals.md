---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: ME 终端
  icon: crafting_terminal
  position: 210
categories:
- devices
item_ids:
- ae2:terminal
- ae2:crafting_terminal
- ae2:pattern_encoding_terminal
- ae2:pattern_access_terminal
---

# ME 终端

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/terminals.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

终端是玩家与 ME 网络交互的主要入口。<ItemLink id="pattern_provider" />、<ItemLink id="import_bus" />与<ItemLink id="storage_bus" />等设备负责物流，而终端让你直接查看库存、存取物品以及控制自动合成。

终端会继承所安装[线缆](cables.md)的颜色，属于[线缆附属部件](../ae2-mechanics/cable-subparts.md)。

## 正确安装终端

终端需要安装在能够连接 ME 网络的方块表面，而不能随意朝向或与网络断开。下面展示了错误和正确的布置方式。

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/terminal_placement.snbt" />
  <IsometricCamera yaw="195" pitch="30" />

  <LineAnnotation color="#ff3333" from="2.5 .5 .5" to="4.5 2.5 .5" alwaysOnTop={true} thickness="0.05"/>
  <LineAnnotation color="#ff3333" from="2.5 2.5 .5" to="4.5 .5 .5" alwaysOnTop={true} thickness="0.05"/>

  <LineAnnotation color="#33ff33" from="-.5 2.5 .5" to="1 .5 .5" alwaysOnTop={true} thickness="0.05"/>
  <LineAnnotation color="#33ff33" from="1 .5 .5" to="1.5 1 .5" alwaysOnTop={true} thickness="0.05"/>
</GameScene>

正确安装时，终端与能源接收器可以紧凑排列，同时保持网络正常连通。

<a name="terminal-ui"></a>

## 普通终端

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/blocks/terminal.snbt" />
  <IsometricCamera yaw="180" />
</GameScene>

普通终端可以查看并访问[ME 网络存储](../ae2-mechanics/import-export-storage.md)，也能对已有配方发起[自动合成](../ae2-mechanics/autocrafting.md)请求。

### 界面操作

界面中央显示网络中的物品与流体，可以按常规方式存入或取出物品：

* 左键点击取出一组，右键点击取出半组。
* 如果某种物品存在自动合成配方，可以使用选取方块键（通常为鼠标中键）打开合成数量设置。数量框支持简单算式，例如 `3*64/2`。
* 按住 Ctrl 可以避免物品顺序在库存变化时频繁重排。
* 使用流体容器点击相应资源，可以在网络和容器之间装入或取出流体。

左侧可以设置物品排序方式、显示数量、是否仅显示可合成物品，以及界面高度等选项。右侧可以安装<ItemLink id="view_cell" />来过滤显示内容。

中央上方的合成状态按钮可以打开自动合成任务界面，查看每个[合成 CPU](crafting_cpu_multiblock.md)正在执行的任务及进度。

### 合成配方

<RecipeFor id="terminal" />

<a name="crafting-terminal-ui"></a>

## 合成终端

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/blocks/crafting_terminal.snbt" />
  <IsometricCamera yaw="180" />
</GameScene>

合成终端包含普通终端的全部功能，并额外提供工作台合成格。合成材料可以直接从[网络存储](../ae2-mechanics/import-export-storage.md)中补充。

界面上还可以把合成格中的材料一键退回 ME 网络或放回玩家背包。对于日常使用，合成终端通常比普通终端更方便。

### 合成配方

<RecipeFor id="crafting_terminal" />

<a name="pattern-encoding-terminal-ui"></a>

## 样板编码终端

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/blocks/pattern_encoding_terminal.snbt" />
  <IsometricCamera yaw="180" />
</GameScene>

样板编码终端用于将[配方写入样板](patterns.md)。它与普通终端一样能显示网络库存，但中央的合成格用于**记录配方**，不是直接生产物品。

操作流程：

1. 在空白槽放入<ItemLink id="blank_pattern" />。
2. 选择配方类型：普通合成、处理、锻造或切石。
3. 在界面中设置原料和产物，也可以从 JEI／REI 导入已有配方。
4. 点击编码按钮将配方写入样板。把已编码的样板放回相应槽位，还能修改旧配方。

普通合成模式可选择是否允许替代材料，例如使用不同种类木板制作木棍；需要配方稳定时，建议关闭不必要的替代设置。

处理模式允许配置物品和流体的输入、输出数量，适合加工机器或多段工业产线。点击物品槽或使用鼠标中键，可以精确调整数量。界面支持较多输入材料与副产物。

### 合成配方

<RecipeFor id="pattern_encoding_terminal" />

<a name="pattern-access-terminal-ui"></a>

## 样板管理终端

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/blocks/pattern_access_terminal.snbt" />
  <IsometricCamera yaw="180" />
</GameScene>

当 ME 网络的<ItemLink id="pattern_provider" />和<ItemLink id="molecular_assembler" />分布在工厂各处时，逐台寻找并更换[样板](patterns.md)非常麻烦。

样板管理终端会集中列出网络中的样板供应器及其样板槽。每一行对应一台供应器，可以按供应器所在方块、位置或自定义名称整理。

如果有大量自动化生产线，建议配合<ItemLink id="name_press" />为供应器命名，方便以后检修。

### 合成配方

<RecipeFor id="pattern_access_terminal" />
