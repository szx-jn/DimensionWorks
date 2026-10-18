#!/usr/bin/env python3
"""Build and validate DimensionWorks standalone update manifests."""

from __future__ import annotations

import hashlib
import re
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
from typing import Iterable
from urllib.parse import urlsplit

try:  # Support both `python -m unittest` and sibling imports from packaging scripts.
    from . import pack_version
except ImportError:  # pragma: no cover - exercised by direct sibling imports
    import pack_version

MANIFEST_NAME = "dimensionworks-update-manifest.tsv"
MANIFEST_HEADER = "# DimensionWorks update manifest 1"
COLUMN_HEADER = "action\tpath\talgorithm\tvalue\tsource"
KINDS = frozenset({"client", "server"})
ACTIONS = frozenset({"remote", "archive"})
ALGORITHMS_BY_ACTION = {
    "remote": ("sha512", 128),
    "archive": ("sha256", 64),
}
UPDATER_SCRIPT_NAMES = frozenset(
    {
        "update-dimensionworks-client.sh",
        "update-dimensionworks-client.bat",
        "update-dimensionworks-server.sh",
        "update-dimensionworks-server.bat",
    }
)
DIGEST_PATTERN = re.compile(r"^[0-9a-f]+$")


class ManifestError(ValueError):
    """Raised when an update manifest violates the canonical format."""


@dataclass(frozen=True)
class ManifestEntry:
    action: str
    path: str
    algorithm: str
    value: str
    source: str


@dataclass(frozen=True)
class UpdateManifest:
    kind: str
    version: str
    release: str
    base: str
    entries: tuple[ManifestEntry, ...]


def _validate_version(version: str, field: str) -> None:
    try:
        pack_version.PackVersion.parse(version)
    except pack_version.VersionRuleError as error:
        raise ManifestError(f"{field} must use x.x.x format: {version!r}") from error


def _validate_release(value: str, field: str, expected_version: str | None = None) -> None:
    if value == "none" and field == "base":
        return
    if not value.startswith("v"):
        raise ManifestError(f"{field} must be a v-prefixed release tag: {value!r}")
    version = value[1:]
    _validate_version(version, field)
    if expected_version is not None and version != expected_version:
        raise ManifestError(
            f"{field} {value!r} does not match manifest version {expected_version!r}"
        )


def _validate_relative_path(value: str, field: str) -> str:
    if not value or value in (".", "..") or value != value.strip():
        raise ManifestError(f"{field} must be a non-empty trimmed path: {value!r}")
    if "\\" in value:
        raise ManifestError(f"{field} must use POSIX separators: {value!r}")
    if any(ord(character) < 32 or ord(character) == 127 for character in value):
        raise ManifestError(f"{field} contains a control character: {value!r}")
    if value.startswith("/") or ":" in value:
        raise ManifestError(f"{field} must be a relative path: {value!r}")

    path = PurePosixPath(value)
    if path.is_absolute() or value != path.as_posix():
        raise ManifestError(f"{field} is not a canonical relative path: {value!r}")
    if any(part in ("", ".", "..") for part in path.parts):
        raise ManifestError(f"{field} contains an unsafe path segment: {value!r}")
    return value


def _validate_remote_source(source: str) -> None:
    if any(character.isspace() or ord(character) < 32 or ord(character) == 127 for character in source):
        raise ManifestError(f"remote source contains whitespace or control characters: {source!r}")
    parsed = urlsplit(source)
    if parsed.scheme not in ("http", "https") or not parsed.netloc:
        raise ManifestError(f"remote source must be an HTTP(S) URL: {source!r}")


def _validate_entry(entry: ManifestEntry) -> None:
    if entry.action not in ACTIONS:
        raise ManifestError(f"unknown action: {entry.action!r}")
    _validate_relative_path(entry.path, "path")

    expected_algorithm, digest_length = ALGORITHMS_BY_ACTION[entry.action]
    if entry.algorithm != expected_algorithm:
        raise ManifestError(
            f"algorithm mismatch: {entry.action} entry must use "
            f"{expected_algorithm}, got {entry.algorithm!r}"
        )
    if (
        len(entry.value) != digest_length
        or DIGEST_PATTERN.fullmatch(entry.value) is None
    ):
        raise ManifestError(
            f"{entry.path}: invalid {entry.algorithm} digest {entry.value!r}"
        )

    if entry.action == "remote":
        _validate_remote_source(entry.source)
    else:
        _validate_relative_path(entry.source, "source")


