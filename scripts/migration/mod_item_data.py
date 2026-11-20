"""实际旧库存中照片、酒品质和料理记录的原生物品组件。"""
from __future__ import annotations

from copy import deepcopy
from typing import Callable

import nbtlib as n

from .item_components import ItemComponentConversion


def convert_stored_item_data(source: n.Compound, target: n.Compound,
                             convert_item: Callable[[n.Compound], n.Compound],
                             convert_entity: Callable[[n.Compound], n.Compound]) -> ItemComponentConversion:
    id = str(source.get("id", ""))
    tag = source.get("tag", n.Compound())
    if str(target.get("id", "")) != id or not isinstance(tag, n.Compound):
        raise ValueError("物品注册名或原标签不符")
    result = deepcopy(target)
    components = result.setdefault("components", n.Compound())
    custom = components.get("minecraft:custom_data", n.Compound())

    def move(old, key, value):
        if key in components:
            raise ValueError(f"物品组件冲突：{key}")
        if custom.get(old) != tag[old]:
            raise ValueError("原版 DFU 后物品持久数据与输入不符")
        components[key] = value
        del custom[old]

    if id == "touhou_little_maid:film" and "MaidInfo" in tag:
        old = tag["MaidInfo"]
        if not isinstance(old, n.Compound) or str(old.get("id", "")) != "touhou_little_maid:maid":
            raise ValueError("女仆照片没有已审计的实体类型")
        move("MaidInfo", "touhou_little_maid:maid_info", convert_entity(deepcopy(old)))
    if id.startswith("kaleidoscope_tavern:") and "BrewLevel" in tag:
        if not isinstance(tag["BrewLevel"], n.Int):
            raise ValueError("酒品质不是原生 Int")
        move("BrewLevel", "kaleidoscope_tavern:brew_level", deepcopy(tag["BrewLevel"]))
    if id == "kaleidoscope_cookery:recipe_item" and "RecipeRecord" in tag:
        old = tag["RecipeRecord"]
        if not isinstance(old, n.Compound) or set(old) != {"input", "output", "type", "flexRecipe"}:
            raise ValueError("料理记录含未覆盖字段")
        if (not isinstance(old["input"], n.List[n.Compound]) or not isinstance(old["output"], n.Compound) or
            not isinstance(old["type"], n.String) or not isinstance(old["flexRecipe"], n.Byte) or
            int(old["flexRecipe"]) not in (0, 1)):
            raise ValueError("料理记录类型不符")
        record = n.Compound({"input": n.List[n.Compound](convert_item(deepcopy(value)) for value in old["input"]),
                             "output": convert_item(deepcopy(old["output"])), "type": deepcopy(old["type"]),
                             "flex_recipe": deepcopy(old["flexRecipe"])})
        if not record["output"] or int(record["output"].get("count", 0)) <= 0:
            raise ValueError("料理记录产物未完整转换")
        move("RecipeRecord", "kaleidoscope_cookery:recipe_record", record)
    if "minecraft:custom_data" in components and not custom:
        del components["minecraft:custom_data"]
    if not components:
        result.pop("components", None)
    return ItemComponentConversion(result, {})
