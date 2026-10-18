# How the app can improve: functionality, UI and text (research, 2026-10-18)

## Method
- Today's feature audit (`docs/audits/FEATURE_AUDIT_2026-10-18.md`), the text-quality scan (`docs/audits/TEXT_QUALITY_2026-10-18.md`) and
  the UI review of Home and Content earlier today.
- A count of the app's own English interface strings (245 of them).
- What comparable scripture and reading apps offer. Store listings and product notes for Quran.com, YouVersion and several Bhagavad
  Gita apps. These are secondary sources (listings and reviews), so they are used for ideas, not for exact claims. Links at the end.
- Platform guidance: Android accessibility for Compose, and the Apple HIG pages already used for the UI review (as principles).

Every idea is checked against the project's own rules: offline first, no account, no tracking, nobody types scripture from page images,
both databases change together, no em dashes.

## What other apps do that people value
| Pattern | Seen in | Here today |
|---|---|---|
| Daily habit: verse of the day, reading plans, reminders | YouVersion (thousands of plans), Gita apps (daily verse widget) | Shloka of the day on Home; no plan, no reminder, no widget |
| Audio that follows the text, verse repeat, auto-advance, speed | Quran.com (repeat settings, auto-scroll, word tooltip), Gita apps | Narration with follow-along and speed; no repeat count, no A-B loop |
| Word by word under the verse | Quran.com, Gita apps (word meanings, sandhi, anvaya) | Hold a word for its meaning; no word-by-word line |
| Commentary next to the verse | Gita apps (Shankara, Ramanuja and others) | Gita Press notes in some chapters only |
| Compare translations side by side | YouVersion (several versions of one verse) | "Other translations" in the verse sheet |
| Highlights, notes, tags, share as an image | YouVersion, Quran apps | Highlights in 4 colours with names, notes, text share; no image |
| Offline download of audio | Gita and Quran apps | Narration is on-device TTS; no recorded recitation offline |

## 1. Functionality

### High value, fits the book
1. **Bhagavat Saptah plan.** The Bhagavatam is traditionally read in seven days, and the Mahatmya (chapter 6) describes the Saptah. A
   built-in 7-day plan (and a 30-day and a one-year plan) with the day's chapters, a tick per day and the place kept is the most
   specific feature this app can have. It also gives Home a reason to change each day. Offline: the plan is a small table of chapter
   ranges.
2. **Gentle reminder.** One local notification at a time the reader picks ("Today's reading: Skandha 3, chapters 4 to 6"). No server.
   It replaces the generic "allow notifications" prompt with one that has a reason, asked when the reader turns the plan on.
3. **Home-screen widget** with the shloka of the day (Jetpack Glance), in the reading language. Tapping it opens the verse.
4. **Repeat and memorise in audio.** Repeat a verse N times, loop a range, pause after each verse for the listener to recite. It pairs
   with the Memorise action and the review cards that already exist.
5. **Word by word for Sanskrit.** A collapsible line under the shloka: each word with its meaning from the book dictionary that is already
   in the app (`dictionary.db`). Holding a word does this today, one word at a time.
6. **Footnotes as notes.** The `*` in English titles and verses, and the Gita Press notes glued into Odia verses, are footnotes. Showing
   them as a small tappable mark with the note in a sheet fixes the text and adds content.
7. **Go to a reference from anywhere.** A "10.29.1" field on the Content page and in the chapter list, not only in Search.

### Medium
8. **Share a verse as an image**: the painting of the chapter, the shloka and the translation, with the reference. Made on the phone.
9. **Reading statistics without pressure**: days read this month, chapters this week. No streak counter that punishes a missed day.
10. **Recorded Sanskrit recitation** as optional download packs (the Downloads strings exist). Only with a source whose licence allows it.
11. **Tablet and landscape**: two panes (chapter list and reader) when the window is wide.
12. **Tags on notes** besides colour names, and a filter by tag in the notebook.

### Fix first (from today's audit)
- Word meanings must not go online by default (F1). Ask once, when the offline dictionary has nothing.
- Ask for notifications only when the reader turns on something that needs them: the reminder, or background playback (F2).
- Bundle Noto Serif Oriya and Noto Sans Oriya (OFL) so Odia conjuncts draw correctly on every phone (F4).

## 2. UI

### Reader
- **Bottom bar scrim**: text still shows faintly right above Listen and the arrows. A taller scrim or a solid bar with a top hairline.
- **One reading-settings sheet** ("Aa") with live preview: size, spacing, theme, script, what is shown. Today these are split between
  the sheet, Settings > Reading and Settings > Appearance.
- **Chapter end**: a clear "Next chapter" card with the next title and its painting, and "Mark as read" if the reader stops early.
- **Progress inside the chapter**: a thin line under the top bar (minutes left is already in the subtitle).

