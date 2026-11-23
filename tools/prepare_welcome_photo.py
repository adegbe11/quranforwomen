"""Prepares the welcome photo from the design mock: removes the text and controls
that were baked into the image, crops to the photo, and fades the bottom into cream.

Usage: python -I tools/prepare_welcome_photo.py <mock.webp> <output.jpg> [v1|v2]
"""
import sys
from PIL import Image, ImageDraw, ImageFilter

src, out = sys.argv[1], sys.argv[2]
im = Image.open(src).convert("RGB")
W, H = im.size

def patch(box, feather=10):
    """Rebuild a box by blending each column from the pixels just above and below it,
    then blend it in through a feathered mask so no edge shows.
    The wall and curtains run vertically, so this keeps their folds."""
    x0, y0, x1, y1 = box
    filled = im.copy()
    src_px = im.load()
    dst = filled.load()
    for x in range(x0, x1):
        top = src_px[x, max(0, y0 - 2)]
        bottom = src_px[x, min(H - 1, y1 + 2)]
        for y in range(y0, y1):
            t = (y - y0) / max(1, y1 - y0)
            dst[x, y] = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
    filled = filled.filter(ImageFilter.GaussianBlur(1.2))
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rectangle(box, fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(feather))
    im.paste(filled, (0, 0), mask)

# Boxes to paint out, per mock. Each verse box stays clear of her hijab.
PROFILES = {
    # First mock (with the mug).
    "v1": [((0, 0, W, 92), 12), ((58, 172, 340, 326), 8), ((58, 326, 212, 408), 8),
           ((60, 286, 150, 336), 4), ((604, 114, 826, 198), 8)],
    # Second mock (plants, no mug). The hijab starts near x=235, y=325.
    "v2": [((0, 0, W, 92), 12), ((66, 163, 328, 316), 8), ((66, 316, 218, 388), 8),
           ((600, 106, 822, 196), 8)],
}
profile = sys.argv[3] if len(sys.argv) > 3 else "v2"
for box, feather in PROFILES[profile]:
    patch(box, feather=feather)

# Keep the photo, fade its bottom into the app's warm cream.
cut = 1150
photo = im.crop((0, 0, W, cut))
cream = (250, 247, 240)
fade = 260
pp = photo.load()
for y in range(cut - fade, cut):
    t = (y - (cut - fade)) / fade
    t = t * t * (3 - 2 * t)
    for x in range(W):
        c = pp[x, y]
        pp[x, y] = tuple(int(c[i] + (cream[i] - c[i]) * t) for i in range(3))

photo.save(out, quality=90, optimize=True, progressive=True)
print("saved", out, photo.size)
