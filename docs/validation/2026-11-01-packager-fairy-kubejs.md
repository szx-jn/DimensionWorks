# 纸箱、女仆妖精与 KubeJS 远程验证

日期：2026-11-01（北京时间）。测试在远程服务器独立实例 `/opt/dw-runtime-validation` 进行，绑定 `127.0.0.1:25585`，使用新建世界；正式服 `/opt/mc` 不参与测试世界修改。

## 构建与运行基线

- Forge 1.20.1 / 47.4.23，Java 17，正式服同一组模组与配置副本。
- Create Mobile Packages fork 测试提交：`09c2b9b`。
- GitHub Actions 测试构建：`41282304043`，构建成功。
- 测试 Jar SHA-256：`23b7fdd1f5f7c693b85dfe7d613c9f5c8c310bcbabffdfe39e23e25aa86106c0`。
- 正式 Release Jar 与测试 Jar 的全部文件内容一致，仅 MANIFEST 的构建时间戳不同。
- 对照测试 `.10` 复现原地拆包、掉落搬运后拆包返还空箱的问题；普通 Mekanism 纸箱返还正常。

## 修复后结果

使用 `scripts/integration/remote_runtime_validation.js` 临时放入隔离实例的服务端脚本目录，实际执行服务器 Java API。最终运行 28 项断言，全部通过：

- 女仆妖精按 NATURAL、SPAWN_EGG、SPAWNER、COMMAND、CHUNK_GENERATION 原因调用生成 API 后，世界中均无妖精；直接添加和召唤命令亦被拦截。
- FakePlayer 实际使用妖精刷怪蛋未产生妖精，普通鸡能正常生成。
- 先关闭限制生成带 `dw_persistence_probe` 标记的妖精，确认实体存在并保存世界；恢复限制后重启，妖精在区块加载时被拦截。
- 工具打包石头后，来源标记能保存/读取；原地拆包恢复石头且无空纸箱。
- 使用实际方块掉落计算取得打包物品，重新放置并拆包后恢复石头且无空纸箱。
- 普通 Mekanism 纸箱仍返还一个空纸箱；打包箱子后七颗钻石保留，且不返还空箱。
- 原有五种 Mekanism 代表配方、自定义二氧化碳配方及 AE2 128³ 空间存储元件配方均存在于服务端配方管理器。
- KubeJS startup/server 脚本均加载成功，服务端脚本日志无 WARN/ERROR，配方加载为 `0 failed recipes`。

远程证据：`baseline-cmp10.log`、`validated-cmp11-first.log`、`persistence-seeded.log`、`validated-cmp11-restart.log`，均位于隔离实例目录。

## 边界

这些是专用服务器的运行集成测试，不包含客户端交互画面检查。SPAWNER/NATURAL 测试调用真实生成 API 并指定生成原因，没有等待刷怪笼或随机自然生成调度器自行刷怪。第三方模组仍有独立的进度、掉落表、Curios 和 Mixin 兼容日志，不应将本次 KubeJS 日志干净解释为所有第三方模组均无问题。旧版本已经彻底丢失来源标记的纸箱物品无法自动恢复来源。

## 下界岩 12% 调整复核

将第二组概率从 80% 提高为 89% 后，远程隔离服重新运行验证。直接从服务端配方管理器读取粉碎配方的 `getRollableResults()`，按物品数量乘以概率求和，验证 `4.48 / 4 - 1 = 12%`（浮点容差 1e-6）。连同原有用例共 29 项断言全部通过，KubeJS 日志无 WARN/ERROR，`0 failed recipes`。远程日志保存为 `validated-cmp11-net12.log`。
