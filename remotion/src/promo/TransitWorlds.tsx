import React from 'react';
import {useCurrentFrame} from 'remotion';
import {Model} from './MinecraftModel';
import {arrive, blend3, eased, impact, phase, Vec3} from './motion';
import {useShot} from './scene-context';
import {Appear3D, Belt, Chain, COLORS, Deployer, Frog, Gear, ItemFlow, Link, Packager,
  Player, Press, Tank, Tiles, Tree, Tube, WindGenerator, WorldLabel} from './ScenePrimitives';

const Parcel:React.FC<{position:Vec3;scale?:number;rotation?:Vec3}>=({position,scale=1,rotation=[0,0,0]})=><group position={position} scale={scale} rotation={rotation}>
  <Model name="parcel"/><Model name="parcel_rigging"/>
</group>;

const WorldGate:React.FC<{position:Vec3;color:string;block:string;label:string;index:number}>=
({position,color,block,label,index})=>{
  const frame=useCurrentFrame();
  return <Appear3D position={position} index={index}>
    <Model name={block} scale={1.15}/>
    <mesh position={[0,1.75,0]} rotation={[0,0,phase(frame,12.37,index*.21)*.02]}>
      <torusGeometry args={[.96,.035,6,48,Math.PI*1.85]}/><meshStandardMaterial color={color} emissive={color} emissiveIntensity={.6}/></mesh>
    <WorldLabel text={label} position={[0,3.15,0]} width={2.2} color={color} index={index}/>
  </Appear3D>;
};

