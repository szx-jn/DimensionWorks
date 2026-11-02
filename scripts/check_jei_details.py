#!/usr/bin/env python3
"""Static metadata and pack fuel-profile consistency checks; never builds or starts Minecraft."""

from __future__ import annotations

import json
import math
from pathlib import Path
import tomllib

ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT / "mods/dimensionworks-jei-ticks"
RESOURCES = PROJECT / "src/main/resources"


def check() -> list[str]:
    problems: list[str] = []
    manifest = json.loads((ROOT / "manifest/mods.json").read_text())
    mods = {mod["id"]: mod for mod in manifest["mods"]}
    entry = mods["dimensionworks_jei_ticks"]
    metadata = tomllib.loads((RESOURCES / "META-INF/mods.toml").read_text())
    declared = metadata["mods"][0]
    if (entry["environment"], entry["project_path"], entry["version"]) != (
        "client", "mods/dimensionworks-jei-ticks", declared["version"]
    ):
        problems.append("JEI details manifest does not match client-only module metadata")
    gradle = (PROJECT / "build.gradle").read_text()
    for dependency in (
        f"version = '{entry['version']}'",
        f"jei-1.20.1-forge:{mods['jei']['version']}",
        f"maven.modrinth:jade:{mods['jade']['version_id']}",
    ):
        if dependency not in gradle:
            problems.append(f"Missing fixed dependency/version: {dependency}")
    mixins = json.loads((RESOURCES / "dimensionworks_jei_ticks.mixins.json").read_text())
    if mixins.get("mixins") or mixins.get("server"):
        problems.append("JEI details mixins must remain client-only")
    package_path = Path(*mixins["package"].split("."))
    for name in mixins["client"]:
        if not (PROJECT / "src/main/java" / package_path / f"{name}.java").is_file():
            problems.append(f"Missing mixin source: {name}")
    lang_root = RESOURCES / "assets/dimensionworks_jei_ticks"
    zh = json.loads((lang_root / "lang/zh_cn.json").read_text())
    en = json.loads((lang_root / "lang/en_us.json").read_text())
    if zh.keys() != en.keys():
        problems.append("Fuel tooltip language key sets differ")
    profiles = json.loads((lang_root / "fuel_profiles.json").read_text())
    for consumer, rows in profiles.items():
        namespace = consumer.split(":", 1)[0]
        if not namespace.startswith("create") and namespace != "draconicevolution":
            problems.append(f"Out-of-scope fuel consumer: {consumer}")
        seen: set[tuple[str, str]] = set()
        for row in rows:
            identity = row["kind"], row["fuel"]
            if identity in seen:
                problems.append(f"Duplicate fuel profile: {consumer} / {identity}")
            seen.add(identity)
            if row["kind"] not in {"item", "fluid"} or ":" not in row["fuel"]:
                problems.append(f"Invalid fuel icon: {identity}")
            if "rate" not in row:
                if not math.isfinite(row["amount"]) or row["amount"] <= 0:
                    problems.append(f"Invalid fuel amount: {identity}")
                if type(row["ticks"]) is not int or row["ticks"] <= 0:
                    problems.append(f"Invalid fuel interval: {identity}")
                if row["unit"] not in {"mB", "B", "个"}:
                    problems.append(f"Invalid fuel unit: {identity}")
    burner = {row["fuel"]: row for row in profiles["create:blaze_burner"]}
    for path in (ROOT / "kubejs/data/dimensionworks/blaze_burner_fuel").glob("*.json"):
        source = json.loads(path.read_text())
        row = burner.get(source["fluid"])
        if row is None or (row["ticks"], row["amount"], row["unit"]) != (
            1, source["amountConsumedPerTick"], "mB"
        ):
            problems.append(f"Liquid burner tooltip differs from pack data: {path.name}")
        elif f"{source['burnTime']}t" not in row["condition"]:
            problems.append(f"Liquid burner heat-cache note differs from pack data: {path.name}")
    return problems


if __name__ == "__main__":
    errors = check()
    for error in errors:
        print(error)
    print(f"JEI/Jade details static checks: {'FAIL' if errors else 'PASS'}")
    raise SystemExit(bool(errors))
