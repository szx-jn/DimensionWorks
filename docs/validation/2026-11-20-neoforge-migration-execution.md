# 1.21.1 NeoForge 迁移执行记录

依据：[完整迁移规划](../superpowers/plans/2026-11-20-neoforge-1.21.1-migration.md)。目标分支 `1.21.1-neoforge`，源整包版本 `0.3.52`。用户已批准旧存档保全与远端操作，并明确禁止中间发行版，允许舍弃凋零风暴。本记录随实际验证更新，不将阶段完成等同于整包完成。

## 交付与操作约束

- 没有触发发行工作流、创建 Release、提升根 VERSION 或更新生产整包。发行工作流新增迁移分支阻断，早于版本计算、构建和发布执行。
- 生产服务器原本已停机；备份期间未启动旧服或打开生产存档。测试材料位于仓库外的私有实验目录，禁止进版本库。
- 本机 16 GiB 内存。Gradle 限 2 GiB、2 个工作线程；当前 GameTest 每次最大 2 GiB，按顺序运行。完整测试服后续限 4–6 GiB，避免与大型构建同时运行。

## 已实施

| 范围 | 当前结果 | 未完成的验收 |
|---|---|---|
| 构建基线 | 根 Wrapper 9.2.1，JAR 与分发包固定官方 SHA-256；MDG 2.0.148、NeoForge 21.1.267、Java 21；共享构建与独立 JAR | 其余 4 个自研项目逐项移植 |
| 拾取保护 | NeoForge 构建；已移除实体与部分转移两项真实 GameTest 通过 | Create 与背包同时拾取的实际产线 |
| 掉落寿命 | NeoForge 构建；改用公开 FTB API，删除反射及静默查询失败；四种组件稀有度的真实 GameTest 通过 | 真实玩家已圈地区块的 5 分钟规则 |
| FTB 强加载限制 | NeoForge 构建；真实团队与圈地 GameTest 通过，强加载被拒绝、正时间不生效、取消圈地可用；目标类与 lambda 注入重定位 | 客户端地图、旧圈地恢复、真实玩家退出与完整停服 |
| RPM 与齿轮之心 | NeoForge 构建；83 项 JUnit、34 项服务器 GameTest 通过；实际玩家附件/物品组件落盘、动力恢复、Mek 缓存操作及 AE2 双 CPU 提交/输出配额 | 图形控制器/拓扑、实际玩家与真实配方生产链、旧档转换和重启对账 |
| ME 机械供能与跨维存储 | NeoForge 编译、111 项 JUnit 与 17 项服务器 GameTest；真实 AE 网络分配、气体运输、能量能力限制、库存组件/数量往返和驱动器拆除 | 实际客户端、龙堆/多方块长期运行、安瓿接口与整包旧档转换 |
| 洞穴工厂与熔液结晶 | NeoForge 编译；31 项 JUnit、16 项必需 GameTest；原有 16 条配方、真实渊海生产与独立重启、42 张动画和统一创造页签检查 | 原档方块/封装转换，完整 JEI/Jade/Ponder 交互与整包产线 |
| 汉化资源 | 资源型 lowcodefml 与格式 34；269 个资源全部逐字节进入目标 JAR，构建通过 | 对目标第三方 JAR 重审全部语言键、指南与客户端显示 |
| 版本检查 | NeoForge 配置任务与确认载荷；22 项 JUnit 与 1 项服务器 GameTest 通过，每次登录重新读取 VERSION | 真实客户端匹配/不匹配握手、更新界面与终端交接 |
| F4 旁观 | NeoForge 载荷；10 项 JUnit 与 2 项服务器 GameTest 通过，坐标/朝向/游戏模式及停服清理 | 真实客户端 F4、跨维度、碰撞、死亡和重连 |
| 魔法皮蛋 | 1.21.1 效果 Holder 与载荷；7 项 JUnit 与 2 项服务器 GameTest 通过，确认授权、持续时间/级别、历史/倒计时、拒绝重放和超限包 | 实际界面、KubeJS 食用/取消/继承/到期死亡 |
| 应力表报警 | Create 6.0.10 的真实方块/Mixin 验证通过，比较器档位、过载信号及实际邻接红石粉输出/清零；Mixin 查询限定到 GaugeBlock | 完整产线长期通知频率；目标清单尚待整批锁定 |
| 批量扳手 | 公开 FTB 接口与目标方块交互；实际 AE2/Mekanism 同批拆除、按键恢复及拒绝交互保留机器两项 GameTest 通过 | 实际客户端选区、FTB 圈地、含库存机器掉落 |
| 冷备份校验 | 压缩包/清单 SHA-256、逐文件校验、原子交付与异常阻断；4 项工具测试通过 | 本地真实快照全量验证见下节 |
| 只读审计 | 所有维度区域与 SavedData/玩家/FTB SNBT，物流真实数量及身份审计；8 项针对性测试通过 | 原服全档读取通过；迁移后的语义对账待完成 |

十二个目标项目由根 `settings.gradle` 明确收录。旧清单与未移植工程暂时保留供逐项比较，不能作为目标版本运行或发布。没有把任意第三方 Forge JAR 直接塞入 NeoForge。

实际发现并处理了一次测试假成功：GameTest 模板命名空间与过滤命名空间不一致，Minecraft 报 `No test functions were given!`，Gradle 却返回成功。改用各自命名空间的独立模板，增加测试任务日志断言，要求注册数量、完成数量及全部必需测试通过一致。随后实际执行拾取保护 2 项、掉落寿命 1 项、FTB 圈地/强加载 1 项并通过。

## 冷备份与真实存档

源端版本及停机检查已完成。快照制作前后所有 4,010 个文件 SHA-256 一致，压缩包为 1,842,967,418 字节，原始载荷 2,648,434,889 字节。包含当前 `world`、模组、依赖、配置和身份相关文件；排除旧日志、崩溃日志与重复历史备份。

压缩包 SHA-256：`1973ca3f099e0e60c4b5463f1626b6df94a6e137dd1fbcce2f54223c626bb201`。

本地压缩包与清单 SHA-256、全部 4,010 个解包文件已校验通过；原压缩包和解包目录已设为只读。临时 SSH 配置已逐字节恢复并重载，两端临时密钥已删除，远端仍保持原来的停机状态。

当前原档 DataVersion 为 3465，含 13 个存储维度定义（包括六洞穴、`dw:transfer`、暮色、天境和 `witherstormmod:bowels`），现役玩家数据 11 份。区域头统计有 305,025 条方块区块、12,209 条实体区块、1,644 条 POI 记录；384 个零字节区域占位文件尚未写入数据。另有 94 个 TrueUUID 历史备份文件，这些仍保存在快照中，但不应重复统计为现役玩家资产。只读审计的方块解码采用批量处理，避免在 Python 中逐个循环数十亿个方块；全档读取已通过：369 个 NBT 文件、318,878 条区域记录、136,273 条物品栈记录、11 名玩家，解析错误为 0。FTB 的 55 个扩展 SNBT 文件由其公开解析器导出只读缓存，原文件及缓存均有 SHA-256 绑定。区域末尾未补齐扇区按实际 payload 长度校验。另修正同一方块多种状态的数量累加并增加回归，重算后的最终报告同样为 0 错误；读取通过不代表组件迁移与资产对账已完成。

另建完整的可写 Forge 参考副本，使用 Java 17、4 GiB 堆和 2 个处理器，只监听 `127.0.0.1:25575`。实测原档启动 `Done (2.656s)!`，14/14 启动脚本及 25/25 服务端脚本加载零错误、零警告。整段参考日志仍有 49 条 ERROR，包括第三方配方/进度引用未安装物品及 Aether Curios 槽位问题；这是源版本基线问题，不能报告为无错误启动，后续目标整包要据实际注册表逐项处理。

参考服完成测试后，通过仅匹配本地工作目录和专用启动参数的 PID 发送正常 JVM 停止信号，日志确认所有维度保存完成且进程退出。封存原档及生产服务器均未被游戏打开；尚未用 NeoForge 转换或打开原档。最终审计没有发现凋零风暴已放置方块、实体、库存物品或位于其维度的玩家；仍有维度定义、配方/进度和世界生成引用，转换阶段必须显式处理，不能仅删除 JAR。

## 目标依赖的最新核对

原调查仅发现 FTB Chunks 2101.1.22 与 Library 2101.1.35；本次实际下载核对发现官方较新的固定文件：

