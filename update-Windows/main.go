package main

import (
	"archive/zip"
	"bufio"
	"bytes"
	"context"
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"crypto/sha512"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/url"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"runtime"
	"sort"
	"strconv"
	"strings"
	"sync"
	"sync/atomic"
	"time"
)

var defaultKind string
var diagnosticRoot string
var diagnosticKind string
var consoleReader = bufio.NewReader(os.Stdin)
var consoleInputOnce sync.Once
var consolePromptLines = make(chan consoleLine, 8)
var consoleInputMu sync.Mutex
var activeDownloadSkips = make(map[uint64]func())
var nextDownloadSkipID atomic.Uint64
var awaitingConsolePrompt bool
var consoleReadErr error
var aria2PathOnce sync.Once
var aria2PathCache string
var aria2PathError error
var aria2RuntimeCleanup func()
var progressMu sync.Mutex
var downloadProgressUI = newProgressTracker()
var downloadCacheRoot string
var parallelDownloadLimit atomic.Int64
var errHTTPSDowngrade = errors.New("拒绝 HTTPS 下载重定向到不安全的 HTTP")

const (
	repository          = "szx-jn/DimensionWorks"
	manifestName        = "dimensionworks-update-manifest.tsv"
	stateName           = ".dimensionworks-update/state.tsv"
	stateSignatureName  = ".dimensionworks-update/state.tsv.hmac"
	stateKeyName        = ".dimensionworks-update/state.key"
	maxManifestBytes    = 16 << 20
	maxTransactionJournalBytes = 64 << 20
	maxManifestEntries  = 100000
	maxArchiveEntries   = 100000
	maxArchiveFileBytes = uint64(4 << 30)
	maxArchiveTotalBytes = uint64(16 << 30)
	maxDownloadBytes    = int64(8 << 30)
	downloadETAExceedsLimit = time.Hour
	downloadETAProbeInterval = time.Second
	maxDownloadETADuration time.Duration = 1<<63 - 1
)

var digestRE = regexp.MustCompile("^[0-9a-f]+$")

type Entry struct {
	Action    string
	Path      string
	Algorithm string
	Value     string
	Source    string
	Sources   []string
}

type Manifest struct {
	Kind    string
	Version string
	Release string
	Base    string
	Entries []Entry
	Bytes   []byte
}

type ReleaseInfo struct {
	TagName string `json:"tag_name"`
	Assets  []ReleaseAsset `json:"assets"`
}

type ReleaseAsset struct {
	Name               string `json:"name"`
	BrowserDownloadURL string `json:"browser_download_url"`
	Size               int64  `json:"size"`
}

type Archive struct {
	reader  *zip.ReadCloser
	entries map[string]*zip.File
}

type Options struct {
	Release        string
	Package        string
	URL            string
	AllowDowngrade bool
	Help           bool
}

type consoleLine struct {
	text string
	err  error
}

// A single reader owns stdin so download cancellation and ordinary prompts do
// not race to consume the same buffered input.
func startConsoleInput() {
	consoleInputOnce.Do(func() {
		go func() {
			for {
				line, err := consoleReader.ReadString('\n')
				handleDownloadInput(line, err)
				if err != nil {
					return
				}
			}
		}()
	})
}

func handleDownloadInput(line string, readErr error) {
	consoleInputMu.Lock()
	defer consoleInputMu.Unlock()
	if readErr != nil { consoleReadErr = readErr }
	var latest uint64
	for id := range activeDownloadSkips {
		if id > latest { latest = id }
	}
	if latest != 0 {
		if strings.EqualFold(strings.TrimSpace(line), "s") {
			activeDownloadSkips[latest]()
		}
		return
	}
	if awaitingConsolePrompt {
		consolePromptLines <- consoleLine{text: line, err: readErr}
	}
}

func readConsoleLine() (string, error) {
	consoleInputMu.Lock()
	if consoleReadErr != nil && len(consolePromptLines) == 0 {
		err := consoleReadErr
		consoleInputMu.Unlock()
		return "", err
	}
	awaitingConsolePrompt = true
	consoleInputMu.Unlock()
	startConsoleInput()
	line := <-consolePromptLines
	consoleInputMu.Lock()
	awaitingConsolePrompt = false
	consoleInputMu.Unlock()
	return line.text, line.err
}

func main() {
	enableUTF8Console()
	kind := defaultKind
	if kind == "" {
		name := strings.ToLower(filepath.Base(os.Args[0]))
		switch {
		case strings.Contains(name, "client"):
			kind = "client"
		case strings.Contains(name, "server"):
			kind = "server"
		}
	}
	err := run(kind, os.Args[1:])
	if aria2RuntimeCleanup != nil { aria2RuntimeCleanup() }
	if err == nil {
		fmt.Println()
		fmt.Println("操作完成。按回车键关闭窗口。")
		waitEnter()
		return
	}
	fmt.Fprintln(os.Stderr, "")
	fmt.Fprintln(os.Stderr, "更新失败：", err)
	if logPath := writeFailureLog(diagnosticRoot, err); logPath != "" {
		fmt.Fprintln(os.Stderr, "诊断日志：", logPath)
	} else {
		fmt.Fprintln(os.Stderr, "当前错误未能写入日志；请先保存上方错误信息。")
	}
	fmt.Fprintln(os.Stderr, "请检查错误信息后按回车键关闭窗口。")
	waitEnter()
	os.Exit(1)
}

func waitEnter() {
	fmt.Print("> ")
	_, _ = readConsoleLine()
}

func validateInstanceRoot(root, kind string) error {
	if kind != "client" && kind != "server" {
		return fmt.Errorf("未知更新器类型：%s", kind)
	}
	modsInfo, err := os.Lstat(filepath.Join(root, "mods"))
	if err != nil || modsInfo.Mode()&os.ModeSymlink != 0 || !modsInfo.IsDir() {
		return fmt.Errorf("未在 %s 找到 mods/ 目录。请把 update-dimensionworks-%s.exe 放到对应客户端或服务端实例根目录后再运行；不要直接从下载目录运行", root, kind)
	}
	markers := []string{"VERSION", "config"}
	if kind == "server" {
		markers = []string{"VERSION", "libraries", "start.sh", "start.bat"}
	}
	found := false
	for _, marker := range markers {
		info, statErr := os.Lstat(filepath.Join(root, marker))
		if statErr == nil && info.Mode()&os.ModeSymlink == 0 && ((marker == "config" || marker == "libraries") && info.IsDir() || (marker != "config" && marker != "libraries") && info.Mode().IsRegular()) {
			found = true
			break
		}
	}
	if !found {
		return fmt.Errorf("目录 %s 缺少 %s 更新器所需的实例标记；请确认 EXE 已放入正确的实例根目录", root, kind)
	}
	return nil
}

func writeFailureLog(root string, cause error) string {
	if root == "" {
		return ""
	}
	rootInfo, err := os.Lstat(root)
	if err != nil || !rootInfo.IsDir() || rootInfo.Mode()&os.ModeSymlink != 0 {
		return ""
	}
	updateRoot := filepath.Join(root, ".dimensionworks-update")
	if info, statErr := os.Lstat(updateRoot); statErr == nil {
		if info.Mode()&os.ModeSymlink != 0 || !info.IsDir() {
			return ""
		}
	} else if os.IsNotExist(statErr) {
		if os.Mkdir(updateRoot, 0755) != nil {
			return ""
		}
	} else {
		return ""
	}
	logDir := filepath.Join(updateRoot, "logs")
	if info, statErr := os.Lstat(logDir); statErr == nil {
		if info.Mode()&os.ModeSymlink != 0 || !info.IsDir() {
			return ""
		}
	} else if os.IsNotExist(statErr) {
		if err := os.Mkdir(logDir, 0700); err != nil {
			return ""
		}
	} else {
		return ""
	}
	logPath := filepath.Join(logDir, "failed-"+time.Now().Format("20060102-150405.000000000")+"-"+fmt.Sprint(os.Getpid())+".log")
	contents := fmt.Sprintf("time=%s\nplatform=%s/%s\nkind=%s\ninstance=%s\nerror=%s\n",
		time.Now().Format(time.RFC3339), runtime.GOOS, runtime.GOARCH, diagnosticKind, root, cause)
	file, err := os.OpenFile(logPath, os.O_CREATE|os.O_EXCL|os.O_WRONLY, 0600)
	if err != nil { return "" }
	_, writeErr := file.Write([]byte(contents))
	syncErr := file.Sync()
	closeErr := file.Close()
	if writeErr != nil || syncErr != nil || closeErr != nil {
		_ = os.Remove(logPath)
		return ""
	}
	return logPath
}
func acquireUpdateLock(lockPath string, input *bufio.Reader, output io.Writer) (func(), error) {
	create := func() (string, error) {
		var nonce [16]byte
		if _, err := rand.Read(nonce[:]); err != nil { return "", fmt.Errorf("生成更新锁标识失败：%w", err) }
		token := hex.EncodeToString(nonce[:])
		file, err := os.OpenFile(lockPath, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0600)
		if err != nil { return "", err }
		_, writeErr := fmt.Fprintf(file, "token=%s\npid=%d\ntime=%s\n", token, os.Getpid(), time.Now().Format(time.RFC3339))
		syncErr := file.Sync()
		closeErr := file.Close()
		if writeErr != nil || syncErr != nil || closeErr != nil {
			_ = os.Remove(lockPath)
			if writeErr != nil { return "", writeErr }
			if syncErr != nil { return "", syncErr }
			return "", closeErr
		}
		return token, nil
	}
	token, err := create()
	if err == nil { return lockReleaseFunc(lockPath, token), nil }
	if !os.IsExist(err) { return nil, fmt.Errorf("无法创建更新锁：%w", err) }

	guardPath := lockPath + ".confirm"
	releaseGuard, guardErr := acquireConfirmationGuard(guardPath)
	if guardErr != nil { return nil, guardErr }
	defer releaseGuard()

	initialInfo, statErr := os.Lstat(lockPath)
	if statErr != nil { return nil, fmt.Errorf("读取现有更新锁失败：%w", statErr) }
	fmt.Fprintln(output, "检测到更新锁 .dimensionworks-update/lock。")
	fmt.Fprint(output, "上一个更新进程是否仍在运行？只有确认它已经退出时才输入 Y（清理残留锁并继续更新）；输入 N 取消更新：")
	var answer string
	var readErr error
	if input == nil {
		answer, readErr = readConsoleLine()
	} else {
		answer, readErr = input.ReadString('\n')
	}
	if readErr != nil && len(answer) == 0 { return nil, fmt.Errorf("无法读取更新锁确认：%w", readErr) }
	if !strings.EqualFold(strings.TrimSpace(answer), "y") {
		return nil, errors.New("用户选择不清理更新锁，已拒绝更新")
	}
	currentInfo, statErr := os.Lstat(lockPath)
	if statErr != nil { return nil, fmt.Errorf("读取更新锁失败：%w", statErr) }
	if !os.SameFile(initialInfo, currentInfo) {
		return nil, errors.New("更新锁在确认期间已被替换，拒绝删除新锁")
	}
	if currentInfo.Mode()&os.ModeSymlink != 0 || !currentInfo.Mode().IsRegular() {
		return nil, errors.New("更新锁不是普通文件，拒绝自动删除")
	}
	if err := os.Remove(lockPath); err != nil { return nil, fmt.Errorf("清理旧更新锁失败：%w", err) }
	token, err = create()
	if err != nil {
		if os.IsExist(err) { return nil, errors.New("更新锁已被另一个更新进程重新创建，本次更新已取消") }
		return nil, fmt.Errorf("清理旧锁后无法创建新更新锁：%w", err)
	}
	return lockReleaseFunc(lockPath, token), nil
}

func lockReleaseFunc(lockPath, token string) func() {
	return func() {
		data, err := os.ReadFile(lockPath)
		if err != nil { return }
		if !strings.Contains(string(data), "token="+token+"\n") { return }
		_ = os.Remove(lockPath)
	}
}

func run(kind string, args []string) error {
	opts, err := parseOptions(args)
	if err != nil {
		return err
	}
	if opts.Help {
		printUsage(kind)
		return nil
	}
	if kind != "client" && kind != "server" {
		return errors.New("该程序未指定 client/server 构建类型")
	}
	if runtime.GOOS != "windows" {
		fmt.Println("运行平台：", runtime.GOOS)
	}
	executable, err := os.Executable()
	if err != nil {
		return fmt.Errorf("无法定位更新程序：%w", err)
	}
	root, err := filepath.Abs(filepath.Dir(executable))
	if err != nil {
		return fmt.Errorf("无法定位实例目录：%w", err)
	}
	if info, err := os.Lstat(root); err != nil || info.Mode()&os.ModeSymlink != 0 {
		return errors.New("实例目录不存在或是符号链接")
	}
	if err := validateInstanceRoot(root, kind); err != nil {
		return err
	}
	diagnosticRoot = root
	diagnosticKind = kind
	fmt.Println("DimensionWorks", kind, "更新器")
	fmt.Println("实例目录：", root)
	fmt.Print("请确认该目录是正确的整合包根目录，且 Minecraft/服务端已退出。输入 Y 继续，其他输入取消：")
	answer, readErr := readConsoleLine()
	if readErr != nil && len(answer) == 0 {
		return fmt.Errorf("无法读取确认输入：%w", readErr)
	}
	if !strings.EqualFold(strings.TrimSpace(answer), "y") {
		return errors.New("用户取消了更新")
	}
	return updateInstance(root, kind, opts)
}

