from copy import deepcopy
import unittest

import nbtlib as n

from scripts.migration.contraptions import assert_contraption_preserved, block_layout, convert_contraption


class ContraptionTests(unittest.TestCase):
    def source(self):
        return n.parse_nbt('{Type:"create:mounted",InitialOrientation:"south",Anchor:{X:1,Y:2,Z:3},Blocks:{Palette:[{Name:"create:deployer",Properties:{facing:"north"}}],BlockList:[{Pos:0L,State:0,Data:{id:"create:deployer",ForgeData:{owner:[I;1,2,3,4]},HeldItem:{id:"minecraft:powered_rail",Count:3b}},UpdateTag:{ForgeData:{owner:[I;1,2,3,4]}}}]},Actors:[{Pos:{X:0,Y:0,Z:0},Data:{HeldItem:{id:"minecraft:powered_rail",Count:1b}}}],items:[{pos:{X:0,Y:0,Z:0},storage:{type:"create:simple",value:{size:27,items:{4:{id:"minecraft:stone",Count:64}}}}}]}')

    def test_entire_actor_storage_and_block_entity_payload_pass_through_callbacks(self):
        source = self.source(); original = deepcopy(source)
        visited = []
        def block_entity(value):
            visited.append(deepcopy(value))
            value["NeoForgeData"] = value.pop("ForgeData")
            return value
        def tree(value):
            if isinstance(value, n.Compound):
                if "Count" in value:
                    value["count"] = n.Int(int(value.pop("Count")))
                return n.Compound({k: tree(v) for k, v in value.items()})
            if isinstance(value, n.List):
                return type(value)(tree(v) for v in value)
            return deepcopy(value)
        result = convert_contraption(source, deepcopy, block_entity, tree)
        self.assertEqual(len(visited), 2)
        self.assertEqual(result["Blocks"]["BlockList"][0]["Data"]["NeoForgeData"]["owner"].tolist(), [1, 2, 3, 4])
        self.assertEqual(result["Actors"][0]["Data"]["HeldItem"]["count"], 1)
        self.assertEqual(result["items"][0]["storage"]["value"]["items"]["4"]["count"], 64)
        self.assertEqual(result["InitialOrientation"], "south")
        self.assertEqual(result["Anchor"], source["Anchor"])
        self.assertEqual(source, original)

    def test_duplicate_position_and_missing_state_fail_instead_of_dropping_machine(self):
        source = self.source()
        source["Blocks"]["BlockList"].append(deepcopy(source["Blocks"]["BlockList"][0]))
        with self.assertRaisesRegex(ValueError, "位置重复"):
            convert_contraption(source, deepcopy, deepcopy, deepcopy)
        source = self.source(); source["Blocks"]["BlockList"][0]["State"] = n.Int(1)
        with self.assertRaisesRegex(ValueError, "索引错误"):
            convert_contraption(source, deepcopy, deepcopy, deepcopy)

    def test_layout_comparison_uses_positions_and_states_instead_of_palette_order(self):
        source = self.source()
        other = deepcopy(source)
        other["Blocks"]["Palette"].insert(0, n.parse_nbt('{Name:"minecraft:stone"}'))
        other["Blocks"]["BlockList"][0]["State"] = n.Int(1)
        self.assertEqual(block_layout(source), block_layout(other))

    def test_native_position_encoding_and_verified_duplicate_hand_inventory_are_preserved(self):
        old = self.source()
        inventory = n.List[n.Compound]([n.parse_nbt('{id:"minecraft:powered_rail",count:1,Slot:0b}')])
        old["Actors"][0]["Pos"] = n.Compound({"X": n.Int(-1), "Y": n.Int(2), "Z": n.Int(0)})
        old["Actors"][0]["Data"]["HeldItem"] = n.parse_nbt('{id:"minecraft:powered_rail",count:1}')
        old["Blocks"]["BlockList"][0]["Pos"] = n.Long(-274877906942)
        old["Blocks"]["BlockList"][0]["Data"]["Inventory"] = inventory
        new = deepcopy(old)
        new["Blocks"]["BlockList"][0]["Data"].pop("Inventory")
        new["Actors"][0]["Pos"] = n.IntArray([-1, 2, 0])
        new["Anchor"] = n.IntArray([1, 2, 3])
        original = deepcopy(old)
        ledger = assert_contraption_preserved(old, new)
        self.assertEqual(ledger[0]["inventory"], inventory)
        self.assertEqual(ledger[0]["reason"], "actor_hand_duplicate")
        self.assertEqual(old, original)

    def test_non_hand_inventory_or_changed_actor_hand_cannot_be_omitted(self):
        old = self.source()
        old["Blocks"]["BlockList"][0]["Data"]["Inventory"] = n.parse_nbt('[{id:"minecraft:powered_rail",Count:1b,Slot:1b}]')
        new = deepcopy(old); new["Blocks"]["BlockList"][0]["Data"].pop("Inventory")
        with self.assertRaisesRegex(ValueError, "不能省略"):
            assert_contraption_preserved(old, new)
        old["Blocks"]["BlockList"][0]["Data"]["Inventory"][0]["Slot"] = n.Byte(0)
        new["Actors"][0]["Data"]["HeldItem"]["Count"] = n.Byte(2)
        with self.assertRaisesRegex(ValueError, "同位置"):
            assert_contraption_preserved(old, new)

    def test_changed_anchor_owner_or_mounted_inventory_prevents_acceptance(self):
        old = self.source()
        for change in ("anchor", "owner", "inventory"):
            new = deepcopy(old)
            if change == "anchor":
                new["Anchor"] = n.IntArray([0, 0, 0])
            elif change == "owner":
                new["Blocks"]["BlockList"][0]["Data"]["ForgeData"]["owner"] = n.IntArray([4, 3, 2, 1])
            else:
                new["items"][0]["storage"]["value"]["items"]["4"]["Count"] = n.Int(63)
            with self.assertRaises(ValueError):
                assert_contraption_preserved(old, new)


if __name__ == "__main__":
    unittest.main()
