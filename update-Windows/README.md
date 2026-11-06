# Go 跨平台更新器

本目录包含 DimensionWorks 客户端与服务端的统一 Go 更新器。目录名沿用历史命名；同一份源码可构建为 Windows、Linux 与 macOS 原生程序。正式发布的 Windows amd64、Linux amd64、macOS arm64 客户端与服务端共六个更新器，全部为内嵌 aria2c 的单文件程序。文件下载仅使用 aria2c，不自动切换 Go 下载器；发行信息查询仍使用 Go HTTP 客户端。玩家不需要额外安装 aria2、curl、wget、Python 或 Java。

## 本地测试

~~~sh
go test ./...
# 按下方步骤准备 aria2-runtime 后，验证内置引擎与竞态
go test -tags bundled_aria2 ./...
go test -race -tags bundled_aria2 ./...
~~~

## 交叉编译

在本目录执行，示例以 Linux/macOS shell 为例：

~~~sh
mkdir -p ../dist
python3 ../scripts/prepare_aria2.py
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
    GOOS="$goos" GOARCH="$goarch" CGO_ENABLED=0 go build -tags bundled_aria2 \
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

每个平台只需下载对应的一个更新器放到实例根目录，无需再下载或摆放 `aria2c`。正式发行只提供上述六个更新器，不再提供额外引擎文件或重复命名的更新器。运行时先校验内嵌引擎 SHA-256，再释放到更新器目录内的进程私有目录，授予 0700 权限，并执行限时版本检查。目录不可写或禁止执行时依次尝试当前目录、用户缓存目录和系统临时目录；失败的释放立即清理，成功的引擎在正常退出时清理。内嵌版本优先，未经固定摘要校验的外置文件不会执行。Windows/macOS 同样内嵌各自平台引擎，不需要额外组件。不带 `bundled_aria2` 的开发构建缺少引擎时会解释原因并停止文件下载。

Linux 示例：

~~~sh
chmod +x /opt/mc/update-dimensionworks-server-linux-amd64
/opt/mc/update-dimensionworks-server-linux-amd64
~~~

Linux/macOS 下载后如更新器没有可执行权限，先赋予更新器执行权限。更新器会对 aria2 文件做 SHA-256 校验，再授予本地执行权限。更新器会检查实例标记并要求确认游戏、启动器或服务端已退出。不支持的系统架构不会发布对应更新器。

不带参数时更新到 GitHub Latest Release，也可使用 --release vX.Y.Z、--package <本地完整包路径>、--url <完整包 URL> 或 --help。

自动更新时会读取当前版本与目标版本之间的发布清单，沿 `base` 关系统计可用的增量包数量和每个包的大小，再与目标完整包大小比较，选择传输总量较小的方案。首次运行、没有可信签名状态时，会从实例根目录的 `VERSION` 读取已安装版本，并扫描实际文件哈希；`VERSION` 仅用于定位增量基线，不作为删除旧文件的依据。历史 TSV 只读取已安装版本与目标版本之间的必要清单，找到基线后立即停止。缺少有效版本、同版本缺失文件修复、降级或增量链越过已安装版本时直接使用完整包，不再一路追溯早期发行版。版本链不完整、增量包缺失或增量包校验失败时，更新器会改用完整包。跨多个版本应用增量包时，会按版本顺序叠加暂存文件，叠加完成后再统一校验目标版本的文件摘要；中间版本的 README 等文件不会提前按最终版本哈希校验，最终摘要不符仍会拒绝应用；用过的增量 ZIP 与本次分段缓存会在应用前清理，整个事务结束后临时目录也会删除。

查询 GitHub Latest Release 或指定 Release 信息时会先直连 GitHub API；直连失败后自动依次尝试 `ghproxy.net` 与 `gh-proxy.com`。下载 GitHub Release 资产时会先直连 GitHub；直连失败后自动依次尝试 `ghproxy.net` 与 `gh-proxy.com`。更新器会为每次连接设置有限的连接与 TLS 握手超时，失败时及时切换镜像。该回退覆盖最新版本清单、指定版本清单、增量包及完整包，其他域名的 URL 不会经过这些镜像。下载期间按 `s` 并回车可取消当前正在尝试的来源并立即切换到下一个；若同时有多个远程文件下载，操作会跳过最近开始尝试的来源。各来源会显示编号和失败原因。

