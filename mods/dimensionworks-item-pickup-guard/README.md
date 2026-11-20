# DimensionWorks 拾取保护

Minecraft 1.21.1 NeoForge 模块，处理 Create 传送带与 Sophisticated Backpacks 拾取或磁铁升级同时读取同一掉落物时的重复拾取问题。

已经移除的 `ItemEntity#getItem()` 返回 `ItemStack.EMPTY`，阻止第二个拾取器再次获取旧物品栈。存活实体及部分转移的剩余物品仍然可用，不禁用第三方拾取功能。

在仓库根目录使用 Java 21：

```sh
./gradlew :dimensionworks-item-pickup-guard:build --no-daemon
./gradlew :dimensionworks-item-pickup-guard:runGameTestServer --no-daemon
```

Mixin 通过 `META-INF/neoforge.mods.toml` 声明，使用 NeoForge 官方映射，不生成 Forge SRG refmap。`src/gameTest/` 独立于正式 JAR。本地构建和两项真实 GameTest 已通过：已移除实体不能再次转移，部分转移仍保留余量。仍须在目标整合包中验证 Create 与背包升级同时工作的产线。
