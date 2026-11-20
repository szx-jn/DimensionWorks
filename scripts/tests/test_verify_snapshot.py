from __future__ import annotations

import hashlib
import io
import json
from pathlib import Path
import sys
import tarfile
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from migration.verify_snapshot import SnapshotError, sha256, verify_and_extract


class SnapshotTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.base = Path(self.temporary.name)
        self.snapshot = self.base / "snapshot"
        self.snapshot.mkdir()
        self.destination = self.base / "verified"

    def create(self, *, omit=False, traversal=False, link=False, bad_file_hash=False):
        files = {"VERSION": b"0.3.52\n", "world/playerdata/a.dat": b"inventory-and-uuid"}
        rows = [{"path": name, "size": len(data), "sha256": hashlib.sha256(data).hexdigest()}
                for name, data in files.items()]
        if bad_file_hash:
            rows[-1]["sha256"] = "0" * 64
        (self.snapshot / "inventory.json").write_text(json.dumps(rows))
        archive = self.snapshot / "server.tar.gz"
        with tarfile.open(archive, "w:gz") as stream:
            for name, data in files.items():
                if omit and name != "VERSION":
                    continue
                member = tarfile.TarInfo("./" + name)
                member.size = len(data)
                stream.addfile(member, io.BytesIO(data))
            if traversal or link:
                member = tarfile.TarInfo("../escaped" if traversal else "link")
                if link:
                    member.type = tarfile.SYMTYPE
                    member.linkname = "../escaped"
                stream.addfile(member)
        report = {"archive": archive.name, "archive_bytes": archive.stat().st_size,
                  "archive_sha256": sha256(archive), "inventory_sha256": sha256(self.snapshot / "inventory.json"),
                  "file_count": len(rows), "payload_bytes": sum(row["size"] for row in rows),
                  "source_version": "0.3.52", "source_unchanged": True, "cold_checked_before_and_after": True}
        (self.snapshot / "complete.json").write_text(json.dumps(report))

    def test_verified_copy_preserves_player_data_and_refuses_overwrite(self):
        self.create()
        result = verify_and_extract(self.snapshot, self.destination)
        self.assertEqual(result["file_count"], 2)
        self.assertEqual((self.destination / "world/playerdata/a.dat").read_bytes(), b"inventory-and-uuid")
        with self.assertRaisesRegex(SnapshotError, "拒绝覆盖"):
            verify_and_extract(self.snapshot, self.destination)

    def test_damaged_download_is_rejected_before_creating_destination(self):
        self.create()
        with (self.snapshot / "server.tar.gz").open("ab") as stream:
            stream.write(b"damaged")
        with self.assertRaisesRegex(SnapshotError, "压缩包"):
            verify_and_extract(self.snapshot, self.destination)
        self.assertFalse(self.destination.with_name("verified.partial").exists())

    def test_missing_or_changed_player_file_never_promotes_partial_copy(self):
        for option in ("omit", "bad_file_hash"):
            with self.subTest(option=option):
                self.create(**{option: True})
                destination = self.base / option
                with self.assertRaises(SnapshotError):
                    verify_and_extract(self.snapshot, destination)
                self.assertFalse(destination.exists())

    def test_unsafe_archive_cannot_write_outside_copy(self):
        for option in ("traversal", "link"):
            with self.subTest(option=option):
                self.create(**{option: True})
                with self.assertRaises(SnapshotError):
                    verify_and_extract(self.snapshot, self.base / option)
                self.assertFalse((self.base / "escaped").exists())


if __name__ == "__main__":
    unittest.main()
