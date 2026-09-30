#!/usr/bin/env python3
"""Build a Modrinth-format (.mrpack) package from manifest/mods.json."""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path
from typing import Any

sys.path.insert(0, str(Path(__file__).resolve().parent))
import pack_hook  # noqa: E402  (sibling packaging preflight hook)
import pack_version  # noqa: E402  (sibling release version rules)


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "manifest" / "mods.json"
DEFAULT_INSTANCE_DIR = (
    Path.home() / "Library" / "Application Support" / "minecraft" / "versions" / "DimensionWorks"
)
FORGE_VERSION = "47.4.23"
USER_AGENT = "DimensionWorks-Packager/0.1"
OVERRIDE_DIRS = ("defaultconfigs", "kubejs", "resourcepacks", "shaderpacks")
VERBOSE = False
REPO_CONFIG_OVERRIDE_DIRS = ("config",)
IGNORED_OVERRIDE_FILES = {".DS_Store", "README", "README.md"}
INSTANCE_CONFIG_TREES = (
    (Path("config"), Path("config"), True),
    (Path("defaultconfigs"), Path("defaultconfigs"), False),
    (Path("local"), Path("local"), False),
    (Path("kubejs/config"), Path("kubejs/config"), False),
)
INSTANCE_CONFIG_FILES = (
    Path("options.txt"),
    Path("log4j2.xml"),
    Path("rhino.local.properties"),
    Path("CustomSkinLoader/CustomSkinLoader.json"),
    Path("minemenu/menu.json"),
)
REPO_OVERRIDE_FILES = (Path("options.txt"),)


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
        if VERBOSE:
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


def copy_override_tree(
    source_root: Path,
    overrides_root: Path,
    *,
    destination_root: Path | None = None,
    ignored_files: frozenset[str] = IGNORED_OVERRIDE_FILES,
    required: bool = False,
) -> int:
    copied = 0
    if not source_root.is_dir():
        if required:
            raise FileNotFoundError(f"missing required override directory: {source_root}")
        return copied
    tree_destination = destination_root or Path(source_root.name)
    for source in source_root.rglob("*"):
        if not source.is_file() or source.name in ignored_files:
            continue
        relative = source.relative_to(source_root)
        destination = overrides_root / tree_destination / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, destination)
        copied += 1
    return copied


def copy_override_file(
    source: Path,
    overrides_root: Path,
    destination_relative: Path,
) -> int:
    if not source.is_file():
        return 0
    destination = overrides_root / destination_relative
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, destination)
    return 1


def build_pack(version: str, output: Path, cache_dir: Path, instance_dir: Path) -> str:
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
    for directory in REPO_CONFIG_OVERRIDE_DIRS:
        copied += copy_override_tree(ROOT / directory, overrides)
    for directory in OVERRIDE_DIRS:
        copied += copy_override_tree(ROOT / directory, overrides)
    for source_relative, destination_relative, required in INSTANCE_CONFIG_TREES:
        copied += copy_override_tree(
            instance_dir / source_relative,
            overrides,
            destination_root=destination_relative,
            ignored_files=frozenset(),
            required=required,
        )
    for relative in INSTANCE_CONFIG_FILES:
        copied += copy_override_file(instance_dir / relative, overrides, relative)

    for relative in REPO_OVERRIDE_FILES:
        copied += copy_override_file(ROOT / relative, overrides, relative)

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

    return (
        f"{output} ({output.stat().st_size} bytes), "
        f"{len(index_files)} downloaded mods, {len(in_repo_mods)} in-repo mods, "
        f"{copied - len(in_repo_mods)} override files"
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", default=None)
    parser.add_argument(
        "--dimension-complete",
        action="store_true",
        help="bump the minor version because a dimension was completed",
    )
    parser.add_argument(
        "--major-bump",
        action="store_true",
        help="bump the major version only when explicitly requested",
    )
    parser.add_argument("--skip-hook", action="store_true", help="skip the packaging preflight hook")
    parser.add_argument("--skip-build", action="store_true", help="skip in-repo mod builds in the hook")
    parser.add_argument("--verbose", action="store_true", help="print every hook step and download")
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
    parser.add_argument(
        "--instance-dir",
        type=Path,
        default=DEFAULT_INSTANCE_DIR,
        help="game instance root containing config/ (default: %(default)s)",
    )
    args = parser.parse_args()
    global VERBOSE
    VERBOSE = args.verbose
    try:
        version = pack_version.resolve_version(
            args.version,
            dimension_complete=args.dimension_complete,
            major_bump=args.major_bump,
        )
    except pack_version.VersionRuleError as error:
        print("PACK-HOOK FAIL")
        print("step: pack-version")
        print(f"detail: {error}")
        raise SystemExit(1)
    output = args.output or ROOT / "dist" / f"DimensionWorks-{version}.mrpack"
    hook_summary = "skipped"
    if not args.skip_hook:
        ok, message = pack_hook.run_all(
            args.instance_dir.expanduser().resolve(),
            skip_build=args.skip_build,
            verbose=args.verbose,
            requested_version=str(version),
            dimension_complete=args.dimension_complete,
            major_bump=args.major_bump,
        )
        if not ok:
            print("PACK-HOOK FAIL")
            print(message)
            raise SystemExit(1)
        hook_summary = message
    pack_summary = build_pack(
        str(version),
        output.resolve(),
        args.cache_dir.resolve(),
        args.instance_dir.expanduser().resolve(),
    )
    pack_version.write_current_version(version)
    print(f"PACK OK: {pack_summary} | hook: {hook_summary}")


if __name__ == "__main__":
    main()
