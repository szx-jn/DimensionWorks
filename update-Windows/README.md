# Windows 原生更新器

本目录包含 DimensionWorks 客户端与服务端的 Windows 原生更新器。实现使用 Go 标准库，不调用 BAT、PowerShell、Python 或 Java。

## 本地检查与构建

在本目录执行：

```powershell
go test ./...
```

编译客户端与服务端（需要 Go 工具链）：

```powershell
$env:GOOS = "windows"
$env:GOARCH = "amd64"
$env:CGO_ENABLED = "0"
go build -trimpath -ldflags="-s -w -X main.defaultKind=client" -o ..\dist\update-dimensionworks-client.exe .
go build -trimpath -ldflags="-s -w -X main.defaultKind=server" -o ..\dist\update-dimensionworks-server.exe .
```

正式构建由 `.github/workflows/windows-updater-ci.yml` 与 `.github/workflows/release-pack.yml` 自动完成。前者运行单元测试、交叉编译并保存短期构建产物；后者把两个 EXE 与 Shell 更新器一起发布到 DimensionWorks Release。

## 使用方式

下载对应 EXE 后，必须先复制到目标客户端实例根目录或服务端根目录，再运行。更新器会检查 `mods/` 与实例标记，拒绝从常见下载目录误启动；开始前需要输入 `Y` 确认游戏、启动器和服务端已退出。

不带参数时更新到 GitHub Latest Release，也可使用：

- `--release vX.Y.Z`
- `--package <本地完整包路径>`
- `--url <完整包 URL>`
- `--help`

更新器读取包内/独立 TSV 清单，校验目标路径和文件摘要；兼容时优先使用差量包，失败则回退完整包。写入前会暂存文件并备份旧文件，应用失败会尝试回滚。错误显示在控制台，诊断日志写入 `.dimensionworks-update/logs/`（如果实例目录可写）。世界、客户端存档、日志以及服务端的 EULA、`server.properties`、JVM 参数、白名单和封禁信息都应保留。

## 边界

当前 CI 已验证 Go 单元测试与 Windows amd64 EXE 编译格式；CI 无法完全代替真实 Windows 游戏实例测试。对正式发布的 EXE 仍应在真实客户端与服务端目录中验证一次更新、失败回滚以及日志落盘。
