#!/usr/bin/env python3
"""生成完全不透明的 16×16 发运终端六面纹理，无第三方 Python 依赖。"""
import json
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "mods/dimensionworks-transfer/src/main/resources/assets/dimensionworks_transfer"
STEEL, DARK, BRASS, GLOW = (88, 109, 120), (24, 38, 48), (208, 165, 71), (68, 222, 228)


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

    def chunk(name: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + name + data + struct.pack(">I", zlib.crc32(name + data))

    raw = b"".join(b"\0" + bytes(channel for pixel in row for channel in pixel) for row in pixels)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))


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


if __name__ == "__main__":
    main()
