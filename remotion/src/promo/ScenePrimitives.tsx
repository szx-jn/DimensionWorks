import React, {useEffect, useLayoutEffect, useMemo, useRef} from 'react';
import {useThree} from '@react-three/fiber';
import {useCurrentFrame} from 'remotion';
import * as THREE from 'three';
import {Model, modelParts, useAssets} from './MinecraftModel';
import {arrive, eased, impact, leave, phase, TRACE_INTERVAL, Vec3, STAGGER} from './motion';
import {useShot} from './scene-context';

export const COLORS = {copper: '#e4aa67', teal: '#64cdbd', paper: '#f2e5c9', red: '#ff7667', violet: '#c1a5ff'};

export const Appear3D: React.FC<{children: React.ReactNode; index?: number; at?: number; position?: Vec3}> =
({children, index = 0, at = 8, position = [0, 0, 0]}) => {
  const frame = useCurrentFrame();
  const rise = arrive(frame, at + index * STAGGER);
  const elements = React.Children.toArray(children);
  return <group position={[position[0], position[1] - (1 - rise) * .55, position[2]]}
    scale={Math.max(0, rise)} rotation={[0, (1 - rise) * -.16, 0]}>
    {elements.length===1?elements:elements.map((child,i)=>{
      const part=arrive(frame,at+(index+i)*STAGGER);
      return <group key={i} scale={Math.max(0,part)} position={[0,-(1-part)*.22,0]}
        rotation={[0,(1-part)*.09,0]}>{child}</group>;
    })}
  </group>;
};

export const Tiles: React.FC<{name?: string; width?: number; depth?: number; y?: number}> =
({name = 'stone', width = 13, depth = 7, y = -1}) => {
  const catalog = useAssets();
  const {invalidate} = useThree();
  const parts = useMemo(() => modelParts(name, catalog, invalidate), [catalog, name, invalidate]);
  const refs = useRef<(THREE.InstancedMesh | null)[]>([]);
  useLayoutEffect(() => {
    const matrix = new THREE.Matrix4();
    for (const mesh of refs.current) {
      if (!mesh) continue;
      for (let i = 0; i < width * depth; i++) {
        matrix.makeTranslation(i % width - (width - 1) / 2, y, Math.floor(i / width) - (depth - 1) / 2);
        mesh.setMatrixAt(i, matrix);
      }
      mesh.instanceMatrix.needsUpdate = true;
    }
  }, [width, depth, y, parts]);
  return <group dispose={null}>{parts.map((part, i) => <instancedMesh key={i}
    ref={mesh => {refs.current[i] = mesh;}} args={[part.geometry, part.material, width * depth]}
    receiveShadow/>)}</group>;
};

export const WorldLabel: React.FC<{text: string; position: Vec3; width?: number; color?: string; index?: number}> =
({text, position, width = 2.4, color = COLORS.paper, index = 0}) => {
  const {camera} = useThree();
  const frame = useCurrentFrame();
  const texture = useMemo(() => {
    const canvas = document.createElement('canvas');
    canvas.width = 768; canvas.height = 144;
    const context = canvas.getContext('2d')!;
    context.fillStyle = 'rgba(12,26,28,0.88)'; context.fillRect(0, 12, 768, 118);
    context.fillStyle = color; context.fillRect(0, 12, 7, 118);
    context.font = '600 84px "PingFang SC",sans-serif';
    const measure = context.measureText(text).width;
    if (measure > 710) context.font = `600 ${Math.floor(84 * 710 / measure)}px "PingFang SC",sans-serif`;
    context.textAlign = 'center'; context.textBaseline = 'middle';
    context.fillText(text, 388, 75);
    const map = new THREE.CanvasTexture(canvas); map.colorSpace = THREE.SRGBColorSpace;
    return map;
  }, [text, color]);
  useEffect(() => () => texture.dispose(), [texture]);
  const lift = arrive(frame, 16 + index * STAGGER);
  return <mesh position={[position[0], position[1] - (1 - lift) * .3, position[2]]}
    quaternion={camera.quaternion} scale={[lift, lift, lift]}>
    <planeGeometry args={[width, width * 144 / 768]}/>
    <meshBasicMaterial map={texture} transparent toneMapped={false} depthTest={false}/>
  </mesh>;
};

