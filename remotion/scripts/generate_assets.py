from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
PUBLIC = ROOT / "public"
source = PUBLIC / "gear_heart_normal.png"
target = PUBLIC / "heart_red.png"

if not source.exists():
    raise SystemExit("Missing actual repository texture: " + str(source))
image = Image.open(source).convert("RGBA")
if image.size != (32, 32):
    raise SystemExit("Expected 32x32 Gear Heart texture, got " + str(image.size))
alpha = image.getchannel("A")
result = Image.new("RGBA", image.size, (0, 0, 0, 0))
a = alpha.load()
p = result.load()
for y in range(32):
    for x in range(32):
        if not a[x, y]:
            continue
        highlight = max(0.0, 1.0 - (((x - 10) / 18) ** 2 + ((y - 7) / 17) ** 2) ** 0.5)
        shade = max(0.0, min(1.0, 0.82 + highlight * 0.22 - y * 0.008))
        red = int(155 + 82 * shade)
        green = int(12 + 28 * shade)
        blue = int(28 + 25 * shade)
        if 8 <= x <= 13 and 6 <= y <= 9 and (x + y) % 3:
            red, green, blue = 255, 91, 102
        p[x, y] = (red, green, blue, a[x, y])
result.save(target)
result.resize((512, 512), Image.Resampling.NEAREST).save(PUBLIC / "heart_red_preview.png")
print("Generated red organic heart using exact source alpha silhouette.")
