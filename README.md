# Quran for Women

Your Daily Companion to the Quran, Dua & a Peaceful Heart.

A native SwiftUI iPhone app. Four tabs: **Today, Quran, Duas, You**. No account, no ads, no tracking. Personal data stays on the device.

## Build

Requires a Mac with Xcode 26.1 or later and [XcodeGen](https://github.com/yonaskolb/XcodeGen).

```bash
brew install xcodegen
```

```bash
xcodegen generate
```

```bash
open QuranForWomen.xcodeproj
```

Pick your team under Signing & Capabilities for both targets. The app uses an App Group (`group.com.quranforwomen.app`) to share the daily verse with the widget, and iCloud key-value storage for optional sync. Register both in your Apple developer account, or change the identifiers in `project.yml`. Then pick an iPhone simulator or device, and run. Deployment target is iOS 17. On iOS 26 and later the system tab bar and sheets pick up the current system materials automatically.

Tests: press Cmd U in Xcode, or run the unit tests in `QuranForWomenTests/` from the command line:

```bash
xcodebuild test -scheme QuranForWomen -destination 'platform=iOS Simulator,name=iPhone 17'
```

## Structure

```
QuranForWomen/
  App/            App entry, root switching, tab view, routes
  DesignSystem/   Color tokens (light and dark), spacing, type, motion, shared components
  Data/           Models, Quran text store, translations, library, audio, reminders
  Features/
    Welcome/      Welcome screen and the open Quran illustration
    Onboarding/   Language and daily moment steps
    Today/        Verse of the day, feelings, continue reading, plan, next prayer, today's dua, dhikr
    Explore/      Feelings and topics, reading plans, quiz, highlights and notes
    Prayer/       Prayer times, prayer log, rest days, Qibla, prayer settings
    Journal/      Journal home, editor, templates, prompts, dua journey, lock and export
    Notes/        Lesson notes: record a khutbah or class, auto arranged from the speaker's words
    Search/       One search tab for surahs, verses, duas and journal pages (iOS 18 and later)
    Quran/        Surah library, search, reader, verse actions, reading settings
    Audio/        Mini player and full player
    Duas/         Categories, dua detail, your own words
    Dhikr/        Tasbih counter
    You/          Saved items, progress, reminder, language, downloads, privacy
  Resources/      Quran text, dua library, widget verses, font, asset catalog
QuranForWomenWidget/  Verse of the Day widget (home screen and lock screen)
tools/
  build_content.py   Rebuilds Resources/quran-uthmani.json and content.json
  export_journal_preview.py  Copies journal templates into the HTML preview
  make_icon.py       Renders the app icon
```

## Platform behavior

| iOS | What she gets |
|---|---|
| 17 | Classic tab bar, floating mini player, material blur instead of glass |
| 18 | Native Tab API with a Search tab, zoom transitions, MeshGradient sky |
| 26 | Liquid Glass everywhere, tab bar shrinks on scroll, tapping Search opens the field |
| 26.1 and later | Mini player becomes the tab bar's bottom accessory (check `tabViewBottomAccessory(isEnabled:)` availability on first build) |
| 27 | Glass stays readable at any transparency setting the user picks, search sits in the tab row |

Reduce Motion and Reduce Transparency are respected everywhere.

## Content and sources

| Content | Source | Notes |
|---|---|---|
| Arabic Quran | Tanzil Project Uthmani text, via api.alquran.cloud `quran-uthmani` | Bundled, unmodified. The leading Basmala of surahs 2 to 114 (not 9) is shown as a heading instead of inside verse 1. Verse numbering unchanged. |
| Translations | api.alquran.cloud editions (Saheeh International, Junagarhi, and others) | Downloaded once on first use, checked for all 6236 verses, then kept offline. |
| Recitation | cdn.islamic.network verse audio (Alafasy, Husary, Minshawi) | Streams verse by verse. |
| Quranic duas | Sliced from the bundled Quran text by `build_content.py` | No Quranic Arabic is typed by hand. |
| Hadith duas and dhikr | Typed, each with a collection and number | **Must be reviewed by a qualified scholar before release.** See `CONTENT_REVIEW.md`. |
| Arabic font | Amiri Quran by Khaled Hosny | SIL Open Font License, included in `Resources/Fonts`. |
| Welcome verse | Quran 13:28, closing sentence of Marmaduke Pickthall's 1930 translation | Public domain. Fetched from api.alquran.cloud `en.pickthall`, not typed from memory. |
| Welcome photo | Supplied by the project owner, cleaned by `tools/prepare_welcome_photo.py` | Confirm you hold the rights. For the sharpest result on large iPhones, supply the original at 1320 px wide or more. |
| Prayer bell | Original, synthesized by `tools/make_prayer_bell.py` | No license needed. |
| Adhan (optional) | Add your own licensed recording | Save a clip of 30 seconds or less as `Resources/Sounds/adhan.wav` (or convert to .caf). The app finds it automatically and makes Adhan the default sound. iOS cuts notification sounds longer than 30 seconds. |

To rebuild the bundled content:

```bash
curl -sS -o raw/uthmani.json https://api.alquran.cloud/v1/quran/quran-uthmani
```

```bash
curl -sS -o raw/meta.json https://api.alquran.cloud/v1/meta
```

```bash
python -I tools/build_content.py raw QuranForWomen/Resources
```

The script asserts 114 surahs, 6236 verses, and prints every sliced Quranic dua for a human check.

## iOS 27

- Builds with Xcode 26.1 or Xcode 27. Calls that only exist in the iOS 27 SDK sit behind `#if compiler(>=6.4)` and `if #available(iOS 27.0, *)`, so Xcode 26 skips them.
- Reading screens (reader, dua, verse collection, journal page, lesson note) shrink the navigation bar on scroll on iOS 27 (`minimizesNavigationBarOnScroll()` in `DesignSystem/Components.swift`).
- The same screens cap their text at a comfortable reading width (`readableWidth()`), for the unfolded iPhone Duo (iOS 27.1) and resized iPhone apps.
- Liquid Glass follows the new iOS 27 glass slider. `readableGlass` keeps text legible at any setting and turns solid with Reduce Transparency.
- No `@State` property is both given a default value and set in `init`, which the iOS 27 `@State` macro rejects. Keep it that way.
- Test on iPhone Duo (folded and unfolded) and with the Xcode 27 resize handles in Previews. The app is portrait only; confirm how iOS 27 presents it unfolded.

## Before App Store release

1. Scholar review of every item in `CONTENT_REVIEW.md`.
2. Confirm licensing terms for each translation you ship, and for the recitation audio, with the rights holders. Some translations are free for non-commercial use only.
3. Test Arabic rendering on device at every reader size, including verses with small high marks (for example 2:255, 18:77, 41:44).
4. Enable the Time Sensitive Notifications capability for the app target (already in the entitlements) so prayer notifications show through Focus.
5. Run through the app with VoiceOver, the largest Dynamic Type size, Reduce Motion, and on an iPhone SE size screen.
6. App Store Connect privacy answers: Data Not Collected, no tracking. This matches the privacy manifests in `QuranForWomen/Resources/PrivacyInfo.xcprivacy` and `QuranForWomenWidget/PrivacyInfo.xcprivacy`. Update both if the app ever sends data anywhere.
7. Check the dark and tinted app icons on an iOS 18 or later home screen (regenerate with `python -I tools/make_icon.py <out.png> light|dark|tinted`).
8. First build: if Xcode rejects `tabViewSearchActivation(.searchTabSelection)` in `App/QuranForWomenApp.swift`, delete that one line. Everything else in that modifier stands on its own.

## Android

The Android app lives in `android/` (Kotlin, Jetpack Compose, minSdk 26, targetSdk 36, code package `com.quranforwomen.app`, Play package `com.quranforwomenapp`). It has every screen and feature of the iPhone app and shares the bundled data (`quran-uthmani.json`, `content.json`, `guidance.json`, the Amiri Quran font and the sounds).

Build on Windows with PowerShell (the Unix `gradlew` script does not run under Git Bash here):

```
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\android\gradlew.bat -p android :app:installDebug        # debug build on a phone or emulator
.\android\gradlew.bat -p android :app:testDebugUnitTest   # unit tests (prayer times, Qibla, data checks)
.\android\gradlew.bat -p android :app:bundleRelease       # Play Store bundle, needs android/keystore.properties
```

- Structure: `data/` stores (Compose state, SharedPreferences and files), `nav/` the router with one stack per tab and predictive back, `ui/` theme and shared components, `feature/` one package per area, `widget/` Glance widgets.
- Differences from iPhone: Google backup instead of iCloud sync (the journal is excluded), fingerprint or screen lock for the journal, Android speech recognition for lesson notes, exact alarms for prayer reminders, no adhan file (bell or phone sound).
- Icons: the app icon comes from the easyappicon export. `python tools/apply_app_icon.py <unpacked zip folder>` applies it to both apps (it also makes the iPhone dark and tinted versions).
- Release checklist: `android/PLAY_STORE.md`.

## Guidance and devotionals

`guidance.json` (both apps) holds scholar approved articles. The app shows only items with `status: "approved"` and a named reviewer, and hides the whole Guidance section while there are none. Drafts for every women's guidance topic and the devotionals are in `content/guidance-drafts.json`, outside the apps, waiting for review. See `CONTENT_NEEDED.md`.
