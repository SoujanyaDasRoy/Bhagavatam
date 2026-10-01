# Bhagavatam

Private, offline Android app for the Shrimad Bhagavat Mahapuran: Sanskrit shlokas with Hindi, Bengali and
English translations, and listening in the paath language. Kotlin + Jetpack Compose, minSdk 26.

## Open and run

1. Android Studio → **Open** → this `Bhagavatam` folder. Let Gradle sync (first sync downloads Gradle 8.11.1 and the libraries).
2. Device Manager → create a **Pixel 8** (or newer) with a **Google Play** Android 15/16 image.
3. Run ▶ `app`.
4. For translation audio, install the Hindi and Bengali voices on the phone/emulator:
   Settings → System → Languages → Text-to-speech output → Google engine → Install voice data.

If Android Studio offers to upgrade the Android Gradle Plugin or Kotlin, accepting is fine.

## What is in here

| Area | Files |
| --- | --- |
| Screens | `ui/screens/` — Onboarding (language, paath), Home, Granth, Adhyayas, Reader (with Sanskrit on/off and book mode), Player, Search, Downloads, Saved, Glossary, Me, Languages |
| Shared UI | `ui/components/` — floating tab bar, mini player, grouped lists, switches, segmented controls |
| Design system | `ui/theme/` — Kesari Deep palette, Skandha colours, 4 reader themes, fonts (Tiro Sanskrit / Hindi / Bangla, Literata, Plus Jakarta Sans, Mukta, Hind Siliguri) |
| App state | `state/AppState.kt` — settings (saved on device), reading position, bookmarks, player |
| Content | `data/SampleData.kt` — sample verses; `data/Strings.kt` — interface text in English, Hindi, Bengali |
| Sanskrit in Bengali script | `util/Transliterate.kt` |

## Prototype limits (next steps)

- **Text:** only Adhyaya 1.1 and a few famous verses are included. The English, Hindi and Bengali lines are
  placeholder wording, not the Gita Press translations. Replace `SampleData` with a Room database built from the PDFs.
- **Audio:** translations are spoken with Android text-to-speech. Sanskrit audio packs are not included, so Sanskrit
  playback runs on a timer for now.
- **Background play:** audio currently stops when the app is closed. Move the player into a Media3
  `MediaSessionService` (foreground service, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS` on Android 13+)
  to get lock-screen and notification controls.
- **Downloads:** the pack states are simulated.
