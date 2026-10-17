# Bhagavatam design system

Light mode **Prabhat**, dark mode **Sandhya**. Ratri (OLED black) and Pothi (sepia) are reader variants. Tokens live in `app/src/main/java/com/bhagavatam/app/ui/theme/Tokens.kt`.

## Principles
- The text comes first; chrome stays quiet.
- One accent (Kesari) for the action: play, active tab, selected option.
- Colour marks place: each Skandha keeps its own flat hue. Everything else is paper, ink and one accent. No gradients, glows or decorative rings.
- Dark is its own palette: lighter accents, rising surfaces instead of shadows, warm text.

## Colour
| Token | Light | Dark | Contrast vs bg (L / D) | Use |
|---|---|---|---|---|
| `bg` | `#F7F4EE` | `#12141C` | - | Screen background |
| `surface` | `#FFFFFF` | `#1C2030` | - | Cards, sheets, mini-player |
| `sunk` | `#ECE7DE` | `#262B3D` | - | Search field, segmented track, progress track |
| `ink` | `#1C1A17` | `#E9E4D8` | 15.8 / 14.5 | Primary text |
| `inkSecondary` | `#6B645A` | `#A8A294` | 5.3 / 7.2 | Captions, meta, inactive tab |
| `line` | `#DDD6C9` | `#2E3346` | - | Dividers, outlines (decorative) |
| `accent` | `#A34E10` | `#F0A868` | 5.2 / 9.2 | Kesari: play, active tab, links to action, selection |
| `onAccent` | `#FFFFFF` | `#12141C` | - | Text and icons on accent |
| `accentTint` | `#F6EADF` | `#2C2530` | - | Soft accent fill: Listen pill, Reading badge |
| `gold` | `#85621A` | `#D9B45E` | 5.1 / 9.3 | Section labels, ornaments |
| `shloka` | `#6E1B1B` | `#F2C38B` | 10.4 / 11.3 | Sanskrit verse text (Sindoor) |
| `link` | `#0F7C80` | `#6FD0D3` | 4.5 / 10.2 | Info, secondary state (Teal) |
| `success` | `#1F7A4D` | `#6CCB9A` | 4.8 / 9.3 | Finished tick |
| `danger` | `#B3261E` | `#FFB4AB` | 6.0 / 10.8 | Errors, destructive |

Skandha hues (1-12): `#E07A1F #D4960F #B8892A #4E8A3E #1F8A70 #0F7C80 #C8552B #1D6FA5 #C98204 #2D3E8C #D9577E #6B4C9A`. Flat fill: hue darkened 25% (light) or 50% (dark). White text is above 4.5:1 on all twelve.

## Type
| Role | Font | Spec |
|---|---|---|
| English interface | Plus Jakarta Sans | 16/22 body, 14/20 semibold labels, 12/16 caption floor |
| Large titles, numerals | Literata | 34/40 semibold |
| Hindi interface | Mukta | as above |
| Bengali interface | Hind Siliguri | as above |
| Sanskrit shloka | Noto Sans Devanagari | 22/40, centred, `shloka` colour |
| Hindi translation | Tiro Devanagari Hindi | 18/32, left-aligned |
| Bengali translation | Noto Serif Bengali | 18/32, left-aligned |
| English translation, IAST | Lexicon (stand-in: Literata) | 17/28 |

Lexicon is a licensed typeface. To use it, add `lexicon_regular.ttf` to `res/font` and point `EnglishReading` in `Type.kt` at it. Indic scripts never go below 15sp; line height 1.8 for Indic, 1.5 for Latin. Line spacing is a setting: 0.9, 1.0 or 1.2 times these values.

## Space, shape, depth
- Spacing 4, 8, 12, 16, 20, 24, 32. Gutter 16.
- Radius: 12 fields, 16 list groups, 22-24 cards and hero, 32 tab bar, circle for play and number chips.
- Light: soft shadow on tab bar and mini-player only. Dark: no shadows; higher layer = lighter surface plus 1dp `line` border.

