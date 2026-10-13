# Local OCR: state, numbers, how to resume (2026-10-13)

Plan: `docs/superpowers/plans/2026-10-13-local-ocr.md`. Code: `content/_localocr/`. Everything under `content/_localocr/work/` is derived from the printed book and is git-ignored.

## Numbers (all measured; method in brackets)
| Step | Result |
|---|---|
| Stock Tesseract (ori) on Skandha 1 to 4 | 25% of words wrong (against the vision transcript) |
| P0 answer-key audit (Claude reading the page images, 23 words) | 2 wrong: the transcript is not clean enough to train on unfiltered |
| P1 Odia line pairs | 17,611 lines cut, 14,957 kept (similarity to the stock reading at least 0.9, no impossible spelling, characters known). 9,972 train (v1:88-410), 4,985 held out (v1:411-578, Skandha 4). 50-line hand check: 46 exact, 4 found with slips (3 punctuation, 1 wa/ba spelling); both causes fixed and the set rebuilt |
| P2 recogniser (CNN+BiLSTM+CTC, 6M parameters, trained on the RTX 3050 in 30 minutes) | held-out Skandha 4: character error 0.38%, word error 1.54% (31,601 words). Measured against the transcript, so it also contains the transcript's own errors |
| P2 independent check (Claude, 26 random Skandha 6 words against the page) | 0 wrong, 95% upper bound 12.9%. One error in a neighbouring word was seen while checking (`ମୁ` read as `ରୁ`), so the word error is not zero |
| P3 Skandha 6 | 110 pages read locally in 450 s; assembled into 20 chapters, 784 units, 116 flagged; chapters 6.10, 6.13, 6.17 have no flag; verses with no unit in 6.3, 6.4, 6.11, 6.12 |
| P4 word-list fixer by one-edit lookup | REJECTED: 363 changes, 5.0% right, made the text worse (1.70% to 3.70%) |
| P4 likelihood rescoring with the Skandha 1 to 3 word list (penalty 4) | word mismatch 1.89% to 1.65% on 800 held-out lines (one model) |
| P4 Bengali | 53,947 lines cut from both volumes, 41,600 kept (33,451 train, 8,149 held out); recogniser NOT yet trained |

## Not done (honest list)
- Skandha 7 to 12 reading is started but slow while the GPU is shared; check `work/read_s7_12.log` and `ls work/pages_a/vision | wc -l` (target 1,449 pages incl. Skandha 6).
- Model B (second seed) training; consensus of two readings; arbitration; human or page-image audit of 600 words.
- Bengali recogniser training and the Bengali repair pass.
- Odia text is not in `content.db` and not in the app.
- A Tesseract fine-tune was tried first and abandoned: the Windows `lstmeval`/`lstmtraining` cannot load more than one `.lstmf` file from a list.

## Commands (run from the repository root; GPU has 4 GB, run one training or read job at a time)
```
py content/_localocr/make_lines.py --all --workers 5          # Odia lines (add --lang bn --vol 1|2 for Bengali)
py content/_localocr/prepare_training.py --min-sim 0.9        # (LOCALOCR_LANG=bn for Bengali)
py content/_localocr/crnn.py train --epochs 30 --name crnn.pt # (LOCALOCR_LANG=bn, --name crnn_bn.pt)
py content/_localocr/crnn.py eval
py content/_localocr/read_pages.py --skandha 7-12 --model crnn.pt --out content/_localocr/work/pages_a
py content/_localocr/read_pages.py --skandha 7-12 --model crnn_b.pt --out content/_localocr/work/pages_b
py content/_odia/odia_verify.py compare --a content/_localocr/work/pages_a/vision --b content/_localocr/work/pages_b/vision --t <tesseract pages>
```
Assemble with `content/_odia/odia_assemble.py --starts data/starts_s5_12_dense.json` (env ODIA_WORK, ODIA_OUT, ODIA_REVIEW), as in `docs/agent/ODIA_PROMPT.md` section 7.
