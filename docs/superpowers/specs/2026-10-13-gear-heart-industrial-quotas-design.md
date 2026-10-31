# 齿轮之心工业瓶颈与修复祝福设计

日期：2026-10-13

## 目标

将齿轮之心从随机惩罚饰品改为七个工业瓶颈组成的固定修复线。修复过程必须对应整合包现有的 Create、Mekanism、AE2 与自研机械动力/Mek 桥接流程，并满足以下约束：

- 诅咒限制工业能力，不使用随机毁物、吞物品、随机冻结或随机失败。
- 所有额度按装备者 UUID 跨 Create 网络、Mekanism 机器和 ME 网络累计。
- 只统计当前加载且活跃的系统，静止、卸载或离线系统不占额度。
- 拆分网络、增加 CPU、堆叠机器不能绕过齿轮之心自身的额度。
- 齿轮之心额度永远不能创造能力，也不能覆盖原模组自身的限制。
- 七项全部修复后，关闭齿轮之心自身的全部额度，只保留原模组限制。

## 范围

实现主体为 `mods/dimensionworks-rpm-limit`。物品、配方、标签、语言和贴图由 `kubejs/` 提供，任务由 `config/ftbquests/` 提供。

不修改 Create、Mekanism、AE2 或 Applied Create 源码，不向 `manifest/mods.json` 新增 Mod。

## 七项诅咒与修复祝福

| 顺序 | 物品 ID 后缀 | 显示名 | 诅咒 | 单项修复祝福 |
| --- | --- | --- | --- | --- |
| 1 | `gear_heart_repair_fragment_1` | 安山校准齿片 | 齿轮之心把装备者网络转速限制为 32 RPM | 恢复 512 RPM |
| 2 | `gear_heart_repair_fragment_2` | 黄铜应力垫片 | 装备者全部活跃 Create 网络合计容量上限 4,096 SU | 立即移除齿轮之心的 Create 容量上限 |
| 3 | `gear_heart_repair_fragment_3` | 冶化同步环 | 装备者全部活跃矿物处理机器合计不超过 8 次操作/秒 | 上限提高到 64 次操作/秒 |
| 4 | `gear_heart_repair_fragment_4` | ME 调度芯片 | 装备者全部 ME 网络同时执行的自动合成任务不超过 1 个 | 上限提高到 80 个 |
| 5 | `gear_heart_repair_fragment_5` | 跨域耦合环 | `gear_heart:cross_process` 桥接物进入 Mek 后处理不超过 16 件/秒 | 上限提高到 128 件/秒 |
| 6 | `gear_heart_repair_fragment_6` | 供能稳压芯 | 同时活跃的 Mek 加工机与 AE2 工作设备各不超过 8 台 | 分别提高到 64 台 |
| 7 | `gear_heart_repair_fragment_7` | 矩阵调度核 | `gear_heart:final_production` 白名单高阶产物不超过 16 件/秒 | 上限提高到 128 件/秒 |

齿轮之心的 RPM 限额优先级低于管理员通过 `/dw rpm <player> <limit>` 设置的玩家上限。命令设置后，齿轮之心不得继续压低该上限；未执行过命令设置时，第一项修复仍可把默认玩家上限从 32 RPM 提升到 512 RPM。Create 配置与仓库硬上限继续生效。

修复必须严格按 1 到 7 的顺序进行。每个碎片使用后立即写入对应修复位并生效，不等待全部完成。

第七项完成后，`kubejs:gear_heart` 自动替换为 `kubejs:gear_heart_enchanted`，显示名改为“全链齿轮之心”。最终状态关闭第一至第七项由齿轮之心添加的全部额度，获得“全链释限”效果。

## 原模组限制

“无上限”仅表示齿轮之心不再附加额度，绝不能绕过原模组限制：

- RPM 仍受玩家命令上限、其他 RPM Limit 外部限制、Create 配置与 10,240 RPM 仓库硬上限约束；齿轮之心 RPM 限额不得覆盖玩家命令上限。
- Create 容量仍受网络实际发电机容量、有效 RPM 缩放、应力消耗和过载判定约束。
- Mek 仍受配方、单机 tick、速度升级、能耗、红石控制和机器自身能力约束。
- AE2 仍受 CPU 数量、单 CPU 忙碌状态、存储、频道、能源和任务本身约束。
- 跨工序与白名单产出仍受实际输入、物流、配方产量和机器处理能力约束。

齿轮之心额度必须通过 `min(齿轮之心额度, 原生可用能力)` 应用。修复或最终释限只能移除前者，不能提高后者。

## 所有权

- Create 使用现有 `RpmLimitManager.owner(KineticBlockEntity)`。
- Mekanism 使用 `TileEntityMekanism#getSecurity#getOwnerUUID`。
- AE2 使用网格节点的 `getOwningPlayerProfileId`，并通过 `IPlayerRegistry` 映射 UUID。
- 新放置系统由原模组自动写入所有者。
- 旧存档中没有所有者的活跃系统进入共享后备额度；每个仍受诅咒的装备者都要额外承担这部分用量，不能通过缺少归属绕过。
- 没有装备受诅咒齿轮之心时不应用任何齿轮之心额度。

