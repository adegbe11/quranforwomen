"""Google Play listing art, version 2: warmer, more emotional, built to read at thumbnail size.

    python film/store_v2.py

Writes film/store/v2/play_screenshot_1.png ... _8.png (1440 x 2560) and play_feature_graphic.png.
Every phone and every callout is a real screen of the app.
"""

import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import render
from render import (BLUSH, CREAM, EMERALD, EMERALD_DEEP, GOLD, SANS, SANS_SEMI, SERIF, SERIF_ITALIC, TEXT, TEXT2,
                    font, radial_glow)
from store import clean_status_bar, phone_image, W, H

HERE = os.path.dirname(os.path.abspath(__file__))
FR = os.path.join(HERE, "store", "frames")
OUT = os.path.join(HERE, "store", "v2")
os.makedirs(OUT, exist_ok=True)
ROSE = (178, 77, 107)
N = 8
SCREEN_H = 1880
SCALE = SCREEN_H / 2424  # source pixels to phone screen pixels


def src(name, dark=True):
    return clean_status_bar(Image.open(os.path.join(FR, name + ".png")), dark)


# Rich headline: words in two colours, centred, wrapped.

def rich_headline(canvas, parts, y, size=108, sub=None, sub_color=TEXT2, family=SANS_SEMI, width=None, sub_size=54):
    width = width or int(W * 0.86)
    f = font(family, size)
    probe = ImageDraw.Draw(Image.new("RGB", (4, 4)))
    words = []
    for text, color in parts:
        if text == "|":
            words.append(("|", color))
            continue
        for w in text.split(" "):
            if w:
                words.append((w, color))
    lines, cur, cur_w = [], [], 0
    space = probe.textlength(" ", font=f)
    for w, c in words:
        if w == "|":
            if cur:
                lines.append(cur)
            cur, cur_w = [], 0
            continue
        ww = probe.textlength(w, font=f)
        if cur and cur_w + space + ww > width:
            lines.append(cur)
            cur, cur_w = [], 0
        cur.append((w, c, ww))
        cur_w += (space if len(cur) > 1 else 0) + ww
    if cur:
        lines.append(cur)
    d = ImageDraw.Draw(canvas)
    lh = int(size * 1.14)
    for line in lines:
        total = sum(x[2] for x in line) + space * (len(line) - 1)
        x = (W - total) / 2
        for w, c, ww in line:
            d.text((x, y), w, font=f, fill=c)
            x += ww + space
        y += lh
    if sub:
        sf = font(SANS, sub_size)
        sw = int(W * 0.8)
        sub_words = sub.split(" ")
        cur = ""
        y += 24
        for w in sub_words:
            test = (cur + " " + w).strip()
            if probe.textlength(test, font=sf) > sw and cur:
                tw = probe.textlength(cur, font=sf)
                d.text(((W - tw) / 2, y), cur, font=sf, fill=sub_color)
                y += int(sub_size * 1.3)
                cur = w
            else:
                cur = test
        tw = probe.textlength(cur, font=sf)
        d.text(((W - tw) / 2, y), cur, font=sf, fill=sub_color)
        y += int(sub_size * 1.3)
    return y


# Callout: a piece of the real screen, enlarged and lifted off the phone.

def callout(name, box, scale, dark=True, radius=40, border=6):
    img = src(name, dark).crop(box)
    img = img.resize((int(img.width * scale), int(img.height * scale)), Image.LANCZOS)
    w, h = img.size
    pad = 70
    out = Image.new("RGBA", (w + 2 * pad, h + 2 * pad), (0, 0, 0, 0))
    sh = Image.new("L", out.size, 0)
    ImageDraw.Draw(sh).rounded_rectangle([pad, pad + 22, pad + w, pad + h + 22], radius, fill=120)
    out.paste((40, 30, 35, 255), (0, 0), sh.filter(ImageFilter.GaussianBlur(30)))
    card = Image.new("RGBA", (w + 2 * border, h + 2 * border), (255, 255, 255, 255))
    m = Image.new("L", card.size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, card.width - 1, card.height - 1], radius + border, fill=255)
    card.putalpha(m)
    inner = img.convert("RGBA")
    mi = Image.new("L", inner.size, 0)
    ImageDraw.Draw(mi).rounded_rectangle([0, 0, w - 1, h - 1], radius, fill=255)
    inner.putalpha(mi)
    card.alpha_composite(inner, (border, border))
    out.alpha_composite(card, (pad - border, pad - border))
    return out