def _validate_manifest(manifest: UpdateManifest) -> None:
    if manifest.kind not in KINDS:
        raise ManifestError(f"unknown manifest kind: {manifest.kind!r}")
    _validate_version(manifest.version, "version")
    _validate_release(manifest.release, "release", manifest.version)
    _validate_release(manifest.base, "base")

    seen_paths: set[str] = set()
    for entry in manifest.entries:
        _validate_entry(entry)
        normalized = entry.path.casefold()
        if normalized in seen_paths:
            raise ManifestError(f"duplicate path: {entry.path}")
        seen_paths.add(normalized)


def _decode_manifest(data: bytes) -> str:
    if not isinstance(data, bytes):
        raise ManifestError("manifest input must be bytes")
    if data.startswith(b"\xef\xbb\xbf"):
        raise ManifestError("manifest must not contain a UTF-8 BOM")
    if b"\r" in data:
        raise ManifestError("manifest must use LF line endings")
    try:
        return data.decode("utf-8")
    except UnicodeDecodeError as error:
        raise ManifestError("manifest must be valid UTF-8") from error


def _parse_header(lines: list[str]) -> tuple[str, str, str, str]:
    if not lines or lines[0] != MANIFEST_HEADER:
        raise ManifestError(f"missing manifest header: {MANIFEST_HEADER!r}")
    headers: dict[str, str] = {}
    for line in lines[1:5]:
        if not line.startswith("# "):
            raise ManifestError(f"invalid manifest header: {line!r}")
        key, separator, value = line[2:].partition(" ")
        if not separator or not value or " " in value:
            raise ManifestError(f"invalid manifest header: {line!r}")
        if key not in ("kind", "version", "release", "base"):
            raise ManifestError(f"unknown manifest header: {key!r}")
        if key in headers:
            raise ManifestError(f"duplicate manifest header: {key!r}")
        headers[key] = value
    if set(headers) != {"kind", "version", "release", "base"}:
        raise ManifestError("manifest headers must be kind, version, release and base")
    return headers["kind"], headers["version"], headers["release"], headers["base"]


def parse_manifest(data: bytes, expected_kind: str | None = None) -> UpdateManifest:
    text = _decode_manifest(data)
    if not text.endswith("\n"):
        raise ManifestError("manifest must end with one LF")
    lines = text[:-1].split("\n")
    if len(lines) < 6 or any(line == "" for line in lines):
        raise ManifestError("manifest contains an empty line")
    kind, version, release, base = _parse_header(lines)
    if expected_kind is not None and kind != expected_kind:
        raise ManifestError(
            f"expected kind {expected_kind!r} manifest, got {kind!r}"
        )
    if lines[5] != COLUMN_HEADER:
        raise ManifestError(f"invalid column header: {lines[5]!r}")

    entries: list[ManifestEntry] = []
    for line_number, line in enumerate(lines[6:], start=7):
        fields = line.split("\t")
        if len(fields) != 5:
            raise ManifestError(
                f"line {line_number}: expected 5 tab-separated fields, got {len(fields)}"
            )
        entry = ManifestEntry(*fields)
        try:
            _validate_entry(entry)
        except ManifestError as error:
            raise ManifestError(f"line {line_number}: {error}") from error
        entries.append(entry)

    manifest = UpdateManifest(kind, version, release, base, tuple(entries))
    _validate_manifest(manifest)
    return manifest


def render_manifest(manifest: UpdateManifest) -> bytes:
    _validate_manifest(manifest)
    lines = [
        MANIFEST_HEADER,
        f"# kind {manifest.kind}",
        f"# version {manifest.version}",
        f"# release {manifest.release}",
        f"# base {manifest.base}",
        COLUMN_HEADER,
        *(
            "\t".join(
                (entry.action, entry.path, entry.algorithm, entry.value, entry.source)
            )
            for entry in manifest.entries
        ),
    ]
    return ("\n".join(lines) + "\n").encode("utf-8")


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        while chunk := source.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def _assert_regular_file(path: Path) -> None:
    if path.is_symlink():
        raise ManifestError(f"symbolic link is not allowed: {path}")
    if not path.is_file():
        raise ManifestError(f"manifest source is not a regular file: {path}")


