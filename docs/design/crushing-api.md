# 统一粉碎 / 研磨工艺 API

## 调用入口

统一入口为 global.DimensionWorksGrinding.register(event, recipe)，旧名 global.DimensionWorksCrushing 仍指向同一个 API，以兼容既有脚本。

一个调用需指定 id、machine、单个物品 ID 或 #tag 输入，以及输出列表。每个输出可指定物品、数量和概率。概率是每个输出项独立判定的概率；例如固定产出 2 个粉末，另有 70% 概率额外产出 1 个粉末，即总量 2 个的概率 30%、3 个的概率 70%。

## 适配器列表

| machine | 实际加工设备 | 当前支持范围 |
|---|---|---|
| create:crushing | Create 粉碎轮 | 多物品输出、概率输出 |
| create:milling | Create 磨石 | 多物品输出、概率输出 |
| mekanism:crushing | Mekanism 粉碎机 | 单个确定物品输出 |
| create_enchantment_industry:grinding | Create: Enchantment Industry 机械磨石 | 最多 4 个物品输出；可选单个流体输入或单个流体输出 |

Mekanism 粉碎适配器遇到概率或多物品输出时会直接报错，而不是悄悄忽略设置。CEI Grinding 使用其 Minecraft 1.20.1 配方类型的标准 Create processing JSON；其流体输入放在 ingredients 中（fluid + amount），流体产物放在 results 中（fluid + amount），并且不允许同时指定流体输入和输出。CEI 的 disenchanting 是附魔拆解/经验转换，不属于物料粉碎，不由该适配器注册。

AE2 15.4.10 的目标版本源码没有独立 Grinder 配方类型/机器实现，不能拿旧版 Quartz Grindstone 文档套用到当前整合包，详见 docs/design/crushing-api-mod-audit.md。

## 示例：Create 磨石概率产物

    global.DimensionWorksGrinding.register(event, {
      id: 'dimensionworks_cave_factory:processing/example_dust',
      machine: 'create:milling',
      input: 'minecraft:raw_iron',
      outputs: [
        { item: 'mekanism:dust_iron', count: 2 },
        { item: 'mekanism:dust_iron', count: 1, chance: 0.7 }
      ]
    })

## 示例：CEI 机械磨石的流体产物

    global.DimensionWorksGrinding.register(event, {
      id: 'dimensionworks_cave_factory:processing/example_fluid',
      machine: 'create_enchantment_industry:grinding',
      input: 'minecraft:glass_bottle',
      fluidOutput: { fluid: 'create_enchantment_industry:experience', amount: 100 }
    })

## 扩展新机器

    global.DimensionWorksGrinding.registerAdapter('some_mod:grinding', {
      register: function (event, recipe) {
        // Convert the shared recipe description into the real mod's recipe.
        // Validate/throw for capabilities that this adapter cannot express.
        return event.custom({ type: 'some_mod:grinding', /* actual fields */ }).id(recipe.id)
      }
    })

适配器必须使用所选模组真实支持的 recipe type 和字段；不能仅因名称里有 crusher/grinder 就假定输入、输出、概率或流体能力一致。

## 当前接入点与验证边界

- 铁矿粉碎配方通过 Create 磨石注册，避开 Create 原版粗铁的粉碎轮配方。
- 洞穴维度脚本中的 Create 粉碎铀碎片与磨制薄荷粉也迁移到共享入口。
- Node 语法检查和 Python 静态测试由 .github/workflows/validate-version-gate.yml 执行。
- 仍需在实际游戏中检查 CEI 流体配方解码、JEI 展示、机器输入输出与自动化；仓库静态测试不能替代游戏内验证。
