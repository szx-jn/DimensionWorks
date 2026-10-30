
package main

import (
	"crypto/hmac"
	"io"
	"bufio"
	"archive/zip"
	"context"
	"bytes"
	"fmt"
	"net/http"
	"net/http/httptest"
	"errors"
	"crypto/sha256"
	"encoding/hex"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"sync/atomic"
	"testing"
	"time"
)
func TestAcquireUpdateLockSerializesConcurrentConfirmation(t *testing.T) {
	path := filepath.Join(t.TempDir(), "lock")
	if err := os.WriteFile(path, []byte("old-lock"), 0600); err != nil { t.Fatal(err) }
	reader, writer := io.Pipe()
	firstResult := make(chan error, 1)
	go func() {
		_, err := acquireUpdateLock(path, bufio.NewReader(reader), &bytes.Buffer{})
		firstResult <- err
	}()
	guard := path + ".confirm"
	deadline := time.Now().Add(2 * time.Second)
	for {
		if _, err := os.Stat(guard); err == nil { break }
		if time.Now().After(deadline) { t.Fatal("first updater did not acquire confirmation guard") }
		time.Sleep(10 * time.Millisecond)
	}
	if _, err := acquireUpdateLock(path, bufio.NewReader(strings.NewReader("Y\\n")), &bytes.Buffer{}); err == nil || !strings.Contains(err.Error(), "另一个更新器") {
		t.Fatalf("second updater should not enter simultaneous lock confirmation: %v", err)
	}
	_, _ = writer.Write([]byte("N\n"))
	_ = writer.Close()
	if err := <-firstResult; err == nil { t.Fatal("first updater should cancel on N") }
	if _, err := os.Stat(guard); err != nil { t.Fatalf("persistent confirmation guard file should remain: %v", err) }
	releaseGuard, err := acquireConfirmationGuard(guard)
	if err != nil { t.Fatalf("confirmation guard was not unlocked: %v", err) }
	releaseGuard()
}

func TestAcquireUpdateLockPromptsAndRefusesOnNo(t *testing.T) {
	path := filepath.Join(t.TempDir(), "lock")
	if err := os.WriteFile(path, []byte("old-lock"), 0600); err != nil { t.Fatal(err) }
	var output bytes.Buffer
	if _, err := acquireUpdateLock(path, bufio.NewReader(strings.NewReader("n\n")), &output); err == nil {
		t.Fatal("expected N to reject the update")
	}
	data, err := os.ReadFile(path)
	if err != nil { t.Fatal(err) }
	if string(data) != "old-lock" { t.Fatalf("N must preserve the existing lock, got %q", data) }
	if !strings.Contains(output.String(), "输入 Y") { t.Fatalf("missing lock confirmation prompt: %s", output.String()) }
}

func TestAcquireUpdateLockYReplacesExistingLock(t *testing.T) {
	path := filepath.Join(t.TempDir(), "lock")
	if err := os.WriteFile(path, []byte("old-lock"), 0600); err != nil { t.Fatal(err) }
	var output bytes.Buffer
	release, err := acquireUpdateLock(path, bufio.NewReader(strings.NewReader("Y\n")), &output)
	if err != nil { t.Fatalf("Y should clear the old lock and continue: %v", err) }
	data, err := os.ReadFile(path)
	if err != nil { t.Fatal(err) }
	if string(data) == "old-lock" || !strings.Contains(string(data), "token=") { t.Fatalf("new lock was not created: %q", data) }
	release()
	if _, err := os.Stat(path); !os.IsNotExist(err) { t.Fatalf("owned lock was not removed: %v", err) }
}

func TestOldLockOwnerCannotRemoveReplacementLock(t *testing.T) {
	path := filepath.Join(t.TempDir(), "lock")
	firstRelease, err := acquireUpdateLock(path, bufio.NewReader(strings.NewReader("")), &bytes.Buffer{})
	if err != nil { t.Fatal(err) }
	old, err := os.ReadFile(path)
	if err != nil { t.Fatal(err) }
	if err := os.Remove(path); err != nil { t.Fatal(err) }
	secondRelease, err := acquireUpdateLock(path, bufio.NewReader(strings.NewReader("")), &bytes.Buffer{})
	if err != nil { t.Fatal(err) }
	firstRelease()
	current, err := os.ReadFile(path)
	if err != nil { t.Fatal(err) }
	if bytes.Equal(old, current) { t.Fatal("replacement lock unexpectedly reused the old token") }
	secondRelease()
	if _, err := os.Stat(path); !os.IsNotExist(err) { t.Fatalf("replacement lock was not released: %v", err) }
}

func TestDownloadLimitStopsOversizedBody(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte("123456789"))
	}))
	defer server.Close()
	destination := filepath.Join(t.TempDir(), "manifest.tsv")
	if err := downloadFileWithLimit(server.URL, destination, false, 4); err == nil {
		t.Fatal("expected oversized response to be rejected during transfer")
	}
	if _, err := os.Stat(destination); !os.IsNotExist(err) {
		t.Fatalf("oversized destination should not remain: %v", err)
	}
}

