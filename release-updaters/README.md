# 独立增量更新器

更新器是独立发布资产，不会被打入客户端 `.mrpack` 或服务端 ZIP。

| 文件 | 用途 |
|---|---|
| `update-dimensionworks-client.sh` | macOS / Linux 客户端 |
| `update-dimensionworks-server.sh` | macOS / Linux 服务端 |
| `update-dimensionworks-client.exe` | Windows 客户端 |
| `update-dimensionworks-server.exe` | Windows 服务端 |

把对应更新器放到客户端实例根目录或服务端根目录，然后双击运行。也可以通过终端使用 `--release vX.Y.Z`、`--package <本地完整包>`、`--url <完整包 URL>` 和 `--help`。不带参数时查询 GitHub Latest Release。

Windows 更新器由项目根目录的 `update-Windows/` Go 程序构建，不依赖 BAT、PowerShell 或 Python。GitHub Actions 会运行 Go 测试、交叉编译客户端与服务端 EXE，并把二者作为正式 Release 资产发布。失败时控制台会保留错误信息；下载与暂存文件会校验摘要，应用阶段支持事务回滚。

更新前请先退出 Minecraft、启动器或服务端。客户端保留存档、日志、崩溃报告、截图和启动器私有数据；服务端保留世界、EULA、`server.properties`、JVM 参数、白名单、封禁、用户缓存和运行日志。服务端已经存在的运行配置不会被默认配置覆盖。状态与事务文件位于 `.dimensionworks-update/`；更新清单记录版本和受管文件摘要。

macOS/Linux Shell 更新器需要系统 `bash`、`curl` 或 `wget`、`unzip` 和 SHA-256 工具。Windows 用户只需运行对应 EXE。
