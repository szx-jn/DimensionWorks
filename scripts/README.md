# Scripts

存放构建、打包、验证和其他开发辅助脚本。

KubeJS 脚本统一放在 `kubejs/`。

## 生成维度钥匙贴图

使用 Python 标准库重新生成 `kubejs:dimension_key` 的 16x16 RGBA 贴图：

```sh
python3 scripts/generate_dimension_key_texture.py
```

## 生成齿轮之心与修复碎片贴图

以 `docs/assets/gear_heart_reference.png` 为用户提供的模板，使用 Pillow 去除黑底并像素化为 32x32，再生成常态、破损、诅咒和祝福四态；脚本同时绘制七个修复碎片共用的 32x32 占位贴图：

```sh
python3 scripts/pixelate_gear_heart_textures.py
```

脚本依赖 Pillow；未安装时请使用隔离的虚拟环境安装，避免改写系统 Python。

脚本输出到 `kubejs/assets/kubejs/textures/item/`，并生成 `docs/assets/gear_heart_states_preview.png` 放大预览。

## 打包整合包

生成可拖入 Prism Launcher、MultiMC、Modrinth App 或 ATLauncher 的 Modrinth `.mrpack`：

```sh
python3 scripts/package_mrpack.py --version 0.1.0-alpha.1
```

脚本以 `manifest/mods.json` 为唯一 Mod 清单，远程文件使用固定版本和固定下载地址。仓库的 `config/`、`defaultconfigs/`、`kubejs/`、`resourcepacks/` 先作为基础写入 `overrides/`，随后直接叠加游戏实例的 `config/`、`defaultconfigs/`、`local/`、`kubejs/config/`，以及 `options.txt`、`log4j2.xml`、`rhino.local.properties`、`CustomSkinLoader/CustomSkinLoader.json`、`minemenu/menu.json`。实例文件同名时优先，确保游戏内当前配置进入整合包。

实例的 `config/` 缺失时打包失败，其余实例配置不存在时跳过。账号文件、`servers.dat`、存档、日志、缓存、地图个人数据、Mod 本体和启动器数据不会写入整合包。默认输出到 `dist/`。macOS 默认实例目录为 `~/Library/Application Support/minecraft/versions/DimensionWorks`，可通过 `--instance-dir` 覆盖。

## 打包前钩子

`scripts/pack_hook.py` 是打包预检钩子。`scripts/package_mrpack.py` 每次运行都会自动先跑它，也可以单独执行：

```sh
python3 scripts/pack_hook.py             # 只输出错误，或一行成功信息
python3 scripts/pack_hook.py --verbose   # 额外打印每一步结果
python3 scripts/pack_hook.py --skip-build
```

钩子按顺序检查：

- `manifest`：条目字段完整，版本不是 `pending-release`，没有移动分支引用。
- `instance-config`：实例 `config/` 存在（打包必需）。
- `json`：`manifest/mods.json`、`kubejs/data`、`kubejs/assets` 与 `config/ftbquests` 下所有 JSON 可解析。
- `kubejs-syntax`：`node --check` 扫全部 KubeJS 脚本。
- `self-propagation`：运行 `scripts/check_cave_self_propagation.js`，校验配方 ID 唯一、主链不消耗自身产物、备链恰好吃 2 个本维残渣。
- `quest-graph`：FTB 任务 ID 唯一、依赖存在、无循环、括号平衡。
- `recipe-ids`：脚本中的配方 ID 不与数据包文件路径冲突。
- `in-repo-build`：逐个执行 `gradle build --no-daemon`，并确认产物 jar 名与清单版本一致。

输出约定：失败时打印 `PACK-HOOK FAIL` 加失败步骤与截断后的错误日志；成功时只打印一行 `PACK-HOOK OK: ...`。打包脚本会把钩子结果附在同一行成功信息里。

### Codex 自动预检

仓库级 Codex 钩子位于 `.codex/hooks.json`。`PreToolUse` 只匹配 `Bash`，再由 `.codex/hooks/pack_preflight.py` 判断命令是否真正调用 `scripts/package_mrpack.py`；读取脚本、搜索脚本名或其他 Bash 命令不会被误触发。

命中打包命令后，Codex 钩子先执行 `scripts/pack_hook.py --skip-build`：

- 失败：返回 `PACK-HOOK FAIL` 和截断后的真实错误日志，并阻止原打包命令启动。
- 成功：只返回一行 `Codex packaging preflight: PACK-HOOK OK: ...`。
- `--skip-hook`：由 Codex 钩子判定为策略违规并阻断，防止绕过预检。
- 普通打包：原命令继续运行，`package_mrpack.py` 随后执行完整预检和仓库内 Mod 构建，因此不会重复构建。

项目级未托管钩子首次启用或内容变化后，需要在 Codex 中以 `/hooks` 审查并信任；未信任前 Codex 不会自动运行。钩子信任按内容哈希记录，因此修改脚本或 `hooks.json` 后需要重新信任。

可以用模拟事件独立验证脚本，不需要真的启动打包：

```sh
echo '{"hook_event_name":"PreToolUse","tool_name":"Bash","tool_input":{"command":"python3 scripts/package_mrpack.py"}}' \
  | python3 .codex/hooks/pack_preflight.py
```

## 每次打包整合包要做什么

1. 对好版本号：自研 Mod 的 `build.gradle` 或 `gradle.properties`、`manifest/mods.json` 里的 `version`，必要时同步 README。
2. 确认 `manifest/mods.json` 没有未固定版本，远程条目都有固定版本 ID 或固定 Release 资产。
3. 确认游戏实例的 `config/` 已经是想进包的状态，打包时实例配置会覆盖仓库配置。
4. 运行 `python3 scripts/package_mrpack.py --version <版本>`；钩子会自动先跑，失败就不会产出整合包。
5. 确认 `dist/DimensionWorks-<版本>.mrpack` 生成，记下大小并把路径交付。
