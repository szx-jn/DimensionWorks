//go:build bundled_aria2

package main

import (
	"bytes"
	"context"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"sync/atomic"
	"testing"
	"time"
)

func TestBundledAria2DownloadsWithoutSidecar(t *testing.T) {
	root := filepath.Join(t.TempDir(), "server with spaces")
	if err := os.Mkdir(root, 0700); err != nil {
		t.Fatal(err)
	}
	engine, cleanup, err := unpackAria2(root, "aria2c-linux-amd64", "6f50816471fbd5e91c04df6cf6f995c6279d295b70187f313e6d3b04f65769fc", bundledAria2)
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
