# Quark 与铁丝网

## 目标与固定版本

加入 `quark:grate`（铁丝网），保留 Quark 默认建筑装饰及其他方块、默认机制，关闭世界生成相关修改，并关闭与 Inventory Profiles Next 重复的 Quark 背包整理。

- Quark：`1.20.1-4.0-462`，Modrinth 固定版本 ID `DijQLauD`。
- 必需前置 Zeta：`1.20.1-1.0-31`，固定版本 ID `mSER3jBI`。
- 两者均为 Minecraft 1.20.1 Forge 版本，客户端和服务端必装。Zeta 是 Quark 的必需运行库，不是额外内容扩展。
- Quark 发布 Jar 声明 Zeta `[1.0-28,2.0)`，Zeta 声明已安装的 Quark 至少为 `4.0-461`；当前选定版本互相满足。
- 来源：[Quark 固定版本](https://modrinth.com/mod/quark/version/DijQLauD)、[Zeta 固定版本](https://modrinth.com/mod/zeta/version/mSER3jBI)。清单以 `manifest/mods.json` 为真相源。

铁丝网是第三方新方块，现有模组与 KubeJS 不能直接提供同一个方块的碰撞、寻路和掉落物穿透行为，因此加入 Quark 和必需前置，不另写兼容 Java 模组，也不修改第三方源码。

## 铁丝网与配方

沿用 Quark 内置配方 `quark:building/crafting/grate`：四个铁栏杆摆成 2×2，合成一个铁丝网。配方条件为模块标志 `grate`；已显式启用 `[building].Grate`，不重复注册 KubeJS 配方。

Quark 自带简体中文名“铁丝网”及 JEI 说明：物品实体可穿过，无牵绳动物会避免主动走上去，玩家、怪物及被牵绳的动物可以通过。上述来自固定发布 Jar 的语言与方块实现，仍需游戏内验收。

## 配置取舍

`config/quark-common.toml` 是 Forge COMMON 配置，必须随客户端和服务端共同分发，不放入只针对 SERVER 配置的 `defaultconfigs/`。

| 配置 | 设置与影响 |
| --- | --- |
| `categories.building` | 开启，保留铁丝网、竖半砖、各种建筑材料、框架玻璃、木柱、绳索等默认建筑功能。 |
| `categories.world` | 关闭整个世界类别，禁止 Quark 的矿物/石头集群、地下环境、新树木、末地地貌、结构与熔岩口袋等世界修改。这个类别中绑定世界内容的木材/石材模块也随之禁用，不将它们误报为仍可自然获得的装饰材料。 |
| `categories.mobs` | 关闭新增生物类别，避免新增生物自然生成和相关生物机制；不影响原版动物及饲料槽模块。 |
| `general.Terrablender Add Region` | 关闭，避免在将来有 TerraBlender 时注入生物群系区域。 |
| `general.Terrablender Modify Vanilla Anyway` | 关闭，不强制修改原版主世界群系参数预设。 |
| `management.Inventory Sorting` | 关闭，背包整理继续交给现有 Inventory Profiles Next。 |
| 其他分类与模块 | 不覆盖默认值，不额外开启默认关闭的实验功能，不安装 Quark Oddities。自动补工具、一键存取等其他背包操作仍沿用默认，不以“关闭整理”为由关闭整个管理分类。 |

配置键已按固定 Zeta Jar 的 `ConfigManager`、`ConfigObjectMapper`、`ForgeBackedConfig` 及 Quark 模块声明静态核对：分类键位于 `[categories]`，模块开关以显示名保留空格和大小写，文件名为 Forge 生成规则中的 `quark-common.toml`。其他分类的模块没有发现独立的世界生成注册入口。

## 保留机制概览

| 类别 | 代表功能 |
| --- | --- |
| 自动化 | 红石合成器、发射器放方块/补种、活塞推动铁杆破坏方块、饲料槽繁殖、溜槽。 |
| 活塞 | 搬运允许移动的方块实体，锁链让连接方块一起移动；保持原版默认推力上限。 |
| 红石输入与元件 | 随机器、视线感应的末影观察者、金属按钮、仅玩家触发的黑曜石压力板、重力沙。 |
| 背包管理 | 一键存取、工具自动补充、整行快捷栏切换、物品链接分享、盔甲架快速换装。Quark 自己的整理除外。 |
| 工具和建造便利 | 回旋镐、抹刀、边缘放置、竖半砖、绳索等；依赖已禁用世界内容的功能仍受原模块条件限制。 |
| 原版微调与客户端 | 双门联动、收割、海绵增强、鞘翅相关便利、提示和查看功能等，遵循默认开关与防重叠逻辑。 |

这些默认机制有部分与 Create 的自动化路线重叠，此取舍由用户明确选择保留。Quark Oddities 的背包、磁铁、矩阵附魔等不在此次依赖范围。

不新增自定义世界生成、强加载区块、常驻机器或实体。保留机制只在玩家使用对应功能或放置设备后起作用；饲料槽会查询附近动物，因此沿用模块默认的查询间隔、数量限制和范围，避免用大量动物阵列堆吞吐。

## 验证与游戏内验收

本地只做 JSON/TOML 语法、固定版本与依赖、配置键和配方引用等静态检查，不构建或运行 Minecraft。正式构建、校验与发布由 `.github/workflows/release-pack.yml` 完成。

游戏内需核对：铁丝网可在 JEI 查询及由铁栏杆合成；掉落物穿透、玩家碰撞与动物寻路正常；建筑分类正常；Quark 整理按钮关闭而 Inventory Profiles Next 正常；新生成区块没有 Quark 世界地貌和新增生物；客户端与专用服务器均可加载。自动化构建通过不等于已完成这些实测。
