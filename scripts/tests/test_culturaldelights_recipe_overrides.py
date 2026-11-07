from __future__ import annotations

import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
RECIPE_ROOT = ROOT / "kubejs" / "data" / "minecraft" / "recipes"


class CulturalDelightsRecipeOverridesTest(unittest.TestCase):
    def test_replaces_empty_vanilla_recipe_resources(self) -> None:
        expected_results = {
            "beetroot_soup.json": "minecraft:beetroot_soup",
            "mushroom_stew.json": "minecraft:mushroom_stew",
            "rabbit_stew_from_brown_mushroom.json": "minecraft:rabbit_stew",
            "rabbit_stew_from_red_mushroom.json": "minecraft:rabbit_stew",
        }

        for filename, result in expected_results.items():
            with self.subTest(filename=filename):
                path = RECIPE_ROOT / filename
                self.assertGreater(path.stat().st_size, 0)
                recipe = json.loads(path.read_text(encoding="utf-8"))
                self.assertEqual(recipe["type"], "minecraft:crafting_shapeless")
                self.assertEqual(recipe["result"]["item"], result)
                self.assertTrue(recipe["ingredients"])


if __name__ == "__main__":
    unittest.main()
