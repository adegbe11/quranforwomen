"""Applies an icon set exported by easyappicon (the zip unpacked into a folder) to both apps.

Usage: python tools/apply_app_icon.py <unpacked folder>

iPhone: the 1024 artwork becomes AppIcon.png (no alpha, as the App Store requires).
The dark and tinted variants are made from the same art with its white background cut away,
so on a dark or tinted home screen the book sits on the system background, not a white tile.
Android: the launcher mipmaps and adaptive foreground are copied as exported; the adaptive
background uses the exported colour; the Play Store icon is replaced.
"""

import os
import shutil
import sys
from PIL import Image, ImageDraw, ImageOps

src = sys.argv[1]
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
IOS = os.path.join(ROOT, "QuranForWomen", "Resources", "Assets.xcassets", "AppIcon.appiconset")
RES = os.path.join(ROOT, "android", "app", "src", "main", "res")

art = Image.open(os.path.join(src, "ios", "AppIcon.appiconset", "ItunesArtwork@2x.png")).convert("RGBA")

# iPhone light icon: flatten onto white, no alpha.
light = Image.new("RGB", art.size, (255, 255, 255))
light.paste(art, mask=art.getchannel("A"))
light.save(os.path.join(IOS, "AppIcon.png"))

# Cut the near-white background away by flooding from the four corners.
cut = art.copy()
w, h = cut.size
for corner in [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)]:
    ImageDraw.floodfill(cut, corner, (0, 0, 0, 0), thresh=40)
cut.save(os.path.join(IOS, "AppIcon-Dark.png"))
gray = ImageOps.grayscale(cut.convert("RGB"))
tinted = Image.merge("RGBA", (gray, gray, gray, cut.getchannel("A")))
tinted.save(os.path.join(IOS, "AppIcon-Tinted.png"))

# Android launcher icons.
for folder in os.listdir(os.path.join(src, "android")):
    path = os.path.join(src, "android", folder)
    if folder.startswith("mipmap-") and os.path.isdir(path):
        dest = os.path.join(RES, folder)
        os.makedirs(dest, exist_ok=True)
        for f in os.listdir(path):
            shutil.copy(os.path.join(path, f), os.path.join(dest, f))
# The old themed icon layer showed the previous artwork; this art has no single tone version.
for folder in os.listdir(RES):
    old = os.path.join(RES, folder, "ic_launcher_monochrome.png")
    if os.path.exists(old):
        os.remove(old)
shutil.copy(os.path.join(src, "android", "playstore-icon.png"), os.path.join(ROOT, "android", "play_store_icon.png"))
print("ok")
