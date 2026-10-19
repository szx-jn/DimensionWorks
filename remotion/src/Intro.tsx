import React from "react";
import { AbsoluteFill, Img, Sequence, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig } from "remotion";

const clamp = { extrapolateLeft: "clamp" as const, extrapolateRight: "clamp" as const };
const fade = (f: number, a: number, b: number) => interpolate(f, [a, b], [0, 1], clamp);
const titleStyle: React.CSSProperties = { fontFamily: "'Microsoft YaHei', sans-serif", color: "#f1e5d0", textAlign: "center", letterSpacing: 5, textShadow: "0 3px 18px rgba(0,0,0,.55)" };
const Specks: React.FC = () => <AbsoluteFill style={{ opacity: .11, pointerEvents: "none", backgroundImage: "radial-gradient(circle,rgba(255,255,255,.5) 0 1px,transparent 1.5px)", backgroundSize: "23px 27px" }} />;

const Anomaly: React.FC = () => {
  const f = useCurrentFrame();
  const pulse = .06 + .06 * (1 + Math.sin(f / 15));
  const fracture = fade(f, 65, 190);
  const horizon = 625 - fade(f, 40, 420) * 90;
  return <AbsoluteFill style={{ background: "linear-gradient(180deg,#101925 0%,#303743 48%,#837064 100%)", overflow: "hidden" }}>
    <AbsoluteFill style={{ background: "radial-gradient(ellipse at 50% 39%,rgba(218,73,72," + pulse + "),transparent 39%)" }} />
    <svg viewBox="0 0 1920 1080" style={{ position: "absolute", width: "100%", height: "100%" }}>
      <path d={"M0 " + horizon + " L180 " + (horizon-45) + " L360 " + (horizon+8) + " L540 " + (horizon-95) + " L730 " + (horizon+12) + " L930 " + (horizon-45) + " L1120 " + (horizon+8) + " L1300 " + (horizon-78) + " L1500 " + (horizon+10) + " L1710 " + (horizon-40) + " L1920 " + (horizon+8) + " L1920 1080 L0 1080Z"} fill="#4a4b4b" />
      <path d="M0 0 L440 0 L690 130 L600 260 L430 228 L290 360 L120 300 L0 355Z" fill="#0a1018" opacity=".72"/>
      <path d="M1920 0 L1510 0 L1400 150 L1520 255 L1700 230 L1830 340 L1920 300Z" fill="#0a1018" opacity=".66"/>
      {Array.from({ length: 8 }, (_, i) => <path key={i} d={"M" + (760+i*28) + " 115 l" + ((i%2 ? 1 : -1)*(25+i*5)) + " " + (75+i*10) + " l" + ((i%2 ? -1 : 1)*14) + " 30"} stroke="#e8a092" strokeWidth="2" fill="none" opacity={fracture*.7} />)}
    </svg>
    <div style={{ position: "absolute", top: 105, width: "100%", ...titleStyle, opacity: fade(f, 8, 40), fontSize: 22, letterSpacing: 10, color: "#d9aaa0" }}>DIMENSIONAL DISRUPTION</div>
    <div style={{ position: "absolute", top: 420, width: "100%", ...titleStyle, fontSize: 31, opacity: fade(f, 15, 40) * (1-fade(f, 385, 430)) }}>天地异变，万物失序。</div>
    <div style={{ position: "absolute", left: "50%", top: "40%", width: 340, height: 340, borderRadius: "50%", border: "1px solid rgba(239,166,147," + (.12+fracture*.4) + ")", transform: "translate(-50%,-50%) scale(" + (1+Math.sin(f/18)*.02+fracture*.12) + ")", boxShadow: "0 0 " + (45+fracture*90) + "px rgba(238,70,68,.17)", opacity: fade(f, 35, 110)*(1-fade(f, 310, 400)) }} />
    <Specks />
  </AbsoluteFill>;
};

