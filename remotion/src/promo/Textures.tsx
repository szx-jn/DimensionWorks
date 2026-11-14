import React from 'react';
import {staticFile} from 'remotion';

export const Sprite:React.FC<{name:string;x?:number;y?:number;size?:number}> =({name,x=-65,y=-60,size=130})=><image href={staticFile(`promo/textures/${name}.png`)} x={x} y={y} width={size} height={size} style={{imageRendering:'pixelated'}}/>;

export const itemTexturesFor=(label:string):string[]=>{
 const rules:[string,string][]=[
 ['下界合金碎片','netherite_scrap'],['下界合金高压结晶','high_pressure_crystal'],
 ['粗铁','raw_iron'],['铁粉','dust_iron'],['铜板','copper_sheet'],['铜锭','copper_ingot'],
 ['铜粉','dust_copper'],['红石','redstone'],['灌注合金','alloy_infused'],
 ['复杂链路处理器','engineering_processor'],['余烬面粉','cinder_flour'],['下界岩','netherrack'],
 ['硫粉','dust_sulfur'],['树脂','forest_resin'],['纤维','root_fiber'],['滤芯','forest_filter'],
 ['框架','ironwood_frame'],['腐殖料','mature_humus'],['树叶','oak_leaves'],['圆石','cobblestone'],
 ['胶料','elastic_compound'],['钢叶带坯','steeleaf_seal_strip'],['弹跳封带','bounce_tape'],
 ['物质球','matter_ball'],['蜂蜜','honey_bottle'],['金属锭','iron_ingot'],
 ];
 return [...new Set(rules.filter(([term])=>label.includes(term)).map(([,name])=>name))].slice(0,2);
};

/** Existing front and top skins on a small animated three-face schematic. */
export const TexturedCase:React.FC<{front:string;top?:string;t:number;active:boolean;width?:number;height?:number}> =({front,top=front,t,active,width=150,height=150})=>{
 const x=-width/2,y=-height/2;
 return <><path d={`M${x} ${y} l25 -20 h${width} l-25 20Z`} fill="#91a798"/>
 <image href={staticFile(`promo/textures/${top}.png`)} x="0" y="0" width="1" height="1" transform={`matrix(${width} 0 -25 20 ${x+25} ${y-20})`} preserveAspectRatio="none" style={{imageRendering:'pixelated'}}/>
 <path d={`M${-x} ${y} l25 -20 v${height} l-25 20Z`} fill="#465c60" stroke="#799491" strokeWidth="3"/>
 <Sprite name={front} x={x} y={y} size={width}/>
 <rect x={x} y={y} width={width} height={height} fill="none" stroke="#abc6b6" strokeWidth="3"/>
 <circle cx={-x-11} cy={y+11} r="5" fill={active?'#9ce8b7':'#657776'} opacity={active?.7+Math.sin(t*4)*.3:1}/>
 <rect x={x+12} y={height/2+12} width={width-24} height="7" rx="3" fill="#132832"/>
 <rect x={x+12} y={height/2+12} width={active?20+(t*35%(width-44)):0} height="7" rx="3" fill="#80d8c8"/>
 </>;
};
