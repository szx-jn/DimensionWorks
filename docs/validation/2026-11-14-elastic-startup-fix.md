# 弹力工具正式 JAR 启动崩溃修复

日期：2026-11-14。修复模块：`dimensionworks-transfer` **0.3.10**；整合包版本记录：**0.3.45**。

## 原因与改动

本机 DimensionWorks 客户端 `crash-2026-11-14_13.53.44-fml.txt` 与 `logs/latest.log` 记录 `ElasticToolInteractionMixin` 无法找到 `MobilePackager.interactLivingEntity`，随后 CMP 与 Transfer 在 CONSTRUCT 阶段初始化失败。

对实际安装的固定 CMP `0.6.1-dimensionworks.15` JAR 执行 `javap`，确认该原版 Item 重写方法在正式产物中名为 `m_6880_`。Mixin 对第三方目标禁用了 remap，原先只使用开发环境方法名，因此[此前开发环境验证](2026-11-14-elastic-packager.md)没有覆盖这个正式命名问题。

按仓库现有第三方 Mixin 做法同时匹配 `interactLivingEntity` 与 `m_6880_`，保留注入调用与必需注入检查，不改第三方源码、不跳过弹力行为。

## 实际验证

- Java 17、Gradle 8.8：`test jar --offline --no-daemon` 成功，23 项 JUnit 测试通过，失败和错误均为零；包含 `reobfJar`。
- 在独立目录 `/tmp/dw-startup-fix-runtime-20261114` 使用 Forge 47.4.23 与 12 个固定正式 JAR，监听 `127.0.0.1:25579`，使用临时世界。Create 的嵌套 JAR 提供 Flywheel、Ponder、Registrate 与 MixinExtras。
- Transfer 0.3.9 的正式 JAR 在该环境复现相同 `ElasticToolInteractionMixin` 错误，日志记录 `Failed to start the minecraft server`。Forge 此失败路径进程退出码为 0，因此以真实错误日志判断失败。
- 只替换 Transfer 为重混淆后的 0.3.10 JAR，其余模组不变，服务器启动到 `Done`，发送 `stop` 后保存各维度并正常退出，未再出现 Mixin 注入失败。
- 最小组合仍有 MegaCells 可选 EMC 接口掉落表缺项日志，与本次启动故障无关；未据此宣称整个日志零错误。
- `git diff --check`、清单 JSON 解析、模块版本/元数据/JAR 一致性和生产 JAR 内容检查通过。测试类与第三方 CMP 类未进入生产 JAR。

## 范围与限制

客户端崩溃原因来自本机真实日志；修复后的运行验证为上述隔离专用服务器，未重启完整客户端或开展多人玩法验证。

## 远程服务器日志核对

首次读取因 Electerm MCP 返回 HTTP 401 而受阻；用户恢复 MCP 后，通过现有 SSH 会话只读检查 `/opt/mc`，确认服务器版本仍为 **0.3.44**。

- `/opt/mc/logs/latest.log` 与 `crash-reports/crash-2026-11-14_05.53.10-fml.txt` 记录服务器在 UTC 05:53:10（北京时间 13:53:10）启动失败。
- 最新日志第 64、311 行记录同一 `ElasticToolInteractionMixin` 找不到 `interactLivingEntity`；CONSTRUCT 阶段最终两个致命错误分别对应 CMP 与 Transfer，崩溃报告中的异常原因均为该注入失败，与客户端一致。
- 服务器和本机 Transfer 0.3.9 的 SHA-256 均为 `7331c30533dafab9453e42168dc39192823d18e5a47ef9fd5e46f7748f1b753e`；固定 CMP 0.6.1-dimensionworks.15 均为 `f71eede7f3b2031c1260f1a8034319f2dbdf07538a8bd2ed506a3287bc82e090`，与本次隔离回归所用旧 JAR 相同。
- 日志还有第三方 Mixin 缺少 `minVersion` 和客户端类被专用服务器拒绝加载的记录；它们没有作为本次最终两个初始化失败的异常原因。此次并未启动修复后的完整服务器，不能据此断言后续加载或运行阶段没有其他问题。

已确认远程实例本次启动崩溃与客户端原因相同。此次不正式发布、不更新客户端或远程服务器、不修改现有玩家世界；修复后的运行验证仍仅为上述隔离服务器。
