import React from 'react';
import {staticFile} from 'remotion';
import {Gear} from '../Cinema';
import {MechanicalPress, Motor, Gearbox, EnergyBlock, DriveBlock} from '../WorkshopIllustrations';
import {Steve} from '../Steve';
import {Sprite,TexturedCase,itemTexturesFor} from './Textures';
export const C={paper:'#f0e6d1',muted:'#abc0be',copper:'#e0a46d',teal:'#80d8c8',violet:'#b396dc',red:'#ed9c91'};
export const Text:React.FC<{x:number;y:number;children:React.ReactNode;size?:number;color?:string}> =({x,y,children,size=32,color=C.paper})=><text x={x} y={y} textAnchor="middle" fill={color} fontSize={size} fontFamily="PingFang SC, sans-serif">{children}</text>;
export const Box:React.FC<{color?:string;children?:React.ReactNode}>=({color='#4b6570',children})=><><path d="M-65 -60 l25 -20 h130 l-25 20Z" fill="#8a9b99" stroke="#c6c4ad" strokeWidth="2"/><path d="M65 -60 l25 -20 v140 l-25 20Z" fill="#223c45" stroke="#849c9c" strokeWidth="2"/><rect x="-65" y="-60" width="130" height="140" rx="3" fill={color} stroke="#adb8a9" strokeWidth="3"/>{children}</>;
export const Person:React.FC<{x:number;y:number;t:number;scale?:number;packed?:boolean;elastic?:boolean}> =({x,y,t,scale=.52,packed=false,elastic=false})=><g transform={`translate(${x} ${y}) scale(${scale})`}>{packed?<><path d="M-85 0 l35 -30 h170 l-35 30Z" fill={elastic?'#8dce73':'#cda77b'}/><path d="M85 0 l35 -30 v150 l-35 30Z" fill={elastic?'#538b64':'#9d754f'}/><rect x="-85" y="0" width="170" height="150" fill={elastic?'#76b978':'#bd9368'} stroke="#f1dfba" strokeWidth="4"/><path d="M-85 74 H85 M0 0 V150" stroke="#edd6a1" strokeWidth="12"/><rect x="-56" y="25" width="18" height="18" fill="#244940"/><rect x="32" y="25" width="18" height="18" fill="#244940"/></>:<Steve x={0} y={0} time={t}/>}</g>;
export const Art:React.FC<{kind:string;x:number;y:number;t:number;active?:boolean;scale?:number;label?:string;sprite?:string}> =({kind,x,y,t,active=true,scale=1,label='',sprite})=>{
 const tick=active?t:0;
 const machine=kind==='factory'||kind==='machine'||kind==='leach'||kind==='condition';
 const materials=sprite?[sprite]:itemTexturesFor(label);
 const item=(['material','output','orb','crystal','cooking'].includes(kind)&&materials.length>0);
 const front=label.includes('结合器')?'combiner_front':label.includes('提纯')?'purifier_front':'factory_front';
 return <g transform={`translate(${x} ${y}) scale(${scale})`}>
 <ellipse cx="10" cy="150" rx="100" ry="20" fill="#050e16" opacity=".35"/>
 {item?<>{materials.map((name,i)=><Sprite key={name} name={name} x={materials.length>1?-100+i*105:-65} y={-50+Math.sin(t*2)*5} size={materials.length>1?100:130}/>)}</>:
 machine?<TexturedCase front={kind==='leach'||kind==='condition'?'twilight_casing':front} top={kind==='leach'?'forest_leacher_top':kind==='condition'?'boar_conditioner_top':front==='combiner_front'?'combiner_top':'factory_top'} t={tick} active={active}/>:
 kind==='press'?<g transform="translate(0 -62) scale(.65)"><MechanicalPress x={0} y={0} time={tick} active={active}/><Sprite name="press_front" x={-55} y={-49} size={110}/></g>:
 kind==='motor'?<Motor x={0} y={0} time={tick}/>:
 kind==='me'?<TexturedCase front="me_controller" t={tick} active={active} width={122} height={122}/>:
 kind==='import'||kind==='export'?<Gearbox x={0} y={0} time={tick} mode={kind==='import'?'IMPORT':'EXPORT'} active={active}/>:
 kind==='drive'?<DriveBlock x={0} y={0} tier={3} size={132}/>:
 kind==='vibration'?<TexturedCase front="vibration_front" t={tick} active={active} width={117} height={117}/>:
 kind==='receiver'?<g transform="scale(1.5)"><EnergyBlock x={0} y={0} kind="kinetic" time={tick}/></g>:
 kind==='pig'||kind==='frog'?<><rect x="-65" y="-18" width="130" height="80" fill={kind==='pig'?'#b68380':'#7cb681'} stroke="#dfcc9a" strokeWidth="3"/><rect x="-46" y="62" width="25" height="38" fill="#745f61"/><rect x="28" y="62" width="25" height="38" fill="#745f61"/><rect x="-30" y="-42" width="70" height="57" fill={kind==='pig'?'#d6a49c':'#9fd0a0'}/><rect x="-18" y="-28" width="10" height="10" fill="#263d40"/><rect x="17" y="-28" width="10" height="10" fill="#263d40"/><rect x="-10" y="-8" width="35" height="18" fill="#ebbaaa"/></>:
 kind==='roots'||kind==='farm'?<>{Array.from({length:9},(_,i)=><g key={i} transform={`translate(${(i%3-1)*43+(Math.floor(i/3)-1)*20} ${(Math.floor(i/3)-1)*35})`}><path d="M-20 0 l20 -12 l40 0 l-20 12Z" fill="#799671"/><path d="M-20 0 h40 v25 h-40Z" fill="#79604b"/><path d="M-12 9 l16 8 l10 -6" fill="none" stroke="#d4b982" strokeWidth="3"/></g>)}</>:
 kind==='wind'?<><rect x="-12" y="-10" width="24" height="150" fill="#b4aa8c"/><g transform={`rotate(${tick*55})`}><circle r="18" fill={C.copper}/>{[0,120,240].map(a=><path key={a} d="M0 -10 L-18 -130 L18 -110 L10 0Z" fill="#d8d5b4" transform={`rotate(${a})`}/>)}</g></>:
 kind==='solar'?<><path d="M-88 -35 L52 -78 L92 5 L-48 47Z" fill="#395984" stroke="#a1bccb" strokeWidth="5"/>{[0,1,2].map(i=><path key={i} d={`M${-55+i*40} ${-45-i*12} l35 75`} stroke="#8cacbe" strokeWidth="3"/>)}<rect x="-7" y="35" width="14" height="75" fill="#a4a38d"/><circle cx="75" cy="-115" r="24" fill="#e9cb88"/></>:
 kind==='tank'||kind==='basin'?<><Box color="#355865"><rect x="-49" y="-38" width="99" height="95" fill="#172e40" stroke="#bac6b1" strokeWidth="4"/><rect x="-43" y={-20+Math.sin(t)*3} width="87" height="71" fill={kind==='basin'?C.copper:C.teal} opacity=".72"/>{[0,1,2,3].map(i=><circle key={i} cx={-30+i*19} cy={28-((t*15+i*14)%50)} r="3" fill="#fff0b7" opacity=".6"/>)}{kind==='basin'&&<Gear x={0} y={-70} radius={27} angle={tick*90}/>}</Box></>:
 kind==='spout'||kind==='deployer'?<><Box color="#5e6660">{kind==='deployer'&&<Sprite name="deployer" x={-65} y={-60} size={130}/>}<rect x="-10" y="-20" width="20" height={60+Math.sin(tick*5)*15} fill={C.copper}/><path d="M-29 45 H30 l-10 28 h-28Z" fill="#c9c3a9"/>{kind==='spout'&&<path d={`M0 70 v${18+(t*22)%40}`} stroke={C.copper} strokeWidth="6"/>}</Box></>:
 kind==='mill'||kind==='crusher'||kind==='gear'||kind==='fan'||kind==='turbine'?<><Box color="#576565">{kind==='mill'&&<Sprite name="millstone" x={-65} y={-60} size={130}/>}<Gear x={0} y={4} radius={44} angle={tick*(kind==='fan'?190:65)} color={C.copper}/></Box></>:
 kind==='reactor'||kind==='shield'?<><path d="M-70 -50 L0 -85 L70 -50 V85 L0 115 L-70 85Z" fill="#3e344d" stroke={C.violet} strokeWidth="4"/><circle cy="10" r={41+Math.sin(tick*4)*3} fill={C.violet} opacity=".8"/><circle cy="10" r="62" fill="none" stroke={C.teal} strokeWidth="5" strokeDasharray="8 12" strokeDashoffset={-tick*24}/></>:
 kind==='belt'||kind==='chain'?<><path d="M-85 38 H75 v35 H-85Z" fill="#3c454b" stroke="#a4b1a6" strokeWidth="5"/>{Array.from({length:7},(_,i)=><path key={i} d={`M${-80+((i*27+tick*40)%170)} 40 v30`} stroke={C.copper} strokeWidth="3"/>)}<Gear x={-65} y={56} radius={17} angle={tick*80}/><Gear x={65} y={56} radius={17} angle={tick*80}/></>:
 kind==='card'||kind==='module'?<><rect x="-52" y="-55" width="104" height="140" rx="6" fill="#284c43" stroke={C.teal} strokeWidth="4"/><rect x="-28" y="-25" width="56" height="55" fill="#172933" stroke={C.copper} strokeWidth="4"/>{[0,1,2,3,4].map(i=><rect key={i} x={-41+i*18} y="64" width="10" height="18" fill="#cba56b"/>)}</>:
 kind==='orb'||kind==='crystal'?<><path d="M0 -75 L58 -18 L30 66 L-30 66 L-58 -18Z" fill={C.violet} stroke="#e1d1ed" strokeWidth="4"/><path d="M0 -75 V65 M-58 -18 H58" stroke="#f1dfef" strokeWidth="3"/></>:
 kind==='cloud'?<><circle cx="-42" cy="-6" r="36" fill="#6f9cad"/><circle cy="-25" r="53" fill="#8badbd"/><circle cx="48" cy="-4" r="35" fill="#6f9cad"/><rect x="-73" y="0" width="151" height="55" rx="20" fill="#8badbd"/><path d="M0 -16 V43 m-22 -22 l22 22 l22 -22" fill="none" stroke={C.paper} strokeWidth="7"/></>:
 kind==='ampoule'?<><rect x="-25" y="-74" width="50" height="25" fill={C.copper}/><rect x="-32" y="-48" width="64" height="140" rx="18" fill="#244d5d" stroke="#c0cec1" strokeWidth="5"/><rect x="-23" y="-10" width="46" height="88" rx="10" fill={C.violet}/></>:
 kind==='maid'||kind==='fairy'||kind==='adventure'?<><Person x={0} y={-80} t={tick} scale={.4}/>{kind==='fairy'&&<path d="M0 20 Q-110 -100 -70 40 Q-90 100 0 40 Q110 -100 70 40 Q90 100 0 40" fill={C.teal} opacity=".3"/>}</>:
 kind==='pack'||kind==='unpack'||kind==='endpoint'?<><Box color={kind==='endpoint'?'#4b3861':'#795e47'}>{kind!=='endpoint'&&<Sprite name="packager" x={-65} y={-60} size={130}/>}<rect x="-38" y="-28" width="76" height="70" fill="#253b41" stroke={C.copper} strokeWidth="4"/><path d="M-21 4 H21 m-11 -12 l11 12 l-11 12" stroke={C.teal} strokeWidth="5" fill="none"/></Box></>:
 kind==='meter'||kind==='controller'?<><Box><rect x="-49" y="-35" width="98" height="80" fill="#142d38"/><path d="M-35 26 A35 35 0 0 1 35 26" stroke={C.teal} strokeWidth="7" fill="none"/><path d={`M0 26 L${Math.sin(tick)*25} -5`} stroke={C.copper} strokeWidth="5"/></Box></>:
 <><path d="M-65 10 L-28 -16 H65 L28 10Z" fill="#e9c294"/><path d="M-65 10 H28 V49 H-65Z" fill="#c19a73"/><path d="M28 10 l37 -26 v39 l-37 26Z" fill="#977656"/><path d="M-41 22 H4" stroke="#f5dfb7" strokeWidth="5"/></>}
 {!item&&materials.length>0&&materials.map((name,i)=><g key={name} transform={`translate(${80+i*48} -68)`}><circle r="29" fill="#183039" stroke="#719185"/><Sprite name={name} x={-23} y={-23} size={46}/></g>)}
 </g>;
};
export const Pipe:React.FC<{a:[number,number];b:[number,number];t:number;active?:boolean;color?:string}>=({a,b,t,active=true,color=C.teal})=>{
 const path=`M${a[0]} ${a[1]} H${(a[0]+b[0])/2} V${b[1]} H${b[0]}`;
 return <g><path d={path} stroke="#263c49" strokeWidth="16" fill="none"/><path d={path} stroke="#668081" strokeWidth="12" fill="none"/><path d={path} stroke={active?color:'#405761'} strokeWidth="5" strokeDasharray="10 22" strokeDashoffset={active?-t*64:0} fill="none"/></g>;
};
