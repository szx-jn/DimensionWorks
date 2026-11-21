# DimensionWorks Cave Factory

`dimensionworks_cave_factory` 为 Alex's Caves 六个洞穴维度提供统一的后期工厂骨架。
目标基线为 Minecraft 1.21.1、NeoForge 21.1.267、Java 21、Create 6.0.10；当前模组版本仍为 `0.1.7`，首个里程碑只让渊海陷窟具备完整生存行为和实际配方。

## 已实现

- 共享 3x3x3 工厂结构：中心控制器、六个面心端口、20 个无 BlockEntity 机壳。
- 端口可用扳手循环为动能输入、物品输入/输出、流体输入/输出或停用；每套结构最多一个动能端口。
- 控制器与稳定器均可右键打开 GUI；物品输出端口会暴露主产物、残渣和满封装。
- 相位引擎每完成 8 个成功批次翻转一次，红石通电时锁定当前相位但保留加工。
- 512 RPM 饱和点、64 SU 应力、RPM Limit 的玩家上限与超速批次 API。
- 超速批次会同步放大应力成本，并在数据包重载或 `RecipeManager` 更换时使缓存失效。
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
- JEI 信息、Jade 状态及其中英文设置项、护目镜读数和 Abyssal Ponder 场景。

## 当前限制

- 只有渊海控制器提供有效生存配方；其余五维内容仅进入创造栏和 JEI，并明确标注预览。
- 本版只有 `pressure_buffer` 与 `phase_converter` 可用；其余模块是后续维度里程碑的预览。
- `phase_converter` 每完成 5 次显式映射的 Create 配方，追加一份主产物。
- 不实现跨维中转端口、路由或端口号；工厂只消费标准 NeoForge 物品与流体能力。
- 封装只强制约束工厂、稳定器、封装容器及玩家背包路径；未知第三方储罐不作全局拦截。
- 五维签名机启用前，对应残渣没有生存来源，因此这五条自增殖备链及其任务节点保持预览。
- 机壳无 BlockEntity；控制器和端口只在数据变化时同步，空闲控制器降频检查。

## 数据入口

配方、标签和模块映射位于：

