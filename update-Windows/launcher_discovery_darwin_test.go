//go:build darwin

package main

import (
	"os"
	"path/filepath"
	"testing"
)

func TestParseLaunchServicesApps(t *testing.T) {
	app := filepath.Join(t.TempDir(), "Prism Launcher.app")
	if err := os.Mkdir(app, 0755); err != nil {
		t.Fatal(err)
	}
	output := "path: " + app + " (0x1234)\npath: /Applications/Unrelated.app (0x5678)\n"
	found := parseLaunchServicesApps(output)
	if len(found) != 1 || found[0].Name != "Prism Launcher" || found[0].Path != app {
		t.Fatalf("unexpected LaunchServices apps: %#v", found)
	}
}
