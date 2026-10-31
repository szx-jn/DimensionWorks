---
navigation:
  parent: example-setups/example-setups-index.md
  title: 主网络示例
  icon: controller
---

# 主网络示例

本指南中的许多自动化示例都会连接到“主网络”。如果你想知道各种[设备](../ae2-mechanics/devices.md)如何组合成一套完整系统，可以参考下面的网络布局：

<GameScene zoom="2.5" interactive={true}>
  <ImportStructure src="../assets/assemblies/treelike_network_structure.snbt" />

    <BoxAnnotation color="#dddddd" min="3.9 0 1.9" max="9.1 5 7.1" thickness="0.05">
        这里放置了大量样板供应器与分子装配室，可以处理普通合成、切石和锻造等样板。棋盘式布局让一个供应器同时连接多个装配室，节省空间；按每组 8 个设备划分也更容易控制频道数量。
    </BoxAnnotation>

    <BoxAnnotation color="#dddddd" min="3.9 0 9.9" max="5.1 3 12.1" thickness="0.05">
        部分加工机器使用[管道子网](pipe-subnet.md)，将加工结果重新送回样板供应器。
    </BoxAnnotation>

    <BoxAnnotation color="#dddddd" min="-0.1 0 8.9" max="1.1 3 13.1" thickness="0.05">
      这里放置了终端和其他常用设备。一般只需要一个合成终端，就能同时满足存取物品和普通合成的需求。
    </BoxAnnotation>

    <BoxAnnotation color="#dddddd" min="-0.1 0 -0.1" max="2.1 3 8.1" thickness="0.05">
      这里是一组自动合成 CPU。部分 CPU 具有较大存储量，其他 CPU 则用于并行处理多个小任务。实际工厂可以根据需要增加更多协处理器。
    </BoxAnnotation>

    <BoxAnnotation color="#dddddd" min="5.9 0 13.9" max="7.1 1 15.1" thickness="0.05">
      控制器最好放在基地中心附近，便于向各处布线。大型工厂可能需要更大的控制器结构。
    </BoxAnnotation>

    <BoxAnnotation color="#dddddd" min="11.9 0 7.9" max="13.1 4 13.1" thickness="0.05">
        不同的存储设备，例如 ME 驱动器和存储总线，都按每组不超过 8 个频道进行规划。
    </BoxAnnotation>

    <BoxAnnotation color="#dddddd" min="10.9 0 0.9" max="13.1 2 7.1" thickness="0.05">
        存储区可使用驱动器和存储总线组合；普通线缆分支应按每组不超过 8 个频道规划。
    </BoxAnnotation>

  <IsometricCamera yaw="315" pitch="30" />
</GameScene>
