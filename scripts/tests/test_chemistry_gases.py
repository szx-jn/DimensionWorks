from __future__ import annotations

import json
import shutil
import subprocess
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


@unittest.skipUnless(shutil.which("node"), "Node.js is required for KubeJS registration checks")
class ChemistryGasesTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        harness = r"""
const fs = require('fs'), vm = require('vm');
const registries = [], recipes = [];
const context = vm.createContext({
  StartupEvents: {
    registry(name, callback) {
      const definitions = [];
      callback({
        create(id) {
          const definition = {id};
          definitions.push(definition);
          const builder = {
            tint(value) { definition.tint = value; return builder; },
            alpha(value) { definition.alpha = value; return builder; },
            usableInEquipment(value) { definition.usableInEquipment = value; return builder; },
            gaseous() { definition.gaseous = true; return builder; }
          };
          return builder;
        }
      });
      registries.push({name, definitions});
    }
  },
  ServerEvents: {
    recipes(callback) {
      callback({
        custom(recipe) {
          recipes.push(recipe);
          const builder = {id(id) { recipe.id = id; return builder; }};
          return builder;
        }
      });
    }
  }
});
vm.runInContext(fs.readFileSync('kubejs/startup_scripts/chemistry_gases.js', 'utf8'), context);
vm.runInContext(fs.readFileSync('kubejs/server_scripts/chemistry_gas_recipes.js', 'utf8'), context);
process.stdout.write(JSON.stringify({registries, recipes}));
"""
        result = subprocess.run(
            ["node", "-e", harness],
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=True,
        )
        payload = json.loads(result.stdout)
        cls.registries = {registry["name"]: registry["definitions"] for registry in payload["registries"]}
        cls.recipes = {recipe["id"]: recipe for recipe in payload["recipes"]}

    def test_chemistry_uses_ccb_registry_and_retains_legacy_mek_ids(self) -> None:
        ccb = self.registries["createcraftedbeginning:gas"]
        legacy = self.registries["mekanism:chemical"]
        self.assertEqual(len(ccb), 14)
        self.assertEqual(len(legacy), 12)
        self.assertTrue(all(definition["id"].startswith("dimensionworks:") for definition in ccb))
        self.assertTrue(all(
            isinstance(definition["tint"], int)
            and definition["alpha"] == 255
            and definition["usableInEquipment"] is True
            for definition in ccb
        ))
        self.assertEqual(
            {definition["id"] for definition in legacy},
            {definition["id"] for definition in ccb if definition["id"] not in {
                "dimensionworks:hydrogen", "dimensionworks:oxygen"
            }},
        )

    def test_all_custom_chemistry_recipe_gases_resolve_to_ccb_definitions(self) -> None:
        registered = {definition["id"] for definition in self.registries["createcraftedbeginning:gas"]}
        references = set()
        for recipe in self.recipes.values():
            self.assertEqual(recipe["type"], "createcraftedbeginning:reactor_kettle")
            self.assertIn("ingredients", recipe)
            for entry in recipe.get("gas_ingredients", []):
                references.add(entry["ingredient"]["gas"])
            references.update(entry["id"] for entry in recipe.get("gas_results", []))
        self.assertTrue(references)
        self.assertTrue(references.issubset(registered))
        self.assertFalse(any(gas_id.startswith("mekanism:") for gas_id in references))
        self.assertEqual(len(self.recipes), 13)

    def test_existing_chemistry_recipe_ids_and_io_are_preserved(self) -> None:
        item = lambda name: {"item": name, "count": 1}
        gas_in = lambda name, amount: {
            "amount": amount,
            "ingredient": {"gas": name},
        }
        gas_out = lambda name, amount: {"id": name, "amount": amount}
        fluid = lambda name, amount: {
            "type": "neoforge:single",
            "fluid": name,
            "amount": amount,
        }
        expected = {
            "dimensionworks:chemistry/create/carbon_monoxide_from_charcoal": {
                "ingredients": [item("minecraft:charcoal")],
                "gas_results": [gas_out("dimensionworks:carbon_monoxide", 125)],
            },
            "dimensionworks:chemistry/create/carbon_dioxide_from_charcoal": {
                "ingredients": [item("minecraft:charcoal")],
                "gas_results": [gas_out("dimensionworks:carbon_dioxide", 250)],
            },
            "dimensionworks:chemistry/nitrogen_from_gunpowder": {
                "ingredients": [item("minecraft:gunpowder")],
                "gas_results": [gas_out("dimensionworks:nitrogen", 1000)],
            },
            "dimensionworks:chemistry/ammonia_haber_process": {
                "gas_ingredients": [
                    gas_in("dimensionworks:nitrogen", 1000),
                    gas_in("dimensionworks:hydrogen", 3000),
                ],
                "gas_results": [gas_out("dimensionworks:ammonia", 2000)],
            },
            "dimensionworks:chemistry/nitric_oxide_from_nitrogen": {
                "gas_ingredients": [
                    gas_in("dimensionworks:nitrogen", 1000),
                    gas_in("dimensionworks:oxygen", 1000),
                ],
                "gas_results": [gas_out("dimensionworks:nitric_oxide", 2000)],
            },
            "dimensionworks:chemistry/nitrogen_dioxide_from_nitric_oxide": {
                "gas_ingredients": [
                    gas_in("dimensionworks:nitric_oxide", 1000),
                    gas_in("dimensionworks:oxygen", 500),
                ],
                "gas_results": [gas_out("dimensionworks:nitrogen_dioxide", 1000)],
            },
            "dimensionworks:chemistry/nitrous_oxide_from_ammonia": {
                "gas_ingredients": [
                    gas_in("dimensionworks:ammonia", 1000),
                    gas_in("dimensionworks:oxygen", 1000),
                ],
                "gas_results": [gas_out("dimensionworks:nitrous_oxide", 500)],
            },
            "dimensionworks:chemistry/hydrogen_sulfide_prc": {
                "ingredients": [
                    {"tag": "c:dusts/sulfur", "count": 1},
                    fluid("minecraft:water", 1000),
                ],
                "gas_ingredients": [gas_in("dimensionworks:hydrogen", 1000)],
                "gas_results": [gas_out("dimensionworks:hydrogen_sulfide", 1000)],
                "processing_time": 100,
            },
            "dimensionworks:chemistry/methane_prc": {
                "ingredients": [
                    item("minecraft:coal"),
                    fluid("minecraft:water", 1000),
                ],
                "gas_ingredients": [gas_in("dimensionworks:hydrogen", 2000)],
                "gas_results": [gas_out("dimensionworks:methane", 1000)],
                "processing_time": 160,
            },
            "dimensionworks:chemistry/ethyne_prc": {
                "ingredients": [
                    item("minecraft:charcoal"),
                    fluid("minecraft:water", 1000),
                ],
                "gas_ingredients": [gas_in("dimensionworks:hydrogen", 1000)],
                "gas_results": [gas_out("dimensionworks:ethyne", 500)],
                "processing_time": 160,
            },
            "dimensionworks:chemistry/ozone_activation": {
                "gas_ingredients": [gas_in("dimensionworks:oxygen", 1000)],
                "gas_results": [gas_out("dimensionworks:ozone", 500)],
            },
            "dimensionworks:chemistry/nitric_acid_prc": {
                "ingredients": [
                    item("minecraft:iron_ingot"),
                    fluid("minecraft:water", 1000),
                ],
                "gas_ingredients": [gas_in("dimensionworks:nitrogen_dioxide", 1000)],
                "gas_results": [gas_out("dimensionworks:nitric_acid", 1000)],
                "processing_time": 200,
            },
        }
        self.assertTrue(set(expected).issubset(self.recipes))
        for recipe_id, io in expected.items():
            with self.subTest(recipe_id=recipe_id):
                recipe = self.recipes[recipe_id]
                for field, value in io.items():
                    self.assertEqual(recipe[field], value)

    def test_co_paths_keep_distinct_exact_temperature_requirements(self) -> None:
        carbon_monoxide = self.recipes["dimensionworks:chemistry/create/carbon_monoxide_from_charcoal"]
        carbon_dioxide = self.recipes["dimensionworks:chemistry/create/carbon_dioxide_from_charcoal"]
        self.assertEqual((carbon_monoxide["temperature"], carbon_monoxide["temperature_matching"]), ("heated", "exact"))
        self.assertEqual((carbon_dioxide["temperature"], carbon_dioxide["temperature_matching"]), ("superheated", "exact"))
        self.assertEqual(carbon_monoxide["gas_results"][0]["amount"], 125)
        self.assertEqual(carbon_dioxide["gas_results"][0]["amount"], 250)

    def test_new_water_electrolysis_supplies_ccb_hydrogen_and_oxygen(self) -> None:
        recipe = self.recipes["dimensionworks:chemistry/hydrogen_oxygen_from_water"]
        self.assertEqual(recipe["ingredients"], [{
            "type": "neoforge:single",
            "fluid": "minecraft:water",
            "amount": 1000,
        }])
        self.assertEqual(recipe["gas_results"], [
            {"id": "dimensionworks:hydrogen", "amount": 2000},
            {"id": "dimensionworks:oxygen", "amount": 1000},
        ])
        self.assertEqual(recipe["processing_time"], 100)


if __name__ == "__main__":
    unittest.main()
