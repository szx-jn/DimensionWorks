#!/usr/bin/env python3
"""Original offline score and Mandarin narration for the six-minute film.

Uses macOS Tingting, FFmpeg, and Python's standard library. Outputs are generated
assets, not checked-in binaries. Caption cues use measured sentence durations.
"""
from pathlib import Path
import subprocess,json,wave,array,math,random
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'public/promo/audio'
OUT.mkdir(parents=True,exist_ok=True)
shots=json.loads((ROOT/'src/promo/shots.json').read_text())
short={4:'这是维度工序。带你看看，我们改变了什么。',7:'处理次数、合成并发、活跃设备和指定产出，也有额度。',10:'我们希望进步来自工程设计，让玩家自由组织产线。',11:'合格的Mek设备，现在可以直接连接机械动力。',12:'发电机保留运行条件，把实际产能转换为应力。',13:'大型能源同样接轴，龙堆的启动与护盾仍需认真处理。',14:'部分机器超速后可以并行加工，应力成本同步提高。',15:'工厂的等级、并行和转速，需要一起考虑。',16:'能量板改为装配件，连接金属、处理器与合金生产。',17:'数字调速和拓扑图，帮助你检查连接与限速。',21:'五代驱动器拓展卡槽与转速，每网最多两台有效。',22:'三十种卡，带来不同的转速和输入损耗取舍。',23:'自动合成避免劣质概率，旧卡还能逐级升级。',24:'应力或总转速任意超限，全部机械输出停机。',25:'铁的循环，需要有限样本、研磨、压制和结合。',27:'铜的循环连接化学还原与提纯，气体也是必要供料。',28:'下界岩先保住回流，再分配平均余量；每轮仍有波动。',30:'蛋糕底料需要红石，四种液体燃料也接入加热。',31:'二十一种熔融材料，连接熔炼、合金与冷冻回收。',32:'十二种新增化学品，与Mek原有体系共同加工。',34:'Create管道能运气体，专用盆盖控制盆内气体流失。',35:'直见天空的熔融金属，三十秒后结晶，仍能流动。',36:'不同转速的加压处理，产出三档带来源的结晶。',37:'下界合金碎片，连接了化学、结晶与序列装配。',38:'配方刻数、燃料条件与设备读数，帮助你检查产线。',40:'玩家包裹可以走传送带，再经青蛙交给锁链运输。',43:'原生跨维物流，目前只在三大原版维度之间互通。',44:'弹力包裹能蓄力发射，落地后还会弹跳。',45:'贴住竖直带面，玩家也能加入立体运输。',46:'根系接触床、树叶、水与腐殖料，共同支撑林地浸出。',47:'两只野猪参与调理，供料、工位与休息都要协调。',49:'暮色材料汇入十步装配，封带升级为弹力工具。',50:'农业、烹饪和冒险之外，女仆也能参与手摇加工。',52:'倒计时到期会死亡，期间再吃一颗会立即死亡。',53:'建筑、观察、领地、存储与更新，也有配套调整。',54:'以下是开发预览。专用端点已实现，尚无生存配方。',55:'频道库存通过云盘和物质球回收，再接回物流。',56:'天境和六洞穴仍受限制，长期生产体系继续建设。',57:'相位、模块、限时封装与稳定化，构成洞穴工艺。',58:'工业考核仍在设计，希望观察真实的工程能力。'}
SR=24000; track=array.array('h',[0])*(360*SR); cues=[]; manifest=[]
for s in shots:
 text=short.get(s['id'],s['voice'])
 if not text:continue
 # Generate sentence-level segments so caption timing follows the actual voice.
 sentences=[x for x in __import__('re').split('(?<=[。！？；])',text) if x]
 pieces=[]
 for j,part in enumerate(sentences):
  src=OUT/f'raw-{s["id"]:02d}-{j}.aiff'; dst=OUT/f'part-{s["id"]:02d}-{j}.wav'
  subprocess.run(['say','-v','Tingting','-r','220','-o',str(src),part],check=True)
  subprocess.run(['ffmpeg','-v','error','-y','-i',str(src),'-ar',str(SR),'-ac','1','-af','silenceremove=start_periods=1:start_threshold=-48dB',str(dst)],check=True)
  with wave.open(str(dst)) as w:n=w.getnframes()
  pieces.append((part,dst,n))
 total=sum(n for _,_,n in pieces)/SR; speed=max(1,total/5.55)
 offset=round((s['start']+.18)*SR)
 for j,(part,dst,n) in enumerate(pieces):
  if speed>1:
   fixed=OUT/f'fit-{s["id"]:02d}-{j}.wav'
   subprocess.run(['ffmpeg','-v','error','-y','-i',str(dst),'-af',f'atempo={speed:.6f}',str(fixed)],check=True);dst=fixed
  with wave.open(str(dst)) as w:samples=array.array('h');samples.frombytes(w.readframes(w.getnframes()))
  for k in range(min(240,len(samples)//2)):
   samples[k]=int(samples[k]*k/240);samples[-k-1]=int(samples[-k-1]*k/240)
  assert offset+len(samples)<=(s['start']+6)*SR
  track[offset:offset+len(samples)]=samples
  cues.append(dict(text=part,startMs=round(offset/SR*1000),endMs=round((offset+len(samples))/SR*1000),timestampMs=None,confidence=None))
  offset+=len(samples)
 manifest.append(dict(id=s['id'],text=text,originalDuration=round(total,3),speed=round(speed,3),end=round(offset/SR,3)))
 print(f'Narration {s["id"]+1}/60, speed {speed:.2f}',flush=True)
with wave.open(str(OUT/'narration.wav'),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(SR);w.writeframes(track.tobytes())
(ROOT/'src/promo/captions.json').write_text(json.dumps(cues,ensure_ascii=False,indent=2)+'\n')
(OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
# Quiet original ambient score: warm chords, clockwork pulse, heartbeat, wind.
rng=random.Random(2601114); music=array.array('h'); last=0
chords=[(130.81,164.81,196),(110,130.81,164.81),(87.31,130.81,174.61),(98,123.47,146.83)]
for k in range(360*SR):
 t=k/SR; region=0 if t<30 else 1 if t<234 else 2 if t<324 else 3
 chord=chords[int(t/12)%4]; fade=min(1,t/2,(360-t)/3)
 last=last*.99+(rng.random()-.5)*.01
 pad=sum(math.sin(2*math.pi*f*t)*.006+math.sin(2*math.pi*(f*2+.2)*t)*.002 for f in chord)
 beat=t%(60/84); pulse=math.exp(-beat*18)*math.sin(2*math.pi*65*t)*(.012 if region else .002)
 heart=math.exp(-(t%1.15)*35)*math.sin(2*math.pi*48*t)*.025+math.exp(-((t+.86)%1.15)*35)*math.sin(2*math.pi*55*t)*.018
 tick=math.exp(-(t%.5)*110)*math.sin(2*math.pi*1200*t)*.009 if t>=30 else 0
 local=t%6; chime=math.exp(-local*6)*math.sin(2*math.pi*660*t)*.008 if t>=30 else 0
 music.append(int(max(-1,min(1,(pad+pulse+tick+chime+(heart if t<30 or t>354 else 0)+last*.17)*fade))*32767))
with wave.open(str(OUT/'score.wav'),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(SR);w.writeframes(music.tobytes())
print('Completed narration, original score, measured captions.',flush=True)