const HeartChange: React.FC = () => {
  const f = useCurrentFrame();
  const { fps } = useVideoConfig();
  const conversion = fade(f, 130, 390);
  const entrance = spring({ frame: f, fps, config: { damping: 19, stiffness: 85, mass: 1 } });
  const scale = interpolate(f, [0, 95, 380, 450], [.65, 1, 1.08, .96], clamp);
  const pulse = 1 + Math.sin(f/8)*.026*(1-conversion);
  const glitch = f > 150 && f < 280 ? Math.sin(f*1.8)*(1-fade(f, 245, 280))*2 : 0;
  return <AbsoluteFill style={{ background: "radial-gradient(ellipse at 50% 42%,#453039 0%,#1a202a 47%,#0e131b 100%)", overflow: "hidden" }}>
    <div style={{ position: "absolute", top: 105, width: "100%", ...titleStyle, fontSize: 22, letterSpacing: 8, color: "#d5aaa4", opacity: fade(f, 10, 40) }}>A HEART REWRITTEN</div>
    <div style={{ position: "absolute", left: "50%", top: "43%", width: 520, height: 520, borderRadius: "50%", transform: "translate(-50%,-50%)", background: "radial-gradient(circle,rgba(222,53,69,.18),transparent 67%)", opacity: .6 }} />
    <Img src={staticFile("heart_red.png")} style={{ position: "absolute", width: 270, height: 270, objectFit: "contain", left: "50%", top: "43%", transform: "translate(-50%,-50%) scale(" + (scale*pulse*entrance) + ") translateX(" + glitch + "px)", imageRendering: "pixelated", opacity: 1-conversion, filter: "drop-shadow(0 0 26px rgba(236,49,65,.32))" }} />
    <Img src={staticFile("gear_heart_normal.png")} style={{ position: "absolute", width: 270, height: 270, objectFit: "contain", left: "50%", top: "43%", transform: "translate(-50%,-50%) scale(" + scale + ") rotate(" + interpolate(f,[180,360],[0,6],clamp) + "deg)", imageRendering: "pixelated", opacity: conversion, filter: "drop-shadow(0 0 24px rgba(225,104,74,.28))" }} />
    <svg viewBox="0 0 1920 1080" style={{ position: "absolute", inset: 0, width: "100%", height: "100%" }}>
      {Array.from({length:7},(_,i)=><path key={i} d={"M"+(810+i*43)+" "+(370+(i%3)*48)+" l"+(i%2?18:-18)+" 21 l"+(i%2?-11:11)+" 19"} stroke="#f1a6a0" strokeWidth="2" fill="none" opacity={fade(f,90+i*14,160+i*18)*(1-fade(f,245,325))}/>)}
      {Array.from({length:12},(_,i)=>{const a=i*Math.PI/6+f*.009;const r=215+conversion*25;return <line key={i} x1={960+Math.cos(a)*r} y1={470+Math.sin(a)*r} x2={960+Math.cos(a)*(r+16)} y2={470+Math.sin(a)*(r+16)} stroke="#c88e78" strokeWidth="2" opacity={conversion*.65}/>})}
    </svg>
    <div style={{ position: "absolute", top: 725, width: "100%", ...titleStyle, fontSize: 31, opacity: fade(f, 15, 48)*(1-fade(f, 370, 410)) }}>鲜活的心脏，正在被异变改写。</div>
    <div style={{ position: "absolute", bottom: 105, width: "100%", ...titleStyle, fontSize: 21, letterSpacing: 7, color: "#d7a49b", opacity: fade(f, 375, 410) }}>GEAR HEART — FORMED BY THE ANOMALY</div>
    <Specks />
  </AbsoluteFill>;
};

