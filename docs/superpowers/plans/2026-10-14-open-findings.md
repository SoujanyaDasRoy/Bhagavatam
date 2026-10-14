# Open findings: plan to close all of them

**Goal:** close every item left open in `docs/agent/LANGUAGE_AUDIT_2026-10-14.md` and `docs/agent/UI_AUDIT_2026-10-14.md` ("Not done" rows), then ship 1.2.2.

**Spec:** the two audit files. **Order:** cheap and certain first, then items that need the phone or a person.

## Global constraints
- Indic text at least 15 sp; touch targets 48 dp; no em or en dashes in readable text; no co-author lines; commit per task, push at the end of each group.
- App text in Hindi and Bengali is mine and unreviewed until the owner or a reader checks it; list new strings in the release notes.
- Nothing is changed in `content.db` except by the Odia pipeline in task 6.

## Tasks

### 1. Fallback label for text shown in another language (language plan task 3)
- **Closes:** Language audit D (Odia chapter without an Odia title shows another language silently), and the same for Bengali verses that fall back to Hindi.
- [ ] Add `AppState.fallbackNote(zone, shownLang)`: returns the language name in its own script when the text shown is not the reading language.
- [ ] Reader: a small line under the chapter title ("Title shown in Hindi") when `contentLang` differs from the reading language, and the existing "No translation" text names the language it falls back to.
- [ ] Unit test in `LangZoneTest`: Odia reading + Hindi app gives the Hindi note; Hindi reading gives none.
- [ ] Commit `ui: say which language a title or verse is shown in when it is not the reading language`.

### 2. Settings values and notification explanation (Language audit B, C)
- [ ] Translate the remaining Settings list values ("Online", "Offline", "100%", "1.0x") through `tr(...)`; grep `ui/screens/Settings*.kt` for string literals passed as value.
- [ ] Before the first notification permission request, show an in-app sentence in the app language ("To control playback from the lock screen, allow notifications") with Continue and Not now; request only after Continue. Android's own dialog stays in the system language.
- [ ] Commit `ui: Settings values in the app language; explain the notification permission first`.

### 3. Odia heading cleaned out of verse 1 (Language audit A)
- [ ] In `content/_odia/odia_assemble.py`, when a chapter's first unit begins with text that matches the page's `chapter_title` or boxed heading (edit distance under 30%), cut it and store it as the chapter title candidate instead.
- [ ] Re-assemble Skandha 6 to 12 and the early pages (the assembled `content/or` files are git-ignored); report how many verses lost a heading.
- [ ] Rebuild `content.db` (both copies) with `add_odia.py`, bump `content_version`, rebuild `search.db`.
- [ ] Spot-check 10.3, 8.14 and 12.12 on the emulator.
- [ ] Commit `odia: chapter headings are not part of verse 1`.

### 4. Odia titles (Language audit D, root cause)
- [ ] Show the Odia chapter title only when it came from the printed contents list, not from the boxed heading: extract the contents pages of both volumes with the local readers, keep titles whose word error is low (A = B).
- [ ] Until that exists, titles stay on the fallback and task 1's note makes it visible.
- [ ] Commit `odia: chapter titles from the printed contents` (only if the extraction passes the same A/B agreement rule).

### 5. Remaining small type and unified capitalisation (UI audit High, Low)
- [ ] Raise the roughly 90 Latin 12 to 14 sp labels that sit inside Hindi or Bengali rows to 15 sp (script check on the surrounding strings, same patch tool as before); keep 12 to 13 sp only for licence and version lines.
- [ ] Sentence case everywhere: list the title-case strings with a grep of `Strings.kt` and `SettingsText.kt` (capital letter after a space inside a short label), change them, and skip proper names.
- [ ] Extend `ContrastTest.noTinyText` to fail under 12 sp and add an allow-list file for the licence lines.
- [ ] Commit `ui: remaining small text and sentence case`.

### 6. TalkBack and spoken order (Language audit F, UI audit Medium)
Needs the phone.
- [ ] Turn TalkBack on; walk Home, Content, chapter (read, bookmark, long press, red underline popup), search, dictionary, Settings, player, in English and Hindi.
- [ ] Record each wrong or missing announcement in `docs/agent/TALKBACK_2026-10-14.md`.
- [ ] Fix with `semantics { contentDescription, role, heading() }` on the offending composables; group the verse number and text into one node.
- [ ] Commit per screen group.

### 7. Reduced motion and spacing on a device (UI audit Medium)
Needs the phone.
- [ ] `settings put global animator_duration_scale 0`: splash must show the final frame, voice pulse static, no stagger; restore to 1.
- [ ] Measure the gaps between the four word-card buttons from a `uiautomator dump`; widen to 8 dp where under.
- [ ] Commit `ui: reduced motion and button spacing verified`.

### 8. Voice navigator and Odia online meanings (Language audit F)
- [ ] Voice navigator: run it in Hindi and Bengali on the phone with three spoken chapter names each; record hit rate; fix the matcher's language list if it only knows English.
- [ ] Odia meaning lookup: for words the offline dictionary lacks, the card says "No Odia entry yet" in the app language instead of staying empty; the Wiktionary online lookup is tried with the `or` section first (it already is in `OnlineMeaning.preferred`).
- [ ] Commit `search, dictionary: Hindi and Bengali voice navigation checked; clear message for Odia words with no entry`.

### 9. Release 1.2.2
- [ ] `gradlew test`, `testDebugUnitTest`, search tests, language matrix at 1.0 and 2.0, `lang_audit.py` for all five reading languages.
- [ ] `versionName 1.2.2`, `versionCode` +1, `assembleRelease`, verify signature fingerprint `CE:64:E1:26`.
- [ ] `gh release create v1.2.2` with `Bhagavatam.apk` and notes that list new Hindi and Bengali strings as unreviewed.
- [ ] On the phone: the update card appears, install, notes and position stay.

## Order and effort
1 and 2 (1.5 hours), 5 (1 hour), 3 and 4 (2 hours, mostly pipeline runs), then the phone session for 6, 7 and 8 (about 1 hour with the owner's phone), then 9.

## Risks
- Task 3 changes verse text boundaries; keep the old assembled files and diff the verse counts per chapter before rebuilding the databases.
- Task 4 may yield few reliable titles; the fallback note (task 1) already covers the gap.
- The notification explanation must not delay playback: Not now continues with the player and keeps text only.

## Self-review
Every "not fixed" row in the language audit (A to F) and every "not closed" row in the UI audit (small text, capitalisation, TalkBack, reduced motion, spacing) has a task: A is 3, B and C are 2, D is 1 and 4, E is not ours (system dialogs), F is 6 and 8; small text and capitalisation are 5; TalkBack is 6; motion and spacing are 7.
