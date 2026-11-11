package main

import (
	"archive/zip"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"testing"
)

func TestReadInstalledVersionFileFormats(t *testing.T) {
	for _, tc := range []struct {
		name, contents, want string
	}{
		{"plain", "0.3.14", "0.3.14"},
		{"unix newline", "0.3.14\n", "0.3.14"},
		{"BOM CRLF whitespace", "\ufeff \t0.3.14\r\n", "0.3.14"},
		{"explicit zero", "0.0.0\n", "0.0.0"},
		{"largest component", "2147483647.0.0\n", "2147483647.0.0"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			root := filepath.Join(t.TempDir(), "维度 工坊")
			if err := os.Mkdir(root, 0755); err != nil {
				t.Fatal(err)
			}
			if err := os.WriteFile(filepath.Join(root, "VERSION"), []byte(tc.contents), 0644); err != nil {
				t.Fatal(err)
			}
			got, err := readInstalledVersion(root)
			if err != nil || got != tc.want {
				t.Fatalf("VERSION = %q, %v; want %q", got, err, tc.want)
			}
		})
	}
}

func TestReadInstalledVersionRejectsMalformedFiles(t *testing.T) {
	for _, contents := range []string{
		"", "v0.3.14", "0.3.14-beta", "0.3", "00.3.14", "0.+3.14", "-1.3.14",
		"0.3.14\n0.3.15", "０.3.14", "2147483648.0.0", "\x000.3.14", "\xff0.3.14",
		"\xff\xfe0\x00.\x003\x00.\x001\x004\x00", strings.Repeat(" ", 65) + "0.3.14",
	} {
		t.Run(hex.EncodeToString([]byte(contents)), func(t *testing.T) {
			root := t.TempDir()
			path := filepath.Join(root, "VERSION")
			if err := os.WriteFile(path, []byte(contents), 0644); err != nil {
				t.Fatal(err)
			}
			got, err := readInstalledVersion(root)
			if err == nil || got != "" || !strings.Contains(err.Error(), path) {
				t.Fatalf("invalid VERSION must report its exact path without a fallback: %q, %v", got, err)
			}
		})
	}
}

func TestReadInstalledVersionNeverSearchesOtherLocations(t *testing.T) {
	parent := t.TempDir()
	root := filepath.Join(parent, "game")
	if err := os.MkdirAll(filepath.Join(root, "config"), 0755); err != nil {
		t.Fatal(err)
	}
	for _, path := range []string{filepath.Join(parent, "VERSION"), filepath.Join(root, "config", "VERSION")} {
		if err := os.WriteFile(path, []byte("9.9.9\n"), 0644); err != nil {
			t.Fatal(err)
		}
	}
	if err := os.WriteFile(filepath.Join(root, "modrinth.index.json"), []byte(`{"name":"DimensionWorks","versionId":"9.9.9"}`), 0644); err != nil {
		t.Fatal(err)
	}
	writeVersionTestState(t, root, "client", "9.9.9")
	if version, err := readInstalledVersion(root); err == nil || version != "" {
		t.Fatalf("missing root VERSION was inferred from other files: %q, %v", version, err)
	}
}

func TestReadInstalledVersionRejectsDirectoryAndSymlink(t *testing.T) {
	root := t.TempDir()
	path := filepath.Join(root, "VERSION")
	if err := os.Mkdir(path, 0755); err != nil {
		t.Fatal(err)
	}
	if _, err := readInstalledVersion(root); err == nil {
		t.Fatal("directory accepted as VERSION")
	}
	if runtime.GOOS == "windows" {
		return
	}
	if err := os.Remove(path); err != nil {
		t.Fatal(err)
	}
	outside := filepath.Join(t.TempDir(), "VERSION")
	if err := os.WriteFile(outside, []byte("9.9.9"), 0644); err != nil {
		t.Fatal(err)
	}
	if err := os.Symlink(outside, path); err != nil {
		t.Fatal(err)
	}
	if _, err := readInstalledVersion(root); err == nil {
		t.Fatal("symlink accepted as VERSION")
	}
}