export const LogisticsWorld:React.FC<{shotId:number}>=({shotId})=>{
  const frame=useCurrentFrame();const {frames}=useShot();
  if(shotId===39)return <>
    {['物品载荷','方块 / NBT','生物','在线玩家'].map((text,i)=><Appear3D key={text} index={i} position={[-4.5+i*3,0,0]}>
      <Packager position={[0,0,-1]}/><Parcel position={[0,.1,.85]} scale={.82}/>
      {i===0?<Model name="copper" position={[0,1.5,.85]} scale={.8}/>:i===1?<Model name="chest" position={[0,1.35,.85]} scale={.62}/>
        :i===2?<Model name="maid" position={[0,.9,.8]} scale={.6}/>:<Player position={[0,.9,.85]} scale={.63}/ >}
      <WorldLabel text={text} position={[0,3.4,0]} width={2.55} index={i}/>
    </Appear3D>)}
  </>;
  if(shotId===40){
    const begin=24;const out=frames*.82;
    let position:Vec3;
    if(frame<frames*.4)position=blend3([-5,.84,1],[0,.84,1],eased(frame,[begin,frames*.4],[0,1]));
    else if(frame<frames*.6){const p=eased(frame,[frames*.4,frames*.6],[0,1]);position=blend3([0,.84,1],[1.4,2.2,-.3],p);position[1]+=Math.sin(p*Math.PI)*.8;}
    else position=blend3([1.4,2.2,-.3],[5,2.2,-.3],eased(frame,[frames*.6,out],[0,1]));
    const packaging=arrive(frame,4);const unpack=eased(frame,[out,out+10],[0,1]);
    return <>
      <Appear3D><Packager position={[-5,0,1]}/><Belt position={[-2.5,0,1]} length={6}/></Appear3D>
      <Appear3D index={1}><group rotation={[0,Math.PI/2,0]} position={[.2,.05,1]}>
        <Frog pulseAt={Math.round(frames*.4)}/></group></Appear3D>
      <Appear3D index={2}><Chain position={[3.2,1.6,-.3]} length={4.2}/><Packager position={[5,1.5,-.3]}/></Appear3D>
      <Player position={[-5,0,1]} scale={Math.max(0,1-packaging)} rotation={-.3}/>
      <Parcel position={position} scale={.86*packaging*(1-unpack)} rotation={[0,.07,.02]}/>
      <Player position={[5,1.5,-.3]} scale={Math.max(0,arrive(frame,out))} rotation={.3}/>
      {['打包 → 水平带','青蛙接运','锁链运输 → 解包'].map((text,i)=><WorldLabel key={text} text={text}
        position={[-3.6+i*3.7,3.9,i===0?1:-.3]} width={3.7} index={i}/ >)}
    </>;
  }
  if(shotId===41)return <>
    <Tiles name="grass" width={13} depth={13}/>
    {[-6,-2,2,6].map((v,i)=><React.Fragment key={i}>
      <Tube points={[[v,.035,-6],[v,.035,6]]} radius={.018} color={COLORS.teal}/>
      <Tube points={[[-6,.035,v],[6,.035,v]]} radius={.018} color={COLORS.teal}/>
    </React.Fragment>)}
    <Appear3D><Model name="terminal" position={[0,0,0]} scale={1.5}/><Player position={[1.8,0,1.3]} walking rotation={-.3}/>
      <Model name="dimension_key" position={[-1.9,1.4,.5]} scale={1.3} rotation={[0,phase(frame,8.73),0]}/></Appear3D>
    <WorldLabel text="48 × 48 · 3 × 3区块" position={[0,4.1,0]} width={5}/>
    <WorldLabel text="钥匙进入 · P键往返" position={[0,2.9,2]} width={4.1} color={COLORS.teal} index={1}/>
  </>;
  if(shotId===42)return <>
    <Appear3D><Model name="terminal" scale={2}/><Parcel position={[-3,.2,2]} scale={.85}/></Appear3D>
    {[['grass','主世界',COLORS.teal],['netherrack','下界',COLORS.red],['end_stone','末地',COLORS.violet],['twilight_log','暮色',COLORS.copper]].map((data,i)=><WorldGate key={i}
      block={data[0]} label={data[1]} color={data[2]} position={[-4.5+i*3,0,-2.1]} index={i}/ >)}
    <Tube points={[[-3,.3,2],[0,.3,1],[0,1.6,0],[3,1.8,-2]]} radius={.035} glow/>
    <ItemFlow name="parcel" from={[-2.6,.7,1.7]} to={[0,1.3,0]} scale={.85} arc={.6}/>
  </>;
  if(shotId===43)return <>
    {['grass','netherrack','end_stone'].map((name,i)=><WorldGate key={name} block={name} label={['主世界','下界','末地'][i]}
      color={[COLORS.teal,COLORS.red,COLORS.violet][i]} position={[-3.7+i*3.7,0,-.4]} index={i}/ >)}
    <Model name="quantum_ring" position={[-1.8,.2,1.6]} scale={1.2}/>
    <Model name="quantum_port" position={[1.8,.2,1.6]} rotation={[0,Math.PI,0]} scale={1.2}/>
    <Link from={[-3.7,.5,0]} to={[0,.5,0]} color={COLORS.teal}/><Link from={[0,.5,0]} to={[3.7,.5,0]} color={COLORS.violet}/>
    <WorldLabel text="原生跨维线路 · 各有边界" position={[0,4.2,1]} width={5.1}/>
  </>;
  if(shotId===44){
    const charged=frame>=60;const p=eased(frame,[60,frames-24],[0,1]);
    const bounce=p<.55?Math.sin(p/.55*Math.PI)*3.1:p<.82?Math.sin((p-.55)/.27*Math.PI)*1.2:Math.sin((p-.82)/.18*Math.PI)*.48;
    const launch=charged?1:0;
    return <>
      <Player position={[-4,0,0]} crouch={!charged} scale={1-eased(frame,[58,70],[0,1])} rotation={-.9}/>
      <Model name="elastic_tool" position={[-3.8,1.1,-.5]} scale={1.15} rotation={[.2,-.5,-.4]}/>
      <Parcel position={[-4+p*8,.12+bounce,0]} scale={charged?1.15*Math.max(0,arrive(frame,60)):0} rotation={[p*2.1,p*.3,p*1.3]}/>
      <group position={[-4,1.05,0]} rotation={[-Math.PI/2,0,0]} scale={.7+impact(frame,60)*1.8}>
        <mesh><torusGeometry args={[.8,.045,6,48,eased(frame,[0,60],[.01,Math.PI*2])]}/>
          <meshStandardMaterial color={COLORS.copper} emissive={COLORS.copper} emissiveIntensity={.55}/></mesh>
      </group>
      <Tube points={[[-4,.04,0],[-1,.04,0],[2,.04,0],[4,.04,0]]} radius={.025}/>
      <Model name="elastic_compound" position={[4,.2,-1.6]} scale={.8}/>
    </>;
  }
  const detach=frame>frames*.78;
  const travel=eased(frame,[12,frames*.48,frames*.75],[.15,4.2,1.2]);
  return <>
    <Appear3D><Belt position={[0,3,0]} length={7} vertical/>
      <mesh position={[0,3,-.6]}><boxGeometry args={[1,6,.22]}/><meshStandardMaterial color="#405347" metalness={.4}/></mesh></Appear3D>
    <Player position={[detach?eased(frame,[frames*.78,frames-10],[.7,3]):.7,detach?eased(frame,[frames*.78,frames-10],[travel,0]):travel,.6]}
      walking={!detach} rotation={-.2}/>
    <Model name="shaft" position={[-1,0,-.8]} scale={[1,6,1]}/>
    <WorldLabel text={detach?'Shift · 主动脱离':frame<frames*.48?'贴住侧面 · 向上运输':'反转 · 立即换向'}
      position={[3,4.8,0]} width={4.1} color={COLORS.teal}/>
  </>;
};

