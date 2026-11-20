"""唯一旧档的 Mekanism 持久数据及物品组件准备；库存读写复用目标作者接口。"""
from __future__ import annotations

from copy import deepcopy
from dataclasses import dataclass
import re
from typing import Callable

import nbtlib as n


# 对照源 10.4.16.80 NBTConstants 与目标 10.7.19.85 SerializationConstants，限定实际旧档字段。
FIELD_NAMES = {
    "EnergyContainers": "energy_containers", "FluidTanks": "fluid_tanks", "GasTanks": "gas_tanks",
    "InfusionTanks": "infusion_tanks", "PigmentTanks": "pigment_tanks", "SlurryTanks": "slurry_tanks",
    "HeatCapacitors": "heat_capacitors", "Items": "items", "Item": "item", "Slot": "slot",
    "Tank": "tank", "Container": "container", "componentConfig": "component_config",
    "componentEjector": "component_ejector", "componentUpgrade": "component_upgrade",
    "componentSecurity": "component_security", "componentFrequency": "component_frequency",
    "securityMode": "security_mode", "ownerName": "owner_name", "activeState": "active_state",
    "controlType": "control_type", "currentRedstone": "current_redstone", "updateDelay": "update_delay",
    "usedSoFar": "used_so_far", "inventoryID": "inventory_id", "chunkSet": "chunk_set",
    "stabilizerChunksToLoad": "stabilizer_chunks_to_load", "dumpLeft": "dump_left", "dumpRight": "dump_right",
    "editMode": "edit_mode", "heatCapacity": "heat_capacity", "energyUsage": "energy_usage",
    "freqList": "freq_list", "HeatCapacitors_stored": "heat_capacitors_stored",
    "FluidTanks_stored": "fluid_tanks_stored", "Items_stored": "items_stored",
    "boxedChemical": "boxed_chemical",
}
ROOT_FIELDS = set(FIELD_NAMES) | {
    "id", "x", "y", "z", "ForgeCaps", "keepPacked", "redstone", "progress", "sorting", "dumping", "mode",
    "data", "owner", "state", "components", "create_mobile_packages:from_mobile_packager",
} | {f"connection{i}" for i in range(6)}
CHEMICAL_NAMES = {"gasName": "gas", "infuseTypeName": "infusion", "pigmentName": "pigment", "slurryName": "slurry"}
MARKER = "create_mobile_packages:from_mobile_packager"
MARKER_COMPONENT = "create_mobile_packages:packager_origin"
CONFIGURATOR_MODES = (
    "configurate_items", "configurate_fluids", "configurate_chemicals", "configurate_chemicals",
    "configurate_chemicals", "configurate_chemicals", "configurate_energy", "configurate_heat", "empty", "rotate", "wrench",
)
SECURITY_MODES = ("public", "private", "trusted")


@dataclass
class MekConversion:
    data: n.Compound
    removed: dict[str, n.Base]


@dataclass
class MekItemPreparation(MekConversion):
    tile_data: n.Compound | None = None


def _put(data: n.Compound, key: str, value: n.Base) -> None:
    if key in data:
        raise ValueError(f"Mekanism 目标字段或组件冲突：{key}")
    data[key] = value


def _config(source: n.Compound) -> n.Compound:
    result = n.Compound()
    for key, value in source.items():
        if re.fullmatch(r"config[0-7]", key):
            if not isinstance(value, n.Compound) or set(value) != {f"side{i}" for i in range(6)}:
                raise ValueError("Mekanism 旧侧面配置结构不符")
            sides = [value[f"side{i}"] for i in range(6)]
            if any(not isinstance(side, n.Int) or not 0 <= int(side) <= 9 for side in sides):
                raise ValueError("Mekanism 旧侧面模式值不符")
            # 保留 legacy transmission 编号（物品为 6），由作者 getLegacyOrdinal / read 接口处理合并。
            result[key] = n.IntArray([int(side) for side in sides])
        elif re.fullmatch(r"eject[0-7]", key) and isinstance(value, n.Byte) and int(value) in (0, 1):
            result[key] = deepcopy(value)
        else:
            raise ValueError(f"Mekanism 旧侧面配置含未覆盖字段：{key}")
    return result


