import React from "react";
import {AbsoluteFill,Audio,Sequence,staticFile,useVideoConfig} from "remotion";
import {PromoScene} from "./PromoScene";
export const Promo:React.FC=()=>{const {fps}=useVideoConfig();return <AbsoluteFill>
<Audio src={staticFile("promo/audio/narration.wav")} volume={1}/>
<Audio src={staticFile("promo/audio/score.wav")} volume={.8}/>
<Sequence name="00:00 天地异变" from={0 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={0}/></Sequence>
<Sequence name="00:06 目光之外" from={6 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={1}/></Sequence>
<Sequence name="00:12 齿轮坠落" from={12 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={2}/></Sequence>
<Sequence name="00:18 三万年后" from={18 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={3}/></Sequence>
<Sequence name="00:24 维度工序" from={24 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={4}/></Sequence>
<Sequence name="00:30 力量与代价" from={30 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={5}/></Sequence>
<Sequence name="00:36 工业的边界" from={36 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={6}/></Sequence>
<Sequence name="00:42 七项工业额度" from={42 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={7}/></Sequence>
<Sequence name="00:48 按顺序修复" from={48 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={8}/></Sequence>
<Sequence name="00:54 全链齿轮之心" from={54 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={9}/></Sequence>
<Sequence name="01:00 用工程组织生产" from={60 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={10}/></Sequence>
<Sequence name="01:06 轴驱动工业机器" from={66 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={11}/></Sequence>
<Sequence name="01:12 发电设施保留运行条件" from={72 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={12}/></Sequence>
<Sequence name="01:18 大型能源与护盾" from={78 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={13}/></Sequence>
<Sequence name="01:24 超速变成真实产能" from={84 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={14}/></Sequence>
<Sequence name="01:30 等级、并行与转速" from={90 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={15}/></Sequence>
<Sequence name="01:36 能量板的装配线" from={96 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={16}/></Sequence>
<Sequence name="01:42 看清网络，再调整" from={102 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={17}/></Sequence>
<Sequence name="01:48 让应力沿 ME 网络传递" from={108 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={18}/></Sequence>
<Sequence name="01:54 两条供能各司其职" from={114 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={19}/></Sequence>
<Sequence name="02:00 输入停止，输出就停止" from={120 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={20}/></Sequence>
<Sequence name="02:06 DDR1 → DDR5" from={126 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={21}/></Sequence>
<Sequence name="02:12 五代 · 六类 · 三十张卡" from={132 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={22}/></Sequence>
<Sequence name="02:18 把制造接入自动化" from={138 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={23}/></Sequence>
<Sequence name="02:24 两种过载，分别检查" from={144 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={24}/></Sequence>
<Sequence name="02:30 铁：研磨、压制、结合" from={150 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={25}/></Sequence>
<Sequence name="02:36 有限样本，持续供料" from={156 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={26}/></Sequence>
<Sequence name="02:42 铜：加工与化学相接" from={162 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={27}/></Sequence>
<Sequence name="02:48 先保住回流，再分配余量" from={168 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={28}/></Sequence>
<Sequence name="02:54 一份原料，两条供应链" from={174 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={29}/></Sequence>
<Sequence name="03:00 热源也接入生产体系" from={180 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={30}/></Sequence>
<Sequence name="03:06 金属进入流体工艺" from={186 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={31}/></Sequence>
<Sequence name="03:12 十二种新增化学品" from={192 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={32}/></Sequence>
<Sequence name="03:18 同样的原料，不同的热态" from={198 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={33}/></Sequence>
<Sequence name="03:24 进气、出气与专用盆盖" from={204 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={34}/></Sequence>
<Sequence name="03:30 露天结晶：先看天空" from={210 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={35}/></Sequence>
<Sequence name="03:36 转速决定压力等级" from={216 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={36}/></Sequence>
<Sequence name="03:42 下界合金的工业制造" from={222 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={37}/></Sequence>
<Sequence name="03:48 用读数理解产线" from={228 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={38}/></Sequence>
<Sequence name="03:54 物品、方块、生物、玩家" from={234 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={39}/></Sequence>
<Sequence name="04:00 玩家也进入运输线路" from={240 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={40}/></Sequence>
<Sequence name="04:06 属于你的中转领地" from={246 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={41}/></Sequence>
<Sequence name="04:12 把自己送往另一个维度" from={252 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={42}/></Sequence>
<Sequence name="04:18 跨维方式有自己的边界" from={258 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={43}/></Sequence>
<Sequence name="04:24 两秒蓄力，沿视线发射" from={264 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={44}/></Sequence>
<Sequence name="04:30 竖直带也能运送你" from={270 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={45}/></Sequence>
<Sequence name="04:36 暮色：从环境中取得原料" from={276 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={46}/></Sequence>
<Sequence name="04:42 活体工位，持续供料" from={282 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={47}/></Sequence>
<Sequence name="04:48 加工、回用与效率" from={288 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={48}/></Sequence>
<Sequence name="04:54 暮色材料汇入十步装配" from={294 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={49}/></Sequence>
<Sequence name="05:00 工厂之外，同样丰富" from={300 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={50}/></Sequence>
<Sequence name="05:06 魔法皮蛋：选择效果" from={306 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={51}/></Sequence>
<Sequence name="05:12 效果有明确的代价" from={312 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={52}/></Sequence>
<Sequence name="05:18 日常工具与运行规则" from={318 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={53}/></Sequence>
<Sequence name="05:24 专用跨维物流 · 开发预览" from={324 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={54}/></Sequence>
<Sequence name="05:30 频道库存的回收路径" from={330 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={55}/></Sequence>
<Sequence name="05:36 六洞穴的长期工业价值" from={336 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={56}/></Sequence>
<Sequence name="05:42 相位、模块与稳定化" from={342 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={57}/></Sequence>
<Sequence name="05:48 考核真实的工程能力" from={348 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={58}/></Sequence>
<Sequence name="05:54 维度工序 · 由你来设计" from={354 * fps} durationInFrames={6 * fps} premountFor={fps}><PromoScene shotId={59}/></Sequence>
</AbsoluteFill>;};
