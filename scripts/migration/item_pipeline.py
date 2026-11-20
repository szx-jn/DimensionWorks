"""唯一旧档的递归物品转换；作者迁移接口缺少结果时返回明确的批处理需求。"""
from __future__ import annotations

from copy import deepcopy
from typing import Callable

import nbtlib as n

from .ae_data import convert_key, convert_storage_cell, key_identity
from .create_data import PACKAGE_IDS, convert_container_item, convert_mechanical_item
from .entity_data import convert_entity_data
from .item_components import authoritative_count, convert_item_components
from .mod_item_data import convert_stored_item_data
from .potion_data import convert_potion_contents
from .registry_ids import convert_enchantment_components, convert_infinity_cell, mapped_id


def unit_source(source: n.Compound) -> n.Compound:
    """精确缓存元数据；真实数量和槽位在每次调用时独立核对、恢复。"""
    value = deepcopy(source)
    value["Count"] = n.Byte(1)
    value.pop("Slot", None)
    value.pop("realCount", None)
    return value


class NativeItemRequired(Exception):
    def __init__(self, source: n.Compound, prepared: n.Compound, kind: str):
        super().__init__(f"物品需要作者原生迁移：{source['id']} ({kind})")
        self.source, self.prepared, self.kind = deepcopy(source), deepcopy(prepared), kind


