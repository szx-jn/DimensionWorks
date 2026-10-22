//go:build windows

package main

import "syscall"

var (
	kernel32          = syscall.NewLazyDLL("kernel32.dll")
	setConsoleOutput  = kernel32.NewProc("SetConsoleOutputCP")
	setConsoleInput   = kernel32.NewProc("SetConsoleCP")
)

func enableUTF8Console() {
	_, _, _ = setConsoleOutput.Call(65001)
	_, _, _ = setConsoleInput.Call(65001)
}
