# Odia transcription: how accuracy is checked and kept

Scope: Skandha 5 to 12 of the Gita Press Odia Bhagavatam, 1,437 pages (v1:579 to v1:1018 and v2:11 to v2:1007). Skandha 1 to 4 are already
transcribed (`content/_odia/vision_transcripts`, 491 pages) and are used as the trusted reference.

## What was measured before writing any of this (2026-10-11, on the 491 trusted pages)

| Measure | Result | What it means |
|---|---|---|
| Tesseract draft against the trusted text | 25% of words differ (page median 24%, worst page 80%) | The existing draft in `content/or` for Skandha 5 to 12 is not usable as text. Example: printed `ପଚାରିଲେ` was read as `ପଗ୍ରରିଲେ`. |
| Tesseract (all 2,054 pages), impossible-spelling rate | 10.6 words in 1000 | The hygiene check below finds OCR damage automatically. |
| Trusted text, impossible-spelling rate | 0 in 117,109 words | The hygiene check has no false alarms on good text. |
| Unknown-word rate against a 56k-word lexicon | 16.3% for correct text, 31.1% for Tesseract | A lexicon is a weak judge in an inflected language. It is only used to break ties, never to accept or reject. |
| Re-assembling the trusted pages | 63 of 64 chapters reproduced byte for byte | The page JSON to chapter path is sound; the one difference is a page outside the trusted range. |

These numbers are why the method below does not trust one reading, and does not trust Tesseract.

## The method: two independent readings, then a measured audit

1. **Pass A** (`odia_vision.py run --pass A`): a vision model reads every page image (2000 px). Fields as in the Bengali pipeline: paragraphs,
   verse numbers, running head, chapter heading, colophon, footnotes. Faithful transcription only, `[?]` where unsure.
2. **Pass B** (`--pass B`): a different, stronger model (default `gemini-2.5-pro`) reads the same page rendered at 2600 px from another cache.
   Different model and different pixels, so the two do not repeat each other's slips.
3. **Tesseract** (already available for every page) is the third voice, only as a tie-breaker. Its errors are systematic
   (`ୟ`/`ଯ`, `ି`/`ୁ`, `ଙ`/`ର`) and rarely coincide with a vision model's.
4. **Word-level consensus** (`odia_verify.py compare`): reading A is the skeleton; B and Tesseract are aligned onto it word by word.
   - A and B agree: accepted (`AB`).
   - A and B differ but Tesseract equals one: that one wins (`AT`, `BT`).
   - No majority: the word is a **dispute**; the chosen word is written with `[?]` and goes on the list.
   - A word only one reading has: **single**; a word B has and A lacks: possible **omission**.
   - Impossible spellings (stray Latin letters, a Devanagari or Bengali letter, ASCII digits, a vowel sign with no consonant,
     two vowel signs in a row, virama followed by a vowel sign) are flagged even when both readings agree.
   - Page structure: running-head chapter, chapter number, verse numbers, colophon and paragraph count must match between A and B.
5. **Arbitration** (`odia_arbitrate.py`): disputes, single words and lexicon-settled words go back to a strong model with the page and the
   candidates; it must pick A, B or T or write the printed word, with a confidence. Only high-confidence answers are applied.
6. **A person** (`odia_review.py queue`) sees what remains: the page image beside the sentence, the word highlighted, keys 1/2/3 to take
   a reading or a field to type the printed word. This list is short because steps 4 and 5 removed everything they could settle.
7. **Structure check** (`odia_assemble.py`): every chapter is compared with the Hindi verse list; a missing or repeated verse number is
   reported in `work_final/review/report.md`. This is the same zero-gap rule as the Bengali pipeline.
8. **Audit** (`odia_verify.py audit-sample`, `odia_review.py audit`, `odia_verify.py audit-score`): a random sample of words that WERE
   accepted is shown to a person, who marks each correct or wrong. The tool reports the error rate with a 95% interval. This is what turns
   "the models agree" into a measured accuracy. Words settled by a model are audited as their own stratum.

## Acceptance rule

| | |
|---|---|
| Agreed words | audit 600 words (about 75 per skandha, seed 7). Accept the text when the 95% upper bound on the error rate is at most 1.0% (0 errors in 300 words already gives about 1.2%; 0 in 600 gives 0.6%). |
| Model-settled words | audit 150. Accept when the upper bound is at most 3%; otherwise send all of them to the person's queue. |
| Structure | zero chapters with a missing or repeated verse, or each one explained in `work_final/review/report.md` with what the print shows. |
| Unresolved `[?]` | zero before `--strip`. |

If the audit fails: read the wrong words, find the pattern (usually one letter pair), and either re-read those pages with pass C
(`--pass C --model <strongest>`), or add the pair to the review keys and re-audit. Never lower the bar to pass.

## What this cannot promise

- Two models can make the same mistake on a smudged or unusual conjunct. The audit samples for exactly this; it measures, it cannot prevent.
- The audit is only as good as the person's reading of the printed page. Take it in sittings of 50 words.
- The text reproduces the printed edition, including its own misprints. Misprints are not corrected (the print is the authority).
- Skandha 10 pages with long full-width commentary are the hardest layout; expect more disputes there.

## Keeping it accurate afterwards

- Every change to a chapter file goes through `odia_verify.py apply` (decision files are kept in `work_v/review/`), never by hand edits of the markdown.
- `verification_status` stays `needs_review` until the audit for that skandha is done and recorded in `docs/agent/REPORT.md`;
  only then `machine_transcribed_clean` is acceptable and `reviewed` is reserved for chapters a person read in full.
- Re-run `odia_verify.py compare` + `report` whenever a page is re-read; the report is the record.
- The tests (`py content/_odia/tests/test_verify.py`) must pass before any change to the tools.