func updateInstance(root, kind string, opts Options) error {
	updateRoot := filepath.Join(root, ".dimensionworks-update")
	if err := os.MkdirAll(updateRoot, 0755); err != nil {
		return fmt.Errorf("无法创建更新目录：%w", err)
	}
	if info, err := os.Lstat(updateRoot); err != nil || info.Mode()&os.ModeSymlink != 0 || !info.IsDir() {
		return errors.New(".dimensionworks-update 必须是普通目录，不能是符号链接")
	}
	previousCacheRoot := downloadCacheRoot
	downloadCacheRoot = filepath.Join(updateRoot, "downloads")
	defer func() { downloadCacheRoot = previousCacheRoot }()
	if err := os.MkdirAll(downloadCacheRoot, 0700); err != nil {
		return fmt.Errorf("无法创建断点续传目录：%w", err)
	}
	if info, err := os.Lstat(downloadCacheRoot); err != nil || info.Mode()&os.ModeSymlink != 0 || !info.IsDir() {
		return errors.New("下载缓存目录不安全")
	}
	lockPath := filepath.Join(updateRoot, "lock")
	releaseLock, err := acquireUpdateLock(lockPath, nil, os.Stdout)
	if err != nil {
		return err
	}
	defer releaseLock()

	if err := cleanupDownloadCache(downloadCacheRoot, 30*24*time.Hour); err != nil {
		return fmt.Errorf("清理过期下载缓存失败：%w", err)
	}
	if err := recoverInterruptedTransactions(root, updateRoot); err != nil {
		return fmt.Errorf("发现未完成的更新事务，自动恢复失败：%w。请勿删除 .dimensionworks-update 下的恢复数据", err)
	}
	installedVersion, err := readInstalledVersion(root)
	if err != nil {
		return err
	}
	fmt.Printf("[版本] %s：%s\n", filepath.Join(root, "VERSION"), installedVersion)

	work, err := os.MkdirTemp(updateRoot, "work-")
	if err != nil {
		return fmt.Errorf("无法创建事务目录：%w", err)
	}
	keepWork := false
	defer func() {
		if !keepWork {
			_ = os.RemoveAll(work)
		}
	}()

	var target *Manifest
	var fullPackage *Archive
	var fullPackageURL string
	var releaseInfo *ReleaseInfo
	releaseTag := ""

	switch {
	case opts.Package != "":
		packagePath, err := filepath.Abs(opts.Package)
		if err != nil {
			return fmt.Errorf("本地包路径无效：%w", err)
		}
		fullPackage, err = openArchive(packagePath)
		if err != nil {
			return fmt.Errorf("无法打开本地更新包：%w", err)
		}
		defer fullPackage.Close()
		target, err = embeddedManifest(fullPackage, kind)
		if err != nil {
			return err
		}
		releaseTag = target.Release
	case opts.URL != "":
		fullPackageURL = opts.URL
		manifestURL, err := manifestURLFromPackageURL(opts.URL)
		if err != nil {
			return err
		}
		manifestBytes, err := downloadBytes(manifestURL, filepath.Join(work, "manifest.tsv"))
		if err != nil {
			return fmt.Errorf("下载更新清单失败：%w；可重新运行并使用 --package 指定已下载的完整包", err)
		}
		target, err = parseManifest(manifestBytes, kind)
		if err != nil {
			return err
		}
		releaseTag = target.Release
	case opts.Release != "":
		releaseTag = opts.Release
		if !validReleaseTag(releaseTag) {
			return fmt.Errorf("发行标签格式无效：%s", releaseTag)
		}
		version := strings.TrimPrefix(releaseTag, "v")
		manifestURL := assetURL(releaseTag, manifestAssetName(kind, version))
		manifestBytes, err := downloadBytes(manifestURL, filepath.Join(work, "manifest.tsv"))
		if err != nil {
			return fmt.Errorf("下载更新清单失败：%w", err)
		}
		target, err = parseManifest(manifestBytes, kind)
		if err != nil {
			return err
		}
		if target.Release != releaseTag {
			return fmt.Errorf("清单发行标签 %s 与请求的 %s 不一致", target.Release, releaseTag)
		}
	default:
		release, err := latestRelease()
		if err != nil {
			return err
		}
		releaseTag = release.TagName
		releaseInfo = &release
		version := strings.TrimPrefix(releaseTag, "v")
		manifestAsset := manifestAssetName(kind, version)
		var manifestURL string
		for _, asset := range release.Assets {
			if asset.Name == manifestAsset {
				manifestURL = asset.BrowserDownloadURL
				break
			}
		}
		if manifestURL == "" {
			manifestURL = assetURL(releaseTag, manifestAsset)
		}
		manifestBytes, err := downloadBytes(manifestURL, filepath.Join(work, "manifest.tsv"))
		if err != nil {
			return fmt.Errorf("下载最新版本清单失败：%w", err)
		}
		target, err = parseManifest(manifestBytes, kind)
		if err != nil {
			return err
		}
		if target.Release != releaseTag {
			return fmt.Errorf("最新发行标签 %s 与清单 %s 不一致", releaseTag, target.Release)
		}
	}

	if target.Kind != kind {
		return fmt.Errorf("更新清单类型错误：需要 %s，实际为 %s", kind, target.Kind)
	}
	if fullPackage != nil {
		pkgManifest, err := embeddedManifest(fullPackage, kind)
		if err != nil {
			return err
		}
		if pkgManifest.Version != target.Version || !bytes.Equal(pkgManifest.Bytes, target.Bytes) {
			return errors.New("本地更新包中的清单与目标清单不一致")
		}
		if kind == "client" {
			if _, err := validateClientMRPackIndex(fullPackage, target); err != nil {
				return fmt.Errorf("MRPack 索引校验失败：%w", err)
			}
		}
	}

	statePath := filepath.Join(root, filepath.FromSlash(stateName))
	current, err := readState(root, statePath, kind)
	if err != nil {
		return fmt.Errorf("现有更新状态无效：%w。请备份并检查 %s", err, statePath)
	}
	legacyState := current == nil
	if current == nil {
		current, err = scanInstalledFiles(root, kind)
		if err != nil {
			return fmt.Errorf("无法初始化现有文件状态：%w", err)
		}
	}
	fmt.Printf("当前版本：%s；目标版本：%s（%s）\n", installedVersion, target.Version, target.Release)
	if comparison, compareErr := compareVersions(target.Version, installedVersion); compareErr != nil {
		return fmt.Errorf("无法比较版本号：%w", compareErr)
	} else if comparison < 0 && !opts.AllowDowngrade {
		return fmt.Errorf("拒绝将版本从 %s 降级到 %s；如确认需要降级，请显式添加 --allow-downgrade", installedVersion, target.Version)
	}

	// Client MRPack archives contain the authoritative file index and overrides,
	// not every third-party mod JAR. Download the archive first, then use its
	// index to validate the target manifest and fetch only files whose hashes differ.
	if kind == "client" && fullPackage == nil {
		if fullPackageURL == "" {
			fullPackageURL = assetURL(target.Release, fullPackageName(kind, target.Version))
		}
		packagePath := filepath.Join(work, "full-package.mrpack")
		if size, sizeErr := remoteContentLength(fullPackageURL); sizeErr == nil && size > 0 {
			if spaceErr := ensureDiskSpace(root, saturatingAdd(uint64(size), 256<<20)); spaceErr != nil {
				return fmt.Errorf("下载完整 MRPack 前磁盘空间不足：%w", spaceErr)
			}
		}
		fmt.Println("[下载] 获取完整 MRPack 文件索引与配置覆盖包：", filepath.Base(fullPackageURL))
		if err := downloadArchiveWithManualFallback(fullPackageURL, packagePath); err != nil {
			return err
		}
		fullPackage, err = openArchive(packagePath)
		if err != nil {
			return fmt.Errorf("MRPack 无效：%w", err)
		}
		defer fullPackage.Close()
		pkgManifest, err := embeddedManifest(fullPackage, kind)
		if err != nil {
			return err
		}
		if pkgManifest.Version != target.Version || pkgManifest.Release != target.Release ||
			!bytes.Equal(pkgManifest.Bytes, target.Bytes) {
			return errors.New("MRPack 内嵌更新清单与目标清单不一致")
		}
		indexedFiles, err := validateClientMRPackIndex(fullPackage, target)
		if err != nil {
			return fmt.Errorf("MRPack 索引校验失败：%w", err)
		}
		fmt.Printf("[MRPack] 已核对 %d 个客户端文件；接下来只下载本地缺失或摘要不同的文件。\n", indexedFiles)
	}

	selected, deletions, err := selectChanges(root, kind, current, target)
	if err != nil {
		return err
	}
	if legacyState && len(deletions) > 0 {
		fmt.Println("[保护] 首次建立签名状态时跳过旧文件清理，避免误删无法验证归属的本地文件")
		deletions = nil
	}
	backupBytes, err := estimateExistingBytes(root, selected)
	if err != nil { return fmt.Errorf("估算本地备份空间失败：%w", err) }
	deletionBackupBytes, err := estimateDeletionBytes(root, deletions)
	if err != nil { return fmt.Errorf("估算待删除文件备份空间失败：%w", err) }
	backupBytes = saturatingAdd(backupBytes, deletionBackupBytes)
	remoteBytes := uint64(0)
	for _, entry := range selected {
		if entry.Action == "remote" {
			if size, sizeErr := remoteContentLength(entry.Source); sizeErr == nil && size > 0 {
				remoteBytes = saturatingAdd(remoteBytes, uint64(size))
			}
		}
	}
	hasArchiveChanges := false
	for _, entry := range selected { if entry.Action == "archive" { hasArchiveChanges = true; break } }
	transferPlan := TransferPlan{}
	if fullPackage == nil && hasArchiveChanges {
		transferPlan = planTransfer(kind, installedVersion, target, releaseInfo, fullPackageURL, work)
		fullPackageURL = transferPlan.FullPackageURL
		printTransferPlan(installedVersion, target, transferPlan)
		packageDiskBytes := transferPlan.FullPackageSize
		if transferPlan.UseIncrementals {
			packageDiskBytes = largestIncrementalSize(transferPlan.Incrementals)
		}
		remoteBytes = saturatingAdd(remoteBytes, packageDiskBytes)
	}
	if err := ensureDiskSpace(root, saturatingAdd(saturatingAdd(saturatingAdd(backupBytes, backupBytes), remoteBytes), 256<<20)); err != nil {
		return fmt.Errorf("磁盘空间预检失败：%w", err)
	}
	userBackup, err := backupModifiedFiles(root, updateRoot, current, selected, legacyState)
	if err != nil {
		return fmt.Errorf("无法备份用户修改文件，已中止更新：%w", err)
	}
	if userBackup != "" {
		fmt.Println("[保护] 已备份被更新覆盖的本地修改：", userBackup)
	}
	if kind == "client" {
		selectedRemote := 0
		totalRemote := 0
		for _, entry := range target.Entries {
			if entry.Action == "remote" {
				totalRemote++
			}
		}
		for _, entry := range selected {
			if entry.Action == "remote" {
				selectedRemote++
			}
		}
		fmt.Printf("[定点更新] MRPack 中 %d 个模组/资源文件，仅需下载 %d 个；%d 个保持原样。\n",
			totalRemote, selectedRemote, totalRemote-selectedRemote)
	}
	fmt.Printf("待更新文件：%d；待清理旧文件：%d\n", len(selected), len(deletions))
	stageRoot := filepath.Join(work, "stage")
	if err := os.MkdirAll(stageRoot, 0755); err != nil {
		return err
	}

	remoteEntries := make([]Entry, 0)
	archiveEntries := make([]Entry, 0)
	for _, entry := range selected {
		if entry.Action == "remote" {
			remoteEntries = append(remoteEntries, entry)
		} else {
			archiveEntries = append(archiveEntries, entry)
		}
	}
	// 多线程下载远程文件，限制并发数以避免源站限流。
	if len(remoteEntries) > 0 {
		const maxConcurrentDownloads = 4
		workers := maxConcurrentDownloads
		if len(remoteEntries) < workers { workers = len(remoteEntries) }
		parallelDownloadLimit.Store(int64(parallelDownloadWorkers / workers))
		jobs := make(chan Entry)
		errs := make(chan error, len(remoteEntries))
		var wg sync.WaitGroup
		for i := 0; i < workers; i++ {
			wg.Add(1)
			go func() {
				defer wg.Done()
				for entry := range jobs {
					stage, stageErr := stagePath(stageRoot, entry.Path)
					if stageErr != nil { errs <- stageErr; continue }
					progressMu.Lock()
					fmt.Printf("[下载] 远程文件：%s\n", entry.Path)
					progressMu.Unlock()
					if downloadErr := downloadVerifiedEntry(entry, stage); downloadErr != nil {
						errs <- fmt.Errorf("%s 下载或摘要校验失败：%w", entry.Path, downloadErr)
					}
				}
			}()
		}
		for _, entry := range remoteEntries { jobs <- entry }
		close(jobs)
		wg.Wait()
		parallelDownloadLimit.Store(parallelDownloadWorkers)
		close(errs)
		for downloadErr := range errs { if downloadErr != nil { return downloadErr } }
	}

	deltaUsed := false
	if len(archiveEntries) > 0 && fullPackage == nil && transferPlan.UseIncrementals {
		fmt.Printf("[下载] 开始获取 %d 个增量包，总计 %s\n", len(transferPlan.Incrementals), humanSize(int64(transferPlan.IncrementalSize)))
		if err := stageIncrementalPackages(root, work, stageRoot, transferPlan.Incrementals, archiveEntries, backupBytes); err == nil {
			deltaUsed = true
		} else {
			clearStaged(stageRoot, archiveEntries)
			_ = os.RemoveAll(filepath.Join(work, "incremental-downloads"))
			fmt.Println("[回退] 增量包链不可用或校验失败，将改用完整包：", err)
		}
	}

	if len(archiveEntries) > 0 && !deltaUsed {
		if fullPackage == nil {
			if fullPackageURL == "" {
				fullPackageURL = assetURL(target.Release, fullPackageName(kind, target.Version))
			}
			packagePath := filepath.Join(work, "full-package.zip")
			fmt.Println("[下载] 获取完整更新包：", filepath.Base(fullPackageURL))
			if err := downloadArchiveWithManualFallback(fullPackageURL, packagePath); err != nil {
				return err
			}
			fullPackage, err = openArchive(packagePath)
			if err != nil {
				return fmt.Errorf("完整更新包无效：%w", err)
			}
			defer fullPackage.Close()
			pkgManifest, err := embeddedManifest(fullPackage, kind)
			if err != nil {
				return err
			}
			if pkgManifest.Version != target.Version || pkgManifest.Release != target.Release ||
				!bytes.Equal(pkgManifest.Bytes, target.Bytes) {
				return errors.New("完整更新包中的清单与目标清单不一致")
			}
		}
		unpackedBytes, sizeErr := estimateArchiveBytes(fullPackage, archiveEntries)
		if sizeErr != nil { return sizeErr }
		if spaceErr := ensureDiskSpace(root, saturatingAdd(saturatingAdd(backupBytes, unpackedBytes), 256<<20)); spaceErr != nil {
			return fmt.Errorf("完整包展开前磁盘空间不足：%w", spaceErr)
		}
		if err := stageArchiveEntries(fullPackage, archiveEntries, stageRoot); err != nil {
			return fmt.Errorf("暂存完整更新包失败：%w", err)
		}
	}

	changes := make([]Change, 0, len(selected)+len(deletions)+2)
	for _, entry := range selected {
		stage, err := stagePath(stageRoot, entry.Path)
		if err != nil {
			return err
		}
		if err := verifyDigest(stage, entry.Algorithm, entry.Value); err != nil {
			return fmt.Errorf("暂存校验失败 %s：%w", entry.Path, err)
		}
		changes = append(changes, Change{Path: entry.Path, Staged: stage})
	}
	for _, stale := range deletions {
		changes = append(changes, Change{Path: stale, Delete: true})
	}
	versionPath := filepath.Join(work, "VERSION")
	if err := os.WriteFile(versionPath, []byte(target.Version+"\n"), 0644); err != nil {
		return err
	}
	stateStage, err := stagePath(stageRoot, stateName)
	if err != nil { return err }
	if err := os.MkdirAll(filepath.Dir(stateStage), 0755); err != nil { return err }
	if err := os.WriteFile(stateStage, target.Bytes, 0644); err != nil { return err }
	stateKey, err := loadOrCreateStateKey(updateRoot)
	if err != nil { return fmt.Errorf("无法创建本地状态签名密钥：%w", err) }
	stateMAC := hmac.New(sha256.New, stateKey)
	_, _ = stateMAC.Write(target.Bytes)
	stateSignature := []byte(hex.EncodeToString(stateMAC.Sum(nil)) + "\n")
	signatureStage, err := stagePath(stageRoot, stateSignatureName)
	if err != nil { return err }
	if err := os.WriteFile(signatureStage, stateSignature, 0600); err != nil { return err }
	changes = append(changes, Change{Path: stateName, Staged: stateStage})
	changes = append(changes, Change{Path: stateSignatureName, Staged: signatureStage})
	changes = append(changes, Change{Path: "VERSION", Staged: versionPath})

	if err := verifyInstalledVersion(root, installedVersion); err != nil {
		return err
	}
	if err := applyTransaction(root, work, changes); err != nil {
		keepWork = true
		return fmt.Errorf("%w；恢复数据保留在 %s", err, work)
	}
	actualVersion, err := readInstalledVersion(root)
	if err != nil {
		return fmt.Errorf("更新文件已应用，但 VERSION 核对失败：%w", err)
	}
	if actualVersion != target.Version {
		return fmt.Errorf("更新文件已应用，但游戏根目录 VERSION 为 %s，目标为 %s", actualVersion, target.Version)
	}
	fmt.Printf("更新成功：%s -> %s\n", installedVersion, actualVersion)
	if deltaUsed {
		fmt.Printf("更新来源：已校验的 %d 个增量包（%s）\n", len(transferPlan.Incrementals), humanSize(int64(transferPlan.IncrementalSize)))
	} else if len(archiveEntries) > 0 {
		fmt.Println("更新来源：完整包")
	}
	return nil
}

func saturatingAdd(a, b uint64) uint64 {
	if ^uint64(0)-a < b { return ^uint64(0) }
	return a+b
}

func estimateExistingBytes(root string, entries []Entry) (uint64, error) {
	var total uint64
	for _, entry := range entries {
		path, exists, err := securePath(root, entry.Path)
		if err != nil { return 0, err }
		if !exists { continue }
		info, err := os.Lstat(path)
		if err != nil { return 0, err }
		if info.Mode()&os.ModeSymlink != 0 || !info.Mode().IsRegular() { return 0, fmt.Errorf("目标不是普通文件：%s", entry.Path) }
		if info.Size() > 0 { total = saturatingAdd(total, uint64(info.Size())) }
	}
	return total, nil
}

func estimateDeletionBytes(root string, paths []string) (uint64, error) {
	var total uint64
	for _, rel := range paths {
		path, exists, err := securePath(root, rel)
		if err != nil { return 0, err }
		if !exists { continue }
		info, err := os.Lstat(path)
		if err != nil { return 0, err }
		if info.Mode()&os.ModeSymlink != 0 || !info.Mode().IsRegular() { return 0, fmt.Errorf("待删除目标不是普通文件：%s", rel) }
		if info.Size() > 0 { total = saturatingAdd(total, uint64(info.Size())) }
	}
	return total, nil
}

func remoteContentLength(source string) (int64, error) {
	var failures []string
	for _, candidate := range downloadCandidates(source) {
		size, err := remoteContentLengthFrom(candidate)
		if err == nil {
			return size, nil
		}
		failures = append(failures, fmt.Sprintf("%s: %v", safeURLForLog(candidate), safeErrorForLog(err)))
	}
	return 0, errors.New(strings.Join(failures, "; "))
}

func remoteContentLengthFrom(source string) (int64, error) {
	client := newHTTPClient(10*time.Second, 5*time.Second)
	req, err := http.NewRequest(http.MethodHead, source, nil)
	if err == nil {
		req.Header.Set("User-Agent", "DimensionWorks-Go-Updater/1.1")
		if resp, headErr := client.Do(req); headErr == nil {
			resp.Body.Close()
			if resp.StatusCode >= 200 && resp.StatusCode < 300 && resp.ContentLength > 0 {
				return resp.ContentLength, nil
			}
		}
	}
	probe, err := http.NewRequest(http.MethodGet, source, nil)
	if err != nil { return 0, err }
	probe.Header.Set("User-Agent", "DimensionWorks-Go-Updater/1.1")
	probe.Header.Set("Range", "bytes=0-0")
	resp, err := client.Do(probe)
	if err != nil { return 0, err }
	defer resp.Body.Close()
	if resp.StatusCode == http.StatusPartialContent {
		matches := regexp.MustCompile("^bytes 0-0/([0-9]+)$").FindStringSubmatch(resp.Header.Get("Content-Range"))
		if len(matches) == 2 {
			if total, parseErr := strconv.ParseInt(matches[1], 10, 64); parseErr == nil && total > 0 {
				_, _ = io.Copy(io.Discard, io.LimitReader(resp.Body, 2))
				return total, nil
			}
		}
	}
	return 0, fmt.Errorf("HEAD 与 Range 探测均无法确定文件大小（HTTP %d）", resp.StatusCode)
}

func ensureDiskSpace(path string, required uint64) error {
	available, err := availableDiskSpace(path)
	if err != nil { return fmt.Errorf("无法读取可用磁盘空间：%w", err) }
	if available < required {
		return fmt.Errorf("可用空间不足：当前 %s，需要至少 %s（含 256 MiB 安全余量）", humanSize(int64(minUint64(available, uint64(^uint64(0)>>1)))), humanSize(int64(minUint64(required, uint64(^uint64(0)>>1)))))
	}
	return nil
}

func minUint64(a, b uint64) uint64 { if a < b { return a }; return b }

func estimateArchiveBytes(archive *Archive, entries []Entry) (uint64, error) {
	if archive == nil { return 0, errors.New("更新归档为空") }
	var total uint64
	for _, entry := range entries {
		if entry.Action != "archive" { continue }
		file := archive.entries[strings.ToLower(entry.Source)]
		if file == nil { return 0, fmt.Errorf("归档缺少文件：%s", entry.Source) }
		total = saturatingAdd(total, file.UncompressedSize64)
	}
	return total, nil
}

func parseOptions(args []string) (Options, error) {
	var out Options
	for i := 0; i < len(args); i++ {
		switch args[i] {
		case "--help", "-h", "/?":
			out.Help = true
		case "--allow-downgrade":
			out.AllowDowngrade = true
		case "--release":
			if i+1 >= len(args) {
				return out, errors.New("--release 后必须提供 vX.Y.Z 标签")
			}
			i++
			out.Release = args[i]
		case "--package":
			if i+1 >= len(args) {
				return out, errors.New("--package 后必须提供本地完整包路径")
			}
			i++
			out.Package = strings.Trim(strings.TrimSpace(args[i]), "\"")
		case "--url":
			if i+1 >= len(args) {
				return out, errors.New("--url 后必须提供完整包 URL")
			}
			i++
			out.URL = args[i]
		default:
			return out, fmt.Errorf("未知参数：%s", args[i])
		}
	}
	count := 0
	for _, value := range []string{out.Release, out.Package, out.URL} {
		if value != "" {
			count++
		}
	}
	if count > 1 {
		return out, errors.New("--release、--package 和 --url 不能同时使用")
	}
	if out.Release != "" && !validReleaseTag(out.Release) {
		return out, errors.New("--release 必须是 vX.Y.Z 格式")
	}
	if out.URL != "" {
		parsed, err := url.Parse(out.URL)
		if err != nil || (parsed.Scheme != "http" && parsed.Scheme != "https") || parsed.Host == "" || parsed.User != nil || parsed.Fragment != "" {
			return out, errors.New("--url 必须是 HTTP 或 HTTPS 完整 URL")
		}
	}
	return out, nil
}

func printUsage(kind string) {
	fmt.Printf("DimensionWorks %s 跨平台更新器（%s/%s）\n\n", kind, runtime.GOOS, runtime.GOARCH)
	fmt.Println("用法：")
	fmt.Println("  updater                            更新到 GitHub 最新发行版")
	fmt.Println("  updater --release vX.Y.Z           更新到指定发行版")
	fmt.Println("  updater --package <完整包路径>     使用本地完整包")
	fmt.Println("  updater --url <完整包 URL>         从 URL 更新")
	fmt.Println("  updater --allow-downgrade          允许安装低于当前版本的发行版")
	fmt.Println("  updater --help                     显示帮助")
	fmt.Println("请先退出 Minecraft、启动器或服务端。更新器会保留存档和运行数据。")
	if len(bundledAria2) > 0 {
		fmt.Println("此更新器已内置 aria2c，无需另外下载引擎。")
		fmt.Println("aria2 1.36.0 © Tatsuhiro Tsujikawa and contributors，GPL-2.0-or-later。")
		fmt.Println("许可证与对应源码：同版 GitHub Release 的 aria2-third-party-source.tar.gz。")
	}
}

