from copy import deepcopy
from pathlib import Path
import sys
import unittest

import nbtlib as n

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from migration.registry_ids import convert_enchantment_components, convert_infinity_cell, mapped_id


class RegistryIdTest(unittest.TestCase):
    def test_reaper_alias_keeps_level_and_other_enchantments(self):
        source = n.parse_nbt('{id:"minecraft:diamond_sword",count:1,components:{"minecraft:enchantments":{levels:{"draconicevolution:reaper_enchantment":3,"minecraft:sharpness":5},show_in_tooltip:0b}}}')
        original = deepcopy(source)
        result = convert_enchantment_components(source, {"draconicevolution:reaper", "minecraft:sharpness"})
        component = result.data["components"]["minecraft:enchantments"]
        self.assertEqual(component["levels"], {"draconicevolution:reaper": n.Int(3), "minecraft:sharpness": n.Int(5)})
        self.assertEqual(component["show_in_tooltip"], n.Byte(0))
        self.assertEqual(result.removed, {"draconicevolution:reaper_enchantment": n.Int(3)})
        self.assertEqual(source, original)

    def test_unknown_or_colliding_enchantment_cannot_be_silently_replaced(self):
        source = n.parse_nbt('{id:"minecraft:diamond_sword",count:1,components:{"minecraft:enchantments":{levels:{"dungeonsdelight:unapproved_effect":1}}}}')
        with self.assertRaisesRegex(ValueError, "无法直接升级"):
            convert_enchantment_components(source, {"dungeonsdelight:serrated_strike"})
        source = n.parse_nbt('{id:"minecraft:diamond_sword",count:1,components:{"minecraft:enchantments":{levels:{"draconicevolution:reaper_enchantment":1,"draconicevolution:reaper":2}}}}')
        with self.assertRaisesRegex(ValueError, "冲突"):
            convert_enchantment_components(source, {"draconicevolution:reaper"})

    def test_approved_retirement_keeps_item_and_full_original_enchantment_records(self):
        old = n.parse_nbt('{id:"minecraft:enchanted_book",Count:3b,tag:{StoredEnchantments:[{id:"dungeonsdelight:life_grasp",lvl:2s},{id:"minecraft:mending",lvl:1s}],Enchantments:[{id:"minecraft:unbreaking",lvl:3s}]}}')
        target = n.parse_nbt('{id:"minecraft:enchanted_book",count:3,components:{"minecraft:stored_enchantments":{levels:{"dungeonsdelight:life_grasp":2,"minecraft:mending":1},show_in_tooltip:0b},"minecraft:enchantments":{levels:{"minecraft:unbreaking":3}},"minecraft:custom_data":{owner:[I;1,2,3,4],dimensionworks_migration:{original_potion:"witherstormmod:old"}}}}')
        original = deepcopy(target)
        result = convert_enchantment_components(target, {"minecraft:mending", "minecraft:unbreaking"}, source=old)
        self.assertEqual(result.data["id"], target["id"])
        self.assertEqual(result.data["count"], target["count"])
        components = result.data["components"]
        self.assertEqual(components["minecraft:stored_enchantments"], n.parse_nbt('{levels:{"minecraft:mending":1},show_in_tooltip:0b}'))
        self.assertEqual(components["minecraft:enchantments"], target["components"]["minecraft:enchantments"])
        custom = components["minecraft:custom_data"]
        self.assertEqual(custom["owner"], target["components"]["minecraft:custom_data"]["owner"])
        archive = custom["dimensionworks_migration"]
        self.assertEqual(archive["original_potion"], "witherstormmod:old")
        self.assertEqual(archive["original_enchantments"]["tag"], old["tag"])
        self.assertEqual(archive["original_enchantments"]["components"], {key: target["components"][key] for key in
                                                                        ("minecraft:enchantments", "minecraft:stored_enchantments")})
        self.assertEqual(target, original)
        self.assertEqual(convert_enchantment_components(result.data, {"minecraft:mending", "minecraft:unbreaking"}).data, result.data)

    def test_retirement_cannot_overwrite_an_existing_archive(self):
        target = n.parse_nbt('{id:"minecraft:enchanted_book",count:1,components:{"minecraft:stored_enchantments":{levels:{"dungeonsdelight:life_grasp":1}},"minecraft:custom_data":{dimensionworks_migration:{original_enchantments:{old:1}}}}}')
        original = deepcopy(target)
        with self.assertRaisesRegex(ValueError, "源记录冲突"):
            convert_enchantment_components(target, set())
        self.assertEqual(target, original)

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
