# DimensionWorks Cave Factory

`dimensionworks_cave_factory` 为 Alex's Caves 六个洞穴维度提供统一的后期工厂骨架。
当前版本为 `0.1.1`，首个里程碑只让渊海陷窟具备完整生存行为和实际配方。

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
- COMMON 配置的 `dimension_access.blockAetherAndAlexCaves` 默认开启，阻止玩家进入天境及 Alex's Caves 六个附加洞穴维度；KubeJS 同时移除六种洞穴维度钥匙的合成并阻止使用。
- `dimension_access.blockTwilightForest` 默认开启，阻止玩家传送进入暮色森林；暮色森林 COMMON 配置同时禁用新传送门创建。离开暮色森林不受此限制。`dw:transfer` 本人发运终端通过服务端调用期间的授权标记进入暮色森林；该例外不会开放传统传送门。
- 通用稳定器以及六个维度的控制器、流体、基质、残渣和模块注册。
- `dimensionworks:molten_*` 熔融金属流体连续露天暴露 30 秒后变为结晶熔融金属流体；保留对应金属色的熔融底纹，并叠加 35% 不透明度的紫色晶体贴图。遮挡时重置本轮计时，计时状态按维度保存。
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

流体稳定器在输出空间检查后、消耗流体前申请 `gear_heart:final_production` 额度；额度不足时整批不开始，不产生部分流体扣除。该接口使用仅暴露 `ItemStack` 的 `FinalProductionGate#tryConsumeItem`，不会把 Mekanism 类型泄漏到 Cave Factory 的编译边界。

## 构建

```sh
cd mods/dimensionworks-cave-factory
gradle test --no-daemon
gradle build --no-daemon
```

默认开发运行不携带全部洞穴维度依赖；专项检查通过显式 profile 提供运行依赖，实际结果见下方版本记录。

## 0.1.2：共享维度工坊页签

复用 `dimensionworks_cave_factory:main` 统一展示所有 `dimensionworks_*` 和 `kubejs` 专属物品，中文标题为「维度工坊」，从其他普通页签移除重复专属物品，搜索页签保持可检索。不依赖其他自研模组的 Java 类，按注册表收录新增物品。


## 0.1.3：结晶金属改为真实流体

移除 21 种结晶熔融金属固体注册、BlockItem、方块模型和固体掉落表，改由 `kubejs/startup_scripts/molten_metals.js` 注册同名流体与桶。世界中的对应 ID 由液体方块使用；流体可进入 Forge 储罐和管道，结晶转换保留源/流动状态及流量，不再生成可拾取的固体。

贴图加载器读取当前资源包中 KubeJS 原始 thick_fluid 静止/流动动画，使用原金属色并叠加现有结晶纹理（35% 不透明度）。不使用旧的非法角度立体晶簇模型。每种金属分别提供静止和流动精灵，保留原动画帧和帧时长。像素只在资源重载时合成，不添加逐 tick 绘图或世界扫描。素材沿用现有资源，不复制第三方原始素材到仓库。

本地专项检查：`gradle -PcrystalCheck runClient`，将随包 `molten_metals.js` 放入 `run-crystal-check/kubejs/startup_scripts/`。检查所有源流体、流体桶、储罐填充/排出、旧固体物品缺席与 42 张动画精灵；该检查源码不进入正式 JAR。

旧世界中的同名方块 ID 对应新的液体方块；Forge 缺失物品映射将旧结晶固体物品 ID 转向对应流体桶，避免直接丢失旧库存。

本次验证：带实际 KubeJS 流体注册的服务端通过 6 项 GameTest（含真实 30 秒外露后的液体源转换）；开发客户端检查 21 种源流体、42 张动画精灵、对应桶、储罐进出以及旧固体物品缺席。

## 暮色传送兼容

暮色入口限制允许已持有 `dimensionworks_transfer.unlocked` 永久标记的玩家使用 TP，以及中转终端当次临时授权。未解锁玩家仍被拦截；传统暮色门由 Transfer 按传送器单独禁用。


## 0.1.6：暮色林地生产

新增林地浸出槽、生物调理槽、林地树脂、根系纤维、熟化腐殖料、铁木框架和林地滤芯；KubeJS 注册 `dimensionworks:forest_extract` 林地原液。全部收录到已有「维度工坊」页签。主世界机器与材料可支援建设，新品只供暮色工艺。

- 浸出槽：只在暮色工作，下方同一层完整 3×3 接触床（缠根泥土、根、活根方块可混用）。每批消耗 8 暮色树叶、1 腐殖料和 1,000 mB 水，产 1,000 mB 原液。
- 生物槽：只在暮色工作，附近需要 2 只成年野猪，每批消耗 8 树叶、1 胡萝卜/土豆/甜菜根、250 mB 水，产 4 腐殖料。每只野猪独占一个工位，完工休息 200 个加载刻。打包不工作，休息与工位记录保存在实体 ForgeData。
- 动力：接水平轴，最低 32 RPM，128 RPM 时每批 10 秒；更高 RPM 不加快工作。每台消耗 4 SU/RPM，沿用现有 RPM/应力网络和成品生产额度。
- 接口：上方输入两类物品，下方输出产物，侧面接水；水罐与原液罐各 10 B。水桶灌水、空桶取原液，空手右键打开界面。缺料、满仓、断动力、红石暂停等有可见原因且不扣输入。
- 原液在加热工作盆分出 2 树脂和 2 纤维；消耗一个滤芯的精制路线分出 4 树脂和 2 纤维。框架、滤芯用现有压块工艺；启动腐殖料有手工与植物混合路线。
- 库存与液体随实际掉落的设施物品保存；便携物品不携带未完成进度。环境每秒查 9 个局部方块，动物每秒局部查询；不强制加载、不结算离线产量、不产生周期掉落实体。

配方与标签位于 `kubejs/data/dimensionworks_cave_factory/recipes/twilight/` 和相应 tags。`twilight_work` JSON 的 `station` 为 `leaching` 或 `conditioning`；`first`、`second` 各含 `ingredient` 与 `count`，`water` 为 mB，`duration` 为 128 RPM 下的工作刻。前者使用 `fluid_result`，后者使用 `result`。输入槽种类分别由树叶、饲料标签与腐殖料物品限定。JEI 显示现场配方与环境条件。

本地验证入口：

```sh
python3 scripts/check_twilight_industry.py
node --check kubejs/startup_scripts/twilight_industry.js
cd mods/dimensionworks-cave-factory
gradle test build --offline --no-daemon
gradle -PtwilightCheck -PtwilightMods='<整合包 mods 目录>' runGameTestServer --offline --no-daemon
```

专项 GameTest 使用真实暮色、KubeJS 与 Create，在忽略的 `run-twilight-check/` 临时世界中验证。测试夹具会加载测试区块，生产实现不会。测试源码不进入正式 JAR。图像由 `scripts/draw_twilight_assets.py` 的标准库像素绘图生成，无生图调用。

本次构建、31 项 JUnit、16 项服务器 GameTest 与静态预检通过。客户端显示、实际 CMP 打包操作、完整自动林场和长期平衡尚未验证，详见 [验证记录](../../docs/validation/2026-11-07-twilight-first-production.md)。
