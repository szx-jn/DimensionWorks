from __future__ import annotations

from copy import deepcopy
from pathlib import Path
import sys
import unittest

import nbtlib as n

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from migration.entity_data import AETHER_PLAYER_FIELDS, convert_entity_data


class EntityDataTest(unittest.TestCase):
    def convert(self, caps):
        return convert_entity_data(n.Compound({"ForgeCaps": n.Compound(caps)}))

    def test_maid_counts_and_power_use_native_compound_payloads(self):
        result = self.convert({"touhou_little_maid:maid_num": n.Int(2),
                               "touhou_little_maid:chat_tokens": n.Int(137),
                               "touhou_little_maid:power": n.Float(5.5)}).data["neoforge:attachments"]
        self.assertEqual(result["touhou_little_maid:maid_num"], {"num": n.Int(2)})
        self.assertEqual(result["touhou_little_maid:chat_tokens"]["num"], 137)
        self.assertIsInstance(result["touhou_little_maid:power"]["power"], n.Float)

    def test_aether_life_shards_health_and_flags_use_native_names(self):
        value = n.Compound({key: n.Int(i) for i, key in enumerate(AETHER_PLAYER_FIELDS)})
        value["SavedHealth"] = n.Float(40)
        result = self.convert({"aether:aether_player": value}).data["neoforge:attachments"]["aether:aether_player"]
        self.assertEqual(result["saved_health"], n.Float(40))
        self.assertEqual(set(result), set(AETHER_PLAYER_FIELDS.values()))
        self.assertEqual(result["life_shard_count"], value["LifeShardCount"])

    def test_curios_nested_inventory_is_preserved_for_later_item_conversion(self):
        inventory = n.Compound({"Curios": n.List[n.Compound]([
            n.Compound({"Identifier": n.String("gear_heart"), "Items": n.List[n.Compound]([
                n.Compound({"id": n.String("dimensionworks:gear_heart"), "Count": n.Byte(1),
                            "tag": n.Compound({"quota": n.Long(9000000000000000)})})
            ])})
        ])})
        source = n.Compound({"ForgeCaps": n.Compound({"curios:inventory": inventory}),
                             "ForgeData": n.Compound({"dimensionworks_rpm_limit": n.Int(2048)}),
                             "Inventory": n.List[n.Compound](), "UUID": n.IntArray([1, 2, 3, 4])})
        original = deepcopy(source)
        result = convert_entity_data(source).data
        self.assertEqual(result["neoforge:attachments"]["curios:inventory"], inventory)
        self.assertEqual(result["NeoForgeData"], source["ForgeData"])
        self.assertEqual(result["UUID"], source["UUID"])
        self.assertEqual(source, original)

    def test_radiation_and_shield_counts_use_native_payload_shapes(self):
        result = self.convert({"mekanism:radiation": n.Compound({"radiation": n.Double(0.03125)}),
                               "twilightforest:cap_shield": n.Compound({"permshields": n.Int(4), "tempshields": n.Int(7)})}).data["neoforge:attachments"]
        self.assertIsInstance(result["mekanism:radiation"], n.Double)
        self.assertEqual(result["twilightforest:fortification_shields"],
                         {"permanent_shields": n.Int(4), "temporary_shields": n.Int(7)})

    def test_aether_mob_drop_slots_and_phoenix_arrow_timers_are_mapped(self):
        result = self.convert({"aether:mob_accessory": n.Compound({"DropChances": n.Compound({
            "aether_gloves": n.Float(0.5), "aether_pendant": n.Float(0.25),
            "hands": n.Float(0.75), "necklace": n.Float(0.125),
        })}), "aether:phoenix_arrow": n.Compound({"FireTime": n.Int(20), "PhoenixArrow": n.Byte(1)})}).data["neoforge:attachments"]
        self.assertEqual(result["aether:mob_accessory"]["drop_chances"], {
            "aether:gloves_slot": n.Float(0.5), "aether:pendant_slot": n.Float(0.25),
            "hand": n.Float(0.75), "necklace": n.Float(0.125),
        })
        self.assertEqual(result["aether:phoenix_arrow"]["fire_time"], 20)

    def test_retired_and_transient_data_are_returned_to_the_ledger(self):
        result = self.convert({"witherstormmod:wither_sickness_tracker": n.Compound({"IsInfected": n.Byte(0)}),
                               "twilightforest:cap_thrown": n.Compound({"throwCooldown": n.Int(0)}),
                               "curios:inventory": n.Compound({"Curios": n.List[n.Compound]()})})
        self.assertEqual(set(result.removed), {"witherstormmod:wither_sickness_tracker", "twilightforest:cap_thrown"})
        self.assertEqual(set(result.data["neoforge:attachments"]), {"curios:inventory"})

    def test_parent_item_capability_and_unknown_fields_block_conversion(self):
        with self.assertRaisesRegex(ValueError, "没有旧能力转换规则：Parent"):
            self.convert({"Parent": n.Compound({"Items": n.List[n.Compound]()})})
        with self.assertRaisesRegex(ValueError, "字段与已审计结构不符"):
            self.convert({"aether:aether_player": n.Compound({"unexpected": n.Int(1)})})

    def test_existing_target_data_is_not_overwritten(self):
        with self.assertRaisesRegex(ValueError, "字段冲突"):
            convert_entity_data(n.Compound({"ForgeData": n.Compound(), "NeoForgeData": n.Compound()}))
        with self.assertRaisesRegex(ValueError, "字段冲突"):
            convert_entity_data(n.Compound({"ForgeCaps": n.Compound(), "neoforge:attachments": n.Compound()}))

    def test_l2_keeps_conditional_data_and_accounts_for_removed_death_timer(self):
        conditions = n.Compound({"active": n.Int(7)})
        result = self.convert({"l2library:conditionals": n.Compound({"data": conditions, "tickSinceDeath": n.Int(3177)})})
        self.assertEqual(result.data["neoforge:attachments"]["l2core:conditionals"], {"data": conditions})
        self.assertEqual(result.removed["l2library:conditionals/tickSinceDeath"], 3177)

    def test_forge_attributes_keep_base_values_and_zero_step_uses_native_default(self):
        values = {"forge:entity_gravity": 0.08, "forge:swim_speed": 1.0,
                  "forge:entity_reach": 3.0, "forge:block_reach": 4.5, "forge:step_height_addition": 0.0}
        source = n.Compound({"attributes": n.List[n.Compound]([
            n.Compound({"id": n.String(key), "base": n.Double(value)}) for key, value in values.items()
        ])})
        original = deepcopy(source)
        result = convert_entity_data(source)
        self.assertEqual({str(a["id"]): float(a["base"]) for a in result.data["attributes"]}, {
            "minecraft:generic.gravity": 0.08, "neoforge:swim_speed": 1.0,
            "minecraft:player.entity_interaction_range": 3.0, "minecraft:player.block_interaction_range": 4.5,
        })
        self.assertEqual(result.removed["forge:step_height_addition"]["base"], 0)
        self.assertEqual(source, original)

    def test_unmapped_attribute_modifiers_are_preserved(self):
        value = n.Compound({"id": n.String("l2damagetracker:crit_rate"), "base": n.Double(0.15),
                            "modifiers": n.List[n.Compound]([n.Compound({
                                "id": n.String("dimensionworks:crit_bonus"), "amount": n.Double(0.2),
                                "operation": n.String("add_value"),
                            })])})
        result = convert_entity_data(n.Compound({"attributes": n.List[n.Compound]([value])}))
        self.assertEqual(result.data["attributes"][0], value)

    def test_unexpected_step_state_and_attribute_collisions_block_conversion(self):
        with self.assertRaisesRegex(ValueError, "零状态"):
            convert_entity_data(n.Compound({"attributes": n.List[n.Compound]([
                n.Compound({"id": n.String("forge:step_height_addition"), "base": n.Double(1)})])}))
        with self.assertRaisesRegex(ValueError, "映射冲突"):
            convert_entity_data(n.Compound({"attributes": n.List[n.Compound]([
                n.Compound({"id": n.String("forge:entity_gravity"), "base": n.Double(0.08)}),
                n.Compound({"id": n.String("minecraft:generic.gravity"), "base": n.Double(0.08)}),
            ])}))


if __name__ == "__main__":
    unittest.main()