export const Tube: React.FC<{points: Vec3[]; color?: string; radius?: number; glow?: boolean}> =
({points, color = COLORS.copper, radius = .035, glow = false}) => {
  const pointKey = points.flat().join(',');
  const curve = useMemo(() => new THREE.CatmullRomCurve3(points.map(p => new THREE.Vector3(...p)), false, 'centripetal'), [pointKey]);
  return <mesh><tubeGeometry args={[curve, Math.max(12, points.length * 10), radius, 6, false]}/>
    <meshStandardMaterial color={color} roughness={.36} metalness={.55}
      emissive={glow ? color : '#000000'} emissiveIntensity={glow ? .7 : 0}/></mesh>;
};

export const Link: React.FC<{from: Vec3; to: Vec3; color?: string; raised?: boolean}> =
({from, to, color = COLORS.copper, raised = false}) => <Tube color={color} points={[
  from, [from[0] + (to[0] - from[0]) * .25, raised ? 2.6 : from[1], from[2]],
  [from[0] + (to[0] - from[0]) * .75, raised ? 2.6 : to[1], to[2]], to,
]}/>;

export const TracePackets: React.FC<{shotId: number; from?: Vec3; to?: Vec3; color?: string}> =
({shotId, from = [-5.4, .065, 3.2], to = [5.4, .065, 3.2], color = COLORS.copper}) => {
  const frame = useCurrentFrame();
  const {frames: durationInFrames} = useShot();
  return <>{Array.from({length: Math.ceil(durationInFrames / TRACE_INTERVAL) + 1}, (_, i) => {
    const local = frame - 12 - i * TRACE_INTERVAL;
    if (local < 0 || local > 49) return null;
    const travel = eased(local, [0, 49], [0, 1]);
    const birth = arrive(local, 0);
    const size = Math.max(0, birth) * leave(local,50) * (.8 + .2 * Math.sin(phase(frame, 1.91, shotId * .13)));
    return <group key={i} position={[
      from[0] + (to[0] - from[0]) * travel,
      from[1] + (to[1] - from[1]) * travel + Math.sin(travel * Math.PI) * .14,
      from[2] + (to[2] - from[2]) * travel,
    ]} scale={size} rotation={[0, frame * .033, .3]}>
      <mesh><boxGeometry args={[.14, .08, .08]}/><meshBasicMaterial color={color} toneMapped={false}/></mesh>
      <mesh position={[-.18, 0, 0]}><boxGeometry args={[.15, .022, .022]}/>
        <meshBasicMaterial color={color} transparent opacity={.35} toneMapped={false}/></mesh>
    </group>;
  })}</>;
};

export const CopperMotif: React.FC<{variant: string; shotId: number; accent: number}> = ({variant, shotId, accent}) => {
  const frame = useCurrentFrame();
  const kick = impact(frame, accent);
  const angle = phase(frame, 13.73, shotId * .17);
  if (variant === 'ring') return <group position={[0, .04, 0]} rotation={[-Math.PI / 2, 0, angle * .035]}
    scale={1 + kick * .04}>
    <mesh><torusGeometry args={[4.25, .025, 5, 72]}/><meshStandardMaterial color={COLORS.copper} emissive={COLORS.copper} emissiveIntensity={.22}/></mesh>
    <mesh rotation={[0, 0, angle]}><torusGeometry args={[4.43, .018, 4, 48, Math.PI * .37]}/>
      <meshBasicMaterial color={COLORS.teal}/></mesh>
  </group>;
  const points: Vec3[] = variant === 'circuit' ? [[-5.5,.04,3],[-5.5,.04,-2.8],[0,.04,-2.8],[5.5,.04,-2.8],[5.5,.04,3]]
    : variant === 'route' ? [[-5.4,.04,3],[-2.7,.04,3],[0,.04,2.7],[2.7,.04,3],[5.4,.04,3]]
    : [[-5.4,.04,3.2],[0,.04,3.2],[5.4,.04,3.2]];
  return <group scale={[1 + kick * .025, 1, 1]}><Tube points={points} radius={.02} glow/>
    {[0,1,2].map(i => <mesh key={i} position={[-4.6+i*4.6,.08,3.2]} rotation={[0,-angle*.02,0]}>
      <boxGeometry args={[.16,.06,.16]}/><meshStandardMaterial color={COLORS.copper} metalness={.6}/></mesh>)}
  </group>;
};

