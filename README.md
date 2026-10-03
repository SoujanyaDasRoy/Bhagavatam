# Bhagavatam

A quiet, private reader and listener for the **Shrimad Bhagavat Mahapuran**, in Sanskrit, Hindi, Bengali and English. Built with pure Jetpack Compose for Android. It lives on your phone, has no ads, no accounts, and does not track you.

- **The Mahatmya and all 12 Skandhas**: 341 chapters, 14,580 verses
- **Multi-Language**: Sanskrit *mool* with Hindi, Bengali, and English translations
- **Spotify-Inspired Audio Player**: Minimal, functional Now Playing experience with synchronized karaoke lyrics
- **Zero-Download YouTube Streams**: Instant full-chapter audio streams in Hindi, Bengali, English, and Sanskrit
- **Fully Local & Offline-First**: Scripture text bundled on-device with zero internet required

---

## Why it exists

Scripture apps often come with missing verses, broken text, intrusive ads, and a constant need for an internet connection. This app is built for the opposite: a complete, sacred text you can open on a train, in a temple, or at dawn, and read or listen to without being interrupted.

---

## Features

### 📖 Reading & Study
- **Layered Translations**: Switch smoothly between Sanskrit shlokas (in Devanagari, Bengali, or IAST Roman script) and translations in Hindi, Bengali, or English. Read them side-by-side or translation-only.
- **Bengali Typography**: Native Bengali numerals and canonical titles rendered with `NotoSerifBengali`.
- **Customizable Reader**: Pinch-to-zoom text scaling, adjustable line height, and fast chapter navigation.
- **Four Sacred Themes**: *Prabhat* (daylight), *Pothi* (manuscript sepia), *Sandhya* (twilight), and *Ratri* (pure dark mode).
- **Highlights & Notes**: Long-press any verse or select text to bookmark, highlight in sacred hues, or attach personal notes stored safely on your device.
- **Omni Search**: Search across verses, characters, story arcs (*Gajendra Moksha*, *Dhruva Charitra*, *Rasa Lila*), or jump straight to any reference (e.g. `10.29.1`).

### 🎧 Listening & Recitation
- **Spotify-Inspired Minimal Player**:
  - **Floating Mini Player**: Glassmorphic pill with artwork thumbnail, playback transport, horizontal swipe-to-dismiss, and one-tap close (`✕`) button.
  - **Expanded Now Playing View**: Square hero artwork, Spotify 5-control transport bar (PlayThrough, Previous, Hero Play/Pause, Next, Loop), thin seekbar scrubber, and live synchronized lyrics sheet.
- **Line-by-Line Karaoke Paath**: Real-time line-by-line Sanskrit karaoke and sentence-level translation highlight. Tap any line to immediately jump recitation there.
- **YouTube Recitation Stream (Zero Download)**: Seamlessly toggle between local TTS recitation and curated YouTube recitation streams for all 12 Skandhas in Hindi, Bengali, English, and Sanskrit.
- **Background & Lock Screen Playback**: Full Android MediaSession integration with Bluetooth headset and notification controls.
- **Sleep Timer & Speed Control**: Set automatic timers (15m, 30m, 45m, 60m) and adjust narration speed (0.75× – 2.0×).

---

## Download and Install (Android)

1. Open the **Releases** page of this repository on your phone: <https://github.com/SoujanyaDasRoy/Bhagavatam/releases>
2. Under the latest release, download `Bhagavatam-release.apk` (or `Bhagavatam-debug.apk`).
3. Open the downloaded file and tap **Install**.
4. Open the app — the entire scripture text is already included offline.

> **Requirements**: Android 8.0 (API 26) or newer.

### Better Voices
Android's built-in text-to-speech engine varies across devices. For high-quality recitation:
- Go to Android **Settings › Accessibility › Text-to-speech output**.
- Set the Preferred engine to **Speech Recognition and Synthesis by Google**.
- Download the high-quality voice packs for Hindi, Bengali, and English.
- You can switch voices anytime via the **Voices** (`🎧`) button in the player.

---

## About the Text

The Sanskrit scripture and translations follow the canonical **Gita Press, Gorakhpur** editions.

---

## For Developers

Built with modern Android standards:
- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose with custom design tokens
- **Audio Engine**: Android TTS + ExoPlayer / Media3 MediaSession + AndroidView Web Streams
- **Database**: Bundled SQLite database (`content.db`)

### Build locally:
```powershell
# Set Java JDK 17+
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'

# Compile and assemble
.\gradlew.bat compileDebugKotlin
.\gradlew.bat assembleDebug
```

---

*ॐ नमो भगवते वासुदेवाय*
