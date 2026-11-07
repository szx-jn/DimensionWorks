from __future__ import annotations

import tomllib
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


class MekanismPipeCurveTest(unittest.TestCase):
    def test_create_speed_matched_pump_rates(self) -> None:
        config = tomllib.loads(
            (ROOT / "config" / "Mekanism" / "tiers.toml").read_text(encoding="utf-8")
        )
        fluid = config["tier"]["transmitters"]["fluid"]

        self.assertEqual(
            [
                fluid["basicPullAmount"],
                fluid["advancedPullAmount"],
                fluid["elitePullAmount"],
                fluid["ultimatePullAmount"],
            ],
            [256, 512, 1024, 2560],
        )
        self.assertEqual(fluid["ultimatePullAmount"], 5120 // 2)


if __name__ == "__main__":
    unittest.main()
