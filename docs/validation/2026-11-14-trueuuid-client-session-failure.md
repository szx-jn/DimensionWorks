# TrueUUID 客户端会话提交失败排查

## 现场证据

2026-11-14 通过 Electerm MCP 只读检查 `/opt/mc`。服务端版本为 `0.3.41`，TrueUUID 为 `1.0.10`，最新日志记录服务器已于北京时间 2026-11-13 23:53 停止。

23:51:58 与 23:52:06 的 `1q_b1` 登录均出现：

```text
auth answer flags: migrationConfirmed=false, missingSessionToken=false
客户端认证包ackOk: false, hasJoinedUrl: (mojang default)
认证失败，原因: 客户端拒绝
该名称已绑定正版 UUID，鉴权失败时不允许以离线模式进入。
```

这证明客户端未完成 Mojang `joinServer`，服务端尚未执行该次登录的 `hasJoined` 验证或离线数据继承。`missingSessionToken=false` 仅说明令牌没有命中空值或占位值判断，不能证明会话有效。现有客户端把 `joinServer` 异常转成 `false`，没有记录具体异常；因此当前证据不能区分会话失效、认证服务不可用或其他客户端错误，也不能把本次故障归因于 LuckPerms 预加载 UUID。

## 有针对性的客户端修改

基于当前正式标签 `v1.0.10` 的独立分支 `codex/client-session-diagnostics`，修改提交：[bcf75b8](https://github.com/szx-jn/TrueUUID-DimensionWorks/commit/bcf75b8)。

- 记录客户端 `joinServer` 异常类型、错误说明和直接底层异常类型；日志中的 access token 会被遮盖。
- 保留向服务端发送失败响应，由原有配置决定拒绝或离线兜底，不改变握手字段、会话验证或玩家数据迁移。
- 服务端拒绝后，客户端优先显示原版会话失效、认证服务不可用或具体认证错误；未出现客户端认证异常时仍显示服务端原有断线原因。

修复不新增依赖、区块加载、实体或常驻任务；仅在已有登录异步任务的失败分支记录原因，并在该连接中保留一个错误文本。

## 实际验证与后续

- 本地 Java 17、Gradle 8.8、Forge 47.4.8 的 `bash ./gradlew build --console=plain` 通过；该工程没有测试用例，不将构建成功描述为测试通过。
- `git diff --check` 通过；客户端 Mixin 已注册，发行 JAR 的 refmap 包含 `handleCustomQuery` 和新增 `onDisconnect` 的 SRG 映射。
- 三个断线文案键已在 Minecraft 1.20.1 语言资源中核对存在。
- 使用明确无效的临时会话向 Mojang `join` 发起独立探针，得到 HTTP 403；这仅验证该探针可收到拒绝响应，不验证真实玩家令牌或游戏登录。
- 修改提交的 [GitHub Actions 构建](https://github.com/szx-jn/TrueUUID-DimensionWorks/actions/runs/43518625169)成功，没有创建正式 Release。

## 本机客户端实测

用户授权操作本机客户端后，先备份原 JAR 到 `/tmp/dw-trueuuid-client-backup-20261114/`，再替换本机 DimensionWorks 实例中的唯一一份 TrueUUID。安装的诊断 JAR SHA-256 为 `0a5ac157ce9a7dd0ec2a1fa96e1260e969ef8f13d6f4779621092c83a96c3e13`。

测试通过终端启动，使用本机 HMCL 保存的 `1q_b1` 正版会话。独立 Forge 47.4.23 服务端位于 `/tmp/dw-trueuuid-login-runtime-20261114/`，仅监听 `127.0.0.1:25576`，使用原版 TrueUUID 1.0.10；不连接或修改 `/opt/mc`、生产世界及其玩家数据。

| 场景 | 实际结果 |
| --- | --- |
| 本机完整 DimensionWorks 客户端连接精简测试服 | 06:31:48 开始连接；客户端 `ackOk=true`，Mojang `hasJoined` HTTP 200，解析为正版 UUID `51b1696c-c4e3-44d9-b7c0-2706b9795eff`。随后因测试服只有 TrueUUID 而出现预期的模组列表不匹配，未进入世界。 |
| 同版本精简客户端使用明确无效的测试令牌 | 06:32:49 诊断分支记录 `InvalidCredentialsException`，客户端 `ackOk=false`；服务端保留已绑定正版名称的拒绝策略。没有 Mixin 注入失败。未通过界面核验断线文案。 |
| 精简客户端恢复同一本机正版会话 | 06:33:33 Mojang 验证 HTTP 200、UUID 正确，服务端记录 `logged in with entity id` 和 `joined the game`；客户端记录已连接到模组服务器。 |

测试后已停止测试客户端和本地测试服，并清除临时会话及启动参数文件。本机 HMCL 当前选择的账号类型确认为 Microsoft。诊断 JAR 保留在本机 DimensionWorks 客户端，便于下次发生故障时取得异常。

当前会话能够通过正版认证，历史 `ackOk=false` 暂未复现，旧日志缺少异常，仍无法确定当时是会话失效还是网络失败。本次修复已验证能记录无效会话的具体失败类型，并保留成功登录和服务端拒绝策略；不能据此声称历史故障根因已消除。没有验证生产整合包进入世界或离线玩家数据继承，也未创建正式 Release 或更改整合包固定的 `1.0.10` 依赖。
