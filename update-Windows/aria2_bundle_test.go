//go:build bundled_aria2

package main

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/hex"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"runtime"
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
