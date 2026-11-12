package main

import (
	"archive/zip"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/url"
	"os"
	"strings"
)

func requiresLauncherDownload(entry Entry) bool {
	sources := entry.Sources
	if len(sources) == 0 {
		sources = []string{entry.Source}
	}
	for _, source := range sources {
		parsed, err := url.Parse(source)
		if err != nil {
			continue
		}
		host := strings.ToLower(parsed.Hostname())
		if host == "curseforge.com" || strings.HasSuffix(host, ".curseforge.com") ||
			host == "forgecdn.net" || strings.HasSuffix(host, ".forgecdn.net") {
			return true
		}
	}
	return false
}

func createLauncherDeltaMRPack(archive *Archive, entries []Entry, destination string) error {
	if archive == nil {
		return errors.New("生成启动器差量包时缺少完整 MRPack")
	}
	indexFile := archive.entries["modrinth.index.json"]
	if indexFile == nil || indexFile.UncompressedSize64 > maxManifestBytes {
		return errors.New("完整 MRPack 缺少有效的 modrinth.index.json")
	}
	reader, err := indexFile.Open()
	if err != nil {
		return err
	}
	data, readErr := io.ReadAll(io.LimitReader(reader, maxManifestBytes+1))
	closeErr := reader.Close()
	if readErr != nil {
		return readErr
	}
	if closeErr != nil {
		return closeErr
	}
	if int64(len(data)) > maxManifestBytes {
		return errors.New("MRPack 索引超过大小限制")
	}

	var index modrinthIndex
	if err := json.Unmarshal(data, &index); err != nil {
		return fmt.Errorf("解析 MRPack 索引失败：%w", err)
	}
	if index.FormatVersion != 1 || index.Game != "minecraft" || index.Name == "" || index.VersionID == "" || index.Dependencies["minecraft"] == "" {
		return errors.New("MRPack 缺少标准 Minecraft 依赖信息，无法生成可导入的差量包")
	}

	wanted := make(map[string]bool, len(entries))
	for _, entry := range entries {
		wanted[strings.ToLower(entry.Path)] = true
	}
	filtered := make([]modrinthIndexFile, 0, len(entries))
	for _, item := range index.Files {
		key := strings.ToLower(item.Path)
		if wanted[key] {
			filtered = append(filtered, item)
			delete(wanted, key)
		}
	}
	if len(wanted) != 0 || len(filtered) != len(entries) {
		return errors.New("启动器差量包文件清单与完整 MRPack 索引不一致")
	}
	if len(filtered) == 0 {
		return errors.New("没有需要交给启动器下载的受限来源文件")
	}
	index.Files = filtered
	outputIndex, err := json.MarshalIndent(index, "", "  ")
	if err != nil {
		return fmt.Errorf("序列化启动器差量 MRPack 索引失败：%w", err)
	}

	output, err := os.Create(destination)
	if err != nil {
		return err
	}
	writer := zip.NewWriter(output)
	entry, err := writer.Create("modrinth.index.json")
	if err == nil {
		_, err = entry.Write(outputIndex)
	}
	closeZipErr := writer.Close()
	syncErr := output.Sync()
	closeFileErr := output.Close()
	if err != nil {
		_ = os.Remove(destination)
		return err
	}
	if closeZipErr != nil {
		_ = os.Remove(destination)
		return closeZipErr
	}
	if syncErr != nil {
		_ = os.Remove(destination)
		return syncErr
	}
	if closeFileErr != nil {
		_ = os.Remove(destination)
		return closeFileErr
	}
	return nil
}
