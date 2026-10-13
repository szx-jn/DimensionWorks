# 齿轮之心工业瓶颈与修复祝福实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把齿轮之心改为七阶段固定修复线，按装备者 UUID 聚合 Create、Mekanism 与 AE2 的工业额度，并保留双向机械供能。

**Architecture:** 纯额度、状态迁移和聚合数学放入 `dimensionworks-rpm-limit` 的 `core`/`quota` 包，以 JUnit 先测；Forge/Mixin 只把真实网络、机器和任务提交接到额度边界。KubeJS 负责物品、配方、标签、语言和贴图，FTB Quests 负责引导，两个自研 Mod 分别构建。

**Tech Stack:** Minecraft 1.20.1、Forge 47.4.16、Java 17、Create 6.0.8、Mekanism 10.4.16.80、AE2 15.4.10、Mixin、JUnit 5、KubeJS、FTB Quests。

**Spec:** `docs/superpowers/specs/2026-10-13-gear-heart-industrial-quotas-design.md`

## Global Constraints

- 不修改 Create、Mekanism、AE2 或 Applied Create 源码；只使用本仓库 Mixin、事件和 KubeJS。
- 不向 `manifest/mods.json` 新增 Mod；依赖只增加到 `dimensionworks-rpm-limit` 的构建与 `mods.toml`。
- 七项修复严格按 `1..7` 顺序；第七项完成后将 `kubejs:gear_heart` 替换为 `kubejs:gear_heart_enchanted`。
- 修复位固定为 `1<<0` 到 `1<<6`，分别对应 RPM、Create 容量、Mek 矿物操作、AE2 并发、跨工序、活跃机器、白名单高阶产物。
- 第二项修复立即关闭齿轮之心自己的 Create 容量额度；第七项完成后关闭全部齿轮之心额度，但不移除原模组限制。
- 诅咒值固定为 `32 RPM / 4096 SU / 8 ops/s / 1 job / 16 items/s / 8 machines / 16 items/s`；修复值固定为 `512 RPM / 无自有容量上限 / 64 ops/s / 80 jobs / 128 items/s / 64 machines / 128 items/s`。
- 额度应用必须是 `min(齿轮之心额度, 原生能力)`；修复或最终释限只能移除齿轮之心额度。
- 只统计当前加载且活跃的对象；注册表使用弱引用/生命周期清理，不扫描卸载区块。
- 旧 `DimensionWorksGearHeartRepaired` 位按原索引迁移；旧 `CustomModelData=2` 祝福物品迁移为受诅咒齿轮之心。
- 双向机械供能按 `abs(RPM)` 计算有效速度和 `abs(RPM) × 8` 应力，零转速不供能。
- 每个生产行为先写失败测试，再写最小实现；所有 Java 构建使用 `gradle build --no-daemon`。
- 未实际进入游戏的内容只报告为构建/静态验证，不声称运行时验证完成。

---

### Task 1: Mek 双向机械输入

**Files:**
- Modify: `mods/dimensionworks-mek-stress/src/main/java/dev/szx/dimensionworks/mekstress/core/MachinePowerMath.java`
- Test: `mods/dimensionworks-mek-stress/src/test/java/dev/szx/dimensionworks/mekstress/core/MachinePowerMathTest.java`
- Modify: `mods/dimensionworks-mek-stress/README.md`

**Interfaces:**
- Produces: `MachinePowerMath.directRpm(float requestedRpm, int maximumRpm) -> float`
- Produces: `directRpm(-4096F, 10240) == 4096F`
- Produces: `directRpm(-20480F, 10240) == 10240F`
- Produces: `directRpm(0F, 10240) == 0F` 和非有限输入为 `0F`

- [x] **Step 1: 写失败测试并确认失败**
- [x] **Step 2: 先取速度绝对值，再执行零速检查、有限性检查和 `directMaxRpm` 收口**
- [x] **Step 3: 运行定向测试确认通过**
- [x] **Step 4: 运行 `gradle build --no-daemon` 并更新 README**

