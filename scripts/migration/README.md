# 一次性存档迁移工具

本目录服务于已获批准的 1.20.1 Forge → 1.21.1 NeoForge 存档保全任务。当前实现备份校验、只读审计及部分离线数据转换规则，尚未完成整个存档转换；禁止据此直接用目标版本打开生产存档。

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

## 注册表预检

```sh
python3 scripts/migration/preflight_registries.py /absolute/private-lab/source-audit.json /absolute/private-lab/target-registry.json --output /absolute/private-lab/registry-preflight.json
```

复用完整只读审计和目标整包运行时注册表，按方块、物品、实体、维度分别比较，不重新扫描或打开原世界。目标快照包含 `minecraft: "1.21.1"` 及 `registries`（注册表 ID 到注册项 ID 数组）。注册项缺失或存在无法判定类型的 Count 记录时，输出私有报告并以 1 退出，阻止把预检当作成功；完整输入、全部所列 ID 存在时返回 0，**仍不证明存档转换通过**。

旧审计中的经验球根记录也有 Count 字段；工具依据源实体注册表及 Entities 根位置单独记账，嵌套 Item 仍按物品统计。物品记录数与 Count 字段合计不代替物流模板外的 Amount、AE2 Key 数量或 Mekanism 化学存储量。其余注册字符串保持未定类型，不按任意注册表中的同名项判定保留。报告保留两份输入 SHA-256，以私有权限创建，拒绝覆盖；不得提交真实报告。

用户已明确舍弃 `caves_delight`、`mekanismdelight`、`kaleidoscope_sauce`、`witherstormmod`。原服务器未发现这四项的实际方块、实体或物品栈，但含 `witherstormmod:bowels` 维度定义。预检继续报告缺失项；后续一次性转换须在新副本中定点处理并记录，不删除同一容器/机器中的其他数据。用户已确认此服务器是唯一旧存档，只针对该封存快照完成转换和对账，不开发任意历史版本或其他单人旧档适配。

## 当前资产保留范围

核心范围为原版、Create、AE2、Mekanism（含官方 Generators）。玩家背包、末影箱、精妙背包及其他存储内的实际物品整体保留，包含其他模组物品；容器、contentsUuid / SavedData 绑定、已有机械用到的附属设备、支撑与连接同样保护。其它内容只在确认不承载这些资产后舍弃，目标 Mod 清单不因此缩减。

```sh
python3 scripts/migration/retention_scope.py /absolute/private-lab/source-audit.json /absolute/private-lab/target-registry.json --output /absolute/private-lab/retention-scope.json
```

工具复用已有只读审计，不扫描或写回原世界。它列出核心和实际使用的附属机械方块、全部已识别物品记录、库存所在的外层实体/方块实体或 SavedData、待清理的其它方块、仍缺少目标注册项的受保护物品与方块。旧审计中的 Count 记录也包括过滤器、模板和展示栈；精确归类前保守保护，不据记录数或 Count 合计计算实物库存。经验球根 Count 不算物品，未知记录仍阻止通过。

其它方块只列 `candidate_for_omission`；没有容器/机械及空间支撑核查之前，`automatic_block_removal_allowed` 始终为 false。清理已舍弃模组外壳时，也必须逐层回收其内部应保留资产，不递归删除内含物。这个报告不等于完整迁移验收，不允许据此直接启动唯一旧档；报告私有、拒绝覆盖，受保护项缺注册或记录未定类型时以 1 退出。

## 工具验证

`entity_data.py` 根据唯一服务器的实际旧能力结构及固定目标 JAR 的持久化格式，将实体 `ForgeData` 转为 `NeoForgeData`、`ForgeCaps` 转为 `neoforge:attachments`。处理 Aether 字段/槽位、女仆数值、Mekanism 辐射、暮色护盾、酿酒状态及 L2 附件；Curios 库存和嵌套数据保留，仍须接入物品组件转换。舍弃的凋零风暴能力、新暮色不持久化的两个瞬时控制状态及新版 L2 不保存的死亡计时器通过 `removed` 返回给私有台账，不静默删除。未知能力、目标字段冲突或结构不符阻止转换。

原版 DFU 将属性改为 `attributes` / `id` / `base` 后，该模块继续映射旧 Forge 重力、游泳速度和两种触及距离，保留数值与修饰器。全档 16,567 条 `forge:step_height_addition` 均是零增量且无永久修饰器，移入台账，让新版实体使用自己的原生台阶基值；非零、未知修饰状态或属性 ID 冲突阻止转换。不把旧“增量为零”写成新版“台阶高度为零”。

该模块只返回新 NBT，不写世界、不更新 DataVersion，也不代表物品、机器或所有玩家已经迁移。箭袋的 `ForgeCaps/Parent` 是物品库存，必须先走独立物品组件转换，不能当作实体附件处理。

`vanilla_dfu.py` / `java/VanillaDfuBridge.java` 编译临时类并复用一个 Java 21 进程执行原版 DFU，固定为本服的 3465 → 3955。类路径不为 1.21.1 时阻止启动；按指定的原生类型转换 NBT，不打开世界。私有日志拒绝覆盖。原版 DFU 不处理所有模组的自定义物品/机器数据，不能只改 DataVersion 就交付存档。

