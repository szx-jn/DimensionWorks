# DimensionWorks 文档

用于保存整合包的设计资料。

已建立：

- [`design/twilight-full-production-plan.md`](design/twilight-full-production-plan.md)：暮色方块的职责与可替代性审查；六个重复/职责不明的机械提案已撤回，先确认独特组合能力再设计产线；下一维度待定。

- [`design/root-version-authority.md`](design/root-version-authority.md)：更新器与登录检测仅以实际游戏根目录 VERSION 判断当前版本、失败处理与文件事务规则。
- [`validation/2026-11-14-trueuuid-client-session-failure.md`](validation/2026-11-14-trueuuid-client-session-failure.md)：TrueUUID 1.0.10 客户端会话提交失败的日志证据、脱敏诊断修改与验证边界。
- [`validation/2026-11-07-trueuuid-automatic-fork.md`](validation/2026-11-07-trueuuid-automatic-fork.md)：基于 TrueUUID 1.0.9 的自动迁移 fork、玩家数据保全和正式发布范围。
- [`validation/2026-11-07-trueuuid-login-recovery.md`](validation/2026-11-07-trueuuid-login-recovery.md)：TrueUUID 1.3.0 登录兼容故障、玩家数据冷备份、两端回退及正版登录恢复。
- [`validation/2026-11-07-trueuuid-migration.md`](validation/2026-11-07-trueuuid-migration.md)：离线转 MSL 登录的重复 UUID 拦截及已撤回的 1.3.0 升级文件事务测试。

- [`validation/2026-11-07-sophisticated-core-compatibility.md`](validation/2026-11-07-sophisticated-core-compatibility.md)：0.3.10 背包与新 Core 的启动兼容性修复、固定版本组合及静态验证边界。

- [`design/transfer-safety-cloud-disk.md`](design/transfer-safety-cloud-disk.md)：中转物品保护、安全落点、九区块可见范围与 F12 持久云盘回收。

- [`design/twilight-industrial-branch.md`](design/twilight-industrial-branch.md)：暮色森林物品调查、工业支线与第三修复碎片关联提案。

- [`design/twilight-resonance-industry.md`](design/twilight-resonance-industry.md)：暮色共振工艺、冒险与工业替代路线、一次性首领压制及增益设备。

- [`design/twilight-ecological-production.md`](design/twilight-ecological-production.md)：暮色环境与生物供料、二次加工、回用优化及 20～30 小时主动工业游玩提案。

- [`design/twilight-progression-survey.md`](design/twilight-progression-survey.md)：暮色 4.3.2508 入门至毕业、三分支进度、区域保护、原生再生资源与全部非配方进度调查；后续产线分期依据。

- [`design/twilight-first-arrival.md`](design/twilight-first-arrival.md)：单向承接主世界工业基础的暮色开场、林地浸出与野猪调理首版规则。
- [`validation/2026-11-07-twilight-first-production.md`](validation/2026-11-07-twilight-first-production.md)：暮色首版产线的服务器 GameTest、构建、静态检查与未验证范围。

- [`design/energy-mechanical-unification.md`](design/energy-mechanical-unification.md)：FE/t 与 SU 统一计量、跨模组供能范围与生命周期约束。
- [`design/energy-plate-production.md`](design/energy-plate-production.md)：Mekanism 能量板的金属注液、机械装配工艺、零能量约束与下游材料接回。
- [`validation/2026-11-03-energy-unification.md`](validation/2026-11-03-energy-unification.md)：能源审查范围、独立客户端验证与存档恢复回归。

- [`design/food-addon-expansion.md`](design/food-addon-expansion.md)：20 项森罗/农夫附属、兼容版本选择、必需库与 Curios 3/1 默认容量。

- [`design/quark-integration.md`](design/quark-integration.md)：Quark 铁丝网、默认建筑与机制、世界生成关闭及背包整理去重。

- [`design/sulfur-production.md`](design/sulfur-production.md)：余烬面粉热分离制硫、原生煤炭制硫替换与硫粉兼容

- [`design/copper-recipe-inventory.md`](design/copper-recipe-inventory.md)：基于 0.2.63 发布包数据和当前脚本的铜、黄铜、青铜配方静态清单，附完整 CSV、条件配方及世界交互说明。

- [`design/stress-gauge-redstone-alert.md`](design/stress-gauge-redstone-alert.md)：Create 应力表过载红石告警行为与实现边界

- [`design/kinetic-network-diagnostics.md`](design/kinetic-network-diagnostics.md)：应力网络恢复账目、最低 RPM 上限与扳手拓扑 GUI

- [`design/me-live-stress-network.md`](design/me-live-stress-network.md)：ME 实时应力额度、RPM 总量过载与两条供能路线造价

- [`design/advanced-speed-controller.md`](design/advanced-speed-controller.md)：高级转速控制器配方、数字面板与进度边界

- [`gameplay/industrial-assessment.md`](gameplay/industrial-assessment.md)：以高中化学课程标准为参考的工业能力考核体系、任务设计规则与绕过审查
- [`gameplay/parcel-dimension-dispatch.md`](gameplay/parcel-dimension-dispatch.md)：CMP 玩家打包、个人物流领地发运终端与暮色森林路线

- [`dimensions.md`](dimensions.md)：六维洞穴的长期工业职责
- [`resources.md`](resources.md)：洞穴资源与效果流体生态
- [`logistics.md`](logistics.md)：跨维物品与流体接口
- [`performance.md`](performance.md)：工厂结构与运行成本约束
- [`localization.md`](localization.md)：客户端简体中文覆盖范围、术语规则与校验流程
- [`superpowers/specs/2026-09-27-dimension-cave-factories-design.md`](superpowers/specs/2026-09-27-dimension-cave-factories-design.md)：六维洞穴工厂总设计与渊海首版实现范围
- [`superpowers/specs/2026-09-27-cave-self-propagation-design.md`](superpowers/specs/2026-09-27-cave-self-propagation-design.md)：六维基础产物的 Create 自增殖主链与残渣备链
- [`superpowers/specs/2026-10-13-gear-heart-industrial-quotas-design.md`](superpowers/specs/2026-10-13-gear-heart-industrial-quotas-design.md)：齿轮之心七阶段工业瓶颈、修复祝福与跨模组额度边界

专题规格位于 [`superpowers/specs/`](superpowers/specs/)，实施计划位于 [`superpowers/plans/`](superpowers/plans/)。

## 应力网络性能分析

- [总体性能方案](design/kinetic-network-performance-plan.md)
- [给 6luna 的实施交接](design/kinetic-network-implementation-handoff.md)
- [真实服务器基准与证据](validation/kinetic-lab/report.md)
