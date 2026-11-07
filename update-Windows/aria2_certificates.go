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
	switch runtime.GOOS {
	case "darwin":
		// Pinned engine uses Apple Security/Keychain, not OpenSSL PEM bundles.
		return nil, noop, nil
	case "linux":
		return prepareLinuxAria2Certificates()
	case "windows":
	default:
		return nil, noop, fmt.Errorf("不支持的证书平台：%s", runtime.GOOS)
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

// Cover common distribution paths instead of the engine's Debian-only default.
func prepareLinuxAria2Certificates() ([]string, func(), error) {
	noop := func() {}
	paths := []string{"/etc/ssl/certs/ca-certificates.crt", "/etc/pki/tls/certs/ca-bundle.crt", "/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem", "/etc/ssl/ca-bundle.pem", "/etc/ssl/cert.pem"}
	if override := os.Getenv("SSL_CERT_FILE"); override != "" {
		if !filepath.IsAbs(override) {
			return nil, noop, fmt.Errorf("SSL_CERT_FILE 必须是绝对路径")
		}
		paths = []string{override}
	}
	for _, path := range paths {
		info, err := os.Stat(path)
		if err != nil || !info.Mode().IsRegular() || info.Size() > 4*1024*1024 {
			continue
		}
		data, err := os.ReadFile(path)
		if err != nil || !x509.NewCertPool().AppendCertsFromPEM(data) {
			continue
		}
		return []string{"--ca-certificate=" + path}, noop, nil
	}
	return nil, noop, fmt.Errorf("找不到有效的 Linux 系统根证书；请安装或更新 ca-certificates，或设置 SSL_CERT_FILE 指向受信任 PEM 文件")
}
