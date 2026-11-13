# 远程运行验证脚本

`remote_runtime_validation.js` 仅用于远程隔离测试服：独立世界、仅绑定本机的端口以及独立配置目录。临时复制到该测试实例的 `kubejs/server_scripts/` 后运行；不得放入正式服，因为脚本会修改 `(8,80,8)` 附近方块并生成或清理测试实体。每次脚本加载只执行一次。

旧存档妖精用例需要先关闭限制脚本，生成 `PersistenceRequired:1b,Tags:["dw_persistence_probe"]` 的妖精，确认其存在并保存世界，再恢复限制与测试脚本并重启。详细验证结果见 `docs/validation/2026-11-01-packager-fairy-kubejs.md`。

`crash_startup_regression.js` 用于启动崩溃回归：临时复制到隔离实例的 `kubejs/server_scripts/` 并启动。它会修改 `(8,120,7)`、`(8,120,8)`、`(10,120,8)`，短暂强制加载对应区块，检查万花筒磨盘 `LivingEntity` 调用及 Mekanism 发电机 tick 后的能量去向。仅用于独立测试世界；不要复制到正式实例或随包分发。日志以 `[DW_CRASH_REGRESSION]` 标记，预期 7 项 PASS、`COMPLETE failures=0`。脚本在检查后清理方块和强制加载；异常时应丢弃测试世界。

`magic_century_egg_regression.js` 用于魔法皮蛋效果回归：临时复制到独立测试实例的 `kubejs/server_scripts/`，与当前 `magic_century_egg.js` 一起加载。使用清空状态的 Forge 假玩家真正调用食用入口，逐个核对当前注册表中的持续增益时长、等级、有害/中性效果及漂浮排除、瞬时治疗，以及重复食用的倒计时截止时间。必须安装至少一个提供持续增益的第三方模组（如农夫乐事），并注册 `kubejs:magic_century_egg`。最小实例仅安装 Forge、KubeJS、Rhino、Architectury 和农夫乐事时，另在 `startup_scripts/` 使用 `StartupEvents.registry('item', event => { event.create('magic_century_egg').food(food => food.hunger(20).saturation(20).alwaysEdible()) })` 作为食物夹具；完整整合包使用正式启动脚本，不添加夹具。预期所有检查 PASS、`COMPLETE failures=0`，`COVERAGE` 行记录实际覆盖数量。脚本最后清理假玩家状态，不自动关闭服务器。该检查必须在实际 Rhino 引擎运行，Node 语法检查无法发现循环内 `const` 重用首次赋值的问题；它不证明完整整合包中所有效果的后续 tick 行为或客户端显示。

`updater_linux_e2e.py <Linux 服务端更新器路径>` 使用真实更新器，在临时实例与本机 HTTP 服务器中测试：无外置引擎、有残留锁、两次确认、拒绝 HEAD、8 MiB 更新、存档保留、签名状态与引擎/锁清理。目标二进制须以 `-tags bundled_aria2` 构建。测试从带空格的目录运行，不接触正式实例。
