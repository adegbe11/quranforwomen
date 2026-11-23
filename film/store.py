"""Google Play listing art for Quran for Women.

    python film/store.py

Writes film/store/play_screenshot_1.png ... _8.png (1440 x 2560, 9:16) and
film/store/play_feature_graphic.png (1024 x 500). Every phone shows a real screen of the app.
The eight screenshots share one panorama: a soft ribbon and light run across all of them, so the
row reads as one piece in the store.
"""

import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import render
from render import (BLUSH, CREAM, EMERALD, EMERALD_DEEP, GOLD, SANS, SANS_SEMI, SERIF, SERIF_ITALIC, TEXT, TEXT2,
                    Phone, font, radial_glow, text_layer)

HERE = os.path.dirname(os.path.abspath(__file__))
FR = os.path.join(HERE, "store", "frames")
OUT = os.path.join(HERE, "store")
W, H = 1440, 2560
N = 8
ICON = render.ICON


def clean_status_bar(img, dark_text):
    """Paints a tidy status bar (time, signal, battery) over the emulator's own."""
    img = img.convert("RGB").copy()
    bar = 118
    arr = np.asarray(img).copy()
    # Take the colour just below the bar, smoothed sideways so stars and dots do not smear down.
    row = Image.fromarray(arr[bar + 2:bar + 10]).filter(ImageFilter.BoxBlur(40))
    arr[:bar] = np.asarray(row)[4:5].repeat(bar, axis=0)
    img = Image.fromarray(arr)
    d = ImageDraw.Draw(img)
    c = (23, 37, 31) if dark_text else (255, 255, 255)
    d.text((60, 32), "9:41", font=font(SANS_SEMI, 44), fill=c)
    x = 1080 - 70
    d.rounded_rectangle([x - 46, 44, x, 70], 6, outline=c, width=3)
    d.rounded_rectangle([x - 42, 48, x - 10, 66], 3, fill=c)
    d.rectangle([x + 2, 52, x + 6, 62], fill=c)
    for i, hgt in enumerate((10, 16, 22, 28)):
        d.rounded_rectangle([x - 110 + i * 12, 72 - hgt, x - 102 + i * 12, 72], 2, fill=c)
    return img


def screen(name, dark_status=True):
    return clean_status_bar(Image.open(os.path.join(FR, name + ".png")), dark_status)


def phone_image(scr, screen_h, angle=0.0):
    p = Phone(screen_h)
    img = p.compose(scr.resize((p.sw, p.sh), Image.LANCZOS))
    if angle:
        img = img.rotate(angle, resample=Image.BICUBIC, expand=True)
    return img


