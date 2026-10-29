# 维度工序 · DimensionWorks

**Minecraft 1.20.1 · Forge 47 · Java 17**

维度工序（DimensionWorks）是一款以多维度探索、工业化生产、自动化资源获取和跨维度物流为核心的 Minecraft 工业整合包。

> **当前正式版：** [![最新正式版](https://img.shields.io/github/v/release/szx-jn/DimensionWorks?label=%E6%9C%80%E6%96%B0%E6%AD%A3%E5%BC%8F%E7%89%88)](https://github.com/szx-jn/DimensionWorks/releases/latest) · [查看全部发行版](https://github.com/szx-jn/DimensionWorks/releases)

## 下载

请前往 **[GitHub Releases](https://github.com/szx-jn/DimensionWorks/releases/latest)** 获取最新版本。正式发布包含客户端整合包、服务端整合包、增量更新文件、更新清单、Windows/Linux/macOS Go 更新器及 SHA-256 校验文件。旧版 Shell 更新脚本暂时保留作为兼容选项。

| 文件 | 用途 |
| --- | --- |
| `DimensionWorks-版本号.mrpack` | 客户端整合包，供兼容的整合包启动器导入 |
| `DimensionWorks-Server-版本号.zip` | 服务端整合包 |
| `*-delta.zip` | 相对上一正式版本生成的增量更新包 |
| `*-update-manifest.tsv` | 更新清单 |
| `update-dimensionworks-client-windows-amd64.exe` | Windows 客户端更新器 |
| `update-dimensionworks-server-windows-amd64.exe` | Windows 服务端更新器 |
| `update-dimensionworks-{client,server}-linux-{amd64,arm64}` | Linux 更新器 |
| `update-dimensionworks-{client,server}-macos-{amd64,arm64}` | macOS 更新器 |
| `update-dimensionworks-client.sh` / `update-dimensionworks-server.sh` | 旧版 Shell 兼容更新器 |
| `SHA256SUMS` | 发布文件 SHA-256 校验值 |

增量包用于版本更新，不是完整整合包；首次安装请下载对应的完整客户端或服务端包。具体文件以每个 Release 实际提供的资产为准。

## 项目方向

- **Create 6**：机械加工、物流与生产自动化
- **Mekanism**：高级工业、能源与资源处理
- **Applied Energistics 2**：数字化存储、物品物流与自动化
- **多维度资源生态**：让不同维度在长期生产链中发挥作用
- **跨维度物流**：逐步构建统一的多维度工业网络

核心目标不是简单堆叠机器，而是让资源获取、加工、运输、存储和自动化形成相互关联的生产体系。玩家应当通过理解工艺、构建产线、处理系统约束来推进科技，而不是只靠记配方或堆叠材料。

## 设计原则

1. **资源获取多样化**：采矿、农业、生物、流体、化学与工业副产物回收都可以成为资源链的一部分。
2. **维度具有长期价值**：不同维度应融入持续运行的生产体系，而不只是一次性探索地点。
3. **自动化是核心进阶路径**：关键进度应鼓励玩家构建可持续、可扩展的生产线，而不是把机器当作孤立的合成工具。
4. **跨维度物流是核心系统**：逐步形成多维度协同的工业网络。
5. **重视长期运行性能**：设计时考虑区块加载、机器数量和服务器运行成本。
6. **发布产物可验证**：对客户端、服务端及更新文件进行打包检查，并提供 SHA-256 校验值。

### 工业能力考核与反绕过原则

这是整合包的核心设计约束，尤其适用于修复碎片、阶段性关键物品与终局进度。

- **禁止低成本替代路线**：不同获取方式可以存在，但不能以近似成本获得同等成果。手工合成允许存在，也可以最终成功；但若它绕过了预期工业流程，就必须付出极高的材料、时间、操作量或其他可衡量代价，使其成为可行但明显不划算的备用方案，而不是最优解。
- **不能用单台机器刷穿科技**：关键材料不能仅凭一台机器重复运行，就轻易、稳定且廉价地大量产出。设计必须让关键产出受到真实工艺条件、前后工序、输入供给、动力与吞吐能力、物流组织或副产物循环等约束。
- **难度必须来自理解与工程，而非单纯堆料**：不能只把配方改成更多材料、更长等待时间，就声称玩家掌握了工业技术。阶段性目标应要求玩家理解工艺原理、组织多工序协作、解决产能瓶颈，并在新情境中运用已学会的机制。
- **让自动化成为有意义的选择**：手工路线可以作为昂贵的兜底路径，但自动化应当在持续产出、资源效率、操作负担和扩展能力上具有显著优势。玩家应当因为理解并搭建工业体系而获得切实回报。
- **按系统整体审查绕过漏洞**：评估配方时不能只看单个配方。还要检查其他 Mod 的机器、替代合成、重复加工、循环配方、副产物回收和跨系统转换，避免某条低成本捷径破坏阶段目标。

因此，关键物品的验收标准不只是“玩家能不能合成出来”，而是“玩家是否需要理解并运用这一阶段的工业系统”。目标是让玩家通过学习和工程设计获得进展，而不是被迫进行没有技术含量的重复劳动。

## 当前开发状态

项目已建立可重复的客户端与服务端打包发布流程。发布工作流会生成完整包、增量包、更新清单、校验文件，以及 Windows amd64、Linux amd64/arm64、macOS amd64/arm64 的 Go 更新器，并在发布前验证二进制格式、归档结构及清单一致性。旧版 Shell 更新器暂时保留用于兼容。

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
├── release-updaters/   # 旧版 Shell 兼容更新器
├── update-Windows/     # 跨平台 Go 更新器与测试（目录名沿用历史命名）
├── server/             # 服务端相关配置与说明
├── docs/               # 设计文档与规划
├── VERSION             # 当前整合包版本
└── README.md
```

## 参与开发

欢迎通过 GitHub Issues 反馈问题、提出建议，或通过 Pull Request 贡献改进。提交问题时，请尽量附上整合包版本、复现步骤、相关日志以及客户端或服务端环境信息。

- [提交 Issue](https://github.com/szx-jn/DimensionWorks/issues)
- [查看发布记录与下载](https://github.com/szx-jn/DimensionWorks/releases)
- [查看 GitHub Actions 构建记录](https://github.com/szx-jn/DimensionWorks/actions)

## License

本仓库中由 DimensionWorks 项目原创的代码、配置、脚本和文档默认采用 MIT License。

第三方 Mod、资源、数据包及其他外部组件不适用本仓库的 MIT License，应遵循各自的许可证和授权条款。
