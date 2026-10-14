# UI audit fixes: implementation plan

> **For agentic workers:** execute task by task. Steps use checkboxes. One commit per task, no co-author lines, no em dashes in readable text.

**Goal:** close the High and Medium findings of `docs/agent/UI_AUDIT_2026-10-14.md` without changing the look of the app.

**Architecture:** one type scale object and one touch-target modifier replace hand-typed sizes; the font-scale cap and reduced-motion check become small helpers used in the two places that need them.

**Tech stack:** Kotlin, Jetpack Compose, existing `ui/theme` tokens, emulator and the Samsung SM-A556E for checks.

**Spec:** `docs/agent/UI_AUDIT_2026-10-14.md` (findings), `CLAUDE.md` section 5 (rules).

## Global constraints
- Indic text at or above 15 sp with line height 1.8; Latin metadata at or above 12 sp; nothing below 11 sp (Apple floor).
- Touch targets at least 48 dp, 8 dp apart.
- Text colours at least 4.5:1, icons and large text at least 3:1.
- System font scale up to 200% must not cut text off or hide a control.
- No change to stored data or to the content databases.

## File map
| File | Change |
|---|---|
| `ui/theme/Type.kt` | add `AppType` scale: `caption 12`, `meta 13`, `body 15`, `indic 16`, ... |
| `ui/components/Touch.kt` (new) | `Modifier.minTouch()` and `TouchIconButton` (48 dp area, icon size unchanged) |
| `ui/components/Bars.kt` | uncap font scale; tab bar and mini player grow with the text |
| `ui/screens/Splash.kt`, `VoiceNavigatorSheet.kt` | skip infinite and staggered animation when animations are off |
| `ui/components/Art.kt` | contrast check hook used by tests and `skGradient` |
| `ui/screens/Library.kt` | one dark hero (resume), quiet cards; mandala off tiles (optional, last) |
| `ui/screens/{Player,DictionaryScreen,AnnotationSheet,Reader,Me,SearchResultsScreen,CreditsScreen,Onboarding}.kt` | replace small hard-coded sizes |
| `app/src/test/.../ContrastTest.kt` (new) | tile gradient contrast and text-token contrast |
| `tools/language_matrix.py` | add `--fontscale` to take the matrix at 1.0, 1.3, 2.0 |

---

## Task 1: Measure first (no code change to the app)
- [ ] Add `--fontscale <x>` to `tools/language_matrix.py` (sets `settings put system font_scale x` before each run and restores 1.0 at the end).
- [ ] Take the Home, Content, reader top, dictionary and player screens at 1.0 and 2.0 in English and Odia; save to `docs/agent/ui_before/`. This is the "before" evidence.
- [ ] Sample real pixel colours of one Skandha tile at four points (top-left, centre behind the number, bottom-left behind the count, bottom-right) with Pillow and compute contrast against the text colour. Write the numbers in the audit file.
- [ ] Commit: `ui audit: before-screenshots and tile contrast measured`.

**Done when:** the tile contrast finding is either closed (all 4:1 or better for large, 4.5:1 for small text) or turned into a number to fix in Task 6.

## Task 2: Type scale and small text
- [ ] Add to `Type.kt`:
  ```kotlin
  object AppType { val caption = 12.sp; val meta = 13.sp; val body = 15.sp; val indic = 16.sp }
  ```
- [ ] Replace every `fontSize = 9.sp` to `11.sp` and Indic `fontSize` below 15 with the scale, file by file, in this order (counts of sizes under 15 sp): `Player.kt` 28, `DictionaryScreen.kt` 19, `AnnotationSheet.kt` 19, `Reader.kt` 15, `Me.kt` 10, `SearchResultsScreen.kt` 8, `CreditsScreen.kt` 6, then the rest.
- [ ] Rule while replacing: a string that can be Hindi, Bengali or Odia gets `AppType.indic` and line height 1.8; Latin-only metadata (licence line, "CC BY-SA", version) gets `caption`.
- [ ] After each file: `gradlew assembleDebug`, look at that screen on the emulator at 1.0 and 1.3, fix any clipping with `maxLines` and `heightIn(min)` instead of shrinking the text.
- [ ] Add a unit test that fails the build if a new `fontSize = 1[0-1].sp` appears (a text scan of `ui/`, like `tools/agent/commit_check.py`).
- [ ] Commit per file group: `ui: type scale, no text under 12 sp (Player)` and so on.

**Done when:** `grep -rE "fontSize = (9|10|11)\.sp" ui` is empty and no Indic string is under 15 sp.

## Task 3: Touch targets
- [ ] New `Touch.kt`:
  ```kotlin
  fun Modifier.minTouch() = this.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
  @Composable fun TouchIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) =
      IconButton(onClick, modifier.minTouch(), content = content)
  ```
- [ ] Apply to the six controls found: `Bars.kt:267` (next), `Player.kt:288, 321` (close, options), `Reader.kt:539` (bookmark), `AnnotationSheet.kt:172` (swatches 44 to 48), `SearchResultsScreen.kt:394` (voice 44 to 48). Keep the drawn icon at its current size; only the hit area grows.
- [ ] Check 8 dp spacing between neighbours in the player transport row and in the word card (save, report, copy, close are 36 dp apart today).
- [ ] Commit: `ui: 48 dp touch targets on icon buttons`.

