# 地面物品重复吸取修复设计

## 问题

Create 传送带与 Sophisticated Backpacks 的磁铁/拾取升级可能在同一服务端 Tick 处理同一个 ItemEntity。Create 的物品转移流程在成功接收完整堆叠后调用 `ItemEntity#discard()`；Minecraft 的实体移除不会自动清空 ItemEntity 内部的 ItemStack。若后续拾取逻辑仍读取该实体，就可能再次转移同一堆叠。

整合包锁定版本：
- Minecraft 1.20.1 / Forge 47.4.23
- Create 6.0.8
- Sophisticated Backpacks 3.26.3.2157
- Sophisticated Core 1.5.1.2335
- Sophisticated Backpacks Create Integration 0.1.10.167

## 目标

- 一个已经移除的 ItemEntity 不再向后续拾取逻辑暴露可转移物品。
- 不禁用 Create 传送带、背包磁铁或拾取升级。
- 未移除实体的剩余堆叠仍可继续被其他机器吸取。
- 不修改第三方 Mod，不依赖第三方内部类或私有实现。
- 只在实体已移除后返回空堆叠，不改变仍在世界中的物品数量。

## 实现

在服务端与客户端通用的 ItemEntity#getItem() 入口添加 Mixin：当实体已处于 removed 状态时返回 ItemStack.EMPTY；否则调用原方法。

这使第一个成功完成完整转移并 discard 实体的拾取者成为唯一有效拾取者。之后同 Tick 的其他拾取者即使仍持有实体引用，也无法再取得旧堆叠。部分转移不 discard 实体，后续拾取者只能看到更新后的剩余堆叠，因此不会丢失合法剩余物品。

## 边界与风险

- 此保护针对通过 ItemEntity#getItem() 读取物品的拾取路径；若某个 Mod 缓存了旧 ItemStack 引用并在实体移除后继续转移，仍需针对该调用点补充保护。
- 不把所有 setItem/discard 调用都当作拾取：实体移除后读取空堆叠是更窄的兼容性边界。
- 该改动不保证修复其他类型实体或非 ItemEntity 的复制问题。

## 验收

1. Create 传送带单独吸取：数量正确。
2. Sophisticated Backpacks 磁铁/拾取升级单独吸取：数量正确。
3. 两者在同一 Tick 竞争同一个 ItemEntity：物品总量不增加、不丢失。
4. 背包/传送带只接收部分数量时，地面剩余数量准确，仍可继续吸取。
5. 移除实体后重复调用 getItem() 返回空堆叠。
6. Forge 47.4.23 下 Mod 构建通过，并确认专用服务器加载不触发客户端类缺失。
