# 全方块纸箱打包验证

日期：2026-11-08（北京时间）。

## 行为与实现

Create Mobile Packages fork `4e435c2` / `0.6.1-dimensionworks.15` 在已有纸箱来源标记补丁上扩展 Mekanism 的打包规则。普通纸箱和打包工具均允许所有已注册非空气方块，仅排除 `minecraft:barrier` 与 `dimensionworks_transfer:dispatch_terminal`。硬度、方块标签黑名单、方块实体类型黑名单及模组命名空间黑名单均不再排除方块类型；世界/领地交互、破坏事件和机器安全权限继续检查。

每次仅保存点击位置的方块状态与完整方块实体 NBT，不扫描整个结构。替换和恢复方块时使用 `UPDATE_ALL | UPDATE_KNOWN_SHAPE`，保留邻居通知并跳过会删除门、床和高植物其他部分的形状更新。拆包保留正常的邻居状态调整，但若调整结果为空气则保留存储的原方块；分体结构可逐块搬运、重新组装，随后遵守方块自身的支撑与运行规则。仅在交互时执行，不新增实体、区块票据或持续扫描。

工具来源标记、普通纸箱的空箱返还，以及中转维度原有 AE2 解包限制均保留。

## 本地验证

- 两个仓库的 `git diff --check`、Mixin JSON/类引用、版本与变更记录一致性检查通过。
- CMP：Java 17 / Gradle 8.8，`./gradlew test build` 成功，6 项 JUnit 测试通过；本地产物仅用于开发验证。
- Transfer：18 项已有 JUnit 测试通过，`gradle jar` 编译及重混淆成功。检查发行 JAR，确认未包含 GameTest 类、结构模板或人工黑名单标签。
- Forge 1.20.1 / 47.4.16 独立 GameTest 世界：使用实际 Mekanism 10.4.16.80、Create 6.0.8、AE2 15.4.10、Transfer 0.3.8 与新 CMP 开发 JAR，6 项必需集成测试全部通过。

集成测试包括：

1. 遍历实际加载的非空气方块注册项，类型策略恰好只排除屏障与发运终端。
2. 两种入口各执行 16 类代表方块的打包与拆包：石头、基岩、末地传送门框架、末地/下界传送门、命令方块、结构方块、拼图方块、刷怪笼、活塞头、水、箱子、AE2 控制器/线缆宿主、Mekanism 终极箱柜和 Create 铁轨；保留方块实体测试数据及对应空箱返还行为。
3. 人工给石头和箱子方块实体添加真实 Mekanism 黑名单所引用的标签，并设置四个命名空间黑名单，确认不再因这些规则拒绝打包。人工标签只位于 `src/gameTest/resources`。
4. 门、床和向日葵的两部分分别打包、按顺序拆包，保留相邻部分、方向与上下/头尾状态。
5. 带七颗钻石的箱子经过方块实体保存/读取、真实掉落计算、搬运和重新放置后拆包，完整载荷、物品数与来源标记均保留。
6. 两种入口均不能打包屏障与发运终端；取消破坏事件后，箱子、七颗钻石、完整 NBT 和打包物品均保留。复用已有跨维度实物测试，确认 AE2 纸箱搬到中转维度后仍不能拆包，载荷与来源标记不变。

复验命令：在 `mods/dimensionworks-transfer` 中使用 Java 17 / Gradle 8.8，执行 `gradle -PtransferTestModsDir=<固定版本模组目录> -PtransferGameTestNamespaces=dimensionworks_cardboard test runGameTestServer --no-daemon`。

## 验证边界

上述为编译、单元测试与专用服务器 API 集成测试，没有执行真实客户端交互检查，也没有穷举所有第三方多方块结构的搬运与重组。允许打包方块类型不代表自动搬运整个机器阵列、维持外部网络连接或绕过权限。正式产物由 GitHub Actions 构建与发布，整合包引用固定的 CMP GitHub Release 资产。

CMP 正式发行的 [GitHub Actions 42427343824](https://github.com/szx-jn/Create-Mobile-Packages/actions/runs/42427343824) 构建与发布均成功；已核对 [0.6.1-dimensionworks.15 固定 JAR](https://github.com/szx-jn/Create-Mobile-Packages/releases/download/1.20.1-0.6.1-dimensionworks.15/create_mobile_packages-1.20.1-0.6.1-dimensionworks.15.jar)，GitHub 资产 SHA-256 为 `f71eede7f3b2031c1260f1a8034319f2dbdf07538a8bd2ed506a3287bc82e090`。
