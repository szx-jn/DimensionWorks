# Maid Storage Manager 与 Create 附属启动崩溃修复

日期：2026-11-21

## 故障证据

`/opt/mc/logs/latest.log` 在数据包预加载期间退出，尚未打开世界。首个致命异常为：

```text
ClassCastException:
net.ty.createcraftedbeginning.recipe.ChilledCompactingRecipe
cannot be cast to
com.simibubi.create.content.kinetics.mixer.CompactingRecipe
at maid_storage_manager@1.15.6/...GeneratorCreateCompact.transformAllIngredients
```

同一组合的隔离复现还发现 `ChilledMixingRecipe` 与标准 `MixingRecipe` 的同类强转。

## 原因与修复

Create: Crafted Beginning 0.1.1 将自定义冷压缩、冷搅拌配方分别注册在 Create 的 `create:compacting`、`create:mixing` 配方类型中。Maid Storage Manager 1.15.6 按配方类型读取时直接把结果当作 Create 的标准 `CompactingRecipe` / `MixingRecipe`，没有检查具体类。二者对共享配方类型的假设不兼容；与世界存档或缺失 SNBT 无关。

自研 `dimensionworks_maid_ae_crank` 0.1.1 在 Msm 缓存标准 Create 压缩/搅拌配方前按具体配方类过滤。自定义冷加工配方继续保留在 Create 自身配方管理器中，不进入 Msm 标准机器自动化缓存。没有修改第三方 JAR 或其源码。

## 验证

- `:dimensionworks-maid-ae-crank:build`：通过。
- `:dimensionworks-maid-ae-crank:runGameTestServer`：在 NeoForge 21.1.267、Create 6.0.10、Msm 1.15.6、Create: Crafted Beginning 0.1.1 实际加载组合下启动，加载 3778 条配方；8 项必需 GameTest 全部通过。
- 首次回归运行在过滤冷压缩配方后暴露冷搅拌的同类异常；将过滤覆盖到两个类型后，完整隔离启动通过。
- 生产服务器故障日志未出现 `Starting minecraft server`、`Preparing level` 或 `Done`；因此失败发生在打开世界之前。生产服务器修复后的启动仍须在正式包部署后复核。
