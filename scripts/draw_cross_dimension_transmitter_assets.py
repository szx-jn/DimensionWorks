#!/usr/bin/env python3
"""Draw compact 16x16 Create/Mekanism-style channel transmitter textures."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / "mods/dimensionworks-mek-stress/src/main/resources/assets/dimensionworks_mek_stress"

def texture(tier: int) -> Image.Image:
    image = Image.new("RGBA", (16, 16), (28, 37, 42, 255))
    draw = ImageDraw.Draw(image)
    dark = (35, 45, 49, 255)
    steel = (91, 105, 108, 255)
    light = (157, 164, 151, 255)
    brass = (193, 139, 54, 255)
    brass_hi = (236, 193, 89, 255)
    glass = (29, 73, 85, 255)
    glow = (96, 213, 209, 255)

    draw.rectangle((0, 0, 15, 15), fill=dark)
    draw.rectangle((1, 1, 14, 14), fill=steel)
    draw.rectangle((2, 2, 13, 13), fill=dark)
    # Opposed shaft couplers visibly tie the device to Create's mechanical network.
    draw.rectangle((0, 6, 3, 9), fill=brass)
    draw.rectangle((12, 6, 15, 9), fill=brass)
    draw.rectangle((0, 7, 2, 8), fill=brass_hi)
    draw.rectangle((13, 7, 15, 8), fill=brass_hi)
    # Reinforced face plate and corner rivets.
    draw.rectangle((3, 3, 12, 12), fill=steel)
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        draw.point((x, y), fill=light)
    draw.rectangle((4, 4, 11, 11), fill=glass)
    draw.line((4, 5, 11, 5), fill=(59, 119, 126, 255))
    # Tier identity comes from added coil/gear structures as well as color.
    draw.rectangle((5, 6, 10, 10), fill=(43, 59, 62, 255))
    if tier == 1:
        draw.rectangle((7, 6, 8, 10), fill=brass)
        draw.point((7, 7), fill=brass_hi)
        draw.rectangle((5, 11, 10, 11), fill=glow)
    elif tier == 2:
        draw.rectangle((5, 6, 6, 10), fill=brass)
        draw.rectangle((9, 6, 10, 10), fill=brass)
        draw.rectangle((7, 6, 8, 10), fill=brass_hi)
        draw.rectangle((5, 11, 6, 11), fill=glow)
        draw.rectangle((9, 11, 10, 11), fill=glow)
    else:
        draw.rectangle((5, 6, 6, 10), fill=brass_hi)
        draw.rectangle((7, 6, 8, 10), fill=glow)
        draw.rectangle((9, 6, 10, 10), fill=brass_hi)
        draw.rectangle((4, 11, 11, 11), fill=glow)
        draw.rectangle((6, 4, 9, 4), fill=brass_hi)
    draw.rectangle((5, 2, 10, 2), fill=brass)
    draw.rectangle((6, 1, 9, 1), fill=brass_hi)
    return image

for tier in range(1, 4):
    texture(tier).save(ROOT / f"textures/block/cross_dimension_transmitter_tier{tier}.png")

unpacker = Image.new("RGBA", (16, 16), (29, 36, 38, 255))
d = ImageDraw.Draw(unpacker)
d.rectangle((1, 1, 14, 14), fill=(86, 99, 98, 255))
d.rectangle((2, 2, 13, 13), fill=(35, 46, 48, 255))
d.rectangle((3, 3, 12, 12), fill=(177, 128, 48, 255))
d.rectangle((4, 4, 11, 11), fill=(45, 71, 74, 255))
d.rectangle((5, 5, 10, 10), fill=(105, 195, 184, 255))
d.rectangle((6, 6, 9, 9), fill=(29, 60, 68, 255))
d.point((7, 7), fill=(231, 199, 105, 255))
for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
    d.point((x, y), fill=(212, 179, 91, 255))
unpacker.save(ROOT / "textures/block/matter_ball_unpacker.png")

ball = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(ball)
d.ellipse((2, 2, 13, 13), fill=(94, 70, 54, 255), outline=(38, 34, 31, 255))
d.arc((2, 2, 13, 13), 205, 35, fill=(191, 137, 62, 255), width=2)
d.line((5, 5, 10, 10), fill=(217, 186, 103, 255), width=2)
d.line((10, 5, 5, 10), fill=(87, 187, 178, 255), width=2)
d.rectangle((7, 7, 8, 8), fill=(222, 238, 177, 255))
ball.save(ROOT / "textures/item/matter_ball.png")

for kind, primary in (("item", (210, 152, 58, 255)), ("fluid", (63, 163, 197, 255))):
    card = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(card)
    d.polygon(((3, 1), (12, 1), (15, 4), (15, 12), (12, 15), (3, 15), (1, 12), (1, 4)),
              fill=(48, 57, 59, 255), outline=(22, 28, 30, 255))
    d.rectangle((3, 3, 12, 12), fill=primary)
    d.rectangle((4, 4, 11, 11), fill=(37, 52, 57, 255))
    if kind == "item":
        d.rectangle((5, 5, 10, 10), fill=(179, 126, 49, 255))
        d.rectangle((6, 6, 9, 9), fill=(232, 199, 110, 255))
        d.line((3, 13, 12, 13), fill=(243, 214, 137, 255))
    else:
        d.polygon(((8, 4), (11, 8), (10, 11), (8, 12), (6, 11), (5, 8)), fill=(75, 199, 224, 255))
        d.point((8, 7), fill=(205, 244, 242, 255))
        d.line((3, 13, 12, 13), fill=(160, 226, 238, 255))
    card.save(ROOT / f"textures/item/{kind}_capacity_upgrade.png")
