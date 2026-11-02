//go:build bundled_aria2

package main

import _ "embed"

// The pinned executable is downloaded and verified before release compilation.
//
//go:embed aria2-runtime/aria2c-linux-amd64
var bundledAria2 []byte
