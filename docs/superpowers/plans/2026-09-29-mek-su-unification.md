# MEK SU 统一与应力守恒实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 删除输出卡路线，把合格 Mekanism 加工机器的 FE 成本按 `2.5 FE = 1 SU` 统一为 SU，并让 ME Gearbox EXPORT 只向下游发放实际扣除的应力。

**Architecture:** 纯数学放入 `core` 包并以 JUnit 先测；Mixin 只负责把游戏中真实对象接到这些边界上。ME Network 的 `q` 仍负责全局库存和带宽调度，Create 网络容量由本 Mod 的齿轮箱桥接层按实际扣费动态计算。

**Tech Stack:** Java 17、Forge 1.20.1、Mixin、JUnit 5、Create 6.0.8、Applied Create 1.1.5、Mekanism 10.4.16、AE2 15.4.10、Jade 11.13.2。

**Spec:** `docs/superpowers/specs/2026-09-29-mek-su-unification-design.md`

## Global Constraints

- 只改 `StressRules.isEligible` 覆盖的 Mekanism 加工机器，不改能量设备、线缆、充电台、发电机和多方块。
- 换算默认值固定为 `2.5 FE = 1 SU`，SU 消耗向上取整。
- 不复制第三方贴图；Memory Drive 直接引用 `ae2:textures/guis/drive.png`。
- 删除 `stress_output_card` 的全部注册、配方、资源、总线逻辑和 Jade 显示。
- 不改第三方 Mod 源码；依赖变化只通过 Mixin 和本仓库资源完成。
- 每个生产行为先有失败测试，再写最小实现并重新运行测试。

---

### Task 1: FE→SU 与齿轮箱供能数学

**Files:**
- Create: `mods/dimensionworks-mek-stress/src/main/java/dev/szx/dimensionworks/mekstress/core/SuConversion.java`
- Create: `mods/dimensionworks-mek-stress/src/main/java/dev/szx/dimensionworks/mekstress/core/StressTransfer.java`
- Test: `mods/dimensionworks-mek-stress/src/test/java/dev/szx/dimensionworks/mekstress/core/SuConversionTest.java`
- Test: `mods/dimensionworks-mek-stress/src/test/java/dev/szx/dimensionworks/mekstress/core/StressTransferTest.java`

**Interfaces:**
- Produces: `SuConversion.ceilFromFe(long fe, double fePerSu) -> long`
- Produces: `StressTransfer.outputRpm(int configuredRpm, double q) -> int`
- Produces: `StressTransfer.chargedSu(long requestedSu, long availableSu) -> long`
- Produces: `StressTransfer.capacityPerRpm(long chargedSu, int outputRpm) -> float`
- Produces: `StressTransfer.fullSpeedLoad(Iterable<Float> stressPerRpm, int configuredRpm) -> double`

- [ ] **Step 1: 写失败测试**

```java
@Test
void oneFePointFiveRoundsUpToASu() {
    assertEquals(1L, SuConversion.ceilFromFe(3L, 2.5D));
}

@Test
void gearboxNeverAdvertisesMoreThanChargedSu() {
    assertEquals(700L, StressTransfer.chargedSu(1_000L, 700L));
    assertEquals(10.9375F, StressTransfer.capacityPerRpm(700L, 64), 1.0E-6F);
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `gradle test --tests '*SuConversionTest' --tests '*StressTransferTest' --no-daemon`
Expected: FAIL，提示类或方法不存在。

- [ ] **Step 3: 写最小实现**

实现向上取整、非负钳制、`round(configuredRpm * q)`、`min(requested, available)`、`charged / rpm` 和按 RPM 汇总 stress-per-RPM。

- [ ] **Step 4: 运行测试确认通过**

Run: `gradle test --tests '*SuConversionTest' --tests '*StressTransferTest' --no-daemon`
Expected: PASS。

---

### Task 2: MEK 处理成本与 Jade 净化

**Files:**
- Create: `mods/dimensionworks-mek-stress/src/main/java/dev/szx/dimensionworks/mekstress/memory/ProcessingEnergyRegistry.java`
- Create: `mods/dimensionworks-mek-stress/src/main/java/dev/szx/dimensionworks/mekstress/integration/jade/JadeEnergySanitizer.java`
- Create: `mods/dimensionworks-mek-stress/src/main/java/dev/szx/dimensionworks/mekstress/mixin/mek/JadeDataProviderMixin.java`
- Modify: `MachineEnergyContainerMixin.java`
- Modify: `MachinePowerManager.java`
- Test: `ProcessingEnergyRegistryTest.java`
- Test: `JadeEnergySanitizerTest.java`

**Interfaces:**
- Consumes: `SuConversion.ceilFromFe(long, double)`
- Produces: `ProcessingEnergyRegistry.record(GlobalPos, long tick, long fe)`
- Produces: `ProcessingEnergyRegistry.consumeConvertedSu(GlobalPos, long tick, double fePerSu) -> long`
- Produces: `JadeEnergySanitizer.removeEnergyElements(CompoundTag data, boolean eligible) -> boolean`

- [ ] **Step 1: 写失败测试**

```java
@Test
void recordsAndSumsFePerTick() {
    registry.record(pos, 10L, 5L);
    registry.record(pos, 10L, 3L);
    assertEquals(4L, registry.consumeConvertedSu(pos, 10L, 2.5D));
    assertEquals(0L, registry.consumeConvertedSu(pos, 11L, 2.5D));
}

