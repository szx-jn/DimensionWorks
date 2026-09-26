#!/usr/bin/env python3
"""Build a Modrinth-format (.mrpack) package from manifest/mods.json."""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "manifest" / "mods.json"
DEFAULT_VERSION = "0.1.0-alpha.1"
FORGE_VERSION = "47.4.23"
USER_AGENT = "DimensionWorks-Packager/0.1"
OVERRIDE_DIRS = ("config", "defaultconfigs", "kubejs", "resourcepacks")
IGNORED_OVERRIDE_FILES = {".DS_Store", "README", "README.md"}


def request_json(url: str) -> Any:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def download(url: str, destination: Path) -> None:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=180) as response:
        with destination.open("wb") as output:
            shutil.copyfileobj(response, output)


def hashes(path: Path) -> tuple[str, str, int]:
    sha1 = hashlib.sha1()
    sha512 = hashlib.sha512()
    size = 0
    with path.open("rb") as source:
        while chunk := source.read(1024 * 1024):
            size += len(chunk)
            sha1.update(chunk)
            sha512.update(chunk)
    return sha1.hexdigest(), sha512.hexdigest(), size


def cached_file(cache_dir: Path, source: str, filename: str, url: str) -> Path:
    destination = cache_dir / source / filename
    destination.parent.mkdir(parents=True, exist_ok=True)
    if not destination.is_file() or destination.stat().st_size == 0:
        print(f"download: {url}")
        download(url, destination)
    return destination


def environment(mod: dict[str, Any]) -> dict[str, str]:
    requirement = "required" if mod.get("required", True) else "optional"
    supported = mod["environment"]
    if supported == "both":
        return {"client": requirement, "server": requirement}
    if supported == "client":
        return {"client": requirement, "server": "unsupported"}
    if supported == "server":
        return {"client": "unsupported", "server": requirement}
    raise ValueError(f"{mod['id']}: unsupported environment {supported!r}")


def mod_file(
    mod: dict[str, Any],
    filename: str,
    download_url: str,
    sha1: str,
    sha512: str,
    size: int,
) -> dict[str, Any]:
    return {
        "path": f"mods/{filename}",
        "hashes": {"sha1": sha1, "sha512": sha512},
        "env": environment(mod),
        "downloads": [download_url],
        "fileSize": size,
    }


def resolve_modrinth(mod: dict[str, Any]) -> dict[str, Any]:
    version = request_json(f"https://api.modrinth.com/v2/version/{mod['version_id']}")
    files = version.get("files", [])
    selected = next((item for item in files if item.get("primary")), None)
    if selected is None and files:
        selected = files[0]
    if selected is None:
        raise ValueError(f"{mod['id']}: Modrinth version has no files")
    return mod_file(
        mod,
        selected["filename"],
        selected["url"],
        selected["hashes"]["sha1"],
        selected["hashes"]["sha512"],
        selected["size"],
    )


def curseforge_url(mod: dict[str, Any]) -> str:
    file_id = int(mod["file_id"])
    return (
        f"https://mediafilez.forgecdn.net/files/{file_id // 1000}/{file_id % 1000:03d}/"
        f"{urllib.parse.quote(mod['file_name'])}"
    )


def resolve_curseforge(mod: dict[str, Any], cache_dir: Path) -> dict[str, Any]:
    url = curseforge_url(mod)
    path = cached_file(cache_dir, "curseforge", mod["file_name"], url)
    sha1, sha512, size = hashes(path)
    return mod_file(mod, mod["file_name"], url, sha1, sha512, size)


def resolve_github(mod: dict[str, Any], cache_dir: Path) -> dict[str, Any]:
    url = (
        f"https://github.com/{mod['repository']}/releases/download/"
        f"{urllib.parse.quote(mod['release'])}/{urllib.parse.quote(mod['asset'])}"
    )
    path = cached_file(cache_dir, "github", mod["asset"], url)
    sha1, sha512, size = hashes(path)
    return mod_file(mod, mod["asset"], url, sha1, sha512, size)


