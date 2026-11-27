from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts"))

import check_architecture  # noqa: E402


class ArchitectureGraphTest(unittest.TestCase):
    def test_parses_gradle_project_dependencies(self) -> None:
        source = """
        dependencies {
            implementation project(':dimensionworks-rpm-limit')
            testImplementation project(path: ':dimensionworks-cave-factory')
        }
        """
        self.assertEqual(
            check_architecture.gradle_project_dependencies(source),
            {"dimensionworks-rpm-limit", "dimensionworks-cave-factory"},
        )

    def test_detects_cycles_in_combined_dependency_graph(self) -> None:
        graph = {
            "dimensionworks-a": {"dimensionworks-b"},
            "dimensionworks-b": {"dimensionworks-a"},
            "dimensionworks-c": set(),
        }
        self.assertEqual(
            check_architecture.strongly_connected_components(graph),
            [["dimensionworks-a", "dimensionworks-b"]],
        )


if __name__ == "__main__":
    unittest.main()
