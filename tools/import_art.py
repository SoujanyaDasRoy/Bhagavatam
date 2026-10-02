"""Turns generated images into app resources.
  Put files in content/art/raw/ named sk_01.png, sk_00.jpg, home_hero.png, ch_10_29.png ...
  Run from the project root:  python tools/import_art.py
Each is cropped to 16:10, resized to 1280x800 and saved as WebP into app/src/main/res/drawable-nodpi/.
Needs Pillow:  pip install pillow
"""
import os, re, sys
from PIL import Image

RAW = "content/art/raw"
OUT = "app/src/main/res/drawable-nodpi"
W, H = 1280, 800
OK = re.compile(r"^(sk_\d{2}|home_hero|ch_\d{1,2}_\d{1,3})$")

def main():
    if not os.path.isdir(RAW):
        sys.exit(f"Put your images in {RAW} first.")
    os.makedirs(OUT, exist_ok=True)
    done = skipped = 0
    for fn in sorted(os.listdir(RAW)):
        stem, ext = os.path.splitext(fn)
        if ext.lower() not in (".png", ".jpg", ".jpeg", ".webp"):
            continue
        if not OK.match(stem):
            print("skip (name must look like sk_03, ch_10_29 or home_hero):", fn); skipped += 1; continue
        im = Image.open(os.path.join(RAW, fn)).convert("RGB")
        r = W / H
        w, h = im.size
        if w / h > r:
            nw = int(h * r); im = im.crop(((w - nw) // 2, 0, (w - nw) // 2 + nw, h))
        else:
            nh = int(w / r); im = im.crop((0, (h - nh) // 2, w, (h - nh) // 2 + nh))
        im = im.resize((W, H), Image.LANCZOS)
        dst = os.path.join(OUT, stem + ".webp")
        im.save(dst, "WEBP", quality=80, method=6)
        print(f"{stem}: {os.path.getsize(dst) // 1024} KB"); done += 1
    print(f"{done} imported, {skipped} skipped. Rebuild the app to see them.")

if __name__ == "__main__":
    main()