func TestSequentialDownloadResumesFromSavedRange(t *testing.T) {
	var first atomic.Bool
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("ETag", "\"stable-v1\"")
		if r.Header.Get("Range") == "bytes=4-" {
			w.Header().Set("Content-Range", "bytes 4-9/10")
			w.Header().Set("Content-Length", "6")
			w.WriteHeader(http.StatusPartialContent)
			_, _ = w.Write([]byte("ghijkl"))
			return
		}
		first.Store(true)
		w.Header().Set("Content-Length", "10")
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte("abcd"))
		if flusher, ok := w.(http.Flusher); ok { flusher.Flush() }
		<-r.Context().Done()
	}))
	defer server.Close()
	cache := t.TempDir()
	oldCache := downloadCacheRoot
	downloadCacheRoot = cache
	defer func(){ downloadCacheRoot = oldCache }()
	destination := filepath.Join(t.TempDir(), "payload.bin")
	ctx, cancel := context.WithTimeout(context.Background(), 100*time.Millisecond)
	defer cancel()
	if err := downloadSequential(ctx, server.URL, destination, 1024); err == nil {
		t.Fatal("first transfer should stop after its context expires")
	}
	if !first.Load() { t.Fatal("initial full download request was not made") }
	part, metaPath, err := downloadResumePaths(server.URL, destination)
	if err != nil { t.Fatal(err) }
	info, err := os.Stat(part)
	if err != nil { t.Fatalf("partial data was not retained: %v", err) }
	if info.Size() != 4 { t.Fatalf("expected 4 retained bytes, got %d", info.Size()) }
	if _, err := os.Stat(metaPath); err != nil { t.Fatalf("resume metadata was not retained: %v", err) }
	if err := downloadSequential(context.Background(), server.URL, destination, 1024); err != nil {
		t.Fatalf("resume failed: %v", err)
	}
	data, err := os.ReadFile(destination)
	if err != nil { t.Fatal(err) }
	if string(data) != "abcdghijkl" { t.Fatalf("resumed content mismatch: %q", data) }
	if _, err := os.Stat(metaPath); !os.IsNotExist(err) { t.Fatalf("resume metadata should be removed after success: %v", err) }
}

func TestRangedDownloadResumesCompletedSegmentsAfterRestart(t *testing.T) {
	data := bytes.Repeat([]byte("dimensionworks"), int((parallelDownloadThreshold+4096)/14+1))
	var failed atomic.Bool
	var segmentRequests atomic.Int64
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("ETag", "\"range-v1\"")
		rangeValue := r.Header.Get("Range")
		if rangeValue == "bytes=0-0" {
			w.Header().Set("Content-Range", fmt.Sprintf("bytes 0-0/%d", len(data)))
			w.Header().Set("Content-Length", "1")
			w.WriteHeader(http.StatusPartialContent)
			_, _ = w.Write(data[:1])
			return
		}
		segmentRequests.Add(1)
		if failed.CompareAndSwap(false, true) {
			time.Sleep(100 * time.Millisecond)
			w.WriteHeader(http.StatusBadGateway)
			return
		}
		var begin, end int
		if _, err := fmt.Sscanf(rangeValue, "bytes=%d-%d", &begin, &end); err != nil || begin < 0 || end >= len(data) || begin > end {
			http.Error(w, "bad range", http.StatusBadRequest)
			return
		}
		body := data[begin:end+1]
		w.Header().Set("Content-Range", fmt.Sprintf("bytes %d-%d/%d", begin, end, len(data)))
		w.Header().Set("Content-Length", fmt.Sprint(len(body)))
		w.WriteHeader(http.StatusPartialContent)
		_, _ = w.Write(body)
	}))
	defer server.Close()
	cache := t.TempDir()
	oldCache := downloadCacheRoot
	downloadCacheRoot = cache
	defer func(){ downloadCacheRoot = oldCache }()
	destination := filepath.Join(t.TempDir(), "package.zip")
	if err := downloadRanged(context.Background(), server.URL, destination, int64(len(data))); err == nil {
		t.Fatal("first ranged attempt should fail once")
	}
	cached, err := filepath.Glob(filepath.Join(cache, "*.part"))
	if err != nil { t.Fatal(err) }
	if len(cached) == 0 { t.Fatal("completed segments were not persisted") }
	before := segmentRequests.Load()
	if err := downloadRanged(context.Background(), server.URL, destination, int64(len(data))); err != nil {
		t.Fatalf("second ranged attempt should resume: %v", err)
	}
	if segmentRequests.Load()-before >= int64(parallelDownloadWorkers) {
		t.Fatal("resume redownloaded every segment instead of reusing completed ones")
	}
	got, err := os.ReadFile(destination)
	if err != nil { t.Fatal(err) }
	if !bytes.Equal(got, data) { t.Fatal("resumed archive content does not match source") }
}

func TestCopyFileAtomicPreservesDestinationOnLimitFailure(t *testing.T) {
	root := t.TempDir()
	source := filepath.Join(root, "source.bin")
	destination := filepath.Join(root, "destination.bin")
	if err := os.WriteFile(source, []byte("source-content"), 0600); err != nil { t.Fatal(err) }
	if err := os.WriteFile(destination, []byte("original-destination"), 0600); err != nil { t.Fatal(err) }
	if err := copyFileLimited(source, destination, 0600, 4); err == nil {
		t.Fatal("expected limited copy to reject an oversized source")
	}
	got, err := os.ReadFile(destination)
	if err != nil { t.Fatal(err) }
	if string(got) != "original-destination" { t.Fatalf("failed copy damaged destination: %q", got) }
}

func TestCopyFileAtomicRejectsSymlinkSource(t *testing.T) {
	if runtime.GOOS == "windows" { t.Skip("Windows symlink creation may require elevated privileges") }
	root := t.TempDir()
	target := filepath.Join(root, "outside.txt")
	link := filepath.Join(root, "source-link")
	if err := os.WriteFile(target, []byte("outside"), 0600); err != nil { t.Fatal(err) }
	if err := os.Symlink(target, link); err != nil { t.Fatal(err) }
	if err := copyFile(link, filepath.Join(root, "copy.txt"), 0600); err == nil {
		t.Fatal("symlink source must be rejected")
	}
}

