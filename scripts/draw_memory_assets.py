#!/usr/bin/env python3
"""Draw 16x16 pixel textures for the ME memory drives and cards."""
from pathlib import Path
import json
import struct
import subprocess
import zlib

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
TYPES={
 "defective":((102,111,125),(151,159,169)), "economy":((16,143,121),(52,210,177)),
 "balanced":((21,153,204),(49,218,239)), "high_speed":((191,34,156),(246,64,198)),
 "high_storage":((113,54,201),(177,105,249)), "final":((226,162,40),(255,222,100))}

def drive(level):
    c=Canvas(); a=DDR_ACCENTS[level-1]
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
    c.rect(0,0,175,0,(255,255,255)); c.rect(0,0,0,198,(255,255,255))
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
            c.rect(x-1,y-1,x+16,y+16,(55,55,55))
            c.rect(x,y,x+15,y+15,(139,139,139))
            c.rect(x,y,x+14,y+1,(255,255,255)); c.rect(x,y+2,x+1,y+14,(210,210,210))
            c.rect(x+2,y+2,x+14,y+14,(139,139,139))
    # Inventory and hotbar bays, including all 36 vanilla slot wells.
    c.rect(4,130,171,212,(139,139,139)); c.rect(5,131,170,211,(198,198,198))
    c.rect(4,130,171,130,(85,85,85)); c.rect(5,131,170,131,(255,255,255))
    for row in range(3):
        for col in range(9):
            x=8+col*18; y=133+row*18
            c.rect(x-1,y-1,x+16,y+16,(55,55,55)); c.rect(x,y,x+15,y+15,(139,139,139))
            c.rect(x,y,x+14,y+1,(255,255,255)); c.rect(x,y+2,x+1,y+14,(210,210,210)); c.rect(x+2,y+2,x+14,y+14,(139,139,139))
    for col in range(9):
        x=8+col*18; y=193
        c.rect(x-1,y-1,x+16,y+16,(55,55,55)); c.rect(x,y,x+15,y+15,(139,139,139))
        c.rect(x,y,x+14,y+1,(255,255,255)); c.rect(x,y+2,x+1,y+14,(210,210,210)); c.rect(x+2,y+2,x+14,y+14,(139,139,139))
    return c

def card(ddr, kind):
    c=Canvas(); accent,chip=TYPES[kind]; da=DDR_ACCENTS[ddr-1]
    # Card silhouette and silver bevel; proportions follow the selected image.
    c.rect(3,0,12,0,NAVY); c.rect(2,1,13,1,NAVY); c.rect(1,2,14,12,NAVY)
    c.rect(2,2,13,11,SHADOW); c.rect(3,2,12,2,METAL); c.rect(2,3,2,10,METAL); c.rect(13,3,13,10,EDGE)
    c.rect(3,3,12,10,(24,43,61)); c.rect(4,3,11,3,EDGE)
    # card family is carried by corner circuitry / rails, DDR generation by a tiny top edge marker.
    c.rect(4,4,4,5,accent); c.rect(11,4,11,5,accent); c.rect(4,8,4,9,accent); c.rect(11,8,11,9,accent)
    c.rect(7,2,8,2,da)
    # colored traces, intentionally sparse at 16px resolution
    c.px(5,5,accent); c.px(5,6,accent); c.px(6,6,accent); c.px(10,5,accent); c.px(10,6,accent); c.px(9,6,accent)
    # central chip and metallic socket
    c.rect(5,6,10,9,EDGE); c.rect(6,6,9,9,METAL); c.rect(6,7,9,8,chip); c.px(6,7,GLOW)
    # five gold contacts along the short edge
    c.rect(3,12,12,12,NAVY)
    for x in (3,5,7,9,11):
        c.rect(x,13,x+1,14,(224,164,61)); c.px(x,13,(255,213,105))
    c.rect(4,15,11,15,NAVY)
    if kind == "defective":
        c.px(9,7,(72,76,82)); c.px(10,8,(72,76,82)); c.px(5,4,(84,89,97))
    if kind == "economy": c.px(8,5,accent)
    if kind == "high_speed": c.rect(5,3,5,4,accent); c.rect(10,3,10,4,accent)
    if kind == "high_storage": c.rect(6,5,9,5,accent)
    if kind == "final":
        for x in (4,11): c.px(x,3,(255,211,91))
    return c

