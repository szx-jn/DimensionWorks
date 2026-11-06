package main

import (
	"context"
	"errors"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"testing"
	"time"
)

func requireBundledAria2(t *testing.T) {
	t.Helper()
	if len(bundledAria2) == 0 { t.Skip("requires go test -tags bundled_aria2") }
	if _, err := aria2ExecutablePath(); err != nil { t.Fatal(err) }
}

// Legacy transport helpers are tested directly; production uses only aria2c.
func downloadGoForTest(source, destination string) error {
	err := downloadRanged(context.Background(), source, destination, maxDownloadBytes)
	if err == nil { return nil }
	return downloadSequential(context.Background(), source, destination, maxDownloadBytes)
}

func TestAria2ErrorExplainsCausesAndRecovery(t *testing.T) {
	for _, test := range []struct{ code, detail, want string; local, single bool }{
		{"2", "", "超时", false, false},
		{"6", "", "域名解析", false, false},
		{"7", "", "无法连接", false, false},
		{"9", "", "磁盘空间", true, false},
		{"16", "", "写入权限", true, false},
		{"19", "", "单连接", false, true},
		{"24", "", "认证", false, false},
		{"1", "SSL certificate verify failed", "证书", false, true},
		{"0", "unknown engine failure", "原始信息", false, false},
	} {
		err := newAria2Error("下载任务", test.code, nil, test.detail)
		if !strings.Contains(err.Error(), test.want) || err.localFailure() != test.local || err.retrySingleConnection() != test.single {
			t.Errorf("code %s: %s, local=%v single=%v", test.code, err, err.localFailure(), err.retrySingleConnection())
		}
	}
	cause := &os.PathError{Op: "exec", Path: "aria2c", Err: os.ErrPermission}
	err := newAria2Error("启动引擎", "", cause, "")
	if !errors.Is(err, os.ErrPermission) || !strings.Contains(err.Error(), "noexec") { t.Fatal(err) }
}

func TestAria2ErrorRedactsSecretsAndTerminalControls(t *testing.T) {
	err := newAria2Error("下载任务", "", nil, "\x1b[31merrorCode=7 https://user:password@example.com/file?token=secret token:abcdef --rpc-secret=abcd\r\n")
	message := err.Error()
	for _, forbidden := range []string{"password", "token=secret", "abcdef", "abcd", "\x1b", "\r"} {
		if strings.Contains(message, forbidden) { t.Fatalf("leaked %q: %s", forbidden, message) }
	}
	if err.code != "7" || !strings.Contains(message, "无法连接") { t.Fatal(message) }
}

func shellAria2Fixture(t *testing.T, body string) (string, []byte) {
	t.Helper()
	if runtime.GOOS == "windows" { t.Skip("POSIX process fixture") }
	data := []byte("#!/bin/sh\n" + body)
	path := filepath.Join(t.TempDir(), "aria2c")
	if err := os.WriteFile(path, data, 0700); err != nil { t.Fatal(err) }
	return path, data
}

func TestAria2StartupCapturesOriginalErrorWithoutWaitingForRPC(t *testing.T) {
	engine, _ := shellAria2Fixture(t, "echo 'errorCode=1 missing shared library libssl' >&2\nexit 1\n")
	start := time.Now()
	err := downloadWithAria2(context.Background(), engine, "http://127.0.0.1/file", filepath.Join(t.TempDir(), "file"), 100)
	if err == nil || !strings.Contains(err.Error(), "libssl") || !strings.Contains(err.Error(), "运行依赖") {
		t.Fatalf("lost startup diagnosis: %v", err)
	}
	if time.Since(start) > 2*time.Second { t.Fatalf("waited for dead engine: %v", time.Since(start)) }
}

func TestPrepareAria2TriesNextWritableRoot(t *testing.T) {
	_, data := shellAria2Fixture(t, "echo 'aria2 version 1.36.0'\n")
	badRoot := filepath.Join(t.TempDir(), "regular-file")
	if err := os.WriteFile(badRoot, []byte("occupied"), 0600); err != nil { t.Fatal(err) }
	goodRoot := t.TempDir()
	path, cleanup, err := prepareBundledAria2([]string{badRoot, goodRoot}, "aria2c", sha256Text(string(data)), data)
	if err != nil { t.Fatal(err) }
	defer cleanup()
	if !strings.HasPrefix(path, goodRoot+string(filepath.Separator)) { t.Fatal(path) }
	cleanup()
	if _, err := os.Stat(path); !os.IsNotExist(err) { t.Fatal("engine remains") }
}

func TestPrepareAria2CleansUpFailedExecution(t *testing.T) {
	root := t.TempDir()
	data := []byte("invalid executable")
	_, _, err := prepareBundledAria2([]string{root}, "aria2c", sha256Text(string(data)), data)
	if err == nil || !strings.Contains(err.Error(), "架构") { t.Fatalf("missing format error: %v", err) }
	entries, err := os.ReadDir(root)
	if err != nil || len(entries) != 0 { t.Fatalf("failed engine remains: %v %v", entries, err) }
}