func TestReadStateRejectsTampering(t *testing.T) {
	root := t.TempDir()
	updateRoot := filepath.Join(root, ".dimensionworks-update")
	if err := os.MkdirAll(updateRoot, 0700); err != nil { t.Fatal(err) }
	key := bytes.Repeat([]byte{0x5a}, 32)
	if err := os.WriteFile(filepath.Join(updateRoot, "state.key"), key, 0600); err != nil { t.Fatal(err) }
	data := manifestBytes("client", "0.2.1", "v0.2.0", nil)
	statePath := filepath.Join(updateRoot, "state.tsv")
	if err := os.WriteFile(statePath, data, 0600); err != nil { t.Fatal(err) }
	mac := hmac.New(sha256.New, key)
	_, _ = mac.Write(data)
	if err := os.WriteFile(statePath+".hmac", []byte(hex.EncodeToString(mac.Sum(nil))+"\n"), 0600); err != nil { t.Fatal(err) }
	if _, err := readState(root, statePath, "client"); err != nil { t.Fatalf("valid signed state rejected: %v", err) }
	if err := os.WriteFile(statePath, manifestBytes("client", "0.2.2", "v0.2.1", nil), 0600); err != nil { t.Fatal(err) }
	if _, err := readState(root, statePath, "client"); err == nil || !strings.Contains(err.Error(), "签名校验失败") {
		t.Fatalf("tampered state must be rejected: %v", err)
	}
}

func TestReadStateLegacyManifestFallsBackToRescan(t *testing.T) {
	root := t.TempDir()
	updateRoot := filepath.Join(root, ".dimensionworks-update")
	if err := os.MkdirAll(updateRoot, 0700); err != nil { t.Fatal(err) }
	statePath := filepath.Join(updateRoot, "state.tsv")
	if err := os.WriteFile(statePath, manifestBytes("client", "0.2.1", "v0.2.0", nil), 0600); err != nil { t.Fatal(err) }
	state, err := readState(root, statePath, "client")
	if err != nil { t.Fatal(err) }
	if state != nil { t.Fatal("unsigned legacy state must be ignored and rebuilt from local files") }
}

func TestLoadOrCreateStateKeyPersistsStableKey(t *testing.T) {
	root := t.TempDir()
	first, err := loadOrCreateStateKey(root)
	if err != nil { t.Fatal(err) }
	second, err := loadOrCreateStateKey(root)
	if err != nil { t.Fatal(err) }
	if len(first) != 32 || !bytes.Equal(first, second) { t.Fatal("state key was not stable across reads") }
}

func TestLegacyStateMigrationBacksUpPreviouslyUntrackedTargets(t *testing.T) {
	root := t.TempDir()
	updateRoot := filepath.Join(root, ".dimensionworks-update")
	if err := os.MkdirAll(updateRoot, 0700); err != nil { t.Fatal(err) }
	rel := "config/user-settings.cfg"
	target := filepath.Join(root, filepath.FromSlash(rel))
	if err := os.MkdirAll(filepath.Dir(target), 0755); err != nil { t.Fatal(err) }
	if err := os.WriteFile(target, []byte("user-owned-settings"), 0644); err != nil { t.Fatal(err) }
	current, err := parseManifest(manifestBytes("client", "0.0.0", "v0.0.0", nil), "client")
	if err != nil { t.Fatal(err) }
	selected := []Entry{{Action: "archive", Path: rel, Algorithm: "sha256", Value: sha256Text("new-pack-default"), Source: rel}}
	backupDir, err := backupModifiedFiles(root, updateRoot, current, selected, true)
	if err != nil { t.Fatal(err) }
	if backupDir == "" { t.Fatal("legacy migration should back up an existing target not present in old state") }
	backup, err := os.ReadFile(filepath.Join(backupDir, filepath.FromSlash(rel)))
	if err != nil { t.Fatal(err) }
	if string(backup) != "user-owned-settings" { t.Fatalf("legacy backup content mismatch: %q", backup) }
}

func TestCompareVersions(t *testing.T) {
	cases := []struct { left, right string; want int }{
		{"0.2.10", "0.2.9", 1},
		{"1.0.0", "1.0.0", 0},
		{"0.1.99", "0.2.0", -1},
	}
	for _, tc := range cases {
		got, err := compareVersions(tc.left, tc.right)
		if err != nil { t.Fatalf("%s vs %s: %v", tc.left, tc.right, err) }
		if got != tc.want { t.Errorf("%s vs %s: got %d, want %d", tc.left, tc.right, got, tc.want) }
	}
	if _, err := compareVersions("1.0", "1.0.0"); err == nil { t.Fatal("expected malformed version to fail") }
}


func sha256Text(text string) string {
	sum := sha256.Sum256([]byte(text))
	return hex.EncodeToString(sum[:])
}

func manifestBytes(kind, version, base string, entries []Entry) []byte {
	m := &Manifest{
		Kind: kind, Version: version, Release: "v" + version, Base: base, Entries: entries,
	}
	return renderManifest(m)
}


func TestProgressResetClearsStaleSpeedBaseline(t *testing.T) {
	tracker := newProgressTracker()
	tracker.begin("file", "file.zip")
	tracker.setTotal("file", 100)
	tracker.add("file", 50)
	tracker.mu.Lock()
	tracker.lastSpeed = 12345
	tracker.mu.Unlock()
	tracker.reset("file")
	tracker.mu.Lock()
	defer tracker.mu.Unlock()
	if tracker.lastSpeed != 0 { t.Fatalf("stale speed remained after reset: %f", tracker.lastSpeed) }
	if tracker.lastBytes != 0 { t.Fatalf("speed baseline should reset to zero, got %d", tracker.lastBytes) }
	if tracker.files["file"].written != 0 { t.Fatalf("written bytes should reset, got %d", tracker.files["file"].written) }
}

func TestHTTPClientRejectsHTTPSDowngradeRedirect(t *testing.T) {
	plain := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		_, _ = w.Write([]byte("should-not-be-requested"))
	}))
	defer plain.Close()
	secure := httptest.NewTLSServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, plain.URL, http.StatusFound)
	}))
	defer secure.Close()
	client := newHTTPClient(5*time.Second, 5*time.Second)
	client.Transport = secure.Client().Transport
	resp, err := client.Get(secure.URL)
	if resp != nil { resp.Body.Close() }
	if err == nil { t.Fatal("expected HTTPS-to-HTTP redirect to be rejected") }
}

