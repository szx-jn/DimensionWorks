# DimensionWorks Stress Gauge Alarm

Minecraft 1.21.1 / NeoForge 21.1.267 / Create 6.0.10 扩展。Create 应力表所在动力网络过载时，应力表向所有方向提供 15 级弱红石信号；过载解除或动力停止后信号归零。信号通过原版方块红石查询接口提供，红石粉的传播与衰减由 Minecraft 原版处理。

仅当应力表映射出的模拟红石强度发生变化时，才通知邻居和比较器；网络容量或应力数值改变但输出档位不变时跳过邻居传播。此项目尚未加入 `manifest/mods.json`，因此暂不参与整合包打包。

Java 单元测试覆盖 Create 相同的 0–15 输出取整与夹取规则。NeoForge GameTest 会加载实际 Create 应力表及 Mixin，验证 50% 应力对应 7 级比较器输出、过载对应 15 级红石输出。Create 的 `updateFromNetwork` 注入使用 `remap=false`；红石查询方法限定在 Create `GaugeBlock`，不再注入所有 Minecraft 方块；仅应力表声明为红石源。

从仓库根目录使用 Java 21 执行 `./gradlew :dimensionworks-stress-gauge-alarm:build :dimensionworks-stress-gauge-alarm:runGameTestServer --no-daemon`。目标构建、2 项 JUnit 与真实 GameTest 已通过；测试也覆盖相邻红石粉收到 15 级输出、解除过载后清零与动力停止后的比较器清零；编译使用固定官方 Create 发行文件 `UjX6dr61`，其内嵌 Registrate、Ponder 与 Flywheel 仅提取至忽略的构建目录供编译，逐字节与实际运行依赖一致。仍需在完整产线验证红石粉传播、过载解除与邻居通知频率。
