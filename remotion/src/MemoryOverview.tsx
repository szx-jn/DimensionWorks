import React from "react";
import {Img, staticFile, useCurrentFrame, useVideoConfig} from "remotion";
import {Caption, palette, ramp, Scene} from "./Cinema";
import {DriveBlock, Label} from "./WorkshopIllustrations";

// Values checked against MemoryTier, MemoryCardType and the shipped COMMON config.
export const DRIVE_TIERS = [
  {tier: 1, slots: 4, rpm: 256}, {tier: 2, slots: 8, rpm: 512},
  {tier: 3, slots: 16, rpm: 1024}, {tier: 4, slots: 32, rpm: 2048}, {tier: 5, slots: 64, rpm: 4096},
];
export const CARD_TYPES = [
  {id: "economy", name: "经济型", multiplier: "0.5×", loss: "2%", color: "#96bcab"},
  {id: "balanced", name: "均衡型", multiplier: "1×", loss: "1.5%", color: "#83bed6"},
  {id: "high_speed", name: "高速型", multiplier: "2×", loss: "1.75%", color: "#d8b67b"},
  {id: "high_storage", name: "低损耗型", multiplier: "0.5×", loss: "0.75%", color: "#b6a2d7"},
  {id: "defective", name: "劣质型", multiplier: "0.25×", loss: "20%", color: "#d59688"},
  {id: "final", name: "最终型", multiplier: "4×", loss: "0%", color: "#e0d9b3"},
];

export const MemoryOverview: React.FC = () => {
  const t = useCurrentFrame() / useVideoConfig().fps;
  const cards = ramp(t, 5.1, 5.7);
  return <Scene duration={15} label="应用机动 / 驱动器与内存卡">
    <svg width="1920" height="1080" viewBox="0 0 1920 1080" style={{position: "absolute", inset: 0}}>
      <Label x={960} y={195} size={35} color={palette.paper}>{cards < .5 ? "DDR1—DDR5 · 从四个卡槽到六十四个" : "同代内存卡 · 在转速与输入损耗之间取舍"}</Label>
      <g opacity={1 - cards}>
        {DRIVE_TIERS.map((drive, i) => <g key={drive.tier} opacity={ramp(t, .15 + i * .16, .6 + i * .16)}>
          <DriveBlock x={350 + i * 305} y={395} tier={drive.tier} size={135}/>
          <Label x={350 + i * 305} y={525} size={30} color={palette.paper}>DDR{drive.tier}</Label>
          <Label x={350 + i * 305} y={591} size={26}>{drive.slots} 卡槽</Label>
          <Label x={350 + i * 305} y={646} size={26}>{drive.rpm} RPM</Label>
        </g>)}
        <Label x={960} y={730} size={21}>上行：插卡空间　下行：驱动器基础转速额度</Label>
        <Label x={960} y={798} size={21}>每个逻辑 ME 网络最多 2 台有效驱动器 · 不提供应力容量或库存</Label>
      </g>
    </svg>
    <div style={{position: "absolute", left: 255, right: 255, top: 260, opacity: cards}}>
      <div style={{display: "grid", gridTemplateColumns: "180px repeat(5, 1fr) 280px", alignItems: "center", textAlign: "center", color: palette.muted, fontSize: 20, height: 55, marginBottom: 8}}>
        <div>卡型</div>{DRIVE_TIERS.map(d => <div key={d.tier}>DDR{d.tier}<div style={{fontSize: 16, marginTop: 5}}>基准 {d.rpm} RPM</div></div>)}<div style={{lineHeight: 1.5}}>额外 RPM 倍率<br/>每卡输入扣除</div>
      </div>
      {CARD_TYPES.map((card, i) => <div key={card.id} style={{display: "grid", gridTemplateColumns: "180px repeat(5, 1fr) 280px", alignItems: "center", textAlign: "center", height: 77, background: i % 2 ? "#22363b88" : "#132a3088", borderBottom: "1px solid #9bb3ad22"}}>
        <div style={{color: card.color, fontSize: 24}}>{card.name}</div>
        {DRIVE_TIERS.map(d => <div key={d.tier} style={{display: "flex", alignItems: "center", justifyContent: "center"}}><Img src={staticFile(`memory/card_ddr${d.tier}_${card.id}.png`)} style={{width: 64, height: 64, imageRendering: "pixelated"}}/></div>)}
        <div style={{fontSize: 24, color: card.color}}>{card.multiplier} / {card.loss}</div>
      </div>)}
      <div style={{display: "flex", justifyContent: "space-between", fontSize: 18, lineHeight: 1.65, color: palette.muted, marginTop: 13}}><span>同代卡才生效 · 最多扣除全网 99% 应力</span><span>手工正常卡有 25% 劣质概率 · 最终卡暂无生存配方</span></div>
    </div>
    <Caption end={5.3}>驱动器拓展转速额度，内存卡决定如何分配这份余地。</Caption>
    <Caption start={6} end={14.7}>更多转速，也意味着新的取舍。</Caption>
  </Scene>;
};