const caveBlocks=['cave_magnetic','cave_primordial','cave_toxic','cave_abyssal','cave_forlorn','cave_candy'];
export const CAVE_NAMES=['磁场洞穴','原始洞穴','毒化洞穴','渊海陷窟','异寂空谷','糖果龋洞'];

export const TwilightWorld:React.FC<{shotId:number}>=({shotId})=>{
  const frame=useCurrentFrame();const {frames}=useShot();
  if(shotId===46)return <>
    {['elastic_tool','twilight_upgrade','terminal'].map((name,i)=><Appear3D key={name} index={i} position={[-3.8+i*3.8,.1,0]}>
      <Model name={name} scale={1.7} rotation={[0,.12,0]}/>
      <WorldLabel text={['弹力打包工具','暮色升级卡','个人发运终端'][i]} position={[0,3.3,0]} width={3.3} index={i}/>
    </Appear3D>)}<Model name="twilight_log" position={[-5.2,0,-2]} scale={1.4}/>
  </>;
  if(shotId===47){
    const combine=eased(frame,[42,frames*.65],[0,1]);
    return <>
      <Appear3D><Model name="table" scale={1.8}/><Model name="terminal" position={[4.2,0,0]} scale={1.7}/></Appear3D>
      <Model name="twilight_log" position={blend3([-3,1.2,0],[0,1.8,0],combine)} scale={.7}/>
      {[0,1,2].map(i=><Appear3D key={i} index={i}>
        <Model name="trophy" position={blend3([-3.4+i*.7,1.4,1.5],[0,1.8,0],combine)} scale={.58}/>
      </Appear3D>)}
      <ItemFlow name="twilight_upgrade" from={[.3,1.9,0]} to={[4.2,1.9,0]} start={Math.round(frames*.65)} scale={.85}/>
      <WorldLabel text="1块暮色木头 + 3个首领奖杯" position={[-1,4.1,0]} width={5.8}/>
      <WorldLabel text="无序合成 · 奖杯可重复" position={[1,3.2,1.6]} width={4.2} color={COLORS.copper} index={1}/>
    </>;
  }
  if(shotId===48)return <>
    {caveBlocks.map((name,i)=><Appear3D key={name} index={i} position={[-3.8+(i%3)*3.8,0,Math.floor(i/3)*2.6-1.3]}>
      <Model name={name} scale={1.2}/><WorldLabel text={CAVE_NAMES[i]} position={[0,2.1,0]} width={2.7} color={i===3?COLORS.teal:COLORS.copper} index={i}/>
    </Appear3D>)}
    <Model name="terminal" position={[0,2.7,-2.2]} scale={.85}/>
  </>;
  return <>
    <Appear3D><Belt length={9}/><Deployer position={[-2.5,0,0]}/><Press position={[.5,0,0]}/><Deployer position={[3,0,0]} offset={23}/></Appear3D>
    {['steeleaf_strip','ironwood_spring','small_ironwood_spring','arctic_fur','steeleaf'].map((name,i)=><Appear3D key={name} index={i}
      position={[-4+i*2,3.1,-2.2]}><Model name={name} scale={.56} rotation={[0,.12,0]}/></Appear3D>)}
    <ItemFlow name="elastic_compound" from={[-4,.84,0]} to={[1,.84,0]} start={12}/>
    <ItemFlow name="bounce_tape" from={[1,.84,0]} to={[4.4,.84,0]} start={55}/>
    <WorldLabel text={`十步装配 · ${Math.min(10,Math.floor(eased(frame,[12,frames-20],[1,11])))} / 10`}
      position={[0,3.55,1]} width={4.3} color={COLORS.copper}/>
  </>;
};

