#!/usr/bin/env python3
"""Build a complete DimensionWorks NeoForge server pack from manifest/mods.json."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path
from typing import Any

sys.path.insert(0, str(Path(__file__).resolve().parent))
import pack_hook  # noqa: E402  (sibling packaging preflight hook)
import pack_manifest  # noqa: E402  (sibling update manifest helpers)
import package_mrpack  # noqa: E402  (sibling shared manifest download helpers)
import pack_version  # noqa: E402  (sibling release version rules)


ROOT = package_mrpack.ROOT
MANIFEST = package_mrpack.MANIFEST
DEFAULT_INSTANCE_DIR = package_mrpack.DEFAULT_INSTANCE_DIR
DEFAULT_CACHE_DIR = Path("/private/tmp/dimensionworks-pack-cache")
NEOFORGE_VERSION = package_mrpack.NEOFORGE_VERSION
USER_AGENT = "DimensionWorks-Server-Packager/0.1"
SERVER_OVERRIDE_DIRS = ("config", "defaultconfigs", "kubejs")
IGNORED_FILES = frozenset({".DS_Store", "README", "README.md", "__pycache__"})
INSTALL_TIMEOUT_SECONDS = 1200
INSTALLER_URL = (
    "https://maven.neoforged.net/releases/net/neoforged/neoforge/"
    f"{NEOFORGE_VERSION}/neoforge-{NEOFORGE_VERSION}-installer.jar"
)

SERVER_PROPERTIES = """\
allow-flight=true
allow-nether=true
broadcast-console-to-ops=true
broadcast-rcon-to-ops=true
difficulty=normal
enable-command-block=true
enable-query=false
enable-rcon=false
enable-status=true
enforce-secure-profile=false
entity-broadcast-range-percentage=75
force-gamemode=false
function-permission-level=2
gamemode=survival
generate-structures=true
hardcore=false
hide-online-players=false
level-name=world
level-seed=
level-type=minecraft\\:normal
log-ips=true
max-build-height=320
max-players=20
max-tick-time=-1
max-world-size=29999984
motd=DimensionWorks
network-compression-threshold=256
online-mode=true
op-permission-level=4
player-idle-timeout=0
prevent-proxy-connections=false
pvp=true
query.port=25565
rate-limit=0
require-resource-pack=false
resource-pack=
resource-pack-prompt=
resource-pack-sha1=
server-ip=
server-port=25565
simulation-distance=6
spawn-animals=true
spawn-monsters=true
spawn-npcs=true
spawn-protection=0
sync-chunk-writes=false
view-distance=8
white-list=false
"""

USER_JVM_ARGS = """\
-Xms2G
-Xmx6G
-XX:+UseG1GC
-XX:+ParallelRefProcEnabled
-XX:MaxGCPauseMillis=200
-XX:+UnlockExperimentalVMOptions
-XX:+DisableExplicitGC
-XX:+AlwaysPreTouch
-XX:G1NewSizePercent=30
-XX:G1MaxNewSizePercent=40
-XX:G1HeapRegionSize=8M
-XX:G1ReservePercent=20
-XX:G1HeapWastePercent=5
-XX:G1MixedGCCountTarget=4
-XX:InitiatingHeapOccupancyPercent=15
-XX:G1MixedGCLiveThresholdPercent=90
-XX:G1RSetUpdatingPauseTimePercent=5
-XX:SurvivorRatio=32
-XX:+PerfDisableSharedMem
-XX:MaxTenuringThreshold=1
-Dfile.encoding=UTF-8
-Djava.net.preferIPv4Stack=true
"""

START_SH = f"""\
#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${{BASH_SOURCE[0]}}")"
exec java -Dauthlibinjector.ignoredPackages=cn.alini.trueuuid. "-Ddimensionworks.instanceDir=$PWD" @user_jvm_args.txt @libraries/net/neoforged/neoforge/{NEOFORGE_VERSION}/unix_args.txt nogui "$@"
"""

START_BAT = f"""\
@echo off
setlocal
cd /d "%~dp0"
java -Dauthlibinjector.ignoredPackages=cn.alini.trueuuid. "-Ddimensionworks.instanceDir=%CD%" @user_jvm_args.txt @libraries\\net\\neoforged\\neoforge\\{NEOFORGE_VERSION}\\win_args.txt nogui %*
"""


def read_text(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read().decode("utf-8").strip()


def download_file(url: str, destination: Path) -> None:
    curl = shutil.which("curl")
    if curl:
        process = subprocess.run(
            [
                curl,
                "-L",
                "--fail",
                "--silent",
                "--show-error",
                "--retry",
                "5",
                "--retry-delay",
                "2",
                "--connect-timeout",
                "30",
                "--output",
                str(destination),
                url,
            ],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
        )
        if process.returncode != 0:
            raise RuntimeError(f"download failed: {url}\n{process.stdout.strip()}")
        return

    with urllib.request.urlopen(
        urllib.request.Request(url, headers={"User-Agent": USER_AGENT}),
        timeout=600,
    ) as response, destination.open("wb") as output:
        shutil.copyfileobj(response, output)


def file_hashes(path: Path) -> tuple[str, str]:
    sha1 = hashlib.sha1()
    sha512 = hashlib.sha512()
    with path.open("rb") as source:
        while chunk := source.read(1024 * 1024):
            sha1.update(chunk)
            sha512.update(chunk)
    return sha1.hexdigest(), sha512.hexdigest()


def cached_download(
    cache_dir: Path,
    source: str,
    filename: str,
    url: str,
    *,
    sha1: str = "",
    sha512: str = "",
    verbose: bool = False,
) -> Path:
    destination = cache_dir / source / filename
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.is_file() and destination.stat().st_size:
        actual_sha1, actual_sha512 = file_hashes(destination)
        if (not sha1 or actual_sha1 == sha1) and (not sha512 or actual_sha512 == sha512):
            return destination

    if verbose:
        print(f"download: {url}")
    temporary = destination.with_suffix(destination.suffix + ".part")
    temporary.unlink(missing_ok=True)
    download_file(url, temporary)
    actual_sha1, actual_sha512 = file_hashes(temporary)
    if sha1 and actual_sha1 != sha1:
        temporary.unlink(missing_ok=True)
        raise ValueError(f"{filename}: sha1 mismatch")
    if sha512 and actual_sha512 != sha512:
        temporary.unlink(missing_ok=True)
        raise ValueError(f"{filename}: sha512 mismatch")
    temporary.replace(destination)
    return destination


def find_java(explicit: str | None) -> str:
    candidates: list[str] = []
    if explicit:
        if not Path(explicit).is_file():
            raise FileNotFoundError(f"Java executable not found: {explicit}")
        candidates.append(explicit)
    if not explicit and (java_home := os.environ.get("JAVA_HOME")):
        candidates.append(str(Path(java_home) / "bin" / "java"))
    if not explicit and sys.platform == "darwin":
        try:
            detected = subprocess.check_output(
                ["/usr/libexec/java_home", "-v", "21"],
                stderr=subprocess.DEVNULL,
                text=True,
            ).strip()
        except (OSError, subprocess.CalledProcessError):
            pass
        else:
            candidates.append(str(Path(detected) / "bin" / "java"))
    if not explicit:
        candidates.extend([
            "/usr/lib/jvm/java-21-openjdk/bin/java",
            "/usr/lib/jvm/java-21-openjdk-amd64/bin/java",
            shutil.which("java") or "",
        ])
    rejected: list[str] = []
    for candidate in candidates:
        if not candidate or not Path(candidate).is_file():
            continue
        result = subprocess.run(
            [candidate, "-version"], stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT, text=True, timeout=10,
        )
        version = re.search(r'version "(\d+)', result.stdout)
        if result.returncode == 0 and version and int(version.group(1)) >= 21:
            return candidate
        rejected.append(candidate)
        if explicit:
            raise ValueError(f"Java 21 or newer required: {candidate}")
    raise FileNotFoundError("Java 21 not found; pass --java /path/to/java; rejected: " + ", ".join(rejected))


def copy_tree(source: Path, destination: Path) -> int:
    if not source.is_dir():
        raise FileNotFoundError(f"missing required directory: {source}")
    copied = 0
    for path in sorted(source.rglob("*")):
        relative = path.relative_to(source)
        if any(part in IGNORED_FILES for part in relative.parts) or not path.is_file():
            continue
        target = destination / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, target)
        copied += 1
    return copied


def resolve_server_mod(
    mod: dict[str, Any],
    staging: Path,
    cache_dir: Path,
    *,
    verbose: bool,
) -> bool:
    source = mod["source"]
    entry: dict[str, Any]
    cached_path: Path
    if source == "modrinth":
        entry = package_mrpack.resolve_modrinth(mod)
        cached_path = cached_download(
            cache_dir,
            "modrinth",
            Path(entry["path"]).name,
            entry["downloads"][0],
            sha1=entry["hashes"]["sha1"],
            sha512=entry["hashes"]["sha512"],
            verbose=verbose,
        )
    elif source == "curseforge":
        entry = package_mrpack.resolve_curseforge(mod, cache_dir)
        cached_path = cached_download(
            cache_dir,
            "curseforge",
            Path(entry["path"]).name,
            entry["downloads"][0],
            sha1=entry["hashes"]["sha1"],
            sha512=entry["hashes"]["sha512"],
            verbose=verbose,
        )
    elif source == "github-release":
        entry = package_mrpack.resolve_github(mod, cache_dir)
        cached_path = cached_download(
            cache_dir,
            "github",
            Path(entry["path"]).name,
            entry["downloads"][0],
            sha1=entry["hashes"]["sha1"],
            sha512=entry["hashes"]["sha512"],
            verbose=verbose,
        )
    elif source in {"in-repo", "github-source"}:
        cached_path = package_mrpack.in_repo_jar(mod) if source == "in-repo" else package_mrpack.fixed_source.build_jar(mod, cache_dir)
        entry = {"path": f"mods/{cached_path.name}"}
    else:
        raise ValueError(f"{mod['id']}: unsupported source {source!r}")

    destination = staging / entry["path"]
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(cached_path, destination)
    return source in {"in-repo", "github-source"}


def install_neoforge(staging: Path, cache_dir: Path, java: str, *, verbose: bool) -> None:
    installer = cached_download(
        cache_dir,
        "neoforge",
        f"neoforge-{NEOFORGE_VERSION}-installer.jar",
        INSTALLER_URL,
        sha1=read_text(INSTALLER_URL + ".sha1"),
        verbose=verbose,
    )
    if verbose:
        print(f"install NeoForge {NEOFORGE_VERSION}: {staging}")
    process = subprocess.run(
        [java, "-Xmx512M", "-XX:ActiveProcessorCount=2", "-jar", str(installer), "--installServer", str(staging)],
        cwd=str(staging),
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        timeout=INSTALL_TIMEOUT_SECONDS,
    )
    if process.returncode != 0:
        tail = process.stdout.splitlines()[-40:]
        raise RuntimeError("NeoForge installer failed:\n" + "\n".join(tail))

    neoforge_root = (
        staging / "libraries" / "net" / "neoforged" / "neoforge" / NEOFORGE_VERSION
    )
    required = (
        staging / "run.sh", staging / "run.bat",
        neoforge_root / "unix_args.txt", neoforge_root / "win_args.txt",
    )
    missing = [str(path) for path in required if not path.is_file()]
    if missing:
        raise FileNotFoundError("NeoForge installer did not create: " + ", ".join(missing))


def write_server_files(staging: Path, *, pack_version_value: str, servers_mods: int) -> None:
    (staging / "VERSION").write_text(f"{pack_version_value}\n", encoding="utf-8")
    (staging / "server.properties").write_text(SERVER_PROPERTIES, encoding="utf-8")
    (staging / "eula.txt").write_text(
        "# Set eula=true only after you agree to the Minecraft EULA:\n"
        "# https://aka.ms/MinecraftEULA\n"
        "eula=false\n",
        encoding="utf-8",
    )
    (staging / "user_jvm_args.txt").write_text(USER_JVM_ARGS, encoding="utf-8")
    (staging / "start.sh").write_text(START_SH, encoding="utf-8")
    (staging / "start.bat").write_text(START_BAT, encoding="utf-8")
    readme = f"""\