func validReleaseTag(tag string) bool {
	return strings.HasPrefix(tag, "v") && versionRE.MatchString(strings.TrimPrefix(tag, "v"))
}

func manifestAssetName(kind, version string) string {
	if kind == "server" {
		return "DimensionWorks-Server-" + version + "-update-manifest.tsv"
	}
	return "DimensionWorks-" + version + "-update-manifest.tsv"
}

func fullPackageName(kind, version string) string {
	if kind == "server" {
		return "DimensionWorks-Server-" + version + ".zip"
	}
	return "DimensionWorks-" + version + ".mrpack"
}

func deltaAssetName(kind, version string) string {
	if kind == "server" {
		return "DimensionWorks-Server-" + version + "-delta.zip"
	}
	return "DimensionWorks-" + version + "-delta.zip"
}

func assetURL(tag, name string) string {
	return "https://github.com/" + repository + "/releases/download/" + url.PathEscape(tag) + "/" + url.PathEscape(name)
}

func manifestURLFromPackageURL(raw string) (string, error) {
	parsed, err := url.Parse(raw)
	if err != nil || parsed.Host == "" || (parsed.Scheme != "http" && parsed.Scheme != "https") {
		return "", errors.New("--url 必须是 HTTP 或 HTTPS 完整包 URL")
	}
	base := filepath.Base(parsed.Path)
	ext := filepath.Ext(base)
	if ext != ".zip" && ext != ".mrpack" {
		return "", errors.New("--url 的文件名必须以 .zip 或 .mrpack 结尾")
	}
	name := strings.TrimSuffix(base, ext) + "-update-manifest.tsv"
	parsed.Path = strings.TrimSuffix(parsed.Path, base) + name
	return parsed.String(), nil
}

func newHTTPClient(timeout, responseHeaderTimeout time.Duration) *http.Client {
	transport := &http.Transport{
		Proxy: http.ProxyFromEnvironment,
		DialContext: (&net.Dialer{Timeout: 8 * time.Second, KeepAlive: 30 * time.Second}).DialContext,
		TLSHandshakeTimeout: 8 * time.Second,
		ResponseHeaderTimeout: responseHeaderTimeout,
	}
	return &http.Client{
		Transport: transport,
		Timeout: timeout,
		CheckRedirect: func(req *http.Request, via []*http.Request) error {
			if len(via) >= 5 {
				return errors.New("HTTP 重定向次数超过限制")
			}
			if len(via) > 0 && via[len(via)-1].URL.Scheme == "https" && req.URL.Scheme != "https" {
				return errHTTPSDowngrade
			}
			return nil
		},
	}
}

func latestRelease() (ReleaseInfo, error) {
	apiURL := "https://api.github.com/repos/" + repository + "/releases/latest"
	client := newHTTPClient(30*time.Second, 10*time.Second)
	return latestReleaseFrom(client, releaseAPISources(apiURL))
}

func releaseAPISources(apiURL string) []string {
	return []string{apiURL, "https://ghproxy.net/" + apiURL, "https://gh-proxy.com/" + apiURL}
}

func latestReleaseFrom(client *http.Client, sources []string) (ReleaseInfo, error) {
	var result ReleaseInfo
	var failures []string
	for _, source := range sources {
		req, err := http.NewRequest(http.MethodGet, source, nil)
		if err != nil {
			failures = append(failures, fmt.Sprintf("%s: %v", safeURLForLog(source), err))
			continue
		}
		req.Header.Set("User-Agent", "DimensionWorks-Go-Updater/1.1")
		req.Header.Set("Accept", "application/vnd.github+json")
		resp, err := client.Do(req)
		if err != nil {
			failures = append(failures, fmt.Sprintf("%s: %v", safeURLForLog(source), err))
			continue
		}
		if resp.StatusCode != http.StatusOK {
			failures = append(failures, fmt.Sprintf("%s: HTTP %d", safeURLForLog(source), resp.StatusCode))
			_ = resp.Body.Close()
			continue
		}
		data, readErr := io.ReadAll(io.LimitReader(resp.Body, 4<<20+1))
		_ = resp.Body.Close()
		if readErr != nil {
			failures = append(failures, fmt.Sprintf("%s: 读取发行信息失败：%v", safeURLForLog(source), readErr))
			continue
		}
		if len(data) > 4<<20 {
			failures = append(failures, fmt.Sprintf("%s: 发行信息超过 4 MiB 限制", safeURLForLog(source)))
			continue
		}
		var candidate ReleaseInfo
		if err := json.Unmarshal(data, &candidate); err != nil {
			failures = append(failures, fmt.Sprintf("%s: 无法解析发行信息：%v", safeURLForLog(source), err))
			continue
		}
		if !validReleaseTag(candidate.TagName) {
			failures = append(failures, fmt.Sprintf("%s: 最新发行标签格式无效：%s", safeURLForLog(source), candidate.TagName))
			continue
		}
		return candidate, nil
	}
	return result, fmt.Errorf("无法查询 GitHub 发行版：%s", strings.Join(failures, "; "))
}

func downloadBytes(source, destination string) ([]byte, error) {
	if err := downloadFileWithLimit(source, destination, maxManifestBytes); err != nil {
		return nil, err
	}
	info, err := os.Stat(destination)
	if err != nil {
		return nil, err
	}
	if info.Size() > maxManifestBytes {
		_ = os.Remove(destination)
		return nil, fmt.Errorf("清单文件超过大小限制：%s", humanSize(info.Size()))
	}
	return os.ReadFile(destination)
}

func downloadFile(source, destination string) error {
	return downloadFileWithLimit(source, destination, maxDownloadBytes)
}

func downloadCandidates(source string) []string {
	candidates := []string{source}
	parsed, err := url.Parse(source)
	if err != nil || parsed.Scheme != "https" {
		return candidates
	}
	if strings.EqualFold(parsed.Host, "edge.forgecdn.net") {
		// Follow HMCL's MirrorRule pattern: preserve the official URL and add
		// the MCIM equivalent as a fallback, keeping path and query unchanged.
		mirror := *parsed
		mirror.Host = "mod.mcimirror.top"
		candidates = append(candidates, mirror.String())
		return candidates
	}
	if parsed.Host == "github.com" && strings.Contains(parsed.Path, "/releases/download/") {
		return append(candidates, "https://ghproxy.net/"+source, "https://gh-proxy.com/"+source)
	}
	return candidates
}

func safeURLForLog(raw string) string {
	parsed, err := url.Parse(raw)
	if err != nil { return "<invalid-url>" }
	parsed.User = nil
	if parsed.RawQuery != "" { parsed.RawQuery = "redacted" }
	parsed.Fragment = ""
	return parsed.String()
}

func safeErrorForLog(err error) string {
	if err == nil { return "" }
	urlPattern := regexp.MustCompile(`https?://[^"'\s]+`)
	return urlPattern.ReplaceAllStringFunc(err.Error(), func(raw string) string {
		trimmed := strings.TrimRight(raw, ".,);]")
		suffix := raw[len(trimmed):]
		return safeURLForLog(trimmed) + suffix
	})
}

func downloadFileWithLimit(source, destination string, maxBytes int64) (resultErr error) {
	if maxBytes <= 0 || maxBytes > maxDownloadBytes {
		return fmt.Errorf("下载大小限制无效：%d", maxBytes)
	}
	parsed, err := url.Parse(source)
	if err != nil || parsed.Host == "" || (parsed.Scheme != "http" && parsed.Scheme != "https") {
		return fmt.Errorf("无效下载 URL：%s", source)
	}
	return downloadFromCandidates(downloadSourceOrder.order(downloadCandidates(source)), destination, maxBytes)
}

func downloadFromCandidates(candidates []string, destination string, maxBytes int64) (resultErr error) {
	progressBegin(destination, filepath.Base(destination))
	defer func() { progressFinish(destination, resultErr == nil) }()
	startConsoleInput()
	var failures []string
	for index, candidate := range candidates {
		progressReset(destination)
		ctx, cancel := context.WithCancel(context.Background())
		var etaExceeded atomic.Bool
		stopETA := watchDownloadETA(ctx, func() {
			if etaExceeded.CompareAndSwap(false, true) {
				cancel()
			}
		}, newDownloadETAProbe(destination), downloadETAExceedsLimit, downloadETAProbeInterval)
		var skipped atomic.Bool
		skipID := nextDownloadSkipID.Add(1)
		consoleInputMu.Lock()
		activeDownloadSkips[skipID] = func() {
			if skipped.CompareAndSwap(false, true) {
				cancel()
			}
		}
		consoleInputMu.Unlock()
		fmt.Printf("[下载] 来源 %d/%d：%s（输入 s 并回车可立即跳过此来源）\n", index+1, len(candidates), safeURLForLog(candidate))
		err := downloadOnceLimitedContext(ctx, candidate, destination, maxBytes)
		stopETA()
		consoleInputMu.Lock()
		delete(activeDownloadSkips, skipID)
		consoleInputMu.Unlock()
		cancel()
		if skipped.Load() {
			fmt.Printf("\n[下载] 已跳过来源 %d/%d，切换到下一个来源。\n", index+1, len(candidates))
			failures = append(failures, fmt.Sprintf("%s: 用户跳过", safeURLForLog(candidate)))
			continue
		}
		if etaExceeded.Load() {
			fmt.Printf("\n[下载] 来源预计剩余时间超过 1 小时，切换到下一个来源。\n")
			failures = append(failures, fmt.Sprintf("%s: 预计剩余时间超过 1 小时", safeURLForLog(candidate)))
			continue
		}
		if err == nil {
			if len(candidates) > 1 {
				downloadSourceOrder.remember(candidate)
			}
			return nil
		} else {
			fmt.Printf("\n[下载] 当前来源失败：%s\n", safeErrorForLog(err))
			var engineErr *aria2Error
			if errors.As(err, &engineErr) && engineErr.localFailure() {
				return err
			}
			failures = append(failures, fmt.Sprintf("%s: %s", safeURLForLog(candidate), safeErrorForLog(err)))
		}
	}
	if assetURL, fallbackErr := resolveGitHubAPIAssetURL(githubReleaseSource(candidates)); fallbackErr == nil {
		fmt.Printf("\n[下载] 常规来源失败，尝试 GitHub 资产 API 回退。\n")
		ctx, cancel := context.WithTimeout(context.Background(), downloadAttemptTimeout)
		defer cancel()
		if err := downloadOnceLimitedContext(ctx, assetURL, destination, maxBytes); err == nil {
			return nil
		} else {
			failures = append(failures, fmt.Sprintf("GitHub 资产 API: %s", safeErrorForLog(err)))
		}
	}
	return errors.New(strings.Join(failures, "\n"))
}

const (
	parallelDownloadThreshold int64 = 3 << 20
	parallelDownloadWorkers = 8
)

const downloadAttemptTimeout = 6 * time.Hour

var errSingleConnection = errors.New("使用单连接下载")
var githubAPIBaseURL = "https://api.github.com"

func githubReleaseAssetParts(source string) (string, string, string, string, bool) {
	parsed, err := url.Parse(source)
	if err != nil || parsed.Scheme != "https" || parsed.Host != "github.com" {
		return "", "", "", "", false
	}
	parts := strings.Split(strings.Trim(parsed.Path, "/"), "/")
	if len(parts) != 6 || parts[2] != "releases" || parts[3] != "download" {
		return "", "", "", "", false
	}
	return parts[0], parts[1], parts[4], parts[5], true
}

func githubReleaseSource(candidates []string) string {
	for _, candidate := range candidates {
		if _, _, _, _, ok := githubReleaseAssetParts(candidate); ok {
			return candidate
		}
	}
	return ""
}

func resolveGitHubAPIAssetURL(source string) (string, error) {
	owner, repository, tag, filename, ok := githubReleaseAssetParts(source)
	if !ok {
		return "", errors.New("不是 GitHub Release 资产链接")
	}
	client := newHTTPClient(20*time.Second, 10*time.Second)
	releaseURL := fmt.Sprintf(
		"%s/repos/%s/%s/releases/tags/%s",
		strings.TrimRight(githubAPIBaseURL, "/"),
		url.PathEscape(owner),
		url.PathEscape(repository),
		url.PathEscape(tag),
	)
	req, err := http.NewRequest(http.MethodGet, releaseURL, nil)
	if err != nil {
		return "", err
	}
	req.Header.Set("User-Agent", "DimensionWorks-Go-Updater/1.1")
	req.Header.Set("Accept", "application/vnd.github+json")
	resp, err := client.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return "", fmt.Errorf("GitHub Release API 返回 HTTP %d", resp.StatusCode)
	}
	var release struct {
		Assets []struct {
			Name string `json:"name"`
			URL  string `json:"url"`
		} `json:"assets"`
	}
	if err := json.NewDecoder(io.LimitReader(resp.Body, 4<<20)).Decode(&release); err != nil {
		return "", fmt.Errorf("解析 GitHub Release API 失败：%w", err)
	}
	var assetURL string
	for _, asset := range release.Assets {
		if asset.Name == filename {
			assetURL = asset.URL
			break
		}
	}
	if assetURL == "" {
		return "", fmt.Errorf("GitHub Release 中找不到资产：%s", filename)
	}
	assetReq, err := http.NewRequest(http.MethodGet, assetURL, nil)
	if err != nil {
		return "", err
	}
	assetReq.Header.Set("User-Agent", "DimensionWorks-Go-Updater/1.1")
	assetReq.Header.Set("Accept", "application/octet-stream")
	assetResp, err := client.Do(assetReq)
	if err != nil {
		return "", err
	}
	defer assetResp.Body.Close()
	if assetResp.StatusCode < 200 || assetResp.StatusCode >= 400 {
		return "", fmt.Errorf("GitHub 资产 API 返回 HTTP %d", assetResp.StatusCode)
	}
	finalURL := assetResp.Request.URL
	if finalURL == nil || strings.EqualFold(finalURL.Host, "api.github.com") {
		return "", errors.New("GitHub 资产 API 未返回可下载的重定向地址")
	}
	return finalURL.String(), nil
}

func downloadOnce(source, destination string) error {
	return downloadOnceLimited(source, destination, maxDownloadBytes)
}

func downloadOnceLimited(source, destination string, maxBytes int64) error {
	return downloadOnceLimitedContext(context.Background(), source, destination, maxBytes)
}

func downloadOnceLimitedContext(parent context.Context, source, destination string, maxBytes int64) error {
	if err := os.MkdirAll(filepath.Dir(destination), 0755); err != nil {
		return newAria2Error("准备下载文件", "", err, "无法创建下载目录")
	}
	enginePath, err := aria2ExecutablePath()
	if err != nil { return err }
	err = downloadWithAria2(parent, enginePath, source, destination, maxBytes)
	var engineErr *aria2Error
	if parent.Err() == nil && errors.As(err, &engineErr) && engineErr.retrySingleConnection() {
		fmt.Printf("[下载] %s\n[下载] 使用内嵌 aria2c 单连接重试当前来源。\n", safeErrorForLog(err))
		progressReset(destination)
		return downloadWithAria2Connections(parent, enginePath, source, destination, maxBytes, 1)
	}
	return err
}

func estimatedDownloadETA(total, downloaded int64, bytesPerSecond float64) (time.Duration, bool) {
	if total <= 0 || downloaded < 0 || downloaded >= total || bytesPerSecond <= 0 {
		return 0, false
	}
	seconds := float64(total-downloaded) / bytesPerSecond
	if seconds >= float64(maxDownloadETADuration)/float64(time.Second) {
		return maxDownloadETADuration, true
	}
	return time.Duration(seconds * float64(time.Second)), true
}

func newDownloadETAProbe(key string) func() (time.Duration, bool) {
	var mu sync.Mutex
	var previousTime time.Time
	var previousBytes int64
	return func() (time.Duration, bool) {
		downloadProgressUI.mu.Lock()
		file := downloadProgressUI.files[key]
		var total, written int64
		if file != nil {
			total, written = file.total, file.written
		}
		downloadProgressUI.mu.Unlock()
		if file == nil || total <= 0 || written <= 0 || written >= total {
			mu.Lock()
			previousTime = time.Time{}
			previousBytes = 0
			mu.Unlock()
			return 0, false
		}
		now := time.Now()
		mu.Lock()
		defer mu.Unlock()
		if previousTime.IsZero() || written < previousBytes {
			previousTime = now
			previousBytes = written
			return 0, false
		}
		elapsed := now.Sub(previousTime).Seconds()
		delta := written - previousBytes
		if elapsed <= 0 {
			return 0, false
		}
		if delta == 0 {
			// A short pause has no measurable ETA. Let aria2's transfer idle
			// timeout handle stalls instead of treating them as an hour-long ETA.
			return 0, false
		}
		previousTime = now
		previousBytes = written
		return estimatedDownloadETA(total, written, float64(delta)/elapsed)
	}
}

func watchDownloadETA(ctx context.Context, cancel context.CancelFunc, estimate func() (time.Duration, bool), limit, interval time.Duration) func() {
	if estimate == nil || limit <= 0 || interval <= 0 {
		return func() {}
	}
	done := make(chan struct{})
	var once sync.Once
	go func() {
		ticker := time.NewTicker(interval)
		defer ticker.Stop()
		for {
			select {
			case <-ctx.Done():
				return
			case <-done:
				return
			case <-ticker.C:
				eta, ok := estimate()
				if ok && eta > limit {
					cancel()
					return
				}
			}
		}
	}()
	return func() { once.Do(func() { close(done) }) }
}

func aria2ExecutablePath() (string, error) {
	aria2PathOnce.Do(func() {
		aria2PathCache, aria2PathError = findAria2ExecutablePath()
	})
	return aria2PathCache, aria2PathError
}

func hasAria2BuildForRuntime() bool {
	return runtime.GOOS == "windows" && runtime.GOARCH == "amd64" ||
		runtime.GOOS == "linux" && runtime.GOARCH == "amd64" ||
		runtime.GOOS == "darwin" && runtime.GOARCH == "arm64"
}

func findAria2ExecutablePath() (string, error) {
	if !hasAria2BuildForRuntime() {
		return "", newAria2Error("准备引擎", "", fmt.Errorf("不支持的平台 %s/%s", runtime.GOOS, runtime.GOARCH), "")
	}
	executable, err := os.Executable()
	if err != nil { return "", newAria2Error("准备引擎", "", err, "") }
	dir := filepath.Dir(executable)
	var name, expectedDigest string
	switch runtime.GOOS {
	case "windows":
		name, expectedDigest = "aria2c-windows-amd64.exe", "098a065ab71f639bf7048e790c870756fd6e83de9cc678915bbd07077d473fa2"
	case "linux":
		name, expectedDigest = "aria2c-linux-amd64", "6f50816471fbd5e91c04df6cf6f995c6279d295b70187f313e6d3b04f65769fc"
	case "darwin":
		name, expectedDigest = "aria2c-macos-arm64", "1527c4d071c16c1266880e93750e385cdd4e9d2a950d7f56cb5a21125e3bd311"
	}
	if len(bundledAria2) > 0 {
		candidate, cleanup, err := prepareBundledAria2(aria2ExtractionRoots(dir), name, expectedDigest, bundledAria2)
		if err == nil {
			aria2RuntimeCleanup = cleanup
			return candidate, nil
		}
		return "", err
	}
	candidate := filepath.Join(dir, name)
	info, statErr := os.Lstat(candidate)
	if statErr != nil || info.Mode()&os.ModeSymlink != 0 || !info.Mode().IsRegular() {
		return "", newAria2Error("准备引擎", "", errors.New("此构建未内嵌 aria2c，且没有经过校验的同目录引擎；请使用正式发布的单文件更新器"), "")
	}
	if err := verifyDigest(candidate, "sha256", expectedDigest); err != nil {
		return "", newAria2Error("准备引擎", "", err, "")
	}
	if runtime.GOOS != "windows" {
		if err := os.Chmod(candidate, 0700); err != nil { return "", newAria2Error("准备引擎", "", err, "") }
	}
	if err := probeAria2Executable(candidate); err != nil { return "", err }
	return candidate, nil
}

