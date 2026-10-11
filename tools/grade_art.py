"""Applies tailored classical Pahari/Kangra miniature color grading, contrast curves,
and UI left-scrim tone balancing to Bhagavatam artwork.
"""
import os, glob
import numpy as np
from PIL import Image, ImageEnhance, ImageFilter

RAW = "content/art/raw"
OUT = "app/src/main/res/drawable-nodpi"
W, H = 1280, 800

# Color grading profiles per asset (temperature_shift_rgb, contrast, saturation, left_vignette_weight)
PROFILES = {
    "home_hero": {"tint": (0.96, 0.98, 1.05), "contrast": 1.12, "sat": 1.10, "gamma": 0.96, "left_darken": 0.12},
    "sk_00": {"tint": (1.04, 1.01, 0.94), "contrast": 1.08, "sat": 1.08, "gamma": 0.97, "left_darken": 0.08},
    "sk_01": {"tint": (1.06, 1.01, 0.92), "contrast": 1.10, "sat": 1.12, "gamma": 0.96, "left_darken": 0.10},
    "sk_02": {"tint": (1.05, 1.02, 0.94), "contrast": 1.10, "sat": 1.10, "gamma": 0.96, "left_darken": 0.08},
    "sk_03": {"tint": (1.03, 1.00, 0.96), "contrast": 1.10, "sat": 1.12, "gamma": 0.96, "left_darken": 0.10},
    "sk_04": {"tint": (0.97, 1.03, 0.97), "contrast": 1.12, "sat": 1.12, "gamma": 0.95, "left_darken": 0.10},
    "sk_05": {"tint": (0.96, 1.04, 0.98), "contrast": 1.08, "sat": 1.10, "gamma": 0.97, "left_darken": 0.08},
    "sk_06": {"tint": (0.96, 1.02, 1.04), "contrast": 1.12, "sat": 1.14, "gamma": 0.95, "left_darken": 0.10},
    "sk_07": {"tint": (1.06, 1.00, 0.94), "contrast": 1.12, "sat": 1.14, "gamma": 0.95, "left_darken": 0.10},
    "sk_08": {"tint": (0.96, 1.00, 1.06), "contrast": 1.12, "sat": 1.14, "gamma": 0.95, "left_darken": 0.10},
    "sk_09": {"tint": (1.05, 1.02, 0.94), "contrast": 1.10, "sat": 1.10, "gamma": 0.96, "left_darken": 0.08},
    "sk_10": {"tint": (0.96, 0.98, 1.06), "contrast": 1.12, "sat": 1.15, "gamma": 0.95, "left_darken": 0.10},
    "sk_11": {"tint": (1.05, 0.98, 1.02), "contrast": 1.10, "sat": 1.12, "gamma": 0.96, "left_darken": 0.08},
}

def grade_image(img: Image.Image, stem: str) -> Image.Image:
    prof = PROFILES.get(stem, {"tint": (1.0, 1.0, 1.0), "contrast": 1.08, "sat": 1.08, "gamma": 0.97, "left_darken": 0.08})
    
    # 1. Convert to float array
    arr = np.array(img, dtype=np.float32) / 255.0
    h, w, c = arr.shape
    
    # 2. Color tint / mineral pigment curve
    r_tint, g_tint, b_tint = prof["tint"]
    arr[:, :, 0] *= r_tint
    arr[:, :, 1] *= g_tint
    arr[:, :, 2] *= b_tint
    
    # 3. Tone curve (Filmic S-curve & Gamma)
    gamma = prof["gamma"]
    arr = np.power(arr, gamma)
    
    # S-curve contrast boost
    contrast = prof["contrast"]
    arr = (arr - 0.5) * contrast + 0.5
    
    # 4. Subtle left-side & bottom gradient darkening for UI readability
    left_darken = prof["left_darken"]
    x = np.linspace(0, 1, w, dtype=np.float32)
    y = np.linspace(0, 1, h, dtype=np.float32)
    xx, yy = np.meshgrid(x, y)
    
    # Left shadow ramp: strongest on left edge (0 to 0.45)
    left_mask = np.clip((0.45 - xx) / 0.45, 0, 1) ** 1.5
    # Bottom shadow ramp (0.60 to 1.0)
    bottom_mask = np.clip((yy - 0.60) / 0.40, 0, 1) ** 1.5
    
    shadow_vignette = 1.0 - (left_mask * left_darken + bottom_mask * 0.08)
    shadow_vignette = np.clip(shadow_vignette, 0.75, 1.0)
    
    for i in range(3):
        arr[:, :, i] *= shadow_vignette
        
    arr = np.clip(arr, 0.0, 1.0) * 255.0
    graded_pil = Image.fromarray(arr.astype(np.uint8))
    
    # 5. Saturation enhancement
    enhancer = ImageEnhance.Color(graded_pil)
    graded_pil = enhancer.enhance(prof["sat"])
    
    return graded_pil

def main():
    os.makedirs(OUT, exist_ok=True)
    files = sorted(glob.glob(os.path.join(RAW, "*.*")))
    print(f"Found {len(files)} raw images to grade...")
    
    for p in files:
        stem, ext = os.path.splitext(os.path.basename(p))
        if ext.lower() not in (".png", ".jpg", ".jpeg", ".webp"):
            continue
        if stem.startswith("test_"):
            continue
            
        im = Image.open(p).convert("RGB")
        w, h = im.size
        
        # If image has outer border margins (sk_* and home_hero), strip them first to achieve full-bleed
        if stem != "ch_0_1" and (stem.startswith("sk_") or stem == "home_hero"):
            # Strip 58px on left/right and 46px on top/bottom
            im = im.crop((58, 46, w - 58, h - 46))
            w, h = im.size

        # 16:10 crop & resize
        r = W / H
        if w / h > r:
            nw = int(h * r)
            im = im.crop(((w - nw) // 2, 0, (w - nw) // 2 + nw, h))
        else:
            nh = int(w / r)
            im = im.crop((0, (h - nh) // 2, w, (h - nh) // 2 + nh))
        im = im.resize((W, H), Image.LANCZOS)
        
        # Apply color grading
        graded = grade_image(im, stem)
        
        # Save as optimized WebP
        dst = os.path.join(OUT, f"{stem}.webp")
        graded.save(dst, "WEBP", quality=82, method=6)
        print(f"Graded & Saved {stem}.webp (borderless): {os.path.getsize(dst) // 1024} KB")

if __name__ == "__main__":
    main()