# DimensionWorks 服务端

- Minecraft 1.21.1
- NeoForge {NEOFORGE_VERSION}
- Java 21
- 整合包版本 {pack_version_value}
- 服务端 Mod {servers_mods} 个

## 启动

首次启动前，阅读并同意 Minecraft EULA 后把 `eula.txt` 改为 `eula=true`。

Linux：

```sh
bash start.sh
```

Windows：

```bat
start.bat
```

开服面板的启动命令填写：

```sh
bash start.sh
```

工作目录必须设置为解压后的服务端根目录。默认分配 2G 初始内存和 6G 最大内存；如果面板内存不足，请修改 `user_jvm_args.txt` 中的 `-Xms` 与 `-Xmx`。

## 配置

- `mods/`：服务端 Mod，仅包含清单中标记为 `both` 或 `server` 的条目。
- `config/`、`defaultconfigs/`：整合包配置。
- `kubejs/`：配方、标签、服务端脚本与数据。
- `server.properties`：默认服务端设置，可在开服后自行修改。
- `libraries/`：固定 NeoForge {NEOFORGE_VERSION} 的运行库，已经预装，不需要再次运行安装器。

这个压缩包不包含世界存档、日志、RCON 密码、玩家名单或其他运行时私有数据。
"""
    (staging / "README.md").write_text(readme, encoding="utf-8")
    (staging / "start.sh").chmod(0o755)
    (staging / "run.sh").chmod(0o755)


def validate_server_tree(staging: Path, expected_mods: int) -> None:
    required_paths = (
        Path("VERSION"),
        Path("start.sh"),
        Path("start.bat"),
        Path("run.sh"),
        Path("run.bat"),
        Path("server.properties"),
        Path("eula.txt"),
        Path("user_jvm_args.txt"),
        Path("README.md"),
        Path("libraries") / "net" / "neoforged" / "neoforge" / NEOFORGE_VERSION / "unix_args.txt",
        Path("libraries") / "net" / "neoforged" / "neoforge" / NEOFORGE_VERSION / "win_args.txt",
    )
    missing = [str(path) for path in required_paths if not (staging / path).is_file()]
    if missing:
        raise FileNotFoundError("server tree is missing: " + ", ".join(missing))
    actual_mods = len(list((staging / "mods").glob("*.jar")))
    if actual_mods != expected_mods:
        raise ValueError(f"expected {expected_mods} server mods, found {actual_mods}")


def write_zip(staging: Path, output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = output.with_suffix(output.suffix + ".part")
    temporary.unlink(missing_ok=True)
    with zipfile.ZipFile(temporary, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for path in sorted(staging.rglob("*")):
            if not path.is_file():
                continue
            relative = path.relative_to(staging)
            info = zipfile.ZipInfo.from_file(path, relative.as_posix())
            info.compress_type = zipfile.ZIP_DEFLATED
            if path.stat().st_mode & 0o111:
                info.external_attr = (0o100755 & 0xFFFF) << 16
            with path.open("rb") as source:
                archive.writestr(info, source.read(), compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)
    temporary.replace(output)


def build_server_pack(
    version: str,
    output: Path,
    cache_dir: Path,
    java: str,
    *,
    verbose: bool,
    manifest_output: Path | None = None,
    base_release: str | None = None,
) -> str:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    package_mrpack.validate_platform(manifest)

    server_mods = [
        mod
        for mod in manifest.get("mods", [])
        if mod.get("environment") in ("both", "server") and mod.get("required", True)
    ]
    client_only = [
        mod for mod in manifest.get("mods", []) if mod.get("environment") == "client"
    ]

    temporary_root = Path(tempfile.mkdtemp(prefix="dimensionworks-server-", dir=str(cache_dir)))
    staging = temporary_root / "pack"
    staging.mkdir(parents=True)
    try:
        install_neoforge(staging, cache_dir, java, verbose=verbose)
        copied = 0
        for directory in SERVER_OVERRIDE_DIRS:
            copied += copy_tree(ROOT / directory, staging / directory)

        in_repo_count = 0
        for mod in server_mods:
            in_repo_count += int(
                resolve_server_mod(mod, staging, cache_dir, verbose=verbose)
            )
        write_server_files(
            staging,
            pack_version_value=version,
            servers_mods=len(server_mods),
        )
        release = f"v{version}"
        update_manifest = pack_manifest.make_server_manifest(
            staging,
            version,
            release,
            base_release,
        )
        manifest_bytes = pack_manifest.render_manifest(update_manifest)
        if manifest_output is not None:
            manifest_output.parent.mkdir(parents=True, exist_ok=True)
            manifest_output.write_bytes(manifest_bytes)
        pack_manifest.validate_no_updater_scripts(
            [
                path.relative_to(staging).as_posix()
                for path in sorted(staging.rglob("*"))
                if path.is_file()
            ]
        )
        validate_server_tree(staging, len(server_mods))
        write_zip(staging, output)
    finally:
        shutil.rmtree(temporary_root, ignore_errors=True)

    return (
        f"{output} ({output.stat().st_size} bytes), {len(server_mods)} server mods "
        f"({in_repo_count} in-repo), {len(client_only)} client-only mods excluded, "
        f"{copied} config/KubeJS files"
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", default=None, help="server pack version; defaults to VERSION")
    parser.add_argument("--output", type=Path, default=None)
    parser.add_argument(
        "--manifest-output",
        type=Path,
        default=None,
        help="write the standalone server update manifest here",
    )
    parser.add_argument(
        "--base-release",
        default=None,
        help="previous release tag used by update compatibility",
    )
    parser.add_argument("--cache-dir", type=Path, default=DEFAULT_CACHE_DIR)
    parser.add_argument("--java", default=None, help="Java executable used only by the NeoForge installer")
    parser.add_argument("--instance-dir", type=Path, default=DEFAULT_INSTANCE_DIR)
    parser.add_argument(
        "--dimension-complete",
        action="store_true",
        help="allow an explicit minor version because a dimension was completed",
    )
    parser.add_argument("--skip-hook", action="store_true", help="skip the packaging preflight")
    parser.add_argument("--skip-build", action="store_true", help="skip in-repo Mod builds")
    parser.add_argument("--verbose", action="store_true", help="print checks, downloads and progress")
    args = parser.parse_args()

    try:
        current = pack_version.read_current_version()
        version = str(current if args.version is None else pack_version.PackVersion.parse(args.version))
    except pack_version.VersionRuleError as error:
        print("SERVER PACK FAIL")
        print(f"detail: {error}")
        return 1

    cache_dir = args.cache_dir.expanduser().resolve()
    cache_dir.mkdir(parents=True, exist_ok=True)
    output = (
        args.output.expanduser().resolve()
        if args.output
        else ROOT / "dist" / f"DimensionWorks-Server-{version}.zip"
    )
    manifest_output = args.manifest_output or output.with_name(
        f"{output.stem}-update-manifest.tsv"
    )

    hook_summary = "skipped"
    if not args.skip_hook:
        ok, message = pack_hook.run_all(
            args.instance_dir.expanduser().resolve(),
            skip_build=args.skip_build,
            verbose=args.verbose,
            requested_version=str(version),
            dimension_complete=args.dimension_complete,
        )
        if not ok:
            print("PACK-HOOK FAIL")
            print(message)
            return 1
        hook_summary = message

    try:
        java = find_java(args.java)
        summary = build_server_pack(
            version,
            output,
            cache_dir,
            java,
            verbose=args.verbose,
            manifest_output=manifest_output.resolve(),
            base_release=args.base_release,
        )
    except (OSError, ValueError, RuntimeError, subprocess.TimeoutExpired) as error:
        print("SERVER PACK FAIL")
        print(f"detail: {error}")
        return 1

    print(f"SERVER PACK OK: {summary} | hook: {hook_summary}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
