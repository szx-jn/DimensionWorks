#!/usr/bin/env python3
"""Generate the 16x16 RGBA texture for the KubeJS dimension key."""

from __future__ import annotations

import argparse
import struct
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT = ROOT / "kubejs" / "assets" / "kubejs" / "textures" / "item" / "dimension_key.png"

COLORS = {
    ".": (0, 0, 0, 0),
    "D": (117, 67, 15, 255),
    "G": (230, 171, 56, 255),
    "L": (255, 226, 128, 255),
    "C": (39, 190, 211, 255),
    "c": (176, 247, 240, 255),
}

PIXELS = [
    "................",
    "....DDDD........",
    "...DGGGGD.......",
    "..DGG..GGD......",
    "..DG.CC.GD......",
    "..DG.cc.GD......",
    "..DGG..GGD......",
    "...DGGGGD.......",
    "....DGGD........",
    ".....DGGD.......",
    "......DGGD......",
    ".......DGGD.....",
    "........DGGD....",
    "........DGGDD...",
    ".......DGG.DD...",
    "............D...",
]


def png_chunk(chunk_type: bytes, data: bytes) -> bytes:
    checksum = zlib.crc32(chunk_type)
    checksum = zlib.crc32(data, checksum)
    return (
        struct.pack(">I", len(data))
        + chunk_type
        + data
        + struct.pack(">I", checksum & 0xFFFFFFFF)
    )


def rgba_rows() -> bytes:
    if len(PIXELS) != 16 or any(len(row) != 16 for row in PIXELS):
        raise ValueError("texture map must be exactly 16x16")

    raw = bytearray()
    for row in PIXELS:
        raw.append(0)
        for pixel in row:
            try:
                raw.extend(COLORS[pixel])
            except KeyError as error:
                raise ValueError(f"unknown texture color: {pixel!r}") from error
    return bytes(raw)


def write_png(output: Path) -> None:
    header = struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0)
    image = (
        b"\x89PNG\r\n\x1a\n"
        + png_chunk(b"IHDR", header)
        + png_chunk(b"IDAT", zlib.compress(rgba_rows(), level=9))
        + png_chunk(b"IEND", b"")
    )
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(image)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args()
    output = args.output.resolve()
    write_png(output)
    print(f"generated {output}")


if __name__ == "__main__":
    main()
