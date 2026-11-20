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
import sys

if __package__:
    from .preflight_registries import compare_registries
    from .registry_ids import ITEM_IDS, BLOCK_IDS
else:
    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
    from preflight_registries import compare_registries
    from migration.registry_ids import ITEM_IDS, BLOCK_IDS


CORE_NAMESPACES = frozenset({"minecraft", "create", "ae2", "mekanism", "mekanismgenerators"})
DISCARDED_NAMESPACES = frozenset({"caves_delight", "mekanismdelight", "kaleidoscope_sauce", "witherstormmod"})
RETENTION_POLICY = "industrial_storage_players_v1"

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
    "extendedae:circuit_cutter", "extendedae:ex_charger", "extendedae:ex_drive", "extendedae:ex_inscriber",
    "dimensionworks_mek_stress:memory_drive_ddr1", "dimensionworks_rpm_limit:advanced_speed_controller",
    "dimensionworks_transfer:dispatch_terminal",
    "sophisticatedbackpacks:copper_backpack", "sophisticatedbackpacks:diamond_backpack",
    "sophisticatedbackpacks:gold_backpack", "sophisticatedbackpacks:iron_backpack",
    "sophisticatedbackpacks:netherite_backpack", "sophisticatedstorage:diamond_barrel",
    "sophisticatedstorage:diamond_chest", "sophisticatedstorage:gold_chest",
})

