#!/usr/bin/env python3
"""将 GitHub 最新正式版客户端、服务端完整/增量包与发行日志同步到 BBSMC。"""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request
import uuid
import zipfile

BBSMC_API = "https://api.bbsmc.net/v2"


def github_json(repository: str, endpoint: str) -> dict:
    result = subprocess.run(
        ["gh", "api", f"repos/{repository}/{endpoint}"],
        check=True, capture_output=True, text=True,
    )
    return json.loads(result.stdout)


def is_latest_release(repository: str, release: dict) -> bool:
    latest = github_json(repository, "releases/latest")
    return (
        not release["draft"]
        and not release["prerelease"]
        and not latest["draft"]
        and not latest["prerelease"]
        and release["id"] == latest["id"]
        and release["tag_name"] == latest["tag_name"]
    )


def download_asset(repository: str, tag: str, asset: dict) -> bytes:
    with tempfile.TemporaryDirectory(prefix="dimensionworks-bbsmc-") as directory:
        subprocess.run(
            ["gh", "release", "download", tag, "--repo", repository,
             "--pattern", asset["name"], "--dir", directory],
            check=True, capture_output=True, text=True,
        )
        data = (Path(directory) / asset["name"]).read_bytes()
    if len(data) != asset["size"]:
        raise ValueError(f"GitHub {asset['name']} 下载大小与发行资产不一致")
    digest = asset.get("digest")
    if digest and digest != f"sha256:{hashlib.sha256(data).hexdigest()}":
        raise ValueError(f"GitHub {asset['name']} 下载摘要与发行资产不一致")
    return data


def version_metadata(release: dict, project_id: str, files: dict[str, bytes]) -> dict:
    version = release["tag_name"][1:]
    filename = f"DimensionWorks-{version}.mrpack"
    with zipfile.ZipFile(io.BytesIO(files[filename])) as archive:
        index = json.loads(archive.read("modrinth.index.json"))
    if index["game"] != "minecraft" or index["versionId"] != version:
        raise ValueError("MRPack 游戏或版本号与 GitHub 发行版不一致")
    if "forge" not in index["dependencies"]:
        raise ValueError("DimensionWorks MRPack 缺少 Forge 依赖")
    if not isinstance(release["body"], str) or not release["body"].strip():
        raise ValueError("GitHub 发行版更新日志为空，停止同步")
    return {
        "project_id": project_id,
        "version_number": version,
        "name": release["name"],
        "changelog": release["body"],
        "game_versions": [index["dependencies"]["minecraft"]],
        "loaders": ["forge"],
        "version_type": "release",
        "featured": False,
        "status": "listed",
        "dependencies": [],
        "file_parts": list(files),
        "primary_file": filename,
        "disk_only": False,
    }


def bbsmc_json(endpoint: str, token: str, *, data: bytes | None = None,
               content_type: str = "application/json") -> dict | list | None:
    headers = {
        "User-Agent": "DimensionWorks-BBSMC-Sync/1.0 (github.com/szx-jn/DimensionWorks)",
        "Accept": "application/json",
        "Content-Type": content_type,
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(f"{BBSMC_API}/{endpoint}", data=data, headers=headers)
    try:
        with urllib.request.urlopen(request, timeout=600 if data is not None else 180) as response:
            if response.status == 204:
                return None
            return json.load(response)
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"BBSMC HTTP {error.code}: {detail}") from None


def matching_version(versions: list, metadata: dict, files: dict[str, bytes]) -> dict | None:
    matches = [v for v in versions if v["version_number"] == metadata["version_number"]]
    if not matches:
        return None
    if len(matches) == 1:
        version = matches[0]
        existing = {file["filename"]: file for file in version["files"]}
        if (
            version["project_id"] == metadata["project_id"]
            and version["changelog"] == metadata["changelog"]
            and version["status"] == "listed"
            and len(existing) == len(version["files"])
            and metadata["primary_file"] in existing
            and all(
                filename in files
                and file["hashes"].get("sha512") == hashlib.sha512(files[filename]).hexdigest()
                and file["primary"] == (filename == metadata["primary_file"])
                for filename, file in existing.items()
            )
        ):
            return version
    raise ValueError("BBSMC 已存在同版本，但文件、更新日志或列出状态不同；停止同步，不覆盖")


def already_synced(versions: list, metadata: dict, files: dict[str, bytes]) -> bool:
    version = matching_version(versions, metadata, files)
    return version is not None and len(version["files"]) == len(files)


