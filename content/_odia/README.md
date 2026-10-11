# Odia translation (Gita Press Odia edition) - machine-transcribed draft

`content/or/` holds all 341 chapters of the Gita Press Odia Bhagavatam (Mahatmya 6, Skandha 1-12), one `.md` per chapter, same layout as `bn/`:
`or/mahatmya/chapter-NN.md`, `or/skandha-NN/chapter-NN.md`, each with the frontmatter, `### ଶ୍ଳୋକ N` blocks, a `<!-- page: N -->` comment, the colophon and `### ଟୀକା` footnotes.
Chapter and verse numbering follows the Hindi edition (`data/expected_verses.json`); `source_pages` are `v1:N` (Part 1) and `v2:N` (Part 2) PDF page numbers.

## What this is NOT
Every file is `verification_status: needs_review`. The text is plain Tesseract 5 OCR (`ori`, tessdata_best) with layout analysis, not a vision-model transcription.
- About 40% of the translation units carry `[?]` marks on low-confidence words and a FLAGS comment in the page comment. Residual OCR errors remain in the unflagged text too.
- Known systematic misreads: the `ଙ୍କ` / `ଙ୍କର` / `ଙ୍କୁ` endings (corrected only when the repaired word is attested in the book; the rest are flagged), `ଥି` read as `ଥୁ`, `ଚା` read as `ଗ୍ବ`, `ପୃ` read as `ପୂ`. Every automatic correction is in `_odia/review/autofix_log.csv`.
- 67 chapters have `title: ""`: the chapter heading box was not read on the start page. Titles that exist are OCR and often noisy.
- Skandha 10 pages with long full-width commentary below the verses are the weakest: Sanskrit lines can be mixed into the translation text.
- Verses with no translation unit (unreadable markers) and chapters that did not match the Hindi verse list are in `_odia/review/report.md` and `flagged.csv`.
- The Sanskrit column is not stored (verse numbers only); the Odia translation is.

## How it was made
`_odia/odia_ocr.py` (render 300 dpi, find the column rule, OCR the Sanskrit and translation columns separately, read headings, colophons and footnotes) writes one JSON per page.
`odia_fix.py` applies the conservative corrections. `odia_assemble.py` cuts chapters and verses using verse-number restarts, heading boxes, colophons and the running-head chapter number checked against the Hindi verse counts, and writes the `.md` files.
`page_json.tgz` has the per-page JSON so the assembly can be re-run without OCR. `rerun_starts.py` / `redo.py` re-OCR chapter-start pages.
Run order: `odia_ocr.py run --vol N`, `rerun_starts.py N`, `odia_fix.py`, `odia_assemble.py` (env `ODIA_WORK`, `ODIA_OUT`, `ODIA_REVIEW`). `config.json` has the PDF paths (override with `ODIA_PDF_1`, `ODIA_PDF_2`) and skips Part 2 front matter (pages 1-10).

## Upgrade to Hindi-level quality (vision pass)
`odia_vision.py` is the Bengali method for Odia: a vision model reads each page image and writes the same per-page JSON into `work_v/vision/`. The Tesseract files above stay as the fallback.
Setup, pilot and full run are in the docstring at the top of the script (about 2,044 pages, roughly 6M input and 3M output tokens, needs an API key). Then `$env:ODIA_WORK='work_v'; python odia_assemble.py`.

## Next
For publishable accuracy run a vision-model pass over the flagged pages (as planned for `_bengali/bn_extract`), then add an `add_odia.py` modelled on `add_bengali.py`; the `### ଶ୍ଳୋକ N` blocks are compatible with its parser.