func TestVerifyInstalledVersionDetectsChanges(t *testing.T) {
	root := t.TempDir()
	path := filepath.Join(root, "VERSION")
	if err := os.WriteFile(path, []byte("0.3.14\n"), 0644); err != nil {
		t.Fatal(err)
	}
	initial, err := readInstalledVersion(root)
	if err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(path, []byte("0.3.15\n"), 0644); err != nil {
		t.Fatal(err)
	}
	if err := verifyInstalledVersion(root, initial); err == nil {
		t.Fatal("changed VERSION did not invalidate the update plan")
	}
}

func writeVersionTestState(t *testing.T, root, kind, version string) {
	t.Helper()
	updateRoot := filepath.Join(root, ".dimensionworks-update")
	if err := os.MkdirAll(updateRoot, 0755); err != nil {
		t.Fatal(err)
	}
	key, err := loadOrCreateStateKey(updateRoot)
	if err != nil {
		t.Fatal(err)
	}
	data := manifestBytes(kind, version, "none", nil)
	mac := hmac.New(sha256.New, key)
	_, _ = mac.Write(data)
	if err := os.WriteFile(filepath.Join(updateRoot, "state.tsv"), data, 0644); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(updateRoot, "state.tsv.hmac"), []byte(hex.EncodeToString(mac.Sum(nil))+"\n"), 0600); err != nil {
		t.Fatal(err)
	}
}

func writeVersionTestPackage(t *testing.T, kind, version string, corrupt bool) string {
	t.Helper()
	path := filepath.Join(t.TempDir(), "update.zip")
	file, err := os.Create(path)
	if err != nil {
		t.Fatal(err)
	}
	writer := zip.NewWriter(file)
	source := "config/version-probe.txt"
	if kind == "client" {
		source = "overrides/" + source
	}
	payload := "updated file"
	entry := Entry{Action: "archive", Path: "config/version-probe.txt", Source: source, Algorithm: "sha256", Value: sha256Text(payload)}
	if corrupt {
		payload = "corrupt file"
	}
	manifest := manifestBytes(kind, version, "none", []Entry{entry})
	items := []struct {
		name string
		data []byte
	}{
		{source, []byte(payload)},
	}
	if kind == "client" {
		items = append(items, struct {
			name string
			data []byte
		}{"modrinth.index.json", []byte(`{"formatVersion":1,"game":"minecraft","files":[]}`)})
	}
	for _, item := range items {
		member, err := writer.Create(item.name)
		if err != nil {
			t.Fatal(err)
		}
		if _, err := member.Write(item.data); err != nil {
			t.Fatal(err)
		}
	}
	if err := writer.Close(); err != nil {
		t.Fatal(err)
	}
	if err := file.Close(); err != nil {
		t.Fatal(err)
	}
	manifestPath := strings.TrimSuffix(path, filepath.Ext(path)) + "-update-manifest.tsv"
	if err := os.WriteFile(manifestPath, manifest, 0644); err != nil {
		t.Fatal(err)
	}
	return path
}

