package main

import (
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"testing"
)

func TestDraggedPath(t *testing.T) {
	path := filepath.Join(t.TempDir(), "启动器 with space.jar")
	if err := os.WriteFile(path, []byte("fixture"), 0600); err != nil {
		t.Fatal(err)
	}
	inputs := []string{path, `"` + path + `"`}
	if runtime.GOOS != "windows" {
		inputs = append(inputs, "'"+path+"'", strings.ReplaceAll(path, " ", `\ `))
	}
	for _, input := range inputs {
		got, err := draggedPath(input)
		if err != nil || got != path {
			t.Fatalf("%q => %q, %v", input, got, err)
		}
	}
}

func TestChooseLauncherCustomIsLastPlusOne(t *testing.T) {
	if runtime.GOOS == "linux" {
		t.Skip("Linux has no client launcher support")
	}
	path := filepath.Join(t.TempDir(), "HMCL launcher.jar")
	if err := os.WriteFile(path, []byte("fixture"), 0600); err != nil {
		t.Fatal(err)
	}
	for _, count := range []int{0, 1, 3} {
		items := make([]Launcher, count)
		lines := []string{fmt.Sprint(count + 1), `"` + path + `"`}
		read := func() (string, error) { line := lines[0]; lines = lines[1:]; return line, nil }
		got, err := chooseLauncher(items, read)
		if err != nil || got.Path != path || got.Name != "HMCL" {
			t.Fatalf("count=%d: %#v, %v", count, got, err)
		}
	}
}

func TestChooseLauncherRegisteredAndCancel(t *testing.T) {
	items := []Launcher{{Name: "HMCL", Path: "registered"}}
	got, err := chooseLauncher(items, func() (string, error) { return "1", nil })
	if err != nil || got.Path != "registered" {
		t.Fatalf("%#v, %v", got, err)
	}
	for _, input := range []string{"0", "3", "invalid"} {
		if _, err := chooseLauncher(items, func() (string, error) { return input, nil }); err == nil {
			t.Fatalf("accepted %s", input)
		}
	}
}
