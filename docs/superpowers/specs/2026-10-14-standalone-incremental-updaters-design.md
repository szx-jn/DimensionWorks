# DimensionWorks 独立增量更新器设计

## 目标

为已有客户端实例与服务端目录提供可在实例根目录运行的独立增量更新器。更新器使用 Bash（macOS/Linux）与原生 Go EXE（Windows），不依赖 Python、BAT 或 PowerShell，不进入 `.mrpack` 或服务端 ZIP，并能把旧安装中的整合包受管内容同步到目标发行版。

更新成功后，受管文件与目标发行版逐项一致；世界、日志、崩溃报告、账号与开服运行数据保持不变。

## 非目标

- 不替代 Modrinth Launcher、Prism Launcher、MultiMC 或其他启动器。
- 不在游戏或服务端运行时热更新文件；运行前必须退出游戏或停止服务端。
- 不迁移玩家存档、世界、账号、服务器白名单或运维密钥。
- 不修改第三方 Mod 源码。
- 不要求最终用户安装 Python、Git、Gradle 或 Java 工具链之外的依赖。

## 发行资产

每个 GitHub Release 发布以下独立资产：

| 资产 | 用途 |
| --- | --- |
| `DimensionWorks-<version>.mrpack` | 客户端完整 Modrinth 包 |
| `DimensionWorks-Server-<version>.zip` | 服务端完整安装包 |
| `DimensionWorks-<version>-update-manifest.tsv` | 客户端完整受管文件清单 |
| `DimensionWorks-Server-<version>-update-manifest.tsv` | 服务端完整受管文件清单 |
| `DimensionWorks-<version>-delta.zip` | 客户端相对上一发行版的差量文件 |
| `DimensionWorks-Server-<version>-delta.zip` | 服务端相对上一发行版的差量文件 |
| `update-dimensionworks-client-windows-amd64.exe` | Windows amd64 客户端更新器 |
| `update-dimensionworks-server-windows-amd64.exe` | Windows amd64 服务端更新器 |
| `update-dimensionworks-client-linux-amd64` | Linux amd64 客户端更新器 |
| `update-dimensionworks-server-linux-amd64` | Linux amd64 服务端更新器 |
| `update-dimensionworks-client-macos-arm64` | macOS arm64 客户端更新器 |
| `update-dimensionworks-server-macos-arm64` | macOS arm64 服务端更新器 |
| `SHA256SUMS` | 上述资产的 SHA-256 |

所有平台的更新器共用仓库 `update-Windows/` 中的 Go 源码，由 GitHub Actions 交叉编译为 Windows amd64、Linux amd64 和 macOS arm64 的客户端/服务端原生二进制。打包器必须验证 `.mrpack` 与服务端完整 ZIP 中不存在 Shell 更新器、Windows EXE 或旧版 BAT 更新器。更新器只能作为独立 Release 资产发布，首次使用时由用户复制到对应实例或服务端根目录。

## 更新清单格式

使用 UTF-8、LF 换行、无 BOM 的 TSV，避免最终用户依赖 JSON 工具。格式如下：

```text
# DimensionWorks update manifest 1
# kind client
# version 0.1.17
# release v0.1.17
# base v0.1.16
action	path	algorithm	value	source
remote	mods/example.jar	sha512	<hash>	https://...
archive	config/example.cfg	sha256	<hash>	overrides/config/example.cfg
```

字段固定为：

1. `action`：`remote` 或 `archive`。
2. `path`：更新后相对实例根目录的 POSIX 路径。
3. `algorithm`：远程文件使用 `sha512`，归档文件使用 `sha256`。
4. `value`：十六进制摘要。
5. `source`：远程 URL 或完整 ZIP/MRPACK 内的归档成员路径。

路径必须是规范相对路径，禁止绝对路径、`..`、反斜杠、控制字符、符号链接与重复项。服务端清单中的所有文件均为 `archive`；客户端远程 Mod 和光影为 `remote`，`overrides/` 与元数据为 `archive`。

`.mrpack` 与完整服务端 ZIP 内各自携带同一格式的 `dimensionworks-update-manifest.tsv`，便于使用本地包更新。发布的独立清单与包内清单内容一致。清单自身不列为受管文件。

## 客户端受管范围

客户端受管范围由目标清单决定，包括：

