"""唯一旧档的 AE 无线终端、量子链接与通用元件状态转换。"""
from copy import deepcopy
from typing import Callable
from uuid import UUID
import math

import nbtlib as n

from .ae_data import convert_key
from .item_components import inventory_items


AE_STATE_IDS = {"ae2:quantum_entangled_singularity", "ae2:wireless_crafting_terminal",
                "ae2wtlib:wireless_universal_terminal", "ae2omnicells:complex_omni_cell_1m"}


def convert_ae_item(source: n.Compound, target: n.Compound,
                    convert_item: Callable[[n.Compound], n.Compound]) -> n.Compound:
    id = str(source["id"])
    tag = source.get("tag", n.Compound())
    result = deepcopy(target)
    if id not in AE_STATE_IDS or not tag:
        return result
    components = result.setdefault("components", n.Compound())
    custom = components.get("minecraft:custom_data", n.Compound())

    def move(old, new, value):
        if new in components or custom.get(old) != tag[old]:
            raise ValueError("AE 物品组件与原始状态冲突")
        components[new] = deepcopy(value)
        del custom[old]

    def inventory(key, size):
        if not isinstance(tag[key], n.List):
            raise ValueError("AE 物品库存不是旧槽位列表")
        entries = n.List[n.Compound]()
        for slot, old in enumerate(inventory_items(n.Compound({"Size": n.Int(size), "Items": tag[key]}))):
            if old:
                stack = convert_item(old)
                stack.pop("Slot", None)
                if not stack or int(stack.get("count", 0)) <= 0:
                    raise ValueError("AE 终端内的非空物品未转换")
                entries.append(n.Compound({"slot": n.Int(slot), "item": stack}))
        return entries

    if id == "ae2:quantum_entangled_singularity":
        if set(tag) != {"freq"} or not isinstance(tag["freq"], n.Long):
            raise ValueError("量子纠缠奇点频率结构不符")
        move("freq", "ae2:entangled_singularity_id", tag["freq"])
    elif id == "ae2omnicells:complex_omni_cell_1m":
        fields = {"ae_universal_cell_uuid", "ae_universal_cell_bytes_usage", "ae_universal_cell_types_usage",
                  "ae_universal_cell_state", "ae_universal_cell_show_tooltip_stacks"}
        if set(tag) != fields or not isinstance(tag["ae_universal_cell_uuid"], n.String):
            raise ValueError("通用元件结构与唯一旧档不符")
        uuid = UUID(str(tag["ae_universal_cell_uuid"]))
        if str(uuid) != str(tag["ae_universal_cell_uuid"]):
            raise ValueError("通用元件 UUID 不是原始规范字符串")
        words = [(uuid.int >> shift) & 0xffffffff for shift in (96, 64, 32, 0)]
        move("ae_universal_cell_uuid", "ae2omnicells:cell_uuid", n.IntArray([v if v < 2**31 else v - 2**32 for v in words]))
        for old, new, type in [("bytes_usage", "bytes_usage", n.Long), ("types_usage", "types_usage", n.Int),
                              ("state", "state", n.String)]:
            key = "ae_universal_cell_" + old
            if not isinstance(tag[key], type) or (type != n.String and int(tag[key]) < 0):
                raise ValueError("通用元件用量或状态类型不符")
            move(key, "ae2omnicells:cell_" + new, tag[key])
        key = "ae_universal_cell_show_tooltip_stacks"
        move(key, "ae2omnicells:cell_show_tooltip_stacks", n.List[n.Compound]([convert_key(v, convert_item) for v in tag[key]]))
    else:
        settings = n.Compound()
        for key in ("sort_by", "sort_direction", "view_mode", "filter_type"):
            if key in tag:
                if not isinstance(tag[key], n.String) or custom.get(key) != tag[key]:
                    raise ValueError("AE 终端界面设置与原始字符串不符")
                settings[key] = deepcopy(tag[key])
                del custom[key]
        if settings:
            if "ae2:exported_settings" in components:
                raise ValueError("AE 终端界面设置组件冲突")
            components["ae2:exported_settings"] = settings
        for old, new in [("internalMaxPower", "ae2:energy_capacity"), ("internalCurrentPower", "ae2:stored_energy")]:
            if old in tag:
                if not isinstance(tag[old], n.Double) or not math.isfinite(float(tag[old])) or float(tag[old]) < 0:
                    raise ValueError("AE 终端能源不是非负有限 Double")
                move(old, new, tag[old])
        if "accessPoint" in tag:
            point = tag["accessPoint"]
            if (not isinstance(point, n.Compound) or set(point) != {"pos", "dimension"} or
                not isinstance(point["pos"], n.IntArray) or len(point["pos"]) != 3 or not isinstance(point["dimension"], n.String)):
                raise ValueError("AE 无线链接维度或坐标结构不符")
            move("accessPoint", "ae2:wireless_link_target", point)
        for old, new, size in [("upgrades", "ae2:upgrades", 2), ("craftingGrid", "ae2:crafting_inv", 9)]:
            if old in tag:
                move(old, new, inventory(old, size))
        if "singularity" in tag:
            items = inventory("singularity", 1)
            if len(items) != 1 or str(items[0]["item"]["id"]) != "ae2:quantum_entangled_singularity":
                raise ValueError("AE 无线终端量子链接物品未完整转换")
            move("singularity", "ae2wtlib:singularity", items[0]["item"])
        if id == "ae2wtlib:wireless_universal_terminal":
            for old, new in [("crafting", "has_crafting_terminal"), ("pattern_encoding", "has_pattern_encoding_terminal"),
                             ("ex_pattern_access", "has_ex_pattern_access_terminal")]:
                if old in tag:
                    if not isinstance(tag[old], n.Byte) or int(tag[old]) != 1:
                        raise ValueError("通用终端模块不是旧档已确认的已安装状态")
                    move(old, "ae2wtlib:" + new, n.Compound())
            for old in ("restock", "pick_block", "craft_if_missing"):
                if old in tag:
                    if not isinstance(tag[old], n.Byte) or int(tag[old]) not in (0, 1):
                        raise ValueError("通用终端控制不是布尔值")
                    move(old, "ae2wtlib:" + old, tag[old])
            for old, new, type in [("currentTerminal", "current_terminal", n.String), ("magnet_settings", "magnet_settings", n.Byte)]:
                if old in tag:
                    if not isinstance(tag[old], type):
                        raise ValueError("通用终端当前界面或磁铁模式类型不符")
                    move(old, "ae2wtlib:" + new, tag[old])
            keys = {"mode", "substitute", "substituteFluids"}
            if set(tag) & keys != keys or str(tag["mode"]) != "PROCESSING" or any(
                    not isinstance(tag[k], n.Byte) or int(tag[k]) not in (0, 1) for k in keys - {"mode"}):
                raise ValueError("通用终端编码状态与唯一旧档不符")
            value = n.Compound({k: deepcopy(tag[k]) for k in keys})
            if "ae2wtlib:pattern_encoding_logic" in components:
                raise ValueError("通用终端编码组件冲突")
            components["ae2wtlib:pattern_encoding_logic"] = value
            for k in keys:
                if custom.get(k) != tag[k]:
                    raise ValueError("通用终端原始编码状态冲突")
                del custom[k]
    if "minecraft:custom_data" in components and not custom:
        del components["minecraft:custom_data"]
    return result
