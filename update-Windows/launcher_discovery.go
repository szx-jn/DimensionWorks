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
	Name       string
	Path       string
	Arguments  string
	WorkingDir string
}

func supportedLauncherName(value string) string {
	name := strings.ToLower(strings.TrimSpace(value))
	if runtime.GOOS == "windows" {
		name = strings.TrimSuffix(name, ".exe")
	}
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
	selected, err := chooseLauncher(launchers, readConsoleLine)
	if err != nil {
		return err
	}
	fmt.Printf("[启动器] 使用 %s：%s\n", selected.Name, selected.Path)
	fmt.Println("[MRPack] 本次差量包：", packagePath)
	if err := openMRPackWithLauncher(selected, packagePath); err != nil {
		return fmt.Errorf("无法通过 %s 打开 MRPack：%w", selected.Name, err)
	}
	if err := revealMRPack(packagePath); err != nil {
		fmt.Printf("[文件管理器] 无法展示差量包：%v\n请手动打开文件所在目录：%s\n", err, filepath.Dir(packagePath))
	} else {
		fmt.Println("[文件管理器] 已请求打开所在文件夹并选中差量 MRPack，可将该文件拖入启动器导入界面。")
	}
	if selected.Name == "Prism Launcher" {
		fmt.Println("[启动器] 已请求 Prism Launcher 导入 MRPack。请在启动器中完成下载。")
	} else {
		fmt.Printf("[启动器] 已启动 %s。请在启动器的导入界面选择文件：%s\n", selected.Name, packagePath)
	}
	return nil
}

func chooseLauncher(launchers []Launcher, readLine func() (string, error)) (Launcher, error) {
	fmt.Println()
	if len(launchers) == 0 {
		fmt.Println("没有检测到已注册的受支持启动器，请选择自定义并提供启动器文件。")
	} else {
		fmt.Println("检测到以下已注册的 Minecraft 启动器：")
	}
	for i, launcher := range launchers {
		fmt.Printf("[%d] %s\n", i+1, launcher.Name)
	}
	fmt.Printf("[%d] 自定义启动器\n", len(launchers)+1)
	fmt.Println("[0] 取消")
	fmt.Print("请选择用于导入 MRPack 的启动器，输入编号：")
	line, readErr := readLine()
	if readErr != nil && strings.TrimSpace(line) == "" {
		return Launcher{}, fmt.Errorf("读取启动器选择失败：%w", readErr)
	}
	choice, err := strconv.Atoi(strings.TrimSpace(line))
	if err != nil || choice < 0 || choice > len(launchers)+1 {
		return Launcher{}, errors.New("启动器编号无效")
	}
	if choice == 0 {
		return Launcher{}, errors.New("用户取消了启动器选择")
	}
	if choice <= len(launchers) {
		return launchers[choice-1], nil
	}
	switch runtime.GOOS {
	case "windows":
		fmt.Println("请将启动器快捷方式（.lnk）、.exe 或 .jar 文件拖入终端，然后按回车。")
	case "darwin":
		fmt.Println("请将启动器 .app、.jar 或可执行文件拖入终端，然后按回车。")
	}
	fmt.Print("启动器文件路径：")
	line, readErr = readLine()
	if readErr != nil && strings.TrimSpace(line) == "" {
		return Launcher{}, fmt.Errorf("读取自定义启动器路径失败：%w", readErr)
	}
	return customLauncherFromInput(line)
}

func customLauncherFromInput(input string) (Launcher, error) {
	path, err := draggedPath(input)
	if err != nil {
		return Launcher{}, err
	}
	path, err = filepath.Abs(path)
	if err != nil {
		return Launcher{}, err
	}
	launcher, err := resolveCustomLauncher(path)
	if err != nil {
		return Launcher{}, err
	}
	if name := supportedLauncherName(filepath.Base(launcher.Path) + " " + launcher.Arguments); name != "" && launcher.Name == "" {
		launcher.Name = name
	}
	if launcher.Name == "" {
		launcher.Name = filepath.Base(launcher.Path)
	}
	return launcher, nil
}

func startLauncherCommand(cmd *exec.Cmd) error {
	if err := cmd.Start(); err != nil {
		return err
	}
	go func() { _ = cmd.Wait() }()
	return nil
}

// open/gio exit after dispatching to the app, so report dispatch errors synchronously.
func runLauncherDelegate(cmd *exec.Cmd) error {
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("%w：%s", err, strings.TrimSpace(string(output)))
	}
	return nil
}
