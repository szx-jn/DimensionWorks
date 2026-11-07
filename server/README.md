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

整合包固定使用 [TrueUUID `1.0.9-dimensionworks.1`](https://github.com/szx-jn/TrueUUID-DimensionWorks/releases/tag/v1.0.9-dimensionworks.1) 的 Forge 1.20.1 修改版，基于社区版 1.0.9 保留 Mojang/MSL 会话认证和客户端协议，增加自动继承离线存档。**客户端与服务端同步使用这一修改版，且各自 `mods/` 中只保留一份 TrueUUID。** 1.3.0 在现有 Forge/LuckPerms 环境中存在登录顺序兼容问题，该升级已撤回。启用此策略时，部署服的 `server.properties` 保持 `online-mode=false`，由 TrueUUID 接管登录认证。完整服务端包的原版在线模式默认值仍为 `true`，服主启用此部署方式时需明确配置。

`config/trueuuid-common.toml` 必须同时满足：

- `allowOfflineOnTimeout=false`、`allowOfflineOnFailure=false`、`allowOfflineForUnknownOnly=false`：禁止离线登录。1.0.9 的未知名字失败决策独立读取最后一项，三个开关必须一起关闭。
- `knownPremiumDenyOffline=true`：保护已经验证过的玩家名。
- `auth.autoMigrateOfflineData=true`：只有真实会话校验成功后，才自动备份并继承同名离线存档；认证失败或本地代理宽限不会触发迁移。
- `[auth]` 下的字面键 `"yggdrasil.apiRootWhitelist"=["https://skin.mslmc.net/api/yggdrasil/sessionserver/session/minecraft/hasJoined"]`：允许 MSL 实际使用的会话校验地址。1.0.9 按字符串包含关系匹配，并非精确主机名匹配；不要使用宽泛的域名片段。**从 1.3.0 回退时须移除 `[auth.yggdrasil]` 嵌套表并恢复此字面键。** 正版客户端使用的空认证地址仍走 Mojang 会话服务。
- `auth.recentIpGrace.enabled=false`：关闭普通同 IP 短时重连宽限；1.0.9 另有固定的本地代理宽限，此项不控制它。
- `auth.nomojang.enabled=false`：保持 Mojang 在线校验。

MSLX 面板中的认证地址为 `https://skin.mslmc.net/api/yggdrasil`，其启动命令加载已部署的 `authlib-injector.jar`。玩家客户端也需按账号来源正确登录；只有注册 MSL 账号但用纯离线方式启动游戏仍会被拒绝。

服务端使用 `authlib-injector` 时，必须在 JVM 参数中加入 `-Dauthlibinjector.ignoredPackages=cn.alini.trueuuid.`，并重启加载。代理会改写模组内的 Mojang 地址为本机 HTTP 代理地址，而 TrueUUID 1.3.0 的会话校验只接受公网 HTTPS 地址；不排除该包会使正版会话校验失败。此参数只排除 TrueUUID 的地址改写，MSL 会话仍由 TrueUUID 根据白名单验证。生成的 `start.sh` / `start.bat` 已携带参数；MSLX 不读取这两个脚本，须在面板的额外 JVM 参数中添加。如果已有 `authlibinjector.ignoredPackages`，将 `cn.alini.trueuuid.` 以逗号追加到原列表，保留其他排除项。

该参数解决认证地址改写，不能解决 1.3.0 与 LuckPerms 的预登录 UUID 不一致。2026-11-07 11:32:45（北京时间），两端统一为 1.0.9 后，现网 Mojang 会话校验返回 HTTP 200，玩家以原正版 UUID 和原坐标进入，玩家确认背包、位置及任务数据完整。故障原因、冷备份和验证边界见 [TrueUUID 登录恢复](../docs/validation/2026-11-07-trueuuid-login-recovery.md)。

### 从离线账号继承玩家数据

修改版默认在真实会话认证成功后、Forge 继续登录和读取玩家数据前执行继承。先备份涉及的原文件，再写目标并刷新相关 FTB 缓存，最后将旧文件归档到 `world/trueuuid-backups/automatic-migrations/` 下的独立事务目录。相同的目标副本允许归档重复旧文件；不同目标进度、UUID 绑定冲突或不兼容的缓存结构会停止迁移，保留双方数据。失败时回滚文件和缓存，异常退出留下的事务在下次世界加载前恢复。

玩家 NBT 只替换根 UUID，保留背包、末影箱、位置、经验和 ForgeCaps。原版进度、统计及上游已支持的玩家路径继续继承，FTB 数据同步处理 UUID 引用。完整冷备份仍应包含整个 `world/` 与 `config/`、TrueUUID 注册表及 LuckPerms 数据库；迁移内部的局部备份不能替代它。

自动模式通常不需要手动命令。管理员在已验证目标身份、相关玩家离线时可使用：

- `trueuuid migrateuuid <玩家名>`：保全模式，不覆盖不同的目标进度。
- `trueuuid migrateuuid <玩家名> overwrite`：明确选择离线存档后覆盖目标，仍先备份双方。
- `trueuuid cleanupuuid <玩家名>`：仅在已验证目标具有对应副本时归档旧数据，保留目标进度；缺少副本时拒绝清理。

不要对已恢复且正常游玩的账号盲目选择覆盖模式，保留全部备份及归档。关闭 `auth.autoMigrateOfflineData` 后使用 1.0.9 的客户端确认流程。

迁移只检查已知玩家路径和有界的 FTB 文本目录，不扫描区块文件、不加载区块、不生成实体，没有常驻扫描。开发阶段文件事务与 NBT 测试已通过；按维护者要求，修改版未完成游戏内登录和 FTB 缓存验收。修改范围及验证边界见 [1.0.9 自动继承 fork](../docs/validation/2026-11-07-trueuuid-automatic-fork.md)，原始故障记录见 [TrueUUID 迁移验证](../docs/validation/2026-11-07-trueuuid-migration.md)。

更新器保留已有 `server.properties`、`user_jvm_args.txt` 和 `config/`；缺失的配置才从新包补齐。因此不会覆盖部署服的认证策略，也不会自动迁移上述白名单键或纠正旧的离线兜底设置。已有服务器升级时需同步修改 `config/trueuuid-common.toml`，安排客户端与服务端一同更新 TrueUUID，并重启服务端加载新 JAR。MSLX 的 `ServerList.json` 位于面板数据目录，不在整合包更新范围；面板启动不读取整合包的 `start.sh`，认证代理参数应配置在面板中。

## FTB Chunks 强加载限制

服务端默认配置将 `max_force_loaded_chunks` 设为 `0`，保留区块认领、保护、地图与路径点，但不向普通玩家提供 FTB Chunks 强加载额度。不要给玩家或玩家组授予 LuckPerms 数值权限 `ftbchunks.max_force_loaded`，因为该权限可以覆盖默认额度。专用的三个强加载方块不受此配置影响。

Linux/macOS Shell 服务端更新器会在更新时只同步当前世界的 `serverconfig/ftbchunks-world.snbt`，并备份被替换的文件；世界存档的其他内容不变。Windows EXE 更新器暂不执行这项现有世界迁移，使用它更新旧世界时需手动将 `defaultconfigs/ftbchunks-world.snbt` 复制到 `<level-name>/serverconfig/ftbchunks-world.snbt`。

**已有世界首次应用限制时，还需在服务器控制台执行一次** `ftbchunks admin unload_everything`，清除此前由 FTB Chunks 建立的强加载请求。该命令不会清除三个专用强加载方块的机制。执行前应先停用不需要的 FTB Chunks 强加载，并确认该命令在当前服务端可用。
