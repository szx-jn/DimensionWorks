#!/usr/bin/env python3
"""Draw drive/GUI textures and extract approved 64x64 cards (requires Pillow)."""
from collections import deque
from pathlib import Path
import json
import struct
import zlib
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSET = ROOT / "mods/dimensionworks-mek-stress/src/main/resources/assets/dimensionworks_mek_stress"

class Canvas:
    def __init__(self, w=16, h=16, color=(0, 0, 0, 0)):
        self.w, self.h = w, h
        self.p = [color] * (w * h)
    def px(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h: self.p[y*self.w+x] = (*c, 255) if len(c)==3 else c
    def rect(self, x1, y1, x2, y2, c):
        for y in range(y1, y2+1):
            for x in range(x1, x2+1): self.px(x,y,c)
    def save(self, path):
        path.parent.mkdir(parents=True, exist_ok=True)
        raw = b"".join(b"\0" + bytes(v for px in self.p[y*self.w:(y+1)*self.w] for v in px) for y in range(self.h))
        def chunk(tag, data): return struct.pack(">I",len(data))+tag+data+struct.pack(">I",zlib.crc32(tag+data)&0xffffffff)
        path.write_bytes(b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">2I5B",self.w,self.h,8,6,0,0,0))+chunk(b"IDAT",zlib.compress(raw,9))+chunk(b"IEND",b""))

NAVY=(18,31,50); EDGE=(37,57,79); SHADOW=(48,64,86); METAL=(91,105,128); LIGHT=(135,148,169); HI=(172,183,197); CYAN=(40,210,235); GLOW=(113,244,250)
DDR_ACCENTS=[(53,130,238),(45,177,207),(54,205,190),(58,220,219),(97,232,230)]

def drive(level):
    c=Canvas(color=(*NAVY,255)); a=DDR_ACCENTS[level-1]
    # AE2-like metal chassis with ten narrow cell windows; the BE renderer
    # overlays the type-colored light only when a card is installed.
    c.rect(2,0,13,0,NAVY); c.rect(1,1,14,1,EDGE); c.rect(0,2,15,13,EDGE)
    c.rect(1,2,14,2,METAL); c.rect(1,3,14,12,SHADOW); c.rect(2,3,13,11,METAL)
    c.rect(2,3,13,3,LIGHT); c.rect(2,11,13,12,SHADOW); c.rect(1,13,14,14,NAVY); c.rect(2,14,13,14,EDGE); c.rect(3,15,12,15,NAVY)
    c.rect(4,3,11,12,NAVY); c.rect(5,4,10,11,EDGE)
    for row in range(5):
        y=4+row*2
        for x in (5,9):
            c.rect(x,y,x+1,y,SHADOW)
    c.rect(6,1,9,1,NAVY); c.rect(7,1,8,1,a)
    for x in (1,14): c.px(x,4,a); c.px(x,11,a)
    return c

def drive_gui():
    c=Canvas(176,215,(198,198,198,255))
    # Classic AE2 bevel frame and header strip.
    c.rect(0,0,175,0,(255,255,255)); c.rect(0,0,0,214,(255,255,255))
    c.rect(175,1,175,214,(55,55,55)); c.rect(1,214,174,214,(55,55,55))
    c.rect(2,2,173,2,(151,151,151)); c.rect(2,3,173,19,(190,190,190))
    for x in (139,157):
        c.rect(x,3,x+16,19,(55,55,55)); c.rect(x+1,4,x+15,18,(139,139,139))
        c.rect(x+1,4,x+14,5,(255,255,255)); c.rect(x+1,6,x+2,17,(210,210,210))
    c.rect(3,20,172,20,(85,85,85)); c.rect(3,21,172,21,(255,255,255))
    # Main storage bay and compact network status panel.
    c.rect(4,23,83,121,(139,139,139)); c.rect(5,24,82,120,(198,198,198))
    c.rect(86,23,171,121,(139,139,139)); c.rect(87,24,170,120,(198,198,198))
    # Six matching information wells in two equal-width columns, plus a full-width status well.
    for x in (89,130):
        for y in (25,50,75):
            c.rect(x,y,x+38,y+22,(85,85,85)); c.rect(x+1,y+1,x+37,y+21,(174,174,174))
            c.rect(x+1,y+1,x+36,y+2,(224,224,224)); c.rect(x+1,y+3,x+2,y+20,(205,205,205))
    c.rect(89,100,168,117,(85,85,85)); c.rect(90,101,167,116,(174,174,174))
    c.rect(90,101,166,102,(224,224,224)); c.rect(90,103,91,115,(205,205,205))
    # Four by five storage slots, with the same 18px pitch as the menu.
    for row in range(5):
        for col in range(4):
            x=8+col*18; y=26+row*18
            draw_slot(c,x,y)
    # Inventory and hotbar bays, including all 36 vanilla slot wells.
    c.rect(4,130,171,212,(139,139,139)); c.rect(5,131,170,211,(198,198,198))
    c.rect(4,130,171,130,(85,85,85)); c.rect(5,131,170,131,(255,255,255))
    for row in range(3):
        for col in range(9):
            x=8+col*18; y=133+row*18
            draw_slot(c,x,y)
    for col in range(9):
        x=8+col*18; y=193
        draw_slot(c,x,y)
    return c

def draw_slot(c, x, y):
    # The item and click target occupy exactly x..x+15, y..y+15.
    c.rect(x-1,y-1,x+16,y+16,(55,55,55))
    c.rect(x+16,y,x+16,y+16,(255,255,255))
    c.rect(x,y+16,x+15,y+16,(255,255,255))
    c.rect(x,y,x+15,y+15,(139,139,139))

