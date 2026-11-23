# App Store Submission Kit

Quran for Women · bundle ID `com.quranforwomen.app` · version 1.0 (build 1) · iPhone only · iOS 17 and later

Everything below was checked against the code in this folder on October 10, 2026. Character counts were measured, not estimated.

## 1. Name and subtitle

| Field | Text | Count | Limit |
|---|---|---|---|
| App name | Quran for Women: Dua & Dhikr | 28 | 30 |
| Subtitle | Daily verse, prayer, journal | 28 | 30 |

The name under the icon on the Home Screen stays "Quran for Women" (`CFBundleDisplayName` in `project.yml`). If you prefer the short name in the store too, use `Quran for Women` (15). Words in the name and subtitle are already searchable, so they are left out of the keywords.

## 2. Promotional text

Count: 141 of 170. You can change this at any time without a new build.

```
Read the Quran in your language, find duas with their sources, keep a private journal, and see prayer times. No account, no ads, no tracking.
```

## 3. Description

Count: 2,689 of 4,000.

```
Quran for Women is a calm daily companion for reading the Quran, making dua and remembering Allah. It has no account, no ads and no tracking. What you write stays on your iPhone.

TODAY
Each day opens with a verse of the day, the next prayer time, today's dua and a short moment of dhikr. Tap the five prayer circles to keep a simple log of your prayers. There are no streaks.

THE QURAN
The full Arabic text (Tanzil, Uthmani script) is built in and works offline. Read surah by surah, or turn the pages of the Mushaf. Pick a translation in one of 13 languages. It downloads once, then works offline too. Listen verse by verse with Mishary Alafasy, Mahmoud Khalil Al-Husary or Mohamed Siddiq Al-Minshawi, with repeat.
Save verses, highlight them in four colors, add your own notes, and share a verse as text or as an image.

VERSES FOR HOW YOU FEEL
Choose a feeling, such as anxious, lonely, tired or grateful, and read verses that speak to it. There are also topics, including Women in the Quran, Allah's Mercy and Patience.

READING PLANS AND QUIZ
Read the whole Quran in 30, 60, 120 or 365 days. A short quiz is built only from the app's own verified content.

DUAS AND DHIKR
39 duas in 9 groups, including morning and evening, worry and hardship, marriage and family, motherhood and sleep. Each dua shows the Arabic, a transliteration, the meaning and where it comes from in the Quran or hadith. A dhikr counter helps you count your tasbih. Your Own Words is a place for your personal prayers, kept apart from Quran and hadith.

JOURNAL AND LESSON NOTES
Write freely, or start from a template or a prompt. Keep a dua journey and mark duas as answered. Record a khutbah or class and the app writes it out and arranges the speaker's own words into notes. The audio is not saved. You can lock the journal with Face ID or your passcode and export it as a text file.

PRAYER TIMES AND QIBLA
Prayer times are worked out on your iPhone from your location. Choose from 8 calculation methods and Standard or Hanafi Asr. Get a notification at each prayer with a soft bell or the standard iPhone sound. A compass shows the Qibla direction. Rest days pause prayer reminders during your period.

ALSO
A Verse of the Day widget for the Home Screen and Lock Screen. A daily reminder to read at the time you choose, and an evening reminder for duas and dhikr. Optional iCloud sync of your saved verses, highlights, notes and prayer log between your own devices. Dark mode, Dynamic Type and VoiceOver support.

SOURCES
Arabic text from the Tanzil Project. Translations from AlQuran Cloud. Recitation from Islamic Network. Every dua lists its source. No verse or hadith is written or changed by the app.
```

Checked claims: 13 translations (`Data/TranslationStore.swift`), 3 reciters (`Data/AudioPlayer.swift`), 9 feelings and 6 topics, 39 duas in 9 categories, 8 dhikr phrases (`Resources/content.json`), plan lengths 30, 60, 120, 365 (`Features/Explore/ExploreViews.swift`), 8 methods and 2 Asr options (`Data/PrayerTimes.swift`), four highlight colors (`COMPETITORS.md`, verse actions sheet), share as text or image, Face ID or passcode lock and text export (`Features/Journal/JournalEditor.swift`). Before you submit, see blocker B1 below: the Rest days switch currently has no way to be reached in the app.

## 4. Keywords

Count: 99 of 100. Comma separated, no spaces.

```
muslim,muslimah,islam,islamic,salah,namaz,qibla,tasbih,hadith,surah,ayah,koran,mushaf,azkar,sisters
```

