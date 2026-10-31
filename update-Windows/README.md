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

自动更新时会读取当前版本与目标版本之间的发布清单，沿 `base` 关系统计可用的增量包数量和每个包的大小，再与目标完整包大小比较，选择传输总量较小的方案。版本链不完整、增量包缺失或增量包校验失败时，更新器会改用完整包。跨多个版本应用增量包时，会按版本顺序暂存并校验最终文件摘要；用过的增量 ZIP 与本次分段缓存会在应用前清理，整个事务结束后临时目录也会删除。

查询 GitHub Latest Release 信息时会先直连 GitHub API；直连失败后自动通过 `gh-proxy.com` 重试。下载 GitHub Release 资产时会先直连 GitHub；直连失败后自动依次尝试 `ghproxy.net` 与 `gh-proxy.com`。更新器会为每次连接设置有限的连接与 TLS 握手超时，失败时及时切换镜像。该回退覆盖最新版本清单、指定版本清单、增量包及完整包，其他域名的 URL 不会经过这些镜像。

## 安全与数据保护

更新器校验更新清单、相对路径与文件摘要，拒绝压缩包路径穿越、符号链接、重复或大小写冲突路径，并在写入前暂存和备份文件；应用失败时尝试回滚。大于 8 MiB 且服务器支持 HTTP Range 的文件会按文件大小和同时下载的文件数动态分配并发额度，最多 8 路分段请求；分段响应、Content-Range、字节数和最终摘要均需校验，服务器不支持 Range 时回退单连接下载。 下载期间使用统一进度面板汇总并发任务，动态显示总体进度条、已下载/总大小、实时速度、预计剩余时间、活跃下载数与当前文件；分段和单连接下载均按实际写入字节更新（分段写入会计入所属文件），Range 与单连接回退使用独立的最长 6 小时尝试上下文，并按预期数据量调整空闲超时，避免慢速连接被过早中断；分段请求失败会立即取消其他分段；HTTP 响应头设有独立超时，并保留系统代理设置。镜像重试或 Range 回退会重置本次尝试的计数。替换文件时使用同目录随机临时文件，避免使用可预测临时路径，并在 macOS/Linux 上保留归档文件的可执行权限。

客户端存档与服务端运行数据遵循清单中的保留规则。跨平台构建与自动化测试不能完全替代真实实例验证；发布前仍应检查各平台的实际更新、失败回滚、权限与日志行为。

## 安全与恢复策略

- 更新前会校验待删除文件的旧版本摘要；检测到本地修改时跳过自动删除。
- 覆盖已被本地修改的受管理文件前，会将原文件复制到 `.dimensionworks-update/user-backups-*`，备份不会随本次更新临时目录清理。
- 下载、更新清单和 ZIP 展开均有资源上限，避免异常响应耗尽磁盘或内存。
- 分段探测必须返回严格匹配的单字节 `Content-Range`。
- 下载采用按预期数据量调整的空闲超时，并设置较宽松的单次尝试总时限；慢速但持续有数据的连接不会因为固定短时限被过早中断。
- 下载前会估算备份、远程文件和更新包的空间需求；解压前再按 ZIP 实际声明的展开大小复核可用空间，并保留额外安全余量。
- 文件替换使用操作系统的替换原语，避免先删除旧目标文件再重命名造成的空窗期。
- 正式替换前会将逐文件操作写入并同步到事务日志；下次启动时会先恢复未完成的事务，再开始新更新。
- 检测到已有更新锁时，会询问用户：输入 Y 清理旧锁并继续，输入 N 或其他内容取消；更新器不检查 PID 或进程状态；确认流程由操作系统文件锁串行化，避免两个更新器同时清理同一把锁。新锁带有随机所有者标识，旧进程退出时不会误删后来创建的新锁。
- HTTP 清单与发行信息读取有独立大小上限；下载体在写入时执行大小限制，ZIP 展开同时校验实际展开长度，避免仅依赖 ZIP 头部声明。
- 本地状态清单使用 HMAC-SHA256 签名，单独修改 state.tsv 会被检测并拒绝更新；旧版无签名状态会被视为不可信并重新扫描本地文件，避免沿用伪造的旧文件删除记录。签名密钥保存在 `.dimensionworks-update/state.key`，请与实例备份一起保留。 该机制用于发现状态文件单独损坏或被修改；无法防御拥有相同账户权限、同时替换状态文件和密钥的本机攻击者。
- 支持断点续传：单连接下载按 ETag/Last-Modified 校验后续传；多路 Range 下载会缓存已完成分段、同步落盘并保存 SHA-256 校验值；重新运行时只复用校验通过且资源 ETag/Last-Modified 未变化的分段。
- 默认拒绝版本降级；确需降级时显式使用 `--allow-downgrade`。
