"""唯一旧档的递归物品转换；作者迁移接口缺少结果时返回明确的批处理需求。"""
from __future__ import annotations

from copy import deepcopy
from typing import Callable

import nbtlib as n

from .ae_data import convert_cable_bus_parts, convert_key, convert_storage_cell, key_identity
from .ae_items import AE_STATE_IDS, convert_ae_item
from .auxiliary_items import AUXILIARY_IDS, convert_auxiliary_item
from .create_data import PACKAGE_IDS, convert_container_item, convert_mechanical_item
from .entity_data import convert_entity_data
from .fluid_data import convert_block_fluids
from .hive_data import normalize_hive_fields
from .item_components import authoritative_count, convert_item_components
from .mod_item_data import convert_stored_item_data
from .potion_data import convert_active_effects, convert_potion_contents
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


def convert_render_info_fluids(value: n.Compound, fluid_ids: set[str],
                               legacy_fluid: Callable[[n.Compound], n.Compound]) -> n.Compound:
    """转换精妙核心展示组件中的旧 FluidStack，不能把它当实际槽位库存。"""
    result = deepcopy(value)
    tanks = result.get("tanks")
    if tanks is None:
        return result
    if not isinstance(tanks, n.List):
        raise ValueError("精妙展示组件 tanks 不是列表")
    for tank in tanks:
        if not isinstance(tank, n.Compound):
            raise ValueError("精妙展示组件 tank 不是 compound")
        info = tank.get("info")
        if info is None:
            continue
        if not isinstance(info, n.Compound):
            raise ValueError("精妙展示组件 info 不是 compound")
        fluid = info.get("fluid")
        if fluid is None:
            continue
        if not isinstance(fluid, n.Compound) or not fluid:
            raise ValueError("精妙展示组件 fluid 不是 compound")
        if set(fluid) == {"FluidName", "Amount"}:
            info["fluid"] = legacy_fluid(fluid)
            continue
        allowed = {"id", "amount", "components"}
        if (not set(fluid) <= allowed or not isinstance(fluid.get("id"), n.String) or
                not isinstance(fluid.get("amount"), n.Int) or int(fluid["amount"]) <= 0 or
                str(fluid["id"]) not in fluid_ids or
                ("components" in fluid and not isinstance(fluid["components"], n.Compound))):
            raise ValueError("精妙展示组件 fluid 含未覆盖的注册名、字段或数量")
    return result


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
        self.block_entities = set(registries["minecraft:block_entity_type"])
        self.ae_key_types = set(registries.get("ae2:keytypes", []))
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

    def _legacy_actor(self, value) -> bool:
        return (isinstance(value, n.Compound) and str(value.get("id", "")) in self.entities and
                "count" not in value and ("Count" not in value or "UUID" in value) and
                any(key in value for key in ("ForgeCaps", "ForgeData", "Attributes", "ActiveEffects")))

    def _needs_restore(self, value) -> bool:
        if isinstance(value, n.Compound):
            if self._legacy_actor(value):
                return True
            if "id" in value and "Count" in value and "UUID" not in value:
                return True
            return any(self._needs_restore(v) for v in value.values())
        return isinstance(value, n.List) and any(self._needs_restore(v) for v in value)

    def _restore_payloads(self, old, fixed, *, oversized_inventory: bool = False, actor_root: bool = False):
        if isinstance(old, n.Compound):
            if actor_root and self._legacy_actor(old):
                return self.entity(old)
            if "id" in old and "Count" in old and "UUID" not in old:
                return self.item(old, oversized_inventory=oversized_inventory)
            result = deepcopy(fixed)
            for key, child in old.items():
                if self._needs_restore(child):
                    if key not in result:
                        if (key == "buyB" and "buy" in old and "sell" in old and isinstance(child, n.Compound)
                            and str(child.get("id", "")) == "minecraft:air"):
                            # 原版 1.21 报价不再保存空的第二项成本；air 即使带旧 Count/tag 也不是库存。
                            self.changes.append({"kind": "empty_trade_second_cost", "removed": deepcopy(child)})
                            continue
                        raise ValueError("原版 DFU 移除了原实体库存位置：" + key)
                    result[key] = self._restore_payloads(child, result[key], oversized_inventory=oversized_inventory, actor_root=True)
            return result
        if isinstance(old, n.List) and self._needs_restore(old):
            if not isinstance(fixed, n.List) or len(old) != len(fixed):
                raise ValueError("原版 DFU 改变了实体库存列表长度")
            return type(fixed)(self._restore_payloads(a, b, oversized_inventory=oversized_inventory, actor_root=True) if self._needs_restore(a) else deepcopy(b)
                               for a, b in zip(old, fixed))
        return deepcopy(fixed)

    def entity(self, source: n.Compound) -> n.Compound:
        if str(source.get("id", "")) not in self.entities:
            raise ValueError("内嵌实体缺少已核对的目标注册名")
        old = deepcopy(source)
        if "Contraption" in old:
            if self.convert_contraption is None:
                raise ValueError("机械实体须接入完整原生移动载荷")
            old["Contraption"] = self.convert_contraption(old["Contraption"])
        old.pop("ActiveEffects", None)  # CurativeItems 是治愈描述，不能当库存或用旧数值效果 ID 转换。
        data = self._restore_payloads(old, self.dfu.update_entity(old))
        return self._actor(source, data)

    def player(self, source: n.Compound) -> n.Compound:
        old = deepcopy(source)
        old.pop("ActiveEffects", None)
        data = self._restore_payloads(old, self.dfu.update(old, "player"))
        return self._actor(source, data)

    def _actor(self, source: n.Compound, data: n.Compound) -> n.Compound:
        effects = convert_active_effects(source, data, self.effects)
        if effects.removed:
            self.changes.append({"kind": "active_effects", "changed": effects.removed})
        data = effects.data
        converted = convert_entity_data(data)
        if source.get("UUID") != converted.data.get("UUID") or source.get("Owner") != converted.data.get("Owner"):
            raise ValueError("内嵌实体的 UUID 或所有者改变")
        for attribute in converted.data.get("attributes", []):
            if str(attribute["id"]) not in self.attributes:
                raise ValueError("内嵌实体属性缺少目标注册名")
        if converted.removed:
            self.changes.append({"kind": "entity_fields", "removed": converted.removed})
        return self.tree(converted.data)

    def block_entity(self, source: n.Compound) -> n.Compound:
        """普通方块实体及 Create 更新标签；Mekanism 必须接入已验证的专用机器转换。"""
        id = str(source.get("id", ""))
        if id.startswith("mekanism"):
            raise ValueError("Mekanism 方块实体须使用专用机器转换")
        fluids = convert_block_fluids(source, self.fluids)
        if fluids.removed:
            self.changes.append({"kind": "block_fluid_fields", "removed": fluids.removed})
        source = fluids.data
        if id == "create:basin":
            for key in ("DimensionWorksBasinGas", "DimensionWorksBasinInputGas"):
                if key in source:
                    old = source[key]
                    if old != n.Compound({"gasName": n.String("mekanism:empty"), "amount": n.Long(0)}):
                        raise ValueError("搅拌盆化学缓存含未覆盖的非空旧状态")
                    self.changes.append({"kind": "empty_basin_chemical", "field": key, "removed": deepcopy(old)})
                    source[key] = n.Compound()
        if id:
            target = mapped_id(id, self.block_entities, block_entity=True)
            data = self._restore_payloads(source, self.dfu.update(deepcopy(source), "block_entity"),
                                       oversized_inventory=id.startswith("sophisticatedstorage:"))
            data["id"] = n.String(target)
        else:
            # Create UpdateTag 无 id；只处理已明确存在的物品与平台持久字段，不编造实体类型。
            data = self.tree(source)
        data, part_names = convert_cable_bus_parts(data, self.items, native_key_types=self.ae_key_types)
        if part_names:
            self.changes.append({"kind": "ae2_part_registry_names", "removed": part_names})
        converted = convert_entity_data(data)
        if converted.removed:
            self.changes.append({"kind": "block_entity_fields", "removed": converted.removed})
        hive = normalize_hive_fields(self.tree(converted.data))
        if hive.removed:
            self.changes.append({"kind": "hive_duplicate_fields", "removed": hive.removed})
        return hive.data

    def tree(self, value, *, oversized_inventory: bool = False):
        """只转换仍为旧格式的节点；现代组件栈不再次走 DFU。"""
        if isinstance(value, n.Compound):
            if self._legacy_actor(value):
                return self.entity(value)
            if "id" in value and "Count" in value and "UUID" not in value:
                return self.item(value, oversized_inventory=oversized_inventory)
            if "#c" in value:
                old = deepcopy(value)
                amount = old.pop("#", None)
                result = convert_key(old, self.item)
                if amount is not None:
                    # 本服 AE2 总线 config[] 使用 #=0L 表示过滤模板；真实元件储量另有正数校验。
                    if not isinstance(amount, n.Long) or int(amount) < 0:
                        raise ValueError("旧 AE2 键数量不是非负 Long")
                    result["#"] = deepcopy(amount)
                return result
            result = n.Compound()
            for key, child in value.items():
                converted = self.tree(child, oversized_inventory=oversized_inventory)
                if key == "sophisticatedcore:render_info_tag":
                    if not isinstance(converted, n.Compound):
                        raise ValueError("精妙展示组件不是 compound")
                    converted = convert_render_info_fluids(converted, self.fluids, self.fluid)
                result[key] = converted
            return result
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
            if id in AE_STATE_IDS:
                result = convert_ae_item(source, result, self.item, native_key_types=self.ae_key_types)
            if id in AUXILIARY_IDS:
                def enchantments(old):
                    book = self.dfu.update(n.Compound({"id": n.String("minecraft:enchanted_book"), "Count": n.Byte(1),
                                                       "tag": n.Compound({"StoredEnchantments": deepcopy(old)})}), "item_stack")
                    converted = convert_enchantment_components(book, self.enchantments)
                    return converted.data["components"]["minecraft:stored_enchantments"]
                result = convert_auxiliary_item(source, result, enchantments)
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
            converted = convert_enchantment_components(result, self.enchantments, source=source)
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
            result["count"] = n.Int(1)
            self.ready[identity] = deepcopy(result)
        result = deepcopy(self.ready[identity])
        result["count"] = n.Int(count)
        if "Slot" in raw:
            result["Slot"] = deepcopy(raw["Slot"])
        return result
