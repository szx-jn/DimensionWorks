from __future__ import annotations

import hashlib
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts"))

import pack_manifest  # noqa: E402


class ManifestRoundTripTest(unittest.TestCase):
    def test_round_trip_is_canonical_utf8_lf(self) -> None:
        manifest = pack_manifest.UpdateManifest(
            kind="client",
            version="1.2.3",
            release="v1.2.3",
            base="v1.2.2",
            entries=(
                pack_manifest.ManifestEntry(
                    "remote",
                    "mods/remote.jar",
                    "sha512",
                    "a" * 128,
                    "https://example.com/remote.jar",
                ),
                pack_manifest.ManifestEntry(
                    "archive",
                    "config/example.cfg",
                    "sha256",
                    "b" * 64,
                    "overrides/config/example.cfg",
                ),
            ),
        )

        rendered = pack_manifest.render_manifest(manifest)
        parsed = pack_manifest.parse_manifest(rendered, expected_kind="client")

        self.assertEqual(parsed, manifest)
        self.assertFalse(rendered.startswith(b"\xef\xbb\xbf"))
        self.assertNotIn(b"\r", rendered)
        self.assertTrue(rendered.endswith(b"\n"))
        self.assertEqual(
            rendered.splitlines()[:6],
            [
                b"# DimensionWorks update manifest 1",
                b"# kind client",
                b"# version 1.2.3",
                b"# release v1.2.3",
                b"# base v1.2.2",
                b"action\tpath\talgorithm\tvalue\tsource",
            ],
        )

    def test_rejects_wrong_expected_kind(self) -> None:
        manifest = self.make_manifest(kind="server")
        with self.assertRaisesRegex(pack_manifest.ManifestError, "kind"):
            pack_manifest.parse_manifest(
                pack_manifest.render_manifest(manifest),
                expected_kind="client",
            )

    @staticmethod
    def make_manifest(
        *,
        kind: str = "client",
        entries: tuple[pack_manifest.ManifestEntry, ...] | None = None,
    ) -> pack_manifest.UpdateManifest:
        return pack_manifest.UpdateManifest(
            kind=kind,
            version="1.2.3",
            release="v1.2.3",
            base="v1.2.2",
            entries=entries
            or (
                pack_manifest.ManifestEntry(
                    "archive",
                    "config/example.cfg",
                    "sha256",
                    "b" * 64,
                    "overrides/config/example.cfg",
                ),
            ),
        )


class ManifestValidationTest(unittest.TestCase):
    def parse_with_entry(
        self,
        *,
        path: str = "config/example.cfg",
        action: str = "archive",
        algorithm: str = "sha256",
        value: str = "b" * 64,
        source: str = "overrides/config/example.cfg",
    ) -> pack_manifest.UpdateManifest:
        entry = pack_manifest.ManifestEntry(action, path, algorithm, value, source)
        manifest = pack_manifest.UpdateManifest(
            kind="client",
            version="1.2.3",
            release="v1.2.3",
            base="v1.2.2",
            entries=(entry,),
        )
        return pack_manifest.parse_manifest(pack_manifest.render_manifest(manifest))

    def test_rejects_invalid_paths(self) -> None:
        invalid_paths = (
            "/config/example.cfg",
            "../config/example.cfg",
            "config/../example.cfg",
            "config\\example.cfg",
            "./config/example.cfg",
            "config//example.cfg",
            "config/example\u0000.cfg",
            ".",
            "..",
        )
        for path in invalid_paths:
            with self.subTest(path=path):
                with self.assertRaises(pack_manifest.ManifestError):
                    self.parse_with_entry(path=path)

    def test_rejects_duplicate_and_case_colliding_paths(self) -> None:
        for paths in (
            ("config/example.cfg", "config/example.cfg"),
            ("config/example.cfg", "CONFIG/EXAMPLE.CFG"),
        ):
            with self.subTest(paths=paths):
                entries = tuple(
                    pack_manifest.ManifestEntry(
                        "archive",
                        path,
                        "sha256",
                        "b" * 64,
                        f"overrides/{path}",
                    )
                    for path in paths
                )
                manifest = pack_manifest.UpdateManifest(
                    kind="client",
                    version="1.2.3",
                    release="v1.2.3",
                    base="v1.2.2",
                    entries=entries,
                )
                with self.assertRaisesRegex(pack_manifest.ManifestError, "duplicate"):
                    pack_manifest.parse_manifest(pack_manifest.render_manifest(manifest))

    def test_rejects_unknown_action(self) -> None:
        with self.assertRaisesRegex(pack_manifest.ManifestError, "action"):
            self.parse_with_entry(action="download")

    def test_rejects_action_algorithm_mismatch(self) -> None:
        with self.assertRaisesRegex(pack_manifest.ManifestError, "algorithm"):
            self.parse_with_entry(action="remote", algorithm="sha256", value="b" * 64)

    def test_rejects_bad_digest(self) -> None:
        bad_digests = ("B" * 64, "z" * 64, "a" * 63)
        for value in bad_digests:
            with self.subTest(value=value):
                with self.assertRaisesRegex(pack_manifest.ManifestError, "digest"):
                    self.parse_with_entry(value=value)

    def test_rejects_remote_source_with_whitespace(self) -> None:
        with self.assertRaisesRegex(pack_manifest.ManifestError, "remote source"):
            self.parse_with_entry(
                action="remote",
                algorithm="sha512",
                value="a" * 128,
                source="https://example.com/remote file.jar",
            )

    def test_rejects_header_and_field_count_problems(self) -> None:
        valid = pack_manifest.render_manifest(
            pack_manifest.UpdateManifest(
                kind="client",
                version="1.2.3",
                release="v1.2.3",
                base="v1.2.2",
                entries=(),
            )
        )
        malformed = (
            valid.replace(b"# DimensionWorks update manifest 1", b"# wrong header"),
            valid.replace(b"# version 1.2.3", b"# version 1.2"),
            valid.replace(
                b"action\tpath\talgorithm\tvalue\tsource\n",
                b"action\tpath\talgorithm\tvalue\n",
            ),
        )
        for data in malformed:
            with self.subTest(data=data):
                with self.assertRaises(pack_manifest.ManifestError):
                    pack_manifest.parse_manifest(data)


