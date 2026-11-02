# 远程运行验证脚本

`remote_runtime_validation.js` 仅用于远程隔离测试服：独立世界、仅绑定本机的端口以及独立配置目录。临时复制到该测试实例的 `kubejs/server_scripts/` 后运行；不得放入正式服，因为脚本会修改 `(8,80,8)` 附近方块并生成或清理测试实体。每次脚本加载只执行一次。

旧存档妖精用例需要先关闭限制脚本，生成 `PersistenceRequired:1b,Tags:["dw_persistence_probe"]` 的妖精，确认其存在并保存世界，再恢复限制与测试脚本并重启。详细验证结果见 `docs/validation/2026-11-01-packager-fairy-kubejs.md`。

`crash_startup_regression.js` 用于启动崩溃回归：临时复制到隔离实例的 `kubejs/server_scripts/` 并启动。它会修改 `(8,120,7)`、`(8,120,8)`、`(10,120,8)`，短暂强制加载对应区块，检查万花筒磨盘 `LivingEntity` 调用及 Mekanism 发电机 tick 后的能量去向。仅用于独立测试世界；不要复制到正式实例或随包分发。日志以 `[DW_CRASH_REGRESSION]` 标记，预期 7 项 PASS、`COMPLETE failures=0`。脚本在检查后清理方块和强制加载；异常时应丢弃测试世界。