func downloadWithAria2(parent context.Context, enginePath, source, destination string, maxBytes int64) error {
	for attempt := 0; ; attempt++ {
		err := downloadWithAria2Connections(parent, enginePath, source, destination, maxBytes, 16)
		var engineErr *aria2Error
		if parent.Err() != nil || attempt >= 2 || !errors.As(err, &engineErr) { return err }
		detail := strings.ToLower(engineErr.detail)
		if !strings.Contains(detail, "rpc") || (!strings.Contains(detail, "bind") && !strings.Contains(detail, "address already in use")) { return err }
		fmt.Println("[下载] 本机 RPC 端口被占用，自动选择新端口重试。")
	}
}

func downloadWithAria2Connections(parent context.Context, enginePath, source, destination string, maxBytes int64, connections int) error {
	ctx, cancel := context.WithTimeout(parent, downloadAttemptTimeout)
	defer cancel()
	if err := parent.Err(); err != nil { return err }
	if info, err := os.Lstat(destination); err == nil && !info.Mode().IsRegular() {
		return newAria2Error("准备下载文件", "", errors.New("目标不是普通文件"), destination)
	}
	defer os.Remove(destination + ".aria2")

	rpcPort, err := reserveAria2RPCPort()
	if err != nil { return newAria2Error("RPC 接口", "", err, "无法分配回环端口") }
	rpcSecret, err := randomAria2RPCSecret()
	if err != nil { return newAria2Error("RPC 接口", "", err, "无法生成随机令牌") }
	// Choose DNS by the source host type on every platform. Remote downloads
	// must also work when native DNS retains a loopback or fake-IP answer that
	// bypasses the active system route or TUN.
	args := []string{
		"--no-conf=true",
		"--continue=false",
		"--allow-overwrite=true",
		"--auto-file-renaming=false",
		"--file-allocation=none",
		"--check-certificate=true",
		"--http-accept-gzip=false",
		"--async-dns=" + strconv.FormatBool(aria2AsyncDNS(source)),
		"--user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36",
		fmt.Sprintf("--max-connection-per-server=%d", connections),
		fmt.Sprintf("--split=%d", connections),
		"--min-split-size=1M",
		"--connect-timeout=10",
		"--timeout=60",
		"--max-tries=3",
		"--retry-wait=2",
		"--enable-rpc=true",
		"--rpc-listen-all=false",
		fmt.Sprintf("--rpc-listen-port=%d", rpcPort),
		"--rpc-secret=" + rpcSecret,
		"--console-log-level=warn",
		"--show-console-readout=false",
		"--summary-interval=0",
		"--dir=" + filepath.Dir(destination),
		"--out=" + filepath.Base(destination),
	}
	if strings.HasPrefix(source, "https://") {
		certificateArgs, cleanupCertificates, err := prepareAria2Certificates(ctx)
		if err != nil { return newAria2Error("准备证书", "", err, "无法读取系统受信任根证书") }
		defer cleanupCertificates()
		args = append(args, certificateArgs...)
	}
	// Match Go's proxy selection, including NO_PROXY, for the engine process.
	proxyURL := ""
	if request, err := http.NewRequest(http.MethodGet, source, nil); err == nil {
		if proxy, err := http.ProxyFromEnvironment(request); err == nil && proxy != nil {
			proxyURL = proxy.String()
		}
	}
	args = append(args, "--all-proxy="+proxyURL, "--http-proxy="+proxyURL, "--https-proxy="+proxyURL, "--ftp-proxy=")
	args = append(args, source)
	command := exec.CommandContext(ctx, enginePath, args...)
	var output limitedOutput
	command.Stdout = &output
	command.Stderr = &output
	if err := command.Start(); err != nil {
		return newAria2Error("启动引擎", "", err, output.String())
	}
	processDone := make(chan struct{})
	var waitErr error
	go func() {
		waitErr = command.Wait()
		close(processDone)
	}()
	rpcURL := fmt.Sprintf("http://127.0.0.1:%d/jsonrpc", rpcPort)
	rpcClient := &http.Client{
		Transport: &http.Transport{Proxy: nil},
		Timeout:   2 * time.Second,
	}
	defer func() {
		cancel()
		<-processDone
		rpcClient.CloseIdleConnections()
	}()
	startupCtx, stopStartup := context.WithCancel(ctx)
	defer stopStartup()
	go func() {
		select {
		case <-processDone: stopStartup()
		case <-startupCtx.Done():
		}
	}()
	if err := waitForAria2RPC(startupCtx, rpcClient, rpcURL, rpcSecret); err != nil {
		exited := false
		select {
		case <-processDone: exited = true
		default:
		}
		cancel()
		<-processDone
		if parent.Err() != nil { return parent.Err() }
		if waitErr != nil && exited {
			return newAria2Error("启动引擎", "", waitErr, output.String())
		}
		return newAria2Error("RPC 接口", "", err, output.String())
	}
	stopStartup()

	monitorDone := make(chan struct{})
	var tooLarge atomic.Bool
	var completed atomic.Bool
	var engineFailure atomic.Value
	var rpcFailure atomic.Value
	go func() {
		defer close(monitorDone)
		ticker := time.NewTicker(250 * time.Millisecond)
		defer ticker.Stop()
		var reported int64
		var rpcFailedSince time.Time
		for {
			select {
			case <-ctx.Done():
				return
			case <-ticker.C:
				status, statusErr := getAria2ActiveStatus(ctx, rpcClient, rpcURL, rpcSecret)
				if statusErr != nil {
					if rpcFailedSince.IsZero() { rpcFailedSince = time.Now() }
					if time.Since(rpcFailedSince) >= 5*time.Second {
						rpcFailure.Store(statusErr)
						cancel()
						return
					}
					continue
				}
				for _, item := range status {
					completed, completedErr := strconv.ParseInt(item.CompletedLength, 10, 64)
					total, totalErr := strconv.ParseInt(item.TotalLength, 10, 64)
					if completedErr != nil || totalErr != nil || completed < 0 || total < 0 { continue }
					if total > maxBytes || completed > maxBytes {
					tooLarge.Store(true)
					cancel()
					return
				}
					if total > 0 { progressSetTotal(destination, total) }
					if completed > reported {
						progressAdd(destination, completed-reported)
						reported = completed
					} else if completed < reported {
						progressRewind(destination, reported-completed)
						reported = completed
					}
				}
				// RPC mode keeps aria2 alive after its final download. Observe
				// the terminal result instead of waiting forever for process exit.
				var stopped []aria2DownloadResult
				stoppedErr := callAria2RPC(ctx, rpcClient, rpcURL, rpcSecret, "aria2.tellStopped",
					[]any{0, 1, []string{"status", "errorCode", "errorMessage", "totalLength", "completedLength"}}, &stopped)
				if stoppedErr != nil {
					if rpcFailedSince.IsZero() { rpcFailedSince = time.Now() }
					if time.Since(rpcFailedSince) >= 5*time.Second {
						rpcFailure.Store(stoppedErr)
						cancel()
						return
					}
				} else {
					rpcFailedSince = time.Time{}
				}
				if stoppedErr == nil && len(stopped) > 0 {
					if stopped[0].Status == "complete" {
						completed.Store(true)
						cancel()
						return
					}
					if stopped[0].Status == "error" || stopped[0].Status == "removed" {
						engineFailure.Store(stopped[0])
						cancel()
						return
					}
				}
				if info, err := os.Stat(destination); err == nil && info.Size() > maxBytes {
					tooLarge.Store(true)
					cancel()
					return
				}
			}
		}
	}()

	<-processDone
	cancel()
	<-monitorDone
	rpcClient.CloseIdleConnections()
	if tooLarge.Load() {
		_ = os.Remove(destination)
		return newAria2Error("校验下载文件", "", fmt.Errorf("下载超过大小限制：%s", humanSize(maxBytes)), "")
	}
	if parent.Err() != nil { return parent.Err() }
	if failure := rpcFailure.Load(); failure != nil {
		return newAria2Error("RPC 接口", "", failure.(error), output.String())
	}
	if failure := engineFailure.Load(); failure != nil {
		_ = os.Remove(destination)
		result := failure.(aria2DownloadResult)
		return newAria2Error("下载任务", result.ErrorCode, nil, result.ErrorMessage+" "+output.String())
	}
	if waitErr != nil && !completed.Load() {
		_ = os.Remove(destination)
		message := strings.TrimSpace(output.String())
		if errors.Is(ctx.Err(), context.DeadlineExceeded) { waitErr = ctx.Err() }
		return newAria2Error("下载任务", "", waitErr, message)
	}
	info, err := os.Stat(destination)
	if err != nil { return newAria2Error("校验下载文件", "", err, "下载结束但未生成目标文件") }
	if !info.Mode().IsRegular() || info.Size() <= 0 || info.Size() > maxBytes {
		_ = os.Remove(destination)
		return newAria2Error("校验下载文件", "", errors.New("下载文件类型或大小无效"), "")
	}
	progressSetTotal(destination, info.Size())
	return nil
}

type limitedOutput struct {
	mu   sync.Mutex
	data []byte
}

func (w *limitedOutput) Write(data []byte) (int, error) {
	originalLength := len(data)
	const outputLimit = 16 << 10
	if len(data) > outputLimit { data = data[len(data)-outputLimit:] }
	w.mu.Lock()
	defer w.mu.Unlock()
	if len(w.data)+len(data) > outputLimit {
		w.data = append(w.data[:0], w.data[len(w.data)+len(data)-outputLimit:]...)
	}
	w.data = append(w.data, data...)
	return originalLength, nil
}

func (w *limitedOutput) String() string {
	w.mu.Lock()
	defer w.mu.Unlock()
	return string(w.data)
}

type aria2Status struct {
	TotalLength     string `json:"totalLength"`
	CompletedLength string `json:"completedLength"`
}

type aria2DownloadResult struct {
    Status string `json:"status"`
    ErrorCode string `json:"errorCode"`
    ErrorMessage string `json:"errorMessage"`
    TotalLength string `json:"totalLength"`
    CompletedLength string `json:"completedLength"`
}

type aria2RPCResponse struct {
	Result json.RawMessage `json:"result"`
	Error  *struct {
		Code    int    `json:"code"`
		Message string `json:"message"`
	} `json:"error"`
}

func reserveAria2RPCPort() (int, error) {
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil { return 0, err }
	defer listener.Close()
	return listener.Addr().(*net.TCPAddr).Port, nil
}

func randomAria2RPCSecret() (string, error) {
	var secret [32]byte
	if _, err := rand.Read(secret[:]); err != nil { return "", err }
	return hex.EncodeToString(secret[:]), nil
}

func waitForAria2RPC(ctx context.Context, client *http.Client, endpoint, secret string) error {
	waitCtx, cancel := context.WithTimeout(ctx, 5*time.Second)
	defer cancel()
	ticker := time.NewTicker(100 * time.Millisecond)
	defer ticker.Stop()
	var lastErr error
	for {
		var version json.RawMessage
		if err := callAria2RPC(waitCtx, client, endpoint, secret, "aria2.getVersion", []any{}, &version); err == nil {
			return nil
		} else {
			lastErr = err
		}
		select {
		case <-waitCtx.Done():
			if lastErr != nil { return lastErr }
			return waitCtx.Err()
		case <-ticker.C:
		}
	}
}

func getAria2ActiveStatus(ctx context.Context, client *http.Client, endpoint, secret string) ([]aria2Status, error) {
	var status []aria2Status
	if err := callAria2RPC(ctx, client, endpoint, secret, "aria2.tellActive", []any{[]string{"totalLength", "completedLength"}}, &status); err != nil {
		return nil, err
	}
	return status, nil
}

func callAria2RPC(ctx context.Context, client *http.Client, endpoint, secret, method string, params []any, output any) error {
	allParams := make([]any, 0, len(params)+1)
	allParams = append(allParams, "token:"+secret)
	allParams = append(allParams, params...)
	payload, err := json.Marshal(map[string]any{
		"jsonrpc": "2.0",
		"id":      "dimensionworks-updater",
		"method":  method,
		"params":  allParams,
	})
	if err != nil { return err }
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, endpoint, bytes.NewReader(payload))
	if err != nil { return err }
	req.Header.Set("Content-Type", "application/json")
	resp, err := client.Do(req)
	if err != nil { return err }
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK { return fmt.Errorf("RPC HTTP %d", resp.StatusCode) }
	data, err := io.ReadAll(io.LimitReader(resp.Body, 1<<20))
	if err != nil { return err }
	var result aria2RPCResponse
	if err := json.Unmarshal(data, &result); err != nil { return err }
	if result.Error != nil { return fmt.Errorf("RPC %d: %s", result.Error.Code, result.Error.Message) }
	if output == nil { return nil }
	return json.Unmarshal(result.Result, output)
}

func progressAdd(key string, count int64) { downloadProgressUI.add(key, count) }
func progressRewind(key string, count int64) { downloadProgressUI.rewind(key, count) }

type activityReader struct {
	reader io.Reader
	touch func()
}

func (r activityReader) Read(buffer []byte) (int, error) {
	n, err := r.reader.Read(buffer)
	if n > 0 { r.touch() }
	return n, err
}

func newIdleTimeoutContext(parent context.Context, idle time.Duration) (context.Context, func(), func()) {
	ctx, cancel := context.WithCancel(parent)
	timer := time.AfterFunc(idle, cancel)
	touch := func() { timer.Reset(idle) }
	stop := func() { timer.Stop(); cancel() }
	return ctx, stop, touch
}

func downloadIdleTimeout(expectedBytes int64) time.Duration {
	const minimum = 2 * time.Minute
	const maximum = 10 * time.Minute
	if expectedBytes <= 0 { return 3 * time.Minute }
	extra := time.Duration(expectedBytes/(512*1024)) * time.Second
	idle := minimum + extra
	if idle > maximum { return maximum }
	return idle
}

type resumeMetadata struct {
	Source string `json:"source"`
	ETag string `json:"etag"`
	LastModified string `json:"last_modified"`
	Total int64 `json:"total"`
}

func cleanupDownloadCache(cacheRoot string, maxAge time.Duration) error {
	info, err := os.Lstat(cacheRoot)
	if err != nil { return err }
	if info.Mode()&os.ModeSymlink != 0 || !info.IsDir() { return errors.New("下载缓存目录不安全") }
	entries, err := os.ReadDir(cacheRoot)
	if err != nil { return err }
	cutoff := time.Now().Add(-maxAge)
	for _, entry := range entries {
		path := filepath.Join(cacheRoot, entry.Name())
		fileInfo, err := os.Lstat(path)
		if err != nil { return err }
		if fileInfo.Mode()&os.ModeSymlink != 0 { return fmt.Errorf("下载缓存包含符号链接：%s", path) }
		if !fileInfo.Mode().IsRegular() { return fmt.Errorf("下载缓存包含非普通文件：%s", path) }
		if fileInfo.ModTime().Before(cutoff) {
			if err := os.Remove(path); err != nil { return err }
		}
	}
	return nil
}

func downloadResumePaths(source, destination string) (string, string, error) {
	if downloadCacheRoot == "" {
		part := destination + ".part"
		return part, part + ".json", nil
	}
	info, err := os.Lstat(downloadCacheRoot)
	if err != nil || info.Mode()&os.ModeSymlink != 0 || !info.IsDir() {
		return "", "", errors.New("断点续传缓存目录不安全")
	}
	sum := sha256.Sum256([]byte(source))
	base := filepath.Join(downloadCacheRoot, hex.EncodeToString(sum[:]))
	return base + ".part", base + ".json", nil
}

func loadResumeMetadata(path, source string) (*resumeMetadata, error) {
	info, err := os.Lstat(path)
	if os.IsNotExist(err) { return nil, nil }
	if err != nil { return nil, err }
	if info.Mode()&os.ModeSymlink != 0 || !info.Mode().IsRegular() || info.Size() > 4096 {
		return nil, errors.New("断点续传元数据不安全或超过大小限制")
	}
	data, err := readRegularFile(path, 4096)
	if err != nil { return nil, err }
	var meta resumeMetadata
	if err := json.Unmarshal(data, &meta); err != nil { return nil, err }
	if meta.Source != source || meta.Total < 0 || (meta.ETag == "" && meta.LastModified == "") {
		return nil, nil
	}
	return &meta, nil
}

func saveResumeMetadata(path string, meta resumeMetadata) error {
	data, err := json.Marshal(meta)
	if err != nil { return err }
	temp, err := os.CreateTemp(filepath.Dir(path), ".resume-meta-*")
	if err != nil { return err }
	tempPath := temp.Name()
	defer os.Remove(tempPath)
	if _, err := temp.Write(data); err != nil { _ = temp.Close(); return err }
	if err := temp.Sync(); err != nil { _ = temp.Close(); return err }
	if err := temp.Close(); err != nil { return err }
	return replaceExistingFile(tempPath, path)
}