Run: `cd mods/dimensionworks-mek-stress && gradle build --no-daemon`
Expected: `BUILD SUCCESSFUL`；产物 `build/libs/dimensionworks_mek_stress-0.3.4.jar`。

---

### Task 2: 状态、严格修复顺序与额度核心

**Files:**
- Modify: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/GearHeartState.java`
- Modify: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/GearHeartEffects.java`
- Modify: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/GearHeartForgeEvents.java`
- Modify: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/RpmLimitConfig.java`
- Create: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/quota/GearHeartQuotaType.java`
- Create: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/quota/GearHeartQuotaPolicy.java`
- Create: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/quota/FixedIntervalTokenBucket.java`
- Test: `mods/dimensionworks-rpm-limit/src/test/java/dev/szx/dimensionworks/rpmlimit/GearHeartStateTest.java`
- Test: `mods/dimensionworks-rpm-limit/src/test/java/dev/szx/dimensionworks/rpmlimit/quota/GearHeartQuotaPolicyTest.java`
- Test: `mods/dimensionworks-rpm-limit/src/test/java/dev/szx/dimensionworks/rpmlimit/quota/FixedIntervalTokenBucketTest.java`

**Interfaces:**
- Produces: `GearHeartQuotaType` 的七个枚举值，顺序与修复位一致。
- Produces: `GearHeartQuotaPolicy.limit(type, repairedMask) -> double`，最终状态返回正无穷。
- Produces: `GearHeartQuotaPolicy.isReleased(type, repairedMask) -> boolean`。
- Produces: `FixedIntervalTokenBucket.tryTake(UUID owner, GearHeartQuotaType type, double amount, double perSecond, long tick) -> boolean`。
- Produces: `GearHeartState.nextRepairIndex(int repairedMask) -> int`，返回下一个允许的 1-based 阶段。
- Produces: `GearHeartState.canRepair(int repairedMask, int fragmentIndex) -> boolean`。
- Produces: `GearHeartState.migrateLegacyCursed(ItemStack)`、`GearHeartState.isFullyReleased(ItemStack)`。

- [ ] **Step 1: 写失败测试**

```java
@Test
void repairsMustBeStrictlySequential() {
    assertEquals(1, GearHeartState.nextRepairIndex(0));
    assertFalse(GearHeartState.canRepair(0, 2));
    assertTrue(GearHeartState.canRepair(0b0000011, 3));
    assertTrue(GearHeartState.isFullyReleased(maskWithBits(0, 6)));
}

@Test
void secondRepairRemovesCreateQuotaImmediately() {
    assertEquals(Double.POSITIVE_INFINITY,
        GearHeartQuotaPolicy.limit(GearHeartQuotaType.CREATE_CAPACITY, 0b0000010));
}

@Test
void repairedAeQuotaIsEightyAndFinalReleaseIsUnlimited() {
    assertEquals(80.0, GearHeartQuotaPolicy.limit(GearHeartQuotaType.AE_CRAFTING_JOBS, 0b00001000));
    assertEquals(Double.POSITIVE_INFINITY,
        GearHeartQuotaPolicy.limit(GearHeartQuotaType.AE_CRAFTING_JOBS, 0b1111111));
}

