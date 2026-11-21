from copy import deepcopy
import unittest

import nbtlib as n

from scripts.migration.level_data import convert_forced_chunks, convert_level_attachments, convert_world_gen_settings, promote_brewing_metadata


class LevelDataTests(unittest.TestCase):
    def test_completed_file_brewing_marker_does_not_modify_payload_or_allow_unknown_versions(self):
        source = n.parse_nbt('{"brewinandchewin:data_version":2,DataVersion:3955,data:{Items:[{id:"brewinandchewin:beer",count:20,components:{"minecraft:custom_data":{archived_version:2}}}]}}')
        original = deepcopy(source)
        result = promote_brewing_metadata(source, 100)
        self.assertEqual(result.data["data"], source["data"])
        self.assertEqual(result.data["brewinandchewin:data_version"], n.Int(100))
        self.assertEqual(result.removed, {"brewinandchewin:data_version": n.Int(2)})
        self.assertEqual(source, original)
        source["brewinandchewin:data_version"] = n.Int(3)
        with self.assertRaisesRegex(ValueError, "唯一旧档"):
            promote_brewing_metadata(source, 100)
        with self.assertRaisesRegex(ValueError, "目标酿酒"):
            promote_brewing_metadata(original, 101)

    def test_world_generator_upgrade_preserves_seed_other_dimensions_and_audited_source(self):
        source = n.parse_nbt('{seed:2685045119030130642L,generate_features:1b,bonus_chest:0b,dimensions:{"minecraft:overworld":{type:"minecraft:overworld",generator:{type:"minecraft:noise",settings:"minecraft:overworld"}},"twilightforest:twilight_forest":{type:"twilightforest:twilight_forest_type",generator:{type:"twilightforest:structure_locating_wrapper",noise_generation_settings:"twilightforest:twilight_noise_gen",generate_dark_forest_canopy:1b,wrapped_generator:{type:"minecraft:noise"},landmark_placement_allowed_biomes:{},dark_forest_canopy_height:19}},"witherstormmod:bowels":{type:"witherstormmod:bowels",generator:{type:"minecraft:flat"}}}}')
        target = n.parse_nbt('{"minecraft:overworld":{type:"minecraft:overworld",generator:{type:"minecraft:flat"}},"twilightforest:twilight_forest":{type:"twilightforest:twilight_forest_type",generator:{type:"minecraft:noise",settings:"twilightforest:twilight_noise_gen",biome_source:{type:"twilightforest:twilight_biomes",terrain_data:"twilightforest:biome_grid"}}}}')
        original = deepcopy(source)
        converted = convert_world_gen_settings(source, target, empty_retired_dimensions={"witherstormmod:bowels"})
        self.assertEqual(converted.data["seed"], source["seed"])
        self.assertEqual(converted.data["dimensions"]["minecraft:overworld"], source["dimensions"]["minecraft:overworld"])
        self.assertEqual(converted.data["dimensions"]["twilightforest:twilight_forest"], target["twilightforest:twilight_forest"])
        self.assertEqual(converted.removed["twilightforest:twilight_forest/source_generator"], source["dimensions"]["twilightforest:twilight_forest"]["generator"])
        self.assertEqual(source, original)
        with self.assertRaisesRegex(ValueError, "尚未证明"):
            convert_world_gen_settings(source, target, empty_retired_dimensions=set())
        with self.assertRaisesRegex(ValueError, "未授权"):
            convert_world_gen_settings(source, target, empty_retired_dimensions={"minecraft:overworld"})

    def test_time_and_maid_bindings_keep_state_and_reject_nonempty_legacy_inventory(self):
        source = n.parse_nbt('{"aether:aether_time":{DayTime:1723456L,EternalDay:0b},"maid_storage_manager:block_occupy":{occupied:[]},"maid_storage_manager:maid_persist":{},"maid_storage_manager:inventory_list":{}}')
        ids = {"aether:aether_time", "maid_storage_manager:craft_block_occupy",
               "maid_storage_manager:maid_item_persist_data", "maid_storage_manager:inventory_list_data"}
        original = deepcopy(source)
        result = convert_level_attachments(source, ids)
        self.assertEqual(result.data["aether:aether_time"], n.parse_nbt('{day_time:1723456L,eternal_day:0b,should_wait:0b}'))
        self.assertEqual(result.data["maid_storage_manager:craft_block_occupy"], source["maid_storage_manager:block_occupy"])
        self.assertEqual(source, original)
        with self.assertRaisesRegex(ValueError, "未处理内容"):
            convert_level_attachments(n.parse_nbt('{"maid_storage_manager:inventory_list":{items:[{id:"minecraft:diamond",Count:1b}]}}'), ids)

    def test_only_audited_retired_and_empty_cache_records_can_be_omitted(self):
        source = n.parse_nbt('{"witherstormmod:chunk_loader":{},"witherstormmod:chunk_loading_block_entities":{LoadedEntities:[]},"twilightforest:teleporter_cache":{dest:[]}}')
        result = convert_level_attachments(source, set())
        self.assertEqual(result.data, n.Compound())
        self.assertEqual(result.removed, dict(source))
        for text in ('{"witherstormmod:chunk_loading_block_entities":{LoadedEntities:[{id:"minecraft:chest"}]}}',
                     '{"twilightforest:teleporter_cache":{dest:[{X:4,Y:6,Z:8}]}}'):
            with self.assertRaises(ValueError):
                convert_level_attachments(n.parse_nbt(text), set())

    def test_ticket_controller_preserves_long_chunk_and_compound_machine_positions(self):
        source = n.parse_nbt('{Forced:[L;4294967294L],ForgeForced:[{Mod:"mekanism",ModForced:[{Chunk:-8589934593L,TickingBlocks:[{X:-33,Y:-61,Z:1024}]}]}]}')
        original = deepcopy(source)
        result = convert_forced_chunks(source, {"mekanism:chunk_loader"})
        self.assertEqual(result.data["Forced"].tolist(), source["Forced"].tolist())
        self.assertEqual(result.data["ModForced"][0]["Controller"], n.String("mekanism:chunk_loader"))
        self.assertEqual(result.data["ModForced"][0]["ModForced"], source["ForgeForced"][0]["ModForced"])
        self.assertEqual(source, original)
        with self.assertRaisesRegex(ValueError, "未注册"):
            convert_forced_chunks(source, set())

    def test_incompatible_ticket_controller_can_be_retired_with_a_complete_ledger(self):
        source = n.parse_nbt('{Forced:[L;4L],ForgeForced:[{Mod:"create_power_loader",ModForced:[{Chunk:8L,TickingBlocks:[{X:1,Y:2,Z:3}]}]},{Mod:"mekanism",ModForced:[{Chunk:9L,TickingBlocks:[{X:4,Y:5,Z:6}]}]}]}')
        result = convert_forced_chunks(
            source,
            {"create_power_loader:chunk_load_manager", "mekanism:chunk_loader"},
            {"mekanism:chunk_loader"},
        )
        self.assertEqual([str(row["Controller"]) for row in result.data["ModForced"]],
                         ["create_power_loader:chunk_load_manager"])
        self.assertEqual(result.removed["ForgeForced/mekanism:chunk_loader"],
                         source["ForgeForced"][1])
        with self.assertRaisesRegex(ValueError, "不存在"):
            convert_forced_chunks(
                source,
                {"create_power_loader:chunk_load_manager", "mekanism:chunk_loader"},
                {"missing:controller"},
            )


if __name__ == "__main__":
    unittest.main()
