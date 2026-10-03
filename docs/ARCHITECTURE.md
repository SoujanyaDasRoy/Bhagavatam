# Architecture Specification

> **Bhagavatam Android Client & Subsystems Architecture**

---

## 1. High-Level Architecture Overview

The Bhagavatam app is built using modern **Android Jetpack Compose** architecture following **MVI / Single Source of Truth (SSOT)** state principles.

```mermaid
graph TD
    subgraph UI Layer (Jetpack Compose)
        Nav[AppNav / Navigation] --> Screens[Screens: Home, Reader, Player, Search, Library, Settings, Me]
        Screens --> Components[Components: Bars, Art, MarkedText, Common]
        Screens --> Theme[Theme & Tokens: Kesari Deep]
    end

    subgraph State & Business Logic
        AppState[AppState (Singleton State Holder)]
        FocusCtrl[FocusController]
        Translit[Transliterate (Sanskrit -> Bengali)]
    end

    subgraph Audio Subsystem
        Narrator[Narrator (TTS Controller)]
        NarrationText[NarrationText (Verse Planner)]
        PlaybackService[PlaybackService (MediaSession Foreground Service)]
    end

    subgraph Data & Storage Layer
        ContentDb[ContentDb (SQLite Reader)]
        Annotations[Annotations & Notes]
        WordSearch[FTS5 Search & Snippets]
        Prefs[SharedPreferences / DataStore]
    end

    Screens <--> AppState
    AppState --> ContentDb
    AppState --> Narrator
    AppState --> Annotations
    Narrator --> NarrationText
    Narrator <--> PlaybackService
    Narrator --> FocusCtrl
```

---

## 2. Core Modules & Subsystems

### 2.1 UI Layer (`app/ui/`)
- **Navigation (`AppNav.kt`):** Floating bottom navigation bar managing top-level destinations (`Home`, `Granth/Library`, `Search`, `Me/Saved`) with smooth animated cross-fades.
- **Reader Screen (`Reader.kt`):**
  - Displays chapter title, Sanskrit verses (with Devanagari or Bengali script), translation text in active language, speaker headings, and annotations.
  - Pinch-to-zoom text scaling, horizontal swipe for chapter navigation, word-tap for dictionary lookup, and long-press for verse bookmarking.
- **Player Screen (`Player.kt`):**
  - Full-screen and mini-player interfaces.
  - Active sentence highlighting synchronized with TTS output.
  - Controls for speed, voice selection, pause duration, and chapter playlist tracking.
- **Theme (`theme/`):**
  - Four distinct reader themes: **Prabhat** (light parchment `#F7F4EE`), **Sandhya** (deep night `#12141C`), **Pothi** (classic sepia), and **Ratri** (OLED pitch black).
  - Skandha-specific color palettes (12 unique traditional hues).
  - Strict adherence to WCAG AA contrast ratios (minimum 4.5:1).

### 2.2 Data Layer (`app/data/`)
- **`ContentDb.kt`:**
  - High-performance SQLite database helper reading directly from `app/src/main/assets/content.db`.
  - Zero-allocation queries mapping verses, chapters, and skandhas into typed data classes.
- **`Annotations.kt`:**
  - Stores user highlights, notes, and bookmarks with resilient text anchoring (re-finds offsets even if text changes slightly across updates).
- **`WordSearch.kt` & `GoogleSnippet.kt`:**
  - SQLite FTS5 full-text indexing supporting cross-lingual queries across Sanskrit, Hindi, English, and Bengali.
  - Generates highlighted snippets with exact match offsets.

### 2.3 Audio & Narration Engine (`app/audio/`)
- **`NarrationText.kt` (Pure Kotlin):**
  - Parses verses into structured `VersePlan` objects containing sentence segments and dynamic silence pauses.
  - Handles dialog intro splits, IAST anglicization, footnote stripping, and Sanskrit pada cadence.
- **`Narrator.kt`:**
  - Android TTS engine manager supporting one-verse lookahead queuing to eliminate inter-verse audio latency.
  - Hindi voice fallback for Sanskrit recitation (due to absence of native Sanskrit TTS engines on Android).
- **`PlaybackService.kt`:**
  - `MediaSessionService` implementation keeping audio alive in the background and presenting lock-screen / notification controls.

### 2.4 Transliteration Subsystem (`app/util/Transliterate.kt`)
- Transforms Sanskrit Devanagari Unicode characters into East Indic Bengali script in real-time.
- Accurately preserves halant ligatures, nuktas, visarga, anusvara, and Vedic accents.
