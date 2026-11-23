"""Synthesizes the prayer notification bell: three warm bell strikes, loud and clear.

Original sound made from sine partials, so there are no licensing issues.
iOS plays notification sounds up to 30 seconds, from Linear PCM WAV, AIFF or CAF.

Usage: python -I tools/make_prayer_bell.py <output.wav>
"""

import math
import struct
import sys
import wave

RATE = 44100

def bell(freq, start, length, gain):
    """A struck bell: inharmonic partials with different decay times."""
    partials = [(1.0, 1.0, 1.6), (2.0, 0.55, 1.1), (2.76, 0.35, 0.8), (5.4, 0.18, 0.45), (0.5, 0.25, 2.2)]
    samples = {}
    n = int(length * RATE)
    for i in range(n):
        t = i / RATE
        attack = min(1.0, t / 0.004)
        v = 0.0
        for ratio, amp, decay in partials:
            v += amp * math.exp(-t / decay) * math.sin(2 * math.pi * freq * ratio * t)
        samples[int(start * RATE) + i] = v * attack * gain
    return samples

def main(path):
    total = int(9.0 * RATE)
    mix = [0.0] * total
    # Three strikes, descending, then a final sustained strike.
    strikes = [(659.25, 0.0), (523.25, 1.1), (440.0, 2.2), (523.25, 3.6)]
    for freq, start in strikes:
        for idx, v in bell(freq, start, 5.0, 1.0).items():
            if idx < total:
                mix[idx] += v
    # Fade the tail, then normalize near full scale so it is loud.
    for i in range(total):
        t = i / RATE
        if t > 8.0:
            mix[i] *= max(0.0, 9.0 - t)
    peak = max(abs(v) for v in mix) or 1.0
    scale = 0.95 / peak
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, v * scale)) * 32767)) for v in mix))
    print("saved", path)

main(sys.argv[1])