def main():
    block=ASSET/"textures/block"; item=ASSET/"textures/item"; models=ASSET/"models"
    # The accepted concept image is RGB with a checkerboard baked into the background.
    # Crop the original card renders directly, then key only near-neutral bright pixels.
    concept=Path("/Users/szx/.codex/generated_images/01a18d74-3b7e-7900-b9c8-43c4269efd2a/exec-d2077738-197a-4bd0-b043-76e553acec95.png")
    if not concept.exists(): raise SystemExit(f"Missing approved concept image: {concept}")
    card_x={"defective":80,"economy":360,"balanced":640,"high_speed":920,"high_storage":1200,"final":1480}
    for d in range(1,6):
        drive(d).save(block/f"memory_drive_ddr{d}_front.png")
        # Minimal, subdued side/top/bottom faces consistent with the approved front.
        for face, base in (("side",(65,79,100)),("top",(98,111,132)),("bottom",(39,53,73))):
            c=Canvas(); c.rect(0,0,15,15,base); c.rect(0,0,15,0,NAVY); c.rect(0,15,15,15,NAVY)
            if face=="side": c.rect(2,3,13,12,(77,91,111)); c.rect(4,6,11,9,(50,65,84)); c.px(7,7,DDR_ACCENTS[d-1])
            elif face=="top": c.rect(2,2,13,13,(107,120,141)); c.rect(5,6,10,9,NAVY); c.rect(7,7,8,8,DDR_ACCENTS[d-1])
            else: c.rect(2,2,13,13,(48,62,81)); c.rect(3,3,12,12,(58,72,91))
            c.save(block/f"memory_drive_ddr{d}_{face}.png")
        model={"parent":"minecraft:block/block","textures":{"particle":f"dimensionworks_mek_stress:block/memory_drive_ddr{d}_side","front":f"dimensionworks_mek_stress:block/memory_drive_ddr{d}_front","side":f"dimensionworks_mek_stress:block/memory_drive_ddr{d}_side","top":f"dimensionworks_mek_stress:block/memory_drive_ddr{d}_top","bottom":f"dimensionworks_mek_stress:block/memory_drive_ddr{d}_bottom"},"elements":[{"from":[1,0,1],"to":[15,16,15],"faces":{"north":{"texture":"#front"},"south":{"texture":"#side"},"east":{"texture":"#side"},"west":{"texture":"#side"},"up":{"texture":"#top"},"down":{"texture":"#bottom"}}}]}
        (models/"block"/f"memory_drive_ddr{d}.json").write_text(json.dumps(model,indent=2)+"\n")
        (models/"item"/f"memory_drive_ddr{d}.json").write_text(json.dumps({"parent":f"dimensionworks_mek_stress:block/memory_drive_ddr{d}"},indent=2)+"\n")
        for kind, x in card_x.items():
            target=item/f"memory_card_ddr{d}_{kind}.png"
            # Alpha key uses high brightness plus low saturation, preserving the colored
            # chip, dark body, gold contacts, and most silver bevel pixels.
            subprocess.run(["magick",str(concept),"-crop",f"220x300+{x}+495","+repage","-resize","48x64!","-background","none","-gravity","center","-extent","64x64","(","+clone","-fx",'(((r+g+b)/3)>0.68 && (max(r,max(g,b))-min(r,min(g,b)))<0.18)?0:1',")","-compose","CopyOpacity","-composite","-fill",f"rgb({DDR_ACCENTS[d-1][0]},{DDR_ACCENTS[d-1][1]},{DDR_ACCENTS[d-1][2]})","-draw","rectangle 31,2 32,3",str(target)],check=True)
            (models/"item"/f"memory_card_ddr{d}_{kind}.json").write_text(json.dumps({"parent":"minecraft:item/generated","textures":{"layer0":f"dimensionworks_mek_stress:item/memory_card_ddr{d}_{kind}"}},indent=2)+"\n")
    drive_gui().save(ASSET/"textures/gui/memory_drive.png")
    print("Cropped 30 combined 64x64 memory-card textures from the approved concept and drew 20 16x16 drive-face textures; updated 35 item/block models.")

if __name__ == "__main__": main()
