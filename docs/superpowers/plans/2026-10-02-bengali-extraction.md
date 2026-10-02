# Bengali extraction: Mahatmya, Skandha 3 (rest), Skandha 10 to 12

**Goal:** Bengali translation text in `content.db` and the app for the Mahatmya, Skandha 3 chapters 12 to 33, and Skandha 10, 11 and 12, as faithful to the Gita Press Bengali PDFs as a careful transcription can make it, with every doubtful verse marked.

**Architecture:** Reuse `content/_bengali/bn_extract`: it renders each PDF page, takes a per-page transcription in a fixed JSON schema, cuts it into verses at every `॥ N ॥`, cross-checks verse numbers against the Hindi edition and the text against an independent OCR, and writes one markdown file per chapter with flags. Only the transcription step needs a model. There is no API key on this machine, so pages are read directly from the rendered images (the existing pilot was done the same way: `"model": "claude-agent"`). A small ingest tool turns a compact per-page text format into the JSON the assembler expects. Checked chapters are copied into `content/bn/` and `add_bengali.py` writes them to `content.db`.

## Scope (measured)
| Part | Verses | Chapters | PDF pages (estimated) |
|---|---|---|---|
| Mahatmya (Padma), vol 1 | 481 | 6 | 26 to 77 (52, from config) |
| Skandha 3, chapters 12 to 33, vol 1 | 989 | 22 | about 286 to 436 |
| Skandha 10, vol 2 | 3,946 | 90 | about 168 to 802 |
| Skandha 11, vol 2 | 1,367 | 31 | about 803 to 1008 |
| Skandha 12, vol 2 | 566 | 13 | about 1009 to 1084 |
| **Total** | **7,349** | **162** | **about 1,100** |

(6.6 verses per page was measured on the 40 chapters already done. The exact start of each Skandha is found from the page itself when the work reaches it.)

## Rules
- Faithful transcription only: same words, spelling, punctuation and dashes. `[?]` right after any word that is not clearly legible. Never guess silently.
- Translation only. No Sanskrit, speaker headings of the Sanskrit column, running headers, page boxes or footnotes inside paragraphs.
- Every translation unit keeps its printed verse marker (`॥ १ ॥`, `॥ ४-५ ॥`).
- A chapter goes into the app as extracted, flagged verses included, and every flag stays listed in `content/bn/REVIEW.md`. A verse with no translation unit simply has no Bengali (the app already says so).
- Source files in `content/bn/` are never edited by the import.

## Order of work (most important first; each step ends with the app rebuilt and checked)
1. **Tooling.** `bn_ingest.py` (compact page format to JSON, with checks), `skandha_pages` override in the assembler (a run that starts mid-Skandha needs to know which Skandha it is in), `add_bengali.py` reads the assembled format too.
2. **Mahatmya**, pages 26 to 77.
3. **Skandha 3**, chapters 12 to 33.
4. **Skandha 10**, then **11**, then **12**.
5. **Review pass** on flagged verses against the page images; update `content/bn/REVIEW.md`.

## Per batch (about 4 pages)
render pages, read each image, write the compact text, `ingest`, and every 20 to 40 pages run `assemble` and look at the report: missing verse numbers, extra numbers, low OCR agreement, `[?]`. Fix at the source page, not in the output.

## Done means
- `assemble` reports, per chapter, which verses are present against the Hindi verse numbers.
- `bn_verify` (letters of every source translation equal the database text; no markers, headings or Latin letters in the text; every pointer lands on a verse with text) passes on the shipped database.
- App builds, lint and tests pass; Bengali verified on the device for a chapter from each part.

## Risks
- Volume: about 1,100 pages is several million tokens of reading and writing. Work is resumable (a page is done when its JSON exists), so it can stop and continue at any page.
- Vision transcription of printed Bengali is good but not perfect: the pilot flagged 3 of 86 units. The OCR cross-check and verse-number check are what catch the rest, and anything below the thresholds stays flagged.
- Page ranges are estimates until each Skandha's first page has been seen.
- Alternative for the bulk: run the same tool with an Anthropic API key (about 5.8M input and 2.3M output tokens for the whole book); the ingest format and everything after it are identical.
