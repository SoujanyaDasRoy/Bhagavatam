# Prompt to send to Antigravity

Copy everything below the line into Antigravity as one message, with the project folder `C:\Users\sdroy\OneDrive\Desktop\Coding\Startups\Bhagavatam` open.

---

You are working on the Bhagavatam Android app (Kotlin, Jetpack Compose): an offline reader and listener for the Shrimad Bhagavat Mahapuran in Sanskrit, Hindi, Bengali and English. You will do three pieces of work, in this order: (A) finish and verify the reader's language switcher, (B) build the search system, (C) build the offline dictionary for English, Hindi, Bengali and Odia. The plans, rules and tools are already written. Your job is to execute them carefully and report honestly.

## 1. Read first, in this order
1. `AGENTS.md` - the working agreement. It is binding. Sections 7 (dictionary) and 8 (search) are specific to this work. Then `docs/agent/DECISIONS.md`: the owner's final answers. They outrank code, data files and your own judgment.
2. `docs/superpowers/plans/2026-10-09-search.md`
3. `docs/superpowers/plans/2026-10-09-dictionary-db.md`
4. `docs/agent/RUNBOOKS.md` - step-by-step procedures (verify a UI change, check a database, commit).
5. `CLAUDE.md` and `DESIGN_SYSTEM.md` - architecture and visual rules.
6. `content/_search/gold_queries.json` and `content/_dict/sources.json`.

## 2. Permissions for this job
- Create a branch first: `git switch -c feature/switcher-search-dictionary`. Work only on that branch.
- You may commit locally, in small commits, adding only the files you changed by name. Never `git add -A`. Run `tools/agent/commit_check.sh` before every commit. No co-author lines, no tool credits in commit messages.
- Never push, merge, rebase onto main, force anything, or publish a release.
- The working tree already holds another agent's uncommitted work (YouTube streaming, player redesign, release notes, `AppState.kt`, `SampleData.kt`, `build.gradle.kts`, and more). Do not edit, revert, stash or commit those files. If you must change one of them, make the smallest edit and say so in your report.
- Do not delete anything outside files you created.

## 3. Before you start (report the results)
- `git status --short` and note which files are not yours.
- `adb devices`. An emulator (`emulator-5554`) is usually present. The owner may also connect a Samsung Galaxy A35 by USB; if it is listed, test on it as well (1080 x 2340, so measure positions from a screenshot, do not reuse the emulator tap coordinates).
- `df -h /c` (the drive was full once; check before big downloads).
- Baseline: `tools/agent/verify.sh` and `python content/_search/eval_queries.py` and `python tools/agent/dict_check.py --selftest`. Write down what passes now.
- If the build says `Failed to find target with hash string 'android-35'`, install it: `sdkmanager "platforms;android-35"` (see AGENTS.md section 6).

## 4. Work package A - language switcher (implemented, not yet verified on a device)
Files: `app/src/main/java/com/bhagavatam/app/ui/screens/LanguageSwitcher.kt` (new), `Reader.kt` (chip row removed, pill added), `app/src/test/.../ui/screens/LanguageSwitcherTest.kt`.
1. Build and install, open a chapter, and check by screenshot, in English and Hindi app language, one light and one dark theme, font scale 1.0 and 1.3:
   - the pill in the top bar names the language being read, and shrinks to an icon after the chapter title scrolls into the bar;
   - tapping it opens a sheet: "Show" (Translation only / With shloka), then either a one-language radio list or the shloka script plus translation switches;
   - a language with no text for the chapter is greyed out with its reason in readable text;
   - the last translation under the shloka cannot be switched off;
   - after a switch the reader stays at the same verse (check this; fix it if not);
   - touch targets at least 48dp, Indic text at least 15sp, nothing clipped at font scale 1.3.
2. Fix what you find. Keep the unit tests passing; add tests for anything you change.
3. The Player has its own separate language bar. Replace it with the same component only if that can be done without touching the other agent's uncommitted `Player.kt` changes; otherwise leave it and say so.
4. Commit when verified.

## 5. Work package B - search (follow `2026-10-09-search.md`, Tasks 0 to 8)
- Task 0 is a gate (see section 7). Do Tasks 1 to 4 (pure Python: lock the key, build `search.db`, aliases, typo suggestions) before any Kotlin.
- The acceptance target is `python content/_search/eval_queries.py --engine index`: no `required` query may fail, and each `target` query that starts passing is promoted to `required` in the same change.
- Do not use SQLite FTS5. Do not search by scanning columns with LIKE. The Kotlin loose key must give the same output as the Python one, tested from one shared pairs file.
- Aliases and related words must be checked against the book text before they are added.

## 6. Work package C - dictionary (follow `2026-10-09-dictionary-db.md`, Tasks 0 to 7)
- Task 0 is a gate (see section 7). Only use sources marked `verified` in `content/_dict/sources.json`. Samsad Bangla and the Oxford McGregor dictionary are rejected (copyrighted); do not use them.
- Use the official Wikimedia dumps. Never crawl the web API page by page (it returns 429 within minutes). Polite User-Agent, resume support, back-off.
- Pipeline first (Tasks 1 to 4), then Android (Tasks 5 to 7). Do not start the Android work until `python tools/agent/dict_check.py <db> --max-mb 30` prints no FAIL and the real coverage numbers are in section 5 of the plan.
- `Normalise.kt` and `Lemmatizer.kt` serve both search and the dictionary. Write them once, in whichever package reaches Kotlin first, and reuse them in the other.
- Odia is incomplete and its source text is an unverified draft. Report Odia coverage as a number and say so in the app.

## 7. Gates and questions
Search Task 0 and Dictionary Task 0 are answered: see `docs/agent/DECISIONS.md`. Proceed past those gates using those answers. For any new licence or size question, write the exact question, with the evidence, to `docs/agent/QUESTIONS.md`. Do not guess on a licence. Keep working on everything that does not depend on the answer (pure code, tests, the pipeline for sources already `verified`), then stop at the gate and say so.

## 8. Hard rules (details in AGENTS.md)
- One agent at a time in a file. Check `git status` before editing.
- Never stage book text (Hindi, English, Bengali, Odia chapter files), `content.db`, `search.db`, `dictionary.db`, page images, PDFs, APKs or `.env` files. They are in `.gitignore`; keep it that way.
- Do not hand-type scripture or translation text. Extraction is done by the tools in `content/_bengali` and `content/_odia`.
- No em dashes or en dashes in anything a person reads. Every new UI string in English, Hindi and Bengali.
- Use `readingFont()` and the design tokens; reuse `SettingsGroup`, `tr()` and existing helpers.
- Do not build or add scraping of YouTube or any ad-free workaround.
- Online features stay optional and send only the single word being looked up.

## 9. Definition of done, per package
- Build passes, unit tests pass, and the relevant checker passes (`eval_queries.py`, `dict_check.py`).
- Verified on a device or emulator by screenshot, with the language, theme and font-scale combinations listed above, and airplane mode on for search and dictionary.
- Sizes and timings measured and written into section 5 of the plan: `search.db` at most 8 MB, `dictionary.db` at most 30 MB, first search results under 50 ms.
- Committed locally on the branch with `commit_check.sh` clean.

## 10. Final report
Write `docs/agent/REPORT.md` and give the same summary in your last message: for each package, what changed (files), what you verified and how, what you could not verify, real numbers (sizes, timings, coverage, gold query results), open questions for the owner, and anything you noticed but did not fix. Do not say "done" for anything you did not run.
