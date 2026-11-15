import React from 'react';
import {useCurrentFrame} from 'remotion';
import {Model} from './MinecraftModel';
import {arrive, blend3, eased, impact, phase, Vec3} from './motion';
import {useShot} from './scene-context';
import {Appear3D, Belt, COLORS, Deployer, Gear, ItemFlow, Link, Packager, Player, Press,
  Shaft, Tank, Tiles, Tree, Tube, WindGenerator, WorldLabel} from './ScenePrimitives';

export const StoryWorld: React.FC<{shotId:number}> = ({shotId}) => {
  const frame=useCurrentFrame(); const {frames}=useShot();
  if(shotId===0&&frame>=28&&frame<84){
    const cut=frame<56?1:2;
    const local=frame-(cut===1?28:56);
    const reveal=arrive(local);
    return <>
      <Tiles width={9} depth={5}/>
      <group position={[0,1.6,0]} scale={1+.08*impact(local,3)}>
        <Model name="heart_damaged" position={[0,0,1.1]} scale={2.2} rotation={[.12,.05+Math.sin(phase(frame,15.73))*.13,-.08]}/>
        {cut===1?<group rotation={[Math.PI/2,0,0]} scale={.75+.25*reveal}>
          <mesh><torusGeometry args={[2.15,.09,6,64]}/><meshStandardMaterial color={COLORS.copper} metalness={.65}/></mesh>
        </group>:<Gear position={[0,1,-2.2]} large scale={3.1} speed={7.13}/ >}
      </group>
      <WorldLabel text={cut===1?'断裂 · 闭合':'闭合 · 齿轮'} position={[0,4.8,0]} width={4}/>
    </>;
  }
  const falling=shotId===2;
  const growth=shotId===3?eased(frame,[0,frames-20],[.15,1]):1;
  const fall=eased(frame,[0,45],[4.8,.2]);
  return <>
    <Tiles name="dirt" width={19} depth={11} y={-2}/><Tiles name="grass" width={19} depth={11}/>
    {[[-5,0,-2],[4.6,0,-3],[1.2,0,-4.8]].map((p,i)=><Tree key={i} position={p as Vec3}
      scale={shotId===3?Math.max(.12,growth-i*.06):.84+i*.09} index={i}/ >)}
    <Model name="cobblestone" position={[-2,-.14,1]} scale={[1.3,.55,1]}/>
    <Model name="cobblestone" position={[1.2,-.08,-.9]} scale={[1.1,.7,1.1]}/>
    <Player position={[shotId===4?eased(frame,[0,frames-30],[-3,-.3]):-2.5,0,1.4]}
      rotation={-.8} walking={shotId===4}/>
    {shotId===1?<group position={[0,1.1,.7]}>
      <Model name={frame<65?'heart_normal':'heart_damaged'} scale={2.9} rotation={[.08,phase(frame,12.37),-.06]}/>
      <Gear position={[0,1,-2.4]} large scale={3} speed={8.19}/>
    </group>:<Model name="heart_damaged" scale={falling?.8:shotId===4?.53:.62}
      position={[falling?-.2:0,falling?fall:shotId===4?1.25:.15,falling?.6:.3]}
      rotation={falling?[eased(frame,[0,45],[0,2.9]),frame*.045,.3]:[-Math.PI/2,0,.15]}/ >}
    {falling&&frame>=42&&Array.from({length:12},(_,i)=>{
      const local=frame-42-i%3*4; const p=eased(local,[0,34],[0,1]);
      return local<0?null:<mesh key={i} position={[Math.cos(i*2.17)*p*2,.05+Math.sin(p*Math.PI)*.5,.6+Math.sin(i*2.17)*p*2]}
        scale={.075*(1-p)+.012} rotation={[i,frame*.09,i*.7]}><boxGeometry/>
        <meshStandardMaterial color={i%2?COLORS.copper:'#779274'}/></mesh>;
    })}
    {shotId===0&&(frame<28?<Tube points={[[-6,4.6,-2],[-2.8,4.5,-.5],[-.4,4.1,.1],[3,3.6,-1]]} radius={.075} glow/>
      :<group scale={eased(frame,[84,89],[.65,1])} position={[0,eased(frame,[84,89],[.15,0]),0]}>
        <Tube points={[[-6,.08,2],[-2,.08,2],[0,.08,.3],[3,.08,2]]} radius={.035} glow/>
        <Tube points={[[0,.08,.3],[1.8,.08,-1.9],[4,.08,-2.4]]} radius={.035} glow/>
      </group>)}
    {shotId===3&&[0,1,2].map(i=><Appear3D key={i} at={35+i*53} index={i}
      position={[-4+i*3.5,0,2]}><Model name="wheat" scale={1.1}/></Appear3D>)}
  </>;
};

