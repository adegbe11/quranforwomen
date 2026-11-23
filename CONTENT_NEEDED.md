# Content Needed

The app is ready to show guidance articles and devotionals, but it will never write them itself.
Everything below must be written by a qualified writer and checked by a qualified scholar.
Nothing appears in the app until an item is marked `"approved"` and has a named reviewer.

Send the work back in the formats at the end of this file. The same files are used by the iPhone
and Android apps.

Files in the app:

- iPhone: `QuranForWomen/Resources/guidance.json`
- Android: `android/app/src/main/assets/guidance.json`

Both files must stay identical. They ship today with an empty list, so the Guidance section in
Explore is hidden.

---

## 1. Women's guidance

One article per topic. Each article should be plain, kind, and short enough to read in a few
minutes. Every ruling needs its evidence: Quran verses as `surah:ayah` and hadith with the
collection and number. Where scholars differ, say so and name the views.

Use the section name `Women's guidance` for all of these.

| # | Topic | Suggested article id |
|---|---|---|
| 1 | Menstruation and worship (what she may and may not do) | `menstruation-and-worship` |
| 2 | Postpartum bleeding (nifas) and worship | `postpartum` |
| 3 | Ghusl (when it is needed and how to do it) | `ghusl` |
| 4 | Wudu (how to do it, what breaks it) | `wudu` |
| 5 | Salah for women | `salah` |
| 6 | Fasting, and making up missed fasts | `fasting-and-missed-fasts` |
| 7 | Reading and touching the Quran during menstruation | `quran-during-menstruation` |
| 8 | Pregnancy (worship, fasting, rulings) | `pregnancy` |
| 9 | Motherhood | `motherhood` |
| 10 | Marriage | `marriage` |
| 11 | Women's rights in Islam | `womens-rights` |
| 12 | Modesty | `modesty` |
| 13 | Life stages (girlhood, marriage, motherhood, older age) | `life-stages` |
| 14 | A guide for new Muslim women | `new-muslim-guide` |
| 15 | Frequently asked questions | `faq` |
| 16 | Glossary of terms used in the app | `glossary` |

For the FAQ and the glossary, one article each is fine. Write each question or term as its own
paragraph, for example `"Ghusl: a full ritual bath..."`.

## 2. Devotionals

Use the section name `Devotionals` for single devotionals (one short reading per day).

For a devotional plan (a series meant to be read over several days), use the plan's name as the
section, for example `"Ramadan devotionals"`, and send one article per day. Put the day in the
title, for example `"Day 1 · Beginning with intention"`. Articles appear in the order they are
listed in the file, so list the days in order.

## 3. Hadith grades for the bundled duas and dhikr

Each hadith dua and dhikr phrase in `content.json` needs an authenticity grade from a scholar,
with the grading source. Please also confirm the wording, diacritics and reference number
(see `CONTENT_REVIEW.md`).

Hadith duas:

| id | Title | Cited source |
|---|---|---|
| `waking` | On waking up | Sahih al-Bukhari 6324 |
| `morning-bika` | In the morning | Sunan Abi Dawud 5068 · Jami' at-Tirmidhi 3391 |
| `no-harm` | For protection, morning and evening | Sunan Abi Dawud 5088 · Jami' at-Tirmidhi 3388 |
| `heart-firm` | For a steady heart | Jami' at-Tirmidhi 2140 |
| `huda` | For guidance and contentment | Sahih Muslim 2721 |
| `hamm-hazan` | For worry and sadness | Sahih al-Bukhari 6369 |
| `distress` | In times of distress | Sahih al-Bukhari 6346 · Sahih Muslim 2730 |
| `ya-hayyu` | Asking for His mercy | Jami' at-Tirmidhi 3524 |
| `musibah` | After a loss | Sahih Muslim 918 |
| `a-inni` | Help me remember You | Sunan Abi Dawud 1522 |
| `barakallah` | For a newly married couple | Sunan Abi Dawud 2130 · Jami' at-Tirmidhi 1091 |
| `sayyid-istighfar` | The best way to ask forgiveness | Sahih al-Bukhari 6306 |
| `afuw` | In the last nights of Ramadan | Jami' at-Tirmidhi 3513 |
| `kalimat` | When you stop at a place | Sahih Muslim 2708 |
| `sleep-bismika` | Before sleeping | Sahih al-Bukhari 6324 |
| `sleep-qini` | Lying down to sleep | Sunan Abi Dawud 5045 · Jami' at-Tirmidhi 3398 |

Dhikr phrases:

| id | Phrase | Cited source |
|---|---|---|
| `subhanallah` | SubhanAllah | Taught to Fatimah · Sahih al-Bukhari 3705 |
| `alhamdulillah` | Alhamdulillah | Taught to Fatimah · Sahih al-Bukhari 3705 |
| `allahu-akbar` | Allahu Akbar | Taught to Fatimah · Sahih al-Bukhari 3705 |
| `tahlil` | La ilaha illallah | Jami' at-Tirmidhi 3383 |
| `astaghfirullah` | Astaghfirullah | Sahih Muslim 2702 |
| `subhanallahi-wa-bihamdihi` | SubhanAllahi wa bihamdihi | Sahih al-Bukhari 6405 |
| `hawqala` | La hawla wa la quwwata illa billah | Sahih al-Bukhari 6384 |
| `salawat` | Allahumma salli 'ala Muhammad | Sahih Muslim 408 |

