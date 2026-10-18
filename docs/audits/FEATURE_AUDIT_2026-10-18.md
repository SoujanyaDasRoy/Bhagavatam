# Feature audit, 2026-10-18

Build: debug, version 1.2.7 code base plus the unreleased work on branch `feature/verse-actions` (study features, Home and Content
redesign, Odia app language, text-quality checks). Device: Pixel_8 emulator, 1080 x 2400, Android 15, Google TTS installed.

## How it was checked
| Method | What it covered |
|---|---|
| Fresh install (`pm clear`), then the app driven through the screens with taps, screenshots and the accessibility tree | onboarding, Home, Content, chapter list, reader, verse sheet, word meaning card, note editor, mini player, search, Saved, Dictionary, every Settings screen |
| `tools/qa/lang_audit.py` (walks the tabs and every Settings row, lists text in the wrong script and unlabelled tap targets) | app language English, Hindi, Bengali and Odia, each with the same reading language |
| Unit tests | 168 tests in 28 suites, 0 failures (`gradlew :app:testDebugUnitTest`, clean run) |
| `logcat` over the whole session | no crash, no ANR |
| Data checks | both `content.db` copies identical (content_version 41), `search.db` copies identical and on the same version, `tools/qa/scan_text_quality.py` |
| Code reading | every feature below was traced to the file that implements it |

**Not exercised in this session** (honest list): the full player screen, sleep timer and speed while playing, lock-screen controls,
Bluetooth, the review session (only reached through code and its tests), backup and restore end to end, the update download, voice
search, dark themes at font scale 2.0, a physical phone. These were tested in earlier sessions (`APP_TEST_2026-10-13.md`,
`LANGUAGE_AUDIT_2026-10-14.md`, the study-features plan) but not again after the latest changes.

A note on the test count: an earlier report in this branch quoted 302 passing tests. A clean run now counts 168 `@Test` methods and
168 results. The earlier figure could not be reproduced and should be taken as wrong.

## Summary
| Area | Features | State | Open findings |
|---|---|---|---|
| First run | app language (4), reading language (5), translations alongside, preview verse, offline note | works | F3 |
| Home | resume card with play, shloka of the day, Saved and Dictionary tiles, recently read, progress line, painted story cards, Content row (current Skandha and the next two), update banner, light/dark switch | works | F6 |
| Content | Mahatmya banner, 12 painted Skandha cards, "Reading" ring on the current Skandha, chapter counts, progress only once started | works | |
| Chapter list | title, Sanskrit name, about line, counts, "Continue chapter N", per-chapter verse count, finished tick, "Reading" badge | works | F5 |
| Reader | 5 reading languages, Sanskrit in 3 scripts, Roman letters, translations under the shloka, speaker lines, verse numbers, chapter picture, minutes left, find in chapter, pinch to resize, swipe for next chapter, chapter-end block, hold the language pill to step through translations, place kept on language change | works | F4, F5, F8 |
| Verse actions (sheet) | bookmark, highlight in 4 colours, note, memorise, share, copy with reference, listen from here, other translations, report an error | works (bookmark and note editor exercised) | F8 |
| Hold and select | hold a word for its meaning, hold and drag to select, Sentence / All widen, colours, note, bookmark, copy | works (meaning card exercised) | F8 |
| Word meaning | offline dictionary, base word, More meanings, Save word, Add to review, occurrences in the book, online Wiktionary lookup | works | **F1** |
| Search | words in all languages, stories, references (10.29.1), spelling-tolerant keys, language and Skandha filters, recent searches, voice search | works ("Gajendra": 1 story, 25 verses in 4 languages) | F7 |
| Dictionary screen | prefix search, language filter, names and terms of the book, saved words, credits | works | |
| Notebook (Saved) | all / bookmarks / highlights, filter by colour, search, share as text, size guard | works | |
| Review | spaced review (1, 3, 7, 21 days) of verses and words, Home row when due | code and tests only | F8 |
| Listening | TTS narration with pauses and lookahead, Sanskrit through the Hindi voice, mini player, player, speed, sleep timer, play through chapter / Skandha / book, background and lock screen, follow along | narration starts and plays (media session PLAYING) | F2 |
| Settings | appearance (4 themes, follow phone), reading (size, spacing, layout switches), listening, words and notes (meaning language, online lookup, colours and their names, backup, share notes), languages, library, your data, updates, tips | every screen opens and is labelled | F1, F7 |
| Languages | app language EN, HI, BN, **OR (new)**; reading SA, EN, HI, BN, OR; the page follows the reading language, the frame the app language | no stray script found by the audit in any of the 4 app languages | F4, F9 |
| Backup | one JSON file: notes, highlights, saved words, review cards, bookmarks, position, settings; validated before anything is replaced, one transaction | tests only this session | |
| Updates | GitHub release check, notification, in-app banner and install | not exercised | |
| Content data | 341 chapters, 14,580 verses; Bengali 29 and Odia 65 verses without a translation; 7 Odia verses hidden as garbled | see the text-quality report | F4 |

## Findings
Severity: High = wrong behaviour or a broken promise; Medium = real friction; Low = polish.

