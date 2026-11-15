# 维度工序视频工程

## 六分钟 3D 宣传片

`DimensionWorksPromo` 为 1920×1080、30 fps、10,800 帧（6:00）。按[原企划](../docs/design/six-minute-promo-content-plan.md)保留 11 章、60 镜头，重新安排为 3–9 秒的不同长度。具体镜头、母题与内容状态见[3D 制作稿](../docs/design/six-minute-promo-3d.md)。`DimensionWorksIntro` 仍为独立的 90 秒历史工程。

主体使用 Three.js / React Three Fiber / `@remotion/three` 的真实透视场景。方块从 Minecraft 与对应模组的 JSON、OBJ、Java 方块实体模型或 Bedrock 实体几何烘焙，物品按原贴图的透明轮廓挤出。当前资源集包含 217 个模型、325 张贴图；人物、机器、传动轴、压力机压头、搅拌头、青蛙、锁链与包裹均有实际几何，保留像素贴图的最近邻采样。

铜色传动线作为母题：前 3 秒在第28、56、84帧完成线、环、齿轮与分支线路的转场，后续作为传动、回流、ME线路和包裹路径复现。进场使用显式参数弹簧，同时改变位置、缩放与角度；组内间隔4帧，出场10帧。背景叠加渐变纹理、3D内容、颗粒和暗角，镜头中包含6帧的视觉重音。持续动作具有不同的周期或相位。

### 准备与预览

本次环境为 macOS、Python 3.13、Node.js 24、FFmpeg。首次安装 Python 依赖建议使用独立虚拟环境，避免改动系统环境：

```sh
python3 -m venv ~/.cache/dimensionworks-promo-3d/venv
source ~/.cache/dimensionworks-promo-3d/venv/bin/activate
python -m pip install -r requirements-promo.txt
npm ci
npm run promo:models
npm run promo:audio
npx remotion studio src/index.ts --no-open
```

默认读取 `~/Library/Application Support/minecraft/versions/DimensionWorks/mods` 与同安装目录的 Minecraft 1.20.1 资源档案。可向模型脚本传入 `--minecraft 路径 --instance 实例名`。仓库自研资源优先于已安装的旧 JAR；第三方资源只从脚本所列固定版本提取，风力机 Java 模型固定到 Mekanism `v1.20.1-10.4.16.80` 标签。缺失资源与未支持的加载器会报错，需要核对原路径。

`public/promo/3d/` 包含烘焙模型、原资产副本、纹理与 `sources.json` 来源/哈希记录，整个目录不提交。第三方资源遵循原模组许可，不作为仓库原创代码分发。没有修改游戏文件或整合包清单。

Studio 选择 `/DimensionWorksPromo`。`PromoScenes` 中60个单镜头合成与主时间轴使用相同起止时长。主时间轴仍使用显式 JSX Sequence，便于逐镜头编辑。

### 配音与内存

配音使用免费的 Edge 神经语音 `zh-CN-YunxiNeural`，正常语速 `+0%`，不调用 macOS `say`。第一次合成需要联网；59段请求逐个执行并缓存，之后可复用。旁白经过精简，镜头时间按实际录音长度分配，不通过音频加速塞进固定时长。

字幕来自语音服务的实测句边界，保存在 `src/promo/captions.json`。原创合成配乐与旁白进行侧链避让，立体声混音使用两遍实测响度归一化；`mix.wav` 为360秒、48 kHz立体声，主片仅使用这条新混音。SRT与章节表位于 `public/promo/audio/`。

模型按场景载入纹理，地面使用实例化网格，几何与材质复用，Canvas按需绘制，不让静止预览持续空转。先完成配音，再渲染视频；导出固定单并发、Node堆上限1.5 GB、编码2线程，不启动 Minecraft、Blender或本地大型语音模型。

### 自检与导出

```sh
npx tsc --noEmit
npm run promo:check
npm run promo:qa
npm run promo:sample
npm run promo:render
python scripts/extract_promo_frames.py out/dimensionworks-promo-3d.mp4
```

`promo:check` 使用 TypeScript AST 检查当前宣传片引用的源码中所有 `interpolate` 的 easing/clamp、spring参数，核对60个不同长度镜头、注册时长、实测字幕与资源路径，并检查视觉事件间隔。历史90秒片没有被本次改成3D，不在本次动效规则检查的范围内。

`promo:qa` 使用一个浏览器逐个渲染60个镜头和片头第1、15、30、45、60、85、89帧，输出到忽略的 `out/qa/`。成片后 `extract_promo_frames.py` 的 `extractFrames` 再从实际MP4抽出指定帧与每镜头代表帧。需要人工看构图、遮挡和液面等内容；源码检查不能替代画面检查。

`promo:render` 输出 `out/dimensionworks-promo-3d.mp4`，H.264 / AAC，CRF18、1080p。追加绝对输出路径可改变位置。发布到桌面前须检查时长、帧率、帧数、全片解码、响度与成片抽帧结果。

