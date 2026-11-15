import React from 'react';
import {useCurrentFrame} from 'remotion';
import {Model} from './MinecraftModel';
import {arrive, eased, phase, Vec3} from './motion';
import {useShot} from './scene-context';
import {Appear3D, Belt, COLORS, Deployer, Gas, Gear, ItemFlow, Link, Liquid, Millstone,
  Mixer, Press, Shaft, Tank, Tube, WorldLabel} from './ScenePrimitives';

const ReturnLoop:React.FC<{name?:string; color?:string}>=({name='raw_iron',color=COLORS.copper})=><>
  <Tube points={[[4,.4,0],[4,.4,-2.2],[0,.4,-2.5],[-4,.4,-2.2],[-4,.4,0]]} color={color}/>
  <ItemFlow name={name} from={[3.7,.52,-2.2]} to={[-3.7,.52,-2.2]} start={50} interval={67} scale={.5}/>
</>;

export const MaterialsWorld:React.FC<{shotId:number}>=({shotId})=>{
  const frame=useCurrentFrame();const {frames}=useShot();
  if(shotId===25)return <>
    <Appear3D position={[-4,0,0]}><Millstone/><WorldLabel text="研磨 · 铁粉" position={[0,2.1,0]} width={2.5}/></Appear3D>
    <Appear3D index={1}><Belt position={[-1,0,0]} length={5}/><Press position={[-1,0,0]}/></Appear3D>
    <Appear3D index={2} position={[3.3,0,0]}><Model name="combiner" rotation={[0,Math.PI,0]} scale={1.4}/>
      <WorldLabel text="结合 · 回到粗铁" position={[0,2.6,0]} width={3}/></Appear3D>
    <ItemFlow name="iron_dust" from={[-3.4,.84,0]} to={[2.7,.84,0]} scale={.65}/>
    <ReturnLoop/><ItemFlow name="raw_iron" from={[3.8,.85,0]} to={[5.6,.85,1.7]} start={75}/>
    <WorldLabel text="回流优先 · 余量外供" position={[0,4.1,-1.5]} width={4.5} color={COLORS.copper}/>
  </>;
  if(shotId===26){
    const items=['zinc','gold','lapis','quartz','amethyst','tin','lead','osmium','fluorite'];
    const names=['锌','金','青金石','石英','紫水晶','锡','铅','锇','萤石'];
    return <>{items.map((name,i)=><Appear3D key={name} index={i} position={[-3.8+(i%3)*3.8,.2,Math.floor(i/3)*1.85-1.85]}>
      <Model name={name} scale={.9} rotation={[0,.25+Math.sin(phase(frame,6.37,i*.27))*.12,0]}/>
      <WorldLabel text={names[i]} position={[0,1.55,0]} width={1.5} index={i}/>
      <mesh position={[0,-.12,0]}><cylinderGeometry args={[.82,.92,.2,6]}/><meshStandardMaterial color="#324b44" metalness={.3}/></mesh>
    </Appear3D>)}</>;
  }
  if(shotId===27)return <>
    <Appear3D position={[-3.5,0,0]}><Mixer color="#7fa1b1" heat="normal"/>
      <WorldLabel text="氧化铜 + CO" position={[0,4.1,0]} width={2.8}/></Appear3D>
    <Appear3D index={1} position={[0,0,0]}><Model name="crusher" rotation={[0,Math.PI,0]} scale={1.25}/>
      <WorldLabel text="还原、粉碎" position={[0,2.6,0]} width={2.4} index={1}/></Appear3D>
    <Appear3D index={2} position={[3.5,0,0]}><Model name="purifier" rotation={[0,Math.PI,0]} scale={1.25}/>
      <WorldLabel text="耗氧提纯 · 回流" position={[0,2.6,0]} width={3} index={2}/></Appear3D>
    <Belt length={7}/><ItemFlow name="copper_dust" from={[-3.2,.84,0]} to={[3.2,.84,0]}/>
    <Tank position={[1.6,0,-1.7]} color={COLORS.teal}/><Gas position={[1.6,0,-1.7]}/>
    <ReturnLoop name="copper" color={COLORS.teal}/>
  </>;
  if(shotId===28)return <>
    <Appear3D position={[-3.9,0,0]}>
      <Model name="netherrack" position={[-1.3,1.2,0]} scale={.7}/>
      {[-1,1].map(sign=><group key={sign} position={[sign*.84,1.6,0]} rotation={[Math.PI/2,0,0]} scale={.8}>
        <group rotation={[0,phase(frame,sign*2.17,.1),0]}><Model name="crushing_wheel" position={[0,-.5,0]}/></group>
      </group>)}
    </Appear3D>
    <Appear3D index={1}><Belt position={[.2,0,0]} length={6}/><Press position={[.3,0,0]}/></Appear3D>
    <Appear3D index={2}><Model name="chest" position={[4.1,0,.4]} scale={1.2}/></Appear3D>
    <ItemFlow name="cinder_flour" from={[-3,.84,0]} to={[4.1,.84,0]} interval={43}/>
    <ReturnLoop name="netherrack"/>
    <WorldLabel text="平均 4.48份 · 概率产出" position={[-2.8,3.8,0]} width={3.8}/>
    <WorldLabel text="4份回流 · 余量先入缓冲" position={[2.8,3.8,0]} width={3.8} color={COLORS.teal} index={1}/>
  </>;
  if(shotId===29)return <>
    <Appear3D position={[-3,0,0]}><Belt length={4}/><Model name="spout" position={[0,2.1,0]}/>
      <Tank position={[-1.7,0,-1.8]} color="#684638" tall/>
      <WorldLabel text="面粉 + 250 mB 原油 → 红石" position={[0,3.8,0]} width={4.7}/>
      <ItemFlow name="redstone" from={[-1.4,.84,0]} to={[1.4,.84,0]} start={24}/></Appear3D>
    <Appear3D index={1} position={[3,0,0]}><Mixer heat="normal" color="#d4b25b"/>
      <Millstone position={[1.5,0,1]}/><WorldLabel text="普通加热 → 研磨 → 硫" position={[0,4.1,0]} width={4.2} index={1}/>
      <ItemFlow name="sulfur" from={[.5,1,0]} to={[1.5,1,1]} start={31}/></Appear3D>
    <Model name="cinder_flour" position={[0,1.3,1.4]} scale={1.05} rotation={[0,.2,0]}/>
    <Link from={[0,.8,1.4]} to={[-3,.8,0]}/><Link from={[0,.8,1.4]} to={[3,.8,0]}/>
  </>;
  if(shotId===30)return <>
    {['normal','normal','super'].map((heat,i)=><Appear3D key={i} index={i} position={[-3.5+i*3.5,0,0]}>
      <Model name="burner" scale={1.55}/><Model name={heat==='super'?'blaze_super':'blaze'} position={[0,.2,0]} scale={1.55}/>
      <Tank position={[0,0,-1.9]} color={i===2?COLORS.teal:'#cd995e'}/>
      <WorldLabel text={['柴油 / 乙醇','生物柴油','汽油 · 超级加热'][i]} position={[0,2.65,0]} width={3.25} index={i}/>
      <Gas color={i===2?COLORS.teal:COLORS.copper} position={[0,.4,0]}/>
    </Appear3D>)}
  </>;
  return <>
    <Appear3D position={[-4.1,0,0]}><Mixer heat="normal" color="#fac47a"/>
      <WorldLabel text="1 锭 = 1 B" position={[0,4,0]} width={2.65}/></Appear3D>
    <Appear3D index={1}><Tank position={[-.9,0,0]} color="#e6a260" tall/><Tank position={[.5,0,0]} color="#dad5b5" tall/></Appear3D>
    <Appear3D index={2} position={[3.25,0,0]}><Model name="fan" rotation={[0,Math.PI/2,0]} scale={1.3}/><Belt position={[1,0,0]} length={3}/></Appear3D>
    <Link from={[-3.5,.65,0]} to={[-.9,.65,0]}/><Link from={[.5,.65,0]} to={[3.1,.65,0]}/>
    <ItemFlow name="gold" from={[-5.4,1.8,0]} to={[-4.1,1.5,0]} start={16} arc={.3}/>
    <ItemFlow name="iron" from={[3.4,.84,0]} to={[5.2,.84,0]} start={55}/>
    <WorldLabel text="熔炼 · 合金 · 鼓风冷冻回收" position={[0,4.1,1.5]} width={6} color={COLORS.copper} index={1}/>
  </>;
};

