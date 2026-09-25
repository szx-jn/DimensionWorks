# 中转维度钥匙设计

## 目标

使用 KubeJS 注册一次性“维度钥匙”。玩家右键消耗钥匙后永久解锁中转维度；此后通过可自定义按键执行与 `/dwtransfer` 相同的传送。在中转维度再次按键时，返回进入前保存的维度、坐标和朝向。

## 组成

### KubeJS 物品

- 物品 ID：`kubejs:dimension_key`
- 中文名称：`维度钥匙`
- 英文名称：`Dimension Key`
- 最大堆叠：1
- 获取方式：本阶段只注册物品，不添加配方，后续通过 KubeJS 数据脚本补充。
- 贴图：由 Python 脚本生成的 16x16 像素图，外观为带发光宝石的维度钥匙。
- 资源路径：`kubejs/assets/kubejs/textures/item/dimension_key.png`
- 贴图生成脚本：`scripts/generate_dimension_key_texture.py`

右键使用行为：

1. 仅处理主手使用。
2. 若玩家已有标签 `dimensionworks_transfer.unlocked`，不消耗物品并提示已解锁。
3. 若未解锁，添加永久标签并消耗 1 个物品。
4. 解锁后仍由玩家按配置按键进入中转维度，不在使用物品时立即传送。

### 永久访问状态

- 使用原版实体标签 `dimensionworks_transfer.unlocked` 记录永久解锁状态。
- 标签随玩家数据持久化，不要求 KubeJS 与自研 Mod 共享自定义 NBT。
- `/dwtransfer` 和按键入口执行同一服务端命令，二者权限行为一致。

### 返回点

- 在主世界 `SavedData` 中加入 `dimensionworks_transfer_returns`。
- 每个玩家保存一个返回点：维度 ID、坐标、yaw、pitch。
- 从普通维度进入中转维度前覆盖保存当前返回点。
- 在中转维度执行入口时传送到保存的返回点。
- 返回成功后删除该返回点；目标维度不存在或返回失败时保留并报告错误。
- 返回点跨服务器重启保留。

### 按键

- KeyMapping ID：`key.dimensionworks_transfer.toggle`
- 分类：`key.categories.dimensionworks_transfer`
- 默认键：`P`
- 由 `dimensionworks-transfer` 自研 Mod 注册，因此玩家可在原版“控制”界面修改。
- 客户端只发送 `/dwtransfer`，不在客户端决定权限或切换维度。

## 服务端行为

`/dwtransfer` 只有在玩家实体执行时有效，并分为两种路径：

1. 玩家位于 `dw:transfer`：
   - 存在返回点时返回保存位置并清除返回点。
   - 不存在返回点时提示无可用返回点，不改变维度。
2. 玩家位于其他维度：
   - 没有 `dimensionworks_transfer.unlocked` 标签时拒绝传送并提示需要消耗维度钥匙。
   - 已解锁时保存当前位置并调用现有 `TerritoryManager.sendToTransfer`，继续使用现有分地、建造和边界逻辑。

## 文件边界

- KubeJS 启动脚本注册物品、模型和显示名称。
- KubeJS 服务端脚本处理右键消耗和永久解锁。
- `mods/dimensionworks-transfer` 增加按键注册、返回点存储和命令权限校验。
- 自研 Mod 不依赖 KubeJS 编译期 API，只依赖原版标签和 `/dwtransfer` 命令。

## 测试与验证

- Java 单元测试覆盖返回点 NBT 往返、缺失字段拒绝和已有返回点覆盖。
- `gradle build --no-daemon` 必须通过。
- KubeJS 脚本使用 `node --check` 做语法检查。
- JSON 与语言文件做解析检查。
- Python 生成贴图后检查 PNG 格式、16x16 尺寸和透明通道。
- 重新构建自研 Mod、更新 `manifest/mods.json` 固定版本并生成 `.mrpack`。
- 不以静态检查代替实际游戏验证；未实际运行的部分在交付时明确说明。
