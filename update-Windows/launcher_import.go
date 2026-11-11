package main

import (
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

func validateLauncherInstanceRoot(path, updaterRoot string) (string, error) {
	if strings.TrimSpace(path) == "" {
		return "", errors.New("启动器实例目录不能为空")
	}
	abs, err := filepath.Abs(path)
	if err != nil {
		return "", fmt.Errorf("启动器实例路径无效：%w", err)
	}
	info, err := os.Lstat(abs)
	if err != nil {
		return "", fmt.Errorf("无法访问启动器实例目录：%w", err)
	}
	if info.Mode()&os.ModeSymlink != 0 || !info.IsDir() {
		return "", errors.New("启动器实例路径必须是普通目录，不能是符号链接")
	}
	if filepath.Dir(abs) == abs {
		return "", errors.New("不能把磁盘根目录作为临时实例")
	}
	updaterAbs, err := filepath.Abs(updaterRoot)
	if err != nil {
		return "", err
	}
	if strings.EqualFold(filepath.VolumeName(abs), filepath.VolumeName(updaterAbs)) {
		relative, err := filepath.Rel(abs, updaterAbs)
		if err != nil {
			return "", err
		}
		if relative == "." || (relative != ".." && !strings.HasPrefix(relative, ".."+string(os.PathSeparator))) {
			return "", errors.New("不能选择包含 DimensionWorks 当前实例的目录")
		}
	}
	modsInfo, err := os.Lstat(filepath.Join(abs, "mods"))
	if err != nil || modsInfo.Mode()&os.ModeSymlink != 0 || !modsInfo.IsDir() {
		return "", errors.New("所选目录中没有有效的 mods 文件夹；请选择启动器新安装实例的游戏目录")
	}
	return abs, nil
}

func launcherEntrySource(root string, entry Entry) (string, int64, error) {
	source, exists, err := securePath(root, entry.Path)
	if err != nil {
		return "", 0, err
	}
	if !exists {
		return "", 0, fmt.Errorf("启动器实例缺少文件：%s；请确认启动器已完成整合包下载", entry.Path)
	}
	info, err := os.Lstat(source)
	if err != nil {
		return "", 0, err
	}
	if info.Mode()&os.ModeSymlink != 0 || !info.Mode().IsRegular() {
		return "", 0, fmt.Errorf("启动器下载文件不是普通文件：%s", entry.Path)
	}
	return source, info.Size(), nil
}

func copyVerifiedLauncherEntry(root string, entry Entry, destination string) error {
	source, _, err := launcherEntrySource(root, entry)
	if err != nil {
		return err
	}
	if err := copyFileLimited(source, destination, 0644, maxDownloadBytes); err != nil {
		return fmt.Errorf("复制启动器文件 %s 失败：%w", entry.Path, err)
	}
	if err := verifyDigest(destination, entry.Algorithm, entry.Value); err != nil {
		_ = os.Remove(destination)
		return fmt.Errorf("启动器文件 %s 摘要校验失败：%w；请确认启动器已下载正确版本", entry.Path, err)
	}
	return nil
}

func promptLauncherInstance(updateRoot, updaterRoot, packagePath string) (string, error) {
	savedPathFile := filepath.Join(updateRoot, "launcher-instance-path.txt")
	var savedPath string
	if data, err := readRegularFile(savedPathFile, 32<<10); err == nil {
		savedPath = strings.Trim(strings.TrimSpace(string(data)), "\"")
		if savedPath != "" {
			if valid, validateErr := validateLauncherInstanceRoot(savedPath, updaterRoot); validateErr == nil {
				savedPath = valid
			} else {
				savedPath = ""
			}
		}
	}

	fmt.Println()
	fmt.Println("第三方文件将由启动器下载，更新器不会再直接请求这些 Mod 的下载源。")
	if packagePath != "" {
		fmt.Println("请将下面的 MRPack 导入 HMCL 或其他支持 Modrinth 的启动器，等待启动器显示安装成功：")
		fmt.Println(packagePath)
	}
	fmt.Println("随后在启动器中打开这次新建实例的游戏文件夹，复制其完整路径。")
	fmt.Println("注意：请只选择本次新建的临时实例；更新成功后，更新器会自动删除这个目录。")
	if savedPath != "" {
		fmt.Println("上次使用的临时实例路径仍然有效，直接按回车可继续使用：", savedPath)
	}
	fmt.Print("启动器新实例的游戏目录路径：")
	line, readErr := readConsoleLine()
	if readErr != nil && strings.TrimSpace(line) == "" {
		return "", fmt.Errorf("无法读取启动器实例路径：%w", readErr)
	}
	path := strings.Trim(strings.TrimSpace(line), "\"")
	if path == "" {
		path = savedPath
	}
	validated, err := validateLauncherInstanceRoot(path, updaterRoot)
	if err != nil {
		return "", err
	}
	if err := os.WriteFile(savedPathFile, []byte(validated+"\n"), 0600); err != nil {
		return "", fmt.Errorf("无法记住启动器实例路径：%w", err)
	}
	return validated, nil
}

func removeLauncherInstance(path, updaterRoot string) error {
	validated, err := validateLauncherInstanceRoot(path, updaterRoot)
	if err != nil {
		return err
	}
	if err := os.RemoveAll(validated); err != nil {
		return fmt.Errorf("删除临时启动器实例失败：%w", err)
	}
	return nil
}
