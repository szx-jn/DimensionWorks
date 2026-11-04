# 给 6luna 的应力网络优化实施交接

先读 [总体方案](kinetic-network-performance-plan.md) 和 [实测报告](../validation/kinetic-lab/report.md)。本文件定义实施任务、接口语义和交付条件。当前测试分支已实现并验证低风险的缓存失效、ME snapshot 去重、容量配额总账和红石输出优化；虚拟负载批量提交、动力源发现索引、拓扑算法改造仍未实现。

## 1. 实施范围和优先级

按以下顺序逐项实现，每项独立提交并复测。不要一次重写整个 Create 图算法。

| 顺序 | 任务 | 状态 | 主要文件 | 原因 |
| --- | --- | --- | --- | --- |
| 1 | 失效原因与加载同步标志拆分 | 已实现；常规失效不再触发恢复强制同步 | rpm-limit/optimizer/KineticNetworkOptimizer | 数值未变的普通失效也会强制广播 |
| 2 | ME 服务读取去重、输入变更失效 | 已实现；同 tick 纯读复用，按动力网络订阅失效 | mek-stress/memory/MemoryGridService、mixin/create/KineticNetworkMixin | 读取路径存在计算及输出副作用 |
| 3 | 容量所有者总账增量维护 | 已实现；按样本差值更新、容量读不遍历全局网络样本 | rpm-limit/quota/CreateCapacityRegistry | 已测出全局 G² 样本访问 |
| 4 | 统一提交与受约束的批处理 | 未实现；仍保留即时 Create 更新契约 | 两个 Registry、GeneratorShaftMixin、Optimizer | 已测出 U×N 成员更新 |
| 5 | 应力表输出变化检测 | 已实现；只在模拟红石档位变化时通知邻居 | stress-gauge-alarm/StressGaugeBlockEntityMixin | 避免红石反馈放大 |
| 6 | 动力源发现与所有者索引 | 未实现 | GeneratorPowerBridge、ReactorPowerBridge、RpmLimitManager | 降低每轴重复探测与上限更新尖峰 |
| 7 | 拓扑增量、RPM 频数、保存版本 | 未实现 | Optimizer、NetworkRpmLimit、KineticPersistence | 减少恢复和拓扑变化时全量重建 |
| 8 | 传播算法与通知模型 | 未实现 | RotationPropagator 适配或正式 fork | 风险最高，最后处理 |

## 2. 权威状态与失效接口

建议为每个 KineticNetwork 维护 NetworkState，包含 topologyVersion、speedRuleVersion、contributionVersion、quotaVersion、publicationVersion、loadGeneration，以及 stress/naturalCapacity/effectiveCapacity、物理持久化余额。命名可调整，语义不可混用。

建议引入 invalidate(network, reason)、upsert/removeMember、upsert/removeSource、commit(network, cause)、requestPublication(network)、flush(network)。原 invalidate(network) 作为 UNKNOWN_EXTERNAL_MUTATION 保守兼容入口；已知调用者逐个迁移。版本单调递增，避免依赖 gameTime 唯一标识变化。普通无变化读取不得写版本。

| 原因 | 应力贡献 | 自然容量 | RPM 最低值 | 配额 | 保存快照 | 恢复强制同步 |
| --- | --- | --- | --- | --- | --- | --- |
| 单成员负载变化 | 单点增量 | 保留 | 保留 | 保留 | 物理成员才失效 | 否 |
| 动力源出力变化 | 视转速是否变化 | 单点增量 | 保留 | 受影响所有者 | 物理成员才失效 | 否 |
| 成员加入/移除 | 单点增减 | 源才增减 | 所有者频数增减 | 源才调整 | 物理成员才失效 | 否 |
| 所有者额度变化 | 保留 | 保留 | 独立 RPM 变化另处理 | 受影响桶 | 保留自然账目 | 否 |
| 有效转速规则变化 | 重算受影响成员 | 重算受影响源 | 更新对应限额 | 源变化才调整 | 失效 | 否 |
| NBT 恢复 | 恢复余额并增量加入 | 同左 | 拓扑版本变化 | 更新样本 | 失效 | 是，每轮一次 |
| 未知第三方修改 | 完整重建 | 完整重建 | 失效 | 样本重算 | 失效 | 否 |

WeakHashMap.clear 不会自动缩小底层表；反复 clear 的开销应结合 invalidate_only 场景判断。避免把“标记失效”实现成每次都清空多个大型表。有效性用版本标志表示，真正重建时再替换贡献表。清理生命周期采用明确注销；弱引用只是兜底。

重建使用临时贡献表，遍历和剪除完成后一次发布。当前 onMemberPruned 会调用完整 invalidate，可能清掉本轮前面已记录的贡献；不得随后把不完整贡献表置为 valid。添加“多成员+中途剪除+逐个成员差量更新”的行为测试。

## 3. 合并刷新时的同步边界

实测已有 beginBatch/endBatch 可显著减少广播，但不能直接包围完整服务器 tick 后宣布完成。

每次变更先更新权威账本，并计算更新前后的过载状态。以下情况立即 flush：过载翻转、实际有效转速改变、成员迁移、加工前请求严格状态、未知兼容回调要求。纯数字变化且没有此类屏障时，允许 tick 末按网络身份去重发布。首次接入的虚拟负载必须先扣账再允许加工；同 tick 减容也必须先对后续机器生效。

