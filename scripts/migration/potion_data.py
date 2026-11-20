"""唯一旧档的药水预设转换；物品、已有自定义效果和源注册名全部保留。"""
from __future__ import annotations

from copy import deepcopy

import nbtlib as n

from .item_components import ItemComponentConversion


# 原固定 Forge 资产与目标作者资产的注册、效果、时长及等级已逐项核对。
EFFECT_IDS = {
    "l2complements:flame": "l2complements:soul_burning",
    "l2complements:frozen": "l2complements:frost",
    "l2complements:armor_reduce": "l2complements:armor_corrosion",
    "l2complements:stone_cage": "l2complements:incarceration",
}
POTION_PRESETS = {
    "miners_delight:mining_fatigue": ("minersdelight:mining_fatigue", "minecraft:mining_fatigue", 3600, 0),
    "miners_delight:long_mining_fatigue": ("minersdelight:long_mining_fatigue", "minecraft:mining_fatigue", 9600, 0),
    "miners_delight:strong_mining_fatigue": ("minersdelight:strong_mining_fatigue", "minecraft:mining_fatigue", 1800, 1),
    "l2complements:flame": ("l2complements:soul_burning", "l2complements:soul_burning", 600, 0),
    "l2complements:long_flame": ("l2complements:long_soul_burning", "l2complements:soul_burning", 1000, 0),
    "l2complements:strong_flame": ("l2complements:strong_soul_burning", "l2complements:soul_burning", 400, 1),
    "l2complements:frozen": ("l2complements:frost", "l2complements:frost", 3600, 0),
    "l2complements:long_frozen": ("l2complements:long_frost", "l2complements:frost", 9600, 0),
    "l2complements:armor_reduce": ("l2complements:armor_corrosion", "l2complements:armor_corrosion", 1200, 0),
    "l2complements:long_armor_reduce": ("l2complements:long_armor_corrosion", "l2complements:armor_corrosion", 3600, 0),
    "l2complements:strong_armor_reduce": ("l2complements:strong_armor_corrosion", "l2complements:armor_corrosion", 600, 1),
    "l2complements:stone_cage": ("l2complements:incarceration", "l2complements:incarceration", 1200, 0),
    "l2complements:long_stone_cage": ("l2complements:long_incarceration", "l2complements:incarceration", 3600, 0),
}
WITHER_PRESETS = {
    "witherstormmod:wither": (900, 0),
    "witherstormmod:long_wither": (1800, 0),
    "witherstormmod:strong_wither": (432, 1),
}


def convert_effect(source: n.Compound, native_ids: set[str]) -> n.Compound:
    result = deepcopy(source)
    if not isinstance(result.get("id"), n.String):
        raise ValueError("药水效果没有已转换的字符串注册名")
    old = str(result["id"])
    id = EFFECT_IDS.get(old, old)
    if id not in native_ids:
        raise ValueError(f"药水效果无法直接升级：{old}")
    result["id"] = n.String(id)
    return result


