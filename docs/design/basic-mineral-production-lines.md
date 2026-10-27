# 基础矿物增殖：当前配方矩阵

适用版本：Minecraft 1.20.1 Forge；Create 6.0.8；Mekanism 10.4.16.80；KubeJS 2001.6.5；KubeJS Create 2001.3.0；KubeJS Mekanism UNOFFICIAL 2001.1.5.1。

## 当前设计边界

铁线已改为 Create → Mekanism 串联产线；其他矿物仍保留两条旧的临时独立配方，后续按讨论结果逐条重做。通用矿物残渣设计已淘汰，不再注册 kubejs:mineral_residue 或 residue_recovery 配方。

## 铁矿增殖

1. **制粉 — Create 磨石**：粗铁 ×1，必定产出铁粉 ×2，并有 70% 概率额外产出铁粉 ×1。因此得到 2 个铁粉的概率为 30%，得到 3 个的概率为 70%。
2. **Create 机械压制**：粗铁 ×1 + 安山合金 ×2 → 铁质活化料 ×1。
3. **Mekanism 结合器**：铁质活化料 ×1 + 铁粉 ×1 → 粗铁 ×2。

选用磨石而不是粉碎轮，是为了避开 Create 原生的粗铁粉碎轮配方。铁粉使用统一研磨 API 注册概率产物；自定义中间物需在完整重启后加载。

## 统一粉碎 / 研磨 API

实现位于 kubejs/server_scripts/crushing_api.js，入口为 global.DimensionWorksGrinding.register(event, recipe)。当前支持 Create 粉碎轮、Create 磨石、Mekanism 粉碎机，以及 Create: Enchantment Industry 机械磨石。CEI 的独立 grinding 类型允许最多四个物品结果和单个流体输入或输出。Mekanism 粉碎适配器只接受固定单产物，不会静默吞掉概率设置。完整语义审计见 docs/design/crushing-api-mod-audit.md。

## 其他目标

铜、锌、金、煤、红石、青金石、石英、紫水晶、锡、铅、锇、萤石仍是早期临时设计，尚未完成 Create → Mekanism 连续产线重构。钻石、绿宝石、远古残骸和铀的增殖仍暂缓。

## 静态验收

- 检查脚本语法、目标列表和配方 ID 唯一性。
- 检查铁矿配方链和概率配置。
- 检查没有 mineral_residue 或 residue_recovery 残留引用。
- 检查已有 Create crushing/milling 调用没有绕开统一入口。
- 游戏内仍需核对配方解码、JEI 显示、实际扣料与自动化运行；静态检查不能替代实测。
