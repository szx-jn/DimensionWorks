//go:build darwin

package main

import (
	"os/exec"
	"path/filepath"
	"strings"
)

func discoverRegisteredLaunchers() ([]Launcher, error) {
	output, err := exec.Command("mdfind", "kMDItemContentType == 'com.apple.application-bundle'").Output()
	if err != nil {
		return nil, err
	}
	var found []Launcher
	for _, path := range strings.Split(string(output), "\n") {
		path = strings.TrimSpace(path)
		if path == "" {
			continue
		}
		name := supportedLauncherName(filepath.Base(path))
		if name != "" {
			found = append(found, Launcher{Name: name, Path: path})
		}
	}
	return normalizeLaunchers(found), nil
}

func openMRPackWithLauncher(launcher Launcher, packagePath string) error {
	if launcher.Name == "Prism Launcher" {
		return startLauncher("open", "-a", launcher.Path, "--args", "--import", packagePath)
	}
	// LaunchServices sends the selected document to the chosen application.
	return startLauncher("open", "-a", launcher.Path, packagePath)
}
