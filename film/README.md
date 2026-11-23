# Quran for Women · Launch film

Every screen in the film is the real app, recorded on an Android phone emulator. Nothing is mocked up.

## Files (in `out/`)

| File | Format | Use |
|---|---|---|
| `quran_for_women_master_60s_4k.mp4` | 3840 x 2160 (4K), 30 fps, 58 s | Website, YouTube, launch post |
| `quran_for_women_vertical_30s_4k.mp4` | 2160 x 3840 (4K), 30 fps, 28 s | Reels, TikTok, Shorts, Stories |
| `quran_for_women_hook_6s_4k.mp4` | 2160 x 3840 (4K), 30 fps, 6 s | Paid social opener |

Rebuild any of them with `python film/render.py master|vertical|hook`. The recordings are in `clips/`.

## The story

**Whatever the day brings, return to what grounds you.**

Stillness, then recognition, discovery, connection, peace, and an invitation.

| Time | Scene | On screen |
|---|---|---|
| 0:00 | A woman reading the Quran by the window. The light rises. | Before the day begins, / a moment that is yours. |
| 0:06 | The phone rises. The lotus draws itself and the app opens. | Quran for Women |
| 0:10 | Today: the sky, the five prayers, the verse of the day. | Every day begins here. |
| 0:16 | The Mushaf: the cover opens, the pages turn. | The Mushaf. All 604 pages. |
| 0:22 | The reader. Recitation plays and the highlight follows each verse. | Listen, verse by verse. |
| 0:28 | Worry and Hardship, then "For worry and sadness", Sahih al-Bukhari 6369. Saved. | A dua for the hard moments. |
| 0:34 | Dhikr: SubhanAllah, counted to 33. The ring turns gold. | Remember Allah, one count at a time. |
| 0:39 | The journal: a page is written and saved. | Write what is on your heart. |
| 0:44 | Prayer times, the Isha countdown, the Qibla. | Prayer times and the Qibla, wherever you are. |
| 0:49 | The next prayer widget on the home screen. | At a glance. |
| 0:52 | End card: icon, name, tagline, invitation. | Quran for Women · Your Daily Companion to the Quran, Dua & a Peaceful Heart. · Discover the app · For iPhone and Android |

Every verse and dua on screen comes from the app's verified data: Al-Fatihah and Al-Baqarah (Tanzil text), Al-Baqarah 2:153 as the verse of the day, and the dua from Sahih al-Bukhari 6369 with its source shown.

## Sound

- **Score:** original, written in code in `audio.py` (soft piano, warm strings, low bass, long reverb, about 72 bpm in D major). It starts with piano alone, the strings enter after the first line, it fills out in the middle, and it resolves on a held D major chord as the end card arrives, with the app's own prayer bell. Because it is generated, there are no music licences to clear.
- **Voiceover:** the product led script below, read by a neural voice (Microsoft "Ava", via edge-tts) and mixed with the music dipping about 8 dB under each line. Treat it as a strong guide track: for the store launch, have a professional voice read the same lines at the same times (voice ready files go in `vo_cache`, or edit the cues in `render.py`).
- **No recitation** is used under music, text or transitions.
- To make a version without music (some viewers prefer it), call `audio.mix(..., music=False)` in `render.py`.

## Before you publish it

- **The welcome photo.** The opening uses the app's welcome photo. Confirm you own the rights to use it in advertising, or replace it with a filmed shot (see below).
- **No store badges yet.** The end card says "Discover the app" and "For iPhone and Android". Add the official App Store and Google Play badges only once the listings are live, and follow each store's badge rules.
- **Android footage.** The film shows the Android app. That is right for Google Play. For App Store ads, Apple requires iPhone footage: record the same scenes on an iPhone once the app is built on a Mac, then run the renderer with those clips.
- **No claims.** The film promises nothing the app does not do, and makes no claims about peace, healing, or answered prayers.

## What a production studio would add

This film is made only from the app and one photo. A studio version would add:
- A filmed opening: a woman in a blush hijab by a window at first light, an open Quran on a stand (never on the floor), shot on a 35mm or 50mm lens at about f/2, slow push in, natural light only.
- A real hand holding the phone for the Mushaf page turn, matched to the on-screen turn.
- A licensed voiceover, mixed to -16 LUFS for social and -14 LUFS for YouTube.
