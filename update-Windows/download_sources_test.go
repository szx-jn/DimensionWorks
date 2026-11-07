package main

import (
	"bytes"
	"fmt"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"reflect"
	"sync/atomic"
	"testing"
	"time"
)

func TestSourcePreferenceReusesWorkingMirrorAcrossReleaseAssets(t *testing.T) {
	var preference sourcePreference
	first := downloadCandidates(assetURL("v0.3.20", "manifest.tsv"))
	if got := preference.order(first); !reflect.DeepEqual(got, first) {
		t.Fatalf("first download should use default order: %v", got)
	}
	preference.remember(first[1])
	// A different repository and asset should reuse the working mirror.
	next := downloadCandidates("https://github.com/szx-jn/TrueUUID-DimensionWorks/releases/download/v1.0.9/mod.jar")
	want := []string{next[1], next[0], next[2]}
	if got := preference.order(next); !reflect.DeepEqual(got, want) {
		t.Fatalf("working mirror was not preferred or fallbacks were lost: %v", got)
	}
	preference.remember(next[2])
	want = []string{next[2], next[0], next[1]}
	if got := preference.order(next); !reflect.DeepEqual(got, want) {
		t.Fatalf("replacement mirror was not preferred: %v", got)
	}
	if next[0] != "https://github.com/szx-jn/TrueUUID-DimensionWorks/releases/download/v1.0.9/mod.jar" {
		t.Fatal("candidate order was mutated")
	}
	other := []string{"https://example.com/mod.jar"}
	if got := preference.order(other); !reflect.DeepEqual(got, other) {
		t.Fatalf("unrelated source changed: %v", got)
	}
}

func TestAria2ReusesWorkingSourceAndFallsBackIfItFails(t *testing.T) {
	requireBundledAria2(t)
	downloadSourceOrder.mu.Lock()
	previous := downloadSourceOrder.origin
	downloadSourceOrder.origin = ""
	downloadSourceOrder.mu.Unlock()
	t.Cleanup(func() {
		downloadSourceOrder.mu.Lock()
		downloadSourceOrder.origin = previous
		downloadSourceOrder.mu.Unlock()
	})
	var requests [3]atomic.Int32
	var mirrorFailed atomic.Bool
	servers := make([]*httptest.Server, 3)
	for i := range servers {
		servers[i] = httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			requests[i].Add(1)
			if i == 0 || (i == 1 && mirrorFailed.Load()) {
				w.WriteHeader(http.StatusNotFound)
				return
			}
			fmt.Fprint(w, "asset")
		}))
		t.Cleanup(servers[i].Close)
	}
	candidates := []string{servers[0].URL, servers[1].URL, servers[2].URL}
	for attempt := 0; attempt < 3; attempt++ {
		if attempt == 2 {
			mirrorFailed.Store(true)
		}
		destination := filepath.Join(t.TempDir(), "asset")
		if err := downloadFromCandidates(downloadSourceOrder.order(candidates), destination, 100); err != nil {
			t.Fatal(err)
		}
		data, err := os.ReadFile(destination)
		if err != nil || string(data) != "asset" {
			t.Fatalf("wrong downloaded bytes: %q, %v", data, err)
		}
		if attempt == 1 && (requests[0].Load() != 1 || requests[1].Load() != 2 || requests[2].Load() != 0) {
			t.Fatal("second asset did not reuse the working source")
		}
	}
	if requests[0].Load() != 2 || requests[1].Load() != 3 || requests[2].Load() != 1 {
		t.Fatal("preferred source failure did not try the remaining fallbacks")
	}
}

func TestAria2KeepsSourceThroughShortTransferPause(t *testing.T) {
	requireBundledAria2(t)
	data := bytes.Repeat([]byte("x"), 64<<10)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Length", fmt.Sprint(len(data)))
		_, _ = w.Write(data[:32<<10])
		w.(http.Flusher).Flush()
		select {
		case <-time.After(7 * time.Second):
		case <-r.Context().Done():
			return
		}
		_, _ = w.Write(data[32<<10:])
	}))
	defer server.Close()
	destination := filepath.Join(t.TempDir(), "paused-asset")
	if err := downloadFileWithLimit(server.URL, destination, int64(len(data))); err != nil {
		t.Fatalf("short transfer pause should not be treated as an ETA over one hour: %v", err)
	}
	got, err := os.ReadFile(destination)
	if err != nil || !bytes.Equal(got, data) {
		t.Fatalf("paused transfer did not complete with correct bytes: %v", err)
	}
}
