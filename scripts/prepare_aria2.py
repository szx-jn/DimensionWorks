#!/usr/bin/env python3
"""Stage pinned, verified aria2 executables for single-file updater builds."""
from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
import urllib.request

COMMIT = "7012040fec926e16fe8f6c403cf038527f5c18b9"
ENGINES = {
    "windows-amd64": ("aria2c-windows-amd64.exe", "win32/x64/engine/aria2c.exe", "098a065ab71f639bf7048e790c870756fd6e83de9cc678915bbd07077d473fa2"),
    "linux-amd64": ("aria2c-linux-amd64", "linux/x64/engine/aria2c", "6f50816471fbd5e91c04df6cf6f995c6279d295b70187f313e6d3b04f65769fc"),
    "macos-arm64": ("aria2c-macos-arm64", "darwin/arm64/engine/aria2c", "1527c4d071c16c1266880e93750e385cdd4e9d2a950d7f56cb5a21125e3bd311"),
}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=Path(__file__).resolve().parents[1] / "update-Windows/aria2-runtime")
    parser.add_argument("--platform", choices=ENGINES)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    for platform, (name, source, digest) in ENGINES.items():
        if args.platform and platform != args.platform:
            continue
        target = args.output / name
        if not target.is_file() or hashlib.sha256(target.read_bytes()).hexdigest() != digest:
            url = f"https://raw.githubusercontent.com/agalwood/Motrix/{COMMIT}/extra/{source}"
            with urllib.request.urlopen(url, timeout=90) as response:
                data = response.read()
            if hashlib.sha256(data).hexdigest() != digest:
                raise ValueError(f"{name}: SHA-256 mismatch")
            temporary = target.with_suffix(target.suffix + ".tmp")
            temporary.write_bytes(data)
            temporary.replace(target)
        target.chmod(0o755)
        print(f"Verified {platform}: {target.name}", flush=True)


if __name__ == "__main__":
    main()
