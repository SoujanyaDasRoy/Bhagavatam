# Running the Bengali extraction locally, without an API key

Status: `ocr_local.py` is written but NOT proven. Its layout step (finding the footnote rule and full-width chapter headings) gave
wrong answers on several test pages, and the OCR half could not be run because Tesseract is not installed on the machine it was written on.
Expect to tune it against a few real pages before trusting a long run. Everything after it (assemble, promote, import) is the same
tested code that produced the chapters already in the app.

## How the chapters already in the app were made (the exact process)

1. `python bn_ingest.py render A B --vol N` renders PDF pages A..B to PNG files in `work/images/`.
2. A person or assistant reads each PNG and types the Bengali translation column into a batch text file
   (`work/batches/*.txt`): one `@@ vol page` block per page, with `sv:` (Sanskrit verse numbers on the page), `label:` (printed page number),
   `cont: 1` (first paragraph continues from the previous page), `chapter:` / `title:` / `colophon:` where a chapter starts or ends,
   `fn:` for footnotes and one `P:` line per printed paragraph. Words that cannot be read are marked `[?]`.
3. `python bn_ingest.py ingest FILE` turns the batch into `work/vision/vN_pNNNN.json` and prints checks (verse markers per page, stray
   Latin or Devanagari letters, Sanskrit verse numbers with no matching marker).
4. `python bn_extract.py assemble` cuts the text into verses at every `॥ N ॥`, compares the verse numbers with the Hindi edition
   (`data/expected_verses.json`), cross-checks each verse against the bundled OCR text (`data/ocr`), and writes `out/` and `review/`.
5. `python ../../_build/promote_bengali.py` copies only chapters where every Hindi verse number has a Bengali unit into `content/bn/`.
6. `python ../../_build/add_bengali.py <content.db> <version>` writes `verse.bn`, `verse.bn_from`, `chapter.title_bn`.
7. Copy `content/content.db` to `app/src/main/assets/content.db`, raise `versionCode` / `versionName` in `app/build.gradle.kts`,
   `./gradlew :app:assembleRelease` (JDK 17), and the APK lands in `apk/`.

The only step that needs "reading the page" is step 2. `ocr_local.py` replaces it with Tesseract so no person or API key is involved.

## Setup (Windows, PowerShell)

1. Install Tesseract with Bengali data: `winget install UB-Mannheim.Tesseract-OCR`, then in the installer's component list tick
   "Additional language data" and Bengali (and Hindi, optional, helps the Sanskrit verse numbers). Or download `ben.traineddata`
   from the tessdata_best repository into `C:\Program Files\Tesseract-OCR\tessdata\`.
2. In `content\_bengali\bn_extract`: `python -m pip install -r requirements.txt` (PyMuPDF, Pillow) and `python -m pip install numpy`.
3. `python ocr_local.py check` must show `ben language data: ok`.

## Try it before a long run

```
python ocr_local.py layout --vol 1 --pages 356-361
```
This prints, per page, where the footnote rule is, where the gap between the two columns is, and how many full-width bands
(chapter headings, closing lines) it found. Compare with the page images in `work\images`. Known problems from the first test:
the footnote rule was often not found (it printed y = page height) and some pages showed bands that are not there.
If the numbers are wrong on your pages, the thresholds at the top of `analyse()` in `ocr_local.py` are what to adjust.

Then a pilot on pages whose text is already in the app, so you can compare:
```
python ocr_local.py run --vol 1 --pages 350-355 --after-chapter 22 --force
python bn_extract.py assemble
```
Open `review\index.html` and look at the result beside the page images. (This overwrites the hand-made records for those pages in
`work\vision`; copy that folder first if you want to keep them.)

## The real runs (in order, no gaps)

```
python ocr_local.py run --vol 1 --pages 362-402 --after-chapter 25      # Skandha 3 chapters 25-33 (chapter 25 starts on 359, already done)
python ocr_local.py run --vol 2 --pages 803-1008 --after-chapter 0      # Skandha 11 (if page 803 opens chapter 1)
python ocr_local.py run --vol 2 --pages 1009-1084 --after-chapter 0     # Skandha 12
python ocr_local.py run --vol 2 --pages 168-802 --after-chapter 0       # Skandha 10
python bn_extract.py assemble
python ..\..\_build\promote_bengali.py
```
Pages must go in order because chapter numbers are counted up from `--after-chapter` (kept in `work\local_state.json`).
Delete that file to start counting again. Check the real first page of each Skandha against the images first.

## What to expect of the quality

Tesseract reads Bengali conjuncts and punctuation poorly. Low-confidence words get ` [?]`. The cross-check in `assemble` compares
against OCR from a similar engine, so its agreement score will look better than the text really is. Do not treat any chapter as
verified: read through `review\index.html` first, and strip the `[?]` marks only after correcting the word. If you would rather not show
uncorrected OCR in the app, leave those chapters out of `content\bn` (promote copies everything complete, so remove the folders you
do not want before running `add_bengali.py`).

## Afterwards

Come back with the result of `promote_bengali.py` and the path of the new chapters and the import, version bump, rebuild and device
check can be done as before.