2026-11-15 成片验证：TypeScript、Python、JSON、依赖锁与动效规则检查通过，视觉事件最大间隔62帧。实际MP4的10,800帧与完整音轨解码通过，第1、15、30、45、60帧及60个镜头代表帧已检查构图。最终混音为−16.2 LUFS、−1.35 dBTP；音轨校准后66个抽帧校验和保持一致。桌面成片与工程导出文件的SHA-256一致。

### 内容边界

当前稿同步了一级跨维端点与物质球解包器的生存配方、云盘回收路径、原版能量板与 `low_loss` 卡型。二、三级端点及最终卡仍暂无生存配方；六洞穴旅行已开放，渊海工业体系有首版，其余洞穴工业内容与工业考核仍按开发状态说明。视频中的结构剖面、压力检查、结晶和长时间效果均标明示意或时间压缩，视频验证不代表游戏内运行验证。

### 维护入口

- `src/promo/shots.json`：镜头内容、正常语速旁白和实际起止时间；保留原企划的动作描述供对照。
- `src/promo/model-sources.json`、`scripts/prepare_promo_models.py`：实际资产路径和轻量模型烘焙。
- `src/promo/CoreWorlds.tsx`、`ProcessWorlds.tsx`、`TransitWorlds.tsx`：各章节的3D场景与机械动作。
- `src/promo/scene-plan.ts`、`motion.ts`：取景、视觉事件、弹簧与显式缓动。
- `scripts/generate_promo_neural_audio.py`：自然语速旁白、实测字幕、配乐与新混音。
- `scripts/render_promo.mjs`、`check_promo.mjs`、`extract_promo_frames.py`：单并发渲染与完整自检入口。

## 既有 90 秒开场

以下保留90秒开场的历史说明；其制作阶段的限制不适用于上述已获授权并导出的六分钟宣传片。

Remotion 合成 `DimensionWorksIntro` 固定为 **1920×1080、30 fps、2700 帧（90 秒）**。
主题是「异变留在了世界的运作之中」，整体保持宁静、陌生与好奇，不为玩家规定使命或唯一工业路线。

## 时间轴

| 时间 | 内容 |
| --- | --- |
| 00:00–00:06 | 普通天地与当年的异象 |
| 00:06–00:13 | 鲜活心脏改写为齿轮之心 |
| 00:13–00:17 | 齿轮坠落，史蒂夫失去意识 |
| 00:17–00:24 | 7 秒昼夜与草木，三万年流逝 |
| 00:24–00:33 | 苏醒与交互距离 +2、急迫 II、虚弱 I、夜视 I |
| 00:33–00:39 | 创造马达经传动杆驱动 Mekanism 工厂；九类工厂轮流高亮 |
| 00:39–00:55 | 输入齿轮箱—玻璃线缆—ME 控制器—玻璃线缆—输出齿轮箱—机械压力机；AE 供能与 DDR 两条附加支路 |
| 00:55–01:10 | DDR1–5 卡槽/基础 RPM、六类共 30 张卡与倍率/单卡损耗 |
| 01:10–01:25 | 同温同种熔融金属的空气接触对照；每锅三个正常燃烧的烈焰人燃烧室；最后两秒提示特殊加工分离 |
| 01:25–01:27 | 晶体细响接回心脏与自然景象 |
| 01:27–01:30 | 正式片名「维度工序 · DimensionWorks」 |

## 叙事与实现边界

- 自然景象是普通方块化风景，未设定新地形、工业遗迹、野外工厂或其他建造者。动力和金属段采用抽离背景的独立机制插画，不代表出生地已有设备。
- 动力段依据当前 Mekanism 机械供能与修改后的应用机动：展示输入、传递、分配和输出，不宣称所有设备均可任意双向转换 SU/FE。ME 网络不凭空发电，也不把应力表现为可无限积累的库存。
- 当前脚本注册 21 种熔融材料；画面选取铜、铁、金作为代表，不逐项罗列。
- **空气异常物质、熔融结晶、空气隔绝与特殊加工分离是目标版本的机制预告，本视频没有实现这些游戏机制。** 不代表当前已可玩或已完成游戏内验证。
- 结晶金属保持液态、热光与流动纹理；晶体随流体移动，不表现成冷却凝固、永久报废或管道必然堵塞。同温同种金属保持一致的流动速度，暴露样本逐渐出现晶体，隔绝样本保持原状。
- 容器剖面和气流是关系示意，不规定游戏里的密封判定、容器方块或施工方式。结晶处理仅给出局部分离示意，不指定方块、名称、配方或普通过滤方法。
- 心脏与结晶只用声音呼应，不确认共同成因或共同修复手段。不展示七碎片顺序、配方与工业额度清单；饰品面板也是电影化重建，并非游戏录屏。苏醒段明确列出实际效果，虚弱使用负面效果配色，不作为增益宣传。

