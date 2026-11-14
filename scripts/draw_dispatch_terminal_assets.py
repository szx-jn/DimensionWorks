#!/usr/bin/env python3
"""生成发运终端六面纹理、暮色升级卡与六洞穴像素插画，无第三方依赖。"""
import json
from pathlib import Path
import struct
import zlib
import random

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "mods/dimensionworks-transfer/src/main/resources/assets/dimensionworks_transfer"
STEEL, DARK, BRASS, GLOW = (88, 109, 120), (24, 38, 48), (208, 165, 71), (68, 222, 228)
CAVES = ("magnetic_caves", "primordial_caves", "toxic_caves", "abyssal_chasm", "forlorn_hollows", "candy_cavity")


def png(pixels) -> bytes:
    def chunk(name: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + name + data + struct.pack(">I", zlib.crc32(name + data))
    raw = b"".join(b"\0" + bytes(channel for pixel in row for channel in pixel) for row in pixels)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", len(pixels[0]), len(pixels), 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))


def texture(kind: str) -> bytes:
    pixels = [[(*STEEL, 255) for _ in range(16)] for _ in range(16)]

    def box(x0, y0, x1, y1, color):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                pixels[y][x] = (*color, 255)

    box(0, 0, 15, 15, DARK)
    box(1, 1, 14, 14, STEEL)
    box(2, 2, 13, 13, DARK)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        box(x, y, x, y, (188, 206, 207))
    if kind == "front":
        box(3, 3, 12, 11, BRASS)
        box(4, 4, 11, 10, (22, 62, 73))
        box(5, 5, 10, 5, GLOW)
        box(5, 7, 8, 7, GLOW)
        box(5, 9, 6, 9, GLOW)
        box(9, 12, 10, 12, GLOW)
        box(4, 12, 5, 12, BRASS)
    elif kind == "side":
        for y in (4, 6, 8, 10):
            box(4, y, 11, y, STEEL)
        box(3, 12, 12, 12, BRASS)
    elif kind == "top":
        box(4, 4, 11, 11, BRASS)
        box(5, 5, 10, 10, DARK)
        box(7, 6, 8, 9, GLOW)
        box(6, 7, 9, 8, GLOW)
    else:
        box(3, 3, 12, 12, STEEL)
        box(5, 5, 10, 10, DARK)
        box(6, 6, 9, 9, BRASS)

    return png(pixels)


def cave_scene(kind):
    palettes = {
        "magnetic_caves": ((13, 28, 44), (30, 70, 86)),
        "primordial_caves": ((30, 55, 43), (86, 123, 66)),
        "toxic_caves": ((32, 31, 38), (71, 86, 40)),
        "abyssal_chasm": ((9, 22, 49), (22, 70, 107)),
        "forlorn_hollows": ((20, 14, 32), (55, 32, 66)),
        "candy_cavity": ((85, 37, 84), (198, 110, 149)),
    }
    top, bottom = palettes[kind]
    pixels = [[(*(int(top[c] + (bottom[c] - top[c]) * y / 81) for c in range(3)), 255)
               for _ in range(108)] for y in range(82)]

    def box(x0, y0, x1, y1, color):
        for y in range(max(0, y0), min(81, y1) + 1):
            for x in range(max(0, x0), min(107, x1) + 1):
                pixels[y][x] = (*color, 255)

    def polygon(points, color):
        for y in range(max(0, min(p[1] for p in points)), min(81, max(p[1] for p in points)) + 1):
            crossings = []
            for (x1, y1), (x2, y2) in zip(points, points[1:] + points[:1]):
                if (y1 <= y < y2) or (y2 <= y < y1):
                    crossings.append(x1 + (y - y1) * (x2 - x1) / (y2 - y1))
            crossings.sort()
            for left, right in zip(crossings[::2], crossings[1::2]):
                box(int(left), y, int(right), y, color)

    def ellipse(cx, cy, rx, ry, color):
        for y in range(cy - ry, cy + ry + 1):
            for x in range(cx - rx, cx + rx + 1):
                if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1:
                    box(x, y, x, y, color)

    rng = random.Random(sum(map(ord, kind)))
    for _ in range(75):
        x, y = rng.randrange(4, 104), rng.randrange(4, 77)
        color = tuple(min(255, c + 12) for c in pixels[y][x][:3])
        box(x, y, x, y, color)
    rock = tuple(max(0, c - 8) for c in top)
    polygon([(0, 0), (108, 0), (108, 15), (95, 11), (84, 22), (72, 9),
             (58, 14), (49, 7), (34, 20), (22, 10), (12, 24), (0, 15)], rock)

    if kind == "magnetic_caves":
        for x, h in ((8, 37), (25, 25), (78, 32), (93, 47)):
            box(x, 77 - h, x + 9, 77, (54, 71, 79))
            box(x + 1, 78 - h, x + 3, 75, (126, 154, 158))
            box(x + 5, 81 - h, x + 8, 75, (75, 94, 100))
        box(40, 34, 68, 62, (37, 49, 60))
        box(43, 35, 49, 55, (75, 207, 218))
        box(60, 35, 66, 55, (240, 95, 87))
        box(43, 52, 66, 60, (170, 196, 192))
        box(50, 35, 59, 50, (19, 42, 57))
        for x, y, color in ((46, 22, (122, 230, 240)), (61, 16, (255, 133, 111)), (53, 7, (154, 227, 231))):
            polygon([(x, y - 4), (x + 4, y), (x, y + 4), (x - 4, y)], color)
        box(3, 74, 104, 78, (34, 44, 49))
    elif kind == "primordial_caves":
        polygon([(0, 56), (17, 46), (29, 58), (43, 47), (66, 57), (84, 46), (108, 58), (108, 82), (0, 82)], (38, 71, 42))
        box(19, 29, 24, 74, (96, 75, 46))
        for x, y, rx in ((20, 26, 18), (13, 35, 14), (29, 36, 15)):
            ellipse(x, y, rx, 8, (96, 153, 70))
        for x, y in ((50, 22), (83, 28), (91, 50)):
            box(x - 2, y + 8, x + 2, 74, (124, 105, 66))
            polygon([(x, y), (x + 7, y + 10), (x + 2, y + 17), (x - 6, y + 10)], (246, 180, 58))
            box(x - 1, y + 4, x + 1, y + 11, (255, 223, 119))
        for x, y in ((41, 66), (69, 73), (97, 70)):
            polygon([(x, y), (x - 12, y - 9), (x - 6, y - 11), (x, y - 3),
                     (x + 6, y - 14), (x + 11, y - 11), (x + 3, y), (x, y + 7)], (127, 182, 81))
        box(3, 76, 104, 78, (111, 138, 76))
    elif kind == "toxic_caves":
        polygon([(0, 67), (20, 61), (36, 67), (66, 60), (93, 63), (108, 57), (108, 82), (0, 82)], (100, 128, 48))
        box(7, 69, 101, 72, (183, 218, 67))
        box(37, 37, 61, 67, (52, 66, 62))
        box(38, 38, 60, 42, (155, 172, 95))
        box(40, 45, 58, 62, (218, 183, 61))
        ellipse(49, 53, 3, 3, (33, 39, 35))
        for points in ([(48, 49), (43, 43), (54, 43)], [(45, 54), (39, 59), (42, 50)], [(52, 54), (59, 50), (57, 60)]):
            polygon(points, (33, 39, 35))
        for x, y in ((23, 42), (76, 37), (85, 52)):
            box(x - 2, y - 3, x + 2, y + 3, (148, 170, 57))
            box(x, y - 4, x, y + 4, (218, 240, 109))
        polygon([(3, 48), (17, 36), (22, 66), (3, 70)], (84, 78, 61))
        polygon([(88, 27), (105, 34), (105, 67), (91, 60)], (84, 78, 61))
    elif kind == "abyssal_chasm":
        for y in (28, 44, 62):
            box(5, y, 104, y, (27, 78, 115))
        polygon([(0, 75), (13, 63), (30, 72), (55, 66), (80, 74), (99, 60), (108, 69), (108, 82), (0, 82)], (19, 39, 65))
        for x, y in ((18, 47), (83, 37), (91, 55)):
            box(x, y, x + 2, 73, (30, 113, 135))
            polygon([(x, y + 7), (x - 7, y + 2), (x - 3, y + 11), (x, y + 14)], (42, 156, 169))
        for x, y in ((36, 39), (48, 30), (60, 47)):
            box(x - 1, y, x + 1, 71, (81, 124, 182))
            ellipse(x, y, 7, 4, (62, 220, 217))
            ellipse(x, y - 1, 3, 2, (179, 255, 229))
        for x, y in ((65, 17), (74, 27), (29, 22), (55, 12)):
            ellipse(x, y, 2, 2, (106, 181, 220))
            box(x, y, x, y, (13, 42, 74))
    elif kind == "forlorn_hollows":
        polygon([(0, 74), (18, 61), (37, 72), (59, 65), (82, 71), (103, 60), (108, 82), (0, 82)], (33, 25, 41))
        for x, y in ((15, 29), (83, 23), (97, 39)):
            box(x, y, x + 3, 75, (18, 15, 26))
            polygon([(x, y + 13), (x - 9, y + 3), (x - 7, y + 15), (x, y + 22)], (18, 15, 26))
        polygon([(37, 73), (40, 37), (47, 28), (54, 35), (61, 27), (69, 41), (73, 73)], (11, 11, 20))
        box(47, 41, 51, 43, (232, 221, 165))
        box(60, 41, 64, 43, (232, 221, 165))
        for x, y in ((25, 52), (77, 58), (39, 18), (71, 14)):
            box(x, y, x + 2, y + 1, (174, 116, 185))
        box(4, 77, 103, 78, (80, 58, 93))
    else:
        polygon([(0, 70), (20, 62), (39, 70), (62, 62), (83, 70), (108, 60), (108, 82), (0, 82)], (76, 43, 56))
        box(8, 72, 99, 75, (162, 103, 212))
        for x, y in ((22, 33), (83, 23)):
            box(x - 2, y, x + 2, 72, (248, 220, 188))
            ellipse(x, y, 12, 12, (248, 164, 178))
            ellipse(x, y, 8, 8, (255, 231, 181))
            ellipse(x, y, 4, 4, (219, 95, 143))
        for x, y, color in ((46, 49, (107, 222, 206)), (67, 42, (251, 216, 125)), (95, 58, (236, 156, 217))):
            polygon([(x, y - 8), (x + 5, y), (x + 4, y + 16), (x - 4, y + 16), (x - 5, y)], color)
            box(x - 1, y - 3, x, y + 13, (255, 239, 216))

    for x0, y0, x1, y1 in ((0, 0, 107, 2), (0, 79, 107, 81), (0, 0, 2, 81), (105, 0, 107, 81)):
        box(x0, y0, x1, y1, DARK)
    for x0, y0, x1, y1 in ((2, 2, 105, 2), (2, 79, 105, 79), (2, 2, 2, 79), (105, 2, 105, 79)):
        box(x0, y0, x1, y1, BRASS)
    return pixels


def upgrade_card():
    pixels = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    for y in range(2, 14):
        for x in range(1, 15):
            color = BRASS if x in (1, 14) or y in (2, 13) else (63, 91, 53)
            pixels[y][x] = (*color, 255)
    for x in (4, 8, 12):
        for y in range(5, 8):
            for dx in range(-1, 2):
                pixels[y][x + dx] = (227, 212, 167, 255)
        pixels[6][x - 1] = (*DARK, 255)
        pixels[6][x + 1] = (*DARK, 255)
    for x in range(3, 13):
        pixels[10][x] = (*GLOW, 255)
    for y in (9, 11):
        pixels[y][11] = (*GLOW, 255)
    return png(pixels)


def main():
    textures = ASSETS / "textures/block"
    for kind, name in (("front", "dispatch_terminal"), ("side", "dispatch_terminal_side"),
                       ("top", "dispatch_terminal_top"), ("bottom", "dispatch_terminal_bottom")):
        (textures / f"{name}.png").write_bytes(texture(kind))
    model = {
        "parent": "minecraft:block/block", "ambientocclusion": False,
        "textures": {"particle": "dimensionworks_transfer:block/dispatch_terminal_side",
                     "front": "dimensionworks_transfer:block/dispatch_terminal",
                     "side": "dimensionworks_transfer:block/dispatch_terminal_side",
                     "top": "dimensionworks_transfer:block/dispatch_terminal_top",
                     "bottom": "dimensionworks_transfer:block/dispatch_terminal_bottom"},
        "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
            direction: {"uv": [0, 0, 16, 16], "texture": f"#{texture_name}"}
            for direction, texture_name in (("north", "front"), ("south", "side"), ("east", "side"),
                                            ("west", "side"), ("up", "top"), ("down", "bottom"))}}]
    }
    (ASSETS / "models/block/dispatch_terminal.json").write_text(
        json.dumps(model, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    destinations = ASSETS / "textures/gui/destinations"
    scenes = [cave_scene(kind) for kind in CAVES]
    for kind, pixels in zip(CAVES, scenes):
        (destinations / f"{kind}.png").write_bytes(png(pixels))
    overview = [[(*DARK, 255) for _ in range(108)] for _ in range(82)]
    for i, scene in enumerate(scenes):
        x0, y0 = 4 + (i % 3) * 34, 4 + (i // 3) * 38
        for y in range(35):
            for x in range(32):
                overview[y0 + y][x0 + x] = scene[y * 82 // 35][x * 108 // 32]
    (destinations / "alexscaves.png").write_bytes(png(overview))
    item_textures = ASSETS / "textures/item"
    item_textures.mkdir(parents=True, exist_ok=True)
    (item_textures / "twilight_dispatch_upgrade.png").write_bytes(upgrade_card())


if __name__ == "__main__":
    main()
