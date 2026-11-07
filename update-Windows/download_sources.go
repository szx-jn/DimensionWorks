package main

import (
	"net/url"
	"sync"
)

// Remember a working asset source for this process, across manifests, mods and
// archives. A failed preferred source still falls back to every other candidate.
var downloadSourceOrder sourcePreference

type sourcePreference struct {
	mu     sync.Mutex
	origin string
}

func downloadOrigin(source string) string {
	parsed, err := url.Parse(source)
	if err != nil {
		return ""
	}
	return parsed.Scheme + "://" + parsed.Host
}

func (p *sourcePreference) order(candidates []string) []string {
	p.mu.Lock()
	preferred := p.origin
	p.mu.Unlock()
	for i, candidate := range candidates {
		if preferred != "" && downloadOrigin(candidate) == preferred {
			ordered := append([]string{candidate}, candidates[:i]...)
			return append(ordered, candidates[i+1:]...)
		}
	}
	return candidates
}

func (p *sourcePreference) remember(source string) {
	p.mu.Lock()
	p.origin = downloadOrigin(source)
	p.mu.Unlock()
}
