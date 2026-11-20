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
