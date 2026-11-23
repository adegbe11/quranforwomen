"""Builds the widget picker previews for Android (res/drawable-nodpi/widget_preview_*.png).

The next prayer and dhikr previews are cropped from a real home screen capture
(android/shots/93_widgets_home.png). The verse, dua and progress previews are drawn in the
same style: emerald card, gold labels, soft lines where the text goes. They carry no scripture;
the live widgets show the real text from the bundled data.

Usage: python tools/make_widget_previews.py
"""

import json
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "android", "app", "src", "main", "res", "drawable-nodpi")
SHOT = os.path.join(ROOT, "android", "shots", "93_widgets_home.png")
AMIRI = os.path.join(ROOT, "android", "app", "src", "main", "res", "font", "amiri_quran.ttf")
SERIF = "C:/Windows/Fonts/georgia.ttf"
SANS = "C:/Windows/Fonts/segoeuib.ttf"

EMERALD = (6, 78, 59)
CREAM = (250, 247, 240)
GOLD = (233, 207, 143)

os.makedirs(RES, exist_ok=True)
def rounded_card(w, h):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, w - 1, h - 1], radius=int(min(w, h) * 0.12), fill=EMERALD)
    return img, d


def crop(box, name):
    img = Image.open(SHOT).convert("RGBA")
    card = img.crop(box)
    # Clear the corners so only the card shows in the picker.
    mask = Image.new("L", card.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, card.size[0] - 1, card.size[1] - 1], radius=int(card.size[0] * 0.13), fill=255)
    out = Image.new("RGBA", card.size, (0, 0, 0, 0))
    out.paste(card, (0, 0), mask)
    out.save(os.path.join(RES, name))


def wide(name, label, reference):
    """No scripture is drawn here: only a title and the reference. The live widget shows the real text."""
    w, h = 900, 440
    img, d = rounded_card(w, h)
    d.text((44, 44), label, font=ImageFont.truetype(SANS, 26), fill=GOLD)
    for i, width in enumerate((0.86, 0.72, 0.8)):
        y = 150 + i * 56
        d.rounded_rectangle([w - 44 - int((w - 88) * width), y, w - 44, y + 18], radius=9, fill=(250, 247, 240, 70))
    d.text((44, h - 74), reference, font=ImageFont.truetype(SANS, 28), fill=GOLD)
    img.save(os.path.join(RES, name))


def progress(name):
    w, h = 440, 440
    img, d = rounded_card(w, h)
    d.text((40, 40), "READING", font=ImageFont.truetype(SANS, 24), fill=GOLD)
    d.text((40, 230), "12", font=ImageFont.truetype(SERIF, 96), fill=CREAM)
    d.text((40, 350), "of 114 surahs", font=ImageFont.truetype(SANS, 28), fill=GOLD)
    img.save(os.path.join(RES, name))


# Real widgets from the home screen capture (pixels in the 1080 x 2424 capture).
crop((67, 552, 519, 1139), "widget_preview_prayer.png")
crop((561, 552, 1013, 1139), "widget_preview_dhikr.png")
wide("widget_preview_verse.png", "VERSE OF THE DAY", "Al-Baqarah 2:152")
wide("widget_preview_dua.png", "DUA OF THE DAY", "Quran 2:201")
progress("widget_preview_progress.png")
print("ok")
