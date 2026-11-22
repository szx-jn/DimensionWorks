package main

import (
	"archive/zip"
	"crypto/sha512"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"strings"
	"sync"
	"time"
)

const bbsmcVersionsURL = "https://api.bbsmc.net/v2/project/doVGSZq0/version"

type bbsmcVersion struct {
	Version string `json:"version_number"`
	Files   []struct {
		Name   string            `json:"filename"`
		URL    string            `json:"url"`
		Size   int64             `json:"size"`
		Hashes map[string]string `json:"hashes"`
	} `json:"files"`
}

var (
	bbsmcVersionsOnce sync.Once
	bbsmcVersions     []bbsmcVersion
	bbsmcVersionsErr  error
)

func loadBBSMCVersions() ([]bbsmcVersion, error) {
	bbsmcVersionsOnce.Do(func() {
		client := newHTTPClient(30*time.Second, 10*time.Second)
		request, err := http.NewRequest(http.MethodGet, bbsmcVersionsURL, nil)
		if err != nil {
			bbsmcVersionsErr = err
			return
		}
		request.Header.Set("Accept", "application/json")
		response, err := client.Do(request)
		if err != nil {
			bbsmcVersionsErr = err
			return
		}
		defer response.Body.Close()
		if response.StatusCode != http.StatusOK {
			bbsmcVersionsErr = fmt.Errorf("BBSMC 版本接口返回 HTTP %d", response.StatusCode)
			return
		}
		data, err := io.ReadAll(io.LimitReader(response.Body, 4<<20+1))
		if err != nil {
			bbsmcVersionsErr = err
			return
		}
		if len(data) > 4<<20 {
			bbsmcVersionsErr = fmt.Errorf("BBSMC 版本清单超过 4 MiB")
			return
		}
		bbsmcVersionsErr = json.Unmarshal(data, &bbsmcVersions)
	})
	return bbsmcVersions, bbsmcVersionsErr
}

func bbsmcAsset(version, name string) (string, int64, string, bool) {
	versions, err := loadBBSMCVersions()
	if err != nil {
		return "", 0, "", false
	}
	for _, candidate := range versions {
		if candidate.Version != version {
			continue
		}
		for _, file := range candidate.Files {
			if file.Name != name {
				continue
			}
			if !strings.HasPrefix(file.URL, "https://cdn.bbsmc.net/") || file.Size <= 0 {
				return "", 0, "", false
			}
			digest := file.Hashes["sha512"]
			if len(digest) != 128 {
				return "", 0, "", false
			}
			if _, err := hex.DecodeString(digest); err != nil {
				return "", 0, "", false
			}
			return file.URL, file.Size, strings.ToLower(digest), true
		}
	}
	return "", 0, "", false
}

func downloadBBSMCAsset(version, name, destination string, maxBytes int64) error {
	source, size, expectedDigest, ok := bbsmcAsset(version, name)
	if !ok {
		return fmt.Errorf("BBSMC 未提供 %s %s", version, name)
	}
	if size > maxBytes {
		return fmt.Errorf("BBSMC 文件大小超过限制：%s", name)
	}
	if err := downloadFileWithLimit(source, destination, maxBytes); err != nil {
		return err
	}
	info, err := os.Stat(destination)
	if err != nil {
		return err
	}
	if info.Size() != size {
		_ = os.Remove(destination)
		return fmt.Errorf("BBSMC 文件大小校验失败：%s", name)
	}
	file, err := os.Open(destination)
	if err != nil {
		return err
	}
	hash := sha512.New()
	_, copyErr := io.Copy(hash, file)
	closeErr := file.Close()
	if copyErr != nil {
		return copyErr
	}
	if closeErr != nil {
		return closeErr
	}
	if hex.EncodeToString(hash.Sum(nil)) != expectedDigest {
		_ = os.Remove(destination)
		return fmt.Errorf("BBSMC SHA-512 校验失败：%s", name)
	}
	return nil
}

func downloadReleaseAssetPreferred(version, name, githubURL, destination string, maxBytes int64) error {
	if err := downloadBBSMCAsset(version, name, destination, maxBytes); err == nil {
		return nil
	} else {
		fmt.Printf("[回退] BBSMC 资产不可用，改用 GitHub 与镜像站：%s\n", name)
		_ = os.Remove(destination)
	}
	return downloadFileWithLimit(githubURL, destination, maxBytes)
}

func releaseManifestBytes(kind, tag, version, destination string) ([]byte, error) {
	name := manifestAssetName(kind, version)
	wrappedPath := destination + ".bbsmc.zip"
	if err := downloadBBSMCAsset(version, name+".zip", wrappedPath, 16<<20); err == nil {
		archive, openErr := zip.OpenReader(wrappedPath)
		if openErr == nil {
			data, readErr := readManifestFromZip(archive, name)
			closeErr := archive.Close()
			_ = os.Remove(wrappedPath)
			if readErr == nil && closeErr == nil {
				manifest, parseErr := parseManifest(data, kind)
				if parseErr == nil && manifest.Release == tag && manifest.Version == version {
					return data, nil
				}
			}
		}
		_ = os.Remove(wrappedPath)
		fmt.Printf("[回退] BBSMC 封装清单无效，改用 GitHub 与镜像站：%s\n", name)
	} else {
		fmt.Printf("[回退] BBSMC 清单不可用，改用 GitHub 与镜像站：%s\n", name)
	}
	if err := downloadFileWithLimit(assetURL(tag, name), destination, maxManifestBytes); err != nil {
		return nil, err
	}
	return os.ReadFile(destination)
}

func readManifestFromZip(archive *zip.ReadCloser, name string) ([]byte, error) {
	if len(archive.File) != 1 || archive.File[0].Name != name ||
		archive.File[0].Mode()&os.ModeSymlink != 0 ||
		archive.File[0].UncompressedSize64 > maxManifestBytes {
		return nil, fmt.Errorf("BBSMC 清单 ZIP 条目不符合预期")
	}
	file, err := archive.File[0].Open()
	if err != nil {
		return nil, err
	}
	defer file.Close()
	data, err := io.ReadAll(io.LimitReader(file, maxManifestBytes+1))
	if err != nil {
		return nil, err
	}
	if len(data) > maxManifestBytes {
		return nil, fmt.Errorf("清单文件超过大小限制")
	}
	return data, nil
}

func preferredReleaseAssetURL(version, name, fallback string) string {
	if source, _, _, ok := bbsmcAsset(version, name); ok {
		return source
	}
	return fallback
}
