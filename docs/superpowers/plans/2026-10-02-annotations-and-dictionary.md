# Highlights, notes, bookmarks and tap-a-word dictionary: research and plan

> For agentic workers: use superpowers:subagent-driven-development or superpowers:executing-plans, task by task. Steps use `- [ ]`.

**Goal:** Let the reader highlight any span of text, attach a personal note, bookmark specific parts of a chapter, and tap any word for a meaning (offline first, online optional).

**Architecture:** One `annotation` table (SQLite via the Room already usable in the project) anchored to a verse plus the exact quoted text. Rendering uses `AnnotatedString` spans over the existing reader `Text` blocks. Word taps use text-layout hit testing. Meanings come from a small offline SQLite dictionary built at build time from the app's own vocabulary, with an opt-in online fallback and a hand-off to the system "look up" as the zero-licence option.

**Tech stack:** Kotlin, Jetpack Compose, SQLite/Room, FTS5 (already used for search), Python build scripts in `content/_build`.

## What exists today (checked in the code)

- `AppState.bookmarks` is a list of verse refs ("1.3.28"); `AppState.highlights` is a map ref -> colour. Both are stored as newline-joined strings in SharedPreferences (`AppState.kt` around line 178).
- A long press on a verse block toggles the bookmark (`Reader.kt`, `VerseBlock`). Highlights have no UI that creates them, and cover a whole verse only.
- Translation text is drawn as plain `Text` (`VerseBlock`, `BookParagraph`), so there is nothing to tap per word yet.
- `Me.kt` already lists saved verses with swipe-to-remove, and a Glossary screen exists.

## Research findings

### Annotations
- A span highlight needs the character range inside the text as displayed. The app rewrites dashes at load (`tidyDashes`) and the Bengali text can be replaced by a later content update, so a bare offset will drift. Store verse ref, language layer, start, end AND the quoted string. On load, re-find the quote near the stored offset; if it is gone, keep the note and mark the highlight "text changed".
- Compose gives two ways in: wrap text in `SelectionContainer` and replace the floating toolbar (`LocalTextToolbar`) with one that has Highlight, Note, Define; or run your own tap and long-press handling on the text layout (`onTextLayout` plus `getOffsetForPosition`). The first gives familiar drag handles for free. The second is needed anyway for single-word taps. Use both: tap = word, long-press-and-drag = selection.
- "Bookmark specific parts" is the same record with a kind: a bookmark with no range marks a verse; one with a range marks a passage. Keep one table so the Saved screen can list all three kinds together.

### Dictionary
Checked against the source pages on 2026-10-02 (web search was down, so primary pages only):
- **Wiktionary extracts (kaikki.org).** Machine-readable Wiktionary in JSONL. The full English dump is 23.9 GB raw (2.8 GB compressed), so it cannot ship; the method below extracts only the words the app contains. Wiktionary text is community-licensed with share-alike terms; I could not read the licence page from the fetch, so confirm the exact licence and the attribution it needs before shipping.
- **Cologne Digital Sanskrit Dictionaries.** Hosts Monier-Williams, Apte (1890 and the revised 1957 edition), Macdonell and many others, with inflected-form data. The page asks users to acknowledge the data in an app. I could not find a licence statement in what I fetched, so the licence for bundling must be confirmed with the maintainers.
- **dictionaryapi.dev (online, English).** Free, no key. A test call for "dharma" returned definitions, phonetics and audio with per-item licences. It is an unofficial community service with no stated uptime promise, so treat it as a best-effort fallback only.
- **WordNet.** Princeton's licence page did not return readable text in the fetch. Check the licence directly before relying on it.
- **Google.** There is no free official dictionary API. The legitimate Google route is the system hand-off: an `ACTION_PROCESS_TEXT` or web-search intent opens whatever dictionary app the user already has, with no licensing on our side.
- **"Use in a sentence."** Best offline answer needs no new data: show the same word in other verses of the book. The app already has full-text search, so a concordance is a query, not a dataset. Wiktionary examples can add short sentences where they exist.

### The hard part: Sanskrit
A tapped Sanskrit word is an inflected, sandhi-joined form, not a headword. Plain lookup misses most words. Options, in order of effort: (1) strip common endings and try the stem; (2) use the inflected-form tables from the Cologne data to map form -> headword; (3) run a sandhi splitter at build time on every shloka and store the word-by-word split in the database, so runtime is a simple lookup. Option 3 is the right one for a closed text: the book is fixed, so do the analysis once, offline, and ship the result.

## Global constraints

- All text shown to people uses plain hyphens, never em dashes (project rule).
- Offline first: nothing requires the network; online lookups are opt-in and say what word is sent.
- No new heavy dependency without a reason; reuse the existing theme tokens (`Brand`, `LocalReaderColors`, `Radius`, `Motion`).
- Three UI languages (EN, HI, BN) for every new string, via `SettingsText.kt` / `Strings.kt`.
- Dictionary data shipped must have its licence confirmed and attribution added to an About screen.

