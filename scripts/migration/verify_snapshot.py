#!/usr/bin/env python3
"""校验冷备份，并将逐文件校验通过的副本原子交付到新目录。"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import tarfile


class SnapshotError(ValueError):
    pass


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for data in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(data)
    return digest.hexdigest()


def relative_path(name: str) -> str:
    if name.startswith("./"):
        name = name[2:]
    path = PurePosixPath(name)
    if (not name or path.is_absolute() or "\\" in name
            or any(part in ("", ".", "..") for part in name.split("/"))):
        raise SnapshotError(f"备份路径不合法：{name!r}")
    return name


def verify_and_extract(snapshot: Path, destination: Path) -> dict:
    report = json.loads((snapshot / "complete.json").read_text())
    if report.get("source_unchanged") is not True or report.get("cold_checked_before_and_after") is not True:
        raise SnapshotError("缺少冷备份及源文件未变更的证据")
    inventory_path = snapshot / "inventory.json"
    if sha256(inventory_path) != report["inventory_sha256"]:
        raise SnapshotError("文件清单 SHA-256 不匹配")
    rows = json.loads(inventory_path.read_text())
    entries = {}
    case_paths = set()
    for row in rows:
        name = relative_path(row["path"])
        if name in entries or name.casefold() in case_paths:
            raise SnapshotError(f"文件清单有重复或大小写冲突：{name}")
        if type(row["size"]) is not int or row["size"] < 0:
            raise SnapshotError(f"文件大小不合法：{name}")
        entries[name] = row
        case_paths.add(name.casefold())
    if len(entries) != report["file_count"] or sum(row["size"] for row in rows) != report["payload_bytes"]:
        raise SnapshotError("文件清单总数或总大小不匹配")
    archive_name = relative_path(report["archive"])
    if "/" in archive_name:
        raise SnapshotError("压缩包必须位于快照目录内")
    archive = snapshot / archive_name
    if archive.stat().st_size != report["archive_bytes"] or sha256(archive) != report["archive_sha256"]:
        raise SnapshotError("压缩包大小或 SHA-256 不匹配")

    partial = destination.with_name(destination.name + ".partial")
    if destination.exists() or partial.exists():
        raise SnapshotError("目标或未完成目录已存在，拒绝覆盖")
    partial.mkdir(parents=True, mode=0o700)
    seen = set()
    # 不使用 extractall：只写入清单内普通文件，拒绝链接与逃逸路径。
    with tarfile.open(archive, mode="r|gz") as stream:
        for member in stream:
            if member.isdir():
                if member.name not in (".", "./"):
                    relative_path(member.name.rstrip("/"))
                continue
            name = relative_path(member.name)
            if not member.isfile():
                raise SnapshotError(f"压缩包包含非普通文件：{name}")
            if name not in entries or name in seen:
                raise SnapshotError(f"压缩包包含未登记或重复文件：{name}")
            expected = entries[name]
            if member.size != expected["size"]:
                raise SnapshotError(f"文件大小不匹配：{name}")
            target = partial / name
            target.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
            digest = hashlib.sha256()
            with stream.extractfile(member) as source, target.open("xb") as output:
                for data in iter(lambda: source.read(1024 * 1024), b""):
                    digest.update(data)
                    output.write(data)
            target.chmod(0o600)
            if digest.hexdigest() != expected["sha256"]:
                raise SnapshotError(f"文件 SHA-256 不匹配：{name}")
            seen.add(name)
    if seen != entries.keys():
        raise SnapshotError(f"压缩包缺少 {len(entries.keys() - seen)} 个文件")
    if (partial / "VERSION").read_text().strip() != report["source_version"]:
        raise SnapshotError("备份版本与完成报告不匹配")
    partial.rename(destination)
    return {key: report[key] for key in ("source_version", "archive_sha256", "file_count", "payload_bytes")}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("snapshot", type=Path, help="含 complete.json、inventory.json 和压缩包的目录")
    parser.add_argument("destination", type=Path, help="尚不存在的解包目录；失败只留下 .partial 隔离目录")
    args = parser.parse_args()
    try:
        result = verify_and_extract(args.snapshot.resolve(), args.destination.resolve())
    except (SnapshotError, OSError, KeyError, json.JSONDecodeError, tarfile.TarError) as error:
        parser.exit(1, f"备份校验失败：{error}\n")
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
