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
	"net/http"
	"net/url"
	"os"
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
var consoleReader = bufio.NewReader(os.Stdin)
var progressMu sync.Mutex
var downloadProgressUI = newProgressTracker()
var downloadCacheRoot string
var parallelDownloadLimit atomic.Int64

const (
	repository          = "szx-jn/DimensionWorks"
	manifestName        = "dimensionworks-update-manifest.tsv"
	stateName           = ".dimensionworks-update/state.tsv"
	stateSignatureName  = ".dimensionworks-update/state.tsv.hmac"
	stateKeyName        = ".dimensionworks-update/state.key"
	maxManifestBytes    = 16 << 20
	maxManifestEntries  = 100000
	maxArchiveEntries   = 100000
	maxArchiveFileBytes = uint64(4 << 30)
	maxArchiveTotalBytes = uint64(16 << 30)
	maxDownloadBytes    = int64(8 << 30)
)

var versionRE = regexp.MustCompile("^[0-9]+\\.[0-9]+\\.[0-9]+$")
var digestRE = regexp.MustCompile("^[0-9a-f]+$")

type Entry struct {
	Action    string
	Path      string
	Algorithm string
	Value     string
	Source    string
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
	Assets  []struct {
		Name               string `json:"name"`
		BrowserDownloadURL string `json:"browser_download_url"`
	} `json:"assets"`
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
	_, _ = consoleReader.ReadString('\n')
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
		if err := os.Mkdir(logDir, 0755); err != nil {
			return ""
		}
	} else {
		return ""
	}
	logPath := filepath.Join(logDir, "failed-"+time.Now().Format("20060102-150405")+"-"+fmt.Sprint(os.Getpid())+".log")
	contents := fmt.Sprintf("time=%s\nerror=%s\n", time.Now().Format(time.RFC3339), cause)
	if err := os.WriteFile(logPath, []byte(contents), 0644); err != nil {
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
	fmt.Fprint(output, "请确认上一个更新进程是否仍在运行。输入 Y 清理锁并继续更新，输入 N 取消更新：")
	answer, readErr := input.ReadString('\n')
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
	fmt.Println("DimensionWorks", kind, "更新器")
	fmt.Println("实例目录：", root)
	fmt.Print("请确认该目录是正确的整合包根目录，且 Minecraft/服务端已退出。输入 Y 继续，其他输入取消：")
	answer, readErr := consoleReader.ReadString('\n')
	if readErr != nil && len(answer) == 0 {
		return fmt.Errorf("无法读取确认输入：%w", readErr)
	}
	if !strings.EqualFold(strings.TrimSpace(answer), "y") {
		return errors.New("用户取消了更新")
	}

	updateRoot := filepath.Join(root, ".dimensionworks-update")
	if err := os.MkdirAll(updateRoot, 0755); err != nil {
		return fmt.Errorf("无法创建更新目录：%w", err)
	}
	if info, err := os.Lstat(updateRoot); err != nil || info.Mode()&os.ModeSymlink != 0 || !info.IsDir() {
		return errors.New(".dimensionworks-update 必须是普通目录，不能是符号链接")
	}
	downloadCacheRoot = filepath.Join(updateRoot, "downloads")
	if err := os.MkdirAll(downloadCacheRoot, 0700); err != nil {
		return fmt.Errorf("无法创建断点续传目录：%w", err)
	}
	if info, err := os.Lstat(downloadCacheRoot); err != nil || info.Mode()&os.ModeSymlink != 0 || !info.IsDir() {
		return errors.New("下载缓存目录不安全")
	}
	lockPath := filepath.Join(updateRoot, "lock")
	releaseLock, err := acquireUpdateLock(lockPath, consoleReader, os.Stdout)
	if err != nil {
		return err
	}
	defer releaseLock()

	if err := recoverInterruptedTransactions(root, updateRoot); err != nil {
		return fmt.Errorf("发现未完成的更新事务，自动恢复失败：%w。请勿删除 .dimensionworks-update 下的恢复数据", err)
	}

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
		manifestBytes, err := downloadBytes(manifestURL, filepath.Join(work, "manifest.tsv"), true)
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
		manifestBytes, err := downloadBytes(manifestURL, filepath.Join(work, "manifest.tsv"), true)
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
		manifestBytes, err := downloadBytes(manifestURL, filepath.Join(work, "manifest.tsv"), false)
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
	}

	statePath := filepath.Join(root, filepath.FromSlash(stateName))
	current, err := readState(root, statePath, kind)
	if err != nil {
		return fmt.Errorf("现有更新状态无效：%w。请备份并检查 %s", err, statePath)
	}
	if current == nil {
		current, err = bootstrapState(root, kind)
		if err != nil {
			return fmt.Errorf("无法初始化现有文件状态：%w", err)
		}
	}
	fmt.Printf("当前版本：%s；目标版本：%s（%s）\n", current.Version, target.Version, target.Release)
	if comparison, compareErr := compareVersions(target.Version, current.Version); compareErr != nil {
		return fmt.Errorf("无法比较版本号：%w", compareErr)
	} else if comparison < 0 && !opts.AllowDowngrade {
		return fmt.Errorf("拒绝将版本从 %s 降级到 %s；如确认需要降级，请显式添加 --allow-downgrade", current.Version, target.Version)
	}

	selected, deletions, err := selectChanges(root, kind, current, target)
	if err != nil {
		return err
	}
	backupBytes, err := estimateExistingBytes(root, selected)
	if err != nil { return fmt.Errorf("估算本地备份空间失败：%w", err) }
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
	if fullPackage == nil && hasArchiveChanges {
		if fullPackageURL == "" { fullPackageURL = assetURL(target.Release, fullPackageName(kind, target.Version)) }
		if size, sizeErr := remoteContentLength(fullPackageURL); sizeErr == nil && size > 0 {
			remoteBytes = saturatingAdd(remoteBytes, uint64(size))
		}
	}
	if err := ensureDiskSpace(root, saturatingAdd(saturatingAdd(saturatingAdd(backupBytes, backupBytes), remoteBytes), 256<<20)); err != nil {
		return fmt.Errorf("磁盘空间预检失败：%w", err)
	}
	userBackup, err := backupModifiedFiles(root, updateRoot, current, selected)
	if err != nil {
		return fmt.Errorf("无法备份用户修改文件，已中止更新：%w", err)
	}
	if userBackup != "" {
		fmt.Println("[保护] 已备份被更新覆盖的本地修改：", userBackup)
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
					if downloadErr := downloadVerified(entry.Source, stage, entry.Algorithm, entry.Value); downloadErr != nil {
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
	if len(archiveEntries) > 0 && fullPackage == nil && releaseTag != "" &&
		validReleaseTag(target.Base) && current.Release == target.Base {
		deltaName := deltaAssetName(kind, target.Version)
		deltaPath := filepath.Join(work, "delta.zip")
		fmt.Println("[下载] 尝试差量更新包：", deltaName)
		if err := downloadFile(assetURL(releaseTag, deltaName), deltaPath, true); err == nil {
			delta, openErr := openArchive(deltaPath)
			if openErr == nil {
				unpackedBytes, sizeErr := estimateArchiveBytes(delta, archiveEntries)
				if sizeErr != nil { _ = delta.Close(); return sizeErr }
				if spaceErr := ensureDiskSpace(root, saturatingAdd(saturatingAdd(saturatingAdd(backupBytes, backupBytes), unpackedBytes), 256<<20)); spaceErr != nil {
					_ = delta.Close()
					return fmt.Errorf("差量包展开前磁盘空间不足：%w", spaceErr)
				}
				stageErr := stageArchiveEntries(delta, archiveEntries, stageRoot)
				_ = delta.Close()
				if stageErr == nil {
					deltaUsed = true
				} else {
					clearStaged(stageRoot, archiveEntries)
					fmt.Println("[回退] 差量包不完整或校验失败，将改用完整包：", stageErr)
				}
			} else {
				fmt.Println("[回退] 差量包不可用：", openErr)
			}
		} else {
			fmt.Println("[回退] 差量包下载失败，将尝试完整包：", err)
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
	changes = append(changes, Change{Path: "VERSION", Staged: versionPath})
	changes = append(changes, Change{Path: stateName, Staged: stateStage})
	changes = append(changes, Change{Path: stateSignatureName, Staged: signatureStage})

	if err := applyTransaction(root, work, changes); err != nil {
		keepWork = true
		logPath := filepath.Join(root, ".dimensionworks-update", "failed-"+time.Now().Format("20060102-150405")+".log")
		_ = os.WriteFile(logPath, []byte(err.Error()+"\nRecovery data: "+work+"\n"), 0644)
		return fmt.Errorf("%w；恢复数据保留在 %s", err, work)
	}
	fmt.Printf("更新成功：%s -> %s\n", current.Version, target.Version)
	if deltaUsed {
		fmt.Println("更新来源：已校验的差量包")
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

func remoteContentLength(source string) (int64, error) {
	client := newHTTPClient(10*time.Second, 5*time.Second)
	req, err := http.NewRequest(http.MethodHead, source, nil)
	if err == nil {
		req.Header.Set("User-Agent", "DimensionWorks-Go-Updater/1.1")
		if resp, headErr := client.Do(req); headErr == nil {
			resp.Body.Close()
			if resp.StatusCode >= 200 && resp.StatusCode < 300 && resp.ContentLength >= 0 {
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
		if err != nil || (parsed.Scheme != "http" && parsed.Scheme != "https") || parsed.Host == "" {
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
}

func compareVersions(left, right string) (int, error) {
	parse := func(value string) ([3]int, error) {
		var result [3]int
		parts := strings.Split(value, ".")
		if len(parts) != 3 { return result, fmt.Errorf("无效版本号：%s", value) }
		for i, part := range parts {
			number, err := strconv.Atoi(part)
			if err != nil || number < 0 { return result, fmt.Errorf("无效版本号：%s", value) }
			result[i] = number
		}
		return result, nil
	}
	a, err := parse(left)
	if err != nil { return 0, err }
	b, err := parse(right)
	if err != nil { return 0, err }
	for i := range a {
		if a[i] < b[i] { return -1, nil }
		if a[i] > b[i] { return 1, nil }
	}
	return 0, nil
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
				return errors.New("拒绝 HTTPS 下载重定向到不安全的 HTTP")
			}
			return nil
		},
	}
}

func latestRelease() (ReleaseInfo, error) {
	var result ReleaseInfo
	req, err := http.NewRequest(http.MethodGet, "https://api.github.com/repos/"+repository+"/releases/latest", nil)
	if err != nil {
		return result, err
	}
	req.Header.Set("User-Agent", "DimensionWorks-Go-Updater/1.1")
	req.Header.Set("Accept", "application/vnd.github+json")
	client := newHTTPClient(30*time.Second, 10*time.Second)
	resp, err := client.Do(req)
	if err != nil {
		return result, fmt.Errorf("无法查询 GitHub 最新发行版：%w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return result, fmt.Errorf("GitHub API 返回 HTTP %d", resp.StatusCode)
	}
	data, err := io.ReadAll(io.LimitReader(resp.Body, 4<<20+1))
	if err != nil { return result, fmt.Errorf("读取 GitHub 发行信息失败：%w", err) }
	if len(data) > 4<<20 { return result, errors.New("GitHub 发行信息超过 4 MiB 限制") }
	if err := json.Unmarshal(data, &result); err != nil {
		return result, fmt.Errorf("无法解析 GitHub 发行信息：%w", err)
	}
	if !validReleaseTag(result.TagName) {
		return result, fmt.Errorf("GitHub 最新发行标签格式无效：%s", result.TagName)
	}
	return result, nil
}

func downloadBytes(source, destination string, mirrors bool) ([]byte, error) {
	if err := downloadFileWithLimit(source, destination, mirrors, maxManifestBytes); err != nil {
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

func downloadFile(source, destination string, mirrors bool) error {
	return downloadFileWithLimit(source, destination, mirrors, maxDownloadBytes)
}

func downloadFileWithLimit(source, destination string, mirrors bool, maxBytes int64) (resultErr error) {
	if maxBytes <= 0 || maxBytes > maxDownloadBytes {
		return fmt.Errorf("下载大小限制无效：%d", maxBytes)
	}
	progressBegin(destination, filepath.Base(destination))
	defer func() { progressFinish(destination, resultErr == nil) }()
	parsed, err := url.Parse(source)
	if err != nil || parsed.Host == "" || (parsed.Scheme != "http" && parsed.Scheme != "https") {
		return fmt.Errorf("无效下载 URL：%s", source)
	}
	candidates := []string{source}
	if mirrors && strings.HasPrefix(source, "https://github.com/") && strings.Contains(source, "/releases/download/") {
		candidates = append(candidates, "https://ghproxy.net/"+source, "https://gh-proxy.com/"+source)
	}
	var failures []string
	for index, candidate := range candidates {
		progressReset(destination)
		fmt.Printf("[下载] 来源 %d/%d：%s\n", index+1, len(candidates), candidate)
		if err := downloadOnceLimited(candidate, destination, maxBytes); err == nil {
			return nil
		} else {
			failures = append(failures, fmt.Sprintf("%s: %v", candidate, err))
		}
	}
	return errors.New(strings.Join(failures, "\n"))
}

const (
	parallelDownloadThreshold int64 = 8 << 20
	parallelDownloadWorkers = 8
)

const downloadAttemptTimeout = 6 * time.Hour

func downloadOnce(source, destination string) error {
	return downloadOnceLimited(source, destination, maxDownloadBytes)
}

func downloadOnceLimited(source, destination string, maxBytes int64) error {
	if err := os.MkdirAll(filepath.Dir(destination), 0755); err != nil {
		return err
	}

	// Range 与单连接回退使用独立 Context，避免 Range 超时后
	// 把已经过期的 Context 继续传给单连接请求。
	rangeCtx, cancelRange := context.WithTimeout(context.Background(), downloadAttemptTimeout)
	rangeErr := downloadRanged(rangeCtx, source, destination, maxBytes)
	cancelRange()
	if rangeErr == nil {
		return nil
	}
	fmt.Printf("[下载] 分段下载失败，清理分段并使用独立连接重试：%v\n", rangeErr)

	progressReset(destination)
	sequentialCtx, cancelSequential := context.WithTimeout(context.Background(), downloadAttemptTimeout)
	defer cancelSequential()
	return downloadSequential(sequentialCtx, source, destination, maxBytes)
}

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
	data, err := os.ReadFile(path)
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
	req.Header.Set("Range", "bytes=0-0")
	resp, err := newHTTPClient(0, 20*time.Second).Do(req)
	if err != nil { return err }
	probeBody, bodyErr := io.ReadAll(io.LimitReader(activityReader{reader: resp.Body, touch: touchProbe}, 2))
	resp.Body.Close()
	if bodyErr != nil { return fmt.Errorf("读取 Range 探测响应失败：%w", bodyErr) }
	if resp.StatusCode != http.StatusPartialContent { return fmt.Errorf("服务器不支持 HTTP Range（状态码 %d）", resp.StatusCode) }
	matches := regexp.MustCompile("^bytes 0-0/([0-9]+)$").FindStringSubmatch(resp.Header.Get("Content-Range"))
	if len(matches) != 2 || len(probeBody) != 1 || (resp.ContentLength >= 0 && resp.ContentLength != 1) {
		return errors.New("服务器返回无效的 Range 探测响应")
	}
	if encoding := strings.TrimSpace(resp.Header.Get("Content-Encoding")); encoding != "" && !strings.EqualFold(encoding, "identity") {
		return fmt.Errorf("Range 探测响应使用了不支持的内容编码：%s", encoding)
	}
	total, err := strconv.ParseInt(matches[1], 10, 64)
	if err != nil || total <= 0 || total > maxBytes { return errors.New("服务器返回无效或超限的 Content-Range") }
	validator := resp.Header.Get("ETag")
	if validator == "" { validator = resp.Header.Get("Last-Modified") }
	progressSetTotal(destination, total)
	if total < parallelDownloadThreshold { return fmt.Errorf("文件仅 %s，使用单连接下载", humanSize(total)) }

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
				if err := downloadRangeSegment(rangeCtx, source, partPaths[index], destination, begin, end, total, validator); err != nil {
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

func downloadRangeSegment(ctx context.Context, source, destination, progressKey string, begin, end, total int64, validator string) error {
	idleCtx, stopIdle, touch := newIdleTimeoutContext(ctx, downloadIdleTimeout(end-begin+1))
	defer stopIdle()
	req, err := http.NewRequestWithContext(idleCtx, http.MethodGet, source, nil)
	if err != nil { return err }
	req.Header.Set("User-Agent", "DimensionWorks-Go-Updater/1.1")
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

func downloadVerified(source, destination, algorithm, expected string) error {
	if err := downloadFile(source, destination, true); err != nil {
		return err
	}
	if err := verifyDigest(destination, algorithm, expected); err != nil {
		_ = os.Remove(destination)
		return err
	}
	return nil
}

func downloadArchiveWithManualFallback(source, destination string) error {
	if err := downloadFile(source, destination, true); err == nil {
		return nil
	} else {
		fmt.Println()
		fmt.Println("自动下载完整包失败。可以把完整包下载到本地后，输入文件路径继续。")
		fmt.Println("直接按回车将取消本次更新。")
		fmt.Println("下载错误：", err)
		fmt.Print("完整包路径：")
		line, readErr := consoleReader.ReadString('\n')
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
		if err != nil || parsed.Host == "" || (parsed.Scheme != "http" && parsed.Scheme != "https") || strings.ContainsAny(entry.Source, " \t\r\n") {
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

func stageArchiveEntries(archive *Archive, entries []Entry, stageRoot string) error {
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
		if err := verifyDigest(stage, entry.Algorithm, entry.Value); err != nil {
			return fmt.Errorf("归档文件摘要不匹配 %s：%w", entry.Path, err)
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
	key, err := os.ReadFile(keyPath)
	if err != nil { return nil, err }
	signature, err := os.ReadFile(signaturePath)
	if err != nil { return nil, err }
	provided, err := hex.DecodeString(strings.TrimSpace(string(signature)))
	if err != nil || len(provided) != sha256.Size {
		return nil, errors.New("state.tsv.hmac 签名格式无效")
	}
	data, err := os.ReadFile(path)
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
		return os.ReadFile(path)
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

func bootstrapState(root, kind string) (*Manifest, error) {
	roots := []string{"mods", "config", "defaultconfigs", "kubejs", "libraries"}
	rootFiles := []string{"README.md", "start.sh", "start.bat", "run.sh", "run.bat"}
	if kind == "client" {
		roots = []string{"mods", "shaderpacks", "config", "defaultconfigs", "kubejs", "resourcepacks", "local"}
		rootFiles = []string{"modrinth.index.json", "options.txt", "log4j2.xml", "rhino.local.properties", "CustomSkinLoader/CustomSkinLoader.json", "minemenu/menu.json"}
	}
	state := &Manifest{Kind: kind, Version: "0.0.0", Release: "v0.0.0", Base: "none"}
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
	state.Bytes = renderManifest(state)
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

func backupModifiedFiles(root, updateRoot string, current *Manifest, selected []Entry) (string, error) {
	if current == nil || len(selected) == 0 {
		return "", nil
	}
	managed := make(map[string]Entry, len(current.Entries))
	for _, entry := range current.Entries {
		managed[strings.ToLower(entry.Path)] = entry
	}
	backupDir := ""
	for _, entry := range selected {
		previous, ok := managed[strings.ToLower(entry.Path)]
		if !ok {
			continue
		}
		source, exists, err := securePath(root, entry.Path)
		if err != nil {
			return backupDir, err
		}
		if !exists {
			continue
		}
		actual, err := hashFile(source, previous.Algorithm)
		if err != nil {
			return backupDir, fmt.Errorf("检查 %s 失败：%w", entry.Path, err)
		}
		if actual == previous.Value {
			continue
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

func appendTransactionJournal(work string, item Applied) error {
	path := filepath.Join(work, "transaction.jsonl")
	file, err := os.OpenFile(path, os.O_CREATE|os.O_RDWR, 0600)
	if err != nil { return err }
	defer file.Close()
	start, err := file.Seek(0, io.SeekEnd)
	if err != nil { return err }
	payload, err := json.Marshal(item)
	if err != nil { return err }
	payload = append(payload, '\n')
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
		if os.IsNotExist(err) { continue }
		if err != nil { return err }
		if journalInfo.Mode()&os.ModeSymlink != 0 || !journalInfo.Mode().IsRegular() { return fmt.Errorf("事务日志不是普通文件：%s", journal) }
		if journalInfo.Size() > maxManifestBytes {
			return fmt.Errorf("事务日志超过大小限制：%s", journal)
		}
		data, err := os.ReadFile(journal)
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

