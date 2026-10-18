# Text quality scan, 2026-10-18

Made with `py tools/qa/scan_text_quality.py` and the rules in `app/.../data/TextQuality.kt`. Nothing in `content.db` was changed; the app now hides what is clearly broken and says so.

## Odia verses
The scan went over 13402 Odia verses.

| Finding | Verses | What the app does |
|---|---|---|
| Page symbols, signs Odia never uses, or mostly doubtful (`[?]`) words: the page was not read | 11: 0.1.1, 0.6.3, 0.6.30, 3.6.34, 3.12.44, 6.1.1, 6.2.24, 7.1.1, 9.23.6, 10.50.1, 10.84.51 | Shows "Odia translation not available" and the verse stays in the other languages |
| Odia far **longer** than the Hindi of the same verse (over 3 times): the verse probably took in the next one | 68 | Shown as it is, listed here for an Odia reader to check against the book |
| Odia far **shorter** than the Hindi (under 0.4 times): part of the verse is missing | 22 | Shown as it is, listed here |

Longer: 0.1.38, 0.5.39, 1.1.2, 1.2.9, 1.3.27, 1.4.27, 1.6.21, 1.7.34, 1.7.43, 3.18.3, 3.20.25, 3.28.19, 4.1.53, 4.14.7, 4.14.36, 4.27.23, 5.2.5, 5.7.2, 5.8.5, 5.8.28, 5.13.13, 5.14.35, 5.15.8, 5.16.6, 5.21.14, 5.22.6, 5.24.23, 6.2.5, 6.2.25, 6.6.10, 6.7.29, 6.9.53, 7.5.8, 7.5.25, 8.9.25, 8.12.42, 9.13.7, 9.20.20, 9.24.9, 9.24.32, 10.3.2, 10.3.5, 10.3.6, 10.6.10, 10.8.5, 10.8.27, 10.8.32, 10.8.33, 10.9.5, 10.9.7, 10.9.17, 10.9.19, 10.22.28, 10.34.1, 10.34.7, 10.43.13, 10.43.18, 10.50.1, 10.55.5, 10.85.11, 11.6.25, 11.7.64, 11.8.2, 11.9.5, 11.11.33, 11.19.29, 11.21.24, 11.26.26

Shorter: 0.6.3, 1.1.1, 2.10.42, 3.30.16, 4.14.46, 4.17.36, 5.18.1, 5.19.3, 7.9.1, 9.1.42, 9.5.27, 9.22.49, 10.38.1, 10.39.57, 10.44.4, 10.44.5, 10.45.45, 10.51.64, 11.14.7, 11.22.8, 11.28.35, 12.13.23

## Chapter titles
Hindi: no placeholders. Odia: only chapters with a real Odia title have one.

Bengali: 66 titles are empty, only "Nth chapter", have the ordinal in front, or sit in brackets. The app now drops the ordinal and, when nothing is left, shows the Hindi title (then English).

