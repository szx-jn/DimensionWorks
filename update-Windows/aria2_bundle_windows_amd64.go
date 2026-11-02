//go:build bundled_aria2

package main

import _ "embed"

//go:embed aria2-runtime/aria2c-windows-amd64.exe
var bundledAria2 []byte
