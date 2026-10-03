# Art & Asset Specification

> **Kangra / Pahari Miniature Painting Art Direction & Assets Pipeline**

---

## 1. Visual Style & Aesthetic Principles

All artwork in the Bhagavatam application adheres to the classical **Kangra / Pahari Miniature Painting** school (c. late 18th century, Himachal Pradesh / Garhwal):
- **Medium & Texture:** Opaque gouache on antique cream/buff wasli paper with visible natural pigment grain, delicate mineral washes, and faint natural aging.
- **Color Palette:** Saffron, deep indigo, forest green, sindoor vermilion, warm madder red, ochre, pearl white, and powdered gold accents.
- **Compositional Layout:**
  - **Aspect Ratio:** **16:10** landscape (`1280x800` or `1920x1200`).
  - **Layout Rule:** Primary divine/narrative subject positioned in the **center or right third**. The **left third must remain calm and uncluttered** (mist, sky, soft water, or tree foliage) to allow text and scrim readability in reader/card UI.
  - **Mood:** Devotional, serene, sacred, timeless.

---

## 2. Asset Naming & Directory Locations

| Asset Type | File Name Pattern | Destination Directory | Description |
|---|---|---|---|
| Home Hero | `home_hero.webp` | `app/src/main/res/drawable-nodpi/` | Main banner on Home screen |
| Mahatmya Skandha | `sk_00.webp` | `app/src/main/res/drawable-nodpi/` | Mahatmya section card art |
| Skandhas 1 to 12 | `sk_01.webp` .. `sk_12.webp` | `app/src/main/res/drawable-nodpi/` | Skandha overview card art |
| Chapter Backgrounds | `ch_<S>_<C>.webp` (e.g. `ch_0_1.webp`) | `app/src/main/res/drawable-nodpi/` | Chapter reader header art |

---

## 3. Art Prompts & Ingestion Workflow

### 3.1 Prompt Generation
Chapter-by-chapter prompts are curated in `content/art/PROMPTS.md` and can be automatically templated with:
```powershell
python tools/make_chapter_prompts.py
```

### 3.2 Converting and Importing Art
Convert generated PNG/JPEG images into optimized WebP and install directly into the Android resource directory:
```powershell
python tools/import_art.py path/to/generated_image.png --name ch_0_1 --install
```

### 3.3 Prompt Template Standard
```text
A genuine classical Kangra miniature painting, Pahari school, 18th century style, opaque gouache on aged handmade wasli paper.
Scene: [Specific incident / narrative description from the chapter].
Style details: Flowing fine-line brushwork, almond eyes, intricate floral patterns on attire, soft rolling hills, blooming kadamba trees, lotus pond.
Lighting & Atmosphere: Soft golden hour light, luminous atmosphere, sacred and peaceful.
Composition: 16:10 wide landscape format. Left third is calm atmospheric sky and misty river; main figures placed in center and right.
```
