# Go 跨平台更新器

本目录包含 DimensionWorks 客户端与服务端的统一 Go 更新器。目录名沿用历史命名；同一份源码可构建为 Windows、Linux 与 macOS 原生程序。正式发布的 Windows amd64、Linux amd64、macOS arm64 客户端与服务端共六个更新器，全部为内嵌 aria2c 的单文件程序。文件下载仅使用 aria2c，不自动切换 Go 下载器；发行信息查询仍使用 Go HTTP 客户端。玩家不需要额外安装 aria2、curl、wget、Python 或 Java。

## 本地测试

~~~sh
go test ./...
# 按下方步骤准备 aria2-runtime 后，验证内置引擎与竞态
go test -tags bundled_aria2 ./...
go test -race -tags bundled_aria2 ./...
# 按需在当前代理/TUN 环境验证 GitHub 下载，不允许镜像回退
DIMENSIONWORKS_TEST_GITHUB=1 go test -tags bundled_aria2 -run '^TestAria2EnvironmentGitHubDownload$' ./...
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

每次运行在恢复未完成事务后，仅从更新器所在游戏根目录的 `VERSION` 读取当前安装版本；有无签名状态都使用同一读取入口。当前版本显示、降级判断、增量基线和成功结果均以这个文件为准，`state.tsv`、更新清单、`modrinth.index.json`、模组与启动器版本名均不提供本地版本。文件必须是 UTF-8 编码、最多 64 字节的普通文件，支持开头的 UTF-8 BOM、首尾空格/制表符和 LF/CRLF；内容严格为 `x.x.x`，禁止前缀、前导零和后缀，每段最大 `2147483647`。缺失、不可读或非法时报告实际路径并中止，不猜测版本、不回退为 `0.0.0`；文件明确写入 `0.0.0` 时才使用该值。

自动更新读取当前版本与目标版本之间的发布清单，沿 `base` 关系统计增量包数量和大小，与目标完整包比较后选择传输总量较小的方案。签名状态仅保存受管理文件及摘要，用于文件筛选、备份和删除保护；首次运行或无签名状态时扫描实际文件摘要，并跳过旧文件删除。历史 TSV 只读取根目录 `VERSION` 与目标之间的必要清单，找到基线后停止。同版本修复、降级或增量链越过已安装版本时使用完整包。版本链不完整、增量包缺失或校验失败时改用完整包。跨版本增量按顺序叠加暂存，完成后统一校验目标文件摘要；中间版本文件不提前按最终摘要校验。用过的增量 ZIP 与分段缓存在应用前清理，整个事务结束后删除临时目录。

应用文件前重新核对根目录 `VERSION`，期间版本变化则中止并要求重新运行。新 `VERSION` 在与更新文件、签名状态相同的可回滚事务中最后写入，完成后从根目录重新读取并报告版本；下载、校验或应用失败不会提前标记为目标版本。完整约束见 [`root-version-authority.md`](../docs/design/root-version-authority.md)。

CurseForge 模组文件使用 HMCL 风格的候选来源规则：保留 edge.forgecdn.net 官方 URL 为首选，并将相同路径映射到 mod.mcimirror.top 作为备用来源；官方源失败（包括 404）后才尝试镜像。该规则位于所有平台共用的 Go 下载实现中，因此 Windows、Linux、macOS 客户端更新器行为一致；正式发布工作流从同一源码构建三个客户端二进制。下载完成后仍按更新清单摘要校验，不因切换镜像降低完整性要求。当前更新器主要下载整包/增量包与更新清单，不会逐条解析 modrinth.index.json 来单独更新模组；该索引仍由 Modrinth 兼容启动器在安装 MRPack 时处理。\n\n查询 GitHub Latest Release 或指定 Release 信息时会先直连 GitHub API；直连失败后自动依次尝试 `ghproxy.net` 与 `gh-proxy.com`。下载 GitHub Release 资产时，首次下载先直连 GitHub，失败后依次尝试 `ghproxy.net` 与 `gh-proxy.com`；三路均失败时，再通过 GitHub Release API 查找同名资产并解析签名下载地址做最后一次回退，避免 GitHub Release 边缘节点持续返回 `502/504` 时更新直接中止。本次更新后续文件优先使用最近成功的来源，避免每个清单、模组和增量包都重新等待不可用的直连。首选来源失败时仍尝试其余来源；来源偏好仅保存在当前进程中，下次启动重新选择。更新器会为每次连接设置有限的连接与 TLS 握手超时，失败时及时切换镜像。该回退覆盖最新版本清单、指定版本清单、增量包及完整包，其他域名的 URL 不会经过这些镜像。下载期间按 `s` 并回车可取消当前正在尝试的来源并立即切换到下一个；若同时有多个远程文件下载，操作会跳过最近开始尝试的来源。各来源会显示编号和失败原因。

