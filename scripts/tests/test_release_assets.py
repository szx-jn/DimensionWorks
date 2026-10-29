from __future__ import annotations

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github" / "workflows" / "release-pack.yml"


class ReleaseWorkflowTest(unittest.TestCase):
    def test_workflow_publishes_all_standalone_assets_and_refuses_overwrite(self) -> None:
        text = WORKFLOW.read_text(encoding="utf-8")
        required = (
            "pack_version.resolve_version",
            "inputs.release_version",
            "inputs.dimension_complete",
            "--dimension-complete",
            "refusing to overwrite it",
            "build_server_delta.py",
            "DimensionWorks-${PACK_VERSION}-update-manifest.tsv",
            "DimensionWorks-${PACK_VERSION}-delta.zip",
            "DimensionWorks-Server-${PACK_VERSION}-update-manifest.tsv",
            "DimensionWorks-Server-${PACK_VERSION}-delta.zip",
            "update-dimensionworks-client-windows-amd64.exe",
            "update-dimensionworks-server-windows-amd64.exe",
            "update-dimensionworks-client-linux-amd64",
            "update-dimensionworks-server-linux-amd64",
            "update-dimensionworks-client-linux-arm64",
            "update-dimensionworks-server-linux-arm64",
            "update-dimensionworks-client-macos-amd64",
            "update-dimensionworks-server-macos-amd64",
            "update-dimensionworks-client-macos-arm64",
            "update-dimensionworks-server-macos-arm64",
            "SHA256SUMS",
            "git push origin",
        )
        for fragment in required:
            with self.subTest(fragment=fragment):
                self.assertIn(fragment, text)

    def test_workflow_keeps_updater_scripts_out_of_packages(self) -> None:
        text = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("Updater script embedded in client package", text)
        self.assertIn("Updater script embedded in server package", text)
        self.assertIn("cmp \"dist/DimensionWorks-${PACK_VERSION}-update-manifest.tsv\"", text)

    def test_server_packager_supports_explicit_minor_release(self) -> None:
        text = (ROOT / "scripts" / "package_server.py").read_text(encoding="utf-8")
        self.assertIn('"--dimension-complete"', text)
        self.assertIn("dimension_complete=args.dimension_complete", text)

    def test_mekanism_dependency_accepts_runtime_mod_version(self) -> None:
        for relative in (
            "mods/dimensionworks-rpm-limit/src/main/resources/META-INF/mods.toml",
            "mods/dimensionworks-mek-stress/src/main/resources/META-INF/mods.toml",
        ):
            text = (ROOT / relative).read_text(encoding="utf-8")
            self.assertIn('modId="mekanism"', text)
            self.assertIn('versionRange="[10.4.16,10.5)"', text)


if __name__ == "__main__":
    unittest.main()
