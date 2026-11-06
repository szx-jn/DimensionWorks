# DimensionWorks FTB Chunks Force-load Disable

This Forge 1.20.1 mod is pinned to the pack's FTB Chunks 2001.3.8.

- Rejects every FTB Chunks force-load request on the server, including packet, command, API, and admin paths.
- Blocks FTB Chunks from marking a claimed chunk as force-loaded or adding its own chunk tickets, while still allowing ticket removal.
- Prevents saved force-load entries from being restored into active tickets at startup.
- Hides force-load markers and tooltip details, and disables the force-load expiry wheel control in the FTB Chunks map.
- Cancels the Shift+left-click force-load action in the client map before it sends a packet.
- Leaves claims, unclaims, claim protection, teams, waypoints, and other mods' chunk tickets untouched.

Existing force-load metadata may remain in FTB Chunks team data, but this mod prevents that metadata from being turned into FTB Chunks chunk tickets. The map no longer displays the force-loaded marker or its tooltip status. A stale marker in saved team data does not mean the chunk has an active ticket. Players can still unclaim chunks and use all non-FTB-Chunks force-loading mechanisms.

The mod deliberately does not patch Minecraft's generic chunk-ticket API, so the pack's dedicated force-load blocks and other mods are not globally disabled.

Standalone Gradle builds resolve ForgeGradle and Mixin from their official plugin repositories. The complete client and server guard set is released as mod version 0.1.2.

The Mixin targets are declared as external optional targets at compile time; the mod metadata still requires the pack's pinned FTB Chunks version at runtime.

A full server restart is required after installing or updating this mod. The server-side guards are authoritative: even if a client sends a force-load request directly, FTB Chunks rejects it before a claim can become force-loaded. The GUI mixins target the package names used by the pack's pinned 2001.3.8 release.

## 停服权限兼容（0.1.3）

FTB Chunks 2001.3.8 在玩家登录、退出时都会查询 `ftbchunks.chunk_load_offline`，即使整合包已经禁止 FTB Chunks 强加载。服务端停服时，LuckPerms 可能先关闭 API；FTB XMod Compat 2.1.3 的查询随后抛出 `LuckPermsProvider$NotLoadedException`，使玩家退出清理和后续世界保存中断。

本模块在 `PermissionsHelper.getChunkLoadOffline(ServerPlayer, boolean)` 入口直接返回 `false`，将离线强加载权限与既有禁用策略统一，并避免访问正在关闭的权限提供者。保留 FTB Chunks 的整个玩家退出流程，继续清理强加载缓存、停止玩家追踪及移除既有区块票据；认领额度、领地保护和其他权限查询仍使用原有实现。无需新增模组、修改第三方源码或更换权限后端。

此检查只在已有权限查询时执行，开销为常量，不增加区块加载、实体、机器或每 tick 扫描。更新后需要完整重启服务端；仅重新加载 KubeJS 或配置不能加载新的 Mixin。

本地验证：Java 17 / Gradle 8.8 的 `build` 通过，JSON/TOML 语法、Mixin 类路径及清单/模组版本一致性检查通过。使用与服务器 JAR 的 SHA-256 一致的 FTB Chunks 2001.3.8 目标类及本地产物中的 Mixin，隔离测试复现了原始权限提供者关闭异常；转换后的目标类在提供者正常、关闭以及不同默认值下均拒绝离线强加载，认领与保护权限查询仍正常调用提供者。此测试验证目标类转换和调用路径，不代表已在实际玩家停服场景中验证。
