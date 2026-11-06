package main

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"time"
)

// Use the updater's executable filesystem, since system temp directories may be
// mounted noexec. Each process owns its private directory and cleans it on exit.
func unpackAria2(root, name, expectedDigest string, data []byte) (string, func(), error) {
	digest := sha256.Sum256(data)
	if hex.EncodeToString(digest[:]) != expectedDigest {
		return "", nil, fmt.Errorf("内置 aria2 SHA-256 校验失败")
	}
	dir, err := os.MkdirTemp(root, ".dimensionworks-aria2-")
	if err != nil {
		return "", nil, err
	}
	cleanup := func() { _ = os.RemoveAll(dir) }
	candidate := filepath.Join(dir, name)
	if err := os.WriteFile(candidate, data, 0600); err != nil {
		cleanup()
		return "", nil, err
	}
	if err := verifyDigest(candidate, "sha256", expectedDigest); err != nil {
		cleanup()
		return "", nil, err
	}
	if err := os.Chmod(candidate, 0700); err != nil {
		cleanup()
		return "", nil, err
	}
	return candidate, cleanup, nil
}

func aria2ExtractionRoots(executableDir string) []string {
	roots := []string{executableDir}
	if cwd, err := os.Getwd(); err == nil {
		roots = append(roots, cwd)
	}
	if cache, err := os.UserCacheDir(); err == nil {
		roots = append(roots, cache)
	}
	return append(roots, os.TempDir())
}

func probeAria2Executable(path string) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	command := exec.CommandContext(ctx, path, "--no-conf=true", "--version")
	var output limitedOutput
	command.Stdout, command.Stderr = &output, &output
	if err := command.Run(); err != nil {
		if ctx.Err() != nil {
			err = ctx.Err()
		}
		return newAria2Error("启动引擎", "", err, output.String())
	}
	if !strings.Contains(output.String(), "aria2 version") {
		return newAria2Error("启动引擎", "", fmt.Errorf("引擎未返回版本信息"), output.String())
	}
	return nil
}

func prepareBundledAria2(roots []string, name, digest string, data []byte) (string, func(), error) {
	actual := sha256.Sum256(data)
	if hex.EncodeToString(actual[:]) != digest {
		return "", nil, newAria2Error("准备引擎", "", fmt.Errorf("内嵌 aria2c SHA-256 校验失败"), "")
	}
	seen := make(map[string]bool)
	var failures []string
	for _, root := range roots {
		root = filepath.Clean(root)
		if seen[root] {
			continue
		}
		seen[root] = true
		path, cleanup, err := unpackAria2(root, name, digest, data)
		if err == nil {
			err = probeAria2Executable(path)
			if err == nil {
				return path, cleanup, nil
			}
			cleanup()
		}
		failures = append(failures, fmt.Sprintf("%s: %v", root, err))
	}
	return "", nil, newAria2Error("准备引擎", "", fmt.Errorf("所有释放位置均失败"), strings.Join(failures, "; "))
}
