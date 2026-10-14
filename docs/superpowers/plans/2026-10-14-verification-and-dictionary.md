# Device checks, language matrix and dictionary coverage: plan

**Status:** plan only, nothing below is done. Written 2026-10-14 after the PR description listed these as "not run".

## 1. What is open
| # | Item | State |
|---|---|---|
| A | Backup and restore on a device | code written, never run |
| B | Telegram "report an error" button on a phone | code written, run only as a build check |
| C | Release APK (signed with the new key) on a physical phone | built and signature checked, never installed |
| D | 12-screen language matrix (3 app languages x 4 reading languages) | not run |
| E | `dictionary.db` against the latest text in all 4 languages | not measured until today, numbers below |

## 2. Dictionary: what is true today
`dictionary.db` is **not built from the book's text**. It is built from Wiktionary dumps (`content/_dict/build_dictionary.py`, built 2026-10-10, 37,797 entries: English 17,276, Bengali 11,149, Hindi 7,916, Odia 1,456). The book's text only decided which entries to keep (`coverage_probe.py`), and for Odia that was the **old Tesseract draft**, before the new text existed. So it has not been updated for the latest text in any language, and Odia is the weakest.

Measured now, exact forms (the app also strips endings at lookup, so real coverage is somewhat higher; that is not measured):

| Language | Distinct words in the book | Tokens covered | Distinct words covered |
|---|---|---|---|
| English | 17,483 | 95.9% | 83.7% |
| Bengali | 49,443 | 75.9% | 25.9% |
| Hindi | 36,679 | 72.6% | 23.2% |
| Odia | 43,314 | 29.6% | 3.2% |
| Sanskrit (shloka) | 73,169 | 6.5% | 1.1% |

Odia numbers include words that are misreads (the text is unreviewed), so some of the 97% uncovered are errors, not missing entries.

### Dictionary tasks
1. **Re-run the vocabulary probe on the current `content.db`** for hi, bn, en, or, sa (the probe still reads `content/or/*.md`; point it at the database, leave out `[?]` words). Output stays in `content/_dict/work/` (git-ignored).
2. **Measure with the app's own lookup** (forms plus the ending-stripping in `Lemmatizer.kt`), not exact forms, and write the real coverage per language in `docs/agent/REPORT.md`.
3. **Pick the target per language** with the owner. Proposal: Hindi, Bengali and English at least 90% of tokens; Odia as high as Wiktionary allows; Sanskrit by the Sanskrit dump (see 4).
4. **Fill gaps from sources that are allowed:** the Wiktionary dumps already fetched (check Sanskrit `sa` entries are fetched and parsed, since the shloka layer is the least covered and it is the layer people long-press most), plus Odia Wiktionary re-parse. No scraping of the Gita Press text or of other dictionaries with unclear licences. Record every new source in the `source` table and in the credits screen.
5. **Rebuild** with `build_dictionary.py`, run `tools/agent/dict_check.py` (it fails if FTS5 appears), bump `dictionary_version`, copy to `app/src/main/assets/`, bump `versionCode`.
6. **Size gate:** stay under the size already shipped (16.4 MB) plus an agreed margin, since it is bundled in the APK.

Exit test: for 200 random distinct words per language that occur at least twice in the book, a long-press shows an entry or a base-word line in at least the agreed share of cases, measured by a script, plus 20 spot checks by eye per language.

## 3. Device checks
All three need things only the owner has. What I will do and what I need:

### A. Backup and restore (phone or emulator)
1. Install the build, set unusual values: reading language Odia, a bookmark, a highlight, a note, a saved word, text size, theme.
2. Settings > Words & notes > Save a backup file, then check the JSON opens and lists the keys.
3. Clear the app data (or uninstall), reinstall, Restore, reopen.
4. Pass when position, settings, notes, highlights, bookmarks and saved words are all back and no crash. Also test a wrong file (a text file, an empty file) and see the "not a Bhagavatam backup" message.
5. Known risk to check: after restore the app kills its own process; make sure it reopens cleanly and does not show the onboarding again.

### B. Telegram button on a phone
Needs: a phone with Telegram installed, and the build with `telegram.user` set.
1. Long-press a Hindi word, tap the speech-bubble icon, confirm the clipboard holds `verse: word -> ` and the chat of the maker opens.
2. Tap a red-underlined Odia word, choose "No, send the correct word", same check.
3. Phone **without** Telegram: the link must open in the browser, not crash.
4. Build without `telegram.user`: the icon must be hidden and the Odia popup must show only "Yes, it is correct".

### C. Release APK on a physical phone
Needs: the phone, USB debugging on (or the APK file sent to it).
1. Confirm the signature with `apksigner verify --print-certs` (SHA-256 begins `CE:64:E1:26`).
2. Install over a debug build: expect "signature mismatch", which proves the key change. Uninstall, install fresh.
3. First run on a clean phone: onboarding, first chapter, search, dictionary, listening, Odia, backup.
4. Airplane mode for all of the above except the online meaning lookup.
5. Install the **next** release signed with the same key over it, and confirm notes and position survive.
6. Record the phone model and Android version in the test report.

## 4. D. The 12-screen language matrix
App language (English, Hindi, Bengali) x reading language (Sanskrit, English, Hindi, Bengali). Odia is a fifth reading language and is added: 3 x 5 = 15 combinations.
- **Method:** a script sets the app's preferences through `adb` (the same way the emulator test used `run-as`), opens a fixed chapter, and takes a screenshot of the reader, the Home screen, the chapter end and the language sheet in light and dark. 15 combinations x 4 screens x 2 themes = 120 screenshots, taken unattended.
- **What is checked on each:** no line mixes the two languages where the rule says one (reference line, reading time, digits, tips, chapter end), no missing glyph boxes, no text cut off at 1.3x font scale, nothing under 15 sp for Indic text.
- **Output:** a contact sheet per app language in `docs/agent/` and a short table of failures. Failures become fixes in `LangZone` use, not special cases.
- **Also in this task:** the fallback-language label (when a verse has no text in the reading language, say which language is shown), and the `DESIGN_SYSTEM.md` section for the rule.

## 5. Order and effort
1. Dictionary tasks 1 and 2 (measure with the real lookup): short, no owner input.
2. Language matrix script and screenshots: unattended after the script exists.
3. A and the emulator part of B and C (signature check, clean install, offline): I can do these.
4. Phone checks B and C on a physical phone: need the owner's phone for about 15 minutes.
5. Dictionary tasks 3 to 6 after the owner picks the targets.
6. Fix list from 2 to 4, rebuild, `versionCode` bump, one more full pass of A to C on the final APK.

## 6. Decisions needed from the owner
1. Dictionary coverage targets (suggested above), and whether Sanskrit shloka words need their own dictionary effort or are covered by the Hindi and English meanings.
2. Whether the Odia dictionary should wait for the Odia text to be reviewed (misreads would be dictionary "gaps" until then).
3. Which phone and how to connect it (USB debugging, or I give you the APK and a checklist).
4. Whether the matrix should include the Odia reading language now (recommended) or after review.