## 计账模型

统一使用服务器主线程中的固定间隔令牌桶，不使用随机数：

- RPM 与设备数量是离散额度。
- 容量、操作、物品和产出使用每 tick 补充的浮点令牌，配置值以每秒速率表达。
- 每个额度只保留当前 tick 所需的最小余量，避免长期积累超额库存。
- 额度引擎按 `(playerUUID, quotaType)` 隔离。
- 最终释限时直接禁用额度读取，不依赖令牌桶补满。
- 所有聚合仅遍历当前活跃对象；不扫描卸载区块。

## 技术挂点

### Create 容量

在 `KineticNetwork#calculateCapacity` 返回前计算该网络自然容量，再按所有者聚合同一玩家的全部活跃网络。诅咒激活时按比例缩放或截断，使合计不超过 4,096 SU；第二项修复及最终状态不施加齿轮之心容量限制。

网络注册表使用弱引用或网络自身生命周期清理，避免保存已移除网络。容量计算不得递归调用自身。

### Mekanism 加工

在 `TileEntityMekanism#onUpdateServer` 建立当前机器上下文，在 `CachedRecipe#process` 执行前查询额度：

- 矿物处理类别由富化、粉碎、净化、化学注入及对应工厂机器类识别。
- 跨工序类别检查配方的物品输入表示是否命中 `gear_heart:cross_process`。
- 额度不足时停止本轮加工，不消耗输入、不产出、不重置已有进度。
- 同一 tick 的多个操作一次申请全部所需令牌，避免部分执行。

### AE2 自动合成

在 `CraftingService#submitJob` 提交非模拟任务前，统计该服务及同一所有者其他活跃服务中的忙碌 CPU：

- 超过齿轮之心并发额度时返回 AE2 原生“无可用 CPU”结果。
- 模拟计划不占额度。
- 任务完成后由下一次提交重新统计，不维护易失的额外任务表。
- 不改变 AE2 原生 CPU 选择、忙碌、存储和能源规则。

### 高阶产物

第七项只处理 `gear_heart:final_production` 标签中的物品。Create、Mekanism 和 AE2 出口在执行或插入白名单产物前申请令牌：

- Create/Mek 在确认本轮能够完成且输出空间足够后申请，额度不足则不开始本轮。
- AE2 在最终输出进入请求方或网络前申请，额度不足则保持任务未完成并等待下一 tick。
- 不吞输入、不删除已生成但尚未允许交付的产物。
- 标签由 KubeJS 数据包维护，第三方或未列入标签的物品不受第七项限制。

## 物品与状态

保留现有注册 ID，避免破坏旧存档：

- `kubejs:gear_heart`
- `kubejs:gear_heart_enchanted`
- `kubejs:gear_heart_repair_fragment_1` 至 `_7`

`GearHeartState` 使用七个顺序位保存修复记录。旧 `DimensionWorksGearHeartRepaired` NBT 继续兼容：

- 旧攻击伤害位映射到第 1 项。
- 旧转速位映射到第 2 项。
- 其余旧位按原索引迁移。
- 迁移后删除旧语义，不继承随机惩罚。
- 旧 `CustomModelData=2` 祝福占位物品继续迁移为受诅咒齿轮之心。
- 第七项修复后复制修复 NBT 到最终物品，并清除仅用于旧破损模型的 `CustomModelData`。

最终物品仍在 Curios 的 `gear_heart` 槽中不可手动取下、死亡保留。

## 修复碎片制作

配方全部放在 KubeJS，不写 Java 配方引擎：

1. 安山校准齿片：Create 机械压力机处理 `create:andesite_alloy`。
2. 黄铜应力垫片：Create 压片组合 `create:iron_sheet` 与 `create:brass_sheet`。
3. 冶化同步环：Mekanism 富化室处理 `create:precision_mechanism`。
4. ME 调度芯片：Create 机械合成，3x3 模式 `LCL / EPE / LCL`，其中 L 为 `ae2:logic_processor`，C 为 `mekanism:basic_control_circuit`，E 为 `create:electron_tube`，P 为 `create:precision_mechanism`。
5. 跨域耦合环：Create 机械合成，3x3 模式 `SAS / ADA / SCS`，其中 S 为 `mekanism:ingot_steel`，A 为 `mekanism:alloy_infused`，D 为 `dimensionworks_mek_stress:memory_drive_ddr1`，C 为 `mekanism:basic_control_circuit`。
6. 供能稳压芯：Create 机械合成，3x3 模式 `GUG / EAE / GUG`，其中 G 为 `mekanism:universal_cable`，U 为 `ae2:fluix_crystal`，E 为 `create:electron_tube`，A 为 `appliedcreate:me_gearbox`。
7. 矩阵调度核：Create 机械合成，3x3 模式 `APA / MCM / APA`，其中 A 为 `ae2:engineering_processor`，P 为 `dimensionworks_mek_stress:memory_card_ddr3_balanced`，M 为 `mekanism:alloy_atomic`，C 为 `create:precision_mechanism`。

