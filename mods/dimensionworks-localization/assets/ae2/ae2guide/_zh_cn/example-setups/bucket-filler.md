---
navigation:
  parent: example-setups/example-setups-index.md
  title: 桶填充装置
  icon: minecraft:water_bucket
---

# 桶填充装置

另请参阅[桶排空装置](bucket-emptier.md)。

此装置使用了<ItemLink id="pattern_provider" />，因此适合接入你的[自动合成](../ae2-mechanics/autocrafting.md)系统。

有时会遇到不太方便的情况：你需要装在桶里的流体，而不是流体本身。有些机器可以帮你完成这件事（例如 Thermal Expansion 的流体转置机），但你不一定总能找到合适的模组来处理。好在原版 Minecraft 提供了一种稍微不那么方便的办法：<ItemLink id="minecraft:dispenser" />。

**通常不必这样做，因为[样板编码终端](../items-blocks-machines/terminals.md#pattern-encoding-terminal)中的流体替代功能允许你在合成配方中直接使用流体，而不是使用桶。**

<GameScene zoom="6" interactive={true}>
  <ImportStructure src="../assets/assemblies/bucket_filler.snbt" />

<BoxAnnotation color="#dddddd" min="2 1 0" max="3 2 1">
        (1) 样板供应器：设置为“有红石信号时锁定合成”，并放入相应的处理样板。

        <Row>
        ![填充样板](../assets/diagrams/water_fill_pattern_small.png)
        ![填充样板](../assets/diagrams/lava_fill_pattern_small.png)
        </Row>
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="3 1.1 0.1" max="3.2 1.9 0.9">
        (2) 接口：保持默认配置。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="3.1 1.1 0.8" max="3.9 1.9 1">
        (3) 存储总线 #1：保持默认配置。
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="4.05 1.05 0.8" max="4.95 1.95 1">
        (4) 成型面板：使用反转卡将桶加入黑名单。
        <Row><ItemImage id="minecraft:bucket" scale="2" /><ItemImage id="inverter_card" scale="2" /></Row>
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="3.2 2 1.2" max="3.8 2.2 1.8">
        (5) 输入总线：使用反转卡将桶加入黑名单。
        <Row><ItemImage id="minecraft:bucket" scale="2" /><ItemImage id="inverter_card" scale="2" /></Row>
  </BoxAnnotation>

<BoxAnnotation color="#dddddd" min="2.1 2 0.1" max="2.9 2.2 0.9">
        (6) 存储总线 #2：保持默认配置。
  </BoxAnnotation>

<DiamondAnnotation pos="0 1.5 0.5" color="#00ff00">
        接入主网络
    </DiamondAnnotation>

  <IsometricCamera yaw="225" pitch="45" />
</GameScene>

## 配置

* <ItemLink id="pattern_provider" /> (1) 设置为“有红石信号时锁定合成”，并放入相应的<ItemLink id="processing_pattern" />。

    ![充能器样板](../assets/diagrams/water_fill_pattern.png)
    ![充能器样板](../assets/diagrams/lava_fill_pattern.png)

* <ItemLink id="interface" /> (2) 保持默认配置。
* 第一个<ItemLink id="storage_bus" /> (3) 保持默认配置。
* <ItemLink id="formation_plane" /> (4) 使用反转卡将桶加入黑名单。
  <Row><ItemImage id="minecraft:bucket" scale="2" /><ItemImage id="inverter_card" scale="2" /></Row>
* <ItemLink id="import_bus" /> (5) 使用反转卡将桶加入黑名单。
  <Row><ItemImage id="minecraft:bucket" scale="2" /><ItemImage id="inverter_card" scale="2" /></Row>
* 第二个<ItemLink id="storage_bus" /> (6) 保持默认配置。

## 工作原理

1. <ItemLink id="pattern_provider" />将材料推入<ItemLink id="interface" />。（实际上，为了优化性能，它会通过存储总线和成型面板直接输出，仿佛它们是供应器面的延伸；物品并不会真正进入接口。）
2. 按照[管道子网](pipe-subnet.md#providing-to-multiple-places)中介绍的机制，桶会经由<ItemLink id="formation_plane" />进入<ItemLink id="minecraft:dispenser" />，成型面板则会放置流体。
3. <ItemLink id="minecraft:comparator" />检测到发射器中有桶，于是同时为发射器供能并锁定<ItemLink id="pattern_provider" />。
4. 发射器用桶收取流体，装满的桶留在发射器内。
5. <ItemLink id="import_bus" />从发射器中取出装满流体的桶，并通过<ItemLink id="storage_bus" />将其存回样板供应器，送回主网络。
6. 比较器检测到发射器已空，于是解除对供应器的锁定。