const FallingGear: React.FC = () => {
  const f = useCurrentFrame();
  const fall = interpolate(f,[42,180],[-500,570],clamp);
  const rot = interpolate(f,[42,180,195,245],[-70,230,244,260],clamp);
  const impact = fade(f,178,185)*(1-fade(f,205,235));
  return <AbsoluteFill style={{ background: "linear-gradient(180deg,#18212c 0%,#46505a 65%,#252b31 100%)", overflow: "hidden" }}>
    <div style={{ position: "absolute", top: 105, width: "100%", ...titleStyle, fontSize: 22, letterSpacing: 8, color: "#e2c1ad", opacity: fade(f,10,35) }}>A MOMENT OF ABSURD FATE</div>
    <div style={{ position: "absolute", left: "50%", top: 610, width: 175, height: 125, borderRadius: "45% 45% 20% 20%", background: "linear-gradient(110deg,#343b44,#1a2028 60%,#4b5157)", transform: "translate(-50%,-50%)", opacity: 1-fade(f,210,250) }} />
    <div style={{ position: "absolute", left: "50%", top: 555, width: 112, height: 112, borderRadius: "50%", background: "radial-gradient(circle,#2c3034,#131820 70%)", transform: "translate(-50%,-50%)", opacity: 1-fade(f,210,250) }} />
    <svg viewBox="0 0 1920 1080" style={{ position: "absolute", inset: 0, width: "100%", height: "100%" }}>
      <g transform={"translate(960 "+fall+") rotate("+rot+") scale(1.6)"}>
        <path d="M-52 -35 L-30 -55 L30 -55 L52 -35 L52 35 L30 55 L-30 55 L-52 35Z" fill="#b9a58a" stroke="#4d555d" strokeWidth="7"/>
        <circle r="31" fill="#333b42" stroke="#d0bda0" strokeWidth="8"/><circle r="13" fill="#8d4c43" stroke="#e0b08d" strokeWidth="4"/>
        {Array.from({length:8},(_,i)=><rect key={i} x="-6" y="-70" width="12" height="20" rx="2" fill="#d1bea2" stroke="#59616a" strokeWidth="2" transform={"rotate("+(i*45)+")"}/>)}
      </g>
      {Array.from({length:12},(_,i)=><path key={i} d={"M960 790 l"+Math.cos(i*Math.PI/6)*(70+impact*150)+" "+Math.sin(i*Math.PI/6)*(25+impact*95)} stroke="#e6c7a2" strokeWidth="2" opacity={impact*.8} fill="none" />)}
    </svg>
    <div style={{ position: "absolute", bottom: 95, width: "100%", ...titleStyle, fontSize: 28, opacity: fade(f,25,50)*(1-fade(f,230,265)) }}>一枚齿轮从天而降，命运偏偏砸中了你。</div>
    <AbsoluteFill style={{ background: "#fff1d8", opacity: impact*.7 }} />
    <AbsoluteFill style={{ background: "#06090d", opacity: fade(f,245,295) }} />
  </AbsoluteFill>;
};

const TimePassing: React.FC = () => {
  const f=useCurrentFrame();
  const years=Math.floor(interpolate(f,[0,540],[0,30000],clamp));
  const ground=690-fade(f,0,540)*50;
  return <AbsoluteFill style={{ background: "linear-gradient(180deg,#b4c2c5 0%,#e4d8bf 58%,#b5b39a 100%)", overflow: "hidden" }}>
    <div style={{ position: "absolute", top: 100, width: "100%", ...titleStyle, fontSize: 22, color: "#485761", letterSpacing: 9, opacity: fade(f,10,38) }}>THIRTY THOUSAND YEARS</div>
    <svg viewBox="0 0 1920 1080" style={{ position: "absolute", inset: 0, width: "100%", height: "100%" }}>
      <circle cx="1500" cy="210" r={95+Math.sin(f/35)*5} fill="#f1e6c8" opacity=".8"/>
      <path d={"M0 "+ground+" L230 "+(ground-120)+" L430 "+(ground-42)+" L680 "+(ground-180)+" L910 "+(ground-55)+" L1140 "+(ground-150)+" L1380 "+(ground-30)+" L1620 "+(ground-170)+" L1920 "+(ground-45)+" L1920 1080 L0 1080Z"} fill="#8b9a91"/>
      <path d={"M0 "+(ground+95)+" Q480 "+(ground+25)+" 930 "+(ground+130)+" T1920 "+(ground+75)+" L1920 1080 L0 1080Z"} fill="#647b60"/>
      {Array.from({length:18},(_,i)=>{const x=40+i*108;const grow=fade(f,120+i*3,310+i*4);const h=(90+(i*31)%100)*grow;return <g key={i} transform={"translate("+x+" "+(ground+160)+")"} opacity={.12+grow*.88}><path d={"M0 0 L0 "+(-h)} stroke="#514b3f" strokeWidth="7"/><path d={"M0 "+(-h+25)+" l-35 -35 l18 0 l-20 -30 l36 17 l16 -34 l8 46 l28 -16 l-22 32 l20 5 l-38 28Z"} fill={i%3===0?"#64795b":i%3===1?"#788c65":"#526c55"}/></g>})}
      {Array.from({length:9},(_,i)=>{const x=120+i*210;return <path key={i} d={"M"+x+" "+(ground-60)+" l18 -70 l-8 -40 l26 -30 l-12 80 l20 60Z"} fill="#f4eee0" opacity={fade(f,140+i*10,420)*.28}/>})}
    </svg>
    <div style={{ position: "absolute", bottom: 185, width: "100%", textAlign: "center", color: "#34444b", fontSize: 54, fontFamily: "Georgia,serif", letterSpacing: 2, opacity: fade(f,25,55) }}>{years.toLocaleString("en-US")} YEARS LATER</div>
    <div style={{ position: "absolute", bottom: 115, width: "100%", textAlign: "center", color: "#4a5655", fontSize: 24, letterSpacing: 4, opacity: fade(f,100,145) }}>世界慢慢复原，工业的痕迹归于尘土。</div>
    <Specks />
  </AbsoluteFill>;
};

