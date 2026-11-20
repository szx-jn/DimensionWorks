"""本服方块实体的旧流体载荷；物品内部数据交给独立物品/作者转换。"""
from copy import deepcopy

import nbtlib as n

from .item_components import ItemComponentConversion


def convert_block_fluids(source: n.Compound, native_ids: set[str]) -> ItemComponentConversion:
    removed = n.List[n.Compound]()

    def visit(value, path):
        if isinstance(value, n.Compound):
            if "id" in value and ("Count" in value or "count" in value):
                return deepcopy(value)  # 不改动原始物品缓存键或已验证的组件/归档数据。
            if "FluidName" in value:
                shapes = ({"FluidName", "Amount"}, {"FluidName", "Amount", "In"},
                          {"FluidName", "Amount", "In", "Progress"},
                          {"FluidName", "Amount", "Location", "Pulling"})
                if (set(value) not in shapes or not isinstance(value["FluidName"], n.String) or
                    not isinstance(value["Amount"], n.Int) or int(value["Amount"]) < 0 or
                    str(value["FluidName"]) not in native_ids):
                    raise ValueError("方块流体含未覆盖字段、注册名或数量")
                amount = int(value["Amount"])
                if (amount == 0) != (str(value["FluidName"]) == "minecraft:empty"):
                    raise ValueError("零量具名流体可能是过滤模板，不能当空储罐删除")
                result = n.Compound({k: visit(v, path + "/" + k) for k, v in value.items()
                                     if k not in {"FluidName", "Amount"}})
                fluid = n.Compound()
                if amount:
                    fluid["id"] = deepcopy(value["FluidName"])
                    fluid["amount"] = deepcopy(value["Amount"])
                # NeoForge FluidTank、Create 管内 Flow/OpenEnd 都改为在 Fluid 子标签中编码。
                # 直接 FluidStack（如自研 MoltenExposure[*]/Fluid）仍使用平铺 id/amount。
                field = path.rsplit("/", 1)[-1]
                nested = "In" in value or "Location" in value or field in {"TankContent", "Stomach", "fluid"}
                if nested:
                    if amount or "In" in value:
                        result["Fluid"] = fluid
                else:
                    result.update(fluid)
                removed.append(n.Compound({"path": n.String(path), "legacy_fluid": n.Compound({
                    "FluidName": deepcopy(value["FluidName"]), "Amount": deepcopy(value["Amount"])}),
                    "nested_fluid": n.Byte(nested)}))
                return result
            return n.Compound({k: visit(v, path + "/" + k) for k, v in value.items()})
        if isinstance(value, n.List):
            return type(value)(visit(v, path + f"[{i}]") for i, v in enumerate(value))
        return deepcopy(value)

    data = visit(source, "")
    return ItemComponentConversion(data, {"legacy_fluids": removed} if removed else {})
