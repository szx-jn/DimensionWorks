"""唯一旧档的 AE2 键及存储元件组件转换；储量始终保持 Long。"""
from __future__ import annotations

from copy import deepcopy
import io
from typing import Callable

import nbtlib as n


def key_identity(value: n.Compound) -> bytes:
    """忽略 Compound 顺序，保留组件中的 NBT 类型和 List 顺序；value 不含储量。"""
    def ordered(v):
        if isinstance(v, n.Compound):
            return n.Compound({key: ordered(v[key]) for key in sorted(v)})
        if isinstance(v, n.List):
            return type(v)(ordered(child) for child in v)
        return deepcopy(v)
    output = io.BytesIO()
    n.File(ordered(value)).write(output)
    return output.getvalue()


def convert_key(source: n.Compound, convert_item: Callable[[n.Compound], n.Compound]) -> n.Compound:
    kind = str(source.get("#c", ""))
    if kind == "ae2:i":
        if set(source) - {"#c", "id", "tag", "caps", "#"}:
            raise ValueError("存在未覆盖的 AE2 物品键字段")
        stack = n.Compound({"id": deepcopy(source["id"]), "Count": n.Byte(1)})
        if "tag" in source:
            stack["tag"] = deepcopy(source["tag"])
        if "caps" in source:
            stack["ForgeCaps"] = deepcopy(source["caps"])
        converted = convert_item(stack)
        if not converted or int(converted.get("count", 0)) != 1:
            raise ValueError("AE2 物品键转换结果不是有效单件模板")
        result = n.Compound({"#t": n.String(kind), "id": deepcopy(converted["id"])})
        if converted.get("components"):
            result["components"] = deepcopy(converted["components"])
    elif kind == "ae2:f":
        if set(source) - {"#c", "id", "#"}:
            raise ValueError("存在未覆盖的 AE2 流体键数据")
        result = n.Compound({"#t": n.String(kind), "id": deepcopy(source["id"])})
    else:
        raise ValueError(f"未审计的 AE2 键类型：{kind}")
    if "#" in source:
        if not isinstance(source["#"], n.Long) or int(source["#"]) <= 0:
            raise ValueError("AE2 键储量类型或数值不符")
        result["#"] = deepcopy(source["#"])
    return result


def convert_storage_cell(source: n.Compound, target: n.Compound,
                         convert_item: Callable[[n.Compound], n.Compound]) -> n.Compound:
    """调用方须完整转换键的物品组件；本函数不替代 Mekanism 等物品规则。"""
    tag = source.get("tag", n.Compound())
    if set(tag) != {"keys", "amts", "ic"}:
        raise ValueError("存储元件结构与唯一旧档审计不符")
    if not isinstance(tag["amts"], n.LongArray) or not isinstance(tag["ic"], n.Long):
        raise ValueError("存储元件储量不是 Long / LongArray")
    keys, amounts = tag["keys"], tag["amts"]
    if len(keys) != len(amounts):
        raise ValueError("存储元件键和储量数组长度不符")
    if sum(int(value) for value in amounts) != int(tag["ic"]):
        raise ValueError("存储元件缓存总量与逐键储量不符")
    entries = n.List[n.Compound]()
    positions = {}
    for key, amount in zip(keys, amounts):
        if "#" in key or int(amount) <= 0:
            raise ValueError("存储元件键含重复储量或非正数")
        entry = convert_key(key, convert_item)
        identity = key_identity(entry)
        if identity in positions:
            stored = entries[positions[identity]]
            merged = int(stored["#"]) + int(amount)
            if merged > 9223372036854775807:
                raise ValueError("相同目标 AE2 键合并后的储量超出 Long")
            stored["#"] = n.Long(merged)
        else:
            positions[identity] = len(entries)
            entry["#"] = n.Long(int(amount))
            entries.append(entry)
    data = deepcopy(target)
    components = data.setdefault("components", n.Compound())
    if "ae2:storage_cell_inv" in components:
        raise ValueError("存储元件组件冲突")
    custom = components.get("minecraft:custom_data", n.Compound())
    for name in ("keys", "amts", "ic"):
        if name not in custom or custom[name] != tag[name]:
            raise ValueError("原版 DFU 后存储元件数据与输入不符")
        del custom[name]
    if "minecraft:custom_data" in components and not custom:
        del components["minecraft:custom_data"]
    components["ae2:storage_cell_inv"] = entries
    return data


def convert_universal_cell_data(source: n.Compound,
                                convert_item: Callable[[n.Compound], n.Compound]) -> tuple[n.Compound, list[dict]]:
    """转换本服 UUID 文件的完整通用元件库存，显式对账合并后的 Long 数量。"""
    if (not isinstance(source, n.Compound) or set(source) != {"inventory"} or
        not isinstance(source["inventory"], n.Compound) or set(source["inventory"]) != {"entries", "error_entries"} or
        not isinstance(source["inventory"]["entries"], n.List) or
        not isinstance(source["inventory"]["error_entries"], n.List) or source["inventory"]["error_entries"]):
        raise ValueError("通用元件含未覆盖结构或原有读取错误，不能跳过记录")
    entries = n.List[n.Compound]()
    positions = {}
    merges = []
    for index, old in enumerate(source["inventory"]["entries"]):
        if (not isinstance(old, n.Compound) or set(old) != {"key", "amount"} or
            not isinstance(old["key"], n.Compound) or not isinstance(old["amount"], n.Long) or
            int(old["amount"]) <= 0 or "#" in old["key"]):
            raise ValueError("通用元件键结构或 Long 储量不符")
        key = convert_key(old["key"], convert_item)
        identity = key_identity(key)
        if identity in positions:
            target = entries[positions[identity]]
            amount = int(target["amount"]) + int(old["amount"])
            if amount > 9223372036854775807:
                raise ValueError("通用元件相同目标键合并后的储量超出 Long")
            merges.append({"source_index": index, "key": deepcopy(key),
                           "previous_amount": deepcopy(target["amount"]), "added_amount": deepcopy(old["amount"]),
                           "merged_amount": n.Long(amount)})
            target["amount"] = n.Long(amount)
        else:
            positions[identity] = len(entries)
            entries.append(n.Compound({"key": key, "amount": deepcopy(old["amount"])}))
    result = deepcopy(source)
    result["inventory"]["entries"] = entries
    return result, merges
