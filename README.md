# 维度工序 · DimensionWorks

Minecraft 1.20.1 Forge 多维度工业自动化整合包。

## 项目定位

**维度工序（DimensionWorks）**以多维度探索、工业化生产、自动化资源获取与跨维度物流为核心。

当前核心方向：

- Create 6：机械自动化与基础工业
- Mekanism：高级工业、能源与资源处理
- Applied Energistics 2：数字化存储、物流与自动化
- 多维度资源生态：让不同维度长期参与工业体系
- 跨维度物流：逐步建立统一的工业网络

> 项目目前处于早期设计与开发阶段，Mod 列表、配方、科技树和具体维度职责仍会持续调整。

## 开发环境

- Minecraft：1.20.1
- Mod Loader：Forge
- Java：17

## 仓库结构

```text
DimensionWorks/
├── config/             # 整合包配置
├── defaultconfigs/     # 默认配置
├── kubejs/             # KubeJS 配方、脚本与数据
├── resourcepacks/      # 自定义资源包
├── scripts/            # 独立的辅助/构建脚本
├── server/             # 服务端相关配置与说明
├── docs/               # 设计文档、科技树、维度与资源规划
├── LICENSE
├── .editorconfig
├── .gitattributes
├── .gitignore
└── README.md
```

## 设计原则

1. **资源不等于矿石**：资源获取应包含采矿、农业、生物、流体、化学、环境机制、工业副产物回收等多种方式。
2. **维度具有长期工业价值**：不同维度不应只是一次性探索和搜刮地点。
3. **自动化服务于生产体系**：重点是建立生产链，而不是单纯堆叠机器。
4. **跨维度物流是核心系统**：最终目标是形成完整的多维度工业网络。
5. **服务器性能优先**：自动化设计需要考虑区块加载、机器数量和长期运行成本。

## 状态

🚧 早期开发 / 设计阶段

## License

本仓库中由 DimensionWorks 项目原创的代码、配置、脚本和文档默认采用 MIT License。

第三方 Mod、资源、数据包以及其他外部组件不适用本仓库的 MIT License，应以其各自的许可证和授权条款为准。
