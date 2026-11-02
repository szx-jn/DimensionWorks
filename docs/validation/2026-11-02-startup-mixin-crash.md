# 0.2.72 启动崩溃回归

## 复现与根因

使用 GitHub Release 的 `DimensionWorks-Server-0.2.72.zip`，在独立目录、Java 17.0.21、Forge 47.4.23 下启动；测试服仅绑定 `127.0.0.1:25577`，使用独立世界。

实际捕获到两处致命注入错误：

- `dimensionworks_mek_stress.mixins.json:mekgenerators.GeneratorNoFeExportMixin` 的 Redirect 期望 `CableUtils.emit(EnumSet, …)`，但 Mekanism 10.4.16.80 的真实字节码调用 `emit(Set, …)`，严格注入检查报 `(0/1) succeeded`。
- `kaleidoscope_compat.mixins.json:kaleidoscope_cookery.MillstoneBlockEntityMixin` 的处理器参数仍为 `Mob`，而烹饪 1.6.0 的 `canBindEntity` 参数已改为 `LivingEntity`，报 `Invalid descriptor`。

修复为自研模组 0.3.19 的 `Set<Direction>` 签名，并固定 Kaleidoscope Compat 为 Modrinth `HvOVFiEQ`（2.6.5-forge+mc1.20.1-Patch.2）。使用 `javap` 核对了目标 `emit` 调用和上游 Patch.2 的 `LivingEntity` 处理器，保留严格注入要求。

## 本地验证

本次用户明确要求本地验证，覆盖仓库旧的禁止本地构建约定。安装 Java 17 和 Gradle 8.8；导入执行环境 CA，Java 网络请求使用环境提供的 HTTP/HTTPS 代理。首次未配置代理时 LuckPerms 下载依赖失败属于测试环境问题，补齐代理后正常启用。

先用正式产物依赖编译修复类并替换测试副本中的对应类，同时替换兼容模组；随后完整启动服务端，出现 `Done (36.353s)!`，原有两处崩溃不再出现。

在同一隔离实例重启后执行 `scripts/integration/crash_startup_regression.js`，得到 7 项 PASS、`COMPLETE failures=0`：放置发电机、相邻储能方块与磨盘；调用磨盘 `LivingEntity` 绑定判断；向发电机注入测试能量；等待实际 tick 后，发电机内部能量耗尽且相邻储能方块仍无能量。脚本只用于测试实例，不随整合包分发。

静态检查：`git diff --check`、清单 JSON 解析与 `pack_hook.check_manifest()`、运行探针 `node --check` 均通过。仓库 Python 测试 73 项通过。

完整 Gradle 8.8 `build test` 在本地通过，81 项 Java 单元测试零失败。首次测试发现 `mods.toml` 版本仍为 0.3.18，已同步到 0.3.19 后重跑通过；构建文件、模组元数据及清单三处版本一致。

随后用 Gradle 生成的完整重混淆 `dimensionworks_mek_stress-0.3.19.jar` 替换测试实例中的旧包，再次正常启动至 `Done (4.081s)!`；同一运行回归再次得到 7 项 PASS、`COMPLETE failures=0`。最后通过 `stop` 保存并关闭测试世界。

## 验证边界

以上是实际 Forge 服务端和世界 tick 验证，未执行图形客户端登录或玩家手工交互。第三方模组仍有非致命的可选整合资源告警；不将启动通过描述为整份日志无 ERROR。
