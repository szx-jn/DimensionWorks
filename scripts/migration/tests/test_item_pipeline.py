from copy import deepcopy
import unittest

import nbtlib as n

from scripts.migration.item_pipeline import ItemPipeline, NativeItemRequired, unit_source


class Dfu:
    def update_entity(self, source):
        return deepcopy(source)

    def update(self, source, kind):
        if kind in {"player", "block_entity"}:
            assert "ActiveEffects" not in source
            return deepcopy(source)
        assert kind == "item_stack"
        data = n.Compound({"id": deepcopy(source["id"]), "count": n.Int(int(source["Count"]))})
        if source.get("tag"):
            data["components"] = n.Compound({"minecraft:custom_data": deepcopy(source["tag"])})
        return data


def pipeline():
    return ItemPipeline(Dfu(), {"minecraft:item": ["minecraft:stone", "create:filter", "sophisticatedbackpacks:diamond_backpack"],
                                "minecraft:entity_type": [], "minecraft:attribute": [], "minecraft:fluid": ["minecraft:lava"],
                                "minecraft:enchantment": [], "minecraft:potion": [], "minecraft:mob_effect": [],
                                "minecraft:block_entity_type": []})


class ItemPipelineTests(unittest.TestCase):
    def test_negative_byte_uses_real_count_only_in_protected_inventory(self):
        old = n.parse_nbt('{id:"minecraft:stone",Count:-64b,realCount:960,Slot:13}')
        converter = pipeline()
        with self.assertRaisesRegex(ValueError, "未经确认"):
            converter.item(old)
        result = converter.item(old, oversized_inventory=True)
        self.assertEqual(int(result["count"]), 960)
        self.assertEqual(int(result["Slot"]), 13)
        self.assertEqual(int(old["Count"]), -64)

    def test_nested_author_conversion_finishes_before_parent(self):
        old = n.parse_nbt('{id:"create:filter",Count:1b,tag:{Items:{Size:18,Items:[{id:"sophisticatedbackpacks:diamond_backpack",Count:1b,Slot:5,tag:{contentsUuid:[I;1,2,3,4]}}]}}}')
        converter = pipeline()
        with self.assertRaises(NativeItemRequired) as raised:
            converter.item(old)
        needed = raised.exception
        self.assertEqual(needed.kind, "backpack")
        self.assertNotIn("Slot", needed.source)
        converter.register_native(needed.source, n.parse_nbt('{id:"sophisticatedbackpacks:diamond_backpack",count:1,components:{"sophisticatedcore:storage_uuid":[I;1,2,3,4]}}'))
        result = converter.item(old)
        entry = result["components"]["create:filter_items"][0]
        self.assertEqual(int(entry["slot"]), 5)
        self.assertEqual(entry["item"]["components"]["sophisticatedcore:storage_uuid"].tolist(), [1, 2, 3, 4])
        self.assertNotIn("Slot", entry["item"])

    def test_native_cache_restores_each_actual_quantity_and_slot(self):
        source = n.parse_nbt('{id:"minecraft:stone",Count:12b,Slot:1}')
        converter = pipeline()
        converter.register_native(source, n.parse_nbt('{id:"minecraft:stone",count:1}'))
        other = n.parse_nbt('{id:"minecraft:stone",Count:5b,Slot:7}')
        self.assertEqual(int(converter.item(source)["count"]), 12)
        self.assertEqual(int(converter.item(other)["count"]), 5)
        self.assertEqual(int(converter.item(other)["Slot"]), 7)

    def test_rejects_conflicting_native_metadata_and_count(self):
        source = n.parse_nbt('{id:"minecraft:stone",Count:1b}')
        converter = pipeline()
        converter.register_native(source, n.parse_nbt('{id:"minecraft:stone",count:1}'))
        with self.assertRaisesRegex(ValueError, "冲突"):
            converter.register_native(source, n.parse_nbt('{id:"minecraft:stone",count:1,components:{"minecraft:custom_data":{owner:"other"}}}'))
        with self.assertRaisesRegex(ValueError, "数量"):
            converter.register_native(source, n.parse_nbt('{id:"minecraft:stone",count:2}'))

    def test_cache_keeps_metadata_nbt_types_distinct(self):
        converter = pipeline()
        converter.item(n.parse_nbt('{id:"minecraft:stone",Count:1b,tag:{value:3}}'))
        converter.item(n.parse_nbt('{id:"minecraft:stone",Count:1b,tag:{value:3L}}'))
        self.assertEqual(len(converter.ready), 2)

    def test_fluid_registration_amount_and_empty_marker(self):
        converter = pipeline()
        self.assertEqual(converter.fluid(n.parse_nbt('{FluidName:"minecraft:lava",Amount:9000}')), n.parse_nbt('{id:"minecraft:lava",amount:9000}'))
        self.assertEqual(converter.fluid(n.parse_nbt('{Empty:""}')), n.Compound())
        self.assertEqual(len(converter.changes), 1)
        with self.assertRaisesRegex(ValueError, "注册名"):
            converter.fluid(n.parse_nbt('{FluidName:"missing:fluid",Amount:9000}'))

    def test_sophisticated_render_fluid_uses_current_fluid_stack_shape(self):
        converter = pipeline()
        source = n.parse_nbt('{"sophisticatedcore:render_info_tag":{tanks:[{position:"left",info:{fillRatio:0.25f,fluid:{FluidName:"minecraft:lava",Amount:9000}}},{position:"right",info:{}}]}}')
        result = converter.tree(source)
        component = result["sophisticatedcore:render_info_tag"]
        self.assertEqual(component["tanks"][0]["info"]["fluid"], n.parse_nbt('{id:"minecraft:lava",amount:9000}'))
        self.assertEqual(component["tanks"][0]["info"]["fillRatio"], n.Float(0.25))
        self.assertEqual(component["tanks"][1]["info"], n.Compound())
        self.assertEqual(converter.tree(result), result)

    def test_player_inventory_uuid_and_named_effect_are_converted_together(self):
        converter = pipeline(); converter.effects.add("twilightforest:frosted")
        source = n.parse_nbt('{UUID:[I;1,2,3,4],Inventory:[{id:"minecraft:stone",Count:12b,Slot:7b}],ActiveEffects:[{Id:214,"forge:id":"twilightforest:frosted",Amplifier:1b,Duration:77,Ambient:0b,ShowParticles:1b,ShowIcon:1b,CurativeItems:[{id:"minecraft:milk_bucket",Count:1b}]}]}')
        result = converter.player(source)
        self.assertEqual(result["UUID"], source["UUID"])
        self.assertEqual(result["Inventory"][0], n.parse_nbt('{id:"minecraft:stone",count:12,Slot:7b}'))
        self.assertEqual(result["active_effects"][0]["id"], "twilightforest:frosted")
        self.assertNotIn("ActiveEffects", result)

    def test_sophisticated_block_inventory_preserves_real_count_and_slot(self):
        converter = pipeline(); converter.block_entities.add("sophisticatedstorage:chest")
        source = n.parse_nbt('{id:"sophisticatedstorage:chest",x:1,y:2,z:3,storageWrapper:{contents:{inventory:{Size:2,Items:[{id:"minecraft:stone",Count:-64b,realCount:960,Slot:1}]}}}}')
        result = converter.block_entity(source)
        item = result["storageWrapper"]["contents"]["inventory"]["Items"][0]
        self.assertEqual(item, n.parse_nbt('{id:"minecraft:stone",count:960,Slot:1}'))

    def test_mechanical_entity_retains_outer_uuid_and_uses_complete_native_payload(self):
        converter = pipeline(); converter.entities.add("create:stationary_contraption")
        source = n.parse_nbt('{id:"create:stationary_contraption",UUID:[I;1,2,3,4],Contraption:{Type:"create:bearing",Blocks:{Palette:[{Name:"minecraft:stone"}],BlockList:[{Pos:7L,State:0}]}}}')
        original = deepcopy(source); native = deepcopy(source["Contraption"])
        native["Anchor"] = n.IntArray([1, 2, 3])
        with self.assertRaisesRegex(ValueError, "完整原生"):
            converter.entity(source)
        converter.convert_contraption = lambda old: deepcopy(native)
        result = converter.entity(source)
        self.assertEqual(result["UUID"], original["UUID"])
        self.assertEqual(result["Contraption"], native)
        self.assertEqual(source, original)

    def test_beehive_entities_with_and_without_uuid_are_restored_from_original(self):
        class NestedDfu(Dfu):
            def update(self, source, kind):
                result = super().update(source, kind)
                if kind == "block_entity":
                    for bee in result["Bees"]:
                        bee["EntityData"]["attributes"] = n.parse_nbt('[{id:"minecraft:generic.max_health",base:999.0d}]')
                        bee["EntityData"]["Health"] = n.Float(1)
                    result["bees"] = n.List[n.Compound]([n.Compound({"ticks_in_hive":v["TicksInHive"],
                        "entity_data":deepcopy(v["EntityData"])}) for v in result["Bees"]])
                return result

            def update_entity(self, source):
                result = deepcopy(source)
                result["attributes"] = n.List[n.Compound]([n.Compound({"id":v["Name"], "base":v["Base"]}) for v in result.pop("Attributes")])
                return result

        converter = pipeline(); converter.dfu = NestedDfu()
        converter.block_entities.add("minecraft:beehive"); converter.entities.add("minecraft:bee")
        converter.attributes.add("minecraft:generic.max_health")
        source = n.parse_nbt('{id:"minecraft:beehive",x:1,y:2,z:3,Bees:[{TicksInHive:100,EntityData:{id:"minecraft:bee",Health:10.0f,Attributes:[{Name:"minecraft:generic.max_health",Base:20.0d}]}},{TicksInHive:200,EntityData:{id:"minecraft:bee",UUID:[I;1,2,3,4],Health:8.0f,Attributes:[{Name:"minecraft:generic.max_health",Base:18.0d}]}}]}')
        original = deepcopy(source); result = converter.block_entity(source)
        for i, bee in enumerate(result["bees"]):
            self.assertEqual(bee["entity_data"]["Health"], original["Bees"][i]["EntityData"]["Health"])
            self.assertEqual(bee["entity_data"].get("UUID"), original["Bees"][i]["EntityData"].get("UUID"))
            self.assertEqual(bee["entity_data"]["attributes"][0]["base"], original["Bees"][i]["EntityData"]["Attributes"][0]["Base"])
            self.assertNotIn("Attributes", bee["entity_data"])
        self.assertNotIn("Bees", result)
        self.assertEqual(source, original)

    def test_ae_bus_zero_quantity_filter_keeps_key_instead_of_erasing_configuration(self):
        converter = pipeline()
        source = n.parse_nbt('{south:{config:[{"#c":"ae2:i",id:"minecraft:stone","#":0L}]}}')
        result = converter.tree(source)
        self.assertEqual(result, n.parse_nbt('{south:{config:[{"#t":"ae2:i",id:"minecraft:stone","#":0L}]}}'))
        source["south"]["config"][0]["#"] = n.Long(-1)
        with self.assertRaisesRegex(ValueError, "非负 Long"):
            converter.tree(source)

    def test_twilight_chest_inventory_is_kept_under_native_chest_entity_type(self):
        converter = pipeline(); converter.block_entities.add("minecraft:chest")
        source = n.parse_nbt('{id:"twilightforest:tf_chest",x:1,y:2,z:3,Items:[{id:"minecraft:stone",Count:17b,Slot:7b}],ForgeCaps:{}}')
        result = converter.block_entity(source)
        self.assertEqual(result["id"], "minecraft:chest")
        self.assertEqual(result["Items"][0], n.parse_nbt('{id:"minecraft:stone",count:17,Slot:7b}'))
        self.assertEqual([int(result[k]) for k in ("x", "y", "z")], [1, 2, 3])

    def test_removed_air_second_trade_cost_keeps_real_cost_output_and_stock(self):
        class MerchantDfu(Dfu):
            def update_entity(self, source):
                result = deepcopy(source)
                for recipe in result["Offers"]["Recipes"]:
                    recipe.pop("buyB")
                return result

        converter = pipeline(); converter.dfu = MerchantDfu(); converter.entities.add("minecraft:villager")
        source = n.parse_nbt('{id:"minecraft:villager",UUID:[I;1,2,3,4],Offers:{Recipes:[{buy:{id:"minecraft:stone",Count:8b},buyB:{id:"minecraft:air",Count:1b,tag:{Charged:0b}},sell:{id:"minecraft:stone",Count:12b},uses:4,maxUses:16}]},Inventory:[{id:"minecraft:stone",Count:3b,Slot:1b}]}')
        result = converter.entity(source)
        recipe = result["Offers"]["Recipes"][0]
        self.assertNotIn("buyB", recipe)
        self.assertEqual(recipe["buy"]["count"], 8)
        self.assertEqual(recipe["sell"]["count"], 12)
        self.assertEqual((recipe["uses"], recipe["maxUses"]), (4, 16))
        self.assertEqual(result["Inventory"][0]["count"], 3)
        self.assertEqual(converter.changes[0]["removed"], source["Offers"]["Recipes"][0]["buyB"])
        source["Offers"]["Recipes"][0]["buyB"]["id"] = n.String("minecraft:stone")
        with self.assertRaisesRegex(ValueError, "库存位置"):
            converter.entity(source)

    def test_only_audited_empty_basin_chemical_buffers_are_normalized(self):
        converter = pipeline(); converter.block_entities.add("create:basin")
        source = n.parse_nbt('{id:"create:basin",x:1,y:2,z:3,DimensionWorksBasinGas:{gasName:"mekanism:empty",amount:0L},Items:[{id:"minecraft:stone",Count:9b}]}')
        result = converter.block_entity(source)
        self.assertEqual(result["DimensionWorksBasinGas"], n.Compound())
        self.assertEqual(result["Items"][0]["count"], 9)
        source["DimensionWorksBasinGas"] = n.parse_nbt('{gasName:"mekanism:hydrogen",amount:7L}')
        with self.assertRaisesRegex(ValueError, "非空旧状态"):
            converter.block_entity(source)


if __name__ == "__main__":
    unittest.main()
