from __future__ import annotations

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
API = ROOT / "kubejs/server_scripts/crushing_api.js"
MINERAL = ROOT / "kubejs/server_scripts/basic_mineral_propagation.js"
ITEMS = ROOT / "kubejs/startup_scripts/mineral_processing_items.js"


class CrushingApiTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.api = API.read_text(encoding="utf-8")
        cls.mineral = MINERAL.read_text(encoding="utf-8")
        cls.items = ITEMS.read_text(encoding="utf-8")

    def test_create_and_mekanism_adapters_share_one_registry(self) -> None:
        self.assertIn("adapters['create:crushing']", self.api)
        self.assertIn("adapters['create:milling']", self.api)
        self.assertIn("adapters['mekanism:crushing']", self.api)
        self.assertIn("registerAdapter: registerAdapter", self.api)
        self.assertIn("global.DimensionWorksCrushing", self.api)

    def test_create_supports_independent_chance_outputs(self) -> None:
        self.assertIn("stack.withChance(output.chance)", self.api)
        self.assertIn("event.recipes.create.crushing(outputs, recipe.input)", self.api)
        self.assertIn("event.recipes.create.milling(outputs, recipe.input)", self.api)

    def test_mekanism_rejects_unsupported_chance_outputs(self) -> None:
        self.assertIn("only supports one guaranteed output", self.api)
        self.assertIn("event.recipes.mekanism.crushing(", self.api)

    def test_iron_yield_is_two_plus_seventy_percent_bonus(self) -> None:
        self.assertIn("machine: 'create:milling'", self.mineral)
        self.assertIn("{ item: 'mekanism:dust_iron', count: 2 }", self.mineral)
        self.assertIn("{ item: 'mekanism:dust_iron', count: 1, chance: 0.7 }", self.mineral)
        self.assertIn("'kubejs:iron_concentrate'", self.mineral)
        self.assertIn("'mekanism:dust_iron'", self.mineral)

    def test_intermediate_item_is_registered(self) -> None:
        self.assertIn("event.create('iron_concentrate')", self.items)
        self.assertIn(".texture('minecraft:item/raw_iron')", self.items)


if __name__ == "__main__":
    unittest.main()
