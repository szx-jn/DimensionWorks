from copy import deepcopy
from pathlib import Path
import sys
import unittest

import nbtlib as n

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from migration.create_data import convert_container_item


def item(value):
    return n.Compound({"id": deepcopy(value["id"]), "count": n.Int(int(value["Count"])),
                       "components": n.Compound({"minecraft:custom_data": deepcopy(value.get("tag", n.Compound()))})})


def target(source):
    return n.Compound({"id": source["id"], "count": n.Int(1), "components": n.Compound({
        "minecraft:custom_data": deepcopy(source["tag"]), "minecraft:custom_name": n.String('"Keep"')})})


class CreateDataTest(unittest.TestCase):
    def test_filter_keeps_foreign_item_metadata_last_slot_and_modes(self):
        source = n.parse_nbt('{id:"create:filter",Count:1b,tag:{Items:{Size:18,Items:[{Slot:17,'
                             'id:"farmersdelight:beef_stew",Count:5b,tag:{customStock:42L}}]},Blacklist:1b,RespectNBT:1b}}')
        original = deepcopy(source)
        result = convert_container_item(source, target(source), item).data
        components = result["components"]
        entry = components["create:filter_items"][0]
        self.assertEqual(entry["slot"], n.Int(17))
        self.assertEqual(entry["item"]["count"], 5)
        self.assertEqual(entry["item"]["components"]["minecraft:custom_data"]["customStock"], n.Long(42))
        self.assertEqual(components["create:filter_items_blacklist"], n.Byte(1))
        self.assertEqual(components["create:filter_items_respect_nbt"], n.Byte(1))
        self.assertEqual(components["minecraft:custom_name"], '"Keep"')
        self.assertNotIn("minecraft:custom_data", components)
        self.assertEqual(source, original)

    def test_package_keeps_address_nested_entity_and_inventory_count(self):
        source = n.parse_nbt('{id:"create:cardboard_package_12x10",Count:1b,tag:{Address:"Factory *",'
                            'Items:{Size:9,Items:[{Slot:8,id:"create_mobile_packages:entity_package",Count:1b,'
                            'tag:{EntityTag:{id:"minecraft:sheep",UUID:[I;1,2,3,4],Items:[{Slot:0b,id:"minecraft:wheat",Count:64b}]}}}]}}}')
        result = convert_container_item(source, target(source), item).data["components"]
        self.assertEqual(result["create:package_address"], "Factory *")
        entry = result["create:package_contents"][0]
        self.assertEqual(entry["slot"], 8)
        entity = entry["item"]["components"]["minecraft:custom_data"]["EntityTag"]
        self.assertEqual(entity["UUID"].tolist(), [1, 2, 3, 4])
        self.assertEqual(entity["Items"][0]["Count"], n.Byte(64))

    def test_capacity_duplicate_slot_unknown_field_and_invalid_mode_block(self):
        for text in ['{id:"create:filter",Count:1b,tag:{Items:{Size:9,Items:[]}}}',
                     '{id:"create:filter",Count:1b,tag:{Items:{Size:18,Items:[{Slot:0,id:"minecraft:iron_ingot",Count:1b},'
                     '{Slot:0,id:"minecraft:gold_ingot",Count:1b}]}}}',
                     '{id:"create:filter",Count:1b,tag:{Blacklist:2b}}',
                     '{id:"create:cardboard_package_12x10",Count:1b,tag:{Items:{Size:9,Items:[]},Unknown:1}}']:
            source = n.parse_nbt(text)
            with self.subTest(text=text), self.assertRaises(ValueError):
                convert_container_item(source, target(source), item)

    def test_component_collision_changed_dfu_data_and_empty_conversion_block(self):
        source = n.parse_nbt('{id:"create:filter",Count:1b,tag:{Items:{Size:18,Items:[{Slot:0,id:"minecraft:iron_ingot",Count:1b}]}}}')
        data = target(source); data["components"]["create:filter_items"] = n.List[n.Compound]()
        with self.assertRaisesRegex(ValueError, "冲突"):
            convert_container_item(source, data, item)
        data = target(source); data["components"]["minecraft:custom_data"]["Items"]["Size"] = n.Int(1)
        with self.assertRaisesRegex(ValueError, "与输入不符"):
            convert_container_item(source, data, item)
        with self.assertRaisesRegex(ValueError, "非空物品"):
            convert_container_item(source, target(source), lambda _: n.Compound())


if __name__ == "__main__":
    unittest.main()
