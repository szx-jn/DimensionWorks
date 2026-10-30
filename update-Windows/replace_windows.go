//go:build windows

package main

import (
	"fmt"
	"syscall"
	"unsafe"
)

var moveFileExW = syscall.NewLazyDLL("kernel32.dll").NewProc("MoveFileExW")

const (
	moveFileReplaceExisting = 0x1
	moveFileWriteThrough    = 0x8
)

// replaceExistingFile atomically replaces destination without deleting the
// previous file before the replacement is ready.
func replaceExistingFile(source, destination string) error {
	from, err := syscall.UTF16PtrFromString(source)
	if err != nil { return err }
	to, err := syscall.UTF16PtrFromString(destination)
	if err != nil { return err }
	result, _, callErr := moveFileExW.Call(
		uintptr(unsafe.Pointer(from)),
		uintptr(unsafe.Pointer(to)),
		moveFileReplaceExisting|moveFileWriteThrough,
	)
	if result == 0 {
		if callErr != syscall.Errno(0) { return fmt.Errorf("MoveFileExW: %w", callErr) }
		return syscall.EINVAL
	}
	return nil
}
