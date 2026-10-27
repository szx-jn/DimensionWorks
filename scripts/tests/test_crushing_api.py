from __future__ import annotations

import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
API = ROOT / "kubejs/server_scripts/crushing_api.js"
MINERAL = ROOT / "kubejs/server_scripts/basic_mineral_propagation.js"
CAVE = ROOT / "kubejs/server_scripts/cave_self_propagation.js"
ITEMS = ROOT / "kubejs/startup_scripts/mineral_processing_items.js"
AUDIT = ROOT / "docs/design/crushing-api-mod-audit.md"


class CrushingApiTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.api = API.read_text(encoding="utf-8")
        cls.mineral = MINERAL.read_text(encoding="utf-8")
        cls.cave = CAVE.read_text(encoding="utf-8")
        cls.items = ITEMS.read_text(encoding="utf-8")
        cls.audit = AUDIT.read_text(encoding="utf-8")

    def test_create_and_mekanism_adapters_share_one_api(self) -> None:
        self.assertIn("adapters['create:crushing']", self.api)
        self.assertIn("adapters['create:milling']", self.api)
        self.assertIn("adapters['mekanism:crushing']", self.api)
        self.assertIn("global.DimensionWorksGrinding = api", self.api)
        self.assertIn("global.DimensionWorksCrushing = api", self.api)
        self.assertIn("registerAdapter: registerAdapter", self.api)

    def test_create_supports_independent_chance_outputs(self) -> None:
        self.assertIn("stack.withChance(output.chance)", self.api)
        self.assertIn("event.recipes.create.crushing(outputs, recipe.input)", self.api)
        self.assertIn("event.recipes.create.milling(outputs, recipe.input)", self.api)

    def test_mekanism_rejects_unsupported_chance_outputs(self) -> None:
        self.assertIn("only supports one guaranteed output", self.api)
        self.assertIn("event.recipes.mekanism.crushing(", self.api)

    def test_create_enchantment_industry_has_a_fluid_aware_grinding_adapter(self) -> None:
        self.assertIn("adapters['create_enchantment_industry:grinding']", self.api)
        self.assertIn("type: 'create_enchantment_industry:grinding'", self.api)
        self.assertIn("recipe.fluidInput", self.api)
        self.assertIn("recipe.fluidOutput", self.api)
        self.assertIn("up to four item outputs", self.api)
        self.assertIn("not both", self.api)
        self.assertIn("amount: recipe.fluidInput.amount", self.api)
        self.assertNotIn("forge:fluid_stack", self.api)

    def test_iron_yield_is_two_plus_seventy_percent_bonus(self) -> None:
        self.assertIn("machine: 'create:milling'", self.mineral)
        self.assertIn("{ item: 'mekanism:dust_iron', count: 2 }", self.mineral)
        self.assertIn("{ item: 'mekanism:dust_iron', count: 1, chance: 0.7 }", self.mineral)
        self.assertIn("'kubejs:iron_concentrate'", self.mineral)
        self.assertIn("'mekanism:dust_iron'", self.mineral)

    def test_existing_create_crushing_and_milling_calls_use_the_shared_api(self) -> None:
        self.assertIn("machine: 'create:crushing'", self.cave)
        self.assertIn("machine: 'create:milling'", self.cave)
        self.assertIn("global.DimensionWorksGrinding.register(event", self.cave)
        self.assertNotIn("namespace.crushing(", self.cave)
        self.assertNotIn("namespace.milling(", self.cave)

        for path in (ROOT / "kubejs/server_scripts").glob("*.js"):
            if path.name == API.name:
                continue
            source = path.read_text(encoding="utf-8")
            self.assertNotIn("event.recipes.create.crushing(", source, path.name)
            self.assertNotIn("event.recipes.create.milling(", source, path.name)

    def test_intermediate_item_is_registered_and_localized(self) -> None:
        lang = json.loads(
            (ROOT / "kubejs/assets/kubejs/lang/zh_cn.json").read_text(encoding="utf-8")
        )
        self.assertIn("event.create('iron_concentrate')", self.items)
        self.assertIn(".texture('minecraft:item/raw_iron')", self.items)
        self.assertEqual(lang["item.kubejs.iron_concentrate"], "铁质活化料")

    def test_semantic_mod_audit_covers_related_addons(self) -> None:
        for mod in (
            "create",
            "kubejs_create",
            "mekanism",
            "kubejs_mekanism",
            "create_enchantment_industry",
            "ae2",
            "laowu",
            "create_dragons_plus",
            "sliceanddice",
            "dimensionworks_cave_factory",
        ):
            self.assertIn(mod, self.audit)
        self.assertIn("135 个模组条目", self.audit)
        self.assertIn("其余 100 个模组条目", self.audit)


if __name__ == "__main__":
    unittest.main()