配方 ID 使用 `kubejs:gear_heart_repair_fragment_<n>`，并设置固定输入以保证任务图和预检可检查。

## 贴图

扩展现有 `scripts/pixelate_gear_heart_textures.py`，输出七个独立 32x32 RGBA 文件：

- `gear_heart_repair_fragment_1.png`
- 至
- `gear_heart_repair_fragment_7.png`

七个轮廓分别表现齿片、垫片、同步环、逻辑芯片、耦合环、稳压芯与调度核。颜色依次使用安山铁灰、黄铜、Mek 青色、AE2 绿色、Create 铜色、能源紫青和最终金青组合。

生成脚本同时输出 `docs/assets/gear_heart_fragments_preview.png`，使用最近邻放大检查像素轮廓，不使用 AI 图片生成。

## 任务

更新 `config/ftbquests/quests/chapters/07DDD0CAE18A7217.snbt`：

- 保留出生时获得齿轮之心的初始任务。
- 新增七个获取碎片任务，依赖严格串联。
- 每个任务显示对应诅咒、修复祝福和制作阶段。
- 新增最终“全链齿轮之心”任务，依赖第七个碎片任务。
- 任务 ID 保持唯一，不复用已有 ID。

## 配置与文档

删除旧的攻击伤害、产出失败、随机冻结和夜晚饥饿配置，新增 `[gearHeart]` 额度配置：

- `rotationCursedRpm = 32`
- `rotationRepairedRpm = 512`
- `createCapacityCursedSu = 4096`
- `mekOreOperationsCursedPerSecond = 8`
- `mekOreOperationsRepairedPerSecond = 64`
- `aeCraftingJobsCursed = 1`
- `aeCraftingJobsRepaired = 80`
- `crossProcessItemsCursedPerSecond = 16`
- `crossProcessItemsRepairedPerSecond = 128`
- `activeMachinesCursed = 8`
- `activeMachinesRepaired = 64`
- `finalProductionItemsCursedPerSecond = 16`
- `finalProductionItemsRepairedPerSecond = 128`

更新 `dimensionworks-rpm-limit/README.md`、`kubejs/README.md` 与脚本 README，说明修复流程、额度边界、贴图和配方。

## 依赖与版本

`dimensionworks-rpm-limit` 增加 Mekanism 与 AE2 编译/运行依赖和 `mods.toml` 依赖声明。现有 Create 与 Curios 依赖保留。

实现完成后将自研 Mod 版本从 `0.4.4` 提升为 `0.4.5`，同步 `build.gradle`、`mods.toml` 与 `manifest/mods.json` 中已有条目。根 `VERSION` 不手工修改，留给打包脚本处理。

## 测试

### 单元测试

- 七项顺序、位掩码、单项修复与第七项最终转化。
- 第二项修复立即释放 Create 自身额度。
- 第四项修复额度为 80，最终释限时完全移除。
- 令牌桶在整数速率、零速率、超额请求和跨 tick 补充时确定性工作。
- 所有者聚合、共享后备额度与多玩家隔离。
- `min(齿轮之心额度, 原生能力)` 永远不提高原生能力。
- 旧 NBT 和旧祝福占位物品迁移。

### 合同测试

- Create 容量 Mixin 目标与聚合调用存在。
- Mek 上下文与 `CachedRecipe#process` 门控存在。
- AE2 `CraftingService#submitJob` 在模拟任务之外执行并发门控。
- 白名单标签只影响第七项。
- 随机诅咒方法与旧配置键已移除。

### 构建与静态验证

- `cd mods/dimensionworks-rpm-limit && gradle build --no-daemon`
- `node --check` 检查全部 KubeJS 脚本。
- 解析 `kubejs/data`、`kubejs/assets`、`config/ftbquests` JSON。
- 运行任务图、配方 ID、命名空间与贴图尺寸检查。
- 运行 `python3 scripts/pack_hook.py --skip-build` 做仓库静态预检。

### 运行时验收

实际进入游戏后验证：

- 正转和反转机械网络均能为 Mek 机器供能。
- 七项修复顺序、提示、NBT 和最终物品转化。
- 两名玩家的额度互不干扰。
- 拆分 Create 网络、增加 AE2 CPU、堆叠 Mek 机器不能突破单项额度。
- 第二项修复后 Create 不再有齿轮之心容量限制。
- 第四项修复后并发上限为 80，但不会超过原生可用 CPU。
- 七项完成后所有齿轮之心额度关闭，原模组限制继续生效。

未实际进入游戏前，不把构建或静态检查描述为运行时验证完成。