def upload_files(metadata: dict, files: dict[str, bytes], token: str,
                 *, version_id: str | None = None) -> dict | None:
    boundary = f"dimensionworks-{uuid.uuid4().hex}"
    parts = [
        (f'--{boundary}\r\nContent-Disposition: form-data; name="data"\r\n'
         'Content-Type: application/json\r\n\r\n').encode()
        + json.dumps({} if version_id is not None else metadata, ensure_ascii=False).encode("utf-8")
    ]
    for filename, data in files.items():
        parts.extend([
            (f'\r\n--{boundary}\r\nContent-Disposition: form-data; name="{filename}"; '
             f'filename="{filename}"\r\nContent-Type: application/octet-stream\r\n\r\n').encode(),
            data,
        ])
    parts.append(f"\r\n--{boundary}--\r\n".encode())
    # 创建/补附件均只提交一次；重跑前先核对已有文件，避免重复上传。
    return bbsmc_json(
        "version" if version_id is None else f"version/{version_id}/file",
        token, data=b"".join(parts),
        content_type=f"multipart/form-data; boundary={boundary}",
    )


def sync_release(repository: str, tag: str, project_id: str, token: str,
                 *, dry_run: bool = False) -> str:
    if not re.fullmatch(r"v[0-9]+\.[0-9]+\.[0-9]+", tag):
        raise ValueError("只同步 vX.Y.Z 格式的正式发行版")
    release = github_json(repository, f"releases/tags/{tag}")
    if release["tag_name"] != tag:
        raise ValueError("GitHub 返回的发行标签与本次发布不一致")
    if not is_latest_release(repository, release):
        return f"BBSMC SKIP: {tag} 不是 GitHub 最新正式版"
    if not token and not dry_run:
        raise ValueError("未配置 GitHub Actions Secret BBSMC_TOKEN")
    version = tag[1:]
    files = {}
    for filename in (
        f"DimensionWorks-{version}.mrpack",
        f"DimensionWorks-Server-{version}.zip",
        f"DimensionWorks-Server-{version}-delta.zip",
    ):
        assets = [asset for asset in release["assets"] if asset["name"] == filename]
        if len(assets) != 1 or assets[0]["state"] != "uploaded":
            raise ValueError(f"GitHub 发行版缺少唯一且已上传的 {filename}")
        files[filename] = download_asset(repository, tag, assets[0])
    metadata = version_metadata(release, project_id, files)
    versions = bbsmc_json(f"project/{project_id}/version", token)
    existing = matching_version(versions, metadata, files)
    if existing is not None and len(existing["files"]) == len(files):
        return f"BBSMC OK: {tag} 文件与更新日志已同步，无需重复上传"
    if dry_run:
        return f"BBSMC DRY-RUN OK: {tag} 已核对客户端、服务端完整/增量包与发行日志，未上传"
    # 下载期间可能已有更晚版本完成发布；写入前再次确认最新正式版身份。
    if not is_latest_release(repository, release):
        return f"BBSMC SKIP: {tag} 已被更新的 GitHub 正式版替代"
    if existing is None:
        uploaded = upload_files(metadata, files, token)
    else:
        names = {file["filename"] for file in existing["files"]}
        missing = {filename: data for filename, data in files.items() if filename not in names}
        upload_files(metadata, missing, token, version_id=existing["id"])
        uploaded = bbsmc_json(f"version/{existing['id']}", token)
    if not already_synced([uploaded], metadata, files):
        raise ValueError("BBSMC 返回的版本、文件或更新日志与本次上传不一致")
    return f"BBSMC OK: {tag} 客户端、服务端完整/增量包与发行日志已同步（{uploaded['id']}）"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tag", required=True, help="正式发布工作流成功创建的 GitHub 标签")
    parser.add_argument("--dry-run", action="store_true", help="只读取并校验，不上传版本")
    args = parser.parse_args()
    try:
        print(sync_release(
            os.environ["GITHUB_REPOSITORY"], args.tag,
            os.environ["BBSMC_PROJECT_ID"], os.environ.get("BBSMC_TOKEN", ""),
            dry_run=args.dry_run,
        ))
    except (KeyError, ValueError, RuntimeError, OSError, zipfile.BadZipFile,
            subprocess.CalledProcessError) as error:
        if isinstance(error, subprocess.CalledProcessError):
            message = error.stderr.strip()
        else:
            message = str(error)
        print(f"BBSMC FAIL: {message}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
