//go:build !windows

package main

import "os"

// replaceExistingFile atomically replaces destination on POSIX filesystems
// when source and destination are on the same filesystem.
func replaceExistingFile(source, destination string) error {
	return os.Rename(source, destination)
}