@Test
void tokenBucketDoesNotAccumulateAcrossTicks() {
    FixedIntervalTokenBucket bucket = new FixedIntervalTokenBucket();
    assertTrue(bucket.tryTake(owner, GearHeartQuotaType.MEK_ORE_OPERATIONS, 0.4, 8.0, 0));
    assertTrue(bucket.tryTake(owner, GearHeartQuotaType.MEK_ORE_OPERATIONS, 0.4, 8.0, 1));
    assertFalse(bucket.tryTake(owner, GearHeartQuotaType.MEK_ORE_OPERATIONS, 0.5, 8.0, 1));
    assertTrue(bucket.tryTake(owner, GearHeartQuotaType.MEK_ORE_OPERATIONS, 0.4, 8.0, 100));
    assertFalse(bucket.tryTake(owner, GearHeartQuotaType.MEK_ORE_OPERATIONS, 0.5, 8.0, 100));
    assertFalse(bucket.tryTake(owner, GearHeartQuotaType.MEK_ORE_OPERATIONS, 0.4, 0.0, 101));
}
```

- [ ] **Step 2: 运行失败测试**

Run: `cd mods/dimensionworks-rpm-limit && gradle test --tests '*GearHeartStateTest' --tests '*GearHeartQuotaPolicyTest' --tests '*FixedIntervalTokenBucketTest' --no-daemon`
Expected: FAIL，类或方法不存在。

- [ ] **Step 3: 实现状态位、严格顺序、最终物品替换、旧 NBT 迁移、配置键和令牌桶**

删除 `outputFailureChance`、`freezeChance`、`freezeInterval*`、`freezeDurationTicks` 及随机诅咒方法；保留饰品基础增益、Curios 不可取下和死亡保留。第七项修复时复制修复 NBT 到 `gear_heart_enchanted`，清除仅用于旧破损模型的 `CustomModelData`。

- [ ] **Step 4: 运行测试确认通过**

Run: 同 Step 2
Expected: PASS；随后 `gradle build --no-daemon` 通过。

---

### Task 3: 所有权、Create 容量聚合与 RPM 释限

**Files:**
- Create: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/quota/GearHeartOwnerResolver.java`
- Create: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/quota/CreateCapacityRegistry.java`
- Modify: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/KineticNetworkMixin.java`
- Modify: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/RpmLimitManager.java`
- Test: `mods/dimensionworks-rpm-limit/src/test/java/dev/szx/dimensionworks/rpmlimit/quota/CreateCapacityRegistryTest.java`
- Test: `mods/dimensionworks-rpm-limit/src/test/java/dev/szx/dimensionworks/rpmlimit/mixin/KineticNetworkMixinContractTest.java`

**Interfaces:**
- Produces: `GearHeartOwnerResolver.SHARED_OWNER`。
- Produces: `GearHeartOwnerResolver.owner(KineticBlockEntity) -> UUID`
- Produces: `CreateCapacityRegistry.limitNaturalCapacity(KineticNetwork, float naturalCapacity) -> float`
- Produces: `CreateCapacityRegistry.register(KineticNetwork, UUID owner, float naturalCapacity)`
- Produces: `CreateCapacityRegistry.clearRemoved()`，只清理弱键和已移除网络。
- Consumes: `GearHeartQuotaPolicy.limit(CREATE_CAPACITY, repairedMask)`。

- [ ] **Step 1: 写失败测试**

```java
@Test
void scalesOwnerAggregateTo4096WithoutExceedingIt() {
    Map<UUID, Double> raw = Map.of(ownerA, 3000.0, ownerB, 5000.0);
    assertEquals(3000.0, CreateCapacityRegistry.scaleBucket(raw, ownerA, 4096.0));
    assertEquals(4096.0, CreateCapacityRegistry.scaleBucket(raw, ownerB, 4096.0));
}