export const HeartWorld:React.FC<{shotId:number}>=({shotId})=>{
  const frame=useCurrentFrame();const {frames}=useShot();
  if(shotId===6)return <>
    <Appear3D><Model name="motor" position={[-3.8,0,0]}/><Shaft x={-2} length={3}/><Model name="combiner" position={[0,0,0]} rotation={[0,Math.PI,0]}/></Appear3D>
    <Appear3D index={1}><Belt position={[3.6,0,0]} length={3}/><Press position={[3.6,0,0]}/></Appear3D>
    <WorldLabel text="32 RPM" position={[-3,3,0]} width={3}/>
    <WorldLabel text="4,096 SU · 全部活跃网络合计" position={[2.5,3.5,0]} width={4.1} index={1}/>
    <Model name="heart_damaged" position={[-.3,2.2,-1.8]} scale={1.2} rotation={[0,phase(frame,8.17),0]}/>
  </>;
  if(shotId===7){
    const labels=['32 RPM','4,096 SU','矿物 8次/秒','AE 并发 1项','跨工序 16件/秒','设备各 8台','指定产物 16件/秒'];
    const icons=['cog','motor','iron','ae_assembler','parcel','combiner','copper'];
    return <>{labels.map((label,i)=>{
      const x=i<4?-4.5+i*3:-3+(i-4)*3;
      const z=i<4?-1.45:1.55;
      return <Appear3D key={label} index={i} position={[x,0,z]}>
        <Model name={icons[i]} scale={i===2||i===6?1.1:1} rotation={[0,i*.21,0]}/>
        <WorldLabel text={label} position={[0,1.6,0]} width={2.7} index={i}/>
      </Appear3D>;
    })}</>;
  }
  if(shotId===8){
    const active=Math.min(6,Math.floor(eased(frame,[20,frames-20],[0,7])));
    return <>
      {Array.from({length:7},(_,i)=><Appear3D key={i} index={i} position={[-4.65+i*1.55,.2,0]}>
        <Model name={`fragment${i+1}`} scale={.88} rotation={[0,-.12,.04]}/>
        <mesh position={[0,-.1,0]}><boxGeometry args={[1.25,.16,1]}/><meshStandardMaterial color={i<=active?COLORS.copper:'#354b47'} metalness={.4}/></mesh>
        <WorldLabel text={['512','1,024','2,048','4,096','5,120','8,192','10,240'][i]+' RPM'} position={[0,1.7,0]} width={1.7} index={i}/>
      </Appear3D>)}
      <Model name="heart_normal" position={[0,2.7,-1.8]} scale={1.7} rotation={[0,phase(frame,11.17),0]}/>
      <Tube points={[[-4.8,.1,1],[0,.1,1],[4.8,.1,1]]} glow/>
    </>;
  }
  if(shotId===10)return <>
    <Appear3D><Belt length={7}/><Press position={[-2,0,0]}/><Deployer position={[1,0,0]}/><Tank position={[4,0,-1]} tall/></Appear3D>
    <ItemFlow from={[-3,.84,0]} to={[3,.84,0]}/>
    <WorldLabel text="布局、缓冲、动力 · 自己组织" position={[0,3.8,-1]} width={5.5}/>
  </>;
  const full=shotId===9;
  const labels=full?['触及 +4','急迫 Ⅲ','夜视 Ⅰ','伤害 ×1.50','10,240 RPM','心脏额度解除']
    :['永久佩戴','触及 +2','急迫 Ⅱ','夜视 Ⅰ','伤害 ×0.90'];
  return <>
    <Appear3D><Model name={full?'heart_blessed':'heart_damaged'} position={[0,.65,.8]} scale={2.6}
      rotation={[0,.1+Math.sin(phase(frame,14.17))*.17,-.05]}/><Gear large position={[0,1.5,-2]} scale={2.8} speed={9.73}/></Appear3D>
    {labels.map((text,i)=>{
      const left=i%2===0;const y=3.9-Math.floor(i/2)*1.15;
      return <WorldLabel key={text} text={text} position={[left?-3.3:3.3,y,0]} width={2.75}
        color={text.includes('0.90')?COLORS.red:full?COLORS.copper:COLORS.teal} index={i}/ >;
    })}
  </>;
};

