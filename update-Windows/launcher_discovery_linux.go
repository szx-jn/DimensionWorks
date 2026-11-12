//go:build linux

package main

import (
	"os"
	"net/url"
	"path/filepath"
	"strings"
)

func discoverRegisteredLaunchers() ([]Launcher, error) {
	dirs := []string{}
	if home, err := os.UserHomeDir(); err == nil {
		dirs = append(dirs, filepath.Join(home, ".local", "share", "applications"))
	}
	if dataHome := os.Getenv("XDG_DATA_HOME"); dataHome != "" {
		dirs = append(dirs, filepath.Join(dataHome, "applications"))
	}
	dataDirs := os.Getenv("XDG_DATA_DIRS")
	if dataDirs == "" {
		dataDirs = "/usr/local/share:/usr/share"
	}
	for _, dir := range filepath.SplitList(strings.ReplaceAll(dataDirs, ":", string(os.PathListSeparator))) {
		dirs = append(dirs, filepath.Join(dir, "applications"))
	}
	var found []Launcher
	seen := make(map[string]bool)
	for _, dir := range dirs {
		entries, err := os.ReadDir(dir)
		if err != nil {
			continue
		}
		for _, entry := range entries {
			if entry.IsDir() || !strings.HasSuffix(strings.ToLower(entry.Name()), ".desktop") {
				continue
			}
			path := filepath.Join(dir, entry.Name())
			data, err := os.ReadFile(path)
			if err != nil {
				continue
			}
			name := ""
			inDesktopEntry := false
			for _, line := range strings.Split(string(data), "\n") {
				line = strings.TrimSpace(line)
				if strings.HasPrefix(line, "[") {
					inDesktopEntry = line == "[Desktop Entry]"
					continue
				}
				if inDesktopEntry && strings.HasPrefix(line, "Name=") {
					name = strings.TrimSpace(strings.TrimPrefix(line, "Name="))
					break
				}
			}
			canonical := supportedLauncherName(name)
			if canonical == "" || seen[canonical] {
				continue
			}
			seen[canonical] = true
			found = append(found, Launcher{Name: canonical, Path: path, DesktopFile: path})
		}
	}
	return normalizeLaunchers(found), nil
}

func openMRPackWithLauncher(launcher Launcher, packagePath string) error {
	// gio launch invokes the registered desktop entry and supplies the file URI.
	absolute, err := filepath.Abs(packagePath)
	if err != nil {
		return err
	}
	fileURI := (&url.URL{Scheme: "file", Path: filepath.ToSlash(absolute)}).String()
	return startLauncher("gio", "launch", launcher.DesktopFile, fileURI)
}
