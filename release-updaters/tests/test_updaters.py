from __future__ import annotations

import hashlib
import http.server
import os
import shutil
import subprocess
import sys
import tempfile
import threading
import unittest
import zipfile

# Keep regression fixtures deterministic when aria2c is installed on CI hosts.
os.environ.setdefault("DW_UPDATER_DISABLE_ARIA2", "1")
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
UPDATERS = ROOT / "release-updaters"
sys.path.insert(0, str(ROOT / "scripts"))

import pack_manifest  # noqa: E402
import build_server_delta  # noqa: E402

SCRIPT_NAMES = (
    "update-dimensionworks-client.sh",
    "update-dimensionworks-server.sh",
)
PRESERVED_TOKENS = {
    "client": ("saves", "logs", "crash-reports", "screenshots"),
    "server": (
        "logs",
        "crash-reports",
        "debug",
        "screenshots",
        "world",
        "config",
        "defaultconfigs",
        "LuckPerms",
        "eula.txt",
        "server.properties",
        "user_jvm_args.txt",
        "usercache.json",
        "ops.json",
        "whitelist.json",
        "banned-ips.json",
        "banned-players.json",
    ),
}


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha512(data: bytes) -> str:
    return hashlib.sha512(data).hexdigest()


class UpdaterScriptContractTest(unittest.TestCase):
    def test_shell_updaters_exist_with_required_contract(self) -> None:
        for name in SCRIPT_NAMES:
            with self.subTest(name=name):
                path = UPDATERS / name
                self.assertTrue(path.is_file(), f"missing updater: {path}")
                data = path.read_bytes()
                self.assertNotIn(b"\r", data, f"{name} must use LF in repository")
                self.assertNotIn(b"python", data.lower(), f"{name} must not require Python")
                text = data.decode("utf-8")
                self.assertIn("--release", text)
                self.assertIn("--package", text)
                self.assertIn("--url", text)
                self.assertIn("--help", text)
                self.assertIn("-delta.zip", text)
                self.assertIn("[进度]", text)
                self.assertIn(".dimensionworks-update", text)
                self.assertIn("state.tsv", text)
                self.assertIn("VERSION", text)
                self.assertIn("download_forever", text)
                self.assertIn("aria2c", text)
                self.assertIn("--max-connection-per-server=8", text)
                self.assertIn("--split=8", text)
                self.assertIn("--min-split-size=1M", text)
                self.assertIn('download_forever "$candidate"', text)
                forever_block = text.split("download_forever()", 1)[1].split("download_forever_full()", 1)[0]
                self.assertNotIn("--max-time", forever_block)
                self.assertNotIn("--timeout", forever_block)
                self.assertNotIn("--connect-timeout", forever_block)
                if name.endswith(".bat"):
                    self.assertIn("New-BootstrapState", text)
                    self.assertIn("serverRuntimeNames", text)
                else:
                    self.assertIn("bootstrap_state", text)
                kind = "server" if "server" in name else "client"
                for token in PRESERVED_TOKENS[kind]:
                    self.assertIn(token, text, f"{name} must preserve {token}")

    def test_shell_scripts_parse_and_do_not_embed_batch(self) -> None:
        for name in (
            "update-dimensionworks-client.sh",
            "update-dimensionworks-server.sh",
        ):
            with self.subTest(name=name):
                path = UPDATERS / name
                result = subprocess.run(
                    ["bash", "-n", str(path)],
                    capture_output=True,
                    text=True,
                    check=False,
                )
                self.assertEqual(result.returncode, 0, result.stderr)
                text = path.read_text(encoding="utf-8")
                if name == "update-dimensionworks-server.sh":
                    self.assertNotIn("kill -KILL", text, "server updater must not force-kill a saving server")
                    self.assertIn('"$waited" -lt 180', text)
                self.assertNotIn("powershell", text.lower())
                self.assertNotIn("cmd.exe", text.lower())

    def test_legacy_windows_batch_scripts_removed(self) -> None:
        for name in (
            "update-dimensionworks-client.bat",
            "update-dimensionworks-server.bat",
        ):
            with self.subTest(name=name):
                self.assertFalse((UPDATERS / name).exists(), f"obsolete updater remains: {name}")