def convert_active_effects(source: n.Compound, target: n.Compound,
                          native_ids: set[str]) -> ItemComponentConversion:
    """本服实体/玩家效果使用 Forge 保存的名称；不沿用旧模组数值 ID。"""
    result = deepcopy(target)
    if "ActiveEffects" not in source:
        return ItemComponentConversion(result, {})
    if not isinstance(source["ActiveEffects"], n.List) or "active_effects" in source:
        raise ValueError("旧实体效果列表格式或字段冲突")
    effects = n.List[n.Compound]()
    for old in source["ActiveEffects"]:
        fields = {"Id", "forge:id", "Amplifier", "Duration", "Ambient", "ShowParticles", "ShowIcon", "CurativeItems"}
        if (not isinstance(old, n.Compound) or set(old) != fields or not isinstance(old["Id"], n.Int) or
                not isinstance(old["forge:id"], n.String) or not isinstance(old["Amplifier"], n.Byte) or
                not isinstance(old["Duration"], n.Int) or any(not isinstance(old[k], n.Byte) or int(old[k]) not in (0, 1)
                    for k in ("Ambient", "ShowParticles", "ShowIcon"))):
            raise ValueError("实体效果含未覆盖的状态字段或类型")
        if old["CurativeItems"] != n.parse_nbt('[{id:"minecraft:milk_bucket",Count:1b}]'):
            raise ValueError("实体效果治愈物不是唯一旧档已核对的牛奶")
        value = n.Compound({"id": deepcopy(old["forge:id"]), "amplifier": deepcopy(old["Amplifier"]),
                            "duration": deepcopy(old["Duration"]), "ambient": deepcopy(old["Ambient"]),
                            "show_particles": deepcopy(old["ShowParticles"]), "show_icon": deepcopy(old["ShowIcon"]),
                            # 旧牛奶治愈与原版图腾清除行为对应固定 NeoForge 原生 cures。
                            "neoforge:cures": n.List[n.String](["milk", "protected_by_totem"])})
        effects.append(convert_effect(value, native_ids))
    if "ActiveEffects" in result or "active_effects" in result:
        raise ValueError("实体效果须从剥离旧效果的独立 DFU 结果接入，不能覆盖未知列表")
    result["active_effects"] = effects
    return ItemComponentConversion(result, {"original_active_effects": deepcopy(source["ActiveEffects"])})


def convert_potion_contents(target: n.Compound, native_potions: set[str], native_effects: set[str],
                            catalog: n.Compound | None) -> ItemComponentConversion:
    result = deepcopy(target)
    components = result.get("components", {})
    component = components.get("minecraft:potion_contents")
    if component is None:
        return ItemComponentConversion(result, {})
    if not isinstance(component, n.Compound):
        raise ValueError("药水组件不是 Compound")
    removed = {}
    effects = component.get("custom_effects", n.List[n.Compound]())
    if not isinstance(effects, n.List):
        raise ValueError("自定义药水效果不是列表")
    converted = n.List[n.Compound]([convert_effect(effect, native_effects) for effect in effects])
    if "custom_effects" in component:
        component["custom_effects"] = converted
    old = component.get("potion")
    if old is None:
        return ItemComponentConversion(result, removed)
    if not isinstance(old, n.String):
        raise ValueError("药水预设不是字符串注册名")
    old = str(old)
    if old in POTION_PRESETS:
        id, effect, duration, amplifier = POTION_PRESETS[old]
        if id not in native_potions or catalog is None or id not in catalog:
            raise ValueError("药水更名缺少固定目标的原生预设证据")
        preset = catalog[id]
        if (len(preset) != 1 or str(preset[0].get("id")) != effect or
                int(preset[0].get("duration", 0)) != duration or int(preset[0].get("amplifier", 0)) != amplifier):
            raise ValueError("目标药水预设的效果、时长或等级与旧档不一致")
        component["potion"] = n.String(id)
        removed["original_potion"] = n.String(old)
    elif old in WITHER_PRESETS:
        if "minecraft:wither" not in native_effects:
            raise ValueError("原版凋零效果没有目标注册名")
        duration, amplifier = WITHER_PRESETS[old]
        effect = n.Compound({"id": n.String("minecraft:wither"), "duration": n.Int(duration), "show_icon": n.Byte(1)})
        if amplifier:
            effect["amplifier"] = n.Byte(amplifier)
        # 旧预设先应用，然后应用原有 custom_effects，与原生 PotionContents 的顺序一致。
        component["custom_effects"] = n.List[n.Compound]([effect, *converted])
        del component["potion"]
        custom = components.setdefault("minecraft:custom_data", n.Compound())
        archive = custom.setdefault("dimensionworks_migration", n.Compound())
        if "original_potion" in archive:
            raise ValueError("药水迁移源记录冲突")
        archive["original_potion"] = n.String(old)
        removed["original_potion"] = n.String(old)
    elif old not in native_potions:
        raise ValueError(f"药水预设无法直接升级：{old}")
    return ItemComponentConversion(result, removed)