export const LifeWorld:React.FC<{shotId:number}>=({shotId})=>{
  const frame=useCurrentFrame();const {frames}=useShot();
  if(shotId===50)return <>
    <Tiles name="grass" width={13} depth={7}/>
    {[-4.7,-3.7,-2.7].flatMap(x=>[-1,0,1].map(z=><group key={`${x}-${z}`} position={[x,0,z]}>
      <Model name="farmland"/><Model name="wheat" position={[0,1,0]}/></group>))}
    <Appear3D><Model name="ae_vibration" position={[1.1,0,0]} scale={1.15}/>
      <Model name="ae_crank" position={[.45,.6,0]} rotation={[0,0,phase(frame,2.23)]}/>
      <Model name="maid" position={[-.5,0,.2]} rotation={[0,.8+Math.sin(phase(frame,4.13))*.025,0]}/></Appear3D>
    <Appear3D index={1}><Model name="table" position={[3,0,0]}/><Tank position={[4.7,0,-.5]} color="#c09365" tall/></Appear3D>
    <WorldLabel text="女仆参与 AE 手摇加工" position={[.3,3.4,0]} width={4.7}/>
    <WorldLabel text="农业 · 烹饪 · 酿造" position={[-2,4,1.8]} width={4.5} color={COLORS.copper} index={1}/>
  </>;
  if(shotId===51)return <>
    <Appear3D><Model name="magic_egg" position={[0,.4,0]} scale={2.9} rotation={[0,phase(frame,14.37),-.08]}/></Appear3D>
    {['搜索效果','滚动多选','固定 Ⅴ级','继承上次选择','确认后 10分钟'].map((text,i)=><WorldLabel key={text} text={text}
      position={[-3.5+(i%2)*7,3.8-Math.floor(i/2)*1.15,.1]} width={3.1} index={i} color={i===4?COLORS.copper:COLORS.teal}/ >)}
  </>;
  if(shotId===52){
    const deathAt=Math.round(frames*.56);const death=eased(frame,[deathAt,deathAt+10],[0,1]);
    return <>
      <Model name="magic_egg" position={[-3,.7,0]} scale={2.2} rotation={[0,.14,0]}/>
      <group position={[2.2,0,0]} rotation={[0,0,-death*Math.PI/2]} scale={1-death*.3}><Player/></group>
      {frame>=deathAt&&Array.from({length:24},(_,i)=>{
        const local=frame-deathAt;const spread=eased(local,[0,7,38],[0,1.15,1.8]);
        const size=arrive(local-i%4*4)*eased(local,[28,40],[.055,0]);
        return <mesh key={i} position={[2.2+Math.cos(i*2.4)*spread,1.7+Math.sin(i*1.7)*spread,Math.sin(i*2.4)*spread]}
          scale={Math.max(0,size)} rotation={[i,frame*.07,0]}><boxGeometry/>
          <meshBasicMaterial color={[COLORS.copper,COLORS.teal,COLORS.red,COLORS.violet][i%4]}/></mesh>;
      })}
      <WorldLabel text={frame<deathAt?'确认后 10分钟倒计时':'到期死亡 · 再食立即死亡'} position={[0,4.2,0]} width={5.9} color={COLORS.red}/>
    </>;
  }
  return <>
    {['table','grass','chest','parcel','cog','heart_normal'].map((name,i)=><Appear3D key={name} index={i}
      position={[-3.8+(i%3)*3.8,.1,Math.floor(i/3)*2.7-1.35]}>
      <Model name={name} scale={1.05} rotation={[0,i*.12,0]}/>
      <WorldLabel text={['建筑工具','观察 / 返回','领地与存储','封装搬运','运行修复','汉化与更新'][i]}
        position={[0,2,0]} width={2.7} index={i}/>
    </Appear3D>)}
  </>;
};

