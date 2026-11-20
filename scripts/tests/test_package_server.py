from __future__ import annotations

import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import package_mrpack
import package_server


class NativeServerPackagingTest(unittest.TestCase):
    def test_refuses_forge_manifest_before_downloading_or_installing(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = root / "mods.json"
            manifest.write_text(json.dumps({"minecraft": {"version": "1.20.1", "loader": "forge"}, "mods": []}))
            with patch.object(package_server, "MANIFEST", manifest), patch.object(package_server, "install_neoforge") as install:
                with self.assertRaisesRegex(ValueError, "1.21.1 NeoForge"):
                    package_server.build_server_pack("0.3.52", root / "pack.zip", root, "java", verbose=False)
                install.assert_not_called()
            with patch.object(package_mrpack, "MANIFEST", manifest), patch.object(package_mrpack, "resolve_modrinth") as resolve:
                with self.assertRaisesRegex(ValueError, "1.21.1 NeoForge"):
                    package_mrpack.build_pack("0.3.52", root / "pack.mrpack", root, root)
                resolve.assert_not_called()

    def test_java_17_and_missing_explicit_java_do_not_fall_back(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            java = Path(directory) / "java"
            java.touch()
            with patch.object(package_server.subprocess, "run", return_value=subprocess.CompletedProcess([], 0, 'openjdk version "17.0.19"')) as run:
                with self.assertRaisesRegex(ValueError, "Java 21"):
                    package_server.find_java(str(java))
                self.assertEqual(run.call_count, 1)
            with self.assertRaisesRegex(FileNotFoundError, "executable not found"):
                package_server.find_java(str(java.with_name("missing")))

    def test_missing_windows_arguments_are_installer_failure(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for filename in ("run.sh", "run.bat"):
                (root / filename).touch()
            args = root / "libraries/net/neoforged/neoforge" / package_server.NEOFORGE_VERSION
            args.mkdir(parents=True)
            (args / "unix_args.txt").touch()
            with patch.object(package_server, "cached_download", return_value=root / "installer.jar"), patch.object(package_server, "read_text", return_value="sha1"), patch.object(package_server.subprocess, "run", return_value=subprocess.CompletedProcess([], 0, "")):
                with self.assertRaisesRegex(FileNotFoundError, "win_args.txt"):
                    package_server.install_neoforge(root, root, "java", verbose=False)

    def test_start_script_uses_native_args_in_instance_with_spaces(self) -> None:
        with tempfile.TemporaryDirectory(prefix="native server ") as directory:
            root = Path(directory)
            (root / "run.sh").touch()
            package_server.write_server_files(root, pack_version_value="0.3.52", servers_mods=0)
            java = root / "java"
            java.write_text('#!/bin/sh\nprintf "%s\\n" "$PWD" "$@" > java-call.txt\nexit 7\n')
            java.chmod(0o755)
            env = dict(os.environ, PATH=str(root) + os.pathsep + os.environ["PATH"])
            result = subprocess.run(["bash", str(root / "start.sh"), "--demo"], cwd="/", env=env)
            self.assertEqual(result.returncode, 7)
            arguments = (root / "java-call.txt").read_text().splitlines()
            self.assertEqual(arguments[0], str(root))
            self.assertIn("-Ddimensionworks.instanceDir=" + str(root), arguments)
            self.assertIn("@libraries/net/neoforged/neoforge/" + package_server.NEOFORGE_VERSION + "/unix_args.txt", arguments)
            self.assertEqual(arguments[-2:], ["nogui", "--demo"])


if __name__ == "__main__":
    unittest.main()
