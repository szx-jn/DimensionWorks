# 统一粉碎工艺 API

## 目标

把所有“把输入物品磨碎并产生产物”的配方注册统一到 **global.DimensionWorksCrushing.register(event, recipe)**。调用方只声明机器类型、输入与产物分布；底层适配器负责转换为各模组的真实配方。

## 当前适配器

| machine | 实际机器 | 输出能力 |
|---|---|---|
| create:crushing | Create 粉碎轮 | 多产物、独立概率产物 |
| create:milling | Create 磨石 | 多产物、独立概率产物 |
| mekanism:crushing | Mekanism 粉碎机 | 单个固定产物 |

接口允许通过 registerAdapter(name, adapter) 扩展其他模组机器。不能把每个模组的真实配方能力假设为相同：例如当前 Mekanism KubeJS 粉碎配方适配器只接受一个确定产物；传入概率或多个产物会直接报错，防止静默丢失设定。

## 调用示例

    global.DimensionWorksCrushing.register(event, {
      id: 'dimensionworks_cave_factory:processing/example_dust',
      machine: 'create:milling',
      input: 'minecraft:raw_iron',
      outputs: [
        { item: 'mekanism:dust_iron', count: 2 },
        { item: 'mekanism:dust_iron', count: 1, chance: 0.7 }
      ]
    })

chance 是每个产物独立判定的概率，不是互斥结果表。示例表示必定得到 2 个铁粉，并有 70% 概率额外得到 1 个，因此总结果是 2 个（30%）或 3 个（70%）。

## 铁矿产线接入

1. Create 磨石：粗铁 ×1 → 铁粉 ×2，另有 70% 概率额外产出铁粉 ×1。
2. Create 机械压制：粗铁 ×1 + 安山合金 ×2 → 铁质活化料 ×1。
3. Mekanism 结合器：铁质活化料 ×1 + 铁粉 ×1 → 粗铁 ×2。

选择磨石而不是粉碎轮，是因为 Create 原版已经有粗铁的粉碎轮配方；另外注册相同输入的粉碎轮配方会制造配方冲突风险。铁质活化料为 KubeJS 注册的中间物品，需要完整重启客户端/服务端以加载新物品注册。

## 静态验收边界

静态测试负责检查 API 分派、配方能力约束、概率配置、物品注册与 JavaScript 语法。它不能代替游戏内检查；发布前仍需检查 JEI 配方显示、磨石概率输出、结合器两项输入和自动化运行。
