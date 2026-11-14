from __future__ import annotations

import unittest
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github" / "workflows" / "release-pack.yml"


class ReleaseWorkflowTest(unittest.TestCase):
    def test_bbsmc_sync_requires_successful_publish_and_has_no_separate_trigger(self) -> None:
        text = WORKFLOW.read_text(encoding="utf-8")
        triggers = text.split("permissions:", 1)[0]
        self.assertIn("workflow_dispatch:", triggers)
        for event in ("release:", "push:", "pull_request:", "workflow_run:"):
            self.assertNotIn(event, triggers)
        publish = text.split("      - name: 发布 GitHub 正式版本", 1)[1].split(
            "      - name: 回写已发布版本号", 1,
        )[0]
        self.assertIn("id: publish", publish)
        self.assertLess(publish.index("gh release create"), publish.index('echo "tag=$TAG"'))
        self.assertIn("published_tag: ${{ steps.publish.outputs.tag }}", text)
        job = text.split("  sync-bbsmc:", 1)[1]
        self.assertIn("needs: release", job)
        self.assertIn("needs.release.result == 'success'", job)
        self.assertIn("needs.release.outputs.published_tag != ''", job)
        self.assertIn("contents: read", job)
        self.assertIn("secrets.BBSMC_TOKEN", job)
        self.assertIn("RELEASE_TAG: ${{ needs.release.outputs.published_tag }}", job)
        self.assertIn('sync_bbsmc_release.py --tag "$RELEASE_TAG"', job)
        self.assertNotIn("package_mrpack.py", job)

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
            "DimensionWorks-Server-${PACK_VERSION}-update-manifest.tsv",
            "DimensionWorks-Server-${PACK_VERSION}-delta.zip",
            "update-dimensionworks-client-windows-amd64.exe",
            "update-dimensionworks-server-windows-amd64.exe",
            "update-dimensionworks-server-linux-amd64",
            "update-dimensionworks-client-macos-arm64",
            "update-dimensionworks-server-macos-arm64",
            "aria2c-windows-amd64.exe",
            "aria2c-linux-amd64",
            "aria2c-macos-arm64",
            "aria2-third-party-source.tar.gz",
            "SHA256SUMS",
            "git push origin",
        )
        for fragment in required:
            with self.subTest(fragment=fragment):
                self.assertIn(fragment, text)

    def test_release_has_exactly_five_bundled_updaters_and_no_loose_engines(self) -> None:
        text = WORKFLOW.read_text(encoding="utf-8")
        assets = text.split("assets=(", 1)[1].split(")", 1)[0]
        updaters = re.findall(r'"dist/(update-dimensionworks-[^"\n]+)"', assets)
        self.assertEqual(len(updaters), 5)
        self.assertEqual(len(set(updaters)), 5)
        self.assertNotIn('update-dimensionworks-client-linux-amd64', assets)
        self.assertNotIn('"dist/aria2c-', assets)
        self.assertIn('"dist/DimensionWorks-${PACK_VERSION}-update-manifest.tsv"', assets)
        self.assertIn('"dist/DimensionWorks-Server-${PACK_VERSION}-update-manifest.tsv"', assets)
        self.assertIn("go build -tags bundled_aria2", text)
        self.assertIn("python3 scripts/prepare_aria2.py", text)

    def test_workflow_does_not_watch_removed_shell_updater_directory(self) -> None:
        text = WORKFLOW.read_text(encoding="utf-8")
        self.assertNotIn("release-updaters/**", text)
        self.assertIn("scripts/package_mrpack.py", text)

    def test_aria2_source_download_falls_back_to_github_asset_api(self) -> None:
        text = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("download_github_release()", text)
        self.assertIn("Accept: application/octet-stream", text)
        self.assertIn(
            "download_github_release aria2/aria2 release-1.36.0 "
            "aria2-1.36.0.tar.xz",
            text,
        )

    def test_workflow_keeps_updater_scripts_out_of_packages(self) -> None:
        text = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("Updater script embedded in client package", text)
        self.assertIn("Updater script embedded in server package", text)
        self.assertIn('--manifest "dist/DimensionWorks-Server-${PACK_VERSION}-update-manifest.tsv"', text)
        self.assertIn("must remain a standalone Release asset", text)
        client_packager = (ROOT / "scripts" / "package_mrpack.py").read_text(encoding="utf-8")
        server_packager = (ROOT / "scripts" / "package_server.py").read_text(encoding="utf-8")
        self.assertNotIn("archive.write(staging / pack_manifest.MANIFEST_NAME", client_packager)
        self.assertNotIn("(staging / pack_manifest.MANIFEST_NAME).write_bytes", server_packager)

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
