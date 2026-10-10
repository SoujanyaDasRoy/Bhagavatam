# Handoff (updated 2026-10-11)

Branch `feature/switcher-search-dictionary`. Nothing pushed. The other agent's uncommitted YouTube/player work is still in the tree: do not touch it.

## Done
- Reader language switcher: committed (45e9a9a), checked on emulator.
- Search Phase 1 (pure Python): DONE (commit beefd56, search.db 5.85 MB, 28/28 gold queries pass).
- Dictionary Phase 2 (pure Python): DONE (commit a38a8e9, Tasks 1 to 4 of plan). Details below.

## Dictionary Phase 2: what exists
| File | What |
|---|---|
| `content/_dict/coverage_probe.py` | Task 1: corpus vocabulary counts and coverage analyzer |
| `content/_dict/fetch.py` | Task 2: downloads dumps from dumps.wikimedia.org and Kaikki.org with resume and back-off |
| `content/_dict/parse_wiktionary.py` | Task 3: parses XML dumps and Wiktextract JSONL, strips wiki markup, enforces no em/en dashes |
| `content/_dict/tests/test_parse.py` | Unit tests for wikitext cleaning and 4-language entry parsing (all pass) |
| `content/_dict/build_dictionary.py` | Task 4: builds `dictionary.db` (FTS5 prefix search, skey tatsama mapping, deduplication) |

Commands:
- `py content/_dict/build_dictionary.py` (builds `content/_dict/dictionary.db` in ~80s)
- `py tools/agent/dict_check.py content/_dict/dictionary.db --max-mb 30` (PASS: 0 fails)
- `py -m unittest content/_dict/tests/test_parse.py` (all tests pass)

Numbers:
- `dictionary.db`: 16.34 MB on disk (budget 30 MB). 37,797 valid entries (en: 17,276, bn: 11,149, hi: 7,916, or: 1,456), 96,172 senses.
- Book text coverage (raw tokens before Lemmatizer stemming):
  - English: 96.0% tokens (14,660 / 19,238 forms; 960 / 1,000 top words)
  - Bengali: 75.8% tokens (12,814 / 49,574 forms; 841 / 1,000 top words)
  - Hindi: 75.3% tokens (8,558 / 36,063 forms; 788 / 1,000 top words)
  - Odia: 26.2% tokens (1,459 / 65,025 forms; 266 / 1,000 top words; draft text)

## Next Phase: Phase 3a (Search core in Kotlin, plan Task 5)
1. Port loose key to Kotlin (`LooseKey.kt`). Parity test must read `content/_search/tests/loose_pairs.json`.
2. Implement `Normalise.kt` and `Lemmatizer.kt` (to serve both Search and Dictionary).
3. Implement `SearchQuery` and `SearchIndex` matching `content/_search/search_engine.py`. Unit tests only, no UI.
4. Next after 3a: Phase 3b (UI screens), Phase 4 (App dictionary), Phase 5 (Device check).