| Chapter | Problem | Stored text |
|---|---|---|
| 2.2 | empty |  |
| 4.21 | empty |  |
| 9.10 | empty |  |
| 10.10 | empty |  |
| 2.1 | only the number | প্রথম অধ্যায় |
| 2.3 | only the number | তৃতীয় অধ্যায় |
| 2.4 | only the number | চতুর্থ অধ্যায় |
| 2.5 | only the number | পঞ্চম অধ্যায় |
| 3.3 | only the number | (দ্বিতীয় অধ্যায়) |
| 3.5 | only the number | পঞ্চম অধ্যায় |
| 3.8 | number in front | অষ্টম অধ্যায় ব্রহ্মার উৎপত্তি |
| 4.4 | only the number | চতুর্থ অধ্যায় |
| 4.5 | number in front | পঞ্চম অধ্যায় বীরভদ্র কর্তৃক দক্ষযজ্ঞ ধ্বংস এবং দক্ষবধ |
| 4.13 | only the number | ত্রয়োদশ অধ্যায় |
| 4.14 | only the number | চতুর্দশ অধ্যায় |
| 5.2 | only the number | দ্বিতীয় অধ্যায় |
| 5.4 | only the number | চতুর্থ অধ্যায় |
| 5.5 | only the number | পঞ্চম অধ্যায় |
| 5.6 | only the number | ষষ্ঠ অধ্যায় |
| 6.1 | only the number | প্রথম অধ্যায় |
| 6.2 | only the number | দ্বিতীয় অধ্যায় |
| 6.3 | only the number | তৃতীয় অধ্যায় |
| 6.4 | only the number | চতুর্থ অধ্যায় |
| 6.5 | only the number | পঞ্চম অধ্যায় |
| 6.6 | only the number | ষষ্ঠ অধ্যায় |
| 6.7 | only the number | সপ্তম অধ্যায় |
| 6.9 | only the number | নবম অধ্যায় |
| 6.10 | only the number | দশম অধ্যায় |
| 6.13 | only the number | ত্রয়োদশ অধ্যায় |
| 6.14 | only the number | চতুর্দশ অধ্যায় |
| 6.15 | only the number | পঞ্চদশ অধ্যায় |
| 7.3 | only the number | তৃতীয় অধ্যায় |
| 7.4 | only the number | চতুর্থ অধ্যায় |
| 7.8 | only the number | অষ্টম অধ্যায় |
| 7.11 | only the number | একাদশ অধ্যায় |
| 7.12 | only the number | দ্বাদশ অধ্যায় |
| 7.14 | only the number | চতুর্দশ অধ্যায় |
| 7.15 | only the number | (পঞ্চদশ অধ্যায়) |
| 8.2 | only the number | দ্বিতীয় অধ্যায় |
| 8.3 | only the number | তৃতীয় অধ্যায় |
| 8.4 | only the number | চতুর্থ অধ্যায় |
| 8.8 | only the number | অষ্টম অধ্যায় |
| 8.13 | only the number | ত্রয়োদশ অধ্যায় |
| 8.14 | only the number | চতুর্দশ অধ্যায় |
| 8.15 | only the number | পঞ্চদশ অধ্যায় |
| 9.1 | only the number | প্রথম অধ্যায় |
| 9.2 | only the number | দ্বিতীয় অধ্যায় |
| 9.3 | only the number | তৃতীয় অধ্যায় |
| 9.5 | only the number | পঞ্চম অধ্যায় |
| 9.8 | only the number | অষ্টম অধ্যায় |
| 9.9 | only the number | নবম অধ্যায় |
| 9.12 | only the number | দ্বাদশ অধ্যায় |
| 9.14 | only the number | চতুর্দশ অধ্যায় |
| 10.9 | only the number | নবম অধ্যায় |
| 10.12 | only the number | দ্বাদশ অধ্যায় |
| 10.14 | only the number | চতুর্দশ অধ্যায় |
| 11.1 | only the number | প্রথম অধ্যায় |
| 11.2 | only the number | দ্বিতীয় অধ্যায় |
| 11.8 | only the number | অষ্টম অধ্যায় |
| 11.11 | only the number | (পঞ্চম অধ্যায়) |
| 11.13 | only the number | ত্রয়োদশ অধ্যায় |
| 11.15 | only the number | পঞ্চদশ অধ্যায় |
| 12.4 | only the number | চতুর্থ অধ্যায় |
| 12.5 | number in front | পঞ্চম অধ্যায় শ্রীশুকদেবের অন্তিম উপদেশ |
| 12.12 | only the number | (চতুর্থ অধ্যায়) |
| 12.13 | only the number | ত্রয়োদশ অধ্যায় |


---

# Odia verses that need a person, 2026-10-18 (second pass)

Group-aware scan: a translation printed once for a group of verses is compared with the Hindi of the whole group (verses whose `or_from` points to it).
This removed 39 of the 68 earlier "too long" cases: the following verses already point to the first, so the group was right all along.

Rule of this project: nobody types scripture from page images. The list below is for an Odia reader or for the local OCR re-run
(`content/_localocr/`), then `content/_build/add_odia.py` on both `content.db` copies.

