//go:build !bundled_aria2 || !((linux && amd64) || (windows && amd64) || (darwin && arm64))

package main

var bundledAria2 []byte
