from pathlib import Path
import sys
import unittest
from copy import deepcopy
import nbtlib as n

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from migration.ae_data import convert_key, convert_storage_cell


class AeDataTest(unittest.TestCase):
    def item(self, source):
        self.received = deepcopy(source)
        return n.Compound({"id": source["id"], "count": n.Int(1),
                           "components": n.Compound({"minecraft:custom_data": source.get("tag", n.Compound())})})

    def source(self):
        return n.Compound({"id": n.String("ae2:item_storage_cell_256k"), "Count": n.Byte(1), "tag": n.Compound({
            "keys": n.List[n.Compound]([
                n.Compound({"#c": n.String("ae2:i"), "id": n.String("minecraft:diamond_pickaxe"),
                            "tag": n.Compound({"quota": n.Long(9000000000000000)}), "caps": n.Compound()})]),
            "amts": n.LongArray([9000000000000000]), "ic": n.Long(9000000000000000)})})

    def target(self, source):
        return n.Compound({"id": source["id"], "count": n.Int(1), "components": n.Compound({
            "minecraft:custom_data": deepcopy(source["tag"]), "minecraft:custom_name": n.String('"Keep"')})})

    def test_cell_preserves_long_storage_component_metadata_and_source(self):
        source = self.source(); original = deepcopy(source)
        result = convert_storage_cell(source, self.target(source), self.item)
        entry = result["components"]["ae2:storage_cell_inv"][0]
        self.assertEqual(entry["#"], n.Long(9000000000000000))
        self.assertEqual(entry["#t"], "ae2:i")
        self.assertNotIn("#c", entry)
        self.assertIsInstance(entry["#"], n.Long)
        self.assertEqual(entry["components"]["minecraft:custom_data"]["quota"], 9000000000000000)
        self.assertEqual(result["components"]["minecraft:custom_name"], '"Keep"')
        self.assertNotIn("minecraft:custom_data", result["components"])
        self.assertEqual(self.received["Count"], 1)
        self.assertIn("ForgeCaps", self.received)
        self.assertEqual(source, original)

    def test_fluid_amount_is_preserved_without_item_count_or_unit_conversion(self):
        source = n.Compound({"#c": n.String("ae2:f"), "id": n.String("minecraft:lava"), "#": n.Long(16000)})
        self.assertEqual(convert_key(source, self.item), {
            "#t": n.String("ae2:f"), "id": source["id"], "#": source["#"],
        })
        self.assertFalse(hasattr(self, "received"))

    def test_storage_array_length_and_cached_total_mismatches_block_conversion(self):
        source = self.source(); source["tag"]["ic"] = n.Long(5)
        with self.assertRaisesRegex(ValueError, "缓存总量"):
            convert_storage_cell(source, self.target(source), self.item)
        source["tag"]["amts"] = n.LongArray([])
        with self.assertRaisesRegex(ValueError, "数组长度"):
            convert_storage_cell(source, self.target(source), self.item)

    def test_unknown_key_types_and_extra_fluid_metadata_are_not_discarded(self):
        with self.assertRaisesRegex(ValueError, "未审计"):
            convert_key(n.Compound({"#c": n.String("appmek:chemical"), "id": n.String("mekanism:oxygen")}), self.item)
        with self.assertRaisesRegex(ValueError, "未覆盖"):
            convert_key(n.Compound({"#c": n.String("ae2:f"), "id": n.String("minecraft:lava"),
                                    "tag": n.Compound({"unknown": n.Int(1)})}), self.item)

    def test_existing_component_and_changed_custom_data_block_conversion(self):
        source = self.source(); target = self.target(source)
        target["components"]["ae2:storage_cell_inv"] = n.List[n.Compound]()
        with self.assertRaisesRegex(ValueError, "组件冲突"):
            convert_storage_cell(source, target, self.item)
        target = self.target(source); target["components"]["minecraft:custom_data"]["ic"] = n.Long(0)
        with self.assertRaisesRegex(ValueError, "与输入不符"):
            convert_storage_cell(source, target, self.item)


if __name__ == "__main__":
    unittest.main()
