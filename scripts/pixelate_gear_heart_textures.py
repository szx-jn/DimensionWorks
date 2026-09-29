#!/usr/bin/env python3
"""Pixelate the Gear Heart template into 32x32 Minecraft item textures.

The source image is a user-provided high-resolution pixel-art template. The
script removes its connected black background, crops and centers the subject,
downsamples it to Minecraft's 32x32 item resolution, and derives normal,
damaged, cursed, and blessed variants from the same template.
"""

from __future__ import annotations

from collections import deque
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
REFERENCE = ROOT / "docs/assets/gear_heart_reference.png"
TEXTURE_DIR = ROOT / "kubejs/assets/kubejs/textures/item"
PREVIEW = ROOT / "docs/assets/gear_heart_states_preview.png"

SIZE = 32
PREVIEW_SCALE = 8
PALETTE_COLORS = 24
BACKGROUND_THRESHOLD = 20
ALPHA_THRESHOLD = 96


def clamp(value: float) -> int:
    return max(0, min(255, round(value)))


def remove_connected_background(image: Image.Image) -> Image.Image:
    """Remove near-black pixels connected to any image corner."""
    image = image.convert("RGBA")
    width, height = image.size
    rgb = image.convert("RGB")
    source = rgb.load()
    background = Image.new("1", image.size, 0)
    visited = background.load()
    queue: deque[tuple[int, int]] = deque()

    for point in ((0, 0), (width - 1, 0), (0, height - 1), (width - 1, height - 1)):
        visited[point] = 1
        queue.append(point)

    while queue:
        x, y = queue.popleft()
        for point in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
            nx, ny = point
            if not (0 <= nx < width and 0 <= ny < height) or visited[point]:
                continue
            if max(source[point]) <= BACKGROUND_THRESHOLD:
                visited[point] = 1
                queue.append(point)

    alpha = Image.new("L", image.size, 255)
    alpha.paste(0, mask=background)
    image.putalpha(alpha)
    return image


