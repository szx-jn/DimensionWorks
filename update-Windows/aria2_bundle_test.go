//go:build bundled_aria2

package main

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"sync/atomic"
	"testing"
	"time"
)

func TestBundledAria2DownloadsWithoutSidecar(t *testing.T) {
	if !hasAria2BuildForRuntime() {
		t.Skip("no bundled engine for this architecture")
	}
	root := filepath.Join(t.TempDir(), "server with spaces")
	if err := os.Mkdir(root, 0700); err != nil {
		t.Fatal(err)
	}
	name := "aria2c"
	if runtime.GOOS == "windows" {
		name += ".exe"
	}
	digest := sha256.Sum256(bundledAria2)
	engine, cleanup, err := unpackAria2(root, name, hex.EncodeToString(digest[:]), bundledAria2)
	if err != nil {
		t.Fatal(err)
	}
	defer cleanup()
	for _, size := range []int{4096, 8 << 20} {
		var heads atomic.Int64
		data := bytes.Repeat([]byte("download-probe-"), size/len("download-probe-")+1)[:size]
		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if r.Method == http.MethodHead {
				heads.Add(1)
				http.Error(w, "HEAD is not supported", http.StatusMethodNotAllowed)
				return
			}
			http.ServeContent(w, r, "probe.bin", time.Time{}, bytes.NewReader(data))
		}))
		destination := filepath.Join(t.TempDir(), "download with spaces.bin")
		ctx, cancel := context.WithTimeout(context.Background(), 20*time.Second)
		err := downloadWithAria2(ctx, engine, server.URL+"/probe.bin", destination, int64(size+1))
		cancel()
		server.Close()
		if err != nil {
			t.Fatalf("real bundled engine, size %d: %v", size, err)
		}
		if heads.Load() != 0 {
			t.Fatal("download depends on a HEAD request")
		}
		got, err := os.ReadFile(destination)
		if err != nil || !bytes.Equal(got, data) {
			t.Fatalf("download differs: %v", err)
		}
	}
}

func TestBundledAria2ReportsHTTPAndCertificateFailures(t *testing.T) {
	requireBundledAria2(t)
	engine, err := aria2ExecutablePath()
	if err != nil {
		t.Fatal(err)
	}
	missing := httptest.NewServer(http.NotFoundHandler())
	defer missing.Close()
	untrusted := httptest.NewTLSServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { _, _ = w.Write([]byte("untrusted")) }))
	defer untrusted.Close()
	for _, test := range []struct{ name, source, want string }{
		{"missing", missing.URL, "文件"},
		{"untrusted certificate", untrusted.URL, "证书"},
	} {
		t.Run(test.name, func(t *testing.T) {
			ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
			defer cancel()
			destination := filepath.Join(t.TempDir(), "download")
			err := downloadWithAria2(ctx, engine, test.source, destination, 100)
			var engineErr *aria2Error
			if !errors.As(err, &engineErr) || !strings.Contains(err.Error(), test.want) || ctx.Err() != nil {
				t.Fatalf("unexpected diagnosis: %v", err)
			}
			if _, err := os.Stat(destination); !os.IsNotExist(err) {
				t.Fatalf("failed download remains: %v", err)
			}
		})
	}
}

func TestBundledAria2SwitchesSourceWhenETAExceedsOneHour(t *testing.T) {
	requireBundledAria2(t)
	var cancelled, fallbackCalled atomic.Bool
	slow := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Length", "1073741824")
		_, _ = w.Write(bytes.Repeat([]byte("a"), 64<<10))
		w.(http.Flusher).Flush()
		select {
		case <-r.Context().Done():
			cancelled.Store(true)
			return
		case <-time.After(2 * time.Second):
		}
		_, _ = w.Write(bytes.Repeat([]byte("b"), 64<<10))
		w.(http.Flusher).Flush()
		select {
		case <-r.Context().Done():
			cancelled.Store(true)
		case <-time.After(10 * time.Second):
		}
	}))
	defer slow.Close()
	data := []byte("fast source data")
	fast := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		fallbackCalled.Store(true)
		_, _ = w.Write(data)
	}))
	defer fast.Close()
	destination := filepath.Join(t.TempDir(), "file")
	start := time.Now()
	if err := downloadFromCandidates([]string{slow.URL, fast.URL}, destination, 2<<30); err != nil {
		t.Fatal(err)
	}
	if time.Since(start) > 10*time.Second || !fallbackCalled.Load() {
		t.Fatal("did not promptly switch source")
	}
	deadline := time.Now().Add(time.Second)
	for !cancelled.Load() && time.Now().Before(deadline) {
		time.Sleep(10 * time.Millisecond)
	}
	if !cancelled.Load() {
		t.Fatal("old source request remains active")
	}
	got, err := os.ReadFile(destination)
	if err != nil || !bytes.Equal(got, data) {
		t.Fatalf("fallback bytes: %q %v", got, err)
	}
}
