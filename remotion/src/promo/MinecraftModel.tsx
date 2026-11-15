import React, {createContext, useContext, useEffect, useMemo, useState} from 'react';
import {useThree} from '@react-three/fiber';
import {cancelRender, continueRender, delayRender, staticFile, useCurrentFrame} from 'remotion';
import * as THREE from 'three';
import {Vec3} from './motion';

type TextureInfo = {
  file: string; width: number; height: number; cutout: boolean;
  columns: number; rows: number; frames: {index: number; ticks: number}[];
};
type MeshGroup = {texture: string; emissive: boolean; tint: string; positions: number[]; uv: number[]};
export type AssetCatalog = {models: Record<string, {groups: MeshGroup[]}>; textures: Record<string, TextureInfo>};
const Assets = createContext<AssetCatalog | null>(null);
let catalogPromise: Promise<AssetCatalog> | undefined;

export const AssetProvider: React.FC<{children: React.ReactNode}> = ({children}) => {
  const [catalog, setCatalog] = useState<AssetCatalog | null>(null);
  const [handle] = useState(() => delayRender('Loading actual Minecraft model geometry'));
  useEffect(() => {
    catalogPromise ??= fetch(staticFile('promo/3d/models.json')).then(async response => {
      if (!response.ok) throw new Error(`Model catalog: HTTP ${response.status}`);
      return response.json() as Promise<AssetCatalog>;
    });
    let mounted = true;
    catalogPromise.then(data => {
      if (mounted) setCatalog(data);
      continueRender(handle);
    }).catch(cancelRender);
    return () => {mounted = false; continueRender(handle);};
  }, [handle]);
  return catalog ? <Assets.Provider value={catalog}>{children}</Assets.Provider> : null;
};

const textures = new Map<string, THREE.Texture>();
const meshes = new Map<string, {geometry: THREE.BufferGeometry; material: THREE.MeshStandardMaterial; texture: string}[]>();

const textureFor = (key: string, info: TextureInfo, invalidate: () => void) => {
  const cached = textures.get(key);
  if (cached) return cached;
  const handle = delayRender(`Loading pixel texture ${key}`);
  const texture = new THREE.TextureLoader().load(staticFile(info.file), () => {invalidate(); continueRender(handle);},
    undefined, error => cancelRender(new Error(`Cannot load ${key}: ${String(error)}`)));
  texture.colorSpace = THREE.SRGBColorSpace;
  texture.minFilter = THREE.NearestFilter;
  texture.magFilter = THREE.NearestFilter;
  texture.generateMipmaps = false;
  texture.wrapS = texture.wrapT = THREE.RepeatWrapping;
  texture.repeat.set(1 / info.columns, 1 / info.rows);
  textures.set(key, texture);
  return texture;
};

export const useAssets = () => {
  const value = useContext(Assets);
  if (!value) throw new Error('Minecraft models require AssetProvider');
  return value;
};

export const modelParts = (name: string, catalog: AssetCatalog, invalidate: () => void) => {
  const cached = meshes.get(name);
  if (cached) return cached;
  const source = catalog.models[name];
  if (!source) throw new Error(`Missing actual model: ${name}`);
  const result = source.groups.map(group => {
    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute('position', new THREE.Float32BufferAttribute(group.positions, 3));
    geometry.setAttribute('uv', new THREE.Float32BufferAttribute(group.uv, 2));
    geometry.computeVertexNormals();
    const info = catalog.textures[group.texture];
    const map = textureFor(group.texture, info, invalidate);
    const material = new THREE.MeshStandardMaterial({
      map, color: group.tint, roughness: 0.82, metalness: 0.05,
      side: THREE.DoubleSide, alphaTest: info.cutout ? 0.07 : 0,
      emissive: group.emissive ? '#ffffff' : '#000000',
      emissiveMap: group.emissive ? map : undefined,
      emissiveIntensity: group.emissive ? 0.6 : 0,
    });
    return {geometry, material, texture: group.texture};
  });
  meshes.set(name, result);
  return result;
};

export const Model: React.FC<{
  name: string; position?: Vec3; rotation?: Vec3; scale?: number | Vec3;
  castShadow?: boolean;
}> = ({name, position = [0, 0, 0], rotation = [0, 0, 0], scale = 1, castShadow = true}) => {
  const catalog = useAssets();
  const {invalidate} = useThree();
  const frame = useCurrentFrame();
  const parts = useMemo(() => modelParts(name, catalog, invalidate), [catalog, name, invalidate]);
  const coreUniforms = useMemo(() => ({time:{value:0},coreTexture:{value:parts[0].material.map}}), [parts]);
  coreUniforms.time.value=frame/30;
  for (const part of parts) {
    const info = catalog.textures[part.texture];
    if (info.frames.length) {
      const ticks = frame * 20 / 30 * 1.017 + part.texture.length * .17;
      const total = info.frames.reduce((sum, entry) => sum + entry.ticks, 0);
      let cursor = ticks % total;
      let index = info.frames[0].index;
      for (const entry of info.frames) {
        index = entry.index;
        if (cursor < entry.ticks) break;
        cursor -= entry.ticks;
      }
      part.material.map!.offset.set((index % info.columns) / info.columns,
        1 - (Math.floor(index / info.columns) + 1) / info.rows);
    }
  }
  return <group position={position} rotation={rotation} scale={scale} dispose={null}>
    {parts.map((part, index) => name==='reactor_core'?<mesh key={index} geometry={part.geometry} castShadow={castShadow}>
      <shaderMaterial uniforms={coreUniforms}
        vertexShader={'varying vec2 vUv; varying vec3 vPos; varying vec3 vNormal; void main(){vUv=uv;vPos=position;vNormal=normalize(normalMatrix*normal);gl_Position=projectionMatrix*modelViewMatrix*vec4(position,1.0); }'}
        fragmentShader={'uniform float time; uniform sampler2D coreTexture; varying vec2 vUv; varying vec3 vPos; varying vec3 vNormal; void main(){float pulse=0.5+0.5*sin(vPos.y*19.0-time*2.17+sin(vPos.x*11.0+time*1.73)*2.0);float tex=texture2D(coreTexture,vUv).r;vec3 c=mix(vec3(0.42,0.05,0.012),vec3(1.0,0.62,0.18),pulse);float rim=pow(1.0-abs(vNormal.z),2.0);gl_FragColor=vec4(c*(0.7+0.3*tex)+vec3(0.3,0.08,0.01)*rim,1.0);}'}/>
    </mesh>:<mesh key={index} geometry={part.geometry} material={part.material}
      castShadow={castShadow} receiveShadow/>) }
  </group>;
};
