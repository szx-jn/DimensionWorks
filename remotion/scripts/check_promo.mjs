// Requested animation audit, plus source references and measured speech timing.
import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import ts from 'typescript';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=relative=>fs.readFileSync(path.join(root,relative),'utf8');
const shots=JSON.parse(read('src/promo/shots.json'));
const captions=JSON.parse(read('src/promo/captions.json'));
const catalog=JSON.parse(read('public/promo/3d/models.json'));
const audio=JSON.parse(read('public/promo/audio/manifest.json'));
const errors=[];const checked=new Set();let interpolations=0;let springs=0;

function checkSource(relative){
  if(checked.has(relative))return;
  checked.add(relative);
  const source=read(relative);
  const tree=ts.createSourceFile(relative,source,ts.ScriptTarget.Latest,true,relative.endsWith('tsx')?ts.ScriptKind.TSX:ts.ScriptKind.TS);
  const fail=(node,message)=>{const line=tree.getLineAndCharacterOfPosition(node.getStart()).line+1;errors.push(`${relative}:${line} ${message}`);};
  function visit(node){
    if(ts.isImportDeclaration(node)&&ts.isStringLiteral(node.moduleSpecifier)){
      const spec=node.moduleSpecifier.text;
      if(spec.startsWith('.')&&!spec.endsWith('.json')){
        const stem=path.join(path.dirname(relative),spec);
        const target=['.ts','.tsx'].map(ext=>stem+ext).find(file=>fs.existsSync(path.join(root,file)));
        if(target)checkSource(target);
      }
    }
    if(ts.isCallExpression(node)&&ts.isIdentifier(node.expression)){
      const name=node.expression.text;
      if(name==='interpolate'){
        interpolations++;
        const options=node.arguments[3];
        if(!options||!ts.isObjectLiteralExpression(options))fail(node,'interpolate needs an explicit options object');
        else{
          const values=new Map(options.properties.map(p=>[p.name?.getText(tree).replaceAll("'",'').replaceAll('"',''),p]));
          if(!values.has('easing'))fail(node,'missing easing');
          for(const side of ['extrapolateLeft','extrapolateRight']){
            const property=values.get(side);
            if(!property||!ts.isPropertyAssignment(property)||property.initializer.getText(tree).replaceAll("'",'').replaceAll('"','')!=='clamp')fail(node,`${side} must be clamp`);
          }
        }
      }
      if(name==='spring'){
        springs++;
        const options=node.arguments[0];
        const config=options&&ts.isObjectLiteralExpression(options)?options.properties.find(p=>p.name?.getText(tree)==='config'):null;
        if(!config||!ts.isPropertyAssignment(config)||!ts.isObjectLiteralExpression(config.initializer))fail(node,'spring needs an explicit config');
        else for(const key of ['damping','stiffness','mass'])if(!config.initializer.properties.some(p=>p.name?.getText(tree)===key))fail(node,`spring missing ${key}`);
      }
      if(['useFrame','setInterval','requestAnimationFrame'].includes(name))fail(node,'animation must derive from the Remotion frame');
    }
    if(ts.isJsxAttribute(node)&&node.name.getText(tree)==='name'&&node.initializer&&ts.isStringLiteral(node.initializer)){
      const parent=node.parent.parent;
      if(parent.tagName?.getText(tree)==='Model'&&!catalog.models[node.initializer.text])fail(node,`unbaked model ${node.initializer.text}`);
    }
    ts.forEachChild(node,visit);
  }
  visit(tree);
}
checkSource('src/promo/Promo.tsx');
assert.equal(shots.length,60);
let cursor=0;
for(const [index,shot] of shots.entries()){
  assert.equal(shot.id,index);assert.equal(shot.start,cursor);assert.ok(shot.duration>=3);
  assert.equal(shot.duration*30,Math.round(shot.duration*30));cursor+=shot.duration;
}
assert.equal(cursor,360);assert.ok(new Set(shots.map(s=>s.duration)).size>6,'Shot durations must vary');

const promo=read('src/promo/Promo.tsx');
const sequencePattern=/<Sequence from=\{(\d+)\} durationInFrames=\{(\d+)\}[^>]*><PromoScene shotId=\{(\d+)\}/g;
const sequences=[...promo.matchAll(sequencePattern)];
assert.equal(sequences.length,60);
for(const match of sequences){const shot=shots[Number(match[3])];assert.equal(Number(match[1]),shot.start*30);assert.equal(Number(match[2]),shot.duration*30);}
assert.ok(promo.includes("promo/audio/mix.wav"),'Only the neural mix should be used');
const registrations=read('src/Root.tsx');
for(const shot of shots){
  const pattern=new RegExp(`id="PromoScene${String(shot.id).padStart(2,'0')}"[^\\n]*durationInFrames=\\{${shot.duration*30}\\}`);
  assert.ok(pattern.test(registrations),`Registered shot ${shot.id} has the wrong duration`);
}

