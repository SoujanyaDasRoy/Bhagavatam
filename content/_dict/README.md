# content/_dict - pipeline for dictionary.db

Plan and rules: `docs/superpowers/plans/2026-10-09-dictionary-db.md`. Working agreement: `AGENTS.md` (section 7).

| File | State |
|---|---|
| `coverage_probe.py` | works: counts the book's words per language, writes `work/vocab_<lang>.tsv`, can measure a headword list against them |
| `sources.json` | source list with licences and status (verified / unverified / rejected) |
| `fetch.py`, `parse_wiktionary.py`, `build_dictionary.py` | to be written (Tasks 2 to 4) |
| `add_sanskrit.py` | works: adds Sanskrit entries (English Wiktionary's Sanskrit words, Wiktextract JSON from kaikki.org, CC BY-SA 4.0) for the words of the book; run it, then `add_book_words.py`. Input: `work/dumps/sanskrit_kaikki/kaikki-sanskrit.jsonl` (145 MB, not committed). 2026-10-18: Sanskrit words with a meaning rose from 19% to 59% of the book's Sanskrit text; compounds are still not split |
| `add_curated.py` | works: hand-written glosses for frequent book words no source has (`curated/{or,hi,bn,sa}.tsv`, CC0, unreviewed) |
| `book_gloss.py` | works: gives the words no dictionary has a meaning from the book itself. Aligns each verse's words across the translations (IBM Model 1 with a spelling prior: Odia, Bengali and Sanskrit to Hindi, Hindi to English, English to Hindi) and splits words into dictionary parts (Sanskrit sandhi, compounds). Writes table `book_gloss` into **content.db** (both copies), not into dictionary.db, because it is derived from the Gita Press text. 2026-10-18: words of the book with a meaning (by tokens) en 99.7%, hi 98.2%, bn 97.5%, or 94.9%, sa 71.0%; any other word shows its verse in another language |
| `work/` | downloads, vocabulary files and JSONL; in `.gitignore`, never commit (derived from the Gita Press text or large) |

Order after any change: `add_curated.py` -> `add_book_words.py` -> `book_gloss.py` (all with `content/_dict/dictionary.db`), then copy dictionary.db to the app.

Check a built database: `python tools/agent/dict_check.py content/_dict/dictionary.db --max-mb 32`
Copy the passing file to `app/src/main/assets/dictionary.db` (also in `.gitignore`).

Never crawl the Wikimedia web API page by page (it returns 429 within minutes). Use the dumps.

## Hand-written meanings (glossing.py)

`curated/glossed/*.tsv` holds meanings written by hand (CC0, source `curated`, pos `glossed`) for every word of the chapter markdown that no other source covered: 52,677 groups, 2,637 marked `?` as misreads or clipped fragments (`curated/glossed/misread.txt`). The queue is empty (`py -I content/_dict/glossing.py status`). Rebuild order: `add_curated.py`, `glossing.py load`, `add_book_words.py`, `book_gloss.py`, copy `dictionary.db` to `app/src/main/assets/`, `tools/agent/dict_check.py --max-mb 60`, `tools/agent/word_coverage.py`.

Limits: the meanings are machine and hand made and have not been read by native speakers; words that are OCR garbage or cut-off fragments carry a guessed gloss or none. dictionary.db is about 50 MB, so the size budget is 60 MB.
Token coverage (word_coverage, 2026-10-20): Hindi 99.90%, Sanskrit 99.68%, English 99.89%, Bengali 99.58%, Odia 99.72%.
