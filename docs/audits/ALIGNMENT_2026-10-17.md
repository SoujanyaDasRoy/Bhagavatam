# Bengali and Odia verse alignment, 2026-10-17

While filling the last missing verses, the translations were checked against the Hindi text of each verse. `tools/content/check_alignment.py` repeats the checks.

## What was wrong
- **Misnumbered Odia units.** When the extractor could not read a verse marker it numbered the unit from the previous one (`marker read as 27, set to 41 from the sequence`). One early slip shifted every later verse of the chapter. Relabelled from the number the page actually showed: Skandha 6 chapter 1 (verses 27-68), Mahatmya 6, Skandha 5 chapter 11, 6.2, 9.8, 9.11, 10.8, 10.16 and 10.25. The Hindi length fit of these chapters went from 0.79-0.97 to 0.92-1.00.
- **Another chapter's text under a verse.** Bengali 2.1 (verses 24-39 held Skandha 3 text), Bengali 3.3 (verses 8-28 held the Skandha 3 chapter 2 text), Odia 6.1 (verses 37-40 held the end of 5.26). Rebuilt from the page transcriptions.
- **A page paragraph copied under several verses.** 72 of the verses added by the gap-filling scripts in 1.2.4 to 1.2.6 were copies of a neighbouring verse. The independent Tesseract check only proved the words were on the page, not that they belonged to that verse. All 72 were removed again.
- **A neighbour's text in front of a verse** (Bengali 9.24, 11.5, 3.9, Odia 10.31): split at the sentence end.

## Result
Bengali: 29 verses without a translation. Odia: 65. Both lists are in the README. The earlier figures (2 and 18) counted the copies as translations.

## Still to look at
`check_alignment.py` flags verses whose length is far from the Hindi (215 Bengali, 68 Odia). Most are verses printed jointly or with long commentary, but they have not been checked one by one. Four Bengali chapters (9.10, 10.10, 11.11, 12.12) hold whole paragraphs under each verse number in the source and need a separate look.

## Rule for the next fill
A verse is filled only from a page transcription that carries its own printed marker, and only when it passes `check_alignment.py` (no copy of a neighbour, length within 0.4 to 3.5 of the Hindi).
