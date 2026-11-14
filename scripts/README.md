# Scripts

存放构建、打包、验证和其他开发辅助脚本。

KubeJS 脚本统一放在 `kubejs/`。

## 生成维度钥匙贴图

使用 Python 标准库重新生成 `kubejs:dimension_key` 的 16x16 RGBA 贴图：

```sh
python3 scripts/generate_dimension_key_texture.py
```

## 生成齿轮之心与修复碎片贴图

以 `docs/assets/gear_heart_reference.png` 为用户提供的模板，使用 Pillow 去除黑底并像素化为 32x32，再生成常态、破损和祝福三态；脚本同时绘制七个修复碎片各自的 32x32 贴图：

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

更新器是独立发布资产，不会进入 `.mrpack` 或服务端 ZIP。每个 Release 发布五个原生 Go 更新器：

- Windows amd64：`update-dimensionworks-client-windows-amd64.exe`、`update-dimensionworks-server-windows-amd64.exe`
- Linux amd64：仅服务端 `update-dimensionworks-server-linux-amd64`
- macOS arm64：`update-dimensionworks-client-macos-arm64`、`update-dimensionworks-server-macos-arm64`

首次使用时把对应更新器复制到客户端实例根目录或服务端根目录执行。所有平台均使用 `update-Windows/` 中同一套 Go 源码交叉编译，不依赖 Shell、BAT、PowerShell 或 Python。更新器默认查询 GitHub Latest Release，也支持 `--release vX.Y.Z`、`--package <本地完整包>`、`--url <完整包 URL>` 和 `--help`。更新器读取 Release 侧的独立 `*-update-manifest.tsv` 资产；使用本地 `--package` 时读取完整包同目录的配套清单，优先使用客户端或服务端差量 ZIP，只复制或下载摘要变化的受管文件，并在首次运行没有状态文件时从受管目录建立摘要基线，保护无可信基线的自定义文件。已有未签名状态直接报错。终端会实时显示下载、检查、暂存、应用和删除进度。`.dimensionworks-update/state.tsv` 记录版本和受管文件状态，事务目录保存回滚备份。

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
- `update-assets`：确认 `update-Windows/` 的 Go 源码完整、版本提示界面调用本地启动器，以及 `LocalUpdaterLauncher` 选择的客户端更新器文件名与发布工作流一致；GitHub Actions 会测试并交叉编译 Windows amd64、Linux amd64、macOS arm64 更新器。

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

手动触发 `.github/workflows/release-pack.yml` 后，工作流先查询 GitHub 最新 Release，以本地 `VERSION` 与远端标签较大者为基线推进补丁版本；目标标签已存在时直接失败，绝不覆盖。随后生成客户端 `.mrpack`、服务端 ZIP、两个独立更新清单、服务端差量 ZIP、五个原生 Go 更新器和 `SHA256SUMS`，上传工作流 artifacts 并创建对应的 `v<版本>` GitHub Release。发布成功后把新的 `VERSION` 提交回触发分支。

`skip_tests` 默认关闭。维护者明确要求停止测试时，可在手动发行中设为 `true`：自研 Mod 构建使用 `-x test`，跳过 Go 测试、内嵌 aria2 下载测试和 Linux 更新事务运行验证，仍执行打包静态预检、完整构建和产物格式/摘要检查。该状态会自动写入发行说明。钩子通过 `DIMENSIONWORKS_SKIP_TESTS=true` 接收这一构建选项，未设置时保留原流程。

### BBSMC 最新正式版同步

正式发布工作流中的 `sync-bbsmc` 任务只在 GitHub 发布任务成功、且成功创建了本次发行标签后运行。脚本再次查询 GitHub Latest Release，确认本次发行仍是最新的非草稿、非预发布版本；上传前再检查一次，避免下载期间已有更新版本发布。普通提交、推送、PR、独立构建和旧发行版不会触发同步。

发行说明的四个输入栏至少填写一条本次变更，否则工作流在构建前失败；不再用历史更新器通用说明替代本次日志。同步同一正式发行中的三个固定资产：`DimensionWorks-<版本>.mrpack`、`DimensionWorks-Server-<版本>.zip` 和 `DimensionWorks-Server-<版本>-delta.zip`。逐字使用 GitHub Release 正文作为 BBSMC 版本更新日志，保留中文及换行。游戏版本和 Forge 类型从 MRPack 索引确定，MRPack 始终为主文件，两个服务端 ZIP 为附加下载。每个下载核对资产大小及 GitHub 提供的 SHA-256，上传后核对三个文件的 SHA-512、主文件和更新日志。同步不上传更新清单、更新器或其他发行资产，也不修改 BBSMC 项目设置。