export const ChemistryWorld:React.FC<{shotId:number}>=({shotId})=>{
  const frame=useCurrentFrame();const {frames}=useShot();
  if(shotId===32){
    const formulas=['CO₂','CO','CH₄','C₂H₂','N₂','NH₃','NO','NO₂','N₂O','H₂S','O₃','HNO₃'];
    return <>{formulas.map((text,i)=><Appear3D key={text} index={i} position={[-4.6+(i%6)*1.84,0,Math.floor(i/6)*2.2-1.1]}>
      <Model name="ampoule" scale={1.35} rotation={[0,.13,0]}/>
      <mesh position={[0,.68,.015]}><boxGeometry args={[.23,.51,.16]}/>
        <meshStandardMaterial color={i%3===0?COLORS.copper:i%3===1?COLORS.teal:COLORS.violet} transparent opacity={.65}/></mesh>
      <WorldLabel text={text} position={[0,1.65,0]} width={1.8} index={i}/>
    </Appear3D>)}</>;
  }
  if(shotId===33)return <>
    {[-2.6,2.6].map((x,i)=><Appear3D key={i} index={i} position={[x,0,0]}>
      <Mixer heat={i?'super':'normal'} color={i?'#77bcb1':'#a6a38b'}/>
      <ItemFlow name="charcoal" from={[-1.2,2.2,0]} to={[0,1.5,0]} start={i?20:12} arc={.3}/>
      <Gas color={i?COLORS.teal:COLORS.copper} position={[0,.9,0]}/>
      <WorldLabel text={i?'超级加热 · 250 mB CO₂':'普通加热 · 125 mB CO'} position={[0,4.2,0]} width={4.4} index={i}/>
    </Appear3D>)}
  </>;
  if(shotId===34){
    const closing=frame>=frames*.4;const closed=frame>=frames*.4+12;
    const lid=eased(frame,[frames*.4,frames*.4+12],[.2,0]);
    return <>
      <Appear3D><Model name="basin" scale={2}/><Liquid position={[0,1.67,0]} width={1.5} depth={1.5} color="#79a69b"/>
        <Model name={closing?'lid_basin':'lid_basin_open'} position={[0,2+(closing?lid:0),0]} scale={2}
          rotation={[0,0,closing?lid*.25:0]}/></Appear3D>
      <Appear3D index={1}><Tank position={[-3.3,0,-.3]} color={COLORS.teal} tall/><Model name="pump" position={[-1.8,.5,0]} rotation={[0,Math.PI/2,0]}/></Appear3D>
      <Appear3D index={2}><Model name="vacuum_base" position={[3.1,2,0]}/><Model name="vacuum_head" position={[3.1,.85,0]}/><Model name="basin" position={[3.1,0,0]}/></Appear3D>
      <Link from={[-3.3,.6,0]} to={[-.9,.6,0]} color={COLORS.teal}/>
      <Link from={[.9,.6,0]} to={[3.1,.6,0]} color={COLORS.violet}/>
      <Gas position={[0,.45,0]} leak={!closed}/>
      <WorldLabel text={closed?'专用盆盖关闭 · 气体保留':'盆盖未关闭 · 200 mB/秒流失'} position={[0,3.45,0]} width={5.1}
        color={closed?COLORS.teal:COLORS.red}/>
      <WorldLabel text="独立进气" position={[-3.1,2.6,0]} width={2.4} index={1}/>
      <WorldLabel text="独立出气" position={[3.1,2.6,0]} width={2.4} index={2}/>
    </>;
  }
  if(shotId===35){
    const crystallized=frame>frames*.48;
    return <>
      {[-2.6,2.6].map((x,i)=><Appear3D key={i} index={i} position={[x,0,0]}>
        <Model name="basin" scale={1.8}/><Liquid position={[0,1.52,0]} width={1.36} depth={1.36}
          color="#e3b478" crystalline={i===0&&crystallized} crystalAt={Math.round(frames*.48)}/>
        <Model name="pump" position={[-1.45,.15,0]}/><Tank position={[1.5,0,0]} color="#e3b478"/>
        {i===1&&<><Model name="stone" position={[-.5,2.9,0]}/><Model name="stone" position={[.5,2.9,0]}/>
          <mesh position={[1.2,1.5,-.7]}><boxGeometry args={[.1,3,.1]}/><meshStandardMaterial color="#4d594c"/></mesh></>}
        <WorldLabel text={i===0?(crystallized?'露天 · 结晶仍是液体':'直见天空 · 连续计时'):'上方遮挡 · 不结晶'} position={[0,3.3,0]}
          width={4.25} color={i===0?COLORS.copper:COLORS.paper} index={i}/>
      </Appear3D>)}
      <WorldLabel text={`${Math.min(30,Math.round(eased(frame,[24,frames*.48],[0,30])))} / 30 秒 · 时间压缩示意`}
        position={[0,.5,2.9]} width={5.5} color={COLORS.teal} index={2}/>
      <ItemFlow name="bucket" from={[-3.8,.8,1]} to={[-1.4,.8,1]} start={60} scale={.35}/>
    </>;
  }
  if(shotId===36)return <>
    {['low','medium','high'].map((tier,i)=><Appear3D key={tier} index={i} position={[-3.7+i*3.7,0,0]}>
      <Model name="basin"/><Liquid color={['#c6b092','#86b5a9','#d9a571'][i]} crystalline/>
      <Model name="vacuum_base" position={[0,2.35,0]}/>
      <Model name="vacuum_head" position={[0,.8+Math.sin(phase(frame,3.71,i*.2))*.04,0]}/>
      <Model name="vacuum_cog" position={[0,2.35,0]} rotation={[0,phase(frame,3.97-i*.71,i*.17),0]}/>
      <WorldLabel text={['低压 ≤512 RPM','中压 ≤2,048 RPM','高压 >2,048 RPM'][i]} position={[0,3.8,0]} width={3.4} index={i}/>
      <ItemFlow name={`crystal_${tier}`} from={[0,.9,0]} to={[0,1,1.8]} start={35+i*4} interval={57} scale={.55}/>
    </Appear3D>)}
  </>;
  if(shotId===37)return <>
    <Appear3D position={[-4.5,0,0]}><Mixer color="#766c85" heat="super"/>
      <WorldLabel text="化学 · 混合金属" position={[0,4,0]} width={3}/></Appear3D>
    <Appear3D index={1} position={[-1.6,0,0]}><Tank color="#b198a1" tall/><Model name="crystal_high" position={[0,3.3,0]} scale={.8}/></Appear3D>
    <Appear3D index={2}><Belt position={[2.3,0,0]} length={5}/><Deployer position={[1.6,0,0]}/><Press position={[4,0,0]}/></Appear3D>
    <Link from={[-4,.7,0]} to={[-1.6,.7,0]} color={COLORS.violet}/>
    <ItemFlow name="crystal_high" from={[-.5,.84,0]} to={[2.7,.84,0]} start={27}/>
    <ItemFlow name="netherite_scrap" from={[4,.84,0]} to={[5.3,.84,0]} start={85}/>
    <WorldLabel text="高压结晶 → 序列装配 → 2个碎片" position={[1.7,3.6,1.4]} width={5.2} index={1}/>
  </>;
  return <>
    <Appear3D><Press/><Belt length={6}/><Model name="speed_controller" position={[-3.6,0,-.5]}/><Model name="burner" position={[3.1,0,-.5]}/></Appear3D>
    <WorldLabel text="配方刻数 · 实际转速 · 燃料条件" position={[0,3.45,0]} width={5.5}/>
    <WorldLabel text="先读状态，再找瓶颈" position={[0,1.75,1.8]} width={4.5} color={COLORS.teal} index={1}/>
    <ItemFlow name="copper_sheet" from={[-2.5,.84,0]} to={[2.5,.84,0]}/>
  </>;
};
