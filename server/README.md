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
