import React from "react";
import {useCurrentFrame, useVideoConfig} from "remotion";
import {Caption, Gear, palette, ramp, Scene} from "./Cinema";

const SvgText: React.FC<{x: number; y: number; children: React.ReactNode; color?: string; size?: number}> = ({x, y, children, color = palette.muted, size = 23}) => <text x={x} y={y} textAnchor="middle" fill={color} fontSize={size} letterSpacing="3">{children}</text>;

const Machine: React.FC<{x: number; active: number}> = ({x, active}) => <g transform={`translate(${x} 390)`}>
  <path d="M0 0 L38 -28 H208 L170 0Z" fill="#667b7b"/><path d="M170 0 L208 -28 V172 L170 200Z" fill="#283c43"/>
  <rect width="170" height="200" fill="#42575a" stroke="#82938b" strokeWidth="3"/>
  <rect x="23" y="26" width="124" height="77" fill="#13282c" stroke="#8b9d91" strokeWidth="2"/>
  {Array.from({length: 5}, (_, i) => <rect key={i} x={37 + i * 21} y={80 - active * (15 + i % 3 * 12)} width="10" height={8 + active * (15 + i % 3 * 12)} fill={palette.teal} opacity={.3 + active * .7}/>)}
  {[127, 143, 159].map(y => <path key={y} d={`M26 ${y} H143`} stroke="#23383c" strokeWidth="7"/>)}
</g>;

export const PowerConnections: React.FC = () => {
  const t = useCurrentFrame() / useVideoConfig().fps;
  const network = ramp(t, 4.5, 5.4);
  const active = ramp(t, .8, 2.5);
  return <Scene duration={13} label="异变之后 / 动力的联系">
    <div style={{position: "absolute", inset: 0, opacity: .15, backgroundImage: "linear-gradient(#65847b 1px, transparent 1px), linear-gradient(90deg,#65847b 1px, transparent 1px)", backgroundSize: "72px 72px", maskImage: "radial-gradient(ellipse,black,transparent 70%)"}}/>
    <svg viewBox="0 0 1920 1080" width="1920" height="1080" style={{position: "absolute", inset: 0}}>
      <SvgText x={960} y={218} color={palette.paper} size={35}>{network < .5 ? "转动，驱动了另一种机器" : "应用机动 · 让转动沿网络传递"}</SvgText>
      <g opacity={1 - network}>
        <path d="M500 508 H1290" stroke="#8d8d77" strokeWidth="18"/><path d="M500 502 H1290" stroke="#bfb294" strokeWidth="3"/>
        {Array.from({length: 10}, (_, i) => <path key={i} d={`M${560 + i * 64} ${508 + Math.sin(t * 4) * 6} v12`} stroke="#4c6463" strokeWidth="4"/>)}
        <Gear x={465} y={508} radius={100} angle={t * 70}/><Machine x={1290} active={active}/>
        <SvgText x={465} y={710}>机械动力 · SU</SvgText><SvgText x={1380} y={710}>原本使用 FE 的加工设备</SvgText>
      </g>
      <g opacity={network}>
        <path d="M420 508 H635 M1280 508 H1480" stroke="#b6a180" strokeWidth="16"/>
        <Gear x={410} y={508} angle={t * 70} radius={84}/>
        <rect x="620" y="427" width="120" height="162" fill="#3d5154" stroke="#d99765" strokeWidth="3"/>
        <rect x="1195" y="427" width="120" height="162" fill="#3d5154" stroke="#7fb8b1" strokeWidth="3"/>
        <path d="M740 508 H865 M1055 508 H1195" stroke="#344f57" strokeWidth="14"/>
        <path d="M740 508 H865 M1055 508 H1195" stroke="#8cbbb8" strokeWidth="2"/>
        {Array.from({length: 7}, (_, i) => {
          const p = (t * .23 + i / 7) % 1;
          const x = 744 + p * 444;
          return <rect key={i} x={x} y="503" width="9" height="10" fill="#d2e3cd" opacity={x > 855 && x < 1065 ? 0 : .8}/>;
        })}
        <rect x="865" y="402" width="190" height="212" rx="3" fill="#233b43" stroke="#a3bfc0" strokeWidth="3"/>
        {[0, 1, 2, 3].map(i => <g key={i}><rect x="888" y={427 + i * 40} width="144" height="24" fill="#142a31" stroke="#5d7f82"/><rect x="1009" y={433 + i * 40} width="10" height="10" fill={palette.teal} opacity={.5 + .4 * Math.sin(t * 3 + i)}/></g>)}
        <Gear x={1480} y={508} angle={-t * 70} radius={84} color={palette.teal}/>
        <SvgText x={680} y={690}>输入端</SvgText><SvgText x={960} y={690}>ME 网络 · 动力分配</SvgText><SvgText x={1255} y={690}>输出端</SvgText>
        <SvgText x={960} y={755} size={18}>动力来自输入，网络将它传向另一端。</SvgText>
      </g>
    </svg>
    <Caption end={5.2}>可转动与能量之间，已有了新的联系。</Caption>
    <Caption start={5.5} end={12.7}>机械的力量，也能沿着网络传向另一端。</Caption>
  </Scene>;
};