const motion=read('src/promo/motion.ts');
const constant=name=>Number(motion.match(new RegExp(`export const ${name} = (\\d+)`))[1]);
assert.ok(constant('STAGGER')>=3&&constant('STAGGER')<=6);
assert.ok(constant('ENTER_FRAMES')>=18&&constant('ENTER_FRAMES')<=24);
assert.ok(constant('EXIT_FRAMES')<constant('ENTER_FRAMES'));
assert.ok(constant('EXIT_FRAMES')>=8&&constant('EXIT_FRAMES')<=12);
assert.equal(constant('TRACE_INTERVAL'),53);
const planSource=read('src/promo/scene-plan.ts').replace("import shots from './shots.json';",`const shots=${JSON.stringify(shots)};`);
const runtime=ts.transpileModule(planSource,{compilerOptions:{module:ts.ModuleKind.ESNext,target:ts.ScriptTarget.ES2022}}).outputText;
const {scenePlans,shotEvents}=await import(`data:text/javascript;base64,${Buffer.from(runtime).toString('base64')}`);
let maxGap=0;
for(const shot of shots){
  const plan=scenePlans[shot.id];assert.ok(plan.layout!=='left-right');
  assert.ok(plan.accent+6<shot.duration*30-10);
  const events=[...shotEvents(shot.id),shot.duration*30];
  for(let i=1;i<events.length;i++)maxGap=Math.max(maxGap,events[i]-events[i-1]);
}
assert.ok(maxGap<=90,`No-new-element gap ${maxGap} exceeds 90 frames`);
assert.ok([28,56,84].every(f=>shotEvents(0).includes(f)),'Three motif changes must occur before 3 seconds');
assert.ok(read('src/promo/PromoScene.tsx').includes('grain.png'));
assert.ok(read('src/promo/PromoScene.tsx').includes('radial-gradient(ellipse at 50% 45%'));
assert.ok(read('src/promo/PromoScene.tsx').includes('<ThreeCanvas width={width} height={height}'));

let lastEnd=0;
for(const cue of captions){
  const shot=shots[cue.shotId];assert.ok(shot);assert.ok(cue.endMs>cue.startMs);assert.ok(cue.startMs>=lastEnd);
  assert.ok(cue.startMs>=shot.start*1000);assert.ok(cue.endMs<=(shot.start+shot.duration)*1000);
  lastEnd=cue.endMs;
}
assert.equal(audio.length,59);
for(const clip of audio){
  assert.equal(clip.voice,'zh-CN-YunxiNeural');assert.ok(clip.start+clip.duration<=shots[clip.id].start+shots[clip.id].duration);
}
for(const [name,model] of Object.entries(catalog.models))for(const group of model.groups){
  assert.equal(group.positions.length/3,group.uv.length/2,`Invalid UV geometry in ${name}`);
  assert.ok(group.positions.every(Number.isFinite));
  assert.ok(fs.existsSync(path.join(root,'public',catalog.textures[group.texture].file)));
}
assert.equal(errors.length,0,errors.join('\n'));
const report={checkedFiles:[...checked],interpolations,springs,scenes:60,totalFrames:10800,
  distinctDurations:[...new Set(shots.map(s=>s.duration))].sort((a,b)=>a-b),maxVisualEventGap:maxGap,
  staggerFrames:constant('STAGGER'),enterFrames:constant('ENTER_FRAMES'),exitFrames:constant('EXIT_FRAMES'),
  motifChanges:[28,56,84],models:Object.keys(catalog.models).length,textures:Object.keys(catalog.textures).length,
  captions:captions.length,voice:'zh-CN-YunxiNeural',result:'passed'};
fs.mkdirSync(path.join(root,'out/qa'),{recursive:true});
fs.writeFileSync(path.join(root,'out/qa/static-audit.json'),JSON.stringify(report,null,2)+'\n');
console.log(`Passed: ${checked.size} active source files; all easing/clamps and spring parameters; 60 retimed shots; maximum visual-event gap ${maxGap} frames; actual assets and measured narration.`);