class MakeManifestTest(unittest.TestCase):
    def test_client_manifest_maps_remote_and_archive_entries(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            staging = Path(temp)
            index_data = b'{"formatVersion": 1}\n'
            override_data = b"client-config\n"
            (staging / "modrinth.index.json").write_bytes(index_data)
            version_file = staging / "overrides" / "VERSION"
            version_file.parent.mkdir(parents=True, exist_ok=True)
            version_file.write_text("1.2.3\n", encoding="utf-8")
            override = staging / "overrides" / "config" / "example.cfg"
            override.parent.mkdir(parents=True)
            override.write_bytes(override_data)

            manifest = pack_manifest.make_client_manifest(
                [
                    {
                        "path": "mods/remote.jar",
                        "hashes": {"sha512": "a" * 128},
                        "downloads": ["https://example.com/remote.jar"],
                    }
                ],
                staging,
                version="1.2.3",
                release="v1.2.3",
            )

        self.assertEqual(manifest.kind, "client")
        self.assertEqual(manifest.base, f"v{manifest.version}")
        self.assertEqual([entry.path for entry in manifest.entries], [
            "config/example.cfg",
            "modrinth.index.json",
            "mods/remote.jar",
        ])
        by_path = {entry.path: entry for entry in manifest.entries}
        self.assertEqual(by_path["mods/remote.jar"].action, "remote")
        self.assertEqual(by_path["mods/remote.jar"].algorithm, "sha512")
        self.assertEqual(
            by_path["mods/remote.jar"].source,
            "https://example.com/remote.jar",
        )
        self.assertEqual(by_path["config/example.cfg"].action, "archive")
        self.assertEqual(by_path["config/example.cfg"].algorithm, "sha256")
        self.assertEqual(by_path["config/example.cfg"].value, hashlib.sha256(override_data).hexdigest())
        self.assertEqual(
            by_path["config/example.cfg"].source,
            "overrides/config/example.cfg",
        )
        self.assertEqual(by_path["modrinth.index.json"].source, "modrinth.index.json")

    def test_client_manifest_rejects_symlinks(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            staging = Path(temp)
            (staging / "modrinth.index.json").write_bytes(b"{}\n")
            target = staging / "target"
            target.write_bytes(b"data")
            link = staging / "overrides" / "config" / "linked.cfg"
            link.parent.mkdir(parents=True)
            link.symlink_to(target)
            with self.assertRaisesRegex(pack_manifest.ManifestError, "symbolic link"):
                pack_manifest.make_client_manifest(
                    [],
                    staging,
                    version="1.2.3",
                    release="v1.2.3",
                )

    def test_server_manifest_scans_staging_and_excludes_itself(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            staging = Path(temp)
            server_file = staging / "mods" / "server.jar"
            server_file.parent.mkdir(parents=True)
            server_file.write_bytes(b"server-mod")
            (staging / "dimensionworks-update-manifest.tsv").write_text(
                "self",
                encoding="utf-8",
            )
            (staging / "VERSION").write_text("1.2.3\n", encoding="utf-8")

            manifest = pack_manifest.make_server_manifest(
                staging,
                version="1.2.3",
                release="v1.2.3",
            )

        self.assertEqual(manifest.kind, "server")
        self.assertEqual(manifest.base, f"v{manifest.version}")
        self.assertEqual(len(manifest.entries), 1)
        entry = manifest.entries[0]
        self.assertEqual(entry.action, "archive")
        self.assertEqual(entry.path, "mods/server.jar")
        self.assertEqual(entry.algorithm, "sha256")
        self.assertEqual(entry.value, hashlib.sha256(b"server-mod").hexdigest())
        self.assertEqual(entry.source, "mods/server.jar")

    def test_server_manifest_preserves_explicit_base(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            manifest = pack_manifest.make_server_manifest(
                Path(temp),
                version="1.2.3",
                release="v1.2.3",
                base="v1.2.2",
            )

        self.assertEqual(manifest.base, "v1.2.2")
        self.assertEqual(manifest.release, "v1.2.3")


class UpdaterScriptExclusionTest(unittest.TestCase):
    def test_rejects_updater_scripts_anywhere_in_archive(self) -> None:
        updater_names = (
            "update-dimensionworks-client.sh",
            "update-dimensionworks-client.bat",
            "update-dimensionworks-server.sh",
            "update-dimensionworks-server.bat",
        )
        for name in updater_names:
            with self.subTest(name=name):
                with self.assertRaisesRegex(pack_manifest.ManifestError, "updater"):
                    pack_manifest.validate_no_updater_scripts(
                        ["release-updaters/" + name]
                    )

    def test_accepts_regular_archive_members(self) -> None:
        pack_manifest.validate_no_updater_scripts(
            [
                "dimensionworks-update-manifest.tsv",
                "mods/example.jar",
                "overrides/update-dimensionworks-readme.txt",
            ]
        )


if __name__ == "__main__":
    unittest.main()