- `mods/`
- `shaderpacks/`
- `config/`
- `defaultconfigs/`
- `kubejs/`
- `resourcepacks/`
- `local/`
- `options.txt`、`log4j2.xml`、`rhino.local.properties`
- `CustomSkinLoader/CustomSkinLoader.json`
- `minemenu/menu.json`
- 实例根目录的 `modrinth.index.json`

更新器下载完整 `.mrpack` 到临时目录，但只把其中发生变化的 `overrides/` 文件应用到实例。远程 Mod 与光影按清单逐文件校验；摘要一致时跳过下载，不一致或缺失时只下载对应文件。

客户端始终保留：

- `saves/`
- `logs/`
- `crash-reports/`
- `screenshots/`
- 账号、会话与启动器私有数据
- 未列入目标清单且不属于受管目录的其他运行数据

`local/` 当前已经进入客户端 overrides，因此属于受管范围；若后续要改为私有数据，必须先修改清单生成规则和本设计。

## 服务端受管范围

服务端受管范围是完整服务端 ZIP 中除明确保留项外的所有文件，包括：

- `mods/`
- `config/`
- `defaultconfigs/`
- `kubejs/`
- `libraries/`
- `start.sh`、`start.bat`
- `run.sh`、`run.bat`
- `README.md`
- 根目录其他 Forge 与整合包运行文件

服务端始终保留：

- `eula.txt`
- `server.properties`
- `user_jvm_args.txt`
- `logs/`
- `crash-reports/`
- `debug/`
- `screenshots/`
- `world/`、`world_nether/`、`world_the_end/`
- `server.properties` 中 `level-name` 指定的世界目录
- `usercache.json`、`ops.json`、`whitelist.json`
- `banned-ips.json`、`banned-players.json`
- 更新器自身与旧版更新器文件
- `.dimensionworks-update/`
- 未列入旧状态清单或目标清单的其他运行与运维文件

保留项即使出现在新 ZIP 中，也不得覆盖已有实例。全新服务端安装仍使用 ZIP 中的默认 `server.properties`、`eula.txt` 和 `user_jvm_args.txt`。

## 版本与发布源

更新器默认查询 GitHub `szx-jn/DimensionWorks` 的 Latest Release，也支持：

- `--release v0.1.17`
- `--package <本地完整包路径>`
- `--url <完整包 URL>`
- `--help`

成功后把目标版本写入：

- `.dimensionworks-update/state.tsv`
- 实例根目录 `VERSION`
- 客户端额外更新根目录 `modrinth.index.json`

更新器采用发行包携带的版本，不自行递增。版本必须满足 `x.x.x`，Release 标签必须为对应的 `v<version>`。

发布工作流以本地 `VERSION` 与 GitHub 最新 Release 标签中的较大者为基线，推进一个补丁版本。若目标标签已经存在，工作流必须失败，不得覆盖发行资产。发布成功后把新的 `VERSION` 提交到触发工作流的分支。

## 客户端更新流程

1. 根据脚本所在目录确定实例根目录，在 `.dimensionworks-update/` 创建原子锁。
2. 解析参数，获取目标清单；兼容旧状态时优先获取客户端差量 ZIP。
3. 校验清单 schema、类型、版本、路径和摘要格式。
4. 无旧状态时，在受管目录内建立初始状态；受管目录外的文件不纳入删除范围。
5. 差量不可用、状态不兼容或本地文件漂移时才下载并解压 `.mrpack`，执行 ZIP 完整性与路径穿越检查。
6. 对每个远程项校验现有文件；只有缺失或摘要不符时下载，并在临时文件上校验成功后再改名。
7. 对归档项计算当前摘要，与目标清单比较，确定需要复制的最小文件集合。
8. 把所有旧状态中存在、目标状态中消失的受管文件移入事务备份并删除。
9. 复制变化文件，校验所有目标摘要。
10. 原子写入 `VERSION` 与新状态；失败时恢复备份并删除本次新增文件。各平台 Go 更新器在下载、检查、暂存、应用和删除阶段实时输出进度。

## 服务端更新流程

服务端优先使用差量：

1. 读取旧状态版本与目标清单的 `base`。
2. 若旧状态版本等于 `base`，下载并校验差量 ZIP。
3. 对目标清单逐项校验当前文件；所有需要变化且无法从当前文件验证通过的文件必须能从差量中取得，否则退回完整 ZIP。
4. 完整 ZIP 流程与客户端相同，但清单内归档成员直接位于 ZIP 根目录。
5. 应用、验证、提交状态与回滚规则一致。

