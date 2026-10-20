# Speed and Ease Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Home appears sooner, chapters open and turn without a pause, paintings stop eating memory, and three small frictions go.

**Architecture:** Start-up opens only the text before the first screen; the dictionary and search index open on first use. Databases are copied out of the APK only when their own version changed (a small `db_versions.properties` asset written by the builders). Paintings go through one composable that decodes them at display size off the main thread with a memory cache, and the shipped files shrink to 1280 x 800. The reader loads chapters off the main thread and reads the neighbours ahead.

**Tech Stack:** Kotlin, Jetpack Compose, Android SQLite, BitmapFactory, androidx.profileinstaller; Python builders in `content/_build`, `content/_dict`, `content/_search`, `tools/art`.

**Spec:** `docs/research/SPEED_AND_EASE_2026-10-18.md`

## Global Constraints
- minSdk 26, targetSdk 34; no new library.
- Everything works offline; no network in any task.
- `content/content.db` and `app/src/main/assets/content.db` change together (CLAUDE.md 5.5).
- No em or en dashes in text people read; Indic text 15sp or more; touch targets 48dp.
- Commits without co-author lines (owner's rule).
- Build and test: `JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-17-amd64-windows.2 ./gradlew --offline :app:testDebugUnitTest -Dorg.gradle.java.home="$JAVA_HOME"`; release APK in `%LOCALAPPDATA%\BhagavatamBuild\app\outputs\apk\release`.

## Review Focus
1. A word looked up, or a search typed, in the first second after start, before the dictionary or index is open: it must still get its answer (open on demand), never "no entry".
2. An update that changes a database but forgets to change its version: the old copy would stay. The builders must write the version file themselves, and a version file missing from the APK must mean "copy".
3. A painting name with no drawable, a very wide or tall layout, and a phone low on memory: no crash, the gradient stays.
4. Swiping chapters quickly while the next one is still being read ahead: no duplicate read, no list from the wrong chapter.
5. A reader who removed the three seeded bookmarks in an older version: their saved list must not change on update.

---

### Task 1: Performance check script and baseline

**Files:**
- Create: `tools/qa/perf_check.py`
- Create: `docs/audits/PERF_2026-10-18.md`

- [ ] **Step 1:** Write `perf_check.py` with `--install <apk>`, `--first` and `--cold N`: installs, measures `am start -W` TotalTime, takes a filmstrip (screencap loop) and reports the time the Home texts ("Jai Shree", "जय श्री") are first in a uiautomator dump, then `dumpsys meminfo` Native Heap and TOTAL PSS after Home has settled, and the APK size. The scratch versions `perf.py` and `frames.py` from the research session show the calls.
- [ ] **Step 2:** Run it on the current release build. Expected: numbers close to the spec's table. Write them as the "Before" column of `PERF_2026-10-18.md`.
- [ ] **Step 3:** Commit `tools/qa/perf_check.py docs/audits/PERF_2026-10-18.md docs/research/SPEED_AND_EASE_2026-10-18.md`.

### Task 2: Open only the text before the first screen

**Files:**
- Modify: `app/src/main/java/com/bhagavatam/app/MainActivity.kt`
- Modify: `app/src/main/java/com/bhagavatam/app/data/DictionaryDb.kt`, `data/SearchIndex.kt`, `data/Dictionary.kt`, `ui/screens/SearchResultsScreen.kt`, `BhagavatamApp.kt`

**Interfaces:**
- Produces: `DictionaryDb.ensureOpen(): Boolean` and `SearchIndex.ensureOpen(): Boolean` (open with the application context kept by `BhagavatamApp.instance`, return `isOpen`; safe from any thread, `@Synchronized`).

- [ ] **Step 1:** `BhagavatamApp` keeps `companion object { lateinit var instance: BhagavatamApp }` set in `onCreate`.
- [ ] **Step 2:** `MainActivity`: the splash waits only for `ContentDb.open`; after `ready = true`, `launch(Dispatchers.IO) { SearchIndex.ensureOpen(); DictionaryDb.ensureOpen() }`. Keep the 700 ms minimum splash.
- [ ] **Step 3:** Every `if (!DictionaryDb.isOpen) return emptyList()` in `Dictionary.kt` becomes `if (!DictionaryDb.ensureOpen()) return emptyList()`; `SearchResultsScreen` uses `SearchIndex.ensureOpen()` where it reads `isOpen` (both run on IO already).
- [ ] **Step 4:** Build, install, run `perf_check.py --cold 3`. Expected: Home visible earlier than the baseline. Long-press a word within two seconds of start: the meaning appears.
- [ ] **Step 5:** Commit.

### Task 3: No full scan of the verse table at start

**Files:**
- Modify: `content/_build/add_bengali.py` (write `meta.bn_chapters`), `app/src/main/java/com/bhagavatam/app/data/SampleData.kt`
- Test: `app/src/test/java/com/bhagavatam/app/data/ChapterListTest.kt`

**Interfaces:**
- Produces: `fun parseChapterList(s: String?): Set<String>` in `SampleData.kt` (comma-separated "S.C"; null or blank gives an empty set).

- [ ] **Step 1: Failing test.** `parseChapterList("1.1,10.29")` equals `setOf("1.1", "10.29")`; `parseChapterList(null)` and `parseChapterList("")` are empty.
- [ ] **Step 2:** Run it, expect FAIL (unresolved).
- [ ] **Step 3:** Implement; in `attach`, read `SELECT value FROM meta WHERE key='bn_chapters'`. When the key is missing (an older database), run the old DISTINCT query on a background thread (`Thread { ... }.start()` filling `bengaliChapters`), never on the opening thread.
- [ ] **Step 4:** `add_bengali.py` writes `bn_chapters` (sorted "S.C" list of chapters with any Bengali text) into `meta`; run it the cheap way on both db copies: a short script that computes the same list from the current `verse` table and inserts the key (the full `add_bengali.py` run is not needed now). Check both copies are identical (`cmp`).
- [ ] **Step 5:** Tests pass; commit.

### Task 4: Copy a database out of the APK only when it changed

**Files:**
- Create: `content/_build/db_versions.py` (writes `app/src/main/assets/db_versions.properties`)
- Modify: `content/_build/build_db.py`, `add_bengali.py`, `add_odia.py`, `content/_dict/book_gloss.py`, `content/_dict/add_book_words.py`, `content/_search/build_index.py` (each calls `db_versions.write()` at the end)
- Modify: `data/ContentDb.kt`, `data/DictionaryDb.kt`, `data/SearchIndex.kt`
- Create: `app/src/main/java/com/bhagavatam/app/data/AssetVersions.kt`
- Test: `app/src/test/java/com/bhagavatam/app/data/AssetVersionsTest.kt`

**Interfaces:**
- Produces: `object AssetVersions { fun parse(text: String): Map<String, String>; fun needsCopy(stored: String?, inApk: String?, fileExists: Boolean): Boolean }` and `fun read(ctx): Map<String, String>`.
- The version of a file is the SHA-1 of its bytes, first 16 hex digits (`db_versions.write()` computes it for `content.db`, `dictionary.db`, `search.db` in `app/src/main/assets`).

- [ ] **Step 1: Failing tests.** `needsCopy("a", "a", true)` is false; `needsCopy("a", "b", true)` true; `needsCopy("a", "a", false)` true; `needsCopy(null, "a", true)` true; `needsCopy("a", null, true)` true (no version file in the APK means copy, as now); `parse("content.db=ab12\ndictionary.db=cd34\n")` gives the two pairs.
- [ ] **Step 2:** Run, expect FAIL.
- [ ] **Step 3:** Implement. Each opener stores the copied file's version in its SharedPreferences (`assetHash`) and copies only when `needsCopy` says so; the app version code is no longer a reason to copy. `ContentDb` keeps its `content_version` choice between bundled and downloaded copies.
- [ ] **Step 4:** Write `db_versions.py` and hook it into the builders; run it once now. Tests pass.
- [ ] **Step 5:** Check on the emulator: install the build twice with a raised `versionCode` in between and no database change; the second start is as fast as a normal cold start (perf_check), and `run-as` shows the database files' timestamps unchanged (debug build).
- [ ] **Step 6:** Commit (the properties file is small and safe to commit: it holds only hashes).

### Task 5: Paintings decoded at display size, off the main thread

**Files:**
- Create: `app/src/main/java/com/bhagavatam/app/ui/components/ArtImage.kt`
- Modify: `ui/components/Art.kt` (`ArtBackdrop`, `ChapterBanner`), `ui/components/Bars.kt:201`, `ui/screens/Player.kt:349`, `ui/screens/Onboarding.kt:88`
- Test: `app/src/test/java/com/bhagavatam/app/ui/components/SampleSizeTest.kt`

**Interfaces:**
- Produces: `fun sampleSizeFor(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int` (largest power of two that keeps both sides at or above the request; 1 when the request is 0) and `@Composable fun ArtImage(resId: Int, modifier: Modifier, contentScale: ContentScale = ContentScale.Crop)`.

- [ ] **Step 1: Failing test.** `sampleSizeFor(1920, 1200, 960, 600)` is 2; `sampleSizeFor(1920, 1200, 400, 250)` is 4; `sampleSizeFor(1920, 1200, 1920, 1200)` is 1; `sampleSizeFor(1920, 1200, 0, 0)` is 1.
- [ ] **Step 2:** Run, expect FAIL.
- [ ] **Step 3:** Implement `ArtImage`: `BoxWithConstraints` gives the size in pixels; `produceState<ImageBitmap?>(null, resId, w, h)` decodes on `Dispatchers.IO` with `inJustDecodeBounds` then `inSampleSize`, `inPreferredConfig = RGB_565` (paintings have no alpha); one `LruCache<String, ImageBitmap>` sized to 1/8 of `Runtime.maxMemory()` keyed by `"$resId@$w"`; until ready, nothing is drawn (the gradient under it shows). Any decode error leaves the gradient.
- [ ] **Step 4:** Replace the five `painterResource(<painting>)` calls with `ArtImage`. Tests pass.
- [ ] **Step 5:** Install, open Home and scroll the stories; `dumpsys meminfo` Native Heap well under the 136 MB baseline (record it). Commit.

### Task 6: Ship paintings at 1280 x 800

**Files:**
- Modify: `tools/art/import_art.py` (`W, H = 1280, 800`, `quality=78`), `content/art/IMAGE_SPECS.md` (state what the importer writes)
- Re-encode: the 13 `app/src/main/res/drawable-nodpi/*.webp` at 1920 x 1200 (from `content/art/raw` sources when present, else from the WebP itself)

- [ ] **Step 1:** Change the importer; re-encode the 13 files.
- [ ] **Step 2:** Check: every painting is 1280 x 800, the folder shrinks (record before and after), the images still look right on Home and the reader (screenshot).
- [ ] **Step 3:** Commit.

### Task 7: Start-up profile for the app's code

**Files:**
- Create: `app/src/main/baseline-prof.txt`

- [ ] **Step 1:** Rules: `HSPLcom/bhagavatam/app/**->**(**)**` and `Lcom/bhagavatam/app/**;` (the app's own classes, compiled ahead of time at install by profileinstaller).
- [ ] **Step 2:** Release build; check the APK contains `assets/dexopt/baseline.prof` (`unzip -l`). Install, force compilation as on a store install (`adb shell cmd package compile -f -m speed-profile com.bhagavatam.app`), run `perf_check.py --cold 3`, record.
- [ ] **Step 3:** Commit.

### Task 8: Chapters open and turn without a pause

**Files:**
- Modify: `ui/screens/Reader.kt` (line 140), `data/SampleData.kt`
- Test: `app/src/test/java/com/bhagavatam/app/data/NeighbourTest.kt` only if `neighbour` changes (it should not)

**Interfaces:**
- Consumes: `SampleData.versesFor(s, a)` (cache is a `ConcurrentHashMap`; `readVerses` is `@Synchronized`), `SampleData.neighbour(s, a, ±1)`.
- Produces: `fun SampleData.cachedVerses(s: Int, a: Int): List<Verse>?` (cache only, no read).

- [ ] **Step 1:** Reader: `val verses by produceState(initialValue = SampleData.cachedVerses(skandha, adhyaya) ?: emptyList(), skandha, adhyaya) { value = withContext(Dispatchers.IO) { SampleData.versesFor(skandha, adhyaya) } }`. While the list is empty the screen shows its top bar and title only (no "empty chapter" flash: anything that reads `verses.isEmpty()` to show a message waits until the read finished, via a `loaded` flag).
- [ ] **Step 2:** After the chapter is shown, `LaunchedEffect(skandha, adhyaya) { withContext(Dispatchers.IO) { neighbour(+1) and neighbour(-1) -> versesFor } }`.
- [ ] **Step 3:** Check by hand: open a chapter, swipe five chapters forward quickly and back; the right chapter is always shown, the resume position still works, Listen still plays the shown chapter. Unit tests pass. Commit.

### Task 9: Stable data for Compose

**Files:**
- Modify: `data/SampleData.kt` (`Verse`), `data/Episodes.kt` (`Episode`), `data/Dictionary.kt` (`DictEntry`, `DictSense`, `DictSource`)

- [ ] **Step 1:** Mark the five classes `@androidx.compose.runtime.Immutable` (they are never changed after creation; check no `var` fields).
- [ ] **Step 2:** Tests pass; commit.

### Task 10: Dictionary prefix search on the index

**Files:**
- Modify: `data/Dictionary.kt` (`search`)
- Test: `app/src/test/java/com/bhagavatam/app/data/PrefixRangeTest.kt`

**Interfaces:**
- Produces: `fun prefixUpper(prefix: String): String` (the prefix with its last char raised by one; `"कृ"` -> `"कॄ"`, `"kri"` -> `"krj"`).

- [ ] **Step 1: Failing test** for `prefixUpper` on a Roman and a Devanagari prefix.
- [ ] **Step 2:** Run, expect FAIL.
- [ ] **Step 3:** `search` uses `norm >= ? AND norm < ?` (prefix, prefixUpper) with `lang = ?` when a language is chosen, and one such query per language joined with `UNION ALL` for "all"; headword matches only when `norm` misses (keep the result order rules). Verify in Python on the asset that `EXPLAIN QUERY PLAN` shows the index range and the time per query falls.
- [ ] **Step 4:** Tests pass; commit.

### Task 11: Three frictions

**Files:**
- Modify: `ui/screens/Library.kt` (Content screen: a "Go to verse" field), `state/AppState.kt:349`
- Test: reuse `SearchQueryTest` for `SearchQuery.parseReference` (already covers "10.29.1")

- [ ] **Step 1:** Content screen top: a single-line field "Go to 10.29.1" (four languages; digits in the app language allowed: `SearchQuery.parseReference` after `asciiDigits`); Enter opens the chapter at that verse through the existing open-chapter callback; an unknown reference shows the existing "not found" toast.
- [ ] **Step 2:** New installs start with no bookmarks: the default list `listOf("1.3.28", "10.29.1", "1.1.3")` becomes empty only when `prefs` has never stored "bookmarks" AND "onboarded" is false (a fresh install); an existing install keeps what it has.
- [ ] **Step 3:** Check on the emulator after a fresh install: Home shows no "Saved 3"; the field opens 10.29.1. Commit.

### Task 12: Measure again and report

**Files:**
- Modify: `docs/audits/PERF_2026-10-18.md`

- [ ] **Step 1:** Release build; `perf_check.py --install <apk> --first` and `--cold 3`; record the "After" column (first launch, cold start, Home visible, native heap, APK size, painting folder size).
- [ ] **Step 2:** Full unit test run; language matrix spot check (EN and OR app language, Home and reader).
- [ ] **Step 3:** Commit.
