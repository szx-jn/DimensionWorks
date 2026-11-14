# 弹力包裹两秒蓄力与满蓄力增强验证

日期：2026-11-14。实现模块：`dimensionworks-transfer` 0.3.13。按用户确认，满蓄力调整为此前力度的 5 倍。

## 行为边界

- 地面蓄力上限从 60 tick 改为 40 tick，即正常 20 TPS 下 2 秒。
- 未满蓄力的 1–39 tick 保持原式 `0.35 + 3.3625 × tick / 60`，不缩小分母，不增加倍率。
- 只有达到 40 tick 才使用 `3.7125 × 5 = 18.5625` 格/tick；继续按住不再提高力度。
- 客户端蓄力条仍使用原位置、尺寸和三段颜色，按 40 tick 填满并整体变金色。
- 原版速度同步包将每个向量分量截断为 ±3.9。新增仅用于满蓄力的服务端到本人的完整速度包，三个分量使用 double；未满蓄力保留原速度同步路径。弹力通道协议升级为 2，两端须使用同版模块。
- 反弹、潜行抑制、移速、视角方向、解包与离地清理、竖直传送带及其他包裹行为沿用原实现。只增加松开满蓄力时的一次数据包，不新增实体、区块票据或扫描。

## 本地验证

本机 Java 17 / 已安装 Gradle 8.14；发布工作流仍使用仓库规定的 Gradle 8.8，未修改工具链基线。

1. `gradle test jar compileGameTestJava --no-daemon` 成功：25 项 JUnit 全部通过，失败、错误和跳过均为零。蓄力测试逐 tick 比较 1–39 tick 的原力度，覆盖 39→40 tick 边界、上限、重复释放、离地与空中行为；新数据包测试覆盖完整 18.5625 格/tick 的正负速度分量编解码。
2. 使用已有固定版模组与独立 `run-gametest-recovery` 目录，执行 `-PelasticRecipeCheck -PtransferGameTestNamespaces=dimensionworks_elastic runGameTestServer --offline --no-daemon`：16 项必需 GameTest 全部通过，包含服务端恰好 40 tick 满蓄力、完整朝向/俯仰发射、20 tick 原力度，以及既有弹力、配方和竖直传送带检查。
3. 使用独立 `run-elastic-client` 世界，执行 `-PelasticRecipeCheck -PelasticClientCheck runClient --offline --no-daemon`：输出 `ELASTIC_CLIENT_CHECK_PASS` 并正常保存退出。实际模型、包裹渲染、金色蓄力条、发射与解包检查通过；移动前采样最大速度为 **18.562499587899115 格/tick**，与目标的差异来自视角向量浮点精度。
4. `git diff --check`、清单 JSON 语法与重复键、模块版本与 `mods.toml` 一致性、文档链接及开发 JAR 内容检查通过。开发 JAR 包含新的速度包，不包含 GameTest、客户端检查或第三方 CMP 类；仅用于本地验证。

首次客户端断言在移动后读取速度，受到碰撞与阻力影响；改为玩家移动前采样。第二次检查在客户端预测刚满时松键，服务端尚未蓄满；检查恢复持续按住 60 个客户端 tick 后松键，确保服务端已经达到 40 tick 上限。上述调整仅涉及测试程序，生产代码的未满蓄力力度未变。

本地结果覆盖单元、专用服务器 API 与独立单人客户端，未验证完整整合包多人远程联机和全部视角/碰撞组合。正式整合包由 GitHub Actions 工作流构建与发布，本地开发 JAR 不作为正式产物。
