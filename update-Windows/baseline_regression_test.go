package main

import (
	"crypto/sha256"
	"fmt"
	"os"
	"path/filepath"
	"testing"
)

func TestRootVersionAndFileInventoryAreIndependent(t *testing.T) {
	root := t.TempDir()
	if err := os.WriteFile(filepath.Join(root, "VERSION"), []byte("0.2.63\n"), 0644); err != nil {
		t.Fatal(err)
	}
	local := []byte("locally edited README")
	if err := os.WriteFile(filepath.Join(root, "README.md"), local, 0644); err != nil {
		t.Fatal(err)
	}
	version, err := readInstalledVersion(root)
	if err != nil || version != "0.2.63" {
		t.Fatalf("installed VERSION not read: %q, %v", version, err)
	}
	current, err := scanInstalledFiles(root, "server")
	if err != nil {
		t.Fatal(err)
	}
	if current.Version != "" || current.Release != "" {
		t.Fatalf("file inventory must not supply an installed version: %+v", current)
	}
	if len(current.Entries) != 1 || current.Entries[0].Value != fmt.Sprintf("%x", sha256.Sum256(local)) {
		t.Fatalf("bootstrap must hash actual local files: %+v", current.Entries)
	}
	trusted, err := readState(root, filepath.Join(root, filepath.FromSlash(stateName)), "server")
	if err != nil || trusted != nil {
		t.Fatalf("VERSION must not create trusted deletion state: %v, %v", trusted, err)
	}
}

func TestMissingOrInvalidVersionFailsExplicitly(t *testing.T) {
	for _, version := range []string{"", "invalid", "v0.2.63", "0.2.63-beta"} {
		t.Run(version, func(t *testing.T) {
			root := t.TempDir()
			if version != "" {
				if err := os.WriteFile(filepath.Join(root, "VERSION"), []byte(version), 0644); err != nil {
					t.Fatal(err)
				}
			}
			version, err := readInstalledVersion(root)
			if err == nil || version != "" {
				t.Fatalf("invalid VERSION must fail without a fabricated version: %q, %v", version, err)
			}
		})
	}
}

func TestIncrementalHistoryStopsAtInstalledVersion(t *testing.T) {
	for _, tc := range []struct {
		name, current, base       string
		wantHistory, wantPackages int
		wantError                 bool
	}{
		{"two upgrades", "v0.2.63", "v0.2.64", 1, 2, false},
		{"one upgrade", "v0.2.64", "v0.2.64", 0, 1, false},
		{"same version repair", "v0.2.65", "v0.2.64", 0, 0, true},
		{"downgrade", "v0.2.66", "v0.2.64", 0, 0, true},
		{"invalid baseline", "", "v0.2.64", 0, 0, true},
		{"explicit zero version", "v0.0.0", "v0.0.0", 0, 1, false},
		{"skipped installed baseline", "v0.2.63", "v0.2.62", 0, 0, true},
		{"broken chain crosses baseline", "v0.2.63", "v0.2.64", 1, 0, true},
	} {
		t.Run(tc.name, func(t *testing.T) {
			var manifests, infos int
			target := &Manifest{Kind: "server", Version: "0.2.65", Release: "v0.2.65", Base: tc.base}
			chain, err := incrementalReleaseChainWithLoaders("server", tc.current, target, nil, t.TempDir(),
				func(kind, tag, work string) (*Manifest, error) {
					manifests++
					if tag != "v0.2.64" {
						t.Fatalf("unnecessary historical TSV requested: %s", tag)
					}
					base := "v0.2.63"
					if tc.name == "broken chain crosses baseline" {
						base = "v0.2.62"
					}
					return &Manifest{Kind: "server", Version: "0.2.64", Release: tag, Base: base}, nil
				}, func(tag string) (*ReleaseInfo, error) { infos++; return &ReleaseInfo{TagName: tag}, nil })
			if (err != nil) != tc.wantError {
				t.Fatalf("chain error = %v", err)
			}
			if manifests != tc.wantHistory || infos != tc.wantHistory {
				t.Fatalf("history requests: TSV=%d API=%d, want %d", manifests, infos, tc.wantHistory)
			}
			if len(chain) != tc.wantPackages {
				t.Fatalf("got %d packages, want %d", len(chain), tc.wantPackages)
			}
			if len(chain) == 2 && (chain[0].manifest.Release != "v0.2.64" || chain[1].manifest.Release != "v0.2.65") {
				t.Fatal("incremental packages must apply oldest first")
			}
		})
	}
}
