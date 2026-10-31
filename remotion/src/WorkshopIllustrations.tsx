import React from "react";
import {staticFile} from "remotion";
import {Gear, palette} from "./Cinema";

export const Label: React.FC<{x: number; y: number; children: React.ReactNode; size?: number; color?: string}> = ({x, y, children, size = 22, color = palette.muted}) => <text x={x} y={y} textAnchor="middle" fill={color} fontSize={size} letterSpacing="2">{children}</text>;

/** Original three-face illustrations; no third-party models or texture copies. */
const Case: React.FC<{width?: number; height?: number; front?: string; edge?: string; children?: React.ReactNode}> = ({width = 100, height = 100, front = "#526266", edge = "#a6b5a7", children}) => <>
  <path d={`M${-width / 2} ${-height / 2} l22 -15 h${width} l-22 15Z`} fill="#788887" stroke={edge} strokeWidth="2"/>
  <path d={`M${width / 2} ${-height / 2} l22 -15 v${height} l-22 15Z`} fill="#293e43" stroke={edge} strokeWidth="2"/>
  <rect x={-width / 2} y={-height / 2} width={width} height={height} fill={front} stroke={edge} strokeWidth="3"/>
  {children}
</>;

export const Motor: React.FC<{x: number; y: number; time: number; scale?: number}> = ({x, y, time, scale = 1}) => <g transform={`translate(${x} ${y}) scale(${scale})`}>
  <Case width={112} height={112} front="#64516f" edge="#bcabbc">
    <rect x="-46" y="-44" width="92" height="88" fill="#a18da4" stroke="#342e45" strokeWidth="4"/>
    <path d="M-42 -38 L42 38 M-42 38 L42 -38" stroke="#d0b0d2" strokeWidth="5"/>
    <Gear x={0} y={0} radius={33} angle={time * 110} color="#e1d0b2"/>
    <circle cx="-36" cy="-32" r="4" fill="#97d9b5"/>
  </Case>
  <rect x="56" y="-12" width="34" height="24" fill="#b6ae94" stroke="#3d4b4c" strokeWidth="2"/>
</g>;

export const Shaft: React.FC<{from: number; to: number; y: number; time: number; active?: boolean}> = ({from, to, y, time, active = true}) => <g>
  <rect x={from} y={y - 10} width={to - from} height="20" fill="#928d77" stroke="#d1c5a3" strokeWidth="2"/>
  <path d={`M${from} ${y + Math.sin(active ? time * 9 : 0) * 6} H${to}`} stroke={active ? "#ecce9c" : "#747d73"} strokeWidth="4"/>
  {Array.from({length: Math.max(1, Math.floor((to - from) / 38))}, (_, i) => <path key={i} d={`M${from + 20 + i * 38} ${y - 8} v16`} stroke="#596366" strokeWidth="3"/>)}
</g>;

export const GlassCable: React.FC<{path: string; time: number; active?: boolean; color?: string; opacity?: number}> = ({path, time, active = true, color = palette.teal, opacity = 1}) => <g opacity={opacity}>
  <path d={path} fill="none" stroke="#97adba" strokeWidth="17" strokeLinejoin="round" opacity=".35"/>
  <path d={path} fill="none" stroke="#263c4a" strokeWidth="10" strokeLinejoin="round"/>
  <path d={path} fill="none" stroke={active ? color : "#61757c"} strokeWidth="4" strokeDasharray="7 19" strokeDashoffset={active ? -time * 45 : 0}/>
</g>;

export const Gearbox: React.FC<{x: number; y: number; time: number; mode: "IMPORT" | "EXPORT"; active?: boolean}> = ({x, y, time, mode, active = true}) => <g transform={`translate(${x} ${y})`}>
  <Case front="#697976" edge="#c5b79a"><rect x="-40" y="-40" width="80" height="80" fill="#384c52"/>
    <Gear x={0} y={-4} radius={27} angle={active ? time * (mode === "IMPORT" ? 70 : -70) : 0} color={mode === "IMPORT" ? palette.copper : palette.teal}/>
    <path d="M-17 33 H15 l-8 -6 M15 33 l-8 6" fill="none" stroke="#e4d8b2" strokeWidth="3"/>
  </Case>
</g>;

export const Controller: React.FC<{x: number; y: number; time: number; online: boolean}> = ({x, y, time, online}) => <g transform={`translate(${x} ${y})`}>
  <Case width={122} height={122} front="#252c3d" edge={online ? "#a58bbd" : "#667680"}>
    <rect x="-48" y="-48" width="96" height="96" fill="#151e2b" stroke="#6b5a82" strokeWidth="6"/>
    {Array.from({length: 9}, (_, i) => <rect key={i} x={-35 + i % 3 * 25} y={-35 + Math.floor(i / 3) * 25} width="18" height="18" fill={online ? ["#ba82c6", "#75c5b2", "#c4b483"][(i + Math.floor(time)) % 3] : "#43495c"}/>)}
  </Case>
</g>;

export const FACTORY_TYPES = [
  {id: "smelting", name: "熔炼", color: "#dd9d60"},
  {id: "enriching", name: "富集", color: "#80b6a0"},
  {id: "crushing", name: "粉碎", color: "#adbac4"},
  {id: "compressing", name: "压缩", color: "#a29ac4"},
  {id: "combining", name: "融合", color: "#b5c97e"},
  {id: "purifying", name: "提纯", color: "#7cbacb"},
  {id: "injecting", name: "压射", color: "#ce9aab"},
  {id: "infusing", name: "灌注", color: "#c59b6a"},
  {id: "sawing", name: "锯木", color: "#a7b68d"},
];

