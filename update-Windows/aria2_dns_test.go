package main

import (
	"os"
	"path/filepath"
	"testing"
)

func TestAria2DNSUsesEngineResolverForRemoteHostsAcrossPlatforms(t *testing.T) {
	for _, source := range []string{
		assetURL("v0.3.24", "manifest.tsv"),
		"https://api.github.com/repos/szx-jn/DimensionWorks/releases/latest",
		"https://ghproxy.net/https://github.com/szx-jn/DimensionWorks/releases/download/v0.3.24/manifest.tsv",
		"https://workshop.example.com/package.zip",
	} {
		if !aria2AsyncDNS(source) {
			t.Errorf("%s should use engine DNS on every platform", source)
		}
	}
	for _, source := range []string{
		"http://localhost/package.zip",
		"http://127.0.0.1/package.zip",
		"http://[::1]/package.zip",
		"https://files.local/package.zip",
		"https://build.internal/package.zip",
		"https://workshop.lan/package.zip",
	} {
		if aria2AsyncDNS(source) {
			t.Errorf("%s must retain native local DNS resolution", source)
		}
	}
}

// This checks the production engine arguments in the current network
// environment. CI remains independent of external connectivity.
func TestAria2EnvironmentGitHubDownload(t *testing.T) {
	if os.Getenv("DIMENSIONWORKS_TEST_GITHUB") != "1" {
		t.Skip("manual network check: set DIMENSIONWORKS_TEST_GITHUB=1")
	}
	requireBundledAria2(t)
	destination := filepath.Join(t.TempDir(), "manifest.tsv")
	if err := downloadOnceLimited(assetURL("v0.3.24", manifestAssetName("client", "0.3.24")), destination, maxManifestBytes); err != nil {
		t.Fatal(err)
	}
	data, err := os.ReadFile(destination)
	if err != nil {
		t.Fatal(err)
	}
	manifest, err := parseManifest(data, "client")
	if err != nil || manifest.Kind != "client" || manifest.Version != "0.3.24" {
		t.Fatalf("GitHub did not return the expected update manifest: %v", err)
	}
}
