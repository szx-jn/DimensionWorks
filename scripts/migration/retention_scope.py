#!/usr/bin/env python3
"""从既有只读审计列出旧档保留范围；只生成清单，不删除或转换世界。"""
from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import json
import os
from pathlib import Path
import re

if __package__:
    from .preflight_registries import compare_registries
else:
    from preflight_registries import compare_registries


CORE_NAMESPACES = frozenset({"minecraft", "create", "ae2", "mekanism", "mekanismgenerators"})
DISCARDED_NAMESPACES = frozenset({"caves_delight", "mekanismdelight", "kaleidoscope_sauce", "witherstormmod"})

# 唯一旧档中实际使用的附属机械、物流和存储部件，不按模糊名称前缀扩大保留范围。
MACHINE_BLOCKS = frozenset({
    "appliedcreate:kinetic_energy_acceptor", "appliedcreate:me_gearbox",
    "create_connected:brass_chute", "create_connected:encased_chain_cogwheel",
    "create_connected:fan_blasting_catalyst", "create_connected:fan_haunting_catalyst",
    "create_connected:fan_sanding_catalyst", "create_connected:fan_smoking_catalyst",
    "create_connected:fan_splashing_catalyst", "create_connected:inverted_gearshift",
    "create_connected:item_silo", "create_connected:parallel_gearbox", "create_connected:six_way_gearbox",
    "create_dragons_plus:fluid_hatch", "createadditionallogistics:lazy_cogwheel",
    "create_enchantment_industry:blaze_forger", "create_enchantment_industry:grindstone_drain",
    "create_enchantment_industry:mechanical_grindstone", "create_enchantment_industry:printer",
    "create_enchantment_industry:super_experience_block", "create_integrated_farming:chicken_roost",
    "create_power_loader:brass_chunk_loader", "createdieselgenerators:bulk_fermenter",
    "createdieselgenerators:distillation_tank", "createdieselgenerators:large_diesel_engine",
    "createdieselgenerators:oil_barrel", "createdieselgenerators:pumpjack_bearing",
    "createdieselgenerators:pumpjack_crank", "createdieselgenerators:pumpjack_hole",
    "expatternprovider:circuit_cutter", "expatternprovider:ex_charger",
    "expatternprovider:ex_drive", "expatternprovider:ex_inscriber", "laowu:hissing_collector",
    "dimensionworks_mek_stress:memory_drive_ddr1", "dimensionworks_rpm_limit:advanced_speed_controller",
    "dimensionworks_transfer:dispatch_terminal",
    "sophisticatedbackpacks:copper_backpack", "sophisticatedbackpacks:diamond_backpack",
    "sophisticatedbackpacks:gold_backpack", "sophisticatedbackpacks:iron_backpack",
    "sophisticatedbackpacks:netherite_backpack", "sophisticatedstorage:diamond_barrel",
    "sophisticatedstorage:diamond_chest", "sophisticatedstorage:gold_chest",
})

REGISTRY_ID = re.compile(r"[a-z0-9_.-]+:[a-z0-9_./-]+")
REGION_RECORD = re.compile(r"\.mca#\d+(?=/|$)")
OWNER_RECORD = re.compile(r"/(?:block_entities|Entities|entities)\[\d+\]")


def namespace(id: str) -> str:
    if not REGISTRY_ID.fullmatch(id):
        raise ValueError(f"注册名不符：{id}")
    return id.split(":", 1)[0]


def item_action(id: str, *, in_inventory: bool) -> str:
    """舍弃外壳也不能递归舍弃库存；调用方必须逐层处理其子项。"""
    ns = namespace(id)
    if ns in DISCARDED_NAMESPACES:
        return "discard_shell_after_salvage"
    if in_inventory or ns in CORE_NAMESPACES:
        return "preserve"
    return "candidate_for_omission"


def block_action(id: str, *, holds_inventory: bool = False, supports_machine: bool = False) -> str:
    ns = namespace(id)
    if ns in DISCARDED_NAMESPACES:
        if holds_inventory or supports_machine:
            return "requires_asset_recovery"
        return "discard_candidate"
    if ns in CORE_NAMESPACES or id in MACHINE_BLOCKS or holds_inventory or supports_machine:
        return "preserve"
    # 不知道玩家搭建范围，不能根据 namespace 直接把方块变成空气。
    return "candidate_for_omission"


