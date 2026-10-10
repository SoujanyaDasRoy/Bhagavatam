# Handoff (updated 2026-10-11)

Branch `feature/switcher-search-dictionary`. Nothing pushed. The other agent's uncommitted YouTube/player work is still in the tree: do not touch it.

## Done
- Reader language switcher: committed (45e9a9a), checked on the emulator (en, hi, bn, dark, font scale 1.3).
- Search Phase 1 (pure Python): DONE and verified. Details below.
- Odia text and OCR output are in `.gitignore` (same as bn/hi/en).

## Search Phase 1: what exists
| File | What |
|---|---|
| `content/_search/prototype.py` | the loose key (rule A, anusvara bug fixed on 2026-10-11, see DECISIONS.md) |
| `content/_search/build_index.py` | `content.db` + `aliases.json` -> `search.db` (schema v2, WITHOUT ROWID tables, no timestamps) |
| `content/_search/search_engine.py` | reference reader: `Index.verses / chapters / lookup / suggest / story_chapters`. The Kotlin `SearchIndex` must behave like this file |
| `content/_search/aliases.json` | 18 name groups (loose key) + 2 related-word groups (exact form); build fails if a listed form is not in the book |
| `content/_search/tests/` | `test_loose_pairs.py` (98 pairs, shared with Kotlin), `test_search_db.py` |

Commands: `py content/_search/build_index.py` then `py content/_search/eval_queries.py` (default engine is the index), `py content/_search/tests/test_search_db.py`, `py content/_search/tests/test_loose_pairs.py`.

Numbers: `search.db` 5.85 MB (hard limit 8, target 4 to 5: missed by about 1 MB; 108,744 of 165,109 keys occur in one verse only), build 15 s, identical bytes on a second build. Gold queries: 28 of 28 pass (all now `required`), including the six former targets (synonym-en, synonym-hi, typo-1, typo-2, alias-narasimha, alias-rasa-lila).

## Design points the Kotlin side must keep
- One word expands to: its loose key, plus every alias group it belongs to (by key), plus every related group it belongs to (by EXACT form, never by key: the key joins मगर with नगर), plus, only if all that is empty and the key has 4+ letters, the nearest key one edit away (longer candidates first, then document frequency).
- Words AND together. Scope "chapter" ANDs at chapter level. Stories are not in `search.db`: match them in the app from `Episodes.kt` with the same loose key (all query keys inside the story's title and keyword keys).
- Keys shorter than 3 letters are not indexed (so 2-letter words find nothing: known limit).
- `meta.content_version` must equal `content.db`'s; the app refuses a mismatch.
- The loose key is a sound match: मगर also finds नगर. Ranking must put the exact word first (the index does not store exact forms except for related-word members; compare against the verse text when ranking).

## Left
1. Phase 3 (Kotlin search, plan Tasks 5 to 8): `LooseKey.kt` parity test reading `content/_search/tests/loose_pairs.json`, `SearchQuery`, `SearchIndex`, results screen, in-chapter find, asset copy of `search.db` (it is gitignored: build it, copy to `app/src/main/assets/search.db`).
2. Dictionary Phase 2 (Python) and Phase 4 (Kotlin).
3. Phase 5 device checks; Samsung A35 never tested.
4. Not verified yet: reader keeps its position after a language switch; Bengali pill label looks slightly smaller than Hindi.
5. Open owner question: the other agent's uncommitted YouTube/player work (AppState.kt, Player.kt, Search.kt ...) needs finishing or parking before Phase 3 hooks into `Search.kt`.
