# Tools

Run everything from the project root, for example `py tools/content/missing_report.py`.

| Folder | Scripts | Job |
|---|---|---|
| `content/` | `missing_report.py`, `fill_gaps.py`, `fill_missing.py`, `fill_odia_gaps.py`, `verify_gaps.py` | list, fill and check the verses that still have no Bengali or Odia translation |
| `art/` | `import_art.py`, `make_chapter_prompts.py`, `batch_generate_art.py`, `grade_art.py`, `generate_icons.py`, `svg2vd.py` | paintings and icons |
| `qa/` | `lang_audit.py`, `language_matrix.py` | language and screen checks |
| `dev/` | `generate_graphify.py`, `make_code_map.py` | code map and knowledge graph |
| `agent/` | checks used by coding agents | |

Generated data (databases, page images, OCR output) is never committed.
