# Odia transcription, Skandha 5 to 12: what you do, then the prompt for Antigravity

## Before you start (you, once)
1. **A Gemini API key.** Google AI Studio, "Get API key". The free tier is enough for the pilot; for the full run check the rate limits shown
   there (the tools are restartable, so a daily limit only means "continue tomorrow"). Do not put the key in a file in the project.
   In the Antigravity terminal: `$env:GEMINI_API_KEY = "<the key>"` (it lasts for that terminal session).
2. **The PDFs** are already at `C:\Users\sdroy\Downloads\Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-1.pdf` and `...Part-2.pdf`.
3. `pip install google-genai pymupdf pillow` once.
4. Open the project folder `Bhagavatam` in Antigravity. Model for the session: **Gemini 3.8 Flash (Medium)** is enough. The agent only
   runs scripts and reads reports; the reading of the pages is done by the model the script calls (Flash for pass A, Pro for pass B and arbitration).
   Do not choose a model marked "Leaving soon".
5. Time and size, as estimates: 1,437 pages, about 3,000 input and 1,600 output tokens per page and pass (4.3M in, 2.3M out per pass). Two passes
   plus arbitration. A pass at 3 workers is roughly 1 to 3 hours depending on limits.

## Free tier limit (found on 2026-10-12 in `work_v/errors.log`)
273 of 287 errors were `GenerateRequestsPerDayPerProjectPerModel-FreeTier`, limit 20: the free Gemini tier allows about **20 requests per model per day**.
That is 20 pages a day per model, so a skandha is out of reach on it (Skandha 6 alone is 110 pages per pass). The run so far got 131 pages by
switching between model names, which mixed models inside one pass (pass B is 112 of 119 pages `gemini-flash-lite`, not the stronger model the plan names).
- Real fix: enable billing on the AI Studio project (then the per-day cap no longer applies; check the per-minute limits and prices there),
  or use another key. Then pass A and pass B each get ONE model: `py odia_vision.py status` shows whether they are uniform.
- The tool now stops at once on a per-day cap (exit code 3, "N pages still to do") instead of retrying, and `status` shows progress per pass and model.
- Skandha 5 was read with mixed models and a weak pass B; once billing is on, re-read pass B for it (`--pass B --force --skandha 5 --model gemini-2.5-pro`).

## Skandha 6 only (110 pages, v1:696 to v1:805), after billing is on
```
cd content\_odia
$env:GEMINI_API_KEY = "<key>"; $env:ODIA_PDF_1 = "C:\Users\sdroy\Downloads\Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-1.pdf"
py odia_vision.py run --skandha 6 --pass A --model gemini-2.5-flash --workers 3
py odia_vision.py run --skandha 6 --pass B --model gemini-2.5-pro --workers 2
py odia_verify.py compare --skandha 6 --lexicon work_v\lexicon.txt
py odia_arbitrate.py run --skandha 6 --model gemini-2.5-pro
py odia_verify.py apply work_v\arbitration\decisions_model.json --source model
py odia_verify.py report
py odia_review.py queue --limit 300
py odia_vision.py status --skandha 6
```
The same commands with `--skandha 7`, `8` ... do the others, one skandha at a time.

## Your part during the run (the only human steps)
- **Review list** (after pass A, B and arbitration): open `content\_odia\work_v\review\queue.html`, answer with keys 1/2/3 or type the printed word,
  press "Download answers", put the file where the agent says. The list is the words nothing could settle; expect a few hundred to a few
  thousand words, which you can do in sittings of 50.
- **Audit** (once the list is done): open `work_v\audit\audit.html`, mark about 600 random accepted words correct or wrong. This is what tells us the accuracy.

## The prompt (paste everything below this line into Antigravity)

---

You are working on the Bhagavatam Android app project (folder open). Task: produce an accurate, checked Odia transcription of Skandha 5 to 12 of the
Gita Press Odia Shrimad Bhagavata Mahapurana (1,437 pages) using the tools that already exist in `content/_odia`, and report honestly.

## 0. Read first, in this order, fully
1. `AGENTS.md` (binding; rule 6 is the order of authority; section 9 is Odia work)
2. `docs/agent/ODIA_ACCURACY.md` (the method, the measured numbers, the acceptance rule)
3. `docs/agent/DECISIONS.md` and `docs/agent/HANDOFF.md`
4. `content/_odia/README.md` and the docstrings at the top of `odia_vision.py`, `odia_verify.py`, `odia_arbitrate.py`, `odia_review.py`, `odia_finalize.py`

## 1. Hard rules
- **You do not transcribe pages yourself.** No reading images and typing text, not even "just the difficult words". Every word of Odia text comes from
  the scripts, which call the model with the page image. If the API key is missing or the quota is exhausted: stop and tell the owner, do not work around it.
- Never print, log, echo or write the API key. Never write it into any file.
- Book text, page images and everything under `content/_odia/work_*`, `content/or`, `content/_odia/vision_transcripts` stays out of git
  (they are in `.gitignore`). Run `tools/agent/commit_check.sh` before any commit; commit only code and docs, by file name, never `git add -A`.
- Do not edit, revert or commit unrelated files. Do not push. No co-author lines. No em or en dashes in anything people read.
- Do not weaken a threshold, skip the audit, or change `odia_text.hygiene` / `SEVERE` to make numbers look better. If a check seems wrong, say why and ask.
- Never say "done" for a step you did not run. Quote the real numbers.

