# App language and reading language: one rule, written down

**Status:** decided and implemented 2026-10-14 (tasks 1 and 2; the 12-screen matrix of task 4 is not done). Written after the owner asked for "a rule for app language and reader language, which things are to be what".

## 1. The problem, seen in the app
The app has two separate language settings, as the owner decided: the **app language** (menus and labels) and the **reading language** (the text). Today each screen decides by itself which one to follow, so a Bengali reader with an English app sees mixed screens. Found in the code and in screenshots:

| Where | Follows | Looks like |
|---|---|---|
| Chapter title (reader, Home, Library, search results) | reading language | Bengali title |
| "Skandha 10 · Chapter 29", "48 shlokas · 19 min read" | app language | English line above a Bengali title |
| Verse number in front of each paragraph | app language digits | "13" next to Bengali text |
| Verse number at the end of the text (`॥ ১৩ ॥`) | the book's script digits | Bengali digits in the same paragraph |
| Chapter end block (closing line, buttons, notes) | reading language (changed 2026-10-13 at the owner's request) | Bengali buttons under English chrome |
| "Notes" heading at the chapter end | app language | English heading over Bengali notes |
| First-use tips | app language | English tip over a Bengali page |
| Search filters (English, Hindi, Bengali chips) | app language | consistent |
| Player title, now playing | reading language; buttons app language | consistent |
| Word meaning language | its own setting (the word's language, English or app language) | consistent |

There is no written rule, so every new feature guesses.

## 2. What other products do (research)
- Interface language and content language are separate axes in many products: interface follows a user or device preference, content has its own setting, and they may differ. (Feature requests and docs for Kagi News, Wiki.js, Drupal and an Oppia learning app say the same.)
- Common practice: default the content language from the device, let the person change it any time, keep the content language explicit and visible, and show a clear fallback when the text does not exist in the chosen language.
- Use the language's own name, not a flag.
- Sources are informal (product docs and feature requests), not a standard; platform guidance (Android per-app language, W3C internationalisation) was not read in this session.

## 3. Proposed rule
One sentence: **the page shows the language you read in; the frame around the page shows the language of the app.**

**Reading language (R)** governs everything that is the book or a name from the book:
1. The verse text, its Sanskrit script choice, and the audio language (unchanged).
2. Chapter and Skandha titles, story titles and their descriptions (unchanged).
3. The closing block of a chapter: the closing line, its translation, the notes and the previous/next chapter buttons with their small labels (done 2026-10-13).
4. Numbers that sit inside the reading text: verse numbers in front of and at the end of a paragraph, chapter and verse references printed with the book text. They use R's digits (Bengali digits for Bengali, Devanagari digits for Hindi and Sanskrit, ASCII for English).
5. The "Shloka 10.29.4" caption of a peeked shloka.
6. The title and artist text on the lock screen and notification.

**App language (A)** governs the frame:
1. Menus, tabs, Settings, buttons, dialogs, errors, tips, onboarding.
2. The reader's top bar controls and the reading-time line ("48 shlokas · 19 min read").
3. Search field, filters and the "no results" text; result snippets stay in the language that matched, labelled.
4. Numbers in the frame (counts, progress "0 / 341", timers) use A's digits.
5. Dictionary credits and licences.

**Own settings, never inferred:** the Sanskrit script (Devanagari, Bengali, Roman), the word-meaning language, the audio voice.

**Fallbacks (never silent):**
- R is Sanskrit (no translation language chosen): titles follow A (already so).
- A text does not exist in R for a chapter or verse (for example Odia, or a Bengali verse still pointing to a neighbour): show the next language in the order R, then A, then English, with a small label naming the language shown.
- A string has no translation in A: English, never a blank.

**Defaults on first run:** A comes from the phone (English, Hindi or Bengali; anything else is English). R starts as the same language as A, with the shloka shown, so the first screen is not mixed.

## 4. Open decisions for the owner
1. Reference line in the reader ("Skandha 10 · Chapter 29") and the reading time: keep in A (proposed) or move to R?
2. Digits in the reading text: R's digits (proposed, so a Bengali page shows Bengali digits everywhere) or A's digits?
3. First-run default for R: same as A (proposed) or Sanskrit with an English translation as today?
4. Should the mini player and lock-screen title follow R (proposed) or A?

## 5. Tasks
1. **One source of truth.** Add `LangZone` (`Frame` or `Page`) and a function `state.langFor(zone)` in `AppState`, and use it in place of loose `uiLang`/`titleLang` reads. Unit test the mapping, including Sanskrit and the fallbacks.
2. **Audit and fix** the rows of the table above that break the rule: verse-number digits (prefix and end), the chapter-end "Notes" heading, the shloka caption, the lock-screen text. Do not touch the frame.
3. **Fallback label.** Where text is shown in a fallback language, show a small label with that language's own name.
4. **Screenshot matrix.** App language (English, Hindi, Bengali) by reading language (Sanskrit, English, Hindi, Bengali): 12 reader screens, 12 Home screens, light and dark. Look for mixed lines.
5. **Document** the rule in `DESIGN_SYSTEM.md` and as one comment next to `LangZone`, so new features follow it.

Tasks 1 and 2 are the work; 3 to 5 make it stick. Odia fits the rule unchanged (R = Odia needs the font and the text; A stays English, Hindi or Bengali).

## 6. Owner decisions (2026-10-14) and what was built
| Decision | Answer | Built |
|---|---|---|
| Reference line ("Skandha 10 · Chapter 29") and reading time | reading language | words from the reading language's strings, in the reader and on the Home resume card |
| Digits in the reading text | app language | `digitsTo()` writes every digit in the translation in the app's digits (a 1-for-1 swap, text offsets unchanged) |
| First-run reading language | the reader's own language | choosing the app language on the first screen switches on that translation only; the second screen starts from it |
| Lock-screen and mini-player title | reading language | already so (`titleLang`) |

Also built: `LangZone { FRAME, PAGE }`, `zoneLang()` and `AppState.langFor(zone)` as the single place for the rule; the chapter-end "Notes" heading and the peeked shloka caption follow the reading language; unit tests for the zones and the digit rule.

Interpretation to confirm: "digits in the reading text: app language" was applied to the translation text of verses (including the number at the end of a paragraph). The Sanskrit shloka layer keeps its own printed digits.

Not done: the 12-screen screenshot matrix (3 app languages by 4 reading languages), the fallback label for text missing in the reading language, and a `DESIGN_SYSTEM.md` section.