const Crystal: React.FC<{x: number; y: number; scale?: number; angle?: number}> = ({x, y, scale = 1, angle = 0}) => <g transform={`translate(${x} ${y}) rotate(${angle}) scale(${scale})`}>
  <path d="M0 -19 L12 -4 L7 15 L-7 18 L-13 -1Z" fill="#d2becb" stroke="#f7e8e1" strokeWidth="1.5"/><path d="M0 -19 L-2 1 L7 15 M-13 -1 L-2 1 L12 -4 M-2 1 L-7 18" fill="none" stroke="#8d7b9c" strokeWidth="1"/>
</g>;

/** Conceptual section, not a specified in-game tank, seal or recipe. */
const MeltSample: React.FC<{x: number; time: number; sealed: boolean; color: string; crystalAmount: number; id: string}> = ({x, time, sealed, color, crystalAmount, id}) => <g transform={`translate(${x} 0)`}>
  <defs>
    <linearGradient id={`${id}-metal`} x1="0" y1="0" x2="0" y2="1"><stop stopColor="#ffe5a6"/><stop offset=".22" stopColor={color}/><stop offset="1" stopColor="#873e30"/></linearGradient>
    <clipPath id={`${id}-clip`}><rect x="-245" y="423" width="490" height="228"/></clipPath>
  </defs>
  <ellipse cx="0" cy="600" rx="310" ry="170" fill={color} opacity=".055"/>
  <path d="M-260 392 V666 H260 V392" fill="#1b2a2e" stroke="#98a5a0" strokeWidth="5"/>
  <g clipPath={`url(#${id}-clip)`}>
    <path d={`M-250 437 Q-145 ${421 + Math.sin(time * 2) * 7} -30 437 T250 437 V660 H-250Z`} fill={`url(#${id}-metal)`}/>
    {Array.from({length: 7}, (_, i) => <path key={i} d={`M${-430 + ((time * 24 + i * 97) % 740)} ${467 + i % 4 * 43} q60 -19 130 0`} fill="none" stroke="#ffe3ac" strokeWidth="3" opacity=".32"/>)}
    {Array.from({length: 23}, (_, i) => <g key={i} opacity={ramp(crystalAmount, i / 30, i / 30 + .2)}><Crystal x={-217 + ((i * 79 + time * 12) % 430)} y={470 + i % 4 * 44 + Math.sin(time * 1.3 + i) * 9} angle={time * 9 + i * 31} scale={.5 + i % 3 * .15}/></g>)}
  </g>
  {sealed && <g><path d="M-270 390 H270" stroke="#b5c3bb" strokeWidth="12"/><path d="M-256 382 H256" stroke="#334d51" strokeWidth="3"/><rect x="-45" y="365" width="90" height="17" fill="#718980"/></g>}
  {Array.from({length: 6}, (_, i) => <path key={i} d={`M${-185 + i * 73} 730 q${Math.sin(time * 4 + i) * 14} -15 0 -31 t0 -32`} stroke="#e8a161" strokeWidth="4" fill="none" opacity={.5 + Math.sin(time * 3 + i) * .17}/>)}
  <path d="M-240 742 H240" stroke="#b78c67" strokeWidth="5"/>
  <SvgText x={0} y={803} size={21}>{sealed ? "隔绝空气" : "接触空气"}</SvgText>
</g>;