Left out on purpose: words already in the name or subtitle (quran, women, dua, dhikr, daily, verse, prayer, journal), "adhan" and "azan" (no adhan recording ships, only a bell), "tafsir" (not in the app), and competitor names.

## 5. Categories

- Primary: **Books**. The Bible apps for women this app is measured against rank in Books (see `COMPETITORS.md`), and the core of the app is reading.
- Secondary: **Lifestyle**.

Reference is a reasonable alternative to Lifestyle.

## 6. Age rating questionnaire

Answer every content question with **None** and every yes or no question with **No**:

| Question | Answer | Why |
|---|---|---|
| Violence (cartoon, realistic, graphic) | None | Scripture is shown as text only, with no images |
| Sexual content, nudity | None | |
| Profanity or crude humor | None | |
| Horror or fear themes | None | |
| Mature or suggestive themes | None | |
| Alcohol, tobacco, drug use | None | |
| Simulated gambling, contests | None | The quiz has no prizes |
| Medical or treatment information | None | Rest days only pauses reminders. It gives no medical advice and tracks no cycle |
| Health or wellness topics | No | Same reason |
| Unrestricted web access | No | The app opens no web pages |
| User generated content shared with others | No | Journal and notes stay on the device. Sharing uses the iOS share sheet that she controls |
| Messaging or chat | No | |
| Advertising | No | |
| Parental controls, age assurance | No | |

Expected result: **4+**. Apple updated this questionnaire in 2025, so the exact wording you see may differ. If a question is not listed here, the honest answer for this app is None or No.

## 7. App Privacy (nutrition label)

**Answer: Data Not Collected.** Tracking: **No**. This matches both privacy manifests (`QuranForWomen/Resources/PrivacyInfo.xcprivacy` and `QuranForWomenWidget/PrivacyInfo.xcprivacy`), which declare no collected data types, no tracking and no tracking domains.

Apple's definition: data is "collected" when it is sent off the device in a way that lets you (the developer) or a third party partner access it for longer than needed to serve the request in real time. The app has no server, no account, no analytics SDK, no ads SDK and no crash reporting SDK. Network code exists in exactly two places (`Data/TranslationStore.swift` and `Data/AudioPlayer.swift`).

| Data type | What the app does | Why it is not "collected" |
|---|---|---|
| Precise or coarse location | Read once with `CLLocationManager` at about 1 km accuracy, stored in UserDefaults on the iPhone, used to calculate prayer times and the Qibla. iOS also turns the coordinates into a city name with `CLGeocoder`, an Apple service. | Never sent to you or to any partner. The city name lookup is an Apple system service, not a partner of yours. |
| Audio data | The microphone feeds speech recognition only while she records a lesson. The audio is never written to a file. | Never sent to you. Recognition runs on the iPhone when the device supports it (`requiresOnDeviceRecognition`). On older devices iOS may send the audio to Apple's speech service, which is Apple's own system service. |
| User content (journal, lesson notes, verse notes, personal duas) | Stored on the iPhone. The journal file is encrypted at rest (`completeFileProtectionUnlessOpen`). | Never sent to you. |
| Saved verses, highlights, verse notes, saved duas, personal duas, reading place, prayer log, rest days, dhikr counts, reading plan | Stored on the iPhone. If she turns on iCloud Sync, the same data is written to her own iCloud key value storage. | iCloud sync goes to her own iCloud account under her Apple Account. You cannot read it. Apple treats this as not collected. |
| Biometrics | Face ID is handled by iOS (`LAContext`). The app only receives "yes" or "no". | The app never sees face data. |
| Identifiers, usage data, diagnostics | None. | Nothing is gathered. |
| Contact info, purchases, financial info, health, browsing or search history | None. | Search runs on the device and is not stored. |

Network requests the app makes:

- `api.alquran.cloud`: one request per translation she downloads. It names the translation edition only.
- `cdn.islamic.network`: one request per verse of recitation she plays. It names the reciter and verse only.

These services see the request and her IP address, as any web server does. No personal data, account or device ID is sent, and they are not your partners. Say this in the privacy policy (done in `PRIVACY_POLICY.md`).

The location lines match the code. The About screen says "Location is only used for prayer times and the Qibla", and the permission prompt says it is never shared with us. The city name lookup goes to Apple through iOS, and the privacy policy says so.

## 8. Export compliance

`ITSAppUsesNonExemptEncryption` is set to `false` in `project.yml`, so App Store Connect will not ask for export documents for each build.

