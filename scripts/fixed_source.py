"""从固定 GitHub 源码和双重校验值构建第三方 fork，不发布中间发行版。"""
from __future__ import annotations

import hashlib
import os
import re
import shutil
import stat
import subprocess
import urllib.request
import zipfile
from pathlib import Path, PurePosixPath
from typing import Any


def validate_source(mod: dict[str, Any]) -> None:
    patterns = {
        "repository": r"[A-Za-z0-9][A-Za-z0-9-]*/[A-Za-z0-9][A-Za-z0-9_.-]*",
        "commit": r"[0-9a-f]{40}",
        "source_sha256": r"[0-9a-f]{64}",
        "sha256": r"[0-9a-f]{64}",
        "file_name": r"[A-Za-z0-9_.+-]+\.jar",
    }
    for field, pattern in patterns.items():
        value = mod.get(field)
        if not isinstance(value, str) or not re.fullmatch(pattern, value):
            raise ValueError(f"github-source: invalid {field}")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        while chunk := source.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def extract_source(archive: Path, destination: Path) -> None:
    with zipfile.ZipFile(archive) as source:
        entries = source.infolist()
        roots = set()
        for entry in entries:
            parts = PurePosixPath(entry.filename).parts
            if not parts or entry.filename.startswith("/") or ".." in parts or "\\" in entry.filename:
                raise ValueError("github-source: unsafe ZIP path")
            roots.add(parts[0])
            if stat.S_ISLNK(entry.external_attr >> 16):
                raise ValueError("github-source: symlink in ZIP")
        if len(roots) != 1:
            raise ValueError("github-source: ZIP must have one repository directory")
        for entry in entries:
            parts = PurePosixPath(entry.filename).parts[1:]
            if not parts or entry.is_dir():
                continue
            target = destination.joinpath(*parts)
            target.parent.mkdir(parents=True, exist_ok=True)
            with source.open(entry) as data, target.open("wb") as output:
                shutil.copyfileobj(data, output)


def build_jar(mod: dict[str, Any], cache_dir: Path, *, java_home: str | None = None) -> Path:
    validate_source(mod)
    cache = cache_dir / "github-source" / mod["repository"] / mod["commit"]
    artifact = cache / "source" / "build" / "libs" / mod["file_name"]
    if artifact.is_file() and sha256(artifact) == mod["sha256"]:
        return artifact
    cache.mkdir(parents=True, exist_ok=True)
    archive = cache / "source.zip"
    if not archive.is_file() or sha256(archive) != mod["source_sha256"]:
        url = f"https://codeload.github.com/{mod['repository']}/zip/{mod['commit']}"
        request = urllib.request.Request(url, headers={"User-Agent": "DimensionWorks-fixed-source/1"})
        partial = cache / "source.zip.part"
        try:
            with urllib.request.urlopen(request, timeout=120) as source, partial.open("wb") as output:
                shutil.copyfileobj(source, output)
            if sha256(partial) != mod["source_sha256"]:
                raise ValueError(f"{mod['id']}: fixed source checksum mismatch")
            partial.replace(archive)
        finally:
            partial.unlink(missing_ok=True)
    directory = cache / "source"
    if directory.exists():
        shutil.rmtree(directory)
    directory.mkdir()
    extract_source(archive, directory)
    environment = dict(os.environ, BRANCH_NAME="", BUILD_NUMBER="")
    if java_home:
        environment["JAVA_HOME"] = java_home
    log = cache / "build.log"
    with log.open("w", encoding="utf-8") as output:
        result = subprocess.run(
            ["bash", "gradlew", "--no-daemon", "--max-workers=2", "--console=plain", "jar"],
            cwd=directory, env=environment, stdout=output, stderr=subprocess.STDOUT, timeout=900,
        )
    if result.returncode:
        raise RuntimeError(f"{mod['id']}: fixed source build failed; see {log}")
    if not artifact.is_file() or sha256(artifact) != mod["sha256"]:
        raise ValueError(f"{mod['id']}: fixed source artifact checksum mismatch; see {log}")
    return artifact
