from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts"))

import pack_version  # noqa: E402


class ResolveReleaseVersionTest(unittest.TestCase):
    def test_explicit_loader_migration_is_a_one_time_exception(self) -> None:
        forge = pack_version.PackVersion.parse("0.3.52")
        self.assertEqual(str(pack_version.resolve_version("0.4.0", current=forge)), "0.4.0")
        self.assertEqual(str(pack_version.resolve_version(None, current=forge)), "0.3.53")
        with self.assertRaises(pack_version.VersionRuleError):
            pack_version.resolve_version("0.4.0", current=pack_version.PackVersion.parse("0.3.51"))

    def test_uses_local_version_when_no_release_exists(self) -> None:
        self.assertEqual(
            pack_version.resolve_release_version("0.1.14", None),
            pack_version.PackVersion(0, 1, 15),
        )

    def test_uses_github_version_when_local_is_behind(self) -> None:
        self.assertEqual(
            pack_version.resolve_release_version("0.1.14", "v0.1.16"),
            pack_version.PackVersion(0, 1, 17),
        )

    def test_uses_local_version_when_github_is_behind(self) -> None:
        self.assertEqual(
            pack_version.resolve_release_version(pack_version.PackVersion(0, 2, 3), "v0.1.99"),
            pack_version.PackVersion(0, 2, 4),
        )

    def test_rejects_invalid_github_tags(self) -> None:
        for tag in ("0.1.16", "v0.1", "v0.1.16-beta"):
            with self.subTest(tag=tag):
                with self.assertRaises(pack_version.VersionRuleError):
                    pack_version.resolve_release_version("0.1.14", tag)


if __name__ == "__main__":
    unittest.main()