# 实际放置的装置、包裹和运输矿车属于工业载荷。其它世界实体不再适配。
INDUSTRIAL_ENTITIES = frozenset({
    "create:package", "create:seat", "create:stationary_contraption", "create:super_glue",
    "create:contraption", "create:carriage_contraption", "create:gantry_contraption",
    "minecraft:minecart", "minecraft:chest_minecart", "minecraft:hopper_minecart",
    "minecraft:furnace_minecart", "minecraft:tnt_minecart", "minecraft:command_block_minecart",
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
    return "discard_after_archive"


def block_action(id: str, *, holds_inventory: bool = False, supports_machine: bool = False) -> str:
    ns = namespace(id)
    if ns in DISCARDED_NAMESPACES:
        if holds_inventory or supports_machine:
            return "requires_asset_recovery"
        return "discard_candidate"
    if ns in CORE_NAMESPACES or id in MACHINE_BLOCKS or supports_machine:
        return "preserve"
    # 最新授权明确舍弃范围外内容；非工业容器有库存不再自动扩大保留范围。
    return "discard_after_archive"


def entity_action(id: str, *, carries_machine: bool = False) -> str:
    namespace(id)
    return "preserve" if id in INDUSTRIAL_ENTITIES or carries_machine else "discard_after_archive"


def inventory_owner(location: str) -> str:
    """保守定位外层记录；无已知容器结构时保护整个 chunk / SavedData。"""
    region = REGION_RECORD.search(location)
    if region:
        owner = OWNER_RECORD.search(location, region.end())
        return location[:owner.end() if owner else region.end()]
    match = re.match(r"(.+?\.(?:dat|nbt))(?:/|$)", location)
    if match:
        return match[1]
    # FTB 或其他审计项尚无可判定的外层结构，不猜测删除边界。
    return location


def build_scope(source: dict, target: dict, *, protected_owners: set[str] | None = None,
                verified_item_mappings: list[dict] | None = None) -> dict:
    filtered = dict(source)
    filtered["item_stacks"] = [row for row in source["item_stacks"]
                              if protected_owners is None or inventory_owner(row["location"]) in protected_owners]
    preflight = compare_registries(filtered, target)
    verified = {}
    for row in verified_item_mappings or []:
        if row.get("source") != "expatternprovider:infinity_cell" or row.get("target") != "extendedae:infinity_cobblestone_cell" or not re.fullmatch(r"[0-9a-f]{64}", row.get("source_snbt_sha256", "")):
            raise ValueError("条件映射不是已核对的无限圆石元件")
        key = (row["source"], row["source_snbt_sha256"])
        if key in verified:
            raise ValueError("条件物品映射重复")
        verified[key] = row["target"]
    source_items = {row["K"] for row in source["registries"]["minecraft:item"]["ids"]}
    records = [row for row in source["item_stacks"] if row["id"] in source_items]
    protected_records, discarded_records, omitted_records = [], [], []
    owners = Counter()
    for row in records:
        owner = inventory_owner(row["location"])
        if protected_owners is not None and owner not in protected_owners:
            omitted_records.append(row)
            continue
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
    required_missing = {"minecraft:item": [], "minecraft:block": []}
    resolved = {"minecraft:item": [], "minecraft:block": []}
    for domain, ids, aliases in (("minecraft:item", protected_item_ids, ITEM_IDS),
                                ("minecraft:block", protected_blocks, BLOCK_IDS)):
        native = set(target["registries"][domain])
        for row in preflight["domains"][domain]["missing"]:
            old = row["id"]
            if old not in ids:
                continue
            id = aliases.get(old)
            if domain == "minecraft:item" and id is None:
                affected = [stack for stack in protected_records if stack["id"] == old]
                destinations = {verified.get((old, stack.get("snbt_sha256", ""))) for stack in affected}
                if len(destinations) == 1 and None not in destinations:
                    id = destinations.pop()
            if id in native:
                resolved[domain].append({"source": old, "target": id})
            else:
                required_missing[domain].append(row)
    return {
        "format": 1, "read_only": True, "save_conversion_verified": False,
        "retention_policy": RETENTION_POLICY,
        "inventory_owner_scope_verified": protected_owners is not None,
        "automatic_block_removal_allowed": False,
        "core_namespaces": sorted(CORE_NAMESPACES), "discarded_namespaces": sorted(DISCARDED_NAMESPACES),
        "protected_item_ids": sorted(protected_item_ids), "protected_item_records": len(protected_records),
        "discarded_item_records": discarded_records,
        "omitted_nonretained_owner_records": omitted_records,
        "inventory_owner_records": dict(sorted(owners.items())),
        "protected_block_counts": dict(sorted(protected_blocks.items())),
        "machine_exception_block_counts": {id: count for id, count in sorted(protected_blocks.items())
                                           if id in MACHINE_BLOCKS},
        "other_block_candidates": dict(sorted(candidates.items())),
        "required_missing_registries": required_missing,
        "resolved_registry_ids": resolved,
        "unclassified_count_records": [row for row in preflight["unclassified_count_records"]
                                       if protected_owners is None or inventory_owner(row["location"]) in protected_owners],
        "dimensions_requiring_asset_location_audit": source["declared_dimensions"],
        "notes": [
            "保留原版与工业方块、背包/工业存储/AE2附属存储及玩家库存；范围外内容按用户最新授权归档并舍弃。",
            "玩家及保留容器内部物品不按命名空间删除；四项已舍弃模组须逐项回收受保护内部资产后记账。",
            "记录含模板和展示栈，不用记录数或 Count 合计对账真实库存、AE2 Long 储量或物流 Amount。",
            "保护外层容器记录、contentsUuid 与 SavedData，不按外壳 namespace 递归删除内含物。",
            "清理只能在副本中按完整位置/方块状态台账执行；工业连接和移动机械载荷须先闭合保留。",
            "未提供受保护外层记录清单时物品仍保守保护；这个报告不能代替逐方块/实体执行台账。",
            "维度、区块和身份必须足以承载受保护资产；这个报告不转换世界，也不授权打开旧档。",
        ],
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source_audit", type=Path)
    parser.add_argument("target_registry", type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--protected-owners", type=Path,
                        help="已按实际方块状态、工业实体和玩家文件核对的完整外层记录 JSON 清单")
    args = parser.parse_args()
    source_bytes, target_bytes = args.source_audit.read_bytes(), args.target_registry.read_bytes()
    owners = None
    if args.protected_owners:
        owner_bytes = args.protected_owners.read_bytes()
        owner_report = json.loads(owner_bytes)
        if owner_report.get("format") != 1 or owner_report.get("retention_policy") != RETENTION_POLICY or owner_report.get("complete") is not True:
            raise ValueError("受保护外层记录清单未完成或保留策略不符")
        owner_rows = owner_report["owners"]
        if not isinstance(owner_rows, list) or any(not isinstance(row, str) or not row for row in owner_rows) or len(set(owner_rows)) != len(owner_rows):
            raise ValueError("受保护外层记录清单格式不符或重复")
        if owner_report.get("source_audit_sha256") != hashlib.sha256(source_bytes).hexdigest():
            raise ValueError("受保护外层记录清单与本次源审计不一致")
        owners = set(owner_rows)
    result = build_scope(json.loads(source_bytes), json.loads(target_bytes), protected_owners=owners,
                         verified_item_mappings=owner_report.get("verified_item_mappings", []) if owners is not None else None)
    if owners is not None:
        result["protected_owners_sha256"] = hashlib.sha256(owner_bytes).hexdigest()
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
