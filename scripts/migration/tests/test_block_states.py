import unittest

import nbtlib as n

from scripts.migration.block_states import convert_state


class BlockStateTests(unittest.TestCase):
    def test_keeps_machine_orientation_and_added_native_default(self):
        catalog = n.parse_nbt('{"create:depot":{properties:{facing:["east","west"],waterlogged:["true","false"]},default_state:{Name:"create:depot",Properties:{facing:"west",waterlogged:"false"}}}}')
        source = n.parse_nbt('{Name:"create:depot",Properties:{facing:"east"}}')
        result = convert_state(source, catalog)
        self.assertEqual(str(result.data["Properties"]["facing"]), "east")
        self.assertEqual(str(result.data["Properties"]["waterlogged"]), "false")
        self.assertFalse(result.removed)

    def test_records_known_aurora_variant_without_removing_block(self):
        catalog = n.parse_nbt('{"twilightforest:aurora_block":{properties:{},default_state:{Name:"twilightforest:aurora_block"}}}')
        result = convert_state(n.parse_nbt('{Name:"twilightforest:aurora_block",Properties:{variant:"14"}}'), catalog)
        self.assertEqual(str(result.data["Name"]), "twilightforest:aurora_block")
        self.assertEqual(str(result.removed["variant"]), "14")

    def test_rejects_loss_of_water_or_unknown_property(self):
        catalog = n.parse_nbt('{"twilightforest:firefly":{properties:{},default_state:{Name:"twilightforest:firefly"}}}')
        with self.assertRaisesRegex(ValueError, "移除"):
            convert_state(n.parse_nbt('{Name:"twilightforest:firefly",Properties:{waterlogged:"true"}}'), catalog)
        with self.assertRaisesRegex(ValueError, "移除"):
            convert_state(n.parse_nbt('{Name:"twilightforest:firefly",Properties:{facing:"north"}}'), catalog)

    def test_rejects_missing_block_and_invalid_direction_value(self):
        catalog = n.parse_nbt('{"create:depot":{properties:{facing:["east","west"]},default_state:{Name:"create:depot",Properties:{facing:"east"}}}}')
        with self.assertRaisesRegex(ValueError, "目录"):
            convert_state(n.parse_nbt('{Name:"missing:machine"}'), catalog)
        with self.assertRaisesRegex(ValueError, "不支持"):
            convert_state(n.parse_nbt('{Name:"create:depot",Properties:{facing:"up"}}'), catalog)


if __name__ == "__main__":
    unittest.main()