func downloadSequential(ctx context.Context, source, destination string, maxBytes int64) error {
	part, metaPath, err := downloadResumePaths(source, destination)
	if err != nil { return err }
	meta, err := loadResumeMetadata(metaPath, source)
	if err != nil { _ = os.Remove(metaPath); meta = nil }
	if meta != nil && meta.Total > maxBytes {
		_ = os.Remove(part)
		_ = os.Remove(metaPath)
		meta = nil
	}
	if partInfo, partErr := os.Lstat(part); partErr == nil {
		if partInfo.Mode()&os.ModeSymlink != 0 || !partInfo.Mode().IsRegular() {
			return errors.New("断点续传临时文件不是普通文件")
		}
	} else if !os.IsNotExist(partErr) {
		return partErr
	}
	offset := int64(0)
	if meta != nil {
		if info, statErr := os.Lstat(part); statErr == nil && info.Mode().IsRegular() && info.Mode()&os.ModeSymlink == 0 && info.Size() <= meta.Total {
			offset = info.Size()
		} else {
			_ = os.Remove(part)
			_ = os.Remove(metaPath)
			meta = nil
		}
	}
	if meta != nil && offset == meta.Total && offset > 0 {
		if err := replaceExistingFile(part, destination); err != nil { return err }
		_ = os.Remove(metaPath)
		return nil
	}
	idleCtx, stopIdle, touch := newIdleTimeoutContext(ctx, downloadIdleTimeout(maxBytes))
	defer stopIdle()
	req, err := http.NewRequestWithContext(idleCtx, http.MethodGet, source, nil)
	if err != nil { return err }
	req.Header.Set("User-Agent", "DimensionWorks-Go-Updater/1.1")
	if offset > 0 && meta != nil {
		req.Header.Set("Range", fmt.Sprintf("bytes=%d-", offset))
		if meta.ETag != "" { req.Header.Set("If-Range", meta.ETag) } else { req.Header.Set("If-Range", meta.LastModified) }
	}
	resp, err := newHTTPClient(0, 30*time.Second).Do(req)
	if err != nil { return err }
	defer resp.Body.Close()
	restartFresh := func(reason string) error {
		_ = resp.Body.Close()
		_ = os.Remove(part)
		_ = os.Remove(metaPath)
		if reason != "" { fmt.Println("[续传] ", reason, "，将从头下载") }
		return downloadSequential(ctx, source, destination, maxBytes)
	}

	appendMode := false
	total := resp.ContentLength
	if offset > 0 && meta != nil && resp.StatusCode == http.StatusPartialContent {
		matches := regexp.MustCompile("^bytes ([0-9]+)-([0-9]+)/([0-9]+)$").FindStringSubmatch(resp.Header.Get("Content-Range"))
		if len(matches) != 4 { return restartFresh("续传响应范围无效") }
		begin, e1 := strconv.ParseInt(matches[1], 10, 64)
		end, e2 := strconv.ParseInt(matches[2], 10, 64)
		declaredTotal, e3 := strconv.ParseInt(matches[3], 10, 64)
		if e1 != nil || e2 != nil || e3 != nil || begin != offset || declaredTotal != meta.Total || end < begin || end >= declaredTotal {
			return restartFresh("续传响应与已保存的文件长度不匹配")
		}
		if meta.ETag != "" && resp.Header.Get("ETag") != meta.ETag { return restartFresh("续传资源 ETag 已变化") }
		if meta.ETag == "" && resp.Header.Get("Last-Modified") != meta.LastModified { return restartFresh("续传资源 Last-Modified 已变化") }
		if encoding := strings.TrimSpace(resp.Header.Get("Content-Encoding")); encoding != "" && !strings.EqualFold(encoding, "identity") { return restartFresh("续传响应使用了压缩编码") }
		total = declaredTotal
		appendMode = true
	} else if resp.StatusCode == http.StatusOK {
		offset = 0
		if total < 0 {
			meta = nil
			_ = os.Remove(metaPath)
		} else {
			meta = &resumeMetadata{Source: source, ETag: resp.Header.Get("ETag"), LastModified: resp.Header.Get("Last-Modified"), Total: total}
			if meta.ETag == "" && meta.LastModified == "" {
				_ = os.Remove(metaPath)
				meta = nil
			} else if err := saveResumeMetadata(metaPath, *meta); err != nil {
				return fmt.Errorf("保存断点续传元数据失败：%w", err)
			}
		}
	} else if resp.StatusCode == http.StatusRequestedRangeNotSatisfiable && offset > 0 {
		_ = resp.Body.Close()
		_ = os.Remove(part)
		_ = os.Remove(metaPath)
		return downloadSequential(ctx, source, destination, maxBytes)
	} else {
		return fmt.Errorf("HTTP %d，续传请求未获得可用响应", resp.StatusCode)
	}
	if total > maxBytes { _ = os.Remove(part); _ = os.Remove(metaPath); return fmt.Errorf("下载文件超过大小限制：%s", humanSize(total)) }
	if resp.ContentLength >= 0 && appendMode && resp.ContentLength != total-offset {
		return restartFresh("续传响应长度不匹配")
	}
	if offset > 0 { downloadProgressUI.add(destination, offset) }
	progressSetTotal(destination, total)
	flags := os.O_CREATE|os.O_WRONLY
	if appendMode { flags |= os.O_APPEND } else { flags |= os.O_TRUNC }
	file, err := os.OpenFile(part, flags, 0644)
	if err != nil { return err }
	writer := &downloadProgress{writer: file, key: destination}
	written, copyErr := io.Copy(writer, io.LimitReader(activityReader{reader: resp.Body, touch: touch}, maxBytes-offset+1))
	syncErr := file.Sync()
	closeErr := file.Close()
	if copyErr != nil { return copyErr }
	if syncErr != nil { return syncErr }
	if closeErr != nil { return closeErr }
	if written > maxBytes-offset { return fmt.Errorf("下载文件超过大小限制：%s", humanSize(offset+written)) }
	if total >= 0 && offset+written != total { return fmt.Errorf("下载未完成，已保存 %d/%d 字节；再次运行可继续下载", offset+written, total) }
	if err := replaceExistingFile(part, destination); err != nil { return err }
	_ = os.Remove(metaPath)
	return nil
}

// downloadRanged only enables parallel mode after a real 206 + Content-Range probe.
// Every segment is written to a separate temporary file and validated before assembly.
func rangePartPath(source, destination, validator string, total, index int64) (string, error) {
	base := destination
	if downloadCacheRoot != "" && validator != "" {
		info, err := os.Lstat(downloadCacheRoot)
		if err != nil || info.Mode()&os.ModeSymlink != 0 || !info.IsDir() {
			return "", errors.New("分段下载缓存目录不安全")
		}
		sum := sha256.Sum256([]byte(source+"\\n"+validator))
		base = filepath.Join(downloadCacheRoot, hex.EncodeToString(sum[:]))
	}
	return fmt.Sprintf("%s.range-%d-%02d.part", base, total, index), nil
}

func downloadRanged(ctx context.Context, source, destination string, maxBytes int64) error {
	probeParent, cancelProbe := context.WithTimeout(ctx, 20*time.Second)
	defer cancelProbe()
	probeCtx, stopIdle, touchProbe := newIdleTimeoutContext(probeParent, 2*time.Minute)
	defer stopIdle()
	req, err := http.NewRequestWithContext(probeCtx, http.MethodGet, source, nil)
	if err != nil { return err }
	req.Header.Set("User-Agent", "DimensionWorks-Go-Updater/1.1")
	req.Header.Set("Accept-Encoding", "identity")
	req.Header.Set("Range", "bytes=0-0")
	resp, err := newHTTPClient(0, 20*time.Second).Do(req)
	if err != nil { return err }
	probeBody, bodyErr := io.ReadAll(io.LimitReader(activityReader{reader: resp.Body, touch: touchProbe}, 2))
	resp.Body.Close()
	if bodyErr != nil { return fmt.Errorf("读取 Range 探测响应失败：%w", bodyErr) }
	if resp.StatusCode == http.StatusOK { return fmt.Errorf("%w：当前来源返回 HTTP 200，没有接受 Range 分段请求；将使用单连接下载。下载器只能根据当前来源的 HTTP 响应判断，其他下载器可能使用了不同的来源或请求方式", errSingleConnection) }
	if resp.StatusCode != http.StatusPartialContent { return fmt.Errorf("Range 探测失败（HTTP %d）", resp.StatusCode) }
	matches := regexp.MustCompile("^bytes 0-0/([0-9]+)$").FindStringSubmatch(resp.Header.Get("Content-Range"))
	if len(matches) != 2 || len(probeBody) != 1 || (resp.ContentLength >= 0 && resp.ContentLength != 1) {
		return errors.New("服务器返回无效的 Range 探测响应")
	}
	if encoding := strings.TrimSpace(resp.Header.Get("Content-Encoding")); encoding != "" && !strings.EqualFold(encoding, "identity") {
		return fmt.Errorf("Range 探测响应使用了不支持的内容编码：%s", encoding)
	}
	total, err := strconv.ParseInt(matches[1], 10, 64)
	if err != nil || total <= 0 || total > maxBytes { return errors.New("服务器返回无效或超限的 Content-Range") }
	validator := rangeValidator(resp.Header)
	// Reuse the probed endpoint: release redirects can be expensive, rate-limited,
	// or yield different mirror origins/validators for each concurrent request.
	segmentSource := resp.Request.URL.String()
	progressSetTotal(destination, total)
	if total <= parallelDownloadThreshold { return fmt.Errorf("%w：当前文件为 %s，未超过启用分段下载的 %s 阈值", errSingleConnection, humanSize(total), humanSize(parallelDownloadThreshold)) }

	segmentBudget := int64(parallelDownloadWorkers)
	if configured := parallelDownloadLimit.Load(); configured > 0 && configured < segmentBudget {
		segmentBudget = configured
	}
	// Small assets use fewer connections; large assets can use the full budget.
	segments := (total + (16<<20) - 1) / (16 << 20)
	if segments < 2 { segments = 2 }
	if segments > segmentBudget { segments = segmentBudget }
	if total < segments { segments = total }
	chunkSize := (total + segments - 1) / segments
	partPaths := make([]string, 0, segments)
	for i := int64(0); i < segments; i++ {
		path, err := rangePartPath(source, destination, validator, total, i)
		if err != nil { return err }
		partPaths = append(partPaths, path)
	}
	jobs := make(chan int64)
	errs := make(chan error, segments)
	rangeCtx, cancelRange := context.WithCancel(ctx)
	defer cancelRange()
	var wg sync.WaitGroup
	var completed atomic.Int64
	var finished atomic.Int64
	var printMu sync.Mutex
	workers := int(segments)
	fmt.Printf("[下载] 启用 %d 路分段并发，总大小 %s\n", workers, humanSize(total))
	for i := 0; i < workers; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for {
				var index int64
				var ok bool
				select {
				case <-rangeCtx.Done(): return
				case index, ok = <-jobs: if !ok { return }
				}
				begin := index * chunkSize
				end := begin + chunkSize - 1
				if end >= total { end = total - 1 }
				expected := end-begin+1
				cachedInfo, cacheErr := os.Lstat(partPaths[index])
				if cacheErr == nil {
					if cachedInfo.Mode()&os.ModeSymlink != 0 || !cachedInfo.Mode().IsRegular() {
						select { case errs <- fmt.Errorf("缓存分段不是普通文件：%s", partPaths[index]): default: }
						cancelRange(); return
					}
					checksumPath := partPaths[index] + ".sha256"
					checksumInfo, checksumErr := os.Lstat(checksumPath)
					checksumOK := false
					if checksumErr == nil && checksumInfo.Mode().IsRegular() && checksumInfo.Mode()&os.ModeSymlink == 0 && checksumInfo.Size() == 64 {
						checksumBytes, readErr := os.ReadFile(checksumPath)
						checksumOK = readErr == nil && verifyDigest(partPaths[index], "sha256", string(checksumBytes)) == nil
					}
					if cachedInfo.Size() == expected && checksumOK {
						completed.Add(expected); finished.Add(1); downloadProgressUI.add(destination, expected)
						continue
					}
					_ = os.Remove(partPaths[index])
					_ = os.Remove(checksumPath)
				} else if !os.IsNotExist(cacheErr) {
					select { case errs <- cacheErr: default: }; cancelRange(); return
				}
				if err := downloadRangeSegmentWithRetry(rangeCtx, segmentSource, partPaths[index], destination, begin, end, total, validator); err != nil {
					select { case errs <- fmt.Errorf("分段 %d：%w", index+1, err): default: }
					cancelRange(); return
				}
				completed.Add(expected); finished.Add(1)
				printMu.Lock()
				fmt.Printf("[下载进度] 分段完成 %d/%d（%s/%s）\n", finished.Load(), segments, humanSize(completed.Load()), humanSize(total))
				printMu.Unlock()
			}
		}()
	}
sendJobs:
	for i := int64(0); i < segments; i++ {
		select { case jobs <- i: case <-rangeCtx.Done(): break sendJobs }
	}
	close(jobs)
	wg.Wait()
	close(errs)
	for segmentErr := range errs { if segmentErr != nil { return segmentErr } }
	if completed.Load() != total { return fmt.Errorf("分段总长度不符：%d/%d", completed.Load(), total) }

	assembled := destination + ".assembled"
	_ = os.Remove(assembled)
	out, err := os.OpenFile(assembled, os.O_CREATE|os.O_TRUNC|os.O_WRONLY, 0644)
	if err != nil { return err }
	var copied int64
	for _, path := range partPaths {
		in, openErr := os.Open(path)
		if openErr != nil { _ = out.Close(); _ = os.Remove(assembled); return openErr }
		n, copyErr := io.Copy(out, in)
		closeErr := in.Close()
		copied += n
		if copyErr != nil || closeErr != nil {
			_ = out.Close(); _ = os.Remove(assembled)
			if copyErr != nil { return copyErr }; return closeErr
		}
	}
	if err := out.Sync(); err != nil { _ = out.Close(); _ = os.Remove(assembled); return err }
	if err := out.Close(); err != nil { _ = os.Remove(assembled); return err }
	if copied != total { _ = os.Remove(assembled); return fmt.Errorf("合并后长度不符：%d/%d", copied, total) }
	if err := replaceExistingFile(assembled, destination); err != nil { _ = os.Remove(assembled); return err }
	for _, path := range partPaths { _ = os.Remove(path); _ = os.Remove(path + ".sha256") }
	return nil
}

func writeSmallFileAtomic(path string, data []byte, mode os.FileMode) error {
	temp, err := os.CreateTemp(filepath.Dir(path), ".dw-meta-*")
	if err != nil { return err }
	tempPath := temp.Name()
	defer os.Remove(tempPath)
	if _, err := temp.Write(data); err != nil { _ = temp.Close(); return err }
	if err := temp.Sync(); err != nil { _ = temp.Close(); return err }
	if err := temp.Chmod(mode); err != nil { _ = temp.Close(); return err }
	if err := temp.Close(); err != nil { return err }
	return replaceExistingFile(tempPath, path)
}

// RFC 9110 forbids weak entity tags in If-Range. Prefer a strong ETag,
// otherwise use Last-Modified; final file digests remain mandatory.
func rangeValidator(header http.Header) string {
	etag := strings.TrimSpace(header.Get("ETag"))
	if strings.HasPrefix(etag, "\"") && strings.HasSuffix(etag, "\"") {
		return etag
	}
	return header.Get("Last-Modified")
}

func downloadRangeSegmentWithRetry(ctx context.Context, source, destination, progressKey string, begin, end, total int64, validator string) error {
	var lastErr error
	for attempt := 0; attempt < 3; attempt++ {
		if attempt > 0 {
			timer := time.NewTimer(time.Duration(attempt) * time.Second)
			select {
			case <-ctx.Done(): timer.Stop(); return ctx.Err()
			case <-timer.C:
			}
		}
		lastErr = downloadRangeSegment(ctx, source, destination, progressKey, begin, end, total, validator)
		if lastErr == nil { return nil }
		if ctx.Err() != nil { return ctx.Err() }
	}
	return lastErr
}

func downloadRangeSegment(ctx context.Context, source, destination, progressKey string, begin, end, total int64, validator string) error {
	idleCtx, stopIdle, touch := newIdleTimeoutContext(ctx, downloadIdleTimeout(end-begin+1))
	defer stopIdle()
	req, err := http.NewRequestWithContext(idleCtx, http.MethodGet, source, nil)
	if err != nil { return err }
	req.Header.Set("User-Agent", "DimensionWorks-Go-Updater/1.1")
	req.Header.Set("Accept-Encoding", "identity")
	req.Header.Set("Range", fmt.Sprintf("bytes=%d-%d", begin, end))
	if validator != "" { req.Header.Set("If-Range", validator) }
	resp, err := newHTTPClient(0, 30*time.Second).Do(req)
	if err != nil { return err }
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusPartialContent {
		return fmt.Errorf("HTTP %d，预期 206 Partial Content", resp.StatusCode)
	}
	if validator != "" && resp.Header.Get("ETag") != validator && resp.Header.Get("Last-Modified") != validator {
		return errors.New("分段资源校验标识变化，拒绝混合不同版本文件")
	}
	contentRange := regexp.MustCompile("^bytes ([0-9]+)-([0-9]+)/([0-9]+)$").FindStringSubmatch(resp.Header.Get("Content-Range"))
	if len(contentRange) != 4 {
		return fmt.Errorf("Content-Range 格式无效：%q", resp.Header.Get("Content-Range"))
	}
	gotBegin, err1 := strconv.ParseInt(contentRange[1], 10, 64)
	gotEnd, err2 := strconv.ParseInt(contentRange[2], 10, 64)
	gotTotal, err3 := strconv.ParseInt(contentRange[3], 10, 64)
	if err1 != nil || err2 != nil || err3 != nil || gotBegin != begin || gotEnd != end || gotTotal != total {
		return fmt.Errorf("Content-Range 不匹配：%q", resp.Header.Get("Content-Range"))
	}
	if encoding := strings.TrimSpace(resp.Header.Get("Content-Encoding")); encoding != "" && !strings.EqualFold(encoding, "identity") {
		return fmt.Errorf("Range 响应使用了不支持的内容编码：%s", encoding)
	}
	expected := end-begin+1
	if resp.ContentLength >= 0 && resp.ContentLength != expected {
		return fmt.Errorf("分段长度声明不匹配：%d/%d", resp.ContentLength, expected)
	}
	file, err := os.OpenFile(destination, os.O_CREATE|os.O_TRUNC|os.O_WRONLY, 0644)
	if err != nil { return err }
	progressWriter := &downloadProgress{writer: file, key: progressKey}
	succeeded := false
	defer func() {
		if !succeeded { downloadProgressUI.rewind(progressKey, progressWriter.written) }
	}()
	n, copyErr := io.Copy(progressWriter, io.LimitReader(activityReader{reader: resp.Body, touch: touch}, expected+1))
	syncErr := file.Sync()
	closeErr := file.Close()
	if copyErr != nil { _ = os.Remove(destination); return copyErr }
	if syncErr != nil { _ = os.Remove(destination); return syncErr }
	if closeErr != nil { _ = os.Remove(destination); return closeErr }
	if n != expected { _ = os.Remove(destination); return fmt.Errorf("分段实际长度不匹配：%d/%d", n, expected) }
	digest, err := hashFile(destination, "sha256")
	if err != nil { _ = os.Remove(destination); return err }
	if err := writeSmallFileAtomic(destination+".sha256", []byte(digest), 0600); err != nil { _ = os.Remove(destination); return err }
	succeeded = true
	return nil
}

type downloadProgress struct {
	writer  io.Writer
	key     string
	written int64
}

type progressFile struct {
	name    string
	total   int64
	written int64
	active  bool
}

type progressTracker struct {
	mu          sync.Mutex
	files       map[string]*progressFile
	lastRefresh time.Time
	lastBytes   int64
	lastSpeed   float64
}

func newProgressTracker() *progressTracker {
	return &progressTracker{files: make(map[string]*progressFile)}
}

func (p *progressTracker) begin(key, name string) {
	p.mu.Lock()
	p.files[key] = &progressFile{name: name, active: true}
	p.mu.Unlock()
}

func (p *progressTracker) reset(key string) {
	p.mu.Lock()
	if file := p.files[key]; file != nil {
		file.total = 0
		file.written = 0
		var currentBytes int64
		for _, current := range p.files {
			currentBytes += current.written
		}
		p.lastBytes = currentBytes
		p.lastRefresh = time.Now()
		p.lastSpeed = 0
	}
	p.mu.Unlock()
}

