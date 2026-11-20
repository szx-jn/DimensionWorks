from copy import deepcopy
from pathlib import Path
import sys
import unittest

import nbtlib as n

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from migration.mod_item_data import convert_stored_item_data


def item(value):
    return n.Compound({"id": value["id"], "count": n.Int(int(value["Count"]))}) if value else n.Compound()


def target(source):
    return n.Compound({"id": source["id"], "count": n.Int(int(source["Count"])), "components": n.Compound({
        "minecraft:custom_data": deepcopy(source["tag"]), "minecraft:custom_name": n.String('"Keep"')})})


class ModItemDataTest(unittest.TestCase):
    def test_film_uses_entity_conversion_and_keeps_identity_and_nested_inventory(self):
        source = n.parse_nbt('{id:"touhou_little_maid:film",Count:1b,tag:{MaidInfo:{id:"touhou_little_maid:maid",'
                            'UUID:[I;1,2,3,4],Owner:[I;5,6,7,8],MaidTaskInventory:{Size:9,Items:[{Slot:3,id:"minecraft:bread",Count:32b}]}}}}')
        original = deepcopy(source)
        calls = []
        def entity(value):
            calls.append(deepcopy(value))
            result = deepcopy(value)
            result["MaidTaskInventory"]["Items"] = n.List[n.Compound](item(v) for v in value["MaidTaskInventory"]["Items"])
            return result
        data = convert_stored_item_data(source, target(source), item, entity).data["components"]
        stored = data["touhou_little_maid:maid_info"]
        self.assertEqual(stored["UUID"].tolist(), [1, 2, 3, 4])
        self.assertEqual(stored["Owner"].tolist(), [5, 6, 7, 8])
        self.assertEqual(stored["MaidTaskInventory"]["Items"][0]["count"], 32)
        self.assertEqual(len(calls), 1)
        self.assertNotIn("minecraft:custom_data", data)
        self.assertEqual(source, original)

    def test_recipe_duplicate_inputs_output_and_flexible_mode_survive(self):
        source = n.parse_nbt('{id:"kaleidoscope_cookery:recipe_item",Count:1b,tag:{RecipeRecord:{type:"kaleidoscope_cookery:pot",'
                            'input:[{id:"minecraft:ender_pearl",Count:1b},{id:"minecraft:ender_pearl",Count:1b}],'
                            'output:{id:"kaleidoscope_cookery:sweet_and_sour_ender_pearls",Count:2b},flexRecipe:1b}}}')
        data = convert_stored_item_data(source, target(source), item, deepcopy).data["components"]["kaleidoscope_cookery:recipe_record"]
        self.assertEqual(len(data["input"]), 2)
        self.assertEqual(data["input"][0], data["input"][1])
        self.assertEqual(data["output"]["count"], 2)
        self.assertEqual(data["flex_recipe"], n.Byte(1))
        self.assertEqual(data["type"], "kaleidoscope_cookery:pot")

    def test_wine_quality_count_and_unrelated_custom_data_are_retained(self):
        source = n.parse_nbt('{id:"kaleidoscope_tavern:wine",Count:7b,tag:{BrewLevel:6,privateFlag:42L}}')
        data = convert_stored_item_data(source, target(source), item, deepcopy).data
        self.assertEqual(data["count"], 7)
        self.assertEqual(data["components"]["kaleidoscope_tavern:brew_level"], n.Int(6))
        self.assertEqual(data["components"]["minecraft:custom_data"], {"privateFlag": n.Long(42)})
        self.assertEqual(data["components"]["minecraft:custom_name"], '"Keep"')

    def test_unknown_record_fields_changed_dfu_and_existing_components_block(self):
        source = n.parse_nbt('{id:"kaleidoscope_cookery:recipe_item",Count:1b,tag:{RecipeRecord:{type:"kaleidoscope_cookery:pot",'
                            'input:[],output:{id:"minecraft:bread",Count:1b},flexRecipe:0b,unknown:1}}}')
        with self.assertRaisesRegex(ValueError, "未覆盖字段"):
            convert_stored_item_data(source, target(source), item, deepcopy)
        source = n.parse_nbt('{id:"kaleidoscope_tavern:wine",Count:1b,tag:{BrewLevel:6}}')
        data = target(source); data["components"]["minecraft:custom_data"]["BrewLevel"] = n.Int(0)
        with self.assertRaisesRegex(ValueError, "与输入不符"):
            convert_stored_item_data(source, data, item, deepcopy)
        data = target(source); data["components"]["kaleidoscope_tavern:brew_level"] = n.Int(0)
        with self.assertRaisesRegex(ValueError, "组件冲突"):
            convert_stored_item_data(source, data, item, deepcopy)


if __name__ == "__main__":
    unittest.main()
