package main

import (
	"encoding/json"
	"io"
	"path/filepath"
	"strings"
	"testing"
)

func TestRequiresLauncherDownload(t *testing.T) {
	cases := []struct {
		name string
		entry Entry
		want bool
	}{
		{"CurseForge CDN", Entry{Source: "https://edge.forgecdn.net/files/1/2/mod.jar"}, true},
		{"CurseForge fallback candidate", Entry{Source: "https://cdn.modrinth.com/mod.jar", Sources: []string{"https://cdn.modrinth.com/mod.jar", "https://edge.forgecdn.net/files/1/2/mod.jar"}}, true},
		{"Modrinth", Entry{Source: "https://cdn.modrinth.com/data/id/version/mod.jar"}, false},
		{"GitHub", Entry{Source: "https://github.com/example/mod/releases/download/v1/mod.jar"}, false},
	}
	for _, test := range cases {
		if got := requiresLauncherDownload(test.entry); got != test.want {
			t.Errorf("%s: requiresLauncherDownload() = %v, want %v", test.name, got, test.want)
		}
	}
}

func TestCreateLauncherDeltaMRPackKeepsDependenciesAndOnlySelectedFiles(t *testing.T) {
	const indexJSON = `{
  "formatVersion": 1,
  "game": "minecraft",
  "versionId": "1.2.3",
  "name": "DimensionWorks",
  "dependencies": {
    "minecraft": "1.20.1",
    "forge": "47.4.23"
  },
  "files": [
    {
      "path": "mods/restricted.jar",
      "hashes": {"sha512": "` + strings.Repeat("a", 128) + `"},
      "downloads": ["https://edge.forgecdn.net/files/1/2/restricted.jar"],
      "env": {"client": "required", "server": "unsupported"},
      "fileSize": 123
    },
    {
      "path": "mods/direct.jar",
      "hashes": {"sha512": "` + strings.Repeat("b", 128) + `"},
      "downloads": ["https://cdn.modrinth.com/direct.jar"],
      "env": {"client": "required", "server": "unsupported"},
      "fileSize": 456
    }
  ]
}`
	archive := writeTestMRPack(t, indexJSON)
	destination := filepath.Join(t.TempDir(), "delta.mrpack")
	entry := Entry{Action: "remote", Path: "mods/restricted.jar", Algorithm: "sha512", Value: strings.Repeat("a", 128), Source: "https://edge.forgecdn.net/files/1/2/restricted.jar"}
	if err := createLauncherDeltaMRPack(archive, []Entry{entry}, destination); err != nil {
		t.Fatalf("createLauncherDeltaMRPack() failed: %v", err)
	}
	delta, err := openArchive(destination)
	if err != nil {
		t.Fatal(err)
	}
	defer delta.Close()
	indexFile := delta.entries["modrinth.index.json"]
	if indexFile == nil {
		t.Fatal("delta MRPack is missing modrinth.index.json")
	}
	reader, err := indexFile.Open()
	if err != nil {
		t.Fatal(err)
	}
	data, readErr := io.ReadAll(reader)
	closeErr := reader.Close()
	if readErr != nil {
		t.Fatal(readErr)
	}
	if closeErr != nil {
		t.Fatal(closeErr)
	}
	var got modrinthIndex
	if err := json.Unmarshal(data, &got); err != nil {
		t.Fatal(err)
	}
	if len(got.Files) != 1 || got.Files[0].Path != "mods/restricted.jar" {
		t.Fatalf("delta files = %#v, want only restricted.jar", got.Files)
	}
	if got.Dependencies["minecraft"] != "1.20.1" || got.Dependencies["forge"] != "47.4.23" {
		t.Fatalf("delta dependencies were not preserved: %#v", got.Dependencies)
	}
	if len(delta.entries) != 1 {
		t.Fatalf("delta archive has %d entries, want only index", len(delta.entries))
	}
}
