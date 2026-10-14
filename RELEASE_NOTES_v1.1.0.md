# Bhagavatam v1.1.0 Release Notes

**Bhagavatam v1.1.0** introduces an ad-free YouTube audio streaming engine and an updated peach-terracotta player layout.

---

## 🌟 What's New in v1.1.0

### 🎧 Ad-Free Native YouTube Audio Streaming Engine
- **InnerTube Direct CDN Extraction**:
  - Direct raw audio stream extraction (Opus 160kbps / AAC 128kbps) bypassing YouTube's web/app frontend and JavaScript ad-injection layers.
  - Zero commercials, zero audio interruptions, and zero tracking.
- **Media3 ExoPlayer & Network Cache**:
  - Integrated with AndroidX Media3 ExoPlayer with disk LRU caching (`YouTubeMediaCache`) for smooth, buffered, and data-efficient listening.
  - Background audio playback with lockscreen and notification media controls via `PlaybackService`.
- **High-Like Authentic Audio Catalog**:
  - **Hindi (हिन्दी)**: Pt. Pradeep Pandey & Gita Press Gorakhpur complete chapter recitations.
  - **Bengali (বাংলা)**: Sri Gita Press Bengali & Bhaktivedanta chapter-by-chapter path.
  - **English**: Yaśodā Kumāra Dāsa & acclaimed Srimad Bhagavatam audiobooks.
  - **Sanskrit (संस्कृत)**: Traditional Vedic meter shloka path by Sanatan Prem Pooja.
  - Automatic fallback scoring algorithm that queries and resolves the #1 most liked and viewed full chapter recordings on YouTube.

---

### 🎨 Warm Peach & Terracotta Player UI
- **Warm Sand/Peach Aesthetic**:
  - Multi-stop gradient background transitioning from soft rose/sand (`#E8D5CA`) to warm peach cream (`#FDF7F3`).
- **Clean Hero Artwork**:
  - Rounded square artwork (`RoundedCornerShape(22.dp)`) with subtle drop shadow and **no cluttering overlay badges**.
- **Track Metadata & Favorite**:
  - Bold track title in dark espresso (`#1F1A16`), reciter/subtitle in terracotta (`#9E4424`), and heart bookmark button.
- **Precision Scrubber Bar**:
  - Terracotta slider (`#A34828`) with real-time stream elapsed position (`0:00`) and total duration countdown.
- **5-Button Transport Bar**:
  - Shuffle / PlayThrough mode, Skip Previous, Hero Circular Terracotta Play/Pause button (`68.dp`), Skip Next, and Repeat/Loop.
- **5-Item Bottom Utility Bar**:
  - 📱 **This phone**: Current active audio output route.
  - 💬 **Lyrics**: Synchronized karaoke Sanskrit shloka highlighting and translation layers.
  - 🎛️ **Equalizer / Sound Settings**: Playback speed adjustment (`0.75×` to `2.0×`), audio mode toggle (Karaoke Paath vs. YouTube Stream), and narration language selection.
  - 🌙 **Sleep**: Quick sleep timer presets (`15m`, `30m`, `45m`, `60m`, Off).
  - 📑 **Queue**: Up-next chapter and verse list with direct jump functionality.

---

### ⚡ Performance & Polish
- Reduced APK memory footprint with streaming disk cache management.
- Instant responsive scrubbing with fraction-based stream seeking.
- Multi-language title typography matching selected script (`NotoSerifBengali`, `NotoDevanagari`, `Jakarta`).

---

## 📦 APK Download
- **Debug APK**: [`apk/Bhagavatam-debug.apk`](file:///c:/Users/sdroy/OneDrive/Desktop/Coding/Startups/Bhagavatam/apk/Bhagavatam-debug.apk)
- **Release APK**: [`apk/Bhagavatam-release.apk`](file:///c:/Users/sdroy/OneDrive/Desktop/Coding/Startups/Bhagavatam/apk/Bhagavatam-release.apk)
