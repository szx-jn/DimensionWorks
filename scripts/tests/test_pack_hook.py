import unittest

from scripts.pack_hook import has_dependency_cycle


class QuestDependencyCycleTest(unittest.TestCase):
    def test_accepts_acyclic_graph(self) -> None:
        graph = {
            "root": {"middle"},
            "middle": {"leaf"},
            "leaf": set(),
            "independent": set(),
        }
        self.assertFalse(has_dependency_cycle(graph))

    def test_detects_multi_node_cycle(self) -> None:
        graph = {
            "a": {"b"},
            "b": {"c"},
            "c": {"a"},
        }
        self.assertTrue(has_dependency_cycle(graph))

    def test_detects_self_cycle(self) -> None:
        self.assertTrue(has_dependency_cycle({"a": {"a"}}))

    def test_ignores_missing_nodes_for_cycle_detection(self) -> None:
        self.assertFalse(has_dependency_cycle({"a": {"missing"}}))


if __name__ == "__main__":
    unittest.main()
