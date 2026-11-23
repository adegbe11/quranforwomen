# Google Play Submission Kit

Quran for Women · package `com.quranforwomenapp` · version 1.0.0 (code 1) · Android 8.0 (API 26) and later · targets API 36

## 1. Store listing

| Field | Text | Count | Limit |
|---|---|---|---|
| App name | Quran for Women: Dua & Dhikr | 28 | 30 |
| Short description | Quran, duas, dhikr, prayer times and a private journal. No ads. | 64 | 80 |

Full description (paste as is):

```
Quran for Women is a calm daily companion for reading the Quran, making dua and remembering Allah. No account, no ads, no tracking. What you write stays on your phone.

TODAY
A verse of the day, the next prayer, today's dua and a moment of dhikr. Tap the five prayer circles to keep a simple prayer log. No streaks.

THE QURAN
The full Arabic text (Tanzil, Uthmani script) is built in and works offline. Read surah by surah, browse by juz or hizb, or turn the pages of the Mushaf with a real page turn. Translations in 13 languages and an optional transliteration download once, then work offline. Listen verse by verse with three reciters, repeat a verse or a range, change the speed, set a sleep timer, and download a surah to listen offline.
Save verses, highlight them, add notes, keep favourite surahs, and share a verse or a whole surah as text or an image.

FEELINGS, TOPICS, PLANS AND CHALLENGES
Verses for how you feel and topics such as Women in the Quran. Read the whole Quran in 30, 60, 90 days, 4 months, a year, or your own length, or one surah a day. Gentle 7 and 30 day challenges built from the app's verified content.

DUAS AND DHIKR
Duas from the Quran and Sunnah, each with Arabic, transliteration, meaning and source. Share a dua as a card. A tasbih counter with targets, your own dhikr, an optional counting sound, a daily goal and a 30 day history.

JOURNAL AND LESSON NOTES
Write freely or from a template or prompt. Keep a dua journey and mark duas as answered. Record a khutbah or class and the app writes it out on your phone and arranges the speaker's own words into notes. The audio is never saved. Lock the journal with your fingerprint, face or screen lock, and export it as a text file.

PRAYER, QIBLA AND CALENDAR
Prayer times worked out on your phone, 8 calculation methods, Standard or Hanafi Asr, a notification at each prayer with a soft bell, a Qibla compass, rest days that pause reminders during your period, an Islamic calendar with the main days, a fasting tracker, suhoor and iftar reminders in Ramadan, and a private make up prayers tracker.

WIDGETS
Verse of the day, dua of the day, next prayer, reading progress and dhikr.

SOURCES
Arabic text from the Tanzil Project. Translations from AlQuran Cloud. Recitation from Islamic Network. Every dua lists its source. No verse or hadith is written or changed by the app.
```

Category: Books & Reference (or Lifestyle). Tags: Quran, Islam, Prayer. Contact email: add your support email. Privacy policy URL: the hosted `site/privacy.html` (same page as iPhone, see section 5).

## 2. Graphics

All made from real screens of the app with `python film/store.py`, in `film/store/`:
- App icon 512 x 512: `android/play_store_icon.png`.
- Feature graphic 1024 x 500: `film/store/play_feature_graphic.png`.
- Phone screenshots, 1440 x 2560 (9:16), upload in this order: `play_screenshot_1.png` to `play_screenshot_8.png`. They share one background, so they read as a single panorama in the store.
  1. Quran for Women, No ads, No account, Private
  2. Turn every page
  3. Listen, verse by verse
  4. A dua for every moment
  5. Remember Allah
  6. Prayer times and Qibla
  7. A private journal
  8. Your day at a glance
- Promo video: upload `film/out/share_master_60s_4k.mp4` to YouTube (unlisted is fine) and paste the link.

## 3. Data safety form (answers that match the code)

- Does your app collect or share any of the required user data types? **No.**
  - Location: used only on the device to work out prayer times and the Qibla. It is never sent to us. Android's Geocoder may contact Google to turn coordinates into a city name; Google counts on-device processing and system services as not "collected" by the developer.
  - Audio (lesson notes): speech recognition runs through Android's own SpeechRecognizer with offline preferred; the audio is never stored or sent by the app. If the phone has no offline model, Android's recognizer may process speech in its own service. Mention this in the privacy policy.
  - Journal, notes, saved items: stored only on the device. Saved items and settings may be included in the user's own end-to-end encrypted Google backup (Android Auto Backup); the journal is excluded (`res/xml/data_extraction_rules.xml`).
- Is all data encrypted in transit? Yes (all network requests are HTTPS: api.alquran.cloud, cdn.islamic.network).
- Can users request deletion? Yes, in app: You › Settings › Privacy & About › Erase My Data, and by uninstalling.

Network requests the app makes: api.alquran.cloud (one download per translation or transliteration), cdn.islamic.network (recitation audio). No analytics, no ads, no account.

## 4. Content rating and declarations

- Content rating questionnaire: no violence, no user generated content shared with others, no gambling. Expected rating: Everyone.
- Ads: No ads.
- Target audience: 13 and over (not designed for children, so the Families policy does not apply).
- Permissions declarations:
  - `SCHEDULE_EXACT_ALARM`: prayer reminders at the exact prayer time. Users can turn it off; the app falls back to inexact alarms. (Do not use `USE_EXACT_ALARM`.)
  - `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Quran recitation playing with the screen off. Play asks for a short video showing playback; record the full player while the screen locks.
  - `RECORD_AUDIO`: lesson notes, only while she taps Record.
  - `ACCESS_COARSE_LOCATION`: city level only; precise location is never requested.

## 5. Privacy policy

`PRIVACY_POLICY.md` and `site/privacy.html` describe the iPhone app. Before publishing on Play, add an Android paragraph: Google backup instead of iCloud sync (journal excluded), Android Geocoder for the city name, Android speech recognition for lesson notes, fingerprint or screen lock instead of Face ID.

## 6. Build and release

1. Create an upload key once (keep it safe, never commit it):
   `keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000`
2. Create `android/keystore.properties` with `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.
3. Build the bundle (PowerShell): set `JAVA_HOME` to Android Studio's `jbr`, then `.\gradlew.bat -p android :app:bundleRelease`. Upload `app/build/outputs/bundle/release/app-release.aab`. Turn on Play App Signing.
4. Before upload, test the release build on a real phone: every tab, permissions allowed and denied, a lesson recording, the journal lock, notifications at a prayer time, widgets, Ramadan reminders (set the phone date into Ramadan to test), and dark mode.
5. Bump `versionCode` for every upload.

Without `keystore.properties`, release builds are signed with the debug key for local testing only. Play rejects debug signed bundles, so they can never be uploaded by mistake.

## 7. Still needed from people

- Scholar review of `CONTENT_REVIEW.md` and the drafts listed in `CONTENT_NEEDED.md`.
- Licences confirmed for translations, recitation audio and the welcome photo (same as iPhone).
- Feature graphic, final screenshots, support email, hosted privacy policy.