export const Gear: React.FC<{position?: Vec3; large?: boolean; speed?: number; offset?: number; scale?: number}> =
({position = [0,0,0], large = false, speed = 2.37, offset = 0, scale = 1}) => {
  const frame = useCurrentFrame();
  return <group position={position} rotation={[Math.PI/2, 0, 0]} scale={scale}>
    <group rotation={[0, phase(frame, speed, offset), 0]}><Model name={large ? 'large_cog' : 'cog'} position={[0,-.5,0]}/></group>
  </group>;
};

export const Shaft: React.FC<{x: number; z?: number; length?: number; y?: number; running?: boolean}> =
({x, z = -.6, length = 3, y = .5, running = true}) => {
  const frame = useCurrentFrame();
  return <group position={[x,y,z]} rotation={[0,0,-Math.PI/2]}>
    {Array.from({length}, (_, i) => <Model key={i} name="shaft" position={[0,i-(length-1)/2-.5,0]}
      rotation={[0,running ? phase(frame,2.37,i*.07) : 0,0]}/>)}
  </group>;
};

export const Belt: React.FC<{position?: Vec3; length?: number; vertical?: boolean}> =
({position = [0,0,0], length = 5, vertical = false}) => {
  const frame = useCurrentFrame();
  return <group position={position} rotation={vertical ? [Math.PI/2,0,0] : [0,-Math.PI/2,0]}>
    {Array.from({length}, (_, i) => <group key={i} position={[0,0,i-(length-1)/2]}>
      <Model name={i===0?'belt_start':i===length-1?'belt_end':'belt_middle'}/>
      <Model name="belt_bottom"/>
      {(i===0||i===length-1)&&<group position={[0,.5,0]} rotation={[0,0,phase(frame,1.87,i*.11)]}>
        <Model name="belt_pulley" position={[0,-.5,0]}/></group>}
    </group>)}
    {Array.from({length:length*3},(_,i)=><mesh key={i} position={[0,.813,((i/3+frame/30*.81)%length)-(length-1)/2-.5]}>
      <boxGeometry args={[.92,.009,.025]}/><meshStandardMaterial color="#242522" roughness={.9}/></mesh>)}
  </group>;
};

export const ItemFlow: React.FC<{name?: string; from: Vec3; to: Vec3; interval?: number; scale?: number; start?: number; arc?: number; running?: boolean}> =
({name='iron',from,to,interval=53,scale=.55,start=12,arc=0,running=true}) => {
  const frame = useCurrentFrame(); const {frames: durationInFrames} = useShot();
  return <>{Array.from({length:Math.ceil(durationInFrames/interval)+2},(_,i)=>{
    const local=frame-start-i*interval;
    if(local<0||local>100) return null;
    const travel=eased(running?local:Math.min(local,38),[0,100],[0,1]);
    const entry=arrive(local);
    return <Model key={i} name={name} scale={scale*Math.max(0,entry)*leave(local,101)}
      position={[from[0]+(to[0]-from[0])*travel,from[1]+(to[1]-from[1])*travel+Math.sin(travel*Math.PI)*arc,from[2]+(to[2]-from[2])*travel]}
      rotation={[-Math.PI/2,0,.14+Math.sin(phase(frame,4.13,i*.14))*.035]}/>;
  })}</>;
};

