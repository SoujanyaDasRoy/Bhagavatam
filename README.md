# Bhagavatam

An offline-first, zero-gap Android application and high-precision data pipeline for the complete **Shrimad Bhagavat Mahapuran** (Padma Purana Mahatmya and 12 Skandhas, 341 chapters, 14,580 verses).

![Android minSdk 26](https://img.shields.io/badge/Android-minSdk%2026%20%7C%20Target%2035-3DDC84?logo=android&logoColor=white)
![Kotlin 2.0](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![SQLite FTS5](https://img.shields.io/badge/Storage-SQLite%20(Embedded)-003B57?logo=sqlite&logoColor=white)
![Build](https://img.shields.io/badge/Build-Gradle%208.11-02303A?logo=gradle&logoColor=white)

---

## Overview

The **Bhagavatam** project provides a distraction-free, zero-gap digital edition of the Shrimad Bhagavat Mahapuran. It combines verified canonical texts across multiple languages with natural human-paced recitation, traditional Kangra/Pahari miniature art, and fast cross-lingual search.

### Problem It Solves
1. **Scattered and Incomplete Translations:** Many digital editions suffer from missing verses, OCR errors, or dropped commentary layers. This repository implements an automated extraction and auditing pipeline guaranteeing 100% verse coverage against original Gita Press editions.
2. **Network Dependency & Bloat:** Most spiritual applications require constant connectivity, push notifications, and intrusive analytics. Bhagavatam is 100% offline-first with zero tracking.
3. **Robotic Audio Playback:** Standard text-to-speech tools fail on Sanskrit verse cadence and compound Indic names. The narration engine dynamically calculates natural breathing pauses and sentence-level verse plans.

---

## Key Features

- **Zero-Gap Multilingual Scripture (14,580 Verses):**
  - **Sanskrit Mool Shlokas:** Metrical formatting with speaker identification and chapter titles.
  - **English Transliteration (Roman):** Deterministic phonetic rendering with diacritic preservation and ASCII-folded smart search.
  - **Hindi Translations:** Full Gita Press Gorakhpur editions across all 341 chapters.
  - **Bengali Translations:** Full Gita Press Volumes 1 & 2 integration with preserved multi-verse grouping units (`bn_from`).
  - **English Translations:** Accurate chapter commentary and verse translations.
  - **Real-Time Script Switching:** Seamless toggle between Devanagari (`देवनागरी`), Bengali (`বাংলা লিপি`), and English Roman (`English (Roman)`).
- **Natural Offline Narration Engine:**
  - Clause-level sentence segmentation (`NarrationText`) with contextual breathing pauses.
  - One-verse lookahead queue (`Narrator`) eliminating pause latency between shlokas.
  - Background playback, Bluetooth headset controls, and lock-screen media notifications via Android `PlaybackService`.
  - Automatic Hindi/Bengali voice fallback for Sanskrit recitation.
- **Fast Cross-Lingual Search & Discovery:**
  - Database-backed multi-layer search across Sanskrit, Roman transliteration, Hindi, Bengali, and English.
  - Contextual keyword snippets with exact character match highlighting.
  - Interactive thematic Lila and character guides.
  - Built-in Voice Navigator (`VoiceNavigatorSheet`) for hands-free chapter and incident jumping.
- **Kangra / Pahari Miniature Art:**
  - Classical 18th-century Pahari gouache style backgrounds (16:10 wide landscape) for every Skandha and chapter.
  - High-efficiency WebP assets loaded seamlessly under reader scrims.
- **Reader Study Tools:**
  - Persistent bookmarks, multi-color text highlights, and personal notes.
  - Interactive word hit-testing for offline glossary, definitions, and sandhi lookup.
  - Four curated reader themes: **Prabhat** (light parchment), **Sandhya** (deep night), **Pothi** (sepia), and **Ratri** (OLED black).
- **OTA Self-Updating:**
  - In-app background GitHub releases checker with Android 8–15 permission handling (`Updater.kt`).

---

## Tech Stack

### Mobile Client (Android)
- **Language & Runtime:** Kotlin 2.0 (JVM 17 Target)
- **UI Framework:** Jetpack Compose (BOM), Material 3, Compose Navigation
- **Architecture:** MVI / Unidirectional Data Flow with unified `AppState`
- **Performance:** Android Baseline Profiles (`androidx.profileinstaller`), R8 full-mode minification & shrinking

### Audio & Media Subsystem
- **Speech Engine:** Native Android Text-To-Speech (`android.speech.tts`) with sequence epoch guards
- **Media Playback:** Android `MediaSessionService` / `MediaSession` foreground service
- **Audio Focus:** `AudioFocusRequest` with automatic ducking and noisy-audio pause handling

### Data & Persistence
- **Storage:** Pre-compiled SQLite 3 database (`content.db`) packed in app assets
- **Search:** Indexed SQLite multi-column search (`sa`, `iast`, `iast_plain`, `hi`, `bn`, `en`)
- **Settings & User State:** SharedPreferences with debounced lifecycle persistence

### Content Pipeline & Vision Extraction
- **OCR & Extraction:** Python 3.10+, Gemini 2.5 Flash Vision API (`google-genai`)
- **PDF Processing:** PyMuPDF (`fitz`) for 200 DPI scan rendering
- **Data Verification:** Automated gap detector and numbering reconciler (`numbering_fixes.json`)

---

## Project Structure

```text
Bhagavatam/
├── app/                                # Android Application
│   ├── src/main/
│   │   ├── assets/
│   │   │   └── content.db              # Embedded master SQLite database (14,580 verses)
│   │   ├── java/com/bhagavatam/app/
│   │   │   ├── audio/                  # NarrationText, Narrator, PlaybackService
│   │   │   ├── data/                   # ContentDb, Annotations, Updater, WordSearch, Strings
│   │   │   ├── state/                  # AppState (global state management)
│   │   │   ├── ui/                     # Compose UI screens, components, and theme
│   │   │   └── util/                   # Transliterate.kt (Devanagari <-> Bengali)
│   │   └── res/
│   │       ├── drawable-nodpi/         # Multi-density and WebP Kangra art assets
│   │       ├── mipmap-*/               # Adaptive launcher icons (mdpi to xxxhdpi)
│   │       └── font/                   # Indic & Latin typography (Noto, Tiro, Literata)
│   └── build.gradle.kts
│
├── content/                            # Scripture Pipeline & Canonical Sources
│   ├── _bengali/bn_extract/            # Gemini Vision PDF extraction engine
│   ├── _build/                         # Promotion and SQLite compiler scripts
│   ├── art/                            # Art direction and prompt matrices (PROMPTS.md)
│   ├── bn/                             # Canonical Bengali chapter markdown (341 chapters)
│   ├── en/                             # Canonical English chapter markdown
│   ├── hi/                             # Canonical Hindi chapter markdown
│   └── content.db                      # Master canonical SQLite database
│
├── docs/                               # Technical Specifications
│   ├── ARCHITECTURE.md                 # Android & audio architecture deep-dive
│   ├── DATA_PIPELINE.md                # PDF extraction & verification manual
│   └── ART_AND_ASSETS.md               # Kangra art prompt guidelines & tooling
│
├── tools/                              # Developer Utilities
│   ├── generate_graphify.py            # AST Knowledge Graph scanner
│   ├── generate_icons.py               # Launcher icon generator
│   ├── import_art.py                   # Image optimizer & WebP asset importer
│   └── make_chapter_prompts.py         # Batch art prompt generator
│
├── apk/                                # Ready-to-install Android APKs
│   ├── Bhagavatam-release.apk          # Production release build (R8 minified, 15.4 MB)
│   └── Bhagavatam-debug.apk            # Debug development build (43.1 MB)
│
├── CLAUDE.md                           # Developer & AI pair-programming manual
├── DESIGN_SYSTEM.md                    # Design tokens & typography standards
└── README.md
```

---

## Getting Started

### Prerequisites
- **Android Studio:** Ladybug (2024.2.1) or newer
- **Android SDK:** `compileSdk 35`, `minSdk 26`
- **JDK:** Java 17
- **Device / Emulator:** Android 8.0+ (API 26+) with Google TTS voice data installed (Hindi / Bengali)

### Building the Android App

1. **Clone the repository:**
   ```bash
   git clone https://github.com/SoujanyaDasRoy/Bhagavatam.git
   cd Bhagavatam
   ```

2. **Build the Release APK (Recommended, 15.4 MB):**
   ```powershell
   # Windows PowerShell
   $env:JAVA_HOME = "C:\Users\sdroy\.gradle\jdks\eclipse_adoptium-17-amd64-windows.2"
   .\gradlew.bat assembleRelease
   ```
   *The generated APK is placed in [`apk/Bhagavatam-release.apk`](apk/Bhagavatam-release.apk).*

3. **Build the Debug APK:**
   ```powershell
   .\gradlew.bat assembleDebug
   ```
   *The generated APK is placed in [`apk/Bhagavatam-debug.apk`](apk/Bhagavatam-debug.apk).*

4. **Install on connected device:**
   ```powershell
   adb install -r apk/Bhagavatam-release.apk
   ```

---

## Content Pipeline Workflows

The repository contains standalone Python tooling to extract and compile the scripture database.

### 1. Vision Extraction from Gita Press PDFs
```powershell
cd content/_bengali/bn_extract
python vision_extract.py run --vol 1 --pages 788-879 --workers 2
```

### 2. Assembly, Verification & Gap Audit
```powershell
python bn_extract.py assemble
```

### 3. Promoting and Compiling SQLite DB
```powershell
python ../../_build/promote_bengali.py --force
python ../../_build/add_bengali.py ../../content.db
python ../../_build/add_bengali.py ../../../app/src/main/assets/content.db
```

---

## Design System & Typography

The application uses **Kesari Deep**, a custom design system optimized for readability of complex Indic scripts:
- **Palette:** Warm parchment background (`#F7F4EE`), Kesari saffron accent (`#A34E10`), Sindoor shloka text (`#6E1B1B`), and 12 distinct Skandha hues.
- **Typography:**
  - Sanskrit Shlokas: Noto Sans Devanagari (22sp / 40sp line height)
  - Hindi Translations: Tiro Devanagari Hindi (18sp / 32sp line height)
  - Bengali Translations: Noto Serif Bengali (18sp / 32sp line height)
  - English Commentary: Literata / Plus Jakarta Sans
- **Accessibility:** Touch targets minimum 48dp, full screen-reader semantic labeling, and WCAG AA contrast compliance.

---

## Documentation Links

- [CLAUDE.md](CLAUDE.md) — Comprehensive developer guide and architecture invariants
- [ARCHITECTURE.md](docs/ARCHITECTURE.md) — Deep-dive into Compose state and the audio subsystem
- [DATA_PIPELINE.md](docs/DATA_PIPELINE.md) — Gita Press extraction, numbering fixes, and verification
- [ART_AND_ASSETS.md](docs/ART_AND_ASSETS.md) — Art direction, aspect ratios, and asset pipelines
- [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) — Color tokens, typography, and spacing specs
