# 独立增量更新器实现计划

> 历史实施计划：本文记录最初的 Bash + Batch 方案。Windows Batch 更新器现已被 `update-Windows/` 中的原生 Go EXE 取代；当前设计以 `docs/superpowers/specs/2026-10-14-standalone-incremental-updaters-design.md` 为准。


> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 DimensionWorks 客户端与服务端提供独立发布、可回滚的 Bash/Batch 增量更新器，并让 GitHub Release 不再发生版本覆盖。

**Architecture:** 打包器生成严格校验的 TSV 受管文件清单并嵌入完整包；独立脚本从 Release 获取清单和必要文件，使用状态清单、事务备份、摘要验证和完整包回退同步受管内容。服务端 Release 额外生成相对上一版的差量 ZIP，客户端远程文件按 URL 增量下载。

**Tech Stack:** Python 3.17 标准库、Bash 3+、PowerShell 5.1/.NET、GitHub Actions、Modrinth `.mrpack`、Forge 1.20.1。

**Spec:** `docs/superpowers/specs/2026-10-14-standalone-incremental-updaters-design.md`

## Global Constraints

- 更新脚本绝不能出现在 `.mrpack` 或完整服务端 ZIP 内；只能作为独立 Release 资产发布。
- 清单是 UTF-8、LF、无 BOM 的 TSV；路径必须是规范相对 POSIX 路径，禁止绝对路径、`..`、反斜杠、控制字符、符号链接和重复项。
- 清单头部固定包含 `# DimensionWorks update manifest 1`、`kind`、`version`、`release`、`base` 五类信息；数据行字段为 `action path algorithm value source`。
- 客户端远程文件使用 `sha512`，归档文件使用 `sha256`；服务端所有文件均为 `archive`。
- 更新器只删除旧状态清单中存在、目标清单中不存在的受管文件；保留世界、日志、账号、EULA、`server.properties`、`user_jvm_args.txt` 和未知运行数据。
- 脚本不得依赖 Python；Shell 依赖系统 `curl/wget`、`unzip`、SHA-256 工具；Batch 依赖 PowerShell/.NET。
- 所有目标文件先暂存并校验；应用失败必须回滚；状态文件最后原子写入。
- 版本格式为 `x.x.x`；Release 标签为 `v<version>`；工作流目标版本为本地 `VERSION` 与 GitHub 最新标签中的较大者再加补丁，目标标签存在时失败而不是覆盖。
- 仅修改本工作树；不修改第三方 Mod 源码；构建与测试失败必须报告真实输出。

---

### Task 1: 清单库与打包器接入

**Files:**
- Create: `scripts/pack_manifest.py`
- Create: `scripts/tests/test_pack_manifest.py`
- Modify: `scripts/package_mrpack.py`
- Modify: `scripts/package_server.py`
- Modify: `scripts/pack_hook.py`
- Modify: `scripts/README.md`

**Interfaces:**
- `ManifestError(ValueError)`
- `ManifestEntry(action: str, path: str, algorithm: str, value: str, source: str)`
- `UpdateManifest(kind: str, version: str, release: str, base: str, entries: tuple[ManifestEntry, ...])`
- `parse_manifest(data: bytes, expected_kind: str | None = None) -> UpdateManifest`
- `render_manifest(manifest: UpdateManifest) -> bytes`
- `make_client_manifest(index_entries: list[dict], staging_root: Path, version: str, release: str, base: str) -> UpdateManifest`
- `make_server_manifest(staging_root: Path, version: str, release: str, base: str) -> UpdateManifest`
- `validate_no_updater_scripts(archive_paths: Iterable[str]) -> None`

**Requirements:**
1. 清单库校验 TSV 头部、字段数、action、算法、摘要长度、路径安全、kind、版本和重复路径。
2. `make_client_manifest` 把远程 Mod/光影转为 `remote`，把 `overrides/` 内容和 `modrinth.index.json` 转为 `archive`；远程条目使用现有 SHA-512，归档条目计算 SHA-256。
3. `make_server_manifest` 扫描 staging 中所有文件，排除 `dimensionworks-update-manifest.tsv`，全部生成 `archive`。
4. `package_mrpack.py --manifest-output` 写独立客户端清单，且 `.mrpack` 根目录写 `dimensionworks-update-manifest.tsv`；`package_server.py` 同样提供 `--manifest-output`。
5. 打包输出前验证归档中不存在四个更新脚本名；`pack_hook.py` 增加清单格式、脚本源文件存在、禁止内嵌脚本和资产命名检查。
6. 测试覆盖：正常 round-trip、非法路径、重复路径、错误 kind、错误摘要、客户端远程/归档映射、服务端排除清单文件、脚本禁止内嵌。

- [ ] **Step 1:** 写失败的 `test_pack_manifest.py`，运行 `python3 -m unittest scripts.tests.test_pack_manifest -v`，确认失败。
- [ ] **Step 2:** 实现 `pack_manifest.py`，运行同一测试并确认通过。
- [ ] **Step 3:** 接入两个打包器与预检，运行 `python3 -m unittest discover -s scripts/tests -v`。
- [ ] **Step 4:** 用 `--skip-hook` 的小型 fixture 验证 `.mrpack` 和服务端 ZIP 的包内清单与独立清单一致，并确认脚本名不存在。
- [ ] **Step 5:** 更新中文脚本文档，提交 `feat(pack): emit standalone update manifests`。

### Task 2: 四个独立更新脚本

**Files:**
- Create: `release-updaters/update-dimensionworks-client.sh`
- Create: `release-updaters/update-dimensionworks-client.bat`
- Create: `release-updaters/update-dimensionworks-server.sh`
- Create: `release-updaters/update-dimensionworks-server.bat`
- Create: `release-updaters/tests/test_updaters.py`

