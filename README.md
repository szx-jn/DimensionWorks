# 维度工序 · DimensionWorks

**Minecraft 1.20.1 · Forge 47 · Java 17**

维度工序（DimensionWorks）是一款以多维度探索、工业化生产、自动化资源获取和跨维度物流为核心的 Minecraft 工业整合包。

> **当前版本：0.1.20** · [查看最新正式版与下载文件](https://github.com/szx-jn/DimensionWorks/releases/latest)

## 下载

请前往 **[GitHub Releases](https://github.com/szx-jn/DimensionWorks/releases/latest)** 获取最新版本。正式发布包含客户端整合包、服务端整合包、增量更新文件、更新清单、独立更新脚本及 SHA-256 校验文件。

| 文件 | 用途 |
| --- | --- |
| `DimensionWorks-版本号.mrpack` | 客户端整合包，供兼容的整合包启动器导入 |
| `DimensionWorks-Server-版本号.zip` | 服务端整合包 |
| `*-delta.zip` | 相对上一正式版本生成的增量更新包 |
| `*-update-manifest.tsv` | 更新清单 |
| `update-dimensionworks-client.*` | 客户端独立更新脚本 |
| `update-dimensionworks-server.*` | 服务端独立更新脚本 |
| `SHA256SUMS` | 发布文件 SHA-256 校验值 |

增量包用于版本更新，不是完整整合包；首次安装请下载对应的完整客户端或服务端包。具体文件以每个 Release 实际提供的资产为准。

## 项目方向

- **Create 6**：机械加工、物流与生产自动化
- **Mekanism**：高级工业、能源与资源处理
- **Applied Energistics 2**：数字化存储、物品物流与自动化
- **多维度资源生态**：让不同维度在长期生产链中发挥作用
- **跨维度物流**：逐步构建统一的多维度工业网络

核心目标不是简单堆叠机器，而是让资源获取、加工、运输、存储和自动化形成相互关联的生产体系。

## 当前开发状态

项目已建立可重复的客户端与服务端打包发布流程。发布工作流会生成完整包、增量包、更新清单、校验文件、macOS/Linux Shell 更新器，以及客户端与服务端 Windows EXE，并在发布前验证归档结构及清单一致性。

**当前仍处于持续开发阶段。** Mod 组合、配方、科技进度、维度规划和配置可能随版本变化；版本号代表发布版本，不意味着所有设计目标均已完成。

## 技术环境

- Minecraft：1.20.1
- Mod Loader：Minecraft Forge
- Java：17
- 构建环境：GitHub Actions、Gradle 8.8 与 Python 辅助打包脚本

## 仓库结构

```text
DimensionWorks/
├── config/             # 整合包配置
├── defaultconfigs/     # 默认配置
├── kubejs/             # KubeJS 配方、脚本与数据
├── resourcepacks/      # 自定义资源包
├── scripts/            # 版本管理、打包及验证脚本
├── release-updaters/   # 客户端与服务端独立更新脚本
├── server/             # 服务端相关配置与说明
├── docs/               # 设计文档与规划
├── VERSION             # 当前整合包版本
└── README.md
```

## 设计原则

1. **资源获取多样化**：采矿、农业、生物、流体、化学与工业副产物回收都可以成为资源链的一部分。
2. **维度具有长期价值**：不同维度应融入持续运行的生产体系，而不只是一次性探索地点。
3. **自动化服务于生产**：围绕完整生产链设计机器与物流。
4. **跨维度物流是核心系统**：逐步形成多维度协同的工业网络。
5. **重视长期运行性能**：设计时考虑区块加载、机器数量和服务器运行成本。
6. **发布产物可验证**：对客户端、服务端及更新文件进行打包检查，并提供 SHA-256 校验值。

## 参与开发

欢迎通过 GitHub Issues 反馈问题、提出建议，或通过 Pull Request 贡献改进。提交问题时，请尽量附上整合包版本、复现步骤、相关日志以及客户端或服务端环境信息。

- [提交 Issue](https://github.com/szx-jn/DimensionWorks/issues)
- [查看发布记录与下载](https://github.com/szx-jn/DimensionWorks/releases)
- [查看 GitHub Actions 构建记录](https://github.com/szx-jn/DimensionWorks/actions)

## License

本仓库中由 DimensionWorks 项目原创的代码、配置、脚本和文档默认采用 MIT License。

第三方 Mod、资源、数据包及其他外部组件不适用本仓库的 MIT License，应遵循各自的许可证和授权条款。
