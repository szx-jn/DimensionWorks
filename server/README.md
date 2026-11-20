# Server

存放服务端部署模板、说明和可分发配置。

服务端包由 `scripts/package_server.py` 从以下现有来源生成：

- `manifest/mods.json`：筛选 `environment` 为 `both` 或 `server` 的必需 Mod。
- `mods/<name>/`：构建自研 Mod，并在 `environment` 允许时加入服务端。固定第三方 fork 通过 `scripts/fixed_source.py` 校验源码与 JAR 后加入。
- `config/`、`defaultconfigs/`、`kubejs/`：随包分发的服务端配置、脚本与数据。
- Minecraft 1.21.1 / 固定版 NeoForge（版本来自根 `gradle.properties`），Java 21：包含已安装的 `libraries/` 与启动参数。

迁移期间清单若仍指向 Forge，打包会明确拒绝，禁止生成混合加载器包。目标清单闭合并验证后，生成命令：

```sh
python3 scripts/package_server.py
```

默认输出到 `dist/DimensionWorks-Server-<当前 VERSION>.zip`。压缩包根目录可直接作为服务端目录使用；Linux 或开服面板启动命令填写 `bash start.sh`，Windows 运行 `start.bat`。首次启动前必须由服主阅读并同意 Minecraft EULA，把 `eula.txt` 改为 `eula=true`。

服务端包不提交世界存档、日志、密钥、玩家名单或其他运行时私有数据。

## MSL 与正版双重认证

整合包迁移候选使用 [TrueUUID 原生 fork 固定提交](https://github.com/szx-jn/TrueUUID-DimensionWorks/tree/b2fa74999f4df0e492f5ef3fb018dca3b7dbbd89)，保留原 1.0.10 认证策略和身份注册表。源码与产物均固定校验值，不发布中间发行版。客户端与服务端必须使用同一版本，且各自 `mods/` 中只保留一份 TrueUUID。

MSL Skin 的 authlib-injector 服务端认证路径使用原生在线验证（`online-mode=true`）；TrueUUID 的自定义握手仍由服务端独立完成会话验证。认证成功后才继承同名离线玩家数据，已存在的验证身份档案优先，不覆盖后续进度。继承操作先备份并标记 pending，全部复制完成后记录 complete；失败拒绝本次登录。

4 项 JUnit、13 项原生 GameTest 和隔离开发客户端检查已通过，原服 9 条现役身份记录在副本上保持一致。真实 Mojang/MSL 登录与完整整包尚待验收；模拟认证与构建通过不能代替实际登录。世界的跨版本转换由独立迁移流程完成，不能仅替换 mods 后直接启动旧世界。

## FTB Chunks 强加载限制

服务端默认配置将 `max_force_loaded_chunks` 设为 `0`，保留区块认领、保护、地图与路径点，但不向普通玩家提供 FTB Chunks 强加载额度。不要给玩家或玩家组授予 LuckPerms 数值权限 `ftbchunks.max_force_loaded`，因为该权限可以覆盖默认额度。专用的三个强加载方块不受此配置影响。

新世界从 `defaultconfigs/ftbchunks-world.snbt` 建立世界配置；运行中的世界以 `<level-name>/serverconfig/ftbchunks-world.snbt` 为准。当前跨平台 Go 更新器不迁移世界配置。