@Test
void sanitizerRemovesOnlyEnergyEntries() {
    assertTrue(JadeEnergySanitizer.removeEnergyElements(data, true));
    assertFalse(data.getList("mekData", 10).getCompound(0).contains("energy"));
    assertTrue(data.getList("mekData", 10).getCompound(0).contains("text"));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `gradle test --tests '*ProcessingEnergyRegistryTest' --tests '*JadeEnergySanitizerTest' --no-daemon`
Expected: FAIL。

- [ ] **Step 3: 写最小实现并接入 Mixin**

`MachineEnergyContainerMixin.extract` 在内部加工成功时记录请求 FE；`MachinePowerManager.updateDirectStress` 使用上一 tick 的换算 SU。Jade Mixin 在作为参数判定合格后调用净化器。

- [ ] **Step 4: 运行测试确认通过**

Run: `gradle test --tests '*ProcessingEnergyRegistryTest' --tests '*JadeEnergySanitizerTest' --no-daemon`
Expected: PASS。

---

### Task 3: 删除输出卡与 AE 机器路由

**Files:**
- Delete: `card/StressOutputCardItem.java`
- Delete: `memory/MachinePowerRegistry.java`
- Delete: `mixin/ae2/ExportBusPartMixin.java`
- Delete: `mixin/ae2/IOBusPartMixin.java`
- Delete: output-card assets
- Modify: `DimensionWorksMekStress.java`
- Modify: `IMemoryGridService.java`
- Modify: `MemoryGridService.java`
- Modify: `StressRules.java`
- Modify: `MemoryJadePlugin.java`
- Modify: `dimensionworks_mek_stress.mixins.json`
- Modify: `kubejs/startup_scripts/me_memory.js`
- Modify: `kubejs/server_scripts/me_memory.js`
- Test: `ResourceRetirementTest.java`

**Interfaces:**
- Produces: 无 `STRESS_OUTPUT_CARD` 注册、无 AE 输出总线 mixin、无 `reportOutputBus` 接口。

- [ ] **Step 1: 写失败测试**

断言 mixin JSON 不含 `ae2.ExportBusPartMixin`/`IOBusPartMixin`，主源码不含 `STRESS_OUTPUT_CARD`，KubeJS 不含输出卡配方与创造标签条目。

- [ ] **Step 2: 运行测试确认失败**

Run: `gradle test --tests '*ResourceRetirementTest' --no-daemon`
Expected: FAIL，现有资源仍存在。

- [ ] **Step 3: 删除最小文件集并清理引用**

同时从 Jade 移除 Bus data provider/component，从 `MemoryGridService` 移除 `machineDemands`，保留齿轮箱 demand。

- [ ] **Step 4: 运行测试确认通过**

Run: `gradle test --tests '*ResourceRetirementTest' --no-daemon`
Expected: PASS。

---

### Task 4: 应力守恒齿轮箱 Mixin

**Files:**
- Modify: `mixin/appliedcreate/MEGearboxBlockEntityMixin.java`
- Test: `MEGearboxMixinContractTest.java`

**Interfaces:**
- Consumes: `StressTransfer` 与 `IMemoryGridService`
- Produces: 动态 `getGeneratedSpeed()`、动态 `calculateAddedStressCapacity()` 与精确扣费 `tickExport`。

- [ ] **Step 1: 写失败契约测试**

通过反射断言 mixin 同时声明 `getGeneratedSpeed`、`calculateAddedStressCapacity` 和 `tickExport` 注入处理器，并断言不再有缩放配置转速的 `setConfiguredSpeed` 调用处理器。

- [ ] **Step 2: 运行测试确认失败**

Run: `gradle test --tests '*MEGearboxMixinContractTest' --no-daemon`
Expected: FAIL。

- [ ] **Step 3: 写最小 Mixin**

取消原版 `tickExport`，用 `KineticNetwork.members` 的 stress-per-RPM 与配置 RPM 计算满速负载；上报需求；按 `q` 计算输出 RPM；模拟/执行精确 SU 扣除；更新 unique 输出状态并调用 `updateGeneratedRotation()`。

- [ ] **Step 4: 运行测试确认通过**

Run: `gradle test --tests '*MEGearboxMixinContractTest' --no-daemon`
Expected: PASS。

---

### Task 5: Memory Drive 4×5 UI

**Files:**
- Create: `core/MemoryDrivePaging.java`
- Modify: `memory/MemoryDriveMenu.java`
- Modify: `client/MemoryDriveScreen.java`
- Test: `MemoryDrivePagingTest.java`

**Interfaces:**
- Produces: `MemoryDrivePaging.SLOTS_PER_PAGE = 20`
- Produces: `MemoryDrivePaging.pageCount(int totalSlots) -> int`
- Produces: `MemoryDrivePaging.pageOffset(int page) -> int`
- Produces: `MemoryDrivePaging.slotX(int index) -> int`
- Produces: `MemoryDrivePaging.slotY(int index) -> int`

- [ ] **Step 1: 写失败测试**

```java
assertEquals(4, MemoryDrivePaging.pageCount(64));
assertEquals(20, MemoryDrivePaging.pageCount(4), "至少保留一页");
assertEquals(20, MemoryDrivePaging.pageOffset(1));
assertEquals(18, MemoryDrivePaging.slotX(1));
assertEquals(20, MemoryDrivePaging.slotY(1));
```

- [ ] **Step 2: 运行测试确认失败**

Run: `gradle test --tests '*MemoryDrivePagingTest' --no-daemon`
Expected: FAIL。

- [ ] **Step 3: 写最小实现并重排界面**

背景改为 `new ResourceLocation("ae2", "textures/guis/drive.png")`；`imageWidth=176`、`imageHeight=199`；20 个槽位放在左侧紧凑区域；按钮受 `menu.pageCount()` 限制。

- [ ] **Step 4: 运行测试确认通过**

Run: `gradle test --tests '*MemoryDrivePagingTest' --no-daemon`
Expected: PASS。

---

### Task 6: 配置、文档、版本与完整验证

**Files:**
- Modify: `MekStressConfig.java`
- Modify: `defaultconfigs/dimensionworks_mek_stress-common.toml`
- Modify: `config/dimensionworks_mek_stress-common.toml`
- Modify: lang files
- Modify: `README.md`
- Modify: `build.gradle`
- Modify: `src/main/resources/META-INF/mods.toml`
- Modify: `manifest/mods.json`

**Interfaces:**
- Produces: `MekStressConfig.fePerSu() -> double`
- Produces: 版本 `0.2.2`。

- [ ] **Step 1: 写配置/资源失败测试**

断言配置默认值为 `2.5`、版本三处一致、lang 不再含输出卡、页面文案不再硬编码 7 页。

- [ ] **Step 2: 运行测试确认失败**

Run: `gradle test --tests '*PackagingContractTest' --no-daemon`
Expected: FAIL。

- [ ] **Step 3: 更新配置、文档和版本**

README 描述只保留 Create 直连和 ME Gearbox IMPORT/EXPORT 两条路线，写明换算、守恒和 4×5 分页。

- [ ] **Step 4: 运行全部验证**

Run: `gradle test --no-daemon`
Run: `gradle build --no-daemon`
Expected: 全部 PASS，产物为 `build/libs/dimensionworks_mek_stress-0.2.2.jar`。