func (p *progressTracker) setTotal(key string, total int64) {
	p.mu.Lock()
	if file := p.files[key]; file != nil && total >= 0 {
		file.total = total
	}
	p.mu.Unlock()
}

func (p *progressTracker) add(key string, count int64) {
	if count <= 0 { return }
	p.mu.Lock()
	if file := p.files[key]; file != nil {
		file.written += count
		p.renderLocked(false)
	}
	p.mu.Unlock()
}

func (p *progressTracker) rewind(key string, count int64) {
	p.mu.Lock()
	defer p.mu.Unlock()
	if file := p.files[key]; file != nil && count > 0 {
		file.written -= count
		if file.written < 0 { file.written = 0 }
	}
}

func (p *progressTracker) finish(key string, success bool) {
	p.mu.Lock()
	if file := p.files[key]; file != nil {
		if success && file.total > 0 { file.written = file.total }
		file.active = false
		p.renderLocked(true)
		anyActive := false
		for _, current := range p.files {
			if current.active { anyActive = true; break }
		}
		if !anyActive {
			p.files = make(map[string]*progressFile)
			p.lastBytes = 0
			p.lastRefresh = time.Time{}
		}
	}
	p.mu.Unlock()
}

func (p *progressTracker) renderLocked(force bool) {
	now := time.Now()
	if !force && now.Sub(p.lastRefresh) < 500*time.Millisecond { return }
	var total, downloaded int64
	active := 0
	current := make([]string, 0, 3)
	for _, file := range p.files {
		total += file.total
		downloaded += file.written
		if file.active {
			active++
			if len(current) < 3 { current = append(current, file.name) }
		}
	}
	elapsed := now.Sub(p.lastRefresh).Seconds()
	if p.lastRefresh.IsZero() { elapsed = 0 }
	speed := float64(0)
	if elapsed > 0 { speed = float64(downloaded-p.lastBytes) / elapsed }
	if speed >= 0 { p.lastSpeed = speed }
	p.lastRefresh, p.lastBytes = now, downloaded
	percent := "未知"
	remaining := "未知"
	if total > 0 {
		value := float64(downloaded) / float64(total) * 100
		if value > 100 { value = 100 }
		percent = fmt.Sprintf("%.1f%%", value)
		if p.lastSpeed > 0 && downloaded < total {
			remaining = (time.Duration(float64(total-downloaded)/p.lastSpeed*float64(time.Second))).Round(time.Second).String()
		} else if downloaded >= total {
			remaining = "即将完成"
		}
	}
	bar := "────────────────────"
	if total > 0 {
		filled := int(float64(downloaded) / float64(total) * 20)
		if filled > 20 { filled = 20 }
		bar = strings.Repeat("█", filled) + strings.Repeat("░", 20-filled)
	}
	name := "—"
	if len(current) > 0 { name = strings.Join(current, "、") }
	progressMu.Lock()
	fmt.Printf("\r[更新下载] %s %s | %s/%s | %s/s | 剩余 %s | 活跃 %d | %s",
		bar, percent, humanSize(downloaded), humanSize(total), humanSize(int64(p.lastSpeed)), remaining, active, name)
	if force && active == 0 { fmt.Println() }
	progressMu.Unlock()
}

func progressBegin(key, name string) { downloadProgressUI.begin(key, name) }
func progressReset(key string) { downloadProgressUI.reset(key) }
func progressSetTotal(key string, total int64) { downloadProgressUI.setTotal(key, total) }
func progressFinish(key string, success bool) { downloadProgressUI.finish(key, success) }

func (w *downloadProgress) Write(data []byte) (int, error) {
	n, err := w.writer.Write(data)
	w.written += int64(n)
	downloadProgressUI.add(w.key, int64(n))
	return n, err
}

func humanSize(size int64) string {
	const unit = 1024.0
	if size < 1024 {
		return fmt.Sprintf("%d B", size)
	}
	value := float64(size)
	units := []string{"KB", "MB", "GB", "TB"}
	for _, suffix := range units {
		value /= unit
		if value < unit || suffix == "TB" {
			return fmt.Sprintf("%.1f %s", value, suffix)
		}
	}
	return fmt.Sprintf("%d B", size)
}

func downloadVerifiedEntry(entry Entry, destination string) error {
	if len(entry.Sources) == 0 {
		return downloadVerified(entry.Source, destination, entry.Algorithm, entry.Value)
	}
	var failures []string
	for _, source := range entry.Sources {
		if err := downloadVerified(source, destination, entry.Algorithm, entry.Value); err == nil {
			return nil
		} else {
			failures = append(failures, fmt.Sprintf("%s: %s", safeURLForLog(source), safeErrorForLog(err)))
			_ = os.Remove(destination)
		}
	}
	return fmt.Errorf("MRPack 的 %d 个下载候选均失败：%s", len(entry.Sources), strings.Join(failures, "；"))
}

func downloadVerified(source, destination, algorithm, expected string) error {
	if err := downloadFile(source, destination); err != nil {
		return err
	}
	if err := verifyDigest(destination, algorithm, expected); err != nil {
		_ = os.Remove(destination)
		return err
	}
	return nil
}

func downloadArchiveWithManualFallback(source, destination string) error {
	if err := downloadFile(source, destination); err == nil {
		return nil
	} else {
		fmt.Println()
		fmt.Println("自动下载完整包失败。可以把完整包下载到本地后，输入文件路径继续。")
		fmt.Println("直接按回车将取消本次更新。")
		fmt.Println("下载错误：", err)
		fmt.Print("完整包路径：")
		line, readErr := readConsoleLine()
		if readErr != nil && len(line) == 0 {
			return fmt.Errorf("自动下载失败且无法读取手动包路径：%w", err)
		}
		local := strings.Trim(strings.TrimSpace(line), "\"")
		if local == "" {
			return fmt.Errorf("完整包下载失败：%w", err)
		}
		info, statErr := os.Lstat(local)
		if statErr != nil || !info.Mode().IsRegular() || info.Mode()&os.ModeSymlink != 0 {
			return fmt.Errorf("手动指定的文件不存在、不是普通文件或是符号链接：%s", local)
		}
		if info.Size() > maxDownloadBytes {
			return fmt.Errorf("手动指定的完整包超过大小限制：%s", humanSize(info.Size()))
		}
		return copyFileLimited(local, destination, 0644, maxDownloadBytes)
	}
}

func parseManifest(data []byte, expectedKind string) (*Manifest, error) {
	if len(data) == 0 {
		return nil, errors.New("更新清单为空")
	}
	if len(data) > maxManifestBytes {
		return nil, fmt.Errorf("更新清单超过大小限制：%s", humanSize(int64(len(data))))
	}
	if bytes.HasPrefix(data, []byte{0xef, 0xbb, 0xbf}) {
		return nil, errors.New("更新清单不能包含 UTF-8 BOM")
	}
	if bytes.Contains(data, []byte("\r")) {
		return nil, errors.New("更新清单必须使用 LF 换行符")
	}
	if !bytes.HasSuffix(data, []byte("\n")) {
		return nil, errors.New("更新清单末尾缺少换行符")
	}
	text := string(data)
	lines := strings.Split(strings.TrimSuffix(text, "\n"), "\n")
	if len(lines) < 6 || lines[0] != "# DimensionWorks update manifest 1" {
		return nil, errors.New("更新清单头部无效")
	}
	headers := map[string]string{}
	for index := 1; index <= 4; index++ {
		line := lines[index]
		if !strings.HasPrefix(line, "# ") {
			return nil, errors.New("更新清单头部格式无效")
		}
		key, value, ok := strings.Cut(strings.TrimPrefix(line, "# "), " ")
		if !ok || value == "" || strings.Contains(value, " ") {
			return nil, errors.New("更新清单头部格式无效")
		}
		if _, exists := headers[key]; exists {
			return nil, fmt.Errorf("更新清单含重复头部：%s", key)
		}
		if key != "kind" && key != "version" && key != "release" && key != "base" {
			return nil, fmt.Errorf("更新清单含未知头部：%s", key)
		}
		headers[key] = value
	}
	for _, key := range []string{"kind", "version", "release", "base"} {
		if headers[key] == "" {
			return nil, fmt.Errorf("更新清单缺少头部：%s", key)
		}
	}
	if lines[5] != "action\tpath\talgorithm\tvalue\tsource" {
		return nil, errors.New("更新清单列头无效")
	}
	manifest := &Manifest{Kind: headers["kind"], Version: headers["version"], Release: headers["release"], Base: headers["base"], Bytes: append([]byte(nil), data...)}
	if manifest.Kind != "client" && manifest.Kind != "server" {
		return nil, errors.New("更新清单 kind 必须是 client 或 server")
	}
	if expectedKind != "" && manifest.Kind != expectedKind {
		return nil, fmt.Errorf("更新清单类型不匹配：需要 %s，实际为 %s", expectedKind, manifest.Kind)
	}
	if !versionRE.MatchString(manifest.Version) || manifest.Release != "v"+manifest.Version || !validReleaseTagOrNone(manifest.Base) {
		return nil, errors.New("更新清单版本号或发行标签无效")
	}
	if len(lines)-6 > maxManifestEntries {
		return nil, fmt.Errorf("更新清单条目超过上限：%d", maxManifestEntries)
	}
	seen := map[string]bool{}
	for lineIndex, line := range lines[6:] {
		fields := strings.Split(line, "\t")
		if len(fields) != 5 {
			return nil, fmt.Errorf("更新清单第 %d 行必须包含 5 个字段", lineIndex+7)
		}
		entry := Entry{Action: fields[0], Path: fields[1], Algorithm: fields[2], Value: fields[3], Source: fields[4]}
		if err := validateEntry(entry); err != nil {
			return nil, fmt.Errorf("更新清单第 %d 行：%w", lineIndex+7, err)
		}
		key := strings.ToLower(entry.Path)
		if seen[key] {
			return nil, fmt.Errorf("更新清单存在重复路径：%s", entry.Path)
		}
		seen[key] = true
		if reservedRelative(entry.Path) {
			return nil, fmt.Errorf("更新清单试图覆盖更新器元数据：%s", entry.Path)
		}
		if manifest.Kind == "server" && entry.Action != "archive" {
			return nil, fmt.Errorf("服务端清单只能包含 archive 条目：%s", entry.Path)
		}
		manifest.Entries = append(manifest.Entries, entry)
	}
	return manifest, nil
}

func validReleaseTagOrNone(tag string) bool {
	return tag == "none" || validReleaseTag(tag)
}

func validateEntry(entry Entry) error {
	if err := validateRelative(entry.Path); err != nil {
		return fmt.Errorf("目标路径无效：%w", err)
	}
	if entry.Action == "remote" {
		if entry.Algorithm != "sha512" || len(entry.Value) != 128 || !digestRE.MatchString(entry.Value) {
			return errors.New("remote 条目必须提供小写 SHA-512")
		}
		parsed, err := url.Parse(entry.Source)
		if err != nil || parsed.Host == "" || parsed.User != nil || parsed.Fragment != "" || (parsed.Scheme != "http" && parsed.Scheme != "https") || strings.ContainsAny(entry.Source, " \t\r\n") {
			return errors.New("remote source 必须是无空白的 HTTP(S) URL")
		}
		return nil
	}
	if entry.Action != "archive" {
		return fmt.Errorf("未知操作：%s", entry.Action)
	}
	if entry.Algorithm != "sha256" || len(entry.Value) != 64 || !digestRE.MatchString(entry.Value) {
		return errors.New("archive 条目必须提供小写 SHA-256")
	}
	if err := validateRelative(entry.Source); err != nil {
		return fmt.Errorf("归档源路径无效：%w", err)
	}
	return nil
}

func validateRelative(value string) error {
	if value == "" || value != strings.TrimSpace(value) || strings.Contains(value, "\\") || strings.HasPrefix(value, "/") || strings.Contains(value, ":") {
		return fmt.Errorf("必须是规范相对 POSIX 路径：%q", value)
	}
	for _, r := range value {
		if r < 32 || r == 127 {
			return fmt.Errorf("路径包含控制字符：%q", value)
		}
	}
	for _, part := range strings.Split(value, "/") {
		if part == "" || part == "." || part == ".." {
			return fmt.Errorf("路径包含不安全片段：%q", value)
		}
	}
	return nil
}

func reservedRelative(rel string) bool {
	normalized := strings.ToLower(strings.ReplaceAll(rel, "\\", "/"))
	if normalized == "version" || normalized == stateName || normalized == stateSignatureName || strings.HasPrefix(normalized, ".dimensionworks-update/") {
		return true
	}
	return isUpdaterName(filepath.Base(normalized))
}

func isUpdaterName(base string) bool {
	switch strings.ToLower(base) {
	case "update-dimensionworks-client.exe", "update-dimensionworks-server.exe",
		"update-dimensionworks-client.bat", "update-dimensionworks-server.bat",
		"update-dimensionworks-client.sh", "update-dimensionworks-server.sh",
		"update-dimensionworks-client-linux-amd64", "update-dimensionworks-server-linux-amd64",
		"update-dimensionworks-client-linux-arm64", "update-dimensionworks-server-linux-arm64",
		"update-dimensionworks-client-macos-amd64", "update-dimensionworks-server-macos-amd64",
		"update-dimensionworks-client-macos-arm64", "update-dimensionworks-server-macos-arm64",
		"update-dimensionworks-client-windows-amd64.exe", "update-dimensionworks-server-windows-amd64.exe":
		return true
	default:
		return false
	}
}

func openArchive(path string) (*Archive, error) {
	reader, err := zip.OpenReader(path)
	if err != nil {
		return nil, err
	}
	archive := &Archive{reader: reader, entries: map[string]*zip.File{}}
	var totalUncompressed uint64
	if len(reader.File) > maxArchiveEntries {
		archive.Close()
		return nil, fmt.Errorf("压缩包条目数量超过上限：%d", maxArchiveEntries)
	}
	for _, file := range reader.File {
		name := strings.TrimSuffix(file.Name, "/")
		if name == "" {
			continue
		}
		if err := validateRelative(name); err != nil {
			archive.Close()
			return nil, fmt.Errorf("压缩包路径无效：%s：%w", file.Name, err)
		}
		if file.Mode()&os.ModeSymlink != 0 {
			archive.Close()
			return nil, fmt.Errorf("压缩包包含符号链接：%s", file.Name)
		}
		if file.FileInfo().IsDir() {
			continue
		}
		if file.UncompressedSize64 > maxArchiveFileBytes {
			archive.Close()
			return nil, fmt.Errorf("压缩包单文件展开大小超过上限：%s", name)
		}
		if totalUncompressed > maxArchiveTotalBytes-file.UncompressedSize64 {
			archive.Close()
			return nil, fmt.Errorf("压缩包总展开大小超过上限：%s", name)
		}
		totalUncompressed += file.UncompressedSize64
		if isUpdaterName(filepath.Base(name)) {
			archive.Close()
			return nil, fmt.Errorf("压缩包不允许包含更新器：%s", name)
		}
		key := strings.ToLower(name)
		if _, exists := archive.entries[key]; exists {
			archive.Close()
			return nil, fmt.Errorf("压缩包含重复或大小写冲突的路径：%s", name)
		}
		archive.entries[key] = file
	}
	return archive, nil
}

func (a *Archive) Close() error {
	if a == nil || a.reader == nil {
		return nil
	}
	return a.reader.Close()
}

func embeddedManifest(archive *Archive, kind string) (*Manifest, error) {
	file := archive.entries[strings.ToLower(manifestName)]
	if file == nil {
		return nil, errors.New("完整更新包缺少内嵌更新清单")
	}
	if file.UncompressedSize64 > maxManifestBytes {
		return nil, fmt.Errorf("完整包内的更新清单超过大小限制：%s", humanSize(int64(file.UncompressedSize64)))
	}
	reader, err := file.Open()
	if err != nil {
		return nil, err
	}
	data, readErr := io.ReadAll(io.LimitReader(reader, maxManifestBytes+1))
	closeErr := reader.Close()
	if readErr != nil {
		return nil, readErr
	}
	if closeErr != nil {
		return nil, closeErr
	}
	return parseManifest(data, kind)
}

type modrinthIndex struct {
	FormatVersion int                 `json:"formatVersion"`
	Game          string              `json:"game"`
	Files         []modrinthIndexFile `json:"files"`
}

type modrinthIndexFile struct {
	Path      string            `json:"path"`
	Hashes    map[string]string `json:"hashes"`
	Downloads []string          `json:"downloads"`
	Env       map[string]string `json:"env"`
}

func validateClientMRPackIndex(archive *Archive, target *Manifest) (int, error) {
	if archive == nil {
		return 0, errors.New("缺少 MRPack")
	}
	file := archive.entries["modrinth.index.json"]
	if file == nil {
		return 0, errors.New("MRPack 缺少 modrinth.index.json")
	}
	if file.UncompressedSize64 > maxManifestBytes {
		return 0, fmt.Errorf("MRPack 索引超过大小限制：%s", humanSize(int64(file.UncompressedSize64)))
	}
	reader, err := file.Open()
	if err != nil {
		return 0, err
	}
	data, readErr := io.ReadAll(io.LimitReader(reader, maxManifestBytes+1))
	closeErr := reader.Close()
	if readErr != nil {
		return 0, readErr
	}
	if closeErr != nil {
		return 0, closeErr
	}
	if int64(len(data)) > maxManifestBytes {
		return 0, errors.New("MRPack 索引超过大小限制")
	}
	var index modrinthIndex
	if err := json.Unmarshal(data, &index); err != nil {
		return 0, fmt.Errorf("解析 modrinth.index.json 失败：%w", err)
	}
	if index.FormatVersion != 1 || index.Game != "minecraft" {
		return 0, errors.New("MRPack 索引格式版本或游戏标识不受支持")
	}

	manifestFiles := make(map[string]int)
	for i := range target.Entries {
		if target.Entries[i].Action == "remote" {
			manifestFiles[strings.ToLower(target.Entries[i].Path)] = i
		}
	}
	seen := make(map[string]bool)
	indexed := 0
	for _, item := range index.Files {
		if item.Env["client"] == "unsupported" {
			continue
		}
		if err := validateRelative(item.Path); err != nil {
			return 0, fmt.Errorf("MRPack 文件路径无效：%w", err)
		}
		key := strings.ToLower(item.Path)
		if seen[key] {
			return 0, fmt.Errorf("MRPack 索引包含重复路径：%s", item.Path)
		}
		seen[key] = true
		sha512Value := strings.ToLower(item.Hashes["sha512"])
		if len(item.Downloads) == 0 {
			return 0, fmt.Errorf("%s 缺少下载地址", item.Path)
		}
		sources := make([]string, 0, len(item.Downloads))
		seenSources := make(map[string]bool)
		for _, source := range item.Downloads {
			candidate := Entry{
				Action: "remote", Path: item.Path, Algorithm: "sha512",
				Value: sha512Value, Source: source,
			}
			if err := validateEntry(candidate); err != nil {
				return 0, fmt.Errorf("%s 下载候选地址无效：%w", item.Path, err)
			}
			if !seenSources[source] {
				seenSources[source] = true
				sources = append(sources, source)
			}
		}
		candidate := Entry{
			Action: "remote", Path: item.Path, Algorithm: "sha512",
			Value: sha512Value, Source: sources[0],
		}
		entryIndex, ok := manifestFiles[key]
		if !ok || target.Entries[entryIndex].Value != candidate.Value ||
			target.Entries[entryIndex].Source != candidate.Source {
			return 0, fmt.Errorf("MRPack 索引与更新清单不一致：%s", item.Path)
		}
		target.Entries[entryIndex].Sources = sources
		indexed++
	}
	if indexed != len(manifestFiles) {
		return 0, fmt.Errorf("MRPack 索引文件数与更新清单不一致：索引 %d，清单 %d", indexed, len(manifestFiles))
	}
	return indexed, nil
}