@Test
void repairedCreateCapacityReturnsNaturalValue() {
    assertEquals(9000.0F, registry.limitNaturalCapacity(network, 9000.0F, 0b0000010));
}
```

- [ ] **Step 2: 运行失败测试**
- [ ] **Step 3: 实现弱引用网络注册表和 `calculateCapacity` 返回值缩放**

`KineticNetworkMixin` 必须在原生 `calculateCapacity` RETURN 后调用聚合，且不得再次调用 `calculateCapacity`。所有者为空时使用 `SHARED_OWNER`；共享用量对每个仍受诅咒的装备者都计入预算。第二项修复、最终状态或没有受诅咒装备者时直接返回原生容量。

- [ ] **Step 4: 运行测试与 `gradle build --no-daemon`**

---

### Task 4: Mekanism 矿物、跨工序和活跃机器门控

**Files:**
- Create: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/quota/MekProcessingContext.java`
- Create: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/quota/MekProcessingGate.java`
- Create: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/mek/TileEntityMekanismMixin.java`
- Create: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/mek/CachedRecipeMixin.java`
- Modify: `mods/dimensionworks-rpm-limit/src/main/resources/dimensionworks_rpm_limit.mixins.json`
- Test: `mods/dimensionworks-rpm-limit/src/test/java/dev/szx/dimensionworks/rpmlimit/quota/MekProcessingGateTest.java`
- Test: `mods/dimensionworks-rpm-limit/src/test/java/dev/szx/dimensionworks/rpmlimit/mixin/mek/MekMixinContractTest.java`

**Interfaces:**
- Produces: `MekProcessingContext.push(TileEntityMekanism)` / `pop()`，同一服务器主线程有效。
- Produces: `MekProcessingGate.allowOperations(int operations, Recipe<?> recipe, UUID owner, long tick) -> int`
- Produces: `MekProcessingGate.markActive(UUID owner, Object machine, long tick)`
- Produces: `MekProcessingGate.isActive(UUID owner, Object machine, long tick) -> boolean`
- Consumes: `GearHeartQuotaType.MEK_ORE_OPERATIONS`、`CROSS_PROCESS`、`ACTIVE_MACHINES`。
- Produces: `CachedRecipeMixin` 对 `CachedRecipe#process` 中 `OperationTracker.currentMax` 的 `@Redirect`，额度不足时返回 `0`。

- [ ] **Step 1: 写失败合同测试**
- [ ] **Step 2: 确认 Mixin 目标存在且测试失败**
- [ ] **Step 3: 实现上下文、实际操作数门控和活跃机器 tick 注册**

矿物处理识别富化、粉碎、净化、化学注入及对应 `FactoryType.ENRICHING/CRUSHING/PURIFYING/INJECTING`。跨工序通过 `ItemStackToItemStackRecipe#getInput().getRepresentations()` 命中 `gear_heart:cross_process`。额度不足时返回 `0`，不消耗输入、不产出、不递增 `operatingTicks`、不重置已有进度；同 tick 的 `operations` 一次申请全部令牌。

- [ ] **Step 4: 运行测试与 `gradle build --no-daemon`**

---

### Task 5: AE2 并发、活跃 CPU 与最终输出门控

**Files:**
- Create: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/quota/AeCraftingRegistry.java`
- Create: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/quota/AeCraftingGate.java`
- Create: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/ae2/CraftingServiceMixin.java`
- Create: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/ae2/CraftingCpuLogicMixin.java`
- Modify: `mods/dimensionworks-rpm-limit/src/main/resources/dimensionworks_rpm_limit.mixins.json`
- Test: `mods/dimensionworks-rpm-limit/src/test/java/dev/szx/dimensionworks/rpmlimit/quota/AeCraftingGateTest.java`
- Test: `mods/dimensionworks-rpm-limit/src/test/java/dev/szx/dimensionworks/rpmlimit/mixin/ae2/AeMixinContractTest.java`

**Interfaces:**
- Produces: `AeCraftingRegistry.register(CraftingService, UUID owner)`
- Produces: `AeCraftingRegistry.busyCount(UUID owner) -> int`
- Produces: `AeCraftingGate.allowSubmission(CraftingService, ICraftingPlan, UUID owner, long tick) -> boolean`
- Produces: `AeCraftingGate.allowFinalOutput(UUID owner, AEKey key, long amount, long tick) -> boolean`
- Consumes: `GearHeartQuotaType.AE_CRAFTING_JOBS`、`ACTIVE_MACHINES`、`FINAL_PRODUCTION`。
- Produces: `CraftingServiceMixin` 在 `submitJob` 的 `job.simulation()` 分支之后执行门控；模拟任务不占额度。
- Produces: `CraftingCpuLogicMixin` 在 `CraftingCpuLogic#insert` 的最终输出进入 `job.link` 前申请令牌；拒绝时在修改 `job.waitingFor` 和 `job.remainingAmount` 前返回 `0`。

