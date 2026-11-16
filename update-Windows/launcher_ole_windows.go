//go:build windows

package main

import (
	"fmt"
	"runtime"
	"syscall"
	"time"
	"unsafe"
)

type winRect struct {
	Left, Top, Right, Bottom int32
}

type oleDropSource struct {
	vtable uintptr
	refs   uint32
}

type winGUID struct {
	Data1 uint32
	Data2 uint16
	Data3 uint16
	Data4 [8]byte
}

var (
	ole32DLL   = syscall.NewLazyDLL("ole32.dll")
	shell32DLL = syscall.NewLazyDLL("shell32.dll")
	user32DLL  = syscall.NewLazyDLL("user32.dll")

	procOleInitialize    = ole32DLL.NewProc("OleInitialize")
	procOleUninitialize  = ole32DLL.NewProc("OleUninitialize")
	procDoDragDrop       = ole32DLL.NewProc("DoDragDrop")
	procSHCreateData     = shell32DLL.NewProc("SHCreateDataObject")
	procILCreateFromPath = shell32DLL.NewProc("ILCreateFromPathW")
	procILFree           = shell32DLL.NewProc("ILFree")
	procEnumWindows      = user32DLL.NewProc("EnumWindows")
	procGetWindowPID     = user32DLL.NewProc("GetWindowThreadProcessId")
	procIsWindowVisible  = user32DLL.NewProc("IsWindowVisible")
	procGetWindowRect    = user32DLL.NewProc("GetWindowRect")
	procSetForeground    = user32DLL.NewProc("SetForegroundWindow")
	procSetCursorPos     = user32DLL.NewProc("SetCursorPos")

	iidIUnknown = winGUID{Data4: [8]byte{0xC0, 0, 0, 0, 0, 0, 0, 0x46}}
	iidIDataObj = winGUID{Data1: 0x0000010E, Data4: [8]byte{0xC0, 0, 0, 0, 0, 0, 0, 0x46}}
	iidDropSrc  = winGUID{Data1: 0x00000121, Data4: [8]byte{0xC0, 0, 0, 0, 0, 0, 0, 0x46}}

	dropSourceVTable = [5]uintptr{
		syscall.NewCallback(dropSourceQueryInterface),
		syscall.NewCallback(dropSourceAddRef),
		syscall.NewCallback(dropSourceRelease),
		syscall.NewCallback(dropSourceQueryContinueDrag),
		syscall.NewCallback(dropSourceGiveFeedback),
	}
)

func dropSourceQueryInterface(this, riid, out uintptr) uintptr {
	if riid == 0 || out == 0 {
		return 0x80004003 // E_POINTER
	}
	id := (*winGUID)(unsafe.Pointer(riid))
	if *id != iidIUnknown && *id != iidDropSrc {
		*(*uintptr)(unsafe.Pointer(out)) = 0
		return 0x80004002 // E_NOINTERFACE
	}
	*(*uintptr)(unsafe.Pointer(out)) = this
	dropSourceAddRef(this)
	return 0
}

func dropSourceAddRef(this uintptr) uintptr {
	obj := (*oleDropSource)(unsafe.Pointer(this))
	obj.refs++
	return uintptr(obj.refs)
}

func dropSourceRelease(this uintptr) uintptr {
	obj := (*oleDropSource)(unsafe.Pointer(this))
	if obj.refs > 0 {
		obj.refs--
	}
	return uintptr(obj.refs)
}

func dropSourceQueryContinueDrag(this, escapePressed, keyState uintptr) uintptr {
	if escapePressed != 0 {
		return 0x00040101 // DRAGDROP_S_CANCEL
	}
	return 0x00040100 // DRAGDROP_S_DROP
}

func dropSourceGiveFeedback(this, effect uintptr) uintptr {
	return 0x00040102 // DRAGDROP_S_USEDEFAULTCURSORS
}