## 2. Preflight (report the results)
Run from `content\_odia` in PowerShell:
```
git status --short
py tests\test_verify.py                     # must print PASS
py make_skandha_pages.py                    # prints 1449 pages for skandha 5 to 12 (1437 distinct)
$env:ODIA_PDF_1 = "C:\Users\sdroy\Downloads\Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-1.pdf"
$env:ODIA_PDF_2 = "C:\Users\sdroy\Downloads\Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-2.pdf"
mkdir work_tess; tar -xzf page_json.tgz -C work_tess --strip-components=1     # Tesseract pages -> work_tess\vision
py -c "import os; print('key set' if os.environ.get('GEMINI_API_KEY') else 'NO KEY')"
```
If it prints `NO KEY`, stop here and ask the owner to set `GEMINI_API_KEY`.

## 3. Pilot (one chapter, both passes) - then stop and show the numbers
```
py odia_vision.py run --skandha 5 --pages 579-585 --pass A --workers 2
py odia_vision.py run --skandha 5 --pages 579-585 --pass B --model gemini-2.5-pro --workers 2
py odia_verify.py lexicon
py odia_verify.py compare --skandha 5 --lexicon work_v\lexicon.txt
py odia_verify.py report
```
Report: pages done, tokens used (`_usage` in the page JSON), words, share agreed by A and B, number of disputes, singles, hygiene flags and possible
omissions, and the 5 lowest-scoring pages. Then run `py odia_review.py queue --limit 20` and give the owner the path of `work_v\review\queue.html`, so they can see what a
dispute looks like. **Stop and wait for the owner's go-ahead** if disputes are above 8% of words, or any page failed.

## 4. Full run (restartable; a page is done when its JSON exists)
```
py odia_vision.py run --skandha 5-12 --pass A --workers 3        # -> work_v\vision
py odia_vision.py run --skandha 5-12 --pass B --model gemini-2.5-pro --workers 2    # -> work_v\vision_b
```
Check `work_v\errors.log` after each; re-run the same command until it reports 0 pages left. Do one skandha at a time if limits bite (`--skandha 5`, then `6` ...).

## 5. Compare, arbitrate, report
```
py odia_verify.py lexicon
py odia_verify.py compare --skandha 5-12 --lexicon work_v\lexicon.txt
py odia_verify.py report
py odia_arbitrate.py run --skandha 5-12 --model gemini-2.5-pro --workers 2
py odia_verify.py apply work_v\arbitration\decisions_model.json --source model
py odia_verify.py report
py odia_review.py queue --limit 400
```
Report per skandha: words, % agreed, % settled by Tesseract, disputes before and after arbitration, hygiene flags, open items left for a person.
Then **stop and tell the owner** the path of `work_v\review\queue.html` and how many words are waiting. The owner answers and saves
`decisions.json`; then:
```
py odia_verify.py apply work_v\review\decisions.json
py odia_verify.py report
```
Repeat `queue` / `apply` until the open list is empty or the owner says stop (the owner may also leave some words; they are listed in the report).

## 6. Audit and accept
```
py odia_verify.py audit-sample --n 600 --seed 7
py odia_review.py audit
```
The owner marks the words and saves `results.json`; then `py odia_verify.py audit-score work_v\audit\results.json`. Also, if arbitration settled more than 200 words:
`py odia_verify.py audit-sample --n 150 --seed 7 --stratum arbitrated` and the same steps. Apply the acceptance rule of `docs/agent/ODIA_ACCURACY.md`.
If the audit fails, do not continue to section 7: list the wrong words by letter pair, propose the fix, and ask.

## 7. Assemble and check
```
py odia_finalize.py --consensus work_v\consensus --out work_final --strip --max-unresolved 0
$env:ODIA_WORK="work_final"; $env:ODIA_OUT="work_final\out"; $env:ODIA_REVIEW="work_final\review"
$env:ODIA_METHOD="Vision model (two independent readings), word consensus, human review (odia_verify.py)"
py odia_assemble.py --starts data\starts_s5_12.json
```
Read `work_final\review\report.md`: every chapter 5.1 to 12.13 must be present with no missing or repeated verse. List the exceptions with what the print
shows. Do not copy anything to `content\or` and do not touch `content.db` until the owner has seen this report.

## 8. Hand-off
Write `docs/agent/ODIA_REPORT.md` (numbers only, no book text): per-skandha table (pages, words, agreed %, disputes, arbitrated, human answers,
audit error rate with its interval), chapters with problems, time and token use, what could not be checked, what the owner must decide next.
Update `docs/agent/HANDOFF.md`. Commit the code and docs you changed (not data), locally.

---

## After the report (not part of the prompt)
- Owner decision: promote to the app. That is `py content\_build\add_odia.py content\content.db` and the same for `app\src\main\assets\content.db`
  (both, same time: CLAUDE.md invariant 5), after copying `work_final\out\skandha-NN` to `content\or\skandha-NN`.
- The app has no Odia reading layer yet (the language switcher lists Odia as a draft). Showing Odia text needs app work: a new language,
  an Oriya font, and the switcher rows. Ask for that as a separate task once the text is accepted.
