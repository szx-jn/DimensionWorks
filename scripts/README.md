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
python3 scripts/package_mrpack.py
```

整合包版本固定使用 `x.x.x`，不添加 `alpha`、`beta` 或其他后缀。根目录 `VERSION` 保存当前已采用版本；普通打包自动把补丁号加一，例如当前 `0.1.2` 会生成 `0.1.3`。

完成一个维度后才能增加次版本号：

```sh
python3 scripts/package_mrpack.py --dimension-complete  # 0.1.2 -> 0.2.0
```

主版本默认保持 `0`。只有收到单独指令时才可执行主版本提升：

```sh
python3 scripts/package_mrpack.py --major-bump --version 1.0.0
```

脚本以 `manifest/mods.json` 为唯一 Mod 清单，远程文件使用固定版本和固定下载地址。 客户端独立更新清单会再次依据 `modrinth.index.json` 的 `env.client` 过滤 `unsupported` 项，避免把 LuckPerms 等服务端专用 Mod 下载到客户端；更新器会按旧/新清单差异更新文件，并删除旧版本受管清单中已移除的文件，同时保留存档和用户私有数据。仓库的 `config/`、`defaultconfigs/`、`kubejs/`、`resourcepacks/` 先作为基础写入 `overrides/`，随后直接叠加游戏实例的 `config/`、`defaultconfigs/`、`local/`、`kubejs/config/`，以及 `options.txt`、`log4j2.xml`、`rhino.local.properties`、`CustomSkinLoader/CustomSkinLoader.json`、`minemenu/menu.json`。实例文件同名时优先，确保游戏内当前配置进入整合包。

光影不由仓库嵌入。`manifest/shaderpacks.json` 固定记录 Modrinth 版本 ID 或 CurseForge 文件 ID、文件名、大小和 SHA-1/SHA-512；打包时这些文件直接写入 `.mrpack` 下载索引，`shaderpacks/` 不再作为 override 复制。Chocapic13 V6 按作者原始总包下载，安装后在游戏内先解压再选择 High、Low 等预设。

实例的 `config/` 缺失时打包失败，其余实例配置不存在时跳过。账号文件、`servers.dat`、存档、日志、缓存、地图个人数据、Mod 本体和启动器数据不会写入整合包。默认输出到 `dist/`。macOS 默认实例目录为 `~/Library/Application Support/minecraft/versions/DimensionWorks`，可通过 `--instance-dir` 覆盖。

## 打包服务端

生成可直接解压开服的完整 Forge 服务端 zip：

```sh
python3 scripts/package_server.py
```

脚本从同一份 `manifest/mods.json` 中筛选 `environment` 为 `both` 或 `server` 的必需 Mod，排除仅客户端 Mod，并构建 `source` 为 `in-repo` 的自研 Mod。它会安装固定版 Forge `47.4.23`，写入已安装的 `libraries/`、服务端 `config/`、`defaultconfigs/`、`kubejs/`、`server.properties`、`eula.txt`、`user_jvm_args.txt`、Linux/Windows 启动脚本和中文开服说明。

默认输出到 `dist/DimensionWorks-Server-<当前 VERSION>.zip`，不递增 `VERSION`。服务端包不包含世界存档、日志、玩家名单、RCON 密码或其他运行时私有数据。Linux/开服面板启动命令为 `bash start.sh`；Windows 运行 `start.bat`。首次启动前仍需由服主阅读并同意 Minecraft EULA，把 `eula.txt` 改为 `eula=true`。

可指定其他输出路径：

```sh
python3 scripts/package_server.py --output ~/Desktop/DimensionWorks-Server.zip
```

## 独立增量更新器

更新器是独立发布资产，不会进入 `.mrpack` 或服务端 ZIP。每个 Release 发布六个原生 Go 更新器：

- Windows amd64：`update-dimensionworks-client-windows-amd64.exe`、`update-dimensionworks-server-windows-amd64.exe`
- Linux amd64：`update-dimensionworks-client-linux-amd64`、`update-dimensionworks-server-linux-amd64`
- macOS arm64：`update-dimensionworks-client-macos-arm64`、`update-dimensionworks-server-macos-arm64`

首次使用时把对应更新器复制到客户端实例根目录或服务端根目录执行。所有平台均使用 `update-Windows/` 中同一套 Go 源码交叉编译，不依赖 Shell、BAT、PowerShell 或 Python。更新器默认查询 GitHub Latest Release，也支持 `--release vX.Y.Z`、`--package <本地完整包>`、`--url <完整包 URL>` 和 `--help`。更新器读取包内或 Release 侧的 `dimensionworks-update-manifest.tsv`，优先使用客户端或服务端差量 ZIP，只复制或下载摘要变化的受管文件，并在旧状态缺失时从受管目录建立基线，删除旧包遗留文件。终端会实时显示下载、检查、暂存、应用和删除进度。`.dimensionworks-update/state.tsv` 记录版本和受管文件状态，事务目录保存回滚备份。

客户端保留存档、日志、截图和启动器私有数据；服务端保留世界（含 `server.properties` 指定的自定义世界名）、`config/`、`defaultconfigs/`、LuckPerms 配置与数据库、日志、`eula.txt`、`server.properties`、`user_jvm_args.txt`、白名单、封禁和运行数据。已有配置只在缺失时从新包补齐，不覆盖现存配置。更新器等待服务端正常退出，最多 180 秒；超时即中止，不强制杀进程，以免世界规则或权限数据未落盘。运行客户端前退出游戏，更新前建议先手动停止服务端。服务端优先使用 `DimensionWorks-Server-<版本>-delta.zip`，差量缺失、跨版本或本地文件漂移时自动回退完整 ZIP。

## 打包前钩子

`scripts/pack_hook.py` 是打包预检钩子。`scripts/package_mrpack.py` 每次运行都会自动先跑它，也可以单独执行：

```sh
python3 scripts/pack_hook.py             # 只输出错误，或一行成功信息
python3 scripts/pack_hook.py --verbose   # 额外打印每一步结果
python3 scripts/pack_hook.py --skip-build
```

钩子按顺序检查：

- `manifest`：条目字段完整，版本不是 `pending-release`，没有移动分支引用。
- `shaderpack-manifest`：光影来源固定，文件名不重复，SHA-1/SHA-512 与大小合法。
- `pack-version`：版本格式必须是 `x.x.x`；普通更新只能补丁加一，次版本必须标记维度完成，主版本必须显式确认。
- `instance-config`：实例 `config/` 存在（打包必需）。
- `localization`：对比实例中已固定的第三方 Jar，校验汉化 Mod 的语言键、符号占位符和 GuideME/Patchouli 页面覆盖。
- `json`：`manifest/mods.json`、`manifest/shaderpacks.json`、`kubejs/data`、`kubejs/assets` 与 `config/ftbquests` 下所有 JSON 可解析。
- `kubejs-syntax`：`node --check` 扫全部 KubeJS 脚本。
- `self-propagation`：运行 `scripts/check_cave_self_propagation.js`，校验配方 ID 唯一、主链不消耗自身产物、备链恰好吃 2 个本维残渣。
- `quest-graph`：FTB 任务 ID 唯一、依赖存在、无循环、括号平衡。
- `recipe-ids`：脚本中的配方 ID 不与数据包文件路径冲突。
- `in-repo-build`：逐个执行 `gradle build --no-daemon`，并确认产物 jar 名与清单版本一致。
- `update-assets`：确认 `update-Windows/` 的 Go 源码完整；GitHub Actions 会测试并交叉编译 Windows amd64、Linux amd64、macOS arm64 更新器。

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
2. 确认根目录 `VERSION` 是当前已发布版本。普通小更新不要手工跳号，打包脚本会自动生成并写回下一个补丁版。
3. 确认 `manifest/mods.json` 没有未固定版本，远程条目都有固定版本 ID 或固定 Release 资产。
4. 确认 `manifest/shaderpacks.json` 的版本 ID、文件 ID、大小和哈希与原始发布一致。
5. 确认游戏实例的 `config/` 已经是想进包的状态，打包时实例配置会覆盖仓库配置。
6. 运行 `python3 scripts/package_mrpack.py`；钩子会自动先跑，失败就不会产出整合包。
7. 确认 `dist/DimensionWorks-<版本>.mrpack` 生成，记下大小并把路径交付。

## 每次打包服务端要做什么

1. 确认根目录 `VERSION`、自研 Mod 版本和 `manifest/mods.json` 已同步。
2. 确认 `manifest/mods.json` 的 `environment` 标记正确，仅客户端 Mod 不应进入服务端。
3. 运行 `python3 scripts/package_server.py`；钩子会先做静态检查和自研 Mod 构建。
4. 检查服务端 zip 中的 `mods/`、`config/`、`defaultconfigs/`、`kubejs/`、`libraries/` 和启动脚本。
5. 服务端交付前，至少验证压缩包可解压、Forge 启动参数路径存在，并尽可能实际启动一次服务端。

## GitHub Actions 发行

手动触发 `.github/workflows/release-pack.yml` 后，工作流先查询 GitHub 最新 Release，以本地 `VERSION` 与远端标签较大者为基线推进补丁版本；目标标签已存在时直接失败，绝不覆盖。随后生成客户端 `.mrpack`、服务端 ZIP、两个独立更新清单、服务端差量 ZIP、六个原生 Go 更新器和 `SHA256SUMS`，上传工作流 artifacts 并创建对应的 `v<版本>` GitHub Release。发布成功后把新的 `VERSION` 提交回触发分支。
