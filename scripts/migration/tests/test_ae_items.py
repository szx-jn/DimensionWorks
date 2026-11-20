from copy import deepcopy
import unittest

import nbtlib as n

from scripts.migration.ae_items import convert_ae_item


def target(source):
    return n.Compound({"id": source["id"], "count": n.Int(1),
                       "components": n.Compound({"minecraft:custom_data": deepcopy(source["tag"])})})


def nested(source):
    value = n.Compound({"id": source["id"], "count": n.Int(int(source["Count"]))})
    if "Slot" in source:
        value["Slot"] = deepcopy(source["Slot"])
    if str(source["id"]) == "ae2:quantum_entangled_singularity":
        value["components"] = n.Compound({"ae2:entangled_singularity_id": deepcopy(source["tag"]["freq"])})
    return value


class AEItemTests(unittest.TestCase):
    def test_quantum_pairing_frequency_is_long_and_not_custom_data(self):
        old = n.parse_nbt('{id:"ae2:quantum_entangled_singularity",Count:1b,tag:{freq:179369449906700L}}')
        result = convert_ae_item(old, target(old), nested)
        self.assertEqual(result["components"], {"ae2:entangled_singularity_id": n.Long(179369449906700)})

    def test_wireless_terminal_preserves_crafting_slots_upgrades_energy_and_link(self):
        old = n.parse_nbt('{id:"ae2:wireless_crafting_terminal",Count:1b,tag:{sort_by:"AMOUNT",internalMaxPower:14400000.0d,internalCurrentPower:14399957.305395938d,accessPoint:{pos:[I;1,2,3],dimension:"minecraft:overworld"},upgrades:[{id:"ae2wtlib:quantum_bridge_card",Count:1b,Slot:0},{id:"megacells:greater_energy_card",Count:1b,Slot:1}],craftingGrid:[{id:"minecraft:paper",Count:5b,Slot:4},{id:"minecraft:slime_ball",Count:1b,Slot:7}],singularity:[{id:"ae2:quantum_entangled_singularity",Count:1b,Slot:0,tag:{freq:179369449906700L}}]}}')
        original = deepcopy(old)
        result = convert_ae_item(old, target(old), nested)
        components = result["components"]
        self.assertEqual(components["ae2:stored_energy"], old["tag"]["internalCurrentPower"])
        self.assertEqual(components["ae2:wireless_link_target"], old["tag"]["accessPoint"])
        self.assertEqual([int(v["slot"]) for v in components["ae2:crafting_inv"]], [4, 7])
        self.assertEqual([int(v["item"]["count"]) for v in components["ae2:crafting_inv"]], [5, 1])
        self.assertEqual(len(components["ae2:upgrades"]), 2)
        self.assertNotIn("Slot", components["ae2wtlib:singularity"])
        self.assertEqual(components["ae2:exported_settings"], {"sort_by": n.String("AMOUNT")})
        self.assertNotIn("minecraft:custom_data", components)
        self.assertEqual(old, original)

    def test_universal_terminal_preserves_installed_modules_controls_and_encoding(self):
        old = n.parse_nbt('{id:"ae2wtlib:wireless_universal_terminal",Count:1b,tag:{crafting:1b,pattern_encoding:1b,ex_pattern_access:1b,currentTerminal:"crafting",magnet_settings:3b,restock:1b,pick_block:1b,craft_if_missing:0b,mode:"PROCESSING",substitute:0b,substituteFluids:0b}}')
        result = convert_ae_item(old, target(old), nested)["components"]
        self.assertEqual(result["ae2wtlib:has_crafting_terminal"], n.Compound())
        self.assertEqual(result["ae2wtlib:has_ex_pattern_access_terminal"], n.Compound())
        self.assertEqual(result["ae2wtlib:current_terminal"], "crafting")
        self.assertEqual(result["ae2wtlib:magnet_settings"], n.Byte(3))
        self.assertEqual(result["ae2wtlib:pattern_encoding_logic"], n.parse_nbt('{mode:"PROCESSING",substitute:0b,substituteFluids:0b}'))
        self.assertNotIn("minecraft:custom_data", result)

    def test_omni_uuid_usage_and_tooltip_keys_are_preserved_as_typed_components(self):
        old = n.parse_nbt('{id:"ae2omnicells:complex_omni_cell_1m",Count:1b,tag:{ae_universal_cell_uuid:"01234567-89ab-cdef-0123-456789abcdef",ae_universal_cell_bytes_usage:586L,ae_universal_cell_types_usage:107,ae_universal_cell_state:"NOT_EMPTY",ae_universal_cell_show_tooltip_stacks:[{"#c":"ae2:i",id:"minecraft:honeycomb","#":15L}]}}')
        result = convert_ae_item(old, target(old), nested)["components"]
        self.assertEqual(result["ae2omnicells:cell_uuid"].tolist(), [19088743, -1985229329, 19088743, -1985229329])
        self.assertEqual(result["ae2omnicells:cell_bytes_usage"], n.Long(586))
        self.assertEqual(result["ae2omnicells:cell_types_usage"], n.Int(107))
        self.assertEqual(result["ae2omnicells:cell_show_tooltip_stacks"], n.parse_nbt('[{"#t":"ae2:i",id:"minecraft:honeycomb","#":15L}]'))

    def test_duplicate_terminal_inventory_slots_stop_conversion(self):
        old = n.parse_nbt('{id:"ae2:wireless_crafting_terminal",Count:1b,tag:{craftingGrid:[{id:"minecraft:paper",Count:1b,Slot:4},{id:"minecraft:stone",Count:1b,Slot:4}]}}')
        with self.assertRaisesRegex(ValueError, "重复"):
            convert_ae_item(old, target(old), nested)


if __name__ == "__main__":
    unittest.main()
