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
	file, err := os.OpenFile(path, os.O_CREATE|os.O_RDWR, 0600)
	if err != nil { return nil, err }
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
