//go:build !windows

package main

// Unix terminals already use UTF-8 on supported macOS and Linux environments.
func enableUTF8Console() {}
