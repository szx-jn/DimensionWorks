"""已使用辅助物品的目标原生组件；不把剩余旧 custom_data 当成仍有效的玩法。"""
from copy import deepcopy
from typing import Callable

import nbtlib as n


AUXILIARY_IDS = {"quark:ancient_tome", "quark:pathfinders_quill", "create_enchantment_industry:enchanting_template",
                 "twilightforest:filled_magic_map", "supplementaries:cartographers_quill", "minecraft:clock"}


def convert_auxiliary_item(source: n.Compound, target: n.Compound,
                          convert_enchantments: Callable[[n.List], n.Compound]) -> n.Compound:
    id = str(source["id"])
    tag = source.get("tag", n.Compound())
    result = deepcopy(target)
    if id not in AUXILIARY_IDS or not tag:
        return result
    components = result.setdefault("components", n.Compound())
    custom = components.get("minecraft:custom_data", n.Compound())

    def move(old, new, type, *, transformed=None):
        if old not in tag:
            return
        if (not isinstance(tag[old], type) or new in components or custom.get(old) != tag[old] or
                (type == n.Byte and int(tag[old]) not in (0, 1))):
            raise ValueError("辅助物品旧字段、原生组件或类型冲突")
        components[new] = deepcopy(tag[old]) if transformed is None else transformed
        del custom[old]

    if id in {"quark:ancient_tome", "create_enchantment_industry:enchanting_template"} and "StoredEnchantments" in tag:
        key = "quark:tome_enchantments" if id == "quark:ancient_tome" else "minecraft:stored_enchantments"
        # 原版 DFU 已将 remainder 中的 sweeping 更名为 sweeping_edge；比较转换后的完整等级表。
        old = tag["StoredEnchantments"]
        remaining = custom.get("StoredEnchantments")
        if not isinstance(old, n.List) or not isinstance(remaining, n.List) or key in components:
            raise ValueError("辅助物品附魔旧字段或组件冲突")
        value = convert_enchantments(old)
        if value != convert_enchantments(remaining):
            raise ValueError("原版 DFU 后辅助物品附魔与原始等级表不符")
        components[key] = value
        del custom["StoredEnchantments"]
    elif id == "quark:pathfinders_quill":
        move("targetBiome", "quark:target_biome", n.String)
        move("targetBiomeColor", "quark:target_biome_color", n.Int)
        move("targetBiomeUnderground", "quark:target_biome_underground", n.Byte)
    elif id == "twilightforest:filled_magic_map":
        move("map", "minecraft:map_id", n.Int)
    elif id == "supplementaries:cartographers_quill":
        for old, new, type in [("zoomLevel", "zoom", n.Int), ("skipKnown", "skip_known", n.Byte),
                              ("targetStructure", "structure", n.String), ("decoration", "decoration", n.String),
                              ("maxSearchRadius", "search_radius", n.Int)]:
            move(old, "supplementaries:quill_" + new, type)
    elif id == "minecraft:clock":
        move("quark:clock_calculated", "quark:is_clock_calculated", n.Byte)
    if "minecraft:custom_data" in components and not custom:
        del components["minecraft:custom_data"]
    return result
