//go:build linux

package main

import (
	"os"
	"path/filepath"
	"testing"
)

func TestDiscoverRegisteredLaunchersFromXDGDesktopFiles(t *testing.T) {
	home := t.TempDir()
	t.Setenv("HOME", home)
	t.Setenv("XDG_DATA_HOME", filepath.Join(home, "xdg"))
	t.Setenv("XDG_DATA_DIRS", filepath.Join(home, "system"))
	apps := filepath.Join(home, ".local", "share", "applications")
	if err := os.MkdirAll(apps, 0755); err != nil {
		t.Fatal(err)
	}
	desktop := "[Desktop Entry]\nName=Prism Launcher\nExec=prismlauncher %U\nType=Application\n"
	if err := os.WriteFile(filepath.Join(apps, "org.prismlauncher.PrismLauncher.desktop"), []byte(desktop), 0644); err != nil {
		t.Fatal(err)
	}
	found, err := discoverRegisteredLaunchers()
	if err != nil {
		t.Fatal(err)
	}
	if len(found) != 1 || found[0].Name != "Prism Launcher" {
		t.Fatalf("unexpected registered launchers: %#v", found)
	}
}
