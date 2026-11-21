# Create 附属候选接入记录

2026-11-21 按用户指定加入 Create Aeronautics 与 Create: Crafted Beginning（用户中文称“机械动力机巧新生”）。目标整包为 Minecraft 1.21.1 / NeoForge；此次仅更新固定资产清单，不代表已完成客户端、服务器或旧存档专项验收，也没有创建正式发行版。

## 固定资产

| 模组 | 固定版本 | 来源 ID | 环境 | 依赖 |
| --- | --- | --- | --- | --- |
| Create Aeronautics | 1.3.3+mc1.21.1，Modrinth `demX2roU` | `oWaK0Q19` | 双端 | Create 6.0.10、Sable 2.0.6 |
| Sable | 2.0.6+mc1.21.1，Modrinth `fg9dTRz9` | `T9PomCSv` | 双端 | Modrinth 将 Create 列为可选；当前 Create 版本满足其 JAR 范围 |
| Create: Crafted Beginning | 0.1.1，CurseForge 文件 `9658463` | 项目 `1802168` | 双端 | Create `>=6.0.10,<6.1.0` |

Sable 是 Aeronautics 的必需依赖，因此作为第三个清单条目加入。Crafted Beginning 的 JAR 元数据还将 Aeronautics 与 Sable 声明为可选联动，并声明 `createcraftedbeginning` 为实际 mod ID。没有添加 Create: New Age。

## 检查与限制

- Modrinth 官方元数据确认 Aeronautics 1.3.3 与 Sable 2.0.6 均针对 NeoForge 1.21.1，Create 版本依赖满足现有清单。
- 已下载固定 JAR；Aeronautics 与 Sable 的 SHA-512 与 Modrinth 元数据一致，相关 JAR 压缩完整性检查通过。
- CurseForge 官方文件 API 确认 Crafted Beginning 文件 `9658463` 为 `createcraftedbeginning-0.1.1.jar`，支持客户端、服务端、Minecraft 1.21.1 与 NeoForge；JAR 压缩完整性检查通过。上游将 0.1.1 标记为 Beta。
- Crafted Beginning JAR 元数据仅强制要求已存在的 Create 6.0.10；其 JEI、KubeJS、Curios、Sable、Aeronautics 项均为可选依赖。
- Aeronautics 上游源码许可将代码标为 MIT、素材标为 All Rights Reserved；Sable 使用 PolyForm Shield 1.0.0。原始 JAR 带有许可文件，但正式分发含 Aeronautics 素材的整包前，仍须确认上游许可允许该分发方式。此次不发布。
- Aeronautics 1.3.3 会在 Iris 光影激活时关闭热气渲染，并修复创意物理法杖/柱塞发射器的光束与绳索显示；Crafted Beginning 0.1.1 也修复紫水晶镜的 Iris 光束渲染。本整包使用 Iris，仍需在正式发布前做客户端启动与现有光影实测。
- Sable 上游警告其 mixin 覆盖较深、容易与其他模组出现兼容问题。未验证 Aeronautics 物理载具与其他机械模组的运行时冲突、性能或旧存档表现。
