# DimensionWorks Wrench Compatibility

为 Create 扳手补上 FTB Ultimine 的跨模组连锁右键路径。这个 Mod 不修改 Create、AE2、Mekanism 或 Create Ultimine 的源码。

## 问题

1.20.1 的 Create Ultimine 对每个连锁位置直接调用 `ItemStack#useOn`。Create 自己的 `IWrenchable` 方块会响应，但 AE2 的拆除逻辑挂在 Forge `PlayerInteractEvent.RightClickBlock` 上；Mekanism 的配置器又在扳手模式返回 `PASS`，并把拆除放在方块的 `Block#use` 路径中。因此这两种工业方块都不会被 Create Ultimine 的批量 `useOn` 处理。

Mekanism 还有第二层限制：`MekanismUtils.canUseAsWrench` 只接受 Mekanism 配置器或 `mekanism:configurators` 标签，不直接接受 Forge `forge:tools/wrench`。所以 Create 扳手即使单击 Mekanism 方块也可能不按扳手处理。

## 修复

- 通过 KubeJS 把 `create:wrench` 加入 `mekanism:configurators`，让 Mekanism 的单击旋转与拆除路径识别 Create 扳手。
- 注册 FTB Ultimine 的 `RightClickHandler`：当手持 Create 扳手且按住 Shift 时，对每个连锁位置重放 Forge `RightClickBlock` 事件，使 AE2 及其基于 `AEBaseBlockEntity` 的扩展能执行自己的拆除逻辑。
- 对 Mekanism 方块继续走原版 `BlockState#use`，从而保留 Mekanism 的权限检查、NBT 保留、掉落和拆除行为。
- 在重放事件期间临时关闭 FTB 的当前右键处理，避免重入和重复连锁。

## 依赖

- Create 6.0.8
- FTB Ultimine 2001.1.8
- AE2 与 Mekanism 由整合包提供；未安装时不会影响本 Mod 加载

## 构建

```sh
gradle build --no-daemon
```
