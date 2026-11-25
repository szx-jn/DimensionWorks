# 维度工序 · DimensionWorks

**一个以多维度工业生产与跨维度物流为核心的 Minecraft 整合包。**

[![最新正式版](https://img.shields.io/github/v/release/szx-jn/DimensionWorks?label=%E6%9C%80%E6%96%B0%E6%AD%A3%E5%BC%8F%E7%89%88)](https://github.com/szx-jn/DimensionWorks/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

维度工序希望把资源获取、加工、运输、存储和自动化组织成一套相互依赖的工业体系。玩家需要理解工艺、设计生产线、解决产能与物流约束，并逐步建立跨维度协作网络。

## 当前版本

| 项目 | 当前正式发行版 |
| --- | --- |
| 版本 | **0.4.12** |
| Minecraft | 1.21.1 |
| 加载器 | NeoForge |
| NeoForge 版本 | 21.1.267 |
| Java | 21 |

正式发行版已完成从 Minecraft 1.20.1 Forge 到 Minecraft 1.21.1 NeoForge 的版本迁移。旧版与当前版本属于不同 Minecraft/加载器环境，**不要直接用当前版本文件覆盖旧实例**。首次安装请使用完整客户端包；已有实例更新前应确认对应 Release 的更新说明和更新器支持范围。

版本号和可下载文件以 [GitHub Releases](https://github.com/szx-jn/DimensionWorks/releases) 为准。仓库中的迁移记录主要用于追溯迁移过程与验证边界，不应被理解为当前正式版仍停留在迁移前状态。

## 下载与安装

前往 **[GitHub Releases](https://github.com/szx-jn/DimensionWorks/releases/latest)** 下载正式发行版。

- 首次安装：下载完整客户端包 `DimensionWorks-版本号.mrpack`，使用兼容的整合包启动器导入。
- 部署服务器：下载 `DimensionWorks-Server-版本号.zip`，按服务端包内说明启动。
- 更新已有实例：只有在对应版本说明明确支持时，才使用增量更新包或更新器；增量包不是完整安装包。

每次 Release 实际提供的文件可能不同，请以该版本的资产列表和发行说明为准。更新器使用独立的 aria2 下载引擎时，需同时下载对应平台的 `aria2c` 文件并与更新器放在同一目录；缺失或校验失败时会回退到内置下载器。第三方源码与许可信息随 Release 提供。

## 核心体验

- **Create**：机械动力、物品物流与生产自动化。
- **Mekanism**：复杂工业加工与资源处理。
- **Applied Energistics 2**：数字化存储、物流和自动化。
- **多维度资源生态**：让不同维度承担长期生产职责，而不是一次性探索后被废弃。
- **跨维度工业网络**：把分散在不同维度的资源与生产环节连接起来。

整合包的进阶重点不是单纯增加配方材料或延长等待时间，而是让玩家逐步掌握工艺组织、跨模组协作、产能规划、物流设计与系统诊断。关键进度会从整个生产体系审查替代路线，避免单台机器或低成本捷径绕过预期的工业挑战。

## 设计与开发原则

1. **资源来源多样化**：采矿、农业、生物、流体、化学和工业副产物回收都可以成为资源链的一部分。
2. **维度具有长期工业价值**：每个维度都应融入持续运行的生产体系。
3. **自动化带来实际收益**：自动化应在持续产出、资源效率、操作负担和扩展能力上优于长期手工操作。
4. **系统约束必须真实存在**：工艺、动力、吞吐、物流和副产物循环共同决定生产能力。
5. **重视服务器长期性能**：设计时考虑区块加载、机器与实体数量以及持续运行成本。
6. **验证结果如实记录**：构建成功不等于游戏内验证通过；未实际测试的行为不会标记为已验证。

更完整的设计规则见[工业能力考核说明](docs/gameplay/industrial-assessment.md)及[文档索引](docs/README.md)。

## 文档与项目入口

| 内容 | 入口 |
| --- | --- |
| 设计资料、技术规格与验证记录 | [docs/README.md](docs/README.md) |
| 玩家与服主使用指南 | [wiki/README.md](wiki/README.md) |
| 服务端打包与部署说明 | [server/README.md](server/README.md) |
| 项目开发约束与 AI 助手指引 | [AGENTS.md](AGENTS.md) |
| 版本变更与下载 | [GitHub Releases](https://github.com/szx-jn/DimensionWorks/releases) |
| 问题反馈与功能建议 | [GitHub Issues](https://github.com/szx-jn/DimensionWorks/issues) |
| 自动化构建记录 | [GitHub Actions](https://github.com/szx-jn/DimensionWorks/actions) |

## 参与项目

欢迎提交 Bug 报告、兼容性测试结果、游戏体验反馈、设计建议和文档改进，也欢迎提交代码 Pull Request。

提交问题时，请尽量提供：

- 整合包版本，以及客户端或服务端环境；
- 清晰的复现步骤和预期结果；
- 相关日志、崩溃报告或截图；
- 如果是迁移问题，请说明旧版本、更新方式和是否使用了备份副本。

代码贡献会经过项目维护者审查，并需要符合仓库中的设计约束、实现规范和验证要求。

## 许可

本仓库中由 DimensionWorks 项目原创的代码、配置、脚本和文档默认采用 [MIT License](LICENSE)。

第三方 Mod、资源、数据包及其他外部组件不适用本仓库的 MIT License，应遵循各自的许可证和授权条款。
