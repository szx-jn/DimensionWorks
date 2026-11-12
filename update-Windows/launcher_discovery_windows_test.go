//go:build windows

package main

import (
	"os"
	"path/filepath"
	"testing"
)

func TestParseWindowsLauncherRegistry(t *testing.T) {
	executable := filepath.Join(t.TempDir(), "HMCL.exe")
	if err := os.WriteFile(executable, []byte("test"), 0600); err != nil {
		t.Fatal(err)
	}
	output := "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\HMCL\r\n" +
		"    DisplayName    REG_SZ    HMCL\r\n" +
		"    DisplayIcon    REG_SZ    \"" + executable + "\",0\r\n"
	found := parseWindowsLauncherRegistry(output)
	if len(found) != 1 || found[0].Name != "HMCL" || found[0].Path != executable {
		t.Fatalf("unexpected registry parse result: %#v", found)
	}
}

func TestRegistryExecutableParsesQuotedCommand(t *testing.T) {
	executable := filepath.Join(t.TempDir(), "Prism Launcher.exe")
	if err := os.WriteFile(executable, []byte("test"), 0600); err != nil {
		t.Fatal(err)
	}
	got := registryExecutable("\"" + executable + "\" --some-flag")
	if got != executable {
		t.Fatalf("registryExecutable() = %q, want %q", got, executable)
	}
}

func TestParseWindowsRegisteredApplicationOpenCommand(t *testing.T) {
	executable := filepath.Join(t.TempDir(), "HMCL.exe")
	if err := os.WriteFile(executable, []byte("test"), 0600); err != nil {
		t.Fatal(err)
	}
	output := "HKEY_CURRENT_USER\\Software\\Classes\\Applications\\HMCL.exe\\shell\\open\\command\\r\\n" +
		"    (默认)    REG_SZ    \"" + executable + "\" \"%1\"\\r\\n"
	found := parseWindowsLauncherRegistry(output)
	if len(found) != 1 || found[0].Name != "HMCL" || found[0].Path != executable {
		t.Fatalf("unexpected registered application result: %#v", found)
	}
}
