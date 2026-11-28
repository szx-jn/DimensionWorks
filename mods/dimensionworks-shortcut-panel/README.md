# DimensionWorks Shortcut Panel

客户端 NeoForge 1.21.1 模组。按 F12 打开“无按键快捷键”面板，列出当前已注册且未绑定按键的快捷键；点击一项会为该快捷键排入一次点击，然后恢复为无按键。列表可滚动，按 Esc 关闭。

触发使用 Minecraft 的 `KeyMapping` 点击队列，不直接调用第三方模组实现，也不通过反射修改内部状态。面板会临时给所选映射分配一个未占用的合成按键、调用原生 `KeyMapping.click`，再恢复原来的无按键状态，因此只触发所选项目。这个机制模拟一次按键点击；依赖持续按住或直接监听 GLFW 输入的功能不适用。

## 按键默认值

- F12：打开本面板。
- 面板中的条目：无默认按键，由用户点击触发一次。
- 本包原先绑定 F11 或 F12 的快捷键统一清为无按键；Omnisearch 的“打开搜索窗口”和“查询悬停物品”均设为 B。

本模组只在客户端运行，不注册服务端逻辑、命令、网络消息或世界状态。

## 验证

```sh
./gradlew :dimensionworks-shortcut-panel:test :dimensionworks-shortcut-panel:build
python3 scripts/check_architecture.py
```
