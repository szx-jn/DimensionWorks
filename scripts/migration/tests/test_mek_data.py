from __future__ import annotations

from copy import deepcopy
from pathlib import Path
import sys
import unittest

import nbtlib as n

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from migration.mek_data import MARKER, MARKER_COMPONENT, normalize_block_entity, normalize_saved_data, prepare_item


def item(source):
    result = deepcopy(source)
    result["count"] = n.Int(int(result.pop("Count")))
    return result


def target_item(source):
    return n.Compound({"id": deepcopy(source["id"]), "count": n.Int(int(source["Count"])),
                       "components": n.Compound({"minecraft:custom_data": deepcopy(source["tag"])})})


class MekDataTest(unittest.TestCase):
    def test_machine_stock_side_config_upgrades_and_owner_are_preserved_for_native_reader(self):
        source = n.parse_nbt('{id:"mekanism:enrichment_chamber",x:5,y:64,z:-5,ForgeCaps:{},keepPacked:0b,'
                            'Items:[{Slot:0b,Item:{id:"minecraft:redstone_ore",Count:31b}}],'
                            'componentConfig:{config6:{side0:0,side1:8,side2:4,side3:1,side4:1,side5:4},eject6:1b},'
                            'componentUpgrade:{Items:[],upgrades:[{type:0,amount:8}]},'
                            'componentSecurity:{owner:[I;1,2,3,4],securityMode:0}}')
        original = deepcopy(source)
        result = normalize_block_entity(source, item, deepcopy, deepcopy)
        self.assertEqual(result.data["items"][0]["item"]["count"], n.Int(31))
        self.assertEqual(result.data["items"][0]["slot"], n.Byte(0))
        self.assertEqual(list(result.data["component_config"]["config6"]), [0, 8, 4, 1, 1, 4])
        self.assertEqual(result.data["component_upgrade"]["upgrades"][0]["amount"], n.Int(8))
        self.assertEqual(result.data["component_security"]["owner"], source["componentSecurity"]["owner"])
        self.assertEqual(set(result.removed), {"/ForgeCaps", "/keepPacked"})
        self.assertEqual(source, original)

    def test_chemical_long_fluid_amount_heat_and_connections_keep_their_units_and_slots(self):
        source = n.parse_nbt('{id:"mekanism:ultimate_pressurized_tube",x:0,y:64,z:0,'
                            'boxedChemical:{chemicalType:"gas",gasName:"mekanism:oxygen",amount:1024000L},'
                            'GasTanks:[{Tank:0b,stored:{gasName:"mekanism:hydrogen",amount:64000L}}],'
                            'FluidTanks:[{Tank:1b,stored:{FluidName:"minecraft:water",Amount:1278300}}],'
                            'HeatCapacitors:[{Container:0b,heatCapacity:500.0d,stored:150002.40661173983d}],'
                            'connection0:0,connection1:1,connection2:2,connection3:3,connection4:0,connection5:1}')
        result = normalize_block_entity(source, item, deepcopy, deepcopy)
        self.assertEqual(result.data["boxed_chemical"]["amount"], n.Long(1024000))
        self.assertEqual(result.data["gas_tanks"][0]["stored"], n.parse_nbt('{id:"mekanism:hydrogen",amount:64000L}'))
        self.assertEqual(result.data["fluid_tanks"][0]["tank"], n.Byte(1))
        self.assertEqual(result.data["fluid_tanks"][0]["stored"]["amount"], n.Int(1278300))
        self.assertEqual(result.data["heat_capacitors"][0]["stored"], source["HeatCapacitors"][0]["stored"])
        self.assertEqual(list(result.data["connection"]), [0, 1, 2, 3, 0, 1])

    def test_actual_infuse_type_name_is_converted_and_unknown_long_chemical_shape_blocks(self):
        source = n.parse_nbt('{id:"mekanism:metallurgic_infuser",x:1,y:64,z:1,'
                            'InfusionTanks:[{Tank:0b,stored:{infuseTypeName:"mekanism:carbon",amount:40L}}]}')
        result = normalize_block_entity(source, item, deepcopy, deepcopy)
        self.assertEqual(result.data["infusion_tanks"][0]["stored"], n.parse_nbt('{id:"mekanism:carbon",amount:40L}'))
        source["InfusionTanks"][0]["stored"] = n.parse_nbt('{unknownChemicalName:"mekanism:carbon",amount:40L}')
        with self.assertRaisesRegex(ValueError, "注册名字段"):
            normalize_block_entity(source, item, deepcopy, deepcopy)

    def test_packed_block_inventory_and_origin_are_not_removed_with_the_old_box_tag(self):
        source = n.parse_nbt('{id:"mekanism:cardboard_box",x:1,y:64,z:1,'
                            'data:{blockState:{Name:"minecraft:chest"},tileTag:{id:"minecraft:chest",'
                            'Items:[{Slot:2b,id:"farmersdelight:beef_stew",Count:16b}]},'
                            '"create_mobile_packages:from_mobile_packager":1b},'
                            '"create_mobile_packages:from_mobile_packager":1b}')
        calls = []
        def convert_be(value):
            calls.append(deepcopy(value))
            result = deepcopy(value)
            result["Items"] = n.List[n.Compound](item(row) for row in value["Items"])
            return result
        result = normalize_block_entity(source, item, deepcopy, convert_be)
        box = result.data["components"]["mekanism:block_data"]
        self.assertEqual(box["state"]["Name"], n.String("minecraft:chest"))
        self.assertEqual(box["block_entity_tag"]["Items"][0]["count"], n.Int(16))
        self.assertEqual(box["block_entity_tag"]["Items"][0]["Slot"], n.Byte(2))
        self.assertEqual(result.data["components"][MARKER_COMPONENT], n.Byte(1))
        self.assertEqual(len(calls), 1)

    def test_configurator_heat_mode_is_mapped_by_meaning_instead_of_new_ordinal(self):
        source = n.parse_nbt('{id:"mekanism:configurator",Count:1b,tag:{mekData:{state:7},customFlag:9}}')
        result = prepare_item(source, target_item(source), item, deepcopy, deepcopy)
        self.assertEqual(result.data["components"]["mekanism:configurator_mode"], n.String("configurate_heat"))
        self.assertEqual(result.data["components"]["minecraft:custom_data"]["customFlag"], n.Int(9))
        self.assertIsNone(result.tile_data)

    def test_machine_item_keeps_custom_payload_until_native_components_are_collected(self):
        source = n.parse_nbt('{id:"mekanism:enrichment_chamber",Count:1b,'
                            'tag:{mekData:{owner:[I;1,2,3,4],securityMode:0,Items:[{Slot:0b,Item:{id:"minecraft:iron_ingot",Count:60b}}]}}}')
        result = prepare_item(source, target_item(source), item, deepcopy, deepcopy)
        self.assertEqual(result.tile_data["component_security"]["owner"], source["tag"]["mekData"]["owner"])
        self.assertEqual(result.tile_data["items"][0]["item"]["count"], n.Int(60))
        self.assertEqual(result.data["components"]["minecraft:custom_data"]["mekData"], source["tag"]["mekData"])

    def test_saved_frequency_and_evaporation_cache_keep_owner_and_stored_container_counts(self):
        source = n.parse_nbt('{data:{freqList:[{name:"Security",owner:[I;1,2,3,4],publicFreq:1b,securityMode:0,override:0b}],'
                            'cache:[{inventoryID:[I;5,6,7,8],Items_stored:4,FluidTanks_stored:2,HeatCapacitors_stored:1,Items:[]}]}}')
        result = normalize_saved_data(source, item)
        freq = result.data["data"]["freq_list"][0]
        self.assertEqual(freq["owner"], source["data"]["freqList"][0]["owner"])
        self.assertEqual(freq["security_mode"], n.String("public"))
        cache = result.data["data"]["cache"][0]
        self.assertEqual(cache["inventory_id"], source["data"]["cache"][0]["inventoryID"])
        self.assertEqual(cache["fluid_tanks_stored"], n.Int(2))

    def test_unknown_caps_chemical_metadata_direction_and_component_collisions_block(self):
        samples = ['{id:"mekanism:crusher",ForgeCaps:{unknown:{value:1}}}',
                   '{id:"mekanism:basic_chemical_tank",GasTanks:[{Tank:0b,stored:{gasName:"mekanism:hydrogen",amount:4L,unknown:1}}]}',
                   '{id:"mekanism:ultimate_pressurized_tube",connection0:1}',
                   '{id:"mekanism:crusher",keepPacked:1b}',
                   '{id:"mekanism:crusher",Items:[],items:[]}']
        for sample in samples:
            with self.subTest(sample=sample), self.assertRaises(ValueError):
                normalize_block_entity(n.parse_nbt(sample), item, deepcopy, deepcopy)
        source = n.parse_nbt('{id:"mekanism:configurator",Count:1b,tag:{mekData:{state:7}}}')
        target = target_item(source)
        target["components"]["mekanism:configurator_mode"] = n.String("wrench")
        with self.assertRaises(ValueError):
            prepare_item(source, target, item, deepcopy, deepcopy)


if __name__ == "__main__":
    unittest.main()
