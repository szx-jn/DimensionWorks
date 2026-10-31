import React from "react";
import {useCurrentFrame, useVideoConfig} from "remotion";
import {Caption, palette, ramp, Scene} from "./Cinema";
import {Controller, DriveBlock, EnergyBlock, Gearbox, GlassCable, Label, MechanicalPress, Motor, FACTORY_TYPES, FactoryBlock, Shaft} from "./WorkshopIllustrations";

export const DirectPower: React.FC = () => {
  const t = useCurrentFrame() / useVideoConfig().fps;
  const running = t >= .9;
  const selected = Math.min(8, Math.floor(Math.max(0, t - .6) / .56));
  return <Scene duration={6} label="异变之后 / 工厂的机械供能">
    <svg width="1920" height="1080" viewBox="0 0 1920 1080" style={{position: "absolute", inset: 0}}>
      <Label x={960} y={205} size={35} color={palette.paper}>创造马达 → 传动杆 → Mekanism 工厂</Label>
      <Shaft from={408} to={920} y={500} time={t}/>
      <Motor x={300} y={500} time={t} scale={1.2}/>
      <FactoryBlock x={1010} y={500} time={t} active={running} type={selected} scale={1.2}/>
      <path d="M485 420 H820 l-20 -11 M820 420 l-20 11" stroke={palette.copper} fill="none" strokeWidth="3" opacity={ramp(t, .4, .9)}/>
      <Label x={650} y={380} size={23} color={palette.copper}>应力沿传动杆接入工厂</Label>
      <Label x={300} y={700} size={28}>创造马达</Label>
      <Label x={300} y={749} size={19}>演示动力源</Label>
      <Label x={1010} y={700} size={28}>Mekanism 工厂 · {FACTORY_TYPES[selected].name}</Label>
      <Label x={1010} y={749} size={21} color={running ? palette.teal : palette.muted}>{running ? "机械供能已接入 · 运行中" : "等待机械供能"}</Label>
      <Label x={1515} y={295} size={24}>九类工厂</Label>
      {FACTORY_TYPES.map((factory, i) => {
        const x = 1330 + i % 3 * 180;
        const y = 385 + Math.floor(i / 3) * 160;
        return <g key={factory.id}>
          <FactoryBlock x={x} y={y} time={t} active={running && selected === i} type={i} scale={.6}/>
          <Label x={x} y={y + 78} size={21} color={selected === i ? palette.paper : palette.muted}>{factory.name}工厂</Label>
        </g>;
      })}
      <Label x={1515} y={850} size={17}>基础 · 高级 · 精英 · 终极</Label>
    </svg>
    <Caption end={5.8}>转动沿传动杆传开，各类工厂开始运作。</Caption>
  </Scene>;
};

export const NetworkPower: React.FC = () => {
  const t = useCurrentFrame() / useVideoConfig().fps;
  const energyBranch = ramp(t, 2.8, 3.5);
  const driveBranch = ramp(t, 8, 8.6);
  const powered = t >= 7.7;
  const online = t >= 10.5;
  const tier = Math.min(5, 1 + Math.floor(Math.max(0, t - 8.8) / .4));
  return <Scene duration={16} label="应用机动 / 机械、AE 供能与转速额度">
    <svg width="1920" height="1080" viewBox="0 0 1920 1080" style={{position: "absolute", inset: 0}}>
      <Label x={960} y={178} size={33} color={palette.paper}>同一网络，接入三种不同的条件</Label>
      <GlassCable path="M540 530 H854 M976 530 H1250" time={t} active={online} color={palette.copper}/>
      <Shaft from={275} to={440} y={530} time={t}/>
      <Shaft from={1372} to={1592} y={530} time={t} active={online}/>
      <Motor x={185} y={530} time={t}/>
      <Gearbox x={490} y={530} time={t} mode="IMPORT"/>
      <Controller x={915} y={530} time={t} online={powered}/>
      <Gearbox x={1300} y={530} time={t} mode="EXPORT" active={online}/>
      <MechanicalPress x={1650} y={530} time={Math.max(0, t - 10.5)} active={online}/>
      <Label x={185} y={655}>创造马达</Label>
      <Label x={490} y={655}>ME 齿轮箱 · 输入</Label>
      <Label x={915} y={655}>ME 控制器</Label>
      <Label x={1300} y={655}>ME 齿轮箱 · 输出</Label>
      <Label x={1650} y={850}>机械压力机</Label>
      <Label x={710} y={489} size={17}>ME 玻璃线缆</Label>
      <Label x={1110} y={489} size={17}>ME 玻璃线缆</Label>
      <g opacity={energyBranch}>
        <GlassCable path="M440 324 V420 H1160 V324 M915 420 V459" time={t} active={powered}/>
        <Shaft from={339} to={401} y={285} time={t}/>
        <Motor x={280} y={285} time={t} scale={.65}/>
        <EnergyBlock x={440} y={285} kind="kinetic" time={t}/>
        <EnergyBlock x={1160} y={285} kind="vibration" time={t}/>
        <Label x={440} y={355} size={20}>动能接收器</Label>
        <Label x={1160} y={355} size={20}>谐振仓</Label>
        <Label x={440} y={386} size={17}>机械应力 → AE</Label>
        <Label x={1160} y={386} size={17}>燃料 → AE</Label>
        <Label x={1160} y={220} size={17} color={palette.copper}>↓ 燃料输入</Label>
        <Label x={1510} y={285} size={22} color={palette.teal}>AE 供能支路</Label>
      </g>
      <g opacity={driveBranch}>
        <GlassCable path="M915 591 V727" time={t} active={online} color="#be9ecb"/>
        <DriveBlock x={915} y={775} tier={tier} size={96}/>
        <Label x={915} y={867} size={21}>DDR{tier} 内存驱动器 · 转速额度</Label>
        <Label x={575} y={782} size={19}>DDR1 → DDR2 → DDR3 → DDR4 → DDR5</Label>
        <Label x={575} y={819} size={17}>逐代展示，仅一台接入示意网络</Label>
        <Label x={575} y={894} size={18} color={palette.paper}>同一 ME 网络最多接入 2 台内存驱动器</Label>
      </g>
      <g opacity={ramp(t, 10.5, 11)}>
        <circle cx="1195" cy="758" r="6" fill={palette.teal}/>
        <Label x={1305} y={765} size={19} color={palette.teal}>条件齐备 · 冲压开始</Label>
      </g>
    </svg>
    <Caption end={3.1}>应力进入齿轮箱，沿 ME 玻璃线缆接入网络。</Caption>
    <Caption start={3.5} end={8.1}>网络自身，也需要 AE 能量。</Caption>
    <Caption start={8.6} end={15.7}>接入驱动器，另一端的齿轮箱便能带动压力机。</Caption>
  </Scene>;
};
