# 弹力玩家包裹竖直传送带验证

日期：2026-11-14。实现模块：`dimensionworks-transfer` **0.3.12**；整合包代码版本记录 **0.3.48**。本次提交代码和本地验证结果，不正式发布或更新现有实例。

## 实现

- 读取清单确认 Create 为 1.20.1 / 6.0.8，并核对固定 6.0.8-291 源码。其 `BeltBlock.canTransportObjects` 排除竖直带，`BeltMovementHandler.transportEntity` 使用水平/斜坡位移；本实现保留这些限制。
- `ElasticVerticalBelt` 只接受 CMP 包裹标记与弹力标记同时成立的在线玩家。检查包裹碰撞箱附近已加载的竖直带和外侧表面，独立计算上下输送；不进入 Create 物品库存或普通带乘客表。
- 对照 Create 带面渲染的轴向和转向计算方向，速度使用 `getBeltMovementSpeed()`，即 RPM / 480。同一条带两面方向相反；反转立即生效，不累加上一帧速度。
- 在原版 `LivingEntity.travel` 中替代真实接触时的一次普通重力运动，保留玩家横向操作、带宽居中和 `move(SELF)` 的方块碰撞。没有新增实体、贴图、区块加载票据或常驻无重力/无碰撞标记。
- 停机、离开接触面、越过上下末端、Shift、解包、飞行、骑乘、锁链悬挂和引导锁定退出此路径。服务端只有在符合接触条件时清除漂浮状态，不放宽方块碰撞检查。

## 构建与服务器检查

使用 Java 17、Gradle 8.8、Forge 47.4.16 与仓库现有固定 CMP/Create 依赖。运行目录为忽略的 `mods/dimensionworks-transfer/run-gametest-recovery`；测试依赖读取本机已安装固定版本 JAR，不修改玩家实例。

```sh
gradle -PtransferTestModsDir='<固定模组目录>' -PelasticRecipeCheck \
  -PtransferGameTestNamespaces=dimensionworks_elastic,dimensionworks_transfer \
  test jar runGameTestServer --offline --no-daemon
```

**23 项 JUnit 和 36 项必需 GameTest 全部通过**。6 项新增竖直带 GameTest 覆盖：

1. 四个水平轴向、两侧带面及正反转共 16 组实际 `Player.travel` 位移。
2. 连续 30 次实际运动调用跨越三个段接缝，保持固定带速、清除摔落距离、禁止地面蓄力状态并立即响应反转。
3. 普通 CMP 包裹、解包玩家及只有残留弹力标记的玩家不能获得竖直运输；Create 原限制仍保持。
4. Shift、停机、离开带面、越过上下末端和解包后脱离，恢复普通重力与碰撞。
5. 上行遇天花板、下行遇地板时实际碰撞停止，不穿墙，不把落地弹跳变成额外加速。
6. 飞行、骑乘、引导锁定和水平/斜坡/侧向传送带继续使用各自原规则。

其余 30 项为既有弹力工具、制作链、CMP 玩家包裹、锁链、解包、中转与安全检查。第一轮方向覆盖测试在重建轴向时混用了新旧带段，导致 Create 拆链；已改为先拆除旧链再完整布置，重跑全部通过。

最小测试组合仍出现既有 Vintage Improvements `minecraft:blaze_wire` 配方缺项和 MegaCells 可选 EMC 掉落项日志；它们不属于本次代码或新增检查，不据此声称日志零错误。

## 实际客户端检查

使用现有独立 `run-elastic-client/saves/ElasticCheck` 世界，按需启用新增检查入口：

```sh
gradle -PtransferTestModsDir='<固定模组目录>' -PelasticRecipeCheck \
  -PelasticClientCheck -PelasticVerticalBeltClientCheck \
  runClient --offline --no-daemon
```

测试程序在临时世界布置十段竖直带，由真实 Create 创造马达驱动，真实打包玩家并通过已安装的客户端代码运动。上行和反转后的下行各持续 20 个客户端 tick，位移断言为 1.5–2.5 格，客户端与服务端位置差小于 0.5 格；随后验证停机恢复重力、解包状态同步及运输能力清除。

客户端输出 `ELASTIC_VERTICAL_BELT_CLIENT_CHECK_PASS` 后正常保存并退出，Gradle 成功。此程序不截取图片，不修改玩家实例或远程服务器，代码只位于 opt-in `src/elasticClientTest`，不会进入生产 JAR。

## 静态检查与边界

版本更新后重新执行 `test jar`（含 `reobfJar`）。提交前核对差异空白、清单/Mixin JSON、TOML、模块版本、全部 Mixin 源声明、新 Mixin 的正式映射及生产 JAR 内容；测试类、模板和第三方类不混入生产 JAR。

以上验证包括专用服务器真实 API 和独立单人客户端，不代表完整整合包、多人远程网络或所有第三方运动/渲染组合均已穷举验证。正式产物仍须通过 GitHub Actions 发布，本地产物仅用于开发验证。