## Phase 1: Data model (annotations)

### Task 1: Annotation store

**Files:** create `app/src/main/java/com/bhagavatam/app/data/Annotations.kt`; modify `state/AppState.kt`.

**Interfaces:** produces `data class Annotation(id: Long, ref: String, layer: Lang, start: Int, end: Int, quote: String, kind: Kind, colour: Long, note: String, created: Long)` and `enum Kind { BOOKMARK, HIGHLIGHT, NOTE }`; `AppState.annotationsFor(ref)`, `add`, `update`, `remove`.

- [ ] Write a JVM test: re-anchoring finds the quote after the text is shifted by 3 characters, and reports "changed" when the quote is gone.
- [ ] Implement a SQLite table and the re-anchor function.
- [ ] One-time migration: existing `bookmarks` become BOOKMARK rows with start = end = -1; existing `highlights` become whole-verse HIGHLIGHT rows. Keep the old keys until the migration has run once.
- [ ] Run the tests; commit.

## Phase 2: Reader interaction

### Task 2: Render highlights and note markers

**Files:** modify `ui/screens/Reader.kt` (`VerseBlock`, `BookParagraph`).

- [ ] Build the `AnnotatedString` with a `SpanStyle(background = colour.copy(alpha))` per highlight; a small note dot after notes.
- [ ] Check contrast of each highlight colour against the text in all four themes (the project has contrast scripts in the scratchpad).
- [ ] Commit.

### Task 3: Select, highlight, note

**Files:** create `ui/components/AnnotationSheet.kt`; modify `Reader.kt`.

- [ ] Custom text toolbar on selection: Highlight (colour row), Note, Define, Copy.
- [ ] Note sheet: a text field, saved on dismiss, with delete.
- [ ] Tapping an existing highlight opens the same sheet pre-filled.
- [ ] Commit.

### Task 4: Saved screen

**Files:** modify `ui/screens/Me.kt`.

- [ ] List all three kinds with the quoted text, the note, the reference and the date; filter chips; swipe to delete; tap jumps to the verse and scrolls the range into view.
- [ ] Export notes as plain text (share sheet).
- [ ] Commit.

## Phase 3: Tap a word

### Task 5: Word hit-testing

**Files:** create `ui/components/TappableText.kt`.

- [ ] A composable around `BasicText` that reports the tapped word (Unicode word boundaries; for Devanagari and Bengali use `BreakIterator` so combining marks stay attached) and its bounds.
- [ ] Keep long-press for selection and bookmark working; test with TalkBack on.
- [ ] Commit.

### Task 6: Meaning sheet (offline)

**Files:** create `data/Dictionary.kt`, `ui/screens/WordSheet.kt`.

- [ ] Bottom sheet: word, script variants, meaning(s), "In this book" (a concordance of other verses containing the word, from the existing search index), "Look up elsewhere" (system hand-off), and a "Save word" action that adds to the glossary.
- [ ] Commit.

## Phase 4: Dictionary data (build time)

### Task 7: Vocabulary extraction

**Files:** create `content/_build/make_dictionary.py`; output `app/src/main/assets/dictionary.db` (kept out of git like the other text data).

- [ ] Tokenise every shloka and translation in the database; collect the unique words per language.
- [ ] For English, Hindi, Bengali: look each word up in the Wiktionary extract and keep only hits; store headword, part of speech, short gloss, one example.
- [ ] For Sanskrit: run a sandhi splitter on each shloka once; store verse -> list of (surface form, headword, gloss) from the Sanskrit dictionaries. Measure the share of shloka words resolved; report the misses instead of hiding them.
- [ ] Add a size budget (target under 15 MB compressed) and a test that fails if exceeded.
- [ ] Commit the script; commit an attribution file listing each data source and licence.

### Task 8: Online fallback (opt-in)

**Files:** modify `data/Dictionary.kt`, `ui/screens/Settings.kt`.

- [ ] Setting "Look up unknown words online" (default off). When on and offline lookup misses, call the free dictionary API for English words only, cache the result in the database, show "source: online".
- [ ] Never send anything but the single word; handle timeouts and no network quietly.
- [ ] Commit.

## Phase 5: Verification

- [ ] Device check in all four themes and at font scale 1.3: highlight, note, delete, restart the app, confirm everything persists.
- [ ] Update a verse's text in a test database and confirm highlights re-anchor or are marked changed.
- [ ] Record the Sanskrit coverage figure from Task 7 in the About screen text.

## Open questions for the owner

1. Licence confirmation for the Sanskrit dictionary data and the Wiktionary extract (needed before shipping beyond personal use).
2. Whether the Sanskrit word-by-word split is worth the build effort now, or whether English/Hindi/Bengali word taps come first.
3. Whether notes should also sync anywhere (default: local only).