- `kubejs/data/dimensionworks_cave_factory/recipe/survival/`（渊海生存获得链）
- `kubejs/data/dimensionworks_cave_factory/recipe/cave_processing/`
- `kubejs/data/dimensionworks_cave_factory/recipe/fluid_stabilization/`
- `kubejs/data/dimensionworks_cave_factory/recipe/module_recipe_rule/`
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
# 在仓库根目录，JAVA_HOME 指向 Java 21
./gradlew :dimensionworks-cave-factory:test :dimensionworks-cave-factory:build
./gradlew :dimensionworks-cave-factory:runGameTestServer
./gradlew :dimensionworks-cave-factory:runGameTestServer -PcrystalCheck
./gradlew :dimensionworks-cave-factory:runClient -PcrystalCheck
```

当前开发运行固定加载 Raguto Alex’s Caves 2.0.10、维度端口 1.1.0、Citadel Port 2.7.6。`crystalCheck` 额外加载固定 KubeJS/Rhino/Better Advanced Tooltips，并复制熔融材料脚本；GameTest 同时复制本模组 16 条原有配方。测试类、测试标签和空白 GameTest 结构不进入生产 JAR。

以下旧版本记录中的 Forge 命令和检查结果属于迁移前历史；当前目标版本结果见下节。

## 0.1.2：共享维度工坊页签

复用 `dimensionworks_cave_factory:main` 统一展示所有 `dimensionworks_*` 和 `kubejs` 专属物品，中文标题为「维度工坊」，从其他普通页签移除重复专属物品，搜索页签保持可检索。不依赖其他自研模组的 Java 类，按注册表收录新增物品。


## 0.1.3：结晶金属改为真实流体

移除 21 种结晶熔融金属固体注册、BlockItem、方块模型和固体掉落表，改由 `kubejs/startup_scripts/molten_metals.js` 注册同名流体与桶。世界中的对应 ID 由液体方块使用；流体可进入 Forge 储罐和管道，结晶转换保留源/流动状态及流量，不再生成可拾取的固体。

贴图加载器读取当前资源包中 KubeJS 原始 thick_fluid 静止/流动动画，使用原金属色并叠加现有结晶纹理（35% 不透明度）。不使用旧的非法角度立体晶簇模型。每种金属分别提供静止和流动精灵，保留原动画帧和帧时长。像素只在资源重载时合成，不添加逐 tick 绘图或世界扫描。素材沿用现有资源，不复制第三方原始素材到仓库。

本地专项检查：`gradle -PcrystalCheck -PcrystalMods="<整合包 mods 目录>" runClient`，准备任务自动复制当前 `molten_metals.js`。检查所有源流体、流体桶、储罐填充/排出、旧固体物品缺席与 42 张动画精灵；该检查源码不进入正式 JAR。

本次验证：带实际 KubeJS 流体注册的服务端通过 6 项 GameTest（含真实 30 秒外露后的液体源转换）；开发客户端检查 21 种源流体、42 张动画精灵、对应桶、储罐进出以及旧固体物品缺席。

## 暮色与 Alex 传送权限

暮色入口限制允许已持有 `dimensionworks_transfer.unlocked` 永久标记的玩家使用 TP，以及中转终端当次临时授权。未解锁玩家仍被拦截；传统暮色门由 Transfer 按传送器单独禁用。


## 0.1.7：当前发运权限与真实露天结晶

暮色林地原型的 Java 实现、注册、配方、标签、语言、贴图、生成脚本和专项测试全部删除。暮色只提供弹力打包工具与终端升级，见[现行规格](../../docs/design/twilight-dispatch-upgrade.md)。移除结晶固体到桶的缺失映射。Alex 洞穴只接受已升级中转终端的当次授权，普通洞穴钥匙与天境权限按当前规则检查。

露天计时从液体方块实际放置完成后登记，覆盖桶、流体自然流动和世界放置；替换流体重新计时，同种液体流量变化保持当前暴露计时。每 20 tick 仅检查已登记且已加载的位置，连续见天空 600 tick 后转换；遮挡重计时，不强制加载或扫描全世界。源流体保持源状态，流动液体保持流量。

工作盆的敞口结晶与压力产出由 [Pressure Crystals](../dimensionworks-pressure-crystals/README.md) 处理。当前验证使用真实放置路径，不再手动写入计时数据；服务端 9 项 GameTest 覆盖 21 种熔液、倒桶、遮挡、重新倒液和流动液体，客户端核对 21 种源流体、42 张动画精灵、流体桶及储罐进出。详细结果见[本次验证记录](../../docs/validation/2026-11-14-compat-cleanup-and-dispatch.md)。


## 1.21.1 NeoForge 移植验证

保留全部自研注册 ID、六个原有洞穴职责与 16 条配方。物品配方结果使用 `id/count/components`，流体使用 `id/amount/components`；目录改为 `recipe/`、`tags/item/` 和 `tags/fluid/`。稳定化输入 `amount` 可省略，解析时读取服务端配置，网络发送已解析的数值。

封装流体使用不可变组件 `dimensionworks_cave_factory:ampoule_fluid`；来源维度、到期时间和残渣保存在 `minecraft:custom_data`。组件保存、部分排出及过期转换保留名称和其他数据。原 Forge 标签须由整包一次性转换器迁移，当前运行代码不读取旧格式。

能力由 `CFCapabilities` 注册；`ModuleHost.find` 使用方块能力查询，五类 Create 宿主使用附件管理生命周期，通过其原有持久化键保存模块、归属和计数。配方重载后缓存立即失效。稳定器先确认整批产物可容纳，再申请额度和消耗流体。非法库存列表、槽位、物品、流体、归属 UUID 或机器相位显式拒绝；完整旧档转换仍需在开服前离线预检，不能依赖方块实体加载器阻断整座世界。

目标检查：31 项 JUnit、11 项基础 GameTest；`crystalCheck` 共 16 项 GameTest，包含 21 种实际流体结晶与 16 条原始数据配方加载。客户端专项检查覆盖 42 张动画精灵、桶、储罐和统一创造页签；不是 JEI/Jade/Ponder 全界面验收。独立新测试服加载六个原有维度，并在渊海实际生成区块、用动力端口完成两种原配方；正常保存后，新进程重启核对产物、流体、相位和归属。铁流体动画与预期合成逐像素一致。

独立服务端专项运行需要 `-PcrystalCheck -PcaveServerDir=<专用目录>`，该目录必须含内容为 `isolated-cave-check` 的 `dimensionworks-cave-check.marker`，并预先设置本地监听、内存与新世界参数。使用 `runCaveServerCheck` 执行生产检查，追加 `-PcaveReloadOnly` 执行重启检查。准备复制与启动均校验标记；禁止把此检查指向原始存档或生产目录。

维度端口新增 `alexscaves:prismatic_depths`；它不属于原定六维科技树，默认入口限制同样拦截，后续整包脚本必须同步审查。端口 JAR 声明 GPL-3.0、页面标注 LGPL 的差异已记录；没有复制其源码。现有旧档尚未被 NeoForge 打开，全部上述运行证据来自新建隔离世界，完整迁移继续按执行记录推进。