const Buff: React.FC<{label:string;detail:string;symbol:string;index:number;frame:number}> = ({label,detail,symbol,index,frame}) => {
  const enter=spring({frame:frame-index*18,fps:30,config:{damping:19,stiffness:100,mass:.8}});
  return <div style={{display:"flex",alignItems:"center",gap:22,padding:"18px 22px",marginBottom:13,background:"linear-gradient(90deg,rgba(24,31,39,.96),rgba(40,48,56,.88))",border:"1px solid rgba(211,185,141,.4)",borderRadius:7,opacity:enter,transform:"translateX("+(1-enter)*34+"px)",boxShadow:"0 12px 28px rgba(0,0,0,.2)"}}>
    <div style={{width:56,height:56,display:"grid",placeItems:"center",fontSize:31,background:"#343b42",border:"1px solid #c9a77d",borderRadius:6,color:"#e9d2ad"}}>{symbol}</div>
    <div><div style={{fontSize:24,color:"#f0dfc5",fontWeight:700}}>{label}</div><div style={{fontSize:17,color:"#b8c0c4",marginTop:5}}>{detail}</div></div>
  </div>;
};

const Awakening: React.FC = () => {
  const f=useCurrentFrame();
  const panel=fade(f,48,75);
  const heart=fade(f,72,100);
  const fail=fade(f,132,148)*(1-fade(f,165,185));
  const buffs=fade(f,190,220);
  return <AbsoluteFill style={{background:"linear-gradient(135deg,#1c242d,#0d1219 70%)",overflow:"hidden"}}>
    <div style={{position:"absolute",top:70,width:"100%",...titleStyle,fontSize:22,letterSpacing:8,color:"#9eabb0",opacity:fade(f,0,24)}}>AWAKENING</div>
    <div style={{position:"absolute",left:"50%",top:"48%",width:900,height:610,transform:"translate(-50%,-50%) scale("+(0.97+panel*.03)+")",opacity:panel,background:"#c6c8c7",border:"3px solid #363e44",boxShadow:"0 25px 90px rgba(0,0,0,.45)",display:"grid",gridTemplateColumns:"1fr 1.15fr",gap:0,padding:14}}>
      <div style={{background:"#b2b7b8",border:"2px solid #727b7e",padding:20,display:"grid",gridTemplateColumns:"repeat(5,1fr)",gridTemplateRows:"repeat(4,1fr)",gap:7}}>{Array.from({length:20},(_,i)=><div key={i} style={{background:"#989fa2",border:"2px solid #7d8588"}} />)}</div>
      <div style={{padding:28,background:"#bfc4c3",border:"2px solid #727b7e",color:"#333a3d",fontFamily:"'Microsoft YaHei',sans-serif"}}>
        <div style={{fontSize:24,fontWeight:700,marginBottom:22}}>饰品</div>
        <div style={{display:"grid",gridTemplateColumns:"repeat(4,1fr)",gap:9}}>{Array.from({length:16},(_,i)=><div key={i} style={{height:76,background:"#a9b0b0",border:"2px solid #858d8f",position:"relative"}}>{i===5&&<Img src={staticFile("gear_heart_damaged.png")} style={{width:"100%",height:"100%",objectFit:"contain",imageRendering:"pixelated"}}/>}</div>)}</div>
        <div style={{marginTop:24,padding:15,background:"#333b41",color:"#f1dfc5",fontSize:19,opacity:heart}}><div style={{fontSize:22,fontWeight:700,marginBottom:8}}>齿轮之心</div><div style={{color:"#c9b6a0",fontSize:15,lineHeight:1.6}}>一件因诅咒而破损的至高造物。</div><div style={{color:"#e8a39c",fontSize:15,marginTop:8}}>无法取下</div></div>
      </div>
    </div>
    <div style={{position:"absolute",left:"50%",top:"48%",width:900,height:610,transform:"translate(-50%,-50%)",pointerEvents:"none",opacity:fail}}><div style={{position:"absolute",left:255,top:180,width:90,height:90,border:"3px solid #e4c49a",borderRadius:5,boxShadow:"0 0 30px rgba(228,196,154,.55)"}}/><div style={{position:"absolute",left:285,top:215,fontSize:44,color:"#f6e3c2",textShadow:"0 3px 10px #000"}}>×</div></div>
    <div style={{position:"absolute",right:120,top:150,width:440,opacity:buffs}}>
      <div style={{...titleStyle,textAlign:"left",fontSize:25,letterSpacing:3}}>残存的力量</div><div style={{height:2,width:130,background:"#d7ad6d",margin:"17px 0 22px"}}/>
      <Buff label="交互距离 +2" detail="更远地触及方块与实体" symbol="↗" index={0} frame={f-220}/>
      <Buff label="急迫 II" detail="挖掘速度提升" symbol="✦" index={1} frame={f-220}/>
      <Buff label="幸运 I" detail="幸运加持仍未消散" symbol="♧" index={2} frame={f-220}/>
    </div>
    <div style={{position:"absolute",bottom:76,width:"100%",...titleStyle,fontSize:26,letterSpacing:3,opacity:fade(f,225,250)*(1-fade(f,470,500))}}>纵使心脏残损，异变赋予它的力量仍未消逝。</div>
    <Specks />
  </AbsoluteFill>;
};

