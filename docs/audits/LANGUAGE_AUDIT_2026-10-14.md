# Language and feature audit, 2026-10-14

Method: `tools/qa/lang_audit.py` sets the app and reading language, walks Home, Content, Search, Settings and every Settings row, a chapter and its language sheet, and reads each screen with `uiautomator dump`; it lists Latin text in a Hindi or Bengali app and unlabelled tap targets. Run on the emulator for app/reading: HI/HI, BN/BN, HI/OR, BN/EN. Plus source greps for places that handle Hindi, Bengali and English but not Odia.

## Bugs found and fixed (commit with this file)
| # | Bug | Fix |
|---|---|---|
| 1 | **Listening to an Odia chapter read the English text.** `NarrationText.plan` and the probe only knew Hindi, Bengali and English. | Odia text is used; with no Odia voice the player says so ("no voice, text only", checked on the emulator in a Hindi app). |
| 2 | Odia joint verses: a translation printed with an earlier verse was not merged into its row label (English and Bengali were). | `orFrom` used like `enFrom` and `bnFrom`. |
| 3 | Reading in Odia showed chapter and Skandha titles, stories and search results in Hindi or English by accident of fallback, with the Odia font. | One `AppState.contentLang` (reading language, or the app language when Odia is read) used for every title and story name. |
| 4 | Hindi and Bengali apps showed **English search suggestions** ("Krishna", "Gajendra", "Rasa Lila", "Dhruva", "Prahlada"). | Suggestions in Devanagari and Bengali script. |
| 5 | Settings > Languages named the app languages in English under Hindi and Bengali apps ("Hindi", "Bengali"). | Names come from the app-language table (`nameIn`). |
| 6 | Dictionary names and terms showed the English term and English meaning in Hindi and Bengali apps. | Name in the app's script, meanings written in Hindi and Bengali (unreviewed). |
| 7 | The four highlight colours in Words settings had no spoken name. | Named in all three app languages. |

## Found, not fixed
| # | Finding | Why |
|---|---|---|
| A | Odia chapter headings read from the scan are garbled and appear as the first line of verse 1 (for example 10.3 starts with a damaged heading). | Part of the unreviewed machine-read text, not the app. Needs the heading cut from the verse in the Odia pipeline. |
| B | "(first-run) notifications permission" is asked the first time Listen is pressed on Android 13+, in the system's language, not the app's. | Android system dialog. The app could explain why before asking. |
| C | `settings` Words > "Highlight colour" swatches are 48 dp but the preview row text is 12 sp Latin in a Hindi app (English "Online" value). | Minor; value strings "Online", "100%" are not translated in the Settings list (see Settings.kt entries). |
| D | The Home "Shloka of the day" and the Odia reading show a Hindi/English title when the chapter has no Odia title (64 chapters). By design (fallback order), but the person is not told. | Needs the fallback label from the language-rule plan (task 3). |
| E | Tap targets with no label remain in the system file picker and notification dialog (not the app). | Not ours. |
| F | Feature not covered: TalkBack order, voice navigator in Hindi and Bengali, online meaning lookup for Odia words (Wiktionary has `or` entries only for 1,456 words). | Needs the phone and a person. |

## Runs
- HI/HI: Latin text only in search suggestions (fixed), language names (fixed) and dictionary terms (fixed).
- BN/BN: none.
- HI/OR: Latin story titles on Home and Content before fix 3; unlabelled targets only in system dialogs.
- BN/EN: all English strings are the reading text itself, as intended.
