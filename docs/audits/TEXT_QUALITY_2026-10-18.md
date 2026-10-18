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
