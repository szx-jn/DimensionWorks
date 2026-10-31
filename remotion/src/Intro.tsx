import React from "react";
import {AbsoluteFill, Audio, Img, Sequence, staticFile, useCurrentFrame, useVideoConfig} from "remotion";
import {Caption, Gear, Landscape, palette, ramp, Scene} from "./Cinema";
import {MoltenChemistry, PowerConnections} from "./Mechanisms";

const Heart: React.FC<{damaged?: boolean; size?: number; opacity?: number; scale?: number}> = ({damaged = true, size = 210, opacity = 1, scale = 1}) => <Img src={staticFile(damaged ? "gear_heart_damaged.png" : "gear_heart_normal.png")} style={{width: size, height: size, objectFit: "contain", imageRendering: "pixelated", opacity, scale: String(scale), filter: "drop-shadow(0 0 26px #cf786344)"}}/>;

const Anomaly: React.FC = () => {
  const t = useCurrentFrame() / useVideoConfig().fps;
  const disruption = ramp(t, 2, 6);
  return <Scene duration={10} label="那一天" background="#567b7c">
    <Landscape time={t} night={disruption * .6}/>
    <AbsoluteFill style={{background: "radial-gradient(ellipse at 50% 28%, #d1888577, transparent 60%)", opacity: disruption}}/>
    <svg width="1920" height="1080" viewBox="0 0 1920 1080" style={{position: "absolute", inset: 0}}>
      <g opacity={disruption}>
        <path d="M660 42 L790 151 L752 238 L908 283 L865 366 L970 422" stroke="#f1d8b8" strokeWidth="3" fill="none"/>
        <path d="M790 151 L1090 183 L1190 106 L1250 42 M908 283 L1060 252 L1160 345" stroke="#ddb6a7" strokeWidth="2" fill="none"/>
        <ellipse cx="958" cy="270" rx={180 + disruption * 100} ry="118" fill="none" stroke="#bcb3b7" strokeWidth="1" opacity=".5"/>
      </g>
    </svg>
    <Caption start={2.2} end={9.5}>那一天，天地发生了异变。</Caption>
  </Scene>;
};

const HeartChange: React.FC = () => {
  const t = useCurrentFrame() / useVideoConfig().fps;
  const conversion = ramp(t, 2.8, 8.6);
  const beat = 1 + Math.sin(t * Math.PI * 2) * .035 * (1 - conversion);
  return <Scene duration={11} label="目光之外" background="radial-gradient(ellipse at 50% 44%,#45343b,#151f26 65%)">
    <div style={{position: "absolute", left: 960, top: 490, translate: "-50% -50%", width: 500, height: 500, borderRadius: "50%", background: "radial-gradient(circle,#cc68522b,transparent 65%)"}}/>
    <div style={{position: "absolute", left: 960, top: 490, translate: "-50% -50%", scale: String(beat)}}>
      <Img src={staticFile("heart_red.png")} style={{position: "absolute", width: 290, height: 290, imageRendering: "pixelated", opacity: 1 - conversion}}/>
      <Heart damaged={false} size={290} opacity={conversion}/>
    </div>
    <svg width="1920" height="1080" viewBox="0 0 1920 1080" style={{position: "absolute", inset: 0}}>
      {Array.from({length: 12}, (_, i) => {
        const a = i * Math.PI / 6 + t * .09;
        return <path key={i} d={`M${960 + Math.cos(a) * 215} ${490 + Math.sin(a) * 215} l${Math.cos(a) * 18} ${Math.sin(a) * 18}`} stroke="#be9e86" opacity={conversion * .65} strokeWidth="2"/>;
      })}
    </svg>
    <Caption start={.7} end={10.4}>有些变化，发生在目光之外。</Caption>
  </Scene>;
};

const FallingGear: React.FC = () => {
  const t = useCurrentFrame() / useVideoConfig().fps;
  const fall = ramp(t, 1, 3.6);
  const impact = ramp(t, 3.6, 3.68) * (1 - ramp(t, 3.68, 3.95));
  return <Scene duration={6} label="一瞬之间">
    <Landscape time={t} night={.5}/>
    <svg width="1920" height="1080" viewBox="0 0 1920 1080" style={{position: "absolute", inset: 0}}>
      <g opacity={1 - ramp(t, 3.8, 4.2)}><rect x="916" y="590" width="88" height="160" fill="#17292f"/><rect x="909" y="510" width="102" height="100" fill="#203138"/><rect x="886" y="610" width="30" height="116" fill="#263d43"/><rect x="1004" y="610" width="30" height="116" fill="#263d43"/></g>
      <Gear x={960} y={-160 + fall * 690} radius={100} angle={fall * 260}/>
    </svg>
    <AbsoluteFill style={{background: "#fff1d8", opacity: impact * .65}}/>
    <AbsoluteFill style={{background: "#090f13", opacity: ramp(t, 3.95, 4.8)}}/>
  </Scene>;
};

