package main

import (
	"fmt"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"unicode/utf8"
)

const maxVersionFileBytes = 64

var versionRE = regexp.MustCompile(`^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$`)

func parseVersion(value string) ([3]uint64, error) {
	var numbers [3]uint64
	if !versionRE.MatchString(value) {
		return numbers, fmt.Errorf("版本号必须使用 x.x.x 三段数字格式：%q", value)
	}
	for i, part := range strings.Split(value, ".") {
		number, err := strconv.ParseUint(part, 10, 31)
		if err != nil {
			return numbers, fmt.Errorf("版本号每段不能超过 2147483647：%q", value)
		}
		numbers[i] = number
	}
	return numbers, nil
}

// The installed version has exactly one source: <instance root>/VERSION.
// Signed file inventories and release metadata never supply a local version.
func readInstalledVersion(root string) (string, error) {
	path, err := filepath.Abs(filepath.Join(root, "VERSION"))
	if err != nil {
		return "", fmt.Errorf("无法定位游戏根目录 VERSION：%w", err)
	}
	data, err := readRegularFile(path, maxVersionFileBytes)
	if err != nil {
		return "", fmt.Errorf("无法读取游戏根目录版本文件 %s：%w", path, err)
	}
	if !utf8.Valid(data) {
		return "", fmt.Errorf("版本文件 %s 必须使用 UTF-8 编码", path)
	}
	version := strings.Trim(strings.TrimPrefix(string(data), "\ufeff"), " \t\r\n")
	if _, err := parseVersion(version); err != nil {
		return "", fmt.Errorf("版本文件 %s 无效：%w", path, err)
	}
	return version, nil
}

func verifyInstalledVersion(root, expected string) error {
	actual, err := readInstalledVersion(root)
	if err != nil {
		return err
	}
	if actual != expected {
		return fmt.Errorf("游戏根目录 VERSION 在更新期间发生变化：%s -> %s，已中止更新，请重新运行", expected, actual)
	}
	return nil
}

func compareVersions(left, right string) (int, error) {
	a, err := parseVersion(left)
	if err != nil {
		return 0, err
	}
	b, err := parseVersion(right)
	if err != nil {
		return 0, err
	}
	for i := range a {
		if a[i] < b[i] {
			return -1, nil
		}
		if a[i] > b[i] {
			return 1, nil
		}
	}
	return 0, nil
}
