# Handoff (updated 2026-10-11)

Branch `feature/switcher-search-dictionary`. Nothing pushed. The other agent's uncommitted YouTube/player work is still in the tree: do not touch it.

## Done
- Reader language switcher: committed (45e9a9a), checked on emulator.
- Search Phase 1 (pure Python): DONE (commit beefd56, search.db 5.85 MB, 28/28 gold queries pass).
- Dictionary Phase 2 (pure Python): DONE (commit a38a8e9, dictionary.db 16.34 MB, dict_check 0 fails).
- Search Phase 3a (Kotlin core, plan Task 5): DONE (commit 94805d9).
- Search Phase 3a (Kotlin core, plan Task 5): DONE (commit 94805d9).
- Search Phase 3b (Search results UI & Reader in-chapter find, plan Tasks 6 to 8): DONE (commit ab2a4a7).
- Dictionary Phase 4 (Kotlin data layer, UI & Credits, plan Tasks 5 to 7): DONE (commit f53cc60). Details below.

## Dictionary Phase 4: what exists
| File | What |
|---|---|
| `app/.../data/DictionaryDb.kt` | Read-only SQLite helper opening `dictionary.db` copied from assets to files directory |
| `app/.../data/Dictionary.kt` | Offline lookup hierarchy (exact -> Lemmatizer stem -> inflected forms -> skey script fallback), prefix search |
| `app/.../data/SavedWords.kt` | `SavedWordStore` inside SQLite `annotations.db` (saved words never leave device) |
| `app/.../data/Normalise.kt` | Extended with `foldChandrabindu` and `stripNukta` |
| `app/.../state/AppState.kt` | `meaningLangMode`, `preferredMeaningLang`, `savedWords`, `toggleSavedWord`, `removeSavedWord` |
| `app/.../ui/screens/AnnotationSheet.kt` | Extended `MeaningCard` with offline dictionary lookups, POS badge, CC BY-SA 4.0 license tag, bookmark button, "Open in dictionary" |
| `app/.../ui/screens/DictionaryScreen.kt` | 150 ms debounced search, language filter chips (All, EN, HI, BN, OR Draft), active card with IPA/etymology/senses, bookmark/save, copy/share, saved words list |
| `app/.../ui/screens/CreditsScreen.kt` | Source attribution, CC BY-SA 4.0 badges, dump dates, clickable links for all 4 Wiktionary editions, Odia draft notice |
| `app/.../ui/screens/SettingsWords.kt` | "Meaning language" segment control (Word's, English, App language) |
| `app/.../ui/screens/Settings.kt` | Library entry for "Dictionary" and About entry for "Dictionary credits" |
| `app/.../ui/AppNav.kt` | `Routes.DICTIONARY` and `Routes.CREDITS` navigation routes |
| `app/src/test/.../data/DictionaryTest.kt` | Unit tests for normalization folding, stem lookups, and saved word store |

Verification summary:
- Unit tests: 81/81 PASS (`./gradlew.bat --offline :app:testDebugUnitTest`)
- Dictionary database: PASS (`py tools/agent/dict_check.py app/src/main/assets/dictionary.db --max-mb 30`, 16.34 MB, 0 fails)
- Emulator verification:
  - Dictionary prefix search verified ("bhakti", "dharma", "karma") with senses, POS tags, and etymology.
  - Word bookmarking and saved words management verified (toggle, list, remove).
  - Offline lookup verified in Airplane Mode (`adb shell cmd connectivity airplane-mode enable`).
  - Dark mode (`cmd uimode night yes`) and Light mode verified.
  - Font scale 1.3 verified without text clipping.
  - English and Hindi UI verified (`शब्दकोश`, `शब्दकोश आभार`, language chips, Hindi descriptions).
  - `statusBarsPadding` applied to Dictionary and Credits screens to avoid status bar overlap.
  - Zero crashes in `adb logcat -d -s AndroidRuntime:E`.
  - `commit_check.sh`: all checks passed (no book text, no databases, no APKs, no API keys, no em dashes).

## All Phases (1, 2, 3a, 3b, 4) Complete
All three original work tracks:
(A) Reader language switcher (committed)
(B) Offline search pipeline & Kotlin UI (committed)
(C) Offline dictionary pipeline & Kotlin UI (committed)
are finished, tested, and verified on the emulator.

