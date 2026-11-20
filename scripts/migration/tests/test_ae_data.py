from pathlib import Path
import sys
import unittest
from copy import deepcopy
import nbtlib as n

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from migration.ae_data import convert_cable_bus_parts, convert_key, convert_storage_cell


class AeDataTest(unittest.TestCase):
    def test_cable_bus_part_names_keep_complete_cards_filters_connections_and_source(self):
        source = n.parse_nbt('{id:"ae2:cable_bus",down:{id:"expatternprovider:ex_export_bus_part",gn:{p:3,e:25.0d},upgrades:[{id:"ae2:speed_card",count:1,Slot:0}],config:[{"#t":"ae2:i",id:"minecraft:iron_ingot","#":0L}]},up:{id:"ae2:fluix_glass_cable"},owner:[I;1,2,3,4]}')
        original = deepcopy(source)
        data, changes = convert_cable_bus_parts(source, {"extendedae:ex_export_bus_part", "ae2:fluix_glass_cable"})
        self.assertEqual(data["down"]["id"], n.String("extendedae:ex_export_bus_part"))
        self.assertEqual(changes, {"down/id": n.String("expatternprovider:ex_export_bus_part")})
        data["down"]["id"] = source["down"]["id"]
        self.assertEqual(data, source)
        self.assertEqual(source, original)
        with self.assertRaisesRegex(ValueError, "目标注册名"):
            convert_cable_bus_parts(source, {"ae2:fluix_glass_cable"})

    def test_cable_bus_unknown_nonempty_part_cannot_be_silently_omitted(self):
        with self.assertRaisesRegex(ValueError, "注册名字段"):
            convert_cable_bus_parts(n.parse_nbt('{id:"ae2:cable_bus",north:{upgrades:[{id:"ae2:speed_card",count:1}]}}'), set())

    def test_retired_extended_terminal_only_maps_the_audited_empty_normal_mode(self):
        source = n.parse_nbt('{id:"ae2:cable_bus",west:{sort_direction:"ASCENDING",filter_type:"ALL",spin:0b,view_mode:"ALL",sort_by:"NAME",craftingMode:0,id:"expatternprovider:ex_crafting_terminal"}}')
        data, changes = convert_cable_bus_parts(source, {"ae2:crafting_terminal"}, native_key_types={"ae2:i", "ae2:f", "appmek:chemical"})
        self.assertEqual(data["west"]["id"], n.String("ae2:crafting_terminal"))
        self.assertEqual(changes["west/id"], source["west"]["id"])
        self.assertEqual(data["west"]["enabledKeyTypes"], n.List[n.String](["ae2:f", "ae2:i", "appmek:chemical"]))
        self.assertNotIn("filter_type", data["west"])
        self.assertNotIn("craftingMode", data["west"])
        self.assertEqual(changes["west/filter_type"], "ALL")
        self.assertEqual(changes["west/craftingMode"], n.Int(0))
        source["west"]["craftingInv"] = n.parse_nbt('[{id:"minecraft:diamond",count:1,Slot:0}]')
        with self.assertRaisesRegex(ValueError, "库存或扩展"):
            convert_cable_bus_parts(source, {"ae2:crafting_terminal"})

    def test_terminal_type_selection_requires_the_complete_target_registry(self):
        source = n.parse_nbt('{id:"ae2:cable_bus",north:{id:"ae2:pattern_encoding_terminal",filter_type:"ALL",spin:1b,sort_by:"AMOUNT",encodedInv:[{Slot:0,id:"ae2:encoded_pattern",count:1}]}}')
        original = deepcopy(source)
        result, changes = convert_cable_bus_parts(source, {"ae2:pattern_encoding_terminal"}, native_key_types={"ae2:i", "ae2:f", "appliedcreate:stress"})
        self.assertEqual(result["north"]["encodedInv"], source["north"]["encodedInv"])
        self.assertEqual(result["north"]["sort_by"], "AMOUNT")
        self.assertEqual(set(result["north"]["enabledKeyTypes"]), {"ae2:i", "ae2:f", "appliedcreate:stress"})
        self.assertEqual(source, original)
        with self.assertRaisesRegex(ValueError, "完整目标键类型"):
            convert_cable_bus_parts(source, {"ae2:pattern_encoding_terminal"})
        source["north"]["filter_type"] = n.String("UNREVIEWED")
        with self.assertRaisesRegex(ValueError, "已审计的 ALL"):
            convert_cable_bus_parts(source, {"ae2:pattern_encoding_terminal"}, native_key_types={"ae2:i", "ae2:f"})

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

    def test_old_distinct_keys_that_normalize_to_one_native_key_merge_long_amounts(self):
        source = self.source()
        source["tag"] = n.parse_nbt('{keys:[{"#c":"ae2:i",id:"minecraft:dirt",tag:{}},'
                                  '{"#c":"ae2:i",id:"minecraft:dirt"}],amts:[L;23L,24L],ic:47L}')
        def normalized(value):
            return n.Compound({"id": value["id"], "count": n.Int(1)})
        result = convert_storage_cell(source, self.target(source), normalized)
        entries = result["components"]["ae2:storage_cell_inv"]
        self.assertEqual(len(entries), 1)
        self.assertEqual(entries[0]["#"], n.Long(47))
        self.assertEqual(int(source["tag"]["ic"]), sum(int(row["#"]) for row in entries))

    def test_different_component_nbt_types_do_not_merge_storage_keys(self):
        source = self.source()
        source["tag"] = n.parse_nbt('{keys:[{"#c":"ae2:i",id:"minecraft:dirt",tag:{quota:1L}},'
                                  '{"#c":"ae2:i",id:"minecraft:dirt",tag:{quota:1}}],amts:[L;23L,24L],ic:47L}')
        result = convert_storage_cell(source, self.target(source), self.item)
        self.assertEqual(len(result["components"]["ae2:storage_cell_inv"]), 2)


if __name__ == "__main__":
    unittest.main()
