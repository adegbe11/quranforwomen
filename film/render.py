"""Quran for Women · launch film renderer.

Builds the film from real screen recordings of the Android app (film/clips), the welcome photo
and the app icon. Nothing shown is mocked: every screen is the working app.

    python film/render.py master      60s, 1920 x 1080, 30 fps
    python film/render.py vertical    30s, 1080 x 1920, 30 fps
    python film/render.py hook        6s,  1080 x 1920, 30 fps

Writes film/out/<name>.mp4 (H.264, AAC). Requires ffmpeg on PATH.
"""

import math
import os
import subprocess
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
CLIPS = os.path.join(HERE, "clips")
OUT = os.path.join(HERE, "out")
os.makedirs(OUT, exist_ok=True)
FPS = 30

# Brand
EMERALD = (6, 78, 59)
EMERALD_DEEP = (3, 42, 32)
BLUSH = (245, 220, 227)
CREAM = (250, 247, 240)
TEXT = (23, 37, 31)
TEXT2 = (102, 116, 109)
GOLD = (233, 207, 143)

FONTS = os.path.join(HERE, "fonts")
INTER = os.path.join(FONTS, "Inter.ttf")
SANS_SEMI = (INTER, "SemiBold")
SANS = (INTER, "Regular")
SANS_LIGHT = (INTER, "Light")
SERIF = os.path.join(FONTS, "InstrumentSerif-Regular.ttf")
SERIF_ITALIC = os.path.join(FONTS, "InstrumentSerif-Italic.ttf")

PHOTO = os.path.join(ROOT, "android", "app", "src", "main", "res", "drawable-nodpi", "welcome_photo.jpg")
ICON = os.path.join(ROOT, "QuranForWomen", "Resources", "Assets.xcassets", "AppIcon.appiconset", "AppIcon.png")
BELL = os.path.join(ROOT, "android", "app", "src", "main", "res", "raw", "prayer_bell.wav")

SRC_W, SRC_H = 1080, 2424  # screen recording size


# Easing

def clamp(x, a=0.0, b=1.0):
    return max(a, min(b, x))


def ease_out(t):  # expo out, the app's own entrance curve
    t = clamp(t)
    return 1 - pow(2, -10 * t) if t < 1 else 1.0


def ease_in_out(t):
    t = clamp(t)
    return t * t * (3 - 2 * t)


def ease_cubic(t):
    t = clamp(t)
    return 4 * t * t * t if t < 0.5 else 1 - pow(-2 * t + 2, 3) / 2


def mix(a, b, t):
    return a + (b - a) * t


def mixc(c1, c2, t):
    return tuple(int(round(mix(c1[i], c2[i], t))) for i in range(3))


# Fonts and text

_font_cache = {}


def font(path, size):
    key = (path, size)
    if key not in _font_cache:
        if isinstance(path, tuple):
            f = ImageFont.truetype(path[0], size)
            f.set_variation_by_name(path[1])
        else:
            f = ImageFont.truetype(path, size)
        _font_cache[key] = f
    return _font_cache[key]


