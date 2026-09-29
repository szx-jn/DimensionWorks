# DimensionWorks Mekanism Stress

把 Mekanism 耗能机器接入 Applied Create 的 ME 应力网络。

## 功能

- Mekanism 机器不再接受 Forge Energy、能量槽或红石转换能量，只能由安装了 `stress_output_card` 的 AE2 ME 输出总线供能。
- ME 输出总线朝向的 Mekanism 设备，把 ME 网络中的 `appliedcreate:stress` 按转换率写入机器原生 FE 能量缓冲；机器运行时按实际 FE 消耗扣减库存。
- SU 库存与容量以原生 FE 缓冲为唯一存储，Jade、能量条和能量页显示时向下取整换算为 SU。
- 加工速度与应力需求使用 `dimensionworks_rpm_limit` 的阶跃超速曲线，以机器自身等级的最低 RPM 作为 1x 基准。
- Jade 对可接受应力的 Mekanism 机器隐藏原本的 FE 数据，改显示机器当前库存与容量：`内部应力：X / Y SU`。
- 能量立方、感应矩阵、量子纠缠、通用线缆、热导线缆和充能台保留方块与存档数据，但其能量能力失效；其配方由 KubeJS 移除并从 JEI 隐藏。

## 机器转速门槛

- 基础机器：`128 RPM`
- 进阶机器：`512 RPM`
- 精英机器：`2048 RPM`
- 终极机器：`10240 RPM`

供能转速低于机器门槛时不消耗应力，机器也不会运行。达到门槛后，超速曲线以该机器门槛作为 1x 饱和基准。
总线断开、未配置应力或转速低于门槛时，机器已有的内部 SU 会保留但冻结，不能继续驱动加工。

## 应力输出卡

- 物品 ID：`dimensionworks_mek_stress:stress_output_card`
- 只可安装在 AE2 ME 输出总线，最多一张。
- 默认 `32 RPM`、每 tick 最多输出 `1024 SU`，拆下后设置保留。
- 输出总线的配置槽可以把 JEI 中的 `appliedcreate:stress` 拖入作为输出目标；未配置应力时不会供电。
- 手持卡片右键可打开设置界面，修改输出 RPM 与每 tick 最多输出的 SU；数值单位仍为 SU，
  “每 tick”只描述输出速率上限。
- 装卡后总线专职供应力，不再导出普通物品；拆卡后恢复普通导出。
- 卡片 RPM 会受总线所有者当前的 `dimensionworks_rpm_limit` 限速约束。

## 配置

公共配置默认值：

- `joulesPerSu = 2.5`
- `defaultRpm = 32`
- `maxRpm = 10240`
- `defaultStressPerTick = 1024`
- `maxStressPerTick = 1048576`
- `ownerFallbackRpm = 32`

## 兼容修复

- Applied Create 的 ME 齿轮箱应力自定义上限始终至少为 `8 × 当前输出转速`。界面校验、菜单夹取和方块实体存入时会使用同一个动态上限。
- AE2 网格能量存储忽略重复节点移除。FTB Ultimine 连锁破坏线缆总线导致的第二次移除不再抛异常，后续方块可以继续连锁处理。

构建：

```sh
gradle build --no-daemon
```