- [ ] **Step 1: 写失败合同测试**
- [ ] **Step 2: 确认 `CraftingService#submitJob` 与 `CraftingCpuLogic#insert` 目标存在**
- [ ] **Step 3: 实现服务弱注册、忙碌 CPU 聚合、80 并发上限和最终输出令牌门控**
- [ ] **Step 4: 运行测试与 `gradle build --no-daemon`**

---

### Task 6: Create/Mek 高阶产物输出门控

**Files:**
- Modify: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/GearHeartEffects.java`
- Modify: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/BasinOperatingBlockEntityMixin.java`
- Modify: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/MillstoneBlockEntityMixin.java`
- Modify: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/SawBlockEntityMixin.java`
- Modify: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/MechanicalPressBlockEntityMixin.java`
- Modify: `mods/dimensionworks-mek-stress/src/main/java/dev/szx/dimensionworks/mekstress/mixin/mek/OutputHelperMixin.java`
- Create: `mods/dimensionworks-rpm-limit/src/main/java/dev/szx/dimensionworks/rpmlimit/quota/FinalProductionGate.java`
- Test: `mods/dimensionworks-rpm-limit/src/test/java/dev/szx/dimensionworks/rpmlimit/quota/FinalProductionGateTest.java`
- Test: `mods/dimensionworks-rpm-limit/src/test/java/dev/szx/dimensionworks/rpmlimit/mixin/FinalProductionMixinContractTest.java`

**Interfaces:**
- Produces: `FinalProductionGate.tryConsume(UUID owner, ItemStack output, long amount, long tick) -> boolean`
- Produces: `FinalProductionGate.tryConsume(UUID owner, FluidStack/ChemicalStack output, long amount, long tick) -> boolean`
- Consumes: `gear_heart:final_production` 标签、`FINAL_PRODUCTION` 额度。
- Produces: 标签未命中、没有受诅咒装备者或第七项已修复时始终返回 `true`。
- Produces: 共享无所有者系统对每个仍受诅咒装备者分别记账，任何一个额度不足则拒绝。

- [ ] **Step 1: 写失败测试**
- [ ] **Step 2: 确认现有 Create/Mek 输出边界和 Mixin 目标**
- [ ] **Step 3: 在确认本轮能完成且输出空间足够的位置申请令牌**
- [ ] **Step 4: 额度不足时不开始/不插入，不吞输入、不删除已生成产物**
- [ ] **Step 5: 运行测试与两个 Mod 构建**

---

### Task 7: KubeJS 物品、配方、标签、语言和贴图

**Files:**
- Modify: `kubejs/startup_scripts/gear_heart.js`
- Create: `kubejs/server_scripts/gear_heart_recipes.js`
- Create: `kubejs/data/gear_heart/tags/items/final_production.json`
- Create: `kubejs/data/gear_heart/tags/items/cross_process.json`
- Modify: `kubejs/assets/kubejs/lang/zh_cn.json`
- Modify: `kubejs/assets/kubejs/lang/en_us.json`
- Modify: `scripts/pixelate_gear_heart_textures.py`
- Modify: `scripts/README.md`
- Create/Generate: `kubejs/assets/kubejs/textures/item/gear_heart_repair_fragment_1.png` 至 `_7.png`
- Create/Generate: `docs/assets/gear_heart_fragments_preview.png`

**Interfaces:**
- Produces: 七个物品 ID 与固定配方 ID `kubejs:gear_heart_repair_fragment_<n>`。
- Produces: 1：Create pressing `create:andesite_alloy`。
- Produces: 2：Create 压片组合 `create:iron_sheet` + `create:brass_sheet`。
- Produces: 3：Mekanism enriching `create:precision_mechanism`。
- Produces: 4：Create mechanical crafting，模式 `LCL / EPE / LCL`。
- Produces: 5：Create mechanical crafting，模式 `SAS / ADA / SCS`。
- Produces: 6：Create mechanical crafting，模式 `GUG / EAE / GUG`。
- Produces: 7：Create mechanical crafting，模式 `APA / MCM / APA`。
- Produces: `gear_heart:final_production` 和 `gear_heart:cross_process` 标签 JSON。

- [ ] **Step 1: 写 KubeJS/资源失败检查**
- [ ] **Step 2: 注册七个独立贴图并生成最近邻放大预览**
- [ ] **Step 3: 写七个固定 ID 配方、两个标签和中英文 tooltip**
- [ ] **Step 4: 运行 `node --check kubejs/**/*.js`、JSON 解析和贴图尺寸检查**

---

### Task 8: FTB 任务、配置、文档与版本

**Files:**
- Modify: `config/ftbquests/quests/chapters/07DDD0CAE18A7217.snbt`
- Modify: `defaultconfigs/dimensionworks_rpm_limit-common.toml`
- Modify: `config/dimensionworks_rpm_limit-common.toml`
- Modify: `mods/dimensionworks-rpm-limit/README.md`
- Modify: `kubejs/README.md`
- Modify: `mods/dimensionworks-rpm-limit/build.gradle`
- Modify: `mods/dimensionworks-rpm-limit/src/main/resources/META-INF/mods.toml`
- Modify: `manifest/mods.json`

**Interfaces:**
- Produces: 保留初始齿轮之心任务，新增七个严格串联的碎片任务和一个最终任务。
- Produces: `[gearHeart]` 仅包含规格列出的 13 个额度键；删除旧随机诅咒键。
- Produces: `dimensionworks-rpm-limit` 版本 `0.4.5`，`manifest/mods.json` 同步 `0.4.5`。
- Produces: 根 `VERSION` 不修改。
- Produces: README 说明七阶段、双向旋转、原模组限制和未完成游戏内验收项。

- [ ] **Step 1: 写任务/版本/配置合同测试**
- [ ] **Step 2: 更新任务图、配置、文档和三处版本**
- [ ] **Step 3: 运行任务图、JSON、KubeJS 和版本检查**

---

### Task 9: 全量验证与交付

- [ ] **Step 1: 在 worktree 运行 `cd mods/dimensionworks-rpm-limit && gradle build --no-daemon`**
- [ ] **Step 2: 在 worktree 运行 `cd mods/dimensionworks-mek-stress && gradle build --no-daemon`**
- [ ] **Step 3: 运行 `node --check` 检查全部 KubeJS 脚本**
- [ ] **Step 4: 解析 `kubejs/data`、`kubejs/assets`、`config/ftbquests` JSON**
- [ ] **Step 5: 运行 `python3 scripts/pack_hook.py --skip-build`**
- [ ] **Step 6: 运行 `python3 scripts/package_mrpack.py`；只保留钩子错误日志或一行成功信息**
- [ ] **Step 7: 进行最终代码审查，确认没有随机诅咒、旧配置键、重复任务 ID 或未固定的 Mod 版本**
- [ ] **Step 8: 列出构建产物绝对路径；未进游戏的运行时项目明确标为未验证**

## Execution Notes

- 每个任务先由一个实现子 agent 提交，再由一个审查子 agent 检查规格合规和质量；共享文件只能由一个任务触碰。
- 发现计划与规格冲突时以规格为准，并在子 agent 工作区 ledger 中记录 ruling。
- 方向修复已由 Task 1 完成并构建；后续任务不得回退该提交。
- 任务 2-6 是 Java 行为核心，任务 7-8 是内容与交付层，任务 9 只做验证，不在验证阶段补写未测试的生产代码。