## 本地预览与静态检查

需要 Node.js 20+、Python 3.10+，已有素材可直接启动 Studio：

```sh
npm install
npx remotion studio src/index.ts --no-open
npx tsc --noEmit
git diff --check
```

打开 Studio 输出地址下的 `/DimensionWorksIntro`。十一个片段有中文时间轴名称，可以直接定位查看。
所有运动、字幕渐变和场景转场由帧驱动；中文字体优先使用 macOS 的 PingFang SC，回退 Microsoft YaHei 与系统无衬线字体。

`npm run studio` 会先运行素材生成，再打开 Studio。心脏素材生成需要 `python3 -m pip install -r requirements.txt`；音景脚本仅使用 Python 标准库。

本机仅运行 Studio 预览与静态检查，不编译或打包整合包，不导出 MP4；涉及 Mod 的改动由 GitHub Actions 正式发布工作流构建和校验。保留的 `npm run render` 是以后明确要求导出时使用的入口，本次不执行。

## 原创素材与声音

- 正常、破损的 32×32 心脏贴图复制自仓库 KubeJS 资源。`scripts/generate_assets.py` 保留正常心脏的透明轮廓，生成红色变形前素材；不引入第三方图片。
- 方块化风景、机械部件、网络和金属反应均为代码绘制的原创插画，未复制第三方模组模型或贴图。
- `scripts/generate_soundscape.py` 以固定随机种子生成 `public/audio/intro-soundscape.wav`，为 90 秒、22,050 Hz、PCM16 单声道原创合成音景。包括风、水声质感、鸟鸣、双拍心跳、轻微机械声、加热噪声与晶体共振，无配音和外部录音。
- 声音脚本只生成视频音频素材，不调用任何 Mod 或整合包构建。可独立运行 `python3 scripts/generate_soundscape.py` 重建；音频作为源素材随项目保留，避免首次预览缺失音轨。
- `voiceover.json` 保存与各镜头现有字幕一致的旁白句子和字幕时间窗；`scripts/generate_voiceover.py` 使用小米 MiMo Token Plan 的 `mimo-v2.5-tts`、预置中文音色“茉莉”合成分镜音频，并按时间窗组装为 90 秒单轨 WAV。各段统一采用平稳、自然的叙述口吻，保留轻快明亮感但不过度表演，不克隆具体角色。通过 `MIMO_API_KEY` 环境变量传入密钥；可选用 `MIMO_BASE_URL` 覆盖 Token Plan 地址。运行 `npm run voiceover` 后，旁白保存在 `public/audio/voiceover/`，项目不保存密钥。
- 旁白按时间窗放入现有 90 秒时间轴，背景音景降至较轻音量以留出人声空间。生成脚本会检查每段音频是否超出对应镜头时长，并写出实际时长清单。

## 内容核对依据

[整包内容与开场取材核对](CONTENT_AUDIT.md) 记录全部 142 个清单条目、主要魔改入口、访问限制与本片取舍。该记录是清单及源文件静态核对，不代表逐个模组完成了游戏内测试。

## 具体设备示意与参数

- 机械供能使用创造马达作为演示源，经旋转传动杆接入工厂。Mekanism 10.4.16.80 的九类工艺为熔炼、富集、粉碎、压缩、融合、提纯、压射、灌注、锯木；同时说明基础、高级、精英、终极四级。工厂插画展示多加工槽，不再用单台冶炼厂代表全部工厂。
- ME 主链与两条支路使用同样的玻璃线缆插画。AE 供能仅展示应用机动动能接收器（机械应力 → AE）及 AE2 谐振仓（燃料 → AE），不展示外部 FE 输入和创造能源元件。AE 供能标签位于上方支路旁，输出齿轮箱箭头朝向右侧压力机。
- DDR 支路逐代切换展示，仅一台接入示意网络；清楚注明同一逻辑 ME 网络最多两台有效驱动器。供能与驱动器就绪后，输出轴和压力机才开始运动。
- 五代驱动器依次有 4/8/16/32/64 卡槽，基础转速额度为 256/512/1024/2048/4096 RPM。六类卡为经济、均衡、高速、低损耗、劣质、最终；卡型数值来自 `MemoryCardType`、`MemoryTier` 和随包 COMMON 配置。
- 各卡累计扣除全网应力，最高 99%；这与本次修改后的 Mod 默认值、配置范围和运行时硬上限一致。每卡扣除比例、25% 手工劣质概率及最终卡暂无生存配方保持原值。
- `scripts/generate_memory_assets.py` 只从本仓库自研 Mod 同步 5 张驱动器前面贴图和 30 张卡贴图至 `public/memory/`。其他设备为原创带顶面/侧面的方块插画，没有复制第三方模型和贴图。
