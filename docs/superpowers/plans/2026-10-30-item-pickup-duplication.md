# 地面物品重复吸取修复实施计划

- [x] 创建独立 Mod `dimensionworks-item-pickup-guard`，使用 Forge 1.20.1 / Java 17。
- [x] 添加 ItemEntity#getItem() 的最小 Mixin：实体 removed 时返回 ItemStack.EMPTY。
- [x] 配置 Mixin refmap、mods.toml、Mod 主类与 README。
- [x] 将新 Mod 作为 in-repo、both 环境条目加入 manifest/mods.json。
- [ ] 运行 Gradle build，检查 reobfJar 与最终 JAR 中的 Mixin 配置。
- [ ] 运行整合包预检与打包；检查服务端包确实包含新 Mod。
- [ ] 在实际服务器验证 Create 与背包竞争、单独吸取及部分堆叠场景。
- [ ] 通过后更新版本并发布 GitHub Release；不发布到 CurseForge。
