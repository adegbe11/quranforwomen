"""Sound for the launch film: an original score, the voiceover, and the mix.

The score is synthesized here from scratch (soft piano, warm strings pad, low bass, long reverb),
so it is fully original and free of licensing questions. The voiceover is generated with a neural
voice (edge-tts) as a high quality guide; for the store launch, record a professional voice over the
same script and timings (see README.md) and drop it in with `--vo-dir`.
"""

import asyncio
import os
import subprocess
import wave

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SR = 48000
BELL = os.path.join(ROOT, "android", "app", "src", "main", "res", "raw", "prayer_bell.wav")
VOICE = "en-US-AvaNeural"
RATE = "-8%"
PITCH = "-2Hz"


# Voiceover

async def _speak(text, path):
    import edge_tts
    await edge_tts.Communicate(text, VOICE, rate=RATE, pitch=PITCH).save(path)


def voice_line(text, cache_dir):
    os.makedirs(cache_dir, exist_ok=True)
    import hashlib
    key = hashlib.sha1(f"{text}|{VOICE}|{RATE}|{PITCH}".encode()).hexdigest()[:16]
    mp3 = os.path.join(cache_dir, key + ".mp3")
    wav = os.path.join(cache_dir, key + ".wav")
    if not os.path.exists(wav):
        if not os.path.exists(mp3):
            import time
            for attempt in range(8):
                try:
                    tmp = mp3 + ".part"
                    asyncio.run(_speak(text, tmp))
                    if os.path.getsize(tmp) < 2000:
                        raise RuntimeError("empty audio")
                    os.replace(tmp, mp3)
                    break
                except Exception:
                    if attempt == 7:
                        raise
                    time.sleep(4 + attempt * 3)
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", mp3, "-ar", str(SR), "-ac", "1", wav], check=True)
    return read_wav(wav)


def read_wav(path):
    with wave.open(path) as w:
        raw = w.readframes(w.getnframes())
        data = np.frombuffer(raw, dtype=np.int16).astype(np.float32) / 32768
        if w.getnchannels() == 2:
            data = data.reshape(-1, 2).mean(axis=1)
        if w.getframerate() != SR:
            idx = np.linspace(0, len(data) - 1, int(len(data) * SR / w.getframerate()))
            data = np.interp(idx, np.arange(len(data)), data)
    return data.astype(np.float32)


# Score

NOTE = {n: i for i, n in enumerate(["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"])}


def hz(name):
    """'D4' -> frequency."""
    n, octave = name[:-1], int(name[-1])
    return 440.0 * 2 ** ((NOTE[n] + 12 * (octave + 1) - 69) / 12)


def adsr(n, a, d, s, r, sustain_len):
    env = np.zeros(n, np.float32)
    ai, di, si = int(a * SR), int(d * SR), int(sustain_len * SR)
    ri = int(r * SR)
    idx = 0
    seg = min(ai, n); env[:seg] = np.linspace(0, 1, seg, endpoint=False); idx = seg
    seg = min(di, n - idx); env[idx:idx + seg] = np.linspace(1, s, seg, endpoint=False); idx += seg
    seg = min(max(0, si - ai - di), n - idx); env[idx:idx + seg] = s; idx += seg
    seg = min(ri, n - idx); env[idx:idx + seg] = np.linspace(s, 0, seg); idx += seg
    return env


def pad_note(f, length, rng):
    n = int((length + 2.0) * SR)
    t = np.arange(n) / SR
    out = np.zeros(n, np.float32)
    for detune in (-0.0016, 0.0, 0.0019):
        ff = f * (1 + detune)
        phase = rng.uniform(0, 2 * np.pi)
        # Soft, string-like tone: a few gentle partials.
        out += np.sin(2 * np.pi * ff * t + phase)
        out += 0.32 * np.sin(2 * np.pi * 2 * ff * t + phase * 1.3)
        out += 0.12 * np.sin(2 * np.pi * 3 * ff * t + phase * 0.7)
    vib = 1 + 0.004 * np.sin(2 * np.pi * 4.8 * t)
    return out * vib * adsr(n, 1.4, 0.6, 0.85, 1.9, length) / 3


def piano_note(f, vel=1.0, length=3.5):
    n = int(length * SR)
    t = np.arange(n) / SR
    tone = np.zeros(n, np.float32)
    for k, amp in ((1, 1.0), (2, 0.42), (3, 0.18), (4, 0.09), (5, 0.05)):
        inharm = 1 + 0.0004 * k * k
        tone += amp * np.sin(2 * np.pi * f * k * inharm * t) * np.exp(-t * (1.6 + 0.9 * k))
    attack = np.clip(t / 0.004, 0, 1)
    return tone * attack * vel * 0.55


def reverb(x, seconds=3.2, wet=0.38, seed=3):
    rng = np.random.default_rng(seed)
    n = int(seconds * SR)
    ir = rng.standard_normal(n).astype(np.float32) * np.exp(-np.arange(n) / SR * (6.5 / seconds))
    # Darken the tail.
    ir = np.convolve(ir, np.ones(24) / 24, mode="same")
    ir /= np.sqrt(np.sum(ir ** 2))
    size = 1 << int(np.ceil(np.log2(len(x) + n)))
    y = np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(ir, size), size)[: len(x)]
    return (1 - wet) * x + wet * y.astype(np.float32)


