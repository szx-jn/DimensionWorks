from copy import deepcopy
import unittest

import nbtlib as n

from scripts.migration.auxiliary_items import convert_auxiliary_item


def target(source):
    return n.Compound({"id": source["id"], "count": n.Int(1),
                       "components": n.Compound({"minecraft:custom_data": deepcopy(source["tag"])})})


class AuxiliaryItemTests(unittest.TestCase):
    def test_tome_and_template_use_their_distinct_native_enchantment_components(self):
        for id, key in [("quark:ancient_tome", "quark:tome_enchantments"),
                        ("create_enchantment_industry:enchanting_template", "minecraft:stored_enchantments")]:
            old = n.Compound({"id": n.String(id), "Count": n.Byte(1), "tag": n.parse_nbt('{StoredEnchantments:[{id:"minecraft:sharpness",lvl:6s}]}')})
            original = deepcopy(old)
            def convert(value):
                self.assertEqual(value, old["tag"]["StoredEnchantments"])
                return n.parse_nbt('{levels:{"minecraft:sharpness":6}}')
            result = convert_auxiliary_item(old, target(old), convert)
            self.assertEqual(result["components"], {key: n.parse_nbt('{levels:{"minecraft:sharpness":6}}')})
            self.assertEqual(old, original)

    def test_vanilla_sweeping_rename_is_accepted_only_with_equal_full_levels(self):
        old = n.parse_nbt('{id:"quark:ancient_tome",Count:1b,tag:{StoredEnchantments:[{id:"minecraft:sweeping",lvl:3s}]}}')
        fixed = target(old)
        fixed["components"]["minecraft:custom_data"]["StoredEnchantments"][0]["id"] = n.String("minecraft:sweeping_edge")
        def convert(value):
            levels = n.Compound({"minecraft:sweeping_edge": n.Int(int(value[0]["lvl"]))})
            return n.Compound({"levels": levels})
        result = convert_auxiliary_item(old, fixed, convert)
        self.assertEqual(result["components"]["quark:tome_enchantments"], n.parse_nbt('{levels:{"minecraft:sweeping_edge":3}}'))
        fixed["components"]["minecraft:custom_data"]["StoredEnchantments"][0]["lvl"] = n.Short(2)
        with self.assertRaisesRegex(ValueError, "等级表不符"):
            convert_auxiliary_item(old, fixed, convert)

    def test_magic_map_id_is_retained_and_pathfinder_target_stays_usable(self):
        old = n.parse_nbt('{id:"twilightforest:filled_magic_map",Count:1b,tag:{map:183}}')
        result = convert_auxiliary_item(old, target(old), None)
        self.assertEqual(result["components"], {"minecraft:map_id": n.Int(183)})
        old = n.parse_nbt('{id:"quark:pathfinders_quill",Count:1b,tag:{targetBiome:"minecraft:flower_forest",targetBiomeColor:13518562,targetBiomeUnderground:0b}}')
        result = convert_auxiliary_item(old, target(old), None)
        self.assertEqual(result["components"], n.parse_nbt('{"quark:target_biome":"minecraft:flower_forest","quark:target_biome_color":13518562,"quark:target_biome_underground":0b}'))

    def test_component_collision_stops_instead_of_overwriting_map_identity(self):
        old = n.parse_nbt('{id:"twilightforest:filled_magic_map",Count:1b,tag:{map:183}}')
        value = target(old); value["components"]["minecraft:map_id"] = n.Int(192)
        with self.assertRaisesRegex(ValueError, "冲突"):
            convert_auxiliary_item(old, value, None)


if __name__ == "__main__":
    unittest.main()
