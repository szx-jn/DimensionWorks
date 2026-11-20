"""实际旧库存中照片、酒品质和料理记录的原生物品组件。"""
from __future__ import annotations

from copy import deepcopy
from typing import Callable
import re

import nbtlib as n

from .item_components import ItemComponentConversion


def convert_stored_item_data(source: n.Compound, target: n.Compound,
                             convert_item: Callable[[n.Compound], n.Compound],
                             convert_entity: Callable[[n.Compound], n.Compound],
                             convert_fluid: Callable[[n.Compound], n.Compound] | None = None) -> ItemComponentConversion:
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

    if id in {"touhou_little_maid:film", "touhou_little_maid:smart_slab_has_maid"} and "MaidInfo" in tag:
        old = tag["MaidInfo"]
        if not isinstance(old, n.Compound):
            raise ValueError("女仆照片没有已审计的实体类型")
        actor = deepcopy(old)
        missing_id = "id" not in actor
        if id == "touhou_little_maid:smart_slab_has_maid" and "id" not in actor:
            if not isinstance(actor.get("UUID"), n.IntArray) or len(actor["UUID"]) != 4:
                raise ValueError("女仆石板没有已审计的实体 UUID")
            actor["id"] = n.String("touhou_little_maid:maid")
        if str(actor.get("id", "")) != "touhou_little_maid:maid":
            raise ValueError("女仆照片没有已审计的实体类型")
        converted_actor = convert_entity(actor)
        if missing_id:
            # 石板保存的是固定女仆类型的无 id 数据；补 id 仅用于原版 DFU 上下文。
            converted_actor.pop("id", None)
        move("MaidInfo", "touhou_little_maid:maid_info", converted_actor)
    if id == "touhou_little_maid:smart_slab_init" and "InitMaidOwner" in tag:
        if not isinstance(tag["InitMaidOwner"], n.IntArray) or len(tag["InitMaidOwner"]) != 4:
            raise ValueError("女仆初始石板所有者不是原生 UUID")
        move("InitMaidOwner", "touhou_little_maid:init_maid_owner", deepcopy(tag["InitMaidOwner"]))
    if id == "touhou_little_maid:tank_backpack" and "Tanks" in tag:
        if not isinstance(tag["Tanks"], n.Compound) or convert_fluid is None:
            raise ValueError("女仆流体背包缺少完整流体转换")
        move("Tanks", "touhou_little_maid:tanks", convert_fluid(deepcopy(tag["Tanks"])))
    if id in {f"touhou_little_maid:{name}_board_state" for name in ("cchess", "wchess", "gomoku")}:
        fields = {"BoardStateData": "data", "BoardStateDesc": "description", "BoardStateAuthor": "author"}
        if set(fields) & set(tag):
            if not set(fields) <= set(tag) or any(not isinstance(tag[key], n.String) for key in fields):
                raise ValueError("棋谱没有完整字符串数据、说明和作者")
            key = "touhou_little_maid:board_state"
            if key in components or any(custom.get(old) != tag[old] for old in fields):
                raise ValueError("棋谱组件或原版 DFU 数据冲突")
            components[key] = n.Compound({new: deepcopy(tag[old]) for old, new in fields.items()})
            for old in fields:
                del custom[old]
    if id == "patchouli:guide_book" and "patchouli:book" in tag:
        book = tag["patchouli:book"]
        if not isinstance(book, n.String) or not re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_./-]+", str(book)):
            raise ValueError("Patchouli 手册注册名不符")
        move("patchouli:book", "patchouli:book", deepcopy(book))
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
