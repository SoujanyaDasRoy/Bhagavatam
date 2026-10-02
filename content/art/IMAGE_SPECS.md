# Artwork Technical Specifications & Optimization Guide

This document outlines the image pipeline, technical specifications, and compression strategy for all **341 artwork assets** in the Bhagavatam offline mobile app.

---

## 1. Executive Summary

* **Total Images:** 341 (14 Skandha/Home cards + 327 chapter headers)
* **Target Total App Size Footprint:** **~18 MB – 24 MB**
* **Average Size per Image:** **~50 KB – 70 KB**
* **Aspect Ratio:** **16:10**
* **Final App Format:** **Lossy WebP** (`quality=78`, `method=6`)
* **Final In-App Dimensions:** **1024 × 640 px** (or 1280 × 800 px)

---

## 2. Image Pipeline Workflow

```
[ AI Image Generation ] ──> [ content/art/raw/ ] ──> [ python tools/import_art.py ] ──> [ app/src/main/res/drawable-nodpi/ ]
   (1600 × 1000 PNG/JPG)      (sk_00.png, ch_10_29.png)   (Resize to 1024x640, WebP Q78)         (Instant offline loading)
```

---

## 3. Detailed Specifications

### A. Generation Phase (AI Tools: Midjourney / Flux / DALL-E)
* **Prompt Aspect Ratio:** `16:10` (`--ar 16:10`)
* **Generation Resolution:** `1600 × 1000 px` (or closest default AI high-res equivalent)
* **Format:** Save as high-quality PNG or JPG.
* **Naming Conventions:**
  * Skandha Covers / Mahatmya: `sk_00.png` ... `sk_12.png`
  * Home Screen Hero: `home_hero.png`
  * Chapter Illustrations: `ch_<skandha>_<chapter>.png` (e.g. `ch_10_29.png`)

### B. App Production Phase (Processed by `tools/import_art.py`)

| Parameter | Specification | Purpose |
|---|---|---|
| **Format** | **WebP (Lossy)** | 30–40% smaller than JPEG with instant hardware-accelerated decode on Android. |
| **Dimensions** | **1024 × 640 px** | Optimized for mobile banners/cards (retina density without memory bloat). |
| **Quality** | `78` | Sweet spot: strips unnoticeable noise while preserving crisp strokes and gouache textures. |
| **Method** | `method=6` | Slowest/highest compression efficiency during build, zero runtime penalty. |
| **Color Profile & EXIF** | Stripped | Removes unneeded metadata bytes. |
| **RAM per Bitmap** | **~2.6 MB** | Low footprint avoids UI frame drops during fast scrolling. |

---

## 4. How to Import

1. Put your raw generated `.png` / `.jpg` files into:
   ```
   content/art/raw/
   ```
2. Run the import script from the project root:
   ```bash
   python tools/import_art.py
   ```
3. The script automatically crops to 16:10, resizes, converts to WebP, and places the files into:
   ```
   app/src/main/res/drawable-nodpi/
   ```
4. Rebuild the app to see the new images.

---

## 5. Visual Composition Guidelines

* **Left Third:** Keep calm, uncluttered, and low contrast (sky, water, mist) for text readability.
* **Lower Third:** Keep darker/gentle for bottom card titles and numbers.
* **Style Consistency:** Always append the style block from [PROMPTS.md](file:///c:/Users/sdroy/OneDrive/Desktop/Coding/Startups/Bhagavatam/content/art/PROMPTS.md).
* **Text / Typography:** Never generate text, letters, borders, or watermarks in the AI image.
