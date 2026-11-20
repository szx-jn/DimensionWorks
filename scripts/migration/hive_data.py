"""原版蜂巢 DFU 的重复路径清理；以独立实体转换后的原始载荷为准。"""
from copy import deepcopy

import nbtlib as n

from .item_components import ItemComponentConversion


def normalize_hive_fields(source: n.Compound) -> ItemComponentConversion:
    data = deepcopy(source)
    removed = {}
    if str(data.get("id", "")) != "minecraft:beehive":
        return ItemComponentConversion(data, removed)
    if "Bees" in data:
        old = data["Bees"]
        current = data.get("bees", n.List[n.Compound]())
        if not isinstance(old, n.List) or not isinstance(current, n.List) or len(old) != len(current):
            raise ValueError("蜂巢原版转换改变了内嵌生物数量")
        flower_caches = n.List[n.Compound]()
        for i, (previous, target) in enumerate(zip(old, current)):
            if set(previous) - {"EntityData", "MinOccupationTicks", "TicksInHive"} or "EntityData" not in previous:
                raise ValueError("蜂巢旧入巢条目含未覆盖字段")
            if set(target) - {"entity_data", "min_ticks_in_hive", "ticks_in_hive"} or "entity_data" not in target:
                raise ValueError("蜂巢目标入巢条目含未覆盖字段")
            for a, b in (("MinOccupationTicks", "min_ticks_in_hive"), ("TicksInHive", "ticks_in_hive")):
                if previous.get(a) != target.get(b):
                    raise ValueError("蜂巢入巢计时改变")
            expected = deepcopy(previous["EntityData"])
            # 1.21.1 的蜂巢 DFU 清理生物的旧 FlowerPos 导航缓存；不是物品、身份或机器连接。
            if "FlowerPos" in expected and "FlowerPos" not in target["entity_data"]:
                value = expected.pop("FlowerPos")
                if not isinstance(value, n.Compound) or set(value) != {"X", "Y", "Z"} or any(not isinstance(v, n.Int) for v in value.values()):
                    raise ValueError("蜂巢生物旧花朵导航缓存结构未覆盖")
                flower_caches.append(n.Compound({"index": n.Int(i), "FlowerPos": value}))
            if set(expected) != set(target["entity_data"]):
                raise ValueError("蜂巢两个实体路径的持久字段集合不一致")
            # 父级 DFU 可能已局部改写实体；恢复由原始实体独立转换得到的全部状态。
            target["entity_data"] = expected
        removed["Bees"] = data.pop("Bees")
        if flower_caches:
            removed["bee_flower_caches"] = flower_caches
    if "FlowerPos" in data:
        old, new = data["FlowerPos"], data.get("flower_pos")
        if (not isinstance(old, n.Compound) or set(old) != {"X", "Y", "Z"} or
            any(not isinstance(v, n.Int) for v in old.values()) or not isinstance(new, n.IntArray) or
            new.tolist() != [int(old[k]) for k in ("X", "Y", "Z")]):
            raise ValueError("蜂巢目标花朵位置改变或未转换")
        removed["FlowerPos"] = data.pop("FlowerPos")
    return ItemComponentConversion(data, removed)