**Done when:** a grep for `IconButton(... size(3x|40|44)` returns nothing, and the word card buttons can be tapped one at a time with a thumb on the phone.

## Task 4: Font scale to 200%
- [ ] In `Bars.kt` remove `CappedFontScale(max = 1.3f)`. Make the tab bar a `Row` of `Column(icon, label)` with `heightIn(min = 64.dp)` so it grows; labels `maxLines = 2`, `softWrap`.
- [ ] Mini player: let the title wrap to two lines above 1.3 and move the play control to the end; keep the progress hairline.
- [ ] Take Home, Content, reader and player at 2.0 on the emulator; fix the first three things that clip (expected: Skandha tiles, story cards, the language pill).
- [ ] Commit: `ui: tab bar and mini player follow the system font size up to 200%`.

**Done when:** at 2.0 no label is cut and every control is reachable without a horizontal scroll.

## Task 5: Reduced motion
- [ ] Helper:
  ```kotlin
  @Composable fun animationsOn(): Boolean {
      val ctx = LocalContext.current
      return remember { android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
  }
  ```
- [ ] `Splash.kt`: when off, show the final frame immediately (no stagger, no glow animation, no breathing).
- [ ] `VoiceNavigatorSheet.kt:123`: when off, show a static mic.
- [ ] Check with `adb shell settings put global animator_duration_scale 0` that the splash is a still frame and the app still opens; restore to 1.
- [ ] Commit: `ui: no repeating or staggered animation when animations are off`.

## Task 6: Tile contrast
- [ ] From Task 1's measured numbers, add `ContrastTest` that, for each of the 13 Skandha hues, computes the contrast of cream text on both gradient stops and on the mandala overlay peak, and fails under 4.5:1 for text under 18 sp and under 3:1 for large text.
- [ ] If any fail, darken the near stop in `skGradient` (the same loop that already darkens the far stop) and lower the mandala alpha from `0x26` to `0x1A`.
- [ ] Commit: `ui: tile gradients checked in a test (4.5:1)`.

## Task 7: Quieter Home (craft finding, optional)
- [ ] Make only the resume card dark and large; give Saved, Dictionary, progress and story cards the plain paper surface with a hairline border; keep the Kangra art on story cards only.
- [ ] Take Home at 1.0 before and after and compare; keep the change only if the resume card is clearly the first thing the eye reaches.
- [ ] Remove the mandala line art behind Skandha tiles and see if the tile reads worse; if it does, restore it.
- [ ] Commit: `ui: Home has one dark hero`.

## Task 8: Labels, writing and the appearance switch
- [ ] Paath step: "Preview, 1.1.1" becomes "Preview: the first shloka"; "English Transliteration" becomes "Roman letters"; add one line under "Paath" ("the language you read in").
- [ ] Choose one capitalisation: sentence case. Change the title-case strings in `Strings.kt` and `SettingsText.kt`.
- [ ] Decide with the owner: keep the Home moon button or move appearance to Settings only (audit finding, Medium). Default: keep, since it is one tap and reading apps are used in the dark.
- [ ] Verify the five icons with `contentDescription = null` that carry meaning (`Bars.kt:202`, `DictionaryScreen.kt:183, 684, 718`, `CreditsScreen.kt:209`); give them a label from existing strings.
- [ ] Commit: `ui: clearer labels, one capitalisation, icon labels`.

## Task 9: Screen reader and final pass
- [ ] Turn TalkBack on the phone; walk Home, Content, a chapter (read, bookmark, long press word, red underline popup), search, dictionary, settings. Write what is read aloud wrongly into the audit file.
- [ ] Re-take the 1.0 and 2.0 screenshots into `docs/agent/ui_after/`; add the contact sheets to the audit file.
- [ ] Re-run `gradlew test`, the search tests, the language matrix at 1.0, then `assembleRelease`, `versionCode` +1, install on the phone and check the updater card still shows for the next tag.
- [ ] Update `docs/agent/UI_AUDIT_2026-10-14.md` with an "After" table: each finding, closed or why not.
- [ ] Commit and push.

## Order and effort
1 (30 min) then 2 (largest, 2 to 3 hours with checks), 3 (45 min), 4 (1 hour), 5 (30 min), 6 (45 min), then 8, 9; 7 last and only if the owner wants a calmer Home.

## Risks
- Raising text can clip fixed-height cards (Skandha tiles, story cards): use `heightIn(min)` and let rows grow, never `maxLines = 1` on Indic titles.
- Uncapping the tab bar at 200% takes screen height: at 2.0 the labels may need to hide under 360 dp wide screens; fall back to icons with tooltips there.
- The type scan test will block new small text; it needs an allow-list file for the few legitimate 11 sp licence lines.

## Self-review
- Every High and Medium finding in the audit has a task: small text (2), font scale cap (4), touch targets (3), reduced motion (5), tile contrast (1 and 6), appearance switch (8), icon labels (8). Craft notes are Task 7 and writing is Task 8.
- Names used in later tasks (`AppType`, `minTouch`, `TouchIconButton`, `animationsOn`) are defined in Tasks 2, 3 and 5 before use.
