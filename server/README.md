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

部署服使用已固定的 TrueUUID `1.0.9` 验证 Mojang 正版会话或 MSL 统一身份会话。此路径要求部署服的 `server.properties` 保持 `online-mode=false`，因为 TrueUUID 只在原版离线登录流程中发起自己的认证挑战；不能仅把该值改为 `true`，否则会停用 TrueUUID 的双重认证。完整服务端包的原版在线模式默认值仍为 `true`，服主启用此部署方式时需明确配置。

`config/trueuuid-common.toml` 必须同时满足：

- `allowOfflineOnTimeout=false`、`allowOfflineOnFailure=false`、`allowOfflineForUnknownOnly=false`：超时、失败和新用户名均不得降级离线登录。已核对 1.0.9 的失败决策直接读取 `allowOfflineForUnknownOnly`，仅关闭 `allowOfflineOnFailure` 不够。
- `auth.recentIpGrace.enabled=false`、`auth.nomojang.enabled=false`：关闭同 IP 短时重连宽限和跳过正版校验模式。
- `[auth]` 中的字面键 `"yggdrasil.apiRootWhitelist"` 只列出 MSL 的 `https://skin.mslmc.net/api/yggdrasil/sessionserver/session/minecraft/hasJoined`。该键含点但必须保留引号，不要改写为 `[auth.yggdrasil]`。正版客户端使用的空认证地址由 TrueUUID 走 Mojang 会话服务。

MSLX 面板中的认证地址为 `https://skin.mslmc.net/api/yggdrasil`，其启动命令加载已部署的 `authlib-injector.jar`。玩家客户端也需按账号来源正确登录；只有注册 MSL 账号但用纯离线方式启动游戏仍会被拒绝。

上述配置关闭常规离线兜底，但不能消除 TrueUUID 1.0.9 中两条固定行为：认证地址白名单使用 `String.contains` 子串匹配，不能作为严格的 URL 信任边界；已知正版用户名从本地回环地址连接时仍有固定代理宽限，独立于 `recentIpGrace.enabled`。因此不能声称仅靠这些配置已经防住任意伪造认证端点或本地代理绕过；严格限制认证来源还需要修复对应代码。当前核对依据为部署 JAR 字节码，未完成真实账号登录测试。

更新器保留已有 `server.properties`、`user_jvm_args.txt` 和 `config/`；缺失的配置才从新包补齐。因此不会覆盖部署服的认证策略，也不会自动纠正旧的离线兜底设置。已有服务器需单独修改 `config/trueuuid-common.toml` 并重新加载或重启。MSLX 的 `ServerList.json` 位于面板数据目录，不在整合包更新范围；面板启动不读取整合包的 `start.sh`，认证代理参数应配置在面板中。

## FTB Chunks 强加载限制

服务端默认配置将 `max_force_loaded_chunks` 设为 `0`，保留区块认领、保护、地图与路径点，但不向普通玩家提供 FTB Chunks 强加载额度。不要给玩家或玩家组授予 LuckPerms 数值权限 `ftbchunks.max_force_loaded`，因为该权限可以覆盖默认额度。专用的三个强加载方块不受此配置影响。

Linux/macOS Shell 服务端更新器会在更新时只同步当前世界的 `serverconfig/ftbchunks-world.snbt`，并备份被替换的文件；世界存档的其他内容不变。Windows EXE 更新器暂不执行这项现有世界迁移，使用它更新旧世界时需手动将 `defaultconfigs/ftbchunks-world.snbt` 复制到 `<level-name>/serverconfig/ftbchunks-world.snbt`。

**已有世界首次应用限制时，还需在服务器控制台执行一次** `ftbchunks admin unload_everything`，清除此前由 FTB Chunks 建立的强加载请求。该命令不会清除三个专用强加载方块的机制。执行前应先停用不需要的 FTB Chunks 强加载，并确认该命令在当前服务端可用。
