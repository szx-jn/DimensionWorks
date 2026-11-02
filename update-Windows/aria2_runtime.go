package main

import (
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"os"
	"path/filepath"
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
