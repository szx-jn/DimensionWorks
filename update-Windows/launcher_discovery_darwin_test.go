//go:build darwin

package main

import (
	"os"
	"path/filepath"
	"reflect"
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

func TestCustomLauncherCommands(t *testing.T) {
	for _, test := range []struct {
		launcher Launcher
		want     []string
	}{
		{Launcher{Name: "Prism Launcher", Path: "/Applications/Prism Launcher.app"}, []string{"open", "-a", "/Applications/Prism Launcher.app", "--args", "--import", "/tmp/delta.mrpack"}},
		{Launcher{Name: "HMCL", Path: "/Applications/HMCL.app"}, []string{"open", "-a", "/Applications/HMCL.app"}},
		{Launcher{Name: "HMCL", Path: "/tmp/HMCL.jar", WorkingDir: "/tmp"}, []string{"java", "-jar", "/tmp/HMCL.jar"}},
	} {
		cmd := launcherCommand(test.launcher, "/tmp/delta.mrpack")
		if !reflect.DeepEqual(cmd.Args, test.want) || cmd.Dir != test.launcher.WorkingDir {
			t.Fatalf("unexpected command: %#v", cmd)
		}
	}
}