def square_subject(image: Image.Image, margin_ratio: float = 0.02) -> Image.Image:
    """Crop to the opaque subject and center it on a square transparent canvas."""
    alpha_bbox = image.getchannel("A").getbbox()
    if alpha_bbox is None:
        raise RuntimeError("the reference image contains no visible subject")

    subject = image.crop(alpha_bbox)
    side = max(subject.size)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.alpha_composite(subject, ((side - subject.width) // 2, (side - subject.height) // 2))

    margin = round(side * margin_ratio)
    return canvas.crop((margin, margin, side - margin, side - margin))


def quantize_rgba(image: Image.Image, colors: int = PALETTE_COLORS) -> Image.Image:
    alpha = image.getchannel("A")
    quantized = image.convert("RGB").quantize(
        colors=colors,
        method=Image.Quantize.MEDIANCUT,
        dither=Image.Dither.NONE,
    )
    result = quantized.convert("RGBA")
    result.putalpha(alpha)
    return result


def make_pixel_art(reference: Image.Image) -> Image.Image:
    subject = square_subject(remove_connected_background(reference))
    small = subject.resize((SIZE, SIZE), Image.Resampling.LANCZOS)
    alpha = small.getchannel("A").point(lambda value: 255 if value >= ALPHA_THRESHOLD else 0)
    small.putalpha(alpha)
    return quantize_rgba(small)


def transformed_colors(image: Image.Image, state: str) -> Image.Image:
    result = image.copy()
    pixels = result.load()

    for y in range(SIZE):
        for x in range(SIZE):
            red, green, blue, alpha = pixels[x, y]
            if alpha == 0:
                continue

            if state == "damaged":
                gray = red * 0.299 + green * 0.587 + blue * 0.114
                red = red * 0.58 + gray * 0.42
                green = green * 0.58 + gray * 0.42
                blue = blue * 0.58 + gray * 0.42
                red, green, blue = red * 0.82, green * 0.78, blue * 0.76
            elif state == "cursed":
                gray = red * 0.299 + green * 0.587 + blue * 0.114
                red = (red * 0.52 + gray * 0.18) * 0.82 + 118 * 0.30
                green = (green * 0.45 + gray * 0.20) * 0.70 + 34 * 0.30
                blue = (blue * 0.62 + gray * 0.08) * 1.05 + 139 * 0.30
            else:
                red = red * 1.07 + 7
                green = green * 1.05 + 5
                blue = blue * 1.02 + 2

            pixels[x, y] = (clamp(red), clamp(green), clamp(blue), alpha)

    return result


def add_cursed_aura(image: Image.Image) -> Image.Image:
    original_alpha = image.getchannel("A")
    expanded_alpha = original_alpha.filter(ImageFilter.MaxFilter(3))
    aura = Image.new("RGBA", image.size, (0, 0, 0, 0))
    aura_pixels = aura.load()
    original_pixels = original_alpha.load()
    expanded_pixels = expanded_alpha.load()

    for y in range(SIZE):
        for x in range(SIZE):
            if original_pixels[x, y] == 0 and expanded_pixels[x, y] > 0:
                aura_pixels[x, y] = (126, 48, 166, 128)

    return Image.alpha_composite(aura, image)


def apply_damage(image: Image.Image) -> Image.Image:
    result = image.copy()
    draw = ImageDraw.Draw(result)

    chips = (
        (23, 6, 25, 9),
        (8, 18, 9, 20),
        (21, 22, 22, 24),
        (14, 27, 16, 28),
    )
    for left, top, right, bottom in chips:
        for y in range(top, bottom + 1):
            for x in range(left, right + 1):
                if result.getpixel((x, y))[3] > 0:
                    result.putpixel((x, y), (0, 0, 0, 0))

    crack = ((15, 12), (16, 14), (15, 16), (17, 18), (16, 20), (18, 22))
    for point in crack:
        if result.getpixel(point)[3] > 0:
            draw.point(point, fill=(27, 19, 17, 255))
    draw.line(((16, 7), (14, 10), (15, 12)), fill=(40, 29, 24, 255), width=1)
    return result


def make_state(base: Image.Image, state: str) -> Image.Image:
    if state == "normal":
        return base.copy()

    if state == "blessed":
        result = ImageEnhance.Contrast(transformed_colors(base, state)).enhance(1.08)
        result = ImageEnhance.Color(result).enhance(1.10)
        return quantize_rgba(result)

    if state == "cursed":
        result = transformed_colors(base, state)
        result = ImageEnhance.Contrast(result).enhance(1.05)
        return add_cursed_aura(quantize_rgba(result))

    result = apply_damage(transformed_colors(base, state))
    return quantize_rgba(result)

def make_repair_fragment() -> Image.Image:
    """Draw a small brass gear shard used by the seven placeholder repair items."""
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    outline = (52, 33, 22, 255)
    dark = (116, 72, 31, 255)
    brass = (192, 132, 48, 255)
    highlight = (244, 191, 82, 255)
    core = (38, 177, 195, 255)

    shard = (
        (7, 5), (13, 6), (16, 9), (21, 7), (23, 11), (29, 11),
        (30, 17), (26, 20), (29, 25), (24, 28), (19, 27), (16, 31),
        (12, 28), (7, 29), (5, 23), (8, 20), (3, 17), (6, 12)
    )
    draw.polygon(shard, fill=brass, outline=outline)
    draw.line(((7, 5), (13, 6), (16, 9), (21, 7)), fill=highlight, width=1)
    draw.line(((6, 12), (3, 17), (8, 20), (5, 23)), fill=dark, width=1)
    draw.ellipse((10, 10, 21, 21), fill=outline)
    draw.ellipse((12, 12, 19, 19), fill=core)
    draw.point((13, 13), fill=(187, 249, 249, 255))
    draw.line(((22, 20), (25, 22), (23, 25)), fill=outline, width=1)
    return image


def make_preview(images: dict[str, Image.Image]) -> None:
    gap = 8
    preview = Image.new(
        "RGBA",
        (SIZE * 4 * PREVIEW_SCALE + gap * 3, SIZE * PREVIEW_SCALE),
        (28, 28, 31, 255),
    )
    preview_draw = ImageDraw.Draw(preview)

    for index, state in enumerate(("normal", "damaged", "cursed", "blessed")):
        scaled = images[state].resize(
            (SIZE * PREVIEW_SCALE, SIZE * PREVIEW_SCALE),
            Image.Resampling.NEAREST,
        )
        x = index * (SIZE * PREVIEW_SCALE + gap)
        preview.alpha_composite(scaled, (x, 0))
        preview_draw.rectangle(
            (x, 0, x + SIZE * PREVIEW_SCALE - 1, SIZE * PREVIEW_SCALE - 1),
            outline=(70, 70, 76, 255),
        )

    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    preview.save(PREVIEW)


def main() -> None:
    if not REFERENCE.exists():
        raise FileNotFoundError(f"reference image not found: {REFERENCE}")

    TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
    with Image.open(REFERENCE) as source:
        base = make_pixel_art(source)

    images = {}
    for state in ("normal", "damaged", "cursed", "blessed"):
        image = make_state(base, state)
        output = TEXTURE_DIR / f"gear_heart_{state}.png"
        image.save(output)
        images[state] = image
        print(f"wrote {output.relative_to(ROOT)}")

    make_preview(images)
    fragment = make_repair_fragment()
    fragment_output = TEXTURE_DIR / "gear_heart_repair_fragment.png"
    fragment.save(fragment_output)
    print(f"wrote {fragment_output.relative_to(ROOT)}")
    print(f"wrote {PREVIEW.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
