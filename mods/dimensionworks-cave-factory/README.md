# DimensionWorks Cave Factory

`dimensionworks_cave_factory` 为 Alex's Caves 六个洞穴维度提供统一的后期工厂骨架。
当前版本为 `0.1.0`，首个里程碑只让渊海陷窟具备完整生存行为和实际配方。

## 已实现

- 共享 3x3x3 工厂结构：中心控制器、六个面心端口、20 个无 BlockEntity 机壳。
- 端口可用扳手循环为动能输入、物品输入/输出、流体输入/输出或停用；每套结构最多一个动能端口。
- 控制器与稳定器均可右键打开 GUI；物品输出端口会暴露主产物、残渣和满封装。
- 相位引擎每完成 8 个成功批次翻转一次，红石通电时锁定当前相位但保留加工。
- 512 RPM 饱和点、64 SU 应力、RPM Limit 的玩家上限与超速批次 API。
- 超速批次会同步放大应力成本，并在 `RecipeManager` 更换时使缓存失效。
- `cave_processing`、`fluid_stabilization` 与 `module_recipe_rule` 三种数据驱动配方。
- 250 mB 封装、60 秒离维倒计时、懒结算和来源残渣转换。
- 满封装可右键受支持的流体机器倒出；过期时返还空瓶并按原生维度生成残渣。
- 稳定化基础消耗、封装容量、相位数和超速应力倍率均可在 COMMON 配置中调整。
- 通用稳定器以及六个维度的控制器、流体、基质、残渣和模块注册。
- Create 搅拌机、压力机、动力锯、磨石与粉碎轮控制器模块宿主；破坏和潜行空手右键会弹出模块。
- 拆机、潜行空手右键和 Create contraption 捕获前都会弹出模块；在线所有者优先收到模块，否则原地掉落。
- 渊海增压/泄压配方、六种 4:1 稳定化配方、AE2 128³空间存储元件高级接点。
- JEI 信息、Jade 状态、护目镜读数、中英文资源和 Abyssal Ponder 场景。

## 当前限制

- 只有渊海控制器提供有效生存配方；其余五维内容仅进入创造栏和 JEI，并明确标注预览。
- 本版只有 `pressure_buffer` 与 `phase_converter` 可用；其余模块是后续维度里程碑的预览。
- `phase_converter` 每完成 5 次显式映射的 Create 配方，追加一份主产物。
- 不实现跨维中转端口、路由或端口号；工厂只消费标准 Forge 物品与流体能力。
- 封装只强制约束工厂、稳定器、封装容器及玩家背包路径；未知第三方储罐不作全局拦截。
- 五维签名机启用前，对应残渣没有生存来源，因此这五条自增殖备链及其任务节点保持预览。
- 机壳无 BlockEntity；控制器和端口只在数据变化时同步，空闲控制器降频检查。

## 数据入口

配方、标签和模块映射位于：

- `kubejs/data/dimensionworks_cave_factory/recipes/survival/`（渊海生存获得链）
- `kubejs/data/dimensionworks_cave_factory/recipes/cave_processing/`
- `kubejs/data/dimensionworks_cave_factory/recipes/fluid_stabilization/`
- `kubejs/data/dimensionworks_cave_factory/recipes/module_recipe_rule/`
- `kubejs/data/dimensionworks_cave_factory/tags/`
- `kubejs/server_scripts/cave_self_propagation.js`（六维基础产物自增殖主链与残渣备链，只用 Create，不需要本模组运行）
- `kubejs/data/ae2/recipes/network/cells/` 中的 128³空间存储元件覆盖配方

公开 API：

- `AmpouleExpiry`：封装离维观察、结算与堆叠可合并判定。
- `ModuleHost`：模块宿主能力、能力映射与显式配方映射入口。
- `FactoryMachineBlockEntity#phase()`：机器相位状态。
- `FluidStabilizerBlockEntity`：通用 4:1 稳定化接口。

## 构建

```sh
cd mods/dimensionworks-cave-factory
gradle test --no-daemon
gradle build --no-daemon
```

GameTest 源码已随工程编译，但本仓库的开发运行配置不携带 Alex's Caves 与洞穴维度运行依赖，因此当前不声称 GameTest、客户端或专用服已完成真实运行验证。
