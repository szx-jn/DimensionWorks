# 下界合金碎片：混合金属与特质赋予

## 工艺路线

### 1. 化学冶金：制备无特质混合金属

Create 工作盆加热搅拌，输入：

| 输入 | 数量 |
|---|---:|
| Mekanism 液态硫酸（`mekanism:sulfuric_acid`） | 250 mB |
| Mekanism 液态二氧化硫（`mekanism:sulfur_dioxide`） | 250 mB |
| 铁锭 | 1 |
| 金锭 | 1 |
| 铜锭 | 1 |
| 末影粉（`ae2:ender_dust`） | 1 |
| 坚固板（`create:sturdy_sheet`） | 1 |

产出 1 个 `kubejs:mixed_metal`。这是无特质中间物，配方不读取或写入金属 NBT。

两种液体均使用 Mekanism 10.4 原生流体注册与资源贴图，不添加重复流体。Mekanism 原生旋转浓缩器提供气体到液体的转换配方；二氧化硫由该机器将 `mekanism:sulfur_dioxide` 气体冷凝为同 ID 的流体。硫酸直接使用 Mekanism 液态硫酸。两者都能经 Mekanism 流体管道输入工作盆，无需自定义 Java 集成。

### 2. 特质赋予与最终成型

Create 序列组装以 1 个无特质混合金属为起始物：

1. 机械手装配 1 个 `dimensionworks_pressure_crystals:high_pressure_crystal`，要求部分 NBT 精确匹配 `DimensionWorks.Metal = "netherite"`。
2. 机械压力机完成定型。

中间物 `kubejs:incomplete_netherite_scrap` 写入 `DimensionWorks.Metal = "netherite"`，最终产出 2 个 `minecraft:netherite_scrap`。每件混合金属只消耗 1 颗下界合金高压结晶。

## 设计边界

- 只使用下界合金高压结晶赋予特质；其他 20 种金属结晶不参与本配方。
- 铁、金、铜、AE2 末影粉、坚固板和两种化学液体均为真实消耗项。
- 末影粉直接使用 AE2 原生物品 `ae2:ender_dust`，不再经过不存在的末影锭压印与充能步骤。
- Mekanism 原生硫酸、二氧化硫流体的贴图与 Rotary 转换配方由 Mekanism 提供；本功能不覆盖或复制它们。
- 这次只实现配方与中间物注册，不修改压力结晶模组、Mekanism 机器或 Create 工作盆代码。

## 验收

1. JEI 可查到工作盆混合配方，7 种输入全部显示。
2. Mekanism 旋转浓缩器可将二氧化硫气体冷凝为液态二氧化硫；液态硫酸可直接从 Mekanism 现有生产链取得。
3. Mekanism 流体管道可分别向工作盆输入两种液体。
4. 无特质混合金属不能直接压成碎片；没有下界合金高压结晶不能完成序列组装。
5. 使用其他金属特质的高压结晶不能匹配；使用下界合金高压结晶后产出 2 个碎片。
6. 连续加工时液体与固体均按配方数量真实扣除。