const Turbine=()=>{
  const frame=useCurrentFrame();
  return <group scale={.65}>
    {[0,1,2,3,4].flatMap(y=>[-1,1].flatMap(x=>[-1,1].map(z=><Model key={`${x}-${y}-${z}`} name="turbine_casing" position={[x,y,z]}/ >)))}
    {[1,2,3].map(y=><Model key={y} name="turbine_rotor" position={[0,y,0]} rotation={[0,phase(frame,1.93,y*.09),0]}/ >)}
    <Model name="turbine_vent" position={[0,4,1]}/>
  </group>;
};

export const PowerWorld:React.FC<{shotId:number}>=({shotId})=>{
  const frame=useCurrentFrame();const {frames}=useShot();
  if(shotId===11)return <>
    <Appear3D><Model name="motor" position={[-4.5,0,0]}/></Appear3D>
    <Appear3D index={1}><Shaft x={-1.9} length={4}/><Gear position={[-.5,.5,0]}/></Appear3D>
    <Appear3D index={2}><Model name="combiner" position={[1.8,0,0]} rotation={[0,Math.PI,0]} scale={1.5}/></Appear3D>
    <Appear3D index={3}><Belt position={[4.1,0,0]} length={3}/></Appear3D>
    <ItemFlow name="iron" from={[1.8,1.1,0]} to={[5,.84,0]} scale={.7}/>
    {['机械源','传动轴','Mek 加工','真实产出'].map((t,i)=><WorldLabel key={t} text={t} position={[-4.5+i*3.1,2.65,0]} index={i}/ >)}
  </>;
  if(shotId===12)return <>
    <Appear3D><WindGenerator position={[-3.9,0,-.1]} scale={.9}/></Appear3D>
    <Appear3D index={1}><Model name="solar_generator" position={[0,0,0]} scale={1.5}/></Appear3D>
    <Appear3D index={2}><Model name="gas_generator" position={[3.5,0,0]} scale={1.5} rotation={[0,Math.PI,0]}/><Tank position={[4.9,0,-1.3]} color={COLORS.teal}/></Appear3D>
    <Shaft x={0} y={.5} z={1.4} length={10}/>
    {['风力','日照','燃料条件保留'].map((t,i)=><WorldLabel key={t} text={t} position={[-3.9+i*3.7,3.6,1]} index={i}/ >)}
  </>;
  if(shotId===13)return <>
    <Appear3D position={[-4.5,0,0]}><Turbine/><WorldLabel text="涡轮 · 剖面" position={[0,3.9,0]} width={2.5}/></Appear3D>
    <Appear3D index={1} position={[0,2,0]}>
      <Model name="reactor_core" scale={1.25} rotation={[0,phase(frame,12.73),0]}/>
      <mesh scale={1.65}><sphereGeometry args={[1,20,12]}/><meshBasicMaterial color={COLORS.teal} wireframe transparent opacity={.14}/></mesh>
      {[-1,1].map((sign,i)=><Model key={i} name="reactor_stabilizer" position={[sign*2,-.2,0]} rotation={[0,sign*Math.PI/2,0]}/ >)}
      <Model name="reactor_injector" position={[0,-1.4,0]} rotation={[Math.PI/2,0,0]}/>
      <WorldLabel text="龙堆 · 注入与护盾" position={[0,2.35,0]} width={3.3} index={1}/>
    </Appear3D>
    <Appear3D index={2} position={[4.5,0,0]}>
      {[-.7,.7].flatMap(x=>[-.7,.7].flatMap(z=>[0,1,2].map(y=><Model key={`${x}-${y}-${z}`} name="fusion_frame" position={[x,y,z]} scale={.7}/ >)))}
      <Model name="fusion_controller" position={[0,2.1,0]}/><WorldLabel text="聚变输出端" position={[0,3.8,0]} width={2.5} index={2}/>
    </Appear3D>
    <Link from={[-4,.5,0]} to={[0,.5,0]} color={COLORS.teal}/><Link from={[0,.5,0]} to={[4,.5,0]}/>
  </>;
  if(shotId===14){
    const fast=frame>frames*.35;const n=frame<frames*.35?1:frame<frames*.68?5:15;
    return <><Appear3D><Belt length={9}/><Press fast={fast}/></Appear3D>
      <ItemFlow name="iron" from={[-4,.84,0]} to={[4,.84,0]} interval={fast?23:53}/>
      <ItemFlow name="copper_sheet" from={[-3.7,.88,.16]} to={[4.3,.88,.16]} interval={fast?29:67} start={20}/>
      <WorldLabel text={`并行 ×${n}`} position={[-3.4,3,0]} width={3.1} color={COLORS.copper}/>
      <WorldLabel text={`应力成本 ×${n}`} position={[3.4,3,0]} width={3.2} color={COLORS.teal} index={1}/>
    </>;
  }
  if(shotId===15)return <>
    {['basic','advanced','elite','ultimate'].map((tier,i)=><Appear3D key={tier} index={i} position={[-4.5+i*3,0,0]}>
      <Model name={`factory_${tier}`} rotation={[0,Math.PI,0]} scale={1.5}/>
      <WorldLabel text={['基础 · 3槽','高级 · 5槽','精英 · 7槽','终极 · 9槽'][i]} position={[0,2.8,0]} index={i}/>
      <Gear position={[0,.55,1.15]} speed={3.17-i*.41} offset={i*.13}/>
    </Appear3D>)}<Shaft x={0} z={-1.4} length={11}/>
  </>;
  if(shotId===16){
    const pour=eased(frame%73,[0,20,24,38,43,73],[0,0,1,1,0,0]);
    return <><Appear3D><Belt length={9}/><Model name="spout" position={[-2,2.1,0]}/><Deployer position={[.5,0,0]}/><Press position={[3,0,0]}/><Tank position={[-4.2,0,-1.8]} color="#ffcf69" tall/></Appear3D>
      <Tube points={[[-4.2,2.7,-1.8],[-2,2.7,-1.8],[-2,2.7,0]]}/>
      <mesh position={[-2,1.45,0]} scale={[1,pour,1]}><boxGeometry args={[.09,1.1,.09]}/><meshBasicMaterial color="#ffcf69"/></mesh>
      <ItemFlow name="copper_sheet" from={[-4,.84,0]} to={[-.8,.84,0]}/>
      <ItemFlow name="energy_tablet" from={[3,.84,0]} to={[4.4,.84,0]} start={52}/>
      {['redstone','alloy','processor'].map((name,i)=><Appear3D key={name} index={i+1} position={[.4+i*.78,3.5,-1.2]}><Model name={name} scale={.58}/></Appear3D>)}
      <WorldLabel text="注液 → 装配 → 压制" position={[.5,4,1.3]} width={5.3}/>
    </>;
  }
  const limited=frame>50&&frame<117;
  return <>
    <Appear3D><Model name="motor" position={[-4.7,0,0]}/><Shaft x={-2.5} length={4}/><Model name="speed_controller" position={[-.4,0,0]}/></Appear3D>
    <Appear3D index={1}><Model name="combiner" position={[2,0,-1.1]} rotation={[0,Math.PI,0]}/><Belt position={[3.5,0,1.1]} length={3}/><Press position={[3.5,0,1.1]} running={!limited}/></Appear3D>
    <Link from={[0,.7,0]} to={[2,.7,-1.1]} color={limited?COLORS.red:COLORS.teal}/>
    <Link from={[0,.7,0]} to={[3.5,.7,1.1]}/>
    <WorldLabel text={limited?'定位限速节点':'调整后 · 恢复运行'} position={[0,3.9,0]} width={4.5} color={limited?COLORS.red:COLORS.teal}/>
  </>;
};