def in_repo_jar(mod: dict[str, Any]) -> Path:
    project = ROOT / mod["project_path"]
    jar = project / "build" / "libs" / f"{mod['id']}-{mod['version']}.jar"
    if not jar.is_file():
        raise FileNotFoundError(
            f"{mod['id']}: missing {jar}. Build it first with "
            f"`gradle build --no-daemon` in {project}."
        )
    return jar


def copy_override_tree(source_root: Path, overrides_root: Path) -> int:
    copied = 0
    if not source_root.exists():
        return copied
    for source in source_root.rglob("*"):
        if not source.is_file() or source.name in IGNORED_OVERRIDE_FILES:
            continue
        relative = source.relative_to(source_root)
        destination = overrides_root / source_root.name / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, destination)
        copied += 1
    return copied


def build_pack(version: str, output: Path, cache_dir: Path) -> None:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    index_files: list[dict[str, Any]] = []
    in_repo_mods: list[Path] = []
    seen_paths: set[str] = set()

    for mod in manifest["mods"]:
        source = mod["source"]
        if source == "modrinth":
            entry = resolve_modrinth(mod)
        elif source == "curseforge":
            entry = resolve_curseforge(mod, cache_dir)
        elif source == "github-release":
            entry = resolve_github(mod, cache_dir)
        elif source == "in-repo":
            jar = in_repo_jar(mod)
            destination_name = jar.name
            entry = mod_file(
                mod,
                destination_name,
                f"in-repo://{mod['project_path']}",
                "",
                "",
                0,
            )
            entry.pop("downloads", None)
            entry.pop("hashes", None)
            entry.pop("fileSize", None)
            in_repo_mods.append(jar)
        else:
            raise ValueError(f"{mod['id']}: unsupported source {source!r}")

        path = entry["path"]
        if path in seen_paths:
            raise ValueError(f"duplicate pack path: {path}")
        seen_paths.add(path)
        if source != "in-repo":
            index_files.append(entry)

    staging = output.parent / f".{output.stem}.staging"
    if staging.exists():
        shutil.rmtree(staging)
    staging.mkdir(parents=True)

    index = {
        "formatVersion": 1,
        "game": "minecraft",
        "versionId": version,
        "name": "DimensionWorks",
        "summary": "多维度工业自动化整合包",
        "files": index_files,
        "dependencies": {
            "minecraft": manifest["minecraft"]["version"],
            "forge": FORGE_VERSION,
        },
    }
    (staging / "modrinth.index.json").write_text(
        json.dumps(index, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    overrides = staging / "overrides"
    copied = 0
    for directory in OVERRIDE_DIRS:
        copied += copy_override_tree(ROOT / directory, overrides)

    mods_override = overrides / "mods"
    mods_override.mkdir(parents=True, exist_ok=True)
    for jar in in_repo_mods:
        shutil.copy2(jar, mods_override / jar.name)
    copied += len(in_repo_mods)

    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.write(staging / "modrinth.index.json", "modrinth.index.json")
        for path in sorted(overrides.rglob("*")):
            if path.is_file():
                archive.write(path, path.relative_to(staging).as_posix())
    shutil.rmtree(staging)

    print(
        f"created {output} ({output.stat().st_size} bytes), "
        f"{len(index_files)} downloaded mods, {len(in_repo_mods)} in-repo mods, "
        f"{copied - len(in_repo_mods)} override files"
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", default=DEFAULT_VERSION)
    parser.add_argument(
        "--output",
        type=Path,
        default=None,
    )
    parser.add_argument(
        "--cache-dir",
        type=Path,
        default=Path("/private/tmp/dimensionworks-pack-cache"),
    )
    args = parser.parse_args()
    output = args.output or ROOT / "dist" / f"DimensionWorks-{args.version}.mrpack"
    build_pack(args.version, output.resolve(), args.cache_dir.resolve())


if __name__ == "__main__":
    main()
