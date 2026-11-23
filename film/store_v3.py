"""Google Play listing art, version 3: bold enough to read as a thumbnail, best screen first.

    python film/store_v3.py

Writes film/store/v3/play_screenshot_1.png ... _8.png (1440 x 2560) and play_feature_graphic.png.
Same real app screens as version 2. What changed: one short headline per shot in heavy type,
phones about a third larger, the Mushaf first, and a high contrast feature graphic.
"""

import os

from PIL import Image, ImageDraw

import render
from render import CREAM, EMERALD, GOLD, INTER, TEXT, font
from store import W, H, phone_image
from store_v2 import ROSE, callout, chips, emerald_bg, panorama, put, rich_headline, src

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "store", "v3")
os.makedirs(OUT, exist_ok=True)
BOLD = (INTER, "Bold")
N = 8
BIG = 2260  # phone screen height, up from 1880
HEAD = 150


def head(c, parts, sub=None, y=170, sub_color=(80, 92, 86)):
    """A heavy headline, with at most one short line under it."""
    return rich_headline(c, parts, y, size=HEAD, family=BOLD, sub=sub, sub_size=60, sub_color=sub_color,
                         width=int(W * 0.9))


def s_mushaf():
    c = emerald_bg()
    y = head(c, [("The Quran,", CREAM), ("|", CREAM), ("beautifully", GOLD)],
             sub="All 604 pages, with a real page turn.", sub_color=(205, 226, 216))
    chips(c, ["No ads", "No account"], y + 30, fg=EMERALD, fill=(250, 247, 240, 235), outline=(250, 247, 240, 0))
    put(c, phone_image(src("mushaf_turn", False), BIG, angle=-3), W * 0.5, y + 170)
    return c


def s_listen(bg):
    c = bg.copy()
    y = head(c, [("Listen", EMERALD), (" verse", TEXT), ("|", TEXT), ("by verse", TEXT)],
             sub="Three reciters. Learn it by heart.")
    put(c, phone_image(src("reader"), BIG), W * 0.42, y + 60)
    put(c, callout("reader", (50, 740, 1050, 1340), 1.0), W * 0.6, y + 1150, angle=2)
    return c


def s_prayer(bg):
    c = bg.copy()
    y = head(c, [("Never miss a", TEXT), ("|", TEXT), ("prayer", EMERALD)], sub="Prayer times and the Qibla.")
    put(c, phone_image(src("qibla", False), 1900, angle=7), W * 0.74, y + 420)
    put(c, phone_image(src("prayer"), 2080, angle=-4), W * 0.38, y + 200)
    put(c, callout("notif", (40, 600, 1050, 880), 1.0, dark=False, radius=48), W * 0.5, y + 10)
    return c


def s_dua(bg):
    c = bg.copy()
    y = head(c, [("A dua for every", TEXT), ("|", TEXT), ("moment", EMERALD)], sub="Each one with its source.")
    put(c, phone_image(src("dua"), BIG, angle=-3), W * 0.5, y + 60)
    put(c, callout("dua", (40, 1890, 500, 1990), 2.0, radius=60), W * 0.64, y + 1500, angle=-2)
    return c


def s_dhikr(bg):
    c = bg.copy()
    y = head(c, [("Remember", ROSE), (" Allah", TEXT)], sub="A gentle dhikr counter.")
    put(c, phone_image(src("dhikr"), BIG, angle=4), W * 0.62, y + 60)
    put(c, callout("dhikr", (200, 930, 920, 1690), 0.95, radius=360), W * 0.31, y + 780)
    return c


def s_feelings(bg):
    c = bg.copy()
    y = head(c, [("Verses for how", TEXT), ("|", TEXT), ("you ", TEXT), ("feel", ROSE)],
             sub="Anxious, tired, grateful, hopeful.")
    put(c, phone_image(src("feelings"), BIG, angle=3), W * 0.6, y + 60)
    put(c, callout("anxious", (40, 410, 1035, 1215), 0.9), W * 0.38, y + 1180, angle=-3)
    return c


def s_journal(bg):
    c = bg.copy()
    y = head(c, [("Your journal", TEXT), ("|", TEXT), ("stays ", TEXT), ("private", ROSE)],
             sub="Locked. Never leaves your phone.")
    put(c, phone_image(src("journal"), BIG, angle=-3), W * 0.42, y + 60)
    put(c, callout("locked", (40, 960, 1040, 1430), 0.85), W * 0.62, y + 1300, angle=3)
    return c


def s_today(bg):
    c = bg.copy()
    y = head(c, [("Made for", TEXT), ("|", TEXT), ("Muslim women", EMERALD)], sub="No ads. No account. Free.")
    put(c, phone_image(src("mushaf_page", False), 1900, angle=8), W * 0.72, y + 360)
    put(c, phone_image(src("today", False), 2120, angle=-5), W * 0.38, y + 160)
    return c


def feature_graphic():
    FW, FH = 2048, 1000
    c = emerald_bg().resize((FW, FH))
    icon = Image.open(render.ICON).convert("RGBA").resize((190, 190), Image.LANCZOS)
    m = Image.new("L", (190, 190), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, 189, 189], 42, fill=255)
    icon.putalpha(m)
    c.alpha_composite(icon, (120, 150))
    d = ImageDraw.Draw(c)
    d.text((120, 380), "Quran for Women", font=font(BOLD, 104), fill=CREAM)
    d.text((120, 560), "Quran, duas, dhikr", font=font(BOLD, 66), fill=GOLD)
    d.text((120, 650), "and prayer times", font=font(BOLD, 66), fill=GOLD)
    d.text((120, 780), "No ads. No account.", font=font((INTER, "Medium"), 52), fill=(205, 226, 216))
    back = phone_image(src("mushaf_page", False), 1100, angle=9)
    c.alpha_composite(back, (int(FW * 0.9 - back.width / 2), 190))
    front = phone_image(src("today", False), 1180, angle=-6)
    c.alpha_composite(front, (int(FW * 0.74 - front.width / 2), 120))
    return c.convert("RGB").resize((1024, 500), Image.LANCZOS)


if __name__ == "__main__":
    pano = panorama()
    sl = [pano.crop((W * k, 0, W * (k + 1), H)) for k in range(N)]
    shots = [s_mushaf(), s_listen(sl[1]), s_prayer(sl[2]), s_dua(sl[3]),
             s_dhikr(sl[4]), s_feelings(sl[5]), s_journal(sl[6]), s_today(sl[7])]
    for i, s in enumerate(shots, start=1):
        s.convert("RGB").save(os.path.join(OUT, f"play_screenshot_{i}.png"), optimize=True)
    feature_graphic().save(os.path.join(OUT, "play_feature_graphic.png"), optimize=True)
    row = Image.new("RGB", (298 * 8, 512), (40, 40, 40))
    for i in range(8):
        row.paste(Image.open(os.path.join(OUT, f"play_screenshot_{i + 1}.png")).resize((288, 512)), (i * 298, 0))
    row.save(os.path.join(OUT, "preview_row.png"))
    print("done")
