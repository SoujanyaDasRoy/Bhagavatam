# Study features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the reader six study features that help a person understand and remember the text: a notebook with export, spaced review of marked verses and looked-up words, a speaker label, reading progress, and a translation switch that keeps the place.

**Architecture:** Everything stays on the phone. New data goes into `annotations.db` with `CREATE TABLE IF NOT EXISTS` (the pattern `SavedWordStore` already uses), small settings into `SharedPreferences` through `AppState.save {}`. Logic that can be wrong (the review schedule, the speaker carry-forward, the export text, progress arithmetic) lives in plain Kotlin functions with unit tests; screens only call them.

**Tech Stack:** Kotlin, Jetpack Compose, SQLite (`AnnotationStore`), JUnit 4 (`app/src/test`).

**Spec:** the six ideas in the chat of 2026-10-18 ("More E Reader study features"), `docs/research/STUDY_AND_SHARING_FEATURES.md`, and the verse sheet already built on branch `feature/verse-actions` (`VerseSheet.kt`, `AppState.toast`, `AppState.verseSheet`).

## Global Constraints
- Works 100% offline, no account, no network call, nothing leaves the phone unless the user taps share (CLAUDE.md section 5.4).
- Touch targets at least 48 dp with 8 dp between neighbours; Indic text at least 15 sp, line height 1.8; no text under 12 sp (`ContrastTest.noTinyText`).
- Every label goes through `tr(ui, en, hi, bn)`; no hard-coded English in a `contentDescription`.
- No em or en dashes in any text shown to the reader (CLAUDE.md section 5.2). No co-author lines in commits.
- Verse references are `"skandha.adhyaya.num"` strings (`Verse.ref`); a verse that covers a range is found with `num..numEnd`.
- A backup file (`UserBackup`) must carry everything this plan adds, and restoring a backup made before this plan must still work.
- `content.db` is not changed by this plan.

## Facts that shaped the plan
- **Speaker data is sparse but usable.** 1,010 of 14,580 verses carry a speaker line ("श्रीशुक उवाच" 370, "मैत्रेय उवाच" 107, "सूत उवाच" 94, "नारद उवाच" 62). A speaker applies to the verses after it until the next speaker line, so the label has to be carried forward inside the chapter.
- **Only 17 curated stories exist** (`Episodes.kt`), not about 60. A story map would need new hand-written entries, so it is dropped from this plan. The 341 chapter titles already work as a table of contents.
- **`AppState.recent` is recent searches**, not recently read chapters, so "continue reading" needs its own list.
- **Saved screen** (`Me.kt` `SavedScreen`) already lists bookmarks and marks with an All / Bookmarks / Highlights filter; the notebook extends it, it does not start over.
- Chapter progress today is `finished` (a set of "s.a") and one position (`lastSkandha/lastAdhyaya/lastVerse`).

