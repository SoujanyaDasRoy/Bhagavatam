# dictionary.db for English, Hindi, Bengali and Odia: research, features and implementation plan

> For the agent doing the work (Antigravity or any other): read `AGENTS.md` first, then this file. Work task by task in order. Each task has files, a test, and an acceptance check. Tasks marked **GATE** need the owner's decision before you go on. Tick the boxes as you finish.

**Goal:** an offline dictionary inside the app. Press and hold a word in any reading language and get its meaning at once, with no internet; plus a dictionary screen to search and save words.

**Supersedes** the dictionary part of `2026-10-02-annotations-and-dictionary.md` (that plan's Sanskrit word-splitting idea is out of scope here; see "Not in this plan").

---

## 1. Research findings (measured on 2026-10-09)

### 1.1 What the app's own text contains (`content/_dict/coverage_probe.py`)

| Language | Tokens | Distinct word forms | Forms seen 2+ times | Top 5,000 forms cover |
|---|---|---|---|---|
| Hindi | 470,880 | 36,063 | 17,869 | 86.4% of the text |
| Bengali | 516,390 | 49,574 | 24,004 | 81.6% |
| English | 658,202 | 19,238 | 12,480 | 94.5% |
| Odia (Tesseract draft, noisy) | 529,095 | 65,025 | 24,650 | 79.5% |

Meaning: a dictionary only has to cover tens of thousands of forms, not millions. A **closed-corpus dictionary** (entries for words the book actually uses, plus their base forms) is small. Odia numbers are inflated by OCR errors until the Odia text is verified.

### 1.2 Candidate sources and their licences

| Source | What it gives | Licence | Verified how |
|---|---|---|---|
| **Odia Wiktionary** (or.wiktionary.org) | about 108,600 articles; Odia-language definitions, many senses per word (a sample entry had 40) | CC BY-SA 4.0 | licence read from the site's API; entry sampled |
| **Bengali Wiktionary** | about 164,500 articles; Bengali definitions, etymology, IPA, synonyms | CC BY-SA 4.0 | same |
| **Hindi Wiktionary** | about 185,100 articles; translations, and imported text from a published Hindi dictionary (Shabdsagar) | CC BY-SA 4.0 | same; Shabdsagar's own status not checked |
| **English Wiktionary** (en.wiktionary.org) | 9.2 million articles; English glosses for words in every language: Hindi 35,881 words, Bengali 9,977, Odia 2,034, English 1.39 million | CC BY-SA 4.0 | same; counts from kaikki.org per-language pages |
| **Wiktextract / kaikki.org** | machine-readable extracts of English Wiktionary (JSONL). The per-language files are marked "deprecated, will be removed"; raw data is on the raw data page | derived from Wiktionary; kaikki.org states no licence of its own that I could read | not confirmed: treat as CC BY-SA 4.0 because it is Wiktionary text |
| **Wikidata lexemes** | Odia 286, Bengali 11,811 lexical entries (Hindi count not obtained: my query used a wrong language id) | CC0 | counts from the public query service |
| **DSAL, Praharaj "Purnachandra Odia Bhashakosha"** (1931-40) | the standard Odia dictionary | page says "licensed under a Creative Commons License" without naming which | **not confirmed** which CC licence (non-commercial would matter) |
| **DSAL, Samsad Bangla (Biswas)**, **McGregor Hindi-English (Oxford)** | large published dictionaries | copyrighted, redistribution forbidden | read from the site: **do not use** |
| **Tatoeba** | example sentences | CC BY 2.0 FR (some CC0) | read from its downloads page; Odia coverage not checked |
| **IndoWordNet** (Hindi, Bengali, Odia wordnets) | synonym sets | licence not found on its home page | **not confirmed** |
| **Princeton WordNet** (English) | English senses | permissive, needs notice | **not confirmed** (page did not load) |

CC BY-SA 4.0 means: credit the source, say it is CC BY-SA, and share changes under the same licence. The dictionary is a **separate file** (`dictionary.db`), so the share-alike applies to that file, not to the app code. This is not legal advice; the owner decides.

### 1.3 Coverage probe: do the most frequent words have a page?

For the 150 most frequent words of each language (the easiest words, so real coverage of rarer words will be lower):

| Language | Native Wiktionary | English Wiktionary |
|---|---|---|
| Hindi | 110/150 words (83% of their occurrences) | 146/150 (99%) |
| Bengali | 124/150 (87%) | 114/150 (84%) |
| Odia (noisy text) | 68/150 (48%) | 47/150 (35%) |
| English | 149/150 (100%) | 149/150 (100%) |

Conclusions:
1. Hindi and English are well covered. Bengali is good (the native Wiktionary is the better source).
2. **Odia is the weak language.** Even common words are missing about half the time. Part of that is OCR noise in the probe text, so the true figure is unknown until the Odia text is verified. Plan for Odia to be the incomplete dictionary and say so in the app.
3. The English Wiktionary extract for Odia has only about 2,000 words: use the Odia Wiktionary instead, and the Praharaj dictionary if its licence allows.

### 1.4 What did not work as a source
- Samsad Bangla and Oxford McGregor: copyrighted.
- Scraping Wiktionary page by page: Wikimedia answered **429 Too Many Requests** within a few hundred calls. Use the official dumps (`dumps.wikimedia.org`), never loop over the web API.
- Google results: not a data source (terms), and already removed from the app.

---

## 2. Features

**MVP (this plan)**
1. **Offline meaning card.** Press and hold a word: the card shows the base word, part of speech, and up to three meanings, in under 50 ms, with no network. Source label under the meaning ("Wiktionary, CC BY-SA").
2. **Meaning language setting.** Meanings are shown in: the word's own language (default), English, or the app language. If the chosen language has none, fall back to English, then to the word's own language.
3. **Smart lookup.** Normalise spelling (NFC, joiners, chandrabindu/anusvara, nukta); try the exact form, a form table (inflected form to base word), then suffix-stripped stems; for Sanskrit-origin words also look in the other scripts (a Bengali word whose Devanagari spelling is in the Hindi dictionary).
4. **Dictionary screen.** Under Library: a search box with prefix search, a language filter, and an entry page (all senses, part of speech, etymology, pronunciation if known). Reads fully offline.
5. **Saved words.** Save a word from the card or the screen; list with the verse it came from; share as text. Stored locally only.
6. **Credits screen.** Every source with its licence and link, as CC BY-SA requires.
7. **Online fallback stays**, only for words the offline dictionary does not have, and only if the existing "Look up meanings online" setting is on.

**Later (not in this plan, decide after MVP)**
- Examples from the book itself (the in-book concordance already exists) and from Tatoeba.
- "Words in this chapter" list of harder words.
- Sanskrit dictionary (Monier-Williams, Apte via Cologne): licence unconfirmed.
- Pronunciation audio (online files only; skip).

**Non-goals:** sentence translation, copyrighted dictionaries, any scraping at scale, gamified features (flashcards, streaks).

---

## 3. Design

### 3.1 Files and who owns what
```
content/_dict/                 pipeline (scripts committed; data and outputs are NOT committed)
  README.md  sources.json  coverage_probe.py          (exist)
  fetch.py                     download dumps with a polite User-Agent and resume
  parse_<edition>.py           one parser per source -> normalised JSONL
  build_dictionary.py          JSONL -> dictionary.db
  work/                        downloads, vocab_*.tsv, JSONL  (.gitignore)
app/src/main/assets/dictionary.db      shipped file (.gitignore; rebuilt by the pipeline)
app/src/main/java/com/bhagavatam/app/data/
  DictionaryDb.kt              opens the asset like ContentDb does
  Normalise.kt                 spelling normalisation + script key (pure functions, unit tested)
  Lemmatizer.kt                suffix tables (move them out of OnlineMeaning.kt)
  Dictionary.kt                lookup(word, lang, meaningLang): List<Meaning>, search(prefix, lang)
  SavedWords.kt                local table in annotations.db
app/src/main/java/.../ui/screens/DictionaryScreen.kt, SettingsWords.kt (meaning language), CreditsScreen.kt
tools/agent/dict_check.py      verifier (exists, self-tested)
```

### 3.2 Schema (version 1, `dict_check.py` enforces it)
```sql
meta(key, value)                 -- dictionary_version, built, schema
source(id, code, name, licence, url, attribution, retrieved)
entry(id, lang, headword, norm, skey, pos, ipa, etymology, freq, source_id)
sense(id, entry_id, idx, gloss_lang, gloss, example)   -- idx 0 is the first meaning
form(form_norm, entry_id, kind)                        -- inflected or variant spelling -> entry
entry_fts                                              -- FTS5 over headword, for prefix search
-- indexes: entry(lang,norm), entry(skey), form(form_norm)
```
`lang` is `en|hi|bn|or`. `gloss_lang` is the language the meaning is written in. `skey` is a script-neutral key: Bengali and Odia letters mapped to their Devanagari equivalents, so one tatsama word has the same `skey` in every script. `freq` is the word's count in the book (from `vocab_*.tsv`), 0 if absent.

### 3.3 What goes in (size control)
Keep an entry if its headword or any of its forms appears in the book's vocabulary, **or** it is among the 20,000 most useful words of the language by native-Wiktionary link count. Keep at most 6 senses per entry, glosses cut at 240 characters. **Budget: `dictionary.db` at most 30 MB on disk, at most 10 MB added to the APK** (the APK compresses assets). If a build is over, tighten the filters, never drop a language. Measure and record the real size in the plan after Task 4.

### 3.4 Lookup order (app)
1. `norm = Normalise.word(text, lang)`.
2. `entry` where `lang` and `norm` match; then `form` table; then each stem from `Lemmatizer`.
3. If nothing: `entry` by `skey` in the other languages (marked "also in Hindi" etc.).
4. If nothing and the online setting is on: the existing Wiktionary lookup (`OnlineMeaning`).
5. Rank: exact headword first, then by `freq`; group senses by entry; at most 3 senses on the card, all of them on the dictionary screen.

### 3.5 Honest limits to show in the app
- Odia is incomplete; the credits screen and the empty-result message say so.
- A meaning is a dictionary sense, not the verse's meaning. Do not word it as a translation of the verse.

---

## 4. Implementation plan

Order matters: do not start the Android work before Task 4 shows real coverage numbers.

### Task 0 - Sources and licences (GATE)
Files: `content/_dict/sources.json`.
- [x] For every row of the table in 1.2 marked "not confirmed", read the licence from the source's own page and update `sources.json` (`licence`, `status: verified|rejected`). Needed: Praharaj's exact CC licence (and whether it is non-commercial), IndoWordNet, Princeton WordNet, the Shabdsagar text inside Hindi Wiktionary.
- [x] Write to the owner: which sources will be used, with their licences. **Stop and wait for the answer.** Do not download or build anything that depends on an unconfirmed source.
Acceptance: every source in the file has `status` and a licence string; the owner has confirmed the list.

### Task 1 - Corpus vocabulary
Files: `content/_dict/coverage_probe.py` (exists and works).
- [x] Run `python content/_dict/coverage_probe.py`. It writes `work/vocab_<lang>.tsv`.
- [x] Re-run it whenever the Odia text is verified; the Odia numbers are only an estimate now.
Acceptance: five `vocab_*.tsv` files exist; the printed table matches section 1.1 (within a few percent).

### Task 2 - Fetch the sources
Files: `content/_dict/fetch.py`.
- [x] Download the latest Wiktionary dumps for `orwiktionary`, `bnwiktionary`, `hiwiktionary` (pages-articles, bz2) from `dumps.wikimedia.org`; for English use the raw Wiktextract data or the `enwiktionary` dump filtered to the four languages.
- [x] Polite client: a real `User-Agent` with a contact, resume support, no loops over the web API, honour 429 with back-off.
- [x] Record the dump date and file hash in `work/fetched.json` (goes into `source.retrieved`).
Acceptance: files in `work/dumps/`; `fetched.json` lists each with size and date; no web-API crawling in the script.

### Task 3 - Parsers to a common JSONL
Files: `content/_dict/parse_wiktionary.py` (+ per-edition rules), `content/_dict/tests/test_parse.py`.
- [x] Output one JSON object per entry: `lang, headword, pos, glosses[{lang, text}], etymology, ipa, forms[], source`.
- [x] Clean wikitext: links `[[a|b]]` to `b`, remove templates `{{...}}`, `<ref>`, HTML entities, and replace em or en dashes with a spaced hyphen (project rule).
- [x] Keep the pieces that matter per edition: Odia: the numbered or bulleted senses; Bengali: `#` senses, `{{IPA}}`, etymology, synonyms; Hindi: `#` senses and translations (the Shabdsagar section only if Task 0 allows).
- [x] Unit tests with three real entries per edition (the samples in section 1.2 are a start): senses extracted, markup gone.
Acceptance: `pytest content/_dict/tests` passes; a spot check of 20 random entries per language reads cleanly; the count of entries per language is printed.

### Task 4 - Build `dictionary.db`
Files: `content/_dict/build_dictionary.py`.
- [x] Create the schema in 3.2, insert `source` rows with licence and attribution text, apply the filters in 3.3, build `form` from the sources' inflected-form data, compute `skey` and `freq`, build `entry_fts`, `VACUUM`.
- [x] Print a coverage report: for each language, `python coverage_probe.py --against work/headwords_<lang>.txt` figures (share of the book's distinct forms and of its tokens that have an entry) and the top 50 missing frequent words.
- [x] Write the real size and coverage numbers into section 5 of this file.
Acceptance: `python tools/agent/dict_check.py content/_dict/dictionary.db --max-mb 30` prints no FAIL; Hindi, Bengali and English each cover at least 90% of the book's tokens (Odia: report the number, no target until the Odia text is verified). If a target is missed, say so; do not lower it silently.

### Task 5 - Android data layer
Files: `Normalise.kt`, `Lemmatizer.kt`, `DictionaryDb.kt`, `Dictionary.kt`, `SavedWords.kt`; tests under `app/src/test/.../data/` with a tiny fixture database in `app/src/test/resources/`.
- [ ] `Normalise`: NFC, remove U+200C/U+200D, normalise chandrabindu/anusvara and nukta variants, lower case Latin, `skey` mapping Bengali and Odia to Devanagari. Pure functions, tested with real words from each script.
- [ ] `Lemmatizer`: move the suffix tables out of `OnlineMeaning.kt` and extend them (Hindi, Bengali, Odia case endings); keep `OnlineMeaning` using the same class.
- [ ] `DictionaryDb`: copy `assets/dictionary.db` to `files/` once per app version (same approach as `ContentDb`), open read-only.
- [ ] `Dictionary.lookup` and `search` as in 3.4; run off the main thread; return in under 50 ms on the emulator (log the time).
- [ ] `SavedWords`: table in `annotations.db` (word, lang, ref, saved time); never leaves the phone.
Acceptance: `./gradlew :app:testDebugUnitTest` passes with new tests for Normalise, Lemmatizer and lookup order (exact beats stem beats cross-script).

### Task 6 - UI
Files: `ui/components/MarkedText.kt` / `AnnotationSheet.kt` (the card), `ui/screens/DictionaryScreen.kt`, `ui/screens/SettingsWords.kt`, `ui/screens/CreditsScreen.kt`, `ui/AppNav.kt`, `data/Strings.kt` or `tr()` strings.
- [ ] Meaning card reads from `Dictionary` first; online lookup only for misses and only if the setting is on. Source label on every meaning.
- [ ] Dictionary screen: reachable from Settings > Library and from the card ("Open in dictionary"); search with prefix results as you type, language chips, entry page.
- [ ] Save word button on the card and the entry page; saved list in the Dictionary screen.
- [ ] Settings > Words & notes: "Meaning language" (word's language, English, app language).
- [ ] Credits screen listing each source row from `source` with licence and link; reachable from Settings > About and from the dictionary screen.
- [ ] All strings in English, Hindi, Bengali; no em dashes; 48dp touch targets; fonts from `readingFont()`.
Acceptance: `tools/agent/verify.sh` passes; by hand on a device or emulator, in English and Hindi app language, light and dark: hold a Hindi, Bengali and English word and see the offline meaning with airplane mode on; open the Dictionary screen, search a prefix in each language, save and remove a word, open Credits. Screenshots taken and read. Say which of these could not be checked.

### Task 7 - Ship
Files: `app/build.gradle.kts`, `.gitignore`, `README.md`, `CLAUDE.md`.
- [ ] Add `dictionary.db` and `content/_dict/work/` to `.gitignore`; bump `versionCode`/`versionName`.
- [ ] Release build; install; check APK size increase against the 10 MB budget and record it here.
- [ ] README: one line about the offline dictionary and its sources; CLAUDE.md: the `content/_dict` pipeline commands.
- [ ] Commit only if the owner asks; run `tools/agent/commit_check.sh` first. The licence texts and credits are part of the app, so confirm Credits works before any release.
Acceptance: release APK installs, dictionary works offline, size recorded, commit check clean.

---

## 5. Results (fill in as tasks finish)
- Real `dictionary.db` size: 16.34 MB (budget 30 MB); APK increase: (measured in Phase 4 when shipped)
- Coverage of the book (raw tokens before Lemmatizer stemming):
  - English: 96.0% of tokens (14,660 / 19,238 forms; 960 / 1,000 top words)
  - Bengali: 75.8% of tokens (12,814 / 49,574 forms; 841 / 1,000 top words)
  - Hindi: 75.3% of tokens (8,558 / 36,063 forms; 788 / 1,000 top words)
  - Odia: 26.2% of tokens (1,459 / 65,025 forms; 266 / 1,000 top words; noted as unverified draft text)
- Lookup time on the emulator: (to be measured in Phase 4)
- Licence decisions from Task 0: Four verified Wiktionaries (hi, bn, or, en), all CC BY-SA 4.0; Shabdsagar excluded; Samsad & McGregor rejected.

## 6. Risks
- **Odia is thin.** Mitigation: use the Odia Wiktionary and, if the licence allows, Praharaj; state the limit in the app; revisit when the Odia text is verified.
- **Licence of the DSAL Praharaj text** may forbid commercial use. Do not include it until Task 0 clears it.
- **Parsing wikitext is fiddly** and differs by edition. Mitigation: Task 3 tests on real entries; spot checks of 20 random entries per language.
- **Size.** Mitigation: the filters in 3.3 and the hard budget.
- **Another agent editing the same folder.** Follow `AGENTS.md` rule 1.

## 7. Not in this plan
Sanskrit dictionary and word-by-word splitting; Odia text verification itself; audio pronunciation; example sentences.