差量 ZIP 由工作流比较上一版完整包与新完整包生成，只包含新增或内容变化的文件，不包含脚本。客户端与服务端都优先使用差量；首次安装、跨版本更新、没有可靠旧状态、差量缺失或本地受管文件漂移时才使用完整包。

## 事务、锁与回滚

- 锁使用 `.dimensionworks-update/lock/` 的原子目录创建；已有活动锁时拒绝更新。
- 所有下载、解压、哈希与暂存操作位于 `.dimensionworks-update/work/`，与目标实例处于同一卷。
- 应用前备份每个将被覆盖或删除的文件。
- 任一复制、删除或摘要验证失败，立即恢复全部备份并清理本次新增路径。
- `state.tsv` 最后通过同目录临时文件与原子改名提交。
- 更新器必须清理临时目录，但保留可诊断的失败日志与回滚结果。
- 符号链接、目录穿越、大小写冲突和保留路径冲突均作为错误处理，不跟随链接写入实例外。

## 用户可见行为

脚本启动后显示当前状态、目标版本、更新模式和主要计数。成功时输出：

```text
DimensionWorks 更新完成：<old> -> <new>
```

已是最新版本时不做文件变更并以零状态退出。需要回滚时，用户显式传入旧 Release 标签；更新器不会自动降级。

客户端运行前必须退出 Minecraft 与启动器对该实例的占用。服务端运行前必须停止服务端进程。

## 仓库改动

计划涉及：

- `update-Windows/` 保存跨平台原生 Go 更新器源码；发布时交叉编译 Windows amd64、Linux amd64、macOS arm64 六个二进制。
- 新建 `scripts/pack_manifest.py`，集中生成、解析、验证 TSV 清单。
- 修改 `scripts/package_mrpack.py`，生成客户端清单并写入 `.mrpack`。
- 修改 `scripts/package_server.py`，生成服务端清单并写入 ZIP。
- 新建 `scripts/build_server_delta.py`，比较两个服务端 ZIP 并生成差量。
- 修改 `scripts/pack_version.py`，加入本地版本与 GitHub 标签取较大值再递增补丁的纯函数。
- 修改 `scripts/pack_hook.py`，检查脚本、清单格式、资产命名和禁止内嵌脚本。
- 修改 `.github/workflows/release-pack.yml`，修正版本冲突、生成并发布清单/差量/Shell 更新器与 Windows EXE；新增 Windows 更新器测试、交叉编译工作流。
- 修改 `scripts/README.md` 与新增用户文档，说明部署、更新、回滚和保留范围。
- 新增清单、版本、差量、打包输出和工作流静态测试。

## 验证标准

必须通过以下自动化验证：

- TSV 正常解析，拒绝重复路径、绝对路径、`..`、未知 action、非法摘要和错误 kind。
- 版本函数正确处理本地落后、相等、GitHub 标签落后与非法标签。
- `.mrpack` 与服务端 ZIP 中不存在当前 Shell 更新器、Windows EXE 或旧版 BAT 更新器。
- 客户端与服务端完整包都包含有效包内清单，独立清单与包内清单一致。
- 客户端 fixture 更新后，受管目录内容与目标清单一致，仅变化文件被复制。
- 服务端 fixture 使用兼容差量成功增量更新。
- 跨版本、无状态、差量缺文件和本地漂移时可靠回退完整 ZIP。
- 保留的世界、日志、EULA、服务端设置与 JVM 参数在成功和失败回滚后均不改变。
- Go 更新器单元测试、Windows amd64 EXE 交叉编译、Go 源码测试与三平台交叉编译均通过；真实各平台运行验证如未执行必须如实报告。
- 工作流结构、资产名称、校验和与 Release 发布规则通过静态测试。
- 打包预检继续通过，并实际构建客户端与服务端包验证内容。

## 性能与长期运行成本

更新器只在用户手动执行时运行，不常驻游戏或服务端，不影响 tick、区块加载或机器阵列。客户端远程文件按摘要跳过；服务端兼容时只下载差量。临时空间峰值约为完整包大小加变化文件备份大小，服务端首次或回退更新需要约两个完整 ZIP 的磁盘空间。
