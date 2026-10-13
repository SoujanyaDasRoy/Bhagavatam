# Better onboarding: plan

**Status:** plan only, nothing implemented. Written 2026-10-13 from a fresh-install walk-through on the emulator (light and dark).

## What exists today (measured)
Splash (about 5 s) -> step 1 "Choose the language for the app" (English, Hindi, Bengali) -> step 2 "How will you do paath?" (paath language, four "show alongside" switches, a preview) -> Home.
- The step counter reads "2 of 2" on step 2 only; step 1 has no counter, so the person does not know there is a second step.
- Step 1 asks only for the app language. It never says what the app is: offline, no login, listening, search, dictionary.
- Step 2 mixes two decisions (what to read in, what to show beside it) with four switches. "Paath" and "show alongside" are not explained.
- No way to skip; no way to see the choice working before committing (the preview is 3 lines, English only).
- Nothing is said about audio, the dictionary (press and hold a word), saving verses or the search; people discover these by accident.
- On first launch Home says "Resume reading, Stopped at shloka 1" although nothing was read.
- The splash always shows for the same time, even when the database is ready sooner.

## Goals
1. A new reader is reading a verse within 30 seconds and 3 taps.
2. The two choices that matter (app language, reading language) are clear; everything else has a good default and can be changed later.
3. The three things people do not discover alone are shown once, in context, not in a tour: listen, press-and-hold a word, search.
4. Nothing is asked that the app does not need, and nothing is collected (the app has no account).

## Flow
**Step 0. Splash.** Keep the logo; end as soon as the database is open (minimum 1.2 s so it does not flash).

**Step 1. "Read in your language"** (one screen, replaces both current steps):
- Three large language cards (English, हिन्दी, বাংলা), each in its own script with a one-line sample of the same verse translation, so the choice is visible, not abstract. Default = the phone's language if it is one of the three, else English.
- One choice sets BOTH the app language and the reading language. A small link "Different languages for app and reading" opens the existing two-part choice.
- Below the cards, one sentence: "Works fully offline. No sign-in." Primary button "Start".
- The shloka (Sanskrit) shows by default with the chosen translation under it: the layout the app already uses. No switches on this screen.

**Step 2. First chapter, with three coach marks** (not a separate screen; the reader opens on Skandha 1 chapter 1):
1. On the Listen button, once: "Listen to this chapter". Dismiss by tapping anywhere.
2. After the person has scrolled past the first verse, once: "Press and hold any word for its meaning".
3. On the language pill, once: "Change language or show the shloka here".
Each mark appears only once, is skipped if the person already used the feature, and all three can be switched off together ("Don't show tips").

**Step 3. Home gets a first-day state:** "Start with Skandha 1" instead of "Resume reading, stopped at shloka 1", and the same three tips as a dismissible card.

## Rules
- Every string in English, Hindi and Bengali (the existing `tr()` helper). No em or en dashes.
- Touch targets 48dp, text at least 15sp for Indic scripts, contrast from the design tokens, works at font scale 1.3.
- Respects system animation scale. No video, no illustration that needs a download (the APK has its size budget).
- Settings > Languages stays the single place to change this later; the onboarding only writes the same settings.
- "Replay tips" in Settings, so the tips are not lost.

## Data and state
- Existing: `AppState.uiLang`, `readLang`, `showSanskrit`, `showHi/Bn/En`, `updateUiLang`, `updateReadLang`.
- New (SharedPreferences, like the others): `onboardingDone: Boolean`, `tipsSeen: Set<String>` (`listen`, `holdWord`, `languagePill`), `tipsEnabled: Boolean`.
- No new permissions; no network.

## Tasks
1. **State:** add the three preferences and `markTipSeen(id)`; unit test the defaults and the "never shown twice" rule.
2. **Language screen:** one screen with the three cards and sample verse (pure Compose; reuse `LanguageOption` styling and `readingFont`); default from `Locale.getDefault()`; unit test the default choice.
3. **Coach marks:** a small `CoachMark` composable (anchor, text, dismiss) used on the Listen button, the word-hold gesture and the language pill; show only when `tipsEnabled && id !in tipsSeen`.
4. **Home first-day state** and the "Replay tips" row in Settings.
5. **Check on the emulator:** fresh install in English, Hindi and Bengali, light and dark, font scale 1.3; airplane mode; screenshots; no crash in logcat.
6. **Measure:** taps and seconds from first launch to the first verse on screen (target 3 taps, 30 seconds).

## Not in this plan
Account, notifications permission prompt, ratings prompt, a pager tour with slides, any analytics.

## Open decisions for the owner
- Should the app language and reading language be one choice by default (recommended), or two screens as today?
- Show the three tips on first launch only, or also again after an update?