const EndTitle: React.FC = () => {
  const f=useCurrentFrame();
  const {fps}=useVideoConfig();
  const enter=spring({frame:f-18,fps,config:{damping:24,stiffness:65,mass:1.2}});
  const line=interpolate(f,[35,100],[0,560],clamp);
  return <AbsoluteFill style={{background:"radial-gradient(ellipse at 50% 42%,#303640 0%,#141a23 48%,#090d12 100%)",overflow:"hidden"}}>
    <AbsoluteFill style={{opacity:.13,backgroundImage:"linear-gradient(rgba(210,186,151,.3) 1px,transparent 1px),linear-gradient(90deg,rgba(210,186,151,.3) 1px,transparent 1px)",backgroundSize:"84px 84px",transform:"perspective(600px) rotateX(56deg) scale(1.8) translateY("+Math.sin(f/90)*10+"px)"}}/>
    <div style={{position:"absolute",left:"50%",top:"34%",width:190,height:190,transform:"translate(-50%,-50%) scale("+(1+Math.sin(f/35)*.012)*enter+") rotate("+interpolate(f,[0,80],[8,0],clamp)+"deg)",opacity:enter}}><Img src={staticFile("gear_heart_damaged.png")} style={{width:"100%",height:"100%",imageRendering:"pixelated",filter:"drop-shadow(0 0 30px rgba(218,78,75,.35))"}}/></div>
    <div style={{position:"absolute",top:"53%",width:"100%",textAlign:"center",transform:"translateY("+(1-enter)*22+"px)",opacity:enter}}>
      <div style={{fontSize:84,fontWeight:800,letterSpacing:15,color:"#f0e4d0",fontFamily:"'Microsoft YaHei',sans-serif",textShadow:"0 5px 32px rgba(0,0,0,.45)"}}>维度工坊</div>
      <div style={{height:2,width:line,margin:"25px auto 23px",background:"linear-gradient(90deg,transparent,#d6b17c,transparent)"}}/>
      <div style={{fontSize:24,letterSpacing:12,color:"#cdb18d",fontFamily:"Georgia,serif"}}>D I M E N S I O N W O R K S</div>
    </div>
    <div style={{position:"absolute",bottom:85,width:"100%",textAlign:"center",fontSize:18,letterSpacing:5,color:"#8f9aa0",opacity:fade(f,110,150)}}>A WORLD REBUILT. A HEART REMEMBERING.</div>
    <Specks />
  </AbsoluteFill>;
};

export const Intro: React.FC = () => <AbsoluteFill style={{background:"#11151c"}}>
  <Sequence from={0} durationInFrames={450}><Anomaly/></Sequence>
  <Sequence from={450} durationInFrames={450}><HeartChange/></Sequence>
  <Sequence from={900} durationInFrames={300}><FallingGear/></Sequence>
  <Sequence from={1200} durationInFrames={540}><TimePassing/></Sequence>
  <Sequence from={1740} durationInFrames={510}><Awakening/></Sequence>
  <Sequence from={2250} durationInFrames={450}><EndTitle/></Sequence>
</AbsoluteFill>;
