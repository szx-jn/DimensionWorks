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
        self.files = {
            self.filename: mrpack(),
            "DimensionWorks-Server-0.3.41.zip": b"complete-server",
            "DimensionWorks-Server-0.3.41-delta.zip": b"server-delta",
        }
        self.release = {
            "id": 41, "tag_name": "v0.3.41", "draft": False, "prerelease": False,
            "name": "DimensionWorks 0.3.41",
            "body": "第一行更新日志\n\nfix(release): 保留换行与中文\n",
            "assets": [
                {"name": filename, "state": "uploaded", "size": len(data)}
                for filename, data in self.files.items()
            ] + [{"name": "SHA256SUMS", "state": "uploaded"}],
        }
        self.metadata = sync.version_metadata(self.release, "doVGSZq0", self.files)
        self.uploaded = {
            "id": "new-version", "project_id": "doVGSZq0", "status": "listed",
            "version_number": "0.3.41", "changelog": self.release["body"],
            "files": [{
                "filename": filename, "primary": filename == self.filename,
                "hashes": {"sha512": hashlib.sha512(data).hexdigest()},
            } for filename, data in self.files.items()],
        }

    def run_sync(self, **kwargs: object) -> str:
        return sync.sync_release("szx-jn/DimensionWorks", "v0.3.41", "doVGSZq0", "token", **kwargs)

    def downloads(self):
        return mock.patch.object(
            sync, "download_asset",
            side_effect=lambda repo, tag, asset: self.files[asset["name"]],
        )

    def test_only_latest_non_draft_non_prerelease_can_reach_bbsmc(self) -> None:
        for changes in ({"draft": True}, {"prerelease": True}, {"id": 42}):
            with (
                self.subTest(changes=changes),
                mock.patch.object(sync, "github_json", side_effect=[self.release, dict(self.release, **changes)]),
                mock.patch.object(sync, "download_asset") as download,
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

    def test_uploads_three_named_assets_and_exact_release_body_with_client_primary(self) -> None:
        with (
            mock.patch.object(sync, "github_json", return_value=self.release),
            self.downloads() as download,
            mock.patch.object(sync, "bbsmc_json", return_value=[]) as bbsmc,
            mock.patch.object(sync, "upload_files", return_value=self.uploaded) as upload,
        ):
            self.assertIn("BBSMC OK", self.run_sync())
        self.assertEqual(download.call_args_list, [
            mock.call("szx-jn/DimensionWorks", "v0.3.41", asset)
            for asset in self.release["assets"][:3]
        ])
        bbsmc.assert_called_once_with("project/doVGSZq0/version", "token")
        upload.assert_called_once_with(self.metadata, self.files, "token")
        self.assertEqual(self.metadata["file_parts"], list(self.files))
        self.assertEqual(self.metadata["primary_file"], self.filename)
        self.assertEqual(self.metadata["changelog"], self.release["body"])

    def test_latest_changed_during_download_does_not_upload(self) -> None:
        with (
            mock.patch.object(sync, "github_json", side_effect=[
                self.release, self.release, dict(self.release, id=42, tag_name="v0.3.42"),
            ]),
            self.downloads(),
            mock.patch.object(sync, "bbsmc_json", return_value=[]),
            mock.patch.object(sync, "upload_files") as upload,
        ):
            self.assertIn("SKIP", self.run_sync())
        upload.assert_not_called()

    def test_repeated_job_skips_matching_three_files_and_refuses_conflict(self) -> None:
        self.assertTrue(sync.already_synced([self.uploaded], self.metadata, self.files))
        for change in ("changelog", "hash", "status", "primary", "extra", "duplicate"):
            version = copy.deepcopy(self.uploaded)
            if change == "changelog":
                version["changelog"] = "其他日志"
            elif change == "status":
                version["status"] = "draft"
            elif change == "hash":
                version["files"][1]["hashes"]["sha512"] = "other-file-hash"
            elif change == "primary":
                version["files"][1]["primary"] = True
            elif change == "extra":
                version["files"].append(dict(version["files"][1], filename="unexpected.zip"))
            else:
                version["files"].append(version["files"][0])
            with self.subTest(change=change), self.assertRaisesRegex(ValueError, "不覆盖"):
                sync.already_synced([version], self.metadata, self.files)
        with (
            mock.patch.object(sync, "github_json", return_value=self.release),
            self.downloads(),
            mock.patch.object(sync, "bbsmc_json", return_value=[self.uploaded]),
            mock.patch.object(sync, "upload_files") as upload,
        ):
            self.assertIn("无需重复上传", self.run_sync())
        upload.assert_not_called()

    def test_existing_mrpack_gets_only_missing_server_files_and_is_read_back(self) -> None:
        existing = dict(self.uploaded, files=self.uploaded["files"][:1])
        with (
            mock.patch.object(sync, "github_json", return_value=self.release),
            self.downloads(),
            mock.patch.object(sync, "bbsmc_json", side_effect=[[existing], self.uploaded]) as bbsmc,
            mock.patch.object(sync, "upload_files", return_value=None) as upload,
        ):
            self.assertIn("BBSMC OK", self.run_sync())
        upload.assert_called_once_with(
            self.metadata, {name: data for name, data in self.files.items() if name != self.filename},
            "token", version_id="new-version",
        )
        self.assertEqual(bbsmc.call_args_list[-1], mock.call("version/new-version", "token"))

    def test_partial_completion_only_adds_remaining_file_and_dry_run_cannot_write(self) -> None:
        existing = dict(self.uploaded, files=self.uploaded["files"][:2])
        with (
            mock.patch.object(sync, "github_json", return_value=self.release),
            self.downloads(),
            mock.patch.object(sync, "bbsmc_json", side_effect=[[existing], self.uploaded]),
            mock.patch.object(sync, "upload_files", return_value=None) as upload,
        ):
            self.assertIn("BBSMC OK", self.run_sync())
        last_name = list(self.files)[-1]
        upload.assert_called_once_with(
            self.metadata, {last_name: self.files[last_name]}, "token", version_id="new-version",
        )
        with (
            mock.patch.object(sync, "github_json", return_value=self.release),
            self.downloads(),
            mock.patch.object(sync, "bbsmc_json", return_value=[existing]),
            mock.patch.object(sync, "upload_files") as upload,
        ):
            self.assertIn("DRY-RUN OK", self.run_sync(dry_run=True))
        upload.assert_not_called()

    def test_missing_asset_empty_log_wrong_pack_version_or_missing_token_refuses_upload(self) -> None:
        for case in ("client", "full", "delta", "body", "version", "token"):
            release = copy.deepcopy(self.release)
            if case in ("client", "full", "delta"):
                release["assets"].pop(("client", "full", "delta").index(case))
            elif case == "body":
                release["body"] = " \n"
            elif case == "version":
                self.files[self.filename] = mrpack("0.3.40")
            with (
                self.subTest(case=case),
                mock.patch.object(sync, "github_json", return_value=release),
                self.downloads(),
                mock.patch.object(sync, "upload_files") as upload,
                self.assertRaises(ValueError),
            ):
                sync.sync_release("repo", "v0.3.41", "doVGSZq0", "" if case == "token" else "token")
            upload.assert_not_called()
        self.files[self.filename] = mrpack()

    def test_multipart_creation_and_append_preserve_all_bytes_and_handle_204(self) -> None:
        for append in (False, True):
            captured = []

            def open_url(request: object, timeout: int) -> io.BytesIO:
                captured.append(request)
                self.assertEqual(timeout, 600)
                response = io.BytesIO(b"" if append else json.dumps(self.uploaded).encode())
                response.status = 204 if append else 200
                return response

            with self.subTest(append=append), mock.patch.object(sync.urllib.request, "urlopen", side_effect=open_url):
                result = sync.upload_files(
                    self.metadata, self.files, "test-token", version_id="new-version" if append else None,
                )
                self.assertEqual(result, None if append else self.uploaded)
            request = captured[0]
            suffix = "version/new-version/file" if append else "version"
            self.assertEqual(request.full_url, f"https://api.bbsmc.net/v2/{suffix}")
            self.assertEqual(request.get_method(), "POST")
            self.assertEqual(request.get_header("Authorization"), "Bearer test-token")
            message = BytesParser(policy=default).parsebytes(
                f"Content-Type: {request.get_header('Content-type')}\r\nMIME-Version: 1.0\r\n\r\n".encode()
                + request.data,
            )
            parts = list(message.iter_parts())
            self.assertEqual(len(parts), 4)
            self.assertEqual(parts[0].get_param("name", header="content-disposition"), "data")
            self.assertEqual(json.loads(parts[0].get_payload(decode=True)), {} if append else self.metadata)
            self.assertEqual(
                {part.get_filename(): part.get_payload(decode=True) for part in parts[1:]}, self.files,
            )


if __name__ == "__main__":
    unittest.main()
