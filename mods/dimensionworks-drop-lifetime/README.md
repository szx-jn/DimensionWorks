# DimensionWorks 掉落物寿命

Minecraft 1.21.1 NeoForge 模块。在掉落物进入服务端世界时查询 FTB Chunks 圈地状态并设置寿命；不增加每 tick 扫描，也不反复延长到期时间。

| 出生位置与稀有度 | 地面寿命 |
|---|---:|
| FTB Chunks 已圈地的区块 | 5 分钟 |
| 未圈地，普通或未知稀有度 | 25 秒 |
| 未圈地，不普通 | 50 秒 |
| 未圈地，稀有 | 75 秒 |
| 未圈地，史诗 | 180 秒 |

本次移植改用 FTB Chunks 的公开类型化 API，删除反射与查询失败时静默按未圈地处理的逻辑。FTB Chunks 是整合包已有必需依赖，模块元数据同步声明这一要求。管理器尚未加载时不存在可查询的圈地，使用稀有度寿命。组件中的稀有度由 `ItemStack#getRarity()` 读取；忽略客户端及非物品实体，区块重新加载的掉落物仍会按现行规则重新设置寿命。

当前目标依赖：FTB Chunks 2101.1.24、FTB Library 2101.1.40、FTB Teams 2101.1.12、Architectury 13.0.12。使用固定 CurseForge 文件和 Modrinth 版本；这组版本满足实际 JAR 声明的依赖下限。

在仓库根目录使用 Java 21：

```sh
./gradlew :dimensionworks-drop-lifetime:build --no-daemon
./gradlew :dimensionworks-drop-lifetime:runGameTestServer --no-daemon
```

NeoForge 构建及实际 GameTest 已通过；GameTest 覆盖带四种稀有度组件的掉落物进入未圈地区块时的寿命。已圈地的 5 分钟规则仍须在目标完整整合包与真实玩家场景中验收。
