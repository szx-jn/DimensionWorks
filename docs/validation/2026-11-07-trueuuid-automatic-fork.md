# TrueUUID 1.0.10 自动继承修改版

## 发布信息

- fork：[szx-jn/TrueUUID-DimensionWorks](https://github.com/szx-jn/TrueUUID-DimensionWorks)。
- 上游基线：官方 TrueUUID 1.0.9，提交 `8c7eb9bc3df6240dfd7d64594185feee6bcebd60`。
- 修改提交：`4db5772efad8bc3ed8427ea7ba04e3f415186297`；发布工作流提交：`7102fb449bfd5fcbfb383cc59d99ea01fc8294be`。
- 版本：`1.0.10`，Forge 1.20.1，Java 17。
- Release：[v1.0.10](https://github.com/szx-jn/TrueUUID-DimensionWorks/releases/tag/v1.0.10)。
- JAR：[`trueuuid-1.0.10-forge1.20.1.jar`](https://github.com/szx-jn/TrueUUID-DimensionWorks/releases/download/v1.0.10/trueuuid-1.0.10-forge1.20.1.jar)（80,378 字节）。

## 修改范围

保留官方 1.0.9 的认证协议与迁移实现，增加认证成功后的自动继承：

- TrueUUID 自定义握手路径：客户端完成 `joinServer`，服务端独立验证会话后，自动继承同名离线玩家数据。
- 原生 online-mode 路径：在 `PlayerList.placeNewPlayer` 入口执行继承，使 MSL/authlib-injector 等原生 Yggdrasil 验证路径也能在玩家数据加载前迁移。
- 迁移沿用原有 `PlayerDataMigration` 处理与备份机制；迁移失败时拒绝登录，避免玩家进入不完整存档。

MSL Skin 的 authlib-injector 服务端配置要求使用原生 `online-mode=true`。两种认证路径都必须由服务端完成真实会话验证；客户端声明或皮肤查询本身不构成认证成功。

## 构建与验收边界

GitHub Actions 运行 [43030681620](https://github.com/szx-jn/TrueUUID-DimensionWorks/actions/runs/43030681620) 已成功完成 Gradle Build、Actions artifact 上传和 GitHub Release 发布。发布资产为 Forge 1.20.1 JAR。

**构建成功不等于游戏内验收通过。** 尚未完成真实 Mojang/MSL 登录、自动继承结果及 FTB 缓存完整性的游戏内验证。部署前应先备份整个世界、配置、TrueUUID 注册数据和权限数据，并在测试服验证背包、末影箱、位置、经验、进度及相关模组数据。

