package main

import (
	"archive/zip"
	"bytes"
	"context"
	"crypto/sha256"
	"fmt"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"sync/atomic"
	"testing"
)

func TestIncrementalOverlaysVerifyOnlyFinalBytes(t *testing.T) {
	for _, corruptFinal := range []bool{false, true} {
		t.Run(fmt.Sprint(corruptFinal), func(t *testing.T) {
			root := t.TempDir()
			work, stage := filepath.Join(root, "work"), filepath.Join(root, "stage")
			for _, dir := range []string{work, stage} {
				if err := os.Mkdir(dir, 0700); err != nil {
					t.Fatal(err)
				}
			}
			payloads := [][]byte{[]byte("old README"), []byte("latest README")}
			if corruptFinal {
				payloads[1] = []byte("corrupt README")
			}
			var archives [][]byte
			for _, payload := range payloads {
				var buffer bytes.Buffer
				writer := zip.NewWriter(&buffer)
				file, err := writer.Create("README.md")
				if err != nil {
					t.Fatal(err)
				}
				if _, err := file.Write(payload); err != nil {
					t.Fatal(err)
				}
				if err := writer.Close(); err != nil {
					t.Fatal(err)
				}
				archives = append(archives, buffer.Bytes())
			}
			server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				index := 0
				if r.URL.Path == "/second" {
					index = 1
				}
				_, _ = w.Write(archives[index])
			}))
			defer server.Close()
			packages := []IncrementalPackage{
				{Release: "v0.0.2", URL: server.URL + "/first", Size: uint64(len(archives[0]))},
				{Release: "v0.0.3", URL: server.URL + "/second", Size: uint64(len(archives[1]))},
			}
			entries := []Entry{{Action: "archive", Path: "README.md", Source: "README.md", Algorithm: "sha256", Value: fmt.Sprintf("%x", sha256.Sum256([]byte("latest README")))}}
			err := stageIncrementalPackages(root, work, stage, packages, entries, 0)
			if corruptFinal {
				if err == nil {
					t.Fatal("corrupt final overlay accepted")
				}
				return
			}
			if err != nil {
				t.Fatal(err)
			}
			data, err := os.ReadFile(filepath.Join(stage, "README.md"))
			if err != nil || string(data) != "latest README" {
				t.Fatalf("final README: %q, %v", data, err)
			}
		})
	}
}

func TestRangedDownloadReusesRedirectAndHandlesWeakETag(t *testing.T) {
	data := bytes.Repeat([]byte("range-data"), (9<<20)/10+1)[:9<<20]
	const modified = "Sun, 01 Nov 2026 00:00:00 GMT"
	var redirects, transient atomic.Int32
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/release" {
			if redirects.Add(1) > 1 {
				http.Error(w, "redirect rate limit", 429)
				return
			}
			http.Redirect(w, r, "/asset", http.StatusFound)
			return
		}
		if r.Header.Get("Accept-Encoding") != "identity" {
			t.Error("Range must request identity encoding")
		}
		var begin, end int
		if _, err := fmt.Sscanf(r.Header.Get("Range"), "bytes=%d-%d", &begin, &end); err != nil || begin < 0 || end < begin || end >= len(data) {
			http.Error(w, "invalid range", 416)
			return
		}
		if end > 0 {
			if r.Header.Get("If-Range") != modified {
				http.Error(w, "weak ETag is invalid in If-Range", 400)
				return
			}
			if transient.Add(1) == 1 {
				http.Error(w, "temporary failure", 503)
				return
			}
		}
		w.Header().Set("ETag", `W/"mirror-tag"`)
		w.Header().Set("Last-Modified", modified)
		w.Header().Set("Content-Range", fmt.Sprintf("bytes %d-%d/%d", begin, end, len(data)))
		w.Header().Set("Content-Length", fmt.Sprint(end-begin+1))
		w.WriteHeader(http.StatusPartialContent)
		_, _ = w.Write(data[begin : end+1])
	}))
	defer server.Close()
	destination := filepath.Join(t.TempDir(), "download.bin")
	if err := downloadRanged(context.Background(), server.URL+"/release", destination, int64(len(data))); err != nil {
		t.Fatal(err)
	}
	got, err := os.ReadFile(destination)
	if err != nil || !bytes.Equal(got, data) {
		t.Fatalf("assembled bytes differ: %v", err)
	}
	if redirects.Load() != 1 {
		t.Fatalf("resolved release %d times", redirects.Load())
	}
}
