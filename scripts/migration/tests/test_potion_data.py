from copy import deepcopy
import unittest

import nbtlib as n

from scripts.migration.potion_data import convert_potion_contents


class PotionDataTests(unittest.TestCase):
    def test_namespace_upgrade_requires_exact_native_effect_duration_and_level(self):
        source = n.parse_nbt('{id:"minecraft:tipped_arrow",count:12,components:{"minecraft:potion_contents":{potion:"miners_delight:strong_mining_fatigue",custom_color:123}}}')
        original = deepcopy(source)
        catalog = n.parse_nbt('{"minersdelight:strong_mining_fatigue":[{id:"minecraft:mining_fatigue",duration:1800,amplifier:1b}]}')
        result = convert_potion_contents(source, set(catalog), {"minecraft:mining_fatigue"}, catalog)
        self.assertEqual(result.data["count"], 12)
        self.assertEqual(result.data["components"]["minecraft:potion_contents"]["potion"], "minersdelight:strong_mining_fatigue")
        self.assertEqual(result.data["components"]["minecraft:potion_contents"]["custom_color"], 123)
        self.assertEqual(source, original)
        catalog["minersdelight:strong_mining_fatigue"][0]["duration"] = n.Int(180)
        with self.assertRaisesRegex(ValueError, "不一致"):
            convert_potion_contents(source, set(catalog), {"minecraft:mining_fatigue"}, catalog)

    def test_l2_preset_and_custom_effect_alias_keep_custom_parameters(self):
        source = n.parse_nbt('{id:"minecraft:potion",count:1,components:{"minecraft:potion_contents":{potion:"l2complements:strong_flame",custom_effects:[{id:"l2complements:frozen",duration:37,amplifier:2b,ambient:1b}]}}}')
        catalog = n.parse_nbt('{"l2complements:strong_soul_burning":[{id:"l2complements:soul_burning",duration:400,amplifier:1b}]}')
        result = convert_potion_contents(source, set(catalog), {"l2complements:soul_burning", "l2complements:frost"}, catalog)
        effects = result.data["components"]["minecraft:potion_contents"]["custom_effects"]
        self.assertEqual(effects, n.parse_nbt('[{id:"l2complements:frost",duration:37,amplifier:2b,ambient:1b}]'))

    def test_removed_wither_mod_preserves_arrow_effects_order_quantity_and_source_record(self):
        source = n.parse_nbt('{id:"minecraft:tipped_arrow",count:64,components:{"minecraft:custom_data":{owner:"keep"},"minecraft:potion_contents":{potion:"witherstormmod:strong_wither",custom_color:42,custom_effects:[{id:"minecraft:slowness",duration:77,amplifier:2b}]}}}')
        original = deepcopy(source)
        result = convert_potion_contents(source, set(), {"minecraft:wither", "minecraft:slowness"}, None)
        potion = result.data["components"]["minecraft:potion_contents"]
        self.assertNotIn("potion", potion)
        self.assertEqual(potion["custom_color"], 42)
        self.assertEqual(potion["custom_effects"], n.parse_nbt('[{id:"minecraft:wither",duration:432,show_icon:1b,amplifier:1b},{id:"minecraft:slowness",duration:77,amplifier:2b}]'))
        self.assertEqual(result.data["count"], 64)
        custom = result.data["components"]["minecraft:custom_data"]
        self.assertEqual(custom["owner"], "keep")
        self.assertEqual(custom["dimensionworks_migration"]["original_potion"], "witherstormmod:strong_wither")
        self.assertEqual(source, original)

    def test_unknown_potion_and_missing_native_evidence_stop_conversion(self):
        source = n.parse_nbt('{id:"minecraft:potion",count:1,components:{"minecraft:potion_contents":{potion:"missing:important"}}}')
        with self.assertRaisesRegex(ValueError, "无法直接升级"):
            convert_potion_contents(source, set(), set(), None)
        source["components"]["minecraft:potion_contents"]["potion"] = n.String("l2complements:flame")
        with self.assertRaisesRegex(ValueError, "证据"):
            convert_potion_contents(source, {"l2complements:soul_burning"}, {"l2complements:soul_burning"}, None)


if __name__ == "__main__":
    unittest.main()
