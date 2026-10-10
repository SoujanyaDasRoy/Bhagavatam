# content/_search - research and pipeline for the search index

Plan and rules: `docs/superpowers/plans/2026-10-09-search.md`. Working agreement: `AGENTS.md` (section 8).

| File | State |
|---|---|
| `prototype.py` | works: builds in memory the per-language word index and the loose phonetic key, prints index size estimates and key quality checks. Research code, not imported by the app |
| `gold_queries.json` | the objective test set: `required` queries must pass, `target` queries are expected to fail until their feature exists |
| `eval_queries.py` | runs the gold queries: `python content/_search/eval_queries.py` (prototype engine); `--engine index` is for the real `search.db` once it exists |
| `aliases.json`, `build_index.py`, `tests/` | to be written (Tasks 1 to 3) |
| `work/`, `search.db` | outputs; in `.gitignore`, never commit (derived from the Gita Press text) |

Current result on the prototype: all required queries pass; the targets that fail are related words (crocodile and alligator), typos, and name aliases (Narasimha and Nrisimha).
