import React from "react";
import {AbsoluteFill, interpolate, useCurrentFrame, useVideoConfig} from "remotion";

export const palette = {ink: "#101b20", paper: "#eee5ce", muted: "#a7b8b4", copper: "#d99765", teal: "#7fb8b1"};
export const font = "'PingFang SC', 'Microsoft YaHei', sans-serif";
export const ramp = (f: number, start: number, end: number) => interpolate(f, [start, end], [0, 1], {extrapolateLeft: "clamp", extrapolateRight: "clamp"});

export const Caption: React.FC<{children: React.ReactNode; start?: number; end: number}> = ({children, start = 0, end}) => {
  const {fps} = useVideoConfig();
  const t = useCurrentFrame() / fps;
  return <div style={{position: "absolute", bottom: 104, left: 160, right: 160, textAlign: "center", fontFamily: font, fontSize: 31, lineHeight: 1.65, letterSpacing: 3, color: palette.paper, textShadow: "0 2px 16px #081115", opacity: ramp(t, start, start + .45) * (1 - ramp(t, end - .45, end))}}>{children}</div>;
};

export const Scene: React.FC<{children: React.ReactNode; label: string; duration: number; background?: string}> = ({children, label, duration, background}) => {
  const t = useCurrentFrame() / useVideoConfig().fps;
  return <AbsoluteFill style={{background: background ?? "radial-gradient(ellipse at 50% 42%, #293b40, #101b20 74%)", fontFamily: font, color: palette.paper, overflow: "hidden"}}>
    <AbsoluteFill style={{opacity: ramp(t, 0, .4) * (1 - ramp(t, duration - .4, duration))}}>
      {children}
      <div style={{position: "absolute", left: 96, top: 84, fontSize: 17, letterSpacing: 6, color: palette.muted}}>{label}</div>
    </AbsoluteFill>
    <AbsoluteFill style={{pointerEvents: "none", boxShadow: "inset 0 0 190px 40px rgba(3,10,13,.28)"}} />
    <div style={{position: "absolute", top: 0, width: "100%", height: 42, background: "#090f13"}} />
    <div style={{position: "absolute", bottom: 0, width: "100%", height: 42, background: "#090f13"}} />
  </AbsoluteFill>;
};

export const Gear: React.FC<{x: number; y: number; radius?: number; angle: number; color?: string}> = ({x, y, radius = 60, angle, color = palette.copper}) => <g transform={`translate(${x} ${y}) rotate(${angle})`}>
  {Array.from({length: 12}, (_, i) => <rect key={i} x={-radius * .13} y={-radius * 1.15} width={radius * .26} height={radius * .3} fill={color} transform={`rotate(${i * 30})`} />)}
  <circle r={radius * .92} fill="#26393d" stroke={color} strokeWidth={radius * .2}/>
  {[0, 60, 120].map(a => <rect key={a} x={-radius * .75} y={-radius * .08} width={radius * 1.5} height={radius * .16} fill={color} transform={`rotate(${a})`} />)}
  <circle r={radius * .22} fill="#15262c" stroke={color} strokeWidth={4}/>
</g>;

/** Stylized ordinary landscape. No generated ruins, factories or custom terrain. */
export const Landscape: React.FC<{time: number; growth?: number; night?: number}> = ({time, growth = 1, night = 0}) => <svg viewBox="0 0 1920 1080" width="1920" height="1080" style={{position: "absolute", inset: 0}}>
  <rect width="1920" height="1080" fill="#90a8a4"/>
  <circle cx={1430 - time * 3} cy={260 + Math.sin(time * .15) * 75} r="65" fill="#e2d8b1"/>
  <path d="M0 550 H190 V485 H370 V530 H590 V440 H810 V510 H1020 V460 H1220 V520 H1490 V470 H1700 V540 H1920 V1080 H0Z" fill="#667f79"/>
  <path d="M0 700 H260 V650 H520 V710 H820 V670 H1130 V720 H1410 V650 H1680 V690 H1920 V1080 H0Z" fill="#3c6259"/>
  <path d="M1250 695 H1370 L1750 1080 H1180 L1160 810Z" fill="#91b6af"/>
  {Array.from({length: 14}, (_, i) => {
    const x = i * 157 - 40;
    const h = (130 + i % 3 * 40) * growth;
    return <g key={i} transform={`translate(${x} ${790 + i % 3 * 38})`}><rect x="-12" y={-h} width="24" height={h + 80} fill="#4a5142"/><path d={`M-60 ${-h + 25} V${-h - 65} H45 V${-h - 32} H83 V${-h + 42} H24 V${-h + 65} H-78 V${-h + 25}Z`} fill={i % 2 ? "#31554a" : "#456854"}/></g>;
  })}
  <path d="M0 940 H400 V910 H650 V970 H1040 V925 H1280 V990 H1920 V1080 H0Z" fill="#244b40"/>
  {Array.from({length: 30}, (_, i) => <path key={i} d={`M${40 + i * 63} ${965 + i % 4 * 21} v-22 l${Math.sin(time * .6 + i) * 9 + 4} -15`} stroke="#739375" strokeWidth="3" fill="none"/>)}
  <rect width="1920" height="1080" fill="#112333" opacity={night}/>
</svg>;