def _client_index_entry(index_entry: dict[str, object]) -> ManifestEntry:
    path = index_entry.get("path")
    hashes = index_entry.get("hashes")
    downloads = index_entry.get("downloads")
    if not isinstance(path, str):
        raise ManifestError("client index entry is missing path")
    if not isinstance(hashes, dict):
        raise ManifestError(f"{path}: client index entry is missing hashes")
    sha512 = hashes.get("sha512")
    if not isinstance(sha512, str):
        raise ManifestError(f"{path}: client index entry is missing SHA-512")
    if not isinstance(downloads, list) or not downloads:
        raise ManifestError(f"{path}: client index entry has no download URL")
    url = downloads[0]
    if not isinstance(url, str):
        raise ManifestError(f"{path}: client download URL must be a string")
    return ManifestEntry(
        action="remote",
        path=path,
        algorithm="sha512",
        value=sha512.lower(),
        source=url,
    )


def make_client_manifest(
    index_entries: list[dict[str, object]],
    staging_root: Path,
    version: str,
    release: str,
    base: str | None = None,
) -> UpdateManifest:
    resolved_base = f"v{version}" if base is None else base
    entries = [_client_index_entry(index_entry) for index_entry in index_entries]
    index_path = staging_root / "modrinth.index.json"
    _assert_regular_file(index_path)
    entries.append(
        ManifestEntry(
            action="archive",
            path="modrinth.index.json",
            algorithm="sha256",
            value=sha256_file(index_path),
            source="modrinth.index.json",
        )
    )

    overrides_root = staging_root / "overrides"
    if overrides_root.is_symlink():
        raise ManifestError(f"symbolic link is not allowed: {overrides_root}")
    if overrides_root.exists():
        for path in sorted(overrides_root.rglob("*")):
            if path.is_symlink():
                raise ManifestError(f"symbolic link is not allowed: {path}")
            if not path.is_file():
                continue
            source = path.relative_to(staging_root).as_posix()
            target = path.relative_to(overrides_root).as_posix()
            if target == "VERSION":
                continue  # The updater manages VERSION transactionally as metadata.
            entries.append(
                ManifestEntry(
                    action="archive",
                    path=target,
                    algorithm="sha256",
                    value=sha256_file(path),
                    source=source,
                )
            )

    manifest = UpdateManifest(
        kind="client",
        version=version,
        release=release,
        base=resolved_base,
        entries=tuple(sorted(entries, key=lambda entry: entry.path)),
    )
    _validate_manifest(manifest)
    return manifest


def make_server_manifest(
    staging_root: Path,
    version: str,
    release: str,
    base: str | None = None,
) -> UpdateManifest:
    resolved_base = f"v{version}" if base is None else base
    entries: list[ManifestEntry] = []
    if staging_root.is_symlink() or not staging_root.is_dir():
        raise ManifestError(f"server staging root is not a directory: {staging_root}")
    for path in sorted(staging_root.rglob("*")):
        if path.is_symlink():
            raise ManifestError(f"symbolic link is not allowed: {path}")
        if not path.is_file():
            continue
        relative = path.relative_to(staging_root)
        if relative.as_posix() in (MANIFEST_NAME, "VERSION"):
            continue  # VERSION is managed separately by the updater transaction.
        source = relative.as_posix()
        entries.append(
            ManifestEntry(
                action="archive",
                path=source,
                algorithm="sha256",
                value=sha256_file(path),
                source=source,
            )
        )

    manifest = UpdateManifest(
        kind="server",
        version=version,
        release=release,
        base=resolved_base,
        entries=tuple(entries),
    )
    _validate_manifest(manifest)
    return manifest


def validate_no_updater_scripts(archive_paths: Iterable[str]) -> None:
    for path in archive_paths:
        try:
            normalized = _validate_relative_path(path, "archive path")
        except ManifestError as error:
            raise ManifestError(f"unsafe archive path: {error}") from error
        name = PurePosixPath(normalized).name.casefold()
        if name in UPDATER_SCRIPT_NAMES:
            raise ManifestError(f"updater scripts must not be embedded: {normalized}")
