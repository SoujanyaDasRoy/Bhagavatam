# content/_search - search index pipeline

Plan and rules: `docs/superpowers/plans/2026-10-09-search.md`. Working agreement: `AGENTS.md` (section 8). State: `docs/agent/HANDOFF.md`.

| File | What |
|---|---|
| `prototype.py` | the loose phonetic key (`loose_from_roman`, `loose_from_indic`) and the old in-memory prototype |
| `build_index.py` | `content.db` + `aliases.json` -> `search.db`; refuses to build if an alias form is not in the book; `--check` only verifies the aliases |
| `search_engine.py` | reference reader of `search.db` (what the Kotlin `SearchIndex` must reproduce) |
| `aliases.json` | name aliases (by loose key) and related words (by exact form) |
| `gold_queries.json`, `eval_queries.py` | the objective test set and its runner (default engine: the real `search.db`) |
| `compare_rules.py` | tests a candidate key rule on every word of the book (how rules are decided, see `docs/agent/DECISIONS.md`) |
| `tests/` | `test_loose_pairs.py` + `loose_pairs.json` (shared with Kotlin), `test_search_db.py` |
| `work/`, `search.db` | outputs; in `.gitignore`, never commit (derived from the Gita Press text) |

```
py content/_search/build_index.py
py content/_search/eval_queries.py
py content/_search/tests/test_search_db.py
py content/_search/tests/test_loose_pairs.py
```
Result: 28 of 28 gold queries pass; `search.db` is 5.85 MB.
