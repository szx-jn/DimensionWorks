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

整合包固定使用 [TrueUUID `1.0.10`](https://github.com/szx-jn/TrueUUID-DimensionWorks/releases/tag/v1.0.10)（Forge 1.20.1）。该版本以官方 TrueUUID 1.0.9 为基线，仅在其原有认证与迁移机制上增加自动继承：Mojang 正版登录以及 MSL/Yggdrasil 统一认证成功后，自动检查并继承同名离线玩家数据。客户端与服务端必须使用同一版本，且各自 `mods/` 中只保留一份 TrueUUID。

MSL Skin 的 authlib-injector 服务端认证路径使用原生在线验证（`online-mode=true`）；TrueUUID 的自定义离线握手路径则仍由服务端独立完成会话验证。自动继承只在对应认证成功后执行，不以客户端单方面声明作为认证依据。迁移前会备份相关玩家数据；迁移失败会拒绝本次登录，避免玩家以不完整数据进入。

修改版尚未完成游戏内 Mojang/MSL 双路径登录及 FTB 缓存的实际验收。构建成功仅代表 JAR 编译和发布完成，不等于真实服务器兼容性验收。部署前请先完整备份世界、配置和权限数据，并在测试服验证两种登录路径及玩家背包、位置、末影箱、任务数据。

## FTB Chunks 强加载限制

服务端默认配置将 `max_force_loaded_chunks` 设为 `0`，保留区块认领、保护、地图与路径点，但不向普通玩家提供 FTB Chunks 强加载额度。不要给玩家或玩家组授予 LuckPerms 数值权限 `ftbchunks.max_force_loaded`，因为该权限可以覆盖默认额度。专用的三个强加载方块不受此配置影响。

新世界从 `defaultconfigs/ftbchunks-world.snbt` 建立世界配置；运行中的世界以 `<level-name>/serverconfig/ftbchunks-world.snbt` 为准。当前跨平台 Go 更新器不迁移世界配置。
