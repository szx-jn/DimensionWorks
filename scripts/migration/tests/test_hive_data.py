from copy import deepcopy
import unittest

import nbtlib as n

from scripts.migration.hive_data import normalize_hive_fields


class HiveDataTests(unittest.TestCase):
    def test_duplicate_paths_preserve_uuid_health_timers_and_real_flower_position(self):
        source = n.parse_nbt('{id:"minecraft:beehive",FlowerPos:{X:-10,Y:63,Z:7},flower_pos:[I;-10,63,7],Bees:[{TicksInHive:27,MinOccupationTicks:600,EntityData:{id:"minecraft:bee",UUID:[I;1,2,3,4],Health:9.0f,FlowerPos:{X:-11,Y:64,Z:6}}}],bees:[{ticks_in_hive:27,min_ticks_in_hive:600,entity_data:{id:"minecraft:bee",UUID:[I;1,2,3,4],Health:1.0f}}]}')
        original = deepcopy(source)
        result = normalize_hive_fields(source)
        self.assertNotIn("Bees", result.data)
        self.assertNotIn("FlowerPos", result.data)
        bee = result.data["bees"][0]
        self.assertEqual(bee["entity_data"]["Health"], 9.0)
        self.assertEqual(bee["entity_data"]["UUID"], source["Bees"][0]["EntityData"]["UUID"])
        self.assertEqual(bee["ticks_in_hive"], 27)
        self.assertEqual(result.data["flower_pos"].tolist(), [-10, 63, 7])
        self.assertEqual(result.removed["bee_flower_caches"][0]["FlowerPos"], source["Bees"][0]["EntityData"]["FlowerPos"])
        self.assertEqual(source, original)

    def test_missing_bees_changed_timer_or_field_shape_stop_conversion(self):
        for text in ('{id:"minecraft:beehive",Bees:[{EntityData:{id:"minecraft:bee"}}]}',
                     '{id:"minecraft:beehive",Bees:[{TicksInHive:27,EntityData:{id:"minecraft:bee"}}],bees:[{ticks_in_hive:28,entity_data:{id:"minecraft:bee"}}]}',
                     '{id:"minecraft:beehive",FlowerPos:{X:1,Y:2,Z:3},flower_pos:[I;1,2,4]}'):
            with self.assertRaises(ValueError):
                normalize_hive_fields(n.parse_nbt(text))


if __name__ == "__main__":
    unittest.main()
