# 附魔工业 2.x 升级

## 目标与范围

将清单中已有的 Create: Enchantment Industry 从 1.4.1 升级为固定版本 2.5.4b（Modrinth 版本 ID：`fs3kihCQ`），提供烈焰人锻造室（Blaze Forger，`create_enchantment_industry:blaze_forger`）。沿用原模组 ID，客户端与服务端均必装。

这是原模组的新版功能，旧版与 KubeJS 无法直接提供相应机器行为，因此升级现有依赖。无需新增其他模组；清单已包含新版要求的前置。

## 静态兼容性依据

- 来源：<https://modrinth.com/mod/create-enchantment-industry/version/fs3kihCQ>。
- 发布 Jar 的 `META-INF/mods.toml` 要求 Minecraft `[1.20.1, 1.21)`、Forge `[47.1.0, 48)`、Create `[6.0.8, 6.1.0)`、Create: Dragons Plus `[1.11.9, 1.12.0)`。当前模组清单固定的 Create 6.0.8、Dragons Plus 1.11.9 满足要求。
- 可选联动 Touhou Little Maid 要求 `[1.5.3, 1.6.0)`，清单中的 1.5.3 满足要求。
- Jar 中包含锻造配方 `data/create_enchantment_industry/recipes/smithing/blaze_forger.json`，由烈焰人升级锻造模板、烈焰人燃烧室和铁砧制成烈焰人锻造室。
- 新版包含独立的 `create_enchantment_industry:grinding` 配方类型，内置配方仍采用 Create 的 `ingredients` / `results` JSON 结构，与现有粉碎 API 的编码形式一致。

## 存档与运行边界

升级前备份旧存档。2.x 的注册内容与 1.x 存在差异，例如新版不再包含旧祛魔池（`disenchanter`）、附魔指南（`enchanting_guide`）的同名语言键；不能据此假定旧设备、物品或 NBT 会自动迁移。应在副本存档中核对原有附魔产线、流体和物品后再更新正式服务器。

本次仅更新模组清单，不添加自动放置的机器、实体或强加载区块。新产线应按需部署，复用既有动力与物流，避免长期运行空载机器阵列。

以上为发布元数据与 Jar 静态核对，不代表已完成 Minecraft 启动、旧存档迁移或游戏内功能验证。正式打包与发布使用仓库现有 GitHub Actions 流程。