| 项目 | 采用目标 | 固定来源 |
|---|---|---|
| FTB Chunks | 2101.1.24 | [CurseForge 9594476](https://www.curseforge.com/minecraft/mc-mods/ftb-chunks-forge/files/9594476) |
| FTB Library | 2101.1.40 | [CurseForge 9650381](https://www.curseforge.com/minecraft/mc-mods/ftb-library-forge/files/9650381) |
| FTB Teams | 2101.1.12 | [CurseForge 9434081](https://www.curseforge.com/minecraft/mc-mods/ftb-teams-forge/files/9434081) |
| Architectury | 13.0.12 NeoForge | [Modrinth 固定版](https://modrinth.com/mod/architectury-api/version/13.0.12+neoforge) |

实际 JAR 要求 Library ≥2101.1.39、Architectury ≥13.0.11，这组版本闭合。先尝试官方 Maven，收到 403 后改用官方 Modpacks.ch 文件索引、CurseForge 固定 CDN 与 CurseMaven；下载文件核对官方索引 SHA-1，并计算本地 SHA-256。其余完整 Mod 清单、fork 补丁、45 个 KubeJS 脚本、配置、更新器与发行流水线的目标改造仍在后续阶段。

## 阶段检查

十二个模块实际构建通过，服务器 GameTest 共 79 项通过（洞穴基础检查与专项检查合计按 16 个不同测试计）；已移植模块共 266 项 JUnit 通过；审计回归 8 项和现有脚本检查 104 项通过。现有 BBSMC 测试夹具同步当前 `.update` 资产与按大小排列的附件契约，未改发布实现、未上传附件。PR 构建工作流改用 Java 21 与根 Wrapper，只构建已接入 NeoForge 的项目；共享构建变更覆盖所有已接入项目。

## Create 接入与测试配置

编译固定使用官方 Modrinth 发行 `UjX6dr61`（Create 6.0.10，Git ac0c444），与事先锁定的运行 JAR SHA-256 相同。共享脚本从同一个发行 JAR 提取 Registrate 1.3.0+67、Ponder 1.0.82 与 Flywheel 1.0.6，仅放在忽略的构建目录供编译；三份内嵌依赖逐字节一致，运行仍加载原始发行 JAR。未改第三方源码、未发布或重封装其资产。

应力表原实现把 `getSignal` 查询注入全局 `BlockBehaviour`；迁移后将方法限定到 Create `GaugeBlock`，避免无关红石查询都读取方块实体。应力表明确声明红石源，实际测试验证相邻红石粉接收 15 级信号及解除过载后清零，动力停止后比较器清零。速度表保留父类行为。

GameTest 运行明确使用自己的 sourceSet，测试专用 AE2/Mekanism/GuideME 依赖才会进入测试进程，仍不打进生产 JAR。曾出现测试依赖未进入进程、注册查询返回空气；已经修正，实际日志确认目标模组及两个拆除对象均存在。测试模拟连接在登录事件前配置，并声明实际 Architectury 载荷，避免把未完成握手的模拟客户端当成运行故障。变更共享配置后重新执行已接入模块的针对性测试。

## RPM 核心移植

RPM 保留原有公开接口、机器归属、限速与 `DWKineticAccounting` 持久化键；没有把卸载成员账目重建为只含已加载方块。生命周期与网络改用 NeoForge，Create 方块实体 read/write 注入补充注册表上下文，配方缓存根据目标版本实际字段采用 `RecipeHolder` 解包。配置读取删除反射和吞错；异常不再被伪装成成功启用转速。

齿轮之心按 Curios 9 注册不可手动摘下行为，修复位使用 `custom_data`、模型使用独立组件。转换物品复制整份组件，新增测试实际落盘后核对名称、附魔、其他自定义数据和修复位。离线 RPM 读取实际 NeoForge/Curios 附件格式，并以实际 `ServerPlayer.saveWithoutId` 输出验证第三修复阶段及命令覆盖。没有实现旧格式双路径；旧数据须在整包转换时迁移。

34 项 RPM 必需 GameTest 通过。包含真实 Mekanism 缓存操作的初始限流、额度补充、同 tick 拒绝和修复解除；两个独立供电 AE2 CPU 的第一任务、第二空闲 CPU 被配额拒绝、第四修复阶段立即允许第二任务，以及最终产物容量模拟、实际输出拒绝保持等待量、完全修复后实际接收。测试用缓存操作计数、外部发射计划及接收端夹具隔离模式计算，并非完整生产链验收。曾发现 GameTest 无档案缓存不能设置 Mek 真实玩家所有者、未供电 CPU 不出现在可用列表及模拟连接发送重载变化；已按实际夹具条件修正，没有为这些测试问题添加生产绕过。

本次在私有实验目录保留最终 `rpm-port-gametests-v14.log` 和目标 JAR 接口检查资料。该阶段之后仍须推进 ME 供能等模块、第三方闭包、脚本/配置、更新器和转换器仍未完成；没有让 NeoForge 打开生产原档。

## ME 供能与存储模块移植

ME 模块主代码改用 NeoForge 的生命周期、载荷、能力和物品组件。Mek 化学品与能量接口按固定目标 JAR 的真实 ABI 迁移，不保留 Forge 运行双路径。频道、归属 UUID、原始数量和容量键不变；嵌套物品/流体使用注册表感知的目标序列化。云盘不再忽略无效 UUID 或无法解析的库存，读取错误必须显式阻止；列表读取避免 `getList` 遇到错误元素类型后伪装为空库存。

实际测试定位并修复三项问题：新版 Mek `onUpdateServer` 返回同步标记，调度必须合并各次原生更新结果；NeoForge 下内存驱动器缺少 AE2 网络宿主能力，相邻设备会形成两个独立网；完整卡库存保存在方块掉落时，AE2 默认附加掉落会重复生成卡。分别按原生返回值、显式能力注册和单次带库存掉落处理，并增加真实内存网络与实际拆除测试。补齐五级内存驱动器目标掉落表。

能量板测试实际调用两个组件工厂、Mek 严格能量能力和 NeoForge FE 能力，证明带电输入组件清空、不能充入或导出。机器预算采用 `long` 原生整数，单元测试验证超过 double 精度范围时最后一个单位不会丢失或超取；实际停机粉碎机拒绝外部电能，虚拟预算不写入 NBT。移除仅对应旧 `FloatingLong` 缺陷的代码与过时测试；仍保留模拟、执行、同 tick 重新计价等预算测试。

本次最终根构建、111 项模块 JUnit 与 17 项必需 GameTest 通过，私有日志为 `mek-stage-final-validation-v2.log`。测试覆盖实际供电网络分配/总转速过载/内存卡即时撤销/移除动力源、工作盆泵气/智能过滤/开放排放、云盘隔离/部分领取与重复领取、物质球拆分及解包器组件/数量往返、非法所有者/物品/列表拒绝和带卡驱动器实际拆除。测试专用代码不进入生产 JAR。此处往返使用目标格式夹具，不等于原服务器存档已经转换。

当前测试依赖集缺少 Create Factory Logistics，Package Couriers 的 `jar_plane` 配方报缺失物品，完整目标清单阶段必须闭合；非法物品拒绝测试另有预期解析错误。因此不能声称整包日志干净。真实客户端、独立客户端探针、龙堆与多方块长期生产、安瓿与洞穴工厂目标接口仍待验证。自动快照提交 `725f4466` 曾保存中途尚未编译通过的移植，不能用该自动提交作为验证依据；本阶段结果以上述实际检查为准。

## 洞穴工厂与熔液结晶移植

Cave Factory 按 Java 21/NeoForge/Create 6.0.10 迁移；拆出测试 sourceSet，不把空白 GameTest 结构或测试标签打入生产 JAR。公开能力改为 NeoForge 注册，五类 Create 模块宿主使用附件管理生命周期和原有持久化键。删除 mandatory RPM 接口的反射/吞错；重载代数使配方缓存随数据包更新失效，不依赖 RecipeManager 对象变化。

全部六维控制器/流体/基质/残渣/模块 ID 及 16 条原有配方保留。数据目录使用目标 `recipe`、`tags/item`、`tags/fluid`；三种原生 Codec/StreamCodec 保存完整物品/流体组件。稳定化输入省略 amount 时仍读取服务端配置，网络携带已解析的数值。工厂库存读取先验证列表、槽位与物品，拒绝重复槽位/不合法流体/容量越界/归属 UUID/相位；原生方块实体加载器会捕获加载异常，因此全包旧档仍须离线预检，不能用局部抛错当作整个存档安全保证。

封装改用不可变 `ampoule_fluid` 组件，保留自定义名称、其他组件和来源/到期/残渣数据。实际发现可变 FluidStack 不满足物品组件 equals/hashCode 契约，已改为保存 Holder、数量和 DataComponentPatch 的不可变值。实际槽位/流体/模块/归属往返、模拟/部分排出、叠堆拒绝和到期转换均通过。稳定器整批输出可容纳后才申请最终生产额度和消耗流体；真实测试覆盖额度拒绝、满输出、成功生产及重载后新配方生效。

KubeJS 2101 真实启动发现旧 thickTexture/bucketColor API 移除，已按目标纹理/tint API 迁移。旧 Forge sprite_loader 改为 NeoForge SpriteSource；输出到 KubeJS 实际引用的 generated 路径，42 张动画保留所有帧。客户端首次创造页签补测又发现只读集合删除导致真实崩溃，改用 BuildCreativeModeTabContentsEvent.remove；测试主菜单缺动态注册表/标签的问题改为实际进入隔离新世界后验证，未在运行代码添加兼容绕过。

固定目标依赖经下载及官方 SHA-512 校验：Alex 端口 pC1MYhqR 2.0.10、维度端口 OJpP9UtE 1.1.0、Citadel Port mIylVpkN 2.7.6（原调查 XjY0RcQj 是项目 ID）、JEI kmb7KviL 19.71.0.470、Jade eYz2YBGT 15.10.6；专项 KubeJS GDXCpKlp 2101.7.2-build381、Rhino SqkDvOLG 2101.2.8-build91、Better Advanced Tooltips uPFbnD32。Alex JAR 声明 GPL-3.0 而页面 LGPL，记录差异，最终再核对分发许可；未复制第三方源码。端口新增 prismatic_depths 不属于原六维规划，默认入口限制同样拦截，完整脚本/维度工具仍待同步。

31 项 JUnit 与 16 项必需 GameTest 通过，包含 21 金属自然放置/桶/天空遮挡/流量结晶、16 条原始配方实际加载、真实 Create 工作盆模块加产与保存、工厂 13 槽/两个储罐往返及拒绝错误数据。客户端专项检查 21 源流体、42 动画、桶/储罐和统一创造页签；铁流体预览与原公式逐像素一致，0 差异像素。该客户端只加载专项依赖，不等于全部 JEI/Jade/Ponder 功能验证。

专用新建服务器（2 GiB、2 CPU、loopback、平坦主世界）真实加载六个原有洞穴维度，在渊海生成一块区块，用实际动力端口完成两条原配方并保存。正常停服后，第二个独立 JVM 重启核对珍珠 1、重骨 1、海玻璃 2、卤水 500 mB、phase_b 与所有者；`cave-real-server-validation-v3.log` / `cave-real-server-reload-v1.log` 均通过且进程正常退出。准备复制和启动均要求专用目录标记，不能指向生产目录；实际负例确认无标记目录在复制前拒绝且保持为空（cave-directory-guard-check.log）。最终根构建/GameTest 日志 `cave-stage-final-validation-v2.log`，客户端最终日志 `cave-client-crystal-validation-v8.log` 保存于私有实验目录。客户端仍有上游两个缺失字幕 ERROR，不能声称日志无错误。

以上世界全部为新建隔离夹具，尚未使用 NeoForge 转换或打开原服务器存档。剩余 Transfer、Pressure Crystals、Maid AE Crank、JEI Ticks，以及第三方闭包、全脚本/配置、更新器、旧档一次性转换和正式部署继续推进；根 VERSION 和模组版本保持，未创建发行版。

## 定制 Mobile Packages NeoForge 移植

独立 fork 开发分支 `codex/dimensionworks-neoforge-migration` 已提交并推送 `57bf726`，随后修复分部方块并推送 `62db6a8c2a6737eff659cc4f840a97f9e640a754`（完整提交见 GitHub），基于上游固定 1.21.1-0.7.8 `53ec8db0d247718fdadb431531426826560267cc`，逐项核对并迁移原已固定 Forge .15 `4e435c2a5b181e311d681a6f29d8ec0038fc24b6` 的补丁。没有修改原 Forge 工作副本，没有发布 fork 或整包发行版，尚未把开发 JAR 写入正式 manifest。其新建夹具通过不等于全包旧档迁移完成。

本地完整构建、3 项 JUnit 与 19 项必需 GameTest 通过（初次 18 项记录 `cmp-port/native-tests-v17.log`；分部方块修复后最终 `cmp-port/native-tests-v18.log`）；独立实际客户端的新建平坦世界通过原生打包载荷、打包尺寸/第三人称渲染与蜜蜂模型、五次脱离、回收菜单路线和 31 个命名物品同步、超大 NBT 警告（`cmp-port/client-validation-v2.log`）。客户端只加载专项依赖，不代表整个整合包 UI 已验收。测试和日志不打入分发 JAR。

保留全部原翻译键、注册 ID、网络/玩家/无人机 UUID、完整组件货物与回收槽位。原生 ENTITY_DATA 直接恢复快照，解决刷怪蛋替换 UUID 以及落沙等仅 OP 可设置实体数据被跳过的问题；实际 Create 开包恢复 TNT 原 UUID/fuse，载具及乘客完整移除和恢复。UUID 重复拒绝且快照不消耗。NBT 写入前拒绝原生 StringTag modified-UTF 溢出，避免原生写入日志后用空字符串替换导致打包吞数据。

Mek 原生 BlockData 已改为不可变组件，来源标记移到自有 packager_origin，真实保存/掉落/重放置/拆箱及普通纸箱对照通过。原生 BreakEvent/机器权限保持，禁止 barrier 与本包派遣终端；命名箱和嵌套物品数量/长整数通过。无人机目标和实体位置刷新漏更新已修复；完整投递与原港口回程通过。真实负例发现回程访问未加载原港口会强制加载区块，且旧孤儿删除可丢蜜蜂；改为已加载港口查询和保留虚拟蜜蜂，货物及空载回程在 100 tick 后均保持且不加载区块。

原来的跨维度玩家候选搜索恢复并保留网络/所有者权限，但完整跨维度无人机/移动 Sable 子世界/Carry On 场景仍须合包验证。旧 Forge ItemStack、ENTITY_DATA 内嵌实体及 Create 包裹、Mek 纸箱/原标记、无人机 cargo/回收槽位必须经一次性转换，当前运行代码不猜旧格式。当前开发 JAR SHA-256 `eadd0adaf57f37d0aa87168ab9d3e8e08dfa6d370a5e408869174135a7ff5b06`。共享源码构建从固定提交及归档 SHA-256 `af6dade670680565467b552954d8a652ca945b57c40183738c62618c772ec9c9` 在忽略的构建目录生成依赖，不需要中间发行版。本地工作副本与固定源码归档构建的 JAR 逐字节一致；正式整包清单与正式构建流水线仍须接入该固定来源。

CFA 1.6.0 是上游内嵌原依赖，JAR SHA-256 `e545dc2863842ccf473c6ad26e7ca73a37e1bb45dd529f1e5105b263320f1107`，许可声明 All Rights Reserved；未复制或修改依赖源码，正式分发前核对其许可条件。原 MIT 项目与第三方许可不能混为一谈。

## 中转维度、玩家包裹与六洞穴发运移植

Transfer 接入根 NeoForge 工程，Java 21、Create 6.0.10、Curios 9 与原生有方向载荷。中转协议 4、弹力协议 3，客户端界面处理隔离于客户端类；CMP 调用删除必需接口反射/吞错。暮色直接调用固定 4.8.3345 原生 API 与受保护落点 Invoker，传统传送门入口单独拒绝；原 `dw:transfer`、螺旋领地、文件名、返回点和教程键保留。数据目录迁移为 `recipe`/`tags/item`/`tags/block`，Mixin 只绑定当前原生方法，defaultRequire=1。资源声明覆盖目标客户端格式 34 与数据格式 48。

领地与返回点拒绝非法列表/UUID、重复索引/玩家、偏离螺旋网格和非有限位置，不再静默跳过。34 项 JUnit 已通过，明确提供封存快照路径的专用检查使用原生 NBT 读取原服务器三份 SavedData，逐项核对全部领地、升级/完成标志、分配游标、返回位置和教程载荷；没有写入源文件。教程内旧物品格式仍待整包离线组件转换，不宣称全世界已迁移。

原 1.20 区块追踪方法已改变，改用固定 PlotChunkView 与原生 PlayerChunkSender，保留批次确认及实体跟踪。九个领地运行票据和外围 5×5 渲染覆盖保持；JUnit 核对负坐标和视图差分。真实客户端原生钥匙载荷验证未解锁拒绝、解锁进入、完整 25 块接收、边缘移动不卸载、外圈不发送，返回恢复原位置及七个命名钻石/长整数组件并清理返回点（`transfer-port/client-travel-v1.log`）。

原生 Mek 纸箱/AE2 部件使用组件参数，拦截先于消耗/创建；真实源维度打包、封箱物品放置、拆箱拒绝与完整机器数据保留通过。联合测试暴露 Mek 固定数字 35 没有目标 `UPDATE_KNOWN_SHAPE=16`，造成打包一半门后另一半移除；在定制 fork 中使用当前命名标志并增加门/床/高花回归（19 项 fork GameTest）。未改第三方原仓库或发布发行版。

四个独立实际客户端夹具通过：目的地/六洞穴页面和返回；同模型弹力外观、两秒蓄力金色 HUD、实际 18.5625 发射与脱包；真实马达竖直带上/下行、反转、停机重力及客户端/服务端位置；原生中转往返。日志为 `client-dispatch-v1.log`、`client-elastic-v1.log`、`client-vertical-v1.log`、`client-travel-v1.log`，资源限制 2 GiB/2 CPU/30 FPS，均正常保存退出。部分进程仍有开发帐号公钥服务返回 HTML 和退出音频 ERROR，不把这些夹具称为整个整包无错误验证。

KubeJS 弹力脚本迁移原生十步配方字段与具名 `modifyRecipeResult` 事件，通过 `event.exit(完整物品)` 提交结果，避免普通 return 产生空结果与 String 重载歧义。实际 KubeJS GDXCpKlp/Rhino SqkDvOLG/KubeJS Create 1qmcLagN 和 Vintage CF 1255448/8941692 加载；十步真实序列推进、两类铁木弹簧原料、完成胶带与完整组件升级通过。Vintage 原/目标 JAR 均有不存在的 minecraft:blaze_wire 输出，整包数据层改回真实 vintageimprovements:fiery_wire；保留原料标签，其可达性仍待完整材料闭包审计。Vintage JAR 声明 MIT、页面 LGPL2.1，最终分发许可继续核对。

默认 40 项、配方专项 43 项、六洞穴联合专项 52 项必需 GameTest 均覆盖对应前项；最终联合 52 项已通过（`transfer-port/alex-tests-v2.log`），KubeJS 启动/服务端脚本 0 错误和 0 警告、0 failed recipes。六洞穴均实际发运到达安全位置并拆包、清理临时授权与返回点。渊海原/目标生成器一致，但水域没有干燥支撑，新增严格检查后只在水面空气增加九格平台；占位/库存负例保持原状，生成器、水体与旧建筑不改写。联合 RPM 会按机器归属限速，竖直带测试明确配置高限额归属来隔离运输能力，未添加运行时绕过。

此阶段仍只使用隔离新建世界和封存数据的只读检查。剩余 Pressure Crystals、Maid AE Crank、JEI Ticks、完整第三方闭包、全脚本/配置、更新器、一次性转换器与实际旧世界/生产部署继续推进。根 VERSION 保持，未创建任何发行版。

阶段最终根构建 `build :dimensionworks-transfer:runGameTestServer -PalexDispatchCheck -PtransferSnapshot=<封存源路径>` 通过（`transfer-port/stage-final-validation-v1.log`），已接入的 13 个模块共 300 项 JUnit，0 失败/错误/跳过；Transfer 联合 52 项再次通过。生产 `dimensionworks_transfer-0.3.14.jar` 为 207051 字节、94 个 Java 21 类，SHA-256 `cd293f9c8b401ec0a34fcb05efa3bfb64357b56fee47ebfbeb35451103e8a73d`；核对 NeoForge TOML/JSON 且不含测试类、专项结构或客户端夹具。另两个尚未接入工程的历史测试报告不计入上述数量。客户端运行目录新增忽略规则，未提交运行时数据。

## 压力结晶原生移植

Pressure Crystals 0.1.2 接入根工程，原四个注册 ID、压力边界、21 金属与 600 mB/3 结晶行为保持；Vintage 固定 CF 1255448/8941692，工作盆 Mixin 使用原生注册表参数，流体转换与剩余流体保持 DataComponentPatch，金属特质转入 minecraft:custom_data。存在的计时列表严格解码两个槽，错误拒绝且不部分覆盖当前状态；新建工作盆无计时字段时正常开始，旧格式转换仍待离线迁移器。

4 项 JUnit、19 项实际 GameTest 已通过（`pressure-port/native-tests-v2.log`），包含真实压缩机/大齿轮传动、21×4 档产出、敞口/盖子、600 tick 连续暴露、两槽补料换液、满仓不扣料及恢复、599/600 mB、第二槽独立结算、当前工作盆保存重载及流体组件往返。实际注册时保留统一创造页签策略，没有新页签；完整客户端界面与实际旧档尚未验证。

柴油模组目标 UoPH8lO1 1.21.1-1.3.15 经官方 SHA-512 校验，JAR SHA-256 `56ef1d574278fc311f1ffa223dbd613077b899354a18d01ae8dca2578a4e2990`，声明 MIT，NeoForge >=21.1.174、Create [6.0.7,6.1.0)、可选 Struts >=1.3.0。其未装 Struts 时的掉落表无条件引用未注册物品，整包自有数据覆盖保持原掉落并增加 mod_loaded 条件；专项也装入已有固定 KubeJS Create 以供 Vintage 原生脚本使用。修正后服务器日志 0 ERROR、KubeJS 启动/服务端 0 错误/警告，19 项全部通过并保存退出。可选 Sable 的上游 Mixin 未发现警告仍存在，不称完整依赖闭包已验证。

生产 JAR 为 20948 字节、8 个 Java 21 类，SHA-256 `69cf9485d1524c592d8d82300e7b1d9d8cd399c24ffa955a9265b30975371e51`；JSON/TOML、测试结构与类排除、git diff --check 均检查通过。根 VERSION 保持 0.3.52，不发布发行版；后续继续 Maid AE Crank、JEI Ticks、全包闭包/脚本和真实旧档迁移。

## 女仆 AE2 曲柄原生移植

Maid AE Crank 0.1.0 接入根工程，固定 AE2 7Ac6mXbf 19.2.20、Touhou Little Maid tXG1TkGx 1.5.3-neoforge+mc1.21.1（官方 SHA-512 已核对，JAR SHA-256 `f6db04195820c8508704277ea76d63723804ff236a7b780369ba59ebe5cd9c27`，许可 MIT / CC BY-NC-SA 4.0；代码与模型资源分别遵守许可）。直接调用当前扩展/供能接口，保留原任务 ID、不使用假玩家；搜索先确认区块已加载，图标返回副本。

本地构建与 8 项实际 GameTest 通过（`maid-port/native-tests-v2.log`），包括原生任务发现、女仆任务/UUID/七个钻石保存重载、近处供能160 AE、满能量不摇动不损毁、远处寻路、最近可用目标、移除目标清理记忆、未加载区域不新增区块。新增实际实体由原生 Brain 自主移动并供能的测试，不手动执行任务 tick；日志 0 ERROR，正常保存退出。客户端任务页面、多人长期运行及真实旧档转换继续待验收。

生产 JAR 9057 字节、4 个 Java 21 类，SHA-256 `e730b90c77449ebc9ca54964cf6593d5a748baefc823d048b969827cbc7ba926`；JSON/TOML、测试类/结构排除与 git diff --check 检查通过。根 VERSION 保持 0.3.52，没有新发行版。后续继续 JEI Ticks 和完整整包/旧档迁移。

## JEI 耗时与燃料提示原生移植

JEI Ticks 0.1.1 接入根工程，全部 16 个自研模组已编译。固定 JEI kmb7KviL 19.71.0.470 / MezzConfig sPQlt1iR 0.6.11 / Jade eYz2YBGT 15.10.6，Java 21、客户端 Mixin。原生 JEI widget 移到 lambda 绘制，旧三处重定向改为整个 drawRecipe 的单一 try/finally 上下文；解包 RecipeHolder，风扇直接读当前 Create 服务端配置，Mek 使用原生分类 ID，附魔工业直接读原生经验燃料数据映射。保持注册 ID / 燃料资源接口 / 服务端排除规则。

3 项 JUnit、实际新建世界客户端专项通过（jei-port/client-validation-v7.log，22 秒，正常保存退出，0 ERROR），验证两条 43t/271t 分类与 widget / positioned drawable 上下文隔离，以及原生烧炼 50t、Create 粉碎 250t、Mek 富集 200t 实际绘制。原始截图放大确认 200t 标签正常；缩小预览的误判没有引入额外补丁。NeoForge GatherComponents 注册事件、原生提示组件工厂、269 项燃料的小屏翻页布局、Jade 注册提供者及其元素实际绘制通过；Jade BlockAccessor 为测试构造，不冒充完整世界射线悬停。

实际选装 Diesel UoPH8lO1 / Liquid Fuel rLn2bDVE / CEI qUpvzZ83 / Dragons Plus b0u9vk8C / DE 84xwXpNh 及原生库。附魔工业 8 项、龙堆 3 项、发电机 265 项、高级传送器 1 项燃料 API 返回值通过；CEI 六张引用未注册可选物品的掉落表在整包数据层补 item_exists 条件，保留原始掉落。首次缺 MezzConfig 的真实启动失败已补必需依赖；没有吞掉错误。上游可选数据映射和未装 Sable 警告仍待全闭包核对；目标柴油喷射器真实扣量、45 脚本中的自定义燃料和完整交互仍须后续核实。

最终 root build -PtransferSnapshot=<封存源> 通过（jei-port/root-final-validation-v1.log），16 模块共 307 JUnit，0 失败/错误/跳过；未因无新变化重跑其余服务器专项。生产 JAR 51192 字节、25 个 Java 21 类，SHA-256 d7054cbeb6ab0c6946efdec4d880f024950027a47a2ccc3090315844d64ab5a8，无探针/测试数据。NeoForge TOML/资源 JSON 与 check_jei_details.py 检查通过。根 VERSION 0.3.52 不变，没有中间发行版；完整第三方闭包、全脚本/配置/更新器、一次性转换及真实旧世界仍继续推进。

## 下一阶段准入

私有快照验证及全档读取审计已完成；随后确定缺失注册 ID 的处理、锁定完整目标依赖闭包，移植其余模块和脚本，再实现一次性 NBT/组件转换器。完整客户端/服务端冷启动、重启、库存语义对账、玩家身份及真实生产链验收通过前，禁止给玩家开放跨版本更新。


## TrueUUID 原生移植与玩家身份保护

独立 fork 从原服固定 `v1.0.10` 提交 `7102fb449bfd5fcbfb383cc59d99ea01fc8294be` 移植，已推送 `szx-jn/TrueUUID-DimensionWorks` 的 `codex/dimensionworks-neoforge-migration` 分支，固定提交 `b2fa74999f4df0e492f5ef3fb018dca3b7dbbd89`。源码归档 SHA-256 为 `e10d9bed03fc066141a3adc225d2c4a51905cfea7f27f9e1c3dd8d8bd67f719b`。本地验证 JAR `trueuuid-1.0.10-neoforge1.21.1.jar` 为 83,501 字节、37 个 Java 21 类，SHA-256 为 `06919ff0da0ebb53b6a507531b128ad9a12f965b5f9e50d235ea8650ac2da423`。固定源码归档在独立目录重建后与工作副本 JAR 逐字节一致，构建记录 trueuuid-port/fixed-source-build-v1.log。固定源码构建仍须接入整包流水线；原服 Forge 清单尚未切换，不把这一步描述为整包已升级。源码 LGPL-3.0，与整合包原创 MIT 文件分开。

LOGIN 认证改为原生查询/响应 payload，专用 decoder 保留 nullable 标志与全部字段；畸形响应拒绝解码，其他查询保留原版处理。服务端等待认证时暂停原版 VERIFYING 推进，独立 hasJoined 成功后设置 authenticatedProfile 再进入原版配置阶段。客户端使用原生 User UUID 调用 session service，不传输令牌。配置路径、命令/API、Mojang/Yggdrasil 来源及已有认证策略保留。

原冷备存在同一玩家多次继承的历史记录，原实现未标记完成，可能让旧离线副本覆盖后续进度。移植改为已有验证 playerdata 优先、完整复制后记录 complete；pending 操作阻止继续登录，必须依据备份恢复后处理。身份注册表完整解析后才启用，解析或保存失败明确报错；写入使用同目录原子替换。此次改动保护账号档案继承，不能替代世界/物品组件/机器的一次性跨版本转换。

实际检查：4 项 JUnit、13 项原生 GameTest 全通过，包括原生 packet codec、等待状态、已绑定身份拒绝离线回落、本地模拟 Yggdrasil HTTP 校验、既有档案保护、一次性继承与复制中断。隔离客户端检查通过原生登录响应、缺少令牌标志、无关查询和单人世界 UUID，随后正常保存三维度退出；这是开发会话，真实 Mojang/MSL 账号与完整整包连接仍待验证。最后用原服配置与身份表副本运行全部 GameTest，原有 **9 条现役身份绑定记录**语义保持一致；此前提到的 **94 个历史备份文件**是不同统计对象。

证据日志：私有实验目录 `trueuuid-port/native-reference-v1.log` 与 `native-client-v1.log`，原始身份表仍只读。fork 工作流已改为手动验证且删除 Release 步骤，没有触发该工作流，也没有发布新发行版。生产 JAR 已检查不包含测试类或旧 Forge TOML。

## 当前光影与候选整包验证

按当前 `manifest/shaderpacks.json` 的四组来源，原 Complementary Unbound r5.9.3、Photon v1.3b、Sildur Vibrant Extreme VL 与 Chocapic13 V6 整包均已下载，并核对原固定文件的大小及官方校验值。Chocapic 整包展开为 Extreme、High、Lite、Low、Medium、Ultra 六份原始预设 ZIP。共九份独立光影文件均完成压缩包完整性检查，**还未完成实际渲染，不能宣称已兼容**。

用户已明确允许替换光影加载器，候选使用原生 Iris 与 Sodium。实际依赖元数据证实 Iris 1.8.12 / Sodium 0.6.13 与当前 Supplementaries 不兼容；当前候选 Iris 1.8.14-beta.1 / Sodium 0.8.13 通过依赖检查，同时保留 Sodium Extras 并选择与其 Options API 相容的 Reese 版本。最终必须逐个加载上述九份光影验证实际画面，若需要适配升级版，明确记录来源与替换结果，不能仅凭依赖范围承诺保留。

私有候选集合下载完成 150 个 Modrinth、15 个 CurseForge 固定资产，并检查实际 JAR/JarJar 依赖。此前候选 182 个顶层 JAR、73 个内嵌依赖、216 个 modId 的静态闭合检查为 0 错误，仍有一个相同版本内嵌库字节差异警告。候选服务端在隔离新世界实际 Done 并导出注册表，停止后全部维度保存，但日志仍含大量第三方数据错误，且尚未加载完整脚本与旧世界。

真实目标注册表对照原档发现 ExtendedAE 设备、葡萄作物、矿工乐事等 ID 差异，必须逐项核对其状态/NBT并转换。审计中的 minecraft:experience_orb 来自实体 NBT，不能当成丢失的物品栈；原版 grass 应交由 DFU 改为 short_grass，禁止直接删除。目标注册表目前还未加载全部 KubeJS 注册项，相关缺口不能误判为可放弃内容。


## 百味厨房自动化原生 fork

原固定 Forge 资产对应提交 87cf3ce0eeedb3afa80efd828660f17c24674a34，其主工程已有 NeoForge 1.21.1 源码，旧 Forge 资产由独立二进制补丁子工程生成。本次在独立 fork 删除旧平台补丁工程，保留原生自动化行为和两处教程食谱修复，固定 Java 21、NeoForge 21.1.267、Create UjX6dr61 与 Cookery g8w0b2ly。已推送 szx-jn/KaleidoscopeCookery-Automation 固定提交 9053486f76a7ffb05a44a6c6be54fbb5e7c9a7e7，没有创建发行版。

原生容器 API 与库存语义逐项核对：食谱引用保存 ID、重载后从当前管理器解析；蒸笼只操作实际可见槽位；锅和汤锅拒绝无容量或不适配容器的插入；抽取按实际消耗份数返回，避免多份成品只消耗一份以及覆盖剩余烤肉。物流移交通过模拟与实际抽取保持数量，食谱包裹按实际成品份数请求容器。原注册 ID、生成标签和四个教程保留，没有为旧二进制格式增加运行时猜测。

23 项必需 GameTest 通过（automation-port/native-tests-v8.log），覆盖真实蒸笼 1200 tick 烹饪、汤锅七份实际取出、普通锅整批容量、桶返还、半蒸笼隐藏槽位、组件/库存/进度重载、失效食谱和食谱订单容器数量。实际客户端四个原教程全部注册、绘制并执行指令，随后正常保存三维度退出（native-client-v2.log）。上游 Cookery 在这一小依赖夹具有可选兼容及数据错误，完整合包仍待处理；此记录不代表旧世界或所有料理内容已验收。

验证 JAR kaleidoscopecookery_automation-1.21.1-1.0.1-dimensionworks.2.jar 为 107050 字节、49 个 Java 21 类，SHA-256 856f4368d5b23413fd7b96303ffa9ddad5bbe746384b94d8fd9edfa68ee18235，包含原生成资源与 BSD-3-Clause LICENSE，不包含测试类。固定源码 ZIP 为 156042 字节，SHA-256 b79d03118286cf059736aa87249498497650096b7a0c2c08a422b23448079281，独立重建 JAR 与工作副本逐字节一致（fixed-source-build-v1.log）。整包清单/源码构建流水线尚待接入。原 Forge 工作副本及服务器冷备未修改。


## Mekanism Delight 原内容移植与构建入口

从原作者公开源码 757f569d5097105082bcfae8de45b5417f51a039 建立独立 szx-jn/MekanismDelight fork，已推送 ccb1d24622c530b4b19dc5e20421367020457121。原服务器 JAR 的 42 个配方文件逐项对照；保留原整包实际启用的 18 个物品、拼盘方块与 15 条配方。基础果冻原来经拼盘取得，上游没有独立果冻配方，不把不存在的文件当作迁移丢失。原整包未安装 Mekanism Tools/Evolved Mekanism，不编译其刀具材料适配，原通用合金附加代码及可选资源保留。

原生化学 codec、15 条实际配方及标签、完整物品组件、分级奶昔及取消药效移除事件、原辐射剂量、三种拼盘份数检查通过，6 项必需 GameTest 均通过（mekdelight-port/native-tests-v2.log），该日志 0 ERROR；此前切菜配方 codec 和进度条件错误已修复。核对旧 Mek 实际源码，核合成按完整工序消耗化学品、注射室持续消耗；原 10000 化学量/24000 tick 等配置保持。删除旧平台 datagen，数据文件使用原生单数路径。实际客户端与完整整包仍待验收。

JAR mekanismdelight-1.21.1-1.1.3-dimensionworks.1.jar 为 120370 字节、23 个 Java 21 类，SHA-256 849527b68c780d2ef0b37586853f93947733de70c5fb2596ad9196a78e76c745，保留原 MIT LICENSE，不含测试类。源码归档 1586107 字节，SHA-256 ad593a116e5e450f18df541b261127c09595022d8f584676bdee52e72b78bc79，没有中间 Release。

整包新建固定源码构建入口，客户端/服务端/CI 接入同一个校验构建 helper；32 项相关 Python 检查通过。预检钩子此前误把 py_compile 当作执行打包，并使用旧系统 Python，已限制到实际脚本或模块执行并使用工具 PATH 的 Python。完整目标清单、NeoForge 安装器和更新器切换仍继续推进。

## 光影专项停止点

用户要求停止继续跑光影测试，后续不再执行光影专项。已取得的实际客户端记录：原 Chocapic13 V6 六份预设、Unbound r5.9.3 和 Sildur Extreme-VL 在候选 Iris/Sodium 下实际启用并输出非空场景；原 Photon v1.3b 与官方 v1.4 在低负载配置并关闭 SH_SKYLIGHT/CLOUD_SHADOWS 后成功渲染（photon-native-client-v6.log）。Mac 默认 Photon 有 16 采样器上限及 compute shader 限制；仅关闭三个选项的后续精简配置仍失败，不能用该精简配置替代已通过配置。实际图片保存在私有 shader-port 下。整包联动/特殊维度未验收，不能把专项结果描述为整个迁移已完成。

## NeoForge 安装器、启动脚本与更新依赖

服务端安装器改为官方固定 NeoForge 21.1.267；Java 检测要求 21 以上，显式指定不存在或 Java 17 的路径直接拒绝。Linux/Windows 启动参数转为 net/neoforged/neoforge，保留实例路径和 TrueUUID/authlib-injector 设置。客户端 MRPack 依赖改为 neoforge；两种打包入口在下载前核对清单必须为 1.21.1/neoforge，防止迁移中的旧清单生成混合包。发行工作流工具链改为 Java 21/Gradle 9.2.1，首步禁止本迁移分支发行的规则保留，未触发工作流。

官方安装器在隔离目录实际完成安装（16.61 秒），两个平台参数文件与服务端树检查通过。生成 start.sh 使用 1 GiB/2 CPU 在 loopback 新世界实际 Done (0.305s)，总进程 9.57 秒，stdin stop 正常保存三维度并以 0 退出（pack-closure/installer-server-v1.log）。测试超平坦配置缺 generator-settings 有一条 No key layers 数据错误，不能称为无错完整整包启动；此夹具无原存档、无生产模组。

25 项相关 Python 检查通过（native-packaging-tests-v2.log），覆盖旧加载器清单拒绝、Java 17/错误路径拒绝、Windows 参数缺失和含空格目录的实际启动命令/退出码。跨平台 Go 更新器测试 go test ./... 通过（7.635 秒），受限下载差量 MRPack 保留 Minecraft 1.21.1 / NeoForge 依赖。YAML 和发行说明四类别样例换行检查通过；未执行任何发布命令。VERSION 仍为 0.3.52，目标清单及存档迁移仍继续推进。


## 直接升级优先：用户新增执行约束

用户要求找不到现成升级版、需要自行实现的内容先汇报，不得直接开始新增移植。执行方式改为直接升级优先，具体候选统计、已发生工作和待确认取舍见 docs/superpowers/plans/2026-11-20-neoforge-direct-upgrade-decisions.md。原 182 项中 160 项有现成目标候选；今日再次查询原项目，Caves Delight、原 MekanismDelight、Kaleidoscope Sauce、Wither Storm 的 1.21.1 NeoForge 列表仍均为空。前三项自行实现方案暂缓接入，Wither Storm 按用户先前明确允许舍弃；没有创建发行版。

新增约束到达前，酱料独立 fork 已推送 d5401283c39e6c292b397b579cd18188a0d154a4。保留原 0.1.2 八种酱料、三种作物、15 物品、12 方块、四种药效和 32 配方；按原 MIT 发布 JAR 经 Vineflower 恢复，非作者原始源码。9 项 GameTest 通过（pack-closure/sauce-native-tests-v4.log），包括真实蘸酱、八次搅拌与中途保存、完整组件、破坏掉落、草帽种子和连续三份食用返还三个桶。普通原生客户端 8 种酱料/32 个覆盖食物模型实际绘制通过（sauce-native-client-v2.log），无光影加载器或光影专项，未进行截图视觉验收；第三方小依赖夹具数据警告仍在。

最终酱料 JAR 213009 字节，SHA-256 65cdb1ba75b89614e0259b5f9e53809435b1352d7f7f4a6d0e754a5c2cea7dd1；固定源码 ZIP 280988 字节，SHA-256 04e21f1a818fdd1c9f5155c5e6148acb5587779142c5f30c88f598b4f1b12abf。资源编码/换行整理后 build、JSON/TOML、测试类排除与 git diff --check 通过；固定源码 helper 独立重建产物逐字节一致。未添加到最终清单，是否采用自做 fork 待用户确认。

洞穴料理本地移植已保留 65 个原物品与五种砖、97 条原配方。8 项 GameTest 通过（caves-delight-native-tests-v2.log），覆盖实际配方输入/输出、物品组件、原辐射条件治疗而不删除辐射、连续三碗食用返还、装饰砖掉落、真实投蛋/实体 UUID 保存和原辐射云、11 种原肉食作为原生狼食物。第一次启动发现原空事件订阅类被 NeoForge 拒绝，移除该无监听注解后通过；未进行客户端专项。此工程尚未提交或推送，当前停止继续公开化/接入，按用户最新要求先汇报。原项目 MIT 与 JAR All Rights Reserved 元数据冲突一并记录，不能把本地验证当作公开分发许可已无争议。

ExtendedAE 官方目标源码与原版本 tag 已比对，原 ex_charger/ex_drive/ex_inscriber/circuit_cutter/silicon_block 等在新 extendedae 命名空间仍注册，无需自行移植整个模组。只读定位原世界实际无限元件，record 为 ae2:i / minecraft:cobblestone，原记录 1 个；应映射到现成 extendedae:infinity_cobblestone_cell，不按原默认水行为猜测。完整库存及 AE2 Key 转换仍待实现与运行验收。


## 原生整包脚本与内存驱动器合成专项

按直接升级优先继续适配现成 KubeJS 2101 / Create 6 / Mekanism 10.7，不新增第三方源码移植。当前测试服使用隔离新世界、127.0.0.1:25573、Java 21、2 GiB 堆和 2 CPU；未打开封存旧世界或修改生产原档。

修复实际日志中的三处故障：Create 概率产物由失效的 ItemStack.withChance 改为 CreateItem.of；齿轮之心第六碎片使用实际注册的 mekanism:basic_universal_cable；暮色拆解台区块检查使用原生 KubeJS 方块 id，避免 BlockState.is 重载歧义及不暴露的 equals 方法导致区块晋升崩溃。共享加工 API 保留配方输入、数量和独立概率，采用服务端共享顶层作用域。

内存驱动器升级改为具名 ServerEvents.modifyRecipeResult，读取原生 CraftingInput 第五格驱动器，复制完整 minecraft:block_entity_data；使用实际事件字段 item。回调逐级绑定，避开 Rhino 循环内块级变量重复声明。强力胶保留其他组件，已存在 unbreakable 时不再触发重复库存更新。进度图标转为 id，女仆存储触发器改为目标注册表的 maid_event / altar_craft，保留当前实际物品与祭坛配方 ID。

最终 full-kubejs-server-v9.log：13/13 启动脚本、27/27 服务端脚本加载，均为 0 errors / 0 warnings；服务端数包含 25 个分发脚本和 2 个私有验证脚本。新增 230、移除 381、修改 6 条配方，失败 0 条；Done (0.679s)，40 tick 后正常保存全部维度。进度解析不再出现前轮女仆存储的两条错误。Gradle 本轮实际以退出码 0 完成；该结果仍不能证明完整整包无错误。

私有专项使用真实 AE2 驱动器库存填入带名称/自定义数据的前代内存卡，保存完整方块实体数据，另写入 long 9000000000000000。实际 3×3 CraftingInput 匹配和 recipe.assemble 对 DDR2、DDR3、DDR4、DDR5 四个升级逐项通过：产物 ID/数量正确，方块实体数据逐字相同，输入数据不变。强力胶首次保护成功、再次保护不重复更新、名称与保护组件均保留。上述是实际原生运行检查，不称为 JUnit 或 GameTest；验证脚本仅位于 migration-lab，未随包提交。

运行配方表共 15,947 条；66 个静态整包配方资源中 63 个在运行表中，另外 3 个被自身条件关闭：Cookery 驴肉存在性条件、ComputerCraft 模组条件、Tide/Nether Depths Upgrade 模组条件。需要继续核对原包这些可选内容的保留情况，不把条件关闭当作已保留其玩法。

36 项相关 Python/Node 配方及契约检查通过（kubejs-python-tests-v2.log）；全部 KubeJS 数据 JSON、启动/服务端 JavaScript 语法和 git diff --check 通过。文档已同步加工 API、原生配方目录和驱动器升级组件。失败的 v4/v6/v7 日志保留，未隐藏错误后报成功。

整包仍未验收：现成第三方资产仍有掉落表、配方、标签、数据映射、Curios 槽位及客户端类反射错误；本轮仅验证上述整包自有脚本与指定合成。目标清单、所有旧档及真实认证/生产升级继续待完成。三个自做料理/酱料方案继续待用户决定，VERSION 为 0.3.52，未创建 Release、未跑光影专项。

## 现成升级候选与整包饮料配方适配

重新核对八项现成候选的 1.21.1 / NeoForge 发布元数据，七项仍为既有固定版本；Dimensions Wine 作者今日发布 1.5.10，候选改为 Modrinth `9YaUuGdr`。新官方 JAR 为 1,133,298 字节，SHA-256 a422d42c42ee077a0852869c34172ade7d91342b48e35204bff8f07a857377e0；文件大小与官方 SHA-512 校验通过。候选依赖检查为 184 个顶层 JAR、73 个内嵌依赖、218 个 modId，0 错误，仍有此前相同版本内嵌库字节差异警告。仅替换私有测试夹具资产和候选证据，原 Forge 清单尚未改为目标清单。

新增整包脚本 cocktail_create_recipes.js，在 after_mods 阶段覆盖 Cocktails Delight 1.1.12 的 24 对 Create 注液/排液配方，修正原生流体和产物字段。保留全部 48 个原 ID、250 mB 数量、Create 条件及材料：Kraken Rum、Nut Brown Ale、Rum 保留啤酒杯，其余 21 种保留玻璃瓶。与固定 JAR 的原配方逐项比较材料、数量、容器和条件通过；不修改第三方 JAR 或源码。首次尝试的运行时文件读取被 KubeJS 类过滤器拒绝，已改用直接生成所需整包数据，没有放开类过滤或增加绕过。

对本次修改的 48 条整包配方使用实际原生 RecipeManager 检查：全部正确解码，注液输入容器匹配、250 mB 接受且 249 mB 拒绝；排液返还对应容器及同种 250 mB 流体。full-kubejs-server-v11.log 与 v12.log 均通过。v12 的 13/13 启动脚本、30/30 服务端脚本加载为 0 errors / 0 warnings，其中 26 个分发脚本、4 个私有探针；Done (0.654s)，正常停服保存全部维度，Gradle 退出码 0。运行配方表为 15,995 条，原先 83 个不同失败配方 ID 减为 35 个；仍有第三方可选兼容、掉落、标签等日志错误，不称为整包无错。39 项相关 Python/Node 检查通过，日志 kubejs-python-tests-v3.log。

## 用户收窄第三方测试范围

用户明确要求未由我们修改、从 Mod 网站选择的现成模组不再测试其专项功能。后续只核对版本、加载器、必需依赖和固定资产；测试集中于自研/定制内容、整包改动及保全旧档所需的整包启动、身份/库存/机器存储对账、保存和重启。上述饮料检查针对我们改写的整包配方，不扩展测试原模组玩法。

该指令到达前，v12 曾检查官方 Dimensions Wine 两种玩偶的破坏掉落，两项通过；这不代表整个模组已经验收。指令到达后已移除私有 zz_dimwine_drop_probe.js，不继续其功能专项，不再跑光影专项。当前夹具没有运行中的游戏进程。根 VERSION 仍为 0.3.52，未创建 Release，封存原档未打开或改写。

## 四项内容确定舍弃与唯一服务器旧档范围

用户明确决定舍弃 Caves Delight、原 MekanismDelight、Kaleidoscope Sauce 和凋零风暴，并确认当前服务器是唯一旧存档。迁移和对账仅面向已封存的真实 0.3.52 Forge 服务器世界及其中玩家/身份/机器/存储数据，不扩展其他单人旧档或通用历史版本支持。先前完成的三项自做移植不接入整包，保留历史材料；没有继续发布或新增替代模组。

manifest/mods.json 已移除这四项，从 182 项变为 178 项；其余平台字段仍是迁移中的原 Forge 基线，不能生成混合平台整包。清理凋零风暴客户端/公共配置、Boss Bar 专属段、两个 JEI 分类与洞穴料理排序项、整个凋零风暴 FTB 任务章；核对该章全部任务 ID，没有被其他任务或语言文件引用。移除 caves_delight、mekanismdelight、witherstormmod 汉化文件及 Sauce 可选命名空间。KubeJS 当前没有这四项引用，保留原版凋零玩法和其他任务。候选矩阵、完整规划、执行规则与舍弃决定同步更新；原 182 项调查及其 SHA 仍作历史证据，不冒充当前 178 项清单。

汉化模块 clean / jar 重建通过（localization-retirement-build-v2.log，4 秒），新 JAR 为 282,367 字节，SHA-256 66f4e8f706d80a8844c23004c03095642e07a500a0cc75526e9acd6a4a5ba541，压缩包内四个舍弃命名空间均不存在。JSON/TOML、178 项清单及分发目录引用、文档本地链接、git diff --check 检查通过。没有测试现成第三方模组功能，没有启动游戏测试服。

新增只读 preflight_registries.py，复用既有完整源审计及目标运行注册表，按方块/物品/实体/维度分别比较。旧报告中 467 个带 Count 的经验球实体根记录单独记账，嵌套 Item 仍保留；没有按任意注册表同名或统一字符串替换判断兼容。原档中的未知记录继续阻止预检通过，不能默默删去。原始 Count 合计仍区分于物流 Amount 和 AE2/Mek 数量；报告保持私有权限、两份输入 SHA-256、拒绝覆盖。

实际 source-registry-preflight-v1.json 输出：8 个缺失方块 ID、12 个缺失物品 ID、0 个缺失实体类型、1 个缺失维度定义、0 个未定类型 Count 记录。预检按契约以 1 退出，表示这些差异尚需转换，**不是旧档已经通过验收**。8 个方块包括原版 grass（4,823,571 个，交给原版 DFU 改名）、葡萄、矿工乐事及 ExtendedAE 设备；12 个物品还包含唯一无限元件；凋零风暴 bowels 定义需定点清理。四个舍弃模组在该唯一服务器审计中实际方块/实体/物品栈均为零。15 项迁移工具测试通过，含新预检 7 项；未重复全世界扫描或打开封存世界，实际转换、保存/重启对账仍待完成。

## 目标清单正式切换与必需依赖闭合

manifest/mods.json 已切换为 Minecraft 1.21.1 / NeoForge：共 183 项固定来源，150 Modrinth、15 CurseForge、15 本仓库自研、3 github-source 原包定制 fork。现成候选全部使用既有校验资产；Iris 替换 Oculus、Sodium 替换 Embeddium，不引用 Forge 渲染器。新增 6 项对应真实依赖链：owo（Aether 侧运行依赖）、MezzConfig（JEI）、Lodestone（矿工乐事）、Sodium / Sodium Options API（保留原 Sodium Extras）、Reese（Options API 的必需依赖）。只做目标版本、加载器、文件 SHA 与依赖元数据检查，没有第三方功能专项。

首次草案没有加入 Reese，实际 Options API JAR 的 mandatory 依赖检查失败；随后补齐同一已取得的固定候选，native-manifest-closure-audit-v2.log 为 **183 个顶层 JAR、73 个内嵌依赖、217 个 modId，0 错误、1 条既有 l2damagetracker 相同版本字节差异警告**。草案不包含额外报警工程；原包 15 个自研 JAR 版本与各 build.gradle 相符。三个原有 fork 使用已推送固定提交、源码 SHA 与产物 SHA，不使用移动分支、Actions 产物或新 Release。root README 平台及直接升级报告同步更新。主清单检查、JSON、唯一 ID 与 git diff --check 通过；固定源码、预检钩子及服务端打包的 20 项既有 Python 检查通过（7 / 9 / 4 项），见 native-manifest-*-tests.log。

该切换不等于完整交付：其余配置/汉化引用、完整打包、唯一旧世界的一次性数据转换、真实认证、保存和重启对账仍待完成。生产原档保持封存状态，未打开或升级。根 VERSION 仍为 0.3.52，未触发发行工作流、未创建 Release。

## 唯一旧档的实体附件转换规则

只读 native NBT 探针扫描封存世界 319,192 个 `.dat` / 区域文档，发现 23 种旧能力和 38 种 ForgeData 顶层字段；包括被打包物品携带的嵌套实体，不能只转换玩家文件。使用 `RandomAccessFile("r")` 读取区域，未使用会以读写方式打开文件的原生 RegionFile。`source-cap-shapes-v1.json` 为私有报告，不含在仓库中。

在隔离空世界导出固定 NeoForge 目标的持久化附件、组件编码及目标方块状态属性，不操作第三方玩法。`native-schema-export-v1.log` 正常启动、导出 14 个默认持久化附件 / 218 项组件编码描述、40 tick 后保存停服；临时导出 Mod 及旧私有料理探针已移出测试夹具。Mekanism 默认辐射因等于基线而不写盘，已另核对其 Double codec；暮色的投掷与巨镐控制附件没有序列化器。

新增 `entity_data.py`：离线映射 NeoForgeData、原生附件及实际数值/字段，保留 Curios 库存、UUID、持久化数据及所有者字段；Aether 四种饰品槽位分别映射，避免把原生槽位与通用饰品槽位合并丢失。舍弃能力和新版不持久化的临时字段返回台账，未知能力或字段冲突阻止转换。24 项迁移工具测试通过（其中新增 9 项），私有实际能力样本生成的 18 种目标附件 ID 全部存在。此处只证明转换规则、样本结构和注册 ID；完整旧档原生解码、库存/机器对账及保存重启尚未完成。

进一步核对负数 Count：共有 71 条，69 条来自背包 SavedData，2 条来自 Sophisticated Storage 方块库存，实际数据另有 Int `realCount`（例如石英 448、赛特斯石英粉 3067）。目标 Sophisticated 库存使用自身的 oversized ItemStack codec，数量应转入 Int `count`，不能把旧 byte 当作库存量或按无符号数猜测。箭袋 Parent 与背包/机器组件规则继续实现中，封存原档未改写，VERSION 仍为 0.3.52，未发布。

## 原版 DFU 桥接与背包/箭袋数据原生读写

新增单进程 `VanillaDfu` 桥接，使用固定 Java 21 / Native Minecraft 类路径执行 3465 → 3955 原版 DFU。实际桥接检查通过：grass 物品及方块状态转为 short_grass，槽位/自定义数据保留，不支持的类型阻止转换；类路径版本检查、二进制 NBT 请求/响应及正常退出均完成。未知模组物品内嵌旧栈仍须递归处理，原版 DFU 不能替代模组组件转换。

只读补充扫描同一 319,192 个文档，得到 217 类带旧 tag 的实际物品、AE2 的 item / fluid 键结构及 46 类 Mekanism 设备数据；报告 `source-item-shapes-v1.json` 保持私有。确认固定新版 Sophisticated 已提供旧背包/升级组件迁移接口，本仓库复用这些作者接口，避免重复维护字段和枚举映射；包括颜色的 alpha 转换及旧过滤器/流体/唱片库存。箭袋和数量判断保留必要的离线规则，新增 4 项测试，迁移工具共 28 项通过。

`NativeComponentBatch` 在隔离空世界对 91 条转换样本严格解码/编码，其中 19 类本服实际使用的 Sophisticated 标签样本、1 个实际箭袋能力样本，以及全部 71 条负数 Count 记录。`native-component-batch-v1.log` 为 Done (0.870s)、MIGRATION_COMPONENT_BATCH_DONE rows=91、40 tick 正常保存停服、Gradle 成功；临时 JAR 已移出夹具。私有 `component-roundtrip-v1.json` 核对：71 条实际库存合计 **80,432 个物品**，各 ID / Int 数量一致；箭袋转换前后目标结构完全一致；36 项背包 UUID、容量、颜色、战利品定义/比例值保留，所选 Sophisticated 样本没有遗留旧自定义字段。输入/输出 SHA 均记录。其余第三方可选兼容与网络日志保留，不称为整包无错，也未运行第三方玩法专项或光影专项。

此处仍是转换样本与数量验证，**不代表 50 个背包存储记录、所有玩家或整个世界已经迁移**。AE2 键及储量、Mekanism 机器/打包数据、实体属性新格式、整档生成及保存重启对账继续待完成；生产封存原档未改写，VERSION 仍为 0.3.52，未发布。

## AE2 原生键类型及储量规则

新增 `ae_data.py`，按本服旧结构转换 item / fluid 键、嵌套物品元数据及元件 Long 储量，检查旧 `keys` / `amts` / `ic` 长度、总量、字段、类型和组件冲突。调用方仍须完成键所携物品的完整组件转换。新增 5 项工具测试，合计 33 项通过（ae-data-tests-v2.log）。

首次原生读写 `native-ae-component-batch-v1.log` 发现新版类型字段是 `#t`，旧 `#c` 会触发 AE2 缺失内容回退：流体键被写为物品类型及 `ae2:missing_content_error`。这批结果未通过对账，输入/输出保留在私有诊断目录。修正类型字段并为 NativeComponentBatch 增加递归占位错误拒绝；`native-ae-component-batch-v2.log` 为 Done (0.696s)、rows=2、40 tick 正常保存停服、Gradle 成功。两条**合成 codec 样本**的目标输入/输出结构完全一致：item 键的 9,000,000,000,000,000 Long 数量、损耗 17、精准采集 1，以及 fluid 键的 16,000 Long 数量与原生类型保留。报告 ae-component-roundtrip-v2.json 含输入/输出 SHA，未将合成数量冒充原档库存；临时 JAR 已移出夹具。没有验证第三方玩法或光影。

只读定点提取实际 `region/r.-1.0.mca` 内全部 7 个现役 AE2 物品元件，439 个键，原始缓存总量合计 **3,281,878**；其中 76 个键带旧 tag、1 个键带旧 caps。caps 为酒瓶的 `creategearsandtavern:drink_fluid={}`，仍须按实际原生物品格式处理。私有 source-ae-storage-cells-v1.nbt 保留完整键、储量、损耗、附魔、Mekanism/背包数据、蜂巢内嵌实体等；没有拿物品模板 Count 代替库存数。**实际 7 个元件尚未完成整体转换和对账**，其所携物品、嵌套实体和机器状态继续处理。

玩家的实际原版 DFU 输出还显示：`Attributes` 已转为 `attributes`，条目字段变为 `id` / `base`，但 5 种 Forge 属性 ID 未改名。11 份玩家文件的旧 gravity / swim / step / reach 基值已只读核对，均无这 5 项的永久修饰器；全档属性结构补充审计继续进行，不能仅依据玩家样本判断所有嵌套实体。完整旧档转换、真实身份登录及保存重启验收仍未完成，VERSION 为 0.3.52，未发布。

## 实际全档属性结构与玩家属性转换

补充只读扫描已完成，`source-attribute-shapes-v1.json` 为 319,192 文档、27 种属性；全档 16,567 条旧台阶增量均为 Double Base=0、没有 Modifiers，5 种 Forge 属性的结构均只有 Name / Base。没有假设其他实体与玩家相同。

`entity_data.py` 在原版 DFU 后将旧重力映射为 `minecraft:generic.gravity`、游泳速度映射为 `neoforge:swim_speed`，两种触及距离映射为对应 `minecraft:player.*_interaction_range`；旧零台阶增量返回台账，让目标实体使用自己的原生基值，避免写成零台阶高度。其他属性与永久修饰器保持原值，非零/未知台阶状态或属性 ID 冲突阻止转换。新增 3 项回归，迁移工具 **36 项通过**（entity-attribute-tests-v1.log）。

单个 Java 21 原版 DFU 进程转换实际 11 份玩家的 NBT，再执行附件/属性规则：123 条输出属性 ID 均存在于目标运行注册表，全部玩家 UUID 保持一致，11 条零台阶状态都进入返回台账，进程正常结束（actual-player-attribute-conversion-v1.log）。这些结果只在内存核对，没有写出或启动旧世界，也未将仍待处理的玩家库存、饰品/背包及机器状态认定为已完成迁移。

## 缩小旧档范围后的库存与机械保护

用户允许只优先保留原版、Create、AE2、Mekanism 方块和对应物品，随后明确补充：精妙背包及其他存储内部的物品同样保留，玩家背包不能丢物品，已有机械不能破坏。执行口径已同步到规划 §1.3 与 AGENTS：各类库存整体保留，包含其他模组物品；容器、contentsUuid / SavedData、现有附属机械、支撑与连接列为例外，核心包含官方 Mekanism Generators。原目标 183 项清单不因旧档范围调整被删减。

`retention_scope.py` 复用已完成的源全档审计和目标原生注册表，只生成私有清单，没有再扫描或改写原档。实际结果为 **822 种物品 ID、135,806 条物品记录、10,771 个外层库存/实体/方块实体/SavedData 记录**；经验球根 Count 排除。这里包含模板和展示栈，不是实物总量或精确容器数量，后续仍按 realCount、AE2 Long 与物流 Amount 对账。四项已舍弃模组的实际物品记录为零。

旧档明确使用 **45 种、536 个非核心机械/容器方块**，包含 Create 附属动力和加工设备、柴油机、AE2 附属、自研终端与控制器、33 个精妙背包和 9 个精妙存储；这些不会因为 namespace 不在核心列表而被删掉。其它方块仅列待清理候选，未完成容器/机械及空间支撑核查前，不允许自动替换为空气或整维度删除。

私有 `pack-closure/retention-scope-v1.json` 权限 0600，仍有 12 个受保护物品 ID 和 5 个受保护方块 ID 需要既定注册映射（其中包含旧原版 grass，以及旧 ExtendedAE 设备）；工具按约定以 1 退出，不能据此打开旧档。新增 6 项保护规则回归，迁移 Python 工具 **42 项通过**，`git diff --check` 通过。当前只落实保留清单与清理边界，完整世界转换、所有库存/机械的原生保存重启及真实登录尚未验收；VERSION 仍为 0.3.52，未创建发行版。

## Mekanism 实际持久资产的原生转换（2026-11-21）

从封存原档只读提取 140 条 Mekanism / Generators 方块实体、6 条带状态的 Mekanism 物品、2 条 AE2 中的 Mekanism 物品键，以及安全频率与蒸馏塔缓存两份 SavedData。方块实体包含两个嵌套封箱载荷，不能把这些样本记录重复累加为全服实物总量。仅使用原包作者现有持久化接口和目标固定 JAR，没有自做移植或测试第三方玩法。

新增 `mek_data.py` 与隔离夹具桥接 `NativeMekBatch`，覆盖实际旧字段、全部四种化学库存、侧面与弹出配置、升级、所有者、传输连接、配置工具模式、CMP 封箱来源组件、机器物品内含库存及 SavedData 绑定。原版 DFU 只负责其原生数据类型；Mekanism 物品通过作者方块 `collectComponents()` 生成原生组件，完成后才移除旧自定义数据。未知字段和结构阻止转换，源数据不写回。

首次批次发现两条旧灌注库存的注册名字段实际为 `infuseTypeName`，原生接口把未转换的旧结构读为空。保留失败输入/输出与日志，修正字段并补充拒绝未知 Long 化学结构的回归。第二批 `actual-mek-native-batch-v2.log` 在 2 GiB / 2 CPU 的隔离新世界启动（Done 0.654s），原生转换 **155 条**记录，40 tick 后正常保存停服，Gradle 退出码 0；临时 Mod 已移出。没有打开封存世界或启动其中的机器。

`actual-mek-roundtrip-v2-semantic.json` 为私有报告，含输入/输出 SHA：155 条记录的物品 ID / 数量及流体、化学 ID / 类型 / 储量一致，65 个库存 UUID 绑定保留；140 条内存方块实体经原生保存、复读后完全稳定，8 条物品原生 codec 读写通过。19 条所有者/安全配置、83 条频率组件（含默认空配置）、16 条侧面配置、17 条升级配置、16 条弹出配置及传输方向/处理状态对账通过；6 个全局安全频率和 1 个蒸馏塔缓存保留全部实际库存及身份绑定。第二批没有原生化学读取错误。

已记录两项作者版本变化：热电阻加热器的旧 `energyUsage="2.5000"` 配置在目标作者接口中转为 Long `energy_usage=2`，不是储能余额；新版暮色极光方块移除旧 `variant` 状态，10 条封箱样本中的旧颜色值进入台账，方块 ID 和载荷保留。蒸馏塔的空 `items` 不再显式保存，`items_stored=4` 与其它槽数、热量、流体和库存 UUID 均保留。不会把这些编码差异写成资产丢失，也不声称旧颜色或小数配置逐字保留。

新增 8 项针对库存、槽位、化学、配置、封箱、所有者与拒绝未知结构的回归，迁移 Python 工具 **50 项通过**（mek-data-tests-v2.log）。这里只验收上述实际数据的离线/内存原生读写；所有玩家、全部背包与存储、实际 AE2 元件、整档生成、真实登录及保存重启仍需完成。VERSION 保持 0.3.52，未发布新发行版。

## 实际 AE2 元件及嵌套资产的组件对账（2026-11-21）

新增 `create_data.py`、`mod_item_data.py`、`registry_ids.py`，转换旧过滤器/包裹库存、照片实体、酒品质、料理记录及已核对的官方升级注册名。未知字段、非空未覆盖能力、组件冲突或缺失目标注册项均阻止转换；酒瓶实际空能力进入台账。旧 ExtendedAE 导入总线零件也出现在 AE2 键内，不能仅靠 Count 物品审计覆盖所有物品注册名。没有自做新的第三方移植。

照片与蜂巢内 7 份实体数据使用原版 DFU 和现有附件/属性规则。实际女仆 entity DFU 留下旧 `Attributes`；原版 bee 同时保留新旧列表。新增 `update_entity` 只在独立 player DFU 上下文转换原属性列表，确认重复新列表与结果相等后移除旧副本，保持原实体类型、UUID 和库存。失败的两次准备日志留在私有目录，未写入世界；第三次准备成功（actual-ae-item-preparation-v3.log）。

实际 439 个键先作为单位物品模板完成完整状态转换，其中复用的 3 条 Mekanism/精妙升级原生结果要求原始元数据完全匹配。叶物品在隔离夹具中通过原生 codec（actual-ae-items-native-batch-v1.log，rows=439，正常保存停服）。438 条组件编码一致；唯一烟花组件因等于原生默认值而不再显式保存。固定原生 Fireworks CODEC 的空 explosions 默认列表及 flight_duration=1 已核对，未将空字段省略误判为丢失状态。

随后按原来的 7 个元件装回 439 个原生键，以原生类型、物品 ID、完整组件和每键 Long 数量对账，未发生键合并。`actual-ae-cells-native-batch-v1.log` 为 Done (0.587s)、rows=7、40 tick 正常保存停服、Gradle 退出码 0；`actual-ae-cell-roundtrip-v1.json` 记录逐元件数量及输入/输出 SHA，**原始和原生总量均为 3,281,878**。增强的夹具同时导出 398 种实际键物品的默认组件及 6,362 种目标方块状态约束，注册表覆盖一致，供后续全档机械状态检查；不放置或运行第三方机器。临时 Mod 已移出夹具。

新增针对槽位、嵌套实体、官方映射、组件冲突、同键 Long 合并及隔离属性 DFU 的回归；迁移 Python 工具 **69 项通过**（actual-ae-rule-tests-v5.log），增强的原生 Java 桥接编译及上述原生数据批次通过。这里只确认实际 AE2 元件元数据的离线原生读写，**不代表全部 50 份背包库存、其它存储、玩家、整档和真实登录已迁移**。继续处理这些资产及整档保存重启，生产封存原档未打开，VERSION 保持 0.3.52，未发布。


### 全档物品元数据与方块状态的后续批次

对同一封存快照只读建立去重索引：319,247 份 NBT 文档、135,806 个物品引用、2,731 种完整单位元数据。引用包含展示/配方模板和历史备份，不能据此计算现役物品库存。目标原生目录包含 6,362 种方块；5,892 种真实旧方块状态全部通过属性检查，另有两条包含冒号的 JSON 名称不属于方块状态，单独排除。

批次先暴露药水旧注册名；固定旧/目标作者资产及原生预设核对后，完成 Miner's Delight 命名空间与 L2 更名，保留效果、时长、等级。旧凋零风暴的原版凋零药箭转换为同等原版自定义效果，原药水 ID 进入台账与物品 custom_data。未测试第三方药水的游戏专项功能。收割者附魔按作者更名转换；生命攫取附魔尚无等价目标，相关物品及包着它的元件被阻止，等待用户取舍。

原生失败批次 `storage-items-native-batch-v3.log` 记录 55 条错误，保存逐行完整诊断，未将部分结果当成功。五条作者聚合组件的 Java 对象 equals 不等，但其完整原生持久编码及有效组件编码一致，改用完整编码对账。后续 `storage-items-native-batch-v4.log` 正常保存停服，Gradle 退出码 0：**rows=2,712，complete=true，errors=0**；所有 ID、单位数量与完整原生编码重读一致。45 条背包、25 条升级使用作者既有迁移接口，背包 contentsUuid 均与目标 storage_uuid 相等。`storage-items-native-roundtrip-v4.json` 记录输入/输出指纹与验证边界。

`python3 -m unittest discover -s scripts/migration/tests -q` 实际执行 **91 项通过**；原生 Java 桥接编译及上述批次通过。仍有移动机械、无线终端、通用元件、生命攫取共 9 条单位元数据被明确阻止，10 条空 air 模板另记。仍未完成 50 份精妙背包 SavedData、9 个精妙存储方块、其它网络库存、真实玩家、完整机械与整档保存重启验收。封存原档未打开，VERSION 0.3.52 未变，未发布发行版。

### 无线终端、辅助物品与完整移动装置的后续对账

新增 `ae_items.py` 和 `auxiliary_items.py`，将无线终端能源、链接、槽位、界面设置、量子频率、通用元件 UUID/用量、古卷附魔、地图与羽毛笔状态接入固定目标原生组件。古卷附魔的旧 sweeping 已被原版 DFU 更名，修正为比较完整原生等级表。`storage-items-native-batch-v6.log` 实际为 **rows=2,715，complete=true，errors=0**，正常保存停服、Gradle 退出码 0；六条未生成记录仍为两个机械物品与四条生命攫取相关记录。没有依据“codec 接受 custom_data”声称旧功能仍有效。

新增完整装置转换和隔离 `NativeContraptionBatch`。`actual-contraptions-native-batch-v1.log` 正常保存停服、退出码 0；8 个实际装置、694 个方块全部完成原生载荷读取/保存/重新读取。`actual-contraptions-semantic-v1.json` 进一步证明每个相对位置的方块状态、完整持久方块实体数据、锚点、执行器、挂载库存及其余根状态一致；旧 BlockPos 的 Compound 转 IntArray 后坐标值不变，没有原点偏移。两个矿车物品的独立朝向保留，后续仍须装回物品组件。

固定作者保存结果省略 24 份旧执行器 Inventory：19 份为空列表，5 份只含 slot 0，且与同位置原始及目标 Actor 的 HeldItem 完全相同。仅在逐项证明后允许比较时省略这些副本，原列表保存在私有 `actual-contraptions-semantic-ledger-v1.nbt`，不计为第二份实物库存。非手持槽、不同持物、丢失所有者或改变挂载库存的回归均会失败。

迁移 Python 规则实际执行 **106 项通过**，固定原生 Java 装置桥接已编译并完成上述真实数据批次。这里只确认这些元数据和移动载荷，**50 份背包 SavedData、9 个精妙存储、其它网络实际库存、真实玩家、整档生成及保存重启仍未验收**。生命攫取没有官方等价目标，相关物品和元件维持阻止状态等待用户取舍；没有自行删除效果或自做第三方移植。封存世界未打开，VERSION 保持 0.3.52，未发布。

### 完整背包、精妙存储、通用元件与玩家库存（2026-11-21）

修正早期源覆盖统计：批次中的作者需求可能只是尚未返回父项的递归子项，不能按 native rows 推断源索引覆盖，也不能把差值算成空 air。`storage-items-native-batch-v7.log` 只处理新增的 16 行：两个矿车组件、七份封装生物/包裹记录、一个 MegaCells 元件和六种嵌套背包状态，正常保存停服、退出码 0。完整有效组件、背包容量/UUID/完整 renderInfo 对账通过。随后 `complete-source-item-coverage-v1.json` 逐项检查原始 2,731 条记录：**2,724 条转换成功、3 条实际空 air、4 条生命攫取相关记录待用户取舍**；不以缓存数量代替源记录数。

发现旧实体效果的数值 ID 包含模组 ID；改为按实际保存的 forge:id 写入原生 active_effects，保留时长、等级、显示标志以及本服已核对的牛奶和图腾清除行为。CurativeItems 是效果描述，不是实物库存；源记录保存在台账。固定 MobEffectInstance codec 的 85 种完整状态及 cures 检查通过。

`actual-storage-native-batch-v1.log` 在读取准备文件时发现 EOF，未进入库存读取。原因是离线工具把带文件头的 nbtlib.File 当作内嵌 Compound 写入；失败输入和完整日志封存，修正为纯 Compound 后，增加原生 NbtIo 完整传输检查。第二批暴露通用元件散列表保存的条目顺序变化，不能用列表顺序比较存储键；改为比较完整持久键、Long 储量和其它元数据，没有放宽未知或丢失记录检查。

`actual-storage-native-batch-v3.log` 为 **rows=99、backpacks=50、正常保存停服、退出码 0**。`actual-storage-semantic-v3.json` 独立核对原档 → 准备 → 原生结果：50 个背包 UUID 及全部 SavedData 字段一致；1,176 个库存/升级栈、2,313,098 件物品保留。9 个精妙存储方块按原区块中的真实状态构造，292 栈、1,319,841 件、所有槽位/容量、完整 storageWrapper 和其余持久数据一致；九条默认 false keepPacked 省略进入台账。1,357 个 realCount、71 处负 byte 溢出恢复原值，所有现有数量均在原生容量内。这些计数不包含内嵌存储的独立 Long 总量或模板展示栈，不能当作全服总量。

五份通用元件 UUID 文件的原始 109 个记录转换为 108 个原生键，Long 总量 **5,267** 保留。唯一合并是两个旧白桦木板键只差空 tag，188 与 48 合为 236；目标作者原生 load 同样合并。新增公开 `convert_universal_cell_data` 明确合并并记账，五文件结果与固定原生逐键一致；不同元数据不合并。后续接入整档时需刷新该元件原来的 107 类缓存为实际 106 类，保持 UUID、数量和其它状态。

`actual-players-native-batch-v1.log` 的内存读写先完成，但源对账发现本地夹具的 Curios common 配置为空，默认 charm 1 格小于旧档 2 格，且第二格确有一个物品。仓库已有普通槽位 3 格配置，未自行改设计；同步到夹具后重跑 `actual-players-native-batch-v2.log`，**rows=11、正常保存停服、退出码 0**。`actual-players-semantic-v2.json` 证明实际 UUID/名称/维度/位置、182 个玩家库存栈/1,526 件、24 个 Curios 物品和 65 份原槽位控制状态保留，旧第二槽物品未丢失；末影箱均为空。新增的空槽位及头部外观槽来自现有目标槽位定义，原物品、原槽位和显示/掉落控制保留。FakePlayer 不是实际网络登录，其保存 NBT 不用于最终玩家世界文件，防止生命值或游戏模式受到验证对象影响。

迁移规则当前实际执行 **114 项通过**，两个原生 Java 桥接编译及上述真实记录内存批次通过。**完整世界生成、其它网络/自研 SavedData 接入、实际机器与网络载入、真实登录、全档保存/重启尚未完成**。封存世界未打开，VERSION 0.3.52 未变，未创建发行版；继续完成整档阶段。

### 用户收紧旧档范围及后续转换规则（2026-11-21）

用户因额度限制明确只保留全部原版方块、工业方块、背包类、AE2及附属存储、玩家库存，其余世界内容直接舍弃。规划 §1.3 和仓库规则已同步；保留工业运行所需的 Create 装置、胶水、包裹和运输矿车，及其完整载荷、UUID 和持久连接。不再继续生物、装饰、作物、弹丸及无关容器的兼容与专项验证。保留容器内的跨模组物品仍需完整迁移，源冷快照不变。

用户另已批准舍弃生命攫取效果并保留完整原附魔记录。转换只移除有效附魔表中的 life_grasp，其余附魔、原物品及数量不变；原有效组件和旧 Enchantments / StoredEnchantments 列表同时写入 custom_data.dimensionworks_migration.original_enchantments。实际三本书及含该记录的 MegaCells 元件完成原生保存/重读与完整编码对账，未新增替代效果。

原生整批方块实体和实体读写此前已执行，不因收紧范围重跑。57790 份直接方块实体均有原生结果；准备载荷仍保留 Create 动力图和原始持久字段，不能用脱离世界的原生默认转速覆盖真实机械。新增规则将 1034 份实际空矿车控制器迁为作者的 NORMAL 附件；三处 AE2/ExtendedAE 终端的旧 ALL 筛选设置转为全部四个实际已注册键类型，防止新版静默只启用第一个键类型。五份实际无线终端/照片/父存储元件已完成对应原生组件对账。弃用的生物数据证明不再作为后续验收前提。

`OfflineRegionDfu` 已生成并校验完整基础副本：1379 个区域文件、318878 条区域记录，原载荷隔离后仍需覆盖。新增 `OfflineWorldOverlay` 按带完整源文件 SHA-256 的计划在第二份副本应用保留策略：原版和工业状态必须原样保留；范围外方块变为空气，其方块实体必须依据实际所在方块的状态定点归档；工业实体及载荷身份不得被遗漏。变更区块清除旧光照/高度图缓存并保留方块位置，避免在已清理位置继续使用过期缓存。

实际格式夹具完成 gzip、zlib、raw、1.1 MB 外置区块、零字节区域及全部六条记录的转换/覆盖；保留库存数量和原版方块位置正确，范围外方块/实体按指定清单移除，基础副本指纹不变。五个反例验证未完成计划、源指纹变化、删除工业实体、删除原版方块及缺失方块实体均被阻止；中途失败仍保留不可启动标志。Java 编译与迁移 Python **147 项检查通过**；`.nbt` 补充审计发现五个缓存/空数据文件，未发现库存，源文件未修改。

限定清单已完成：39610 份保留方块实体、1101 个工业实体、11 份玩家数据；18180 份范围外方块实体和 27692 个非工业世界实体归档舍弃。范围 CLI 检查为受保护物品 716 种、工业例外方块 45 种、缺失注册项 0、未定类型 0；官方更名和唯一圆石无限元件配置均有原始记录证明。完整计划首次以 512 MiB 堆读取出现 Java heap space，输出目录尚未创建；后续仅将离线工具上限调整为 1536 MiB，保持两 CPU。

完整覆盖随后在暮色未完成生成的区块发现只含 id=DUMMY 与 x/y/z 的原版待初始化标记，原资产索引未将其计作机器。失败副本保留不可启动标志；核对目标 LevelChunk 原生逻辑后，明确将严格四字段无库存标记按实际方块状态保留或归档，单独记账。其它未索引的真实方块实体仍会阻止转换。新增格式夹具验证各一个保留/舍弃标记及原有五个拒绝条件通过，继续从未修改的基础副本生成新的目标。

完整目标副本尚未完成真实登录及保存/重启。这个阶段不是迁移交付，所有副本继续保留 `.migration-incomplete`，生产原档未打开，VERSION 仍为 0.3.52，未发布。

### 限定范围整档副本的离线验收（2026-11-21）

`target-world-scoped-v4.partial` 已完成覆盖，日志为 `WORLD_OVERLAY_DONE chunks=318877 entities=1101 ready_for_minecraft=false`；基础世界 SHA 未改变。对这份输出重新执行完整只读审计：339 个 NBT 文件、318877 个区域区块、11 名玩家、129971 条物品引用，解析错误 0。区域读取通过不等于逐项保留正确，因此没有据此移除不可启动标志。

新增 `audit_scoped_world.py`，以覆盖计划为准独立重读目标副本，核对目标文件集合与 SHA-256、每个区块 DataVersion、批准方块状态、39610 个保留方块实体、18180 个归档方块实体、12209 个实体区块、1101 个保留实体、27692 个归档实体及 622 个保留 DUMMY 标记。它还核对 266 份完整 NBT、目标文件覆盖记录、计划声明的区块/实体数量，并拒绝目录外方块或未归档实体残留。实际报告 `target-world-semantic-audit-v2.json` 为 complete=true、ready_for_minecraft=false、block_ids_outside_catalogue=[]；覆盖报告 SHA-256 为 `8819aeece791d0af27191915648882b01af44f59ee338da7150f85516b6e839e`。

新增 3 项审计回归并通过，其中完整小世界夹具确认逐项一致，库存数量被修改时拒绝通过；迁移 Python 测试整体实际 **150 项通过**，`py_compile` 与 `git diff --check` 通过。下一阶段从未修改的 v4 副本另建独立游戏副本，移除不可启动标志后在专用 NeoForge 服务端启动，完成实际保存、停服、二次启动和玩家/机械/库存对账。封存原档仍未启动，VERSION 0.3.52 未变。

### 实际 NeoForge 启动、保存与重启（2026-11-21）

首次启动发现并修正了精妙核心展示组件中的 4 条旧 `FluidStack`：3 条在方块实体背包渲染信息、1 条在玩家饰品背包渲染信息。旧值合计为汽油 89,656、Create Enchantment Industry 经验流体 5,741,505 + 2,304,000、精妙核心 XP 2,430,430；转换只把展示字段的 `FluidName/Amount` 改为目标 `id/amount`，流体 ID、数量、槽位和其它组件不变。重新生成并审计后，服务端不再报告无效液体。

保留完整 `chunks.dat` 时，服务端能正常启动，但停服在区块卸载循环中持续占用 CPU；移除整份文件可正常停服，但会丢失自动加载语义。分组复现表明 Create Power Loader 的 50 条票据可正常启动和停服，Mekanism Dimensional Stabilizer 的 25 条票据触发该问题；关闭 Mekanism 全局区块加载可停服，但不作为正式方案。最终只停用这 25 条 Mekanism 持久票据，保留其完整源记录、控制器、25 个唯一区块和停用原因，另保留方块本身、稳定器配置、能量/库存/频率及其余 50 条 Create 票据。代价是这台稳定器升级不再通过该持久票据自动拉起区块；这是明确的功能缩减，不能描述为完整保留。

最终候选 `target-world-scoped-v6.partial` 重新通过离线语义审计：39610 个保留方块实体、18180 个归档方块实体、12209 个实体区块、1101 个保留实体、27692 个归档实体和 622 个 DUMMY 标记逐项符合计划。另建专用服务端实例移除不可启动标志，使用本机 RCON 执行 `save-all flush` 和 `stop`；`final-boot1` 与 `final-boot2` 均为 `Done`、全部维度保存完成、退出码 0、日志 ERROR 0 和无效液体 0。

保存并第二次退出后用固定版本 FTB 解析器导出 52 份 SNBT 再审计：373 个 NBT 文件、318821 个区域区块、11 名玩家、130021 条物品引用，解析错误 0。与保存前相比减少 57 个实体记录、增加 1 个实体记录；减少项全部是计划中保留实体为空的区块，新增项是运行期间产生的实体区块。11 份玩家 UUID、维度、位置与持久键不变。方块数量仅有液态测试运行导致的 4 类变化：石英芽生长、坩埚/熔岩坩埚状态转换，未见保留方块被清理。新增展示流体与票据停用回归后，迁移 Python 测试整体实际 **152 项通过**。

仍未完成真实客户端登录、客户端界面/网络握手、多人并发和长期生产回归。Mekanism 25 条持久区块票据的自动加载功能已明确舍弃并记账；正式发行和生产原档切换仍需用户授权。VERSION 仍为 0.3.52，封存原档未打开，未创建 Release。
