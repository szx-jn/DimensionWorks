# DimensionWorks Maid AE2 Crank

为 Minecraft 1.21.1 NeoForge 的车万女仆增加“AE2 手摇曲柄”工作选项。

## 行为

- 女仆会在自身工作范围内寻找连接到可手摇 AE2 方块、且当前确实可以转动的 `ae2:crank`。
- 女仆会走到曲柄外侧并定期调用 AE2 曲柄自身的 `power()` 逻辑。
- 不使用假玩家模拟右键。AE2 会在假玩家触发曲柄交互时主动移除曲柄，因此必须调用方块实体的原生供能方法。
- 当目标没有可进行的工作时，不调用供能方法，避免触发 AE2 对无效摇动的计数和曲柄销毁逻辑。
- 搜索半径最多 8 格；仅在女仆任务检查时搜索附近已加载方块，不因搜索加载区块。原生女仆检查间隔为 20～39 tick，远处目标寻路后重新检查。
- 兼容 Maid Storage Manager 1.15.6 与 Create: Crafted Beginning 0.1.1 共用 Create `create:compacting` / `create:mixing` 配方类型的行为：将非 Create 标准压缩/搅拌配方从 Msm 对应缓存中排除，不影响它们在 Create 冷加工设备中的使用。

## 依赖

- Minecraft 1.21.1 NeoForge
- Applied Energistics 2 19.2.20
- Touhou Little Maid 1.5.3-neoforge+mc1.21.1

## 构建与验证

在仓库根目录：

```sh
./gradlew :dimensionworks-maid-ae-crank:build :dimensionworks-maid-ae-crank:runGameTestServer
```

原生构建与 8 项实际服务器 GameTest 通过：扩展任务注册、当前女仆任务/UUID/七个钻石保存重载、近处曲柄供能、满能量不开始与不损毁、远处目标先写入寻路、最近可用曲柄选择、移除目标后的任务/记忆清理、未加载区域不新增区块。GameTest 运行时同时加载固定的 Create: Crafted Beginning 0.1.1 和 Maid Storage Manager 1.15.6，用于覆盖共享压缩配方类型的启动回归。另一个夹具实际加入女仆实体，由原生 Brain 自主寻路并摇动曲柄给充能器供能，没有手动任务 tick。

任务 ID `dimensionworks_maid_ae_crank:ae2_crank_task` 保持，图标返回副本防止调用方修改共享物品。没有新增网络协议或自研物品/创造页签。测试类与结构不进入分发 JAR。完整客户端任务页面、多人长期运行和原服务器旧女仆档转换仍待整包验收。
