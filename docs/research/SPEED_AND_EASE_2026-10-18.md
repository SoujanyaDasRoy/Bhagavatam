# Speed and ease of use: research (2026-10-18)

Measured on the Pixel 8 emulator (release build 1.2.9, 40.7 MB APK) and read in the code. The emulator draws frames in
software: Android's own Settings app scrolls there with the same frame times as this app (median 44 ms, 70% janky), so
frame timings are not used as evidence; start-up, memory, database work and image work are.

## What was measured
| Measure | Value | Note |
|---|---|---|
| First frame (splash), cold start | 1.06 to 1.14 s | `am start -W` TotalTime, three runs |
| Home visible, cold start | about 2 to 3 s after launch | filmstrip; the splash waits for three databases |
| First launch after install | language screen after about 4.5 s | includes copying 94 MB of databases out of the APK |
| Native memory with Home open | 136 MB (PSS 155 MB) | mostly decoded paintings |
| Paintings shipped | 13 at 1920 x 1200 | each decodes to 9.2 MB in memory; `IMAGE_SPECS.md` asks for 1280 x 800 |
| Start-up query "chapters with Bengali" | 159 ms on a desktop SSD, warm | full scan of the 56 MB verse table; on a phone likely 0.5 to 2 s |
| Dictionary prefix search | 17 ms per keystroke on the desktop | `LIKE 'x%'` sorts in a temp B-tree; about 5 to 10 times slower on a phone |

## Findings, by effect on the person using the app
1. **Start-up waits for work it does not need.** The splash stays until `content.db`, `search.db` and `dictionary.db` are
   all open (`MainActivity`). Only the text is needed for Home. Opening the text also scans the whole verse table to
   learn which chapters have Bengali (`SampleData.attach`), although every chapter has it now.
2. **94 MB is copied again after every app update**, even when the text did not change: `ContentDb` and `DictionaryDb`
   copy when the app's version code changes, `SearchIndex` when the content version changes. It slows the first start
   after each update and wears the phone's storage.
3. **Paintings are decoded at full size on the main thread.** `painterResource` decodes a 1920 x 1200 WebP synchronously
   while the screen is being composed; Home shows several (story cards, resume card), the reader one, the player one.
   This is the 136 MB of native memory and the most likely cause of stutter on a real mid-range phone. With 341
   chapter paintings coming, the size also matters for the APK: about 300 KB each now, about 120 KB at 1280 x 800.
4. **Opening a chapter reads it on the main thread** (`Reader.kt`: `remember { SampleData.versesFor(...) }`), with the
   per-verse clean-up (titles, dashes, doubt marks, garbled-Odia check). Swiping to the next chapter does the same; the
   neighbours are not read ahead.
5. **No start-up profile for the app's own code.** `profileinstaller` is in, but there is no `baseline-prof.txt`, so the
   app's classes are interpreted or JIT-compiled on every early run.
6. **Data classes shown in lists are not stable for Compose** (`Verse` holds `List` and `Map`), so rows can recompose more
   often than needed.
7. **Ease of use**
   - Jumping to a verse ("10.29.1") is only possible from Search; the Content tab has no such field.
   - A new install starts with three bookmarks the reader never made (`AppState`: 1.3.28, 10.29.1, 1.1.3), so Home says
     "Saved 3".
   - Typing in the dictionary does a slower query than needed on every pause in typing.

## Not changed by this work
The 16:10 artwork format, the database schema (one meta key is added), the offline promise, the language rule.