def _ejector(source: n.Compound) -> n.Compound:
    if set(source) - ({f"color{i}" for i in range(6)} | {"strictInput", "color"}):
        raise ValueError("Mekanism 旧弹出器含未覆盖字段")
    if any(f"color{i}" not in source for i in range(6)):
        raise ValueError("Mekanism 旧弹出器缺少侧面颜色")
    colors = [source[f"color{i}"] for i in range(6)]
    if any(not isinstance(color, n.Int) or not -1 <= int(color) <= 16 for color in colors):
        raise ValueError("Mekanism 旧弹出器颜色值不符")
    strict = source["strictInput"]
    if not isinstance(strict, n.Byte) or int(strict) not in (0, 1):
        raise ValueError("Mekanism 旧弹出器严格输入值不符")
    result = n.Compound({"input_color": n.IntArray([int(color) for color in colors]), "strict_input": deepcopy(strict)})
    if "color" in source:
        result["color"] = deepcopy(source["color"])
    return result


def _walk(value: n.Base, convert_item: Callable[[n.Compound], n.Compound], removed: dict, path: str) -> n.Base:
    if isinstance(value, n.List):
        return type(value)(_walk(child, convert_item, removed, f"{path}[{i}]") for i, child in enumerate(value))
    if not isinstance(value, n.Compound):
        return deepcopy(value)
    if "id" in value and "Count" in value:
        return convert_item(deepcopy(value))  # 不把 ItemStack 的 Count / tag 混同 Mek 的容器字段。
    chemical = set(value) & CHEMICAL_NAMES.keys()
    if chemical:
        if len(chemical) != 1 or set(value) - (chemical | {"amount", "chemicalType"}):
            raise ValueError("Mekanism 旧化学栈含未覆盖字段")
        name = next(iter(chemical))
        if not isinstance(value["amount"], n.Long) or int(value["amount"]) <= 0:
            raise ValueError("Mekanism 化学储量不是正 Long")
        if "chemicalType" in value:
            if str(value["chemicalType"]) != CHEMICAL_NAMES[name]:
                raise ValueError("Mekanism boxed chemical 类型和内容不符")
            removed[path + "/chemicalType"] = deepcopy(value["chemicalType"])
        return n.Compound({"id": deepcopy(value[name]), "amount": deepcopy(value["amount"])})
    if isinstance(value.get("amount"), n.Long) and "id" not in value:
        raise ValueError("Mekanism 化学栈缺少已覆盖注册名字段")
    if "FluidName" in value:
        if set(value) != {"FluidName", "Amount"} or not isinstance(value["Amount"], n.Int) or int(value["Amount"]) <= 0:
            raise ValueError("Mekanism 旧流体栈结构或储量不符")
        return n.Compound({"id": deepcopy(value["FluidName"]), "amount": deepcopy(value["Amount"])})
    result = n.Compound()
    frequency_identity = "publicFreq" in value
    if frequency_identity:
        public = value["publicFreq"]
        if not isinstance(public, n.Byte) or int(public) != 1:
            raise ValueError("唯一旧档之外的 Mekanism 非公开频率结构")
        mode = value.get("securityMode", n.Int(0))
        if not isinstance(mode, n.Int) or not 0 <= int(mode) < len(SECURITY_MODES):
            raise ValueError("Mekanism 旧安全频率模式不符")
        # frequency identity / SecurityFrequency 的 enum codec 使用字符串；componentSecurity 则保留数值。
        _put(result, "security_mode", n.String(SECURITY_MODES[int(mode)]))
    connections = {key for key in value if re.fullmatch(r"connection[0-5]", key)}
    if connections:
        if connections != {f"connection{i}" for i in range(6)}:
            raise ValueError("Mekanism 旧传输部件缺少方向配置")
        values = [value[f"connection{i}"] for i in range(6)]
        if any(not isinstance(child, n.Int) or not 0 <= int(child) <= 3 for child in values):
            raise ValueError("Mekanism 旧连接模式值不符")
        _put(result, "connection", n.IntArray([int(child) for child in values]))
    for key, child in value.items():
        if key in connections:
            continue
        if key == "ForgeCaps":
            if not isinstance(child, n.Compound) or child:
                raise ValueError("Mekanism 方块存在未覆盖 ForgeCaps")
            removed[path + "/ForgeCaps"] = deepcopy(child)
            continue
        if key == "keepPacked":
            if not isinstance(child, n.Byte) or int(child) != 0:
                raise ValueError("Mekanism 存在未覆盖 keepPacked 状态")
            removed[path + "/keepPacked"] = deepcopy(child)
            continue
        if key == "publicFreq":
            removed[path + "/publicFreq"] = deepcopy(child)
            continue
        if frequency_identity and key == "securityMode":
            continue
        converted = (_config(child) if key == "componentConfig" else
                     _ejector(child) if key == "componentEjector" else
                     deepcopy(child) if key == "components" else
                     _walk(child, convert_item, removed, path + "/" + key))
        _put(result, FIELD_NAMES.get(key, key), converted)
    return result


