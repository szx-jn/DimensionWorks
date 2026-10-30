# Go 跨平台更新器

本目录包含 DimensionWorks 客户端与服务端的统一 Go 更新器。目录名沿用历史命名；同一份源码可构建为 Windows、Linux 与 macOS 原生程序。核心实现只依赖 Go 标准库，不要求玩家安装 aria2、curl、wget、Python 或 Java 来运行更新器。

## 本地测试

~~~sh
go test ./...
~~~

## 交叉编译

在本目录执行，示例以 Linux/macOS shell 为例：

~~~sh
mkdir -p ../dist
for target in "windows amd64" "linux amd64" "darwin arm64"; do
  read -r goos goarch <<EOF
$target
EOF
  case "$goos" in
    windows) platform=windows; extension=.exe ;;
    linux) platform=linux; extension= ;;
    darwin) platform=macos; extension= ;;
  esac
  for kind in client server; do
    GOOS="$goos" GOARCH="$goarch" CGO_ENABLED=0 go build \
      -trimpath -ldflags="-s -w -X main.defaultKind=$kind" \
      -o "../dist/update-dimensionworks-$kind-$platform-$goarch$extension" .
  done
done
~~~

正式发布由 .github/workflows/release-pack.yml 构建并发布所有目标；.github/workflows/windows-updater-ci.yml 负责单元测试、跨平台交叉编译和 Windows 环境冒烟测试。

## 发布资产与使用

- Windows amd64：update-dimensionworks-{client,server}-windows-amd64.exe
- Linux amd64：update-dimensionworks-{client,server}-linux-amd64
- macOS Apple Silicon（arm64）：update-dimensionworks-{client,server}-macos-arm64

将对应的更新器放入目标实例根目录，再运行。Linux/macOS 下载后如文件没有可执行权限，先执行 chmod +x ./update-dimensionworks-<kind>-<platform>-<arch>。更新器会检查实例标记并要求确认游戏、启动器或服务端已退出。不支持的系统架构不会发布对应更新器。

不带参数时更新到 GitHub Latest Release，也可使用 --release vX.Y.Z、--package <本地完整包路径>、--url <完整包 URL> 或 --help。

## 安全与数据保护

更新器校验更新清单、相对路径与文件摘要，拒绝压缩包路径穿越、符号链接、重复或大小写冲突路径，并在写入前暂存和备份文件；应用失败时尝试回滚。大于 8 MiB 且服务器支持 HTTP Range 的文件可使用最多 8 路并发分段下载；分段响应、Content-Range、字节数和最终摘要均需校验，服务器不支持 Range 时回退单连接下载。 下载期间使用统一进度面板汇总并发任务，动态显示总体进度条、已下载/总大小、实时速度、预计剩余时间、活跃下载数与当前文件；分段和单连接下载均按实际写入字节更新（分段写入会计入所属文件），Range 与单连接回退使用独立的 15 分钟尝试上下文，避免复用已过期的上下文；分段请求失败会立即取消其他分段；HTTP 响应头设有独立超时，并保留系统代理设置。镜像重试或 Range 回退会重置本次尝试的计数。替换文件时使用同目录随机临时文件，避免使用可预测临时路径，并在 macOS/Linux 上保留归档文件的可执行权限。

客户端存档与服务端运行数据遵循清单中的保留规则。跨平台构建与自动化测试不能完全替代真实实例验证；发布前仍应检查各平台的实际更新、失败回滚、权限与日志行为。

## 安全与恢复策略

- 更新前会校验待删除文件的旧版本摘要；检测到本地修改时跳过自动删除。
- 覆盖已被本地修改的受管理文件前，会将原文件复制到 `.dimensionworks-update/user-backups-*`，备份不会随本次更新临时目录清理。
- 下载、更新清单和 ZIP 展开均有资源上限，避免异常响应耗尽磁盘或内存。
- 分段探测必须返回严格匹配的单字节 `Content-Range`。
- 下载采用按预期数据量调整的空闲超时，并设置较宽松的单次尝试总时限；慢速但持续有数据的连接不会因为固定短时限被过早中断。
- 文件替换使用操作系统的替换原语，避免先删除旧目标文件再重命名造成的空窗期。
- 正式替换前会将逐文件操作写入并同步到事务日志；下次启动时会先恢复未完成的事务，再开始新更新。
- 检测到已有更新锁时，会询问用户：输入 Y 清理旧锁并继续，输入 N 或其他内容取消；更新器不检查 PID 或进程状态。新锁带有随机所有者标识，旧进程退出时不会误删后来创建的新锁。
- HTTP 清单与发行信息读取有独立大小上限；下载体在写入时执行大小限制，ZIP 展开同时校验实际展开长度，避免仅依赖 ZIP 头部声明。
- 默认拒绝版本降级；确需降级时显式使用 `--allow-downgrade`。
