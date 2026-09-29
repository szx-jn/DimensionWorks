# KubeJS

存放 DimensionWorks 的 KubeJS 脚本、配方、标签和数据。

建议结构：

- `startup_scripts/`
- `server_scripts/`
- `client_scripts/`
- `data/`
- `assets/`

## 天境装备规则

`server_scripts/aether_equipment_lock.js` 禁用天境装备的制作、修复和正常获取流程，保留流程功能物。
配套的 `defaultconfigs/aether-client.toml` 与 `defaultconfigs/aether-common.toml` 隐藏天境饰品按钮，并启用 Curios 标准菜单。

## 齿轮之心

`startup_scripts/gear_heart.js` 注册两个最高稀有度的齿轮之心和七个占位修复碎片，不包含配方。
四张齿轮之心贴图均为 32x32；所有状态都不启用附魔光效。

| 物品 | `CustomModelData` | 状态 |
| --- | --- | --- |
| `kubejs:gear_heart` | `0` | 常态外观，携带七条未破除诅咒 |
| `kubejs:gear_heart` | `1` | 破损外观，携带同样的七条未破除诅咒 |
| `kubejs:gear_heart_enchanted` | `0` | 祝福占位，不显示诅咒 |

`kubejs:gear_heart` 本身就是受诅咒物品，不需要手动设置 `CustomModelData`；物品说明会直接列出当前七条诅咒，已破除项在原位置改为绿色效果。
`kubejs:gear_heart_enchanted` 的旧 `CustomModelData=2` 诅咒物品会在装备后自动迁移为
破损态 `kubejs:gear_heart`。`kubejs:gear_heart_repair_fragment_1` 至 `_7` 为占位物品，
右键已装备的破损齿轮之心可分别破除七条诅咒，当前无配方。

`data/curios/` 新增专属 `gear_heart` 槽，只接受齿轮之心；装备后由 RPM Limit 模组拦截
手动取下并设为死亡保留。物品提供交互距离 +2、急迫 II 和幸运 I，诅咒运行效果与
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
