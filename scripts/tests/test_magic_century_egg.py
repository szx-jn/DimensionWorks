from __future__ import annotations

import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
RECIPE_DIR = ROOT / "kubejs" / "data" / "dimensionworks" / "recipes" / "magic_century_egg"


class MagicCenturyEggContractTest(unittest.TestCase):
    def test_sequenced_assembly_is_split_into_short_loops(self) -> None:
        first = json.loads((RECIPE_DIR / "first_assembly.json").read_text(encoding="utf-8"))
        final = json.loads((RECIPE_DIR / "final_assembly.json").read_text(encoding="utf-8"))

        self.assertFalse((RECIPE_DIR / "sequenced_assembly.json").exists())
        for recipe in (first, final):
            self.assertEqual(recipe["type"], "create:sequenced_assembly")
            self.assertEqual(recipe["loops"], 8)
            self.assertLessEqual(len(recipe["sequence"]), 7)

        self.assertEqual(first["ingredient"]["item"], "kubejs:century_egg_washed")
        self.assertEqual(first["results"][0]["item"], "kubejs:century_egg_pressed")
        self.assertEqual(final["ingredient"]["item"], "kubejs:century_egg_pressed")
        self.assertEqual(final["results"][0]["item"], "kubejs:magic_century_egg")
        self.assertNotEqual(first["transitionalItem"], final["transitionalItem"])

        pressing_count = sum(
            entry["type"] == "create:pressing"
            for recipe in (first, final)
            for entry in recipe["sequence"]
        )
        self.assertEqual(pressing_count, 10)

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
