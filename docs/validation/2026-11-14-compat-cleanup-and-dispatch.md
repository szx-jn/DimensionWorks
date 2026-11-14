# 暮色终端升级、结晶修复与历史兼容清理验证

本次工作从 2026-11-14 开始，完整整包运行验证在 2026-11-15 完成。发布前根目录 `VERSION` 为 `0.3.49`；正式版本由发布工作流自动递增，本地没有预先修改整合包版本。

## 修改范围

- 暮色收尾只保留已实现的弹力打包工具与本次终端升级。升级卡用一块 `#twilightforest:logs` 和任意三个击杀 Boss 奖杯无序合成，允许重复，谜题羊奖杯不计入。升级仅作用于自己的个人终端，消耗一张卡；六洞穴使用独立目的地页面，发运检查包裹、会话、距离和升级状态，抵达后解包。
- 世界熔液在真实液体方块放置后登记计时，避免依赖桶或管道没有触发的预放置事件。连续直见天空 600 tick 后转换为对应结晶流体，保留源/流动状态；遮挡和重新倾倒重置计时。
- 敞口工作盆的两个输入槽分别记录连续 600 tick 暴露时间。补液、换液、清空和关盖重置对应计时，当前计时随盆保存。600 mB 结晶熔液结算三颗结晶，剩余量回到普通熔液；停止、抽真空和无设备属于无压力，三档加压使用实际轴速。输出堵塞不消耗原料；第一种金属输出受阻时继续检查第二槽。
- 删除暮色浸出/野猪调理原型的 Java、界面、注册、配方、标签、语言、贴图、专项测试与生成脚本。旧设计文档归档，宣传源工程移除对应素材引用并同步两项升级。
- 删除 Gear Heart 旧物品识别、效果版本标记与迁移、旧根标签读取、ME 内存卡/物品/方块的世界与背包迁移扫描、旧能量字段清理、三参数同步重载、旧研磨 API 别名，以及更新器不用的 Go 分段/续传下载路径。当前低损耗卡直接使用 `low_loss`，不保留 `high_storage` 别名。未签名更新器状态拒绝读取；客户端与服务端版本须一致。
- `AGENTS.md` 写入 1.0.0 正式发布前一律禁止向后兼容的约束。正常的新数据初始化、当前格式读写、真实第三方 API 对接、首装和事务回滚继续属于当前实现，未当作旧版本兼容删除。

自研模块版本：Transfer `0.3.14`、Cave Factory `0.1.7`、RPM Limit `0.4.22`、MEK Stress `0.3.30`、Pressure Crystals `0.1.2`、Version Gate `0.1.4`；清单、Gradle 和模组元数据一致。新卡通过 `CFItems` 中的全仓命名空间收录进入 `dimensionworks_cave_factory:main`，没有新增创造模式页签。

## 本地构建和运行结果

本地使用 Java 17、Gradle 8.8。所有 16 个自研工程逐一完成 `test build --no-daemon`，共 299 项 JUnit，通过且无失败。后续删除驱动器旧字段处理后，对 MEK 模块重新构建并重跑对应 GameTest，通过。

| 运行检查 | 结果 | 关键覆盖 |
|---|---|---|
| RPM Forge GameTest | 30 项通过 | 当前玩家持久数据、七档修复、装备效果、真实动力核算与保存恢复 |
| MEK Forge GameTest | 11 项通过 | 真实 AE 节点、即时应力分配、卡片、过载、停机及恢复 |
| 暮色/Alex Forge GameTest | 9 项通过 | 重复/混合奖杯合成、错误材料拒绝、所有权、重复安装、保存读回、伪造/失效会话、六个真实维度的安全到达及天境限制 |
| 世界结晶 Forge GameTest | 9 项通过 | 五项现有工厂回归，加上全部 21 种熔液的真实放置、桶倾倒、遮挡/重新倾倒、流动液位保留 |
| 工作盆/压力 Forge GameTest | 17 项通过 | 21 种金属 × 四个压力结果共 84 组；真实电机/大齿轮/压缩机传动、全部金属敞口结晶、完整高压循环、真实盆盖、两槽重置、保存读回、599/600 mB 边界、流动输入及输出堵塞恢复 |
| 原生传送客户端 | 通过 | 367×250 实际 GUI 视口中的未升级/升级页面、六洞穴图、按钮边界及返回操作 |
| 原生结晶客户端 | 通过 | 21 个源流体、42 个动画贴图、桶和储罐进出，未注册固体结晶金属物品 |
| 仓库 Python 测试 | 99 项通过 | 现有发布、清单、增量、资源和配方契约检查 |
| Go 更新器 | 通过 | 全套内嵌 aria2 单测、竞态检查、`go vet`、五个目标的交叉编译；macOS 原生端到端更新验证 8 MiB 下载、签名状态、文件内容与清理 |
| 宣传工程 | 通过 | TypeScript、素材生成、Studio 编译与 HTTP 页面、60 个连续镜头及重新实测的字幕时间；不重新导出全片 |

