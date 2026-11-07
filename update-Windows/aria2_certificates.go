package main

import (
	"context"
	"crypto/x509"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"time"
)

// Windows OpenSSL does not automatically read the operating system's roots.
func prepareAria2Certificates(parent context.Context) ([]string, func(), error) {
	noop := func() {}
	if runtime.GOOS != "windows" {
		return nil, noop, nil
	}
	root := os.Getenv("SystemRoot")
	if root == "" || !filepath.IsAbs(root) {
		return nil, noop, fmt.Errorf("Windows SystemRoot 无效")
	}
	ctx, cancel := context.WithTimeout(parent, 15*time.Second)
	defer cancel()
	powershell := filepath.Join(root, "System32", "WindowsPowerShell", "v1.0", "powershell.exe")
	// Export trust anchors only; never promote intermediate certificates.
	script := `$ErrorActionPreference = 'Stop'; [Console]::OutputEncoding = [System.Text.Encoding]::ASCII; $certs = @(Get-ChildItem Cert:\LocalMachine\Root) + @(Get-ChildItem Cert:\CurrentUser\Root); foreach ($cert in $certs) { '-----BEGIN CERTIFICATE-----'; [Convert]::ToBase64String($cert.RawData, [Base64FormattingOptions]::InsertLineBreaks); '-----END CERTIFICATE-----' }`
	data, err := exec.CommandContext(ctx, powershell, "-NoProfile", "-NonInteractive", "-Command", script).Output()
	if err != nil {
		return nil, noop, fmt.Errorf("导出 Windows 根证书失败：%w", err)
	}
	if len(data) > 4*1024*1024 || !x509.NewCertPool().AppendCertsFromPEM(data) {
		return nil, noop, fmt.Errorf("Windows 根证书为空或格式无效")
	}
	file, err := os.CreateTemp("", "dimensionworks-trusted-roots-*.pem")
	if err != nil {
		return nil, noop, err
	}
	cleanup := func() { os.Remove(file.Name()) }
	_, writeErr := file.Write(data)
	closeErr := file.Close()
	if writeErr != nil {
		cleanup()
		return nil, noop, writeErr
	}
	if closeErr != nil {
		cleanup()
		return nil, noop, closeErr
	}
	return []string{"--ca-certificate=" + file.Name()}, cleanup, nil
}
