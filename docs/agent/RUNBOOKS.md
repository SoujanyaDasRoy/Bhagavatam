# Runbooks for coding agents

Step-by-step procedures. `AGENTS.md` has the rules; these are the exact steps. Every runbook ends with a report that says what was verified and what was not.

## 1. Start of every session
1. `git status --short` and `git log --oneline | head -5`. Note files you did not touch that are modified: another agent is working. Leave them alone.
2. Read the screen or file you are about to change, and grep its callers.
3. Make sure an emulator is running: `adb devices` shows `emulator-5554 device`. If not, `emulator -avd Pixel_8`, wait for `adb shell getprop sys.boot_completed` to print `1`.

## 2. Change the UI and verify it
1. Make the change. Reuse tokens and helpers (see `AGENTS.md` section 2).
2. `tools/agent/verify.sh` (debug build, unit tests, install, launch, crash check, a screenshot of the first screen).
3. Navigate to the screen you changed: tap with `adb shell input tap X Y` (tab coordinates are in `AGENTS.md`), take `adb exec-out screencap -p > shot.png`, open the image and read it.
4. Repeat with: app language Hindi (Settings > Languages), a dark theme (`adb shell cmd uimode night yes`), font scale 1.3 (`adb shell settings put system font_scale 1.3`). Reset afterwards (`font_scale 1.0`, `night no`).
5. `adb logcat -d -s AndroidRuntime:E` must be empty of `FATAL`.
6. Report with the list of languages, themes and scales you checked.

## 3. Add a setting
1. `state/AppState.kt`: `var x by mutableStateOf(prefs.getBoolean("x", default))` followed directly by `private set`; an `updateX()` that saves; add it to `resetSettings()` (both the variable and the `save { }` block).
2. Add the row with `SwitchRow` / `ValueRow` inside a `SettingsGroup` on the right page (`SettingsReading.kt`, `SettingsListening.kt`, `SettingsWords.kt`, `SettingsAppearance.kt`), or a new page: new file, new route in `ui/AppNav.kt`, new `Entry` in the hub `Settings.kt`.
3. Strings in three languages with `tr(ui, en, hi, bn)`.
4. Apply the setting where it matters (reader, player) and test that it actually changes behaviour, then test Reset.

## 4. Release APK
1. `git status` clean enough that you know what is in the build.
2. Bump `versionCode` and `versionName` in `app/build.gradle.kts` if the database changed.
3. `tools/agent/verify.sh release`. The file is `apk/Bhagavatam-release.apk`.
4. Open it on the emulator and look at Home, a chapter and the player.
5. Do not publish a GitHub release or attach the APK anywhere without being asked: it contains the book text.

## 5. Check a language in the database
Run this against both `content/content.db` and `app/src/main/assets/content.db` (they must be identical):
```python
import sqlite3, re
d = sqlite3.connect("content/content.db")
col = "bn"   # or "hi", "en", "or" once it exists
rows = d.execute(f'select skandha, chapter, num, "{col}" from verse where "{col}" <> ""').fetchall()
print("rows with text:", len(rows))
print("stray verse markers:", sum("॥" in r[3] for r in rows))
print("Latin letters (non-English columns):", sum(bool(re.search("[A-Za-z]", r[3])) for r in rows) if col != "en" else "n/a")
print("shorter than 8:", sum(len(r[3]) < 8 for r in rows))
print("no final stop:", sum(r[3][-1] not in "।?!\"”’)." for r in rows))
orphans = d.execute(f'select count(*) from verse v where {col}_from is not null and (select "{col}" from verse w where w.skandha=v.skandha and w.chapter=v.chapter and w.num=v.{col}_from) = ""').fetchone()
print("pointers to a verse with no text:", orphans)
```
Every count except "rows with text" must be 0. If not, hold those chapters back (move the chapter file out of `content/bn/`) and re-import; do not ship them.

## 6. Import a new language into the database
1. Extraction output goes to a work folder that is in `.gitignore`. Do not transcribe pages yourself; use the tools (`content/_bengali/bn_extract`, `content/_odia`).
2. Assemble, then read the report: chapters with missing verse numbers are not promoted.
3. Promote only chapters that pass (`content/_build/promote_bengali.py` style).
4. Import (`add_bengali.py` / `add_odia.py`), then run the checks in runbook 5.
5. Copy `content/content.db` to `app/src/main/assets/content.db`, bump the version, run runbook 4.

## 7. Commit and push (only when asked)
1. `git status --short`. If there are files you did not change, ask the user whether they belong in the commit.
2. `git add -A` (or only your files), then `tools/agent/commit_check.sh`. Fix anything it reports.
3. `git commit` with a short subject and a paragraph. No `Co-Authored-By`, no tool credit.
4. `git push` only if the user said push. Report the commit id and the file count.

## 8. Final report template
- What changed (files, behaviour) in plain words.
- What I verified, and how (build, tests, device, languages, themes).
- What I did not verify.
- What is left, and anything that needs the user's decision.

## 9. Build and check the dictionary (see the dictionary plan for the tasks)
1. `python content/_dict/coverage_probe.py` - counts the book's words per language (Task 1). Re-run after the Odia text changes.
2. Pipeline scripts in `content/_dict/` (fetch, parse, build) produce `content/_dict/dictionary.db`.
3. `python tools/agent/dict_check.py content/_dict/dictionary.db --max-mb 30` - schema, licences and credits on every source, markup left in meanings, em dashes, very common words present, a sample to read. No FAIL allowed. `--selftest` checks the checker.
4. Coverage: write headwords to `content/_dict/work/headwords_<lang>.txt` and run `python content/_dict/coverage_probe.py --against content/_dict/work/headwords_hi.txt` (one language at a time) to get the share of the book covered.
5. Copy the passing file to `app/src/main/assets/dictionary.db`, bump the version, then follow runbook 2 (UI verify) and runbook 4 (release). Test with airplane mode on: the meaning card must still work.
6. Report the real size, the coverage per language and what you could not check, in section 5 of the plan.

## 10. Change or check search (see the search plan)
1. Baseline: `python content/_search/eval_queries.py` - note which required queries pass and which targets do.
2. Make the change (prototype first in `content/_search/prototype.py` if it is a rule change, then the real indexer, then Kotlin).
3. Re-run the checker. Required failures must be 0. If a target now passes, promote it (edit `status` in `gold_queries.json`). If you found a wrong or missing result by hand, add it as a gold query with the reason, then fix it.
4. Rebuild the index (`content/_search/build_index.py`, when it exists) and check size and the `content_version` in `meta`.
5. Kotlin: `./gradlew :app:testDebugUnitTest` including the loose-key parity test.
6. On a device or emulator (airplane mode on): type each gold query on the Search screen, in English and Hindi app language, light and dark; screenshot the results and read them; check the first result is the right one and highlighting matches the words. Check timing in the log.
7. Report: gold results (required pass count, targets passing), size, timings, what you could not check.