`item_components.py` 把箭袋 Parent 的库存、空槽和选中槽位转为 `supplementaries:quiver_content`，嵌套旧箭交给调用方转换；未知物品能力、重复/越界槽位或组件冲突阻止转换。`authoritative_count` 仅在已确认的 Sophisticated 库存中读取 Int `realCount`，不按 byte 溢出猜测。

Sophisticated 背包和升级的组件使用固定目标 JAR 自带的 `LegacyBackpackDataMigration.normalizeLegacyData` / `LegacyUpgradeDataMigration.migrate`，不在本仓库重复实现其旧字段映射。`java/NativeComponentBatch.java` 只供仓库外的隔离转换夹具使用，不加入整包清单：编译时增加固定 Sophisticated Core / Backpacks JAR，作为临时 Mod 放入带 `dimensionworks-pack-check.marker=isolated-pack-check` 的夹具。输入 `local/migration-component-input.nbt` 的 `format=1`，`rows` 每项包含 `label`、已做原版 DFU 的 `stack`、`normalize_sophisticated` 及 `item_kind`（`backpack` / `upgrade`）。原生组件严格解码、调用作者现有接口，再编码至新建的私有 `migration-component-output.nbt`；完成后移除临时 Mod。它不读生产世界、不能代表完整旧档或所有背包已经迁移。

`ae_data.py` 转换实际旧 AE2 item / fluid 键，类型字段由 `#c` 改为原生 `#t`；物品元数据和能力交给完整物品转换回调，流体数量不改单位。存储元件的 `keys` / LongArray `amts` / Long `ic` 转为 `ae2:storage_cell_inv`，先核对数组长度和缓存总量；未知字段、类型或储量冲突阻止转换。不得绕过物品回调，把尚未处理的 Mekanism、背包或其他物品状态当作已经保留。

AE2 部分 codec 会把无法解析的键变成缺失内容占位项，即使 `getOrThrow` 未抛错。原生批处理递归拒绝 `ae2:missing_content_error` 组件；对账仍须核对每个键的原生类型、ID、组件和 Long 储量，不能只检查加载成功或组件总数。

`mek_data.py` 对照本服固定 Mekanism 10.4.16.80 与目标 10.7.19.85 的持久化字段，将机器、封箱、已打包机器物品和 SavedData 准备成作者原生接口可以读取的结构。物品库存递归交给完整物品转换回调；化学库存保持 Long，流体保持原单位，热量保持 Double，库存 UUID、所有者和升级原值保留。侧面配置保留旧 transmission 编号并转为 IntArray，由作者接口合并新版化学传输配置；配置工具按模式含义映射，不能直接沿用旧序号。未知根字段、能力、化学名称、配置或冲突阻止转换。

纸箱载荷转为 `mekanism:block_data`，其中 `state` 与 `block_entity_tag` 分别交给方块状态和方块实体转换回调；不能只转换外壳。Create Mobile Packages 来源转为已有的 `create_mobile_packages:packager_origin` 组件。机器物品的旧 `mekData` 保留到原生方块 `collectComponents()` 完成后才删除，避免提前丢掉内含库存。安全频率保留所有者，蒸馏塔缓存保留库存绑定和各类存储槽数。

`java/NativeMekBatch.java` 同样仅用于仓库外的隔离夹具，编译类路径增加固定 Mekanism / Sophisticated Core JAR，不加入整包清单。输入 `local/migration-mek-input.nbt` 的 `format=1`、`mode=convert`，`rows` 逐项指定 `label` 和 `kind`：`block_entity` 含准备后的 `data` 与实际 `state`；`item` 含组件栈 `stack`，机器物品另含 `tile_data`；`security_frequency` / `evaporation_cache` 含准备后的 `data`。它只在内存创建临时方块实体，调用作者加载/保存接口，再复读方块实体并比较；不把方块放入世界、不启动旧机器或读取旧档。输出新建的私有 `local/migration-mek-output.nbt`，完成后移除临时 Mod。

原生读写仍需语义对账。新版 Mekanism 将旧 gas / infusion / pigment / slurry 库存合并为 chemical；空库存、默认弹出器或公开安全模式可能不再显式保存，不能按字段数判断丢失。目标热电阻加热器将本服唯一的旧小数配置 `energyUsage="2.5000"` 原生保存为 Long `energy_usage=2`，须进入台账；这不是储能余额。新版暮色极光方块没有旧 `variant` 状态，封箱中的旧颜色值也须记账，方块 ID、载荷和库存继续保留。仅原生解码成功、样本对账或夹具正常保存均不等于整档机械与玩家库存已经验收。

```sh
python3 -m unittest discover -s scripts/tests -p 'test_verify_snapshot.py' -v
/absolute/private-lab/python/bin/python -m unittest discover -s scripts/migration/tests -v
```

NBT 审计测试单独运行，以免给现有无关脚本检查引入不必要的 Python 依赖。