export const Press: React.FC<{position?: Vec3; fast?: boolean; offset?: number; running?: boolean}> =
({position=[0,0,0],fast=false,offset=0,running=true})=>{
  const frame=useCurrentFrame(); const cycle=fast?43:71;
  const local=running?(frame+offset)%cycle:0;
  const height=eased(local,[0,cycle*.3,cycle*.4,cycle*.46,cycle*.64,cycle],[.52,.52,.08,.03,.52,.52]);
  return <group position={position}>
    <Model name="press_base" position={[0,2,0]}/>
    <Model name="press_head" position={[0,.827+height,0]}/>
    <Shaft x={0} y={2.5} length={3} running={running}/>
    <Gear position={[-1.35,2.5,-.6]} speed={fast?1.31:2.37}/>
    <mesh position={[.9,1.5,-.6]}><boxGeometry args={[.12,3,.12]}/><meshStandardMaterial color="#434a42" metalness={.6}/></mesh>
  </group>;
};

export const Mixer: React.FC<{position?: Vec3; color?: string; heat?: 'normal'|'super'|false; lid?: boolean}> =
({position=[0,0,0],color='#e2a052',heat=false,lid=false})=>{
  const frame=useCurrentFrame(); const dip=eased(frame%83,[0,15,21,61,74,83],[.6,.6,.2,.2,.6,.6]);
  return <group position={position}>
    <Model name="basin" position={[0,heat?.8:0,0]}/>
    <Liquid position={[0,(heat?.8:0)+.83,0]} color={color}/>
    {heat&&<><Model name="burner"/><Model name={heat==='super'?'blaze_super':'blaze'} position={[0,.12,0]}/>
      <pointLight position={[0,.45,0]} color={heat==='super'?COLORS.teal:'#ff9c43'} intensity={2.5} distance={3}/></>}
    <Model name="mixer_base" position={[0,2.8,0]}/>
    <Model name="mixer_pole" position={[0,1.45+dip,0]}/>
    <Model name="mixer_head" position={[0,1+dip,0]} rotation={[0,phase(frame,1.73),0]}/>
    {lid&&<Model name="lid_basin" position={[0,heat?1.8:1,0]}/ >}
    <Gear position={[1.15,3.3,0]} speed={2.17}/>
  </group>;
};

export const Millstone: React.FC<{position?: Vec3}> = ({position=[0,0,0]})=>{
  const frame=useCurrentFrame();
  return <group position={position}><Model name="millstone_base"/>
    <Model name="millstone_inner" rotation={[0,phase(frame,2.71),0]}/><Gear position={[-.75,.5,0]} speed={2.71}/></group>;
};

export const Deployer: React.FC<{position?: Vec3; offset?: number}> = ({position=[0,0,0],offset=0})=>{
  const frame=useCurrentFrame(); const extension=eased((frame+offset)%79,[0,18,30,38,57,79],[.4,.4,-.45,-.45,.4,.4]);
  return <group position={position}><Model name="deployer_base" position={[0,2,0]}/>
    <Model name="deployer_pole" position={[0,1+extension,0]}/><Model name="deployer_hand" position={[0,.95+extension,0]}/>
    <Shaft x={0} y={2.5} length={2}/></group>;
};

