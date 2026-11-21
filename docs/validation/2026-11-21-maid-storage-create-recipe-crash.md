# Maid Storage Manager 启动崩溃调查

日期：2026-11-21

## 故障证据

`/opt/mc/logs/latest.log` 显示服务器在数据包预加载期间退出，尚未打开世界。首个致命异常为：

```text
ClassCastException:
net.ty.createcraftedbeginning.recipe.ChilledCompactingRecipe
cannot be cast to
com.simibubi.create.content.kinetics.mixer.CompactingRecipe
at maid_storage_manager@1.15.6/...GeneratorCreateCompact.transformAllIngredients
```

隔离复现还发现 `ChilledMixingRecipe` 与标准 `MixingRecipe` 的同类强转。此次崩溃不是缺失 SNBT 导致。

## 原因与处理

Create: Crafted Beginning 0.1.1 将冷压缩、冷搅拌配方注册在 Create 的 `create:compacting`、`create:mixing` 配方类型中。Maid Storage Manager 1.15.6 按配方类型读取时直接将其结果强转为 Create 标准配方类，双方对共享配方类型的假设不兼容。

用户决定移除 Maid Storage Manager，不继续维护兼容补丁。整合包 manifest、专属配置、Jade/JEI 顺序及进度引用随之清理。服务器旧世界存在该模组的女仆 Brain 记忆和 NeoForge 数据附件；移除时必须在停服后先完整备份世界及服务端文件，并保留回滚能力。

此前临时编写的 Mixin 回归测试只用于定位故障，不属于最终改动或修复方案。