**Interfaces:**
- 四个脚本都支持 `--release <tag>`、`--package <path>`、`--url <url>`、`--help`；无参数默认 GitHub Latest Release。
- 客户端脚本根目录为脚本所在目录；服务端脚本根目录同样为脚本所在目录。
- 状态文件为 `.dimensionworks-update/state.tsv`，版本标记为根目录 `VERSION`。
- Shell 与 Batch 使用相同的清单字段、保留路径、锁名、备份目录和错误语义。

**Requirements:**
1. Shell 使用 `curl` 或 `wget`，`unzip`，`shasum`/`sha256sum`；Batch 使用 PowerShell `Invoke-WebRequest`、`System.IO.Compression`、`Get-FileHash`。
2. 脚本下载独立清单，按旧状态和当前摘要计算 delta；客户端远程文件只下载缺失或摘要不符的 URL，`.mrpack` 只应用变化的 `overrides/`。
3. 服务端先检查旧状态版本与清单 `base`，兼容时下载差量 ZIP；缺文件、状态缺失、跨版本或摘要漂移时下载完整 ZIP。
4. 旧状态缺失时只在受管目录内建立初始状态；不得遍历或删除 `saves/`、`world/`、日志等保留目录。
5. 实现原子锁、临时暂存、备份、应用、全量摘要验证、状态最后提交和失败回滚；拒绝符号链接和路径穿越。
6. 成功输出 `<old> -> <new>`；已是最新版本零退出；`--help` 不做网络请求。
7. Batch 脚本先复制自身到临时目录再执行更新流程，避免覆盖正在运行的脚本。
8. `test_updaters.py` 做静态检查：四个脚本存在、包含所有 CLI 参数、没有内嵌脚本禁用、LF/CRLF 约束、状态路径和保留目录规则。

- [ ] **Step 1:** 先写静态测试并运行，确认四个脚本缺失时失败。
- [ ] **Step 2:** 实现 Bash 客户端/服务端脚本，运行静态测试和 `bash -n`。
- [ ] **Step 3:** 实现 Batch 客户端/服务端脚本，运行静态测试并检查批处理标签、转义和 PowerShell 调用。
- [ ] **Step 4:** 用本地 fixture 的 `--package` 路径做客户端和服务端应用测试，覆盖新增、修改、删除、保留目录和回滚。
- [ ] **Step 5:** 更新脚本目录 README，提交 `feat(updaters): add standalone client and server scripts`。

### Task 3: 服务端差量、版本函数与 Release 工作流

**Files:**
- Create: `scripts/build_server_delta.py`
- Create: `scripts/tests/test_build_server_delta.py`
- Modify: `scripts/pack_version.py`
- Modify: `scripts/tests/test_pack_version.py`
- Modify: `.github/workflows/release-pack.yml`
- Modify: `scripts/README.md`

**Interfaces:**
- `pack_version.resolve_release_version(local: PackVersion, latest_tag: str | None) -> PackVersion`
- `build_server_delta(base_zip: Path, new_zip: Path, manifest: UpdateManifest, output: Path) -> Path`
- 差量 ZIP 只包含新增或 SHA-256 变化的文件，不包含 `dimensionworks-update-manifest.tsv`。

**Requirements:**
1. `resolve_release_version` 拒绝非法标签，取本地版本与 GitHub 标签较大者再 `next_patch()`。
2. 差量生成器安全解压并比较 ZIP 条目，拒绝路径穿越和符号链接；输出保留可执行位。
3. 工作流先查询 Latest Release，计算并写入 `VERSION`，再构建完整客户端、服务端、两个独立清单和服务端差量；目标标签已存在时失败。
4. 工作流 `SHA256SUMS` 覆盖完整包、独立清单、差量和四个独立脚本；Release 同时上传这些资产，绝不把脚本放入包。
5. 发布成功后提交并推送 `VERSION` 到触发分支；推送失败不得覆盖已有 Release，下一次运行以 GitHub 标签为准。
6. 测试覆盖版本取大、非法标签、差量新增/修改/未变、路径穿越和工作流 YAML 关键步骤。

- [ ] **Step 1:** 写失败的版本和差量测试并运行。
- [ ] **Step 2:** 实现版本函数与差量生成器，运行 `python3 -m unittest discover -s scripts/tests -v`。
- [ ] **Step 3:** 修改工作流并做 YAML/静态断言，确认 Release 资产包含独立脚本且不嵌入完整包。
- [ ] **Step 4:** 更新中文文档，提交 `ci(release): prevent tag overwrite and publish update assets`。

### Task 4: 集成验证与文档收口

**Files:**
- Modify: `scripts/pack_hook.py`
- Modify: `scripts/README.md`
- Modify: `docs/superpowers/specs/2026-10-14-standalone-incremental-updaters-design.md`（仅在实现发现必要偏差时）
- Create: `scripts/tests/test_release_assets.py`

**Requirements:**
1. 增加真实打包器的最小 fixture 测试，检查 `.mrpack`、服务端 ZIP、包内清单和独立清单的格式、哈希和脚本排除。
2. 检查四个脚本的 Release 资产命名和 `SHA256SUMS` 生成规则。
3. 运行完整测试、`pack_hook.py --skip-build`、`bash -n` 和可用的 Windows 语法检查。
4. 中文 README 说明首次把四个脚本复制到实例根目录、更新、回滚、保留数据和网络回退。
5. 提交 `test(updaters): verify release update assets`。

- [ ] **Step 1:** 添加集成测试并运行，确认能发现脚本误打包、清单不一致和差量缺文件。
- [ ] **Step 2:** 执行 `python3 -m unittest discover -s scripts/tests -v`、`python3 scripts/pack_hook.py --skip-build`。
- [ ] **Step 3:** 更新文档并提交。