func TestDownloadRangedUsesConcurrentValidatedSegments(t *testing.T) {
	data := bytes.Repeat([]byte("DimensionWorks-parallel-download-check\x00"), (9<<20)/39+1)
	data = data[:9<<20]
	var active, peak atomic.Int32
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		rangeHeader := r.Header.Get("Range")
		if rangeHeader == "" {
			w.Header().Set("Content-Length", fmt.Sprint(len(data)))
			_, _ = w.Write(data)
			return
		}
		var begin, end int
		if _, err := fmt.Sscanf(rangeHeader, "bytes=%d-%d", &begin, &end); err != nil || begin < 0 || end < begin || end >= len(data) {
			http.Error(w, "invalid range", http.StatusRequestedRangeNotSatisfiable)
			return
		}
		current := active.Add(1)
		for {
			old := peak.Load()
			if current <= old || peak.CompareAndSwap(old, current) { break }
		}
		defer active.Add(-1)
		time.Sleep(15 * time.Millisecond)
		w.Header().Set("Content-Range", fmt.Sprintf("bytes %d-%d/%d", begin, end, len(data)))
		w.Header().Set("Content-Length", fmt.Sprint(end-begin+1))
		w.WriteHeader(http.StatusPartialContent)
		_, _ = w.Write(data[begin : end+1])
	}))
	defer server.Close()

	destination := filepath.Join(t.TempDir(), "download.bin")
	if err := downloadOnce(server.URL, destination); err != nil {
		t.Fatalf("downloadOnce failed: %v", err)
	}
	got, err := os.ReadFile(destination)
	if err != nil { t.Fatal(err) }
	if !bytes.Equal(got, data) { t.Fatal("downloaded content differs from source") }
	if peak.Load() < 2 { t.Fatalf("expected concurrent range requests, peak concurrency=%d", peak.Load()) }
	matches, err := filepath.Glob(destination + ".segment-*")
	if err != nil { t.Fatal(err) }
	if len(matches) != 0 { t.Fatalf("temporary segment files were not cleaned: %v", matches) }
}


func TestDownloadRangedFallsBackOnInvalidContentRange(t *testing.T) {
	data := bytes.Repeat([]byte("invalid-range-fallback"), (9<<20)/22+1)
	data = data[:9<<20]
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		rangeHeader := r.Header.Get("Range")
		if rangeHeader == "" {
			w.Header().Set("Content-Length", fmt.Sprint(len(data)))
			_, _ = w.Write(data)
			return
		}
		var begin, end int
		if _, err := fmt.Sscanf(rangeHeader, "bytes=%d-%d", &begin, &end); err != nil || end >= len(data) {
			http.Error(w, "invalid range", http.StatusRequestedRangeNotSatisfiable)
			return
		}
		w.Header().Set("Content-Length", fmt.Sprint(end-begin+1))
		if begin == 0 && end == 0 {
			w.Header().Set("Content-Range", fmt.Sprintf("bytes 0-0/%d", len(data)))
		} else {
			// The probe succeeds, but segment headers lie about the requested byte offsets.
			w.Header().Set("Content-Range", fmt.Sprintf("bytes %d-%d/%d", begin+1, end+1, len(data)))
		}
		w.WriteHeader(http.StatusPartialContent)
		_, _ = w.Write(data[begin : end+1])
	}))
	defer server.Close()

	destination := filepath.Join(t.TempDir(), "invalid-range.bin")
	if err := downloadOnce(server.URL, destination); err != nil {
		t.Fatalf("download should fall back after invalid Content-Range: %v", err)
	}
	got, err := os.ReadFile(destination)
	if err != nil { t.Fatal(err) }
	if !bytes.Equal(got, data) { t.Fatal("fallback content differs from source") }
	matches, err := filepath.Glob(destination + ".segment-*")
	if err != nil { t.Fatal(err) }
	if len(matches) != 0 { t.Fatalf("invalid segments were not cleaned: %v", matches) }
}

func TestDownloadVerifiedRejectsDigestMismatchAfterDownload(t *testing.T) {
	data := bytes.Repeat([]byte("integrity-check"), (9<<20)/15+1)
	data = data[:9<<20]
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		rangeHeader := r.Header.Get("Range")
		if rangeHeader == "" {
			w.Header().Set("Content-Length", fmt.Sprint(len(data)))
			_, _ = w.Write(data)
			return
		}
		var begin, end int
		if _, err := fmt.Sscanf(rangeHeader, "bytes=%d-%d", &begin, &end); err != nil || begin < 0 || end < begin || end >= len(data) {
			http.Error(w, "invalid range", http.StatusRequestedRangeNotSatisfiable)
			return
		}
		w.Header().Set("Content-Range", fmt.Sprintf("bytes %d-%d/%d", begin, end, len(data)))
		w.Header().Set("Content-Length", fmt.Sprint(end-begin+1))
		w.WriteHeader(http.StatusPartialContent)
		_, _ = w.Write(data[begin : end+1])
	}))
	defer server.Close()

	destination := filepath.Join(t.TempDir(), "digest-mismatch.bin")
	if err := downloadVerified(server.URL, destination, "sha256", sha256Text("not the downloaded payload")); err == nil {
		t.Fatal("expected digest mismatch to reject the downloaded file")
	}
	if _, err := os.Stat(destination); !os.IsNotExist(err) {
		t.Fatalf("rejected download must be removed, stat error=%v", err)
	}
}

