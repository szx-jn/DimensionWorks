# 应力网络诊断夹具

这些探针只安装进隔离副本，不进入正式模组。测试真实 Forge 服务器及 Create/Mek/AE 类，账目夹具把已加载物理方块实体人工加入同一网络；physical_* 场景另外验证真实连接的轴链，不能把两类测试混称为完整工厂压测。

已准备的环境：`/workspace/toolchain/jdk17`、`/workspace/toolchain/gradle-8.8`、`/workspace/stress-lab/{dimensionworks-rpm-limit,dimensionworks-mek-stress}`。Forge 缓存在用户 Gradle 目录。完整本地日志在 `/workspace/stress-lab/logs`，世界在隔离项目的 `run-gametest/world`，旧世界移至实验目录保存。

重现流程：

1. 复制当前两个模组到独立目录，保留兄弟关系。使用 Java17、Gradle8.8。为单元测试保留原仓库目录结构，或把 test.workingDir 指向原模组目录（源码文本契约依赖相对路径）。
2. `python scripts/performance/kinetic-lab/install-probes.py /workspace/stress-lab`。安装器修改隔离副本的 mixin 列表，加入计数器和 GameTest；不要给它生产目录。
3. 为本机设置 Gradle HTTP/HTTPS 代理及 CA 信任，使用继承的代理，不将秘密写入脚本。该环境使用 `/etc/ssl/certs/java/cacerts`。可将 Maven Central 提前，避免公共依赖在每个模组仓库轮流查找。首次构建需要下载 Forge、官方映射和资源。
4. 运行两个模组的 `test`。然后在 mek-stress 工程运行 `runGameTestServer`。测试命名空间为 kineticlab。AE2 common.json 的 channels 设为 infinite，避免本实验 128 输出口在无控制器的测试网中被八频道规则禁用；这只是实验变量，不修改整合包规则。
5. 每轮使用新的 world，保留前轮结果；相同 world 的远处 ME 夹具不会被小型 GameTest 模板清除，会污染后轮稳定 tick 数据。每次启动独立 JVM。可加 `-Xmx3G -XX:ActiveProcessorCount=2` 和 JFR 的 profile 记录。
6. 提取：`python scripts/performance/kinetic-lab/summarize.py docs/validation/kinetic-lab /workspace/stress-lab/logs/mek-lab-run4.log ...`。正式重复实验建议仅汇总全新世界的完整成功轮次。

每场景有一轮预热（rep=-1）及五轮记录。计数器只在测量窗口启用，真实变速同步调用单独计数。quota_samples 表示容量查询为聚合所有者而访问的全局网络样本数；quota_owners 是当前网络被应用的所有者桶数；quota_cleanup_samples 是每秒级服务端清理期间访问的样本数；quota_sources 是重新采样实际 source 的次数。me_reentrant_reads 只统计 snapshot 正在刷新时的重入入口，不等同于完整 snapshot 次数；me_participants 直接统计刷新期间遍历的参与节点数，是判断重复扫描的主要指标。wall_ns 为当前操作墙钟时间，cpu_ns 为服务器线程 CPU 时间。physical 稳定 tick 包括整个测试服务器 tick，不是应力独占耗时。强制加载仅用于固定实验覆盖范围，生产优化不应照搬。

应力改变量夹具直接比较差量总账与逐成员精确重算。批处理 A/B 只演示减少通知的潜力，不证明对所有附属延迟同步安全。ME 测试使用空闲零应力输出，验证纯读取产生的扫描量；带载反馈环的正确性仍需实施时补测。
