# 资源生态

洞穴工厂把资源分为本地输入、效果流体、稳定基质和来源残渣四层。

## 输入分类

- 采矿：钕、方铅、铀、渊海石、秽岩、糖晶矿体等本地地质材料。
- 农业与生态：南洋杉树脂、星树果、海雪、生物荧光质、明胶等。
- 生物：恐龙组织、遗弃者相关材料等高风险生物资源。
- 流体与化学：紫色苏打、酸蚀材料、硫粉和后续扩展的化学中间体。
- 回收：签名机故障、过期封装和六维残渣。

## 效果流体

每维只有一种签名效果流体，基准容量单位为 `250 mB`。流体在原生维度内可正常通过 Create 管道和储罐搬运；普通桶没有装填配方。跨维运输的受支持路径是专用封装容器和通用稳定器。

## 稳定基质

- `polarity_matrix`：极性基质
- `genesis_matrix`：原生基质
- `decay_matrix`：衰变基质
- `pressure_matrix`：渊压基质
- `umbral_matrix`：暗相基质
- `crystal_matrix`：糖晶基质

通用稳定器消耗 `4 x 250 mB` 对应流体生成一个稳定基质，并可在任意维度运行。

## 残渣

六种来源残渣分别是 `polarity_residue`、`genesis_residue`、`decay_residue`、`pressure_residue`、`umbral_residue` 和 `crystal_residue`。过期结算返还空封装，并把内容物转成对应残渣。毒化机制链负责按来源回收这些残渣。

## 基础产物自增殖

六维各 3 类基础产物（共 18 项）都有一条不依赖洞穴工厂的 Create 自增殖循环，全部写在 `kubejs/server_scripts/cave_self_propagation.js`。每项有“一主一备”：主链用可再生母料模拟维度生态，备链消耗 `2 个本维残渣 + 1 个可再生辅料` 回收一项产物。

| 维度 | 自增殖主链产出 | 备链残渣 |
| --- | --- | --- |
| 磁场 | 富铁黏液、粗赤/青钕、三色充能方铅岩 | `polarity_residue` |
| 原始 | 石灰岩、南洋杉树脂、星树果 | `genesis_residue` |
| 毒化 | 硫粉、酸蚀辐射石、铀晶碎片 | `decay_residue` |
| 渊海 | 渊海石、生物荧光质、海雪 | `pressure_residue` |
| 异寂 | 秽岩、荆棘木、纯粹黑暗 | `umbral_residue` |
| 糖果 | 冰糖、明胶、紫色苏打 | `crystal_residue` |

主链不消耗目标产物本身，所以玩家从首个野外样本开始就能净增长。备链的残渣来自签名机结算，在五维签名机启用前处于预览状态；约束与验收口径见 [`superpowers/specs/2026-09-27-cave-self-propagation-design.md`](superpowers/specs/2026-09-27-cave-self-propagation-design.md)。