export const FactoryBlock: React.FC<{x: number; y: number; time: number; active: boolean; type: number; scale?: number}> = ({x, y, time, active, type, scale = 1}) => {
  const factory = FACTORY_TYPES[type];
  return <g transform={`translate(${x} ${y}) scale(${scale})`}>
    <Case width={150} height={165} front="#697879" edge={active ? factory.color : "#a7b5ac"}>
      <rect x="-62" y="-69" width="124" height="139" fill="#243a43" stroke="#364d53" strokeWidth="3"/>
      {[0, 1, 2].map(i => <g key={i}>
        <rect x={-51 + i * 37} y="-53" width="29" height="75" fill="#14272e" stroke="#879b92" strokeWidth="2"/>
        <rect x={-44 + i * 37} y="-40" width="15" height="16" fill={factory.color} opacity={active ? .9 : .3}/>
        <path d={`M${-36 + i * 37} -13 v22 m-6 -7 l6 7 l6 -7`} fill="none" stroke={factory.color} strokeWidth="2"/>
        <rect x={-44 + i * 37} y="12" width="15" height="6" fill={active ? "#d3d8ba" : "#41585a"}/>
      </g>)}
      <rect x="-51" y="35" width="103" height="8" fill="#14272e"/>
      <rect x="-51" y="35" width={active ? 18 + ((time * 31) % 85) : 0} height="8" fill={factory.color}/>
      <circle cx="51" cy="-61" r="4" fill={active ? "#b2d8a7" : "#536964"}/>
    </Case>
  </g>;
};

export const MechanicalPress: React.FC<{x: number; y: number; time: number; active: boolean}> = ({x, y, time, active}) => {
  const phase = (time % 1.8) / 1.8;
  const down = active ? Math.pow(Math.sin(phase * Math.PI), 4) * 116 : 0;
  return <g transform={`translate(${x} ${y})`}>
    <path d="M-70 -52 V230 H-49 V-52 M65 -52 V230 H86 V-52" fill="#44575c" stroke="#9ca99e" strokeWidth="2"/>
    <Case width={115} height={105} front="#80694e" edge="#b6a98e"><Gear x={0} y={0} radius={31} angle={active ? time * 85 : 0}/></Case>
    <rect x="-10" y="53" width="20" height={42 + down} fill="#b8bdad" stroke="#53696d" strokeWidth="3"/>
    <path d={`M-48 ${95 + down} H48 V${116 + down} H-48Z`} fill="#a6aaa0" stroke="#e0d6b4" strokeWidth="3"/>
    <path d="M-83 230 L-60 213 H90 L67 230Z" fill="#a68d6c"/><path d="M-83 230 H67 V252 H-83Z" fill="#4c5553"/>
    <path d={active && phase > .58 ? "M-28 204 L-12 197 H30 L15 204Z" : "M-25 203 L-10 192 H30 L15 203 V212 H-25Z"} fill="#d59b69" stroke="#f0c59d" strokeWidth="2"/>
  </g>;
};

export const EnergyBlock: React.FC<{x: number; y: number; kind: "kinetic" | "vibration"; time: number}> = ({x, y, kind, time}) => <g transform={`translate(${x} ${y})`}>
  <Case width={78} height={78} front="#536b71" edge="#a9b5ab">
    {kind === "kinetic" ? <Gear x={0} y={0} radius={24} angle={time * 70}/> : <><rect x="-25" y="-20" width="50" height="40" fill="#c98b50"/><path d="M-14 13 l14 -26 l14 26Z" fill="#f7d69b"/></>}
  </Case>
</g>;

export const DriveBlock: React.FC<{x: number; y: number; tier: number; size?: number}> = ({x, y, tier, size = 92}) => <g transform={`translate(${x} ${y})`}>
  <Case width={size} height={size} front="#243644" edge="#849eae">
    <image href={staticFile(`memory/drive_ddr${tier}_front.png`)} x={-size / 2 + 3} y={-size / 2 + 3} width={size - 6} height={size - 6} style={{imageRendering: "pixelated"}}/>
  </Case>
</g>;

/** Three individual orange, normally heated burners are placed under each sample. */
export const BlazeBurner: React.FC<{x: number; y: number; time: number}> = ({x, y, time}) => {
  const bob = Math.sin(time * 4) * 3;
  return <g transform={`translate(${x} ${y})`}>
    <ellipse cy="-15" rx="62" ry="64" fill="#e99e43" opacity=".12"/>
    <path d="M-51 30 L-39 20 H51 L40 30Z M-51 30 H40 V45 H-51Z" fill="#4b5150" stroke="#8b8d73" strokeWidth="3"/>
    {[0, 1, 2].map(i => <path key={i} d={`M${-28 + i * 25} 10 q-13 -22 0 ${-40 - Math.sin(time * 5 + i) * 8} q18 24 0 40`} fill={i === 1 ? "#fbd570" : "#e59035"}/>)}
    <g transform={`translate(0 ${bob})`}><rect x="-22" y="-25" width="42" height="29" fill="#f2c456" stroke="#aa6c29" strokeWidth="2"/><rect x="-15" y="-17" width="9" height="5" fill="#513b27"/><rect x="7" y="-17" width="9" height="5" fill="#513b27"/><path d="M-11 -3 H12" stroke="#825027" strokeWidth="3"/></g>
    <path d="M-46 -43 V32 M43 -43 V32 M-46 -43 H43 M-46 -4 H43 M-46 25 H43" stroke="#727e76" strokeWidth="5" fill="none"/>
    <path d="M-46 -43 l12 -9 h89 l-12 9 M43 -43 l12 -9 v75 l-12 9" stroke="#a7a88d" strokeWidth="3" fill="none"/>
  </g>;
};
