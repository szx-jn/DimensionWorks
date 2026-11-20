# DimensionWorks FTB Chunks 强加载限制

Minecraft 1.21.1 NeoForge 模块，针对固定版本 FTB Chunks 2101.1.24。保留圈地、取消圈地、保护、团队与其他模组的区块票据，禁止 FTB Chunks 自身的强加载。

- 服务端拒绝强加载申请、正数强加载时间、启动恢复及添加票据，允许清除旧票据。
- 地图隐藏强加载标记、提示和期限调整，并拦截 Shift+左键强加载操作。
- 离线强加载权限直接返回 false，避免停服时再访问已经关闭的权限提供者；其他权限与玩家退出清理保持完整。

旧的强加载时间字段不等于实际票据；本模块不删除圈地记录，也不修改 Minecraft 的通用区块票据 API。一次完整重启才会加载更新后的 Mixin。

本次迁移按目标 JAR 核对注入位置：平台类切换为 `ftbchunks.neoforge.FTBChunksExpectedImpl`，地图提示的读取改为目标版本真实的 lambda 方法。拒绝结果和鼠标按钮使用公开类型，删除反射。所有运行期目标仍由 Mixin 的必需注入校验检查；目标变动不应静默失效。

在仓库根目录使用 Java 21：

```sh
./gradlew :dimensionworks-ftbchunks-no-forceload:build --no-daemon
./gradlew :dimensionworks-ftbchunks-no-forceload:runGameTestServer --no-daemon
```

本地 NeoForge 构建与真实 FTB GameTest 已通过：创建服务端团队、圈地成功，强加载请求被拒绝，正数强加载时间无法生效，随后取消圈地成功。客户端地图注入位置已核对目标字节码，仍须实际客户端验收；本测试也不代替旧圈地存档恢复或完整整合包停服验收。
