# 0.3.10 Sophisticated 模组启动兼容性修复

## 问题与修复方案

用户提供的 Forge 错误界面显示：Sophisticated Backpacks 在 `common_setup` 阶段找不到 `net/p3pp3rf1y/sophisticatedcore/linkedstorage/ILinkedStorageContentsBinding`。

核对 GitHub Release `v0.3.10` 的客户端更新清单后，确认正式包包含 Core `1.20.1-1.5.26.2457`，但仍固定旧背包 `1.20.1-3.26.3.2157` 与旧 Create 联动 `1.20.1-0.1.10.167`。提交 `520be64` 在加入 Sophisticated Storage 时升级了 Core，未同步更新其消费者。

下载固定 Modrinth 版本的原始 JAR 后，静态检查确认新 Core 已不包含上述接口；旧背包主类及其链接存储代码仍引用该接口，旧 Create 联动的容器菜单也有相同引用。此外，旧背包还引用新 Core 中不存在的 `LinkedStorageGroupManager$GroupChange`。

采用同步升级消费者的修复，固定以下组合：

| 模组 | 版本 | Modrinth 版本 ID |
| --- | --- | --- |
| Sophisticated Core | `1.20.1-1.5.26.2457` | `c90my5iF` |
| Sophisticated Storage | `1.20.1-1.5.15.2220` | `S0AtW2W5` |
| Sophisticated Backpacks | `1.20.1-3.26.19.2239` | `VjliRs3R` |
| Sophisticated Backpacks Create Integration | `1.20.1-0.1.13.193` | `E16cU80t` |

新背包的 `mods.toml` 要求 Core 至少 `1.5.21.+`，Storage 要求至少 `1.5.24.+`，新联动要求背包至少 `3.26.11`、Create 至少 `6.0.7`；当前固定组合满足这些约束。不能只回退 Core 至 `1.5.1.2335`，因为这会违反新增 Storage 的依赖要求。上游联动 `0.1.11.173` 的更新记录说明适配新 Core，`0.1.13.193` 进一步支持背包跨存储系列共享链接内容。

这次改动仅更新 `manifest/mods.json` 中现有背包和 Create 联动的两个固定版本。正式发布时由既有工作流自动递增整合包补丁号并记录 `VERSION`。

## 已执行的静态验证

- 对修复前后涉及的 6 个原始 JAR 校验 Modrinth 文件大小、SHA-512 和 ZIP CRC；全部通过。
- 修复后的 4 项清单版本、项目 ID、Minecraft `1.20.1` 和 Forge 标记与 Modrinth 元数据一致。
- 用 Python 解析类文件常量池中的类型和描述符，核对四个 Sophisticated 模组之间的类引用。旧组合扫描 1,640 个类，有上述 2 个缺失类；新组合扫描 1,638 个类、10,603 条引用，Sophisticated 命名空间内缺失类为 0。
- REA CLI `6.4.0` 对 Core 原包执行 ZIP 清单检查，Evidence ID 为 `ev_b0d55e4c51700fb131a1e041acf55bf3cc0f31d83074d5c2235811aa6a4e691d`。类文件解析和依赖元数据检查由普通 Python 工具执行，REA 清单证据仅用于确认原包身份与归档条目。
- `git diff --check`、`pack_hook.check_manifest()`、`pack_hook.check_json()` 和 `pack_hook.check_pack_version(None, dimension_complete=False, major_bump=False)` 通过；清单检查覆盖 177 项，JSON 检查覆盖 96 个文件，下一补丁版本解析为 `0.3.11`。

## 验证边界与临时处理

本次未执行 Minecraft 图形客户端或服务端启动，也未验证存档加载及背包、Create 装置的游戏内交互。类引用检查仅覆盖这四个模组的类名及描述符，不证明全部方法签名、Mixin 或运行时行为兼容。

在修复整合包正式发布前，使用 `v0.3.10` 的玩家可退出游戏，将 `mods/` 中旧背包与旧 Create 联动 JAR 替换为表内固定版本，各保留一份；Core 与 Storage 使用表内版本。对应下载页：

- [Sophisticated Backpacks 3.26.19.2239](https://modrinth.com/mod/sophisticated-backpacks/version/VjliRs3R)
- [Sophisticated Backpacks Create Integration 0.1.13.193](https://modrinth.com/mod/sophisticated-backpacks-create-integration/version/E16cU80t)
