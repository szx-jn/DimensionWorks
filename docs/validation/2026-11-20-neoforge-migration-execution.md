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

## 下一阶段准入

私有快照验证及全档读取审计已完成；随后确定缺失注册 ID 的处理、锁定完整目标依赖闭包，移植其余模块和脚本，再实现一次性 NBT/组件转换器。完整客户端/服务端冷启动、重启、库存语义对账、玩家身份及真实生产链验收通过前，禁止给玩家开放跨版本更新。
