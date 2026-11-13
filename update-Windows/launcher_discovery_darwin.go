//go:build darwin

package main

import (
	"os/exec"
	"path/filepath"
	"strings"
)

const launchServicesRegistry = "/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister"

func discoverRegisteredLaunchers() ([]Launcher, error) {
	output, err := exec.Command(launchServicesRegistry, "-dump").Output()
	if err != nil {
		return nil, err
	}
	return parseLaunchServicesApps(string(output)), nil
}

func parseLaunchServicesApps(output string) []Launcher {
	var found []Launcher
	for _, line := range strings.Split(output, "\n") {
		line = strings.TrimSpace(line)
		if !strings.HasPrefix(strings.ToLower(line), "path:") {
			continue
		}
		path := strings.TrimSpace(line[len("path:"):])
		if end := strings.Index(strings.ToLower(path), ".app"); end >= 0 {
			path = path[:end+4]
		}
		if path == "" {
			continue
		}
		name := supportedLauncherName(filepath.Base(path))
		if name != "" {
			found = append(found, Launcher{Name: name, Path: path})
		}
	}
	return normalizeLaunchers(found)
}

func openMRPackWithLauncher(launcher Launcher, packagePath string) error {
	if launcher.Name == "Prism Launcher" {
		return startLauncher("open", "-a", launcher.Path, "--args", "--import", packagePath)
	}
	return startLauncher("open", "-a", launcher.Path, packagePath)
}