aria2 下载直接发起 GET，不再强制先执行 5 秒超时的 HEAD 预检；不支持 HEAD 或 HEAD 很慢的来源仍可正常下载。RPC 模式根据终态确认完成，再回收引擎进程，避免下载完成后一直等待进程退出。远程大小限制在下载进度和落盘文件中核对，最终仍校验发布清单摘要。

aria2c 使用 Motrix 同源的 aria2 1.36.0 引擎：每个文件最多 16 个连接/分段、最小分段 1 MiB。Linux 更新器始终使用引擎异步 DNS，避免已证实的系统解析器段错误；Windows 与 macOS 按目标主机自动选择 DNS，本机、局域网及企业保留域名使用系统解析器，普通远程主机使用引擎异步 DNS。该判断不按 GitHub 域名硬编码；异步 DNS 在运行时读取电脑配置的 DNS，并遵循系统路由或 TUN。代理按 Go HTTP 客户端的规则选择。明确的分段、续传、HTTP 兼容性错误或连接提前结束（EOF）使用同一内嵌引擎单连接重试一次；连接被拒绝、网络超时、DNS、证书和未分类错误直接进入来源切换。每个来源按实际新增进度计算剩余时间，一旦估算超过 1 小时就取消并换源；短暂停顿无法计算 ETA，交由引擎的 60 秒传输空闲超时处理，不再把 5 秒停顿误报为剩余超过 1 小时。大小或进度未知时不会猜测 ETA，仍受连接和传输超时限制。各次尝试重新下载当前文件，避免复用没有可信控制文件的部分内容。

引擎释放、启动、RPC 和下载任务分别报告错误原因与处理建议，保留经过 URL、令牌和终端控制字符清理的有限原始信息。错误会说明 DNS、连接超时、HTTP、认证、证书、磁盘空间、写入权限、架构和系统依赖等原因；发行 API 错误不会归为 aria2 引擎失败。本地引擎或磁盘错误会停止下载，避免重复尝试无法解决本地问题的镜像。RPC 启动期间监视进程退出，保留退出输出；RPC 连续异常超过 5 秒回收进程，避免卡死。终端进度从 aria2 的 `completedLength`/`totalLength` 状态读取，RPC 仅绑定本机回环地址，并使用每次下载随机生成的令牌。下载大小和最终摘要仍由更新器验证。发布资产同时包含 GPL 许可证和第三方源码包，版权信息见 [`third_party/aria2/NOTICE.md`](third_party/aria2/NOTICE.md)。

下载任务的网络或 HTTP 错误显示为“下载来源失败”，与引擎释放、启动和本机 RPC 故障区分。连接被拒绝会提示检查域名解析、网络及代理，不再作为未知错误进行单连接重试。关闭 aria2 自带控制台进度，并从原始诊断中移除重复的零字节进度行，保留错误码和真实原因。

已在 FlClash TUN 环境复现 macOS 异常：系统解析器将 `github.com` 返回为 `127.0.0.1`，请求因此进入本机回环并报连接被拒绝；同一引擎按上述远程主机规则启用异步 DNS 后，真实 GitHub 清单下载成功。该方式不固定 IP、不关闭证书校验。TUN 模式由系统路由接管流量，无需更新器显式设置 HTTP 代理。显式代理目前读取 `HTTP_PROXY`、`HTTPS_PROXY` 与 `NO_PROXY` 环境变量；环境变量为空不代表 TUN 未开启。

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

Windows 下载保持 HTTPS 证书校验开启。更新器通过系统 Windows PowerShell 导出本机及当前用户受信任根证书到临时 PEM，传给内嵌 aria2；下载结束后删除。不将中间证书提升为信任根，导出失败明确报错。用于解决 OpenSSL 无法直接读取 Windows 证书库的问题，尚未进行 Windows 实机验证。

跨平台证书策略：macOS 固定引擎使用 Apple Security/钥匙串，由系统验证证书链及主机名，不传不受支持的 PEM 参数。Linux OpenSSL 引擎显式读取 Debian/Ubuntu、RHEL/Fedora、SUSE、Alpine 常见系统 CA 文件，可使用绝对路径 SSL_CERT_FILE；证书缺失或无效明确报错，建议安装 ca-certificates。三个平台始终保持 --check-certificate=true；发行 API 的 Go HTTP 客户端同样保持系统证书及主机名校验。上述结论来自源码及二进制静态审查，不代表所有玩家环境均已实机验证。
