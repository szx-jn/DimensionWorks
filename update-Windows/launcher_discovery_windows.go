//go:build windows

package main

import (
	"encoding/binary"
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"strings"
	"syscall"
	"unicode/utf16"
)

var windowsLauncherNames = map[string][]string{
	"HMCL":           {"HMCL.exe"},
	"PCL":            {"Plain Craft Launcher.exe", "Plain Craft Launcher 2.exe", "PCL.exe", "PCL2.exe"},
	"Prism Launcher": {"prismlauncher.exe", "PrismLauncher.exe"},
}

var quotedExecutable = regexp.MustCompile(`^\s*"([^"]+)"`)

func discoverRegisteredLaunchers() ([]Launcher, error) {
	roots := []string{
		`HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall`,
		`HKLM\Software\Microsoft\Windows\CurrentVersion\Uninstall`,
		`HKLM\Software\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall`,
		`HKCU\Software\Microsoft\Windows\CurrentVersion\App Paths`,
		`HKLM\Software\Microsoft\Windows\CurrentVersion\App Paths`,
		`HKLM\Software\WOW6432Node\Microsoft\Windows\CurrentVersion\App Paths`,
		`HKCU\Software\Classes\Applications`,
		`HKLM\Software\Classes\Applications`,
	}
	var found []Launcher
	for _, root := range roots {
		output, err := exec.Command("reg.exe", "query", root, "/s").Output()
		if err != nil {
			continue
		}
		found = append(found, parseWindowsLauncherRegistry(decodeWindowsRegistryOutput(output))...)
	}
	return normalizeLaunchers(found), nil
}

func decodeWindowsRegistryOutput(data []byte) string {
	if len(data) >= 2 && data[0] == 0xff && data[1] == 0xfe {
		data = data[2:]
	}
	if len(data) >= 2 && len(data)%2 == 0 {
		zeroBytes := 0
		for i := 1; i < len(data); i += 2 {
			if data[i] == 0 {
				zeroBytes++
			}
		}
		if zeroBytes > len(data)/8 {
			words := make([]uint16, len(data)/2)
			for i := range words {
				words[i] = binary.LittleEndian.Uint16(data[i*2 : i*2+2])
			}
			return string(utf16.Decode(words))
		}
	}
	return string(data)
}

func parseWindowsLauncherRegistry(output string) []Launcher {
	type record struct {
		key    string
		values map[string]string
	}
	var records []record
	var current *record
	for _, line := range strings.Split(output, "\n") {
		line = strings.TrimSpace(strings.TrimSuffix(line, "\r"))
		if line == "" {
			continue
		}
		if strings.HasPrefix(strings.ToUpper(line), "HKEY_") {
			records = append(records, record{key: line, values: make(map[string]string)})
			current = &records[len(records)-1]
			continue
		}
		if current == nil {
			continue
		}
		fields := strings.Fields(line)
		if len(fields) < 3 {
			continue
		}
		valueName := strings.ToLower(fields[0])
		if valueName == "(default)" || valueName == "(默认)" {
			valueName = "(default)"
		}
		valueStart := strings.Index(line, fields[1]) + len(fields[1])
		current.values[valueName] = strings.TrimSpace(line[valueStart:])
	}

	var found []Launcher
	for _, item := range records {
		name := item.values["displayname"]
		if name == "" {
			base := filepath.Base(item.key)
			if strings.EqualFold(base, "command") {
				parts := strings.Split(item.key, "\\")
				for i := len(parts) - 1; i >= 0; i-- {
					if strings.HasSuffix(strings.ToLower(parts[i]), ".exe") {
						base = parts[i]
						break
					}
				}
			}
			name = supportedLauncherName(base)
		}
		canonical := supportedLauncherName(name)
		if canonical == "" {
			continue
		}

		executable := registryExecutable(item.values["displayicon"])
		if executable == "" {
			executable = registryExecutable(item.values["(default)"])
		}
		if executable == "" {
			executable = registryExecutable(item.values["open"])
		}
		if executable == "" {
			location := strings.Trim(strings.TrimSpace(item.values["installlocation"]), "\"")
			for _, filename := range windowsLauncherNames[canonical] {
				candidate := filepath.Join(location, filename)
				if info, err := os.Stat(candidate); err == nil && !info.IsDir() {
					executable = candidate
					break
				}
			}
		}
		if executable != "" {
			found = append(found, Launcher{Name: canonical, Path: executable})
		}
	}
	return found
}

func registryExecutable(value string) string {
	value = strings.TrimSpace(value)
	if value == "" {
		return ""
	}
	if match := quotedExecutable.FindStringSubmatch(value); len(match) == 2 {
		value = match[1]
	} else {
		if comma := strings.LastIndex(value, ","); comma >= 0 {
			value = value[:comma]
		}
		if index := strings.Index(strings.ToLower(value), ".exe"); index >= 0 {
			value = value[:index+4]
		}
	}
	value = strings.Trim(value, "\"")
	if !strings.EqualFold(filepath.Ext(value), ".exe") {
		return ""
	}
	if info, err := os.Stat(value); err == nil && !info.IsDir() {
		return value
	}
	return ""
}

func openMRPackWithLauncher(launcher Launcher, packagePath string) error {
	return startLauncherCommand(launcherCommand(launcher, packagePath))
}

func resolveCustomLauncher(path string) (Launcher, error) {
	if !strings.EqualFold(filepath.Ext(path), ".lnk") {
		return customLauncherFile(path)
	}
	if _, err := os.Stat(path); err != nil {
		return Launcher{}, err
	}
	script := `[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false); $ErrorActionPreference='Stop'; $s=(New-Object -ComObject WScript.Shell).CreateShortcut('` + strings.ReplaceAll(path, "'", "''") + `'); @{Path=$s.TargetPath; Arguments=$s.Arguments; WorkingDir=$s.WorkingDirectory} | ConvertTo-Json -Compress`
	data, err := exec.Command("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script).CombinedOutput()
	if err != nil {
		return Launcher{}, fmt.Errorf("解析启动器快捷方式失败：%w：%s", err, strings.TrimSpace(string(data)))
	}
	var shortcut Launcher
	if err := json.Unmarshal(data, &shortcut); err != nil {
		return Launcher{}, fmt.Errorf("解析快捷方式目标失败：%w", err)
	}
	launcher, err := customLauncherFile(shortcut.Path)
	if err != nil {
		return Launcher{}, fmt.Errorf("快捷方式目标无效：%w", err)
	}
	launcher.Arguments = shortcut.Arguments
	if shortcut.WorkingDir != "" {
		launcher.WorkingDir = shortcut.WorkingDir
	}
	return launcher, nil
}

func launcherCommand(launcher Launcher, packagePath string) *exec.Cmd {
	cmd := exec.Command(launcher.Path)
	if strings.EqualFold(filepath.Ext(launcher.Path), ".jar") {
		cmd = exec.Command("java", "-jar", launcher.Path)
	}
	if launcher.Name == "Prism Launcher" {
		cmd.Args = append(cmd.Args, "--import", packagePath)
	}
	if launcher.Arguments != "" {
		// Preserve the shortcut's arguments without interpreting them in a shell.
		prefix := syscall.EscapeArg(cmd.Args[0])
		for _, arg := range cmd.Args[1:] {
			prefix += " " + syscall.EscapeArg(arg)
		}
		cmd.SysProcAttr = &syscall.SysProcAttr{CmdLine: prefix + " " + launcher.Arguments}
	}
	cmd.Dir = launcher.WorkingDir
	return cmd
}
