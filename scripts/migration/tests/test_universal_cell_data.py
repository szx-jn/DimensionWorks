import unittest
from copy import deepcopy

import nbtlib as n

from scripts.migration.ae_data import convert_universal_cell_data


class UniversalCellDataTests(unittest.TestCase):
    def item(self, source):
        result = n.Compound({"id": deepcopy(source["id"]), "count": n.Int(1)})
        tag = source.get("tag", n.Compound())
        if tag:
            result["components"] = n.Compound({"minecraft:custom_data": deepcopy(tag)})
        return result

    def test_equivalent_old_keys_merge_long_quantities_and_record_each_source(self):
        source = n.parse_nbt('{inventory:{entries:[{key:{"#c":"ae2:i",id:"minecraft:birch_planks"},amount:188L},{key:{"#c":"ae2:i",id:"minecraft:birch_planks",tag:{}},amount:48L}],error_entries:[]}}')
        original = deepcopy(source)
        result, ledger = convert_universal_cell_data(source, self.item)
        self.assertEqual(result["inventory"]["entries"], n.parse_nbt('[{key:{"#t":"ae2:i",id:"minecraft:birch_planks"},amount:236L}]'))
        self.assertEqual(ledger[0]["source_index"], 1)
        self.assertEqual(ledger[0]["previous_amount"], n.Long(188))
        self.assertEqual(source, original)

    def test_distinct_complete_item_states_keep_separate_keys(self):
        source = n.parse_nbt('{inventory:{entries:[{key:{"#c":"ae2:i",id:"minecraft:stone",tag:{owner:"one"}},amount:12L},{key:{"#c":"ae2:i",id:"minecraft:stone",tag:{owner:"two"}},amount:8L}],error_entries:[]}}')
        result, ledger = convert_universal_cell_data(source, self.item)
        self.assertEqual(len(result["inventory"]["entries"]), 2)
        self.assertEqual(ledger, [])

    def test_read_errors_bad_quantity_and_long_overflow_stop_instead_of_skipping(self):
        source = n.parse_nbt('{inventory:{entries:[],error_entries:[{key:"unreadable"}]}}')
        with self.assertRaisesRegex(ValueError, "原有读取错误"):
            convert_universal_cell_data(source, self.item)
        source = n.parse_nbt('{inventory:{entries:[{key:{"#c":"ae2:i",id:"minecraft:stone"},amount:1}],error_entries:[]}}')
        with self.assertRaisesRegex(ValueError, "Long"):
            convert_universal_cell_data(source, self.item)
        source = n.parse_nbt('{inventory:{entries:[{key:{"#c":"ae2:i",id:"minecraft:stone"},amount:9223372036854775807L},{key:{"#c":"ae2:i",id:"minecraft:stone"},amount:1L}],error_entries:[]}}')
        with self.assertRaisesRegex(ValueError, "超出 Long"):
            convert_universal_cell_data(source, self.item)


if __name__ == "__main__":
    unittest.main()