const TimePassing: React.FC = () => {
  const t = useCurrentFrame() / useVideoConfig().fps;
  const progress = ramp(t, 0, 10.5);
  const night = Math.pow((1 + Math.sin(t * 2.7)) / 2, 2) * .58 * (1 - ramp(t, 8, 10.5));
  return <Scene duration={12} label="漫长的沉寂">
    <Landscape time={t * 5} growth={.45 + progress * .55} night={night}/>
    <div style={{position: "absolute", top: 290, width: "100%", textAlign: "center", color: "#edf0dc", textShadow: "0 3px 25px #15282b88", fontSize: 83, letterSpacing: 12, opacity: ramp(t, .5, 1.2)}}>{t < 10.5 ? Math.floor(progress * 30000).toLocaleString("en-US") : "30,000"}<span style={{fontSize: 25, marginLeft: 22}}>年</span></div>
    <Caption start={3} end={11.6}>三万年过去。草木已看不出那一天的痕迹。</Caption>
  </Scene>;
};

const Awakening: React.FC = () => {
  const t = useCurrentFrame() / useVideoConfig().fps;
  const panel = ramp(t, 2, 3);
  return <Scene duration={9} label="苏醒">
    <Landscape time={t * .2}/>
    <AbsoluteFill style={{background: "#102126", opacity: panel * .6}}/>
    <div style={{position: "absolute", left: 960, top: 460, translate: "-50% -50%", width: 660, height: 380, opacity: panel, border: "1px solid #b4b7a461", background: "linear-gradient(135deg,#1c3037ee,#13242bf5)", padding: "46px 50px", display: "flex", alignItems: "center", gap: 45, boxShadow: "0 30px 100px #09141877"}}>
      <div style={{border: "2px solid #9d9980", padding: 13, background: "#354443"}}><Heart size={170}/></div>
      <div><div style={{color: palette.muted, letterSpacing: 6, fontSize: 18, marginBottom: 19}}>饰品</div><div style={{fontSize: 36, letterSpacing: 5}}>齿轮之心</div><div style={{fontSize: 23, color: "#d59b85", marginTop: 23}}>破损 · 无法取下</div><div style={{fontSize: 19, color: palette.muted, marginTop: 20}}>微弱的搏动仍在继续。</div></div>
    </div>
    <AbsoluteFill style={{background: "#080f12", opacity: (1 - ramp(t, 0, 1.3)) * .95}}/>
    <Caption start={4} end={8.6}>眼前的世界，似乎仍然熟悉。</Caption>
  </Scene>;
};

const ReturnToWorld: React.FC = () => {
  const t = useCurrentFrame() / useVideoConfig().fps;
  const reveal = ramp(t, .8, 3.5);
  return <Scene duration={5} label="" background="#1b3438">
    <Landscape time={t * .3}/>
    <AbsoluteFill style={{background: "#102026", opacity: .75 * (1 - reveal)}}/>
    <div style={{position: "absolute", left: 960, top: 520 + reveal * 200, translate: "-50% -50%", opacity: 1 - reveal, scale: String(1 + Math.sin(t * Math.PI * 2) * .018)}}><Heart size={220}/></div>
    <div style={{position: "absolute", top: 180, left: 960, width: 1, height: 190, background: "linear-gradient(transparent,#d4dfbd66,transparent)", opacity: reveal}}/>
  </Scene>;
};

const EndTitle: React.FC = () => {
  const t = useCurrentFrame() / useVideoConfig().fps;
  const enter = ramp(t, .3, 1.6);
  return <Scene duration={6} label="">
    <div style={{position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", flexDirection: "column", opacity: enter, translate: `0 ${(1 - enter) * 14}px`}}>
      <Heart size={145}/>
      <div style={{fontSize: 84, letterSpacing: 21, marginTop: 40, fontWeight: 600}}>维度工序</div>
      <div style={{height: 1, width: 490 * enter, background: "linear-gradient(90deg,transparent,#cba47d,transparent)", margin: "30px 0 24px"}}/>
      <div style={{fontFamily: "Georgia,serif", fontSize: 25, letterSpacing: 11, color: "#c2b49b"}}>DIMENSIONWORKS</div>
    </div>
  </Scene>;
};

const Soundscape: React.FC = () => <Audio src={staticFile("audio/intro-soundscape.wav")} volume={.8}/>;

export const Intro: React.FC = () => {
  const {fps} = useVideoConfig();
  return <AbsoluteFill style={{background: "#090f13"}}>
    <Soundscape/>
    <Sequence name="天地异变" from={0} durationInFrames={10 * fps} premountFor={fps}><Anomaly/></Sequence>
    <Sequence name="心脏改写" from={10 * fps} durationInFrames={11 * fps} premountFor={fps}><HeartChange/></Sequence>
    <Sequence name="齿轮坠落" from={21 * fps} durationInFrames={6 * fps} premountFor={fps}><FallingGear/></Sequence>
    <Sequence name="三万年" from={27 * fps} durationInFrames={12 * fps} premountFor={fps}><TimePassing/></Sequence>
    <Sequence name="苏醒" from={39 * fps} durationInFrames={9 * fps} premountFor={fps}><Awakening/></Sequence>
    <Sequence name="动力与应用机动" from={48 * fps} durationInFrames={13 * fps} premountFor={fps}><PowerConnections/></Sequence>
    <Sequence name="熔融金属与空气" from={61 * fps} durationInFrames={18 * fps} premountFor={fps}><MoltenChemistry/></Sequence>
    <Sequence name="回到世界" from={79 * fps} durationInFrames={5 * fps} premountFor={fps}><ReturnToWorld/></Sequence>
    <Sequence name="维度工序" from={84 * fps} durationInFrames={6 * fps} premountFor={fps}><EndTitle/></Sequence>
  </AbsoluteFill>;
};
