from __future__ import annotations

import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "kubejs/server_scripts/basic_mineral_propagation.js"
STARTUP = ROOT / "kubejs/startup_scripts/mineral_residue.js"
LANG = ROOT / "kubejs/assets/kubejs/lang/zh_cn.json"
TEXTURE = ROOT / "kubejs/assets/kubejs/textures/item/mineral_residue.png"

EXPECTED_ITEMS = (
    "minecraft:raw_iron",
    "minecraft:raw_copper",
    "create:raw_zinc",
    "minecraft:raw_gold",
    "minecraft:coal",
    "minecraft:redstone",
    "minecraft:lapis_lazuli",
    "minecraft:quartz",
    "minecraft:amethyst_shard",
    "mekanism:raw_tin",
    "mekanism:raw_lead",
    "mekanism:raw_osmium",
    "mekanism:fluorite_gem",
)


def load_mineral_lines() -> tuple[str, list[dict[str, object]]]:
    source = SCRIPT.read_text(encoding="utf-8")
    match = re.search(
        r"const BASIC_MINERAL_LINES = (\[.*?^\])\n",
        source,
        flags=re.MULTILINE | re.DOTALL,
    )
    if match is None:
        raise AssertionError("BASIC_MINERAL_LINES JSON-compatible declaration was not found")
    return source, json.loads(match.group(1))


class BasicMineralPropagationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.source, cls.lines = load_mineral_lines()

    def test_target_list_is_complete_and_unique(self) -> None:
        items = tuple(line["item"] for line in self.lines)
        self.assertEqual(items, EXPECTED_ITEMS)
        self.assertEqual(len(items), len(set(items)))
        self.assertEqual(len(self.lines), 13)

    def test_every_target_declares_three_recipe_styles(self) -> None:
        suffixes = (
            "mechanical_growth",
            "mekanism_combining",
            "residue_recovery",
        )
        for line in self.lines:
            with self.subTest(item=line["item"]):
                self.assertRegex(line["path"], r"^[a-z0-9_]+$")
                self.assertTrue(line["compactingInputs"])
                self.assertTrue(line["mekanismCarrier"])
                self.assertGreater(int(line["mekanismCarrierCount"]), 0)
                self.assertGreater(int(line["residueCount"]), 0)
                self.assertTrue(line["recoveryInputs"])
                self.assertGreater(int(line["fluidAmount"]), 0)
                self.assertIn(line["heat"], ("heated", "superheated"))
        for suffix in suffixes:
            self.assertIn("recipePath + '" + suffix + "'", self.source)

    def test_mekanism_combiner_uses_counted_kubejs_integration_inputs(self) -> None:
        self.assertIn("event.recipes.mekanism.combining(", self.source)
        self.assertIn("'2x ' + item", self.source)
        self.assertIn("mineral.mekanismCarrierCount + 'x ' + mineral.mekanismCarrier", self.source)
        self.assertNotIn("event.custom(JsonIO.of({", self.source)
        self.assertIn("BASIC_MINERAL_RECIPES_REGISTERED", self.source)

    def test_high_value_targets_are_not_registered_yet(self) -> None:
        items = {line["item"] for line in self.lines}
        for item in (
            "minecraft:diamond",
            "minecraft:emerald",
            "minecraft:ancient_debris",
            "mekanism:raw_uranium",
        ):
            self.assertNotIn(item, items)


    def test_mekanism_raw_item_ids_are_used_as_targets(self) -> None:
        items = {line["item"] for line in self.lines}
        for item in (
            "mekanism:raw_tin",
            "mekanism:raw_lead",
            "mekanism:raw_osmium",
            "mekanism:raw_uranium",
            "mekanism:fluorite_gem",
        ):
            self.assertIn(item, items)

    def test_residue_has_registration_localization_and_texture(self) -> None:
        startup = STARTUP.read_text(encoding="utf-8")
        lang = json.loads(LANG.read_text(encoding="utf-8"))
        self.assertIn("event.create('mineral_residue')", startup)
        self.assertIn("item.kubejs.mineral_residue", lang)
        self.assertIn("item.kubejs.mineral_residue.tooltip", lang)
        self.assertTrue(TEXTURE.is_file())
        self.assertGreater(TEXTURE.stat().st_size, 0)


if __name__ == "__main__":
    unittest.main()
