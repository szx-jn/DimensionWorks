# 超速并行批处理实施计划

**目标：** 给 `dimensionworks-rpm-limit` 加一条超速增益：转速超过机器自身饱和点后，同一次操作按倍数并行处理多份原料与产出，
应力随倍数放大，护目镜显示当前倍数，渲染动画保持在机器原本的饱和转速。

**规格：** `docs/superpowers/specs/2026-09-25-overspeed-batch-design.md`

## 全局约束

- Minecraft 1.20.1、Forge 47、Java 17，Create 依赖保持 `ordering="BEFORE"`。
- Mixin 配置保持 `required: true` 与 `injectors.defaultRequire: 1`，注入失败必须硬报错而不是静默失效。
- 不写随机数、不新增存档字段、不新增同步包。
- 不重跑整段 `tick()`；只重复 Create 原生的配方/加工完成入口，动画状态机保持每 tick 一次。
- 默认曲线：`1:1,2:2,4:2.5,8:3.5,10:5,12:7,14:8.5,16:10,18:11.5,20:15`，封顶 15×。

---

### Task 1：曲线与批量计数（纯逻辑）

**文件：** `OverspeedCurve.java`

- [x] `parse(String)` 解析 `ratio:multiplier` 表，含空串、格式错误、越界与排序处理。
- [x] `step(points, ratio, cap)` 阶跃取值并封顶。
- [x] `multiplier(rpm, saturation, cap)` 用绝对值换算比例，支持反转。
- [x] `batches(multiplier, tick)` 用楼层差分实现确定性小数批次，长期平均严格等于倍数。
- [x] `reload(spec, warn)` 解析失败时保留上一张表并回调告警。

### Task 2：饱和点、工作批次与视觉上限

**文件：** `OverspeedBonus.java`

- [x] 登记搅拌机 512、压力机 512、动力锯 3072、风箱 256、磨石 8192，白名单机器用配置回退值。
- [x] `multiplierOf` 读取原始理论速度后再按玩家上限封顶，避免过载抖动并遵守有效 RPM 上限。
- [x] `extraBatches` 只为服务端计算额外工作次数，`ThreadLocal` 防重入。
- [x] `animationSpeed` 将渲染转速限制在机器自身饱和点，不影响工作与应力。
- [x] `stressMultiplier` 按 `calculateStressApplied` 的返回值放大。
- [x] `fanExtraHandlerPasses` 只重复风箱的物品处理通道。

### Task 3：工作与渲染 Mixin

**文件：** `BasinOperatingBlockEntityMixin.java`、`MillstoneBlockEntityMixin.java`、`PressingBehaviourMixin.java`、客户端渲染 Mixin

- [x] 盆式配方完成入口 → 重复应用配方，不重跑搅拌机/压力机 `tick()`。
- [x] 磨石加工完成入口 → 重复 `process()`。
- [x] 世界物品压力机 → 重复 `applyInWorld()`。
- [x] Flywheel `RotatingInstance#setup` 与非 Flywheel `KineticBlockEntityRenderer#getAngleForBe` → 使用封顶视觉转速。
- [x] 搅拌头旋转方法 → 使用封顶视觉转速。
- [x] `KineticBlockEntity#calculateStressApplied` RETURN → 乘应力倍率。
- [x] `KineticBlockEntity#addToTooltip` RETURN → 追加「超速并行 ×N」。
- [x] `AirCurrent#tickAffectedHandlers` TAIL → 风箱额外物品处理次数。
- [x] 所有新增 Mixin 注册进 common/client 配置。

### Task 4：配置、语言与版本

- [x] `RpmLimitConfig` 新增 `[overspeed]` 六项配置。
- [x] `DimensionWorksRpmLimit` 在配置加载/重载时重新解析断点表。
- [x] 语言键 `create.gui.stressometer.overspeed_batch`，中英各一条。
- [x] 当前版本为 `0.3.3`：`build.gradle`、`mods.toml`、`manifest/mods.json`。
- [x] `defaultconfigs/dimensionworks_rpm_limit-common.toml` 同步 `[overspeed]` 段。

### Task 5：测试与构建

- [x] `OverspeedCurveTest` 覆盖断点边界、封顶、比例换算、反转、解析失败回退、小数平均、下限。
- [x] `gradle clean build --no-daemon` 通过。
- [x] 新 Jar 放进游戏实例并删除旧版本。
- [x] 重新打包 `.mrpack`。

### Task 6：游戏内验收

- [ ] 搅拌机、压力机和磨石在超速时动画保持饱和转速，不再跳帧或加速抽动。
- [ ] 工作倍率仍按断点表生效，原料不足时不多产出也不卡机。
- [ ] 同转速下应力读数同步放大到 N 倍，超出网络容量时稳定过载而不抖动。
- [ ] 风箱 5120 RPM 到顶 15×，且玩家站在气流里不会被推到 15 倍距离。
- [ ] 护目镜在倍数大于 1 时显示「超速并行 ×N」。
