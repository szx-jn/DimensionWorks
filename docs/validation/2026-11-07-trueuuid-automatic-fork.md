# TrueUUID 1.0.9 自动继承修改版

## 发布范围

维护者要求基于可恢复登录的社区版 1.0.9 添加自动继承功能，停止后续测试，
推送 fork 后由 GitHub Actions 构建正式 Mod，再更新整合包清单并发布。
本次不向正在运行的正式服务器安装修改版，也不改动正式世界及恢复备份。

- fork：[szx-jn/TrueUUID-DimensionWorks](https://github.com/szx-jn/TrueUUID-DimensionWorks)。
- 上游 1.0.9 基线：`9c4479a758a3347732a1172c52b4edae8be9b336`。
- 修改版源码：`190243804db92aa2926f9ce266c36c158ceb11b5`。
- 固定版本：`1.0.9-dimensionworks.1`，Forge 1.20.1、Java 17、Forge 47.4.23。
- 固定标签与文件：`v1.0.9-dimensionworks.1` / `trueuuid-1.0.9-dimensionworks.1-forge1.20.1.jar`。
- 正式产物：[TrueUUID JAR](https://github.com/szx-jn/TrueUUID-DimensionWorks/releases/download/v1.0.9-dimensionworks.1/trueuuid-1.0.9-dimensionworks.1-forge1.20.1.jar)，`109,297` 字节。
- 正式 JAR SHA-256：`22e5a84c5de5f21dd3045cdf346f8246a34ad487cf05669e0860665da27d2019`。
- 第三方源码留在独立 fork，保留 GNU LGPL 3.0 许可和上游署名；整合包只引用固定 Release 资产。

## 行为与保全规则

认证和客户端会话协议保留 1.0.9 基线。`auth.autoMigrateOfflineData` 默认开启，
只有真实会话验证成功、登录名与目标身份一致、已绑定 UUID 不冲突时，
才在 Forge 继续登录前继承该名字的离线存档。认证失败、公开皮肤查询和
本地代理宽限不触发自动迁移。使用原始登录名计算离线 UUID。

先备份相关原文件、已有目标和共享引用，保存 SHA-256 与事务状态，再写
目标并刷新 FTB Teams/Chunks/Quests/Essentials 相关缓存，最后归档旧文件。
玩家 NBT 仅替换根 UUID，其他数据保留。相关缓存结构不兼容时提前停止。
目标存在不同进度、缺少对应旧文件或共享数据同时引用双方时，自动模式
拒绝覆盖并保留双方文件，不猜测应当采用哪份进度。

备份位于 `world/trueuuid-backups/automatic-migrations/` 的独立事务目录。
写入失败时回滚，未完成事务在下次存档加载前恢复；外部修改或摘要异常时
停止恢复并保留备份。事务拒绝符号链接、路径越界、过大文件及并发执行。
内部局部备份不能替代完整世界、配置、TrueUUID 注册表和权限数据库冷备份。

管理命令与覆盖/清理语义见 [服务端说明](../../server/README.md)。仅扫描
已知玩家文件与有界 FTB 文本目录，不扫描区块文件、不加载区块、不创建
实体或常驻任务；不会增加机器阵列或跨维度工业网络的持续运行开销。

## 实际验证边界

停止测试前，开发构建及 18 项文件事务、恢复和 NBT 保全测试已通过。
认证基线、Java 17 字节码、Forge SRG mixin 映射及 JAR 中的 LGPL 许可
已做静态校验。上述记录只对应实际执行的开发检查。

维护者要求停止后续测试后，隔离测试服务器和临时 SSH 隧道已停止，
隔离世界及备份保留。没有完成修改版的游戏内 Mojang/MSL 登录、真实玩家
数据迁移或 FTB 缓存验收；此前官方 1.0.9 的恢复成功不能替代这些验收。

Mod 正式发布只执行 Actions 构建与静态 JAR 校验，Gradle 使用 `-x test`。
[正式构建 42342758250](https://github.com/szx-jn/TrueUUID-DimensionWorks/actions/runs/42342758250)
已成功，Release 的源码提交及资产摘要与上述记录一致。
整合包本次正式发布使用 `skip_tests=true`，跳过 Go、aria2 下载及 Linux
更新事务测试，保留构建和压缩包、版本、受管清单及摘要校验。
