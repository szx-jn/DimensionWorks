# Scripts

存放构建、打包、验证和其他开发辅助脚本。

KubeJS 脚本统一放在 `kubejs/`。

## 生成维度钥匙贴图

使用 Python 标准库重新生成 `kubejs:dimension_key` 的 16x16 RGBA 贴图：

```sh
python3 scripts/generate_dimension_key_texture.py
```

## 打包整合包

生成可拖入 Prism Launcher、MultiMC、Modrinth App 或 ATLauncher 的 Modrinth `.mrpack`：

```sh
python3 scripts/package_mrpack.py --version 0.1.0-alpha.1
```

脚本以 `manifest/mods.json` 为唯一 Mod 清单，远程文件使用固定版本和固定下载地址，自研 Mod 构建产物及 `config/`、`defaultconfigs/`、`kubejs/`、`resourcepacks/` 会写入 `overrides/`。默认输出到 `dist/`。
