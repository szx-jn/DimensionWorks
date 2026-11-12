package main

import (
	"os"
	"path/filepath"
	"runtime"
	"testing"
)

func TestSupportedLauncherName(t *testing.T) {
	cases := []struct {
		input string
		want  string
	}{
		{"HMCL", "HMCL"},
		{"Hello Minecraft Launcher", "HMCL"},
		{"PrismLauncher", "Prism Launcher"},
		{"Prism Launcher", "Prism Launcher"},
		{"Plain Craft Launcher 2", ""},
		{"unrelated application", ""},
	}
	for _, test := range cases {
		if got := supportedLauncherName(test.input); got != test.want {
			t.Errorf("supportedLauncherName(%q) = %q, want %q", test.input, got, test.want)
		}
	}
	if runtime.GOOS == "windows" {
		if got := supportedLauncherName("Plain Craft Launcher 2"); got != "PCL" {
			t.Fatalf("Windows PCL detection = %q, want PCL", got)
		}
	}
}

func TestNormalizeLaunchersDeduplicatesAndChecksPath(t *testing.T) {
	root := t.TempDir()
	executable := filepath.Join(root, "hmcl")
	if err := os.WriteFile(executable, []byte("test"), 0600); err != nil {
		t.Fatal(err)
	}
	got := normalizeLaunchers([]Launcher{
		{Name: "HMCL", Path: executable},
		{Name: "HMCL", Path: executable},
		{Name: "Prism Launcher", Path: filepath.Join(root, "missing")},
	})
	if len(got) != 1 || got[0].Name != "HMCL" {
		t.Fatalf("unexpected normalized launchers: %#v", got)
	}
}
