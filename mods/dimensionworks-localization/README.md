# DimensionWorks 简体中文汉化

`dimensionworks_localization` 是随 DimensionWorks 客户端分发的资源型 Forge Mod，只补充当前锁定版本中第三方 Mod 缺失的简体中文内容，不注册物品、方块、命令、网络包或 Java API。

## 覆盖范围

- 按当前安装模组版本补齐全部缺失的 `zh_cn` 语言键；以构建预检统计为准。
- AE2 GuideME 手册 119 页。
- ExtendedAE GuideME 手册 32 页。
- 车万女仆 Patchouli 手册 55 页。

第三方 Jar 已经提供的 `zh_cn` 语言键和中文手册页面不会被覆盖。服务端专用 LuckPerms、Mod 元数据、硬编码在 Java 或渲染代码中的英文，以及无法通过资源覆盖替换的字符串不属于本 Mod 的范围。

## 资源布局

- 普通语言键：`assets/<命名空间>/lang/zh_cn.json`
- GuideME 页面：`assets/<命名空间>/<指南目录>/_zh_cn/`
- Patchouli 页面：`assets/<命名空间>/patchouli_books/<书>/zh_cn/`

Patchouli 页面从锁定版本的 `en_us` 页面复制结构，可见文本通过本 Mod 补充的语言键解析。车万女仆当前版本已经提供这些语言键，因此不重复翻译其正文。

## 术语规则

`AE2`、`ME`、`FE`、`RF`、`SU`、`RPM`、`mB`、`NBT`、`GUI`、`JEI`、`EMI`、`HUD` 等缩写保留原拼写，周边普通词照常翻译。例如 `ME Drive` 写作 `ME 驱动器`。Mod、人物和产品专名同样保留原拼写。

翻译必须保留源英文中的 `%s`、`%1$s`、`%%`、颜色代码、换行、Markdown 链接、`ItemLink` 标签和格式标记。需要调整参数顺序时使用显式位置参数。

初版 GuideME 译文曾采用机器翻译及术语后处理。`0.1.3` 针对原有全部语言 JSON、GuideME 页面及 Patchouli 资源进行集中审校：纠正误译、重复词、中文语序、技术术语及指南操作步骤。保留原有资源结构与页面链接；未发现问题或已有可靠简体中文的内容不作无意义改写。构建预检负责检验语言键与资源完整性，游戏内表现仍应以实际客户端为准。

## 构建

在本目录执行：

```sh
gradle build --no-daemon
```

当前产物为 `build/libs/dimensionworks_localization-0.1.3.jar`。根级 `assets/` 通过 `build.gradle` 的 `processResources` 映射到 Jar 内同名 `assets/` 目录。

## 校验

在仓库根目录执行：

```sh
python3 scripts/check_localization.py
```

校验器扫描 DimensionWorks 本地实例的全部第三方 Jar，合并第三方原始 `zh_cn` 与本 Mod 覆盖，并检查：

- 当前锁定模组的全部缺失语言键和 206 个手册页面是否完整覆盖。
- 是否出现无源键、重复 JSON 键、占位符变化。
- 是否缺页或覆盖了第三方后来已经补齐的条目。
- 是否存在没有对应英文页面或已被第三方补齐的手册覆盖。

`scripts/pack_hook.py` 会在打包预检时运行同一套校验；`--skip-build` 不会跳过汉化校验。

## 基线更新

本 Mod 以当前本地实例锁定的 Jar 版本为基线，不自动跟随移动版本。来源 Mod 更新后，应先运行校验器，按它报告的缺失键、过期覆盖和新增页面更新资源，再重新构建和打包。

## FTB Chunks 2001.3.8

补齐该版本新增的队伍属性默认值中文键，避免发布预检遗漏语言键。此修订对应汉化 Mod 版本 0.1.1。

## 龙之进化及 ECO AE 前置（0.1.2）

新增 Brandon's Core、Draconic Evolution、CodeChicken Lib、LDLib 的 263 个中文语言键；只覆盖第三方原本缺少翻译的文本，保留格式占位符。

## 全量汉化质量审校（0.1.3）

- 涵盖原有 58 个语言 JSON 命名空间及全部翻译键；逐类审查并修正明显机翻和错误表达。
- 审校 AE2 GuideME 119 页、Extended AE GuideME 32 页，共 151 页；核心机制、自动合成、存储、线缆、终端、子网络与生产线示例均进行重点重译。
- 检查车万女仆 Patchouli 的 55 个 JSON：它们沿用原有语言键引用，不把资源键误改为界面文字。
- 在静态核对中检查全部 GuideME 页面间 Markdown 链接路径与 JSON 解析；完整语言键、参数格式及来源 Jar 覆盖仍由 `scripts/check_localization.py` 在打包预检中验证。
