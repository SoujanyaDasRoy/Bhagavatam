# Player and Narration Implementation Plan

> For agentic workers: use superpowers:executing-plans. Steps use checkbox syntax.

**Goal:** A calmer, clearer player, and narration that sounds like a person reading, built on the phone's own text-to-speech engine.

**Architecture:** Canonical text stays untouched. `NarrationText` (pure Kotlin) turns a verse into a `VersePlan`: sentence-sized segments, each with speech text and a pause. `Narrator` queues segments on Android TTS with real silences and one verse of lookahead, so verse changes have no gap. `AppState` orchestrates (epoch-guarded events, resume from segment, focus, fallback). The player highlights the active segment and can start from any sentence.

**Tech Stack:** Kotlin, Jetpack Compose, android.speech.tts, android.media.session (framework only, no new runtime dependency), JUnit 4 (test only).

## Global Constraints
- Offline and private: no network TTS, no API keys.
- No new runtime dependencies. minSdk 26.
- Source verse text is never modified for speech; all speech changes live in `audio/NarrationText.kt`.
- No em or en dashes in any text shown or written.
- Touch targets 48dp, text 4.5:1, honour the system animation scale.

## Evidence that drove the design (see final report)
- Android TTS: rate and pitch are engine-wide, not per utterance; `QUEUE_ADD` plus `playSilentUtterance` is the only portable way to shape pauses.
- Cloud (Google Chirp 3 HD, Azure) has Hindi and Bengali voices but **no Sanskrit voice anywhere**; Piper has Hindi and Bangladeshi Bengali only; AI4Bharat Indic-TTS has no Sanskrit. Sanskrit is therefore read with the Hindi voice.
- Speech content gets no automatic ducking, so the player must pause itself on focus loss.
- A media foreground service needs `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and no runtime prerequisite.

## Tasks
1. **Narration core.** Create `audio/NarrationText.kt` (+ `app/src/test/.../NarrationTextTest.kt`): sentence split with abbreviation guard, dialogue-intro split, long-sentence clause split, English anglicisation of IAST names, parenthetical to comma, footnote and marker removal, Indic joiner cleanup, Sanskrit pada plan with speaker line, time estimate. Test first, then implement.
2. **Narrator.** Create `audio/Narrator.kt`: lazy engine init, `configure(lang, rate, voiceName)`, `speakVerse(epoch, seq, plan, from, pauseScale)`, `stop()`, voice list with friendly labels, preview, failure callbacks.
3. **Player state.** Rewrite the player section of `AppState`: statuses, issues, epoch guard, lookahead queue, resume from segment, tap-to-play segment, estimate of time left, audio focus and noisy-audio pause, silent fallback with a clear message, `keepPlaying` made real.
4. **Player UI.** Rewrite `ui/screens/Player.kt` (portrait and landscape): header, reading area with active-segment highlight and auto-scroll, status line, chapter progress with time left, transport with preparing ring, chips, language control, finished state, error banner. Update `MiniPlayer`.
5. **Voice and pauses.** `VoiceSheet` (voice per language with preview, pause length, install more voices) from the player and from Settings > Listening. Persist in `AppState`.
6. **Background playback.** `audio/PlaybackService.kt` + manifest: foreground media service, `MediaSession`, notification with previous / play / next, notification permission request, singleton `PlayerHost` so playback outlives the activity.
7. **Verify.** Unit tests, debug probe that synthesises representative passages to WAV and measures them, install and drive the UI on the emulator, lint, release build.
