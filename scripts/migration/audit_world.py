#!/usr/bin/env python3
"""只读审计所有维度、玩家及 SavedData；报告保留在私有测试目录。"""
from __future__ import annotations

import argparse
from collections import Counter
import gzip
import hashlib
import io
import json
from pathlib import Path
import re
import struct
import uuid
import zlib

import nbtlib
import numpy as np

REGISTRY_ID = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")


def region_chunks(path: Path):
    """支持旧档的 gzip/zlib/raw 与外置 .mcc，损坏区块不能静默跳过。"""
    with path.open("rb") as source:
        source.seek(0, 2)
        size = source.tell()
        source.seek(0)
        if size == 0:
            return  # RegionFile 可留下尚未写入区块的空占位文件，另记到报告。
        header = source.read(8192)
        if len(header) != 8192:
            raise ValueError("区域文件头不完整")
        used = {0, 1}
        for index in range(1024):
            entry = struct.unpack_from(">I", header, index * 4)[0]
            offset, sectors = entry >> 8, entry & 255
            if entry == 0:
                continue
            if offset < 2 or sectors == 0 or offset + sectors > (size + 4095) // 4096:
                raise ValueError(f"区域槽 {index} 的扇区范围无效")
            allocated = set(range(offset, offset + sectors))
            if allocated & used:
                raise ValueError(f"区域槽 {index} 的扇区重叠")
            used.update(allocated)
            source.seek(offset * 4096)
            length = struct.unpack(">I", source.read(4))[0]
            if length < 1 or length + 4 > sectors * 4096 or offset * 4096 + 4 + length > size:
                raise ValueError(f"区域槽 {index} 的数据长度无效")
            compression = source.read(1)[0]
            if compression & 128:
                match = re.fullmatch(r"r\.(-?\d+)\.(-?\d+)\.mca", path.name)
                if match is None or length != 1:
                    raise ValueError("外置区块索引无效")
                x = int(match[1]) * 32 + index % 32
                z = int(match[2]) * 32 + index // 32
                payload = path.with_name(f"c.{x}.{z}.mcc").read_bytes()
            else:
                payload = source.read(length - 1)
            compression &= 127
            if compression == 1:
                payload = gzip.decompress(payload)
            elif compression == 2:
                payload = zlib.decompress(payload)
            elif compression != 3:
                raise ValueError(f"不支持的区域压缩类型 {compression}，须补实现后重新审计")
            yield index, nbtlib.File.parse(io.BytesIO(payload))


def palette_counts(states) -> Counter:
    palette = states.get("palette", [])
    if not palette:
        raise ValueError("方块状态缺少 palette")
    names = [str(entry["Name"]) for entry in palette]
    if len(names) == 1:
        return Counter({names[0]: 4096})
    bits = max(4, (len(names) - 1).bit_length())
    per_long = 64 // bits
    words = states.get("data", [])
    if len(words) != (4096 + per_long - 1) // per_long:
        raise ValueError("方块状态数据长度与 palette 不匹配")
    # 实际原档有 30 万区块；按 section 批量解码，避免逐方块 Python 循环。
    packed = np.asarray(words).astype(np.uint64)
    shifts = np.arange(per_long, dtype=np.uint64) * bits
    slots = ((packed[:, None] >> shifts) & ((1 << bits) - 1)).reshape(-1)[:4096]
    if int(slots.max()) >= len(names):
        raise ValueError("方块状态引用越界 palette 索引")
    counts = np.bincount(slots.astype(np.int64), minlength=len(names))
    result = Counter()
    # 同一方块的方向、含水等不同状态可以共享 Name，必须累加而不是覆盖。
    for name, count in zip(names, counts):
        result[name] += int(count)
    return +result


def stack_fact(stack) -> dict:
    item_id = str(stack.get("id", ""))
    count = int(stack.get("Count", stack.get("count", 0)))
    # 原始 SNBT 留作迁移前后定点排查；组件变化需要转换器语义对账。
    return {"id": item_id, "count": count,
            "snbt_sha256": hashlib.sha256(stack.snbt().encode()).hexdigest()}


