# 一次性存档迁移工具

本目录服务于已获批准的 1.20.1 Forge → 1.21.1 NeoForge 存档保全任务。当前实现备份校验与只读审计，尚未实现存档转换；禁止据此直接用目标版本打开生产存档。

服务器停机并确认没有 Minecraft Java 进程、监听端口或控制器自动拉起后，制作冷备份。备份包应含当前世界、身份映射、团队、配置、脚本、模组与依赖，并提供：

- `inventory.json`：每个普通文件的 `path`、`size` 和 `sha256`。
- `complete.json`：压缩包名称、大小、SHA-256，清单 SHA-256、文件数量及总大小、原版本，并明确 `source_unchanged` 与 `cold_checked_before_and_after` 为 true。
- 完成报告所指的 `.tar.gz`；源目录制作前后的逐文件校验必须一致。

用户名、UUID、认证配置和整个存档都是私有测试材料，保存在仓库外；不得提交快照、审计报告或测试世界，也不要将完整报告贴进日志。

## 校验及解包

```sh
python3 scripts/migration/verify_snapshot.py /absolute/private-snapshot /absolute/verified-source
```

先校验压缩包与清单，再逐文件验证并写入新目录；成功后才将 `.partial` 目录改名交付。拒绝覆盖已有目标或未完成副本；失败目录需要调查，不能当作正常存档使用。路径逃逸、链接、清单漏项或校验失败都会阻止交付。该工具验证报告与载荷的一致性，报告中的停机证据仍须来自实际源端检查。

## 只读全档审计

在仓库外建立专用 Python 环境，并固定依赖：

```sh
python3 -m venv /absolute/private-lab/python
/absolute/private-lab/python/bin/pip install -r scripts/migration/requirements.txt
./gradlew :dimensionworks-drop-lifetime:writeMigrationClasspath --no-daemon
/absolute/private-lab/python/bin/python scripts/migration/export_ftb_snbt.py /absolute/verified-source/world /absolute/private-lab/ftb-snbt --classpath build/migration/classpath.txt
/absolute/private-lab/python/bin/python scripts/migration/audit_world.py /absolute/verified-source/world --ftb-snbt-cache /absolute/private-lab/ftb-snbt --output /absolute/private-lab/source-audit.json
```

配置 Java 21 JDK 后，导出器复用已锁定的 Minecraft/FTB 类路径，仅调用 FTB 自身解析器读取带注释、可省略逗号的扩展 SNBT；编译结果只存在于临时目录。缓存的每个 NBT 同时记录原文件与缓存文件 SHA-256，不写回原文件，缺失或不匹配阻断审计。

扫描所有维度的 `.mca`、NBT `.dat`、FTB `.snbt` 及世界 JSON。区域文件支持旧档 gzip/zlib/raw 与外置 `.mcc`，允许最后一个完整 payload 所在扇区没有补齐空字节；遇到真实截断、坏扇区、重叠扇区、未知压缩、损坏 NBT 或身份不一致，记录错误并返回失败。它不会启动游戏、写回存档或删掉未知内容。

报告包括实际方块数量、实体与注册字符串、嵌套物品栈、玩家 UUID/位置、物流频道所有者与端点、云盘所有者库存、物流物品的 `Amount/Maximum/Origin`、流体和升级数，以及凋零风暴引用。已安装注册快照与实际引用分列；`trueuuid-backups` 历史文件和未写入的零字节区域占位文件单独记账，不重复计算为现役库存。普通物品栈和物流模板分开记录，不能用模板 `Count=1` 对账真实库存。原始 SNBT 指纹用于定位变化；NBT→组件转换后仍须由转换器进行语义对账，不能仅比较原始指纹或总堆栈数量。

所有已成功校验的原快照和原始解包目录应封存。Minecraft 只允许打开另建的工作副本；先运行原版 Forge 参考测试服，再验证目标 NeoForge 副本，保留进程日志、迁移台账与停止后的对账报告。仅有启动 `Done`、编译成功或无错误退出码均不代表存档迁移通过。

## 工具验证

```sh
python3 -m unittest discover -s scripts/tests -p 'test_verify_snapshot.py' -v
/absolute/private-lab/python/bin/python -m unittest discover -s scripts/migration/tests -v
```

NBT 审计测试单独运行，以免给现有无关脚本检查引入不必要的 Python 依赖。
