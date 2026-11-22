from __future__ import annotations

import json
import shutil
import subprocess
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


@unittest.skipUnless(shutil.which("node"), "Node.js is required for recipe registration checks")
class CopperProcessingTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        harness = r"""
const fs = require('fs'), vm = require('vm');
const recipes = [], removed = [];
const stack = (item, count = 1, components = {}) => ({item, count, components});
function add(json) {
  recipes.push(json);
  const builder = {
    id(id) { json.id = id; return builder; },
    heated() { json.heat = 'heated'; return builder; },
    superheated() { json.heat = 'superheated'; return builder; },
    processingTime(time) { json.processingTime = time; return builder; }
  };
  return builder;
}
const event = {
  remove(filter) { removed.push(filter); },
  custom: add,
  recipes: {create: new Proxy({}, {get: (_, type) => (output, input) => add({type: 'create:' + type, output, input})})}
};
const context = vm.createContext({console: {info() {}}, Item: {of: stack},
  CreateItem: {of(item, chance) { return {...item, chance}; }},
  ServerEvents: {recipes(fn) { fn(event); }}});
for (const file of ['crushing_api.js', 'copper_processing.js'])
  vm.runInContext(fs.readFileSync('kubejs/server_scripts/' + file, 'utf8'), context);
process.stdout.write(JSON.stringify({recipes, removed}));
"""
        result = subprocess.run(
            ["node", "-e", harness], cwd=ROOT, text=True,
            capture_output=True, check=True,
        )
        cls.data = json.loads(result.stdout)
        cls.recipes = {r["id"].rsplit("/", 1)[-1]: r for r in cls.data["recipes"]}

    def test_reduction_preserves_original_inputs_and_outputs(self) -> None:
        recipe = self.recipes["reduction_with_carbon_monoxide"]
        self.assertEqual(recipe["type"], "createcraftedbeginning:reactor_kettle")
        self.assertEqual(recipe["ingredients"], [{"item": "minecraft:raw_copper", "count": 1}])
        self.assertEqual(recipe["gas_ingredients"], [{
            "amount": 250,
            "ingredient": {"gas": "dimensionworks:carbon_monoxide"}
        }])
        self.assertEqual(recipe["results"], [{"id": "minecraft:copper_ingot", "count": 1}])
        self.assertEqual(recipe["gas_results"], [{"id": "dimensionworks:carbon_dioxide", "amount": 250}])
        self.assertEqual((recipe["temperature"], recipe["temperature_matching"]), ("heated", "exact"))

    def test_grinding_has_fixed_and_independent_chance_outputs(self) -> None:
        recipe = self.recipes["dust_from_ingot"]
        self.assertEqual(recipe["type"], "create:crushing")
        self.assertEqual(recipe["input"], "minecraft:copper_ingot")
        products = recipe["output"]
        self.assertEqual([(p["count"], p.get("chance", 1)) for p in products], [(1, 1), (2, 0.8), (1, 0.6)])
        self.assertTrue(all(p["item"] == "mekanism:dust_copper" for p in products))
        self.assertAlmostEqual(sum(p["count"] * p.get("chance", 1) for p in products), 3.2)

    def test_veridium_is_heated_mixing_with_four_real_dust_inputs(self) -> None:
        recipe = self.recipes["veridium_from_dust"]
        self.assertEqual(recipe["type"], "create:mixing")
        self.assertEqual(recipe["heat"], "heated")
        self.assertEqual(recipe["output"], "2x create:veridium")
        self.assertEqual(recipe["input"].count("mekanism:dust_copper"), 4)
        self.assertEqual(recipe["input"].count("minecraft:cobblestone"), 1)
        self.assertEqual(len(recipe["input"]), 5)

    def test_purification_replaces_the_native_copper_clump_routes(self) -> None:
        self.assertIn({"type": "mekanism:purifying", "output": "mekanism:clump_copper"}, self.data["removed"])
        recipe = self.recipes["oxide_from_veridium"]
        self.assertEqual(recipe["type"], "createcraftedbeginning:gas_injection")
        self.assertEqual(recipe["ingredients"], [{"item": "create:veridium", "count": 1}])
        self.assertEqual(recipe["results"], [{"id": "minecraft:raw_copper", "count": 2}])
        self.assertEqual(recipe["gas_ingredients"], [{
            "amount": 1,
            "ingredient": {"gas": "dimensionworks:oxygen"}
        }])


if __name__ == "__main__":
    unittest.main()