def extract_card(concept, x):
    """Remove only background connected to crop edges, before adding transparent padding."""
    crop=concept.crop((x,495,x+220,795)).convert("RGBA")
    w,h=crop.size; pixels=crop.load(); background=set(); queue=deque()
    def background_pixel(x,y):
        r,g,b,a=pixels[x,y]
        return a == 0 or (min(r,g,b)>125 and max(r,g,b)-min(r,g,b)<48)
    def enqueue(x,y):
        if 0<=x<w and 0<=y<h and (x,y) not in background and background_pixel(x,y):
            background.add((x,y)); queue.append((x,y))
    for x0 in range(w): enqueue(x0,0); enqueue(x0,h-1)
    for y0 in range(h): enqueue(0,y0); enqueue(w-1,y0)
    while queue:
        x0,y0=queue.popleft()
        for dx,dy in ((-1,0),(1,0),(0,-1),(0,1)): enqueue(x0+dx,y0+dy)
    for x0,y0 in background: pixels[x0,y0]=(0,0,0,0)
    # Discard isolated fragments of the baked checkerboard outside the card.
    remaining={(x0,y0) for y0 in range(h) for x0 in range(w) if pixels[x0,y0][3]}
    largest=set()
    while remaining:
        seed=remaining.pop(); component={seed}; queue=deque([seed])
        while queue:
            x0,y0=queue.popleft()
            for dx,dy in ((-1,0),(1,0),(0,-1),(0,1)):
                point=(x0+dx,y0+dy)
                if point in remaining:
                    remaining.remove(point); component.add(point); queue.append(point)
        if len(component)>len(largest): largest=component
    for y0 in range(h):
        for x0 in range(w):
            if (x0,y0) not in largest: pixels[x0,y0]=(0,0,0,0)
    crop=crop.crop(crop.getbbox())
    crop.thumbnail((48,60),Image.Resampling.LANCZOS)
    result=Image.new("RGBA",(64,64))
    result.alpha_composite(crop,((64-crop.width)//2,(64-crop.height)//2))
    return result

def main():
    block=ASSET/"textures/block"; item=ASSET/"textures/item"; models=ASSET/"models"
    # The accepted concept image is RGB with a checkerboard baked into the background.
    # Crop the original card renders directly, then key only near-neutral bright pixels.
    concept=ROOT/"scripts/assets/memory_cards_reference.png"
    if not concept.exists(): raise SystemExit(f"Missing approved concept image: {concept}")
    reference=Image.open(concept)
    card_x={"defective":80,"economy":360,"balanced":640,"high_speed":920,"low_loss":1200,"final":1480}
    cards={kind:extract_card(reference,x) for kind,x in card_x.items()}
    for d in range(1,6):
        drive(d).save(block/f"memory_drive_ddr{d}_front.png")
        # Minimal, subdued side/top/bottom faces consistent with the approved front.
        for face, base in (("side",(65,79,100)),("top",(98,111,132)),("bottom",(39,53,73))):
            c=Canvas(); c.rect(0,0,15,15,base); c.rect(0,0,15,0,NAVY); c.rect(0,15,15,15,NAVY)
            if face=="side": c.rect(2,3,13,12,(77,91,111)); c.rect(4,6,11,9,(50,65,84)); c.px(7,7,DDR_ACCENTS[d-1])
            elif face=="top": c.rect(2,2,13,13,(107,120,141)); c.rect(5,6,10,9,NAVY); c.rect(7,7,8,8,DDR_ACCENTS[d-1])
            else: c.rect(2,2,13,13,(48,62,81)); c.rect(3,3,12,12,(58,72,91))
            c.save(block/f"memory_drive_ddr{d}_{face}.png")
        model={"parent":"minecraft:block/block","textures":{"particle":f"dimensionworks_mek_stress:block/memory_drive_ddr{d}_side","front":f"dimensionworks_mek_stress:block/memory_drive_ddr{d}_front","side":f"dimensionworks_mek_stress:block/memory_drive_ddr{d}_side","top":f"dimensionworks_mek_stress:block/memory_drive_ddr{d}_top","bottom":f"dimensionworks_mek_stress:block/memory_drive_ddr{d}_bottom"},"elements":[{"from":[0,0,0],"to":[16,16,16],"faces":{"north":{"texture":"#front"},"south":{"texture":"#side"},"east":{"texture":"#side"},"west":{"texture":"#side"},"up":{"texture":"#top"},"down":{"texture":"#bottom"}}}]}
        (models/"block"/f"memory_drive_ddr{d}.json").write_text(json.dumps(model,indent=2)+"\n")
        (models/"item"/f"memory_drive_ddr{d}.json").write_text(json.dumps({"parent":f"dimensionworks_mek_stress:block/memory_drive_ddr{d}"},indent=2)+"\n")
        for kind, x in card_x.items():
            target=item/f"memory_card_ddr{d}_{kind}.png"
            image=cards[kind].copy()
            # A tiny generation marker stays inside the card, never in its transparent margin.
            for px in (31,32): image.putpixel((px,4),(*DDR_ACCENTS[d-1],255))
            image.save(target)
            (models/"item"/f"memory_card_ddr{d}_{kind}.json").write_text(json.dumps({"parent":"minecraft:item/generated","textures":{"layer0":f"dimensionworks_mek_stress:item/memory_card_ddr{d}_{kind}"}},indent=2)+"\n")
    drive_gui().save(ASSET/"textures/gui/memory_drive.png")
    print("Cropped 30 combined 64x64 memory-card textures from the approved concept and drew 20 16x16 drive-face textures; updated 35 item/block models.")

if __name__ == "__main__": main()
