"""唯一旧档已核对的官方升级注册名，目标存在性仍由运行注册表预检。"""
from __future__ import annotations

from copy import deepcopy

import nbtlib as n

from .item_components import ItemComponentConversion


ITEM_IDS = {
    "minecraft:grass": "minecraft:short_grass",
    "brewincompatdelight:juniper_berries": "cocktailsdelight:juniper_berries",
    "brewincompatdelight:white_grape_seeds": "cocktailsdelight:white_grape_seeds",
    "brewincompatdelight:white_grapes": "cocktailsdelight:white_grapes",
    **{f"miners_delight:{name}": f"minersdelight:{name}" for name in
       ("bat_wing", "cave_carrot", "copper_carrot", "gossypium", "tentacles")},
    **{f"expatternprovider:{name}": f"extendedae:{name}" for name in
       ("ex_charger", "ex_drive", "ex_inscriber", "circuit_cutter", "silicon_block", "ex_import_bus_part")},
}
BLOCK_IDS = {
    "minecraft:grass": "minecraft:short_grass",
    "brewincompatdelight:wild_red_grapes": "cocktailsdelight:wild_red_grapes",
    "brewincompatdelight:wild_white_grape": "cocktailsdelight:wild_white_grapes",
    "miners_delight:wild_cave_carrots": "minersdelight:wild_cave_carrots",
    **{f"expatternprovider:{name}": f"extendedae:{name}" for name in
       ("ex_charger", "ex_drive", "ex_inscriber", "circuit_cutter")},
}


def mapped_id(source: str, target_ids: set[str], *, block: bool = False) -> str:
    """只使用明确映射或原 ID；未知内容、无限元件的可配置载荷不能猜测。"""
    target = (BLOCK_IDS if block else ITEM_IDS).get(source, source)
    if target not in target_ids:
        raise ValueError(f"保留资产没有已核对的目标注册名：{source}")
    return target


def convert_infinity_cell(source: n.Compound, target: n.Compound, target_ids: set[str]) -> ItemComponentConversion:
    """源唯一无限元件为圆石，映射到作者专用变体并核对整个配置。"""
    id = "extendedae:infinity_cobblestone_cell"
    if str(source.get("id", "")) != "expatternprovider:infinity_cell" or id not in target_ids:
        raise ValueError("无限元件原注册名或目标变体不符")
    # 实际字段名及载荷在调用前完成只读提取；禁止默认选择圆石或忽略其它记录。
    expected = n.parse_nbt('{record:{"#c":"ae2:i",id:"minecraft:cobblestone"}}')
    if source.get("tag") != expected:
        raise ValueError("无限元件配置不是唯一旧档已确认的圆石记录")
    result = deepcopy(target)
    result["id"] = n.String(id)
    components = result.get("components", n.Compound())
    custom = components.get("minecraft:custom_data", n.Compound())
    if custom != expected:
        raise ValueError("原版 DFU 后无限元件配置与输入不符")
    del components["minecraft:custom_data"]
    if not components:
        result.pop("components", None)
    return ItemComponentConversion(result, {"record": deepcopy(source["tag"]["record"])})
