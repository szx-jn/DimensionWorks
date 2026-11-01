# DimensionWorks Stress Gauge Alarm

Create 1.20.1 / Forge 47 / Create 6.0.8 扩展。Create 应力表所在动力网络过载时，应力表向所有方向提供 15 级弱红石信号；过载解除或动力停止后信号归零。信号通过原版方块红石查询接口提供，红石粉的传播与衰减由 Minecraft 原版处理。

仅在 Create 网络状态切换时通知邻居，不进行每 tick 扫描。此项目尚未加入 `manifest/mods.json`，因此暂不参与整合包打包。

游戏内验收尚未执行：需验证过载/解除时状态更新、红石粉逐格衰减、服务端加载以及客户端同步。