def lowpass(x, cutoff):
    """Gentle low pass in the frequency domain, with a smooth roll off above the cutoff."""
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    spec *= 1 / np.sqrt(1 + (f / cutoff) ** 4)
    return np.fft.irfft(spec, len(x)).astype(np.float32)


def score(duration, resolve_at):
    """About 72 bpm in D major. Quiet piano alone, strings enter, a fuller middle, and a resolution
    on the end card that lets the last chord ring."""
    rng = np.random.default_rng(11)
    n = int((duration + 1) * SR)
    pad = np.zeros(n, np.float32)
    keys = np.zeros(n, np.float32)
    bass = np.zeros(n, np.float32)
    bar = 60 / 72 * 4  # seconds
    chords = [
        ("D", ["D3", "A3", "E4", "F#4"], ["A4", "F#4", "E4", "D4"]),
        ("B", ["B2", "F#3", "D4", "A4"], ["F#4", "D4", "C#4", "B3"]),
        ("G", ["G2", "D3", "B3", "F#4"], ["B4", "A4", "F#4", "D4"]),
        ("A", ["A2", "E3", "C#4", "G4"], ["E4", "C#4", "D4", "E4"]),
    ]
    t = 0.0
    bar_i = 0
    while t < resolve_at - 0.2:
        root, voicing, melody = chords[bar_i % 4]
        length = min(bar, resolve_at - t)
        s = int(t * SR)
        # Strings enter after the opening line, and swell in the middle of the film.
        strings = 0.0 if t < 4.5 else (0.55 if t < 14 else 0.85)
        if strings:
            for note in voicing:
                v = pad_note(hz(note), length, rng)
                e = min(n, s + len(v))
                pad[s:e] += v[: e - s] * strings
        if t >= 9:
            v = pad_note(hz(voicing[0]) / 2, length, rng) * 0.9
            e = min(n, s + len(v))
            bass[s:e] += v[: e - s]
        # The piano: one note per beat, gentle, with a softer echo an octave up from the second half.
        for k, mn in enumerate(melody):
            nt = t + k * bar / 4
            if nt >= resolve_at - 0.1:
                break
            vel = 0.75 if k == 0 else 0.55
            p = piano_note(hz(mn), vel)
            ss = int(nt * SR)
            e = min(n, ss + len(p))
            keys[ss:e] += p[: e - ss]
            if t > 20 and k in (1, 3):
                up = piano_note(hz(mn) * 2, 0.18)
                ss2 = int((nt + bar / 8) * SR)
                e2 = min(n, ss2 + len(up))
                keys[ss2:e2] += up[: e2 - ss2]
        t += bar
        bar_i += 1
    # Resolution: D major, held to the end.
    s = int(resolve_at * SR)
    hold = duration - resolve_at + 0.5
    for note in ["D2", "A2", "D3", "A3", "D4", "F#4", "A4"]:
        v = pad_note(hz(note), hold, rng) * (0.8 if hz(note) > 200 else 0.6)
        e = min(n, s + len(v))
        pad[s:e] += v[: e - s]
    for k, mn in enumerate(["D4", "F#4", "A4", "D5"]):
        p = piano_note(hz(mn), 0.6 - k * 0.08, 5.0)
        ss = s + int(k * 0.32 * SR)
        e = min(n, ss + len(p))
        keys[ss:e] += p[: e - ss]
    pad = lowpass(pad, 2600)
    mixd = pad * 0.30 + keys * 0.62 + bass * 0.32
    mixd = reverb(mixd)
    tt = np.arange(n) / SR
    mixd *= np.clip(tt / 1.5, 0, 1) * np.clip((duration - tt) / 3.0, 0, 1)
    return mixd[: int(duration * SR)]


# Mix

def mix(duration, cues, out_path, cache_dir, resolve_at, bell_at=None, music=True):
    n = int(duration * SR)
    music_track = score(duration, resolve_at) if music else np.zeros(n, np.float32)
    music_track = music_track / (np.max(np.abs(music_track)) or 1) * 0.55
    vo = np.zeros(n, np.float32)
    duck = np.ones(n, np.float32)
    for at, text in cues:
        line = voice_line(text, cache_dir)
        s = int(at * SR)
        e = min(n, s + len(line))
        vo[s:e] += line[: e - s]
        # Duck the music by about 9 dB under the voice, easing in and out.
        a, b = max(0, s - int(0.35 * SR)), min(n, e + int(0.6 * SR))
        duck[a:b] = np.minimum(duck[a:b], 0.4)
    k = int(0.3 * SR)
    c = np.cumsum(np.concatenate([np.full(k // 2, duck[0]), duck, np.full(k - k // 2, duck[-1])]))
    duck = ((c[k:] - c[:-k]) / k)[:n].astype(np.float32)
    vo = vo / (np.max(np.abs(vo)) or 1) * 0.82
    out = music_track * duck + vo
    if bell_at is not None and os.path.exists(BELL):
        bell = read_wav(BELL)
        s = int(bell_at * SR)
        e = min(n, s + len(bell))
        out[s:e] += bell[: e - s] * 0.16
    out = out / (np.max(np.abs(out)) or 1) * 0.89  # about -1 dBFS peak
    left = out
    right = np.concatenate([np.zeros(18, np.float32), out[:-18]])  # a touch of width
    stereo = np.stack([left, right], axis=1)
    with wave.open(out_path, "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((np.clip(stereo, -1, 1) * 32767).astype(np.int16).tobytes())