专项入口均只在显式选项下启用，不成为每次提交或发布的昂贵前置检查。示例：

```sh
gradle -p mods/dimensionworks-transfer -PalexDispatchCheck -PtransferTestModsDir="<mods目录>" -PtransferGameTestNamespaces=dimensionworks_alex_dispatch runGameTestServer --no-daemon
gradle -p mods/dimensionworks-cave-factory -PcrystalCheck -PcrystalMods="<mods目录>" runGameTestServer --no-daemon
gradle -p mods/dimensionworks-pressure-crystals -PpressureCheck -PpressureMods="<mods目录>" runGameTestServer --no-daemon
gradle -p mods/dimensionworks-mek-stress test build runGameTestServer --no-daemon -x downloadAssets
```

新增专项测试源集不进入正式 JAR。运行世界、日志、截图与本地验证产物都在忽略目录，没有上传为正式发行产物。

## 完整整包验证

按照发布清单搭建独立 Forge 47.4.23 服务端，装入 13 个刚构建的自研服务端 JAR 与 135 个固定第三方 JAR，使用仓库的配置、默认配置和 KubeJS。Modrinth 文件核对固定版本下载摘要，所有 JAR 检查归档完整性。仅监听本机回环地址，使用可丢弃世界，不连接或更新 `/opt/mc`。

完整组合正常到达 `Done`，执行保存并正常关闭；启动和服务端脚本均加载成功。整包初次验证发现第三方牛油果掉落表使用失效条件、食物标签含未注册项、茶壶掉落函数未注册、部分进度引用错误。通过当前格式的数据覆盖和标签修改修正：树叶使用 `minecraft:any_of`；茶壶使用原版 NBT 拷贝携带流体和容器，不拷贝会另行掉落的输入物品；进度使用当前物品/效果及原版动作条件。没有注册旧名称桥接这些数据。

完整整包使用真实已重混淆 JAR 和流体能力接口完成原生功能验证：升级配方实际加载，五档低损耗卡存在且旧名称未注册，退役暮色方块未注册；两种牛油果叶剪刀掉落正确；茶壶掉落携带 500 mB 流体并能重新载入，不复制另行掉落的输入物品；全部 21 种世界熔液实际转换；敞口工作盆从 1,000 mB 普通熔液完成结晶与无压力析出，产出三颗无用结晶并剩余 400 mB 普通熔液；覆盖工作盆的 1,000 mB 铜熔液保持不变。14 个启动脚本和 25 个生产服务端脚本加一个本地测试脚本均加载无脚本错误，保存及退出成功，进程返回 0。日志为 `build/local-validation/full-pack/server-native-functional-pass.log`，检查结果为对应测试目录的 `kubejs/native-functional-results.json`。

## 证据与边界

本机日志保留在 `build/local-validation/` 与各模块的 `build/`。完整组合仍会记录固定第三方 JAR 中未安装扩展的资源引用、Mixin 元数据和 Rhino 客户端类型反射诊断；原生注册表确认相应可选物品/方块不存在，这些信息没有当作本次模组测试成功的证据，也没有通过修改第三方 JAR 或新增旧名称掩盖。

本次没有验证 Mojang/MSL 在线认证、多名真实玩家同时操作、长时间大规模工业产线或所有第三方玩法。自动化构建、单测、GameTest、单客户端及短时整包运行各有覆盖边界，不能据此宣称整个整合包绝对不存在未发现的 bug。

清理前检查点保留在 `/Users/szx/Desktop/project/DimensionWorks-pre-compat-cleanup`，并保留 `codex-pre-compat-cleanup-2026-11-14` stash，便于对照和本地回档。
