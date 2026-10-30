//go:build windows

package main

import (
	"fmt"
	"syscall"
	"unsafe"
)

var getDiskFreeSpaceExW = syscall.NewLazyDLL("kernel32.dll").NewProc("GetDiskFreeSpaceExW")

func availableDiskSpace(path string) (uint64, error) {
	wide, err := syscall.UTF16PtrFromString(path)
	if err != nil { return 0, err }
	var available, total, free uint64
	result, _, callErr := getDiskFreeSpaceExW.Call(
		uintptr(unsafe.Pointer(wide)),
		uintptr(unsafe.Pointer(&available)),
		uintptr(unsafe.Pointer(&total)),
		uintptr(unsafe.Pointer(&free)),
	)
	if result == 0 {
		if callErr != syscall.Errno(0) { return 0, fmt.Errorf("GetDiskFreeSpaceExW: %w", callErr) }
		return 0, syscall.EINVAL
	}
	return available, nil
}
