// One browser, one render worker. Speech is prepared separately.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {bundle} from '@remotion/bundler';
import {openBrowser, renderMedia, renderStill, selectComposition} from '@remotion/renderer';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const shots = JSON.parse(fs.readFileSync(path.join(root, 'src/promo/shots.json'), 'utf8'));
const mode = process.argv[2] ?? 'qa';
if (!['qa', 'render', 'sample'].includes(mode)) throw new Error('Use qa, sample or render');
const chromiumOptions = {gl: 'angle'};
const serveUrl = await bundle({entryPoint: path.join(root, 'src/index.ts'),
  outDir: path.join(root, 'out/promo-3d-bundle'), enableCaching: true});
const browser = await openBrowser('chrome', {chromiumOptions, logLevel: 'error'});
try {
  const composition = await selectComposition({serveUrl, id: 'DimensionWorksPromo',
    puppeteerInstance: browser, chromiumOptions, logLevel: 'error'});
  if (composition.durationInFrames !== 10800 || composition.fps !== 30) throw new Error('Unexpected film timeline');
  if (mode === 'qa') {
    const out = path.join(root, 'out/qa'); fs.mkdirSync(out, {recursive: true});
    const selection=process.argv[3]?process.argv[3].split(',').map(Number):shots.map(s=>s.id);
    const frames = selection.includes(0)?[1,15,30,45,60,85,89].map(frame => ({frame, shotId:0, file:`intro-${String(frame).padStart(3,'0')}.png`})):[];
    for (const shot of shots.filter(s=>selection.includes(s.id))) {
      const local = Math.round(shot.duration * 30 * (shot.id === 22 ? .8 : .65));
      frames.push({frame: Math.round(shot.start*30)+local, shotId:shot.id,
        file:`shot-${String(shot.id).padStart(2,'0')}.png`});
    }
    for (const [index, item] of frames.entries()) {
      await renderStill({serveUrl, composition, frame:item.frame, output:path.join(out,item.file),
        puppeteerInstance:browser, chromiumOptions, scale:.5, imageFormat:'png', logLevel:'error',
        timeoutInMilliseconds:45000});
      console.log(`QA ${index+1}/${frames.length}: ${item.file} (frame ${item.frame})`);
    }
    fs.writeFileSync(path.join(out,'frames.json'),JSON.stringify(frames,null,2)+'\n');
  } else {
    const output = process.argv[3] ? path.resolve(process.argv[3])
      : path.join(root, 'out', mode==='sample'?'promo-3d-sample.mp4':'dimensionworks-promo-3d.mp4');
    fs.mkdirSync(path.dirname(output), {recursive:true});
    let lastReport=0;
    const started=Date.now();
    const result=await renderMedia({serveUrl, composition, outputLocation:output, codec:'h264', crf:18,
      pixelFormat:'yuv420p', audioCodec:'aac', audioBitrate:'192k', imageFormat:'jpeg', jpegQuality:95,
      concurrency:1, puppeteerInstance:browser, chromiumOptions, logLevel:'error', x264Preset:'veryfast',
      timeoutInMilliseconds:60000, offthreadVideoCacheSizeInBytes:64*1024*1024, offthreadVideoThreads:1,
      frameRange:mode==='sample'?[0,119]:undefined,
      ffmpegOverride:({args})=>[...args.slice(0,-1),'-threads','2',args[args.length-1]],
      onProgress:progress=>{
        if(Date.now()-lastReport>10000||progress.progress===1){
          lastReport=Date.now();
          console.log(`Render ${progress.renderedFrames}/${mode==='sample'?120:10800}; encoded ${progress.encodedFrames}; ${(progress.progress*100).toFixed(1)}%; elapsed ${Math.round((Date.now()-started)/1000)}s`);
        }
      },
      metadata:{title:'维度工序 · 3D宣传片',comment:'Actual Minecraft models; frame-driven 3D; neural Mandarin narration'},
    });
    console.log(`Completed: ${output}`);
    fs.writeFileSync(path.join(root,'out',mode==='sample'?'sample-render.json':'full-render.json'),
      JSON.stringify({output,elapsedSeconds:(Date.now()-started)/1000,concurrency:1,slowestFrames:result.slowestFrames},null,2)+'\n');
  }
} finally {
  await browser.close({silent:true});
}
