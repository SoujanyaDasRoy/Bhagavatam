# Bengali Bhagavatam extraction tool

Transcribes the two Gita Press Bengali PDFs (`Shastra\Bengali\শ্রীমদ্ভাগবত মহাপুরাণ ১/২_গীতাপ্রেস.pdf`) into one Markdown file per chapter,
with every doubtful verse flagged. Nothing is silently corrected.

How it works: native page image from the PDF -> vision model transcribes the Bengali translation (right column) as structured JSON ->
text is cut into verses at every `॥ N ॥` -> each verse is compared with independent OCR text (bundled in `data/ocr`, 2,096 pages)
-> verse numbers are checked against the Hindi edition (`data/expected_verses.json`) -> Markdown + review files are written.

## One-time setup (PowerShell, in this folder)
```
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
$env:ANTHROPIC_API_KEY = "sk-ant-..."     # your key; it is only read from the environment, never written to a file
python bn_extract.py check
```
If your PDFs are somewhere else, edit `pdf_dir` in `config.json`.

## Run
```
python bn_extract.py run --vol 1 --pages 228-240     # pilot: 13 pages (start of Skandha 3)
python bn_extract.py assemble
```
Open `review\index.html` in a browser and look at the result before spending on the full run. If it looks good:
```
python bn_extract.py estimate                         # tokens left; multiply by your model's current prices
python bn_extract.py run --vol 1 --pages 26-300 --workers 4
python bn_extract.py run --vol 1 --pages 301-985
python bn_extract.py run --vol 2
python bn_extract.py assemble
```
Stop (Ctrl+C) and restart any time: a page is finished when `work\vision\vN_pNNNN.json` exists. Failed pages are listed in
`work\errors.log`; re-running the same command retries them. `--model claude-opus-5-5` re-reads hard pages with a stronger model
(add `--force` to replace an existing result). Default model is `claude-sonnet-5-5` (override with `$env:BN_MODEL`).

## Output
- `out\skandha-NN\chapter-NN.md`, `out\mahatmya\`, `out\mahatmya-skanda\`: same layout as `content\hi`. Each verse carries a comment
  with its page, OCR agreement and FLAGS. Chapter status is `needs_review` or `machine_transcribed_clean`.
- `review\report.md`: per-chapter counts, missing verse numbers, chapters not found.
- `review\flagged.csv` and `review\index.html`: every flagged verse with the page image beside the text.

## Fixing flagged verses
Edit the verse in `out\...\chapter-NN.md` against the page image, delete the `[?]` and the FLAGS note, and when a whole chapter is checked
change its `verification_status` to `"reviewed"`. `assemble` never overwrites a `reviewed` file.

## What a flag means
- `[?]` after a word: the model could not read it confidently.
- `LOW agreement with OCR` (< 0.30) / `check` (< 0.45): the verse text and the independent OCR disagree. Real text normally scores 0.6-0.8.
- `gap` / `verse number not increasing` / `verses with no translation unit`: a verse number is missing or out of order versus the Hindi edition.
- `Latin letters`, `does not end with a verse marker`: structural problems.
