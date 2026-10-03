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
| English interface and titles | Cardo | 16-17/24 body, 15/20 bold labels, 13/18 caption floor, 34/40 bold display |
| Hindi interface | Mukta | as above |
| Bengali interface | Hind Siliguri | as above |
| Sanskrit shloka | Noto Sans Devanagari | 22/40, centred, `shloka` colour |
| Hindi translation | Tiro Devanagari Hindi | 18/32, left-aligned |
| Bengali translation | Noto Serif Bengali | 18/32, left-aligned |
| English translation, IAST | Lexicon (stand-in: Literata) | 17/28 |

Lexicon is a licensed typeface. To use it, add `lexicon_regular.ttf` to `res/font` and point `EnglishReading` in `Type.kt` at it. Cardo has a small x-height, so keep anything people read at 15sp or more. Indic scripts never go below 15sp; line height 1.8 for Indic, 1.5 for Latin.

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

## Motion
120ms press, 200ms sheets, 300ms screens. Nothing animates while reading. Respect the system animation scale.

## Access
48dp targets, 8dp apart. 4.5:1 text, 3:1 icons. Spoken labels on every icon-only control. State is never colour alone. Layouts hold at 200% font size.

## Migrating screens
Screens still read `Brand.*`. Move them to `LocalAppColors.current` one at a time, starting with `Bars.kt`, then Player, Library, Reader. Provide `LocalAppColors` in `Theme.kt` from the chosen theme (Prabhat, Pothi, Sandhya, Ratri) and add a "Follow system" option.
