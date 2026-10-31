---
navigation:
  parent: ae2-mechanics/ae2-mechanics-index.md
  title: 频道
  icon: controller
---

# 频道

AE2 的[ME 网络](me-network-connections.md)需要用**频道**连接多数[设备](../ae2-mechanics/devices.md)。你可以把频道理解成一台电脑上的 USB 接口：即使设备通电了，没有空闲接口也无法连接工作。

普通线缆和多数完整方块设备最多可传递 **8 个频道**；[致密线缆](../items-blocks-machines/cables.md#dense-cable)最多可传递 **32 个频道**。<ItemLink id="me_p2p_tunnel" />和[量子网络桥](../items-blocks-machines/quantum_bridge.md)也能传输更高数量的频道。

每台需要频道的设备通常占用一个频道。当线路上的频道都被占满时，后续设备即使已经接线，也可能无法正常工作。

<GameScene zoom="7" interactive={true}>
  <ImportStructure src="../assets/assemblies/channel_demonstration_1.snbt" />

  <LineAnnotation color="#33ff33" from="1 .4 .7" to="2.4 .4 .7" alwaysOnTop={true}/>
  <LineAnnotation color="#33ff33" from="1 .6 .7" to="2.4 .6 .7" alwaysOnTop={true}/>
  <LineAnnotation color="#33ff33" from="1 .4 .6" to="2.6 .4 .6" alwaysOnTop={true}/>
  <LineAnnotation color="#33ff33" from="1 .6 .6" to="2.6 .6 .6" alwaysOnTop={true}/>
  <LineAnnotation color="#33ff33" from="1 .6 .6" to="2.6 .6 .6" alwaysOnTop={true}/>

  <LineAnnotation color="#33ff33" from="2.4 .6 .7" to="2.4 .6 1.5" alwaysOnTop={true}/>
  <LineAnnotation color="#33ff33" from="2.4 .4 .7" to="2.4 .4 1.5" alwaysOnTop={true}/>
  <LineAnnotation color="#33ff33" from="2.6 .6 .6" to="2.6 .6 1.5" alwaysOnTop={true}/>
  <LineAnnotation color="#33ff33" from="2.6 .4 .6" to="2.6 .4 1.5" alwaysOnTop={true}/>

  <LineAnnotation color="#33ff33" from="2.1 .6 1.5" to="2.4 .6 1.5" alwaysOnTop={true}/>
  <LineAnnotation color="#33ff33" from="2.6 .4 1.5" to="2.9 .4 1.5" alwaysOnTop={true}/>

  <LineAnnotation color="#33ff33" from="2.6 .6 1.5" to="2.6 .9 1.5" alwaysOnTop={true}/>
  <LineAnnotation color="#33ff33" from="2.4 .1 1.5" to="2.4 .4 1.5" alwaysOnTop={true}/>

  <LineAnnotation color="#33ff33" from="1 .6 .4" to="3.5 .6 .4" alwaysOnTop={true}/>
  <LineAnnotation color="#33ff33" from="1 .4 .4" to="3.5 .4 .4" alwaysOnTop={true}/>

  <LineAnnotation color="#33ff33" from="3.5 .6 .4" to="3.5 .9 .4" alwaysOnTop={true}/>
  <LineAnnotation color="#33ff33" from="3.5 .1 .4" to="3.5 .4 .4" alwaysOnTop={true}/>

  <LineAnnotation color="#33ff33" from="1 .6 .3" to="1.5 .6 .3" alwaysOnTop={true}/>
  <LineAnnotation color="#33ff33" from="1 .4 .3" to="1.5 .4 .3" alwaysOnTop={true}/>

  <LineAnnotation color="#33ff33" from="1.5 .6 .3" to="1.5 .9 .3" alwaysOnTop={true}/>
  <LineAnnotation color="#33ff33" from="1.5 .1 .3" to="1.5 .4 .3" alwaysOnTop={true}/>

  <LineAnnotation color="#ff3333" from="3.5 .5 .5" to="5.5 .5 .5" alwaysOnTop={true}>
  这段线缆的 8 个频道已经全部占用，后续设备无法获取频道  
  </LineAnnotation>

  <LineAnnotation color="#993333" from="1 .5 .5" to="1.25 .5 .5" alwaysOnTop={true}/>
  <LineAnnotation color="#993333" from="1.5 .5 .5" to="1.75 .5 .5" alwaysOnTop={true}/>
  <LineAnnotation color="#993333" from="2 .5 .5" to="2.25 .5 .5" alwaysOnTop={true}/>
  <LineAnnotation color="#993333" from="2.5 .5 .5" to="2.75 .5 .5" alwaysOnTop={true}/>
  <LineAnnotation color="#993333" from="3 .5 .5" to="3.25 .5 .5" alwaysOnTop={true}/>

  <DiamondAnnotation pos="3.6 0.5 0.5" color="#ff0000">
        这段线缆的 8 个频道已经全部占用，后续设备无法获取频道
    </DiamondAnnotation>

  <IsometricCamera yaw="15" pitch="30" />
</GameScene>

使用[智能线缆](../items-blocks-machines/cables.md)能直接查看每段线路的频道使用量，是排查频道不足最方便的工具。

每段网络连接都会消耗少量 AE 能量。在设备和线路数量非常多时，加入<ItemLink id="controller" />并优化频道分配，有时反而能降低整个网络的能耗。

注意：**线缆的颜色不会改变频道数量**。颜色的作用是控制哪些线缆可以互相连接。

## 频道路径选择

对于有<ItemLink id="controller" />的网络，频道通常优先沿最短路径传递：先经过相邻设备与[普通线缆](../items-blocks-machines/cables.md)，再进入[致密线缆](../items-blocks-machines/cables.md)，最终连接控制器。

如果最短路径已经被占满，部分[设备](devices.md)可能拿不到频道，即使网络其他线路还有空余也是如此。可以通过不同颜色的线缆、线缆锚和 P2P 隧道明确分流。

下面的示例中，部分 ME 驱动器没有频道，并不是总容量不够，而是频道集中通过一条较短的线路，导致局部拥堵。

<GameScene zoom="4" interactive={true}>
  <ImportStructure src="../assets/assemblies/channel_path_length_issue.snbt" />

  <LineAnnotation color="#33ff33" from="3 .5 1.4" to="0.4 0.5 1.4" alwaysOnTop={true} thickness="0.05"/>
  <LineAnnotation color="#33ff33" from="0.4 .5 1.4" to="0.4 0.5 3.6" alwaysOnTop={true} thickness="0.05"/>
  <LineAnnotation color="#33ff33" from="0.4 0.5 3.6" to="1.4 0.5 3.6" alwaysOnTop={true} thickness="0.05"/>
  <LineAnnotation color="#33ff33" from="1.4 0.5 3.6" to="1.4 0.5 5" alwaysOnTop={true} thickness="0.05"/>

  <LineAnnotation color="#33ff33" from="3 0.5 3.6" to="1.6 0.5 3.6" alwaysOnTop={true} thickness="0.05"/>
  <LineAnnotation color="#33ff33" from="1.6 0.5 3.6" to="1.6 0.5 5" alwaysOnTop={true} thickness="0.05"/>

  <LineAnnotation color="#ff3333" from="3 .5 1.6" to="0.6 .5 1.6" alwaysOnTop={true} thickness="0.05"/>
  <LineAnnotation color="#ff3333" from="0.6 .5 1.6" to="0.6 .5 3.4" alwaysOnTop={true} thickness="0.05"/>
  <LineAnnotation color="#ff3333" from="0.6 .5 3.4" to="1.4 .5 3.4" alwaysOnTop={true} thickness="0.05"/>

  <LineAnnotation color="#ff3333" from="3 .5 3.4" to="1.6 .5 3.4" alwaysOnTop={true} thickness="0.05"/>

  <BoxAnnotation color="#dddddd" min="1.2 0.2 3.2" max="1.8 0.8 3.8" alwaysOnTop={true} thickness="0.05">
        超过 8 个频道试图通过这里,所以有些被切断。
  </BoxAnnotation>

  <IsometricCamera yaw="90" pitch="90" />

</GameScene>

解决办法是主动约束频道路径，避免让多条分支在同一个位置争抢带宽。通常建议采用接近**树状结构**的布线方式，减少回路和模糊的连接路径。

<GameScene zoom="4" interactive={true}>
  <ImportStructure src="../assets/assemblies/channel_path_length_issue_fix.snbt" />

  <LineAnnotation color="#33ff33" from="3 .5 1.4" to="0.4 0.5 1.4" alwaysOnTop={true} thickness="0.05"/>
  <LineAnnotation color="#33ff33" from="0.4 .5 1.4" to="0.4 0.5 5.6" alwaysOnTop={true} thickness="0.05"/>
  <LineAnnotation color="#33ff33" from="0.4 0.5 5.6" to="1 0.5 5.6" alwaysOnTop={true} thickness="0.05"/>

  <LineAnnotation color="#33ff33" from="3 0.5 3.6" to="1.6 0.5 3.6" alwaysOnTop={true} thickness="0.05"/>
  <LineAnnotation color="#33ff33" from="1.6 0.5 3.6" to="1.6 0.5 5" alwaysOnTop={true} thickness="0.05"/>

  <IsometricCamera yaw="90" pitch="90" />

</GameScene>

## 无控制器网络（临时网络）

没有<ItemLink id="controller" />的 ME 网络称为无控制器网络，默认最多只能让 **8 台需要频道的设备**工作。超过限制后，多余设备将无法分配频道。你可以删除部分设备，或者加入<ItemLink id="controller" />扩展容量。

在无控制器网络中，[智能线缆](../items-blocks-machines/cables.md)显示的是网络整体使用的频道数量，不一定是该段线路上实际流经的频道数。它与有控制器网络的最短路径分配方式不同。

## 网络布线示例

建议从控制器引出致密线缆作为主干，再分出普通线缆，每条普通分支连接不超过 8 台[设备](../ae2-mechanics/devices.md)。

下面是一个不合理的布局示例。普通线缆上的频道被沿途设备逐渐占用：

1. 频道从致密主干进入普通线缆时，上限已经降为 8。
2. 驱动器占用 1 个，剩余 7 个。
3. 两台终端各占 1 个，剩余 5 个。
4. 接口占用 1 个，剩余 4 个。
5. 样板供应器占用 1 个，剩余 3 个。
6. 输入总线占用 1 个，剩余 2 个。
7. 末端多台分子装配室只能分享最后的频道，部分设备无法工作。

问题不在于“网络里没有频道”，而是**局部线路承载量不够**。

<GameScene zoom="4" interactive={true}>
  <ImportStructure src="../assets/assemblies/bad_network_structure.snbt" />

<LineAnnotation color="#33ff33" from="6.5 .5 1.5" to="6 .5 1.5" alwaysOnTop={true} thickness="0.4">
  32 个频道
</LineAnnotation>

<LineAnnotation color="#33ff33" from="6 .5 1.5" to="5.5 .5 1.5" alwaysOnTop={true} thickness="0.2">
  8 个频道
</LineAnnotation>

<LineAnnotation color="#33ff33" from="5.5 .5 1.5" to="5.5 1.5 1.5" alwaysOnTop={true} thickness="0.1">
  2 个频道
</LineAnnotation>

<LineAnnotation color="#33ff33" from="5.5 .5 1.5" to="5.5 .3 1.5" alwaysOnTop={true} thickness="0.071">
  1 个频道
</LineAnnotation>

<LineAnnotation color="#33ff33" from="5.5 1.5 1.5" to="5.5 2.5 1.5" alwaysOnTop={true} thickness="0.071">
  1 个频道
</LineAnnotation>

<LineAnnotation color="#33ff33" from="5.5 2.5 1.5" to="5.5 2.5 1.1" alwaysOnTop={true} thickness="0.071">
  1 个频道
</LineAnnotation>

<LineAnnotation color="#33ff33" from="5.5 .5 1.5" to="4.5 .5 1.5" alwaysOnTop={true} thickness="0.158">
  5 个频道
</LineAnnotation>

<LineAnnotation color="#33ff33" from="4.5 .5 1.5" to="4.5 .3 1.5" alwaysOnTop={true} thickness="0.071">
  1 个频道
</LineAnnotation>

<LineAnnotation color="#33ff33" from="4.5 .5 1.5" to="4.5 1.5 1.5" alwaysOnTop={true} thickness="0.071">
  1 个频道
</LineAnnotation>

<LineAnnotation color="#33ff33" from="4.5 .5 1.5" to="3.5 .5 1.5" alwaysOnTop={true} thickness="0.122">
  3 个频道
</LineAnnotation>

<LineAnnotation color="#33ff33" from="3.5 .5 1.5" to="3.5 2.5 1.5" alwaysOnTop={true} thickness="0.071">
  1 个频道
</LineAnnotation>

<LineAnnotation color="#33ff33" from="3.5 2.5 1.5" to="3.7 2.5 1.5" alwaysOnTop={true} thickness="0.071">
  1 个频道
</LineAnnotation>

<LineAnnotation color="#33ff33" from="3.5 .5 1.5" to="1.5 .5 1.5" alwaysOnTop={true} thickness="0.1">
  2 个频道
</LineAnnotation>

<LineAnnotation color="#33ff33" from="1.5 0.5 1.5" to="1.5 0.3 1.5" alwaysOnTop={true} thickness="0.071">
  1 个频道
</LineAnnotation>

<LineAnnotation color="#33ff33" from="1.5 0.5 1.5" to="0.5 0.5 1.5" alwaysOnTop={true} thickness="0.071">
  1 个频道
</LineAnnotation>

<LineAnnotation color="#33ff33" from="0.5 0.5 1.5" to="0.5 0.5 0.5" alwaysOnTop={true} thickness="0.071">
  1 个频道
</LineAnnotation>

<LineAnnotation color="#ff3333" from="0.5 1.5 1.5" to="0.5 1.3 1.5" alwaysOnTop={true} thickness="0.071">
  无频道
</LineAnnotation>

<LineAnnotation color="#ff3333" from="1.5 1.5 0.5" to="1.5 1.3 0.5" alwaysOnTop={true} thickness="0.071">
  无频道
</LineAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

---

下面是更合理的树形网络布局：

<GameScene zoom="2.5" interactive={true}>
  <ImportStructure src="../assets/assemblies/treelike_network_structure.snbt" />

    <BoxAnnotation color="#dddddd" min="6.9 0 4.9" max="9.1 4 7.1" thickness="0.05">
        样板供应器按频道限制分成多组，每组不超过 8 台设备。
    </BoxAnnotation>

    <BoxAnnotation color="#dddddd" min="5 4 4" max="8 5 5" thickness="0.05">
        普通线缆无法承载全部频道，应改用致密线缆作为主干。
    </BoxAnnotation>

    <BoxAnnotation color="#dddddd" min="5 0 13" max="8 1 14" thickness="0.05">
        使用不同的电缆颜色防止相邻电缆连接。
    </BoxAnnotation>


  <IsometricCamera yaw="315" pitch="30" />
</GameScene>

## 频道模式

AE2 从 Minecraft 1.18 对应版本开始，提供可配置的频道模式。服务器管理员可以通过配置项 `channels`，或者使用 `/ae2 channelmode <mode>` 修改频道模式。单独执行 `/ae2 channelmode` 可以查看当前模式。修改后，现有 ME 网络会重新计算频道。

不同模式适合不同规模的整合包，下面列出可用配置：

| 模式 | 说明 |
| :--- | :--- |
| `default` | 默认限制：普通线缆 8 频道、致密线缆 32 频道，无控制器网络最多 8 频道。 |
| `x2` | 频道容量翻倍：普通线缆 16、致密线缆 64、无控制器网络 16。 |
| `x3` | 频道容量变为三倍：普通线缆 24、致密线缆 96、无控制器网络 24。 |
| `x4` | 频道容量变为四倍：普通线缆 32、致密线缆 128、无控制器网络 32。 |
| `infinite` | 取消频道数量限制，智能线缆仅显示有无频道通过。 |

实际使用哪一种模式，取决于整合包当前的服务端配置；不能仅凭此页推断维度工坊已启用无限频道。