export const MoltenChemistry: React.FC = () => {
  const t = useCurrentFrame() / useVideoConfig().fps;
  const comparison = ramp(t, 3, 4);
  const crystals = ramp(t, 5, 9.5);
  const separation = ramp(t, 16, 16.35);
  return <Scene duration={18} label="异变之后 / 空气留下的反应">
    <svg width="1920" height="1080" viewBox="0 0 1920 1080" style={{position: "absolute", inset: 0}}>
      <SvgText x={960} y={212} color={palette.paper} size={35}>{t < 3.4 ? "金属，进入另一种形态" : t < 16 ? "相同的金属，相同的炉火" : "结晶尚在，金属仍在流动"}</SvgText>
      <g opacity={1 - comparison}>
        {[{x: 450, name: "铜", color: "#e39157"}, {x: 960, name: "铁", color: "#d77a42"}, {x: 1470, name: "金", color: "#edc064"}].map((metal, i) => {
          const melt = ramp(t, .35 + i * .15, 1.7 + i * .15);
          return <g key={metal.name} transform={`translate(${metal.x} 480)`}>
            <ellipse cy="110" rx={105 + melt * 48} ry={30 + melt * 9} fill={metal.color} opacity={melt}/>
            <path d="M-85 -36 L-35 -65 H90 L45 -36Z M45 -36 H-85 V24 H45Z M45 -36 L90 -65 V-5 L45 24Z" fill={metal.color} stroke="#f7d9a0" strokeWidth="2" opacity={1 - melt} transform={`translate(0 ${melt * 110}) scale(1 ${1 - melt * .8})`}/>
            <SvgText x={0} y={225}>{metal.name}</SvgText>
          </g>;
        })}
      </g>
      <g opacity={comparison * (1 - separation)}>
        <MeltSample x={580} time={t} sealed={false} color="#d77a42" crystalAmount={crystals} id="exposed"/>
        <MeltSample x={1340} time={t} sealed color="#d77a42" crystalAmount={0} id="isolated"/>
        <g opacity={ramp(t, 4, 5)}>
          {Array.from({length: 7}, (_, i) => <g key={i} opacity={.18 + i % 3 * .06}><path d={`M${275 + i * 65} 285 q65 24 30 80`} fill="none" stroke="#b8b4c5" strokeWidth="2"/><circle cx={305 + i * 65} cy={290 + ((t * 23 + i * 15) % 99)} r="3" fill="#d2c3db"/></g>)}
        </g>
        <g opacity={ramp(t, 10.5, 11.2)}>
          <path d="M840 565 H955" stroke="#b9a5b2" strokeWidth="2" strokeDasharray="6 7"/><path d="M942 554 L955 565 L942 576" fill="none" stroke="#b9a5b2" strokeWidth="2"/>
          <circle cx="1008" cy="565" r="26" fill="none" stroke="#b9a5b2" strokeWidth="2"/>
          <path d="M1000 556 H1016 M1000 574 H1016" stroke="#b9a5b2" strokeWidth="3"/>
          <SvgText x={963} y={630} size={17}>后续反应停滞</SvgText>
        </g>
      </g>
      <g opacity={separation}>
        <SvgText x={960} y={330} size={18}>特殊加工 · 局部示意</SvgText>
        <path d={`M420 585 Q630 ${560 + Math.sin(t * 3) * 7} 850 585 T1450 585`} fill="none" stroke="#d77a42" strokeWidth="65"/>
        <path d="M420 575 Q630 555 850 575 T1450 575" fill="none" stroke="#ffd99b" strokeWidth="3"/>
        {Array.from({length: 8}, (_, i) => {
          const progress = ramp(t, 16.35, 17.75);
          return <Crystal key={i} x={600 + i * 75 + progress * 210} y={570 - progress * (145 + i % 3 * 30)} angle={i * 25 + t * 5} scale={.8}/>;
        })}
        <SvgText x={960} y={770} size={21}>仍有方法，将它们分离。</SvgText>
      </g>
    </svg>
    <Caption end={3.5}>金属依旧会在高温中熔化。</Caption>
    <Caption start={4} end={9.3}>可如今，空气中多了某些东西。</Caption>
    <Caption start={9.7} end={15.9}>它们留下的结晶，连炉火也难以抹去。</Caption>
  </Scene>;
};
