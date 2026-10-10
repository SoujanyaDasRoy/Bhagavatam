# Handoff (updated 2026-10-11)

Branch `feature/switcher-search-dictionary`. Nothing pushed. The other agent's uncommitted YouTube/player work is still in the tree: do not touch it.

## Done
- Reader language switcher: committed (45e9a9a), checked on emulator.
- Search Phase 1 (pure Python): DONE (commit beefd56, search.db 5.85 MB, 28/28 gold queries pass).
- Dictionary Phase 2 (pure Python): DONE (commit a38a8e9, dictionary.db 16.34 MB, dict_check 0 fails).
- Search Phase 3a (Kotlin core, plan Task 5): DONE (commit 94805d9). Details below.

## Search Phase 3a: what exists
| File | What |
|---|---|
| `app/.../data/Normalise.kt` | NFC, zero-width joiner removal, toDevanagari, skey, exactOf (shared with dict) |
| `app/.../data/Lemmatizer.kt` | Case suffix stripping for bn, hi, or, sa, en; OnlineMeaning delegated to it |
| `app/.../data/LooseKey.kt` | Rule A phonetic key; 100% byte parity with Python prototype.py |
| `app/.../data/SearchQuery.kt` | Scripture reference parser (0.1 to 12.13), quoted phrases, min length tokenization |
| `app/.../data/SearchIndex.kt` | Delta-varint posting decoder, typo suggestions, story matching, chapter bisection |
| `app/src/test/.../data/` | `LooseKeyTest` (98/98 pairs in loose_pairs.json), `SearchQueryTest`, `SearchIndexTest` |

Commands:
- `./gradlew --offline :app:testDebugUnitTest -Dorg.gradle.java.home="C:\Users\sdroy\.gradle\jdks\eclipse_adoptium-17-amd64-windows.2"` (76/76 unit tests PASS)
- `search.db` copied to `app/src/main/assets/search.db` (5.85 MB, gitignored)

## Next Phase: Phase 3b (Search results screen & find in chapter, plan Tasks 6 to 8)
1. Task 6: Build search results UI (query debounce 150 ms, language chips, highlighted snippets, "Did you mean", story cards).
   NOTE: `Search.kt` has another agent's uncommitted edits; build the new screen in new files and make the smallest possible hook-up edit in `Search.kt`.
2. Task 7: In-chapter find bar in the reader with next/previous matching.
3. Task 8: Ship check, version guard, verify on emulator with `tools/agent/verify.sh`.
