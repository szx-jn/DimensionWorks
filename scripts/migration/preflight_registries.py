#!/usr/bin/env python3
"""只读对照旧档审计与目标注册表，不转换、删除或打开存档。"""
from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import json
import os
from pathlib import Path
import re


ENTITY_RECORD = re.compile(r"/(?:Entities|entities)\[\d+\]$")
DOMAINS = ("minecraft:block", "minecraft:item", "minecraft:entity_type", "minecraft:dimension")


def compare_registries(source: dict, target: dict) -> dict:
    if source["format"] != 1 or not source["read_only"] or not source["complete"] or source["errors"]:
        raise ValueError("源审计未完整通过，不能用于迁移预检")
    if target["minecraft"] != "1.21.1":
        raise ValueError("目标注册表不是 Minecraft 1.21.1")
    registries = target["registries"]
    for domain in DOMAINS:
        if not isinstance(registries[domain], list) or not registries[domain]:
            raise ValueError(f"目标缺少完整注册表：{domain}")

    old_items = {row["K"] for row in source["registries"]["minecraft:item"]["ids"]}
    old_entities = {row["K"] for row in source["registries"]["minecraft:entity_type"]["ids"]}
    items = Counter()
    count_fields = Counter()
    locations = {}
    entity_count_records = Counter()
    unclassified_records = []
    for stack in source["item_stacks"]:
        item = stack["id"]
        if item not in old_items:
            # 旧审计也收集带 Count 的实体根记录；经验球 Count 并非物品数量。
            if item in old_entities and ENTITY_RECORD.search(stack["location"]):
                entity_count_records[item] += 1
            else:
                unclassified_records.append(stack)
            continue
        items[item] += 1
        count_fields[item] += stack["count"]
        locations.setdefault(item, []).append(stack["location"])

    domains = {}
    for domain, counts in (("minecraft:block", source["block_counts"]),
                           ("minecraft:entity_type", source["entity_counts"]),
                           ("minecraft:item", items),
                           ("minecraft:dimension", Counter(source["declared_dimensions"]))):
        present = set(registries[domain])
        missing = []
        for id, count in sorted(counts.items()):
            if id in present:
                continue
            row = {"id": id, "observed_records" if domain == "minecraft:item" else "count": count}
            if domain == "minecraft:item":
                row["stored_count_sum"] = count_fields[id]
                row["locations"] = locations[id]
            missing.append(row)
        domains[domain] = {"observed_ids": len(counts),
                           "present_ids": len(counts) - len(missing), "missing": missing}

    return {"format": 1, "read_only": True,
            "registry_ids_present": not unclassified_records and all(not row["missing"] for row in domains.values()),
            "save_conversion_verified": False,
            "domains": domains, "entity_count_records": dict(entity_count_records),
            "unclassified_count_records": unclassified_records,
            "untyped_registry_references": source["registry_references"],
            "notes": ["注册名存在不证明方块状态、库存组件、机器 NBT 或存档格式兼容。",
                      "物品 observed_records 是含 Count 字段的记录数；stored_count_sum 包含模板，不代替物流 Amount 或 AE2/Mek 存储量。",
                      "未定类型的注册字符串另列，不能靠在任一注册表中同名就判定保留。"]}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source_audit", type=Path)
    parser.add_argument("target_registry", type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    source_bytes = args.source_audit.read_bytes()
    target_bytes = args.target_registry.read_bytes()
    report = compare_registries(json.loads(source_bytes), json.loads(target_bytes))
    report["source_audit_sha256"] = hashlib.sha256(source_bytes).hexdigest()
    report["target_registry_sha256"] = hashlib.sha256(target_bytes).hexdigest()
    # 不接受覆盖现有报告或源文件；报告可能包含玩家库存位置，创建时即为私有权限。
    with open(args.output, "x", encoding="utf-8", opener=lambda path, flags: os.open(path, flags, 0o600)) as stream:
        json.dump(report, stream, ensure_ascii=False, indent=2)
        stream.write("\n")
    print(json.dumps({"registry_ids_present": report["registry_ids_present"],
                      "missing_ids": {key: len(row["missing"]) for key, row in report["domains"].items()},
                      "unclassified_count_records": len(report["unclassified_count_records"]),
                      "save_conversion_verified": False}, ensure_ascii=False))
    if not report["registry_ids_present"]:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
