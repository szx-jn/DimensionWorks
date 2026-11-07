# TrueUUID 登录故障与玩家数据保全

## 结论

TrueUUID 1.3.0 在现有 Forge 1.20.1、LuckPerms 5.4.102、MSLX authlib-injector 1.2.8 环境中未通过真实登录验收。此次恢复先回退并固定官方 TrueUUID 1.0.9（Modrinth `Ix9wU2uu`），客户端与服务端统一版本，保持认证失败拒绝登录。后续整合包改用基于 1.0.9 的 [自动继承 fork](2026-11-07-trueuuid-automatic-fork.md)；本页记录官方 1.0.9 的恢复过程，其登录验收结果不能直接当作修改版已经验收。

现网与 HMCL 的 DimensionWorks 实例各只保留一份官方 `trueuuid-1.0.9-forge1.20.1.jar`，SHA-256 为 `775c8a97af77ca5d1bd2f83ce04ddaf1eb7685ebfbd2acec8a2f6f6d274cfd4a`。两端原 1.3.0 JAR 及配置均已备份。

v0.3.13 已发布资产包含 1.3.0；仓库修复不会改写已发布资产。使用该版本排障时，必须同步回退两端 JAR 和服务端配置，不能重新覆盖已恢复的运行环境。

## 三个连续故障

1. 服务端 authlib-injector 改写 TrueUUID 内的 Mojang URL 为本机 HTTP 代理地址。1.3.0 的 `SafeSessionHttpClient` 拒绝非公网 HTTPS 端点，正版会话因此校验失败。将 `cn.alini.trueuuid.` 加入 `authlibinjector.ignoredPackages` 后，现网真实 Mojang 校验恢复 HTTP 200。整合包启动脚本保留该 JVM 参数，MSLX 须在其实际使用的 JVM 参数中配置。
2. 1.3.0 在 Forge 预登录之后替换认证 UUID，LuckPerms 已按离线 UUID 预加载用户。玩家文件加载时，LuckPerms 找不到正版 UUID 的预加载数据，尝试断开尚未建立连接的 `ServerPlayer`，触发 `ForgeConnectionListener.onPlayerLoadFromFile` 空指针，客户端显示“无效的玩家数据”。这条堆栈不能证明玩家 NBT 损坏，不应通过删除玩家文件处理。
3. 仅回退服务端后，1.3.0 客户端仍回送不兼容的旧握手响应，Mojang 校验返回 HTTP 204。将客户端也替换为 1.0.9 并完全重启游戏后，认证和玩家加载同时恢复。

1.0.9 的白名单是 `[auth]` 下带点号的字面键 `"yggdrasil.apiRootWhitelist"`，1.3.0 则使用 `[auth.yggdrasil]` 嵌套表，两者不可混用。回退时恢复旧键、中文提示和 `auth.nomojang.enabled=false`；全部离线兜底开关保持关闭，TrueUUID 注册表保持原样。

## 完整冷备份

服务端正常执行保存和停止，确认全部维度保存、进程退出后，备份整个 `world/` 与 `config/`，同时保留服务器设置和玩家名单。备份包括所有维度、玩家 NBT、原版进度、统计、FTB 数据、既有迁移归档、TrueUUID 注册表和 LuckPerms H2 数据库。

- 备份：`/opt/mc/auth-recovery-backups/20261107-032041/world-and-config-preservation.tar`。
- 大小：`1,639,905,280` 字节。
- SHA-256：`5a3b43362ec02eb650c2851606b2eefc0ed623ac5fb47c2cdaabfe9fb8a25636`。
- 已检查归档包含全部 `1,637` 个世界文件和权限数据库；保存逐文件 SHA-256 清单与备份凭证。

替换服务端 JAR、配置后，再次核对全部 1,637 个世界文件，与替换前逐字节一致。此结果对应停服替换时刻；随后正常登录、保存和玩家游玩会更新世界文件，不能据此要求运行后的世界继续保持相同哈希。

此次回退没有重新执行玩家迁移、删除玩家文件或用旧世界覆盖当前进度。原有离线升级归档和全部恢复备份继续保留。

## 实际登录验收

HMCL 使用已登录的 Microsoft 账号启动 DimensionWorks；本地确认实例路径、唯一 TrueUUID JAR 及其摘要。登录前只读解析玩家 NBT，并记录正版 UUID、背包物品、原坐标及八类相关数据文件摘要，作为核对依据。

2026-11-07 的服务端日志（以下为北京时间）：

| 证据 | 结果 |
| --- | --- |
| 11:32:45 Mojang 会话校验 | HTTP 200，UUID `51b1696c-c4e3-44d9-b7c0-2706b9795eff` |
| 11:32:46 玩家加载 | 以该正版 UUID 进入，坐标与登录前 NBT 完全一致 |
| 登录后的认证类型 | `mode=MOJANG:Mojang` |
| 玩家反馈 | 确认可以进入，玩家数据完整恢复 |

本次实际证明匹配的 1.0.9 客户端与服务端可恢复该正版账号；先前 MSL 账号在 1.0.9 下归档重复旧 UUID 文件后也已实际登录。没有声称 1.3.0 的登录顺序问题已修复，亦未在恢复后重新测试全部账号来源。后续升级必须同时验证 Mojang/MSL 登录、LuckPerms 预加载及离线继承全过程。

回退提交前本地执行 `git diff --check`、清单 JSON 与 TOML 解析、固定版本及认证策略断言，全部通过；`python3 scripts/pack_hook.py --skip-build` 的 12 项检查通过。该回退用于维持现网可用；后续按维护者要求在独立 fork 中基于 1.0.9 添加自动迁移，不继续升级 1.3.0。