**F1, High: word meanings go online by default.** `AppState.onlineMeanings` starts as `true` (also after "Reset settings"), so the
first time someone holds a word the app sends it to Wiktionary. The app says "Works fully offline. No sign-in." on the first screen
and the README says the dictionary is offline. Only the one word is sent, but it happens without being asked.
Fix: default `false`; when the offline dictionary has nothing, show "Look it up online?" with a one-time choice.

**F2, Medium: notifications are asked for twice.** The app's own dialog ("Allow notifications to control playback...") appears on
Home right after onboarding, before anything has been played; choosing "Not now" still leads to Android's own permission dialog
the first time Listen is pressed. Once, after denying it and closing the mini player, the app went to the background (launcher
shown, no crash in `logcat`); this could not be repeated. Fix: ask only when Listen is first pressed, once.

**F3, Low: first-run defaults.** Choosing English on step 1 leaves the reading language on Sanskrit with the English translation
under it, so the first chapter is "Shloka + translation". That is the intended choice for English, but the pill text "Shloka +
translation" is long and the step-2 screen does not say that it will be the default.

**F4, Medium: Odia text and titles.** 7 Odia verses are garbled and hidden (0.6.3, 0.6.30, 3.6.34, 3.12.44, 6.2.24, 9.23.6,
10.84.51); 20 have an Odia text far longer or shorter than the Hindi of the same verse (`TEXT_QUALITY_2026-10-18.md`).
About 130 chapters have no Odia title and show the English one. The Odia interface strings were written in this session and have
not been read by an Odia speaker. Odia uses the phone's serif font (`readingFont(Lang.OR) = FontFamily.Serif`): on the emulator the
conjunct in ଶ୍ରୀମଦ୍ଭାଗବତ is drawn as ଶ୍ରୀମଭାଗବତ. Fix: bundle Noto Serif Oriya and Noto Sans Oriya as the other scripts are.

**F5, Medium: English chapter titles carry print marks.** "śaunaka and other sages enquire of the famous Sūta* (Ugraśravā)": the
footnote star and the lower-case first letter come from the source and show on Home, the chapter list and the reader header.
Fix: strip a trailing `*` and capitalise the first letter when titles are loaded (`SampleData`, like `tidyDashes`).

**F6, Low: Home order on first run.** The new order (resume, shloka of the day, tiles, recently read, progress, stories, Content)
works; on a fresh install the progress line reads "0 of 341 chapters read" with an empty bar, which could be hidden until the first
chapter is finished.

**F7, Low: labels.** Search language chips say "Bangla" while the rest of the app says "Bengali", and the Odia chip is in Odia
script while the others are in English. The audit found clickable items with no label: 2 on Search, 5 in Settings (screen readers
announce them as "button").

**F8, Low: carried over from the study-features review, still open.** `[?]` doubt marks are copied into review answers and shared
text; the selection can stay highlighted when a drag is cancelled by a system gesture; the Home "Review (n due)" count counts every
due card while the Review screen drops cards whose verse or word can no longer be shown, so the two numbers can differ; the four
colour targets in the selection bar are 48dp with 2dp between them (`AnnotationSheet.kt`, the project rule asks for 8dp); "minutes
left" is estimated at 150 words a minute for every language.

**F9, Low: story descriptions** (search, story cards) exist in English, Hindi and Bengali only; Odia shows the English one.

## What works well
- The first launch to the first verse takes three taps and works offline.
- The verse sheet and hold-to-select put every study action one tap away, with labels in the app language.
- The rule "the page follows the reading language, the frame follows the app language" holds on every screen the audit walked,
  now including speaker lines and references.
- Search finds the same story across scripts and languages.
- Data is consistent: both databases identical, search index on the same version, no crash in the session.

## Order of work suggested
1. F1 (privacy promise), F2 (permission once, in context).
2. F5 and the Odia font in F4 (visible on every screen).
3. F7 labels, then F8 items, then F6 and F9.
4. Odia verses and titles in F4 need an Odia reader or the local OCR re-run; nobody types scripture from the page images.

## Update, same day: fixed after this audit
| Finding | What changed |
|---|---|
| F1 online meanings | Off by default and after "Reset settings". When the offline dictionary has nothing, the meaning card offers "Look it up online (sends this word to Wiktionary)" for that one word. |
| F2 notifications | The extra prompt on first Home is gone; Android's permission is asked once, when playback first starts. |
| F4 Odia font | Noto Serif Oriya (reading text, Settings picker) and Noto Sans Oriya (app interface in Odia) bundled in `res/font`; licence in `docs/licenses/NotoOriya-OFL.txt`. Checked on the emulator in Odia. |
| F5 English titles | Footnote stars removed and the first letter capitalised when titles load (`cleanEnglishTitle`, with tests). |
| F7 labels | Search chip "Bangla" is now "Bengali" in English. A second pass over Search and Settings with the accessibility tree found no unlabelled control except Android's own scroll indicator; the earlier count of 7 was not reproduced. Highlight colour buttons now have 8dp between them. |
Still open: F3, F6, F8 (rest), F9, the Odia verse and title data, the Odia interface review by an Odia reader.