第一版保留原始 Create 成员回调契约。若调用方直接读取成员 stress/capacity/getSpeed，则在加工边界执行 ensureCurrent(network)；尚未审查的第三方保持即时发布。不能只保证全网总数正确，却让成员 overStressed 仍旧为 false。

事务使用 try/finally，维护嵌套深度；flush 中产生新的变化放入下一轮待处理集合，按版本判断是否还有实际工作。循环不收敛时记录相关网络与变化来源并走保守同步，不能静默丢弃。服务器关闭清队列。批处理不得吞掉原始错误后继续把不完整账本标成有效。

## 4. ME 版本与计算副作用

拆分 readSnapshot、invalidateInputs(reason)、recomputeAllocation、applyOutputs。getGeneratedSpeed 返回已发布状态，避免无条件重新遍历整个服务。保留原有即时安全规则：输出配置、卡、节点活动、输入容量或上限发生变化时，必须使后续消费读到新分配。

不能仅按 tick 缓存：同 tick 多次调参、减容、节点失效都必须立即反映。需维护参与者集合版本、静态配置版本、输入网络贡献版本、输入源类别版本。当前 inputCapacity 检查网络内有无 ME 输出源以防循环回收；将其改为网络源类别计数，不得删除此反循环规则。

计算时捕获输入版本，生成完整候选分配，版本仍相同时原子发布；再应用有变化的输出。应用中触发重入仅标记 pendingDirty，外层完成后根据新版本重算。不得在更新中途把半份分配暴露给另一个输出口。

针对依赖输入网络状态又影响输出网络的反馈环，记录依赖关系，检测周期；保持当前禁用回收语义，额度必须守恒。短期对缺少事件通知的外部输入每 tick 采样一次，采样值不变不重新分配；向调用方明确这条兼容路径的时效，严格加工读需校验输入版本。

## 5. 配额索引与共享桶

维护 network→owner→contribution、owner→rawTotal、owner→networks，及共享桶→受限网络依赖。源增加、减少、所有者变化均以旧贡献扣除、新贡献加入处理，0 值删除空桶。每次返回配额后容量为本网络各 owner 分量乘以该 owner 当前比例的和。

共享桶变化可能确实影响所有受限所有者；保留这项必要传播。普通 owner 变化只刷新反向索引中的网络。未加载容量继续遵循共享归属，不能在卸载后丢掉余额。全局样本不得借弱引用 GC 随机改变余额；明确移除时维护总账。

将 scaledCurrent/scaleBucket 对已按 UUID 聚合的 Map 再次遍历改为直接 get。缓存所有者限制比例，额度版本改变才计算；自然容量读无变化时不得新建贡献 Map、全局 raw Map 或标记其他网络 pending。

## 6. 红石、发现和所有者更新

应力表记录上次实际输出强度；比较新旧强度后才通知邻居和比较器。首次加载、移除、方块替换和超载恢复必须有明确初值规则。保留其他模组对比较器输出的契约。

建立已加载外部动力端口与轴的绑定索引；普通轴的负结果缓存由邻居/区块/结构事件失效。反应堆绑定核心、端口列表可缓存，generationRate 等动态出力必须按实时版本读。明确滞后一 tick 的容许范围，禁止保存或发现过程强制加载区块。

RpmLimitManager.applyLimitToOwned 当前遍历所有维度加载区块的方块实体；维护 owner→已加载动力成员索引，按涉及网络分组更新。所有者离线缓存和 PlayerPersisted 迁移规则保持。无所有者中继继承网络限额，虚拟成员不引入默认 32 RPM 参与者。

## 7. Create 传播优化的约束

实际传动长链变速仍触发断接和重新传播，即使最终只同步一次，也会产生约 3N 次成员状态更新。先用 JFR 定位 setNetwork、成员删除/加入、RPM 重新计算、贡献表 clear、邻居发现各自占比。

先把一次传播内的账目失效合并，再考虑显式 DFS 工作栈。工作栈必须保存递归返回后的执行位置，保持原邻居遍历顺序、速度冲突破坏、源选择、齿轮传动比及自定义 propagateRotationTo 语义。仅将递归改成普通队列可能改变行为。propagateMissingSource 已用 LinkedList，不能把 remove(0) 当数组搬移瓶颈。成员关系分裂需要访问受影响组件，不承诺 O(1)。

如果需要 fork：固定基线提交，最小补丁，固定 Release 资产；不能直接修改只读审查源码后忽略发布来源约束。

## 8. 验收与提交

每项提交运行相关现有单元测试、真实 GameTest 和本次相应基准，记录实际次数与耗时。源字符串契约测试只证明文本存在，不能替代运行验证。给出同硬件同探针条件下的 CPU 时间和调用次数，避免只选最好 wall time。

必须补充的正确性场景：同 tick 接入/减容后立即加工；过载停机保留虚拟负载；ME 输出回收拒绝；同 tick 额度多次改变；共享桶变动；最低上限成员移除；成员剪除；恢复逆序加载；保存不混入虚拟负载；异常嵌套事务；第三方未知修改回退；红石信号不变/翻转/拆除。

按稳定无变化、变化未越界、过载翻转、实际变速、拓扑拆分分别验收。稳定无变化目标是 0 重建和 0 无理由广播；纯读 ME 版本不变时 0 重算；缓存容量重复读时 0 全局样本扫描。真实变速或过载翻转需要的成员通知应单独计数，不能为了达到“每 tick 一次”而删除必要回调。

最后更新各模组 README、总体设计与实测报告，执行 git diff --check 等静态检查。正式发布仍走仓库工作流；本地实验产物不作为正式发行。
