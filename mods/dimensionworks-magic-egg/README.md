# 魔法皮蛋效果界面

Minecraft 1.21.1 / NeoForge 21.1.267 / Java 21，自研模组 `dimensionworks_magic_egg`，客户端和服务端均需安装。仅负责界面、确认效果与保存选择，不新增物品、配方、创造页签或第三方依赖。

面板、标题与说明在 1.21.1 的 `renderBackground` 阶段绘制，随后由原生 `Screen.render` 绘制控件，避免背景模糊覆盖自定义内容。跨模块图形回归入口见 Transfer 的 `-PtransferClientCheck=gui`。

## 行为与界面

- 保留 `kubejs:magic_century_egg`、原皮蛋禁用规则及现有制作链。KubeJS 在成功食用且没有死亡倒计时时调用界面；倒计时内再次食用仍立即死亡。
- 所有效果固定 V 级，持续效果改为 10 分钟；瞬时效果在确认时立即应用。确认后同时启动 10 分钟死亡倒计时，死亡及烟花粒子沿用 KubeJS。
- 倒计时只在当前服务端运行周期内有效：服务端重启后玩家首次登录会清除旧计时；死亡重生及倒计时触发的处决都会清除 `pending/deadline`。上一次效果选择单独保存，仍可跨死亡、重连和重启继承。
- 界面使用代码绘制面板、文字按钮和勾选框，没有物品槽，不新增 GUI 贴图。支持效果名称/模组 ID 搜索、增益/中立/减益分类、滚动多选、仅看已选、清空，以及悬停查看完整名称和 ID。宽屏显示两列，窄屏显示一列；Tab 导航，方向键、PageUp/PageDown、Home/End 可滚动（搜索框有焦点时保留文本编辑功能）。
- 首次食用进入空选择。已有上次确认记录时，先询问“是否继承上一次的选择”，并列出上次效果供滚动查看。“是”直接应用上次选择并启动倒计时；“否”进入空白选择界面，确认后更新记录。
- 关闭或取消不返还皮蛋，不给予效果，不启动倒计时，也不覆盖上次记录。确认空选择仍会启动倒计时，并记录空选择。
- 选择记录存放于玩家 NeoForge 持久化数据的 `PlayerPersisted.dw_magic_century_egg_last_selection`，保存效果 ID 列表；死亡复活、退出重连与服务器重启后保留。更新后已移除的效果不再继承。此前箱子界面没有存储记录，升级后的首次确认才开始记录。

## KubeJS 接口

```js
const $DWMagicEggSelection = Java.loadClass('dev.szx.dimensionworks.magicegg.MagicEggSelection')
$DWMagicEggSelection.open(player, player.persistentData)
```

`open(ServerPlayer, CompoundTag countdownData)` 必须在服务端线程调用；只应从魔法皮蛋的食用事件调用。界面不暂停单人世界。第二个参数必须是原食用事件的 KubeJS `persistentData`；它与 NeoForge 的 `getPersistentData()` 是不同数据。服务端只有收到有效会话的确认后才向这份 KubeJS 数据写入原有 `dw_magic_century_egg_pending` 和 `dw_magic_century_egg_deadline`，持续时间常量为 `MagicEggSelection.DURATION_TICKS = 12000`，与 KubeJS 使用的玩家 age/tickCount 对齐。

每次食用生成独立会话，效果 ID 必须来自该次打开的注册表；继承读取服务端记录，不信任客户端提交的历史列表。会话一次有效，关闭、退出、玩家替换与服务端关闭时清理。NeoForge 载荷协议为 `2`，网络集合上限为 4096 个效果；只提供目标版本网络接口。效果实例使用 1.21.1 Holder，历史仍按注册 ID 保存，离线迁移必须保全原 `PlayerPersisted` 记录。

搜索、分类、滚动和勾选只在客户端处理；服务端仅在打开时枚举注册表、确认时保存和应用效果，不新增每 tick 扫描、实体或区块加载。到期死亡及烟花继续复用现有 KubeJS tick。

## 开发验证

使用 Java 21，在仓库根目录执行 `./gradlew :dimensionworks-magic-egg:build :dimensionworks-magic-egg:runGameTestServer --no-daemon`，本地 JAR 仅用于开发验证。已通过 7 项 JUnit 回归；JUnit 覆盖会话隔离、无效 ID、重复提交、服务端继承、重新选择、空选择、NBT 保存重载及玩家替换后的历史保留。整合包原有静态契约检查为 `python3 -m unittest scripts.tests.test_magic_century_egg`。

`scripts/integration/magic_century_egg_regression.js` 仅用于独立测试世界，尚需同步 NeoForge + KubeJS 接口，验证食用、确认、取消、继承、10 分钟持续时间与再次食用死亡；不随包分发。客户端还需实际检查不同 GUI 缩放下的文字、搜索、多选、滚动和继承提示。构建和单元测试通过不代表已完成游戏内验证。

实际 NeoForge 服务器 GameTest 2 项通过：无效 token 被拒绝、有效确认才给予 V 级 10 分钟效果并保存历史与原倒计时、重放被拒绝，以及超限载荷在分配列表前被拒绝。测试使用 NeoForge 的模拟连接配置，仍需实际客户端界面、KubeJS 食用事件、取消/继承和到期死亡联合验收。
