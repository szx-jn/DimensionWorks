# 远程运行验证脚本

`remote_runtime_validation.js` 仅用于远程隔离测试服：独立世界、仅绑定本机的端口以及独立配置目录。临时复制到该测试实例的 `kubejs/server_scripts/` 后运行；不得放入正式服，因为脚本会修改 `(8,80,8)` 附近方块并生成或清理测试实体。每次脚本加载只执行一次。

旧存档妖精用例需要先关闭限制脚本，生成 `PersistenceRequired:1b,Tags:["dw_persistence_probe"]` 的妖精，确认其存在并保存世界，再恢复限制与测试脚本并重启。详细验证结果见 `docs/validation/2026-11-01-packager-fairy-kubejs.md`。

`crash_startup_regression.js` 用于启动崩溃回归：临时复制到隔离实例的 `kubejs/server_scripts/` 并启动。它会修改 `(8,120,7)`、`(8,120,8)`、`(10,120,8)`，短暂强制加载对应区块，检查万花筒磨盘 `LivingEntity` 调用及 Mekanism 发电机 tick 后的能量去向。仅用于独立测试世界；不要复制到正式实例或随包分发。日志以 `[DW_CRASH_REGRESSION]` 标记，预期 7 项 PASS、`COMPLETE failures=0`。脚本在检查后清理方块和强制加载；异常时应丢弃测试世界。

`magic_century_egg_regression.js` 临时放入独立 Forge/KubeJS/Rhino 实例，与当前脚本及物品注册一起加载。假玩家真正食用后通过菜单回调跨页选择速度、毒和漂浮，检查确认前无效果/倒计时、确认后仅所选效果为 V 级 / 3600 tick，以及再次食用立即死亡并清理效果和倒计时。预期 `COMPLETE failures=0`。Forge 假玩家固定免伤且覆盖死亡回调，因此死亡断言验证生命值归零、效果清除及倒计时清理，不验证真实玩家死亡界面、掉落和其他模组死亡回调。不证明客户端显示和完整整合包全部免死机制。

`updater_linux_e2e.py <Linux 服务端更新器路径>` 使用真实更新器，在临时实例与本机 HTTP 服务器中测试：无外置引擎、有残留锁、两次确认、拒绝 HEAD、8 MiB 更新、存档保留、签名状态与引擎/锁清理。目标二进制须以 `-tags bundled_aria2` 构建。测试从带空格的目录运行，不接触正式实例。
