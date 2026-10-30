//go:build windows

package main

import (
	"errors"
	"fmt"
	"os"
	"syscall"
	"unsafe"
)

var lockFileExProc = syscall.NewLazyDLL("kernel32.dll").NewProc("LockFileEx")
var unlockFileExProc = syscall.NewLazyDLL("kernel32.dll").NewProc("UnlockFileEx")

const (
	lockfileFailImmediately = 0x00000001
	lockfileExclusiveLock = 0x00000002
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
	var overlapped syscall.Overlapped
	result, _, callErr := lockFileExProc.Call(file.Fd(), lockfileExclusiveLock|lockfileFailImmediately, 0, 1, 0, uintptr(unsafe.Pointer(&overlapped)))
	if result == 0 {
		_ = file.Close()
		if callErr != syscall.Errno(0) { return nil, fmt.Errorf("另一个更新器正在处理更新锁确认：%w", callErr) }
		return nil, errors.New("另一个更新器正在处理更新锁确认")
	}
	return func() {
		_, _, _ = unlockFileExProc.Call(file.Fd(), 0, 1, 0, uintptr(unsafe.Pointer(&overlapped)))
		_ = file.Close()
	}, nil
}
