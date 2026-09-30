# DimensionWorks 开场动画

这是一段 1920x1080、30 fps、34.4 秒的 Remotion 开场动画。内容根据当前整合包设定编写，不依赖外部素材或音乐。

## 叙事

1. **齿轮停摆**：受诅咒的齿轮之心触发 RPM、应力、产出与网络故障。
2. **维度钥匙**：一次性钥匙永久解锁 `dw:transfer`，独立中转工业维度建立。
3. **六维汇流**：磁场、原始、毒化、渊海、异寂、糖果六座洞穴工厂向中转节点输送效果流体与稳定基质。
4. **工业网络**：ME Memory 驱动跨维度机械动力，画面收束到 DimensionWorks 主标题。

## 对应设定

- 中转维度与世界形式：`mods/dimensionworks-transfer/README.md`
- 六维工厂、效果流体和稳定基质：`docs/superpowers/specs/2026-09-27-dimension-cave-factories-design.md`
- ME Memory 的 SU / RPM 标定：`docs/superpowers/specs/2026-09-30-me-memory-card-six-archetypes.md`
- 齿轮之心与 RPM 限制：`mods/dimensionworks-rpm-limit/README.md`

## 使用

```sh
cd opening
npm install
npm run score
npm run dev
```

渲染视频：

```sh
npm run render
```

渲染首帧海报：

```sh
npm run still
```

动画中的齿轮之心与维度钥匙直接引用仓库已有的原创贴图。音轨由 `scripts/generate-opening-score.mjs` 在本地合成，不包含第三方音乐。
