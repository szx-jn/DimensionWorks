"""根据固定目标的原生属性目录转换旧方块状态，未知变化不得静默采用默认值。"""
from __future__ import annotations

from copy import deepcopy
from dataclasses import dataclass
import re

import nbtlib as n

from .registry_ids import BLOCK_IDS


@dataclass
class StateConversion:
    data: n.Compound
    removed: dict[str, n.Base]


# 唯一封存世界中已核对的作者变化；不接受有水的状态或任意其它属性丢弃。
REMOVED_VALUES = {
    ("twilightforest:aurora_block", "variant"): {str(value) for value in range(16)},
    ("twilightforest:firefly", "waterlogged"): {"false"},
    ("twilightforest:cicada", "waterlogged"): {"false"},
    ("mynethersdelight:powdery_cane", "leave"): {"false"},
    ("mynethersdelight:powdery_cane", "base"): {"false"},
}


def convert_state(source: n.Compound, native_schema: n.Compound) -> StateConversion:
    if (not isinstance(source, n.Compound) or set(source) - {"Name", "Properties"} or
        not isinstance(source.get("Name"), n.String) or
        not re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_./-]+", str(source["Name"]))):
        raise ValueError("旧方块状态结构或注册名不符")
    old = str(source["Name"])
    id = BLOCK_IDS.get(old, old)
    if id not in native_schema:
        raise ValueError(f"受保护方块缺少目标状态目录：{old}")
    schema = native_schema[id]
    props = source.get("Properties", n.Compound())
    if not isinstance(props, n.Compound):
        raise ValueError("方块属性不是复合标签")
    data = deepcopy(schema["default_state"])
    if str(data["Name"]) != id:
        raise ValueError("原生状态目录注册名冲突")
    allowed = schema["properties"]
    target = data.get("Properties", n.Compound())
    removed = {}
    if old != id:
        removed["Name"] = deepcopy(source["Name"])
    for name, value in props.items():
        if not isinstance(value, n.String):
            raise ValueError("方块属性值不是原生字符串")
        if name not in allowed:
            if str(value) not in REMOVED_VALUES.get((old, name), set()):
                raise ValueError(f"未覆盖的方块属性移除：{old}/{name}")
            removed[name] = deepcopy(value)
        elif str(value) not in {str(v) for v in allowed[name]}:
            raise ValueError(f"目标不支持该方块属性值：{old}/{name}")
        else:
            target[name] = deepcopy(value)
    if target:
        data["Properties"] = target
    return StateConversion(data, removed)