def put(canvas, img, cx, top, angle=0):
    if angle:
        img = img.rotate(angle, resample=Image.BICUBIC, expand=True)
    canvas.alpha_composite(img, (int(cx - img.width / 2), int(top)))


# Backgrounds

def panorama():
    PW = W * N
    base = np.zeros((H, PW, 3), np.float32)
    grad = np.linspace(0, 1, H)[:, None, None]
    top = np.array((252, 244, 246), np.float32)
    bottom = np.array((248, 230, 236), np.float32)
    base[:] = top * (1 - grad) + bottom * grad
    for k in range(N):
        g, a = radial_glow(PW, H, W * k + W * (0.25 if k % 2 else 0.75), H * 0.15, W * 0.95,
                           np.array((255, 252, 246), np.float32), 0.9)
        base = base * (1 - a) + g * a
        g, a = radial_glow(PW, H, W * k + W * (0.8 if k % 2 else 0.2), H * 0.85, W * 0.8,
                           np.array((243, 205, 216), np.float32), 0.55)
        base = base * (1 - a) + g * a
    img = Image.fromarray(np.clip(base, 0, 255).astype(np.uint8)).convert("RGBA")
    layer = Image.new("RGBA", (PW, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    pts = [(x, H * 0.7 + math.sin(x / W * math.pi * 0.9) * H * 0.11 + math.sin(x / W * 2.3) * H * 0.035)
           for x in range(0, PW + 20, 20)]
    for width, alpha in ((170, 22), (80, 34), (16, 80)):
        d.line(pts, fill=EMERALD + (alpha,), width=width, joint="curve")
    d.line([(x, y - 80) for x, y in pts], fill=ROSE + (90,), width=5, joint="curve")
    img.alpha_composite(layer.filter(ImageFilter.GaussianBlur(3)))
    return img


def emerald_bg():
    g = np.zeros((H, W, 3), np.float32)
    grad = np.linspace(0, 1, H)[:, None, None]
    g[:] = np.array(EMERALD, np.float32) * (1 - grad) + np.array(EMERALD_DEEP, np.float32) * grad
    glow, a = radial_glow(W, H, W * 0.5, H * 0.6, W * 0.95, np.array((22, 120, 90), np.float32), 0.7)
    g = g * (1 - a) + glow * a
    return Image.fromarray(np.clip(g, 0, 255).astype(np.uint8)).convert("RGBA")


def chips(canvas, labels, y, fg=EMERALD, fill=(255, 255, 255, 200), outline=None):
    f = font(SANS_SEMI, 42)
    d0 = ImageDraw.Draw(Image.new("RGB", (4, 4)))
    widths = [int(d0.textlength(t, font=f)) + 90 for t in labels]
    total = sum(widths) + 26 * (len(labels) - 1)
    x = (W - total) / 2
    for t, w in zip(labels, widths):
        chip = Image.new("RGBA", (w, 92), (0, 0, 0, 0))
        cd = ImageDraw.Draw(chip)
        cd.rounded_rectangle([0, 0, w - 1, 91], 46, fill=fill, outline=outline or fg + (50,), width=2)
        bb = cd.textbbox((0, 0), t, font=f)
        cd.text(((w - (bb[2] - bb[0])) / 2 - bb[0], (92 - (bb[3] - bb[1])) / 2 - bb[1]), t, font=f, fill=fg)
        canvas.alpha_composite(chip, (int(x), int(y)))
        x += w + 26


# Shots

def shot_hero(bg):
    c = bg.copy()
    photo = Image.open(render.PHOTO).convert("RGB")
    pw = W
    ph = int(photo.height * pw / photo.width)
    photo = photo.resize((pw, ph), Image.LANCZOS).crop((0, 330, W, 1270)).convert("RGBA")
    fade = Image.new("L", (W, 940), 255)
    fd = ImageDraw.Draw(fade)
    for yy in range(560, 940):
        fd.line([(0, yy), (W, yy)], fill=int(255 * (1 - (yy - 560) / 380) ** 1.5))
    photo.putalpha(fade)
    c.alpha_composite(photo, (0, 0))
    y = rich_headline(c, [("Your daily ", TEXT), ("companion", EMERALD), ("|", TEXT), ("to the Quran", TEXT)], 800,
                      size=124, family=SERIF, sub="Duas, dhikr, prayer times and a private journal.", sub_size=52)
    chips(c, ["No ads", "No account", "Private"], y + 24)
    put(c, phone_image(src("mushaf_page", False), 1500, angle=9), W * 0.74, y + 170)
    put(c, phone_image(src("today", False), 1640, angle=-5), W * 0.40, y + 80)
    return c


def shot_mushaf():
    c = emerald_bg()
    y = rich_headline(c, [("Turn every ", CREAM), ("page", GOLD)], 200, size=120,
                      sub="The full Mushaf, all 604 pages, with a real page turn.", sub_color=(205, 226, 216))
    put(c, phone_image(src("mushaf_turn", False), 1960, angle=-4), W * 0.5, y + 90)
    return c


def shot_reader(bg):
    c = bg.copy()
    y = rich_headline(c, [("Listen", EMERALD), (" verse by verse", TEXT)], 200,
                      sub="Arabic, its meaning and recitation that follows along.")
    put(c, phone_image(src("reader"), SCREEN_H, angle=0), W * 0.40, y + 110)
    put(c, callout("reader", (50, 740, 1050, 1340), SCALE * 1.18), W * 0.58, y + 860, angle=2)
    return c


def shot_feelings(bg):
    c = bg.copy()
    y = rich_headline(c, [("Find comfort in every ", TEXT), ("feeling", ROSE)], 200,
                      sub="Anxious, tired, grateful or in need of hope. Verses for your heart.")
    put(c, phone_image(src("feelings"), SCREEN_H, angle=3), W * 0.62, y + 100)
    put(c, callout("anxious", (40, 410, 1035, 1215), SCALE * 1.05), W * 0.40, y + 1000, angle=-3)
    return c


def shot_dua(bg):
    c = bg.copy()
    y = rich_headline(c, [("A dua for every ", TEXT), ("moment", EMERALD)], 200,
                      sub="Arabic, transliteration and meaning, with its source.")
    put(c, phone_image(src("dua"), SCREEN_H, angle=-3), W * 0.5, y + 110)
    put(c, callout("dua", (40, 1890, 500, 1990), SCALE * 2.4, radius=60), W * 0.62, y + 1180, angle=-2)
    return c


def shot_dhikr(bg):
    c = bg.copy()
    y = rich_headline(c, [("Remember", ROSE), (" Allah", TEXT)], 200,
                      sub="A gentle dhikr counter with soft haptics. No streaks.")
    put(c, phone_image(src("dhikr"), SCREEN_H, angle=4), W * 0.64, y + 100)
    ring = callout("dhikr", (200, 930, 920, 1690), SCALE * 1.2, radius=360)
    put(c, ring, W * 0.33, y + 620)
    return c


def shot_prayer(bg):
    c = bg.copy()
    y = rich_headline(c, [("A reminder at every ", TEXT), ("prayer", EMERALD)], 200,
                      sub="Prayer times and the Qibla, worked out on your phone.")
    put(c, phone_image(src("qibla"), 1600, angle=7), W * 0.70, y + 330)
    put(c, phone_image(src("prayer"), 1760, angle=-4), W * 0.38, y + 230)
    put(c, callout("notif", (40, 600, 1050, 880), SCALE * 1.15, dark=False, radius=48), W * 0.5, y + 40)
    return c


def shot_journal(bg):
    c = bg.copy()
    y = rich_headline(c, [("Your journal stays ", TEXT), ("private", ROSE)], 200,
                      sub="Lock it with your fingerprint. It never leaves your phone.")
    put(c, phone_image(src("journal"), SCREEN_H, angle=-3), W * 0.42, y + 100)
    put(c, callout("locked", (40, 960, 1040, 1430), SCALE * 1.0), W * 0.62, y + 1050, angle=3)
    return c


def feature_graphic():
    FW, FH = 2048, 1000
    g = np.zeros((FH, FW, 3), np.float32)
    gx = np.linspace(0, 1, FW)[None, :, None]
    g[:] = np.array((252, 242, 245), np.float32) * (1 - gx) + np.array((246, 222, 230), np.float32) * gx
    glow, a = radial_glow(FW, FH, FW * 0.25, FH * 0.3, FW * 0.5, np.array((255, 252, 246), np.float32), 0.9)
    g = g * (1 - a) + glow * a
    c = Image.fromarray(np.clip(g, 0, 255).astype(np.uint8)).convert("RGBA")
    # Emerald panel on the right holds the phones.
    panel = Image.new("L", (FW, FH), 0)
    pd = ImageDraw.Draw(panel)
    pd.polygon([(FW * 0.54, 0), (FW, 0), (FW, FH), (FW * 0.46, FH)], fill=255)
    em = emerald_bg().resize((FW, FH))
    c.paste(em, (0, 0), panel.filter(ImageFilter.GaussianBlur(2)))
    icon = Image.open(render.ICON).convert("RGBA").resize((180, 180), Image.LANCZOS)
    m = Image.new("L", (180, 180), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, 179, 179], 40, fill=255)
    icon.putalpha(m)
    c.alpha_composite(icon, (130, 170))
    d = ImageDraw.Draw(c)
    d.text((130, 390), "Quran for Women", font=font(SERIF, 120), fill=TEXT)
    d.text((130, 540), "Your daily companion to the", font=font(SANS, 46), fill=TEXT2)
    f = font(SANS_SEMI, 46)
    d.text((130, 600), "Quran, dua", font=f, fill=EMERALD)
    x = 130 + d.textlength("Quran, dua", font=f)
    d.text((x, 600), " & a ", font=font(SANS, 46), fill=TEXT2)
    x += d.textlength(" & a ", font=font(SANS, 46))
    d.text((x, 600), "peaceful heart", font=f, fill=ROSE)
    back = phone_image(src("mushaf_page", False), 1160, angle=10)
    c.alpha_composite(back, (int(FW * 0.84 - back.width / 2), 190))
    front = phone_image(src("today", False), 1240, angle=-6)
    c.alpha_composite(front, (int(FW * 0.66 - front.width / 2), 120))
    return c.convert("RGB").resize((1024, 500), Image.LANCZOS)


if __name__ == "__main__":
    pano = panorama()
    slices = [pano.crop((W * k, 0, W * (k + 1), H)) for k in range(N)]
    shots = [
        shot_hero(slices[0]), shot_mushaf(), shot_reader(slices[2]), shot_feelings(slices[3]),
        shot_dua(slices[4]), shot_dhikr(slices[5]), shot_prayer(slices[6]), shot_journal(slices[7]),
    ]
    for i, s in enumerate(shots, start=1):
        s.convert("RGB").save(os.path.join(OUT, f"play_screenshot_{i}.png"), optimize=True)
    feature_graphic().save(os.path.join(OUT, "play_feature_graphic.png"), optimize=True)
    row = Image.new("RGB", (298 * 8, 512), (40, 40, 40))
    for i in range(8):
        row.paste(Image.open(os.path.join(OUT, f"play_screenshot_{i + 1}.png")).resize((288, 512)), (i * 298, 0))
    row.save(os.path.join(OUT, "preview_row.png"))
    print("done")
