# Bhagavatam

A quiet, private reader and listener for the **Shrimad Bhagavat Mahapuran**, in Sanskrit, Hindi, Bengali and English. It lives on your phone, has no ads and no accounts, and does not track you.

- The Mahatmya and all 12 Skandhas: 341 chapters, 14,580 verses
- Every chapter in Sanskrit with Hindi, Bengali and English translations
- Reading and listening, side by side

---

## Why it exists

Scripture apps often come with missing verses, broken text, ads and a constant need for the internet. This one is built for the opposite: a complete text you can open on a train, in a temple, or at dawn, and read or listen to without being interrupted.

---

## What you can do

**Read**
- Switch between the Sanskrit shloka (Devanagari, Bengali script or Roman) and the Hindi, Bengali and English translations, one at a time or together.
- Pinch to resize the text, swipe to the next or previous chapter, and pick up exactly where you left off.
- Four themes for different times of day: Prabhat (daylight), Sandhya (twilight), Pothi (manuscript sepia) and Ratri (dark).

**Listen**
- Tap **Listen** on any chapter. The app reads it aloud, verse by verse, and highlights the line being spoken. Tap a line to start from there.
- Keeps playing with the screen off. Controls on the lock screen and Bluetooth headsets.
- Speed, sleep timer, and play through a chapter, a Skandha or the whole book.
- The voice is your phone's own text-to-speech, so how good it sounds depends on the voices installed. See "Better voices" below.

**Study**
- Press and hold a word for a short meaning. Select text to highlight it in a colour, add a note or bookmark a passage. Notes stay on your phone.
- Search all four languages at once, or type a reference like `10.29.1` to jump straight there.
- Browse well-known stories (Gajendra Moksha, Dhruva, Rasa Lila and more).

---

## Download and install (Android)

1. Open the **Releases** page of this repository on your phone: <https://github.com/SoujanyaDasRoy/Bhagavatam/releases>
2. Under the newest release, tap `Bhagavatam-release.apk` to download it.
3. Open the downloaded file. If Android says it cannot install apps from this source, tap **Settings** and allow installs from your browser or file manager, then go back and tap **Install**.
4. Open the app. The whole text is already inside it.

Needs Android 8.0 or newer. The app works fully offline. The only thing that uses the internet is the optional word-meaning lookup, which you can switch off in Settings, under Reading.

> No APK is attached to a release yet. Until one is published, the install steps above will find nothing to download.

### Better voices

Android's built-in speech varies a lot from phone to phone. For a more natural voice: open Android **Settings**, go to *Text-to-speech output*, choose *Speech Recognition & Synthesis from Google*, and install the high-quality voices for Hindi, Bengali and English. The app picks the best installed voice by itself, and you can choose a different one from the headphones button in the player.

---

## About the text

The Sanskrit text and the Hindi, Bengali and English translations follow the Gita Press, Gorakhpur editions. The book text is not stored in this repository.

---

## For developers

Kotlin and Jetpack Compose, Android 8 to 15. Scripture is a bundled SQLite database; audio is Android text-to-speech behind a media session. Build with JDK 17:

```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK
```

More detail: [ARCHITECTURE](docs/ARCHITECTURE.md), [DATA_PIPELINE](docs/DATA_PIPELINE.md), [DESIGN_SYSTEM](DESIGN_SYSTEM.md).
The app needs `content/content.db`, which is built locally and is not in git.

---

*Om Namo Bhagavate Vasudevaya*