func TestDownloadRangedFallsBackWhenServerIgnoresRange(t *testing.T) {
	data := bytes.Repeat([]byte("fallback"), (9<<20)/8)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Simulate a server or mirror that ignores Range and always returns 200.
		w.Header().Set("Content-Length", fmt.Sprint(len(data)))
		_, _ = w.Write(data)
	}))
	defer server.Close()

	destination := filepath.Join(t.TempDir(), "fallback.bin")
	if err := downloadOnce(server.URL, destination); err != nil {
		t.Fatalf("download should fall back to a single connection: %v", err)
	}
	got, err := os.ReadFile(destination)
	if err != nil { t.Fatal(err) }
	if !bytes.Equal(got, data) { t.Fatal("fallback content differs from source") }
	matches, err := filepath.Glob(destination + ".segment-*")
	if err != nil { t.Fatal(err) }
	if len(matches) != 0 { t.Fatalf("fallback left temporary segments: %v", matches) }
}


func TestDownloadRangeSegmentReportsProgressAgainstParentFile(t *testing.T) {
	data := []byte("segment-progress")
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, req *http.Request) {
		w.Header().Set("Content-Range", fmt.Sprintf("bytes 0-%d/%d", len(data)-1, len(data)))
		w.Header().Set("Content-Length", fmt.Sprint(len(data)))
		w.WriteHeader(http.StatusPartialContent)
		_, _ = w.Write(data)
	}))
	defer server.Close()

	key := filepath.Join(t.TempDir(), "archive.zip")
	segment := key + ".segment-00"
	progressBegin(key, "archive.zip")
	progressSetTotal(key, int64(len(data)))
	defer progressFinish(key, false)

	if err := downloadRangeSegment(context.Background(), server.URL, segment, key, 0, int64(len(data)-1), int64(len(data)), ""); err != nil {
		t.Fatalf("downloadRangeSegment failed: %v", err)
	}
	downloadProgressUI.mu.Lock()
	got := downloadProgressUI.files[key].written
	downloadProgressUI.mu.Unlock()
	if got != int64(len(data)) {
		t.Fatalf("parent progress should count segment bytes: got %d, want %d", got, len(data))
	}
}

func TestParseManifestValidClientAndServer(t *testing.T) {
	client := manifestBytes("client", "0.1.39", "v0.1.38", []Entry{
		{Action: "archive", Path: "config/example.cfg", Algorithm: "sha256", Value: sha256Text("x"), Source: "overrides/config/example.cfg"},
		{Action: "remote", Path: "mods/example.jar", Algorithm: "sha512", Value: strings.Repeat("a", 128), Source: "https://example.com/example.jar"},
	})
	parsed, err := parseManifest(client, "client")
	if err != nil {
		t.Fatalf("parse valid client manifest: %v", err)
	}
	if parsed.Version != "0.1.39" || parsed.Release != "v0.1.39" || len(parsed.Entries) != 2 {
		t.Fatalf("unexpected manifest: %#v", parsed)
	}

	server := manifestBytes("server", "0.1.39", "v0.1.38", []Entry{
		{Action: "archive", Path: "mods/example.jar", Algorithm: "sha256", Value: sha256Text("x"), Source: "mods/example.jar"},
	})
	if _, err := parseManifest(server, "server"); err != nil {
		t.Fatalf("parse valid server manifest: %v", err)
	}
}

func TestParseManifestRejectsUnsafeAndMalformedData(t *testing.T) {
	valid := manifestBytes("server", "0.1.39", "v0.1.38", []Entry{
		{Action: "archive", Path: "mods/example.jar", Algorithm: "sha256", Value: sha256Text("x"), Source: "mods/example.jar"},
	})
	cases := map[string][]byte{
		"bom": append([]byte{0xef, 0xbb, 0xbf}, valid...),
		"missing final newline": []byte(strings.TrimSuffix(string(valid), "\n")),
		"CRLF": []byte(strings.ReplaceAll(string(valid), "\n", "\r\n")),
		"path traversal": []byte(strings.Replace(string(valid), "mods/example.jar", "../escape.jar", 1)),
		"wrong kind": valid,
	}
	for name, data := range cases {
		t.Run(name, func(t *testing.T) {
			kind := "server"
			if name == "wrong kind" {
				kind = "client"
			}
			if _, err := parseManifest(data, kind); err == nil {
				t.Fatal("expected invalid manifest to be rejected")
			}
		})
	}
}

func TestReplaceFromFilePreservesExecutableMode(t *testing.T) {
	if runtime.GOOS == "windows" {
		t.Skip("Unix executable permission bits are not meaningful on Windows")
	}
	dir := t.TempDir()
	source := filepath.Join(dir, "source.sh")
	destination := filepath.Join(dir, "destination.sh")
	if err := os.WriteFile(source, []byte("#!/bin/sh\necho safe\n"), 0600); err != nil {
		t.Fatal(err)
	}
	if err := os.Chmod(source, 0751); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(destination, []byte("old"), 0600); err != nil {
		t.Fatal(err)
	}
	if err := replaceFromFile(source, destination); err != nil {
		t.Fatalf("replaceFromFile failed: %v", err)
	}
	info, err := os.Stat(destination)
	if err != nil {
		t.Fatal(err)
	}
	if got := info.Mode().Perm(); got != 0751 {
		t.Fatalf("expected executable mode 0751 to be preserved, got %04o", got)
	}
	got, err := os.ReadFile(destination)
	if err != nil {
		t.Fatal(err)
	}
	if string(got) != "#!/bin/sh\necho safe\n" {
		t.Fatalf("unexpected replacement content: %q", got)
	}
}

func TestValidateRelativePath(t *testing.T) {
	valid := []string{"mods/example.jar", "config/中文配置.toml", "a b/file.txt"}
	for _, path := range valid {
		if err := validateRelative(path); err != nil {
			t.Errorf("%q should be valid: %v", path, err)
		}
	}
	invalid := []string{"", ".", "..", "../outside", "mods/../outside", "/absolute", "C:/windows", "mods\\escape.jar", "mods//file.jar", " mods/file.jar"}
	for _, path := range invalid {
		if err := validateRelative(path); err == nil {
			t.Errorf("%q should be rejected", path)
		}
	}
}

