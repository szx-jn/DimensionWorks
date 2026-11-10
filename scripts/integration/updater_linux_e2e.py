#!/usr/bin/env python3
"""Run a real Linux updater against a disposable server and local HTTP fixture."""
from __future__ import annotations

import argparse
import hashlib
import http.server
import os
from pathlib import Path
import selectors
import shutil
import subprocess
import sys
import tempfile
import threading
import time
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import pack_manifest


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("binary", type=Path)
    args = parser.parse_args()
    binary = args.binary.resolve()
    with tempfile.TemporaryDirectory(prefix="dw-updater-e2e-") as temp:
        root = Path(temp)
        instance = root / "server with spaces"
        web = root / "http"
        (instance / "mods").mkdir(parents=True)
        (instance / "world").mkdir()
        (instance / "world/level.dat").write_bytes(b"preserve world")
        (instance / "VERSION").write_text("0.2.67\n")
        (instance / ".dimensionworks-update").mkdir()
        (instance / ".dimensionworks-update/lock").write_text("token=stale\npid=999999\n")
        updater = instance / "update-dimensionworks-server-linux-amd64"
        shutil.copy2(binary, updater)
        updater.chmod(0o755)
        web.mkdir()
        files = {"mods/e2e.jar": os.urandom(8 << 20)}
        entries = tuple(pack_manifest.ManifestEntry("archive", name, "sha256", hashlib.sha256(data).hexdigest(), name) for name, data in files.items())
        manifest = pack_manifest.render_manifest(pack_manifest.UpdateManifest("server", "0.2.73", "v0.2.73", "none", entries))
        if isinstance(manifest, str):
            manifest = manifest.encode()
        (web / "DimensionWorks-Server-0.2.73-update-manifest.tsv").write_bytes(manifest)
        with zipfile.ZipFile(web / "DimensionWorks-Server-0.2.73.zip", "w") as archive:
            for name, data in files.items():
                archive.writestr(name, data)

        class Handler(http.server.SimpleHTTPRequestHandler):
            def __init__(self, *a, **kw):
                super().__init__(*a, directory=str(web), **kw)

            def copyfile(self, source, outputfile):
                try:
                    super().copyfile(source, outputfile)
                except (BrokenPipeError, ConnectionResetError):
                    pass  # aria2 may close redundant connections after completion.

            def do_HEAD(self):
                self.send_error(405, "HEAD disabled for regression")

            def log_message(self, *_):
                pass

        server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        url = f"http://127.0.0.1:{server.server_port}/DimensionWorks-Server-0.2.73.zip"
        process = subprocess.Popen([str(updater), "--url", url], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, cwd=root)
        selector = selectors.DefaultSelector()
        selector.register(process.stdout, selectors.EVENT_READ)
        transcript = bytearray()
        prompts = [
            ("输入 Y 继续，其他输入取消：", b"y\n"),
            ("输入 Y（清理残留锁并继续更新）；输入 N 取消更新：", b"y\n"),
            ("操作完成。按回车键关闭窗口。", b"\n"),
            ("请检查错误信息后按回车键关闭窗口。", b"\n"),
        ]
        answered: set[int] = set()
        deadline = time.monotonic() + 45
        try:
            while process.poll() is None and time.monotonic() < deadline:
                for key, _ in selector.select(0.2):
                    chunk = os.read(key.fd, 65536)
                    if chunk:
                        transcript.extend(chunk)
                text = transcript.decode(errors="replace")
                for i, (prompt, answer) in enumerate(prompts):
                    if i not in answered and prompt in text:
                        process.stdin.write(answer)
                        process.stdin.flush()
                        answered.add(i)
            if process.poll() is None:
                process.kill()
                raise AssertionError("updater hung during confirmation or download")
            transcript.extend(process.stdout.read())
            text = transcript.decode(errors="replace")
            if process.returncode != 0:
                raise AssertionError(f"updater exited {process.returncode}")
            assert answered == {0, 1, 2}, answered
            assert (instance / "VERSION").read_text().strip() == "0.2.73"
            assert "aria2 引擎未完成" not in text, "unexpected Go fallback"
            assert "sidecar 缺失" not in text, "embedded engine missing"
            for name, data in files.items():
                assert (instance / name).read_bytes() == data, name
            assert (instance / "world/level.dat").read_bytes() == b"preserve world"
            assert (instance / ".dimensionworks-update/state.tsv.hmac").is_file()
            assert not (instance / ".dimensionworks-update/lock").exists()
            assert not list(instance.glob(".dimensionworks-aria2-*"))
            print("PASS: single binary, stale lock confirmations, HEAD disabled, 8 MiB update, world preserved, signed state, engine/lock cleanup")
        except BaseException:
            print(transcript.decode(errors="replace"), file=sys.stderr)
            raise
        finally:
            if process.poll() is None:
                process.kill()
            process.wait()
            selector.close()
            server.shutdown()
            server.server_close()


if __name__ == "__main__":
    main()
