package main

import (
	"archive/zip"
	"bytes"
	"crypto/sha256"
	"fmt"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"
)

func TestIncrementalOverlaysVerifyOnlyFinalBytes(t *testing.T) {
	requireBundledAria2(t)
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