func TestManifestURLFromPackageURL(t *testing.T) {
	cases := map[string]string{
		"https://host.example/release/DimensionWorks-0.1.39.mrpack": "https://host.example/release/DimensionWorks-0.1.39-update-manifest.tsv",
		"https://host.example/release/DimensionWorks-Server-0.1.39.zip?token=x": "https://host.example/release/DimensionWorks-Server-0.1.39-update-manifest.tsv?token=x",
	}
	for input, expected := range cases {
		got, err := manifestURLFromPackageURL(input)
		if err != nil {
			t.Fatalf("%s: %v", input, err)
		}
		if got != expected {
			t.Errorf("manifest URL mismatch: got %s, want %s", got, expected)
		}
	}
}



func TestBackupModifiedFilesBeforeOverwrite(t *testing.T) {
	root := t.TempDir()
	updateRoot := filepath.Join(root, ".dimensionworks-update")
	if err := os.MkdirAll(updateRoot, 0700); err != nil { t.Fatal(err) }
	rel := "mods/custom.jar"
	path := filepath.Join(root, filepath.FromSlash(rel))
	if err := os.MkdirAll(filepath.Dir(path), 0755); err != nil { t.Fatal(err) }
	if err := os.WriteFile(path, []byte("user-patched-content"), 0644); err != nil { t.Fatal(err) }
	previous := Entry{Action: "archive", Path: rel, Algorithm: "sha256", Value: sha256Text("original-content"), Source: rel}
	current, err := parseManifest(manifestBytes("client", "0.2.3", "v0.2.2", []Entry{previous}), "client")
	if err != nil { t.Fatal(err) }
	selected := []Entry{{Action: "archive", Path: rel, Algorithm: "sha256", Value: sha256Text("new-release-content"), Source: rel}}
	backupDir, err := backupModifiedFiles(root, updateRoot, current, selected)
	if err != nil { t.Fatal(err) }
	if backupDir == "" { t.Fatal("expected a persistent backup directory") }
	backup, err := os.ReadFile(filepath.Join(backupDir, filepath.FromSlash(rel)))
	if err != nil { t.Fatal(err) }
	if string(backup) != "user-patched-content" { t.Fatalf("backup content mismatch: %q", backup) }
	original, err := os.ReadFile(path)
	if err != nil || string(original) != "user-patched-content" { t.Fatalf("source changed during backup: %q, %v", original, err) }
}

func TestSelectChangesPreservesModifiedManagedFileOnRemoval(t *testing.T) {
	root := t.TempDir()
	path := filepath.Join(root, "mods", "locally-modified.jar")
	if err := os.MkdirAll(filepath.Dir(path), 0755); err != nil { t.Fatal(err) }
	if err := os.WriteFile(path, []byte("user-modified"), 0644); err != nil { t.Fatal(err) }
	old := Entry{Action: "archive", Path: "mods/locally-modified.jar", Algorithm: "sha256", Value: sha256Text("original-pack-file"), Source: "mods/locally-modified.jar"}
	current, err := parseManifest(manifestBytes("client", "0.2.3", "v0.2.2", []Entry{old}), "client")
	if err != nil { t.Fatal(err) }
	target, err := parseManifest(manifestBytes("client", "0.2.4", "v0.2.3", nil), "client")
	if err != nil { t.Fatal(err) }
	_, deleted, err := selectChanges(root, "client", current, target)
	if err != nil { t.Fatal(err) }
	if len(deleted) != 0 { t.Fatalf("modified user file must not be deleted: %v", deleted) }
	got, err := os.ReadFile(path)
	if err != nil || string(got) != "user-modified" { t.Fatalf("user file was changed: %q, %v", got, err) }
}

func TestParseManifestRejectsOversizedInput(t *testing.T) {
	data := bytes.Repeat([]byte("x"), maxManifestBytes+1)
	if _, err := parseManifest(data, "client"); err == nil { t.Fatal("expected oversized manifest to be rejected") }
}

