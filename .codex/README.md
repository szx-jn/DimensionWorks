# Codex 项目钩子

`.codex/hooks.json` 只在 Bash 工具可能执行整合包打包脚本时触发：

- 匹配 `python3 scripts/package_mrpack.py`、绝对路径调用和等价 Python 调用。
- 先运行 `scripts/pack_hook.py --skip-build`，失败时阻断打包并回传错误日志。
- 成功时只向模型回传一行 `PACK-HOOK OK`。
- 禁止在 Codex 中通过 `package_mrpack.py --skip-hook` 绕过预检。
- 打包命令未携带 `--skip-build` 时，`package_mrpack.py` 随后仍会执行完整预检和仓库内 Mod 构建。

项目钩子属于未托管钩子，首次使用或钩子内容变化后需要在 Codex 中执行 `/hooks` 完成审查与信任。未信任前 Codex 不会自动运行该钩子。

本地模拟一次 `PreToolUse` 输入：

```sh
echo '{"hook_event_name":"PreToolUse","tool_name":"Bash","tool_input":{"command":"python3 scripts/package_mrpack.py"}}' \
  | python3 .codex/hooks/pack_preflight.py
```
