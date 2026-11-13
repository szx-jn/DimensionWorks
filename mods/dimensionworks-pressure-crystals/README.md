# DimensionWorks Pressure Crystals

为 Create 工作盆增加基于 Vintage Improvements 压缩机转速的金属结晶产出。

## 结算规则

- 压缩机处于**加压模式**并有转速时，按转速产出低压、中压或高压奇妙结晶。
- 每 200 tick（10 秒）检查一次当前转速，不要求整个周期持续保持该转速。
- 没有压缩机、压缩机停转或处于减压模式时，产出「看起来一点用也没有的结晶」，不带金属来源特质。
- 每次结算消耗 600 mB 结晶熔融金属，产出 3 个对应压力等级的奇妙结晶；剩余流体转换为对应的普通熔融金属。
- 工作盆无法接收全部产物时，本次不消耗流体。
- 输入流体槽逐个检查，支持工作盆的两个输入流体槽。

| 压缩机转速 | 压力等级 | 产物 |
| --- | --- | --- |
| 无压缩机、0 RPM 或减压模式 | 无 | 看起来一点用也没有的结晶 |
| 0 < RPM ≤ 512 | 低压 | 低压奇妙结晶 |
| 512 < RPM ≤ 2048 | 中压 | 中压奇妙结晶 |
| RPM > 2048 | 高压 | 高压奇妙结晶 |

结晶熔融流体由 `dimensionworks_cave_factory` 提供，普通熔融流体使用 `dimensionworks` 命名空间。物品自动进入整合包统一的「维度工坊」创造栏，不创建独立页签。

## 构建与测试

```sh
gradle test --no-daemon
gradle build --no-daemon
```

压力边界由 `PressureTierTest` 覆盖；完整构建还会检查 Forge、Create 与 Mixin 编译配置。

专项服务器回归使用本地整合包中固定版本的依赖和仓库的 `molten_metals.js`，在忽略的 `run-gametest-pressure/` 临时世界运行，测试源码不会进入正式 JAR：

```sh
gradle -PpressureCheck -PpressureMods='<整合包 mods 目录>' test build runGameTestServer --offline --no-daemon
```

覆盖空闲工作盆的无压力结晶、停转与减压、三档奇妙结晶、第二输入槽、来源特质、600 mB 扣料与剩余流体还原，以及输出满仓不扣料。测试直接设置压缩机转速；玩家搭建传动网络、客户端显示及长期产线运行仍需在完整整合包中验收。


## 金属来源特质

每次结晶产出的低压、中压、高压结晶都会写入 `DimensionWorks.Metal` 字符串 NBT，值为来源金属 ID（例如 `netherite`、`azure_neodymium`）。相同压力等级、不同金属来源的结晶因此不会堆叠为同一物品栈；KubeJS 下界合金碎片序列组装使用该字段进行部分 NBT 匹配，禁止混用不同来源。

## 0.1.1：空闲工作盆正常析出

结晶检查覆盖工作盆 tick 的所有返回路径。Create 在内容没有变化时会提前返回，旧的 `TAIL` 注入只覆盖最后一处返回，导致一次装料后几乎不再结算。现在空闲工作盆也会每 200 tick 检查一次，四种结晶使用同一套析出与空间检查规则。

检查仍只在已加载的工作盆上运行，每 200 tick 查看两个输入槽及上方两格的压缩机；不增加世界扫描、强制区块加载或周期掉落实体。使用漏斗/管道集中收料与循环普通熔融金属即可连续生产。

本次本地验证：4 项 JUnit 与 7 项服务端 GameTest 通过；GameTest 使用实际 Create、Vintage Improvements、RPM Limit 和 KubeJS 流体注册。客户端显示、玩家搭建传动网络与长期自动化尚未验证。