目标为已有的 [维度工序 BBSMC 项目](https://bbsmc.net/modpack/dimensionworks)，项目 ID `doVGSZq0`。首次启用前，登录后直接打开 [BBSMC 个人访问令牌页面](https://bbsmc.net/settings/pats)（当前设置侧栏隐藏了此入口），创建拥有「创建版本、读取版本、修改版本」权限的令牌，其所属账号还须具备项目上传权限；补齐已有版本附件需要 `VERSION_WRITE`。将令牌保存在 GitHub 仓库 Actions Secret `BBSMC_TOKEN` 中，不写入代码、日志或聊天。缺少令牌时同步任务明确失败，已成功发布的 GitHub Release 与版本号回写保留。

同步先只上传 MRPack 创建版本，再按文件大小从小到大逐个追加服务端附件，每次请求只携带一个文件，避免将近 600 MB 的三个产物合并到同一建版请求。每次上传前确认仍是最新正式版；日志记录当前文件、大小与接口，网络错误包含请求方法和接口，便于区分下载、建版和追加附件阶段。

同步失败后，在该正式发布运行中选择 **Re-run failed jobs**，只补跑失败的同步任务。脚本已修复时，手动运行 `.github/workflows/sync-bbsmc.yml`，使用最新脚本补齐当前正式版，不重建或重发 GitHub Release。若 BBSMC 已存在相同版本，先验证已列出、更新日志、现有文件名/摘要与 MRPack 主文件；三个文件齐全则直接成功，只缺服务端附件时通过追加文件接口逐个补齐。已有内容不同或尚未列出时停止报告冲突，不覆盖。若 GitHub 已发布更晚版本，则跳过旧版本补同步。上传请求不会自动重试，中断后重跑会先核对并保留已上传文件，只补缺失附件。BBSMC 的审核和账户限额仍由平台控制。

API 依据：[Modrinth 创建版本](https://docs.modrinth.com/api/operations/createversion/)使用 `file_parts` 与 `primary_file` 的多文件 multipart 请求；[追加版本文件](https://docs.modrinth.com/api/operations/addfilestoversion/)使用 `POST /version/{id}/file`、`VERSION_WRITE` 权限并返回 HTTP 204。另核对 [BBSMC 文件校验实现](https://github.com/bbsmc/app/blob/main/apps/labrinth/src/validate/mod.rs)与[上传实现](https://github.com/bbsmc/app/blob/main/apps/labrinth/src/routes/v3/version_creation.rs)：BBSMC 允许 ZIP，每文件上限 1 GiB，本次完整包小于该限制。2026-11-14 的 0.3.46 原同步任务已实际成功使用 `BBSMC_TOKEN` 创建 MRPack 版本，不能仅凭令牌页面此前的「从未使用」判断鉴权失败。

无需令牌即可执行只读验证，命令下载并核对最新发行的三个文件及更新日志，不创建或修改 BBSMC 版本：

```sh
GITHUB_REPOSITORY=szx-jn/DimensionWorks BBSMC_PROJECT_ID=doVGSZq0 \
  python3 scripts/sync_bbsmc_release.py --tag vX.Y.Z --dry-run
```

定向回归检查：

```sh
python3 -m unittest discover -s scripts/tests -p 'test_sync_bbsmc_release.py' -v
```

## CI 固定 Create 编译依赖

`prepare_create_build_repository.py` 仅允许在 GitHub Actions 中执行。Create Maven 站点证书过期时，从清单固定的 Modrinth Create 6.0.8 原包读取编译依赖；校验固定 SHA-512 和内嵌库版本，将 Flywheel、Ponder、Registrate、MixinExtras 原始 jar 及去掉内嵌依赖的 Create 编译 jar 放入运行器临时 Maven 仓库，并以 Gradle init 脚本限定这些固定坐标的来源。

该步骤不编译或修改第三方 Java 类，保留所需编译依赖，不改变整合包分发的 Create 原包，不关闭 HTTPS 验证。升级清单中的 Create 版本时必须同步审查此脚本的版本与哈希。普通模组构建、独立客户端验证、正式发布共同使用该准备步骤。

`python3 scripts/draw_dispatch_terminal_assets.py` 生成中转发运终端的完整六面模型、不透明 16×16 PNG 材质、暮色升级卡图标，以及六个 Alex 洞穴的 108×82 像素插画和总览图。只使用 Python 标准库；GUI 按实际卡片尺寸缩放整张图像。