aria2 下载直接发起 GET，不再强制先执行 5 秒超时的 HEAD 预检；不支持 HEAD 或 HEAD 很慢的来源仍可正常下载。RPC 模式根据终态确认完成，再回收引擎进程，避免下载完成后一直等待进程退出。远程大小限制在下载进度和落盘文件中核对，最终仍校验发布清单摘要。

aria2c 使用 Motrix 同源的 aria2 1.36.0 引擎：每个文件最多 16 个连接/分段、最小分段 1 MiB，使用系统 DNS，并按 Go HTTP 客户端的规则选择环境代理。分段、续传或 HTTP 兼容性错误使用同一内嵌引擎单连接重试一次；网络超时、DNS 和证书错误直接进入来源切换。每个来源按实际进度计算剩余时间，一旦估算超过 1 小时就取消并换源；已有进度停止增长超过 5 秒视为无有限 ETA，换源。大小或进度未知时不会猜测 ETA，仍受连接和传输超时限制。各次尝试重新下载当前文件，避免复用没有可信控制文件的部分内容。

引擎释放、启动、RPC 和下载任务分别报告错误原因与处理建议，保留经过 URL、令牌和终端控制字符清理的有限原始信息。错误会说明 DNS、连接超时、HTTP、认证、证书、磁盘空间、写入权限、架构和系统依赖等原因；发行 API 错误不会归为 aria2 引擎失败。本地引擎或磁盘错误会停止下载，避免重复尝试无法解决本地问题的镜像。RPC 启动期间监视进程退出，保留退出输出；RPC 连续异常超过 5 秒回收进程，避免卡死。终端进度从 aria2 的 `completedLength`/`totalLength` 状态读取，RPC 仅绑定本机回环地址，并使用每次下载随机生成的令牌。下载大小和最终摘要仍由更新器验证。发布资产同时包含 GPL 许可证和第三方源码包，版权信息见 [`third_party/aria2/NOTICE.md`](third_party/aria2/NOTICE.md)。

## 安全与数据保护

更新器校验更新清单、相对路径与文件摘要，拒绝压缩包路径穿越、符号链接、重复或大小写冲突路径，并在写入前暂存和备份文件；应用失败时尝试回滚。aria2c 负责分段下载与 HTTP 兼容处理，下载体不启用 gzip；更新器根据 RPC 进度和落盘大小执行上限检查，并在应用前校验发布清单摘要。连接超时为 10 秒，传输空闲超时为 60 秒，每次引擎尝试最多重试 3 次；每个来源另有 ETA 超一小时切源规则。RPC 连续故障和用户跳过都会取消并回收引擎。下载期间使用统一进度面板汇总并发任务，显示总体进度、已下载/总大小、速度、预计剩余时间、活跃下载数与当前文件；换源和单连接重试会重置尝试计数。替换文件时使用同目录随机临时文件，并在 macOS/Linux 上保留归档文件的可执行权限。

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
- 检测到已有更新锁时，会询问用户：输入 Y 清理旧锁并继续，输入 N 或其他内容取消；更新器不检查 PID 或进程状态；确认流程由操作系统文件锁串行化，避免两个更新器同时清理同一把锁。锁确认与普通提示统一由同一个控制台输入线程读取，避免输入 Y 后被另一个读取者抢走而卡住。新锁带有随机所有者标识，旧进程退出时不会误删后来创建的新锁。
- HTTP 清单与发行信息读取有独立大小上限；下载体在写入时执行大小限制，ZIP 展开同时校验实际展开长度，避免仅依赖 ZIP 头部声明。
- 本地状态清单使用 HMAC-SHA256 签名，单独修改 state.tsv 会被检测并拒绝更新；旧版无签名状态会被视为不可信并重新扫描本地文件，避免沿用伪造的旧文件删除记录。签名密钥保存在 `.dimensionworks-update/state.key`，请与实例备份一起保留。 该机制用于发现状态文件单独损坏或被修改；无法防御拥有相同账户权限、同时替换状态文件和密钥的本机攻击者。
- aria2c 每次尝试重新下载当前文件，不复用缺少可信控制状态的部分内容；所有文件仍需通过发布清单摘要校验。
- 默认拒绝版本降级；确需降级时显式使用 `--allow-downgrade`。
