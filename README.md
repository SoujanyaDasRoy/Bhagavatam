# Bhagavatam

A quiet, private reader and listener for the **Shrimad Bhagavat Mahapuran** on Android. Sanskrit with Hindi, Bengali, English and (in beta) Odia translations, Kangra-style art, audio narration, and search that works in every script. It lives on your phone: no ads, no account, no tracking, and everything you read or search works offline.

## Download

| | |
|---|---|
| **Latest APK** | <https://github.com/SoujanyaDasRoy/Bhagavatam/releases/latest/download/Bhagavatam.apk> |
| **All releases and notes** | <https://github.com/SoujanyaDasRoy/Bhagavatam/releases/latest> |

1. Open one of the links above on your phone and download the file.
2. Open it and tap **Install**. If Android asks, allow "Install unknown apps" for your browser, once.
3. Open the app. The whole text is already inside, so no internet is needed.

Android 8.0 (API 26) or newer. The app is not on the Play Store.

**Updating:** install the new APK over the old one. Your notes, highlights, bookmarks, saved words and settings stay, as long as the new APK is signed with the same key. If a release ever changes the signing key, use **Settings > Words & notes > Save a backup file** first and **Restore** afterwards.

## What is in it

- **The whole book:** the Mahatmya and all 12 Skandhas, 341 chapters, 14,580 verses.
- **Layers you can mix:** Sanskrit in Devanagari, Bengali or Roman script, with Hindi, Bengali, English or Odia translation under it, or the translation alone.
- **Two language settings:** the app language (menus) and the reading language (the text) are separate.
- **Search:** words, names, stories (Gajendra Moksha, Rasa Lila, ...) or a reference such as `10.29.1`. Spelling variants across scripts find the same verse (Krishna, कृष्ण, কৃষ্ণ, କୃଷ୍ଣ).
- **Dictionary:** press and hold a word for its meaning, offline, with an optional online fallback you can turn off.
- **Notes:** bookmarks, highlights in four colours, personal notes, saved words, all kept on your device, with a one-file backup.
- **Listening:** sentence-by-sentence narration with pauses, background and lock-screen playback, sleep timer, speed control, optional YouTube recitation streams.
- **Themes:** Prabhat (light), Pothi (sepia), Sandhya (warm dark), Ratri (black).

## About the text

The Sanskrit and the Hindi, Bengali and English translations follow the Gita Press, Gorakhpur editions.

**Odia is a beta.** It was read from page scans by software running on one PC, not typed by a person. Words the readers could not agree on are underlined in red. Tap one to say whether it is spelled correctly, and if not, send the right word. About 99 verses and 64 chapter titles have no Odia yet, and no Odia reader has checked the text. Please treat it as unreviewed.

Bengali is complete except 232 verses that have no Bengali translation yet.

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
