# DimensionWorks Stress Gauge Alarm

Create 1.20.1 / Forge 47 / Create 6.0.8 扩展。Create 应力表所在动力网络过载时，应力表向所有方向提供 15 级弱红石信号；过载解除或动力停止后信号归零。信号通过原版方块红石查询接口提供，红石粉的传播与衰减由 Minecraft 原版处理。

仅当应力表映射出的模拟红石强度发生变化时，才通知邻居和比较器；网络容量或应力数值改变但输出档位不变时跳过邻居传播。此项目尚未加入 `manifest/mods.json`，因此暂不参与整合包打包。

Java 单元测试覆盖 Create 相同的 0–15 输出取整与夹取规则；仍需在游戏中验证红石粉逐格衰减、服务端加载以及客户端同步。Create 的 `updateFromNetwork` 注入使用 `remap=false`。
