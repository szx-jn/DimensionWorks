---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: P2P 隧道
  icon: me_p2p_tunnel
  position: 210
categories:
- devices
item_ids:
- ae2:me_p2p_tunnel
- ae2:redstone_p2p_tunnel
- ae2:item_p2p_tunnel
- ae2:fluid_p2p_tunnel
- ae2:fe_p2p_tunnel
- ae2:light_p2p_tunnel
---

# P2P 隧道

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/p2p_tunnels.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

P2P（点对点）隧道能够在 ME 网络内部建立一条专用连接，传输物品、流体、红石信号、能源、光照或 ME [频道](../ae2-mechanics/channels.md)。从外部设备的角度看，隧道两端就像隔着一段距离直接相连。

**P2P 隧道有明确的输入端和输出端，不是双向传输。**

![P2P 隧道示意图](../assets/assemblies/p2p_portal.png)

例如，将物品 P2P 隧道的两端分别连接漏斗与木桶，漏斗就能像直接连接木桶一样转移物品：

<GameScene zoom="4" background="transparent">
  <ImportStructure src="../assets/assemblies/p2p_hopper_barrel.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

但如果两端都连接不能主动输出物品的木桶，物品就不会自动从一个木桶转移到另一个木桶：

<GameScene zoom="4" background="transparent">
  <ImportStructure src="../assets/assemblies/p2p_barrel_barrel.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

红石 P2P 隧道等其他类型也可以按类似方式传递对应信号：

<GameScene zoom="4" background="transparent">
  <ImportStructure src="../assets/assemblies/p2p_redstone.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 隧道类型与设置

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/p2p_tunnels.snbt" />
  <IsometricCamera yaw="180" pitch="90" />
</GameScene>

P2P 隧道可以按需要传输的资源切换类型，通常只需手持对应物品右键隧道：

* **ME P2P**：使用[ME 线缆](../items-blocks-machines/cables.md)设置，负责传输网络频道。
* **红石 P2P**：使用红石元件设置，负责传输红石信号。
* **物品 P2P**：使用箱子、木桶、漏斗等物品容器设置。
* **流体 P2P**：使用桶或其他流体容器设置。
* **能源 P2P**：使用支持能源的物品设置，负责传递 FE 等能源。
* **光照 P2P**：使用火把、荧石等光源设置。

需要注意：**ME P2P 不能通过另一条 ME P2P 隧道再次传输频道**；能源 P2P 的传输也可能有一定损耗，应结合[能源系统](../ae2-mechanics/energy.md)规划。

## 用 P2P 集中传输频道

ME P2P 最常见的用途，是通过少量专用线缆将大量[频道](../ae2-mechanics/channels.md)从控制器附近输送到工厂的其他区域。

例如，8 条输入 P2P 隧道理论上可以分别携带 32 个频道，总计 256 个频道。每个 P2P 设备自身又会占用一个频道。因此，通常会建立专用的[P2P 子网络](../ae2-mechanics/subnetworks.md)传递这些连接，避免占用主网的频道资源。

P2P 隧道可以直接安装在<ItemLink id="controller" />上；也可以用[智能线缆](../items-blocks-machines/cables.md#smart-cable)帮助观察连接情况。

<GameScene zoom="4" interactive={true}>
  <ImportStructure src="../assets/assemblies/p2p_compact_channels.snbt" />

  <BoxAnnotation color="#dddddd" min="1.3 1.3 6.3" max="2 2.7 6.7">
        石英纤维将主网能源提供给 P2P 子网，但不合并网络。
  </BoxAnnotation>

  <IsometricCamera yaw="225" pitch="30" />
</GameScene>

结合[量子网络桥](quantum_bridge.md)，还可以设计更大规模或跨维度的传输架构：

![P2P 隧道与量子桥组合](../assets/diagrams/p2p_quantum_network.png)

## 不能嵌套 ME P2P

ME P2P 隧道无法通过另一条 ME P2P 隧道继续传递其频道。因此不能通过反复嵌套来获得无限带宽；下图展示了嵌套连接失败的情况。

**限制只针对 ME P2P 传输 ME 频道**，其他类型的 P2P 隧道可以在合适的情况下通过 ME P2P 网络承载自身工作所需的连接。

<GameScene zoom="4" background="transparent">
  <ImportStructure src="../assets/assemblies/p2p_nesting.snbt" />
  <IsometricCamera yaw="225" pitch="30" />
</GameScene>

## 配对与连接频率

<GameScene zoom="6" background="transparent">
  <ImportStructure src="../assets/assemblies/p2p_linking_frequency.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

使用<ItemLink id="memory_card" />为两端隧道设置相同的连接频率。频率通常会以隧道背面 2×2 的颜色图案显示。

* **潜行并右键**隧道：生成或复制新的连接频率。
* **右键**目标隧道：将连接频率写入该端。

可以将一个输入端连接多个输出端；不过对于 ME P2P，输入端的频道会在多个输出端之间分配，**不会凭空复制出额外频道**。

## 合成配方

<RecipeFor id="me_p2p_tunnel" />
