---
navigation:
  parent: example-setups/example-setups-index.md
  title: 快速清空与填充存储元件
  icon: io_port
---

# 快速清空与填充存储元件

想把一张[存储元件](../items-blocks-machines/storage_cells.md)中的物品快速转移到箱子、抽屉或背包，或者反过来用外部存储填满元件，可以结合<ItemLink id="io_port" />与专门的子网络。

关键是让子网只有指定的存储目标，这样输入输出端口就不会把物品送往其他地方。

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/cell_dumper_filler.snbt" />

<BoxAnnotation color="#dddddd" min="1 1 0" max="2 2 1">
        （1）ME 输入输出端口：使用界面中间的箭头切换“转入网络”或“写入存储元件”，并安装三张加速卡。
        <ItemImage id="speed_card" scale="2" />
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="0 0.7 0" max="1 1 1">
        （2）存储总线：保持默认配置。
  </BoxAnnotation>

<BoxAnnotation color="#33dd33" min="0 1 0" max="1 2 1">
        在此放置需要填充或清空的容器。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="2 0.35 0.35" max="2.3 0.65 0.65">
        石英纤维：若使用另一个网络供电，就需要通过它共享能源，同时保持网络独立。
  </BoxAnnotation>

<DiamondAnnotation pos="3 0.5 0.5" color="#00ff00">
        连接能源，例如另一个 ME 网络或能源接收器。
    </DiamondAnnotation>

  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 配置

* <ItemLink id="io_port" />（1）：点击界面中间的箭头，切换“从元件转入网络”与“从网络写入元件”；安装 3 张加速卡可达到较高传输速度。
* <ItemLink id="storage_bus" />（2）：保持默认配置，贴在目标存储容器上。

## 清空存储元件

1. <ItemLink id="io_port" />把插入的[存储元件](../items-blocks-machines/storage_cells.md)中的资源转移到子网的[网络存储](../ae2-mechanics/import-export-storage.md)。
2. 子网唯一的存储入口是<ItemLink id="storage_bus" />，所以物品和流体都会进入它指向的外部容器。
3. <ItemLink id="energy_cell" />提供足够的[能源](../ae2-mechanics/energy.md)缓冲，避免一次性转移大量资源时供电不足。

## 填充存储元件

1. 把<ItemLink id="io_port" />切换为向元件写入的模式。
2. 子网通过<ItemLink id="storage_bus" />访问外部容器中的物品或流体，并将其写入存储元件。
3. 同样建议使用<ItemLink id="energy_cell" />提供能源缓冲，防止大规模转移时出现供电波动。

