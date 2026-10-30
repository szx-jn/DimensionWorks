//go:build !windows

package main

import (
	"errors"
	"fmt"
	"os"
	"syscall"
)

func acquireConfirmationGuard(path string) (func(), error) {
	before, beforeErr := os.Lstat(path)
	if beforeErr != nil && !os.IsNotExist(beforeErr) { return nil, beforeErr }
	if beforeErr == nil && (before.Mode()&os.ModeSymlink != 0 || !before.Mode().IsRegular()) {
		return nil, errors.New("更新锁确认文件不是普通文件")
	}
	file, err := os.OpenFile(path, os.O_CREATE|os.O_RDWR, 0600)
	if err != nil { return nil, err }
	opened, err := file.Stat()
	if err != nil { _ = file.Close(); return nil, err }
	after, err := os.Lstat(path)
	if err != nil || after.Mode()&os.ModeSymlink != 0 || !after.Mode().IsRegular() || !os.SameFile(opened, after) {
		_ = file.Close()
		return nil, errors.New("更新锁确认文件在打开期间发生变化")
	}
	if beforeErr == nil && !os.SameFile(before, after) {
		_ = file.Close()
		return nil, errors.New("更新锁确认文件被替换")
	}
	if err := syscall.Flock(int(file.Fd()), syscall.LOCK_EX|syscall.LOCK_NB); err != nil {
		_ = file.Close()
		return nil, fmt.Errorf("另一个更新器正在处理更新锁确认：%w", err)
	}
	return func() {
		_ = syscall.Flock(int(file.Fd()), syscall.LOCK_UN)
		_ = file.Close()
	}, nil
}