class FixtureFactory:
    def __init__(self, temp: str) -> None:
        self.temp = Path(temp)

    def write_client_package(
        self,
        *,
        version: str,
        files: dict[str, bytes],
        remote: dict[str, bytes] | None = None,
        base: str | None = None,
    ) -> Path:
        staging = self.temp / f"client-{version}"
        (staging / "overrides").mkdir(parents=True)
        (staging / "modrinth.index.json").write_text("{}", encoding="utf-8")
        index_entries = []
        for path, data in (remote or {}).items():
            index_entries.append(
                {
                    "path": path,
                    "hashes": {"sha512": sha512(data)},
                    "downloads": [f"https://example.invalid/{Path(path).name}"],
                }
            )
        for path, data in files.items():
            target = staging / "overrides" / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
        manifest = pack_manifest.make_client_manifest(
            index_entries,
            staging,
            version=version,
            release=f"v{version}",
            base=base,
        )
        manifest_bytes = pack_manifest.render_manifest(manifest)
        (staging / pack_manifest.MANIFEST_NAME).write_bytes(manifest_bytes)
        output = self.temp / f"DimensionWorks-{version}.mrpack"
        with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as archive:
            for path in sorted(staging.rglob("*")):
                if path.is_file():
                    archive.write(path, path.relative_to(staging).as_posix())
        return output

    def write_server_package(
        self,
        *,
        version: str,
        files: dict[str, bytes],
        base: str | None = None,
    ) -> Path:
        staging = self.temp / f"server-{version}"
        staging.mkdir(parents=True)
        for path, data in files.items():
            target = staging / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
        manifest = pack_manifest.make_server_manifest(
            staging,
            version=version,
            release=f"v{version}",
            base=base,
        )
        (staging / pack_manifest.MANIFEST_NAME).write_bytes(
            pack_manifest.render_manifest(manifest)
        )
        output = self.temp / f"DimensionWorks-Server-{version}.zip"
        with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as archive:
            for path in sorted(staging.rglob("*")):
                if path.is_file():
                    archive.write(path, path.relative_to(staging).as_posix())
        return output


