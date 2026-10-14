# Bhagavatam Codebase Map

This repository contains three connected systems:

1. An Android/Jetpack Compose reader and audio player.
2. A canonical multilingual scripture corpus and SQLite build pipeline.
3. Art, OCR, prompt-generation, and graphing tools used to produce app content and inspect the codebase.

## System map

```text
content/{en,hi,bn}/ + content/index.json
        |
        |  content/_build/*.py and content/_bengali/bn_extract/*.py
        v
content/content.db  ------------------------------+
        |                                           |
        | copied/assembled as app asset            |
        v                                           |
app/src/main/assets/content.db                     |
        |                                           |
        v                                           |
MainActivity -> ContentDb.open() -> SampleData / typed verse models
        |
        v
AppState (single state holder)
        |
        +--> AppNav -> Compose screens -> reusable components + theme
        +--> Annotations / WordSearch / settings persistence
        +--> NarrationText -> Narrator -> Android TTS
        +--> RecitationStream -> YouTube stream resolver/player/cache
        +--> PlaybackService -> Media3 MediaSession / background controls
```

## Android application

### Startup and navigation

- `app/src/main/java/com/bhagavatam/app/MainActivity.kt` is the Android entry activity. It opens the bundled database on an IO dispatcher, shows the splash screen, requests notification permission when playback starts, and mounts `AppNav`.
- `app/src/main/java/com/bhagavatam/app/BhagavatamApp.kt` owns application-wide services/state.
- `app/src/main/java/com/bhagavatam/app/ui/AppNav.kt` defines routes, onboarding, tab scaffolding, mini-player placement, and navigation between reader, player, search, library, saved items, glossary, languages, and settings.

### State and data

- `app/src/main/java/com/bhagavatam/app/state/AppState.kt` is the single mutable UI/business state holder. It owns preferences, language/script/theme choices, reading position, annotations, and playback state.
- `app/src/main/java/com/bhagavatam/app/data/ContentDb.kt` copies `assets/content.db` into app-private storage and opens the newest available database by `content_version`.
- `app/src/main/java/com/bhagavatam/app/data/SampleData.kt` contains the typed scripture/domain models and database-backed access layer.
- `app/src/main/java/com/bhagavatam/app/data/Annotations.kt` stores highlights, notes, bookmarks, and resilient text anchors.
- `app/src/main/java/com/bhagavatam/app/data/WordSearch.kt` provides full-text search and snippets.
- `app/src/main/java/com/bhagavatam/app/data/Episodes.kt`, `OnlineMeaning.kt`, `Updater.kt`, and the `*Text.kt` files provide feature-specific metadata, dictionary/network meaning lookup, update hooks, and localized copy.

### UI surface

- `app/src/main/java/com/bhagavatam/app/ui/screens/` contains feature screens: home/library, chapter index, reader, player, search, saved verses, glossary, onboarding, voice sheets, and settings sub-screens.
- `app/src/main/java/com/bhagavatam/app/ui/components/` contains shared bars, artwork, marked text, icons, ornaments, and common layout primitives.
- `app/src/main/java/com/bhagavatam/app/ui/theme/` contains colors, typography, motion, design tokens, and reader/app theme selection.
- `app/src/main/java/com/bhagavatam/app/util/Transliterate.kt` converts Sanskrit Devanagari to Bengali script while preserving Indic marks and ligatures.

### Audio

- `audio/NarrationText.kt` turns verse text into sentence/line plans for synchronized narration.
- `audio/Narrator.kt` manages Android TTS, voice selection, queueing, and callbacks into app state.
- `audio/FocusController.kt` coordinates reader focus/follow-audio behavior.
- `audio/PlaybackService.kt` keeps playback alive in the background and exposes Media3/MediaSession controls.
- `audio/RecitationStream.kt` and `audio/stream/` handle remote recitation streams, YouTube resolution, caching, and playback.

### Resources and build

- `app/src/main/res/` contains manifest-adjacent XML, icons, fonts, colors, themes, and bundled chapter artwork.
- `app/build.gradle.kts` defines the Android module, Compose/Media3 dependencies, API levels, release shrinking, and APK copy task.
- `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, and `gradle/wrapper/` define the single-module Gradle build.

## Content and data pipeline

- `content/index.json` is the corpus manifest, mapping skandhas/chapters to language files and verse counts.
- `content/en/`, `content/hi/`, and `content/bn/` hold canonical Markdown scripture by language and section.
- `content/content.db` is the working SQLite database; `app/src/main/assets/content.db` is the database packaged into the APK.
- `content/_build/` contains promotion, parsing, database-building, and repair scripts.
- `content/_bengali/bn_extract/` contains Bengali PDF/image extraction, OCR, assembly, audits, expected-verse checks, and targeted repair scripts. Its `data/` directory contains numbering fixes and expected verse metadata.
- `docs/DATA_PIPELINE.md` describes the Bengali OCR-to-database flow in detail.

## Art and developer tooling

- `content/art/` stores chapter prompts, image specifications, generated art metadata, and raw/generated artwork.
- `tools/` contains SVG conversion, prompt generation, art import/grading, icon generation, graph generation, and batch utilities.
- `tools/make_code_map.py` builds a filtered Graphify snapshot of the app source, Gradle files, content build scripts, Bengali extractor, and database schema.
- `graphify-out/graph.html` is the interactive generated dependency graph; `graphify-out/GRAPH_REPORT.md` is its summary report; `graphify-out/graph.json` is the machine-readable graph.

## Tests

`app/src/test/java/com/bhagavatam/app/` covers verse/database behavior, online meanings, annotation anchors, and narration text. The Bengali extractor has additional scripts under `content/_bengali/bn_extract/scripts/` for OCR and assembly checks.

## Useful starting points

| Goal | Start here |
|---|---|
| Add or change a screen | `ui/AppNav.kt`, then the relevant file under `ui/screens/` |
| Change reading behavior/state | `state/AppState.kt` |
| Change database queries/schema | `data/ContentDb.kt`, `data/SampleData.kt`, `content/_build/build_db.py` |
| Change TTS or karaoke | `audio/NarrationText.kt`, `audio/Narrator.kt`, `audio/FocusController.kt` |
| Change background/stream playback | `audio/PlaybackService.kt`, `audio/stream/`, `audio/RecitationStream.kt` |
| Add notes/highlights/search | `data/Annotations.kt`, `data/WordSearch.kt` |
| Add multilingual scripture content | `content/{en,hi,bn}/`, `content/index.json`, then rebuild `content.db` |
| Rebuild the visual code graph | `python tools/make_code_map.py` |

## Build and verification

```powershell
.\gradlew.bat compileDebugKotlin
.\gradlew.bat assembleDebug
```

The Gradle build copies completed APKs into `apk/`. The repository also includes architecture and design references in `docs/ARCHITECTURE.md`, `docs/DATA_PIPELINE.md`, `docs/ART_AND_ASSETS.md`, and `DESIGN_SYSTEM.md`.
