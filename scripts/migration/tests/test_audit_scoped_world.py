from __future__ import annotations

import hashlib
import io
import json
from pathlib import Path
import struct
import sys
import tempfile
import unittest
import zlib

import nbtlib as n

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from migration.audit_scoped_world import AuditError, audit, canonical, load_report


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_region(path: Path, chunk: n.Compound) -> None:
    document = n.File(chunk)
    payload_buffer = io.BytesIO()
    document.write(payload_buffer)
    payload = zlib.compress(payload_buffer.getvalue())
    header = bytearray(8192)
    struct.pack_into(">I", header, 0, (2 << 8) | 1)
    sector = struct.pack(">I", len(payload) + 1) + b"\x02" + payload
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(header + sector + b"\0" * (4096 - len(sector)))


class ScopedWorldAuditTest(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        root = Path(self.folder.name)
        self.world = root / "world"
        self.world.mkdir()
        (self.world / ".migration-incomplete").write_text(
            "payload_overlay_complete; full_offline_audit_required; do_not_start_minecraft\n"
        )
        self.region = self.world / "region" / "r.0.0.mca"
        self.state = n.Compound({"Name": n.String("minecraft:stone")})
        self.tile = n.Compound({
            "id": n.String("minecraft:chest"),
            "x": n.Int(1),
            "y": n.Int(64),
            "z": n.Int(2),
            "Items": n.List[n.Compound]([
                n.Compound({"id": n.String("minecraft:diamond"), "count": n.Int(48)})
            ]),
        })
        write_region(self.region, n.Compound({
            "DataVersion": n.Int(3955),
            "sections": n.List[n.Compound]([
                n.Compound({"block_states": n.Compound({
                    "palette": n.List[n.Compound]([self.state])
                })})
            ]),
            "block_entities": n.List[n.Compound]([self.tile]),
        }))
        self.plan = root / "plan.nbt"
        n.File(n.Compound({
            "format": n.Int(1),
            "complete": n.Byte(1),
            "target_data_version": n.Int(3955),
            "retention_policy": n.String("industrial_storage_players_v1"),
            "kept_block_ids": n.List[n.String]([n.String("minecraft:stone")]),
            "kept_entity_ids": n.List[n.String](),
            "block_state_rows": n.List[n.Compound]([
                n.Compound({"source": self.state, "data": self.state})
            ]),
            "base_files": n.List[n.Compound]([
                n.Compound({"path": n.String("region/r.0.0.mca"), "sha256": n.String("a" * 64)}),
                n.Compound({"path": n.String(".migration-incomplete"), "sha256": n.String("b" * 64)}),
            ]),
            "block_entities": n.List[n.Compound]([
                n.Compound({
                    "location": n.String("region/r.0.0.mca#0/block_entities[0]"),
                    "source": self.tile,
                    "data": self.tile,
                })
            ]),
            "entity_chunks": n.List[n.Compound](),
            "files": n.List[n.Compound](),
            "omit_files": n.List[n.String]([n.String(".migration-incomplete")]),
            "expected_block_entities": n.Int(1),
            "expected_retired_block_entities": n.Int(0),
            "expected_entities": n.Int(0),
            "expected_retired_entities": n.Int(0),
            "expected_entity_chunks": n.Int(0),
            "expected_output_chunks": n.Long(1),
        })).save(self.plan)
        self.report = root / "overlay.jsonl"
        self.write_report()

    def tearDown(self):
        self.folder.cleanup()

    def write_report(self):
        self.report.write_text("\n".join([
            json.dumps({"path": "region/r.0.0.mca", "operation": "complete_payload_overlay",
                        "chunks": 1, "target_sha256": digest(self.region)}),
            json.dumps({"payload_overlay_complete": True, "ready_for_minecraft": False,
                        "base_world_unchanged": True, "chunks": 1}),
            "",
        ]))

    def test_complete_fixture_matches_every_retained_payload(self):
        result = audit(self.world, self.plan, self.report, Path(self.folder.name) / "result.json")
        self.assertTrue(result["complete"])
        self.assertFalse(result["ready_for_minecraft"])
        self.assertEqual(result["retained_block_entities"], 1)
        self.assertEqual(result["target_block_ids"], {"minecraft:stone": 4096})
        self.assertEqual(result["block_ids_outside_catalogue"], [])

    def test_changed_inventory_and_incomplete_report_are_rejected(self):
        chunk = n.Compound({
            "DataVersion": n.Int(3955),
            "sections": n.List[n.Compound]([
                n.Compound({"block_states": n.Compound({
                    "palette": n.List[n.Compound]([self.state])
                })})
            ]),
            "block_entities": n.List[n.Compound]([
                n.Compound({
                    "id": n.String("minecraft:chest"),
                    "x": n.Int(1), "y": n.Int(64), "z": n.Int(2),
                    "Items": n.List[n.Compound]([
                        n.Compound({"id": n.String("minecraft:diamond"), "count": n.Int(47)})
                    ]),
                })
            ]),
        })
        write_region(self.region, chunk)
        self.write_report()
        with self.assertRaisesRegex(AuditError, "保留方块实体"):
            audit(self.world, self.plan, self.report, Path(self.folder.name) / "result.json")

        self.report.write_text(json.dumps({
            "payload_overlay_complete": True, "ready_for_minecraft": True,
            "base_world_unchanged": True, "chunks": 1,
        }) + "\n")
        with self.assertRaisesRegex(AuditError, "摘要不完整"):
            load_report(self.report)

    def test_canonical_preserves_nbt_types_and_ignores_compound_key_order(self):
        left = n.Compound({"count": n.Int(1), "value": n.Long(1)})
        right = n.Compound({"value": n.Long(1), "count": n.Int(1)})
        self.assertEqual(canonical(left), canonical(right))
        self.assertNotEqual(canonical(n.Int(1)), canonical(n.Long(1)))


if __name__ == "__main__":
    unittest.main()
