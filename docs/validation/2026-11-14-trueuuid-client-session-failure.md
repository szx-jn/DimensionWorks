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

本地 JAR 仅用于开发诊断。尚未运行本次客户端修改的游戏内登录验收，也未部署、创建正式 Release 或更改整合包的 `1.0.10` 依赖。下一步需用此诊断版客户端重新登录，读取 `[TrueUUID] 客户端 joinServer 失败` 记录后，针对实际异常完成账号或网络修复。
