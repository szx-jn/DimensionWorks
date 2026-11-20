from copy import deepcopy
import unittest

import nbtlib as n

from scripts.migration.fluid_data import convert_block_fluids


class FluidDataTests(unittest.TestCase):
    ids = {"minecraft:empty", "minecraft:water", "minecraft:lava"}

    def test_real_tank_and_pipe_flow_keep_id_amount_and_transport_state(self):
        source = n.parse_nbt('{TankContent:{FluidName:"minecraft:lava",Amount:7300},south:{Flow:{FluidName:"minecraft:water",Amount:1000,In:1b,Progress:{Value:0.75f,Target:1.0f}},OpenEnd:{FluidName:"minecraft:water",Amount:411,Location:{Pos:{X:-17,Y:63,Z:7},Face:2},Pulling:1b}}}')
        original = deepcopy(source)
        result = convert_block_fluids(source, self.ids)
        self.assertEqual(result.data["TankContent"], n.parse_nbt('{Fluid:{id:"minecraft:lava",amount:7300}}'))
        self.assertEqual(result.data["south"]["Flow"]["Fluid"], n.parse_nbt('{id:"minecraft:water",amount:1000}'))
        self.assertEqual(result.data["south"]["Flow"]["Progress"], source["south"]["Flow"]["Progress"])
        self.assertEqual(result.data["south"]["OpenEnd"]["Location"], source["south"]["OpenEnd"]["Location"])
        self.assertEqual(result.data["south"]["OpenEnd"]["Fluid"]["amount"], n.Int(411))
        self.assertEqual(len(result.removed["legacy_fluids"]), 3)
        self.assertEqual(source, original)

    def test_empty_tank_and_open_endpoint_preserve_other_fields_and_ledger(self):
        source = n.parse_nbt('{TankContent:{FluidName:"minecraft:empty",Amount:0},OpenEnd:{FluidName:"minecraft:empty",Amount:0,Location:{Pos:{X:1,Y:2,Z:3},Face:4},Pulling:0b}}')
        result = convert_block_fluids(source, self.ids)
        self.assertEqual(result.data["TankContent"], n.Compound())
        self.assertEqual(result.data["OpenEnd"], n.Compound({k:deepcopy(v) for k,v in source["OpenEnd"].items() if k in {"Location", "Pulling"}}))
        self.assertEqual(len(result.removed["legacy_fluids"]), 2)

    def test_burner_tavern_tank_wrappers_and_direct_exposure_stack_are_distinct(self):
        source = n.parse_nbt('{Stomach:{FluidName:"minecraft:lava",Amount:1000},fluid:{FluidName:"minecraft:water",Amount:600},MoltenExposure:[{Fluid:{FluidName:"minecraft:lava",Amount:123}}]}')
        result = convert_block_fluids(source, self.ids)
        self.assertEqual(result.data["Stomach"]["Fluid"], n.parse_nbt('{id:"minecraft:lava",amount:1000}'))
        self.assertEqual(result.data["fluid"]["Fluid"]["amount"], n.Int(600))
        self.assertEqual(result.data["MoltenExposure"][0]["Fluid"], n.parse_nbt('{id:"minecraft:lava",amount:123}'))

    def test_original_and_modern_item_metadata_are_left_for_item_adapter(self):
        source = n.parse_nbt('{Items:[{id:"minecraft:stone",Count:1b,tag:{fluidFilters:[{FluidName:"minecraft:water",Amount:0}]}}],current:{id:"minecraft:stone",count:1,components:{"minecraft:custom_data":{old:{FluidName:"minecraft:water",Amount:0}}}}}')
        result = convert_block_fluids(source, self.ids)
        self.assertEqual(result.data, source)
        self.assertEqual(result.removed, {})

    def test_unknown_fields_ids_bad_types_and_named_zero_stop_conversion(self):
        for source in ('{TankContent:{FluidName:"missing:fluid",Amount:8}}',
                       '{TankContent:{FluidName:"minecraft:water",Amount:8,unknown:1}}',
                       '{TankContent:{FluidName:"minecraft:water",Amount:8L}}',
                       '{TankContent:{FluidName:"minecraft:water",Amount:-1}}'):
            with self.assertRaisesRegex(ValueError, "未覆盖"):
                convert_block_fluids(n.parse_nbt(source), self.ids)
        with self.assertRaisesRegex(ValueError, "过滤模板"):
            convert_block_fluids(n.parse_nbt('{TankContent:{FluidName:"minecraft:water",Amount:0}}'), self.ids)


if __name__ == "__main__":
    unittest.main()