func stageArchiveEntries(archive *Archive, entries []Entry, stageRoot string) error {
	return extractArchiveEntries(archive, entries, stageRoot, true)
}

// Incremental overlays must finish before their bytes can match the target digest.
// Callers disabling verification must verify every final staged entry before apply.
func extractArchiveEntries(archive *Archive, entries []Entry, stageRoot string, verify bool) error {
	if archive == nil {
		return errors.New("没有可用的更新归档")
	}
	for _, entry := range entries {
		if entry.Action != "archive" {
			continue
		}
		file := archive.entries[strings.ToLower(entry.Source)]
		if file == nil {
			return fmt.Errorf("归档缺少文件：%s", entry.Source)
		}
		stage, err := stagePath(stageRoot, entry.Path)
		if err != nil {
			return err
		}
		if err := os.MkdirAll(filepath.Dir(stage), 0755); err != nil {
			return err
		}
		reader, err := file.Open()
		if err != nil {
			return err
		}
		mode := file.Mode().Perm() & 0755
		if mode == 0 {
			mode = 0644
		}
		output, err := os.OpenFile(stage, os.O_CREATE|os.O_TRUNC|os.O_WRONLY, mode)
		if err != nil {
			_ = reader.Close()
			return err
		}
		written, copyErr := io.Copy(output, io.LimitReader(reader, int64(file.UncompressedSize64)+1))
		syncErr := output.Sync()
		closeErr := output.Close()
		readerErr := reader.Close()
		if copyErr != nil {
			return copyErr
		}
		if uint64(written) != file.UncompressedSize64 {
			return fmt.Errorf("归档文件实际展开长度不匹配 %s：%d/%d", entry.Path, written, file.UncompressedSize64)
		}
		if syncErr != nil {
			return syncErr
		}
		if closeErr != nil {
			return closeErr
		}
		if readerErr != nil {
			return readerErr
		}
		if verify {
			if err := verifyDigest(stage, entry.Algorithm, entry.Value); err != nil {
				return fmt.Errorf("归档文件摘要不匹配 %s：%w", entry.Path, err)
			}
		}
	}
	return nil
}

func clearStaged(stageRoot string, entries []Entry) {
	for _, entry := range entries {
		if entry.Action == "archive" {
			path, err := stagePath(stageRoot, entry.Path)
			if err == nil {
				_ = os.Remove(path)
			}
		}
	}
}

func stagePath(stageRoot, rel string) (string, error) {
	if err := validateRelative(rel); err != nil {
		return "", err
	}
	target := filepath.Join(stageRoot, filepath.FromSlash(rel))
	base, err := filepath.Abs(stageRoot)
	if err != nil {
		return "", err
	}
	target, err = filepath.Abs(target)
	if err != nil {
		return "", err
	}
	relPath, err := filepath.Rel(base, target)
	if err != nil || relPath == ".." || strings.HasPrefix(relPath, ".."+string(os.PathSeparator)) {
		return "", fmt.Errorf("暂存路径越界：%s", rel)
	}
	return target, nil
}

func securePath(root, rel string) (string, bool, error) {
	if err := validateRelative(rel); err != nil {
		return "", false, err
	}
	if reservedRelative(rel) {
		return "", false, fmt.Errorf("禁止操作保留路径：%s", rel)
	}
	absRoot, err := filepath.Abs(root)
	if err != nil {
		return "", false, err
	}
	target := filepath.Join(absRoot, filepath.FromSlash(rel))
	target, err = filepath.Abs(target)
	if err != nil {
		return "", false, err
	}
	relative, err := filepath.Rel(absRoot, target)
	if err != nil || relative == ".." || strings.HasPrefix(relative, ".."+string(os.PathSeparator)) {
		return "", false, fmt.Errorf("目标路径越界：%s", rel)
	}
	current := absRoot
	for _, segment := range strings.Split(filepath.ToSlash(relative), "/") {
		current = filepath.Join(current, filepath.FromSlash(segment))
		info, statErr := os.Lstat(current)
		if statErr != nil {
			if os.IsNotExist(statErr) {
				return target, false, nil
			}
			return "", false, statErr
		}
		if info.Mode()&os.ModeSymlink != 0 {
			return "", false, fmt.Errorf("目标路径包含符号链接：%s", rel)
		}
	}
	info, statErr := os.Lstat(target)
	if statErr != nil {
		if os.IsNotExist(statErr) {
			return target, false, nil
		}
		return "", false, statErr
	}
	if info.IsDir() {
		return "", true, fmt.Errorf("目标文件路径已被目录占用：%s", rel)
	}
	return target, true, nil
}

func hashFile(path, algorithm string) (string, error) {
	file, err := os.Open(path)
	if err != nil {
		return "", err
	}
	defer file.Close()
	switch algorithm {
	case "sha256":
		hash := sha256.New()
		if _, err := io.Copy(hash, file); err != nil {
			return "", err
		}
		return hex.EncodeToString(hash.Sum(nil)), nil
	case "sha512":
		hash := sha512.New()
		if _, err := io.Copy(hash, file); err != nil {
			return "", err
		}
		return hex.EncodeToString(hash.Sum(nil)), nil
	default:
		return "", fmt.Errorf("不支持的摘要算法：%s", algorithm)
	}
}

func verifyDigest(path, algorithm, expected string) error {
	actual, err := hashFile(path, algorithm)
	if err != nil {
		return err
	}
	if actual != expected {
		return fmt.Errorf("摘要不匹配：预期 %s，实际 %s", expected, actual)
	}
	return nil
}

func readRegularFile(path string, maxBytes int64) ([]byte, error) {
	info, err := os.Lstat(path)
	if err != nil { return nil, err }
	if info.Mode()&os.ModeSymlink != 0 || !info.Mode().IsRegular() {
		return nil, fmt.Errorf("文件不是普通文件：%s", path)
	}
	if maxBytes >= 0 && info.Size() > maxBytes {
		return nil, fmt.Errorf("文件超过大小限制：%s", path)
	}
	file, err := os.Open(path)
	if err != nil { return nil, err }
	defer file.Close()
	openedInfo, err := file.Stat()
	if err != nil { return nil, err }
	if !os.SameFile(info, openedInfo) { return nil, fmt.Errorf("文件在打开期间发生变化：%s", path) }
	limit := int64(^uint64(0) >> 1)
	if maxBytes >= 0 { limit = maxBytes+1 }
	data, err := io.ReadAll(io.LimitReader(file, limit))
	if err != nil { return nil, err }
	if maxBytes >= 0 && int64(len(data)) > maxBytes { return nil, fmt.Errorf("文件超过大小限制：%s", path) }
	return data, nil
}

func readState(root, path, kind string) (*Manifest, error) {
	info, err := os.Lstat(path)
	if err != nil {
		if os.IsNotExist(err) { return nil, nil }
		return nil, err
	}
	if info.Mode()&os.ModeSymlink != 0 || !info.Mode().IsRegular() {
		return nil, errors.New("state.tsv 必须是普通文件")
	}
	if info.Size() > maxManifestBytes {
		return nil, fmt.Errorf("state.tsv 超过大小限制：%s", humanSize(info.Size()))
	}
	signaturePath := filepath.Join(root, filepath.FromSlash(stateSignatureName))
	signatureInfo, err := os.Lstat(signaturePath)
	if os.IsNotExist(err) {
		fmt.Println("[保护] 检测到旧版未签名状态文件，将重新扫描本地文件，不信任旧删除记录")
		return nil, nil
	}
	if err != nil { return nil, err }
	if signatureInfo.Mode()&os.ModeSymlink != 0 || !signatureInfo.Mode().IsRegular() || signatureInfo.Size() > 128 {
		return nil, errors.New("state.tsv.hmac 不安全或格式无效")
	}
	keyPath := filepath.Join(root, filepath.FromSlash(stateKeyName))
	keyInfo, err := os.Lstat(keyPath)
	if err != nil { return nil, fmt.Errorf("状态签名密钥不可用：%w", err) }
	if keyInfo.Mode()&os.ModeSymlink != 0 || !keyInfo.Mode().IsRegular() || keyInfo.Size() != 32 {
		return nil, errors.New("状态签名密钥文件不安全或长度无效")
	}
	key, err := readRegularFile(keyPath, 32)
	if err != nil { return nil, err }
	signature, err := readRegularFile(signaturePath, 128)
	if err != nil { return nil, err }
	provided, err := hex.DecodeString(strings.TrimSpace(string(signature)))
	if err != nil || len(provided) != sha256.Size {
		return nil, errors.New("state.tsv.hmac 签名格式无效")
	}
	data, err := readRegularFile(path, maxManifestBytes)
	if err != nil { return nil, err }
	mac := hmac.New(sha256.New, key)
	_, _ = mac.Write(data)
	if !hmac.Equal(provided, mac.Sum(nil)) {
		return nil, errors.New("本地更新状态签名校验失败；状态文件可能被修改，请先备份 .dimensionworks-update 后再修复")
	}
	return parseManifest(data, kind)
}

func loadOrCreateStateKey(updateRoot string) ([]byte, error) {
	path := filepath.Join(updateRoot, "state.key")
	if info, err := os.Lstat(path); err == nil {
		if info.Mode()&os.ModeSymlink != 0 || !info.Mode().IsRegular() || info.Size() != 32 {
			return nil, errors.New("状态签名密钥文件不安全或长度无效")
		}
		return readRegularFile(path, 32)
	} else if !os.IsNotExist(err) {
		return nil, err
	}
	key := make([]byte, 32)
	if _, err := rand.Read(key); err != nil { return nil, err }
	temp, err := os.CreateTemp(updateRoot, ".state-key-*")
	if err != nil { return nil, err }
	tempPath := temp.Name()
	defer os.Remove(tempPath)
	if _, err := temp.Write(key); err != nil { _ = temp.Close(); return nil, err }
	if err := temp.Sync(); err != nil { _ = temp.Close(); return nil, err }
	if err := temp.Chmod(0600); err != nil { _ = temp.Close(); return nil, err }
	if err := temp.Close(); err != nil { return nil, err }
	if err := replaceExistingFile(tempPath, path); err != nil { return nil, err }
	return key, nil
}

func scanInstalledFiles(root, kind string) (*Manifest, error) {
	roots := []string{"mods", "config", "defaultconfigs", "kubejs", "libraries"}
	rootFiles := []string{"README.md", "start.sh", "start.bat", "run.sh", "run.bat"}
	if kind == "client" {
		roots = []string{"mods", "shaderpacks", "config", "defaultconfigs", "kubejs", "resourcepacks", "local"}
		rootFiles = []string{"modrinth.index.json", "options.txt", "log4j2.xml", "rhino.local.properties", "CustomSkinLoader/CustomSkinLoader.json", "minemenu/menu.json"}
	}
	// This inventory is only for file hashes. It cannot supply an installed version.
	state := &Manifest{Kind: kind}
	fmt.Println("[状态] 首次初始化，正在扫描实际文件摘要")
	seen := map[string]bool{}
	addFile := func(rel string) error {
		if reservedRelative(rel) || preservePath(root, kind, rel) || strings.EqualFold(rel, "VERSION") {
			return nil
		}
		path, exists, err := securePath(root, rel)
		if err != nil {
			return err
		}
		if !exists {
			return nil
		}
		info, err := os.Stat(path)
		if err != nil || !info.Mode().IsRegular() {
			return nil
		}
		digest, err := hashFile(path, "sha256")
		if err != nil {
			return err
		}
		key := strings.ToLower(rel)
		if !seen[key] {
			if len(state.Entries) >= maxManifestEntries {
				return fmt.Errorf("本地文件数量超过更新清单上限：%d", maxManifestEntries)
			}
			seen[key] = true
			state.Entries = append(state.Entries, Entry{Action: "archive", Path: rel, Algorithm: "sha256", Value: digest, Source: rel})
		}
		return nil
	}
	for _, rootName := range roots {
		start := filepath.Join(root, rootName)
		startInfo, statErr := os.Lstat(start)
		if os.IsNotExist(statErr) { continue }
		if statErr != nil { return nil, fmt.Errorf("检查实例目录 %s 失败：%w", rootName, statErr) }
		if startInfo.Mode()&os.ModeSymlink != 0 || !startInfo.IsDir() {
			return nil, fmt.Errorf("实例目录 %s 不是普通目录", rootName)
		}
		err := filepath.WalkDir(start, func(path string, entry os.DirEntry, walkErr error) error {
			if walkErr != nil {
				return fmt.Errorf("扫描实例文件 %s 失败：%w", path, walkErr)
			}
			if entry.Type()&os.ModeSymlink != 0 {
				if entry.IsDir() {
					return filepath.SkipDir
				}
				return nil
			}
			if entry.IsDir() {
				return nil
			}
			rel, err := filepath.Rel(root, path)
			if err != nil {
				return err
			}
			if err := addFile(filepath.ToSlash(rel)); err != nil {
				return err
			}
			return nil
		})
		if err != nil {
			return nil, err
		}
	}
	for _, rel := range rootFiles {
		if err := addFile(rel); err != nil {
			return nil, err
		}
	}
	sort.Slice(state.Entries, func(i, j int) bool { return state.Entries[i].Path < state.Entries[j].Path })
	return state, nil
}

func renderManifest(manifest *Manifest) []byte {
	lines := []string{
		"# DimensionWorks update manifest 1",
		"# kind " + manifest.Kind,
		"# version " + manifest.Version,
		"# release " + manifest.Release,
		"# base " + manifest.Base,
		"action\tpath\talgorithm\tvalue\tsource",
	}
	for _, entry := range manifest.Entries {
		lines = append(lines, strings.Join([]string{entry.Action, entry.Path, entry.Algorithm, entry.Value, entry.Source}, "\t"))
	}
	return []byte(strings.Join(lines, "\n") + "\n")
}

func backupModifiedFiles(root, updateRoot string, current *Manifest, selected []Entry, forceBackup ...bool) (string, error) {
	force := len(forceBackup) > 0 && forceBackup[0]
	if current == nil || len(selected) == 0 {
		return "", nil
	}
	managed := make(map[string]Entry, len(current.Entries))
	for _, entry := range current.Entries {
		managed[strings.ToLower(entry.Path)] = entry
	}
	backupDir := ""
	for _, entry := range selected {
		previous, managedBefore := managed[strings.ToLower(entry.Path)]
		if !managedBefore && !force { continue }
		source, exists, err := securePath(root, entry.Path)
		if err != nil { return backupDir, err }
		if !exists { continue }
		if managedBefore {
			actual, err := hashFile(source, previous.Algorithm)
			if err != nil { return backupDir, fmt.Errorf("检查 %s 失败：%w", entry.Path, err) }
			if actual == previous.Value && !force { continue }
		}
		if backupDir == "" {
			backupDir, err = os.MkdirTemp(updateRoot, "user-backups-")
			if err != nil {
				return "", err
			}
		}
		destination := filepath.Join(backupDir, filepath.FromSlash(entry.Path))
		if err := os.MkdirAll(filepath.Dir(destination), 0700); err != nil {
			return backupDir, err
		}
		info, err := os.Stat(source)
		if err != nil {
			return backupDir, err
		}
		if err := copyFile(source, destination, info.Mode().Perm()); err != nil {
			return backupDir, fmt.Errorf("备份 %s 失败：%w", entry.Path, err)
		}
		fmt.Printf("[保护] 本地修改将在更新前备份：%s\n", entry.Path)
	}
	return backupDir, nil
}

func selectChanges(root, kind string, current, target *Manifest) ([]Entry, []string, error) {
	currentEntries := map[string]Entry{}
	targetEntries := map[string]Entry{}
	for _, entry := range current.Entries {
		currentEntries[strings.ToLower(entry.Path)] = entry
	}
	for _, entry := range target.Entries {
		targetEntries[strings.ToLower(entry.Path)] = entry
	}
	selected := make([]Entry, 0)
	for _, entry := range target.Entries {
		path, exists, err := securePath(root, entry.Path)
		if err != nil {
			return nil, nil, err
		}
		if preserveExistingPath(root, kind, entry.Path, exists) {
			continue
		}
		if exists {
			digest, err := hashFile(path, entry.Algorithm)
			if err != nil {
				return nil, nil, err
			}
			if digest == entry.Value {
				continue
			}
			if entry.Action == "archive" {
				if previous, ok := currentEntries[strings.ToLower(entry.Path)]; ok && previous.Value == entry.Value {
					// The package has not changed this file; preserve local edits.
					continue
				}
			}
		}
		selected = append(selected, entry)
	}
	deletions := make([]string, 0)
	for _, old := range current.Entries {
		key := strings.ToLower(old.Path)
		if _, exists := targetEntries[key]; exists || preservePath(root, kind, old.Path) {
			continue
		}
		path, exists, err := securePath(root, old.Path)
		if err != nil {
			return nil, nil, err
		}
		if exists {
			digest, hashErr := hashFile(path, old.Algorithm)
			if hashErr != nil {
				return nil, nil, fmt.Errorf("检查待删除文件 %s 失败：%w", old.Path, hashErr)
			}
			if digest != old.Value {
				fmt.Printf("[保护] 文件已被本地修改，跳过自动删除：%s\n", old.Path)
				continue
			}
			deletions = append(deletions, old.Path)
		}
	}
	sort.Slice(selected, func(i, j int) bool { return selected[i].Path < selected[j].Path })
	sort.Strings(deletions)
	return selected, deletions, nil
}

func preservePath(root, kind, rel string) bool {
	normalized := strings.ToLower(strings.ReplaceAll(rel, "\\", "/"))
	parts := strings.Split(normalized, "/")
	if kind == "server" {
		for _, prefix := range []string{"config/", "defaultconfigs/", "plugins/luckperms/", "luckperms/"} {
			if strings.HasPrefix(normalized, prefix) {
				return true
			}
		}
		if configuredWorldPath(root, normalized) {
			return true
		}
	}
	if len(parts) > 0 {
		switch parts[0] {
		case ".dimensionworks-update", "logs", "crash-reports", "screenshots", "world", "world_nether", "world_the_end":
			return true
		}
		if kind == "client" && parts[0] == "saves" {
			return true
		}
		if kind == "server" && parts[0] == "debug" {
			return true
		}
	}
	if kind == "client" {
		return normalized == "dimensionworks.jar" || normalized == "dimensionworks.json"
	}
	if kind == "server" {
		switch normalized {
		case "eula.txt", "server.properties", "user_jvm_args.txt", "usercache.json", "ops.json", "whitelist.json", "banned-ips.json", "banned-players.json":
			return true
		}
	}
	return isUpdaterName(filepath.Base(normalized))
}