def text_layer(lines, width, align="left"):
    """lines: list of (text, font_path, size, color, spacing_after). Returns RGBA image."""
    probe = ImageDraw.Draw(Image.new("RGBA", (10, 10)))
    rows = []
    h = 0
    for text, path, size, color, gap in lines:
        f = font(path, size)
        wrapped = wrap(probe, text, f, width)
        for i, line in enumerate(wrapped):
            bbox = probe.textbbox((0, 0), line, font=f)
            lh = int(size * 1.22)
            rows.append((line, f, color, h, bbox))
            h += lh
        h += gap
    img = Image.new("RGBA", (width, max(1, h)), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for line, f, color, y, bbox in rows:
        w = bbox[2] - bbox[0]
        x = 0 if align == "left" else (width - w) // 2
        d.text((x - bbox[0], y), line, font=f, fill=color)
    return img


def wrap(d, text, f, width):
    words = text.split(" ")
    out, cur = [], ""
    for w in words:
        test = (cur + " " + w).strip()
        if d.textlength(test, font=f) <= width or not cur:
            cur = test
        else:
            out.append(cur)
            cur = w
    if cur:
        out.append(cur)
    return out


# Backgrounds

def radial_glow(w, h, cx, cy, radius, color, strength):
    y, x = np.ogrid[0:h, 0:w]
    dist = np.sqrt((x - cx) ** 2 + (y - cy) ** 2) / radius
    a = np.clip(1 - dist, 0, 1) ** 2 * strength
    layer = np.zeros((h, w, 3), np.float32)
    layer[:] = color
    return layer, a[..., None]


def bg_cream(w, h):
    base = np.zeros((h, w, 3), np.float32)
    top = np.array(CREAM, np.float32)
    bottom = np.array((243, 238, 226), np.float32)
    grad = np.linspace(0, 1, h)[:, None, None]
    base[:] = top * (1 - grad) + bottom * grad
    glow, a = radial_glow(w, h, w * 0.78, h * 0.18, max(w, h) * 0.75, np.array((255, 252, 244), np.float32), 0.9)
    base = base * (1 - a) + glow * a
    blush, a2 = radial_glow(w, h, w * 0.12, h * 0.95, max(w, h) * 0.6, np.array(BLUSH, np.float32), 0.35)
    base = base * (1 - a2) + blush * a2
    return Image.fromarray(np.clip(base, 0, 255).astype(np.uint8), "RGB")


def bg_emerald(w, h):
    base = np.zeros((h, w, 3), np.float32)
    grad = np.linspace(0, 1, h)[:, None, None]
    base[:] = np.array(EMERALD_DEEP, np.float32) * grad + np.array(EMERALD, np.float32) * (1 - grad)
    glow, a = radial_glow(w, h, w * 0.5, h * 0.42, max(w, h) * 0.55, np.array((14, 106, 80), np.float32), 0.8)
    base = base * (1 - a) + glow * a
    return Image.fromarray(np.clip(base, 0, 255).astype(np.uint8), "RGB")


# Device

class Phone:
    """A plain, modern phone body drawn around the real screen recording."""

    def __init__(self, screen_h):
        self.sh = int(screen_h)
        self.sw = int(round(screen_h * SRC_W / SRC_H))
        self.bezel = max(8, int(self.sh * 0.012))
        self.w = self.sw + 2 * self.bezel
        self.h = self.sh + 2 * self.bezel
        r_out = int(self.w * 0.135)
        r_in = r_out - self.bezel
        body = Image.new("RGBA", (self.w, self.h), (0, 0, 0, 0))
        d = ImageDraw.Draw(body)
        d.rounded_rectangle([0, 0, self.w - 1, self.h - 1], r_out, fill=(18, 20, 19, 255))
        d.rounded_rectangle([1, 1, self.w - 2, self.h - 2], r_out - 1, outline=(70, 74, 72, 255), width=2)
        self.body = body
        mask = Image.new("L", (self.sw, self.sh), 0)
        ImageDraw.Draw(mask).rounded_rectangle([0, 0, self.sw - 1, self.sh - 1], r_in, fill=255)
        self.mask = mask
        # Soft contact shadow, drawn once.
        pad = int(self.h * 0.12)
        sh = Image.new("L", (self.w + 2 * pad, self.h + 2 * pad), 0)
        ImageDraw.Draw(sh).rounded_rectangle([pad, pad + int(self.h * 0.03), pad + self.w, pad + self.h], r_out, fill=110)
        self.shadow = sh.filter(ImageFilter.GaussianBlur(int(self.h * 0.035)))
        self.pad = pad

    def sheen(self, k):
        """A soft diagonal band of light that crosses the glass once, k from 0 to 1."""
        if not hasattr(self, "_band"):
            w, h = self.sw, self.sh
            band = Image.new("L", (w * 3, h), 0)
            d = ImageDraw.Draw(band)
            for i in range(60):
                a = int(34 * (1 - abs(i - 30) / 30) ** 2)
                d.polygon([(w + i * 6, 0), (w + i * 6 + 6, 0), (w + i * 6 + 6 - h * 0.45, h), (w + i * 6 - h * 0.45, h)], fill=a)
            self._band = band.filter(ImageFilter.GaussianBlur(max(2, w // 60)))
        x = int(mix(-self.sw * 0.2, -self.sw * 2.4, k))
        return self._band.crop((-x, 0, -x + self.sw, self.sh))

    def compose(self, screen_rgb, sheen=None):
        """screen_rgb: PIL RGB at (sw, sh). Returns RGBA phone with shadow, and the shadow pad."""
        out = Image.new("RGBA", self.shadow.size, (0, 0, 0, 0))
        black = Image.new("RGBA", self.shadow.size, (20, 30, 25, 255))
        out.paste(black, (0, 0), self.shadow)
        out.alpha_composite(self.body, (self.pad, self.pad))
        scr = screen_rgb.convert("RGBA")
        if sheen is not None and 0 < sheen < 1:
            white = Image.new("RGBA", scr.size, (255, 255, 255, 255))
            scr = Image.composite(white, scr, self.sheen(sheen))
        scr.putalpha(self.mask)
        out.alpha_composite(scr, (self.pad + self.bezel, self.pad + self.bezel))
        return out


# Clip reading

class ClipReader:
    """Streams frames of a recording, retimed so [t_in, t_out] fills `frames` output frames."""

    def __init__(self, name, t_in, t_out, frames, size):
        self.size = size
        src_len = t_out - t_in
        speed = src_len / (frames / FPS)
        w, h = size
        vf = f"setpts=(PTS-STARTPTS)/{speed:.5f},fps={FPS},scale={w}:{h}:flags=lanczos"
        cmd = ["ffmpeg", "-v", "error", "-ss", f"{t_in:.3f}", "-to", f"{t_out:.3f}", "-i", os.path.join(CLIPS, name + ".mp4"),
               "-vf", vf, "-frames:v", str(frames), "-f", "rawvideo", "-pix_fmt", "rgb24", "-"]
        self.proc = subprocess.Popen(cmd, stdout=subprocess.PIPE)
        self.n = w * h * 3
        self.last = None

    def next(self):
        buf = self.proc.stdout.read(self.n)
        if len(buf) == self.n:
            self.last = Image.frombuffer("RGB", self.size, buf, "raw", "RGB", 0, 1)
        return self.last

    def close(self):
        try:
            self.proc.stdout.close()
            self.proc.kill()
        except Exception:
            pass


class StillReader:
    def __init__(self, path, size):
        self.img = Image.open(path).convert("RGB").resize(size, Image.LANCZOS)

    def next(self):
        return self.img

    def close(self):
        pass


# Scenes

class Scene:
    def __init__(self, dur):
        self.dur = dur
        self.frames = int(round(dur * FPS))

    def start(self, W, H):
        pass

    def frame(self, i, W, H):
        raise NotImplementedError

    def end(self):
        pass


def fade_text(canvas, layer, x, y, t, t_in=0.25, hold_out=None, rise=18):
    """Draws a text layer that rises and fades in from t_in (seconds into the scene)."""
    if layer is None:
        return
    a = ease_out((t - t_in) / 0.9)
    if hold_out is not None:
        a *= 1 - ease_in_out((t - hold_out) / 0.5)
    if a <= 0.01:
        return
    lay = layer.copy()
    alpha = lay.getchannel("A").point(lambda v: int(v * a))
    lay.putalpha(alpha)
    canvas.alpha_composite(lay, (int(x), int(y + (1 - a) * rise)))


class PhoneScene(Scene):
    """The phone with a real recording, text beside it (landscape) or above it (portrait)."""

    def __init__(self, dur, clip, t_in, t_out, title, sub=None, side="right", still=None, enter="rise", phone_scale=1.0,
                 drift=0.03, bg="cream", text_out=None):
        super().__init__(dur)
        self.clip, self.t_in, self.t_out, self.still = clip, t_in, t_out, still
        self.title, self.sub, self.side, self.enter = title, sub, side, enter
        self.phone_scale, self.drift, self.bg_kind, self.text_out = phone_scale, drift, bg, text_out

    def start(self, W, H):
        portrait = H > W
        screen_h = (H * 0.86 if not portrait else H * 0.62) * self.phone_scale
        self.phone = Phone(screen_h)
        self.reader = StillReader(self.still, (self.phone.sw, self.phone.sh)) if self.still else \
            ClipReader(self.clip, self.t_in, self.t_out, self.frames, (self.phone.sw, self.phone.sh))
        self.bg = bg_cream(W, H) if self.bg_kind == "cream" else bg_emerald(W, H)
        dark = self.bg_kind != "cream"
        c1 = CREAM if dark else TEXT
        c2 = (200, 220, 210) if dark else TEXT2
        if not portrait:
            tw = int(W * 0.36)
            lines = [(self.title, SANS_SEMI, int(H * 0.07), c1, int(H * 0.024))]
            if self.sub:
                lines.append((self.sub, SANS, int(H * 0.036), c2, 0))
            self.text = text_layer(lines, tw)
            self.tx = int(W * 0.09) if self.side == "right" else int(W * 0.55)
            self.ty = int(H * 0.5 - self.text.height / 2)
            self.px = int(W * 0.66) if self.side == "right" else int(W * 0.30)
            self.py = int(H * 0.5)
        else:
            tw = int(W * 0.84)
            lines = [(self.title, SANS_SEMI, int(W * 0.074), c1, int(W * 0.022))]
            if self.sub:
                lines.append((self.sub, SANS, int(W * 0.038), c2, 0))
            self.text = text_layer(lines, tw, align="center")
            self.tx = int((W - tw) / 2)
            self.ty = int(H * 0.085)
            self.px = int(W * 0.5)
            self.py = int(H * 0.6)

    def frame(self, i, W, H):
        t = i / FPS
        canvas = self.bg.convert("RGBA")
        screen = self.reader.next()
        phone = self.phone.compose(screen, sheen=(t - 0.5) / 1.6)
        # Entrance: the phone rises into place on a soft spring, then drifts very slightly.
        e = ease_out(t / 0.9) if self.enter == "rise" else 1.0
        scale = mix(0.94, 1.0, e) * (1 + self.drift * ease_in_out(t / self.dur))
        dy = (1 - e) * H * 0.08
        pw, ph = int(phone.width * scale), int(phone.height * scale)
        phone_s = phone.resize((pw, ph), Image.LANCZOS) if abs(scale - 1) > 1e-3 else phone
        if self.enter == "rise" and e < 1:
            a = phone_s.getchannel("A").point(lambda v: int(v * clamp(e * 1.4)))
            phone_s.putalpha(a)
        canvas.alpha_composite(phone_s, (int(self.px - pw / 2), int(self.py - ph / 2 + dy)))
        fade_text(canvas, self.text, self.tx, self.ty, t, t_in=0.35, hold_out=self.text_out)
        return canvas.convert("RGB")

    def end(self):
        self.reader.close()


class OpeningScene(Scene):
    """A woman reading the Quran by the window. Lines arrive one at a time on the cream page."""

    def __init__(self, dur, lines):
        super().__init__(dur)
        self.lines = lines

    def start(self, W, H):
        self.portrait = H > W
        photo = Image.open(PHOTO).convert("RGB")
        if not self.portrait:
            ph = H
            pw = int(photo.width * H / photo.height)
            self.photo = photo.resize((int(pw * 1.08), int(ph * 1.08)), Image.LANCZOS)
            self.region = (0, 0, pw, H)
        else:
            pw = W
            ph = int(photo.height * W / photo.width)
            self.photo = photo.resize((int(pw * 1.08), int(ph * 1.08)), Image.LANCZOS)
            self.region = (0, 0, W, int(H * 0.64))
        self.bg = bg_cream(W, H)
        self.layers = []
        for k, (text, path, size_ratio) in enumerate(self.lines):
            size = int((H if not self.portrait else W) * size_ratio)
            width = int(W * (0.42 if not self.portrait else 0.84))
            self.layers.append(text_layer([(text, path, size, TEXT, 0)], width, align="left" if not self.portrait else "center"))

    def frame(self, i, W, H):
        t = i / FPS
        canvas = self.bg.convert("RGBA")
        x0, y0, x1, y1 = self.region
        rw, rh = x1 - x0, y1 - y0
        # Slow push in, like a camera on a slider, 1.08 to 1.0 of the oversized photo.
        k = ease_in_out(t / self.dur)
        crop_w = int(mix(self.photo.width, self.photo.width / 1.08, k))
        crop_h = int(crop_w * rh / rw)
        cx = self.photo.width / 2
        cy = self.photo.height * mix(0.48, 0.42, k)
        box = (int(cx - crop_w / 2), int(clamp(cy - crop_h / 2, 0, self.photo.height - crop_h)), 0, 0)
        box = (box[0], box[1], box[0] + crop_w, box[1] + crop_h)
        plate = self.photo.crop(box).resize((rw, rh), Image.LANCZOS)
        # Light rises from black on the first second.
        lift = ease_out(t / 1.6)
        plate = Image.blend(Image.new("RGB", plate.size, (12, 16, 14)), plate, lift)
        canvas.paste(plate, (x0, y0))
        # A soft edge where the photo meets the page.
        if not self.portrait:
            edge = Image.new("L", (160, H))
            for xx in range(160):
                ImageDraw.Draw(edge).line([(xx, 0), (xx, H)], fill=int(255 * (xx / 160) ** 1.6))
            cream = self.bg.crop((x1 - 160, 0, x1, H)).convert("RGBA")
            cream.putalpha(edge)
            canvas.alpha_composite(cream, (x1 - 160, 0))
            tx = int(x1 + (W - x1) * 0.12)
            ty = int(H * 0.36)
        else:
            edge = Image.new("L", (W, 220))
            for yy in range(220):
                ImageDraw.Draw(edge).line([(0, yy), (W, yy)], fill=int(255 * (yy / 220) ** 1.6))
            cream = self.bg.crop((0, y1 - 220, W, y1)).convert("RGBA")
            cream.putalpha(edge)
            canvas.alpha_composite(cream, (0, y1 - 220))
            tx = int(W * 0.08)
            ty = int(y1 + H * 0.04)
        y = ty
        for k2, layer in enumerate(self.layers):
            fade_text(canvas, layer, tx, y, t, t_in=1.0 + k2 * 1.25)
            y += layer.height + int(H * 0.02)
        # Out: the page brightens to cream so the phone can rise on the same ground.
        out = ease_in_out((t - (self.dur - 0.6)) / 0.6)
        if out > 0:
            canvas = Image.blend(canvas.convert("RGB"), self.bg, out).convert("RGBA")
        return canvas.convert("RGB")


class EndCard(Scene):
    def __init__(self, dur, cta="Discover the app"):
        super().__init__(dur)
        self.cta = cta

    def start(self, W, H):
        self.portrait = H > W
        self.bg = bg_emerald(W, H)
        unit = H if not self.portrait else W
        size = int(unit * (0.22 if not self.portrait else 0.32))
        icon = Image.open(ICON).convert("RGBA").resize((size, size), Image.LANCZOS)
        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], int(size * 0.225), fill=255)
        icon.putalpha(mask)
        shadow = Image.new("RGBA", (size + 120, size + 120), (0, 0, 0, 0))
        sm = Image.new("L", shadow.size, 0)
        ImageDraw.Draw(sm).rounded_rectangle([60, 72, 60 + size, 72 + size], int(size * 0.225), fill=120)
        shadow.paste((0, 20, 14, 255), (0, 0), sm.filter(ImageFilter.GaussianBlur(26)))
        shadow.alpha_composite(icon, (60, 60))
        self.icon = shadow
        w = int(W * (0.7 if not self.portrait else 0.86))
        self.name = text_layer([("Quran for Women", SERIF, int(unit * (0.075 if not self.portrait else 0.088)), CREAM, 0)], w, "center")
        self.tag = text_layer([("Your Daily Companion to the Quran, Dua & a Peaceful Heart.", SANS_LIGHT,
                                int(unit * (0.032 if not self.portrait else 0.042)), (226, 238, 232), 0)], w, "center")
        f = font(SANS_SEMI, int(unit * (0.026 if not self.portrait else 0.036)))
        d = ImageDraw.Draw(Image.new("RGB", (10, 10)))
        tw = int(d.textlength(self.cta, font=f))
        bw, bh = tw + int(unit * 0.08), int(unit * (0.07 if not self.portrait else 0.09))
        btn = Image.new("RGBA", (bw, bh), (0, 0, 0, 0))
        bd = ImageDraw.Draw(btn)
        bd.rounded_rectangle([0, 0, bw - 1, bh - 1], bh // 2, fill=CREAM)
        bb = bd.textbbox((0, 0), self.cta, font=f)
        bd.text(((bw - (bb[2] - bb[0])) / 2 - bb[0], (bh - (bb[3] - bb[1])) / 2 - bb[1]), self.cta, font=f, fill=EMERALD)
        self.btn = btn
        self.platforms = text_layer([("For iPhone and Android", SANS, int(unit * (0.022 if not self.portrait else 0.03)), (180, 210, 196), 0)], w, "center")
        self.w = w

    def frame(self, i, W, H):
        t = i / FPS
        canvas = self.bg.convert("RGBA")
        # The icon settles with a small spring, then the name, the line, and the invitation.
        e = ease_out(t / 1.0)
        s = mix(0.86, 1.0, e)
        ic = self.icon.resize((int(self.icon.width * s), int(self.icon.height * s)), Image.LANCZOS)
        a = ic.getchannel("A").point(lambda v: int(v * clamp(e * 1.3)))
        ic.putalpha(a)
        cy = H * (0.30 if not self.portrait else 0.30)
        canvas.alpha_composite(ic, (int(W / 2 - ic.width / 2), int(cy - ic.height / 2)))
        y = int(cy + self.icon.height * 0.5 + H * 0.02)
        fade_text(canvas, self.name, (W - self.w) / 2, y, t, t_in=0.5)
        y += self.name.height + int(H * 0.018)
        fade_text(canvas, self.tag, (W - self.w) / 2, y, t, t_in=1.0)
        y += self.tag.height + int(H * 0.05)
        fade_text(canvas, self.btn, (W - self.btn.width) / 2, y, t, t_in=1.7)
        y += self.btn.height + int(H * 0.035)
        fade_text(canvas, self.platforms, (W - self.w) / 2, y, t, t_in=2.1)
        return canvas.convert("RGB")


# Transitions: each scene's last frames cross into the next with a short, eased dissolve.

_grain = None


def grain(img):
    """Fine, moving film grain, just enough to keep large flat areas from banding at 4K."""
    global _grain
    W, H = img.size
    if _grain is None or _grain[0].shape[:2] != (H, W):
        rng = np.random.default_rng(5)
        _grain = [rng.normal(0, 2.2, (H, W, 1)).astype(np.float32) for _ in range(6)]
    grain.i = getattr(grain, "i", 0) + 1
    arr = np.asarray(img, np.float32) + _grain[grain.i % 6]
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGB")


def render(name, W, H, scenes, overlap=0.4, audio_bell_at=None, cues=(), resolve_at=None):
    total = sum(s.frames for s in scenes) - int(overlap * FPS) * (len(scenes) - 1)
    out_path = os.path.join(OUT, f"{name}.mp4")
    video_tmp = os.path.join(OUT, f"{name}_video.mp4")
    enc = subprocess.Popen(
        ["ffmpeg", "-v", "error", "-y", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(FPS), "-i", "-",
         "-c:v", "libx264", "-preset", "medium", "-crf", "15", "-tune", "film", "-pix_fmt", "yuv420p", "-movflags", "+faststart", video_tmp],
        stdin=subprocess.PIPE)
    ov = int(overlap * FPS)
    written = 0
    prev_tail = []
    for si, sc in enumerate(scenes):
        sc.start(W, H)
        frames = [None] * sc.frames
        for i in range(sc.frames):
            img = sc.frame(i, W, H)
            if si > 0 and i < ov and prev_tail:
                k = ease_in_out((i + 1) / (ov + 1))
                img = Image.blend(prev_tail[i], img, k)
            if si < len(scenes) - 1 and i >= sc.frames - ov:
                frames[i] = img
                continue
            enc.stdin.write(grain(img).tobytes())
            written += 1
        prev_tail = [f for f in frames[sc.frames - ov:]] if si < len(scenes) - 1 else []
        sc.end()
        print(f"  scene {si + 1}/{len(scenes)} done", flush=True)
    enc.stdin.close()
    enc.wait()
    duration = written / FPS
    import audio
    audio.mix(duration, list(cues), os.path.join(OUT, f"{name}_audio.wav"), os.path.join(HERE, "vo_cache"),
              resolve_at if resolve_at is not None else duration - 6, bell_at=audio_bell_at)
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", video_tmp, "-i", os.path.join(OUT, f"{name}_audio.wav"),
                    "-c:v", "copy", "-c:a", "aac", "-b:a", "256k", "-shortest", out_path], check=True)
    os.remove(video_tmp)
    os.remove(os.path.join(OUT, f"{name}_audio.wav"))
    print(f"wrote {out_path} ({duration:.1f}s)")


def make_audio(name, duration, bell_at):
    """A quiet room tone that breathes with the film, and the app's own prayer bell on the end card.
    No music and no recitation: the film stays respectful for viewers who avoid instruments, and no
    sacred audio is used as decoration."""
    sr = 48000
    n = int(duration * sr)
    rng = np.random.default_rng(7)
    white = rng.standard_normal(n).astype(np.float32)
    # Brown noise for a warm room, a gentle band of air above it.
    brown = np.cumsum(white) / 300.0
    brown -= np.convolve(brown, np.ones(4801) / 4801, mode="same")
    air = white.copy()
    k = 40
    air = np.convolve(air, np.ones(k) / k, mode="same") - np.convolve(air, np.ones(400) / 400, mode="same")
    t = np.arange(n) / sr
    swell = 0.55 + 0.45 * np.sin(2 * np.pi * t / 14.0 - 1.2) ** 2
    fade = np.clip(t / 2.0, 0, 1) * np.clip((duration - t) / 2.5, 0, 1)
    mixd = (brown * 0.22 + air * 0.035) * swell * fade
    if bell_at is not None and os.path.exists(BELL):
        import wave
        with wave.open(BELL) as w:
            raw = w.readframes(w.getnframes())
            bell = np.frombuffer(raw, dtype=np.int16).astype(np.float32) / 32768
            if w.getnchannels() == 2:
                bell = bell.reshape(-1, 2).mean(axis=1)
            if w.getframerate() != sr:
                idx = np.linspace(0, len(bell) - 1, int(len(bell) * sr / w.getframerate()))
                bell = np.interp(idx, np.arange(len(bell)), bell)
        start = int(bell_at * sr)
        end = min(n, start + len(bell))
        mixd[start:end] += bell[: end - start] * 0.32
    peak = np.max(np.abs(mixd)) or 1
    mixd = mixd / peak * 0.5
    stereo = np.stack([mixd, mixd * 0.98], axis=1)
    import wave
    with wave.open(os.path.join(OUT, f"{name}_audio.wav"), "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(sr)
        w.writeframes((stereo * 32767).astype(np.int16).tobytes())


# The films

MASTER_VO = [
    (0.9, "Before the day begins, make a little room for Allah."),
    (6.4, "Quran for Women."),
    (10.6, "Each day opens with a verse, your prayers, and a moment for your heart."),
    (16.0, "Read the whole Quran, page by page."),
    (22.6, "Or listen, verse by verse, with its meaning."),
    (27.8, "Find a dua for whatever you are carrying, with its source."),
    (33.6, "Remember Allah, one count at a time."),
    (38.3, "Write what is on your heart. It stays yours."),
    (42.9, "Prayer times and the Qibla, wherever you are."),
    (51.3, "Quran for Women. Your daily companion to the Quran, dua, and a peaceful heart."),
]

VERTICAL_VO = [
    (0.6, "Before the day begins, make a little room for Allah."),
    (4.8, "Each day opens with a verse."),
    (8.3, "Read the whole Quran, page by page."),
    (13.4, "A dua for whatever you are carrying."),
    (17.5, "Remember Allah, one count at a time."),
    (21.1, "Quran for Women. Your daily companion to the Quran, dua, and a peaceful heart."),
]


def master():
    W, H = 3840, 2160
    scenes = [
        OpeningScene(6.2, [
            ("Before the day begins,", SERIF, 0.058),
            ("a moment that is yours.", SERIF_ITALIC, 0.058),
        ]),
        PhoneScene(4.6, "c01_open", 2.9, 9.6, "Quran for Women", "Your daily companion to the Quran, dua and a peaceful heart.", side="right"),
        PhoneScene(5.6, "c02_today", 0.4, 10.0, "Every day begins here.", "A verse for today, your five prayers, and a moment for your heart.", side="left"),
        PhoneScene(7.2, "c03_mushaf", 2.6, 15.2, "The Mushaf.", "All 604 pages. Turn them the way you always have.", side="right"),
        PhoneScene(5.6, "c04_reader", 2.8, 13.6, "Listen, verse by verse.", "The Arabic, its meaning, and a recitation that follows along.", side="left"),
        PhoneScene(6.4, "c05_dua", 3.6, 17.0, "A dua for the hard moments.", "Every dua shows where it comes from.", side="right"),
        PhoneScene(5.0, "c06_dhikr", 5.0, 15.6, "Remember Allah,", "one count at a time.", side="left"),
        PhoneScene(5.0, "c07_journal", 4.6, 15.6, "Write what is on your heart.", "Your journal stays on your phone.", side="right"),
        PhoneScene(5.6, "c08_prayer", 2.4, 16.8, "Prayer times and the Qibla,", "wherever you are.", side="left"),
        PhoneScene(3.6, None, 0, 0, "At a glance.", "The next prayer, right on your home screen.", side="right",
                   still=os.path.join(CLIPS, "home_widgets.png")),
        EndCard(7.4),
    ]
    total = sum(s.dur for s in scenes) - 0.4 * (len(scenes) - 1)
    render("quran_for_women_master_60s_4k", W, H, scenes, audio_bell_at=total - 6.0, cues=MASTER_VO, resolve_at=total - 7.0)


def vertical():
    W, H = 2160, 3840
    scenes = [
        OpeningScene(4.4, [
            ("Before the day begins,", SERIF, 0.07),
            ("a moment that is yours.", SERIF_ITALIC, 0.07),
        ]),
        PhoneScene(4.2, "c02_today", 0.4, 8.6, "Every day begins here.", None),
        PhoneScene(5.6, "c03_mushaf", 2.6, 14.0, "The Mushaf.", "All 604 pages."),
        PhoneScene(4.6, "c05_dua", 3.6, 15.0, "A dua for the hard moments.", "With its source."),
        PhoneScene(4.0, "c06_dhikr", 5.0, 15.6, "Remember Allah,", "one count at a time."),
        EndCard(7.2),
    ]
    total = sum(s.dur for s in scenes) - 0.4 * (len(scenes) - 1)
    render("quran_for_women_vertical_30s_4k", W, H, scenes, audio_bell_at=total - 5.8, cues=VERTICAL_VO, resolve_at=total - 6.8)


def hook():
    W, H = 2160, 3840
    scenes = [
        PhoneScene(4.0, "c03_mushaf", 6.0, 13.0, "The Quran, in your hand.", None, phone_scale=1.08),
        EndCard(2.4),
    ]
    render("quran_for_women_hook_6s_4k", W, H, scenes, audio_bell_at=None, cues=[(4.2, "Quran for Women.")], resolve_at=3.8)


if __name__ == "__main__":
    which = sys.argv[1] if len(sys.argv) > 1 else "master"
    {"master": master, "vertical": vertical, "hook": hook}[which]()
