package main

import (
	"crypto/sha512"
	"encoding/hex"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"testing"
)

func TestValidateLauncherInstanceRoot(t *testing.T) {
	base := t.TempDir()
	instance := filepath.Join(base, "launcher-instance")
	if err := os.MkdirAll(filepath.Join(instance, "mods"), 0755); err != nil {
		t.Fatal(err)
	}
	updater := filepath.Join(base, "DimensionWorks")
	if err := os.MkdirAll(updater, 0755); err != nil {
		t.Fatal(err)
	}
	got, err := validateLauncherInstanceRoot(instance, updater)
	if err != nil {
		t.Fatal(err)
	}
	gotInfo, err := os.Stat(got)
	if err != nil {
		t.Fatal(err)
	}
	instanceInfo, err := os.Stat(instance)
	if err != nil {
		t.Fatal(err)
	}
	if !os.SameFile(gotInfo, instanceInfo) {
		t.Fatalf("validated path %q does not identify selected instance %q", got, instance)
	}
}

func TestValidateLauncherInstanceRootAcceptsDraggedDirectory(t *testing.T) {
	base := t.TempDir()
	instance := filepath.Join(base, "player's 启动器 instance")
	updater := filepath.Join(base, "DimensionWorks")
	for _, path := range []string{filepath.Join(instance, "mods"), updater} {
		if err := os.MkdirAll(path, 0755); err != nil {
			t.Fatal(err)
		}
	}
	inputs := []string{instance, `"` + instance + `"`}
	if runtime.GOOS != "windows" {
		inputs = append(inputs, "'"+strings.ReplaceAll(instance, "'", `'\''`)+"'",
			strings.NewReplacer("'", `\'`, " ", `\ `).Replace(instance))
	}
	for _, input := range inputs {
		got, err := validateLauncherInstanceRoot(input, updater)
		if err != nil {
			t.Fatalf("%q: %v", input, err)
		}
		gotInfo, err := os.Stat(got)
		if err != nil {
			t.Fatal(err)
		}
		instanceInfo, err := os.Stat(instance)
		if err != nil || !os.SameFile(gotInfo, instanceInfo) {
			t.Fatalf("%q resolved to the wrong instance %q: %v", input, got, err)
		}
	}
}

func TestValidateLauncherInstanceRootRejectsUpdaterParent(t *testing.T) {
	base := t.TempDir()
	if err := os.MkdirAll(filepath.Join(base, "mods"), 0755); err != nil {
		t.Fatal(err)
	}
	updater := filepath.Join(base, "DimensionWorks")
	if err := os.MkdirAll(updater, 0755); err != nil {
		t.Fatal(err)
	}
	if _, err := validateLauncherInstanceRoot(base, updater); err == nil || !strings.Contains(err.Error(), "包含 DimensionWorks") {
		t.Fatalf("expected updater parent to be rejected, got %v", err)
	}
}

func TestCopyVerifiedLauncherEntry(t *testing.T) {
	root := t.TempDir()
	if err := os.MkdirAll(filepath.Join(root, "mods"), 0755); err != nil {
		t.Fatal(err)
	}
	data := []byte("launcher downloaded mod")
	source := filepath.Join(root, "mods", "example.jar")
	if err := os.WriteFile(source, data, 0644); err != nil {
		t.Fatal(err)
	}
	sum := sha512.Sum512(data)
	entry := Entry{
		Action:    "remote",
		Path:      "mods/example.jar",
		Algorithm: "sha512",
		Value:     hex.EncodeToString(sum[:]),
	}
	destination := filepath.Join(t.TempDir(), "stage", "mods", "example.jar")
	if err := copyVerifiedLauncherEntry(root, entry, destination); err != nil {
		t.Fatal(err)
	}
	got, err := os.ReadFile(destination)
	if err != nil {
		t.Fatal(err)
	}
	if string(got) != string(data) {
		t.Fatalf("copied content = %q, want %q", got, data)
	}
}

func TestCopyVerifiedLauncherEntryRejectsWrongDigest(t *testing.T) {
	root := t.TempDir()
	if err := os.MkdirAll(filepath.Join(root, "mods"), 0755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(root, "mods", "example.jar"), []byte("wrong"), 0644); err != nil {
		t.Fatal(err)
	}
	entry := Entry{Action: "remote", Path: "mods/example.jar", Algorithm: "sha512", Value: strings.Repeat("0", 128)}
	destination := filepath.Join(t.TempDir(), "stage", "example.jar")
	if err := copyVerifiedLauncherEntry(root, entry, destination); err == nil {
		t.Fatal("expected wrong launcher file digest to fail")
	}
	if _, err := os.Stat(destination); !os.IsNotExist(err) {
		t.Fatalf("invalid staged file should be removed, got %v", err)
	}
}

func TestRemoveLauncherInstance(t *testing.T) {
	base := t.TempDir()
	instance := filepath.Join(base, "temporary-instance")
	if err := os.MkdirAll(filepath.Join(instance, "mods"), 0755); err != nil {
		t.Fatal(err)
	}
	updater := filepath.Join(base, "DimensionWorks")
	if err := os.MkdirAll(updater, 0755); err != nil {
		t.Fatal(err)
	}
	if err := removeLauncherInstance(instance, updater); err != nil {
		t.Fatal(err)
	}
	if _, err := os.Stat(instance); !os.IsNotExist(err) {
		t.Fatalf("temporary launcher instance was not removed: %v", err)
	}
}

func TestLauncherEntrySourceRejectsMissingFile(t *testing.T) {
	root := t.TempDir()
	if err := os.MkdirAll(filepath.Join(root, "mods"), 0755); err != nil {
		t.Fatal(err)
	}
	entry := Entry{Action: "remote", Path: "mods/missing.jar", Algorithm: "sha512", Value: strings.Repeat("0", 128)}
	if _, _, err := launcherEntrySource(root, entry); err == nil || !strings.Contains(err.Error(), "缺少文件") {
		t.Fatalf("expected missing launcher file error, got %v", err)
	}
}

func TestRemoveLauncherInstanceRejectsUpdaterParent(t *testing.T) {
	base := t.TempDir()
	if err := os.MkdirAll(filepath.Join(base, "mods"), 0755); err != nil {
		t.Fatal(err)
	}
	updater := filepath.Join(base, "DimensionWorks")
	if err := os.MkdirAll(updater, 0755); err != nil {
		t.Fatal(err)
	}
	if err := removeLauncherInstance(base, updater); err == nil {
		t.Fatal("must not remove a directory containing the active updater instance")
	}
	if _, err := os.Stat(updater); err != nil {
		t.Fatalf("active updater directory was damaged: %v", err)
	}
}

func TestValidateLauncherInstanceRootAcceptsShaderpackOnly(t *testing.T) {
	base := t.TempDir()
	instance := filepath.Join(base, "launcher-instance")
	if err := os.MkdirAll(filepath.Join(instance, "shaderpacks"), 0755); err != nil {
		t.Fatal(err)
	}
	updater := filepath.Join(base, "DimensionWorks")
	if err := os.MkdirAll(updater, 0755); err != nil {
		t.Fatal(err)
	}
	if _, err := validateLauncherInstanceRoot(instance, updater); err != nil {
		t.Fatalf("shaderpack-only launcher instance should be accepted: %v", err)
	}
}