def panorama():
    """One wide background for all eight shots: cream to blush light, a flowing emerald ribbon."""
    PW = W * N
    base = np.zeros((H, PW, 3), np.float32)
    grad = np.linspace(0, 1, H)[:, None, None]
    base[:] = np.array(CREAM, np.float32) * (1 - grad) + np.array((244, 236, 226), np.float32) * grad
    for k in range(N):
        cx = W * k + W * (0.2 if k % 2 else 0.8)
        g, a = radial_glow(PW, H, cx, H * 0.18, W * 0.9, np.array((255, 252, 245), np.float32), 0.85)
        base = base * (1 - a) + g * a
        g, a = radial_glow(PW, H, W * k + W * (0.85 if k % 2 else 0.15), H * 0.92, W * 0.8, np.array(BLUSH, np.float32), 0.45)
        base = base * (1 - a) + g * a
    img = Image.fromarray(np.clip(base, 0, 255).astype(np.uint8)).convert("RGBA")
    # The ribbon: one smooth wave that runs the full length, like the bookmark ribbon of a Mushaf.
    layer = Image.new("RGBA", (PW, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    pts = []
    for x in range(0, PW + 20, 20):
        y = H * 0.66 + math.sin(x / W * math.pi * 0.9) * H * 0.12 + math.sin(x / W * 2.3) * H * 0.04
        pts.append((x, y))
    for width, alpha in ((150, 26), (70, 40), (14, 90)):
        d.line(pts, fill=EMERALD + (alpha,), width=width, joint="curve")
    gold = [(x, y - 70) for x, y in pts]
    d.line(gold, fill=GOLD + (120,), width=4, joint="curve")
    layer = layer.filter(ImageFilter.GaussianBlur(3))
    img.alpha_composite(layer)
    return img


def headline(canvas, title, sub, y=210, color=TEXT, color2=TEXT2, serif=False):
    lines = [(title, SERIF if serif else SANS_SEMI, 118 if serif else 104, color, 34)]
    if sub:
        lines.append((sub, SANS, 56, color2, 0))
    lay = text_layer(lines, int(W * 0.84), align="center")
    canvas.alpha_composite(lay, (int(W * 0.08), y))
    return y + lay.height


def place(canvas, ph, cx, top):
    canvas.alpha_composite(ph, (int(cx - ph.width / 2), int(top)))


def chip_row(canvas, labels, y, dark=False):
    f = font(SANS_SEMI, 40)
    d = ImageDraw.Draw(Image.new("RGB", (4, 4)))
    widths = [int(d.textlength(t, font=f)) + 84 for t in labels]
    total = sum(widths) + 24 * (len(labels) - 1)
    x = (W - total) / 2
    for t, w in zip(labels, widths):
        chip = Image.new("RGBA", (w, 88), (0, 0, 0, 0))
        cd = ImageDraw.Draw(chip)
        cd.rounded_rectangle([0, 0, w - 1, 87], 44, fill=(255, 255, 255, 34) if dark else EMERALD + (22,),
                             outline=(255, 255, 255, 70) if dark else EMERALD + (60,), width=2)
        bb = cd.textbbox((0, 0), t, font=f)
        cd.text(((w - (bb[2] - bb[0])) / 2 - bb[0], (88 - (bb[3] - bb[1])) / 2 - bb[1]), t, font=f,
                fill=CREAM if dark else EMERALD)
        canvas.alpha_composite(chip, (int(x), int(y)))
        x += w + 24


def hero():
    """Shot 1: emerald, the name, and two phones: Today in front, the Mushaf behind."""
    g = np.zeros((H, W, 3), np.float32)
    grad = np.linspace(0, 1, H)[:, None, None]
    g[:] = np.array(EMERALD, np.float32) * (1 - grad) + np.array(EMERALD_DEEP, np.float32) * grad
    glow, a = radial_glow(W, H, W * 0.5, H * 0.62, W * 0.9, np.array((22, 120, 90), np.float32), 0.7)
    g = g * (1 - a) + glow * a
    canvas = Image.fromarray(np.clip(g, 0, 255).astype(np.uint8)).convert("RGBA")
    icon = Image.open(ICON).convert("RGBA").resize((190, 190), Image.LANCZOS)
    m = Image.new("L", (190, 190), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, 189, 189], 44, fill=255)
    icon.putalpha(m)
    canvas.alpha_composite(icon, ((W - 190) // 2, 150))
    y = headline(canvas, "Quran for Women", "Your daily companion to the Quran, dua and a peaceful heart.",
                 y=380, color=CREAM, color2=(208, 228, 218), serif=True)
    chip_row(canvas, ["No ads", "No account", "Private"], y + 40, dark=True)
    back = phone_image(screen("mushaf_page", False), 1720, angle=9)
    place(canvas, back, W * 0.72, y + 230)
    front = phone_image(screen("today", False), 1880, angle=-5)
    place(canvas, front, W * 0.40, y + 150)
    return canvas.convert("RGB")


SHOTS = [
    # (title, sub, frame, dark_status, layout)
    ("Turn every page", "The full Mushaf, all 604 pages, with a real page turn.", "mushaf_turn", False, "single"),
    ("Listen, verse by verse", "Arabic, its meaning and recitation that follows along.", "reader", True, "single"),
    ("A dua for every moment", "Each one shows its source from the Quran or Sunnah.", "dua", True, "single"),
    ("Remember Allah", "A gentle dhikr counter. No streaks, no pressure.", "dhikr", True, "single"),
    ("Prayer times and Qibla", "Worked out on your phone, wherever you are.", ("prayer", "qibla"), True, "pair"),
    ("A private journal", "Write what is on your heart. It stays on your phone.", "journal", True, "single"),
    ("Your day at a glance", "Verse, dua, next prayer and dhikr on your home screen.", "widgets", False, "single"),
]


def shots():
    pano = panorama()
    out = [hero()]
    for k, (title, sub, frame, dark, layout) in enumerate(SHOTS, start=1):
        canvas = pano.crop((W * k, 0, W * (k + 1), H))
        bottom = headline(canvas, title, sub, y=200)
        top = bottom + 110
        if layout == "single":
            ph = phone_image(screen(frame, dark), 1900, angle=-3 if k % 2 else 3)
            place(canvas, ph, W / 2, top - 60)
        else:
            a = phone_image(screen(frame[1], dark), 1600, angle=7)
            place(canvas, a, W * 0.66, top + 120)
            b = phone_image(screen(frame[0], dark), 1720, angle=-4)
            place(canvas, b, W * 0.36, top - 20)
        out.append(canvas.convert("RGB"))
    return out


def feature_graphic():
    """1024 x 500. Drawn at 2x and reduced for crisp edges."""
    FW, FH = 2048, 1000
    g = np.zeros((FH, FW, 3), np.float32)
    gx = np.linspace(0, 1, FW)[None, :, None]
    g[:] = np.array(EMERALD_DEEP, np.float32) * gx + np.array(EMERALD, np.float32) * (1 - gx)
    glow, a = radial_glow(FW, FH, FW * 0.72, FH * 0.55, FW * 0.5, np.array((24, 124, 94), np.float32), 0.75)
    g = g * (1 - a) + glow * a
    canvas = Image.fromarray(np.clip(g, 0, 255).astype(np.uint8)).convert("RGBA")
    # Ribbon echo of the screenshots.
    layer = Image.new("RGBA", (FW, FH), (0, 0, 0, 0))
    pts = [(x, FH * 0.78 + math.sin(x / FW * math.pi * 1.6) * FH * 0.08) for x in range(0, FW + 20, 20)]
    ImageDraw.Draw(layer).line(pts, fill=GOLD + (110,), width=5, joint="curve")
    canvas.alpha_composite(layer.filter(ImageFilter.GaussianBlur(2)))
    icon = Image.open(ICON).convert("RGBA").resize((200, 200), Image.LANCZOS)
    m = Image.new("L", (200, 200), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, 199, 199], 46, fill=255)
    icon.putalpha(m)
    canvas.alpha_composite(icon, (150, 200))
    name = text_layer([("Quran for Women", SERIF, 124, CREAM, 18),
                       ("Quran, duas, dhikr and prayer times.", SANS, 46, (210, 230, 220), 6),
                       ("No ads. No account.", SANS_SEMI, 46, GOLD, 0)], 900)
    canvas.alpha_composite(name, (150, 450))
    back = phone_image(screen("mushaf_page", False), 1180, angle=10)
    canvas.alpha_composite(back, (int(FW * 0.80 - back.width / 2), 170))
    front = phone_image(screen("today", False), 1260, angle=-6)
    canvas.alpha_composite(front, (int(FW * 0.62 - front.width / 2), 110))
    return canvas.convert("RGB").resize((1024, 500), Image.LANCZOS)


if __name__ == "__main__":
    for i, img in enumerate(shots(), start=1):
        img.save(os.path.join(OUT, f"play_screenshot_{i}.png"), optimize=True)
        print("wrote screenshot", i)
    feature_graphic().save(os.path.join(OUT, "play_feature_graphic.png"), optimize=True)
    print("wrote feature graphic")