## Review Focus
1. **Empty and huge notebooks:** a reader with 0 marks sees a clear empty state; one with 2,000 marks scrolls without a stall (lazy list, no per-row database call).
2. **Export text with Indic digits, quotes and very long notes:** one file, verse references intact, no truncated note.
3. **Review due dates when the clock changes:** moving the phone clock back or across a time zone must not make everything due or hide a due card for years.
4. **Backup round trip:** export, wipe, import restores marks, saved words, review cards and colour names; importing an old backup with none of them does not fail.
5. **Speaker carry-forward at a chapter start and across joint verses:** the first verses of a chapter before any speaker line show no label (not the last chapter's speaker), and a joint verse "5-6" labels once.
6. **Odia and joint verses in review cards:** a card for a verse whose translation is printed with another verse shows the carrier's text, never an empty card.

---

## File Structure
| File | Responsibility |
|---|---|
| `app/.../data/NotebookText.kt` (new) | pure `notebookText(...)` for export, grouping helpers |
| `app/.../ui/screens/Notebook.kt` (new) | notebook screen: grouped marks, colour filter, search, export |
| `app/.../data/ReviewStore.kt` (new) | table `review`, pure `ReviewScheduler`, `ReviewCard` |
| `app/.../ui/screens/Review.kt` (new) | review session screen (verses and words) |
| `app/.../data/Speakers.kt` (new) | pure `speakerLabels(verses)` |
| `app/.../data/Progress.kt` (new) | pure chapter progress and time-left arithmetic |
| `app/.../state/AppState.kt` | colour names, review store, recent chapters, progress accessors |
| `app/.../data/UserBackup.kt` | include the new data |
| `app/.../ui/screens/Reader.kt`, `VerseSheet.kt`, `Library.kt`, `Me.kt`, `Home` in `Library.kt` | entry points and labels |
| `app/src/test/.../data/*Test.kt` | one test file per new pure file |

---

### Task 1: Notebook screen and export
**Files:**
- Create: `app/src/main/java/com/bhagavatam/app/data/NotebookText.kt`, `app/src/main/java/com/bhagavatam/app/ui/screens/Notebook.kt`
- Modify: `app/.../state/AppState.kt` (colour names), `app/.../ui/AppNav.kt` (route `Routes.NOTEBOOK`, replace the `SAVED` destination's content), `app/.../ui/screens/SettingsWords.kt` (rename colours)
- Test: `app/src/test/java/com/bhagavatam/app/data/NotebookTextTest.kt`

**Interfaces:**
- Produces: `data class NotebookEntry(val ref: String, val kind: AnnKind, val colour: Long, val quote: String, val note: String)`; `fun notebookGroups(entries: List<NotebookEntry>): List<Pair<String, List<NotebookEntry>>>` (key `"s.a"`, chapters in book order, entries in verse order); `fun notebookText(entries: List<NotebookEntry>, colourNames: List<String>, chapterTitle: (Int, Int) -> String): String`.
- Produces in `AppState`: `var markNames: List<String>` (four entries, defaults Yellow, Green, Blue, Pink in the app language) and `fun updateMarkName(i: Int, name: String)`.

- [ ] **Step 1: Write failing tests** in `NotebookTextTest`: `groupsAreInBookOrder` (entries 10.29.1, 1.2.3, 1.2.1 give keys `1.2` then `10.29`, and `1.2.1` before `1.2.3`); `exportKeepsLongNoteAndIndicText` (a 3,000 character Odia note appears whole in the output); `exportNamesTheColour` (a pink entry prints the fourth colour name); `emptyGivesEmptyString`.
- [ ] **Step 2: Run** `.\gradlew.bat testDebugUnitTest --tests "*NotebookTextTest"`. Expected: FAIL, unresolved reference.
- [ ] **Step 3: Implement** the two functions in `NotebookText.kt`. Output layout per chapter: a heading line `Skandha 10 · Adhyaya 29 - <title>`, then per entry `ref`, colour name, the quote, the note on its own line; blank line between entries. Use a plain hyphen, never a dash.
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Implement `Notebook.kt`:** `fun NotebookScreen(state: AppState, onBack: () -> Unit, onOpen: (Int, Int) -> Unit)`. A search box (filters quote and note, case-insensitive), colour chips (All plus the four named colours, 48 dp), the entries grouped by chapter under sticky chapter headings using `LazyColumn`, tapping an entry opens its chapter and sets `state.scrollToVerse`, a top-bar Share button sending `notebookText` through `ACTION_SEND`, and the existing bookmarked verses and saved words kept as sections below. Keep `SavedScreen`'s swipe-to-remove for bookmarked verses.
- [ ] **Step 6: Wire** the route and the Me screen's "Saved" row to `NotebookScreen`; add "Colour names" editing to `WordsSettings` (four text fields, `AppState.updateMarkName`); show the names in `SelectionBar` and `VerseSheet` content descriptions instead of the fixed colour words.
- [ ] **Step 7: Verify on the emulator:** create three highlights of different colours in two chapters; the notebook groups them; the colour chip filters; Share opens the chooser with the text; empty state shows with a fresh install.
- [ ] **Step 8: Commit** `notebook: grouped marks, colour filter, search, text export, named colours`.

### Task 2: Review store and scheduler
**Files:**
- Create: `app/.../data/ReviewStore.kt`
- Modify: `app/.../state/AppState.kt`, `app/.../data/UserBackup.kt`
- Test: `app/src/test/java/com/bhagavatam/app/data/ReviewSchedulerTest.kt`

**Interfaces:**
- Produces: `object ReviewScheduler { val steps: List<Int> = listOf(1, 3, 7, 21); fun after(stage: Int, remembered: Boolean): Int; fun dueAt(now: Long, stage: Int): Long; fun isDue(now: Long, dueAt: Long): Boolean }` where `after` returns the next stage (0..steps.size; `steps.size` means learned; a failed card returns 0).
- Produces: `data class ReviewCard(val id: Long, val kind: String, val key: String, val stage: Int, val dueAt: Long, val created: Long)` with `kind` `"verse"` or `"word"` and `key` the verse ref, or `"<lang>|<word>"`.
- Produces: `class ReviewStore(helper: AnnotationStore)` with `fun all(): List<ReviewCard>`, `fun add(kind: String, key: String, now: Long): Long`, `fun update(card: ReviewCard)`, `fun remove(id: Long)`; table `review` created with `CREATE TABLE IF NOT EXISTS`.
- Produces in `AppState`: `val reviewCards: SnapshotStateList<ReviewCard>`, `fun addReview(kind, key)`, `fun answerReview(card: ReviewCard, remembered: Boolean)`, `fun dueReviewCount(now: Long = System.currentTimeMillis()): Int`.

- [ ] **Step 1: Write failing tests** in `ReviewSchedulerTest`: `rememberedMovesToNextStep` (0 to 1 to 2 to 3 to 4); `failedGoesBackToStart` (stage 3, not remembered, returns 0); `learnedStaysLearned` (stage 4, remembered, returns 4); `dueAtAddsTheStepInDays` (stage 1 adds 3 days); `clockMovedBackDoesNotMakeEverythingDue` (a card due in 3 days is not due when `now` is 2 days earlier than when it was created, and is due once `now >= dueAt`); `neverDueMoreThanTwentyOneDaysAhead` (a `dueAt` further than 22 days from `now` is treated as due, so a wrong clock cannot hide a card for years).
- [ ] **Step 2: Run** `--tests "*ReviewSchedulerTest"`. Expected: FAIL.
- [ ] **Step 3: Implement** `ReviewScheduler` as pure functions (days to millis constant `86_400_000L`).
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Implement `ReviewStore`** and the `AppState` members; load `reviewCards` at start like `savedWords`.
- [ ] **Step 6: Extend `UserBackup`** export and import with the `review` rows; an import file without them leaves the table as is.
- [ ] **Step 7: Commit** `review: store, schedule 1-3-7-21 days, backup`.

### Task 3: Review screen, "Memorise" action and word cards
**Files:**
- Create: `app/.../ui/screens/Review.kt`
- Modify: `app/.../ui/screens/VerseSheet.kt` (action "Memorise this verse"), `app/.../ui/screens/AnnotationSheet.kt` (`MeaningCard`: action "Add to review"), `app/.../ui/AppNav.kt` (route `Routes.REVIEW`), `app/.../ui/screens/Me.kt` and the Home screen in `Library.kt` (entry with due count)
- Test: `app/src/test/java/com/bhagavatam/app/data/ReviewCardTextTest.kt`

**Interfaces:**
- Consumes: Task 2's `ReviewCard`, `AppState.addReview`, `answerReview`, `dueReviewCount`.
- Produces: `fun reviewPrompt(card: ReviewCard, state: AppState): String` and `fun reviewAnswer(card: ReviewCard, state: AppState): String` (pure given a verse lookup and dictionary lookup passed in; signature in the implementation takes lambdas `verseOf: (String) -> Verse?` and `meaningOf: (String, Lang) -> String?` so they can be tested).

- [ ] **Step 1: Write failing tests** in `ReviewCardTextTest`: `verseCardPromptIsFirstSanskritLine`; `verseCardAnswerIsTheTranslationInReadingLanguage`; `jointVerseCardUsesTheCarriersText` (a verse whose own translation is empty gives the carrier's text, never an empty string); `wordCardPromptIsTheWordAndAnswerItsFirstMeaning`; `missingVerseGivesNoCard` (returns null so the screen can drop the card).
- [ ] **Step 2: Run** `--tests "*ReviewCardTextTest"`. Expected: FAIL.
- [ ] **Step 3: Implement** `reviewPrompt` and `reviewAnswer` in `Review.kt` (top level, no Compose) using the lambdas.
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Build `ReviewScreen`:** one card at a time: the prompt, a "Show" button, then "Remembered" and "Again" (48 dp, 8 dp apart); a counter "3 of 7 due"; an end state "Nothing due. Next review in N days" computed from the earliest `dueAt`; a "Remove from review" overflow action. No streaks, no points.
- [ ] **Step 6: Add the entry points:** the verse sheet's "Memorise this verse" (toast "Added to review"), the meaning card's "Add to review", a Home row "Review (N due)" shown only when N is above 0, and the same row inside Me.
- [ ] **Step 7: Verify on the emulator:** add a verse and a word, force them due by changing the stored `dueAt` with a debug SQL update, run through "Remembered" and "Again", confirm the stage and due date change.
- [ ] **Step 8: Commit** `review: session screen, memorise a verse, review a looked-up word`.

### Task 4: Speaker label
**Files:**
- Create: `app/.../data/Speakers.kt`
- Modify: `app/.../ui/screens/Reader.kt` (`VerseBlock`, `BookParagraph`, reader settings sheet)
- Test: `app/src/test/java/com/bhagavatam/app/data/SpeakersTest.kt`

**Interfaces:**
- Produces: `fun speakerLabels(verses: List<Verse>): Map<String, String?>` mapping `Verse.ref` to the speaker in force at that verse, or null before the chapter's first speaker line; a verse that carries its own `speaker` starts a new run.
- Produces in `AppState`: `var showSpeakers: Boolean` (default true) with `updateShowSpeakers`.

- [ ] **Step 1: Write failing tests** in `SpeakersTest`: `speakerCarriesForward` (speaker on verse 1, none on 2 and 3, all three labelled the same); `newSpeakerStartsNewRun`; `beforeFirstSpeakerIsNull`; `chapterStartDoesNotInheritFromPreviousChapter` (calling with a second chapter's verses begins with null); `jointVerseLabelsOnce` (a verse `5-6` has one entry).
- [ ] **Step 2: Run** `--tests "*SpeakersTest"`. Expected: FAIL.
- [ ] **Step 3: Implement** `speakerLabels` with a single pass over the verses.
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Show the label** as a small gold line (15 sp) at the start of each run only (when the label differs from the previous verse's) in both reading modes; add a "Show who is speaking" switch to the reader settings sheet.
- [ ] **Step 6: Verify** on 1.1 (Shaunaka and Suta) and 10.29 (Shukadeva) in English and Odia; the label follows the app language through `tr` where the name has a translation, else shows the Devanagari line.
- [ ] **Step 7: Commit** `reader: show who is speaking`.

### Task 5: Reading progress, time left and continue list
**Files:**
- Create: `app/.../data/Progress.kt`
- Modify: `app/.../state/AppState.kt` (recent chapters), `app/.../ui/screens/Library.kt` (Home continue list, Skandha tick and count on the Content page), `app/.../ui/screens/Reader.kt` (time left in the header)
- Test: `app/src/test/java/com/bhagavatam/app/data/ProgressTest.kt`

**Interfaces:**
- Produces: `fun minutesLeft(wordsAfter: Int, wordsPerMinute: Int = 150): Int` (never below 1 while words remain, 0 when none); `fun skandhaProgress(finished: Set<String>, chaptersIn: Int, skandha: Int): Pair<Int, Int>` (done, total); `fun pushRecent(list: List<String>, entry: String, max: Int = 5): List<String>` (most recent first, no duplicates).
- Produces in `AppState`: `val recentChapters: SnapshotStateList<String>` (entries `"s.a"`), updated by `markRead`.

- [ ] **Step 1: Write failing tests** in `ProgressTest`: `minutesLeftRoundsUpAndNeverZeroWhileWordsRemain` (1 word gives 1; 0 words gives 0; 301 words gives 3); `skandhaProgressCountsOnlyThatSkandha`; `pushRecentKeepsFiveMostRecent`; `pushRecentMovesRepeatToFront`.
- [ ] **Step 2: Run** `--tests "*ProgressTest"`. Expected: FAIL.
- [ ] **Step 3: Implement** the three functions.
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Wire:** `markRead` calls `pushRecent` and saves the list; Home shows "Continue" with up to five chapters (title, verse reached); Content shows "7 of 19 chapters read" under each Skandha with a tick when complete; the reader header shows "about N min left" computed from the words after the first visible verse, hidden when `showReadTime` is off.
- [ ] **Step 6: Verify** by opening three chapters and checking the list order and that a finished Skandha shows its tick.
- [ ] **Step 7: Commit** `progress: recent chapters, per-Skandha count, minutes left`.

### Task 6: Switch the reading language without losing the place
**Files:**
- Modify: `app/.../ui/screens/Reader.kt` (language pill / `LanguageSheet`), `app/.../state/AppState.kt`
- Test: `app/src/test/java/com/bhagavatam/app/state/ReadingPositionTest.kt`

**Interfaces:**
- Produces: `fun anchorVerseNum(firstVisibleIndex: Int, headItems: Int, rows: List<Int>): Int` returning the `num` of the verse at the top of the screen; used to scroll back to the same verse after the language, and so the row list, changes.

- [ ] **Step 1: Write failing tests** in `ReadingPositionTest`: `indexInsideHeaderGivesFirstVerse`; `indexMapsToItsVerse`; `pastTheEndGivesLastVerse`.
- [ ] **Step 2: Run** `--tests "*ReadingPositionTest"`. Expected: FAIL.
- [ ] **Step 3: Implement** `anchorVerseNum`.
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: In `ReaderScreen`,** record the anchor verse when the language sheet opens and, after `rowVerses` is rebuilt for the new language, scroll to the same verse with `list.scrollToItem`. Give the reader's language pill a long-press that cycles to the next available translation of this chapter, with a toast naming it.
- [ ] **Step 6: Verify** by scrolling to the middle of Skandha 10 chapter 29, switching Hindi to English to Bengali, and confirming the same verse stays at the top each time.
- [ ] **Step 7: Commit** `reader: keep the place when the reading language changes`.

### Task 7: Release
- [ ] **Step 1:** Run `.\gradlew.bat test assembleRelease`; expected: tests pass.
- [ ] **Step 2:** Run `py tools/content/check_alignment.py` to confirm no text changed; run the emulator pass for tasks 1 to 6 at font scale 1.0 and 2.0 and in Hindi and Bengali app language (`tools/qa/language_matrix.py`).
- [ ] **Step 3:** Bump `versionName` and `versionCode` in `app/build.gradle.kts`, update `README.md` Features, merge, publish the APK as `Bhagavatam.apk` with release notes listing the new features and noting that the Hindi and Bengali labels are not yet reviewed by a native reader.

## Left out on purpose
Streaks, points, push reminders, generated summaries, accounts and cloud sync (they break the privacy promise or add noise); a story map (only 17 stories exist); names and places index, focus ruler and chant repeat (good ideas, but need data work or a separate player change; plan them after this release).

## Self-review
- **Coverage:** notebook and export (Task 1), review of verses and words (Tasks 2 and 3), speaker label (4), progress and continue (5), keep the place when switching translation (6), release (7). The "words I looked up as cards" idea is Task 3's word cards; "compare translations" already exists in the verse sheet and Task 6 covers the switch.
- **Types:** `ReviewCard`, `ReviewScheduler`, `AppState.addReview/answerReview/dueReviewCount` are defined in Task 2 and consumed in Task 3 under the same names; `scrollToVerse` and `toast` come from the verse-actions branch and are used as they are there.
- **Review Focus:** each of the six lines has a test: 1 and 2 in Task 1, 3 in Task 2 (`clockMovedBack...`, `neverDueMoreThanTwentyOneDaysAhead`), 4 in Task 2 step 6 plus the Task 7 backup check, 5 in Task 4, 6 in Task 3.
- **Proportion:** the plan carries signatures, test names and decisions only; no function bodies.