@unittest.skipUnless(
    shutil.which("bash") and shutil.which("unzip"), "shell fixture tools unavailable"
)
class ShellClientFixtureTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_ctx = tempfile.TemporaryDirectory()
        self.temp = Path(self.temp_ctx.name)
        self.fixture = FixtureFactory(self.temp)
        self.instance = self.temp / "instance"
        self.instance.mkdir()
        shutil.copy2(
            UPDATERS / "update-dimensionworks-client.sh",
            self.instance / "update-dimensionworks-client.sh",
        )
        (self.instance / "VERSION").write_text("0.0.1\n", encoding="ascii")
        (self.instance / "saves" / "keep").mkdir(parents=True)
        (self.instance / "saves" / "keep" / "level.dat").write_bytes(b"keep")

    def tearDown(self) -> None:
        self.temp_ctx.cleanup()

    def run_script(self, package: Path) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            ["bash", str(self.instance / "update-dimensionworks-client.sh"), "--package", str(package)],
            cwd=self.instance,
            capture_output=True,
            text=True,
            check=False,
        )

    def test_local_package_adds_modifies_and_deletes_only_managed_files(self) -> None:
        first = self.fixture.write_client_package(
            version="0.0.2",
            files={"config/old.cfg": b"old", "kubejs/server_scripts/old.js": b"old"},
            remote={"mods/remote.jar": b"remote-v1"},
        )
        (self.instance / "mods").mkdir()
        (self.instance / "mods" / "remote.jar").write_bytes(b"remote-v1")
        result = self.run_script(first)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.instance / "config" / "old.cfg").read_bytes(), b"old")
        self.assertEqual((self.instance / "VERSION").read_text(), "0.0.2\n")
        state_text = (self.instance / ".dimensionworks-update" / "state.tsv").read_text(encoding="utf-8")
        self.assertTrue(state_text.startswith("# DimensionWorks update manifest 1\n"))
        self.assertIn("# kind client\n# version 0.0.2\n", state_text)

        second = self.fixture.write_client_package(
            version="0.0.3",
            files={"config/new.cfg": b"new", "config/changed.cfg": b"changed"},
            remote={"mods/remote.jar": b"remote-v2"},
        )
        (self.instance / "mods" / "remote.jar").write_bytes(b"remote-v2")
        result = self.run_script(second)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse((self.instance / "config" / "old.cfg").exists())
        self.assertFalse((self.instance / "kubejs" / "server_scripts" / "old.js").exists())
        self.assertEqual((self.instance / "config" / "changed.cfg").read_bytes(), b"changed")
        self.assertEqual((self.instance / "saves" / "keep" / "level.dat").read_bytes(), b"keep")
        self.assertEqual((self.instance / "VERSION").read_text(), "0.0.3\n")

    def test_rerun_restores_deleted_managed_file_despite_valid_state(self) -> None:
        package = self.fixture.write_client_package(
            version="0.0.2",
            files={"config/managed.cfg": b"managed"},
        )
        first = self.run_script(package)
        self.assertEqual(first.returncode, 0, first.stderr)

        (self.instance / "config" / "managed.cfg").unlink()
        second = self.run_script(package)

        self.assertEqual(second.returncode, 0, second.stdout + "\n" + second.stderr)
        self.assertEqual((self.instance / "config" / "managed.cfg").read_bytes(), b"managed")

    def test_updater_path_containing_minecraft_does_not_kill_itself(self) -> None:
        with tempfile.TemporaryDirectory(dir=ROOT) as temp:
            fixture = FixtureFactory(temp)
            package = fixture.write_client_package(
                version="0.0.2",
                files={"config/value.cfg": b"new"},
            )
            minecraft_instance = Path(temp) / "minecraft" / "DimensionWorks"
            minecraft_instance.mkdir(parents=True)
            shutil.copy2(
                UPDATERS / "update-dimensionworks-client.sh",
                minecraft_instance / "update-dimensionworks-client.sh",
            )
            (minecraft_instance / "VERSION").write_text("0.0.1\n", encoding="ascii")

            result = subprocess.run(
                [
                    "bash",
                    str(minecraft_instance / "update-dimensionworks-client.sh"),
                    "--package",
                    str(package),
                ],
                cwd=minecraft_instance,
                capture_output=True,
                text=True,
                check=False,
            )

            self.assertEqual(result.returncode, 0, result.stdout + "\n" + result.stderr)
            self.assertEqual((minecraft_instance / "VERSION").read_text(), "0.0.2\n")

    def test_failed_update_rolls_back_changed_file_and_preserves_private_data(self) -> None:
        package = self.fixture.write_client_package(
            version="0.0.4",
            files={"config/aaa.cfg": b"new", "config/value.cfg": b"new"},
        )
        (self.instance / "config").mkdir()
        (self.instance / "config" / "aaa.cfg").write_bytes(b"old")
        (self.instance / "config" / "value.cfg").mkdir()
        (self.instance / "config" / "value.cfg" / "keep").write_bytes(b"keep")
        state = self.instance / ".dimensionworks-update"
        state.mkdir()
        (state / "state.tsv").write_text(
            "# DimensionWorks update manifest 1\n"
            "# kind client\n# version 0.0.4\n# release v0.0.4\n# base v0.0.4\n"
            "action\tpath\talgorithm\tvalue\tsource\n"
            f"archive\tconfig/aaa.cfg\tsha256\t{sha256(b'old')}\toverrides/config/aaa.cfg\n",
            encoding="utf-8",
        )
        result = self.run_script(package)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual((self.instance / "config" / "aaa.cfg").read_bytes(), b"old")
        self.assertEqual((self.instance / "config" / "value.cfg" / "keep").read_bytes(), b"keep")
        self.assertEqual((self.instance / "VERSION").read_text(), "0.0.1\n")

    def test_rejects_archive_source_path_traversal_before_apply(self) -> None:
        package = self.fixture.write_client_package(
            version="0.0.5",
            files={"config/value.cfg": b"new"},
        )
        rewritten = self.temp / "traversal.mrpack"
        with zipfile.ZipFile(package) as source, zipfile.ZipFile(rewritten, "w") as target:
            for info in source.infolist():
                data = source.read(info.filename)
                if info.filename == pack_manifest.MANIFEST_NAME:
                    data = data.replace(
                        b"overrides/config/value.cfg",
                        b"../outside.cfg",
                    )
                target.writestr(info, data)
        result = self.run_script(rewritten)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("更新清单格式无效", result.stderr)
        self.assertEqual((self.instance / "VERSION").read_text(), "0.0.1\n")

    def test_url_uses_embedded_manifest_and_bootstraps_old_managed_files(self) -> None:
        package = self.fixture.write_client_package(
            version="0.0.6",
            files={"config/new.cfg": b"new"},
        )
        (self.instance / "config").mkdir()
        (self.instance / "config" / "stale.cfg").write_bytes(b"stale")
        handler = lambda *args, **kwargs: http.server.SimpleHTTPRequestHandler(
            *args, directory=str(self.temp), **kwargs
        )
        server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            url = f"http://127.0.0.1:{server.server_port}/{package.name}"
            result = subprocess.run(
                ["bash", str(self.instance / "update-dimensionworks-client.sh"), "--url", url],
                cwd=self.instance,
                capture_output=True,
                text=True,
                check=False,
            )
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=2)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertFalse((self.instance / "config" / "stale.cfg").exists())
        self.assertEqual((self.instance / "config" / "new.cfg").read_bytes(), b"new")

    def test_release_url_uses_client_delta_without_full_package(self) -> None:
        first = self.fixture.write_client_package(
            version="0.0.8",
            files={"config/value.cfg": b"old", "config/custom.cfg": b"original"},
        )
        result = self.run_script(first)
        self.assertEqual(result.returncode, 0, result.stderr)
        (self.instance / "config" / "custom.cfg").write_bytes(b"user custom")

        second = self.fixture.write_client_package(
            version="0.0.9",
            files={"config/value.cfg": b"new", "config/custom.cfg": b"original"},
            base="v0.0.8",
        )
        delta = self.temp / "DimensionWorks-0.0.9-delta.zip"
        build_server_delta.build_client_delta(first, second, delta)
        manifest = self.temp / "DimensionWorks-0.0.9-update-manifest.tsv"
        with zipfile.ZipFile(second) as package:
            manifest.write_bytes(package.read(pack_manifest.MANIFEST_NAME))

        requested: list[str] = []

        class Handler(http.server.SimpleHTTPRequestHandler):
            def log_message(self, format: str, *args: object) -> None:
                requested.append(self.path)

        handler = lambda *args, **kwargs: Handler(
            *args, directory=str(self.temp), **kwargs
        )
        server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            url = f"http://127.0.0.1:{server.server_port}/{second.name}"
            result = subprocess.run(
                ["bash", str(self.instance / "update-dimensionworks-client.sh"), "--url", url],
                cwd=self.instance,
                capture_output=True,
                text=True,
                check=False,
            )
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=2)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.instance / "config" / "value.cfg").read_bytes(), b"new")
        self.assertEqual((self.instance / "config" / "custom.cfg").read_bytes(), b"user custom")
        self.assertEqual((self.instance / "VERSION").read_text(), "0.0.9\n")
        self.assertIn("/" + manifest.name, requested)
        self.assertIn("/" + delta.name, requested)
        self.assertNotIn("/" + second.name, requested)



