package main

import (
	"errors"
	"fmt"
	"net/url"
	"os"
	"path/filepath"
	"strings"
	"time"
)

const maxIncrementalChainLength = 128

type IncrementalPackage struct {
	Release string
	Version string
	URL     string
	Size    uint64
}

type TransferPlan struct {
	FullPackageURL  string
	FullPackageSize uint64
	Incrementals    []IncrementalPackage
	IncrementalSize uint64
	UseIncrementals bool
	Reason          string
}

type releaseStep struct {
	manifest *Manifest
	info     *ReleaseInfo
}

func largestIncrementalSize(packages []IncrementalPackage) uint64 {
	var largest uint64
	for _, pkg := range packages {
		if pkg.Size > largest {
			largest = pkg.Size
		}
	}
	return largest
}

func humanSizeUint64(size uint64) string {
	if size > uint64(^uint64(0)>>1) {
		return "超出可显示范围"
	}
	return humanSize(int64(size))
}

func printTransferPlan(current, target *Manifest, plan TransferPlan) {
	currentVersion := "未知"
	if current != nil {
		currentVersion = current.Version
	}
	targetVersion := "未知"
	if target != nil {
		targetVersion = target.Version
	}
	fmt.Printf("[计划] 当前版本 %s -> 目标版本 %s\n", currentVersion, targetVersion)
	if len(plan.Incrementals) > 0 {
		for _, pkg := range plan.Incrementals {
			fmt.Printf("[计划] 增量包 %s：%s\n", pkg.Release, humanSizeUint64(pkg.Size))
		}
		fmt.Printf("[计划] 共 %d 个增量包，合计 %s；完整包 %s\n", len(plan.Incrementals), humanSizeUint64(plan.IncrementalSize), formatPlannedSize(plan.FullPackageSize))
	} else if plan.Reason != "" {
		fmt.Printf("[计划] 无可用增量链（%s）；完整包 %s\n", plan.Reason, formatPlannedSize(plan.FullPackageSize))
	} else {
		fmt.Printf("[计划] 无需传输增量包；完整包 %s\n", formatPlannedSize(plan.FullPackageSize))
	}
	if plan.UseIncrementals {
		fmt.Printf("[计划] 选择增量包，预计少传输 %s\n", humanSizeUint64(plan.FullPackageSize-plan.IncrementalSize))
	} else if plan.FullPackageSize > 0 {
		if len(plan.Incrementals) == 0 {
			fmt.Println("[计划] 选择完整包（没有可用的增量链）")
		} else {
			fmt.Printf("[计划] 选择完整包，较增量包少传输 %s\n", humanSizeUint64(plan.IncrementalSize-plan.FullPackageSize))
		}
	} else {
		fmt.Println("[计划] 无法取得完整包大小，保守选择完整包")
	}
}

func formatPlannedSize(size uint64) string {
	if size == 0 {
		return "未知"
	}
	return humanSizeUint64(size)
}

func releaseInfoForTag(tag string) (*ReleaseInfo, error) {
	if !validReleaseTag(tag) {
		return nil, fmt.Errorf("发行标签格式无效：%s", tag)
	}
	apiURL := "https://api.github.com/repos/" + repository + "/releases/tags/" + url.PathEscape(tag)
	client := newHTTPClient(20*time.Second, 8*time.Second)
	info, err := latestReleaseFrom(client, []string{apiURL, "https://gh-proxy.com/" + apiURL})
	if err != nil {
		return nil, err
	}
	if info.TagName != tag {
		return nil, fmt.Errorf("GitHub 返回的发行版 %s 与请求的 %s 不一致", info.TagName, tag)
	}
	return &info, nil
}

func releaseManifestForTag(kind, tag, work string) (*Manifest, error) {
	if !validReleaseTag(tag) {
		return nil, fmt.Errorf("增量链包含无效发行标签：%s", tag)
	}
	version := strings.TrimPrefix(tag, "v")
	path := filepath.Join(work, "manifest-"+version+".tsv")
	data, err := downloadBytes(assetURL(tag, manifestAssetName(kind, version)), path)
	if err != nil {
		return nil, fmt.Errorf("读取增量链清单 %s 失败：%w", tag, err)
	}
	manifest, err := parseManifest(data, kind)
	if err != nil {
		return nil, fmt.Errorf("发行版 %s 的更新清单无效：%w", tag, err)
	}
	if manifest.Release != tag || manifest.Version != version {
		return nil, fmt.Errorf("发行版 %s 的更新清单身份不匹配", tag)
	}
	return manifest, nil
}

