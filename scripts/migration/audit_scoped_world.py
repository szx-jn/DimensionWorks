#!/usr/bin/env python3
"""离线核对限定范围的世界副本是否逐项符合已批准的覆盖计划。"""
from __future__ import annotations

import argparse
from collections import Counter, defaultdict
import hashlib
import json
from pathlib import Path
import re

import nbtlib

try:
    from .audit_world import palette_counts, region_chunks
except ImportError:  # 允许按 README 中的绝对脚本路径直接执行。
    from audit_world import palette_counts, region_chunks

BE_LOCATION = re.compile(r"(.+\.mca)#(\d+)/block_entities\[(\d+)]")
ENTITY_LOCATION = re.compile(r"(.+\.mca)#(\d+)")


class AuditError(ValueError):
    pass


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for data in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(data)
    return digest.hexdigest()


def canonical(value):
    """保留 NBT 类型的有序表示，用于比较写入磁盘后的持久语义。"""
    if isinstance(value, nbtlib.Compound):
        return ("compound", tuple((key, canonical(value[key])) for key in sorted(value)))
    if isinstance(value, nbtlib.List):
        return ("list", tuple(canonical(item) for item in value))
    if isinstance(value, nbtlib.ByteArray):
        return ("byte_array", tuple(int(item) for item in value))
    if isinstance(value, nbtlib.IntArray):
        return ("int_array", tuple(int(item) for item in value))
    if isinstance(value, nbtlib.LongArray):
        return ("long_array", tuple(int(item) for item in value))
    if isinstance(value, str):
        return (type(value).__name__, str(value))
    if isinstance(value, int):
        return (type(value).__name__, int(value))
    if isinstance(value, float):
        return (type(value).__name__, float(value))
    raise AuditError(f"不支持的 NBT 值类型：{type(value).__name__}")


def load_report(path: Path) -> tuple[dict[str, str], dict]:
    hashes: dict[str, str] = {}
    summary = None
    with path.open() as source:
        for line_number, line in enumerate(source, 1):
            row = json.loads(line)
            operation = row.get("operation")
            if operation in ("complete_payload_overlay", "complete_nbt_file"):
                relative = row["path"]
                target_hash = row["target_sha256"]
                if relative in hashes or not re.fullmatch(r"[0-9a-f]{64}", target_hash):
                    raise AuditError(f"覆盖报告第 {line_number} 行路径重复或哈希无效")
                hashes[relative] = target_hash
            elif "payload_overlay_complete" in row:
                summary = row
    if summary is None or summary.get("payload_overlay_complete") is not True \
            or summary.get("ready_for_minecraft") is not False \
            or summary.get("base_world_unchanged") is not True:
        raise AuditError("覆盖报告摘要不完整")
    return hashes, summary


