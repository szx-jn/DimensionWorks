---
navigation:
  parent: example-setups/example-setups-index.md
  title: 存储类型与网络整理
  icon: drive
---

# 多种存储类型与网络整理

利用过滤器、[分区](../items-blocks-machines/cell_workbench.md)和[存储优先级](../ae2-mechanics/import-export-storage.md#storage-priority)，可以为不同类型的物品设置多个存储层级。

常见的存储类型包括：
* 通用存储：存放数量从几个到几千个不等的各类杂物。使用较小的[存储元件](../items-blocks-machines/storage_cells.md)，例如 4k 或 16k 元件。
* 大宗存储：存放数量超过几千个的物品，例如圆石或铁锭。使用较大的元件，例如 256k 元件或 MEGA 附属模组提供的元件。
* 农场的本地存储，详见[专用本地存储](specialized-local-storage.md)以及各种[赛特斯石英农场](simple-certus-farm.md)、[半自动农场](semiauto-certus-farm.md)和[高级农场](advanced-certus-farm.md)。

设置优先级后，物品进入主网络时会优先存入专用的大宗或本地存储；如果过滤器和分区不允许存入这些位置，才会进入通用存储。这意味着物品**不会主动**从一种存储迁移到另一种存储，而是在进入或离开网络时逐渐“迁移”。若要主动转移物品，请使用<ItemLink id="io_port" />。

<GameScene zoom="3" interactive={true}>
  <ImportStructure src="../assets/assemblies/network_storage_types.snbt" />

    <BoxAnnotation color="#33dd33" min="11 0 1" max="12 1.3 2" thickness="0.05">
        大宗存储。此处使用连接到大型存储（例如抽屉）的过滤存储总线，并将其过滤为煤炭。它的优先级较高，因此煤炭进入网络时会存入此处；从网络取出煤炭时则会从其他位置取出，使煤炭逐渐“迁移”到这个抽屉中。

        重要提示：抽屉等经过优化的大型物品栏适合此用途；巨型箱子等未经优化、格数很多的大型物品栏会在使用存储总线时严重影响性能。
    </BoxAnnotation>

    <BoxAnnotation color="#33dd33" min="11 0 3" max="12 1 4" thickness="0.05">
        大宗存储。此处在驱动器中使用分区后的 256k 元件，并设为较高优先级。该元件分区存放圆石和铁锭，并装有均匀分配卡，避免元件被圆石填满而没有空间存放铁锭。驱动器优先级较高，因此圆石或铁锭进入网络时会存入此处；从网络取出时则会从其他位置取出，使它们逐渐“迁移”到该元件中。
    </BoxAnnotation>

    <BoxAnnotation color="#33dddd" min="11 0 5" max="12 1 6" thickness="0.05">
        通用存储。此处的驱动器装满了 16k 元件，且没有进行分区。驱动器的优先级为中性（此处为 0），因此物品进入网络时会先进入专用的大宗或本地存储；物品从网络取出时则会先从这里取出，使有专用存储位置的物品自然地从通用存储中“迁移”出去。
    </BoxAnnotation>

    <BoxAnnotation color="#88ff88" min="11 0 8" max="12 1 9" thickness="0.05">
        此 IO 端口对保持网络整洁很重要。存储优先级不会主动移动物品，因此应定期通过 IO 端口“整理”通用存储所用的元件，将有专用存储位置的物品转移过去。这样可以“整理碎片”，避免物品分散存放在多个位置。
    </BoxAnnotation>

    <BoxAnnotation color="#dd3333" min="14 0 11" max="15 1 12" thickness="0.05">
        生物农场的本地存储。此驱动器中的元件已按要保留的掉落物分区，例如骨头和箭。驱动器本身不设置优先级，因为优先级由主网络访问子网的存储总线决定。元件装有均匀分配卡和溢出销毁卡。
    </BoxAnnotation>

    <BoxAnnotation color="#dd3333" min="14 1 10" max="15 2.3 11" thickness="0.05">
        生物农场的本地存储。此存储总线与接口组合，使主网络能够访问子网的存储。存储总线设为较高优先级，并过滤为子网元件中存放的物品。

        重要提示：由于子网安装了垃圾桶，请务必为此存储总线设置过滤器，否则进入网络的每一种物品、流体等都可能被销毁！
    </BoxAnnotation>

    <BoxAnnotation color="#dd3333" min="14 0 9" max="15 1.3 10" thickness="0.05">
        生物农场的本地存储。此存储总线连接到物质凝聚器，优先级低于驱动器。这意味着无法存入驱动器元件的生物掉落物会溢出到这里并被处理掉。这有助于避免子网被大多已损坏的弓等杂物塞满。
    </BoxAnnotation>

    <BoxAnnotation color="#dd33dd" min="8 1 11.7" max="9 2.3 13" thickness="0.05">
        西瓜农场的本地存储。此装置采用了与赛特斯石英农场示例类似的方法：子网上的存储总线将农场产物存入木桶；主网络上的另一个存储总线（过滤西瓜片并设为较高优先级）则让主网络能够访问这些产物。
    </BoxAnnotation>

  <IsometricCamera yaw="270" pitch="30" />
</GameScene>
