//go:build windows

package main

import (
	"encoding/binary"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"testing"
	"unicode/utf16"
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
	output := "HKEY_CURRENT_USER\\Software\\Classes\\Applications\\HMCL.exe\\shell\\open\\command\r\n" +
		"    (默认)    REG_SZ    \"" + executable + "\" \"%1\"\r\n"
	found := parseWindowsLauncherRegistry(output)
	if len(found) != 1 || found[0].Name != "HMCL" || found[0].Path != executable {
		t.Fatalf("unexpected registered application result: %#v", found)
	}
}

func TestDecodeWindowsRegistryOutputUTF16(t *testing.T) {
	text := "HKEY_CURRENT_USER\\Software\\Applications\\HMCL"
	encoded := utf16.Encode([]rune(text))
	data := make([]byte, len(encoded)*2)
	for i, value := range encoded {
		binary.LittleEndian.PutUint16(data[i*2:], value)
	}
	if got := decodeWindowsRegistryOutput(data); got != text {
		t.Fatalf("decoded registry output = %q, want %q", got, text)
	}
}

func TestCustomWindowsShortcutPreservesTargetArgumentsAndDirectory(t *testing.T) {
	root := t.TempDir()
	target := filepath.Join(root, "HMCL.exe")
	link := filepath.Join(root, "launcher.lnk")
	if err := os.WriteFile(target, []byte("fixture"), 0600); err != nil {
		t.Fatal(err)
	}
	quote := func(s string) string { return "'" + strings.ReplaceAll(s, "'", "''") + "'" }
	script := "$s=(New-Object -ComObject WScript.Shell).CreateShortcut(" + quote(link) + "); $s.TargetPath=" + quote(target) + "; $s.Arguments='--test 123'; $s.WorkingDirectory=" + quote(root) + "; $s.Save()"
	if out, err := exec.Command("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script).CombinedOutput(); err != nil {
		t.Fatalf("%v: %s", err, out)
	}
	got, err := customLauncherFromInput("\"" + link + "\"")
	if err != nil || got.Path != target || got.Arguments != "--test 123" || got.WorkingDir != root || got.Name != "HMCL" {
		t.Fatalf("%#v, %v", got, err)
	}
	cmd := launcherCommand(got, "delta.mrpack")
	if cmd.Dir != root || !strings.HasSuffix(cmd.SysProcAttr.CmdLine, " --test 123") {
		t.Fatalf("shortcut command lost arguments: %#v", cmd)
	}
}