## Components
- Tab bar: min 64dp, 4 tabs, `accentTint` pill on the active tab. Height from content, never `fillMaxHeight` in an unbounded row.
- Mini-player: min 56dp row, 3dp `accent` progress line.
- Continue card: `surface` with 1dp `line` border, radius 24, separate 56dp play button.
- Skandha tile: 148dp, flat hue fill, numeral 40sp, 4dp white progress bar.
- Primary button 54dp; chips and segmented controls 48dp; list rows min 56dp.

## Settings
Sections in task order: Appearance (match phone, theme, accent), Reading (text size, line spacing, languages, Sanskrit), Home, Listening, Library, About. Swatches for choices that look like something; switches and value rows for the rest.

## Startup
The text database opens off the main thread behind a splash on the same night background as the system splash. Speech starts on first play.

## Motion
120ms press, 200ms sheets, 300ms screens. Nothing animates while reading. Respect the system animation scale.

## Access
48dp targets, 8dp apart. 4.5:1 text, 3:1 icons. Spoken labels on every icon-only control. State is never colour alone. Layouts hold at 200% font size.

## Migrating screens
Screens still read `Brand.*`. Move them to `LocalAppColors.current` one at a time, starting with `Bars.kt`, then Player, Library, Reader. Provide `LocalAppColors` in `Theme.kt` from the chosen theme (Prabhat, Pothi, Sandhya, Ratri) and add a "Follow system" option.


## Cards and art (update)
- Skandha cards are gradient cards again: hue darkened 58% to 28%, a lotus mandala in the corner, white text. They sit on both themes unchanged.
- Optional artwork: `sk_NN`, `sk_00`, `ch_S_A` WebP files in `res/drawable-nodpi` replace the mandala under a dark scrim. Missing files fall back silently. Prompts live in `content/art/PROMPTS.md`.
- Icons are Lucide line icons (stroke 2, round caps), tinted by the theme. Filled variants only for play, pause, bookmark, star and heart.
- Home: greeting, continue hero (indigo to plum), four quick actions, reading progress, well-loved stories, Skandha row, shloka of the day.
- Search: 54 dp field with an accent ring on focus, 180 ms debounce, recent searches, story cards, eight verses then "Show more".


## UI pass (Oct 2026)
- Radius scale: 12 fields and segments, 16 list groups and shortcuts, 20 cards and tiles (`Radius.card`), 24 hero (`Radius.large`), 32 tab bar. Use the tokens, not literals.
- Continue hero takes the gradient of the Skandha you are in (same `skGradient` as the Granth cards); the earlier indigo to plum hero is gone.
- Home shortcuts are Saved and Glossary only (Search and Granth are tabs). They stack at font scale above 1.3.
- Rows of cards bleed to the screen edge: the sections pad themselves, the `LazyRow` carries `contentPadding`.
- One heading style for content blocks: `SectionHeading` (18 bold, optional action). `SectionLabel` is the small label above grouped lists. Both sentence case.
- `AppSlider`: round 22dp thumb, 4dp track, no stop dot. Use it instead of Material `Slider`.
- Switches use the accent when on and an outlined track when off; the whole row toggles.
- `BottomScrim` fades content into the page colour behind the tab bar, mini-player and the Reader's Listen pill.
- The playing shloka is tinted with the accent (same colour as the play button), not teal.
- Tab bar and mini-player cap font scale at 1.3 so labels stay on one line.
- `ShlokaText` binds the closing `॥ N ॥` with no-break spaces so a narrow line never strands the last danda.

## Second pass: settings, gestures, motion (Oct 2026)
- **Settings** is a hub: Theme switcher (System / Light / Dark preview tiles) first, then rows for Appearance, Reading, Languages, Listening (icon, title, live value), Library, About. Appearance, Reading and Listening are their own pages. `AppState.themeMode` / `setThemeMode` is the single entry point; the Home header has a one-tap sun/moon toggle.
- **Home header**: "Jay Shree Madhav" over "Shrimad Bhagavat Mahapuran" (Hindi and Bengali use their own script). No time-of-day greeting.
- **One title per chapter.** The Reader shows the chapter title once, in the text. It appears in the top bar only after it scrolls out of view. The source PDFs had the heading running into the first shloka in about 90 chapters; `SampleData.stripLeadingTitle` removes it when verses are read (tolerant of hyphens, joiners and small spelling differences) and moves a leftover "X उवाच" line into the `speaker` field.
- **Gestures**: Reader swipe sideways = previous/next chapter, pinch = text size; Player swipe sideways = previous/next shloka, swipe down closes; mini-player swipe up opens the player; Saved: swipe a bookmark left to remove it.
- **Motion** (120 / 200 / 300 ms from `Motion`, all follow the system animation scale): pages slide a little and fade, tabs cross-fade, the player rises from the bottom, cards dip to 97% when pressed (`Modifier.tappable`), selections animate their colour, progress bars fill, the mini-player grows in, Home sections lift in once per launch (`Modifier.reveal`), notes and previews animate their height.
- `LocalContentColor` is set from the theme, so text without a colour no longer falls back to black in dark looks.

