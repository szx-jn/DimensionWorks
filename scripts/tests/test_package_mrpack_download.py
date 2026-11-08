from __future__ import annotations

import io
import json
import sys
import tempfile
import unittest
import urllib.error
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts"))

import package_mrpack  # noqa: E402


class _Response(io.BytesIO):
    def __enter__(self) -> "_Response":
        return self

    def __exit__(self, *args: object) -> None:
        self.close()


class GitHubReleaseDownloadFallbackTest(unittest.TestCase):
    def test_uses_api_asset_endpoint_after_browser_download_5xx(self) -> None:
        browser_url = (
            "https://github.com/example/project/releases/download/v1.2.3/example.jar"
        )
        release_api = "https://api.github.com/repos/example/project/releases/tags/v1.2.3"
        asset_api = "https://api.github.com/repos/example/project/releases/assets/42"
        release_body = json.dumps(
            {
                "assets": [
                    {
                        "name": "example.jar",
                        "url": asset_api,
                    }
                ]
            }
        ).encode()

        def open_url(request: object, timeout: int) -> _Response:
            url = request.full_url
            if url == browser_url:
                raise urllib.error.HTTPError(browser_url, 504, "Gateway Time-out", {}, None)
            if url == release_api:
                return _Response(release_body)
            if url == asset_api:
                return _Response(b"jar-bytes")
            raise AssertionError(f"unexpected URL: {url}")

        with tempfile.TemporaryDirectory() as temp_dir:
            destination = Path(temp_dir) / "example.jar"
            with (
                mock.patch.dict(package_mrpack.os.environ, {"GH_TOKEN": "test-token"}),
                mock.patch.object(package_mrpack.urllib.request, "urlopen", side_effect=open_url),
                mock.patch.object(package_mrpack.time, "sleep"),
            ):
                package_mrpack.download(browser_url, destination)

            self.assertEqual(destination.read_bytes(), b"jar-bytes")


if __name__ == "__main__":
    unittest.main()