func TestSelectChangesPreservesUnchangedUserModifiedClientConfig(t *testing.T) {
	root := t.TempDir()
	path := filepath.Join(root, "config", "custom.cfg")
	if err := os.MkdirAll(filepath.Dir(path), 0755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(path, []byte("user-edit"), 0644); err != nil {
		t.Fatal(err)
	}
	targetEntry := Entry{Action: "archive", Path: "config/custom.cfg", Algorithm: "sha256", Value: sha256Text("pack-version"), Source: "overrides/config/custom.cfg"}
	current, err := parseManifest(manifestBytes("client", "0.1.38", "v0.1.37", []Entry{targetEntry}), "client")
	if err != nil {
		t.Fatal(err)
	}
	target, err := parseManifest(manifestBytes("client", "0.1.39", "v0.1.38", []Entry{targetEntry}), "client")
	if err != nil {
		t.Fatal(err)
	}
	selected, deleted, err := selectChanges(root, "client", current, target)
	if err != nil {
		t.Fatal(err)
	}
	if len(selected) != 0 || len(deleted) != 0 {
		t.Fatalf("user config should not be overwritten when pack content is unchanged; selected=%v deleted=%v", selected, deleted)
	}
}

func TestSelectChangesRestoresDeletedManagedFile(t *testing.T) {
	root := t.TempDir()
	entry := Entry{Action: "archive", Path: "mods/managed.jar", Algorithm: "sha256", Value: sha256Text("pack"), Source: "mods/managed.jar"}
	current, err := parseManifest(manifestBytes("client", "0.2.3", "v0.2.3", []Entry{entry}), "client")
	if err != nil {
		t.Fatal(err)
	}
	target, err := parseManifest(manifestBytes("client", "0.2.3", "v0.2.3", []Entry{entry}), "client")
	if err != nil {
		t.Fatal(err)
	}

	selected, deleted, err := selectChanges(root, "client", current, target)
	if err != nil {
		t.Fatal(err)
	}
	if len(selected) != 1 || selected[0].Path != entry.Path {
		t.Fatalf("deleted managed file must be restored: selected=%#v deleted=%#v", selected, deleted)
	}
}

func TestSelectChangesPreservesExistingServerRuntimeConfig(t *testing.T) {
	root := t.TempDir()
	if err := os.WriteFile(filepath.Join(root, "server.properties"), []byte("level-name=myworld\n"), 0644); err != nil {
		t.Fatal(err)
	}
	old := Entry{Action: "archive", Path: "server.properties", Algorithm: "sha256", Value: sha256Text("old default"), Source: "server.properties"}
	targetEntry := Entry{Action: "archive", Path: "server.properties", Algorithm: "sha256", Value: sha256Text("new default"), Source: "server.properties"}
	current, err := parseManifest(manifestBytes("server", "0.1.38", "v0.1.37", []Entry{old}), "server")
	if err != nil {
		t.Fatal(err)
	}
	target, err := parseManifest(manifestBytes("server", "0.1.39", "v0.1.38", []Entry{targetEntry}), "server")
	if err != nil {
		t.Fatal(err)
	}
	selected, _, err := selectChanges(root, "server", current, target)
	if err != nil {
		t.Fatal(err)
	}
	if len(selected) != 0 {
		t.Fatalf("existing server.properties must be preserved: %#v", selected)
	}
}


func TestSelectChangesPreservesServerConfigsAndConfiguredWorld(t *testing.T) {
	root := t.TempDir()
	if err := os.WriteFile(filepath.Join(root, "server.properties"), []byte("level-name=custom_world\n"), 0644); err != nil {
		t.Fatal(err)
	}
	files := map[string]string{
		"config/luckperms/luckperms.conf": "user-luckperms-config",
		"config/luckperms/luckperms-h2-v2.mv.db": "user-luckperms-database",
		"defaultconfigs/create-server.toml": "user-default-world-config",
		"custom_world/level.dat": "keepInventory=true",
		"custom_world/serverconfig/create-server.toml": "maxRotationSpeed=10240",
		"config/removed-by-new-release.cfg": "user-config",
	}
	for rel, content := range files {
		path := filepath.Join(root, filepath.FromSlash(rel))
		if err := os.MkdirAll(filepath.Dir(path), 0755); err != nil {
			t.Fatal(err)
		}
		if err := os.WriteFile(path, []byte(content), 0644); err != nil {
			t.Fatal(err)
		}
	}
	oldEntries := []Entry{
		{Action: "archive", Path: "config/luckperms/luckperms.conf", Algorithm: "sha256", Value: sha256Text("old-default"), Source: "config/luckperms/luckperms.conf"},
		{Action: "archive", Path: "config/luckperms/luckperms-h2-v2.mv.db", Algorithm: "sha256", Value: sha256Text("old-database"), Source: "config/luckperms/luckperms-h2-v2.mv.db"},
		{Action: "archive", Path: "defaultconfigs/create-server.toml", Algorithm: "sha256", Value: sha256Text("old-world-default"), Source: "defaultconfigs/create-server.toml"},
		{Action: "archive", Path: "custom_world/level.dat", Algorithm: "sha256", Value: sha256Text("old-level"), Source: "custom_world/level.dat"},
		{Action: "archive", Path: "custom_world/serverconfig/create-server.toml", Algorithm: "sha256", Value: sha256Text("old-serverconfig"), Source: "custom_world/serverconfig/create-server.toml"},
		{Action: "archive", Path: "config/removed-by-new-release.cfg", Algorithm: "sha256", Value: sha256Text("old-config"), Source: "config/removed-by-new-release.cfg"},
	}
	newEntries := []Entry{
		{Action: "archive", Path: "config/luckperms/luckperms.conf", Algorithm: "sha256", Value: sha256Text("new-default"), Source: "config/luckperms/luckperms.conf"},
		{Action: "archive", Path: "config/luckperms/luckperms-h2-v2.mv.db", Algorithm: "sha256", Value: sha256Text("new-database"), Source: "config/luckperms/luckperms-h2-v2.mv.db"},
		{Action: "archive", Path: "defaultconfigs/create-server.toml", Algorithm: "sha256", Value: sha256Text("new-world-default"), Source: "defaultconfigs/create-server.toml"},
	}
	current, err := parseManifest(manifestBytes("server", "0.1.38", "v0.1.37", oldEntries), "server")
	if err != nil {
		t.Fatal(err)
	}
	target, err := parseManifest(manifestBytes("server", "0.1.39", "v0.1.38", newEntries), "server")
	if err != nil {
		t.Fatal(err)
	}
	selected, deleted, err := selectChanges(root, "server", current, target)
	if err != nil {
		t.Fatal(err)
	}
	if len(selected) != 0 {
		t.Fatalf("existing server config/data must not be overwritten: %#v", selected)
	}
	if len(deleted) != 0 {
		t.Fatalf("existing config/world files must not be deleted: %#v", deleted)
	}
}

func TestRecoverInterruptedTransactionRestoresOriginalFile(t *testing.T) {
	root := t.TempDir()
	updateRoot := filepath.Join(root, ".dimensionworks-update")
	work := filepath.Join(updateRoot, "work-interrupted")
	backup := filepath.Join(work, "backup", "config", "value.cfg")
	target := filepath.Join(root, "config", "value.cfg")
	if err := os.MkdirAll(filepath.Dir(backup), 0700); err != nil { t.Fatal(err) }
	if err := os.MkdirAll(filepath.Dir(target), 0755); err != nil { t.Fatal(err) }
	if err := os.WriteFile(backup, []byte("before-crash"), 0600); err != nil { t.Fatal(err) }
	if err := os.WriteFile(target, []byte("partially-updated"), 0644); err != nil { t.Fatal(err) }
	if err := appendTransactionJournal(work, Applied{Path: "config/value.cfg", Backup: backup, Existed: true, Mode: 0644}); err != nil { t.Fatal(err) }
	journal, err := os.OpenFile(filepath.Join(work, "transaction.jsonl"), os.O_APPEND|os.O_WRONLY, 0600)
	if err != nil { t.Fatal(err) }
	if _, err := journal.Write([]byte("{\"Path\":")); err != nil { t.Fatal(err) }
	if err := journal.Close(); err != nil { t.Fatal(err) }
	if err := recoverInterruptedTransactions(root, updateRoot); err != nil { t.Fatalf("recovery failed with a truncated journal tail: %v", err) }
	got, err := os.ReadFile(target)
	if err != nil { t.Fatal(err) }
	if string(got) != "before-crash" { t.Fatalf("recovery did not restore original file: %q", got) }
	if _, err := os.Stat(work); !os.IsNotExist(err) { t.Fatalf("recovered work directory was not removed: %v", err) }
}

func TestApplyTransactionRollsBackEarlierWrites(t *testing.T) {
	root := t.TempDir()
	original := filepath.Join(root, "config", "value.cfg")
	if err := os.MkdirAll(filepath.Dir(original), 0755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(original, []byte("before"), 0644); err != nil {
		t.Fatal(err)
	}
	if runtime.GOOS != "windows" {
		if err := os.Chmod(original, 0751); err != nil {
			t.Fatal(err)
		}
	}
	work := filepath.Join(root, ".dimensionworks-update", "work-test")
	stage := filepath.Join(work, "stage")
	if err := os.MkdirAll(stage, 0755); err != nil {
		t.Fatal(err)
	}
	staged := filepath.Join(stage, "value.cfg")
	if err := os.WriteFile(staged, []byte("after"), 0644); err != nil {
		t.Fatal(err)
	}
	err := applyTransaction(root, work, []Change{
		{Path: "config/value.cfg", Staged: staged},
		{Path: "../escape.cfg", Staged: staged},
	})
	if err == nil {
		t.Fatal("expected transaction to fail")
	}
	data, readErr := os.ReadFile(original)
	if readErr != nil {
		t.Fatal(readErr)
	}
	if string(data) != "before" {
		t.Fatalf("rollback did not restore original file: %q", data)
	}
	if runtime.GOOS != "windows" {
		info, err := os.Stat(original)
		if err != nil {
			t.Fatal(err)
		}
		if got := info.Mode().Perm(); got != 0751 {
			t.Fatalf("rollback did not restore original permissions: got %04o, want 0751", got)
		}
	}
}

func TestOpenArchiveRejectsTraversal(t *testing.T) {
	path := filepath.Join(t.TempDir(), "bad.zip")
	file, err := os.Create(path)
	if err != nil {
		t.Fatal(err)
	}
	archive := zip.NewWriter(file)
	member, err := archive.Create("../outside.txt")
	if err != nil {
		t.Fatal(err)
	}
	_, _ = member.Write([]byte("bad"))
	if err := archive.Close(); err != nil {
		t.Fatal(err)
	}
	if err := file.Close(); err != nil {
		t.Fatal(err)
	}
	if _, err := openArchive(path); err == nil {
		t.Fatal("archive path traversal must be rejected")
	}
}

func TestValidateInstanceRootRequiresAnInstalledInstance(t *testing.T) {
	root := t.TempDir()
	if err := validateInstanceRoot(root, "client"); err == nil {
		t.Fatal("Downloads-like directory must not be accepted as a client instance")
	}
	if err := validateInstanceRoot(root, "server"); err == nil {
		t.Fatal("Downloads-like directory must not be accepted as a server instance")
	}
	if err := os.Mkdir(filepath.Join(root, "mods"), 0755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(root, "VERSION"), []byte("0.1.38\n"), 0644); err != nil {
		t.Fatal(err)
	}
	if err := validateInstanceRoot(root, "client"); err != nil {
		t.Fatalf("valid client instance markers rejected: %v", err)
	}
	if err := validateInstanceRoot(root, "server"); err != nil {
		t.Fatalf("valid server instance markers rejected: %v", err)
	}
}

func TestWriteFailureLogPersistsReadableDiagnostic(t *testing.T) {
	root := t.TempDir()
	logPath := writeFailureLog(root, errors.New("expected diagnostic"))
	if logPath == "" {
		t.Fatal("expected a diagnostic log to be written")
	}
	data, err := os.ReadFile(logPath)
	if err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(string(data), "expected diagnostic") || !strings.Contains(string(data), "\nerror=") {
		t.Fatalf("diagnostic log is not readable: %q", data)
	}
}

func TestOptionsRejectConflictsAndInvalidTags(t *testing.T) {
	if _, err := parseOptions([]string{"--release", "v1.2.3", "--package", "x.zip"}); err == nil {
		t.Fatal("mutually exclusive options should be rejected")
	}
	if _, err := parseOptions([]string{"--release", "1.2.3"}); err == nil {
		t.Fatal("release tag without v should be rejected")
	}
	opts, err := parseOptions([]string{"--help"})
	if err != nil || !opts.Help {
		t.Fatalf("help should parse: opts=%+v err=%v", opts, err)
	}
}


func TestProgressTrackerTracksParallelTransfersAndRetries(t *testing.T) {
	tracker := newProgressTracker()
	tracker.begin("a", "a.jar")
	tracker.begin("b", "b.jar")
	tracker.setTotal("a", 100)
	tracker.setTotal("b", 200)
	tracker.add("a", 40)
	tracker.add("b", 60)
	tracker.reset("a")
	tracker.add("a", 25)

	tracker.mu.Lock()
	defer tracker.mu.Unlock()
	if got := tracker.files["a"].written; got != 25 {
		t.Fatalf("retry should reset prior bytes, got %d", got)
	}
	if got := tracker.files["b"].written; got != 60 {
		t.Fatalf("parallel transfer bytes were lost, got %d", got)
	}
	if got := tracker.files["a"].total; got != 0 {
		t.Fatalf("retry should reset attempt total, got %d", got)
	}
}