const Cable:React.FC<{running?:boolean}>=({running=true})=><>
  {Array.from({length:8},(_,i)=><Model key={i} name="ae_cable" position={[-2.6+i*.75,.5,0]} rotation={[0,Math.PI/2,0]}/ >)}
  <Link from={[-3,.98,0]} to={[3,.98,0]} color={running?COLORS.teal:COLORS.red}/>
</>;

export const MemoryWorld:React.FC<{shotId:number}>=({shotId})=>{
  const frame=useCurrentFrame();const {frames}=useShot();
  if(shotId===21)return <>
    {Array.from({length:5},(_,i)=><Appear3D key={i} index={i} position={[-4.6+i*2.3,0,0]}>
      <Model name={`ddr${i+1}`} scale={1.45} rotation={[0,-.08,0]}/>
      <WorldLabel text={`DDR${i+1} · ${[4,8,16,32,64][i]}槽`} position={[0,2.7,0]} width={2.3} index={i}/>
      <WorldLabel text={`${[256,512,1024,2048,4096][i]} RPM`} position={[0,2.2,0]} width={2} color={COLORS.teal} index={i+1}/>
    </Appear3D>)}<Cable/>
  </>;
  if(shotId===22){
    const types=['economy','balanced','high_speed','low_loss','defective','final'];
    return <>
      {Array.from({length:5},(_,r)=>types.map((type,c)=><Appear3D key={`${r}-${c}`} index={r*6+c}
        position={[-4.7+c*1.88,.2+(4-r)*.78,0]}>
        <Model name={`card_${r+1}_${type}`} scale={.68} rotation={[-.08,.04,0]}/>
      </Appear3D>))}
      {['经济 0.5×','均衡 1×','高速 2×','低损耗 0.5×','劣质 0.25×','最终 4×'].map((text,i)=><WorldLabel key={text} text={text} position={[-4.7+i*1.88,4.45,0]} width={1.8}
        color={i===4?COLORS.red:i===5?COLORS.violet:COLORS.teal} index={i}/ >)}
      {Array.from({length:5},(_,i)=><WorldLabel key={i} text={`DDR${i+1}`} position={[-5.9,3.55-i*.78,.1]} width={1.4} index={i}/ >)}
    </>;
  }
  if(shotId===23){
    const merge=eased(frame,[38,105],[0,1]);
    return <>
      <Appear3D><Model name="ae_assembler" position={[0,0,0]} scale={1.65}/><Model name="ddr3" position={[4,0,0]} scale={1.45}/></Appear3D>
      {[0,1,2,3].map(i=><Appear3D key={i} index={i}>
        <Model name="card_2_high_speed" position={blend3([-4.6+i*.55,1.2,-.2+i*.3],[0,1.2,0],merge)} scale={.63} rotation={[0,.08,.1]}/>
      </Appear3D>)}
      <ItemFlow name="card_3_high_speed" from={[.8,1.1,0]} to={[4,1.1,0]} start={100} scale={.75}/>
      <WorldLabel text="4张同型旧卡 → 4张新卡" position={[0,3.4,0]} width={5.1}/>
      <Model name="card_2_defective" position={[-4,.1,1.6]} scale={.58}/>
      <WorldLabel text="手工 25% 劣质 · AE 合成豁免" position={[-2.5,2.35,1.6]} width={4.2} color={COLORS.red} index={1}/>
    </>;
  }
  const running=shotId===20?!(frame>frames*.34&&frame<frames*.7)
    :shotId===24?!(frame>frames*.26&&frame<frames*.72):true;
  const mechanical=shotId===19;
  const overload=shotId===24;
  return <>
    <Appear3D><Model name="motor" position={[-4.8,0,0]}/><Shaft x={-3.8} length={2} running={running}/></Appear3D>
    <Appear3D index={1}><Model name="me_gearbox" position={[-2.75,0,0]}/></Appear3D>
    <Appear3D index={2}><Model name={running?'controller':'controller_off'} position={[0,0,0]} scale={1.25}/><Cable running={running}/></Appear3D>
    <Appear3D index={3}><Model name="me_gearbox" position={[2.75,0,0]}/><Shaft x={3.75} length={2} running={running}/></Appear3D>
    <Appear3D index={4}><Belt position={[5,0,0]} length={2}/><Press position={[5,0,0]} running={running}/></Appear3D>
    {['真实输入','IMPORT','ME 网络','EXPORT','远端加工'].map((t,i)=><WorldLabel key={t} text={t} position={[-4.8+i*2.4,3.35,i===4?.6:0]} width={1.95} index={i}/ >)}
    {mechanical&&<>
      <Appear3D at={28} position={[-1.8,0,-2.2]}><Model name="ae_acceptor"/>
        <WorldLabel text="动能接收器" position={[0,1.65,0]} width={2.4}/></Appear3D>
      <Appear3D at={32} position={[1.25,0,-2.2]}><Model name="ae_vibration"/>
        <WorldLabel text="谐振仓" position={[0,1.65,0]} width={1.8}/></Appear3D>
      <Link from={[-1.8,.7,-2.2]} to={[0,.7,0]} color={COLORS.violet}/>
      <Link from={[1.25,.7,-2.2]} to={[0,.7,0]} color={COLORS.violet}/>
    </>}
    {(shotId===20||overload)&&<WorldLabel text={running?'输入足额 · 输出运行':overload?'过载 · 全部输出停机':'输入归零 · 立即停止'}
      position={[0,2.4,1.8]} width={5} color={running?COLORS.teal:COLORS.red} index={1}/ >}
    {overload&&[0,1].map(row=>{
      const v=running?.64:row===0&&frame<frames*.5?1.1:row===1&&frame>=frames*.5?1.12:.7;
      return <group key={row} position={[-2.7,.18,2.5+row*.4]}>
        <mesh position={[2.5,0,0]}><boxGeometry args={[5,.1,.24]}/><meshStandardMaterial color="#283c38"/></mesh>
        <mesh position={[Math.min(v,1)*2.5,.08,0]}><boxGeometry args={[Math.min(v,1)*5,.12,.19]}/>
          <meshStandardMaterial color={v>1?COLORS.red:COLORS.teal}/></mesh>
        <mesh position={[4.7,.14,0]}><boxGeometry args={[.03,.2,.31]}/><meshBasicMaterial color={COLORS.copper}/></mesh>
      </group>;
    })}
    <ItemFlow from={[4.5,.84,0]} to={[5.9,.84,0]} running={running} start={36}/>
  </>;
};
