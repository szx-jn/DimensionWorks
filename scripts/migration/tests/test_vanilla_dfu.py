from copy import deepcopy
from pathlib import Path
import sys
import unittest

import nbtlib as n

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from migration.vanilla_dfu import VanillaDfu
from migration.entity_data import convert_entity_data


class AttributeContextDfu(VanillaDfu):
    def __init__(self, fixed):
        self.fixed = fixed
        self.calls = []

    def update(self, value, reference):
        self.calls.append((reference, deepcopy(value)))
        if reference == "entity":
            return deepcopy(self.fixed)
        if reference != "player" or set(value) != {"Attributes"}:
            raise AssertionError("属性转换不能把整个模组实体当作玩家")
        return n.parse_nbt('{attributes:[{id:"forge:entity_gravity",base:0.02d}]}')


class EntityAttributeContextTest(unittest.TestCase):
    def test_unknown_mod_entity_uses_only_its_attribute_list_in_known_dfu_context(self):
        source = n.parse_nbt('{id:"touhou_little_maid:maid",UUID:[I;1,2,3,4],'
                            'Attributes:[{Name:"forge:entity_gravity",Base:0.02d}],'
                            'MaidTaskInventory:{Size:9,Items:[{Slot:3,id:"minecraft:bread",Count:32b}]}}')
        original = deepcopy(source)
        dfu = AttributeContextDfu(source)
        result = dfu.update_entity(source)
        self.assertEqual([reference for reference, _ in dfu.calls], ["entity", "player"])
        self.assertEqual(result["id"], source["id"])
        self.assertEqual(result["UUID"].tolist(), [1, 2, 3, 4])
        self.assertEqual(result["MaidTaskInventory"], source["MaidTaskInventory"])
        self.assertNotIn("Attributes", result)
        self.assertEqual(convert_entity_data(result).data["attributes"][0]["id"], "minecraft:generic.gravity")
        self.assertEqual(source, original)

    def test_already_fixed_vanilla_attributes_do_not_run_a_second_dfu(self):
        source = n.parse_nbt('{id:"minecraft:zombie",attributes:[{id:"minecraft:generic.max_health",base:20.0d}]}')
        dfu = AttributeContextDfu(source)
        self.assertEqual(dfu.update_entity(source), source)
        self.assertEqual(len(dfu.calls), 1)

    def test_conflicting_or_unconverted_attribute_lists_block_instead_of_repeating(self):
        source = n.parse_nbt('{id:"touhou_little_maid:maid",Attributes:[],attributes:[]}')
        with self.assertRaisesRegex(ValueError, "属性列表冲突"):
            AttributeContextDfu(source).update_entity(source)
        with self.assertRaisesRegex(ValueError, "尚未通过"):
            convert_entity_data(n.parse_nbt('{id:"touhou_little_maid:maid",Attributes:[]}'))

    def test_vanilla_bee_redundant_legacy_attributes_require_equal_native_result(self):
        source = n.parse_nbt('{id:"minecraft:bee",Attributes:[{Name:"forge:entity_gravity",Base:0.02d}]}')
        fixed = deepcopy(source)
        fixed["attributes"] = n.parse_nbt('[{id:"forge:entity_gravity",base:0.02d}]')
        result = AttributeContextDfu(fixed).update_entity(source)
        self.assertNotIn("Attributes", result)
        self.assertEqual(result["attributes"], fixed["attributes"])
        fixed["attributes"][0]["base"] = n.Double(0.1)
        with self.assertRaisesRegex(ValueError, "结果冲突"):
            AttributeContextDfu(fixed).update_entity(source)


if __name__ == "__main__":
    unittest.main()
