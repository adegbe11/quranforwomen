"""Synthesizes the soft dhikr counting click: two small beads touching.

Original sound made from damped sine partials and a short breath of noise,
so there are no licensing issues. Short and quiet on purpose, so it can play
on every count without tiring the ear.

Usage: python -I tools/make_dhikr_click.py <output.wav> [<another.wav> ...]
"""

import math
import random
import struct
import sys
import wave

RATE = 44100
LENGTH = 0.09  # seconds


def click():
    rng = random.Random(33)
    n = int(LENGTH * RATE)
    out = []
    # Inharmonic partials, like a small wooden or glass bead.
    partials = [(1850.0, 1.0, 0.018), (2930.0, 0.55, 0.011), (4470.0, 0.25, 0.007), (920.0, 0.35, 0.026)]
    low = 0.0
    for i in range(n):
        t = i / RATE
        attack = min(1.0, t / 0.0008)
        v = 0.0
        for freq, amp, decay in partials:
            v += amp * math.exp(-t / decay) * math.sin(2 * math.pi * freq * t)
        # A very short, softened tick of noise at the moment of touch.
        noise = rng.uniform(-1.0, 1.0)
        low += 0.25 * (noise - low)
        v += 0.6 * low * math.exp(-t / 0.004)
        out.append(v * attack)
    # Gentle fade so the tail never clicks off.
    fade = int(0.02 * RATE)
    for i in range(fade):
        out[n - fade + i] *= 1.0 - i / fade
    return out


def main(paths):
    samples = click()
    peak = max(abs(v) for v in samples) or 1.0
    scale = 0.5 / peak  # soft, well below full scale
    data = b"".join(struct.pack("<h", int(max(-1.0, min(1.0, v * scale)) * 32767)) for v in samples)
    for path in paths:
        with wave.open(path, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(data)
        print("saved", path)


main(sys.argv[1:])
