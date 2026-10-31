import React from "react";
import {useCurrentFrame, useVideoConfig} from "remotion";
import {Caption, palette, ramp, Scene} from "./Cinema";
import {BlazeBurner, Label as SvgText} from "./WorkshopIllustrations";

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
  {[-165, 0, 165].map(offset => <BlazeBurner key={offset} x={offset} y={731} time={time}/>)}
  <SvgText x={0} y={850} size={21}>{sealed ? "隔绝空气" : "接触空气"}</SvgText>
</g>;

export const MoltenChemistry: React.FC = () => {
  const t = useCurrentFrame() / useVideoConfig().fps;
  const comparison = ramp(t, 2.5, 3.3);
  const crystals = ramp(t, 4, 8);
  const separation = ramp(t, 13, 13.35);
  return <Scene duration={15} label="异变之后 / 空气留下的反应">
    <svg width="1920" height="1080" viewBox="0 0 1920 1080" style={{position: "absolute", inset: 0}}>
      <SvgText x={960} y={212} color={palette.paper} size={35}>{t < 2.9 ? "金属，进入另一种形态" : t < 13 ? "相同的金属，相同的炉火" : "结晶尚在，金属仍在流动"}</SvgText>
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
        <g opacity={ramp(t, 3.4, 4.2)}>
          {Array.from({length: 7}, (_, i) => <g key={i} opacity={.18 + i % 3 * .06}><path d={`M${275 + i * 65} 285 q65 24 30 80`} fill="none" stroke="#b8b4c5" strokeWidth="2"/><circle cx={305 + i * 65} cy={290 + ((t * 23 + i * 15) % 99)} r="3" fill="#d2c3db"/></g>)}
        </g>
        <g opacity={ramp(t, 8.5, 9.2)}>
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
          const progress = ramp(t, 13.35, 14.75);
          return <Crystal key={i} x={600 + i * 75 + progress * 210} y={570 - progress * (145 + i % 3 * 30)} angle={i * 25 + t * 5} scale={.8}/>;
        })}
        <SvgText x={960} y={770} size={21}>仍有方法，将它们分离。</SvgText>
      </g>
    </svg>
    <Caption end={2.9}>金属依旧会在高温中熔化。</Caption>
    <Caption start={3.3} end={7.8}>可如今，空气中多了某些东西。</Caption>
    <Caption start={8.2} end={12.9}>它们留下的结晶，连炉火也难以抹去。</Caption>
  </Scene>;
};
