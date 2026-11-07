# TrueUUID 鉴权网络设置自动生效

## 故障与修复范围

2026-11-07 16:42（北京时间），客户端已送达登录响应，服务端对 Mojang
会话服务的 Java HttpClient 请求在读取响应时发生 `EOFException`。
原实现将网络异常转换为空结果，后续按“会话无效”处理，已绑定正版 UUID
的玩家因此看到禁止离线登录的提示。该保护规则本身需要保留。

现网 MSLX 直接启动 Forge，不读取整合包的 `start.sh` 或
`user_jvm_args.txt`。在面板加入 IPv4 与 authlib-injector 排除参数后，
16:55:39 会话校验返回 HTTP 200，16:55:40 玩家以原正版 UUID 进入。
这证明当时的现场恢复成功，不等同于新模组版本已完成游戏内验收。

独立 Java 17 探针还证明：只为鉴权 HttpClient 配置 TLS 1.2，在不传上述
JVM 参数、加载同一 authlib-injector 的情况下，Mojang 会话接口可以正常
响应。默认 HTTPS 路径与 TLS 1.2 的差异足以支持这次有针对性的兼容修复，
尚不能据此断言故障完全由 IPv6 或 TLS 1.3 单独造成。

## 整合包接入

修复放在现有 `szx-jn/TrueUUID-DimensionWorks` fork，固定版本为
`1.0.9-dimensionworks.2`，整合包清单更新到该版本。Forge 47.4.23、
Minecraft 1.20.1、Java 17、客户端协议、认证决策与 UUID 注册表格式保持原基线。

修复源码：[fc5045c7dd84139d57242f41cd2eeb56a76c44b3](https://github.com/szx-jn/TrueUUID-DimensionWorks/commit/fc5045c7dd84139d57242f41cd2eeb56a76c44b3)。
整合包只引用此版本的固定 GitHub Release JAR；正式资产由 fork 的
Actions 工作流生成，整合包发布流程负责递增补丁版本。

[正式构建 42359927015](https://github.com/szx-jn/TrueUUID-DimensionWorks/actions/runs/42359927015)
已成功，固定标签指向上述源码提交。正式资产为
[TrueUUID JAR](https://github.com/szx-jn/TrueUUID-DimensionWorks/releases/download/v1.0.9-dimensionworks.2/trueuuid-1.0.9-dimensionworks.2-forge1.20.1.jar)，
`111,240` 字节，SHA-256 为
`6aca48c45f5e57a224591f0a5d0d9ae5b5bc1e3c651a2bc8456732dd1fe56726`。
本地验证 JAR 与正式资产的 SHA-256 一致。

- 单例异步鉴权客户端自动使用 TLS 1.2，保持默认信任库与主机名校验。
- 连接超时 5 秒，单次请求超时 8 秒；连接中断或超时最多重试一次。
- HTTP 204 等无效会话响应不重试，证书错误不重试。
- 网络异常向登录流程传播并保留日志原因，客户端显示认证服务连接失败。
- 配置随 JAR 加载，不依赖启动脚本、面板参数或修改全局 Java 网络属性。

## 实际验证

本地 `./gradlew build --offline --console=plain` 通过，24 项测试均无失败，
包括 18 项既有文件事务与 NBT 保全测试，以及 6 项新增 HTTPS 传输测试：TLS 1.2 连接、响应读取中断
后恢复、HTTP 204 不重试、重复中断保持异常、无响应请求超时、拒绝不可信
证书及错误主机名。其中 TLS 协商与中断恢复共用一项测试。

从本地验证 JAR 提取实际 `SessionHttpClient.class`，在远端独立临时目录
使用现场 Temurin `17.0.20.1` 与同一 authlib-injector 执行探针。
`java.net.preferIPv4Stack` 和 `authlibinjector.ignoredPackages` 均为 `null`，
模组客户端协议为 `TLSv1.2`；Mojang 与 MSL 会话接口均返回 HTTP 204 /
HTTP 2。探针使用无效会话编号，204 是预期结果，只证明两种认证服务的
HTTPS 请求能够完成，不证明真实玩家身份校验。临时探针目录已删除。

本地 Forge 1.20.1 JAR 的 Java 17 字节码、SRG mixin 映射、协议与身份
存储基线及 LGPL 许可校验通过；发行工作流 YAML、Python 语法与多行
发布说明检查通过。整合包 `pack_hook.py --skip-build` 的 12 项检查通过。

该版本尚未完成游戏内 Mojang/MSL 登录或迁移验收。连接设置不能保证在
认证服务停机、DNS 错误或网络封锁期间仍能登录；真实网络故障继续保留
鉴权失败和原有 UUID 保护策略。
