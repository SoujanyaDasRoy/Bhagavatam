# content/_dict - pipeline for dictionary.db

Plan and rules: `docs/superpowers/plans/2026-10-09-dictionary-db.md`. Working agreement: `AGENTS.md` (section 7).

| File | State |
|---|---|
| `coverage_probe.py` | works: counts the book's words per language, writes `work/vocab_<lang>.tsv`, can measure a headword list against them |
| `sources.json` | source list with licences and status (verified / unverified / rejected) |
| `fetch.py`, `parse_wiktionary.py`, `build_dictionary.py` | to be written (Tasks 2 to 4) |
| `work/` | downloads, vocabulary files and JSONL; in `.gitignore`, never commit (derived from the Gita Press text or large) |

Check a built database: `python tools/agent/dict_check.py content/_dict/dictionary.db --max-mb 30`
Copy the passing file to `app/src/main/assets/dictionary.db` (also in `.gitignore`).

Never crawl the Wikimedia web API page by page (it returns 429 within minutes). Use the dumps.
