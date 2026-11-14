import React from 'react';
import {AbsoluteFill,Img,staticFile,useCurrentFrame,useVideoConfig,interpolate} from 'remotion';
import shots from './shots.json';
import captions from './captions.json';
import {Landscape,Gear} from '../Cinema';
import {DriveBlock,FACTORY_TYPES} from '../WorkshopIllustrations';
import {C,Text,Art,Pipe,Person} from './Illustrations';
import {Sprite} from './Textures';
export const fade=(t:number,a:number,b:number)=>interpolate(t,[a,b],[0,1],{extrapolateLeft:'clamp',extrapolateRight:'clamp'});
const FLOOR=<><path d="M120 775 L510 385 H1460 L1850 775Z" fill="#20343f" stroke="#557378" strokeWidth="2"/>{Array.from({length:10},(_,i)=><path key={i} d={`M${120+i*173} 775 L${510+i*95} 385 M${120+i*43} ${775-i*43} H${1850-i*43}`} stroke="#557578" strokeWidth="1" opacity=".22"/>)}</>;
const Graph:React.FC<{s:typeof shots[number];t:number}>=({s,t})=>{
 const n=s.nodes.length;const cols=n>6?4:n>4?3:n;const points=s.nodes.map((_,i)=>[n>4?(cols===4?310:440)+(i%cols)*(cols===4?420:515):290+i*(1340/(n-1||1)),n>4?405+Math.floor(i/cols)*260:490] as [number,number]);
 const active=s.kind==='live'?(t<2.3||t>4.9):true;
 return <>{points.slice(1).map((p,i)=><Pipe key={i} a={points[i]} b={p} t={t} active={active} color={i%2?C.copper:C.teal}/>)}
 {(s.kind==='loop'||s.kind==='netherrack')&&<path d="M1500 775 H290 V545" fill="none" stroke={C.copper} strokeWidth="7" strokeDasharray="10 16" strokeDashoffset={-t*45}/>}
 {s.nodes.map((node,i)=>{const [kind,...label]=node.split(':');const p=points[i];const reveal=fade(t,Math.min(i,.5+i*.55),Math.min(i,.5+i*.55)+.35);return <g key={node} opacity={.2+.8*reveal} transform={`translate(0 ${(1-reveal)*15})`}><Art kind={kind} label={label.join(':')} x={p[0]} y={p[1]} t={t} active={active} scale={n>4?.85:1.18}/><Text x={p[0]} y={p[1]+(n>4?135:185)} size={n>4?29:34}>{label.join(':')}</Text><circle cx={p[0]-70} cy={p[1]-80} r="18" fill={i===Math.floor(t)?C.copper:'#47616a'}/><Text x={p[0]-70} y={p[1]-74} size={20}>{i+1}</Text></g>})}
 {s.kind==='power'&&<Text x={960} y={760} size={50} color={C.copper}>1 FE/t = 2 SU</Text>}
 {s.kind==='ae'&&<Text x={960} y={825} size={27}>机械应力与 AE 供能分别计量 · 需要正常频道</Text>}
 {s.kind==='live'&&<Text x={960} y={780} size={50} color={active?C.teal:C.red}>{active?'输入足额 → 正常输出':'输入归零 → 输出停机'}</Text>}
 {s.kind==='forest'&&<Text x={960} y={820} size={27}>只在暮色采集 · 不强加载 · 不结算离线产量</Text>}
 {s.kind==='life'&&<Text x={960} y={835} size={25}>整合生态：农业 / 烹饪 / 冒险 · 具体魔改：女仆参与AE手摇曲柄</Text>}
 {s.kind==='boars'&&<Text x={960} y={825} size={27}>128 RPM 达到最高处理速度 · 每只野猪独占工位并休息</Text>}
 </>;
};
const Network:React.FC<{s:typeof shots[number];t:number}>=({s,t})=>{
 const running=s.kind==='live'?(t<2.3||t>4.9):true;
 const nodes=['motor','import','me','export','press'];const labels=['机械输入','IMPORT齿轮箱','ME网络','EXPORT齿轮箱','远端压力机'];
 return <>{nodes.slice(1).map((_,i)=><Pipe key={i} a={[270+i*330,520]} b={[600+i*330,520]} t={t} active={running}/>)}{nodes.map((k,i)=><g key={k}><Art kind={k} x={270+i*330} y={520} t={t} active={running} scale={1.04}/><Text x={270+i*330} y={725} size={31}>{labels[i]}</Text></g>)}
 {s.kind==='ae'&&<><Art kind="receiver" x={780} y={330} t={t} scale={.75}/><Art kind="vibration" x={1090} y={330} t={t} scale={.75}/><Pipe a={[780,365]} b={[930,490]} t={t} color={C.violet}/><Pipe a={[1090,365]} b={[930,490]} t={t} color={C.violet}/><Text x={780} y={275} size={26}>动能接收器</Text><Text x={1090} y={275} size={26}>谐振仓</Text><Text x={960} y={835} size={29} color={C.violet}>两条AE供能支路 · 网络需要供电与正常频道</Text></>}
 {s.kind==='live'&&<Text x={960} y={835} size={42} color={running?C.teal:C.red}>{running?'实时输入足额 → 输出运行':'输入停止 → 额度归零 → 输出停机'}</Text>}
 </>;
};
const Tiles:React.FC<{s:typeof shots[number];t:number}>=({s,t})=>{
 const cols=s.nodes.length>6?4:3;const rows=Math.ceil(s.nodes.length/cols);const gap=22;const w=1440/cols;const h=rows>2?153:235;const startY=rows>2?295:335;
 return <>{s.nodes.map((label,i)=><g key={i} transform={`translate(${240+(i%cols)*(w+gap)} ${startY+Math.floor(i/cols)*(h+gap)})`} opacity={s.kind==='qol'?1:.35+.65*fade(t,i*.28,i*.28+.3)}>
 <rect width={w} height={h} rx="12" fill={i===Math.floor(t)%s.nodes.length?'#3a4d51':'#213842'} stroke={i===Math.floor(t)%s.nodes.length?C.copper:'#526e74'} strokeWidth="2"/>
 {s.kind==='ddr'?<DriveBlock x={w/2} y={65} tier={i+1} size={86}/>:s.kind==='cards'?<image href={staticFile(`memory/card_ddr3_${['economy','balanced','high_speed','high_storage','defective','final'][i]}.png`)} x={w/2-48} y="15" width="96" height="96"/>:<Gear x={w/2} y={rows>2?40:65} radius={rows>2?19:33} angle={t*35} color={s.kind==='chemicals'?C.teal:C.copper}/>}
 <Text x={w/2} y={rows>2?105:155} size={s.kind==='quotas'?28:s.kind==='qol'?30:33}>{label}</Text>
 {s.kind==='qol'&&['扳手联动 · 永久超级胶水\n鼓风染色兼容','F4进入旁观\n再按返回原位置','保留领地保护 · 禁用FTB强加载\n掉落物按领地与稀有度计时','背包 / 箱体 / 抽屉保留\n精妙存储网络组件配方退役','拾取竞争防重复\n封装搬运保留数据','中文信息与版本检查\n跨平台更新器'][i].split('\n').map((line,j)=><Text key={line} x={w/2} y={185+j*31} size={20} color={C.muted}>{line}</Text>)}
 {s.kind==='ddr'&&<Text x={w/2} y={198} size={24} color={C.teal}>{[4,8,16,32,64][i]}槽 · {[256,512,1024,2048,4096][i]} RPM</Text>}
 {s.kind==='cards'&&<Text x={w/2} y={200} size={23} color={i===5?C.violet:C.muted}>{['0.5× / 2%','1× / 1.5%','2× / 1.75%','0.5× / 0.75%','0.25× / 20%','4× / 0% · 暂无配方'][i]}</Text>}
 </g>)}
 {(s.kind==='ddr'||s.kind==='cards')&&<Text x={960} y={880} size={28}>{s.kind==='ddr'?'每个逻辑 ME 网络最多两台有效驱动器':'卡倍率 / 单卡输入损耗 · 卡与驱动器须同代'}</Text>}
 </>;
};
const Gauges:React.FC<{s:typeof shots[number];t:number}>=({s,t})=>{
 const over=s.kind==='overload';const a=over?(t<2?0.76:t<3.4?1.12:.68):t>3?.91:.37;const b=over?(t<4?0.66:t<5.2?1.12:.75):.65;
 return <><Art kind="motor" x={390} y={490} t={t} active={a<=1&&b<=1} scale={1.7}/><Art kind="factory" x={1500} y={490} t={t} active={a<=1&&b<=1} scale={1.7}/>{[a,b].map((v,i)=><g key={i}><Text x={950} y={390+i*185} size={35}>{over?s.nodes[i]:i===0?'转速上限：32 RPM':'自有网络合计：4,096 SU'}</Text><rect x="650" y={430+i*185} width="600" height="48" rx="6" fill="#182b35"/><rect x="650" y={430+i*185} width={Math.min(600,v*550)} height="48" rx="6" fill={v>1?C.red:C.teal}/><path d={`M1200 ${418+i*185} v74`} stroke={C.copper} strokeWidth="4"/></g>)}<Text x={960} y={820} size={45} color={a>1||b>1?C.red:C.teal}>{over?(a>1?'应力输出过载':b>1?'输出转速过载':'两项足额 · 自动恢复'):'限制按玩家聚合当前活跃系统'}</Text></>;
};
const Story:React.FC<{s:typeof shots[number];t:number}>=({s,t})=>{
 const night=s.kind==='time'?(1+Math.sin(t*6))/2*.65:s.kind==='story'?fade(t,2.5,5)*.65:.15;
 return <><Landscape time={s.kind==='time'?t*12:t*.2} growth={.7+fade(t,0,6)*.3} night={night}/>
 {s.kind==='story'&&<><AbsoluteFill style={{background:'radial-gradient(ellipse at 50% 20%,#dcb0a366,transparent 65%)',opacity:fade(t,2,5)}}/><svg width="1920" height="1080"><path d="M740 70 L850 150 L790 260 L1020 310 L980 410 M850 150 L1170 180 L1250 80" fill="none" stroke="#f6ddc0" strokeWidth="4" opacity={fade(t,2.5,4.5)}/></svg><AbsoluteFill style={{background:'#08121c',opacity:1-fade(t,0,1)}}/></>}
 {s.kind==='heart'&&<><AbsoluteFill style={{background:'#112330',opacity:.85}}/><div style={{position:'absolute',left:780,top:320,scale:String(1+Math.sin(t*6)*.03)}}><Img src={staticFile('heart_red.png')} style={{position:'absolute',width:360,opacity:1-fade(t,1.8,4.4),imageRendering:'pixelated'}}/><Img src={staticFile('gear_heart_damaged.png')} style={{width:360,opacity:fade(t,1.8,4.4),imageRendering:'pixelated'}}/></div></>}
 {s.kind==='fall'&&<><svg width="1920" height="1080"><g transform={`rotate(${fade(t,3,4.5)*75} 960 810)`}><Person x={960} y={490} t={t} scale={.95}/></g><Gear x={960} y={-160+fade(t,1,3)*620} radius={95} angle={t*90}/></svg><AbsoluteFill style={{background:'#f9e6c9',opacity:fade(t,3,3.1)*(1-fade(t,3.1,3.45))}}/><AbsoluteFill style={{background:'#050b11',opacity:fade(t,3.7,4.7)}}/></>}
 {s.kind==='time'&&<div style={{position:'absolute',top:350,width:'100%',textAlign:'center',fontSize:110,color:C.paper,fontWeight:600,textShadow:'0 4px 20px #18302b'}}>{Math.floor(fade(t,0,4.3)*30000).toLocaleString('en-US')}<span style={{fontSize:45}}> 年</span></div>}
 {(s.kind==='awake'||s.kind==='end')&&<><svg width="1920" height="1080"><Person x={650} y={530} t={t} scale={.9}/>{s.kind==='end'&&<g opacity=".65"><Art kind="wind" x={280} y={570} t={t}/><Art kind="factory" x={1050} y={680} t={t}/><Art kind="basin" x={1320} y={700} t={t}/><Pipe a={[1080,700]} b={[1340,700]} t={t}/></g>}</svg><div style={{position:'absolute',left:1030,top:340,opacity:fade(t,s.kind==='end'?0:3,.5+(s.kind==='end'?0:3))}}><Img src={staticFile('gear_heart_normal.png')} style={{width:120,imageRendering:'pixelated'}}/><div style={{fontSize:90,color:C.paper,fontWeight:600,marginTop:20}}>维度工序</div><div style={{fontSize:31,letterSpacing:6,color:'#e5cba7',marginTop:16}}>DIMENSIONWORKS</div>{s.kind==='end'&&<><div style={{fontSize:29,color:C.paper,marginTop:35}}>github.com/szx-jn/DimensionWorks</div><div style={{fontSize:25,color:C.muted,marginTop:14}}>GitHub Releases · Minecraft 1.20.1 Forge</div><div style={{fontSize:24,color:C.muted,marginTop:12}}>持续开发中</div></>}</div></>}
 </>;
};
const Comparison:React.FC<{s:typeof shots[number];t:number}>=({s,t})=>{
 const crystal=s.kind==='crystal';const gas=s.kind==='gas';
 return <>{[0,1].map(i=>{const x=530+i*850;const changed=crystal&&i===0&&t>3.65;return <g key={i}>
 <rect x={x-305} y="300" width="610" height="510" rx="18" fill="#1e3540" stroke="#56777c" strokeWidth="2"/>
 <Text x={x} y={350} size={36}>{crystal?(i?'上方遮挡':'能直见天空'):gas?(i?'专用盆盖关闭':'无盖 / 开盖'):(i?'超级加热':'普通加热')}</Text>
 {crystal?<><path d={`M${x-190} 590 l90 -85 h250 l-90 85Z`} fill={changed?'#b194da':'#d99469'} stroke="#edcfaa" strokeWidth="3"/>{i===1&&<path d={`M${x-220} 438 l80 -48 h380 l-80 48Z`} fill="#819695" stroke="#bec9b5" strokeWidth="3"/>}<path d={`M${x-70} 420 V475`} stroke={i?C.red:C.teal} strokeWidth="5" strokeDasharray="8 9"/><Text x={x} y={690} size={55} color={changed?C.violet:C.teal}>{i?'计时重置':`${Math.min(30,Math.floor(t/3.65*30))} 秒`}</Text>{changed&&Array.from({length:8},(_,j)=><path key={j} d={`M${x-145+j*36} ${550+Math.sin(t+j)*9} l12 -15 l10 16 l-10 15Z`} fill="#e4c3f2"/>)}<Text x={x} y={755} size={29}>{changed?'已结晶 · 保留流动状态':'世界中的熔融流体方块'}</Text></>:<><Art kind="basin" x={x} y={500} t={t} scale={1.4}/>{gas&&i===1&&<path d={`M${x-110} 382 h220 v21 h-220Z`} fill="#899b8c" stroke="#decda3" strokeWidth="3"/>}{gas&&i===0&&Array.from({length:9},(_,j)=><circle key={j} cx={x-90+j*23} cy={425-((t*65+j*17)%120)} r="7" fill={C.teal} opacity=".45"/>)}<Text x={x} y={740} size={35} color={C.teal}>{gas?(i?'保留气体':'200 mB / 秒'):(i?'250 mB CO₂':'125 mB CO')}</Text></>}
 </g>})}<Text x={960} y={850} size={26} color={C.muted}>{crystal?'30秒时间压缩示意 · 不代表敞口工作盆或任意储罐判定':gas?'只有专用盆盖有效 · 屋顶和普通方块不算密封盖':'热态决定配方 · 切换气体前抽空出气缓冲'}</Text></>;
};
const Transport:React.FC<{s:typeof shots[number];t:number}>=({s,t})=>{
 const vertical=s.kind==='vertical';const elastic=s.kind==='elastic';
 if(vertical)return <><rect x="875" y="320" width="170" height="450" rx="16" fill="#233a40" stroke={C.copper} strokeWidth="7"/>{Array.from({length:12},(_,i)=><path key={i} d={`M890 ${330+(i*39+(t<3?t:6-t)*75)%420} H1030`} stroke="#a99a79" strokeWidth="6"/>)}<Person x={840+(t>5?(t-5)*-180:0)} y={590+(t<3?-t*65:(t-3)*65)} t={t} packed elastic/><Person x={1120} y={370+(t<3?t*65:195-(t-3)*65)} t={t} packed elastic/><Text x={960} y={840} size={42}>{t<3?'两面相反方向':t<5?'反转立即改变方向':'Shift脱离 · 恢复普通运动'}</Text></>;
 if(elastic){const charging=Math.max(0,Math.min(2,t-1));const launch=Math.max(0,t-3.45);return <><path d="M180 720 H1740" stroke="#829789" strokeWidth="6"/><Person x={500+Math.min(1.85,launch)*670} y={launch===0?580:580-Math.abs(Math.sin(launch*Math.PI))*220} t={t} packed elastic scale={.75}/><rect x="750" y="395" width="600" height="62" rx="10" fill="#1d323b"/>{[0,1,2].map(i=><rect key={i} x={755+i*198} y="400" width={Math.min(190,Math.max(0,(charging*1.5-i)*190))} height="52" fill={charging>=2?'#f0ca78':[C.teal,'#e0a46d',C.red][i]}/>)}<Text x={1050} y={525} size={50}>{t<1?'站稳，按住 Shift':t<3?`${charging.toFixed(1)} / 2.0 秒`:t<3.45?'两秒蓄满 · 全金色':'松键 · 沿视线发射'}</Text><Text x={1050} y={590} size={28}>正常20 TPS · 40 tick蓄满 · 满蓄力力度为此前5倍</Text></>;}

 if(s.kind==='parcel')return <><Graph s={s} t={t}/><Person x={290+Math.min(1,t/5)*1340} y={t<2?400:t<4?300:430} t={t} packed scale={.5}/>{t>5&&<Fireworks x={1630} y={510} t={t-5}/>}</>;
 if(s.kind==='packing')return <>{['material','machine','pig','pack'].map((k,i)=><g key={k}><Art kind={k} x={360+i*400} y={470} t={t}/><Text x={360+i*400} y={730} size={29}>{s.nodes[i]}</Text>{i===3&&<Person x={1560} y={410} t={t} packed={t>3} scale={.6}/>}</g>)}</>;
 return <><Art kind="unpack" x={500} y={540} t={t} scale={2}/><Person x={400+Math.min(t,2)*80} y={480} t={t} packed={t<5} scale={.6}/>{s.nodes.map((n,i)=><g key={i}><path d={`M650 520 H${1050+i%2*450} V${400+Math.floor(i/2)*260}`} stroke={i===Math.floor(t-2)?C.teal:'#4b6674'} fill="none" strokeWidth="7"/><rect x={865+i%2*450} y={330+Math.floor(i/2)*260} width="350" height="170" rx="12" fill="#2d444b" stroke={C.teal}/><Text x={1040+i%2*450} y={425+Math.floor(i/2)*260} size={38}>{n}</Text></g>)}{t>5&&<Fireworks x={1490} y={630} t={t-5}/>}</>;
};
const Fireworks:React.FC<{x:number;y:number;t:number}>=({x,y,t})=><g>{Array.from({length:24},(_,i)=>{const a=i*2.4;return <circle key={i} cx={x+Math.cos(a)*t*150} cy={y+Math.sin(a)*t*150} r="5" fill={[C.copper,C.teal,C.violet][i%3]} opacity={Math.max(0,1-t*.65)}/>})}</g>;
const Egg:React.FC<{s:typeof shots[number];t:number}>=({s,t})=>s.kind==='egg'?<><ellipse cx="425" cy="505" rx="95" ry="130" fill="#4a4540" stroke="#c4b19a" strokeWidth="7"/><path d="M410 390 l-20 100 l70 50 l-45 70" stroke="#d6c7a4" strokeWidth="3" fill="none"/><rect x="670" y="310" width="965" height="470" rx="18" fill="#223942" stroke={C.teal} strokeWidth="3"/><Text x={1150} y={375} size={36}>效果选择 · 搜索 / 分类 / 多选</Text>{['夜视 Ⅴ','急迫 Ⅴ','速度 Ⅴ','继承上次选择'].map((n,i)=><g key={i}><rect x="745" y={425+i*70} width="35" height="35" fill={t>i+1?C.teal:'#405c63'}/><Text x={1130} y={455+i*70} size={34}>{n}</Text></g>)}<Text x={1150} y={825} size={30}>确认后：持续效果10分钟 · 同时启动死亡倒计时</Text></>:<>{[0,1].map(i=><g key={i}><rect x={235+i*800} y="320" width="660" height="470" rx="18" fill="#2a373e" stroke={C.red}/><Text x={565+i*800} y={375} size={36}>{i?'倒计时内再次食用':'倒计时到期'}</Text><Person x={565+i*800} y={500} t={t} scale={.5}/><Text x={565+i*800} y={710} size={48} color={C.red}>{i?(t>4?'立即死亡':'再次食用'):t<3?`${Math.max(0,Math.floor(600-t*200))} 秒`:'死亡＋烟花'}</Text>{t>(i?4:3)&&<Fireworks x={565+i*800} y={530} t={(t-(i?4:3))%1}/>}</g>)}<Text x={960} y={840} size={28}>10分钟时间压缩示意 · 以正常TPS计时</Text></>;
const Territory:React.FC<{t:number}>=({t})=><>{Array.from({length:9},(_,i)=><g key={i} transform={`translate(${660+i%3*170+Math.floor(i/3)*100} ${395+Math.floor(i/3)*115})`}><path d="M-100 0 l100 -60 l165 0 l-100 60Z" fill="#647b6a" stroke="#d9d6b7" strokeWidth="3"/><path d="M-100 0 H65 V30 H-100Z" fill="#444d47"/></g>)}<Art kind="unpack" x={940} y={500} t={t}/><Person x={1160} y={490} t={t}/><Sprite name="dimension_key" x={1350} y={450} size={115}/><Text x={960} y={780} size={54}>48 × 48 · 个人中转领地</Text><Text x={960} y={840} size={29}>第二项修复后解锁 · P键往返 · 安全落点 · 禁止新放置AE方块</Text></>;
export const PromoScene:React.FC<{shotId:number}>=({shotId})=>{
 const s=shots[shotId];const {fps}=useVideoConfig();const frame=useCurrentFrame();const t=frame/fps;
 const story=['story','heart','fall','time','awake','end'].includes(s.kind);
 const graph=['graph','power','generators','reactor','network','ae','live','upgrade','loop','netherrack','split','molten','forest','boars','tape','life','endpoints','cloud','phase','assessment'].includes(s.kind);
 let visual:React.ReactNode;
 if(story)visual=<Story s={s} t={t}/>;
 else if(['network','ae','live'].includes(s.kind))visual=<svg width="1920" height="1080">{FLOOR}<Network s={s} t={t}/></svg>;
 else if(graph)visual=<svg width="1920" height="1080">{FLOOR}<Graph s={s} t={t}/>{s.kind==='endpoints'&&<><Text x={960} y={300} size={31} color={C.violet}>已实现 · 暂无生存配方</Text><Text x={960} y={840} size={27}>三级：50 / 150 / 300 个物品每秒 · 同档 B/秒流体 · 严格配对</Text></>}</svg>;
 else visual=<svg width="1920" height="1080">{FLOOR}
 {['limits','overload'].includes(s.kind)?<Gauges s={s} t={t}/>:
 ['crystal','gas','charcoal'].includes(s.kind)?<Comparison s={s} t={t}/>:
 ['packing','parcel','dispatch','elastic','vertical'].includes(s.kind)?<Transport s={s} t={t}/>:
 ['egg','death'].includes(s.kind)?<Egg s={s} t={t}/>:
 s.kind==='territory'?<Territory t={t}/>:
 s.kind==='cards'?<>{s.nodes.map((n,i)=><Text key={n} x={425+i*245} y={295} size={30} color={C.teal}>{n}</Text>)}{Array.from({length:5},(_,r)=><g key={r}><Text x={205} y={372+r*99} size={30}>DDR{r+1}</Text>{['economy','balanced','high_speed','high_storage','defective','final'].map((type,c)=><g key={type} opacity={c===Math.floor(t)?1:.5}><rect x={375+c*245} y={320+r*99} width="100" height="90" rx="8" fill="#29404a" stroke={c===Math.floor(t)?C.copper:'#45626b'}/><image href={staticFile(`memory/card_ddr${r+1}_${type}.png`)} x={391+c*245} y={330+r*99} width="68" height="68"/></g>)}</g>)}<Text x={960} y={875} size={28}>最终卡：暂无生存配方 · 同代卡生效 · 损耗按全网累计</Text></>:
 s.kind==='repair'?<>{s.nodes.map((n,i)=><g key={i} opacity={.2+.8*fade(t,i,i+.25)}><image href={staticFile('gear_heart_damaged.png')} x={290+i*250} y={650-i*55} width="90" height="90"/><Text x={335+i*250} y={780-i*55} size={42} color={C.teal}>{n}</Text></g>)}<Text x={960} y={330} size={45}>顺序修复 · RPM逐级提升</Text></>:
 s.kind==='effects'||s.kind==='blessing'?<><image href={staticFile(s.kind==='effects'?'gear_heart_damaged.png':'gear_heart_normal.png')} x="320" y="370" width="320" height="320"/><Person x={680} y={480} t={t} scale={.5}/>{s.nodes.map((n,i)=><g key={i} opacity={.25+.75*fade(t,i*.55,i*.55+.3)}><rect x="970" y={325+i*81} width="690" height="65" rx="10" fill="#29434c" stroke={s.kind==='effects'&&i===4?C.red:C.teal}/><Text x={1315} y={369+i*81} size={34}>{n}</Text></g>)}</>:
 s.kind==='overspeed'?<><Art kind="press" x={500} y={480} t={t} scale={2}/><Art kind="material" x={820} y={580} t={t}/><Art kind="output" x={1250} y={580} t={t}/><Text x={1110} y={380} size={86} color={C.copper}>× {t<2?'1':t<4?'5':'15'}</Text><Text x={1100} y={750} size={38}>原料 ×N · 产出 ×N · 应力 ×N</Text><Text x={960} y={840} size={27}>只对适用机器生效 · 不通过超速反复推动实体</Text></>:
 s.kind==='factories'?<>{s.nodes.map((n,i)=><g key={i}><Art kind="factory" x={360+i*390} y={470} t={t*(i+1)*.3} scale={1.2}/><Text x={360+i*390} y={680} size={37}>{n}</Text></g>)}<Text x={960} y={785} size={30}>{FACTORY_TYPES.map(f=>f.name).join(' · ')}</Text><Text x={960} y={835} size={27}>同转速保留等级与并行差异 · 额定目标1024 RPM</Text></>:
 s.kind==='pressure'?<>{[0,1,2].map(i=><g key={i}><Art kind="basin" x={400+i*560} y={510} t={t} scale={1.2}/><Gear x={400+i*560} y={345} radius={42} angle={t*(i+1)*80}/><Text x={400+i*560} y={735} size={29}>{['低压：0 ＜ RPM ≤ 512','中压：512 ＜ RPM ≤ 2,048','高压：RPM ＞ 2,048'][i]}</Text><Art kind="crystal" sprite={['low_pressure_crystal','medium_pressure_crystal','high_pressure_crystal'][i]} x={400+i*560} y={615} t={t} scale={.35}/></g>)}<Text x={960} y={815} size={30}>10秒结算（压缩示意） · 600 mB → 3枚 · 余液恢复</Text><Text x={960} y={860} size={24} color={C.red}>无压缩机 / 停转 / 减压：无金属特质的无用结晶</Text></>:
 s.kind==='topology'?<><Graph s={s} t={t}/><path d={`M${420+t*150} 430 l18 40 l8 -15 l18 -2Z`} fill={C.paper}/><Text x={960} y={850} size={27}>检查限速节点 · 目标转速与实际上限分别显示</Text></>:
 s.kind==='dimensions'?<><Tiles s={s} t={t}/><Text x={960} y={855} size={28}>量子桥 / 量子纠缠 / 跨维快递 · 机器人禁止跨维旅行</Text></>:
 s.kind==='caves'?<><Tiles s={s} t={t}/><Text x={960} y={860} size={28} color={C.violet}>18项基础产物 · 渊海已有首版 · 入口锁定，其余内容部分预览</Text></>:
 s.kind==='jei'?<>{['basin','fuel','meter'].map((k,i)=><Art key={i} kind={k} x={460+i*500} y={500} t={t} scale={1.4}/>)}{s.nodes.map((n,i)=><Text key={i} x={460+i*500} y={740} size={34}>{n}</Text>)}<Text x={960} y={825} size={28}>200t 基础时长示意 · 实际速度仍受转速、升级与加工量影响</Text></>:
 <Tiles s={s} t={t}/>}</svg>;
 const cue=captions.find(c=>frame/fps*1000+s.start*1000>=c.startMs&&frame/fps*1000+s.start*1000<c.endMs);
 return <AbsoluteFill style={{background:'radial-gradient(ellipse at 55% 44%,#314e58,#101f2e 78%)',fontFamily:'PingFang SC, Microsoft YaHei, sans-serif',color:C.paper,overflow:'hidden'}}>
 <AbsoluteFill style={{opacity:fade(t,0,.2)*(1-fade(t,5.82,6))}}>{visual}
 {!story&&<><div style={{position:'absolute',left:130,top:92,fontSize:23,letterSpacing:4,color:C.muted}}>{s.chapter} / 维度工序</div><div style={{position:'absolute',left:130,top:140,fontSize:s.title.length>19?57:66,fontWeight:600,letterSpacing:2}}>{s.title}</div><div style={{position:'absolute',right:125,top:100,fontSize:22,color:s.preview?C.violet:C.teal,padding:'10px 17px',border:'1px solid #67827f',borderRadius:5}}>{s.preview?'开发预览 · 状态以说明为准':'机制示意动画'}</div></>}
 {s.kind==='crystal'&&<div style={{position:'absolute',right:160,top:225,fontSize:24,color:C.muted}}>30秒时间压缩示意</div>}
 {s.kind==='time'&&<div style={{position:'absolute',right:150,top:120,fontSize:24,color:C.paper}}>三万年时间压缩示意</div>}
 {s.kind==='assessment'&&<div style={{position:'absolute',left:135,top:235,fontSize:28,color:C.violet}}>规划中 · 七碎片具体任务分配未定</div>}
 {cue&&<div style={{position:'absolute',bottom:77,left:135,right:135,textAlign:'center',fontSize:37,lineHeight:1.45,color:'#fff1db',textShadow:'0 2px 9px #030b11',background:'#0b1926c9',padding:'12px 24px',borderRadius:8}}>{cue.text}</div>}
 </AbsoluteFill><div style={{position:'absolute',bottom:36,left:130,right:130,height:3,background:'#405b64'}}><div style={{height:3,width:`${(s.start+t)/360*100}%`,background:C.copper}}/></div><div style={{position:'absolute',bottom:13,right:130,fontSize:16,color:C.muted}}>{String(Math.floor((s.start+t)/60)).padStart(2,'0')}:{String(Math.floor((s.start+t)%60)).padStart(2,'0')} / 06:00</div>
 </AbsoluteFill>;
};
