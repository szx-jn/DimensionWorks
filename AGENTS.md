# AGENTS.md

本文件是 DimensionWorks 仓库的项目级提示词，供在此仓库工作的 AI 助手使用。

## 项目定位

**维度工序（DimensionWorks）** 是一个 Minecraft 1.20.1 Forge 多维度工业自动化整合包，核心是多维度探索、工业化生产、自动化资源获取与跨维度物流。

项目处于早期设计与开发阶段。Mod 列表、配方、科技树和维度职责都在持续调整，因此本仓库的多数目录目前只有占位 README。默认先补设计与规格，再写实现，不要跳过设计直接堆代码。

## 技术基线

- Minecraft 1.20.1，Forge，Java 17
- ForgeGradle `[6.0,6.2)`，官方映射（`official` / 1.20.1）
- Gradle 8.8，仓库内不包含 Gradle Wrapper
- 自研 Mod 的 group 为 `dev.szx.dimensionworks`，归档名与 modId 使用下划线，例如 `dimensionworks_rpm_limit`

以下改动属于基线变更，必须先向人确认：升级 Minecraft 大版本、更换 Mod Loader、大幅调整 Forge 或 Gradle 版本。

## 真相源

不要凭记忆写版本号或 Mod 列表，一律读取文件：

- `manifest/mods.json`：整合包 Mod 清单的唯一来源，包含 id、来源、版本、必需性和环境。
- `mods/<name>/`：自研 Mod，每个是独立的 Gradle 工程。
- `kubejs/`：KubeJS 的启动、服务端、客户端脚本与数据。
- `config/`、`defaultconfigs/`：整合包配置与随包分发的默认值。
- `docs/`：设计文档，命名见 `docs/README.md`。
- `server/`、`resourcepacks/`、`scripts/`：服务端模板、资源包与独立辅助脚本。

## 内容放置规则

优先选择影响面最小、最容易被整合包使用者编辑的方式：

- 改配方、标签、掉落、进度与数据 → 放 `kubejs/` 或数据包，不要为此写 Java。
- 改游戏运行行为、且 KubeJS 无法覆盖 → 才在对应 `mods/<name>/` 下写 Java 与 mixin。
- 改需要随整合包分发、但玩家可自行覆盖的默认值 → 放 `defaultconfigs/`。
- 改设计思路、科技树、维度职责、资源生态、物流与性能策略 → 写进 `docs/`。
- 新增 Mod 前的判断顺序：先确认现有 Mod 与 KubeJS 能否实现；确实需要才动 `manifest/mods.json`。

## 设计原则

README 中的原则需要转化成可执行的约束：

1. **资源不等于矿石**：新增资源获取方式时，说明它属于采矿、农业、生物、流体、化学、环境机制还是工业副产物回收。
2. **维度具有长期工业价值**：不接受“一次性探索搜刮后废弃”的维度设计，每个维度都要在长期生产体系中有职责。
3. **自动化服务于生产体系**：重点是完整生产链，不是单纯堆叠机器或追求单机强度。
4. **跨维度物流是核心系统**：涉及维度、资源或自动化的设计，要考虑它如何接入统一的跨维度工业网络。
5. **服务器性能优先**：涉及自动化、机器阵列或维度机制的改动，必须说明区块加载、实体与机器数量、长期运行成本，并给出降低开销的做法。

## 构建与验证

- 每个自研 Mod 独立构建，在对应目录执行：`gradle build --no-daemon`。
- 修改自研 Mod 的 Java、mixins、资源或构建脚本后，先构建通过再汇报结果。
- 构建失败时给出真实错误输出，不要隐藏或改写为“应该没问题”。
- 仅改文档、KubeJS 或 JSON 数据时，说明进行了哪些检查，例如 JSON 语法、命名空间、引用路径。
- 未经实际运行验证的内容不要描述为已验证。

## 产物纪律

以下内容不提交到版本库：

- Gradle 与 IDE 产物：`build/`、`bin/`、`run/`、`.gradle/`、`out/`
- Minecraft 运行时数据：`world/`、`logs/`、`crash-reports/`、`debug/`、`screenshots/`、`server.properties`
- 本地私有配置与密钥：`.env`、`*.local`

`.gitignore` 目前未覆盖 `build/`、`bin/`、`run/`、`.gradle/`，在 `mods/` 下新增 Gradle 工程时注意不要让这些目录进入提交。

## 提交与文档

- 提交信息使用带 scope 的 Conventional Commits，例如 `feat(rpm): ...`、`fix(transfer): ...`、`docs(rpm): ...`、`ci: ...`。
- 文档使用中文，与现有 README 一致。
- 改动行为时同步更新对应 `mods/<name>/README.md`，尤其是暴露给 KubeJS 或命令行的接口。
- 每次构建完成后，在回复里给出产物链接：本地产物（自研 Mod 的 `gradle build` jar、`dist/*.mrpack`）用绝对路径的 Markdown 文件链接，远程产物用可点击 URL。

## 边界

- 不擅自往 `manifest/mods.json` 添加 Mod；新增依赖前先说明理由并确认。
- 不修改第三方 Mod 的源码。需要改动时走 fork，并在整合包中引用固定的 GitHub Release 资产，不引用移动分支或 Actions 产物。`Create-Mobile-Packages` 即为此模式，已建立固定 Release 流程。
- `manifest/mods.json` 中标记为 `pending-release` 或尚未固定版本的条目，在最终整合包交付前必须替换为固定版本。
- 本仓库原创的代码、配置、脚本和文档默认 MIT License；第三方 Mod、资源与数据包遵循各自许可，不要将第三方代码复制进本仓库。