class ShellServerFixtureTest(unittest.TestCase):
    def test_updater_path_containing_minecraft_does_not_kill_itself(self) -> None:
        with tempfile.TemporaryDirectory(dir=ROOT) as temp:
            fixture = FixtureFactory(temp)
            package = fixture.write_server_package(
                version="0.0.2",
                files={"mods/value.jar": b"new"},
            )
            minecraft_instance = Path(temp) / "minecraft" / "DimensionWorks"
            minecraft_instance.mkdir(parents=True)
            shutil.copy2(
                UPDATERS / "update-dimensionworks-server.sh",
                minecraft_instance / "update-dimensionworks-server.sh",
            )
            (minecraft_instance / "VERSION").write_text("0.0.1\n", encoding="ascii")

            result = subprocess.run(
                [
                    "bash",
                    str(minecraft_instance / "update-dimensionworks-server.sh"),
                    "--package",
                    str(package),
                ],
                cwd=minecraft_instance,
                capture_output=True,
                text=True,
                check=False,
            )

            self.assertEqual(result.returncode, 0, result.stdout + "\n" + result.stderr)
            self.assertEqual((minecraft_instance / "VERSION").read_text(), "0.0.2\n")

    def test_rerun_restores_deleted_managed_file_despite_valid_state(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            fixture = FixtureFactory(temp)
            instance = Path(temp) / "server"
            instance.mkdir()
            shutil.copy2(
                UPDATERS / "update-dimensionworks-server.sh",
                instance / "update-dimensionworks-server.sh",
            )
            package = fixture.write_server_package(
                version="0.0.2",
                files={"mods/managed.jar": b"managed"},
            )
            command = [
                "bash",
                str(instance / "update-dimensionworks-server.sh"),
                "--package",
                str(package),
            ]
            first = subprocess.run(
                command,
                cwd=instance,
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(first.returncode, 0, first.stderr)

            (instance / "mods" / "managed.jar").unlink()
            second = subprocess.run(
                command,
                cwd=instance,
                capture_output=True,
                text=True,
                check=False,
            )

            self.assertEqual(second.returncode, 0, second.stdout + "\n" + second.stderr)
            self.assertEqual((instance / "mods" / "managed.jar").read_bytes(), b"managed")

    def test_release_url_uses_server_delta_without_full_package(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            fixture = FixtureFactory(temp)
            instance = Path(temp) / "server"
            instance.mkdir()
            shutil.copy2(
                UPDATERS / "update-dimensionworks-server.sh",
                instance / "update-dimensionworks-server.sh",
            )
            first = fixture.write_server_package(
                version="0.0.8",
                files={"mods/value.jar": b"old", "config/custom.cfg": b"original"},
            )
            result = subprocess.run(
                ["bash", str(instance / "update-dimensionworks-server.sh"), "--package", str(first)],
                cwd=instance,
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            (instance / "config" / "custom.cfg").write_bytes(b"user custom")

            second = fixture.write_server_package(
                version="0.0.9",
                files={"mods/value.jar": b"new", "config/custom.cfg": b"original"},
                base="v0.0.8",
            )
            delta = Path(temp) / "DimensionWorks-Server-0.0.9-delta.zip"
            build_server_delta.build_server_delta(first, second, delta)
            manifest = Path(temp) / "DimensionWorks-Server-0.0.9-update-manifest.tsv"
            with zipfile.ZipFile(second) as package:
                manifest.write_bytes(package.read(pack_manifest.MANIFEST_NAME))

            requested: list[str] = []

            class Handler(http.server.SimpleHTTPRequestHandler):
                def log_message(self, format: str, *args: object) -> None:
                    requested.append(self.path)

            handler = lambda *args, **kwargs: Handler(
                *args, directory=temp, **kwargs
            )
            server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), handler)
            thread = threading.Thread(target=server.serve_forever, daemon=True)
            thread.start()
            try:
                url = f"http://127.0.0.1:{server.server_port}/{second.name}"
                result = subprocess.run(
                    ["bash", str(instance / "update-dimensionworks-server.sh"), "--url", url],
                    cwd=instance,
                    capture_output=True,
                    text=True,
                    check=False,
                )
            finally:
                server.shutdown()
                server.server_close()
                thread.join(timeout=2)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual((instance / "mods" / "value.jar").read_bytes(), b"new")
            self.assertEqual((instance / "config" / "custom.cfg").read_bytes(), b"user custom")
            self.assertEqual((instance / "VERSION").read_text(), "0.0.9\n")
            self.assertIn("/" + manifest.name, requested)
            self.assertIn("/" + delta.name, requested)
            self.assertNotIn("/" + second.name, requested)

    def test_fresh_server_install_writes_default_runtime_files(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            fixture = FixtureFactory(temp)
            instance = Path(temp) / "server"
            instance.mkdir()
            shutil.copy2(
                UPDATERS / "update-dimensionworks-server.sh",
                instance / "update-dimensionworks-server.sh",
            )
            package = fixture.write_server_package(
                version="0.0.8",
                files={
                    "server.properties": b"level-name=world\n",
                    "eula.txt": b"eula=false\n",
                    "user_jvm_args.txt": b"-Xmx2G\n",
                    "mods/new.jar": b"new",
                },
            )
            result = subprocess.run(
                ["bash", str(instance / "update-dimensionworks-server.sh"), "--package", str(package)],
                cwd=instance,
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual((instance / "server.properties").read_text(), "level-name=world\n")
            self.assertEqual((instance / "eula.txt").read_text(), "eula=false\n")
            self.assertEqual((instance / "user_jvm_args.txt").read_text(), "-Xmx2G\n")

    def test_url_uses_embedded_manifest(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            fixture = FixtureFactory(temp)
            instance = Path(temp) / "server"
            instance.mkdir()
            shutil.copy2(
                UPDATERS / "update-dimensionworks-server.sh",
                instance / "update-dimensionworks-server.sh",
            )
            package = fixture.write_server_package(
                version="0.0.7",
                files={"mods/new.jar": b"new"},
            )
            handler = lambda *args, **kwargs: http.server.SimpleHTTPRequestHandler(
                *args, directory=temp, **kwargs
            )
            server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), handler)
            thread = threading.Thread(target=server.serve_forever, daemon=True)
            thread.start()
            try:
                url = f"http://127.0.0.1:{server.server_port}/{package.name}"
                result = subprocess.run(
                    ["bash", str(instance / "update-dimensionworks-server.sh"), "--url", url],
                    cwd=instance,
                    capture_output=True,
                    text=True,
                    check=False,
                )
            finally:
                server.shutdown()
                server.server_close()
                thread.join(timeout=2)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual((instance / "mods" / "new.jar").read_bytes(), b"new")

    def test_server_preserves_runtime_files_and_deletes_obsolete_managed_file(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            fixture = FixtureFactory(temp)
            instance = Path(temp) / "server"
            instance.mkdir()
            shutil.copy2(
                UPDATERS / "update-dimensionworks-server.sh",
                instance / "update-dimensionworks-server.sh",
            )
            (instance / "VERSION").write_text("0.0.1\n", encoding="ascii")
            (instance / "custom_world" / "serverconfig").mkdir(parents=True)
            (instance / "custom_world" / "level.dat").write_bytes(b"keepInventory=true")
            (instance / "custom_world" / "serverconfig" / "create-server.toml").write_bytes(b"maxRotationSpeed=10240")
            (instance / "custom_world" / "serverconfig" / "ftbchunks-world.snbt").write_bytes(b"old force-load policy")
            (instance / "server.properties").write_text("level-name=custom_world\n", encoding="ascii")
            (instance / "eula.txt").write_text("eula=true\n", encoding="ascii")
            (instance / "config" / "luckperms").mkdir(parents=True)
            (instance / "config" / "luckperms" / "luckperms.conf").write_bytes(b"user-luckperms-config")
            (instance / "config" / "luckperms" / "luckperms-h2-v2.mv.db").write_bytes(b"user-luckperms-database")
            (instance / "defaultconfigs").mkdir()
            (instance / "defaultconfigs" / "create-server.toml").write_bytes(b"user-world-default")

            first = fixture.write_server_package(
                version="0.0.5",
                files={
                    "mods/old.jar": b"old",
                    "config/server.cfg": b"old",
                    "config/luckperms/luckperms.conf": b"bad-luckperms-default",
                    "config/luckperms/luckperms-h2-v2.mv.db": b"bad-luckperms-database",
                    "defaultconfigs/create-server.toml": b"bad-world-default",
                    "defaultconfigs/ftbchunks-world.snbt": b"force_load_disabled",
                    "server.properties": b"bad-default",
                    "eula.txt": b"bad-default",
                },
            )
            result = subprocess.run(
                ["bash", str(instance / "update-dimensionworks-server.sh"), "--package", str(first)],
                cwd=instance,
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertEqual((instance / "config" / "server.cfg").read_bytes(), b"old")
            self.assertEqual((instance / "config" / "luckperms" / "luckperms.conf").read_bytes(), b"user-luckperms-config")
            self.assertEqual((instance / "config" / "luckperms" / "luckperms-h2-v2.mv.db").read_bytes(), b"user-luckperms-database")
            self.assertEqual((instance / "defaultconfigs" / "create-server.toml").read_bytes(), b"user-world-default")
            self.assertEqual((instance / "server.properties").read_text(), "level-name=custom_world\n")
            self.assertEqual((instance / "eula.txt").read_text(), "eula=true\n")
            (instance / "config" / "server.cfg").write_bytes(b"user-edited-config")

            second = fixture.write_server_package(
                version="0.0.6",
                files={
                    "mods/new.jar": b"new",
                    "config/server.cfg": b"new",
                    "config/luckperms/luckperms.conf": b"new-luckperms-default",
                    "config/luckperms/luckperms-h2-v2.mv.db": b"new-luckperms-database",
                    "defaultconfigs/create-server.toml": b"new-world-default",
                    "server.properties": b"bad-default-2",
                },
            )
            result = subprocess.run(
                ["bash", str(instance / "update-dimensionworks-server.sh"), "--package", str(second)],
                cwd=instance,
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertFalse((instance / "mods" / "old.jar").exists())
            self.assertEqual((instance / "mods" / "new.jar").read_bytes(), b"new")
            self.assertEqual((instance / "custom_world" / "level.dat").read_bytes(), b"keepInventory=true")
            self.assertEqual((instance / "custom_world" / "serverconfig" / "create-server.toml").read_bytes(), b"maxRotationSpeed=10240")
            self.assertEqual((instance / "custom_world" / "serverconfig" / "ftbchunks-world.snbt").read_bytes(), b"force_load_disabled")
            self.assertEqual((instance / "config" / "server.cfg").read_bytes(), b"user-edited-config")
            self.assertEqual((instance / "config" / "luckperms" / "luckperms.conf").read_bytes(), b"user-luckperms-config")
            self.assertEqual((instance / "config" / "luckperms" / "luckperms-h2-v2.mv.db").read_bytes(), b"user-luckperms-database")
            self.assertEqual((instance / "defaultconfigs" / "create-server.toml").read_bytes(), b"user-world-default")
            self.assertEqual((instance / "server.properties").read_text(), "level-name=custom_world\n")



@unittest.skipUnless(
    shutil.which("bash") and shutil.which("unzip"), "shell fixture tools unavailable"
)
class ShellDownloadRecoveryTest(unittest.TestCase):
    FAKE_CURL = r"""#!/usr/bin/env bash
set -eu
output=""
url=""
resume=0
while (($#)); do
    case "$1" in
        --output) output="$2"; shift 2 ;;
        --continue-at) resume=1; shift 2 ;;
        http://*|https://*) url="$1"; shift ;;
        *) shift ;;
    esac
done
if [[ -n "${DW_TEST_CURL_LOG:-}" ]]; then
    printf '%s\n' "$url" >> "$DW_TEST_CURL_LOG"
fi
if [[ "$url" == *"-update-manifest.tsv" ]]; then
    printf 'partial-manifest' > "$output"
    exit 28
fi
if [[ "$resume" -eq 1 ]]; then
    cp "$DW_TEST_PACKAGE" "$output"
    exit 0
fi
case "$url" in
    *gh-proxy.com*) head -c 300 /dev/zero > "$output" ;;
    *ghproxy.net*) head -c 200 /dev/zero > "$output" ;;
    *) head -c 100 /dev/zero > "$output" ;;
esac
exit 28
"""

    def _run_case(self, kind: str, choice: str, *, local_package: bool = False) -> tuple[subprocess.CompletedProcess[str], Path, Path]:
        temp_ctx = tempfile.TemporaryDirectory()
        self.addCleanup(temp_ctx.cleanup)
        temp = Path(temp_ctx.name)
        fixture = FixtureFactory(temp)
        instance = temp / "instance"
        instance.mkdir()
        script_name = f"update-dimensionworks-{kind}.sh"
        shutil.copy2(UPDATERS / script_name, instance / script_name)
        if kind == "client":
            package = fixture.write_client_package(
                version="0.0.2", files={"config/recovery.cfg": b"recovered"}
            )
        else:
            package = fixture.write_server_package(
                version="0.0.2", files={"mods/recovery.jar": b"recovered"}
            )
        if local_package:
            package_in_instance = instance / package.name
            shutil.copy2(package, package_in_instance)
        fake_bin = temp / "fake-bin"
        fake_bin.mkdir()
        fake_curl = fake_bin / "curl"
        fake_curl.write_text(self.FAKE_CURL, encoding="utf-8")
        fake_curl.chmod(0o755)
        log_path = temp / "curl.log"
        env = os.environ.copy()
        env["PATH"] = f"{fake_bin}{os.pathsep}{env.get('PATH', '')}"
        env["DW_TEST_PACKAGE"] = str(package)
        env["DW_TEST_CURL_LOG"] = str(log_path)
        input_text = "" if local_package else f"{choice}\n{package}\n"
        result = subprocess.run(
            ["bash", str(instance / script_name), "--release", "v0.0.2"],
            cwd=instance,
            input=input_text,
            capture_output=True,
            text=True,
            check=False,
            timeout=30,
            env=env,
        )
        return result, instance, log_path

    def test_choice_one_resumes_source_with_largest_partial_for_client_and_server(self) -> None:
        for kind in ("client", "server"):
            with self.subTest(kind=kind):
                result, instance, _ = self._run_case(kind, "1")
                self.assertEqual(result.returncode, 0, result.stdout + "\n" + result.stderr)
                self.assertIn("所有下载源均失败", result.stdout)
                self.assertIn("已下载 300 字节", result.stdout)
                self.assertIn("gh-proxy.com", result.stdout)
                self.assertIn("人工续传，不限制总时长", result.stderr)
                if kind == "client":
                    self.assertEqual((instance / "config/recovery.cfg").read_bytes(), b"recovered")
                else:
                    self.assertEqual((instance / "mods/recovery.jar").read_bytes(), b"recovered")

    def test_choice_two_installs_user_supplied_package_for_client_and_server(self) -> None:
        for kind in ("client", "server"):
            with self.subTest(kind=kind):
                result, instance, _ = self._run_case(kind, "2")
                self.assertEqual(result.returncode, 0, result.stdout + "\n" + result.stderr)
                self.assertIn("所有下载源均失败", result.stdout)
                self.assertIn("2) 手动指定已上传的完整更新包路径", result.stdout)
                expected_name = "DimensionWorks-0.0.2" if kind == "client" else "DimensionWorks-Server-0.0.2"
                self.assertIn(expected_name, result.stdout)
                if kind == "client":
                    self.assertEqual((instance / "config/recovery.cfg").read_bytes(), b"recovered")
                else:
                    self.assertEqual((instance / "mods/recovery.jar").read_bytes(), b"recovered")

    def test_existing_complete_package_is_found_without_network_for_client_and_server(self) -> None:
        for kind in ("client", "server"):
            with self.subTest(kind=kind):
                result, instance, log_path = self._run_case(kind, "2", local_package=True)
                self.assertEqual(result.returncode, 0, result.stdout + "\n" + result.stderr)
                self.assertIn("找到完整更新包", result.stderr)
                self.assertFalse(log_path.exists(), "local package should avoid all network requests")
                if kind == "client":
                    self.assertEqual((instance / "config/recovery.cfg").read_bytes(), b"recovered")
                else:
                    self.assertEqual((instance / "mods/recovery.jar").read_bytes(), b"recovered")



if __name__ == "__main__":
    unittest.main()
