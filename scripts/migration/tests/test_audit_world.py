from __future__ import annotations

from collections import Counter
import io
from pathlib import Path
import struct
import sys
import tempfile
import unittest
import zlib

import nbtlib as n

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from migration.audit_world import Audit, palette_counts, region_chunks


class WorldAuditTest(unittest.TestCase):
    def test_padded_palette_counts_every_block(self):
        palette = n.List[n.Compound]([n.Compound({"Name": n.String(f"minecraft:block_{i}")}) for i in range(17)])
        values = [i % 17 for i in range(4096)]
        words = []
        for start in range(0, 4096, 12):
            words.append(sum(value << (i * 5) for i, value in enumerate(values[start:start + 12])))
        states = n.Compound({"palette": palette, "data": n.LongArray(words)})
        self.assertEqual(palette_counts(states), Counter(f"minecraft:block_{i}" for i in values))

    def test_stock_ledger_uses_amount_instead_of_prototype_count(self):
        root = n.Compound({"data": n.Compound({"Channels": n.List[n.Compound]([
            n.Compound({"Number": n.Int(2), "Items": n.List[n.Compound]([
                n.Compound({"Stack": n.Compound({"id": n.String("minecraft:diamond"), "Count": n.Byte(1)}),
                            "Amount": n.Int(48), "Maximum": n.Int(64), "Origin": n.String("dw:transfer")})
            ])})
        ])})})
        audit = Audit()
        audit.saved_data(root, "data/dimensionworks_logistics_stock.dat")
        item = audit.custom_ledgers["data/dimensionworks_logistics_stock.dat"][0]["items"][0]
        self.assertEqual(item["amount"], 48)
        self.assertEqual(item["stack"]["count"], 1)
        self.assertEqual(item["origin"], "dw:transfer")

    def test_signed_packed_words_preserve_high_palette_bits(self):
        palette = n.List[n.Compound]([n.Compound({"Name": n.String(f"minecraft:block_{i}")}) for i in range(16)])
        states = n.Compound({"palette": palette, "data": n.LongArray([-1] * 256)})
        self.assertEqual(palette_counts(states), Counter({"minecraft:block_15": 4096}))

    def test_different_states_of_same_block_are_counted_together(self):
        palette = n.List[n.Compound]([
            n.Compound({"Name": n.String("minecraft:oak_log"), "Properties": n.Compound({"axis": n.String(axis)})})
            for axis in ("x", "y")
        ])
        states = n.Compound({"palette": palette, "data": n.LongArray([0x1010101010101010] * 256)})
        self.assertEqual(palette_counts(states), Counter({"minecraft:oak_log": 4096}))

    def test_unwritten_region_placeholder_has_no_chunk_records(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "r.0.0.mca"
            path.touch()
            self.assertEqual(list(region_chunks(path)), [])

    def test_old_identity_backups_are_not_counted_as_active_players(self):
        with tempfile.TemporaryDirectory() as folder:
            world = Path(folder)
            n.File({"Data": n.Compound(), "fml": n.Compound({"Registries": n.Compound({
                "minecraft:item": n.Compound({"ids": n.List[n.Compound]([
                    n.Compound({"K": n.String("witherstormmod:unused")})
                ])})
            })})}, gzipped=True).save(world / "level.dat")
            historical = world / "trueuuid-backups" / "playerdata"
            historical.mkdir(parents=True)
            n.File({"Inventory": n.List[n.Compound]([
                n.Compound({"id": n.String("minecraft:diamond"), "Count": n.Byte(48)})
            ])}, gzipped=True).save(historical / "old.dat")
            result = Audit().scan(world)
            self.assertTrue(result["complete"])
            self.assertEqual(result["players"], {})
            self.assertEqual(result["item_stacks"], [])
            self.assertEqual(len(result["archived_files"]), 1)
            self.assertEqual(result["witherstorm_references"], {})
            self.assertIn("minecraft:item", result["registries"])

    def test_malformed_owner_blocks_audit_instead_of_discarding_inventory(self):
        audit = Audit()
        root = n.Compound({"data": n.Compound({"Owners": n.Compound({"invalid-owner": n.Compound()})})})
        with self.assertRaises(ValueError):
            audit.saved_data(root, "data/dw_cloud_disk.dat")

    def test_region_reads_nested_stacks_and_rejects_corrupt_sectors(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "r.0.0.mca"
            document = n.File({"DataVersion": n.Int(3465), "block_entities": n.List[n.Compound]([
                n.Compound({"id": n.String("minecraft:chest"), "Items": n.List[n.Compound]([
                    n.Compound({"id": n.String("minecraft:diamond"), "Count": n.Byte(48)})
                ])})
            ])})
            buffer = io.BytesIO()
            document.write(buffer)
            compressed = zlib.compress(buffer.getvalue())
            header = bytearray(8192)
            struct.pack_into(">I", header, 0, (2 << 8) | 1)
            payload = struct.pack(">I", len(compressed) + 1) + b"\x02" + compressed
            path.write_bytes(header + payload + b"\0" * (4096 - len(payload)))
            audit = Audit()
            for index, root in region_chunks(path):
                audit.document(root, f"region/r.0.0.mca#{index}")
            self.assertEqual(audit.items[0]["id"], "minecraft:diamond")
            self.assertEqual(audit.items[0]["count"], 48)
            # 源端大量文件没有补齐最后一个扇区；完整 payload 仍应正常读取。
            path.write_bytes(header + payload)
            self.assertEqual(len(list(region_chunks(path))), 1)
            path.write_bytes(header + payload[:-1])
            with self.assertRaisesRegex(ValueError, "数据长度"):
                list(region_chunks(path))
            # 第二个槽占用同一扇区，不能让损坏存档被报告为完整。
            struct.pack_into(">I", header, 4, (2 << 8) | 1)
            path.write_bytes(header + payload + b"\0" * (4096 - len(payload)))
            with self.assertRaisesRegex(ValueError, "重叠"):
                list(region_chunks(path))


if __name__ == "__main__":
    unittest.main()