export const Liquid: React.FC<{position?: Vec3; color?: string; width?: number; depth?: number; crystalline?: boolean; crystalAt?:number}> =
({position=[0,.83,0],color='#cd9250',width=.75,depth=.75,crystalline=false,crystalAt=8})=>{
  const frame=useCurrentFrame();
  const uniforms=useMemo(()=>({time:{value:0},color:{value:new THREE.Color(color)}}),[color]);
  uniforms.time.value=frame/30;
  return <group position={position}>
    <mesh rotation={[-Math.PI/2,0,0]}><planeGeometry args={[width,depth,14,14]}/>
      <shaderMaterial uniforms={uniforms} side={THREE.DoubleSide}
        vertexShader={'uniform float time; varying vec2 vUv; void main(){vUv=uv; vec3 p=position; p.z+=0.012*sin(p.x*14.0+time*2.19)*cos(p.y*17.0-time*1.73); gl_Position=projectionMatrix*modelViewMatrix*vec4(p,1.0); }'}
        fragmentShader={'uniform float time; uniform vec3 color; varying vec2 vUv; void main(){float w=sin((vUv.x+vUv.y)*28.0+time*2.37)*0.05; float stripe=smoothstep(0.86,0.97,sin(vUv.x*31.0-vUv.y*17.0-time*0.73));gl_FragColor=vec4(color*(0.9+w)+stripe*0.18,1.0);}'}/>
    </mesh>
    {crystalline&&[0,1,2,3].map(i=>{
      const birth=arrive(frame,crystalAt+i*4);
      return <mesh key={i} position={[-.23+(i%2)*.45,.018+(1-birth)*.14,Math.floor(i/2)*.37-.17]}
        rotation={[.5,phase(frame,5.73,i*.21),.7]} scale={Math.max(0,birth)*(.09+.01*Math.sin(phase(frame,3.17,i*.21)))}>
        <octahedronGeometry/><meshStandardMaterial color={COLORS.paper} emissive={color} emissiveIntensity={.12} metalness={.4} roughness={.35}/></mesh>;
    })}
  </group>;
};

export const Gas: React.FC<{position?: Vec3; color?: string; leak?: boolean}> =
({position=[0,0,0],color=COLORS.teal,leak=true})=>{
  const frame=useCurrentFrame();
  return <group position={position}>{Array.from({length:8},(_,i)=>{
    const age=(frame-i*5)%57;
    if(age<0||!leak)return null;
    const a=arrive(age);const rise=eased(age,[0,57],[0,1.7]);
    return <mesh key={i} position={[Math.sin(i*2.37)*.24+rise*.12,.6+rise,Math.cos(i*1.73)*.24]}
      scale={.06*Math.max(0,a)*(1+rise*.3)}><boxGeometry/><meshBasicMaterial color={color} transparent
        opacity={eased(age,[0,8,44,57],[0,.65,.4,0])} depthWrite={false}/></mesh>;
  })}</group>;
};

export const Packager: React.FC<{position?: Vec3; active?: boolean}> = ({position=[0,0,0],active=true})=>{
  const frame=useCurrentFrame(); const hatch=active?Math.sin(phase(frame,2.67))*.06:0;
  return <group position={position}><Model name="packager"/><Model name="packager_hatch" position={[0,hatch,.025]}/>
    <Model name="packager_tray" position={[0,.02,.08+hatch]}/></group>;
};

export const Frog: React.FC<{position?: Vec3; active?: boolean; pulseAt?:number}> = ({position=[0,0,0],active=true,pulseAt=35})=>{
  const frame=useCurrentFrame();const local=((frame-pulseAt+35)%89+89)%89;
  const reach=active?eased(local,[0,31,35,39,49,89],[.05,.05,1.3,1.3,.05,.05]):.05;
  return <group position={position}><Model name="frog_base"/>
    <group position={[0,Math.sin(phase(frame,2.93))*.025,0]}><Model name="frog_body"/><Model name="frog_head"/>
      <Model name="frog_tongue" position={[0,0,-reach*.15]} scale={[1,1,1+reach]}/></group>
  </group>;
};

