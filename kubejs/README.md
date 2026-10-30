# KubeJS

存放 DimensionWorks 的 KubeJS 脚本、配方、标签和数据。

建议结构：

- `startup_scripts/`
- `server_scripts/`
- `client_scripts/`
- `data/`
- `assets/`

## ME 动力路线配方

`server_scripts/me_memory.js` 提供 20 种正常卡配方、16 种四卡逐级升级配方和五代驱动器配方。DDR1 使用工程处理器与强化合金；后续驱动器用一台前代驱动器、四个对应电路和四个合金升级。升级回调保留原驱动器的内存卡 NBT 并删除旧 `StoredSu`；保留的旧代卡须另行升级至同代才生效。普通 DDR5 卡不再使用反物质。

`data/dimensionworks/recipes/advanced_speed_controller.json` 保留 5×5 动力合成图案，将原四个粉碎轮和四个电子管分别换为四个工程处理器、四个强化合金，使两条路线共用中后期工业材料。实时 SU/RPM 规则见 [`../docs/design/me-live-stress-network.md`](../docs/design/me-live-stress-network.md)。

## 天境装备规则

`server_scripts/aether_equipment_lock.js` 禁用天境装备的制作、修复和正常获取流程，保留流程功能物。
配套的 `defaultconfigs/aether-client.toml` 与 `defaultconfigs/aether-common.toml` 隐藏天境饰品按钮，并启用 Curios 标准菜单。

## 齿轮之心

`startup_scripts/gear_heart.js` 注册两个最高稀有度的齿轮之心和七个独立贴图的修复碎片；`server_scripts/gear_heart_recipes.js` 提供七个固定 ID 配方。
四张齿轮之心贴图均为 32x32；所有状态都不启用附魔光效。

| 物品 | `CustomModelData` | 状态 |
| --- | --- | --- |
| `kubejs:gear_heart` | `0` | 常态外观，携带七条未修复工业瓶颈 |
| `kubejs:gear_heart` | `1` | 破损外观，携带同样的七条未修复工业瓶颈 |
| `kubejs:gear_heart_enchanted` | `0` | 全链释限，不显示工业瓶颈 |

`kubejs:gear_heart` 本身就是带工业瓶颈的物品，不需要手动设置 `CustomModelData`；物品说明会直接列出当前七条工业瓶颈，已破除项在原位置改为绿色效果。
`kubejs:gear_heart_enchanted` 的旧 `CustomModelData=2` 受损物品会在装备后自动迁移为
破损态 `kubejs:gear_heart`。`kubejs:gear_heart_repair_fragment_1` 至 `_7` 对应七个固定配方，
右键已装备的破损齿轮之心必须按 1 到 7 的顺序破除七条工业瓶颈。

`data/curios/` 新增专属 `gear_heart` 槽，只接受齿轮之心；装备后由 RPM Limit 模组拦截
手动取下并设为死亡保留。物品提供交互距离 +2、急迫 II 和幸运 I，工业额度运行效果与
破除位写入同一模组，详见 `mods/dimensionworks-rpm-limit/README.md`。

贴图生成流程见 `scripts/README.md`。

## 洞穴维度工厂数据

`data/dimensionworks_cave_factory/recipes/survival/` 提供渊海端生存获得链；`recipes/cave_processing/`、`recipes/fluid_stabilization/` 和 `recipes/module_recipe_rule/` 分别承载签名机加工、稳定化与机制模块映射。
`data/ae2/recipes/network/cells/` 把渊压基质接入 128³ 空间存储元件配方。

## 洞穴基础产物自增殖

`server_scripts/cave_self_propagation.js` 为六维共 18 项基础产物各提供一条 Create 主链和一条残渣备链，不依赖洞穴工厂签名机。

- 主链：`dimensionworks_cave_factory:self_propagation/<dimension>/<target>_primary`
- 备链：`dimensionworks_cave_factory:self_propagation/<dimension>/<target>_recovery`
- 中间物：`dimensionworks_cave_factory:self_propagation/support/<dimension>/<name>`

Create 的 Mixer、Press、Crushing Wheels、Saw、Deployer、Spout、Fan 等加工配方每个条目只消耗一个物品，所以 `"2x item"` 的写法会由脚本中的 `expandIngredients` 展开成重复条目。调整配比时只需改这个前缀。

目标组标签位于 `data/dimensionworks_cave_factory/tags/items/self_propagation/` 与同结构的 `tags/fluids/self_propagation/`，供任务、JEI 说明与后续模块映射引用。

## 基础矿物与常规金属三路线

server_scripts/basic_mineral_propagation.js 为 16 种目标资源配置三条不同路线：Create 固体压制、Mekanism 高端合成、Create 加热残渣回收。压制线产出通用矿物残渣；每条路线均要求对应目标的有限样本作为输入，回收线还要消耗残渣、辅料、流体与热量。

