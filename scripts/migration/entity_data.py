"""唯一服务器旧档的 Forge 实体持久化数据 → NeoForge 附件转换。

只处理离线 NBT；物品 ForgeCaps/Parent 必须先由箭袋组件转换处理。
本模块不代表完整世界、物品组件或 DataVersion 已转换。
"""
from __future__ import annotations

from copy import deepcopy
from dataclasses import dataclass

import nbtlib as n


AETHER_PLAYER_FIELDS = {
    "CanGetPortal": "can_get_portal", "CanShowPatreonMessage": "show_patreon_message",
    "CanSpawnInAether": "can_spawn_in_aether", "HasSeenSunSpirit": "seen_sun_spirit",
    "LifeShardCount": "life_shard_count", "LoginsUntilPatreonMessage": "logins_until_patreon_message",
    "RemedyStartDuration": "remedy_start_duration", "SavedHealth": "saved_health",
}
RETIRED_CAPABILITIES = {
    "witherstormmod:wither_storm_data", "witherstormmod:wither_sickness_tracker",
}
# 新版暮色不序列化这两个临时控制附件；不用旧计时器覆盖新实例状态。
TRANSIENT_CAPABILITIES = {
    "twilightforest:cap_thrown", "twilightforest:giant_pick_mine",
}
UNCHANGED_CAPABILITIES = {
    "curios:inventory", "brewinandchewin:raging", "l2complements:soulbound",
    "aether:dropped_item", "create:minecart_controller",
}
ATTRIBUTE_NAMES = {
    "forge:entity_gravity": "minecraft:generic.gravity",
    "forge:swim_speed": "neoforge:swim_speed",
    "forge:entity_reach": "minecraft:player.entity_interaction_range",
    "forge:block_reach": "minecraft:player.block_interaction_range",
}


@dataclass
class EntityDataConversion:
    data: n.Compound
    removed: dict[str, n.Base]


def rename_fields(value: n.Base, names: dict[str, str]) -> n.Compound:
    if not isinstance(value, n.Compound) or set(value) != set(names):
        raise ValueError(f"旧附件字段与已审计结构不符：期望 {sorted(names)}")
    return n.Compound({new: deepcopy(value[old]) for old, new in names.items()})


def convert_payload(id: str, value: n.Base) -> tuple[str, n.Base]:
    if id in UNCHANGED_CAPABILITIES:
        return id, deepcopy(value)
    if id == "aether:aether_player":
        return id, rename_fields(value, AETHER_PLAYER_FIELDS)
    if id == "aether:phoenix_arrow":
        return id, rename_fields(value, {"FireTime": "fire_time", "PhoenixArrow": "is_phoenix_arrow"})
    if id == "aether:mob_accessory":
        result = rename_fields(value, {"DropChances": "drop_chances"})
        chances = result["drop_chances"]
        if not isinstance(chances, n.Compound):
            raise ValueError("Aether DropChances 不是复合标签")
        slots = {"aether_gloves": "aether:gloves_slot", "aether_pendant": "aether:pendant_slot",
                 "hands": "hand", "necklace": "necklace"}
        if set(chances) - set(slots):
            raise ValueError("Aether 存在未映射的生物饰品掉落槽位")
        result["drop_chances"] = n.Compound({slots[key]: item for key, item in chances.items()})
        return id, result
    if id in {"touhou_little_maid:maid_num", "touhou_little_maid:chat_tokens"}:
        if not isinstance(value, n.Int):
            raise ValueError(f"{id} 旧值不是 Int")
        return id, n.Compound({"num": deepcopy(value)})
    if id == "touhou_little_maid:power":
        if not isinstance(value, n.Float):
            raise ValueError("女仆 power 旧值不是 Float")
        return id, n.Compound({"power": deepcopy(value)})
    if id == "brewinandchewin:tipsy_numbed_hearts":
        return "brewinandchewin:tipsy_hearts", rename_fields(value, {
            "numbed_health": "numbed_health", "ticks_until_damage": "ticks_until_damage",
        })
    if id == "mekanism:radiation":
        result = rename_fields(value, {"radiation": "radiation"})["radiation"]
        if not isinstance(result, n.Double):
            raise ValueError("Mekanism radiation 旧值不是 Double")
        return id, result
    if id == "twilightforest:cap_shield":
        return "twilightforest:fortification_shields", rename_fields(value, {
            "permshields": "permanent_shields", "tempshields": "temporary_shields",
        })
    if id == "twilightforest:cap_feather_fan_fall":
        result = rename_fields(value, {"featherFanFalling": "featherFanFalling"})
        return "twilightforest:feather_fan_falling", result["featherFanFalling"]
    if id == "supplementaries:antique_ink":
        return id, rename_fields(value, {"ink": "ink"})["ink"]
    if id == "l2screentracker:screen_tracker":
        if not isinstance(value, n.Compound) or value:
            raise ValueError("旧 screen_tracker 不是已审计的空状态")
        return "l2menustacker:screen_tracker", n.Compound()
    if id == "l2library:conditionals":
        renamed = rename_fields(value, {
            "data": "data", "tickSinceDeath": "tickSinceDeath",
        })
        return "l2core:conditionals", n.Compound({"data": renamed["data"]})
    raise ValueError(f"没有旧能力转换规则：{id}")


def convert_entity_data(source: n.Compound) -> EntityDataConversion:
    data = deepcopy(source)
    removed = {}
    # attributes 的字段结构由原版 DFU 处理；这里只映射实际旧 Forge ID。
    if "attributes" in data:
        attributes = n.List[n.Compound]()
        seen = set()
        for value in data["attributes"]:
            if not isinstance(value, n.Compound) or "id" not in value:
                raise ValueError("原版 DFU 后的属性条目结构错误")
            id = str(value["id"])
            if id == "forge:step_height_addition":
                # 全档 16,567 条均为此结构。旧增量为零，新实体使用原生基值。
                if set(value) != {"id", "base"} or not isinstance(value["base"], n.Double) or float(value["base"]) != 0:
                    raise ValueError("台阶增量属性与唯一旧档已审计的零状态不符")
                removed[id] = deepcopy(value)
                continue
            target = ATTRIBUTE_NAMES.get(id, id)
            if target in seen:
                raise ValueError(f"属性 ID 映射冲突：{target}")
            seen.add(target)
            converted = deepcopy(value)
            converted["id"] = n.String(target)
            attributes.append(converted)
        data["attributes"] = attributes
    if "ForgeData" in data:
        if "NeoForgeData" in data or not isinstance(data["ForgeData"], n.Compound):
            raise ValueError("持久化数据字段冲突或 ForgeData 类型错误")
        data["NeoForgeData"] = data.pop("ForgeData")
    if "ForgeCaps" not in data:
        return EntityDataConversion(data, removed)
    if "neoforge:attachments" in data or not isinstance(data["ForgeCaps"], n.Compound):
        raise ValueError("附件字段冲突或 ForgeCaps 类型错误")
    attachments = n.Compound()
    for id, value in data.pop("ForgeCaps").items():
        if id in RETIRED_CAPABILITIES | TRANSIENT_CAPABILITIES:
            removed[id] = value
            continue
        target, converted = convert_payload(id, value)
        if id == "l2library:conditionals":
            removed[id + "/tickSinceDeath"] = deepcopy(value["tickSinceDeath"])
        if target in attachments:
            raise ValueError(f"多个旧能力映射到同一附件：{target}")
        attachments[target] = converted
    if attachments:
        data["neoforge:attachments"] = attachments
    return EntityDataConversion(data, removed)
