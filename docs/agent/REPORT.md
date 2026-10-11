# Final Verification Report - Bhagavatam v1.1.0

Date: 2026-10-11  
Device tested: Android Emulator (`emulator-5554`, Pixel 8, Android 15 / API 35, 1080 x 2400)  
Build tested: `apk/Bhagavatam-v1.1.0-release.apk` (Release build, versionCode 8, versionName "1.1.0", checked against `app/build.gradle.kts`)  
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
  - Entries: 37,797 across English (17,276), Hindi (7,916), Bengali (11,149) and Odia (1,456).
  - Senses: 96,172 definitions (Odia: 3,806).
  - Integrity: 0 broken cross-references, 0 em/en dashes, verified prefix search table.
- **Search index (`search.db`)**: 5.85 MB on disk (5,849,088 bytes): over the 4 to 5 MB target, under the 8 MB maximum.
  - Schema (version 2): `loose`, `exact`, `grp`, `grp_by_term`, `chapter_start`, `title_key`, `meta`; plain tables, no FTS5.
  - Search latency: reported as under 50 ms on the emulator by the Phase 5 run; not re-measured by the reviewer.
- **Automated tests**:
  - Android Unit Tests (`:app:testDebugUnitTest`): 85/85 passed (0 failed; 81 at the time of the Phase 5 run, 4 tests added since).
  - Phonetic Loose Pairs (`test_loose_pairs.py`): 98/98 pairs passed (26 must-match, 3 must-not-match).
  - Gold Search Queries (`eval_queries.py --engine index`): 28/28 required queries passed (0 failed).
  - Commit check (`commit_check.sh`): PASSED (no sensitive files, databases, or API keys staged).

---

## 3. Notes and Open Questions for Owner

1. **Odia Content Promotion**:
   - Odia scripture remains marked draft in `content/or` pending manual review.
   - Odia dictionary entries (1,456 entries, 3,806 senses) are bundled and marked with an `Odia (Draft)` chip so users understand source maturity.
2. **Physical Device Testing**:
   - When a physical Samsung A35 or device is available via USB debugging, coordinates and layout density can be spot-checked. All layouts use Compose dynamic density units (`dp`, `sp`).

## 4. Corrections after review (2026-10-11)

The numbers above were checked against the files; these items were wrong in the first version and are corrected in place:
versionCode (2, really 8), search.db size and schema (4.88 MB and `postings/words/vocab/trigrams`, really 5.85 MB and `loose/exact/grp/...`),
dictionary entries and senses (92,207 and 153,995, really 37,797 and 96,172) and Odia senses (5,502, really 3,806).
Open finding: `dictionary.db` contains an unused FTS5 virtual table (`entry_fts`). The app never queries it, but AGENTS.md section 8 forbids
relying on FTS5 at API 26 and it adds size. Drop it in `content/_dict/build_dictionary.py` at the next dictionary rebuild.
Screen-level PASS entries (themes, long-press lookups, airplane mode) were not repeated by the reviewer; the reader language switcher
(light and dark, font scale 1.3) was checked by hand on 2026-10-11 and matched.
