import json, glob, os

for f in sorted(glob.glob("work/vision/v2_p*.json")):
    p = int(os.path.basename(f).split("_p")[1].split(".")[0])
    d = json.load(open(f, encoding="utf-8"))
    ch = d.get("chapter_number")
    tit = d.get("chapter_title") or ""
    hd = d.get("skandha_heading") or ""
    # Check around 25-29 (p290-p340), 66-68 (p615-p630), 81-83 (p700-p725)
    if (290 <= p <= 340) or (615 <= p <= 630) or (700 <= p <= 725):
        if ch is not None or "অধ্যায়" in tit or "অধ্যায়" in tit or "অধ্যায়" in hd:
            print(f"p{p:04d}: ch={ch} | tit={tit} | hd={hd}")