## Garbled, hidden in the app since db9a63e
0.1.1, 0.6.3, 0.6.30, 3.6.34, 3.12.44, 6.1.1, 6.2.24, 7.1.1, 9.23.6, 10.50.1, 10.84.51

Each shows "Odia translation not available" and the verse stays readable in the other languages. A re-read of the printed page is needed.

## Extreme length cases (ratio of Odia to the Hindi of its group above 6 or below 0.2)
| Verse | Odia / Hindi | Odia letters | Hindi letters | Paragraphs | `*` | Likely cause |
|---|---|---|---|---|---|---|
| 0.6.3 | 0.17 | 19 | 110 | 1 | 0 | only a fragment of the verse was read |
| 1.1.1 | 0.03 | 15 | 558 | 1 | 0 | only a fragment of the verse was read |
| 5.19.3 | 0.18 | 43 | 233 | 2 | 0 | only a fragment of the verse was read |
| 6.2.5 | 8.05 | 958 | 119 | 2 | 0 | text of the next verses glued in |
| 6.2.25 | 26.78 | 3080 | 115 | 18 | 2 | notes or commentary glued in |
| 7.9.1 | 0.1 | 12 | 124 | 1 | 0 | only a fragment of the verse was read |
| 9.10.37 | 0.19 | 521 | 2676 | 1 | 0 | only a fragment of the verse was read |
| 10.3.2 | 9.79 | 1292 | 132 | 2 | 0 | text of the next verses glued in |
| 10.3.5 | 7.56 | 1141 | 151 | 6 | 0 | text of the next verses glued in |
| 10.3.6 | 8.77 | 1017 | 116 | 4 | 0 | text of the next verses glued in |
| 10.8.5 | 12.01 | 1874 | 156 | 16 | 1 | notes or commentary glued in |
| 10.8.27 | 7.8 | 1428 | 183 | 6 | 0 | text of the next verses glued in |
| 10.8.32 | 97.59 | 9564 | 98 | 65 | 2 | notes or commentary glued in |
| 10.8.33 | 7.39 | 739 | 100 | 4 | 1 | text of the next verses glued in |
| 10.9.7 | 15.51 | 2667 | 172 | 8 | 1 | text of the next verses glued in |
| 10.9.17 | 7.87 | 1189 | 151 | 3 | 3 | notes or commentary glued in |
| 10.22.28 | 96.87 | 16661 | 172 | 26 | 1 | notes or commentary glued in |
| 10.34.1 | 171.76 | 26279 | 153 | 48 | 0 | notes or commentary glued in |
| 10.38.1 | 0.11 | 13 | 122 | 1 | 0 | only a fragment of the verse was read |
| 11.10.14 | 0.2 | 724 | 3648 | 2 | 0 | only a fragment of the verse was read |

How to read the "likely cause" column: many paragraphs or several `*` signs mean the printed notes (Gita Press footnotes) were read into the verse; few paragraphs mean the next verses' text ran in; a very short text means only a line of the verse was read.


## Update, same day: 4 of the 11 garbled verses recovered
In 0.1.1, 6.1.1, 7.1.1 and 10.50.1 the garbled part was the decorated chapter-opening block of the page, read as letters, in front of
the real translation. Dropping the paragraphs that are not Odia text (and a short heading piece left before the verse) leaves a
translation whose length matches the Hindi (0.9 to 1.2 times). Nothing was typed; the remaining paragraphs are the machine reading
as it was. Both `content.db` copies changed together, content_version 41, search index rebuilt.

Still hidden as garbled: 0.6.3, 0.6.30, 3.6.34, 3.12.44, 6.2.24, 9.23.6, 10.84.51. Their whole text is unreadable, or what is left is a
fragment (0.6.30 keeps one line, 6.2.24 three words). They need the printed page read again.

Re-run of `py tools/qa/scan_text_quality.py` after this change: 141 Odia verses listed for a person to look at (group-aware length,
ratio outside 0.4 to 3.5, vowel signs at the start of a word, odd signs), 0 Hindi titles, 28 Bengali titles, 0 Odia titles.
