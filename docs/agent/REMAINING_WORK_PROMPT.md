# Remaining work: prompts per session

How to use: open the project folder in Antigravity, pick the model for the phase, paste the COMMON HEADER and then one PHASE block. Do the phases in order, one session at a time (two agents in the same folder have already cost work). Each phase ends by writing `docs/agent/HANDOFF.md`, which the next session reads first, so a new Google account or a new model starts with full context.

If a phase runs out of quota half way: start a new session with the same prompt. The agent reads HANDOFF.md and continues from the first unticked task in the plan. Nothing is lost, because every task is committed locally when it passes.

Do not use models marked "Leaving soon" for a long phase; they may disappear in the middle of it.

| Phase | Work | Model |
|---|---|---|
| 1 | Search pipeline, pure Python (plan Tasks 1 to 4) | DONE (2026-10-11, commit beefd56; see HANDOFF.md) |
| 2 | Dictionary pipeline, pure Python (plan Tasks 1 to 4) | Claude Sonnet 4.6 (Thinking) |
| 3a | Search core in Kotlin: `LooseKey`, `SearchQuery`, `SearchIndex` + unit tests, no UI (plan Task 5) | Claude Sonnet 4.6 (Thinking) |
| 3b | Search results screen, in-chapter find, ship (plan Tasks 6 to 8) | Claude Opus 4.6 (Thinking) |
| 4 | Dictionary in the app, Kotlin (plan Tasks 5 to 7) | Claude Opus 4.6 (Thinking) |
| 5 | Final check on device, all language, theme and font combinations, report | Gemini 3.8 Flash (Medium) |

---

## COMMON HEADER (paste first, every phase)

You are working on the Bhagavatam Android app (Kotlin, Jetpack Compose): an offline reader and listener for the Shrimad Bhagavat Mahapuran in Sanskrit, Hindi, Bengali and English (Odia in progress). The project folder is open.

