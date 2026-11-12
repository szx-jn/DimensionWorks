package main

import (
	"errors"
	"fmt"
	"os"
	"os/exec"
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
	case runtime.GOOS == "windows" && (name == "pcl" || strings.HasPrefix(name, "pcl 2") || strings.Contains(name, "pcl2") || strings.Contains(name, "plain craft launcher")):
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
		if runtime.GOOS == "windows" && !strings.HasSuffix(strings.ToLower(item.Path), ".exe") {
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

	var selected Launcher
	if len(launchers) == 1 {
		selected = launchers[0]
		fmt.Printf("[启动器] 只检测到 %s，直接使用。\n", selected.Name)
	} else {
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
		selected = launchers[choice-1]
	}
	if err := openMRPackWithLauncher(selected, packagePath); err != nil {
		return fmt.Errorf("无法通过 %s 打开 MRPack：%w", selected.Name, err)
	}
	if selected.Name == "Prism Launcher" {
		fmt.Println("[启动器] 已请求 Prism Launcher 导入 MRPack。请在启动器中完成下载。")
	} else {
		fmt.Printf("[启动器] 已启动 %s。请在启动器的导入界面选择文件：%s\n", selected.Name, packagePath)
	}
	return nil
}

func startLauncher(executable string, args ...string) error {
	cmd := exec.Command(executable, args...)
	if err := cmd.Start(); err != nil {
		return err
	}
	return nil
}