完整目标清单、配方成本和游戏内验收步骤见 ../docs/design/basic-mineral-production-lines.md。

## 烈焰蛋糕红石成本

`server_scripts/blaze_cake_redstone_cost.js` 统一替换所有产出 `create:blaze_cake_base` 的配方，要求鸡蛋、糖、灰烬粉（Create Cinder Flour）和 1 份红石粉经机械压制合成底料。原版将底料注入 250 mB 熔岩制成烈焰蛋糕的配方保留不变；这样附属模组若提供其他底料配方，也不会绕过红石成本。

## 熔融金属与特殊材料

`startup_scripts/molten_metals.js` 通过可复用的 `MoltenMetalFluidDefinition` 注册 21 种熔融金属、合金与特殊材料流体，使用 `dimensionworks:` 命名空间并提供中英文名称。该脚本只注册流体，不自动添加熔炼、浇注或材料加工配方；这些配方应在后续服务端脚本中按各机器的实际能力分别实现。

`server_scripts/molten_metal_recipes.js` 将对应锭熔成 90 mB 流体，并定义黄铜、青铜熔融混合配方。21 种熔融流体的冷却配方位于 `data/create_dragons_plus/recipes/freezing/`：使用 Create: Connected 冷冻触媒和 Create: Dragons Plus 鼓风冷冻类型处理熔融流体桶，输出空桶与 11 个对应锭。桶容量为 1,000 mB，按 90 mB/锭折算输出 990 mB 等价锭材，余 10 mB 为桶装比例误差。

## 高中化学核心气体

- `startup_scripts/chemistry_gases.js` 注册 Mekanism 原版未提供的核心化学品气体，使用独立的 `dimensionworks:` 命名空间，避免覆盖 Mekanism 原版气体。
- `server_scripts/chemistry_gas_recipes.js` 添加对应的 Mekanism 化学氧化室、化学灌注器、加压反应室与太阳中子活化器配方。
- 当前新增：二氧化碳、一氧化碳、氮气、氨气、一氧化氮、二氧化氮、一氧化二氮、硫化氢、甲烷、乙炔、臭氧、硝酸；氢气、氧气、氯气、二氧化硫、三氧化硫、氯化氢、乙烯等复用 Mekanism 原生化学品。
- 这些 Mekanism 气体是游戏内化学品储罐类型；注册为 gas 不代表该物质在常温常压下必然是气态。颜色用于区分，不代表真实颜色。
- 获取方式为整合包生产线的简化模型，并非每条配方都逐原子复刻实验室制备；继续扩充时应优先保持化学反应方向、元素守恒和工业流程合理性。


## Create 工作盆 Mekanism 气体输出

- 工作盆内置 10,000 mB 单气体缓冲，可由 Mekanism 气体管道抽取。
- Create 配方可将内部物品 `dimensionworks_mek_stress:gas_output` 作为唯一产物，并在物品 NBT 指定 `Gas`（气体注册 ID）和 `Amount`（mB 数量）；标记会转为真正的 Mekanism 气体，不会作为物品掉落。
- `server_scripts/chemistry_gas_recipes.js` 提供煤经烈焰人燃烧室加热生成 1,000 mB 二氧化碳的示例。
- 工作盆上方可见天空时，每 20 游戏刻随机损失 100–200 mB；封闭工作盆不损失。该规则只作用于工作盆产出的气体，不影响液体，也不允许管道向工作盆灌入气体。

其他气体配方可复用同一输出物品，将 `Gas` 改为对应注册 ID、`Amount` 改为 mB 数量。工作盆缓冲同一时间只存一种气体；切换气体前先用 Mekanism 管道抽空。

## Create 烈焰人燃烧室柴油动力燃料

`data/dimensionworks/blaze_burner_fuel/` 为 Create: Liquid Fuel 注册 Create: Diesel Generators 的四种有效发动机燃料：柴油、生物柴油、汽油和乙醇。柴油、生物柴油与乙醇提供普通加热；汽油提供超级加热。

燃料定义使用 Create: Liquid Fuel 2.2.0 的 `fluid`、`burnTime`、`superHeat` 和 `amountConsumedPerTick` 字段。当前平衡值分别为柴油 32、生物柴油 24、汽油 24、乙醇 16，均每 tick 消耗 1 mB；这些是整合包自定义燃烧参数，不是柴油动力引擎 SU 参数的直接换算。

原油与植物油未加入：当前柴油动力版本没有为它们注册对应的发动机 FuelType，避免将原料油误作可直接驱动发动机的成品燃料。这些数据只影响 Create 烈焰人燃烧室，不会改变柴油动力引擎自身的燃料倍率或燃烧器规则。
