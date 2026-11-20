"""完整转换唯一旧档的移动机械载荷；保留位置、方块实体、执行器和存储拓扑。"""
from copy import deepcopy
from typing import Callable

import nbtlib as n


def convert_contraption(source: n.Compound, convert_state: Callable, convert_block_entity: Callable,
                        convert_tree: Callable) -> n.Compound:
    if (not isinstance(source, n.Compound) or str(source.get("Type", "")) not in {"create:mounted", "create:bearing"} or
        not isinstance(source.get("Blocks"), n.Compound) or set(source["Blocks"]) != {"Palette", "BlockList"}):
        raise ValueError("移动机械类型或完整方块目录与唯一旧档不符")
    result = deepcopy(source)
    palette = source["Blocks"]["Palette"]
    if not isinstance(palette, n.List) or not palette:
        raise ValueError("移动机械没有完整方块状态目录")
    result["Blocks"]["Palette"] = n.List[n.Compound]([convert_state(state) for state in palette])
    positions = set()
    blocks = n.List[n.Compound]()
    for old in source["Blocks"]["BlockList"]:
        if (set(old) - {"Pos", "State", "Data", "UpdateTag"} or not isinstance(old.get("Pos"), n.Long) or
            not isinstance(old.get("State"), n.Int) or not 0 <= int(old["State"]) < len(palette) or int(old["Pos"]) in positions):
            raise ValueError("移动机械方块位置重复、状态索引错误或字段未覆盖")
        positions.add(int(old["Pos"]))
        block = deepcopy(old)
        for key in ("Data", "UpdateTag"):
            if key in old:
                block[key] = convert_block_entity(deepcopy(old[key]))
                if not isinstance(block[key], n.Compound):
                    raise ValueError("移动机械方块实体数据未完整转换")
        blocks.append(block)
    result["Blocks"]["BlockList"] = blocks
    # 根内的 mounted storage、Actors、Passengers、CapturedMultiblocks 等均须递归转换。
    # 不过滤列表、不移动槽位，也不以重建一个空装置替代原载荷。
    for key, value in source.items():
        if key != "Blocks":
            result[key] = convert_tree(deepcopy(value))
    return result


def block_layout(source: n.Compound) -> dict[int, n.Compound]:
    palette = source["Blocks"]["Palette"]
    result = {}
    for block in source["Blocks"]["BlockList"]:
        position = int(block["Pos"])
        if position in result:
            raise ValueError("移动机械方块位置重复")
        value = n.Compound({"state": deepcopy(palette[int(block["State"])])})
        if "Data" in block:
            value["data"] = deepcopy(block["Data"])
        result[position] = value
    return result


def _comparison_positions(value):
    # 固定 Create 原生写回将旧 BlockPos {X,Y,Z} 改为 IntArray。
    # 只用于对账，不修改实际载荷；坐标值仍须完全一致。
    if isinstance(value, n.Compound):
        if set(value) == {"X", "Y", "Z"} and all(isinstance(value[k], n.Int) for k in ("X", "Y", "Z")):
            return n.IntArray([int(value[k]) for k in ("X", "Y", "Z")])
        return n.Compound({k: _comparison_positions(v) for k, v in value.items()})
    if isinstance(value, n.List):
        return type(value)(_comparison_positions(v) for v in value)
    return deepcopy(value)


def _actors_by_position(source: n.Compound) -> dict[int, n.Compound]:
    result = {}
    for actor in source.get("Actors", []):
        pos = _comparison_positions(actor["Pos"])
        if not isinstance(pos, n.IntArray) or len(pos) != 3:
            raise ValueError("移动机械执行器坐标格式未覆盖")
        x, y, z = map(int, pos)
        packed = ((x & 0x3ffffff) << 38) | ((z & 0x3ffffff) << 12) | (y & 0xfff)
        if packed >= 1 << 63:
            packed -= 1 << 64
        if packed in result:
            raise ValueError("移动机械执行器位置重复")
        result[packed] = actor
    return result


def assert_contraption_preserved(prepared: n.Compound, native: n.Compound) -> n.List:
    """核对完整持久载荷，返回作者省略的已证明空/重复旧库存供私有台账归档。"""
    before = block_layout(prepared)
    after = block_layout(native)
    if set(before) != set(after):
        raise ValueError("移动机械方块位置丢失或改变")
    source_actors = _actors_by_position(prepared)
    target_actors = _actors_by_position(native)
    omitted = n.List[n.Compound]()
    for pos, old in before.items():
        new = after[pos]
        inventory = old.get("data", {}).get("Inventory")
        if inventory is not None and "Inventory" not in new.get("data", {}):
            if str(old["state"]["Name"]) != "create:deployer" or not isinstance(inventory, n.List):
                raise ValueError("移动机械原生结果省略了未覆盖的库存")
            if inventory:
                if len(inventory) != 1 or int(inventory[0].get("Slot", -1)) != 0:
                    raise ValueError("执行器旧库存不是单一手持槽，不能省略")
                stack = n.Compound({k: deepcopy(v) for k, v in inventory[0].items() if k != "Slot"})
                old_hand = source_actors.get(pos, {}).get("Data", {}).get("HeldItem")
                new_hand = target_actors.get(pos, {}).get("Data", {}).get("HeldItem")
                if stack != old_hand or stack != new_hand:
                    raise ValueError("执行器旧库存与同位置原生手持物品不一致")
            omitted.append(n.Compound({"position": n.Long(pos), "inventory": deepcopy(inventory),
                                       "reason": n.String("actor_hand_duplicate" if inventory else "empty_legacy_inventory")}))
            old["data"].pop("Inventory")
        if _comparison_positions(old) != _comparison_positions(new):
            raise ValueError("移动机械方块状态或完整持久数据改变")
    old_root = n.Compound({k: deepcopy(v) for k, v in prepared.items() if k != "Blocks"})
    new_root = n.Compound({k: deepcopy(v) for k, v in native.items() if k != "Blocks"})
    if _comparison_positions(old_root) != _comparison_positions(new_root):
        raise ValueError("移动机械锚点、执行器、库存或其它根状态改变")
    return omitted
