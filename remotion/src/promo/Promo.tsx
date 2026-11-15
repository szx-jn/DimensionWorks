import React from 'react';
import {AbsoluteFill, Audio, Sequence, staticFile} from 'remotion';
import {PromoScene} from './PromoScene';

export const Promo:React.FC=()=> <AbsoluteFill>
  <Audio src={staticFile('promo/audio/mix.wav')}/>
  <Sequence from={0} durationInFrames={120} name="00 · 天地异变"><PromoScene shotId={0}/></Sequence>
  <Sequence from={120} durationInFrames={150} name="01 · 目光之外"><PromoScene shotId={1}/></Sequence>
  <Sequence from={270} durationInFrames={90} name="02 · 齿轮坠落"><PromoScene shotId={2}/></Sequence>
  <Sequence from={360} durationInFrames={150} name="03 · 三万年后"><PromoScene shotId={3}/></Sequence>
  <Sequence from={510} durationInFrames={180} name="04 · 维度工序"><PromoScene shotId={4}/></Sequence>
  <Sequence from={690} durationInFrames={240} name="05 · 力量与代价"><PromoScene shotId={5}/></Sequence>
  <Sequence from={930} durationInFrames={195} name="06 · 工业的边界"><PromoScene shotId={6}/></Sequence>
  <Sequence from={1125} durationInFrames={195} name="07 · 七项工业额度"><PromoScene shotId={7}/></Sequence>
  <Sequence from={1320} durationInFrames={150} name="08 · 按顺序修复"><PromoScene shotId={8}/></Sequence>
  <Sequence from={1470} durationInFrames={180} name="09 · 全链齿轮之心"><PromoScene shotId={9}/></Sequence>
  <Sequence from={1650} durationInFrames={135} name="10 · 用工程组织生产"><PromoScene shotId={10}/></Sequence>
  <Sequence from={1785} durationInFrames={210} name="11 · 轴驱动工业机器"><PromoScene shotId={11}/></Sequence>
  <Sequence from={1995} durationInFrames={195} name="12 · 发电设施保留运行条件"><PromoScene shotId={12}/></Sequence>
  <Sequence from={2190} durationInFrames={150} name="13 · 大型能源与护盾"><PromoScene shotId={13}/></Sequence>
  <Sequence from={2340} durationInFrames={210} name="14 · 超速变成真实产能"><PromoScene shotId={14}/></Sequence>
  <Sequence from={2550} durationInFrames={180} name="15 · 等级、并行与转速"><PromoScene shotId={15}/></Sequence>
  <Sequence from={2730} durationInFrames={180} name="16 · 能量板的装配线"><PromoScene shotId={16}/></Sequence>
  <Sequence from={2910} durationInFrames={165} name="17 · 看清网络，再调整"><PromoScene shotId={17}/></Sequence>
  <Sequence from={3075} durationInFrames={240} name="18 · 让应力沿 ME 网络传递"><PromoScene shotId={18}/></Sequence>
  <Sequence from={3315} durationInFrames={180} name="19 · 两条供能各司其职"><PromoScene shotId={19}/></Sequence>
  <Sequence from={3495} durationInFrames={210} name="20 · 输入停止，输出就停止"><PromoScene shotId={20}/></Sequence>
  <Sequence from={3705} durationInFrames={210} name="21 · DDR1 → DDR5"><PromoScene shotId={21}/></Sequence>
  <Sequence from={3915} durationInFrames={210} name="22 · 五代 · 六类 · 三十张卡"><PromoScene shotId={22}/></Sequence>
  <Sequence from={4125} durationInFrames={180} name="23 · 把制造接入自动化"><PromoScene shotId={23}/></Sequence>
  <Sequence from={4305} durationInFrames={210} name="24 · 两种过载，分别检查"><PromoScene shotId={24}/></Sequence>
  <Sequence from={4515} durationInFrames={270} name="25 · 铁：研磨、压制、结合"><PromoScene shotId={25}/></Sequence>
  <Sequence from={4785} durationInFrames={150} name="26 · 有限样本，持续供料"><PromoScene shotId={26}/></Sequence>
  <Sequence from={4935} durationInFrames={195} name="27 · 铜：加工与化学相接"><PromoScene shotId={27}/></Sequence>
  <Sequence from={5130} durationInFrames={225} name="28 · 先保住回流，再分配余量"><PromoScene shotId={28}/></Sequence>
  <Sequence from={5355} durationInFrames={180} name="29 · 一份原料，两条供应链"><PromoScene shotId={29}/></Sequence>
  <Sequence from={5535} durationInFrames={135} name="30 · 热源也接入生产体系"><PromoScene shotId={30}/></Sequence>
  <Sequence from={5670} durationInFrames={180} name="31 · 金属进入流体工艺"><PromoScene shotId={31}/></Sequence>
  <Sequence from={5850} durationInFrames={135} name="32 · 十二种新增化学品"><PromoScene shotId={32}/></Sequence>
  <Sequence from={5985} durationInFrames={165} name="33 · 同样的原料，不同的热态"><PromoScene shotId={33}/></Sequence>
  <Sequence from={6150} durationInFrames={225} name="34 · 进气、出气与专用盆盖"><PromoScene shotId={34}/></Sequence>
  <Sequence from={6375} durationInFrames={255} name="35 · 露天结晶：先看天空"><PromoScene shotId={35}/></Sequence>
  <Sequence from={6630} durationInFrames={165} name="36 · 转速决定压力等级"><PromoScene shotId={36}/></Sequence>
  <Sequence from={6795} durationInFrames={210} name="37 · 下界合金的工业制造"><PromoScene shotId={37}/></Sequence>
  <Sequence from={7005} durationInFrames={150} name="38 · 用读数理解产线"><PromoScene shotId={38}/></Sequence>
  <Sequence from={7155} durationInFrames={165} name="39 · 物品、方块、生物、玩家"><PromoScene shotId={39}/></Sequence>
  <Sequence from={7320} durationInFrames={240} name="40 · 玩家也进入运输线路"><PromoScene shotId={40}/></Sequence>
  <Sequence from={7560} durationInFrames={210} name="41 · 属于你的中转领地"><PromoScene shotId={41}/></Sequence>
  <Sequence from={7770} durationInFrames={180} name="42 · 把自己送往另一个维度"><PromoScene shotId={42}/></Sequence>
  <Sequence from={7950} durationInFrames={150} name="43 · 跨维方式有自己的边界"><PromoScene shotId={43}/></Sequence>
  <Sequence from={8100} durationInFrames={240} name="44 · 两秒蓄力，沿视线发射"><PromoScene shotId={44}/></Sequence>
  <Sequence from={8340} durationInFrames={150} name="45 · 竖直带也能运送你"><PromoScene shotId={45}/></Sequence>
  <Sequence from={8490} durationInFrames={120} name="46 · 暮色：两项升级"><PromoScene shotId={46}/></Sequence>
  <Sequence from={8610} durationInFrames={180} name="47 · 一块木头，三个奖杯"><PromoScene shotId={47}/></Sequence>
  <Sequence from={8790} durationInFrames={150} name="48 · 升级后发运至六洞穴"><PromoScene shotId={48}/></Sequence>
  <Sequence from={8940} durationInFrames={180} name="49 · 暮色材料汇入十步装配"><PromoScene shotId={49}/></Sequence>
  <Sequence from={9120} durationInFrames={135} name="50 · 工厂之外，同样丰富"><PromoScene shotId={50}/></Sequence>
  <Sequence from={9255} durationInFrames={165} name="51 · 魔法皮蛋：选择效果"><PromoScene shotId={51}/></Sequence>
  <Sequence from={9420} durationInFrames={150} name="52 · 效果有明确的代价"><PromoScene shotId={52}/></Sequence>
  <Sequence from={9570} durationInFrames={150} name="53 · 日常工具与运行规则"><PromoScene shotId={53}/></Sequence>
  <Sequence from={9720} durationInFrames={210} name="54 · 物品、流体与动力，跨维协同"><PromoScene shotId={54}/></Sequence>
  <Sequence from={9930} durationInFrames={150} name="55 · 频道库存的回收路径"><PromoScene shotId={55}/></Sequence>
  <Sequence from={10080} durationInFrames={150} name="56 · 六洞穴的长期工业价值"><PromoScene shotId={56}/></Sequence>
  <Sequence from={10230} durationInFrames={180} name="57 · 相位、模块与稳定化"><PromoScene shotId={57}/></Sequence>
  <Sequence from={10410} durationInFrames={165} name="58 · 考核真实的工程能力"><PromoScene shotId={58}/></Sequence>
  <Sequence from={10575} durationInFrames={225} name="59 · 维度工序 · 由你来设计"><PromoScene shotId={59}/></Sequence>
</AbsoluteFill>;
