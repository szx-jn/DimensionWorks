"""唯一旧档的箭袋组件及扩容库存数量规则。

接收已执行原版 DFU 的目标栈，嵌套旧栈交给调用方转换；不写世界。
"""
from __future__ import annotations

from copy import deepcopy
from dataclasses import dataclass
from typing import Callable

import nbtlib as n


@dataclass
class ItemComponentConversion:
    data: n.Compound
    removed: dict[str, n.Base]


def inventory_items(value: n.Compound) -> list[n.Compound]:
    """保留旧槽位/空槽；越界和重复槽位必须阻止交付。"""
    if not isinstance(value, n.Compound) or set(value) != {"Size", "Items"}:
        raise ValueError("库存结构不符：需要 Size / Items")
    size = int(value["Size"])
    if not 0 <= size <= 256:
        raise ValueError("物品组件库存槽位数量超出 codec 范围")
    items = [n.Compound() for _ in range(size)]
    slots = set()
    for item in value["Items"]:
        slot = int(item["Slot"])
        if not 0 <= slot < size or slot in slots:
            raise ValueError("库存槽位重复或越界")
        slots.add(slot)
        items[slot] = deepcopy(item)
    return items


def convert_item_components(source: n.Compound, target: n.Compound,
                            convert_nested: Callable[[n.Compound], n.Compound]) -> ItemComponentConversion:
    """target 必须已做原版 DFU；Sophisticated 组件交给作者现有迁移接口。"""
    data = deepcopy(target)
    components = data.setdefault("components", n.Compound())
    removed = {}

    def put(key, value):
        if key in components:
            raise ValueError(f"组件冲突：{key}")
        components[key] = value

    caps = source.get("ForgeCaps", n.Compound())
    if not isinstance(caps, n.Compound):
        raise ValueError("物品 ForgeCaps 不是复合标签")
    if caps:
        empty_drink_cap = (str(source.get("id")) == "kaleidoscope_tavern:wine" and
                           set(caps) == {"creategearsandtavern:drink_fluid"} and
                           isinstance(caps["creategearsandtavern:drink_fluid"], n.Compound) and
                           not caps["creategearsandtavern:drink_fluid"])
        if empty_drink_cap:
            removed["creategearsandtavern:drink_fluid"] = deepcopy(caps["creategearsandtavern:drink_fluid"])
        elif str(source.get("id")) != "supplementaries:quiver" or set(caps) != {"Parent"}:
            raise ValueError("未覆盖的物品 ForgeCaps")
        else:
            value = caps["Parent"]
            if not isinstance(value, n.Compound) or set(value) != {"Size", "Items", "SelectedSlot"}:
                raise ValueError("箭袋 Parent 结构不符")
            inv = n.Compound({"Size": value["Size"], "Items": value["Items"]})
            selected = int(value["SelectedSlot"])
            if not 0 <= selected < int(value["Size"]):
                raise ValueError("箭袋选中槽位越界")
            items = n.List[n.Compound]()
            for old in inventory_items(inv):
                item = convert_nested(old) if old else n.Compound()
                item.pop("Slot", None)
                items.append(item)
            put("supplementaries:quiver_content", n.Compound({"items": items, "selected_slot": n.Int(selected)}))
    if "ForgeCaps" in source:
        if not caps:
            removed["ForgeCaps"] = deepcopy(caps)
        if "ForgeCaps" in data:
            if data["ForgeCaps"] != caps:
                raise ValueError("DFU 后物品能力与原数据不一致")
            del data["ForgeCaps"]
        custom = components.get("minecraft:custom_data", n.Compound())
        if "ForgeCaps" in custom:
            if custom["ForgeCaps"] != caps:
                raise ValueError("DFU 后物品能力与原数据不一致")
            del custom["ForgeCaps"]

    if "minecraft:custom_data" in components and not components["minecraft:custom_data"]:
        del components["minecraft:custom_data"]
    if not components:
        data.pop("components", None)
    return ItemComponentConversion(data, removed)


def authoritative_count(source: n.Compound, *, oversized_inventory: bool = False) -> int:
    """realCount 只在已识别的 Sophisticated 库存中使用，不做字节猜测。"""
    if "realCount" in source:
        if not oversized_inventory or not isinstance(source["realCount"], n.Int):
            raise ValueError("realCount 出现在未经确认的库存或类型不符")
        count = int(source["realCount"])
    else:
        count = int(source["Count"])
    if not 0 < count <= 2147483647:
        raise ValueError("旧物品栈实际数量无效")
    return count
