import React, {useLayoutEffect} from 'react';
import {useThree} from '@react-three/fiber';
import {ThreeCanvas} from '@remotion/three';
import {AbsoluteFill, useCurrentFrame, useVideoConfig, staticFile} from 'remotion';
import * as THREE from 'three';
import shots from './shots.json';
import captions from './captions.json';
import {AssetProvider} from './MinecraftModel';
import {StoryWorld, HeartWorld, PowerWorld, MemoryWorld} from './CoreWorlds';
import {MaterialsWorld, ChemistryWorld} from './ProcessWorlds';
import {LogisticsWorld, TwilightWorld, LifeWorld, FutureWorld, EndingWorld} from './TransitWorlds';
import {COLORS, CopperMotif, Tiles, TracePackets} from './ScenePrimitives';
import {arrive, blend3, eased, impact, leave, phase, Vec3} from './motion';
import {scenePlans} from './scene-plan';
import {ShotContext} from './scene-context';
import {shotNotes} from './shot-notes';

const CameraRig:React.FC<{shotId:number}>=({shotId})=>{
  const frame=useCurrentFrame();const {camera,invalidate}=useThree();
  const plan=scenePlans[shotId];const frames=Math.round(shots[shotId].duration*30);
  let origin=plan.camera;let target=plan.target;
  if(plan.layout==='closeup')target=[0,1.2,0];
  if(shotId===0&&frame>=28&&frame<56){origin=[2.2,3.8,9.4];target=[0,2.2,0];}
  if(shotId===0&&frame>=56&&frame<84){origin=[4.1,4.5,10.2];target=[0,2.25,0];}
  if(shotId===1){origin=[5.6,4.5,10];target=[0,1.9,0];}
  if(shotId===45){origin=[6.7,5.1,13];target=[0,2.2,0];}
  if(shotId===22){origin=[.4,4.1,12.8];target=[0,1.9,0];}
  if(shotId===35){origin=[.4,7.2,12];target=[0,1.15,0];}
  if(shotId===54){origin=[.6,6.8,15.2];target=[0,1,0];}
  if(shotId===40){
    const follow=eased(frame,[24,frames*.85],[0,1]);
    target=blend3([-2.5,1,1],[2.5,1.3,-.1],follow);
    origin=[target[0]+6,6,11.5];
  }
  if(shotId===44){
    const p=eased(frame,[60,frames-24],[0,1]);const follow=eased(frame,[60,80],[0,1]);
    target=blend3([-2,1.2,0],[-4+8*p,1.2,0],follow);
    origin=blend3([7,4.3,11],[target[0]+4.2,4.8,8.8],follow);
  }
  const travel=eased(frame,[0,frames],[0,1]);
  const end:Vec3=[origin[0]+plan.dolly[0],origin[1]+plan.dolly[1],origin[2]+plan.dolly[2]];
  const position=blend3(origin,end,travel);const kick=impact(frame,plan.accent);
  useLayoutEffect(()=>{
    camera.position.set(position[0],position[1]+kick*.055,position[2]-kick*.075);
    camera.lookAt(...target);camera.updateMatrixWorld();
    invalidate();
  });
  return null;
};

const World:React.FC<{shotId:number}>=({shotId})=>{
  const frame=useCurrentFrame();
  const plan=scenePlans[shotId];const outdoor=shotId<5||shotId===41||shotId===50||shotId===59;
  const daylight=shotId===3?.5+.5*Math.sin(phase(frame,1.73)):1;
  let scene:React.ReactNode;
  if(shotId<5)scene=<StoryWorld shotId={shotId}/>;
  else if(shotId<11)scene=<HeartWorld shotId={shotId}/>;
  else if(shotId<18)scene=<PowerWorld shotId={shotId}/>;
  else if(shotId<25)scene=<MemoryWorld shotId={shotId}/>;
  else if(shotId<32)scene=<MaterialsWorld shotId={shotId}/>;
  else if(shotId<39)scene=<ChemistryWorld shotId={shotId}/>;
  else if(shotId<46)scene=<LogisticsWorld shotId={shotId}/>;
  else if(shotId<50)scene=<TwilightWorld shotId={shotId}/>;
  else if(shotId<54)scene=<LifeWorld shotId={shotId}/>;
  else if(shotId<59)scene=<FutureWorld shotId={shotId}/>;
  else scene=<EndingWorld/>;
  return <>
    <CameraRig shotId={shotId}/>
    <ambientLight intensity={outdoor?1.05+daylight*.65:1.25}/>
    <hemisphereLight args={['#e4ead9','#374338',outdoor?2:1.3]}/>
    <directionalLight position={[-3,9,5]} color="#ffdeb1" intensity={outdoor?1.2+daylight*2:3.2} castShadow
      shadow-mapSize-width={1024} shadow-mapSize-height={1024}
      shadow-camera-left={-12} shadow-camera-right={12} shadow-camera-top={10} shadow-camera-bottom={-10}
      shadow-bias={-.001} shadow-normalBias={.025}/>
    <directionalLight position={[5,5,-5]} color="#8bd2d0" intensity={2.4}/>
    {!outdoor&&<Tiles/>}{scene}
    <CopperMotif variant={plan.motif} shotId={shotId} accent={plan.accent}/>
    <TracePackets shotId={shotId}/>
  </>;
};