func configuredWorldPath(root, rel string) bool {
	data, err := os.ReadFile(filepath.Join(root, "server.properties"))
	if err != nil {
		return false
	}
	worldName := ""
	for _, line := range strings.Split(strings.ReplaceAll(string(data), "\r\n", "\n"), "\n") {
		line = strings.TrimSpace(line)
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		key, value, ok := strings.Cut(line, "=")
		if ok && strings.EqualFold(strings.TrimSpace(key), "level-name") {
			worldName = strings.TrimSpace(value)
			break
		}
	}
	worldName = strings.ToLower(strings.ReplaceAll(worldName, "\\", "/"))
	if worldName == "" || validateRelative(worldName) != nil {
		return false
	}
	for _, candidate := range []string{worldName, worldName + "_nether", worldName + "_the_end"} {
		if rel == candidate || strings.HasPrefix(rel, candidate+"/") {
			return true
		}
	}
	return false
}

func preserveExistingPath(root, kind, rel string, exists bool) bool {
	return exists && preservePath(root, kind, rel)
}

type Change struct {
	Path   string
	Staged string
	Delete bool
}

type Applied struct {
	Path       string
	Backup     string
	Existed    bool
	TargetPath string
	Mode       os.FileMode
	Done       bool
}

func truncateIncompleteJournalTail(file *os.File) error {
	end, err := file.Seek(0, io.SeekEnd)
	if err != nil || end == 0 { return err }
	if end > maxTransactionJournalBytes { return errors.New("事务日志超过大小限制") }
	var last [1]byte
	if _, err := file.ReadAt(last[:], end-1); err != nil { return err }
	if last[0] == '\n' { return nil }
	position := end
	for position > 0 {
		start := position - 4096
		if start < 0 { start = 0 }
		chunk := make([]byte, position-start)
		if _, err := file.ReadAt(chunk, start); err != nil { return err }
		if index := bytes.LastIndexByte(chunk, '\n'); index >= 0 {
			if err := file.Truncate(start+int64(index)+1); err != nil { return err }
			_, err := file.Seek(0, io.SeekEnd)
			return err
		}
		position = start
	}
	if err := file.Truncate(0); err != nil { return err }
	_, err = file.Seek(0, io.SeekEnd)
	return err
}

func appendTransactionJournal(work string, item Applied) error {
	path := filepath.Join(work, "transaction.jsonl")
	file, err := os.OpenFile(path, os.O_CREATE|os.O_RDWR, 0600)
	if err != nil { return err }
	defer file.Close()
	if err := truncateIncompleteJournalTail(file); err != nil { return err }
	start, err := file.Seek(0, io.SeekEnd)
	if err != nil { return err }
	payload, err := json.Marshal(item)
	if err != nil { return err }
	payload = append(payload, '\n')
	if start+int64(len(payload)) > maxTransactionJournalBytes { return errors.New("事务日志超过大小限制") }
	n, writeErr := file.Write(payload)
	if writeErr != nil || n != len(payload) {
		_ = file.Truncate(start)
		_ = file.Sync()
		if writeErr != nil { return writeErr }
		return io.ErrShortWrite
	}
	if err := file.Sync(); err != nil {
		_ = file.Truncate(start)
		_ = file.Sync()
		return err
	}
	return nil
}

func recoverInterruptedTransactions(root, updateRoot string) error {
	entries, err := os.ReadDir(updateRoot)
	if err != nil { return err }
	for _, entry := range entries {
		if !entry.IsDir() || !strings.HasPrefix(entry.Name(), "work-") { continue }
		work := filepath.Join(updateRoot, entry.Name())
		info, err := os.Lstat(work)
		if err != nil { return err }
		if info.Mode()&os.ModeSymlink != 0 || !info.IsDir() { return fmt.Errorf("事务目录不安全：%s", work) }
		journal := filepath.Join(work, "transaction.jsonl")
		journalInfo, err := os.Lstat(journal)
		if os.IsNotExist(err) {
			// No journal means no target mutation could have started; downloads
			// that can resume live outside work-* and are not removed here.
			if err := os.RemoveAll(work); err != nil { return fmt.Errorf("清理未提交的临时目录失败：%w", err) }
			continue
		}
		if err != nil { return err }
		if journalInfo.Mode()&os.ModeSymlink != 0 || !journalInfo.Mode().IsRegular() { return fmt.Errorf("事务日志不是普通文件：%s", journal) }
		if journalInfo.Size() > maxTransactionJournalBytes {
			return fmt.Errorf("事务日志超过大小限制：%s", journal)
		}
		data, err := readRegularFile(journal, maxTransactionJournalBytes)
		if err != nil { return err }
		completeLength := len(data)
		if completeLength > 0 && data[completeLength-1] != '\n' {
			if last := bytes.LastIndexByte(data, '\n'); last < 0 { completeLength = 0 } else { completeLength = last+1 }
		}
		var records []Applied
		done := false
		for _, line := range bytes.Split(data[:completeLength], []byte{'\n'}) {
			if len(line) == 0 { continue }
			var item Applied
			if err := json.Unmarshal(line, &item); err != nil {
				return fmt.Errorf("事务日志格式损坏：%s：%w", journal, err)
			}
			if item.Done { done = true; break }
			records = append(records, item)
			if len(records) > maxManifestEntries {
				return fmt.Errorf("事务日志条目超过上限：%s", journal)
			}
		}
		if !done {
			journalFile, err := os.OpenFile(journal, os.O_WRONLY, 0600)
			if err != nil { return err }
			if err := journalFile.Truncate(int64(completeLength)); err != nil { _ = journalFile.Close(); return err }
			if err := journalFile.Sync(); err != nil { _ = journalFile.Close(); return err }
			if err := journalFile.Close(); err != nil { return err }
			fmt.Println("[恢复] 检测到上次未完成的更新事务，正在恢复原文件")
			for i := len(records)-1; i >= 0; i-- {
				item := records[i]
				target, exists, err := securePathAllowMetadata(root, item.Path)
				if err != nil { return fmt.Errorf("恢复目标 %s 不安全：%w", item.Path, err) }
				if item.Existed {
					backupRoot := filepath.Join(work, "backup")
					backupAbs, err := filepath.Abs(item.Backup)
					if err != nil { return err }
					baseAbs, err := filepath.Abs(backupRoot)
					if err != nil { return err }
					rel, err := filepath.Rel(baseAbs, backupAbs)
					if err != nil || rel == ".." || strings.HasPrefix(rel, ".."+string(os.PathSeparator)) { return fmt.Errorf("事务备份路径越界：%s", item.Path) }
					backupInfo, err := os.Lstat(backupAbs)
					if err != nil { return fmt.Errorf("事务备份缺失 %s：%w", item.Path, err) }
					if backupInfo.Mode()&os.ModeSymlink != 0 || !backupInfo.Mode().IsRegular() { return fmt.Errorf("事务备份不是普通文件：%s", item.Path) }
					var created []string
					if err := ensureParentDirectories(root, target, &created); err != nil { return err }
					if err := replaceFromFile(backupAbs, target); err != nil { return fmt.Errorf("恢复 %s 失败：%w", item.Path, err) }
					if err := os.Chmod(target, item.Mode); err != nil { return fmt.Errorf("恢复 %s 权限失败：%w", item.Path, err) }
				} else if exists {
					if err := os.Remove(target); err != nil { return fmt.Errorf("清理新文件 %s 失败：%w", item.Path, err) }
				}
			}
			if err := appendTransactionJournal(work, Applied{Done: true}); err != nil { return err }
		}
		if err := os.RemoveAll(work); err != nil { return fmt.Errorf("清理已恢复事务目录失败：%w", err) }
	}
	return nil
}

func revalidateTransactionTarget(root, rel, expectedPath string, expectedExists bool) error {
	path, exists, err := securePathAllowMetadata(root, rel)
	if err != nil { return err }
	if path != expectedPath || exists != expectedExists {
		return fmt.Errorf("更新目标在事务期间发生变化：%s", rel)
	}
	return nil
}

func applyTransaction(root, work string, changes []Change) error {
	backupRoot := filepath.Join(work, "backup")
	applied := make([]Applied, 0, len(changes))
	createdDirs := make([]string, 0)
	rollback := func() error {
		var failures []string
		for i := len(applied) - 1; i >= 0; i-- {
			item := applied[i]
			if item.Existed {
				var recoveryDirs []string
				if err := ensureParentDirectories(root, item.TargetPath, &recoveryDirs); err != nil {
					failures = append(failures, err.Error())
					continue
				}
				if err := replaceFromFile(item.Backup, item.TargetPath); err != nil {
					failures = append(failures, fmt.Sprintf("%s: %v", item.Path, err))
					continue
				}
				if err := os.Chmod(item.TargetPath, item.Mode); err != nil {
					failures = append(failures, fmt.Sprintf("%s 权限恢复失败：%v", item.Path, err))
				}
			} else {
				rollbackPath, exists, pathErr := securePathAllowMetadata(root, item.Path)
				if pathErr != nil {
					failures = append(failures, fmt.Sprintf("%s 回滚路径不安全：%v", item.Path, pathErr))
					continue
				}
				if exists {
					if rollbackPath != item.TargetPath {
						failures = append(failures, fmt.Sprintf("%s 回滚目标发生变化", item.Path))
						continue
					}
					if err := os.Remove(rollbackPath); err != nil {
						failures = append(failures, fmt.Sprintf("%s: %v", item.Path, err))
					}
				}
			}
		}
		for i := len(createdDirs) - 1; i >= 0; i-- {
			_ = os.Remove(createdDirs[i])
		}
		if len(failures) == 0 {
			if err := appendTransactionJournal(work, Applied{Done: true}); err != nil {
				failures = append(failures, "写入回滚完成标记失败："+err.Error())
			}
		}
		if len(failures) > 0 {
			return fmt.Errorf("回滚未完全成功：%s", strings.Join(failures, "; "))
		}
		return nil
	}

	for _, change := range changes {
		target, exists, err := securePathAllowMetadata(root, change.Path)
		if err != nil {
			if rollbackErr := rollback(); rollbackErr != nil {
				return fmt.Errorf("应用 %s 失败：%v；%v", change.Path, err, rollbackErr)
			}
			return fmt.Errorf("应用 %s 失败，已回滚：%w", change.Path, err)
		}
		targetMode := os.FileMode(0644)
		if exists {
			info, err := os.Stat(target)
			if err != nil || !info.Mode().IsRegular() {
				if rollbackErr := rollback(); rollbackErr != nil {
					return fmt.Errorf("目标 %s 不是普通文件；%v", change.Path, rollbackErr)
				}
				return fmt.Errorf("目标不是普通文件：%s", change.Path)
			}
			targetMode = info.Mode().Perm()
		}
		backup := filepath.Join(backupRoot, filepath.FromSlash(change.Path))
		if exists {
			if err := os.MkdirAll(filepath.Dir(backup), 0755); err != nil {
				_ = rollback()
				return err
			}
			if err := copyFile(target, backup, 0600); err != nil {
				_ = rollback()
				return fmt.Errorf("无法备份 %s：%w", change.Path, err)
			}
		}
		item := Applied{Path: change.Path, Backup: backup, Existed: exists, TargetPath: target, Mode: targetMode}
		if err := appendTransactionJournal(work, item); err != nil {
			if rollbackErr := rollback(); rollbackErr != nil {
				return fmt.Errorf("记录事务日志失败：%v；%v", err, rollbackErr)
			}
			return fmt.Errorf("记录事务日志失败，已回滚：%w", err)
		}
		applied = append(applied, item)

		if change.Delete {
			if err := revalidateTransactionTarget(root, change.Path, target, exists); err != nil {
				if rollbackErr := rollback(); rollbackErr != nil { return fmt.Errorf("删除前路径复核失败：%v；%v", err, rollbackErr) }
				return fmt.Errorf("删除前路径复核失败，已回滚：%w", err)
			}
			if exists {
				if err := os.Remove(target); err != nil {
					if rollbackErr := rollback(); rollbackErr != nil {
						return fmt.Errorf("删除 %s 失败：%v；%v", change.Path, err, rollbackErr)
					}
					return fmt.Errorf("删除 %s 失败，已回滚：%w", change.Path, err)
				}
			}
			continue
		}
		if err := ensureParentDirectories(root, target, &createdDirs); err != nil {
			if rollbackErr := rollback(); rollbackErr != nil {
				return fmt.Errorf("创建目标目录失败：%v；%v", err, rollbackErr)
			}
			return fmt.Errorf("创建目标目录失败，已回滚：%w", err)
		}
		if err := revalidateTransactionTarget(root, change.Path, target, exists); err != nil {
			if rollbackErr := rollback(); rollbackErr != nil { return fmt.Errorf("写入前路径复核失败：%v；%v", err, rollbackErr) }
			return fmt.Errorf("写入前路径复核失败，已回滚：%w", err)
		}
		if err := replaceFromFile(change.Staged, target); err != nil {
			if rollbackErr := rollback(); rollbackErr != nil {
				return fmt.Errorf("写入 %s 失败：%v；%v", change.Path, err, rollbackErr)
			}
			return fmt.Errorf("写入 %s 失败，已回滚：%w", change.Path, err)
		}
	}
	if err := appendTransactionJournal(work, Applied{Done: true}); err != nil {
		if rollbackErr := rollback(); rollbackErr != nil {
			return fmt.Errorf("提交事务标记失败：%v；%v", err, rollbackErr)
		}
		return fmt.Errorf("提交事务标记失败，已回滚：%w", err)
	}
	return nil
}

func securePathAllowMetadata(root, rel string) (string, bool, error) {
	if rel == "VERSION" || rel == stateName || rel == stateSignatureName {
		target := filepath.Join(root, filepath.FromSlash(rel))
		parentInfo, parentErr := os.Lstat(filepath.Dir(target))
		if parentErr == nil && (parentInfo.Mode()&os.ModeSymlink != 0 || !parentInfo.IsDir()) {
			return "", false, fmt.Errorf("元数据父目录不安全：%s", filepath.Dir(target))
		}
		if parentErr != nil && !os.IsNotExist(parentErr) {
			return "", false, parentErr
		}
		if info, err := os.Lstat(target); err == nil {
			if info.Mode()&os.ModeSymlink != 0 || info.IsDir() {
				return "", false, fmt.Errorf("元数据目标不是普通文件：%s", rel)
			}
			return target, true, nil
		} else if !os.IsNotExist(err) {
			return "", false, err
		}
		return target, false, nil
	}
	return securePath(root, rel)
}

func ensureParentDirectories(root, target string, created *[]string) error {
	root, err := filepath.Abs(root)
	if err != nil {
		return err
	}
	parent := filepath.Dir(target)
	relative, err := filepath.Rel(root, parent)
	if err != nil {
		return err
	}
	if relative == "." {
		return nil
	}
	current := root
	for _, part := range strings.Split(relative, string(os.PathSeparator)) {
		current = filepath.Join(current, part)
		info, statErr := os.Lstat(current)
		if statErr == nil {
			if info.Mode()&os.ModeSymlink != 0 || !info.IsDir() {
				return fmt.Errorf("目标父目录不安全：%s", current)
			}
			continue
		}
		if !os.IsNotExist(statErr) {
			return statErr
		}
		if err := os.Mkdir(current, 0755); err != nil {
			return err
		}
		*created = append(*created, current)
	}
	return nil
}

func replaceFromFile(source, destination string) error {
	parentInfo, err := os.Lstat(filepath.Dir(destination))
	if err != nil { return fmt.Errorf("替换目标父目录不可用：%w", err) }
	if parentInfo.Mode()&os.ModeSymlink != 0 || !parentInfo.IsDir() {
		return fmt.Errorf("替换目标父目录不安全：%s", filepath.Dir(destination))
	}
	if targetInfo, err := os.Lstat(destination); err == nil {
		if targetInfo.Mode()&os.ModeSymlink != 0 || !targetInfo.Mode().IsRegular() {
			return fmt.Errorf("替换目标不是普通文件：%s", destination)
		}
	} else if !os.IsNotExist(err) {
		return err
	}
	sourceInfo, err := os.Lstat(source)
	if err != nil {
		return err
	}
	if !sourceInfo.Mode().IsRegular() {
		return fmt.Errorf("替换源不是普通文件：%s", source)
	}
	input, err := os.Open(source)
	if err != nil {
		return err
	}
	defer input.Close()
	openedInfo, err := input.Stat()
	if err != nil { return err }
	if !os.SameFile(sourceInfo, openedInfo) { return errors.New("替换源在打开期间发生变化") }

	tempFile, err := os.CreateTemp(filepath.Dir(destination), ".dw-new-*")
	if err != nil {
		return err
	}
	temp := tempFile.Name()
	defer os.Remove(temp)

	if _, err := io.Copy(tempFile, input); err != nil {
		_ = tempFile.Close()
		return err
	}
	if err := tempFile.Sync(); err != nil {
		_ = tempFile.Close()
		return err
	}
	if err := tempFile.Chmod(sourceInfo.Mode().Perm()); err != nil {
		_ = tempFile.Close()
		return err
	}
	if err := tempFile.Close(); err != nil {
		return err
	}
	if err := replaceExistingFile(temp, destination); err != nil {
		return err
	}
	return nil
}

func copyFileLimited(source, destination string, mode os.FileMode, maxBytes int64) error {
	if maxBytes <= 0 { return errors.New("文件复制大小限制无效") }
	return copyFileAtomic(source, destination, mode, maxBytes)
}

func copyFile(source, destination string, mode os.FileMode) error {
	return copyFileAtomic(source, destination, mode, 0)
}

func copyFileAtomic(source, destination string, mode os.FileMode, maxBytes int64) error {
	sourceInfo, err := os.Lstat(source)
	if err != nil { return err }
	if sourceInfo.Mode()&os.ModeSymlink != 0 || !sourceInfo.Mode().IsRegular() {
		return fmt.Errorf("复制源不是普通文件：%s", source)
	}
	input, err := os.Open(source)
	if err != nil { return err }
	defer input.Close()
	openedInfo, err := input.Stat()
	if err != nil { return err }
	if !os.SameFile(sourceInfo, openedInfo) { return errors.New("复制源在打开期间发生变化") }

	if err := os.MkdirAll(filepath.Dir(destination), 0755); err != nil { return err }
	parentInfo, err := os.Lstat(filepath.Dir(destination))
	if err != nil || parentInfo.Mode()&os.ModeSymlink != 0 || !parentInfo.IsDir() {
		return fmt.Errorf("复制目标父目录不安全：%s", filepath.Dir(destination))
	}
	temp, err := os.CreateTemp(filepath.Dir(destination), ".dw-copy-*")
	if err != nil { return err }
	tempPath := temp.Name()
	defer os.Remove(tempPath)
	var written int64
	if maxBytes > 0 {
		written, err = io.Copy(temp, io.LimitReader(input, maxBytes+1))
	} else {
		written, err = io.Copy(temp, input)
	}
	if err != nil { _ = temp.Close(); return err }
	if maxBytes > 0 && written > maxBytes { _ = temp.Close(); return fmt.Errorf("文件超过大小限制：%s", humanSize(written)) }
	if err := temp.Sync(); err != nil { _ = temp.Close(); return err }
	if err := temp.Chmod(mode); err != nil { _ = temp.Close(); return err }
	if err := temp.Close(); err != nil { return err }
	return replaceExistingFile(tempPath, destination)
}