Also please check the note on `al-ikhlas`, which cites Sahih al-Bukhari 5017.

## 4. Optional: more morning and evening adhkar

The Morning and Evening adhkar challenges use only the duas already in `content.json`, one per day.
Today there are three morning duas and only one evening dua (`no-harm`), so the evening challenge
shows the same dua every day. If you would like more variety, send more verified morning and
evening adhkar in the dua format below, in the `morning-evening` category, with "morning" or
"evening" in the title or note so the app knows which challenge they belong to.

---

## Formats

### guidance.json

```json
{
  "schemaVersion": 1,
  "items": [
    {
      "id": "example-article",
      "section": "Women's guidance",
      "title": "Example title",
      "body": [
        "First paragraph.",
        "Second paragraph."
      ],
      "quranRefs": ["surah:ayah", "surah:ayah"],
      "hadithRefs": ["Collection name and number"],
      "author": "Full name of the writer",
      "reviewer": "Full name of the scholar who approved it",
      "reviewedOn": "2026-10-01",
      "status": "approved"
    }
  ]
}
```

Field rules:

| Field | Required | Notes |
|---|---|---|
| `id` | yes | Lowercase letters, numbers and hyphens. Unique across the file. Never change it once released. |
| `section` | yes | `Women's guidance`, `Devotionals`, or a plan name. Spelled the same on every article in the section. |
| `title` | yes | Short and plain. |
| `body` | yes | A list of paragraphs, in order. Plain text only. |
| `quranRefs` | no | Verses as `"surah:ayah"`, one verse per entry. The app shows the verse from its own verified text and opens the reader on tap. Do not paste verse text into `body`. |
| `hadithRefs` | no | Citations exactly as they should appear, for example `"Sahih Muslim 332"`. |
| `author` | no | Shown as "Written by ...". |
| `reviewer` | yes, to be shown | Shown as "Reviewed by ...". An article with no reviewer is never shown. |
| `reviewedOn` | no | Date the review was done, as `YYYY-MM-DD`. |
| `status` | yes | `"draft"` or `"approved"`. Only `"approved"` articles with a reviewer are shown. Drafts can stay in the file. |

### Hadith grades (send as hadith-grades.json)

```json
{
  "items": [
    {
      "id": "morning-bika",
      "source": "Sunan Abi Dawud 5068 · Jami' at-Tirmidhi 3391",
      "grade": "Sahih",
      "gradedBy": "Name of the hadith scholar whose grading is cited",
      "wordingChecked": true,
      "correctedArabic": null,
      "correctedSource": null,
      "notes": "",
      "reviewer": "Full name of the person who checked this",
      "reviewedOn": "2026-10-01"
    }
  ]
}
```

`id` must match an id from the tables in section 3. Use `correctedArabic` or `correctedSource`
only when something needs to change; otherwise leave them as `null`.

### A new dua for content.json

```json
{
  "id": "short-unique-id",
  "category": "morning-evening",
  "title": "In the evening",
  "kind": "hadith",
  "source": "Collection and number",
  "arabic": "Arabic text copied from a verified source",
  "transliteration": "Transliteration",
  "translation": "English translation",
  "note": "Optional short note, for example how many times it is said."
}
```

---

## Drafts waiting for scholar review

Draft text for every topic in sections 1 and 2 is in `content/guidance-drafts.json`. It covers
the 16 topics above plus `repentance` (Repentance and starting again), 30 single devotionals, and
three 7 day plans (`Seven days of patience`, `Seven days of gratitude`, `Seven days of trust in Allah`).

These drafts are **not in the app**. The app only reads the two `guidance.json` files, and every
draft has `"status": "draft"` and `"reviewer": null`, so nothing would show even if copied over
unchanged.

What the scholar should look for:

- `[CHECK: ...]` notes in the text mark points the writer was unsure of. Fix the point and remove
  the note.
- Hadith citations marked `(verify number)` or `(verify number and grade)` need the number, and
  where asked the grade, confirmed. Remove the marker once checked.
- Quran verses are cited by reference only. The app shows the verified text.

To approve an item:

1. Correct the text in `body`, and remove every `[CHECK: ...]` note and `(verify ...)` marker.
2. Set `"status": "approved"`.
3. Set `"reviewer"` to the scholar's full name and `"reviewedOn"` to the date, as `YYYY-MM-DD`.
4. Optionally set `"author"` to the writer's or scholar's name.
5. Copy the item into the `items` list of **both** files, keeping them identical:
   `QuranForWomen/Resources/guidance.json` and `android/app/src/main/assets/guidance.json`.
   For plans, copy the days in order.