### Home and Content
- Done today: shloka of the day moved up, compact tiles, readable scrims, current Skandha marked, painted story cards.
- Next: hide the progress line until the first chapter is finished; show the Saptah day card when a plan is on; a "jump to verse" field
  on Content.

### Consistency
- **Language names**: "Bengali" in the app, "Bangla" in the search chips; one form everywhere, and the same choice for Odia
  (its own script, or the app language, not a mix).
- **Unlabelled controls**: 2 on Search and 5 in Settings are read by TalkBack as "button". Give each a label (`contentDescription` or
  `semantics { contentDescription }`), and merge rows (`mergeDescendants`) so a row is one stop.
- **Touch spacing**: the highlight colour targets are 48dp with 2dp between them; the project rule is 8dp.
- **Dark mode paintings**: dim the art slightly (multiply 85%) so it does not glare at night.
- **Motion**: the Home entrance and card reveals should follow the system "remove animations" setting (check `Motion` tokens use it).

## 3. Text

### Interface copy (245 English strings counted)
- **One word for each thing.** Today "verse" appears in 14 strings and "shloka" in 7, sometimes on the same screen ("Shloka of the
  day", "Bookmark this verse", "Verse numbers"). Pick "shloka" for the Sanskrit and "verse" for the numbered unit, or one word
  throughout, and write it down. The same for "chapter" and "adhyaya" (13 and 1), and "granth": 4 strings say "granth" or "Granth" while
  the tab is called "Content".
- **A short style guide**: sentence case for buttons and titles (the app mostly does this), no em dashes (project rule), numbers in the
  app language's digits, the book's names in the reading language (the rule written on 2026-10-14). Put it in `docs/DESIGN_SYSTEM.md`.
- **Odia interface**: written today without an Odia reader. One read-through by an Odia speaker, with the strings exported to a
  spreadsheet (English, Hindi, Bengali, Odia side by side), is the cheapest way to catch wrong words.

### Book text
| Issue | Size | Way forward |
|---|---|---|
| English titles with a footnote `*` and a lower-case start ("śaunaka ... Sūta*") | many titles | strip the mark at load, capitalise the first letter, keep the footnote as a note (item 6 above) |
| Bengali chapter titles that are only "Nth chapter" or empty | 28 (66 with the ordinal prefix) | the app now shows the Hindi title; the real Bengali titles are in the Gita Press table of contents and can be added from it |
| Odia chapters without a title | about 130 of 341 | same, from the Odia table of contents |
| Odia verses garbled and hidden | 7 | read the page again (local OCR or an Odia reader) |
| Odia verses far longer or shorter than the Hindi | 20 extreme, 141 to look at | split notes out of the verse; re-read the short ones |
| Bengali and Odia verses without any translation | 29 and 65 | listed in the README; same pipeline |
| Story descriptions without Odia | 16 | translate with the Odia interface strings |

## 4. Accessibility checklist for the next release
- Every icon-only control has a label; decorative paintings have none (`contentDescription = null`).
- Rows with a title and a value are one TalkBack stop.
- Snackbars and the "Preparing the voice" state are announced (live region).
- Run Android's Accessibility Scanner on Home, Content, the reader and Settings, and the language matrix at font scale 2.0.

## 5. Suggested order
1. F1 offline meanings, F2 notifications, Odia fonts, English title cleanup, labels and spacing. (Small, fixes broken promises.)
2. Terminology and the style guide, then the Odia string review.
3. Saptah plan with the optional reminder and the widget. (The feature that makes the app its own.)
4. Footnotes as notes, word by word, audio repeat.
5. Verse image sharing, statistics, tablet layout, recitation packs.

## Sources
- Quran.com product update on word-by-word and audio settings: https://quran.com/product-updates/simplifying-word-by-word-and-audio-settings
- Quran app listings: https://apps.apple.com/app/id1447074474, https://apps.apple.com/us/app/-/id6758482829, https://apps.microsoft.com/detail/9p1zd3k91xqt
- YouVersion listing and reviews: https://apps.apple.com/app/282935706, https://bibleinyear.com/blog/youversion-bible-app, https://biblebuyingguide.com/youversion-bible-app-significant-update/, https://learnofchrist.com/resources/compare/youversion-vs-bible-gateway-app
- Bhagavad Gita apps: https://apps.apple.com/app/id6760954797, https://apps.apple.com/app/id1568483426, https://apps.apple.com/us/app/id879311089, https://apps.apple.com/pt/app/id6779870441, https://apps.apple.com/app/id1143882659
- Compose accessibility: https://dev.to/carlosmonzon/jetpack-compose-accessibility-best-practices-38j0, https://www.freecodecamp.org/news/accessibility-in-jetpack-compose-comprehensive-tutorial/
- Odia fonts: https://fontsource.org/fonts/noto-serif-oriya/about, https://fontsource.org/fonts/noto-sans-oriya/about