func incrementalReleaseChain(kind, currentRelease string, target *Manifest, targetInfo *ReleaseInfo, work string) ([]releaseStep, error) {
	if !validReleaseTag(currentRelease) {
		return nil, errors.New("当前实例没有可信发行标签，无法计算增量链")
	}
	if target == nil || !validReleaseTag(target.Release) {
		return nil, errors.New("目标清单没有有效发行标签")
	}

	reverse := []releaseStep{{manifest: target, info: targetInfo}}
	seen := map[string]struct{}{target.Release: {}}
	cursor := target
	for hops := 0; hops < maxIncrementalChainLength; hops++ {
		if cursor.Base == currentRelease {
			for left, right := 0, len(reverse)-1; left < right; left, right = left+1, right-1 {
				reverse[left], reverse[right] = reverse[right], reverse[left]
			}
			return reverse, nil
		}
		if !validReleaseTag(cursor.Base) {
			return nil, fmt.Errorf("发行版 %s 没有通向当前版本 %s 的增量基线", cursor.Release, currentRelease)
		}
		if _, exists := seen[cursor.Base]; exists {
			return nil, fmt.Errorf("增量链出现循环发行标签：%s", cursor.Base)
		}
		seen[cursor.Base] = struct{}{}

		previous, err := releaseManifestForTag(kind, cursor.Base, work)
		if err != nil {
			return nil, err
		}
		if comparison, compareErr := compareVersions(previous.Version, cursor.Version); compareErr != nil || comparison >= 0 {
			return nil, fmt.Errorf("增量链版本顺序无效：%s -> %s", previous.Version, cursor.Version)
		}
		info, _ := releaseInfoForTag(previous.Release)
		reverse = append(reverse, releaseStep{manifest: previous, info: info})
		cursor = previous
	}
	return nil, fmt.Errorf("增量链超过 %d 个版本，停止自动遍历", maxIncrementalChainLength)
}

func releaseAssetSize(info *ReleaseInfo, name, source string) (uint64, error) {
	if info != nil {
		for _, asset := range info.Assets {
			if asset.Name == name && asset.Size > 0 {
				return uint64(asset.Size), nil
			}
		}
	}
	size, err := remoteContentLength(source)
	if err != nil || size <= 0 {
		if err == nil {
			err = errors.New("服务器未提供有效的文件大小")
		}
		return 0, fmt.Errorf("无法取得 %s 的文件大小：%w", name, err)
	}
	return uint64(size), nil
}

func planTransfer(kind string, current, target *Manifest, targetInfo *ReleaseInfo, fullPackageURL, work string) TransferPlan {
	plan := TransferPlan{FullPackageURL: fullPackageURL}
	if plan.FullPackageURL == "" && target != nil {
		plan.FullPackageURL = assetURL(target.Release, fullPackageName(kind, target.Version))
	}
	if target == nil {
		plan.Reason = "目标更新清单不可用"
		return plan
	}
	if targetInfo == nil {
		targetInfo, _ = releaseInfoForTag(target.Release)
	}
	fullName := fullPackageName(kind, target.Version)
	defaultFullURL := assetURL(target.Release, fullName)
	var fullSize uint64
	var fullSizeErr error
	if plan.FullPackageURL != defaultFullURL {
		if size, err := remoteContentLength(plan.FullPackageURL); err == nil && size > 0 {
			fullSize = uint64(size)
		} else {
			fullSizeErr = err
		}
	} else {
		fullSize, fullSizeErr = releaseAssetSize(targetInfo, fullName, plan.FullPackageURL)
	}
	if fullSizeErr == nil {
		plan.FullPackageSize = fullSize
	}

	if current == nil {
		plan.Reason = "无法读取当前版本状态"
		return plan
	}
	chain, err := incrementalReleaseChain(kind, current.Release, target, targetInfo, work)
	if err != nil {
		plan.Reason = err.Error()
		return plan
	}
	for _, step := range chain {
		name := deltaAssetName(kind, step.manifest.Version)
		url := assetURL(step.manifest.Release, name)
		size, sizeErr := releaseAssetSize(step.info, name, url)
		if sizeErr != nil {
			plan.Incrementals = nil
			plan.IncrementalSize = 0
			plan.Reason = sizeErr.Error()
			return plan
		}
		plan.Incrementals = append(plan.Incrementals, IncrementalPackage{
			Release: step.manifest.Release,
			Version: step.manifest.Version,
			URL:     url,
			Size:    size,
		})
		plan.IncrementalSize = saturatingAdd(plan.IncrementalSize, size)
	}
	if len(plan.Incrementals) > 0 && plan.FullPackageSize > 0 && plan.IncrementalSize < plan.FullPackageSize {
		plan.UseIncrementals = true
	}
	return plan
}

