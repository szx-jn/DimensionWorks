"""唯一服务器快照的维度定义、附件与强制区块票据转换；不启动世界。"""
from copy import deepcopy
from dataclasses import dataclass

import nbtlib as n


@dataclass
class LevelDataConversion:
    data: n.Compound
    removed: dict


def promote_brewing_metadata(source: n.Compound, native_version: int) -> LevelDataConversion:
    """仅在载荷完整转换后标记作者的当前文件格式；来源版本进入台账。"""
    if native_version != 100:
        raise ValueError("目标酿酒数据版本不是已核对的 1.21.1 格式")
    result = deepcopy(source)
    field = "brewinandchewin:data_version"
    old = source.get(field)
    if old is None:
        return LevelDataConversion(result, {})
    if not isinstance(old, n.Int) or int(old) not in {2, native_version}:
        raise ValueError("酿酒文件元数据版本与唯一旧档不同")
    result[field] = n.Int(native_version)
    return LevelDataConversion(result, {field: deepcopy(old)} if int(old) != native_version else {})


def convert_world_gen_settings(source: n.Compound, target_dimensions: n.Compound,
                               *, empty_retired_dimensions: set[str]) -> LevelDataConversion:
    """保留种子与已有维度；暮色已删除的生成器使用固定目标资产的原生定义。"""
    if (set(source) != {"seed", "generate_features", "bonus_chest", "dimensions"} or
        not isinstance(source["seed"], n.Long) or not isinstance(source["dimensions"], n.Compound) or
        any(not isinstance(source[key], n.Byte) or int(source[key]) not in {0, 1}
            for key in ("generate_features", "bonus_chest"))):
        raise ValueError("世界生成选项与唯一旧档的已审计结构不同")
    if empty_retired_dimensions - {"witherstormmod:bowels"}:
        raise ValueError("未授权舍弃其它维度")
    data = deepcopy(source)
    removed = {}
    for id, stem in source["dimensions"].items():
        if set(stem) != {"type", "generator"} or not isinstance(stem["generator"], n.Compound):
            raise ValueError("维度定义含未覆盖字段：" + id)
        if id == "witherstormmod:bowels":
            if id not in empty_retired_dimensions:
                raise ValueError("尚未证明凋零风暴维度没有玩家、区块或物品")
            removed[id] = deepcopy(stem)
            del data["dimensions"][id]
            continue
        if id not in target_dimensions or stem["type"] != target_dimensions[id]["type"]:
            raise ValueError("原维度类型缺少固定目标定义：" + id)
        generator = stem["generator"]
        if generator.get("type") == n.String("twilightforest:structure_locating_wrapper"):
            expected = {"type", "noise_generation_settings", "generate_dark_forest_canopy", "wrapped_generator",
                        "landmark_placement_allowed_biomes", "dark_forest_canopy_height"}
            replacement = target_dimensions[id]
            if (id != "twilightforest:twilight_forest" or set(generator) != expected or
                replacement["generator"].get("type") != n.String("minecraft:noise") or
                replacement["generator"].get("biome_source", {}).get("type") != n.String("twilightforest:twilight_biomes")):
                raise ValueError("暮色生成器变化不符合已核对的作者升级格式")
            removed[id + "/source_generator"] = deepcopy(generator)
            data["dimensions"][id] = deepcopy(replacement)
    return LevelDataConversion(data, removed)