def inventory_owner(location: str) -> str:
    """保守定位外层记录；无已知容器结构时保护整个 chunk / SavedData。"""
    region = REGION_RECORD.search(location)
    if region:
        owner = OWNER_RECORD.search(location, region.end())
        return location[:owner.end() if owner else region.end()]
    match = re.match(r"(.+?\.dat)(?:/|$)", location)
    if match:
        return match[1]
    # FTB 或其他审计项尚无可判定的外层结构，不猜测删除边界。
    return location


def build_scope(source: dict, target: dict) -> dict:
    preflight = compare_registries(source, target)
    source_items = {row["K"] for row in source["registries"]["minecraft:item"]["ids"]}
    records = [row for row in source["item_stacks"] if row["id"] in source_items]
    protected_records, discarded_records = [], []
    owners = Counter()
    for row in records:
        owner = inventory_owner(row["location"])
        # Count 记录含库存、模板和展示栈；精确归类前全保护，不把 Count 合计当成库存量。
        if item_action(row["id"], in_inventory=True) == "preserve":
            owners[owner] += 1
            protected_records.append(row)
        else:
            discarded_records.append(row)
    protected_item_ids = {row["id"] for row in protected_records}
    protected_blocks = {id: count for id, count in source["block_counts"].items()
                        if block_action(id) == "preserve"}
    candidates = {id: count for id, count in source["block_counts"].items() if id not in protected_blocks}
    required_missing = {
        "minecraft:item": [row for row in preflight["domains"]["minecraft:item"]["missing"]
                           if row["id"] in protected_item_ids],
        "minecraft:block": [row for row in preflight["domains"]["minecraft:block"]["missing"]
                            if row["id"] in protected_blocks],
    }
    return {
        "format": 1, "read_only": True, "save_conversion_verified": False,
        "automatic_block_removal_allowed": False,
        "core_namespaces": sorted(CORE_NAMESPACES), "discarded_namespaces": sorted(DISCARDED_NAMESPACES),
        "protected_item_ids": sorted(protected_item_ids), "protected_item_records": len(protected_records),
        "discarded_item_records": discarded_records,
        "inventory_owner_records": dict(sorted(owners.items())),
        "protected_block_counts": dict(sorted(protected_blocks.items())),
        "machine_exception_block_counts": {id: count for id, count in sorted(protected_blocks.items())
                                           if id in MACHINE_BLOCKS},
        "other_block_candidates": dict(sorted(candidates.items())),
        "required_missing_registries": required_missing,
        "unclassified_count_records": preflight["unclassified_count_records"],
        "dimensions_requiring_asset_location_audit": source["declared_dimensions"],
        "notes": [
            "玩家及各类存储的物品整体保留，包含其他模组物品；四项已舍弃模组须逐项回收内部资产后记账。",
            "记录含模板和展示栈，不用记录数或 Count 合计对账真实库存、AE2 Long 储量或物流 Amount。",
            "保护外层容器记录、contentsUuid 与 SavedData，不按外壳 namespace 递归删除内含物。",
            "其它方块仅列候选；未证明不属于容器、机械、支撑和连接之前，禁止自动替换为空气。",
            "维度、区块和身份必须足以承载受保护资产；这个报告不转换世界，也不授权打开旧档。",
        ],
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source_audit", type=Path)
    parser.add_argument("target_registry", type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    source_bytes, target_bytes = args.source_audit.read_bytes(), args.target_registry.read_bytes()
    result = build_scope(json.loads(source_bytes), json.loads(target_bytes))
    result["source_audit_sha256"] = hashlib.sha256(source_bytes).hexdigest()
    result["target_registry_sha256"] = hashlib.sha256(target_bytes).hexdigest()
    with open(args.output, "x", encoding="utf-8", opener=lambda path, flags: os.open(path, flags, 0o600)) as stream:
        json.dump(result, stream, ensure_ascii=False, indent=2)
        stream.write("\n")
    print(json.dumps({"protected_item_ids": len(result["protected_item_ids"]),
                      "protected_item_records": result["protected_item_records"],
                      "inventory_owner_records": len(result["inventory_owner_records"]),
                      "machine_exception_block_ids": len(result["machine_exception_block_counts"]),
                      "required_missing_ids": {key: len(rows) for key, rows in result["required_missing_registries"].items()},
                      "automatic_block_removal_allowed": False, "save_conversion_verified": False}, ensure_ascii=False))
    if result["unclassified_count_records"] or any(result["required_missing_registries"].values()):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