This is correct because the app's only encryption is HTTPS through Apple's `URLSession` and AVFoundation, and file protection provided by iOS. It contains no encryption code of its own. This is exempt.

## 9. App Review notes

Paste this into App Review Information > Notes:

```
Thank you for reviewing Quran for Women.

No account or sign in is needed. There is nothing to purchase. There are no ads and no analytics.

WHERE THINGS ARE
Today tab: verse of the day, the next prayer, prayer log circles, today's dua, a moment of dhikr, feelings ("How is your heart?") and today's journal question. Tap "Set up prayer times" to allow location.
Quran tab: The Mushaf (page view), surah list, "Topics, plans and quiz", and "Highlights and notes". Open any surah and tap a verse to save, highlight, add a note, play recitation or share as text or image.
Duas tab: dua categories, the dhikr counter and "Your Own Words" for personal prayers.
Journal tab: two sections, Journal and Notes. In Notes, tap "Record a Lesson" to try the microphone and speech recognition. The audio is not saved. Only the text is kept.
You tab: saved items, reading progress, reading plan, and Settings (language and translation, text size and recitation, downloads, reminders, prayer settings, Qibla, journal, iCloud sync, privacy).
Search tab (iOS 18 and later): surahs, verses, duas and journal pages.

PERMISSIONS, ALL OPTIONAL
Location: only for prayer times and the Qibla, calculated on the device.
Notifications: a daily reading reminder, an evening reminder and one at each prayer time. Prayer notifications are Time Sensitive because she chooses them like an alarm.
Microphone and speech recognition: only while recording a lesson note.
Face ID: only if she turns on the journal lock in Journal > View options > Journal Settings, or You > Settings > Journal. It is off by default. Without Face ID the iPhone passcode works.

PRAYER BELL
The default prayer notification sound is an original bell made for this app (Resources/Sounds/prayer-bell.wav). It is not an adhan recording. She can switch to the standard iPhone sound in prayer settings.

CONTENT
The Arabic Quran is the Tanzil Project Uthmani text, bundled unmodified. Translations are downloaded from api.alquran.cloud and recitation is streamed from cdn.islamic.network. Duas from the Quran are cut from the bundled verse text. Hadith duas list their collection and number. No scripture or hadith is generated or written by AI. Personal prayers are labeled as personal.

To see a translation, the device needs internet the first time. Everything else works offline.
```

## 10. Support and privacy URLs

| Field | Placeholder |
|---|---|
| Support URL | `https://YOUR-DOMAIN/support.html` |
| Privacy Policy URL | `https://YOUR-DOMAIN/privacy.html` |
| Marketing URL (optional) | leave empty, or your home page |

Both must be live public pages before you submit. App Review opens them, and a broken or private link is a common rejection. The pages are ready in `site/`. To use GitHub Pages: push the `site` folder to a public repo, turn on Pages for that folder, then paste the two URLs here and in the privacy policy field. Also replace the contact placeholder (`[support email]`) in `PRIVACY_POLICY.md`, `site/privacy.html` and `site/support.html` with a real address you check.

## 11. Screenshot plan

