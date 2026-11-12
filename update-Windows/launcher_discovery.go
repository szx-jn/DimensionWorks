package main

import (
	"errors"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"sort"
	"strconv"
	"strings"
)

type Launcher struct {
	Name        string
	Path        string
	DesktopFile string
}

func supportedLauncherName(value string) string {
	name := strings.ToLower(strings.TrimSpace(value))
	switch {
	case strings.Contains(name, "prism launcher"), strings.Contains(name, "prismlauncher"):
		return "Prism Launcher"
	case strings.Contains(name, "hmcl"), strings.Contains(name, "hello minecraft"):
		return "HMCL"
	case runtime.GOOS == "windows" && (name == "pcl" || strings.Contains(name, "pcl2") || strings.Contains(name, "plain craft launcher")):
		return "PCL"
	default:
		return ""
	}
}

func normalizeLaunchers(items []Launcher) []Launcher {
	byName := make(map[string]Launcher)
	for _, item := range items {
		if item.Name == "" || item.Path == "" {
			continue
		}
		info, err := os.Stat(item.Path)
		if err != nil || (info.IsDir() && !(runtime.GOOS == "darwin" && strings.HasSuffix(strings.ToLower(item.Path), ".app"))) {
			continue
		}
		if _, exists := byName[item.Name]; !exists {
			byName[item.Name] = item
		}
	}
	result := make([]Launcher, 0, len(byName))
	for _, item := range byName {
		result = append(result, item)
	}
	sort.Slice(result, func(i, j int) bool { return result[i].Name < result[j].Name })
	return result
}

func selectAndOpenMRPack(packagePath string) error {
	launchers, err := discoverRegisteredLaunchers()
	if err != nil {
		return fmt.Errorf("扫描已注册启动器失败：%w", err)
	}
	if len(launchers) == 0 {
		fmt.Println("[启动器] 没有检测到已注册的受支持启动器，请手动打开 MRPack：", packagePath)
		return nil
	}

	fmt.Println()
	fmt.Println("检测到以下已注册的 Minecraft 启动器：")
	for i, launcher := range launchers {
		fmt.Printf("[%d] %s\n", i+1, launcher.Name)
	}
	fmt.Println("[0] 取消")
	fmt.Print("请选择用于导入 MRPack 的启动器，输入编号：")
	line, readErr := readConsoleLine()
	if readErr != nil && strings.TrimSpace(line) == "" {
		return fmt.Errorf("读取启动器选择失败：%w", readErr)
	}
	choice, err := strconv.Atoi(strings.TrimSpace(line))
	if err != nil || choice < 0 || choice > len(launchers) {
		return errors.New("启动器编号无效")
	}
	if choice == 0 {
		return errors.New("用户取消了启动器选择")
	}
	selected := launchers[choice-1]
	if err := openMRPackWithLauncher(selected, packagePath); err != nil {
		return fmt.Errorf("无法通过 %s 打开 MRPack：%w", selected.Name, err)
	}
	fmt.Printf("[启动器] 已请求 %s 打开 MRPack。请在启动器中完成导入和下载。\n", selected.Name)
	return nil
}

func startLauncher(executable string, args ...string) error {
	cmd := exec.Command(executable, args...)
	if err := cmd.Start(); err != nil {
		return err
	}
	return nil
}

func launcherExecutable(path string) string {
	path = strings.TrimSpace(strings.Trim(path, "\"`"))
	if path == "" {
		return ""
	}
	abs, err := filepath.Abs(path)
	if err != nil {
		return ""
	}
	return abs
}