def audit(world: Path, plan_path: Path, overlay_report: Path, output: Path) -> dict:
    if output.exists() or output == world or world in output.parents:
        raise AuditError("报告目标已存在或位于被审计世界中")
    marker = world / ".migration-incomplete"
    if marker.read_text() != "payload_overlay_complete; full_offline_audit_required; do_not_start_minecraft\n":
        raise AuditError("世界没有处于等待全档离线审计的状态")

    plan = nbtlib.load(plan_path)
    if int(plan["format"]) != 1 or not bool(plan["complete"]) \
            or int(plan["target_data_version"]) != 3955 \
            or str(plan["retention_policy"]) != "industrial_storage_players_v1":
        raise AuditError("覆盖计划未完成或版本/保留策略不符")

    base_hashes = {str(row["path"]): str(row["sha256"]) for row in plan["base_files"]}
    omitted = {str(value) for value in plan["omit_files"]}
    if ".migration-incomplete" not in omitted:
        raise AuditError("计划未明确隔离不可启动标志")
    target_hashes, summary = load_report(overlay_report)
    expected_paths = set(base_hashes) - omitted
    expected_paths.update(str(row["path"]) for row in plan["files"])
    expected_paths.add(".migration-incomplete")
    actual_paths = {path.relative_to(world).as_posix() for path in world.rglob("*") if path.is_file()}
    if actual_paths != expected_paths:
        missing = sorted(expected_paths - actual_paths)
        extra = sorted(actual_paths - expected_paths)
        raise AuditError(f"目标文件集合不符，缺少={missing[:5]}，多出={extra[:5]}")

    complete_files = {str(row["path"]): row["data"] for row in plan["files"]}
    for relative in sorted(actual_paths - {".migration-incomplete"}):
        actual_hash = sha256(world / relative)
        if relative in target_hashes:
            expected_hash = target_hashes[relative]
        elif relative in complete_files:
            raise AuditError(f"完整 NBT 文件缺少覆盖报告哈希：{relative}")
        else:
            expected_hash = base_hashes[relative]
        if actual_hash != expected_hash:
            raise AuditError(f"目标文件 SHA-256 不符：{relative}")
    if set(target_hashes) - actual_paths:
        raise AuditError("覆盖报告包含目标世界中不存在的文件")

    allowed_states = {canonical(row["data"]) for row in plan["block_state_rows"]}
    kept_blocks = {str(value) for value in plan["kept_block_ids"]}
    block_entity_rows: dict[str, dict[int, dict[int, dict]]] = defaultdict(lambda: defaultdict(dict))
    expected_kept_be = 0
    expected_retired_be = 0
    for row in plan["block_entities"]:
        match = BE_LOCATION.fullmatch(str(row["location"]))
        if match is None:
            raise AuditError("方块实体位置格式无效")
        path, chunk, index = match[1], int(match[2]), int(match[3])
        if index in block_entity_rows[path][chunk]:
            raise AuditError("方块实体计划包含重复源索引")
        block_entity_rows[path][chunk][index] = row
        if bool(row.get("retired", False)):
            expected_retired_be += 1
        else:
            expected_kept_be += 1
    entity_rows: dict[str, dict[int, dict]] = defaultdict(dict)
    expected_entities = 0
    expected_retired_entities = 0
    for row in plan["entity_chunks"]:
        match = ENTITY_LOCATION.fullmatch(str(row["label"]))
        if match is None:
            raise AuditError("实体区块位置格式无效")
        path, index = match[1], int(match[2])
        if index in entity_rows[path]:
            raise AuditError("实体区块计划包含重复索引")
        entity_rows[path][index] = row
        expected_entities += len(row["data"]["Entities"])
        expected_retired_entities += len(row["retired_actor_uuids"])
    if expected_kept_be + expected_retired_be != int(plan["expected_block_entities"]) \
            or expected_retired_be != int(plan["expected_retired_block_entities"]) \
            or expected_entities != int(plan["expected_entities"]) \
            or expected_retired_entities != int(plan["expected_retired_entities"]):
        raise AuditError("计划声明数量与实际记录不符")

    seen_be: Counter[tuple[str, int, int]] = Counter()
    seen_entities: Counter[tuple[str, int]] = Counter()
    dummy_markers = Counter()
    all_block_ids: Counter[str] = Counter()
    actual_chunks = 0
    region_paths = sorted(path for path in actual_paths if path.endswith(".mca"))
    for relative in region_paths:
        for index, root in region_chunks(world / relative):
            actual_chunks += 1
            if int(root["DataVersion"]) != 3955:
                raise AuditError(f"目标区域仍有非 3955 数据：{relative}#{index}")
            for section in root.get("sections", []):
                states = section.get("block_states", {})
                for state in states.get("palette", []):
                    if canonical(state) not in allowed_states:
                        raise AuditError(f"目标方块状态不在批准映射中：{relative}#{index}")
                if states:
                    all_block_ids.update(palette_counts(states))
            if "/region/" in f"/{relative}":
                actual = {
                    (int(tile["x"]), int(tile["y"]), int(tile["z"])): tile
                    for tile in root.get("block_entities", [])
                }
                prepared = block_entity_rows.get(relative, {}).get(index, {})
                for source_index, row in prepared.items():
                    key = (relative, index, source_index)
                    seen_be[key] += 1
                    data = row.get("data")
                    if bool(row.get("retired", False)):
                        if data is not None:
                            raise AuditError("已归档方块实体仍携带目标数据")
                        continue
                    position = (int(data["x"]), int(data["y"]), int(data["z"]))
                    actual_tile = actual.get(position)
                    if actual_tile is None or canonical(actual_tile) != canonical(data):
                        raise AuditError(f"保留方块实体与计划不符：{relative}#{index}{position}")
                retained_actual = [tile for tile in root.get("block_entities", [])
                                   if str(tile.get("id")) != "DUMMY"]
                expected_count = sum(not bool(row.get("retired", False)) for row in prepared.values())
                if len(retained_actual) != expected_count:
                    raise AuditError(f"方块实体数量不符：{relative}#{index}")
                dummy_markers.update(str(tile["id"]) for tile in root.get("block_entities", [])
                                     if str(tile.get("id")) == "DUMMY")
            if "/entities/" in f"/{relative}":
                row = entity_rows.get(relative, {}).get(index)
                if row is None or canonical(root) != canonical(row["data"]):
                    raise AuditError(f"实体区块与计划不符：{relative}#{index}")
                seen_entities[(relative, index)] += 1

    expected_be_keys = {
        (relative, chunk, source_index)
        for relative, chunks in block_entity_rows.items()
        for chunk, rows in chunks.items()
        for source_index in rows
    }
    expected_entity_keys = {
        (relative, chunk)
        for relative, chunks in entity_rows.items()
        for chunk in chunks
    }
    if set(seen_be) != expected_be_keys or any(count != 1 for count in seen_be.values()):
        raise AuditError("并非所有计划方块实体都恰好应用一次")
    if set(seen_entities) != expected_entity_keys or any(count != 1 for count in seen_entities.values()):
        raise AuditError("并非所有计划实体区块都恰好应用一次")
    if actual_chunks != int(plan["expected_output_chunks"]) or actual_chunks != int(summary["chunks"]):
        raise AuditError("目标区块总数与计划或覆盖报告不符")

    for relative, value in complete_files.items():
        if canonical(nbtlib.load(world / relative)) != canonical(value):
            raise AuditError(f"完整 NBT 文件与计划不符：{relative}")

    result = {
        "format": 1,
        "complete": True,
        "ready_for_minecraft": False,
        "files": len(actual_paths),
        "regions": len(region_paths),
        "chunks": actual_chunks,
        "retained_block_entities": expected_kept_be,
        "retired_block_entities": expected_retired_be,
        "entity_chunks": len(expected_entity_keys),
        "retained_entities": expected_entities,
        "retired_entities": expected_retired_entities,
        "pending_markers": dict(dummy_markers),
        "target_block_ids": dict(sorted(all_block_ids.items())),
        "block_ids_outside_catalogue": sorted(set(all_block_ids) - kept_blocks),
        "overlay_report_sha256": sha256(overlay_report),
    }
    if result["block_ids_outside_catalogue"]:
        raise AuditError(f"目标世界仍有目录外方块：{result['block_ids_outside_catalogue'][:10]}")
    with output.open("x") as stream:
        json.dump(result, stream, ensure_ascii=False, indent=2)
        stream.write("\n")
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("world", type=Path)
    parser.add_argument("plan", type=Path)
    parser.add_argument("--overlay-report", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    try:
        result = audit(args.world.resolve(strict=True), args.plan.resolve(strict=True),
                       args.overlay_report.resolve(strict=True), args.output.resolve())
    except (AuditError, OSError, KeyError, ValueError) as error:
        parser.exit(1, f"目标世界审计失败：{error}\n")
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
