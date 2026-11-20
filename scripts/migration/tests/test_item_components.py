from pathlib import Path
import sys
import unittest
from copy import deepcopy
import nbtlib as n

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from migration.item_components import authoritative_count, convert_item_components


class ItemComponentTest(unittest.TestCase):
    def quiver(self):
        return n.Compound({"id": n.String("supplementaries:quiver"), "Count": n.Byte(1),
                           "ForgeCaps": n.Compound({"Parent": n.Compound({
                               "Size": n.Int(6), "SelectedSlot": n.Int(4), "Items": n.List[n.Compound]([
                                   n.Compound({"Slot": n.Int(4), "id": n.String("minecraft:arrow"), "Count": n.Byte(48)})
                               ])})})})

    def target(self, source):
        return n.Compound({"id": source["id"], "count": n.Int(1), "ForgeCaps": deepcopy(source["ForgeCaps"]),
                           "components": n.Compound({"minecraft:custom_name": n.String('"Arrow bag"')})})

    def nested(self, source):
        return n.Compound({"id": source["id"], "count": n.Int(int(source["Count"])), "Slot": source["Slot"]})

    def test_quiver_keeps_arrow_count_selected_slot_holes_and_custom_name(self):
        source = self.quiver(); original = deepcopy(source)
        result = convert_item_components(source, self.target(source), self.nested).data
        contents = result["components"]["supplementaries:quiver_content"]
        self.assertEqual(len(contents["items"]), 6)
        self.assertEqual(contents["selected_slot"], 4)
        self.assertEqual(contents["items"][4], {"id": n.String("minecraft:arrow"), "count": n.Int(48)})
        self.assertEqual(contents["items"][5], {})
        self.assertEqual(result["components"]["minecraft:custom_name"], '"Arrow bag"')
        self.assertNotIn("ForgeCaps", result)
        self.assertEqual(source, original)

    def test_duplicate_arrow_slots_and_invalid_selection_block_conversion(self):
        source = self.quiver(); parent = source["ForgeCaps"]["Parent"]
        parent["Items"].append(deepcopy(parent["Items"][0]))
        with self.assertRaisesRegex(ValueError, "重复或越界"):
            convert_item_components(source, self.target(source), self.nested)
        parent["Items"].pop(); parent["SelectedSlot"] = n.Int(6)
        with self.assertRaisesRegex(ValueError, "选中槽位越界"):
            convert_item_components(source, self.target(source), self.nested)

    def test_unknown_item_caps_and_target_component_collision_block_conversion(self):
        source = self.quiver(); target = self.target(source)
        target["components"]["supplementaries:quiver_content"] = n.Compound()
        with self.assertRaisesRegex(ValueError, "组件冲突"):
            convert_item_components(source, target, self.nested)
        source["ForgeCaps"]["unknown"] = n.Compound()
        with self.assertRaisesRegex(ValueError, "未覆盖的物品"):
            convert_item_components(source, self.target(source), self.nested)

    def test_oversized_inventory_uses_real_count_not_overflowing_byte(self):
        self.assertEqual(authoritative_count(n.Compound({"Count": n.Byte(-64), "realCount": n.Int(448)}),
                                             oversized_inventory=True), 448)
        self.assertEqual(authoritative_count(n.Compound({"Count": n.Byte(-5), "realCount": n.Int(3067)}),
                                             oversized_inventory=True), 3067)
        with self.assertRaisesRegex(ValueError, "未经确认"):
            authoritative_count(n.Compound({"Count": n.Byte(-64), "realCount": n.Int(448)}))
        with self.assertRaisesRegex(ValueError, "数量无效"):
            authoritative_count(n.Compound({"Count": n.Byte(-64)}))

    def test_actual_empty_drink_cap_is_logged_without_removing_wine_or_quality(self):
        source = n.parse_nbt('{id:"kaleidoscope_tavern:wine",Count:1b,tag:{BrewLevel:6},'
                            'ForgeCaps:{"creategearsandtavern:drink_fluid":{}}}')
        target = self.target(source)
        target["components"]["minecraft:custom_data"] = deepcopy(source["tag"])
        original = deepcopy(source)
        converted = convert_item_components(source, target, self.nested)
        self.assertEqual(converted.data["id"], source["id"])
        self.assertEqual(converted.data["count"], 1)
        self.assertEqual(converted.data["components"]["minecraft:custom_data"]["BrewLevel"], 6)
        self.assertNotIn("ForgeCaps", converted.data)
        self.assertEqual(converted.removed, {"creategearsandtavern:drink_fluid": n.Compound()})
        self.assertEqual(source, original)

    def test_nonempty_drink_cap_or_same_empty_cap_on_unknown_item_blocks(self):
        source = n.parse_nbt('{id:"kaleidoscope_tavern:wine",Count:1b,'
                            'ForgeCaps:{"creategearsandtavern:drink_fluid":{stored:1000}}}')
        with self.assertRaisesRegex(ValueError, "未覆盖的物品"):
            convert_item_components(source, self.target(source), self.nested)
        source = n.parse_nbt('{id:"minecraft:apple",Count:1b,ForgeCaps:{"creategearsandtavern:drink_fluid":{}}}')
        with self.assertRaisesRegex(ValueError, "未覆盖的物品"):
            convert_item_components(source, self.target(source), self.nested)


if __name__ == "__main__":
    unittest.main()
