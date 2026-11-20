from __future__ import annotations

import json
import shutil
import subprocess
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


@unittest.skipUnless(shutil.which("node"), "Node.js is required for generated recipe checks")
class CocktailCreateRecipeTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        harness = r"""
const fs = require('fs'), vm = require('vm');
const generated = [];
vm.runInNewContext(fs.readFileSync('kubejs/server_scripts/cocktail_create_recipes.js', 'utf8'), {
  console: {info() {}},
  ServerEvents: {generateData(stage, callback) {
    callback({json(id, recipe) {generated.push({stage, id, recipe});}});
  }}
});
process.stdout.write(JSON.stringify(generated));
"""
        result = subprocess.run(
            ["node", "-e", harness], cwd=ROOT, capture_output=True, text=True, check=True,
        )
        cls.generated = json.loads(result.stdout)
        cls.recipes = {entry["id"]: entry["recipe"] for entry in cls.generated}

    def test_all_twenty_four_pairs_override_the_published_resources(self) -> None:
        self.assertEqual(len(self.generated), 48)
        self.assertEqual(len(self.recipes), 48)
        self.assertTrue(all(entry["stage"] == "after_mods" for entry in self.generated))
        self.assertTrue(all(recipe["neoforge:conditions"] == [
            {"type": "neoforge:mod_loaded", "modid": "create"}
        ] for recipe in self.recipes.values()))

    def test_preserves_brewin_tankards_in_both_directions(self) -> None:
        tankards = {"kraken_rum", "nut_brown_ale", "rum"}
        for path, filling in self.recipes.items():
            if ":recipe/filling/" not in path:
                continue
            name = path.rsplit("/", 1)[1].removesuffix(".json")
            container = "brewinandchewin:tankard" if name in tankards else "minecraft:glass_bottle"
            emptying = self.recipes[path.replace("/filling/", "/emptying/")]
            self.assertEqual(filling["ingredients"][0], {"item": container})
            self.assertEqual(emptying["results"][0], {"id": container})

    def test_each_pair_conserves_250_mb_and_its_drink_identity(self) -> None:
        for path, filling in self.recipes.items():
            if ":recipe/filling/" not in path:
                continue
            name = path.rsplit("/", 1)[1].removesuffix(".json")
            drink = "cocktailsdelight:" + name
            emptying = self.recipes[path.replace("/filling/", "/emptying/")]
            self.assertEqual(filling["type"], "create:filling")
            self.assertEqual(filling["ingredients"][1], {
                "type": "neoforge:single", "fluid": drink, "amount": 250,
            })
            self.assertEqual(filling["results"], [{"id": drink}])
            self.assertEqual(emptying["type"], "create:emptying")
            self.assertEqual(emptying["ingredients"], [{"item": drink}])
            self.assertEqual(emptying["results"][1], {"id": drink, "amount": 250})


if __name__ == "__main__":
    unittest.main()