export const Chain: React.FC<{position?: Vec3; length?: number}> = ({position=[0,0,0],length=5})=>{
  const frame=useCurrentFrame();
  return <group position={position}>
    {[-length/2,length/2].map((x,i)=><group key={i} position={[x,0,0]}><Model name="chain_base"/>
      <Model name="chain_wheel" position={[0,.5,0]} rotation={[0,phase(frame,2.43,i*.23),0]}/>
      <Model name="chain_shaft"/>
      <mesh position={[0,-.8,0]}><boxGeometry args={[.13,1.6,.13]}/><meshStandardMaterial color="#53625d" metalness={.6}/></mesh>
    </group>)}
    {Array.from({length:30},(_,i)=><mesh key={i} position={[((i/30*length+frame*.015)%length)-length/2,.6,i%2?.16:-.16]}
      rotation={[Math.PI/2,0,i%2?Math.PI/2:0]}><torusGeometry args={[.056,.02,4,6]}/>
      <meshStandardMaterial color="#a8aea0" metalness={.75} roughness={.4}/></mesh>)}
  </group>;
};

export const Player: React.FC<{position?: Vec3; walking?: boolean; rotation?: number; crouch?: boolean; scale?: number}> =
({position=[0,0,0],walking=false,rotation=0,crouch=false,scale=1})=>{
  const frame=useCurrentFrame(); const swing=walking?Math.sin(phase(frame,1.13))*.6:Math.sin(phase(frame,3.17))*.025;
  return <group position={position} rotation={[0,rotation,0]} scale={scale}>
    <group position={[0,crouch?-.18:0,0]} rotation={[crouch?.14:0,0,0]}>
      <Model name="player_head" position={[0,2,0]} rotation={[.03,Math.sin(phase(frame,6.13))*.12,0]}/>
      <Model name="player_body" position={[0,1.5,0]}/>
      <Model name="player_right_arm" position={[-.375,1.45,0]} rotation={[swing,0,.025]}/>
      <Model name="player_left_arm" position={[.375,1.45,0]} rotation={[-swing,0,-.025]}/>
    </group>
    <Model name="player_right_leg" position={[-.125,.75,0]} rotation={[-swing,0,0]}/>
    <Model name="player_left_leg" position={[.125,.75,0]} rotation={[swing,0,0]}/>
  </group>;
};

export const Tree: React.FC<{position?: Vec3; scale?: number; index?: number}> = ({position=[0,0,0],scale=1,index=0})=>{
  const frame=useCurrentFrame();
  return <group position={position} scale={scale} rotation={[0,Math.sin(phase(frame,5.71,index*.33))*.018,0]}>
    {[0,1,2].map(i=><Model key={i} name="log" position={[0,i,0]}/ >)}
    {[-1,0,1].flatMap(x=>[-1,0,1].map(z=><Model key={`${x}-${z}`} name="leaves" position={[x,2.5,z]}/ >))}
    {[-.5,.5].flatMap(x=>[-.5,.5].map(z=><Model key={`top-${x}-${z}`} name="leaves" position={[x,3.5,z]}/ >))}
  </group>;
};

export const WindGenerator: React.FC<{position?: Vec3; scale?: number}> = ({position=[0,0,0],scale=1})=>{
  const frame=useCurrentFrame();
  return <group position={position} scale={scale}><Model name="wind_base"/>
    <group position={[0,4.5,-.375]} rotation={[0,0,phase(frame,3.71)]}><Model name="wind_blades"/></group></group>;
};

export const Tank: React.FC<{position?: Vec3; color?: string; tall?: boolean}> = ({position=[0,0,0],color=COLORS.copper,tall=false})=><group position={position}>
  <Model name={tall?'tank_bottom':'tank'}/>{tall&&<><Model name="tank_middle" position={[0,1,0]}/><Model name="tank_top" position={[0,2,0]}/></>}
  <mesh position={[0,tall?1.3:.5,0]}><boxGeometry args={[.68,tall?2.35:.66,.68]}/>
    <meshStandardMaterial color={color} emissive={color} emissiveIntensity={.12} transparent opacity={.72}/></mesh>
</group>;