## Third pass: contrast, reading layout, data (Oct 2026)
- **Contrast** was measured for every text and background pair in all four looks (Prabhat, Pothi, Sandhya, Ratri), the Reader palette, and the Skandha gradients. Everything is at 4.5:1 or better. Fixed: light gold `#775714` (was `#85621A`, 4.05:1 on the now-playing tint in Pothi), Pothi teal `#0B6E72`, Pothi reader gold, the Skandha gradient's far stop (`skGradient` darkens it only as far as cream text needs), the Mahatmya banner, and the hero label (cream, not gold).
- **Verse layout**: reference left and speaker right; shloka and transliteration centred (transliteration in italics); a short rule; translations left-aligned and named when more than one is shown; a hairline between verses.
- **Dashes**: the printed text uses em dashes with no spaces. They are shown as a spaced hyphen (`tidyDashes` in `SampleData.kt`), and a bare hyphen between digits (verse ranges).
- **Chapter buttons**: round previous/next beside Listen, and Previous / Next with the chapter's title after the last verse. They cross Skandha boundaries (`SampleData.neighbour`).
- **Settings**: the accent colour picker is gone (one accent, Kesari). New "Your data" group: clear reading progress, clear recent searches, remove saved verses, reset settings, each behind a confirmation dialog. Saved verses are now remembered between launches.


## Player and narration (Oct 2026)
- **Pipeline**: canonical verse text (never modified) -> `audio/NarrationText.kt` (`VersePlan`: sentence-sized segments, speakable text, pauses) -> `audio/Narrator.kt` (Android TTS: one utterance per segment, real silent utterances between, next verse queued before the current one ends) -> `AppState` (epoch-guarded events, resume from segment, audio focus, silent fallback) -> player UI.
- **Why the phone's own engine**: offline, private, no keys, no per-use cost. Measured on the emulator (Google engine): every utterance ends with its own silence (English 790 ms, Bengali 550, Hindi 450, Sanskrit 250), so the narrator only adds the shortfall. No engine has a Sanskrit voice (Google Cloud Chirp 3 HD and Azure have Hindi and Bengali, not Sanskrit; Piper has Hindi and Bangladeshi Bengali; AI4Bharat has no Sanskrit), so Sanskrit is read with the Hindi voice and the player says so.
- **Preprocessing** (speech only): footnote marks, brackets to short asides, line-wrap hyphens joined, transliterated names spelt as said (Krsna to Krishna, Soota), text garbled by the old print font skipped, Devanagari letters dropped from English speech, verse markers and dandas removed from shloka lines.
- **Player**: reference and title in the header; the sentence (or shloka line) being said is lit and the rest quieter; tap any sentence to play from there; auto-scroll that yields to the reader's finger; chapter progress with time left; a ring on the play button while the voice prepares; a plain message with a way out when no voice is installed; end-of-chapter Replay and Next chapter; landscape layout; voice and pause settings in a sheet.
- **Background**: `PlaybackService` (foreground media service, `MediaSession`, notification and lock-screen controls). `AppState` lives in `BhagavatamApp`, so playback survives the screen closing.
- **Check it**: debug build, `adb shell am start -n com.bhagavatam.app/.MainActivity --ez narration_probe true` runs `NarrationProbe` (corpus statistics and synthesised WAVs in `files/probe`). Unit tests: `./gradlew :app:testDebugUnitTest`.
