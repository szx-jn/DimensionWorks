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

`VanillaDfu.update_entity` 处理实际照片/蜂巢中实体的旧属性：若 entity DFU 未转换 `Attributes`，只将原属性列表交给原生 player DFU 上下文，保持原实体类型、UUID 和其它数据。原生 bee DFU 同时留下新旧两份属性时，须确认新列表与该独立结果一致，才能删除旧副本；冲突阻止转换。`entity_data.py` 拒绝仍带旧属性列表的输入。嵌套物品须从原始记录完整转换后放回同一库存位置，不能再次转换已组件化的中间栈。

`item_components.py` 把箭袋 Parent 的库存、空槽和选中槽位转为 `supplementaries:quiver_content`，嵌套旧箭交给调用方转换；未知物品能力、重复/越界槽位或组件冲突阻止转换。`authoritative_count` 仅在已确认的 Sophisticated 库存中读取 Int `realCount`，不按 byte 溢出猜测。

已审计的酒瓶空 `creategearsandtavern:drink_fluid` 能力及空根能力进入 `removed` 台账；非空能力不套用此规则。`create_data.py` 将过滤器和包裹的槽位库存、容量、地址、黑白名单及 NBT 匹配标志转换为现有 Create 组件，保留空槽位置，嵌套物品递归转换。`mod_item_data.py` 将女仆照片实体、酒品质及料理记录转换为固定目标作者的原生组件；料理输入顺序、重复项、产物和配方类型保留，照片内实体与库存交给完整回调。

`registry_ids.py` 只提供已核对的官方升级注册名：原版 grass、旧 Miner's Delight / Brewin Compatibility Delight 名称及 ExtendedAE 设备/物品，仍要求目标运行注册表存在。唯一旧无限元件须完整匹配圆石记录，才映射为作者的 `extendedae:infinity_cobblestone_cell`，旧记录进入台账。未知记录或缺失注册项不猜测，不替换为空气。

Sophisticated 背包和升级的组件使用固定目标 JAR 自带的 `LegacyBackpackDataMigration.normalizeLegacyData` / `LegacyUpgradeDataMigration.migrate`，不在本仓库重复实现其旧字段映射。`java/NativeComponentBatch.java` 只供仓库外的隔离转换夹具使用，不加入整包清单：编译时增加固定 Sophisticated Core / Backpacks / Storage JAR，作为临时 Mod 放入带 `dimensionworks-pack-check.marker=isolated-pack-check` 的夹具。输入 `local/migration-component-input.nbt` 的 `format=1`，`rows` 每项包含 `label`、已做原版 DFU 的 `stack`、`normalize_sophisticated` 及 `item_kind`（`backpack` / `storage` / `upgrade`）。原生组件严格解码、调用作者现有接口，再编码至新建的私有 `migration-component-output.nbt`；完成后移除临时 Mod。它不读生产世界、不能代表完整旧档或所有背包已经迁移。

该夹具可选接收 `default_item_ids` 和 `export_block_state_schema`，输出所选物品的原生默认组件及全部方块的属性允许值/默认状态，用于识别默认字段省略及检查旧机械方块状态。只读注册表，不放置或运行第三方机器。

`ae_data.py` 转换实际旧 AE2 item / fluid 键，类型字段由 `#c` 改为原生 `#t`；物品元数据和能力交给完整物品转换回调，流体数量不改单位。存储元件的 `keys` / LongArray `amts` / Long `ic` 转为 `ae2:storage_cell_inv`，先核对数组长度和缓存总量；未知字段、类型或储量冲突阻止转换。不得绕过物品回调，把尚未处理的 Mekanism、背包或其他物品状态当作已经保留。

`key_identity` 按 NBT 类型、组件及列表顺序比较键，只忽略 Compound 的字段顺序。若不同旧编码转为同一原生键，合并其 Long 储量并检查溢出；不同状态或类型不合并。实际 7 个元件的 439 个键未发生合并，已完成离线原生逐键及 Long 对账（详见执行记录），仍须接入整档与实际保存重启验收。

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


## 递归库存转换与严格原生对账

