package main

import (
	"crypto/sha256"
	"encoding/hex"
	"os"
	"runtime"
	"testing"
)

func TestUnpackAria2VerifiesBytesPermissionsAndCleansUp(t *testing.T) {
	root := t.TempDir()
	data := []byte("test engine")
	digest := sha256.Sum256(data)
	path, cleanup, err := unpackAria2(root, "aria2c", hex.EncodeToString(digest[:]), data)
	if err != nil {
		t.Fatal(err)
	}
	defer cleanup()
	info, err := os.Stat(path)
	if err != nil {
		t.Fatal(err)
	}
	if runtime.GOOS != "windows" && info.Mode().Perm() != 0700 {
		t.Fatalf("permissions: %v", info.Mode())
	}
	cleanup()
	if _, err := os.Stat(path); !os.IsNotExist(err) {
		t.Fatalf("engine remains after cleanup: %v", err)
	}
	if _, _, err := unpackAria2(root, "aria2c", "invalid", data); err == nil {
		t.Fatal("accepted corrupt engine")
	}
	entries, err := os.ReadDir(root)
	if err != nil || len(entries) != 0 {
		t.Fatalf("unexpected extracted files: %v, %v", entries, err)
	}
}
