//go:build windows

package main

import (
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"strings"
)

var windowsLauncherNames = map[string][]string{
	"HMCL":          {"HMCL.exe", "HMCL"},
	"PCL":           {"Plain Craft Launcher.exe", "PCL.exe", "PCL2.exe"},
	"Prism Launcher": {"prismlauncher.exe", "PrismLauncher.exe"},
}

var quotedExecutable = regexp.MustCompile(`^\s*"([^"]+)"`)

func discoverRegisteredLaunchers() ([]Launcher, error) {
	roots := []string{
		`HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall`,
		`HKLM\Software\Microsoft\Windows\CurrentVersion\Uninstall`,
		`HKLM\Software\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall`,
		`HKCU\Software\Classes\Applications`,
		`HKLM\Software\Classes\Applications`,
	}
	var found []Launcher
	for _, root := range roots {
		output, err := exec.Command("reg.exe", "query", root, "/s").Output()
		if err != nil {
			// A missing registry branch is normal; other branches are still useful.
			continue
		}
		found = append(found, parseWindowsLauncherRegistry(string(output))...)
	}
	return normalizeLaunchers(found), nil
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
		if valueName == "(default)" {
			valueName = "(default)"
		}
		current.values[valueName] = strings.TrimSpace(strings.TrimPrefix(line, fields[0]+" "+fields[1]))
	}
	var found []Launcher
	for _, item := range records {
		name := item.values["displayname"]
		executable := registryExecutable(item.values["displayicon"])
		if executable == "" {
			executable = registryExecutable(item.values["(default)"])
		}
		if executable == "" {
			executable = registryExecutable(item.values["open"])
		}
		if name == "" {
			base := strings.ToLower(filepath.Base(item.key))
			for candidate, names := range windowsLauncherNames {
				for _, filename := range names {
					if base == strings.ToLower(filename) {
						name = candidate
						break
					}
				}
			}
		}
		canonical := supportedLauncherName(name)
		if canonical == "" {
			continue
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
		value = strings.TrimSpace(value)
		if index := strings.Index(strings.ToLower(value), ".exe"); index >= 0 {
			value = value[:index+4]
		}
	}
	value = strings.Trim(value, "\"")
	if info, err := os.Stat(value); err == nil && !info.IsDir() {
		return value
	}
	return ""
}

func openMRPackWithLauncher(launcher Launcher, packagePath string) error {
	if launcher.Name == "Prism Launcher" {
		return startLauncher(launcher.Path, "--import", packagePath)
	}
	// HMCL and PCL support opening an import file from the application entry point.
	return startLauncher(launcher.Path, packagePath)
}