Read in this order, fully, before touching anything:
1. `AGENTS.md` (binding; rule 6 sets the order of authority: owner decisions, then the real words of the book, then code, then data and test files)
2. `docs/agent/DECISIONS.md` (the owner's final answers; do not re-ask them, do not reopen them)
3. `docs/agent/HANDOFF.md` if it exists (what the previous session finished and what it left)
4. `docs/agent/RUNBOOKS.md`, then the plan for your phase (named below)

Rules for this job:
- Branch `feature/switcher-search-dictionary`. Commit locally in small commits, adding files by name (never `git add -A`). Run `tools/agent/commit_check.sh` before each commit. No co-author lines, no tool credits. Never push, merge, rebase onto main or force anything.
- The working tree holds another agent's uncommitted work (YouTube streaming and player: `AppState.kt`, `Player.kt`, `Search.kt`, `SampleData.kt`, `PlaybackService.kt`, `RecitationStream.kt`, `audio/stream/`, `build.gradle.kts`, release notes). Do not edit, revert, stash or commit those files. If you must touch one, make the smallest edit and say so in the report.
- Book text, `content.db`, `search.db`, `dictionary.db`, page images, PDFs, APKs and `.env` never go into git. Do not hand-type scripture.
- Never use SQLite FTS5. No em dashes or en dashes in text people read. Every new UI string in English, Hindi and Bengali.
- Report honestly: say "done" only for what you ran. Real numbers (sizes, timings, counts), not estimates.
- Before you start: `git status --short`, `adb devices`, `df -h /c`. If the build says `Failed to find target with hash string 'android-35'`, run `sdkmanager "platforms;android-35"`.

When you finish, or when you must stop, do two things:
1. Write or update `docs/agent/HANDOFF.md`: what is done (with the commit hashes), what is left, the exact commands to resume, numbers measured, anything surprising. Keep it under 60 lines.
2. Fill in section 5 (Results) of the plan you worked on, with the real numbers.

---

## PHASE 1: Search pipeline (Python only). DONE, kept for reference

Finished by Claude Code on 2026-10-11 after the Opus session ran out of quota halfway. Read `docs/agent/HANDOFF.md` instead of doing this phase.

Plan: `docs/superpowers/plans/2026-10-09-search.md`, Tasks 1 to 4. Task 0 is answered (see DECISIONS.md).

Goal: a real `content/_search/search.db` built by `content/_search/build_index.py`, queried by `eval_queries.py --engine index` (implement `_engine_index` in that file, same `verses()` and `chapters()` interface as the prototype).

Do:
1. Task 1: the loose key is FINAL (rule A in `prototype.py`, decided in DECISIONS.md). Do not change it. Make sure `py content/_search/tests/test_loose_pairs.py` passes.
2. Task 2: build the index. Own inverted-index tables, not FTS5. Size at most 8 MB, aim for 4 to 5 MB. Record `content_version` in it.
3. Task 3: `aliases.json`. Start from the stories list in `app/.../data/Episodes.kt` and the most frequent names. Every alias and related word must occur in the book text, checked by a script, not from memory. Spelling variants such as Prahlad / Pralhad and Brahma / Bramha go here, not into the key rule.
4. Task 4: "did you mean" (the `vocab` table, about 0.3 MB).

Acceptance: `py content/_search/eval_queries.py --engine index` has 0 failed `required` queries. Each `target` query that now passes is moved to `required` in the same commit. The 6 targets to aim for: synonym-en, synonym-hi, typo-1, typo-2, alias-narasimha, alias-rasa-lila. If one of them cannot pass honestly, say why in HANDOFF.md; do not weaken the query.

---

## PHASE 2: Dictionary pipeline (Python only). Model: Claude Sonnet 4.6 (Thinking)

Plan: `docs/superpowers/plans/2026-10-09-dictionary-db.md`, Tasks 1 to 4. Task 0 is answered: only the four verified Wiktionaries (en, hi, bn, or), CC BY-SA 4.0. Samsad Bangla, Oxford McGregor and Shabdsagar-derived text are out.

Do:
1. Task 1: corpus vocabulary (`content/_dict/coverage_probe.py` already works; reuse it).
2. Task 2: fetch from the official Wikimedia dumps only. Never crawl the web API (429 within minutes). Polite User-Agent, resume, back-off. Downloads go to `content/_dict/work/` (ignored by git). Check `df -h /c` first.
3. Task 3: parsers to a common JSONL. Strip wiki markup; keep the licence and source on every entry.
4. Task 4: build `dictionary.db`.

Acceptance: `py tools/agent/dict_check.py content/_dict/dictionary.db --max-mb 30` prints no FAIL; real coverage numbers (per language: share of the book's words that have an entry) are written to section 5 of the plan. Odia coverage is reported as a number and flagged as built on an unverified draft.
Do not start Android work in this phase.

---

## PHASE 3: Search in the app (Kotlin). 3a: Claude Sonnet 4.6 (Thinking), 3b: Claude Opus 4.6 (Thinking)

Plan: `docs/superpowers/plans/2026-10-09-search.md`, Tasks 5 to 8. Phase 1 is done: read HANDOFF.md first; `content/_search/search_engine.py` is the behaviour to port, `build_index.py` has the schema. Do 3a (Task 5, no UI, pure unit tests) in one session and 3b (Tasks 6 to 8) in the next, so one quota is enough for each.
Stories are not in `search.db`: match them from `Episodes.kt` with the same loose key, as `search_engine.story_chapters` does. Copy a freshly built `search.db` to `app/src/main/assets/search.db` (gitignored) and check `meta.content_version` against the database.

Do:
- Task 5: Kotlin core. The Kotlin loose key must give exactly the Python output: write `LooseKeyTest` that reads `content/_search/tests/loose_pairs.json` (the same file; copy it into `app/src/test/resources/` only if reading across the tree is not possible, and say so). `Normalise.kt` and `Lemmatizer.kt` serve both search and the dictionary: check whether they exist before writing; write once.
- Task 6: results screen. Task 7: in-chapter find. Task 8: ship (asset copy of `search.db`, version check that refuses a mismatching index).
- `Search.kt` has another agent's uncommitted edits: build the new screen in new files and make the smallest possible hook-up edit in `Search.kt`; say so in HANDOFF.md.

Acceptance: build and unit tests pass; first results under 50 ms on the emulator, not on the main thread, stale queries cancelled; airplane mode on; screenshots in English and Hindi app language, light and dark, font scale 1.0 and 1.3; `LanguageSwitcher` untouched. Run `tools/agent/verify.sh`.

---

## PHASE 4: Dictionary in the app (Kotlin). Model: Claude Opus 4.6 (Thinking)

Plan: `docs/superpowers/plans/2026-10-09-dictionary-db.md`, Tasks 5 to 7. Needs Phases 2 and 3 done.

Do:
- Task 5: data layer (open `dictionary.db` read-only from assets, reuse `Normalise.kt` and `Lemmatizer.kt` from Phase 3, do not copy them).
- Task 6: UI. Extend the existing compact word-meaning card (`MeaningCard`); press-and-hold stays the gesture, a single tap on a word does nothing; Google results stay inside the app, never Chrome; online lookups are optional and send only the single word.
- Task 7: Credits screen (source, licence, link per dictionary), accessible from Settings. CC BY-SA 4.0 share-alike applies to `dictionary.db`, not to the app code.

Acceptance: `dict_check.py` still passes on the shipped copy; APK grows by at most 10 MB; works with airplane mode on; screenshots as in Phase 3.

---

## PHASE 5: Final check and report. Model: Gemini 3.8 Flash (Medium)

Mechanical and thorough, not creative. Do not change code except for an obvious one-line fix; list everything else.

1. Build, install, launch (`tools/agent/verify.sh`). Walk a fixed list on the emulator and, if connected, the Samsung A35 (1080 x 2340: take positions from a screenshot, do not reuse emulator coordinates):
   - Reader language pill and sheet: Hindi, Bengali, English, Sanskrit with and without translation; the last translation cannot be switched off; unavailable languages are greyed with a reason; the verse position is kept after a switch.
   - Search: names across scripts (Krishna, कृष्ण, কৃষ্ণ), a typo, a related-word query, airplane mode.
   - Dictionary: press-and-hold a word in Hindi, Bengali, English; Credits screen.
   - Themes Prabhat, Pothi, Sandhya, Ratri; font scale 1.0 and 1.3; touch targets 48dp, Indic text at least 15sp.
2. Run all checks: unit tests, `eval_queries.py --engine index`, `test_loose_pairs.py`, `dict_check.py`, `commit_check.sh`.
3. Write `docs/agent/REPORT.md`: a table of every item above with PASS, FAIL or NOT TESTED, screenshots paths, real numbers, and the open questions for the owner.