`item_pipeline.py` 按完整原始 NBT 元数据缓存单位模板，真实数量、槽位与 Sophisticated `realCount` 在每次转换时独立恢复。嵌套物品需要作者迁移接口时，先完成子项，才生成外层过滤器、容器或元件；未处理的无线终端、通用元件与移动机械明确阻止转换，不返回空物品。缓存中的原生结果必须来自已独立核对的原始记录，注册名、数量或同一元数据的结果冲突均阻止接入。

`block_states.py` 使用固定目标原生状态目录保留方块注册名与每项现有属性。仅接受已核对的作者移除项：暮色极光的旧 variant、火蝇/蝉的 false waterlogged 和下界粉状甘蔗的 false leave/base；移除项进入私有台账，不能以目标默认值吞掉未知属性。实际 5,892 种旧状态已对照目标 6,362 种方块目录通过；这不等于机器放置或运行验证。

`potion_data.py` 核对旧 Miner's Delight 与 L2 固定资产的药水效果、时长、等级，再映射作者新预设。原药箭和自定义效果、数量、颜色保留；已舍弃凋零风暴的三种药水本来只应用原版凋零效果，转换为等时长/等级的原版自定义效果，并在 custom_data 与私有台账保留源药水 ID，不新增替代 Mod。未核对的药水或效果、变更后的目标预设阻止转换。`registry_ids.py` 同样严格核对附魔：收割者按作者更名保留等级，生命攫取没有等价更名，等待用户取舍，不能换成另一附魔或静默移除。

新增女仆智能收容块、熔岩背包、棋谱与 Patchouli 书籍组件转换；女仆实体原本没有 id 时，只在独立 DFU 上下文提供类型，持久化结果保留原来是否有该字段。Create 序列装配的配方、步骤和 Float 进度保留；矿车机械必须提供完整载荷转换回调。空灵魂风帆能力只在完整匹配 `{Parent:{}}` 时记账处理，非空数据阻止转换。

原生组件桥接检查物品 ID、数量、再次编码结果及完整有效组件的原生编码。含 ItemStack 的作者组件可能以对象身份实现 Java equals，不能把这种差异误报为存档丢失；仍要求完整持久数据和有效组件重新编码相等。可选 `collect_errors=true` 仅用于诊断，必须提供私有 `diagnostic_id`：输出 `complete=false`、逐行错误和完整私有异常日志后仍以失败结束。默认失败即停止；只有 `complete=true`、全部行完整且外部源数据对账通过，才可将结果用于下一阶段。`export_potion_effects` 可导出固定原生预设证据。

`ae_items.py` 将量子奇点频率、无线终端能源/链接/升级/合成槽位及界面设置写入目标原生组件。通用终端保留安装模块、控制模式和奇点物品；通用元件 UUID 字符串按原值转为 IntArray，实际库存另存的 SavedData 不以提示物品替代。`auxiliary_items.py` 转换实际古卷/附魔模板、暮色地图、两种羽毛笔和时钟状态。原版 DFU 对附魔余留字段已做更名时，比较完整转换后的等级表，不能直接比较旧字符串，也不能把留在 custom_data 中的旧字段当作新版仍在使用。

当前 106 项 Python 规则测试通过；实际索引的 2,731 种单位元数据中，2,715 条通过原生组件保存/重读，包括作者接口处理的 45 种背包和 25 种升级元数据。余下 6 条为两种矿车机械物品及四条生命攫取相关记录，另有 10 种空 air 模板。矿车载荷已完成下述独立原生读写，仍须装回物品组件；生命攫取等待用户取舍。这些是元数据模板，不是物理库存数量；仍须完成全部背包 SavedData、其它存储、真实玩家库存和完整世界保存/重启对账，不能据此交付存档。

`contraptions.py` 递归转换原装置的全部方块、方块实体、执行器和挂载存储，不重新生成空机械。`java/NativeContraptionBatch.java` 仅在标记隔离夹具中调用固定 Create 的 `Contraption.fromNBT/writeNBT`，核对方块数并重新读取保存结果，不放置方块或生成实体。矿车物品另有 `InitialOrientation`，桥接保留该字段。`assert_contraption_preserved` 以实际相对位置核对状态及完整持久数据，再核对其余全部根字段；坐标 Compound 与 IntArray 只在比较时按相同值规范化，不改写实际输入。

