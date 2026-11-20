from copy import deepcopy
import unittest

import nbtlib as n

from scripts.migration.item_pipeline import ItemPipeline, NativeItemRequired, unit_source


class Dfu:
    def update(self, source, kind):
        assert kind == "item_stack"
        data = n.Compound({"id": deepcopy(source["id"]), "count": n.Int(int(source["Count"]))})
        if source.get("tag"):
            data["components"] = n.Compound({"minecraft:custom_data": deepcopy(source["tag"])})
        return data


def pipeline():
    return ItemPipeline(Dfu(), {"minecraft:item": ["minecraft:stone", "create:filter", "sophisticatedbackpacks:diamond_backpack"],
                                "minecraft:entity_type": [], "minecraft:attribute": [], "minecraft:fluid": ["minecraft:lava"],
                                "minecraft:enchantment": [], "minecraft:potion": [], "minecraft:mob_effect": [],
                                "minecraft:block_entity_type": []})


class ItemPipelineTests(unittest.TestCase):
    def test_negative_byte_uses_real_count_only_in_protected_inventory(self):
        old = n.parse_nbt('{id:"minecraft:stone",Count:-64b,realCount:960,Slot:13}')
        converter = pipeline()
        with self.assertRaisesRegex(ValueError, "未经确认"):
            converter.item(old)
        result = converter.item(old, oversized_inventory=True)
        self.assertEqual(int(result["count"]), 960)
        self.assertEqual(int(result["Slot"]), 13)
        self.assertEqual(int(old["Count"]), -64)

    def test_nested_author_conversion_finishes_before_parent(self):
        old = n.parse_nbt('{id:"create:filter",Count:1b,tag:{Items:{Size:18,Items:[{id:"sophisticatedbackpacks:diamond_backpack",Count:1b,Slot:5,tag:{contentsUuid:[I;1,2,3,4]}}]}}}')
        converter = pipeline()
        with self.assertRaises(NativeItemRequired) as raised:
            converter.item(old)
        needed = raised.exception
        self.assertEqual(needed.kind, "backpack")
        self.assertNotIn("Slot", needed.source)
        converter.register_native(needed.source, n.parse_nbt('{id:"sophisticatedbackpacks:diamond_backpack",count:1,components:{"sophisticatedcore:storage_uuid":[I;1,2,3,4]}}'))
        result = converter.item(old)
        entry = result["components"]["create:filter_items"][0]
        self.assertEqual(int(entry["slot"]), 5)
        self.assertEqual(entry["item"]["components"]["sophisticatedcore:storage_uuid"].tolist(), [1, 2, 3, 4])
        self.assertNotIn("Slot", entry["item"])

    def test_native_cache_restores_each_actual_quantity_and_slot(self):
        source = n.parse_nbt('{id:"minecraft:stone",Count:12b,Slot:1}')
        converter = pipeline()
        converter.register_native(source, n.parse_nbt('{id:"minecraft:stone",count:1}'))
        other = n.parse_nbt('{id:"minecraft:stone",Count:5b,Slot:7}')
        self.assertEqual(int(converter.item(source)["count"]), 12)
        self.assertEqual(int(converter.item(other)["count"]), 5)
        self.assertEqual(int(converter.item(other)["Slot"]), 7)

    def test_rejects_conflicting_native_metadata_and_count(self):
        source = n.parse_nbt('{id:"minecraft:stone",Count:1b}')
        converter = pipeline()
        converter.register_native(source, n.parse_nbt('{id:"minecraft:stone",count:1}'))
        with self.assertRaisesRegex(ValueError, "冲突"):
            converter.register_native(source, n.parse_nbt('{id:"minecraft:stone",count:1,components:{"minecraft:custom_data":{owner:"other"}}}'))
        with self.assertRaisesRegex(ValueError, "数量"):
            converter.register_native(source, n.parse_nbt('{id:"minecraft:stone",count:2}'))

    def test_cache_keeps_metadata_nbt_types_distinct(self):
        converter = pipeline()
        converter.item(n.parse_nbt('{id:"minecraft:stone",Count:1b,tag:{value:3}}'))
        converter.item(n.parse_nbt('{id:"minecraft:stone",Count:1b,tag:{value:3L}}'))
        self.assertEqual(len(converter.ready), 2)

    def test_fluid_registration_amount_and_empty_marker(self):
        converter = pipeline()
        self.assertEqual(converter.fluid(n.parse_nbt('{FluidName:"minecraft:lava",Amount:9000}')), n.parse_nbt('{id:"minecraft:lava",amount:9000}'))
        self.assertEqual(converter.fluid(n.parse_nbt('{Empty:""}')), n.Compound())
        self.assertEqual(len(converter.changes), 1)
        with self.assertRaisesRegex(ValueError, "注册名"):
            converter.fluid(n.parse_nbt('{FluidName:"missing:fluid",Amount:9000}'))


if __name__ == "__main__":
    unittest.main()