Required: 6.9 inch iPhone (1320 x 2868 portrait). App Store Connect scales these down for smaller iPhones. Use light mode for most, and one dark mode shot if you like. Use real content, no fake reviews or ratings, no status bar clutter (use the simulator's clean status bar: `xcrun simctl status_bar booted override --time 9:41`).

| # | Screen | How to reach it | Caption |
|---|---|---|---|
| 1 | Today, morning, with the verse of the day | Today tab | A verse for you, every day |
| 2 | Surah reader with Arabic and English | Quran tab > Al-Baqarah, scroll to 2:255 | Read in your language |
| 3 | The Mushaf, open on a page | Quran tab > The Mushaf | Turn the pages of the Mushaf |
| 4 | Feelings grid | Today > How is your heart? | Verses for how you feel |
| 5 | A dua with source shown | Duas tab > Worry & Hardship > For worry and sadness | Duas with their sources |
| 6 | Journal home with a few pages | Journal tab | A private journal, locked with Face ID |
| 7 | A finished lesson note | Journal tab > Notes > a recorded note | Notes from a khutbah or class |
| 8 | Qibla compass, or the Today prayer header | You > Settings > Qibla | Prayer times and Qibla |
| 9 | Dhikr counter mid count | Duas tab > dhikr | Count your dhikr |
| 10 | Home Screen with the Verse of the Day widget | Home Screen | Today's verse on your Home Screen |

Keep 1 to 6 even if you drop the rest. The first three show up in search results. Fill the journal and notes with your own short sample text, never with real private entries.

## 12. Before you press Submit

**Blockers found while preparing this kit**

- [x] **B1. Fixed.** You > Settings > Prayer Times and the next prayer chip on Today now open Prayer Times. Was: **Rest days cannot be reached.** The Rest days switch, the prayer timeline and the link to Qibla from it live in `PrayerTimesView` (`Features/Prayer/PrayerViews.swift`). The only link to that screen is `PrayerCard`, and `PrayerCard` is not placed on any screen. You > Settings > Prayer Times opens Prayer Settings instead. Add a link (for example make the Today prayer header or the You row open `Route.prayerTimes`), or remove Rest days from the description and support page.
- [x] **B2. Fixed.** About line and permission prompt reworded. Was: **Location wording.** `CLGeocoder` sends coordinates to Apple to get the city name. Either change the About line "Location never leaves this iPhone" and the location permission string to say prayer times are worked out on the iPhone (without "only"), or drop the city name lookup.
- [x] **B3. Fixed.** Erase My Data now calls both stores. Was: **Erase My Data does not erase the journal.** It calls `LibraryStore.eraseAll()` only. `JournalStore.eraseAll()` is never called. Journal pages and lesson notes can be deleted one at a time. Either call both, or keep the privacy policy wording as written (it says this plainly).

**Build and tests**

- [ ] On a Mac with Xcode 26.1 or later: `xcodegen generate`, build, and archive with no errors. This app has never been compiled on this PC.
- [ ] If Xcode rejects `tabViewSearchActivation(.searchTabSelection)` in `App/QuranForWomenApp.swift`, delete that line (README step 8).
- [ ] Run all tests and make sure they pass. Note: the tests in `QuranForWomenTests/` (Mushaf pages, prayer times, Quran store, reading plan) are not listed as a target in `project.yml`, so `xcodegen generate` will not include them. Add a `bundle.unit-test` target and a scheme test action first.
- [ ] App Group `group.com.quranforwomen.app`, iCloud key value storage and Time Sensitive Notifications are registered for the App ID and turned on in Signing & Capabilities.
- [ ] Version 1.0, build 1, or higher than any build already uploaded.

**Content and licensing**

- [ ] A qualified scholar has reviewed every item in `CONTENT_REVIEW.md`, including the 16 hadith duas, the dhikr sources, transliterations, English meanings, the rest days line and the dua journey note.
- [ ] Translation licenses confirmed with the rights holders for each of the 13 editions you ship. Some are free for non-commercial use only. Remove any you cannot clear from `TranslationOption.all`.
- [ ] Recitation audio license confirmed for Alafasy, Husary and Minshawi via Islamic Network.
- [ ] You hold the rights to the welcome photo.
- [ ] Prayer times spot checked against local mosque timetables in several cities.

**Testing on device**

- [ ] TestFlight build installed on a real iPhone. Walk every tab, every permission prompt (allow and deny), recording a lesson, the journal lock, export, iCloud sync between two devices, and the widget.
- [ ] iPhone Duo, folded and unfolded, if you can get one. Confirm how iOS 27 presents a portrait only app when unfolded.
- [ ] iOS 27 checks from README: navigation bar shrinks on scroll on reading screens, text keeps a readable width, Liquid Glass stays readable at every glass slider setting and turns solid with Reduce Transparency, no `@State` with both a default and an `init` value.
- [ ] Also test on iOS 17 (classic tab bar) and iOS 18 (Search tab).
- [ ] Arabic at every reader size, including 2:255, 18:77 and 41:44.
- [ ] VoiceOver, the largest Dynamic Type size, Reduce Motion, and an iPhone SE size screen.
- [ ] Prayer notifications arrive with the bell, and show through Focus.
- [ ] Dark and tinted app icons on an iOS 18 or later Home Screen.

**App Store Connect**

- [ ] Support URL and privacy policy URL are live and public, and the contact address is real.
- [ ] Privacy: Data Not Collected. Tracking: No.
- [ ] Age rating questionnaire filled as in section 6.
- [ ] Review notes pasted. Contact name, phone and email filled in.
- [ ] Screenshots uploaded for 6.9 inch.
- [ ] Price set (Free) and the countries chosen.
- [ ] Copyright line, for example "2026 [your name or company]".
