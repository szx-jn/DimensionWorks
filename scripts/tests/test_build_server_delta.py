from __future__ import annotations

import hashlib
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts"))

import build_server_delta  # noqa: E402
import pack_manifest  # noqa: E402


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def write_zip(path: Path, files: dict[str, bytes], manifest_version: str) -> pack_manifest.UpdateManifest:
    entries = tuple(
        pack_manifest.ManifestEntry(
            "archive",
            name,
            "sha256",
            sha256(data),
            name,
        )
        for name, data in sorted(files.items())
    )
    manifest = pack_manifest.UpdateManifest(
        kind="server",
        version=manifest_version,
        release=f"v{manifest_version}",
        base="none",
        entries=entries,
    )
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as archive:
        for name, data in sorted(files.items()):
            archive.writestr(name, data)
    return manifest


class ServerDeltaTest(unittest.TestCase):
    def test_delta_contains_only_changed_and_new_files(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            base = root / "base.zip"
            new = root / "new.zip"
            output = root / "delta.zip"
            write_zip(base, {"mods/old.jar": b"old", "config/same.cfg": b"same"}, "0.0.1")
            new_manifest = write_zip(
                new,
                {"mods/new.jar": b"new", "config/same.cfg": b"same", "config/changed.cfg": b"changed"},
                "0.0.2",
            )
            build_server_delta.build_server_delta(base, new, output, new_manifest)
            with zipfile.ZipFile(output) as archive:
                self.assertEqual(
                    sorted(archive.namelist()),
                    ["config/changed.cfg", "mods/new.jar"],
                )
                self.assertEqual(archive.read("config/changed.cfg"), b"changed")

    def test_empty_delta_is_valid_archive(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            output = Path(temp) / "empty.zip"
            build_server_delta.write_empty_delta(output)
            with zipfile.ZipFile(output) as archive:
                self.assertEqual(archive.namelist(), [])

    def test_delta_does_not_require_or_embed_a_manifest_in_packages(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            base = root / "base.zip"
            new = root / "new.zip"
            output = root / "delta.zip"
            write_zip(base, {"mods/a.jar": b"a"}, "0.0.1")
            manifest = write_zip(new, {"mods/a.jar": b"b"}, "0.0.2")
            build_server_delta.build_server_delta(base, new, output, manifest)
            with zipfile.ZipFile(new) as archive:
                self.assertNotIn(pack_manifest.MANIFEST_NAME, archive.namelist())

    def test_client_delta_contains_changed_archive_but_not_remote_files(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            base = root / "base.mrpack"
            new = root / "new.mrpack"
            output = root / "client-delta.zip"
            entries = (
                pack_manifest.ManifestEntry(
                    "remote",
                    "mods/remote.jar",
                    "sha512",
                    "a" * 128,
                    "https://example.com/remote.jar",
                ),
                pack_manifest.ManifestEntry(
                    "archive",
                    "config/value.cfg",
                    "sha256",
                    sha256(b"new"),
                    "overrides/config/value.cfg",
                ),
            )
            manifest = pack_manifest.UpdateManifest(
                kind="client",
                version="0.0.2",
                release="v0.0.2",
                base="v0.0.1",
                entries=entries,
            )
            for path, data in (
                (base, b"old"),
                (new, b"new"),
            ):
                with zipfile.ZipFile(path, "w") as archive:
                    archive.writestr("overrides/config/value.cfg", data)
            build_server_delta.build_client_delta(base, new, output, manifest)
            with zipfile.ZipFile(output) as archive:
                self.assertEqual(archive.namelist(), ["overrides/config/value.cfg"])
                self.assertEqual(archive.read("overrides/config/value.cfg"), b"new")


if __name__ == "__main__":
    unittest.main()