export const FutureWorld:React.FC<{shotId:number}>=({shotId})=>{
  const frame=useCurrentFrame();
  if(shotId===54)return <>
    {[1,2,3].map((tier,i)=><Appear3D key={tier} index={i} position={[-3.7+i*3.7,0,0]}>
      <Model name={`endpoint${tier}`} scale={1.45}/>
      <Model name={`endpoint${tier}`} position={[0,0,-2.2]} scale={1.05}/>
      <Tube points={[[0,.3,0],[0,.3,-1.1],[0,.3,-2.2]]} color={i===0?COLORS.teal:COLORS.violet}/>
      <WorldLabel text={`${[50,150,300][i]} 物品/秒 · 同档 B/秒`} position={[0,3.3,0]} width={3.5} index={i}/>
      <WorldLabel text={i===0?'一级 · 已有生存配方':'高阶 · 暂无生存配方'} position={[0,2.7,0]} width={3.4}
        color={i===0?COLORS.teal:COLORS.violet} index={i+1}/>
    </Appear3D>)}<ItemFlow name="copper" from={[-4.7,.9,1.4]} to={[4.7,.9,1.4]}/>
  </>;
  if(shotId===55)return <>
    <Appear3D><Model name="endpoint1" position={[-4,0,0]} scale={1.5}/></Appear3D>
    <Appear3D index={1}><Model name="matter_ball" position={[0,1.6,0]} scale={2} rotation={[0,phase(frame,9.37),0]}/>
      <Gear large position={[0,2,-2]} scale={2.8} speed={7.73}/></Appear3D>
    <Appear3D index={2}><Model name="unpacker" position={[4,0,0]} scale={1.55}/><Belt position={[4.5,0,1.5]} length={3}/></Appear3D>
    <Link from={[-4,.9,0]} to={[0,1.6,0]} color={COLORS.violet} raised/>
    <Link from={[0,1.6,0]} to={[4,.9,0]} color={COLORS.teal} raised/>
    {['频道库存','持久云盘 · F12领取','物质球解包'].map((text,i)=><WorldLabel key={text} text={text} position={[-4+i*4,3.9,0]}
      width={3.8} index={i}/ >)}
    <ItemFlow name="matter_ball" from={[.5,1.5,0]} to={[4,1.1,0]} start={25} scale={.6}/>
  </>;
  if(shotId===56)return <>
    {caveBlocks.map((name,i)=><Appear3D key={name} index={i} position={[-3.8+(i%3)*3.8,0,Math.floor(i/3)*2.4-1.2]}>
      <Model name={name} scale={1.2}/>
      {i===3&&<Model name="abyssal_tower" position={[0,1.2,0]} scale={.82}/ >}
      <WorldLabel text={CAVE_NAMES[i]+(i===3?' · 首版':' · 在建')} position={[0,i===3?3.1:2.05,0]} width={3.2}
        color={i===3?COLORS.teal:COLORS.violet} index={i}/>
    </Appear3D>)}
  </>;
  if(shotId===57)return <>
    <Appear3D position={[-2.7,0,0]}><Model name="abyssal_tower" scale={1.65}/>
      <Model name="factory_port" position={[-1.2,0,0]} scale={1.1}/>
      <WorldLabel text="洞穴工厂 · 相位 / 模块" position={[0,2.8,0]} width={4.2}/></Appear3D>
    <Appear3D index={1}><Model name="stabilizer" position={[2.5,0,0]} scale={1.45}/></Appear3D>
    <ItemFlow name="ampoule" from={[-1.6,1.1,0]} to={[2.5,1.1,0]} scale={.8}/>
    <WorldLabel text="250 mB 安瓿 · 离维 60秒" position={[2.6,3,0]} width={4.3} color={COLORS.violet} index={1}/>
    <WorldLabel text="4比1稳定化 · 持续生产" position={[2.6,2.3,0]} width={4.2} color={COLORS.teal} index={2}/>
  </>;
  return <>
    <Appear3D><Belt length={8}/><Press position={[-2.4,0,0]}/><Deployer position={[.4,0,0]}/>
      <Model name="combiner" position={[3.9,0,0]} rotation={[0,Math.PI,0]}/><Tank position={[0,0,-2.1]} tall/></Appear3D>
    <ItemFlow from={[-3.5,.84,0]} to={[3.5,.84,0]}/>
    <Link from={[0,.6,-2.1]} to={[0,.6,0]} color={COLORS.teal}/>
    <WorldLabel text="组织产线 · 诊断瓶颈 · 持续运行" position={[0,4.1,0]} width={6.5}/>
  </>;
};

export const EndingWorld:React.FC=()=>{
  const frame=useCurrentFrame();
  return <>
    <Tiles name="grass" width={19} depth={11}/>
    <Tree position={[-6.3,0,-2.5]} scale={.83}/><Tree position={[6.5,0,-3.1]} scale={.87} index={1}/>
    <WindGenerator position={[-3.5,0,-2.2]} scale={.73}/>
    <Model name="factory_ultimate" position={[2,0,-2]} scale={1.4} rotation={[0,Math.PI,0]}/>
    <Tank position={[4.1,0,-1.8]} color={COLORS.copper} tall/>
    <Press position={[.4,0,-1.5]}/><Belt position={[.4,0,-1.5]} length={5}/>
    <Player position={[-1.7,0,1.5]} rotation={-.45}/>
    <Model name="heart_blessed" position={[.1,1.5,1.8]} scale={.66} rotation={[0,phase(frame,12.37),0]}/>
    <Tube points={[[-5,.05,2],[-2.2,.05,2],[.1,.05,2],[3.5,.05,2],[4.1,.05,-1.8]]} glow radius={.03}/>
    <ItemFlow name="copper" from={[-2,.84,-1.5]} to={[2.7,.84,-1.5]}/>
  </>;
};
