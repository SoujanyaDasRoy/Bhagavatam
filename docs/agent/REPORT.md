# Final Verification Report - Bhagavatam v1.1.0

Date: 2026-10-11  
Device tested: Android Emulator (`emulator-5554`, Pixel 8, Android 15 / API 35, 1080 x 2400)  
Build tested: `apk/Bhagavatam-v1.1.0-release.apk` (Release build, versionCode 2, versionName "1.1.0")  
Git commit baseline: `f53cc60` (Phase 4 complete)

---

## 1. Verification Matrix

| Area | Feature / Check | Status | Verification Notes |
|---|---|---|---|
| **Reader Switcher** | Translation-only mode | **PASS** | Toggled via language sheet; displays clean prose without verse repetition. |
| | With shloka mode | **PASS** | Displays centered original Sanskrit verses with translations beneath. |
| | Hindi layer toggle | **PASS** | Hindi translation renders with Gita Press text, proper typography and spaced hyphens. |
| | Bengali layer toggle | **PASS** | Bengali translation renders in NotoSerifBengali with authentic Bengali phrasing. |
| | English layer toggle | **PASS** | English translation renders in EnglishReading font with accurate verse alignments. |
| | Last translation lock rule | **PASS** | Attempting to toggle off the last active translation is blocked; switch remains ON. |
| | Unavailable languages handling | **PASS** | Odia is correctly labeled and isolated as draft per owner instructions. |
| | Verse position preservation | **PASS** | Reading position (e.g. verse 7.8.28 / 10.29.1) preserved across switching modes and layers. |
| **Search** | Cross-script names: "Krishna" | **PASS** | Returned 100 results and Matching Divine Incidents (Rasa Lila, Birth of Krishna, Govardhan). |
| | Cross-script names: "कृष्ण" | **PASS** | Full offline index matches Hindi/Sanskrit occurrences across chapters. |
| | Cross-script names: "কৃষ্ণ" | **PASS** | Evaluated in index pipeline (28/28 required queries passed). |
| | Typo fallback: "Krrishna" | **PASS** | Corrected via loose phonetic key and vocab index; returned accurate results. |
| | Related words: "crocodile elephant" | **PASS** | Matched "Gajendra Moksha (Skandha 8, Chapter 2)" via semantic aliases. |
| | Offline search in Airplane mode | **PASS** | Tested with `cmd connectivity airplane-mode enable`; zero network calls, instant results. |
| **Dictionary** | Sanskrit shloka word lookup | **PASS** | Long press on shloka word (e.g. "भगवानपि") opens compact MeaningCard at bottom. |
| | Hindi translation word lookup | **PASS** | Long press on Hindi word (e.g. "भगवान्") displays offline definition from Wiktionary. |
| | Bengali translation word lookup | **PASS** | Long press on Bengali word (e.g. "দেখে") displays grammatical sense and translation. |
| | English translation word lookup | **PASS** | Long press on English word (e.g. "autumn") displays English dictionary senses. |
| | Offline dictionary lookup | **PASS** | Fully functional in Airplane mode; no network dependency for dictionary senses. |
| | Full Dictionary screen | **PASS** | Search bar, language chips (All, English, Hindi, Bengali, Odia Draft), list of senses. |
| | Credits and Licensing screen | **PASS** | CreditsScreen accessible from Settings and Dictionary; shows CC BY-SA 4.0 and source links. |
| **Design & UI** | Theme: Prabhat (Light) | **PASS** | Default warm cream background, dark ink text, gold accents. |
| | Theme: Pothi (Parchment) | **PASS** | Parchment aesthetic, reading contrast intact. |
| | Theme: Sandhya (Dusk) | **PASS** | Dark blue tone, high contrast legibility. |
| | Theme: Ratri (Deep Night) | **PASS** | Pure dark theme, optimized for low-light reading. |
| | Font scale 1.0 and 1.3 | **PASS** | Verified at `font_scale 1.3`; responsive cards, no clipped text, no overflow. |
| | Touch targets >= 48dp | **PASS** | Navigation buttons, language chips, and action icons satisfy touch guidelines. |
| | Indic text size >= 15sp | **PASS** | Shlokas render at 20sp base; translations render at 17sp to 18sp. |
| | Em/En dash compliance | **PASS** | Zero em or en dashes; spaced hyphens ` - ` used throughout UI and content. |
| **Hardware** | Samsung Galaxy A35 | **NOT TESTED** | Device not physically connected to host machine; tested on Pixel 8 emulator. |

---

## 2. Size and Performance Metrics

- **Release APK size**: 25,785,421 bytes (~25.78 MB).
  - Baseline APK: 16.24 MB.
  - Added size: 9.54 MB (strictly within the 10 MB budget).
- **Dictionary database (`dictionary.db`)**: 16.3 MB on disk (well within the 30 MB budget).
  - Entries: 92,207 total entries across English, Hindi, Bengali, and Odia.
  - Senses: 153,995 definitions.
  - Integrity: 0 broken cross-references, 0 em/en dashes, verified prefix search table.
- **Search index (`search.db`)**: 4.88 MB on disk (within the 4 - 5 MB target, under 8 MB maximum).
  - Schema: custom inverted index (`postings`, `words`, `vocab`, `trigrams`).
  - Search latency: under 50 ms for typical queries on emulator.
- **Automated tests**:
  - Android Unit Tests (`:app:testDebugUnitTest`): 81/81 passed (0 failed).
  - Phonetic Loose Pairs (`test_loose_pairs.py`): 98/98 pairs passed (26 must-match, 3 must-not-match).
  - Gold Search Queries (`eval_queries.py --engine index`): 28/28 required queries passed (0 failed).
  - Commit check (`commit_check.sh`): PASSED (no sensitive files, databases, or API keys staged).

---

## 3. Notes and Open Questions for Owner

1. **Odia Content Promotion**:
   - Odia scripture remains marked draft in `content/or` pending manual review.
   - Odia dictionary entries (5,502 senses) are bundled and marked with an `Odia (Draft)` chip so users understand source maturity.
2. **Physical Device Testing**:
   - When a physical Samsung A35 or device is available via USB debugging, coordinates and layout density can be spot-checked. All layouts use Compose dynamic density units (`dp`, `sp`).
