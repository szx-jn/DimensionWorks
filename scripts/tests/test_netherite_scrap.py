from __future__ import annotations

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class NetheriteScrapRecipeContractTest(unittest.TestCase):
    def test_mixing_consumes_all_seven_inputs(self) -> None:
        script = (ROOT / "kubejs/server_scripts/netherite_scrap_crystallization.js").read_text(encoding="utf-8")
        for expected in (
            "mekanism:sulfuric_acid",
            "mekanism:sulfur_dioxide",
            "minecraft:iron_ingot",
            "minecraft:gold_ingot",
            "minecraft:copper_ingot",
            "ae2:ender_dust",
            "create:sturdy_sheet",
        ):
            self.assertIn(expected, script)
        self.assertIn("Fluid.of('mekanism:sulfuric_acid', 250)", script)
        self.assertIn("Fluid.of('mekanism:sulfur_dioxide', 250)", script)
        self.assertIn("'kubejs:mixed_metal'", script)
        self.assertIn(".heated()", script)
        self.assertNotIn("#forge:ingots/enderium", script)
        self.assertIn("dimensionworks:netherite_scrap/mixed_metal", script)

    def test_only_netherite_high_pressure_crystal_assigns_the_trait(self) -> None:
        script = (ROOT / "kubejs/server_scripts/netherite_scrap_crystallization.js").read_text(encoding="utf-8")
        self.assertIn("dimensionworks_pressure_crystals:high_pressure_crystal", script)
        self.assertIn("'Metal': 'netherite'", script)
        self.assertIn("type: 'create:sequenced_assembly'", script)
        self.assertIn("type: 'create:deploying'", script)
        self.assertIn("type: 'create:pressing'", script)
        self.assertIn("count: 2", script)
        self.assertIn("dimensionworks:netherite_scrap/from_mixed_metal", script)
        self.assertNotIn("DW_NETHERITE_CRYSTAL_METALS", script)
        self.assertNotIn("minecraft:echo_shard", script)
        self.assertNotIn("ae2:engineering_processor", script)
        self.assertNotIn("mekanism:alloy_atomic", script)

    def test_mixed_metal_item_is_registered(self) -> None:
        startup = (ROOT / "kubejs/startup_scripts/netherite_scrap.js").read_text(encoding="utf-8")
        self.assertIn("event.create('mixed_metal')", startup)
        self.assertIn("event.create('incomplete_netherite_scrap')", startup)

    def test_native_mekanism_fluids_are_not_duplicated(self) -> None:
        startup_files = list((ROOT / "kubejs/startup_scripts").glob("*.js"))
        combined = "\n".join(path.read_text(encoding="utf-8") for path in startup_files)
        self.assertNotIn("event.create('liquid_sulfur_dioxide')", combined)
        self.assertNotIn("event.create('liquid_sulfuric_acid')", combined)


if __name__ == "__main__":
    unittest.main()