func stageIncrementalPackages(root, work, stageRoot string, packages []IncrementalPackage, entries []Entry, backupBytes uint64) error {
	cacheRoot := filepath.Join(work, "incremental-downloads")
	if err := os.Mkdir(cacheRoot, 0700); err != nil {
		return fmt.Errorf("无法创建增量包临时下载目录：%w", err)
	}
	defer os.RemoveAll(cacheRoot)
	previousCacheRoot := downloadCacheRoot
	downloadCacheRoot = cacheRoot
	defer func() { downloadCacheRoot = previousCacheRoot }()

	staged := make(map[string]bool, len(entries))
	for index, pkg := range packages {
		packagePath := filepath.Join(work, fmt.Sprintf("incremental-%03d.zip", index+1))
		defer os.Remove(packagePath)
		if err := downloadFile(pkg.URL, packagePath); err != nil {
			return fmt.Errorf("下载增量包 %s 失败：%w", pkg.Release, err)
		}
		info, err := os.Lstat(packagePath)
		if err != nil || !info.Mode().IsRegular() || info.Mode()&os.ModeSymlink != 0 {
			return fmt.Errorf("增量包 %s 不是普通文件", pkg.Release)
		}
		if uint64(info.Size()) != pkg.Size {
			return fmt.Errorf("增量包 %s 大小不符：实际 %s，计划 %s", pkg.Release, humanSize(info.Size()), humanSizeUint64(pkg.Size))
		}
		archive, err := openArchive(packagePath)
		if err != nil {
			return fmt.Errorf("增量包 %s 无效：%w", pkg.Release, err)
		}
		present := make([]Entry, 0, len(entries))
		for _, entry := range entries {
			if archive.entries[strings.ToLower(entry.Source)] != nil {
				present = append(present, entry)
			}
		}
		unpackedBytes, sizeErr := estimateArchiveBytes(archive, present)
		if sizeErr != nil {
			_ = archive.Close()
			return fmt.Errorf("无法估算增量包 %s 的展开空间：%w", pkg.Release, sizeErr)
		}
		required := saturatingAdd(saturatingAdd(saturatingAdd(backupBytes, backupBytes), uint64(info.Size())), unpackedBytes)
		required = saturatingAdd(required, 256<<20)
		if spaceErr := ensureDiskSpace(root, required); spaceErr != nil {
			_ = archive.Close()
			return fmt.Errorf("增量包 %s 展开前磁盘空间不足：%w", pkg.Release, spaceErr)
		}
		if err := stageArchiveEntries(archive, present, stageRoot); err != nil {
			_ = archive.Close()
			return fmt.Errorf("暂存增量包 %s 失败：%w", pkg.Release, err)
		}
		if err := archive.Close(); err != nil {
			return fmt.Errorf("关闭增量包 %s 失败：%w", pkg.Release, err)
		}
		for _, entry := range present {
			staged[entry.Path] = true
		}
		if err := os.Remove(packagePath); err != nil {
			return fmt.Errorf("无法清理已使用的增量包 %s：%w", pkg.Release, err)
		}
	}

	for _, entry := range entries {
		if !staged[entry.Path] {
			return fmt.Errorf("增量链缺少目标文件 %s", entry.Path)
		}
		path, err := stagePath(stageRoot, entry.Path)
		if err != nil {
			return err
		}
		if err := verifyDigest(path, entry.Algorithm, entry.Value); err != nil {
			return fmt.Errorf("增量链中的最终文件 %s 校验失败：%w", entry.Path, err)
		}
	}
	if err := os.RemoveAll(cacheRoot); err != nil {
		return fmt.Errorf("无法清理增量包分段缓存：%w", err)
	}
	fmt.Printf("[清理] 已删除 %d 个使用过的增量包及其下载分段缓存\n", len(packages))
	return nil
}
