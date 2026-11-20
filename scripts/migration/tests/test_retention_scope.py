from __future__ import annotations

from copy import deepcopy
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from migration.retention_scope import block_action, build_scope, inventory_owner, item_action


class RetentionScopeTest(unittest.TestCase):
    def setUp(self):
        self.source = {
            "format": 1, "read_only": True, "complete": True, "errors": [],
            "registries": {
                "minecraft:item": {"ids": [{"K": id} for id in
                                            ("minecraft:iron_ingot", "sophisticatedbackpacks:backpack",
                                             "farmersdelight:beef_stew", "kaleidoscope_sauce:old_container")]},
                "minecraft:entity_type": {"ids": [{"K": "minecraft:experience_orb"}]},
            },
            "block_counts": {"minecraft:stone": 4096, "create_connected:parallel_gearbox": 1,
                             "sophisticatedstorage:gold_chest": 1, "other:decorative_block": 3},
            "entity_counts": {"minecraft:experience_orb": 1},
            "item_stacks": [
                {"id": "sophisticatedbackpacks:backpack", "count": 1,
                 "location": "playerdata/a.dat/Inventory[0]"},
                {"id": "farmersdelight:beef_stew", "count": 16,
                 "location": "data/sophisticatedbackpacks.dat/data/backpackContents[0]/contents/inventory/Items[0]"},
                {"id": "minecraft:iron_ingot", "count": -128,
                 "location": "region/r.0.0.mca#3/block_entities[2]/storageWrapper/contents/inventory/Items[0]"},
                {"id": "minecraft:experience_orb", "count": 2,
                 "location": "entities/r.0.0.mca#3/Entities[0]"},
            ],
            "declared_dimensions": ["minecraft:overworld"], "registry_references": {},
        }
        self.target = {"minecraft": "1.21.1", "registries": {
            "minecraft:block": ["minecraft:stone", "sophisticatedstorage:gold_chest"],
            "minecraft:item": ["minecraft:iron_ingot", "sophisticatedbackpacks:backpack", "farmersdelight:beef_stew"],
            "minecraft:entity_type": ["minecraft:experience_orb"], "minecraft:dimension": ["minecraft:overworld"],
        }}

    def test_foreign_inventory_contents_and_oversized_stacks_protect_container_records(self):
        original = deepcopy(self.source)
        result = build_scope(self.source, self.target)
        self.assertEqual(result["protected_item_records"], 3)
        self.assertIn("farmersdelight:beef_stew", result["protected_item_ids"])
        self.assertEqual(result["inventory_owner_records"], {
            "playerdata/a.dat": 1, "data/sophisticatedbackpacks.dat": 1,
            "region/r.0.0.mca#3/block_entities[2]": 1,
        })
        self.assertFalse(result["automatic_block_removal_allowed"])
        self.assertFalse(result["save_conversion_verified"])
        self.assertEqual(self.source, original)

    def test_addon_machine_is_required_and_decor_is_only_a_candidate(self):
        result = build_scope(self.source, self.target)
        self.assertEqual(result["required_missing_registries"]["minecraft:block"],
                         [{"id": "create_connected:parallel_gearbox", "count": 1}])
        self.assertEqual(result["other_block_candidates"], {"other:decorative_block": 3})
        self.assertEqual(block_action("other:support", supports_machine=True), "preserve")
        self.assertEqual(block_action("other:container", holds_inventory=True), "preserve")

    def test_discarded_outer_item_does_not_discard_nested_foreign_or_core_contents(self):
        parent = "playerdata/a.dat/Inventory[1]"
        self.source["item_stacks"].extend([
            {"id": "kaleidoscope_sauce:old_container", "count": 1, "location": parent},
            {"id": "farmersdelight:beef_stew", "count": 7, "location": parent + "/tag/Items[0]"},
            {"id": "minecraft:iron_ingot", "count": 32, "location": parent + "/tag/Items[1]"},
        ])
        result = build_scope(self.source, self.target)
        self.assertEqual(result["protected_item_records"], 5)
        self.assertEqual([row["id"] for row in result["discarded_item_records"]],
                         ["kaleidoscope_sauce:old_container"])
        self.assertEqual(item_action("kaleidoscope_sauce:old_container", in_inventory=True),
                         "discard_shell_after_salvage")
        self.assertEqual(block_action("kaleidoscope_sauce:old_container", holds_inventory=True),
                         "requires_asset_recovery")

    def test_unknown_stack_blocks_and_missing_foreign_inventory_item_is_required(self):
        self.source["item_stacks"].append({"id": "unknown:content", "count": 1,
                                           "location": "playerdata/a.dat/Inventory[2]"})
        self.target["registries"]["minecraft:item"].remove("farmersdelight:beef_stew")
        result = build_scope(self.source, self.target)
        self.assertEqual(result["unclassified_count_records"][0]["id"], "unknown:content")
        self.assertEqual(result["required_missing_registries"]["minecraft:item"][0]["id"], "farmersdelight:beef_stew")

    def test_nested_create_contraption_and_unknown_chunk_structure_keep_outer_record(self):
        self.assertEqual(inventory_owner("entities/r.0.0.mca#0/Entities[2]/Contraption/Blocks[4]/Data/Items[0]"),
                         "entities/r.0.0.mca#0/Entities[2]")
        self.assertEqual(inventory_owner("dimensions/other/industry/region/r.0.0.mca#9/opaque/Items[0]"),
                         "dimensions/other/industry/region/r.0.0.mca#9")
        self.assertEqual(item_action("mekanismgenerators:wind_generator", in_inventory=False), "preserve")
        self.assertEqual(item_action("create_other:decor", in_inventory=False), "candidate_for_omission")

    def test_cli_reports_missing_protected_asset_and_refuses_report_overwrite(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            source, target, output = (root / name for name in ("source.json", "target.json", "report.json"))
            source.write_text(json.dumps(self.source))
            target.write_text(json.dumps(self.target))
            command = [sys.executable, str(Path(__file__).resolve().parents[1] / "retention_scope.py"),
                       str(source), str(target), "--output", str(output)]
            result = subprocess.run(command, capture_output=True, text=True)
            self.assertEqual(result.returncode, 1)
            self.assertEqual(output.stat().st_mode & 0o777, 0o600)
            original = output.read_bytes()
            result = subprocess.run(command, capture_output=True, text=True)
            self.assertIn("FileExistsError", result.stderr)
            self.assertEqual(output.read_bytes(), original)


if __name__ == "__main__":
    unittest.main()
