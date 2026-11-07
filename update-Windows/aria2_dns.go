package main

import (
	"net"
	"net/url"
	"runtime"
	"strings"
)

func aria2AsyncDNS(source string) bool {
	return aria2AsyncDNSForOS(runtime.GOOS, source)
}

func aria2AsyncDNSForOS(goos, source string) bool {
	// The bundled Linux engine crashes in its native resolver path, including
	// for localhost. Its async resolver handles local and remote hosts safely.
	if goos == "linux" {
		return true
	}
	parsed, err := url.Parse(source)
	return err == nil && shouldUseEngineDNS(parsed.Hostname())
}

func shouldUseEngineDNS(host string) bool {
	host = strings.TrimSuffix(strings.ToLower(host), ".")
	if host == "" || host == "localhost" {
		return false
	}
	if net.ParseIP(host) != nil {
		return false
	}
	// Local, split-DNS and private workshop names need the computer's native
	// resolver. All ordinary public hosts use the engine resolver so a stale
	// loopback or fake-IP answer cannot bypass the active system route or TUN.
	for _, suffix := range []string{".local", ".internal", ".intranet", ".lan", ".home", ".test", ".invalid", ".example", ".localdomain"} {
		if strings.HasSuffix(host, suffix) {
			return false
		}
	}
	return true
}
