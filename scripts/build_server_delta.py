#!/usr/bin/env python3
"""Create a client or server update delta from two DimensionWorks packages."""

from __future__ import annotations

import argparse
import hashlib
import stat
import tempfile
import zipfile
from pathlib import Path, PurePosixPath

import pack_manifest


class DeltaError(ValueError):
    """Raised when an archive cannot produce a safe delta."""


def _safe_member(name: str) -> None:
    if not name or name.startswith("/") or "\\" in name:
        raise DeltaError(f"unsafe archive member: {name!r}")
    path = PurePosixPath(name)
    if path.is_absolute() or path.as_posix() != name or any(
        part in ("", ".", "..") for part in path.parts
    ):
        raise DeltaError(f"unsafe archive member: {name!r}")


def _members(archive: zipfile.ZipFile) -> dict[str, zipfile.ZipInfo]:
    result: dict[str, zipfile.ZipInfo] = {}
    for info in archive.infolist():
        if info.is_dir():
            continue
        _safe_member(info.filename)
        mode = info.external_attr >> 16
        if stat.S_ISLNK(mode):
            raise DeltaError(f"symbolic links are not allowed: {info.filename}")
        if info.filename in result:
            raise DeltaError(f"duplicate archive member: {info.filename}")
        result[info.filename] = info
    return result


def _digest(archive: zipfile.ZipFile, info: zipfile.ZipInfo, algorithm: str) -> str:
    digest = hashlib.new(algorithm)
    with archive.open(info) as source:
        while chunk := source.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def build_delta(
    base_zip: Path,
    new_zip: Path,
    output: Path,
    *,
    kind: str,
    manifest: pack_manifest.UpdateManifest,
) -> Path:
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(base_zip) as base, zipfile.ZipFile(new_zip) as new:
        base_members = _members(base)
        new_members = _members(new)
        update_manifest = manifest
        if update_manifest.kind != kind:
            raise DeltaError(f"delta manifest must be a {kind} manifest")
        changed: list[str] = []
        for entry in update_manifest.entries:
            if kind == "server" and entry.action != "archive":
                raise DeltaError(f"server delta entry must be archive: {entry.path}")
            if kind == "server" and entry.source != entry.path:
                raise DeltaError(f"server delta source must match path: {entry.path}")
            if kind == "client" and entry.action == "remote":
                continue
            source_name = entry.source
            _safe_member(source_name)
            new_info = new_members.get(source_name)
            if new_info is None:
                raise DeltaError(f"new package is missing manifest file: {source_name}")
            algorithm = "sha512" if entry.algorithm == "sha512" else "sha256"
            if _digest(new, new_info, algorithm) != entry.value:
                raise DeltaError(f"new package digest mismatch: {entry.path}")
            old_info = base_members.get(source_name)
            if old_info is None or _digest(base, old_info, algorithm) != entry.value:
                changed.append(source_name)

        with tempfile.TemporaryDirectory(prefix="dimensionworks-delta-") as temp:
            staging = Path(temp)
            for relative in changed:
                source = new_members[relative]
                target = staging / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(new.read(source))
                mode = source.external_attr >> 16
                if mode & 0o111:
                    target.chmod(target.stat().st_mode | 0o111)

            temporary = output.with_suffix(output.suffix + ".part")
            temporary.unlink(missing_ok=True)
            with zipfile.ZipFile(
                temporary, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6
            ) as delta:
                for relative in changed:
                    source = staging / relative
                    info = zipfile.ZipInfo.from_file(source, relative)
                    info.compress_type = zipfile.ZIP_DEFLATED
                    if source.stat().st_mode & 0o111:
                        info.external_attr = (0o100755 & 0xFFFF) << 16
                    delta.writestr(info, source.read_bytes(), compress_type=zipfile.ZIP_DEFLATED)
            temporary.replace(output)
    return output


def build_server_delta(
    base_zip: Path, new_zip: Path, output: Path, manifest: pack_manifest.UpdateManifest
) -> Path:
    return build_delta(base_zip, new_zip, output, kind="server", manifest=manifest)


def build_client_delta(
    base_zip: Path, new_zip: Path, output: Path, manifest: pack_manifest.UpdateManifest
) -> Path:
    return build_delta(base_zip, new_zip, output, kind="client", manifest=manifest)


def write_empty_delta(output: Path) -> Path:
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = output.with_suffix(output.suffix + ".part")
    temporary.unlink(missing_ok=True)
    with zipfile.ZipFile(temporary, "w"):
        pass
    temporary.replace(output)
    return output


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", type=Path)
    parser.add_argument("--new", type=Path, required=True)
    parser.add_argument("--manifest", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--kind", choices=("client", "server"), default="server")
    parser.add_argument("--empty", action="store_true")
    args = parser.parse_args()
    try:
        if args.empty:
            if args.base is not None or args.manifest is not None:
                raise DeltaError("--empty cannot be combined with --base or --manifest")
            write_empty_delta(args.output)
        else:
            if args.base is None or args.manifest is None:
                raise DeltaError("--base and --manifest are required unless --empty is used")
            update_manifest = pack_manifest.parse_manifest(
                args.manifest.read_bytes(), expected_kind=args.kind
            )
            build_delta(args.base, args.new, args.output, kind=args.kind, manifest=update_manifest)
    except (OSError, ValueError, zipfile.BadZipFile, DeltaError) as error:
        print(f"DELTA FAIL: {error}")
        return 1
    print(f"DELTA OK: {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