def convert_level_attachments(source: n.Compound, native_ids: set[str]) -> LevelDataConversion:
    data = n.Compound()
    removed = {}
    for id, value in source.items():
        if not isinstance(value, n.Compound):
            raise ValueError("维度旧能力不是复合标签")
        if id.startswith("witherstormmod:"):
            shapes = {"chunk_loader": {}, "chunk_loading_block_entities": {"LoadedEntities": n.List()},
                      "bowels_manager": {"Instances": n.List()}}
            name = id.split(":", 1)[1]
            if name == "auto_spawner":
                if (set(value) != {"TickCount", "HasSpawnedWitherStorm"} or
                    not isinstance(value["TickCount"], n.Int) or not isinstance(value["HasSpawnedWitherStorm"], n.Byte)):
                    raise ValueError("凋零风暴自动生成记录与已审计字段不同")
            elif name not in shapes or dict(value) != shapes[name]:
                raise ValueError("凋零风暴维度能力含未审计的非空数据")
            removed[id] = deepcopy(value)
            continue
        if id == "twilightforest:teleporter_cache":
            if set(value) != {"dest"} or not isinstance(value["dest"], n.List) or value["dest"]:
                raise ValueError("旧暮色传送缓存含未处理目的地")
            removed[id] = deepcopy(value)
            continue
        if id == "maid_storage_manager:block_occupy":
            if set(value) != {"occupied"} or not isinstance(value["occupied"], n.List) or value["occupied"]:
                raise ValueError("女仆占用缓存不是已审计的空记录")
            target = "maid_storage_manager:craft_block_occupy"
            payload = deepcopy(value)
        elif id in {"maid_storage_manager:maid_persist", "maid_storage_manager:inventory_list"}:
            if value:
                raise ValueError("女仆持久库存能力含未处理内容")
            target = {"maid_storage_manager:maid_persist": "maid_storage_manager:maid_item_persist_data",
                      "maid_storage_manager:inventory_list": "maid_storage_manager:inventory_list_data"}[id]
            payload = deepcopy(value)
        elif id == "aether:aether_time":
            if (set(value) != {"DayTime", "EternalDay"} or not isinstance(value["DayTime"], n.Long) or
                not isinstance(value["EternalDay"], n.Byte) or int(value["EternalDay"]) not in {0, 1}):
                raise ValueError("天境时间字段与已审计结构不同")
            target = id
            # 旧版本未持久化 shouldWait；新版构造器的初始值为 false。
            payload = n.Compound({"day_time": deepcopy(value["DayTime"]),
                                  "eternal_day": deepcopy(value["EternalDay"]), "should_wait": n.Byte(0)})
            removed[id + "/source_fields"] = deepcopy(value)
        else:
            raise ValueError("没有维度旧能力转换规则：" + id)
        if target not in native_ids or target in data:
            raise ValueError("维度附件缺少目标注册名或映射冲突：" + target)
        data[target] = payload
    return LevelDataConversion(data, removed)


def convert_forced_chunks(source: n.Compound, controllers: set[str],
                          retired_controllers: set[str] | None = None) -> LevelDataConversion:
    if (set(source) != {"Forced", "ForgeForced"} or not isinstance(source["Forced"], n.LongArray) or
        not isinstance(source["ForgeForced"], n.List)):
        raise ValueError("强制区块记录与本服已审计结构不同")
    retired_controllers = retired_controllers or set()
    rows = n.List[n.Compound]()
    seen = set()
    removed = {"ForgeForced": deepcopy(source["ForgeForced"])}
    for row in source["ForgeForced"]:
        if set(row) != {"Mod", "ModForced"} or not isinstance(row["Mod"], n.String) or not isinstance(row["ModForced"], n.List):
            raise ValueError("模组区块票据含未知字段")
        names = {"create_power_loader": "create_power_loader:chunk_load_manager", "mekanism": "mekanism:chunk_loader"}
        if str(row["Mod"]) not in names:
            raise ValueError("旧档票据模组不在已核对映射中：" + str(row["Mod"]))
        id = names[str(row["Mod"])]
        if id not in controllers or id in seen:
            raise ValueError("实际票据控制器未注册或重复：" + id)
        seen.add(id)
        for ticket in row["ModForced"]:
            if (set(ticket) != {"Chunk", "TickingBlocks"} or not isinstance(ticket["Chunk"], n.Long) or
                not isinstance(ticket["TickingBlocks"], n.List)):
                raise ValueError("强制区块票据含未覆盖载荷")
            for pos in ticket["TickingBlocks"]:
                if set(pos) != {"X", "Y", "Z"} or any(not isinstance(pos[k], n.Int) for k in pos):
                    raise ValueError("强制区块机器坐标不是已审计的 XYZ Int")
        if id in retired_controllers:
            removed["ForgeForced/" + id] = deepcopy(row)
            continue
        # NeoForge 的票据坐标仍是 XYZ 复合标签，不能套用 Create 的 IntArray 格式。
        rows.append(n.Compound({"Controller": n.String(id), "ModForced": deepcopy(row["ModForced"])}))
    if retired_controllers - seen:
        raise ValueError("请求停用的票据控制器不存在：" + ", ".join(sorted(retired_controllers - seen)))
    return LevelDataConversion(n.Compound({"Forced": deepcopy(source["Forced"]), "ModForced": rows}), removed)
