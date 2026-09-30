# Codex 项目钩子

`.codex/hooks.json` 包含两类钩子：打包前检查和回合结束后的自动提交。

## 打包预检

`PreToolUse` 只在 Bash 工具可能执行整合包打包脚本时触发：

- 匹配 `python3 scripts/package_mrpack.py`、绝对路径调用和等价 Python 调用。
- 先运行 `scripts/pack_hook.py --skip-build`，失败时阻断打包并回传错误日志。
- `pack-version` 规则：只接受 `x.x.x`；普通更新自动补丁加一，次版本只允许在明确标记维度完成后增加，主版本只允许显式确认。
- 成功时只向模型回传一行 `PACK-HOOK OK`。
- 禁止在 Codex 中通过 `package_mrpack.py --skip-hook` 绕过预检。
- 打包命令未携带 `--skip-build` 时，`package_mrpack.py` 随后仍会执行完整预检和仓库内 Mod 构建。

## 自动提交与推送

`Stop` 在 Codex 正常结束一轮时运行一次，不针对每个工具调用或中间状态提交：

- 无工作区修改时不创建空提交。
- 有修改时依次执行 `git add -A`、`git commit` 和 `git push origin HEAD`。
- 如果工作区干净但当前分支仍有未推送提交，会继续尝试补推。
- 不执行 `--force`；detached HEAD、未完成的合并/变基、未解决冲突、无 `origin` 或推送失败时只报告失败。
- 失败不会延长或重试当前回合；本地提交已经成功时会保留提交号，下一轮继续补推。
- 成功只报告分支、提交号、变更文件数、远端和推送状态；失败只报告阶段、原因、本地提交号和 Git 的关键错误输出。

项目钩子属于未托管钩子，首次使用或钩子内容变化后需要在 Codex 中执行 `/hooks` 完成审查与信任。未信任前 Codex 不会自动运行对应钩子。

本地模拟一次 `PreToolUse` 输入：

```sh
echo '{"hook_event_name":"PreToolUse","tool_name":"Bash","tool_input":{"command":"python3 scripts/package_mrpack.py"}}' \
  | python3 .codex/hooks/pack_preflight.py
```

本地模拟一次回合结束：

```sh
echo '{"hook_event_name":"Stop","cwd":"'"$PWD"'","stop_hook_active":false}' \
  | python3 .codex/hooks/auto_commit_push.py
```
