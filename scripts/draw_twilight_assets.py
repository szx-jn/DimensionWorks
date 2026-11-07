#!/usr/bin/env python3
"""Draw original 16px Twilight industry textures with Python's standard library."""
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / 'mods/dimensionworks-cave-factory/src/main/resources/assets/dimensionworks_cave_factory/textures'


class Canvas:
    def __init__(self, color=(0, 0, 0, 0)):
        self.pixels = [color] * 256

    def pixel(self, x, y, color):
        if 0 <= x < 16 and 0 <= y < 16:
            self.pixels[y * 16 + x] = (*color, 255) if len(color) == 3 else color

    def rect(self, x1, y1, x2, y2, color):
        for y in range(y1, y2 + 1):
            for x in range(x1, x2 + 1):
                self.pixel(x, y, color)

    def save(self, target):
        target.parent.mkdir(parents=True, exist_ok=True)
        raw = b''.join(b'\0' + bytes(v for c in self.pixels[y * 16:(y + 1) * 16] for v in c) for y in range(16))

        def chunk(tag, data):
            return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

        target.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>2I5B', 16, 16, 8, 6, 0, 0, 0))
                           + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def save(canvas, category, name):
    canvas.save(TEXTURES / category / f'{name}.png')


def draw():
    dark, wood, copper, light = (38, 56, 45), (99, 72, 46), (169, 103, 60), (226, 205, 151)
    c = Canvas()
    c.rect(4, 5, 11, 13, (92, 53, 28)); c.rect(5, 4, 10, 12, (170, 99, 35))
    c.rect(6, 3, 8, 10, (231, 160, 54)); c.rect(7, 1, 8, 4, (250, 202, 101))
    c.rect(6, 6, 7, 8, (255, 224, 146)); c.pixel(10, 11, (211, 131, 43))
    save(c, 'item', 'forest_resin')
    c = Canvas()
    for offset in range(4):
        for y in range(2, 14):
            x = 4 + offset * 2 + ((y // 3 + offset) % 2)
            c.pixel(x, y, (194, 186, 132)); c.pixel(x + 1, y, (119, 137, 84))
    c.rect(3, 7, 12, 8, wood); c.rect(4, 7, 11, 7, copper)
    save(c, 'item', 'root_fiber')
    c = Canvas()
    c.rect(3, 7, 12, 12, (54, 43, 29)); c.rect(4, 5, 11, 11, (86, 65, 37))
    c.rect(5, 4, 8, 6, (118, 91, 49)); c.rect(8, 6, 13, 8, (96, 77, 40))
    for x, y in [(4, 8), (7, 6), (10, 10), (12, 7), (6, 11)]: c.pixel(x, y, (150, 140, 66))
    c.rect(6, 4, 8, 4, (105, 141, 65)); c.pixel(9, 3, (156, 182, 87))
    save(c, 'item', 'mature_humus')
    c = Canvas()
    c.rect(2, 2, 13, 13, dark); c.rect(3, 3, 12, 12, wood); c.rect(5, 5, 10, 10, (0, 0, 0, 0))
    for x, y in [(3, 3), (11, 3), (3, 11), (11, 11)]:
        c.rect(x, y, x + 1, y + 1, copper); c.pixel(x, y, light)
    c.rect(6, 3, 9, 3, (115, 144, 78)); c.rect(3, 6, 3, 9, (115, 144, 78))
    save(c, 'item', 'ironwood_frame')
    c = Canvas()
    c.rect(2, 3, 13, 12, dark); c.rect(3, 4, 12, 11, copper); c.rect(4, 5, 11, 10, light)
    for x in range(5, 12, 2): c.rect(x, 5, x, 10, (135, 151, 98))
    for y in range(6, 11, 2): c.rect(4, y, 11, y, (104, 123, 77))
    save(c, 'item', 'forest_filter')
    for name in ['twilight_casing', 'twilight_shaft', 'forest_leacher_top', 'boar_conditioner_top']:
        c = Canvas((*dark, 255))
        if name.startswith('twilight'):
            c.rect(1, 1, 14, 14, wood)
            for y in [3, 7, 11]: c.rect(1, y, 14, y, (79, 58, 39))
            c.rect(0, 0, 15, 1, copper); c.rect(0, 14, 15, 15, copper)
            c.rect(0, 2, 1, 13, copper); c.rect(14, 2, 15, 13, copper)
            for x, y in [(2, 2), (13, 2), (2, 13), (13, 13)]: c.pixel(x, y, light)
            if name == 'twilight_shaft':
                c.rect(4, 4, 11, 11, dark); c.rect(5, 5, 10, 10, (91, 105, 98))
                c.rect(7, 6, 8, 9, (191, 198, 176)); c.rect(6, 7, 9, 8, (191, 198, 176))
        else:
            base = (73, 105, 65) if name == 'forest_leacher_top' else (74, 57, 33)
            c.rect(0, 0, 15, 15, base)
            for y in range(16):
                for x in range(16):
                    if (x * 3 + y * 7) % 11 < 2: c.pixel(x, y, (123, 147, 77))
                    if name == 'forest_leacher_top' and (x + y * 3) % 13 == 0: c.pixel(x, y, (163, 191, 106))
        save(c, 'block', name)
    for name in ['forest_extract_still', 'forest_extract_flow']:
        c = Canvas((76, 112, 66, 255))
        for y in range(16):
            for x in range(16):
                v = (x * 3 + y * (5 if name.endswith('still') else 1)) % 17
                if v < 3: c.pixel(x, y, (133, 163, 83))
                elif v > 13: c.pixel(x, y, (53, 86, 48))
        save(c, 'block', name)
    print('Twilight textures: 5 items, 4 machine surfaces, 2 fluid sprites.')


if __name__ == '__main__':
    draw()