export const PromoScene:React.FC<{shotId:number}>=({shotId})=>{
  const frame=useCurrentFrame();const {width,height}=useVideoConfig();const shot=shots[shotId];
  const frames=Math.round(shot.duration*30);const global=frame+Math.round(shot.start*30);
  const plan=scenePlans[shotId];const story=shotId<5||shotId===59;
  const chapterEntry=arrive(frame,2);const entry=arrive(frame,6);const outro=leave(frame,frames);
  const headOpacity=eased(entry,[0,1],[0,1])*outro;
  const noteEntry=arrive(frame,22);const badgeEntry=arrive(frame,10);const readoutEntry=arrive(frame,18);
  const cue=captions.find(c=>global/30*1000>=c.startMs&&global/30*1000<c.endMs&&c.shotId===shotId);
  const cueLocal=cue?global-Math.round(cue.startMs*30/1000):0;
  const captionRise=arrive(cueLocal);const captionExit=cue?leave(global,Math.round(cue.endMs*30/1000)):1;
  const accent=impact(frame,plan.accent);const drift=Math.sin(phase(frame,17.73,shotId*.31))*26;
  const sky=story||shotId===12||shotId===35;const purple=shotId>=46&&shotId<=49||shot.preview;
  const gradient=sky?'radial-gradient(ellipse at 48% 26%,#526f68 0%,#253b3b 48%,#101b24 88%)'
    :purple?'radial-gradient(ellipse at 54% 38%,#41434e 0%,#222b35 50%,#121d23 90%)'
    :'radial-gradient(ellipse at 52% 38%,#40554d 0%,#203b38 47%,#101d26 90%)';
  return <AbsoluteFill style={{overflow:'hidden',fontFamily:'"PingFang SC",sans-serif',color:COLORS.paper,backgroundImage:gradient}}>
    <AbsoluteFill style={{backgroundImage:'repeating-linear-gradient(108deg,transparent 0px,transparent 102px,#b4d2a90b 103px,transparent 104px)',
      backgroundPosition:`${drift}px ${drift*.47}px`}}/>
    <div style={{position:'absolute',inset:0,opacity:outro,transform:`translateX(${(1-outro)*-65}px) scale(${1+(1-outro)*.025})`}}>
      <ThreeCanvas width={width} height={height} dpr={1} shadows="soft" frameloop="demand"
        camera={{position:plan.camera,fov:41,near:.1,far:100}}
        gl={{antialias:true,alpha:true,preserveDrawingBuffer:true,powerPreference:'low-power'}}
        onCreated={({gl})=>{gl.toneMapping=THREE.ACESFilmicToneMapping;gl.toneMappingExposure=1;}}>
        <ShotContext.Provider value={{id:shotId,frames}}><AssetProvider><World shotId={shotId}/></AssetProvider></ShotContext.Provider>
      </ThreeCanvas>
    </div>
    <AbsoluteFill style={{pointerEvents:'none',backgroundImage:`url(${staticFile('promo/3d/grain.png')})`,
      backgroundPosition:`${frame%7*19}px ${frame%5*23}px`,mixBlendMode:'soft-light',opacity:.64}}/>
    <AbsoluteFill style={{pointerEvents:'none',background:'radial-gradient(ellipse at 50% 45%,transparent 30%,#06131b90 100%)'}}/>
    <div style={{position:'absolute',left:108,top:64,fontSize:20,letterSpacing:5,color:'#c0c6b7',
      opacity:eased(chapterEntry,[0,1],[0,1])*outro,transform:`translateY(${(1-chapterEntry)*-15}px)`}}>
      <span style={{color:COLORS.copper,marginRight:22}}>{String(shotId+1).padStart(2,'0')}</span>
      {shot.chapter} <span style={{opacity:.58,fontSize:16,marginLeft:22}}>DIMENSIONWORKS</span>
    </div>
    <div style={{position:'absolute',left:108,top:107,right:290,fontSize:story?78:shot.title.length>19?51:60,
      fontWeight:story?700:600,lineHeight:1.18,letterSpacing:story?8:2,fontFamily:story?'"Songti SC","PingFang SC",serif':undefined,
      opacity:headOpacity,transform:`translateY(${(1-entry)*32}px) rotate(${(1-entry)*-.7}deg)`,transformOrigin:'left center',
      textShadow:'0 3px 18px #0d1a1b'}}>
      {shotId===59?'维度工序 · 由你来设计':shot.title}
    </div>
    <div style={{position:'absolute',right:108,top:69,fontSize:19,color:shot.preview?COLORS.violet:COLORS.teal,
      borderLeft:`2px solid ${shot.preview?COLORS.violet:COLORS.copper}`,padding:'5px 14px',letterSpacing:1,
      opacity:eased(badgeEntry,[0,1],[0,1])*outro,transform:`translateX(${(1-badgeEntry)*20}px) scale(${.94+.06*badgeEntry})`}}>
      {shot.preview?'开发状态见说明':story?'3D 叙事':'机制演示'}
    </div>
    {shotId===11&&<div style={{position:'absolute',left:122,bottom:224,fontSize:53,color:COLORS.copper,
      fontFamily:'"SFMono-Regular",monospace',opacity:eased(readoutEntry,[0,1],[0,1])*outro,
      transform:`translateY(${(1-readoutEntry)*18}px) scale(${.95+.05*readoutEntry})`,transformOrigin:'left center'}}>1 FE/t = 2 SU</div>}
    {shotId===44&&<div style={{position:'absolute',right:130,bottom:228,fontSize:34,fontVariantNumeric:'tabular-nums',color:COLORS.copper,
      opacity:eased(readoutEntry,[0,1],[0,1])*outro,
      transform:`translateX(${(1-readoutEntry)*25}px) scale(${.95+.05*readoutEntry})`,transformOrigin:'right center'}}>
      {frame>=60?'松手发射 · 落地弹跳':`潜行蓄力 ${Math.min(2,frame/30).toFixed(1)} / 2.0 秒`}
    </div>}
    {shotNotes[shotId]&&<div style={{position:'absolute',left:135,right:135,bottom:180,textAlign:'center',fontSize:24,
      letterSpacing:.5,color:shot.preview?COLORS.violet:'#d5d8c6',lineHeight:1.45,textShadow:'0 2px 6px #061419',
      opacity:eased(noteEntry,[0,1],[0,1])*outro,transform:`translateY(${(1-noteEntry)*20}px)`}}>{shotNotes[shotId]}</div>}
    {cue&&<div style={{position:'absolute',left:170,right:170,bottom:76,textAlign:'center',fontSize:35,lineHeight:1.45,
      padding:'13px 26px',background:'linear-gradient(90deg,#081b1b00,#081b1be8 12%,#081b1be8 88%,#081b1b00)',
      textShadow:'0 2px 6px #06151a',color:'#fff0d4',opacity:eased(captionRise,[0,1],[0,1])*captionExit*outro,
      transform:`translateY(${(1-captionRise)*18+(1-captionExit)*-10}px) scale(${.985+.015*captionRise})`}}>{cue.text}</div>}
    <div style={{position:'absolute',left:108,right:108,bottom:37,height:2,background:'#aac4b52b'}}>
      <div style={{width:`${global/10800*100}%`,height:2,background:COLORS.copper}}/>
    </div>
    <div style={{position:'absolute',bottom:12,right:108,fontSize:14,fontFamily:'monospace',letterSpacing:1,color:'#a7b6a5'}}>
      {String(Math.floor(global/1800)).padStart(2,'0')}:{String(Math.floor(global/30)%60).padStart(2,'0')} / 06:00
    </div>
    <div style={{position:'absolute',left:-40,top:214,height:8,width:width+80,background:COLORS.copper,
      opacity:accent*.42,transform:`translateX(${eased(frame,[plan.accent,plan.accent+6],[-width,width])}px) skewX(-24deg) scaleY(${.35+accent*.65})`}}/>
  </AbsoluteFill>;
};