func TestUpdateUsesRootVersionDespiteConflictingSignedState(t *testing.T) {
	for _, kind := range []string{"client", "server"} {
		for _, tc := range []struct {
			name, installed, state, target string
			wantFailure                    bool
		}{
			{"upgrade with newer state", "0.3.14", "9.9.9", "0.3.15", false},
			{"downgrade with older state", "0.3.16", "0.1.0", "0.3.15", true},
			{"same version repair", "0.3.15", "9.9.9", "0.3.15", false},
			{"explicit zero", "0.0.0", "9.9.9", "0.0.1", false},
		} {
			t.Run(kind+"/"+tc.name, func(t *testing.T) {
				root := t.TempDir()
				if err := os.WriteFile(filepath.Join(root, "VERSION"), []byte(tc.installed+"\n"), 0644); err != nil {
					t.Fatal(err)
				}
				writeVersionTestState(t, root, kind, tc.state)
				pkg := writeVersionTestPackage(t, kind, tc.target, false)
				err := updateInstance(root, kind, Options{Package: pkg})
				if (err != nil) != tc.wantFailure {
					t.Fatalf("update error = %v, want failure %v", err, tc.wantFailure)
				}
				want := tc.target
				if tc.wantFailure {
					want = tc.installed
				}
				got, readErr := readInstalledVersion(root)
				if readErr != nil || got != want {
					t.Fatalf("root VERSION = %q, %v; want %q", got, readErr, want)
				}
				if tc.wantFailure {
					if !strings.Contains(err.Error(), "拒绝将版本") {
						t.Fatalf("unexpected failure: %v", err)
					}
					if _, err := os.Stat(filepath.Join(root, "config", "version-probe.txt")); !os.IsNotExist(err) {
						t.Fatalf("rejected downgrade changed files: %v", err)
					}
				} else {
					data, err := os.ReadFile(filepath.Join(root, "config", "version-probe.txt"))
					if err != nil || string(data) != "updated file" {
						t.Fatalf("updated payload = %q, %v", data, err)
					}
					state, err := readState(root, filepath.Join(root, filepath.FromSlash(stateName)), kind)
					if err != nil || state == nil || state.Version != tc.target {
						t.Fatalf("new file inventory failed verification: %v, %v", state, err)
					}
				}
			})
		}
	}
}

func TestUpdateRejectsInvalidRootVersionBeforeOpeningPackage(t *testing.T) {
	for _, contents := range []string{"", "invalid", "v0.3.14"} {
		t.Run(contents, func(t *testing.T) {
			root := t.TempDir()
			if contents != "" {
				if err := os.WriteFile(filepath.Join(root, "VERSION"), []byte(contents), 0644); err != nil {
					t.Fatal(err)
				}
			}
			writeVersionTestState(t, root, "client", "0.3.14")
			err := updateInstance(root, "client", Options{Package: filepath.Join(root, "does-not-exist.zip")})
			if err == nil || !strings.Contains(err.Error(), filepath.Join(root, "VERSION")) {
				t.Fatalf("invalid root VERSION did not stop the update before package access: %v", err)
			}
		})
	}
}

func TestInvalidPackageDoesNotAdvanceRootVersion(t *testing.T) {
	root := t.TempDir()
	if err := os.WriteFile(filepath.Join(root, "VERSION"), []byte("0.3.14\n"), 0644); err != nil {
		t.Fatal(err)
	}
	writeVersionTestState(t, root, "client", "0.3.14")
	err := updateInstance(root, "client", Options{Package: writeVersionTestPackage(t, "client", "0.3.15", true)})
	if err == nil {
		t.Fatal("invalid package accepted")
	}
	if got, err := readInstalledVersion(root); err != nil || got != "0.3.14" {
		t.Fatalf("failed update advanced VERSION: %q, %v", got, err)
	}
}

func TestInterruptedVersionTransactionIsRecoveredBeforeReading(t *testing.T) {
	root := t.TempDir()
	work := filepath.Join(root, ".dimensionworks-update", "work-interrupted")
	backup := filepath.Join(work, "backup", "VERSION")
	if err := os.MkdirAll(filepath.Dir(backup), 0755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(backup, []byte("0.3.14\n"), 0644); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(root, "VERSION"), []byte("interrupted-invalid"), 0644); err != nil {
		t.Fatal(err)
	}
	if err := appendTransactionJournal(work, Applied{Path: "VERSION", Backup: backup, Existed: true, TargetPath: filepath.Join(root, "VERSION"), Mode: 0644}); err != nil {
		t.Fatal(err)
	}
	err := updateInstance(root, "client", Options{Package: writeVersionTestPackage(t, "client", "0.3.15", false)})
	if err != nil {
		t.Fatal(err)
	}
	if got, err := readInstalledVersion(root); err != nil || got != "0.3.15" {
		t.Fatalf("update did not use recovered VERSION: %q, %v", got, err)
	}
}
