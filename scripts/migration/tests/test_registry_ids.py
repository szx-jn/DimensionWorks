from copy import deepcopy
from pathlib import Path
import sys
import unittest

import nbtlib as n

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from migration.registry_ids import convert_infinity_cell, mapped_id


class RegistryIdTest(unittest.TestCase):
    def test_missing_inventory_id_or_unlisted_namespace_member_is_not_replaced(self):
        with self.assertRaisesRegex(ValueError, "没有已核对"):
            mapped_id("miners_delight:bat_wing", {"minecraft:air"})
        with self.assertRaisesRegex(ValueError, "没有已核对"):
            mapped_id("miners_delight:unreviewed", {"minersdelight:unreviewed"})
        self.assertEqual(mapped_id("expatternprovider:ex_import_bus_part", {"extendedae:ex_import_bus_part"}),
                         "extendedae:ex_import_bus_part")
        self.assertEqual(mapped_id("brewincompatdelight:wild_white_grape", {"cocktailsdelight:wild_white_grapes"}, block=True),
                         "cocktailsdelight:wild_white_grapes")

    def source(self):
        return n.parse_nbt('{id:"expatternprovider:infinity_cell",Count:1b,tag:{record:{"#c":"ae2:i",id:"minecraft:cobblestone"}}}')

    def target(self, source):
        return n.Compound({"id": source["id"], "count": n.Int(1),
                           "components": n.Compound({"minecraft:custom_data": deepcopy(source["tag"])})})

    def test_infinite_cell_variant_is_selected_from_actual_record_and_logged(self):
        source = self.source(); original = deepcopy(source)
        converted = convert_infinity_cell(source, self.target(source), {"extendedae:infinity_cobblestone_cell"})
        self.assertEqual(converted.data["id"], "extendedae:infinity_cobblestone_cell")
        self.assertEqual(converted.data["count"], 1)
        self.assertNotIn("components", converted.data)
        self.assertEqual(converted.removed["record"], source["tag"]["record"])
        self.assertEqual(source, original)

    def test_other_infinite_records_or_extra_metadata_cannot_use_cobblestone_default(self):
        source = self.source(); source["tag"]["record"] = n.parse_nbt('{"#c":"ae2:f",id:"minecraft:water"}')
        with self.assertRaisesRegex(ValueError, "圆石记录"):
            convert_infinity_cell(source, self.target(source), {"extendedae:infinity_cobblestone_cell"})
        source = self.source(); source["tag"]["owner"] = n.IntArray([1, 2, 3, 4])
        with self.assertRaisesRegex(ValueError, "圆石记录"):
            convert_infinity_cell(source, self.target(source), {"extendedae:infinity_cobblestone_cell"})
        source = self.source(); data = self.target(source); data["components"]["minecraft:custom_data"]["record"]["id"] = n.String("minecraft:dirt")
        with self.assertRaisesRegex(ValueError, "与输入不符"):
            convert_infinity_cell(source, data, {"extendedae:infinity_cobblestone_cell"})


if __name__ == "__main__":
    unittest.main()
