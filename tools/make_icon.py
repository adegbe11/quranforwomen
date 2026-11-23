"""Renders the 1024x1024 app icon: an open book in warm cream on deep emerald.

Usage: python -I tools/make_icon.py <output.png> [light|dark|tinted]

light   the standard icon on emerald
dark    the book on a transparent background, for the iOS 18 dark home screen
tinted  a grayscale book on a transparent background, which iOS tints
"""

import sys
from PIL import Image, ImageDraw, ImageFilter

S = 1024
SCALE = 4  # supersample for smooth edges
W = S * SCALE

EMERALD = (6, 78, 59)
EMERALD_DEEP = (4, 58, 44)
CREAM = (250, 247, 240)
CREAM_SHADE = (232, 222, 202)
BLUSH = (245, 220, 227)

VARIANT = sys.argv[2] if len(sys.argv) > 2 else "light"

if VARIANT == "light":
    # Gentle vertical light, brighter at the top.
    grad = Image.new("RGB", (1, W))
    for y in range(W):
        t = y / (W - 1)
        grad.putpixel((0, y), tuple(int(EMERALD[i] * (1 - t) + EMERALD_DEEP[i] * t) for i in range(3)))
    img = grad.resize((W, W))
else:
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    if VARIANT == "tinted":
        CREAM, CREAM_SHADE, BLUSH = (255, 255, 255), (205, 205, 205), (150, 150, 150)

draw = ImageDraw.Draw(img)

cx = W // 2
top = int(W * 0.33)
bottom = int(W * 0.70)
spread = int(W * 0.30)
lift = int(W * 0.035)

# Soft shadow under the book.
shadow = Image.new("L", (W, W), 0)
sd = ImageDraw.Draw(shadow)
sd.ellipse([cx - spread * 1.05, bottom + lift * 0.4, cx + spread * 1.05, bottom + lift * 2.2], fill=110)
shadow = shadow.filter(ImageFilter.GaussianBlur(W * 0.02))
if VARIANT == "light":
    img.paste((2, 40, 30), mask=shadow)

# Two pages, slightly curved upward at the outer edges.
def page_polygon(side):
    sign = -1 if side == "left" else 1
    pts = []
    steps = 40
    # top edge from spine to outer corner
    for i in range(steps + 1):
        t = i / steps
        x = cx + sign * spread * t
        y = top + lift * (1 - (1 - t) ** 2) * -1 + lift
        pts.append((x, y))
    # bottom edge from outer corner back to spine
    for i in range(steps, -1, -1):
        t = i / steps
        x = cx + sign * spread * t
        y = bottom - lift * (t ** 2) * 0.6
        pts.append((x, y))
    return pts

for side in ("left", "right"):
    draw.polygon(page_polygon(side), fill=CREAM)

# Spine shading.
spine = Image.new("L", (W, W), 0)
spd = ImageDraw.Draw(spine)
spd.rectangle([cx - W * 0.012, top + lift, cx + W * 0.012, bottom], fill=150)
spine = spine.filter(ImageFilter.GaussianBlur(W * 0.012))
img.paste(CREAM_SHADE, mask=spine)

# Quiet text lines on each page.
line_color = (200, 192, 176) if VARIANT != "tinted" else (175, 175, 175)
for side in ("left", "right"):
    sign = -1 if side == "left" else 1
    for k in range(5):
        y = top + lift + W * 0.05 + k * W * 0.045
        x0 = cx + sign * W * 0.05
        x1 = cx + sign * (spread - W * 0.05)
        draw.rounded_rectangle(
            [min(x0, x1), y, max(x0, x1), y + W * 0.012], radius=W * 0.006, fill=line_color
        )

# A blush ribbon marker.
rx = cx + W * 0.13
draw.polygon(
    [(rx, top + lift * 0.6), (rx + W * 0.04, top + lift * 0.6), (rx + W * 0.04, bottom + W * 0.06),
     (rx + W * 0.02, bottom + W * 0.04), (rx, bottom + W * 0.06)],
    fill=BLUSH,
)

img = img.resize((S, S), Image.LANCZOS)
img.save(sys.argv[1])
print("saved", sys.argv[1])
