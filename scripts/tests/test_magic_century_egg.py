from __future__ import annotations

import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
RECIPE_DIR = ROOT / "kubejs" / "data" / "dimensionworks" / "recipes" / "magic_century_egg"


class MagicCenturyEggContractTest(unittest.TestCase):
    def test_sequenced_assembly_is_split_into_short_loops(self) -> None:
        recipes = [
            json.loads(path.read_text(encoding="utf-8"))
            for path in sorted(RECIPE_DIR.glob("*.json"))
        ]

        self.assertEqual(len(recipes), 10)
        self.assertFalse((RECIPE_DIR / "sequenced_assembly.json").exists())
        for recipe in recipes:
            if recipe["type"] == "create:sequenced_assembly":
                self.assertLessEqual(len(recipe["sequence"]), 7)

        by_result = {
            recipe["results"][0]["item"]: recipe
            for recipe in recipes
            if "results" in recipe
        }
        smelting = next(recipe for recipe in recipes if recipe["type"] == "minecraft:smelting")
        self.assertEqual(by_result["kubejs:century_egg_pressed"]["loops"], 8)
        self.assertEqual(by_result["kubejs:century_egg_vanilla_enriched"]["loops"], 1)
        self.assertEqual(by_result["kubejs:century_egg_industrial_enriched"]["loops"], 1)
        self.assertEqual(by_result["kubejs:century_egg_ae_enriched"]["loops"], 1)
        self.assertEqual(by_result["kubejs:century_egg_eco_enriched"]["loops"], 1)
        self.assertEqual(by_result["kubejs:century_egg_eco_components_enriched"]["loops"], 1)
        self.assertEqual(by_result["kubejs:magic_century_egg"]["loops"], 8)
        self.assertEqual(by_result["kubejs:century_egg_washed"]["type"], "create:splashing")
        self.assertEqual(smelting["result"], "kubejs:century_egg_smelted")
        self.assertEqual(by_result["kubejs:century_egg_mixture"]["type"], "create:mixing")

        vanilla = by_result["kubejs:century_egg_vanilla_enriched"]
        vanilla_inputs = {
            step["ingredients"][1]["item"] for step in vanilla["sequence"]
        }
        self.assertEqual(vanilla_inputs, {
            "minecraft:golden_apple",
            "minecraft:ender_pearl",
            "minecraft:diamond",
        })

        industrial = by_result["kubejs:century_egg_industrial_enriched"]
        industrial_inputs = {
            step["ingredients"][1]["item"] for step in industrial["sequence"]
        }
        self.assertEqual(industrial_inputs, {
            "ae2:engineering_processor",
            "ae2:cell_component_256k",
            "mekanism:pellet_polonium",
            "mekanism:ultimate_control_circuit",
            "mekanism:alloy_atomic",
            "create:precision_mechanism",
        })

        ae_inputs = {
            step["ingredients"][1]["item"]
            for step in by_result["kubejs:century_egg_ae_enriched"]["sequence"]
        }
        self.assertEqual(ae_inputs, {
            "ae2:cell_component_1k",
            "ae2omnicells:omni_cell_component_1k",
            "ae2omnicells:complex_omni_cell_component_1k",
            "ae2omnicells:quantum_omni_cell_component_1k",
            "ae2:fluix_crystal",
            "ae2:charged_certus_quartz_crystal",
            "ae2:certus_quartz_crystal",
        })

        eco_inputs = {
            step["ingredients"][1]["item"]
            for step in by_result["kubejs:century_egg_eco_enriched"]["sequence"]
        }
        self.assertEqual(eco_inputs, {
            "neoecoae:energized_crystal",
            "neoecoae:energized_fluix_crystal",
            "neoecoae:crystal_ingot",
            "neoecoae:crystal_matrix",
            "neoecoae:energized_superconductive_ingot",
            "neoecoae:superconducting_processor",
            "neoecoae:eco_cell_component_16m",
        })

        eco_component_inputs = {
            step["ingredients"][1]["item"]
            for step in by_result["kubejs:century_egg_eco_components_enriched"]["sequence"]
        }
        self.assertEqual(eco_component_inputs, {
            "neoecoae:eco_cell_component_64m",
            "neoecoae:eco_cell_component_256m",
            "neoecoae:eco_computation_cell_l4",
            "neoecoae:eco_computation_cell_l6",
            "neoecoae:eco_computation_cell_l9",
            "neoecoae:computation_parallel_core_l4",
            "neoecoae:computation_threading_core_l4",
        })

    def test_original_century_egg_is_removed_and_hidden(self) -> None:
        server = (ROOT / "kubejs" / "server_scripts" / "magic_century_egg.js").read_text(encoding="utf-8")
        startup = (ROOT / "kubejs" / "startup_scripts" / "magic_century_egg.js").read_text(encoding="utf-8")
        client = (ROOT / "kubejs" / "client_scripts" / "magic_century_egg.js").read_text(encoding="utf-8")

        self.assertIn("output: DW_ORIGINAL_CENTURY_EGG_ID", server)
        self.assertIn("kaleidoscope_chinesefood:century_egg_congee", server)
        self.assertIn("dwPurgeOriginalCenturyEgg", server)
        self.assertIn("event.remove('kaleidoscope_chinesefood:century_egg')", startup)
        self.assertIn("event.hide('kaleidoscope_chinesefood:century_egg')", client)

    def test_magic_egg_uses_stable_food_definition(self) -> None:
        startup = (ROOT / "kubejs" / "startup_scripts" / "magic_century_egg.js").read_text(encoding="utf-8")
        server = (ROOT / "kubejs" / "server_scripts" / "magic_century_egg.js").read_text(encoding="utf-8")

        self.assertNotIn(".name(", startup)
        self.assertNotIn(".hunger(99999)", startup)
        self.assertIn(".rarity('epic')", startup)
        self.assertIn(".hunger(20)", startup)
        self.assertIn(".effect('minecraft:absorption'", startup)
        self.assertIn(".effect('minecraft:levitation'", startup)
        self.assertIn(".eaten(dwMarkMagicEggEaten)", startup)
        self.assertIn("isBeneficial()", startup)
        self.assertIn("const DW_MAGIC_EGG_DURATION_TICKS = 20 * 60 * 3", startup)
        self.assertIn("const DW_MAGIC_EGG_DURATION_TICKS = 20 * 60 * 3", server)

    def test_eating_again_triggers_death_immediately(self) -> None:
        startup = (ROOT / "kubejs" / "startup_scripts" / "magic_century_egg.js").read_text(encoding="utf-8")
        server = (ROOT / "kubejs" / "server_scripts" / "magic_century_egg.js").read_text(encoding="utf-8")

        self.assertNotIn("DW_MAGIC_EGG_PENDING", startup)
        self.assertNotIn("DW_MAGIC_EGG_DEADLINE", startup)
        self.assertIn("if (data.getBoolean(DW_MAGIC_EGG_PENDING))", server)
        self.assertIn("dwStartMagicEggFireworks(level, player.x, player.y, player.z)", server)
        self.assertIn("player.hurt(level.damageSources().genericKill(), DW_MAGIC_EGG_DAMAGE)", server)


if __name__ == "__main__":
    unittest.main()
