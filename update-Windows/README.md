# Go 跨平台更新器

本目录包含 DimensionWorks 客户端与服务端的统一 Go 更新器。目录名沿用历史命名；同一份源码可构建为 Windows、Linux 与 macOS 原生程序。核心实现只依赖 Go 标准库，不要求玩家安装 aria2、curl、wget、Python 或 Java 来运行更新器。

## 本地测试

\`\`\`sh
go test ./...
\`\`\`

## 交叉编译

在本目录执行，示例以 Linux/macOS shell 为例：

\`\`\`sh
mkdir -p ../dist
for target in "windows amd64" "linux amd64" "linux arm64" "darwin amd64" "darwin arm64"; do
  read -r goos goarch <<EOF
$target
EOF
  case "$goos" in
    windows) platform=windows; extension=.exe ;;
    linux) platform=linux; extension= ;;
    darwin) platform=macos; extension= ;;
  esac
  for kind in client server; do
    GOOS="$goos" GOARCH="$goarch" CGO_ENABLED=0 go build \\
      -trimpath -ldflags="-s -w -X main.defaultKind=$kind" \\
      -o "../dist/update-dimensionworks-$kind-$platform-$goarch$extension" .
  done
done
\`\`\`

正式发布由 \`.github/workflows/release-pack.yml\` 构建并发布所有目标；\`.github/workflows/windows-updater-ci.yml\` 负责单元测试、跨平台交叉编译和 Windows 环境冒烟测试。

## 发布资产与使用

- Windows amd64：\`update-dimensionworks-{client,server}-windows-amd64.exe\`
- Linux amd64 / arm64：\`update-dimensionworks-{client,server}-linux-{amd64,arm64}\`
- macOS Intel / Apple Silicon：\`update-dimensionworks-{client,server}-macos-{amd64,arm64}\`

将对应的更新器放入目标实例根目录，再运行。Linux/macOS 下载后如文件没有可执行权限，先执行 \`chmod +x ./update-dimensionworks-<kind>-<platform>-<arch>\`。更新器会检查实例标记并要求确认游戏、启动器或服务端已退出。

不带参数时更新到 GitHub Latest Release，也可使用 \`--release vX.Y.Z\`、\`--package <本地完整包路径>\`、\`--url <完整包 URL>\` 或 \`--help\`。

## 安全与数据保护

更新器校验更新清单、相对路径与文件摘要，拒绝压缩包路径穿越、符号链接、重复或大小写冲突路径，并在写入前暂存和备份文件；应用失败时尝试回滚。大于 8 MiB 且服务器支持 HTTP Range 的文件可使用最多 8 路并发分段下载；分段响应、Content-Range、字节数和最终摘要均需校验，服务器不支持 Range 时回退单连接下载。替换文件时使用同目录随机临时文件，避免使用可预测临时路径，并在 macOS/Linux 上保留归档文件的可执行权限。

客户端存档与服务端运行数据遵循清单中的保留规则。跨平台构建与自动化测试不能完全替代真实实例验证；发布前仍应检查各平台的实际更新、失败回滚、权限与日志行为。
