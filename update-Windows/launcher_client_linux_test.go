//go:build linux

package main

import (
	"strings"
	"testing"
)

func TestLinuxClientUpdateRejectedBeforeInstanceAccess(t *testing.T) {
	err := run("client", nil)
	if err == nil || !strings.Contains(err.Error(), "Linux 仅支持服务端") {
		t.Fatalf("unexpected result: %v", err)
	}
}
