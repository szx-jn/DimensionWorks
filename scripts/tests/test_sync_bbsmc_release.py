from __future__ import annotations

import copy
from email.parser import BytesParser
from email.policy import default
import hashlib
import io
import json
from pathlib import Path
import sys
import unittest
from unittest import mock
import zipfile

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts"))

import sync_bbsmc_release as sync  # noqa: E402


def mrpack(version: str = "0.3.41") -> bytes:
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w") as archive:
        archive.writestr("modrinth.index.json", json.dumps({
            "game": "minecraft", "versionId": version,
            "dependencies": {"minecraft": "1.20.1", "forge": "47.4.23"},
        }))
    return buffer.getvalue()


class BbsmcSyncTest(unittest.TestCase):
    def setUp(self) -> None:
        self.filename = "DimensionWorks-0.3.41.mrpack"
        self.data = mrpack()
        self.release = {
            "id": 41, "tag_name": "v0.3.41", "draft": False, "prerelease": False,
            "name": "DimensionWorks 0.3.41",
            "body": "第一行更新日志\n\nfix(release): 保留换行与中文\n",
            "assets": [
                {"name": self.filename, "state": "uploaded", "size": len(self.data)},
                {"name": "DimensionWorks-Server-0.3.41.zip", "state": "uploaded"},
                {"name": "SHA256SUMS", "state": "uploaded"},
            ],
        }
        self.metadata = sync.version_metadata(
            self.release, "doVGSZq0", self.filename, self.data,
        )
        self.uploaded = {
            "id": "new-version", "project_id": "doVGSZq0", "status": "listed",
            "version_number": "0.3.41", "changelog": self.release["body"],
            "files": [{
                "filename": self.filename,
                "hashes": {"sha512": hashlib.sha512(self.data).hexdigest()},
            }],
        }

    def run_sync(self, **kwargs: object) -> str:
        return sync.sync_release("szx-jn/DimensionWorks", "v0.3.41", "doVGSZq0", "token", **kwargs)

    def test_only_latest_non_draft_non_prerelease_can_reach_bbsmc(self) -> None:
        for changes in ({"draft": True}, {"prerelease": True}, {"id": 42}):
            with self.subTest(changes=changes):
                latest = dict(self.release, **changes)
                with (
                    mock.patch.object(sync, "github_json", side_effect=[self.release, latest]),
                    mock.patch.object(sync, "download_mrpack") as download,
                    mock.patch.object(sync, "bbsmc_json") as bbsmc,
                ):
                    self.assertIn("SKIP", self.run_sync())
                download.assert_not_called()
                bbsmc.assert_not_called()
        for flag in ("draft", "prerelease"):
            with (
                self.subTest(flag=flag),
                mock.patch.object(sync, "github_json", return_value=dict(self.release, **{flag: True})),
                mock.patch.object(sync, "bbsmc_json") as bbsmc,
            ):
                self.assertIn("SKIP", self.run_sync())
                bbsmc.assert_not_called()

    def test_upload_uses_only_named_mrpack_and_exact_release_body(self) -> None:
        with (
            mock.patch.object(sync, "github_json", return_value=self.release),
            mock.patch.object(sync, "download_mrpack", return_value=self.data) as download,
            mock.patch.object(sync, "bbsmc_json", return_value=[]) as bbsmc,
            mock.patch.object(sync, "upload_version", return_value=self.uploaded) as upload,
        ):
            self.assertIn("BBSMC OK", self.run_sync())
        download.assert_called_once_with("szx-jn/DimensionWorks", "v0.3.41", self.release["assets"][0])
        bbsmc.assert_called_once_with("project/doVGSZq0/version", "token")
        upload.assert_called_once_with(self.metadata, self.filename, self.data, "token")
        self.assertEqual(self.metadata["changelog"], self.release["body"])

    def test_latest_changed_during_download_does_not_upload(self) -> None:
        with (
            mock.patch.object(sync, "github_json", side_effect=[
                self.release, self.release, dict(self.release, id=42, tag_name="v0.3.42"),
            ]),
            mock.patch.object(sync, "download_mrpack", return_value=self.data),
            mock.patch.object(sync, "bbsmc_json", return_value=[]),
            mock.patch.object(sync, "upload_version") as upload,
        ):
            self.assertIn("SKIP", self.run_sync())
        upload.assert_not_called()

    def test_repeated_job_skips_matching_version_and_refuses_conflict(self) -> None:
        self.assertTrue(sync.already_synced([self.uploaded], self.metadata, self.filename, self.data))
        for change in ("changelog", "files", "status"):
            version = copy.deepcopy(self.uploaded)
            if change == "changelog":
                version[change] = "其他日志"
            elif change == "status":
                version[change] = "draft"
            else:
                version["files"][0]["hashes"]["sha512"] = "other-file-hash"
            with self.subTest(change=change), self.assertRaisesRegex(ValueError, "不覆盖"):
                sync.already_synced([version], self.metadata, self.filename, self.data)
        with (
            mock.patch.object(sync, "github_json", return_value=self.release),
            mock.patch.object(sync, "download_mrpack", return_value=self.data),
            mock.patch.object(sync, "bbsmc_json", return_value=[self.uploaded]),
            mock.patch.object(sync, "upload_version") as upload,
        ):
            self.assertIn("无需重复上传", self.run_sync())
        upload.assert_not_called()

    def test_missing_mrpack_or_empty_log_or_wrong_pack_version_refuses_upload(self) -> None:
        for case in ("asset", "body", "version"):
            release = copy.deepcopy(self.release)
            data = self.data
            if case == "asset":
                release["assets"] = release["assets"][1:]
            elif case == "body":
                release["body"] = "  \n"
            else:
                data = mrpack("0.3.40")
            with (
                self.subTest(case=case),
                mock.patch.object(sync, "github_json", return_value=release),
                mock.patch.object(sync, "download_mrpack", return_value=data),
                mock.patch.object(sync, "upload_version") as upload,
                self.assertRaises(ValueError),
            ):
                self.run_sync()
            upload.assert_not_called()

    def test_missing_token_fails_and_dry_run_cannot_write(self) -> None:
        with (
            mock.patch.object(sync, "github_json", return_value=self.release),
            mock.patch.object(sync, "download_mrpack", return_value=self.data),
            mock.patch.object(sync, "bbsmc_json", return_value=[]),
            mock.patch.object(sync, "upload_version") as upload,
        ):
            with self.assertRaisesRegex(ValueError, "BBSMC_TOKEN"):
                sync.sync_release("repo", "v0.3.41", "project", "")
            self.assertIn("DRY-RUN OK", sync.sync_release("repo", "v0.3.41", "project", "", dry_run=True))
        upload.assert_not_called()

    def test_multipart_contains_json_first_and_one_binary_file_only(self) -> None:
        captured = []

        def open_url(request: object, timeout: int) -> io.BytesIO:
            captured.append(request)
            return io.BytesIO(json.dumps(self.uploaded).encode())

        with mock.patch.object(sync.urllib.request, "urlopen", side_effect=open_url):
            self.assertEqual(
                sync.upload_version(self.metadata, self.filename, self.data, "test-token"), self.uploaded,
            )
        request = captured[0]
        self.assertEqual(request.full_url, "https://api.bbsmc.net/v2/version")
        self.assertEqual(request.get_method(), "POST")
        self.assertEqual(request.get_header("Authorization"), "Bearer test-token")
        message = BytesParser(policy=default).parsebytes(
            f"Content-Type: {request.get_header('Content-type')}\r\nMIME-Version: 1.0\r\n\r\n".encode()
            + request.data,
        )
        parts = list(message.iter_parts())
        self.assertEqual(len(parts), 2)
        self.assertEqual(parts[0].get_param("name", header="content-disposition"), "data")
        self.assertEqual(json.loads(parts[0].get_payload(decode=True)), self.metadata)
        self.assertEqual(parts[1].get_filename(), self.filename)
        self.assertEqual(parts[1].get_payload(decode=True), self.data)


if __name__ == "__main__":
    unittest.main()