def convert_box_data(source: n.Compound, convert_state: Callable, convert_block_entity: Callable) -> MekConversion:
    if not isinstance(source, n.Compound) or set(source) - {"blockState", "tileTag", MARKER} or "blockState" not in source:
        raise ValueError("Mekanism 旧纸箱载荷结构不符")
    result = n.Compound({"state": convert_state(deepcopy(source["blockState"]))})
    if "tileTag" in source:
        result["block_entity_tag"] = convert_block_entity(deepcopy(source["tileTag"]))
    removed = {}
    if MARKER in source:
        marker = source[MARKER]
        if not isinstance(marker, n.Byte) or int(marker) not in (0, 1):
            raise ValueError("CMP 纸箱来源标记不符")
        removed[MARKER] = deepcopy(marker)
    return MekConversion(result, removed)


def normalize_block_entity(source: n.Compound, convert_item: Callable, convert_state: Callable,
                           convert_block_entity: Callable) -> MekConversion:
    if str(source.get("id", "")).split(":", 1)[0] not in {"mekanism", "mekanismgenerators"}:
        raise ValueError("输入不是 Mekanism 方块实体")
    if set(source) - ROOT_FIELDS:
        raise ValueError(f"Mekanism 方块含未覆盖根字段：{sorted(set(source) - ROOT_FIELDS)}")
    original = deepcopy(source)
    removed = {}
    if str(source["id"]) == "mekanism:cardboard_box" and "data" in original:
        box = convert_box_data(original.pop("data"), convert_state, convert_block_entity)
        components = original.setdefault("components", n.Compound())
        _put(components, "mekanism:block_data", box.data)
        for key, value in box.removed.items():
            removed["data/" + key] = value
            if key == MARKER:
                _put(components, MARKER_COMPONENT, deepcopy(value))
    if MARKER in original:
        value = original.pop(MARKER)
        if not isinstance(value, n.Byte) or int(value) not in (0, 1):
            raise ValueError("CMP 方块来源标记不符")
        components = original.setdefault("components", n.Compound())
        if MARKER_COMPONENT in components and components[MARKER_COMPONENT] != value:
            raise ValueError("CMP 外层及纸箱载荷来源冲突")
        components[MARKER_COMPONENT] = deepcopy(value)
        removed[MARKER] = deepcopy(value)
    return MekConversion(_walk(original, convert_item, removed, ""), removed)


def normalize_saved_data(source: n.Compound, convert_item: Callable) -> MekConversion:
    removed = {}
    return MekConversion(_walk(source, convert_item, removed, ""), removed)


def prepare_item(source: n.Compound, target: n.Compound, convert_item: Callable, convert_state: Callable,
                 convert_block_entity: Callable) -> MekItemPreparation:
    old = source.get("tag", n.Compound()).get("mekData")
    if not isinstance(old, n.Compound) or str(target.get("id")) != str(source.get("id")):
        raise ValueError("Mekanism 物品原数据或目标注册名不符")
    data = deepcopy(target)
    components = data.setdefault("components", n.Compound())
    custom = components.get("minecraft:custom_data", n.Compound())
    if custom.get("mekData") != old:
        raise ValueError("原版 DFU 后 Mekanism 物品数据与输入不符")
    id = str(source["id"])
    removed = {}
    if id == "mekanism:configurator":
        if set(old) != {"state"} or not isinstance(old["state"], n.Int) or not 0 <= int(old["state"]) < len(CONFIGURATOR_MODES):
            raise ValueError("Mekanism 旧配置工具模式不符")
        _put(components, "mekanism:configurator_mode", n.String(CONFIGURATOR_MODES[int(old["state"])]))
    elif id == "mekanism:cardboard_box":
        if set(old) != {"data"}:
            raise ValueError("Mekanism 纸箱物品含未覆盖数据")
        box = convert_box_data(old["data"], convert_state, convert_block_entity)
        _put(components, "mekanism:block_data", box.data)
        removed.update(box.removed)
        if MARKER in box.removed:
            _put(components, MARKER_COMPONENT, deepcopy(box.removed[MARKER]))
    else:
        if set(old) - ROOT_FIELDS:
            raise ValueError("Mekanism 机器物品含未覆盖字段")
        tile = _walk(old, convert_item, removed, "mekData")
        security = n.Compound()
        for key in ("owner", "security_mode"):
            if key in tile:
                security[key] = tile.pop(key)
        if security:
            _put(tile, "component_security", security)
        # 保留原自定义数据，直到原生方块 collectComponents 完成后由 Java 桥接删除 mekData。
        return MekItemPreparation(data, removed, tile)
    del custom["mekData"]
    if not custom:
        del components["minecraft:custom_data"]
    return MekItemPreparation(data, removed)
