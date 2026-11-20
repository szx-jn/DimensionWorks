from __future__ import annotations

from copy import deepcopy
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from migration.preflight_registries import compare_registries


class RegistryPreflightTest(unittest.TestCase):
    def setUp(self):
        self.source = {
            "format": 1, "read_only": True, "complete": True, "errors": [],
            "registries": {
                "minecraft:item": {"ids": [{"K": "minecraft:diamond"}]},
                "minecraft:entity_type": {"ids": [{"K": "minecraft:experience_orb"}, {"K": "minecraft:item"}]},
            },
            "block_counts": {"minecraft:stone": 4096},
            "entity_counts": {"minecraft:experience_orb": 1},
            "item_stacks": [{"id": "minecraft:diamond", "count": 48,
                             "location": "entities/r.0.0.mca#0/Entities[1]/Item"}],
            "declared_dimensions": ["minecraft:overworld"],
            "registry_references": {"unknown:chemical": 3},
        }
        self.target = {"minecraft": "1.21.1", "registries": {
            "minecraft:block": ["minecraft:stone"], "minecraft:item": ["minecraft:diamond"],
            "minecraft:entity_type": ["minecraft:experience_orb", "minecraft:item"],
            "minecraft:dimension": ["minecraft:overworld"],
        }}

    def test_matching_ids_do_not_claim_save_conversion_or_typed_unknown_references(self):
        result = compare_registries(self.source, self.target)
        self.assertTrue(result["registry_ids_present"])
        self.assertFalse(result["save_conversion_verified"])
        self.assertEqual(result["untyped_registry_references"], {"unknown:chemical": 3})

    def test_experience_orb_count_is_not_an_item_but_nested_item_remains(self):
        self.source["item_stacks"].append({"id": "minecraft:experience_orb", "count": 2,
                                           "location": "DIM-1/entities/r.0.0.mca#0/Entities[0]"})
        self.target["registries"]["minecraft:item"] = ["minecraft:stone"]
        result = compare_registries(self.source, self.target)
        self.assertEqual(result["entity_count_records"], {"minecraft:experience_orb": 1})
        missing = result["domains"]["minecraft:item"]["missing"]
        self.assertEqual([row["id"] for row in missing], ["minecraft:diamond"])
        self.assertEqual(missing[0]["stored_count_sum"], 48)
        self.assertEqual(missing[0]["locations"], ["entities/r.0.0.mca#0/Entities[1]/Item"])

    def test_unknown_count_record_blocks_instead_of_being_deleted_or_ignored(self):
        self.source["item_stacks"].append({"id": "unknown:device", "count": 1, "location": "playerdata/a.dat/Inventory[0]"})
        result = compare_registries(self.source, self.target)
        self.assertFalse(result["registry_ids_present"])
        self.assertEqual(result["unclassified_count_records"][0]["id"], "unknown:device")

    def test_same_name_in_a_different_registry_does_not_satisfy_missing_item(self):
        self.target["registries"]["minecraft:item"] = ["minecraft:stone"]
        self.target["registries"]["minecraft:block"].append("minecraft:diamond")
        result = compare_registries(self.source, self.target)
        self.assertFalse(result["registry_ids_present"])
        self.assertEqual(result["domains"]["minecraft:item"]["missing"][0]["observed_records"], 1)

    def test_missing_dimension_and_blocks_are_reported_without_removal(self):
        self.source["block_counts"]["old:machine"] = 2
        self.source["declared_dimensions"].append("old:dimension")
        original = deepcopy(self.source)
        result = compare_registries(self.source, self.target)
        self.assertEqual(result["domains"]["minecraft:block"]["missing"], [{"id": "old:machine", "count": 2}])
        self.assertEqual(result["domains"]["minecraft:dimension"]["missing"], [{"id": "old:dimension", "count": 1}])
        self.assertEqual(self.source, original)

    def test_rejects_incomplete_audit_and_missing_target_registry(self):
        self.source["errors"] = [{"location": "r.0.0.mca", "error": "truncated"}]
        with self.assertRaisesRegex(ValueError, "未完整"):
            compare_registries(self.source, self.target)
        self.source["errors"] = []
        self.target["registries"]["minecraft:item"] = []
        with self.assertRaisesRegex(ValueError, "完整注册表"):
            compare_registries(self.source, self.target)


if __name__ == "__main__":
    unittest.main()
