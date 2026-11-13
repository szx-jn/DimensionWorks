package main

import (
	"errors"
	"fmt"
	"net/url"
	"os"
	"path/filepath"
	"runtime"
	"strings"
)

func draggedPath(input string) (string, error) {
	input = strings.TrimSpace(input)
	if input == "" {
		return "", errors.New("路径不能为空")
	}
	// Also accept a pasted, unescaped path containing spaces.
	if _, err := os.Stat(input); err == nil {
		return input, nil
	}
	var path string
	if runtime.GOOS == "windows" {
		path = strings.TrimPrefix(strings.TrimSuffix(input, "\""), "\"")
	} else {
		var result strings.Builder
		var quote byte
		for i := 0; i < len(input); i++ {
			c := input[i]
			if c == '\\' && quote != '\'' {
				if i+1 == len(input) {
					return "", errors.New("路径末尾的转义不完整")
				}
				i++
				result.WriteByte(input[i])
			} else if quote != 0 {
				if c == quote {
					quote = 0
				} else {
					result.WriteByte(c)
				}
			} else if c == '\'' || c == '"' {
				quote = c
			} else if c == ' ' || c == '\t' {
				return "", errors.New("请只拖入一个文件或目录；含空格的路径请保留终端添加的引号或转义")
			} else {
				result.WriteByte(c)
			}
		}
		if quote != 0 {
			return "", errors.New("路径的引号未闭合")
		}
		path = result.String()
	}
	if strings.HasPrefix(path, "file:") {
		uri, err := url.Parse(path)
		if err != nil || uri.Scheme != "file" || (uri.Host != "" && uri.Host != "localhost") {
			return "", errors.New("文件 URI 必须指向本机文件或目录")
		}
		path = uri.Path
	}
	if path == "" {
		return "", errors.New("路径不能为空")
	}
	return path, nil
}

func customLauncherFile(path string) (Launcher, error) {
	info, err := os.Stat(path)
	if err != nil {
		return Launcher{}, fmt.Errorf("无法访问启动器文件：%w", err)
	}
	if runtime.GOOS == "darwin" && info.IsDir() && strings.EqualFold(filepath.Ext(path), ".app") {
		return Launcher{Path: path}, nil
	}
	if !info.Mode().IsRegular() {
		return Launcher{}, errors.New("请选择启动器应用或普通文件")
	}
	if strings.EqualFold(filepath.Ext(path), ".jar") ||
		(runtime.GOOS == "windows" && strings.EqualFold(filepath.Ext(path), ".exe")) ||
		(runtime.GOOS != "windows" && info.Mode().Perm()&0111 != 0) {
		return Launcher{Path: path, WorkingDir: filepath.Dir(path)}, nil
	}
	return Launcher{}, errors.New("启动器格式不受支持；.jar 需要已安装 Java，AppImage 或原生程序需要可执行权限")
}
