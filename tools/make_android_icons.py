"""Builds the Android launcher icons from the same artwork as the iOS icon.

Usage: python tools/make_android_icons.py
Runs tools/make_icon.py for the light, dark and tinted variants, then writes:
  ic_launcher / ic_launcher_round   legacy icons for Android 7 and earlier launchers
  ic_launcher_foreground            the book for the adaptive icon (on emerald)
  ic_launcher_monochrome            the book in one tone, for themed icons on Android 13 and later
  play_store_icon.png               512 x 512 for the Play listing
"""

import os
import subprocess
import sys
import tempfile
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "android", "app", "src", "main", "res")
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

tmp = tempfile.mkdtemp()
paths = {}
for variant in ("light", "dark", "tinted"):
    out = os.path.join(tmp, f"{variant}.png")
    subprocess.run([sys.executable, os.path.join(ROOT, "tools", "make_icon.py"), out, variant], check=True)
    paths[variant] = out

light = Image.open(paths["light"]).convert("RGBA")
dark = Image.open(paths["dark"]).convert("RGBA")
tinted = Image.open(paths["tinted"]).convert("RGBA")


def adaptive_layer(art, px):
    """Adaptive layers are 108dp with the visible circle 66dp across, so the 1024 art,
    whose book already sits inside its own margins, is placed at 108/72 of the safe size."""
    canvas = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    inner = int(px * 72 / 108)
    art = art.resize((inner, inner), Image.LANCZOS)
    off = (px - inner) // 2
    canvas.alpha_composite(art, (off, off))
    return canvas


def rounded(img, radius_ratio):
    size = img.size[0]
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * radius_ratio), fill=255)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


for name, scale in DENSITIES.items():
    folder = os.path.join(RES, f"mipmap-{name}")
    os.makedirs(folder, exist_ok=True)
    legacy = int(48 * scale)
    layer = int(108 * scale)
    rounded(light.resize((legacy, legacy), Image.LANCZOS), 0.22).save(os.path.join(folder, "ic_launcher.png"))
    rounded(light.resize((legacy, legacy), Image.LANCZOS), 0.5).save(os.path.join(folder, "ic_launcher_round.png"))
    adaptive_layer(dark, layer).save(os.path.join(folder, "ic_launcher_foreground.png"))
    mono = adaptive_layer(tinted, layer)
    # Themed icons use only the alpha channel; keep the book's shape with full white.
    white = Image.new("RGBA", mono.size, (255, 255, 255, 0))
    white.putalpha(mono.getchannel("A"))
    white.save(os.path.join(folder, "ic_launcher_monochrome.png"))

store = os.path.join(ROOT, "android", "play_store_icon.png")
light.resize((512, 512), Image.LANCZOS).convert("RGB").save(store)
print("ok")
