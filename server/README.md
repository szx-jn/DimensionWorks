# Server

存放服务端部署模板、说明和可分发配置。

服务端包由 `scripts/package_server.py` 从以下现有来源生成：

- `manifest/mods.json`：筛选 `environment` 为 `both` 或 `server` 的必需 Mod。
- `mods/<name>/`：构建自研 Mod，并在 `environment` 允许时加入服务端。
- `config/`、`defaultconfigs/`、`kubejs/`：随包分发的服务端配置、脚本与数据。
- 固定版 Forge `47.4.23`：包含已安装的 `libraries/` 与启动参数。

生成命令：

```sh
python3 scripts/package_server.py
```

默认输出到 `dist/DimensionWorks-Server-<当前 VERSION>.zip`。压缩包根目录可直接作为服务端目录使用；Linux 或开服面板启动命令填写 `bash start.sh`，Windows 运行 `start.bat`。首次启动前必须由服主阅读并同意 Minecraft EULA，把 `eula.txt` 改为 `eula=true`。

服务端包不提交世界存档、日志、密钥、玩家名单或其他运行时私有数据。

## MSL 与正版双重认证

整合包固定使用 TrueUUID `1.3.0` 的 Forge 1.20.1 正式构建，验证 Mojang 正版会话或 MSL 统一身份会话。启用此策略时，客户端与服务端都必须安装该构建，部署服的 `server.properties` 保持 `online-mode=false`，由 TrueUUID 接管登录认证。完整服务端包的原版在线模式默认值仍为 `true`，服主启用此部署方式时需明确配置。

`config/trueuuid-common.toml` 必须同时满足：

- `allowOfflineOnTimeout=false`、`allowOfflineOnFailure=false`、`allowOfflineForUnknownOnly=false`：禁止离线登录。1.3.0 的超时和会话校验失败始终拒绝；`allowOfflineOnFailure` 只控制客户端明确报告没有登录会话时是否允许申请离线身份。
- `knownPremiumDenyOffline=true`：保护已经验证过的玩家名。
- `[auth.yggdrasil]` 下的 `apiRootWhitelist=["skin.mslmc.net"]`：按精确主机名信任 MSL。1.3.0 已修正配置路径，**升级时必须将 1.0.9 的字面键 `"yggdrasil.apiRootWhitelist"` 改为此嵌套表**。正版客户端使用的空认证地址仍走 Mojang 会话服务。
- `auth.recentIpGrace.enabled=false` 和 `ttlSeconds=10` 仅保留旧配置兼容值；1.3.0 已移除同 IP 重连宽限。旧 `auth.nomojang` 项已删除。

MSLX 面板中的认证地址为 `https://skin.mslmc.net/api/yggdrasil`，其启动命令加载已部署的 `authlib-injector.jar`。玩家客户端也需按账号来源正确登录；只有注册 MSL 账号但用纯离线方式启动游戏仍会被拒绝。

1.3.0 的白名单采用精确主机名匹配，自定义接口还受 HTTPS/443、`hasJoined` 路径、DNS/IP 和禁止重定向等限制。此次已核对发布 JAR 与对应源码，并执行迁移文件事务测试；这些检查不等于已完成新版的真实 MSL/Mojang 账号登录验收。

### 从离线账号继承玩家数据

玩家切换到同名正版或 MSL 账号后，1.3.0 会在会话验证成功后显示继承确认。管理员也可在玩家离线时执行 `trueuuid migrateuuid <玩家名>`，明确批准将旧离线数据迁移到该名字已验证的 UUID。迁移会备份旧数据和已有目标数据，完成写入后删除原目录内的旧 UUID 文件，并记录事务日志；失败时回滚。因此，成功迁移后再次登录不再因残留的离线文件重复进入迁移流程。

1.0.9 的继承命令只复制文件，原目录内的旧文件会继续触发“检测到重复 UUID 玩家数据”。已经执行过该旧版命令的服务器，须先核对新 UUID 下的数据确实继承完整，再执行 `trueuuid cleanupuuid <玩家名>`，将重复旧文件移入 `world/trueuuid-backups/offline-cleanups/`。不要在尚未继承数据时清理唯一的旧存档。新版本的正常迁移成功后无需追加清理命令。

原版进度与统计使用相同的 `<UUID>.json` 文件名；1.3.0 将进度备份单独放在 `vanilla/advancements/`，避免被统计备份覆盖。迁移只处理该玩家的已知文件路径，不加载区块、不生成实体，也不增加机器或常驻扫描。此次故障记录和验证范围见 [TrueUUID 迁移验证](../docs/validation/2026-11-07-trueuuid-migration.md)。

更新器保留已有 `server.properties`、`user_jvm_args.txt` 和 `config/`；缺失的配置才从新包补齐。因此不会覆盖部署服的认证策略，也不会自动迁移上述白名单键或纠正旧的离线兜底设置。已有服务器升级时需同步修改 `config/trueuuid-common.toml`，安排客户端与服务端一同更新 TrueUUID，并重启服务端加载新 JAR。MSLX 的 `ServerList.json` 位于面板数据目录，不在整合包更新范围；面板启动不读取整合包的 `start.sh`，认证代理参数应配置在面板中。

## FTB Chunks 强加载限制

服务端默认配置将 `max_force_loaded_chunks` 设为 `0`，保留区块认领、保护、地图与路径点，但不向普通玩家提供 FTB Chunks 强加载额度。不要给玩家或玩家组授予 LuckPerms 数值权限 `ftbchunks.max_force_loaded`，因为该权限可以覆盖默认额度。专用的三个强加载方块不受此配置影响。

Linux/macOS Shell 服务端更新器会在更新时只同步当前世界的 `serverconfig/ftbchunks-world.snbt`，并备份被替换的文件；世界存档的其他内容不变。Windows EXE 更新器暂不执行这项现有世界迁移，使用它更新旧世界时需手动将 `defaultconfigs/ftbchunks-world.snbt` 复制到 `<level-name>/serverconfig/ftbchunks-world.snbt`。

**已有世界首次应用限制时，还需在服务器控制台执行一次** `ftbchunks admin unload_everything`，清除此前由 FTB Chunks 建立的强加载请求。该命令不会清除三个专用强加载方块的机制。执行前应先停用不需要的 FTB Chunks 强加载，并确认该命令在当前服务端可用。