class Audit:
    def __init__(self, snbt_cache: Path | None = None):
        self.references = Counter()
        self.blocks = Counter()
        self.entities = Counter()
        self.items = []
        self.custom_ledgers = {}
        self.players = {}
        self.data_versions = Counter()
        self.auxiliary_files = {}
        self.registries = {}
        self.dimensions = []
        self.archived_files = []
        self.empty_region_files = []
        self.nbt_files = 0
        self.chunks = 0
        self.errors = []
        self.snbt_cache = snbt_cache
        self.snbt_sources = (json.loads((snbt_cache / "sources.json").read_text()) if snbt_cache else {})

    def walk(self, value, location: str):
        if isinstance(value, nbtlib.Compound):
            if "id" in value and ("Count" in value or "count" in value):
                self.items.append({"location": location, **stack_fact(value)})
            for name, child in value.items():
                if name == "fml" and location == "level.dat":
                    continue  # 注册快照另列；不能把安装的每个 ID 都算成存档正在使用。
                self.walk(child, location + "/" + name)
        elif isinstance(value, nbtlib.List):
            for index, child in enumerate(value):
                self.walk(child, f"{location}[{index}]")
        elif isinstance(value, nbtlib.String) and REGISTRY_ID.fullmatch(str(value)):
            self.references[str(value)] += 1

    def document(self, root, location: str):
        if location == "level.dat":
            self.registries = root.get("fml", {}).get("Registries", nbtlib.Compound()).unpack(json=True)
            self.dimensions = list(root.get("Data", {}).get("WorldGenSettings", {}).get("dimensions", {}))
        self.walk(root, location)
        data = root.get("Level", root)
        self.data_versions[int(root.get("DataVersion", data.get("DataVersion", 0)))] += 1
        for section in data.get("sections", []):
            if "block_states" in section:
                self.blocks.update(palette_counts(section["block_states"]))
        for entity in data.get("Entities", data.get("entities", [])):
            self.entities[str(entity.get("id", "missing-id"))] += 1

    def saved_data(self, root, relative: str):
        data = root.get("data", root)
        if relative.endswith("dimensionworks_logistics_stock.dat"):
            channels = []
            for channel in data.get("Channels", []):
                items = [{"stack": stack_fact(row["Stack"]), "amount": int(row["Amount"]),
                          "maximum": int(row["Maximum"]), "origin": str(row["Origin"])}
                         for row in channel.get("Items", [])]
                # 保留流体原始字段，不能混同物品 prototype Count 与真实 Amount。
                channels.append({"number": int(channel["Number"]), "items": items,
                                 "fluids": channel.get("Fluids", nbtlib.List[nbtlib.Compound]()).unpack(json=True),
                                 "item_upgrades": int(channel.get("ItemUpgrades", 0)),
                                 "fluid_upgrades": int(channel.get("FluidUpgrades", 0))})
            self.custom_ledgers[relative] = channels
        elif relative.endswith("dw_cloud_disk.dat"):
            owners = {}
            for owner, record in data.get("Owners", {}).items():
                uuid.UUID(owner)  # 无效所有者不能沿用旧代码的静默丢弃。
                owners[owner] = {"items": [stack_fact(stack) for stack in record.get("Items", [])],
                                 "notices": int(record.get("Notices", 0))}
            self.custom_ledgers[relative] = owners
        elif relative.endswith("dimensionworks_logistics_channels.dat"):
            self.custom_ledgers[relative] = data.unpack(json=True)

    def scan(self, world: Path) -> dict:
        active = []
        for path in world.rglob("*"):
            if not path.is_file():
                continue
            relative = path.relative_to(world)
            if relative.parts[0] == "trueuuid-backups":
                self.archived_files.append({"path": relative.as_posix(), "bytes": path.stat().st_size})
            else:
                active.append(path)
        for path in sorted(p for p in active if p.suffix in (".dat", ".nbt", ".snbt", ".json")):
            relative = path.relative_to(world).as_posix()
            try:
                if path.suffix in (".snbt", ".json"):
                    raw = path.read_bytes()
                    self.auxiliary_files[relative] = {"bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest()}
                    if path.suffix == ".json":
                        json.loads(raw)
                        continue
                    if self.snbt_cache is not None:
                        cached = self.snbt_sources[relative]
                        if hashlib.sha256(raw).hexdigest() != cached["source_sha256"]:
                            raise ValueError("FTB SNBT 缓存与原始文件不一致")
                        cache_file = self.snbt_cache / (relative + ".nbt")
                        if hashlib.sha256(cache_file.read_bytes()).hexdigest() != cached["nbt_sha256"]:
                            raise ValueError("FTB SNBT 缓存 NBT 校验失败")
                        root = nbtlib.load(cache_file)
                    else:
                        root = nbtlib.parse_nbt(raw.decode("utf-8"))
                else:
                    root = nbtlib.load(path)
                self.document(root, relative)
                self.saved_data(root, relative)
                if path.parent.name == "playerdata":
                    identity = root.get("UUID")
                    actual = (str(uuid.UUID(bytes=b"".join(struct.pack(">I", int(part) & 0xffffffff)
                                                        for part in identity))) if identity is not None else None)
                    if actual is not None and actual != path.stem:
                        raise ValueError("玩家文件名与 NBT UUID 不一致")
                    self.players[path.stem] = {"uuid": actual, "dimension": str(root.get("Dimension", "")),
                                              "position": root.get("Pos", nbtlib.List[nbtlib.Double]()).unpack(json=True),
                                              "persistent_keys": list(root.get("ForgeData", root.get("NeoForgeData", {})))}
                self.nbt_files += 1
            except Exception as error:
                self.errors.append({"location": relative, "error": str(error)})
        for path in sorted(p for p in active if p.suffix == ".mca"):
            relative = path.relative_to(world).as_posix()
            try:
                if path.stat().st_size == 0:
                    self.empty_region_files.append(relative)
                for index, root in region_chunks(path):
                    self.document(root, f"{relative}#{index}")
                    self.chunks += 1
                    if self.chunks % 25000 == 0:
                        print(f"Audited {self.chunks} region chunk records", flush=True)
            except Exception as error:
                self.errors.append({"location": relative, "error": str(error)})
        return {"format": 1, "read_only": True, "complete": not self.errors,
                "nbt_files": self.nbt_files, "chunks": self.chunks, "data_versions": dict(self.data_versions),
                "players": self.players, "registry_references": dict(sorted(self.references.items())),
                "block_counts": dict(sorted(self.blocks.items())), "entity_counts": dict(sorted(self.entities.items())),
                "item_stacks": self.items, "custom_ledgers": self.custom_ledgers, "errors": self.errors,
                "auxiliary_files": self.auxiliary_files,
                "registries": self.registries, "declared_dimensions": self.dimensions,
                "archived_files": sorted(self.archived_files, key=lambda row: row["path"]),
                "empty_region_files": self.empty_region_files,
                "witherstorm_references": {key: count for key, count in self.references.items() if "witherstorm" in key}}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("world", type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--ftb-snbt-cache", type=Path, help="FTB 自身解析器生成的独立 NBT 审计缓存")
    args = parser.parse_args()
    world = args.world.resolve(strict=True)
    if not (world / "level.dat").is_file():
        parser.error("目标目录没有 level.dat")
    output = args.output.resolve()
    if output == world or world in output.parents:
        parser.error("报告不能写入被审计的存档")
    if output.exists():
        parser.error("报告已存在，拒绝覆盖")
    result = Audit(args.ftb_snbt_cache).scan(world)
    with output.open("x") as stream:
        json.dump(result, stream, ensure_ascii=False, indent=2)
        stream.write("\n")
    output.chmod(0o600)
    print(json.dumps({"complete": result["complete"], "nbt_files": result["nbt_files"],
                      "chunks": result["chunks"], "players": len(result["players"]),
                      "item_stacks": len(result["item_stacks"]), "errors": len(result["errors"])}))
    if result["errors"]:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
