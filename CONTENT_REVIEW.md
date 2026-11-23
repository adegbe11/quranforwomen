# Content Review Checklist

Religious content must be checked by a qualified person before release. This file lists exactly what needs review and why.

## Safe by construction (spot check only)

- **Quran text**: bundled unmodified from Tanzil Uthmani. `build_content.py` checks 114 surahs and 6236 verses.
- **Quranic duas** (23 entries): the Arabic is cut from the verified verse text, starting at a marked word. Check that each start point is correct. The script prints all of them.
  - 2:201, 2:250, 3:8, 3:35, 3:38, 3:173, 7:23, 14:40, 14:41, 17:24, 20:25 to 28, 20:114, 21:87, 23:118, 25:74, 27:19, 28:24, 46:15, 66:11, 2:255, 112, 113, 114

## Needs scholar review

Hadith duas. The Arabic was typed, not copied from a verified digital source. Check each against the cited collection: wording, diacritics, and reference number.

| ID | Title | Cited source |
|---|---|---|
| waking | On waking up | Sahih al-Bukhari 6324 |
| morning-bika | In the morning | Sunan Abi Dawud 5068 · Jami' at-Tirmidhi 3391 |
| no-harm | For protection, morning and evening | Sunan Abi Dawud 5088 · Jami' at-Tirmidhi 3388 |
| heart-firm | For a steady heart | Jami' at-Tirmidhi 2140 |
| huda | For guidance and contentment | Sahih Muslim 2721 |
| hamm-hazan | For worry and sadness | Sahih al-Bukhari 6369 |
| distress | In times of distress | Sahih al-Bukhari 6346 · Sahih Muslim 2730 |
| ya-hayyu | Asking for His mercy | Jami' at-Tirmidhi 3524 |
| musibah | After a loss | Sahih Muslim 918 |
| a-inni | Help me remember You | Sunan Abi Dawud 1522 |
| barakallah | For a newly married couple | Sunan Abi Dawud 2130 · Jami' at-Tirmidhi 1091 |
| sayyid-istighfar | The best way to ask forgiveness | Sahih al-Bukhari 6306 |
| afuw | In the last nights of Ramadan | Jami' at-Tirmidhi 3513 |
| kalimat | When you stop at a place | Sahih Muslim 2708 |
| sleep-bismika | Before sleeping | Sahih al-Bukhari 6324 |
| sleep-qini | Lying down to sleep | Sunan Abi Dawud 5045 · Jami' at-Tirmidhi 3398 |

Dhikr phrases and their cited sources (Sahih al-Bukhari 3705, 6384, 6405; Jami' at-Tirmidhi 3383; Sahih Muslim 408, 2702).

## Also review

- English meanings of every dua. They are plain-language renderings written for this app, not quotes from a published translation.
- Transliterations.
- The `note` lines on duas (for example who narrated a hadith or the context of a verse).
- Hadith grading. Some Tirmidhi and Abu Dawud narrations have differing gradings; decide which to keep.

## Verse selections and short texts

- **Feelings and topics** (`content.json`, `feelings` and `topics`): whole verses only, taken from the bundled text. Check that each verse fits its title, especially "Women in the Quran".
- **Verses that read differently out of context.** Each is widely quoted on its own, but confirm it suits where it appears:
  - 25:70 (Regretful) opens with "Except for those who repent", continuing 25:68 to 69.
  - 14:7 (daily verse, Grateful) ends with a warning after the promise of increase.
  - 7:156 (Allah's Mercy) mentions punishment before "My mercy encompasses all things".
- **Daily verse change.** 2:216 ("Fighting has been enjoined upon you...") was replaced by 20:25 ("My Lord, expand for me my breast") as a gentler daily verse. Confirm the full list of 24 in `tools/build_content.py`.
- **Rest days note** in Prayer Times now reads: "Prayer pauses during your period and is not made up." Confirm the wording.
- **Prayer times**: calculated on device. Spot check against local mosque timetables in several cities.

## Journal

- **Dua journey note**: "every sincere dua is answered: it is given, saved for the Hereafter, or a harm of the same size is turned away (Musnad Ahmad)". Confirm the hadith number (narrated by Abu Sa'id al-Khudri) and wording.
- **Templates and prompts** (`Data/JournalContent.swift`): 34 templates in 9 collections and 11 prompt packs, written for this app. Check terms such as muhasabah, tadabbur, istikhara and tawakkul are used correctly, and that the verses attached to templates fit.

## Rules the app follows

- No AI-generated or invented scripture. Daily verses are whole verses from a fixed list.
- Recitation is only offered for duas made of whole verses, so audio never contains words that are not on screen.
- Personal prayers live in "Your Own Words" and are labeled as personal, never as Quran or Hadith.
