"""唯一旧档中 Create 过滤器/包裹的库存组件转换，不写世界。"""
from __future__ import annotations

from copy import deepcopy
from typing import Callable

import nbtlib as n

from .item_components import ItemComponentConversion, inventory_items


PACKAGE_IDS = {f"create:cardboard_package_{size}" for size in ("10x8", "10x12", "12x10", "12x12")}
VANILLA_FIELDS = {"display", "Damage", "Enchantments", "RepairCost", "Unbreakable", "HideFlags"}


def convert_container_item(source: n.Compound, target: n.Compound,
                           convert_nested: Callable[[n.Compound], n.Compound]) -> ItemComponentConversion:
    id = str(source.get("id", ""))
    if id not in PACKAGE_IDS | {"create:filter"} or str(target.get("id", "")) != id:
        raise ValueError("输入不是旧档已审计的 Create 容器物品")
    tag = source.get("tag", n.Compound())
    fields = {"Items", "Blacklist", "RespectNBT"} if id == "create:filter" else {"Items", "Address"}
    if not isinstance(tag, n.Compound) or set(tag) - fields - VANILLA_FIELDS:
        raise ValueError("Create 容器物品含未覆盖字段")
    result = deepcopy(target)
    if not set(tag) & fields:
        return ItemComponentConversion(result, {})
    components = result.setdefault("components", n.Compound())
    custom = components.get("minecraft:custom_data", n.Compound())

    def put(key, value):
        if key in components:
            raise ValueError(f"Create 物品组件冲突：{key}")
        components[key] = value

    if "Items" in tag:
        size = 18 if id == "create:filter" else 9
        inventory = tag["Items"]
        if not isinstance(inventory, n.Compound) or int(inventory.get("Size", -1)) != size:
            raise ValueError("Create 容器容量与目标原生接口不符")
        entries = n.List[n.Compound]()
        for slot, old in enumerate(inventory_items(inventory)):
            if not old:
                continue
            converted = convert_nested(old)
            if not converted or int(converted.get("count", 0)) <= 0:
                raise ValueError("Create 容器中的非空物品未完整转换")
            converted = deepcopy(converted)
            converted.pop("Slot", None)
            entries.append(n.Compound({"slot": n.Int(slot), "item": converted}))
        put("create:filter_items" if id == "create:filter" else "create:package_contents", entries)
    for old, new in [("Blacklist", "create:filter_items_blacklist"), ("RespectNBT", "create:filter_items_respect_nbt")]:
        if old in tag:
            value = tag[old]
            if not isinstance(value, n.Byte) or int(value) not in (0, 1):
                raise ValueError("Create 过滤器布尔值不符")
            put(new, deepcopy(value))
    if "Address" in tag:
        if not isinstance(tag["Address"], n.String):
            raise ValueError("Create 包裹地址不是字符串")
        put("create:package_address", deepcopy(tag["Address"]))
    for field in set(tag) & fields:
        if custom.get(field) != tag[field]:
            raise ValueError("原版 DFU 后 Create 容器数据与输入不符")
        del custom[field]
    if "minecraft:custom_data" in components and not custom:
        del components["minecraft:custom_data"]
    return ItemComponentConversion(result, {})