func launcherWindowForPID(pid uint32) (uintptr, winRect) {
	var best uintptr
	var bestRect winRect
	var bestArea int64
	callback := syscall.NewCallback(func(hwnd, _ uintptr) uintptr {
		if visible, _, _ := procIsWindowVisible.Call(hwnd); visible == 0 {
			return 1
		}
		var windowPID uint32
		procGetWindowPID.Call(hwnd, uintptr(unsafe.Pointer(&windowPID)))
		if windowPID != pid {
			return 1
		}
		var rect winRect
		if result, _, _ := procGetWindowRect.Call(hwnd, uintptr(unsafe.Pointer(&rect))); result == 0 {
			return 1
		}
		area := int64(rect.Right-rect.Left) * int64(rect.Bottom-rect.Top)
		if area > bestArea {
			best, bestRect, bestArea = hwnd, rect, area
		}
		return 1
	})
	procEnumWindows.Call(callback, 0)
	runtime.KeepAlive(callback)
	return best, bestRect
}

func waitLauncherWindow(pid uint32) (uintptr, winRect, error) {
	deadline := time.Now().Add(20 * time.Second)
	for time.Now().Before(deadline) {
		if hwnd, rect := launcherWindowForPID(pid); hwnd != 0 {
			return hwnd, rect, nil
		}
		time.Sleep(250 * time.Millisecond)
	}
	return 0, winRect{}, fmt.Errorf("启动器进程 %d 在 20 秒内没有创建可见窗口", pid)
}

func oleDropMRPack(hwnd uintptr, rect winRect, packagePath string) error {
	runtime.LockOSThread()
	defer runtime.UnlockOSThread()

	hr, _, _ := procOleInitialize.Call(0)
	if int32(hr) < 0 {
		return fmt.Errorf("OleInitialize 失败：HRESULT 0x%08X", uint32(hr))
	}
	defer procOleUninitialize.Call()

	path, err := syscall.UTF16PtrFromString(packagePath)
	if err != nil {
		return err
	}
	pidl, _, _ := procILCreateFromPath.Call(uintptr(unsafe.Pointer(path)))
	if pidl == 0 {
		return fmt.Errorf("无法为差量 MRPack 创建 Shell PIDL")
	}
	defer procILFree.Call(pidl)

	pidls := [1]uintptr{pidl}
	dataObject := uintptr(0)
	hr, _, _ = procSHCreateData.Call(
		0, 1,
		uintptr(unsafe.Pointer(&pidls[0])),
		0,
		uintptr(unsafe.Pointer(&iidIDataObj)),
		uintptr(unsafe.Pointer(&dataObject)),
	)
	if int32(hr) < 0 || dataObject == 0 {
		return fmt.Errorf("SHCreateDataObject 失败：HRESULT 0x%08X", uint32(hr))
	}
	dataVTable := *(**[12]uintptr)(unsafe.Pointer(dataObject))
	defer syscall.SyscallN(dataVTable[2], dataObject)

	source := oleDropSource{vtable: uintptr(unsafe.Pointer(&dropSourceVTable[0])), refs: 1}
	x := int32(rect.Left + (rect.Right-rect.Left)/2)
	y := int32(rect.Top + (rect.Bottom-rect.Top)/2)
	procSetForeground.Call(hwnd)
	procSetCursorPos.Call(uintptr(uint32(x)), uintptr(uint32(y)))

	var effect uint32
	hr, _, _ = procDoDragDrop.Call(
		dataObject,
		uintptr(unsafe.Pointer(&source)),
		1, // DROPEFFECT_COPY
		uintptr(unsafe.Pointer(&effect)),
	)
	runtime.KeepAlive(&source)
	runtime.KeepAlive(&dropSourceVTable)
	if int32(hr) < 0 {
		return fmt.Errorf("DoDragDrop 失败：HRESULT 0x%08X", uint32(hr))
	}
	if effect == 0 {
		return fmt.Errorf("启动器窗口未接受该 MRPack 的 OLE 拖放")
	}
	return nil
}

func openMRPackWithLauncher(launcher Launcher, packagePath string) error {
	cmd := launcherCommand(launcher, "")
	if err := cmd.Start(); err != nil {
		return fmt.Errorf("启动 %s 失败：%w", launcher.Name, err)
	}
	pid := uint32(cmd.Process.Pid)
	go func() { _ = cmd.Wait() }()

	hwnd, rect, err := waitLauncherWindow(pid)
	if err != nil {
		return err
	}
	if err := oleDropMRPack(hwnd, rect, packagePath); err != nil {
		return fmt.Errorf("向 %s 模拟 OLE 文件拖放失败：%w", launcher.Name, err)
	}
	return nil
}
