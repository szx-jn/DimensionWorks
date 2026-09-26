# KubeJS

存放 DimensionWorks 的 KubeJS 脚本、配方、标签和数据。

建议结构：

- `startup_scripts/`
- `server_scripts/`
- `client_scripts/`
- `data/`
- `assets/`

## 天境装备规则

`server_scripts/aether_equipment_lock.js` 禁用天境装备的制作、修复和正常获取流程，保留流程功能物。
配套的 `defaultconfigs/aether-client.toml` 与 `defaultconfigs/aether-common.toml` 隐藏天境饰品按钮，并启用 Curios 标准菜单。
