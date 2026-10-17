# Study and sharing features for the Bhagavatam app

Research note, 2026-10-18. It compares what other scripture and text-study apps do, then picks what fits this app: offline, no account, no tracking, five languages, one maker.

## 1. What the app already has
Bookmarks, four highlight colours, notes, saved words, dictionary with book word list, search (word, name, story, reference), narration, theme and language settings, backup file, report-a-mistake button. Sharing exists in two places only: the notes list as plain text (`SettingsWords.kt`) and a word card (`DictionaryScreen.kt`). A verse cannot be shared from the reader yet.

## 2. What other apps do (and what the sources say)
| App | Study strength | Sharing strength | Note |
|---|---|---|---|
| [Bhagavad Gita, Song of God (Mukundananda)](https://apps.apple.com/app/id1568483426) | Sanskrit, transliteration, word meanings, translation and commentary per verse; verse of the day; reading progress; offline | none found in the listing | closest to our content |
| [10 Minute Gita](https://apps.apple.com/us/app/6758332047) | text split into 239 daily readings of 10 minutes; audio with word-by-word highlight | sign-in sync | habit through small daily units |
| [Gitaamrit](https://marlvel.ai/api/llm/apps/books/com-milind-gitaamrit) | Hindi meaning, explanation and context per verse; daily streak, 7-day history | none found | streaks for Hindi readers |
| [YouVersion (Bible)](https://www.churchtrac.com/blog/the-best-bible-apps-2025) | hundreds of reading plans | groups, verse and note sharing | strongest for habit and groups, thin on depth per a vendor comparison |
| [Logos](https://www.logos.com/popular-bible-apps/leaders) | commentaries, lexicons, shared reading plan | group plan | deepest, paid, $10 to $16 a month |
| [Sefaria](https://designing-knowledge.siggraph.org/wp/?p=126) | every verse links to commentaries and other texts; side panel | source sheets: a page of chosen verses plus your own text, published or downloaded as PDF; study-partner video | the best model for sharing study, open data |
| [Remem.me](https://www.remem.me/spaced-repetition) | spaced-repetition verse memorising | n/a | cites Cepeda et al. 2006 for spacing |
| verse-image apps ([Verse](https://mwm.ai/apps/verse-bible-ai/6759293735), [Snap & Share](https://apps.apple.com/app/id6758765768)) | n/a | verse as a designed card for WhatsApp and social media; often a paid feature | people share pictures more than text |

Caveats: most sources are store listings, vendor blogs or marketing pages; no source measured whether any feature improves learning of Sanskrit shlokas. The search found nothing on Sanskrit memorising or Anki for verse, and nothing on whether share cards bring installs. Spaced repetition itself has a research base (Cepeda et al. 2006, a meta-analysis), but the "200% better" figure on an app page is not safe to quote.

## 3. Which features help study most (ranked for this app)
1. **Word by word reading** (Sanskrit word, transliteration, meaning, then translation). This is how the Gita apps teach and how a learner moves from reading a translation to reading the shloka. The dictionary and `book_word` tables are most of the data; what is missing is a per-verse word split with meanings, which needs a reviewed source.
2. **Cross-links and a side panel** (Sefaria idea): from a verse, open the same story in other Skandhas (the app already has story search), a character, a place, a theme. Cheap start: a hand-made list of 100 stories with verse ranges, which `Episodes.kt` already holds.
3. **Reading plans in small daily units** (10 Minute Gita, YouVersion): "Bhagavatam in a year" (about 40 verses a day), a Skandha at a time, a story a day. Progress and a gentle streak; keep it optional and local.
4. **Listen and read together** with the spoken word highlighted. Narration exists; the highlight of the sentence being read is the missing step.
5. **Memorise a verse** with spaced repetition: mark a verse, the app asks for it again after 1, 3, 7, 21 days, first line as a prompt, then reveal. Small, offline, no account. Value is real for shlokas, but unproven here, so ship it as a simple test mode first.
6. **Notes that stay useful**: tags (story, doubt, to ask), a "doubts" list that goes to a teacher, search inside notes, export.
7. **Quiet depth for later**: commentary (only if a licensed or public-domain one is found; the Gita Press edition is copyrighted).

## 4. How people can share with each other
Order by effort and by fit with "no account, no tracking":
1. **Share a verse** (hours): the verse in the reading language, the Sanskrit, the reference (for example `Bhagavatam 10.29.1`) and a link to the app's download page. Plain text first.
2. **Share a verse card** (days): a 1080 by 1080 image rendered from the same theme and a painting, for WhatsApp status. The research shows this is what apps in this space offer, often paid. Keep it free; add the small app name line at the bottom, no tracking.
3. **Share a study sheet** (Sefaria source sheet idea; a week): choose verses, add a note to each, export as PDF or as one text message. This is the best way for a teacher or a study circle to hand out a topic. No server needed: the PDF or text is the sheet.
4. **Share a file that the app opens** (a week): a small `.bhagavatam` file listing verse references and notes. Opening it in the app shows the sheet in the receiver's own reading language. It carries references only, not the copyrighted text, which also keeps the licence clean.
5. **Share a link that opens the verse** (a week plus a web page): `https://soujanyadasroy.github.io/Bhagavatam/v/10.29.1` opens the app at that verse if installed, otherwise the download page. Needs Android App Links and a static GitHub Pages file; still no account. Show only the reference on the web page, not the translation, because of the licence.
6. **Study circles with a live shared view** (not recommended now): accounts, a server, moderation and privacy duties. It breaks the "no account, no tracking" promise.

## 5. Constraints to keep
- **Licence:** the Gita Press text may not be republished by the maker. Share cards for personal use are normal; a public web page with the full text is not. References, the owner's own notes and public-domain art are safe.
- **Offline and private:** nothing leaves the phone unless the user taps share.
- **Text accuracy:** the Bengali and Odia text is machine-read ([audit](../audits/ALIGNMENT_2026-10-17.md)); put a small "machine-read, check the book" line on shared Odia cards until a reader has checked it.
- **Android specifics:** a text share is `ACTION_SEND` (already used for notes); an image share needs a `FileProvider` URI (already declared for updates); links need App Links verification.

## 6. Suggested order
1. Verse share (text) in the reader. 2. Verse card image. 3. Study sheet export (PDF or text). 4. Reading plans with a local streak. 5. Highlight the spoken sentence. 6. Memorise mode. 7. Reference links and a `.bhagavatam` sheet file. Decide after 1 to 3 whether people use them before building 4 to 7.

## Sources
[Song of God app](https://apps.apple.com/app/id1568483426), [10 Minute Gita](https://apps.apple.com/us/app/6758332047), [Gitaamrit](https://marlvel.ai/api/llm/apps/books/com-milind-gitaamrit), [Bible apps compared](https://www.churchtrac.com/blog/the-best-bible-apps-2025), [Logos](https://www.logos.com/popular-bible-apps/leaders), [Sefaria](https://designing-knowledge.siggraph.org/wp/?p=126), [Remem.me spaced repetition](https://www.remem.me/spaced-repetition), [Verse](https://mwm.ai/apps/verse-bible-ai/6759293735), [Snap and Share](https://apps.apple.com/app/id6758765768).
