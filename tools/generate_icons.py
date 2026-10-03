#!/usr/bin/env python3
"""
generate_icons.py - Generates Android multi-density adaptive launcher icons and logo marks
from assets/logo/bhagavatam.png.
"""

from pathlib import Path
from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parent.parent
SRC_LOGO = ROOT / "assets" / "logo" / "bhagavatam.png"
RES_DIR = ROOT / "app" / "src" / "main" / "res"

DENSITIES = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}

def make_adaptive_foreground(src_img: Image.Image, size: int) -> Image.Image:
    """
    Android adaptive icon foreground is size x size, with the icon safely placed
    within the central 70% safe-zone to avoid clipping by squircle/circle masks.
    """
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    inner_size = int(size * 0.72)
    resized = src_img.resize((inner_size, inner_size), Image.Resampling.LANCZOS)
    
    # If RGB, convert to RGBA
    if resized.mode != "RGBA":
        resized = resized.convert("RGBA")
        
    offset = ((size - inner_size) // 2, (size - inner_size) // 2)
    canvas.paste(resized, offset, mask=resized if "A" in resized.getbands() else None)
    return canvas

def make_background(size: int) -> Image.Image:
    """Dark Kesari/night background for the launcher icon."""
    return Image.new("RGBA", (size, size), (18, 20, 28, 255)) # #12141C

def generate():
    if not SRC_LOGO.exists():
        print(f"Error: Source logo not found at {SRC_LOGO}")
        return

    print(f"Processing source logo from {SRC_LOGO}...")
    src = Image.open(SRC_LOGO)
    
    # 1. Generate Mipmap densities
    for folder_name, size in DENSITIES.items():
        out_folder = RES_DIR / folder_name
        out_folder.mkdir(parents=True, exist_ok=True)
        
        # Foreground
        fg = make_adaptive_foreground(src, size)
        fg_path = out_folder / "ic_launcher_foreground.webp"
        fg.save(fg_path, "WEBP", quality=95)
        
        # Background
        bg = make_background(size)
        bg_path = out_folder / "ic_launcher_background.webp"
        bg.save(bg_path, "WEBP", quality=95)
        
        # Monochrome
        mono = make_adaptive_foreground(src.convert("L").convert("RGBA"), size)
        mono_path = out_folder / "ic_launcher_monochrome.webp"
        mono.save(mono_path, "WEBP", quality=90)
        
        print(f"Generated {folder_name} ({size}x{size})")

    # 2. Generate logo_mark.webp for Splash & About
    nodpi_dir = RES_DIR / "drawable-nodpi"
    nodpi_dir.mkdir(parents=True, exist_ok=True)
    mark_path = nodpi_dir / "logo_mark.webp"
    
    mark_img = src.resize((512, 512), Image.Resampling.LANCZOS)
    mark_img.save(mark_path, "WEBP", quality=95)
    print(f"Generated {mark_path} (512x512)")

if __name__ == "__main__":
    generate()
