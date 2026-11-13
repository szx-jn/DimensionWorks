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
	cmd := launcherCommand(launcher, packagePath)
	if strings.EqualFold(filepath.Ext(launcher.Path), ".app") {
		return runLauncherDelegate(cmd)
	}
	return startLauncherCommand(cmd)
}

func resolveCustomLauncher(path string) (Launcher, error) { return customLauncherFile(path) }

func launcherCommand(launcher Launcher, packagePath string) *exec.Cmd {
	var cmd *exec.Cmd
	switch strings.ToLower(filepath.Ext(launcher.Path)) {
	case ".app":
		args := []string{"-a", launcher.Path}
		if launcher.Name == "Prism Launcher" {
			args = append(args, "--args", "--import", packagePath)
		}
		cmd = exec.Command("open", args...)
	case ".jar":
		cmd = exec.Command("java", "-jar", launcher.Path)
	default:
		args := []string{}
		if launcher.Name == "Prism Launcher" {
			args = append(args, "--import", packagePath)
		}
		cmd = exec.Command(launcher.Path, args...)
	}
	cmd.Dir = launcher.WorkingDir
	return cmd
}
