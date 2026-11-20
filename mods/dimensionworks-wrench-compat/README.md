# DimensionWorks Wrench Compatibility

Minecraft 1.21.1 NeoForge 的跨模组批量扳手处理。通过 FTB Ultimine 的公开右键接口，在手持 Create 扳手且按住 Shift 时，对所选位置执行 AE2 的事件拆除与 Mekanism 的方块交互。仅处理已加载位置，不创建实体或强制加载区块；不修改第三方源码。

AE2 的拆除挂在 `PlayerInteractEvent.RightClickBlock`，Mekanism 1.21.1 的拆除位于 `BlockState#useItemOn`。处理器先重放 NeoForge 事件；取消且消费交互时计为成功，方块交互被拒绝时保留机器。Mekanism 再调用 `useItemOn`，仅在明确返回 `PASS_TO_DEFAULT_BLOCK_INTERACTION` 时执行 `useWithoutItem`，保留模组自身的拆除、掉落与数据处理。

扳手判定使用 `c:tools/wrench`，射线范围使用原版 `blockInteractionRange()`。处理期间临时释放 FTB 按键，`finally` 恢复，线程内重入保护防止重复连锁。目标 Mekanism 已直接识别当前 Create 扳手；真实测试未另加旧版 `mekanism:configurators` 标签桥接。

## 固定依赖

- Create 6.0.10（官方固定文件 `UjX6dr61`，编译依赖取自同一发行 JAR）。
- FTB Ultimine 2101.1.15（CurseForge 8231400），Architectury 13.0.12，FTB Library 2101.1.40。
- AE2 19.2.20 与 Mekanism 10.7.19.85 在完整整包中提供；本模块元数据仍标为可选。GameTest 独立加载两者及 GuideME 21.1.22。

## 本地验证

在仓库根目录使用 Java 21：

```sh
./gradlew :dimensionworks-wrench-compat:build :dimensionworks-wrench-compat:runGameTestServer --no-daemon
```

构建与两项实际服务器 GameTest 通过：同一批次实际拆除 AE2 能量接收器和 Mekanism 基础能量立方并恢复 FTB 按键；其他模组拒绝方块交互时保留机器。测试夹具在登录前声明 NeoForge 和 Architectury 的模拟连接通道，使用普通服务端玩家实体。仍须实测客户端连锁选区、FTB 圈地权限、带库存机器掉落与完整产线，不能把这两项测试视为旧档迁移验收。
