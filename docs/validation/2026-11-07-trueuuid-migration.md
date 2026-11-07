# TrueUUID 离线数据迁移故障与升级验证

**最终状态：1.3.0 升级已撤回，仓库与现网使用 1.0.9。** 本页保留升级前的文件事务测试记录；后续真实登录失败、完整冷备份和两端回退验收见 [TrueUUID 登录恢复](2026-11-07-trueuuid-login-recovery.md)。

## 现网故障

一名玩家从离线启动切换为同名 MSL 统一认证账号后，服务端已识别其皮肤站身份，却因“检测到重复 UUID 玩家数据”拒绝登录。管理员执行 `trueuuid migrateuuid <玩家名>` 后得到成功提示，仍不能进入。

实际部署的 TrueUUID 1.0.9 JAR 中，`migrateOfflineToVerified` 备份并复制离线文件，但没有移除原目录的旧文件。`needsOfflineUpgrade` 只要仍能发现该名字的离线数据，就继续触发迁移拦截，并不把管理员命令成功执行视为迁移已经完成。

本次逐项核对了 `playerdata`、`playerdata_old`、进度、统计、FTB Chunks、FTB Essentials、FTB Teams、FTB Quests，共 8 个文件。新 UUID 下的文件与旧文件逐字节一致，或只包含预期的 UUID 文本替换。随后将这 8 个旧文件移入独立备份目录，并校验归档文件和新身份文件的 SHA-256，8/8 均通过。服务端日志在 2026-11-07 10:26:57（北京时间）记录该玩家成功进入，玩家也确认登录恢复。

此次现网恢复发生在 **1.0.9 加归档清理** 的环境，不能当作 1.3.0 已部署或真实账号验证通过的证据。

## 已撤回的升级目标

- 上游正式版：[TrueUUID v1.3.0](https://github.com/YuWan-030/TrueUUID/releases/tag/v1.3.0)。
- 对应源码提交：`fa91c727c049df67efba1a0b0c0bc0d79c00dd5b`，从 `v1.3.0` 标签检出，仅用于读取和验证。
- 当时的 Modrinth 固定版本 ID：`DBcYjyan`；当时的清单版本号：`1.3.0+forge-1.20.1`。
- 正式文件：[trueuuid-1.3.0-forge-1.20.1.jar](https://cdn.modrinth.com/data/fMgRLYNA/versions/DBcYjyan/trueuuid-1.3.0-forge-1.20.1.jar)。
- 发布 JAR 的 SHA-256：`4ad014cf05a9a14620579b54b01f5ee14a306c58665c7b632d84ba1f116eee88`，与 GitHub Release 资产摘要一致；SHA-1、SHA-512 和大小也已对照 Modrinth 校验。
- JAR 的全部类文件版本为 61，符合 Java 17；元数据要求 Forge 47+ 和 Minecraft `[1.20.1,1.20.2)`。

1.3.0 的 Forge 1.20.1 `PlayerDataMigration` 调用发布 JAR 内的 `MigrationExecutor`：先备份离线文件和已有目标文件，再原子写入目标，最后删除离线源文件并写入 `COMMITTED`。失败时恢复已写入目标和已删除源文件。因此它直接修复本次“迁移成功后残留旧文件，再次登录被拦截”的原因。

进度与统计都使用 `<UUID>.json`，新版把进度备份单独放入 `vanilla/advancements/`，避免二者相互覆盖。该修改同样已从对应源码及发布 JAR 字节码核对。

## 1.3.0 配置兼容记录

当时升级 1.3.0 时，将 1.0.9 的 `[auth]` 字面键 `"yggdrasil.apiRootWhitelist"` 改为 `[auth.yggdrasil]` 下的 `apiRootWhitelist`，固定精确主机名 `skin.mslmc.net`，继续关闭离线登录，保留已验证名称保护；旧 `auth.nomojang` 项移除，同 IP 重连键只保留关闭的兼容值。该嵌套配置不能用于现已回退的 1.0.9，当前配置以仓库 TOML 和服务端说明为准。

Forge 1.20.1 的 `NameRegistry` 仍读取 `config/trueuuid-registry.json` 的 `premiumUuid`、验证时间、`authSource` 和 `authDisplayName` 等旧字段，字段结构已静态核对。升级应保留此注册表及现有存档。更新器保留既有配置，因此旧服务器必须同步迁移白名单键，并让客户端与服务端一起替换 TrueUUID JAR，再重启加载。

## 实际验证

| 检查 | 结果与边界 |
| --- | --- |
| 发布文件完整性与兼容声明 | Modrinth SHA-1/SHA-512/大小、GitHub SHA-256、类文件版本和 Forge/Minecraft 范围均通过 |
| JSON/TOML 与配置约束 | 清单固定版本、嵌套 MSL 白名单、关闭离线登录、翻译键均通过 |
| 整合包 Modrinth 解析器 | 实际解析出固定的 Forge 1.20.1 JAR 路径及预期 SHA-1，通过 |
| 上游迁移单元测试 | 用 Java 17 编译 `v1.3.0` 的 `MigrationExecutorTest`、`MigrationPlannerTest`，直接链接正式 JAR 执行；11 项成功，0 项失败或跳过 |
| 整合包迁移复测 | 用 Java 17 编译并执行仓库探针，直接调用正式 JAR 的迁移引擎；8 个文件、旧/新数据备份、进度/统计备份分离、旧文件清除、共享 UUID 引用和重复迁移均通过 |
| 现网登录恢复 | 1.0.9 下归档重复文件后，服务端日志和玩家反馈均确认成功 |

上游单元测试还覆盖目标已有数据的备份、迁移中途失败回滚、缺失文件、重复目标、超大文本及符号链接拒绝。它们验证文件事务，不启动 Minecraft，不代表新版已完成 MSL/Mojang 真实账号、客户端确认窗口或 FTB 运行缓存的验收。

整合包探针只操作自动创建的临时目录，不读取实际玩家存档。使用 Java 17 和下载的正式 JAR 可复测：

```sh
java -cp /path/to/trueuuid-1.3.0-forge-1.20.1.jar scripts/integration/TrueUuidMigrationProbe.java
```

成功输出：`PASS: 8 UUID files, separate backups, source cleanup, global references, repeated migration`。

## 静态包证据

REA 包检查只提供静态 ZIP 清单，不执行目标。迁移结论结合了 `javap`、对应源码和上述正式 JAR 文件事务测试。

- 1.0.9 包证据：`ev_d25b5a9f0037b44ae7a481417e944a02bcc1b5e0420e530a84b5c1ac33cfe860`。
- 1.3.0 包证据：`ev_ce5bd991582f080b7f7f76597482f4a9c57af9e60fe2b22a6874936df05e96e5`。

上述静态检查和文件事务测试在正式发布前执行。后续 v0.3.13 正式发布及现网部署暴露了真实登录兼容问题，最终回退到 1.0.9；这些迁移测试通过不能视为 1.3.0 的完整登录验收通过。