唯一旧档的 8 个装置、694 个方块已完成上述原生重读及外部完整载荷对账。作者写回省略的 24 份旧执行器 Inventory，19 份为空，5 份逐项证明是同位置执行器手持槽的重复记录；原始列表进入私有台账。其它非空槽、持物不一致、坐标/所有者/挂载库存改变均阻止验收。客户端 UpdateTag 是派生数据，不计为独立实物库存；完整持久 Data、执行器持物和挂载库存仍必须保留。这里只证明装置载荷，不能等同于全档机械运行与保存重启已完成。

### 源索引覆盖与完整存储对账的当前口径

上面的 2,715 / 10 是早期批次统计，现已纠正：批次包含递归子项，不能用批次行数相减计算未处理父项或空物品。后续 16 行批次完成两个矿车组件、带实体效果的物品、一个 MegaCells 元件以及六种嵌套背包状态；重新逐项调用全部 2,731 条原始索引后，**2,724 条可用原始模板完整转换，3 条是真实空 air，4 条生命攫取相关记录仍待用户取舍**。检查每个原始模板，而不是仅统计去重缓存数。

`ItemPipeline.player` 从原始记录递归恢复玩家库存。实体/玩家的 `ActiveEffects` 使用 Forge 保存的字符串效果 ID，不沿用可能变化的旧数字 ID；时长、等级、显示标志及本服实际牛奶/图腾治愈行为接入目标原生格式，源列表进入台账。治愈描述中的 CurativeItems 不计作库存。未知效果字段或治愈物阻止转换，85 种实际效果状态已通过固定原生 MobEffectInstance codec 与治愈集合检查。

`convert_universal_cell_data` 处理 UUID 文件里的真实通用元件库存，保持 Long 数量并明确记录相同目标键的合并；读取错误、未知结构、溢出均阻止通过。原生 AEKey 保存的是显式组件，不能套用 ItemStack 默认组件合并不同 AE 键。实际两个旧白桦木板键只差一个空 tag，188 + 48 合并为 236；五个文件的 109 个原记录成为 108 个原生键，总量 5,267 保留，UUID/文件路径不变。元件上的类型缓存必须在接入整档时与这份真实库存对齐，不能把提示栈或缓存当作库存。

`java/NativeStorageBatch.java` 在带标记的新世界夹具中读写 50 份完整 BackpackStorage、实际状态对应的 9 个精妙存储方块实体和 5 份通用元件 SavedData。背包通过作者包内 contents-source 接口提供独立内存数据，未装入夹具全局世界存储；实际 InventoryHandler/UpgradeHandler 核对全部槽位和容量。通用元件由散列表写入，比较完整键/Long 数量及其它元数据，允许条目顺序变化。原生结果仍须与原始记录逐项独立对账。

当前已完成完整背包/精妙存储载荷、身份绑定、库存主槽/升级槽、元数据及容量对账：背包 1,176 栈、2,313,098 件，精妙存储 292 栈、1,319,841 件；1,357 个 realCount 和 71 处负 byte 数量按原值恢复。这里只统计这些主槽和升级槽，不是全服实物总量，内嵌元件、过滤模板等另按完整状态对账。九个默认 false keepPacked 不再显式保存，原值进入台账。

`java/NativePlayerBatch.java` 使用实际 UUID/名称的内存 FakePlayer 检查完整玩家/末影箱/Curios 库存，不代表网络登录。必须在 load 前初始化目标槽位，禁止读取后 reset。首次夹具未应用仓库现有 Curios 配置，charm 从 2 格退回默认 1 格，暴露一个真实已占用第二槽；同步现有 `config/curios-common.toml` 的普通槽位 3 格设置后，11 份玩家数据、65 份原槽位状态及全部原物品/槽位对账通过。FakePlayer 保存结果不作为最终世界玩家文件；最终仍用完整转换的原始玩家数据，避免验证对象改变生命值、游戏模式等非库存状态。

当前 Python 规则 **114 项通过**。生产封存原档未打开，完整世界生成、实际机器/网络接入、真实登录及整档保存/重启仍未完成；这些批次通过不能作为整档交付。