class ItemPipeline:
    def __init__(self, dfu, registries: dict[str, list[str]], *,
                 convert_contraption: Callable[[n.Compound], n.Compound] | None = None,
                 potion_effects: n.Compound | None = None):
        self.dfu = dfu
        self.items = set(registries["minecraft:item"])
        self.entities = set(registries["minecraft:entity_type"])
        self.attributes = set(registries["minecraft:attribute"])
        self.fluids = set(registries["minecraft:fluid"])
        self.enchantments = set(registries["minecraft:enchantment"])
        self.potions = set(registries["minecraft:potion"])
        self.effects = set(registries["minecraft:mob_effect"])
        self.potion_effects = potion_effects
        self.convert_contraption = convert_contraption
        self.ready: dict[bytes, n.Compound] = {}
        self.pending: dict[bytes, NativeItemRequired] = {}
        self.changes: list[dict] = []

    def register_native(self, source: n.Compound, result: n.Compound) -> None:
        old = unit_source(source)
        id = str(old["id"])
        expected = ("extendedae:infinity_cobblestone_cell" if id == "expatternprovider:infinity_cell"
                    else mapped_id(id, self.items))
        if str(result.get("id", "")) != expected or int(result.get("count", 1)) != 1:
            raise ValueError("原生物品结果更改了注册名或单位模板数量")
        data = deepcopy(result)
        data["count"] = n.Int(1)
        data.pop("Slot", None)
        identity = key_identity(old)
        if identity in self.ready and self.ready[identity] != data:
            raise ValueError("同一原始元数据有冲突的原生结果")
        self.ready[identity] = data
        self.pending.pop(identity, None)

    def fluid(self, source: n.Compound) -> n.Compound:
        if not source:
            return n.Compound()
        if source == n.Compound({"Empty": n.String("")}):
            self.changes.append({"kind": "empty_fluid_marker", "removed": deepcopy(source)})
            return n.Compound()
        if (set(source) != {"FluidName", "Amount"} or not isinstance(source["FluidName"], n.String) or
            not isinstance(source["Amount"], n.Int) or int(source["Amount"]) <= 0 or
            str(source["FluidName"]) not in self.fluids):
            raise ValueError("流体背包包含未覆盖的注册名、字段或数量")
        return n.Compound({"id": deepcopy(source["FluidName"]), "amount": deepcopy(source["Amount"])})

    @staticmethod
    def _old_items(value) -> bool:
        if isinstance(value, n.Compound):
            if "id" in value and "Count" in value and "UUID" not in value:
                return True
            return any(ItemPipeline._old_items(v) for v in value.values())
        return isinstance(value, n.List) and any(ItemPipeline._old_items(v) for v in value)

    def _restore_items(self, old, fixed):
        if isinstance(old, n.Compound):
            if "id" in old and "Count" in old and "UUID" not in old:
                return self.item(old)
            result = deepcopy(fixed)
            for key, child in old.items():
                if self._old_items(child):
                    if key not in result:
                        raise ValueError("原版 DFU 移除了原实体库存位置")
                    result[key] = self._restore_items(child, result[key])
            return result
        if isinstance(old, n.List) and self._old_items(old):
            if not isinstance(fixed, n.List) or len(old) != len(fixed):
                raise ValueError("原版 DFU 改变了实体库存列表长度")
            return type(fixed)(self._restore_items(a, b) if self._old_items(a) else deepcopy(b)
                               for a, b in zip(old, fixed))
        return deepcopy(fixed)

    def entity(self, source: n.Compound) -> n.Compound:
        if str(source.get("id", "")) not in self.entities:
            raise ValueError("内嵌实体缺少已核对的目标注册名")
        data = self._restore_items(source, self.dfu.update_entity(deepcopy(source)))
        converted = convert_entity_data(data)
        if source.get("UUID") != converted.data.get("UUID") or source.get("Owner") != converted.data.get("Owner"):
            raise ValueError("内嵌实体的 UUID 或所有者改变")
        for attribute in converted.data.get("attributes", []):
            if str(attribute["id"]) not in self.attributes:
                raise ValueError("内嵌实体属性缺少目标注册名")
        if converted.removed:
            self.changes.append({"kind": "entity_fields", "removed": converted.removed})
        return self.tree(converted.data)

    def tree(self, value, *, oversized_inventory: bool = False):
        """只转换仍为旧格式的节点；现代组件栈不再次走 DFU。"""
        if isinstance(value, n.Compound):
            if str(value.get("id", "")) in self.entities and "UUID" in value and any(
                    key in value for key in ("ForgeCaps", "ForgeData", "Attributes")):
                return self.entity(value)
            if "id" in value and "Count" in value and "UUID" not in value:
                return self.item(value, oversized_inventory=oversized_inventory)
            if "#c" in value:
                old = deepcopy(value)
                amount = old.pop("#", None)
                result = convert_key(old, self.item)
                if amount is not None:
                    if not isinstance(amount, n.Long) or int(amount) <= 0:
                        raise ValueError("旧 AE2 键数量不是正 Long")
                    result["#"] = deepcopy(amount)
                return result
            return n.Compound({key: self.tree(child, oversized_inventory=oversized_inventory)
                               for key, child in value.items()})
        if isinstance(value, n.List):
            return type(value)(self.tree(child, oversized_inventory=oversized_inventory) for child in value)
        return deepcopy(value)

    def item(self, raw: n.Compound, *, oversized_inventory: bool = False) -> n.Compound:
        if not raw or str(raw.get("id", "")) == "minecraft:air":
            return n.Compound()
        if set(raw) - {"id", "Count", "tag", "ForgeCaps", "Slot", "realCount"}:
            raise ValueError("旧物品栈含未审计根字段")
        count = authoritative_count(raw, oversized_inventory=oversized_inventory)
        if count == 0:
            return n.Compound()
        source = unit_source(raw)
        identity = key_identity(source)
        if identity not in self.ready:
            if identity in self.pending:
                raise self.pending[identity]
            result = self.dfu.update(source, "item_stack")
            id = str(source["id"])
            if id == "expatternprovider:infinity_cell":
                converted = convert_infinity_cell(source, result, self.items)
                result = converted.data
                self.changes.append({"kind": "infinite_variant", "removed": converted.removed})
            else:
                result["id"] = n.String(mapped_id(id, self.items))
                if str(result["id"]) != id:
                    self.changes.append({"kind": "item_id", "old": id, "new": str(result["id"])})
            converted = convert_item_components(source, result, self.item)
            result = converted.data
            if converted.removed:
                self.changes.append({"kind": "item_caps", "removed": converted.removed})
            tag = source.get("tag", n.Compound())
            if id in PACKAGE_IDS | {"create:filter"}:
                result = convert_container_item(source, result, self.item).data
            if id.startswith("create:") and ("SequencedAssembly" in tag or "Contraption" in tag):
                result = convert_mechanical_item(source, result, self.convert_contraption).data
            if id.startswith(("touhou_little_maid:", "kaleidoscope_tavern:")) or id in {
                    "kaleidoscope_cookery:recipe_item", "patchouli:guide_book"}:
                converted = convert_stored_item_data(source, result, self.item, self.entity, self.fluid)
                result = converted.data
                if converted.removed:
                    self.changes.append({"kind": "stored_item_format", "changed": converted.removed})
            if isinstance(tag.get("EntityTag"), n.Compound):
                if "minecraft:entity_data" not in result.get("components", {}):
                    raise ValueError("原版 DFU 未转换打包实体组件")
                result["components"]["minecraft:entity_data"] = self.entity(tag["EntityTag"])
            hive = tag.get("BlockEntityTag", n.Compound())
            if id in {"minecraft:bee_nest", "minecraft:beehive"} and "Bees" in hive:
                bees = result.get("components", {}).get("minecraft:bees")
                if bees is None or len(bees) != len(hive["Bees"]):
                    raise ValueError("原版 DFU 改变了蜂巢实体数量")
                for bee, old in zip(bees, hive["Bees"]):
                    bee["entity_data"] = self.entity(old["EntityData"])
            if "keys" in tag and "amts" in tag:
                result = convert_storage_cell(source, result, self.item)
            result = self.tree(result, oversized_inventory=id.startswith("sophisticated"))
            converted = convert_potion_contents(result, self.potions, self.effects, self.potion_effects)
            result = converted.data
            if converted.removed:
                self.changes.append({"kind": "potion_preset", "changed": converted.removed})
            converted = convert_enchantment_components(result, self.enchantments)
            result = converted.data
            if converted.removed:
                self.changes.append({"kind": "enchantment_id", "old_levels": converted.removed})
            if tag and id.startswith("sophisticated"):
                kind = ("backpack" if id.startswith("sophisticatedbackpacks:") and id.endswith("backpack")
                        else "storage" if id.startswith("sophisticatedstorage:") and "_upgrade" not in id
                        else "upgrade")
                needed = NativeItemRequired(source, result, kind)
                self.pending[identity] = needed
                raise needed
            if "mekData" in tag:
                needed = NativeItemRequired(source, result, "mekanism")
                self.pending[identity] = needed
                raise needed
            if id in {"ae2:wireless_crafting_terminal", "ae2wtlib:wireless_universal_terminal",
                      "ae2omnicells:complex_omni_cell_1m"} and tag:
                raise ValueError("无线终端或通用元件尚需完整状态转换")
            result["count"] = n.Int(1)
            self.ready[identity] = deepcopy(result)
        result = deepcopy(self.ready[identity])
        result["count"] = n.Int(count)
        if "Slot" in raw:
            result["Slot"] = deepcopy(raw["Slot"])
        return result
