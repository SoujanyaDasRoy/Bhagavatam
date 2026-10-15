# Bhagavatam

A quiet, private reader and listener for the **Shrimad Bhagavat Mahapuran** on Android. Sanskrit with Hindi, Bengali, English and (in beta) Odia translations, Indian miniature paintings, audio narration, and search that works in every script. It lives on your phone: no ads, no account, no tracking, and everything you read or search works offline.

[Download the latest APK](https://github.com/SoujanyaDasRoy/Bhagavatam/releases/latest/download/Bhagavatam.apk) | [Features](#features) | [Missing verses](#missing-translation-verses) | [Report a mistake](#report-a-mistake)

## Features

- **The whole book:** the Mahatmya and all 12 Skandhas, 341 chapters, 14,580 verses.
- **Layers you can mix:** Sanskrit in Devanagari, Bengali or Roman script, with Hindi, Bengali, English or Odia translation under it, or the translation alone.
- **Two language settings:** the app language (menus) and the reading language (the text) are separate.
- **Search:** words, names, stories (Gajendra Moksha, Rasa Lila, ...) or a reference such as `10.29.1`. Spelling variants across scripts find the same verse (Krishna, कृष्ण, কৃষ্ণ, କୃଷ୍ଣ).
- **Dictionary:** press and hold a word for its meaning, offline, with an optional online fallback you can turn off.
- **Notes:** bookmarks, highlights in four colours, personal notes, saved words, all kept on your device, with a one-file backup.
- **Listening:** sentence-by-sentence narration with pauses, background and lock-screen playback, sleep timer, speed control, optional YouTube recitation streams.
- **Art:** the Mahatmya and every Skandha card carries a public-domain Indian miniature painting (Pahari, Guler, Basholi). Sources are in [docs/ART_CREDITS.md](docs/ART_CREDITS.md).
- **Themes:** Prabhat (light), Pothi (sepia), Sandhya (warm dark), Ratri (black).

## Download

| | |
|---|---|
| **Latest APK** | <https://github.com/SoujanyaDasRoy/Bhagavatam/releases/latest/download/Bhagavatam.apk> |
| **All releases and notes** | <https://github.com/SoujanyaDasRoy/Bhagavatam/releases/latest> |

1. Open one of the links above on your phone and download the file.
2. Open it and tap **Install**. If Android asks, allow "Install unknown apps" for your browser, once.
3. Open the app. The whole text is already inside, so no internet is needed.

Android 8.0 (API 26) or newer. The app is not on the Play Store.

**Version 1.2.3.** The app checks GitHub for a newer release (Settings has a switch for update notifications) and shows a card with the notes when there is one.

**Updating:** install the new APK over the old one. Your notes, highlights, bookmarks, saved words and settings stay, as long as the new APK is signed with the same key. If a release ever changes the signing key, use **Settings > Words & notes > Save a backup file** first and **Restore** afterwards.

## About the text

The Sanskrit and the Hindi, Bengali and English translations follow the Gita Press, Gorakhpur editions.

**Odia is a beta.** It was read from page scans by software running on one PC, not typed by a person. Words the readers could not agree on are underlined in red. Tap one to say whether it is spelled correctly, and if not, send the right word. About 122 verses have no Odia yet, and only 210 of 341 chapter titles are read from the printed contents (the others show the title in another language, with a note). No Odia reader has checked the text. Please treat it as unreviewed.

Bengali is complete except 232 verses that have no Bengali translation yet; the lists are in [Missing translation verses](#missing-translation-verses).

## Missing translation verses

Every verse below has the Sanskrit and the Hindi and English text, but no Bengali or Odia translation yet. Numbers are the verse numbers printed in the chapter. Counts are from the shipped database (`content.db`, content version 12 and later).

**Bengali: 232 verses without a translation.**

| Skandha | Chapter | Missing verses | Count |
|---|---|---|---|
| 2 | 2 | 35-37 | 3 |
| 2 | 4 | 20 | 1 |
| 2 | 5 | 19 | 1 |
| 2 | 6 | 41-45 | 5 |
| 2 | 7 | 42-53 | 12 |
| 2 | 8 | 29 | 1 |
| 2 | 9 | 45 | 1 |
| 2 | 10 | 9, 30-51 | 23 |
| 3 | 3 | 9-28 | 20 |
| 4 | 2 | 11-35 | 25 |
| 7 | 1 | 32, 45 | 2 |
| 7 | 5 | 10, 34 | 2 |
| 7 | 6 | 6-30 | 25 |
| 7 | 9 | 15 | 1 |
| 7 | 10 | 5, 24, 55, 62 | 4 |
| 7 | 13 | 9, 14-15, 35 | 4 |
| 7 | 15 | 74 | 1 |
| 8 | 5 | 31 | 1 |
| 8 | 6 | 1 | 1 |
| 8 | 10 | 55-57 | 3 |
| 8 | 11 | 9 | 1 |
| 8 | 12 | 20 | 1 |
| 8 | 16 | 6, 61-62 | 3 |
| 8 | 19 | 33 | 1 |
| 8 | 20 | 1-2, 33-34 | 4 |
| 9 | 2 | 1-2 | 2 |
| 9 | 3 | 5 | 1 |
| 9 | 5 | 4 | 1 |
| 9 | 6 | 7 | 1 |
| 9 | 9 | 30 | 1 |
| 9 | 12 | 9 | 1 |
| 9 | 14 | 2, 35 | 2 |
| 9 | 15 | 9 | 1 |
| 9 | 19 | 26-29 | 4 |
| 9 | 21 | 4 | 1 |
| 9 | 22 | 26 | 1 |
| 9 | 24 | 45, 59-60 | 3 |
| 11 | 2 | 27-55 | 29 |
| 12 | 2 | 7-44 | 38 |

Per Skandha: 2: 47, 3: 20, 4: 25, 7: 39, 8: 15, 9: 19, 11: 29, 12: 38.

**Odia (beta): 122 verses without a translation.**

| Skandha | Chapter | Missing verses | Count |
|---|---|---|---|
| Mahatmya | 1 | 79-80 | 2 |
| Mahatmya | 4 | 45 | 1 |
| Mahatmya | 6 | 16-17, 39 | 3 |
| 1 | 6 | 38 | 1 |
| 5 | 2 | 23 | 1 |
| 5 | 3 | 16 | 1 |
| 5 | 4 | 18-19 | 2 |
| 5 | 9 | 11-12 | 2 |
| 5 | 10 | 22-25 | 4 |
| 5 | 12 | 16 | 1 |
| 5 | 14 | 3 | 1 |
| 5 | 19 | 31 | 1 |
| 5 | 21 | 13 | 1 |
| 5 | 22 | 4 | 1 |
| 5 | 25 | 1 | 1 |
| 6 | 1 | 22 | 1 |
| 6 | 3 | 34-35 | 2 |
| 6 | 4 | 54 | 1 |
| 6 | 6 | 28 | 1 |
| 6 | 11 | 5 | 1 |
| 7 | 6 | 30 | 1 |
| 7 | 8 | 23 | 1 |
| 7 | 14 | 17 | 1 |
| 8 | 5 | 31 | 1 |
| 8 | 12 | 35 | 1 |
| 8 | 15 | 33 | 1 |
| 8 | 22 | 35-36 | 2 |
| 8 | 24 | 61 | 1 |
| 9 | 3 | 33 | 1 |
| 9 | 4 | 52 | 1 |
| 9 | 7 | 27 | 1 |
| 9 | 8 | 8 | 1 |
| 9 | 11 | 26 | 1 |
| 9 | 14 | 48-49 | 2 |
| 9 | 16 | 16, 37 | 2 |
| 9 | 24 | 14-18, 28-31 | 9 |
| 10 | 9 | 16 | 1 |
| 10 | 11 | 53 | 1 |
| 10 | 14 | 48, 56 | 2 |
| 10 | 15 | 6, 46-52 | 8 |
| 10 | 24 | 36-38 | 3 |
| 10 | 26 | 3 | 1 |
| 10 | 28 | 7 | 1 |
| 10 | 30 | 23 | 1 |
| 10 | 31 | 17 | 1 |
| 10 | 33 | 27-28 | 2 |
| 10 | 36 | 28 | 1 |
| 10 | 37 | 21, 28 | 2 |
| 10 | 43 | 17 | 1 |
| 10 | 51 | 36 | 1 |
| 10 | 52 | 38 | 1 |
| 10 | 56 | 6 | 1 |
| 10 | 59 | 34 | 1 |
| 10 | 60 | 29 | 1 |
| 10 | 63 | 3-4 | 2 |
| 10 | 68 | 23 | 1 |
| 10 | 74 | 33 | 1 |
| 10 | 78 | 34 | 1 |
| 10 | 80 | 42 | 1 |
| 11 | 1 | 6-7 | 2 |
| 11 | 2 | 14 | 1 |
| 11 | 6 | 9, 43 | 2 |
| 11 | 10 | 31-32 | 2 |
| 11 | 11 | 27, 32 | 2 |
| 11 | 12 | 3-6 | 4 |
| 11 | 16 | 22 | 1 |
| 11 | 17 | 54 | 1 |
| 11 | 18 | 22 | 1 |
| 11 | 19 | 30-32 | 3 |
| 11 | 25 | 30 | 1 |
| 11 | 28 | 14-15 | 2 |
| 11 | 30 | 38 | 1 |
| 11 | 31 | 1-4 | 4 |
| 12 | 10 | 18, 25 | 2 |
| 12 | 12 | 67 | 1 |

Per Skandha: Mahatmya: 6, 1: 1, 5: 16, 6: 6, 7: 3, 8: 6, 9: 18, 10: 36, 11: 27, 12: 3.

## Report a mistake

Press and hold a word, then tap the speech-bubble icon. The verse and word are copied and Telegram opens a chat with the maker, where you paste them and type the correct word. For a red-underlined Odia word, tap it and choose "No, send the correct word".

## For developers

- **Stack:** Kotlin, Jetpack Compose, Media3, SQLite. `minSdk 26`, `targetSdk 35`, JDK 17.
- **Layout:** `app/` is the Android app; `content/` holds the data pipeline (Bengali and Odia extraction, database build, search index); `docs/` has the architecture, data pipeline and decisions; `tools/` has helper scripts.
- **Not in the repository:** the scanned page images, the extracted translation text and the built databases (`content.db`) are not stored here because the Gita Press translations are copyrighted. A fresh clone therefore cannot build a complete APK. Put a `content.db` in `app/src/main/assets/` first (see `docs/DATA_PIPELINE.md`).
- **Build:**

  ```powershell
  .\gradlew.bat assembleDebug      # apk\Bhagavatam-v<version>-debug.apk
  .\gradlew.bat assembleRelease    # signed with the key named in local.properties
  .\gradlew.bat test
  ```

- **Local settings (`local.properties`, never committed):**
  - `sdk.dir`: the Android SDK.
  - `telegram.user`: the maker's Telegram username for the "report a mistake" button. Left out, the button is hidden.
  - `release.storeFile`, `release.storePassword`, `release.keyAlias`, `release.keyPassword`: the release signing key. Left out, release builds use the debug key and cannot update an app signed with the release key.
- **Publishing a release:** attach the APK to a GitHub release under the file name `Bhagavatam.apk`, so the "Latest APK" link above keeps working.
- **Where decisions live:** `docs/agent/DECISIONS.md` (owner decisions and rules), `AGENTS.md` (how automated agents work here).

---

*ॐ नमो भगवते वासुदेवाय*
