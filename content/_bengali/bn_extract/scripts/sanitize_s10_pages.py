import json, glob, os, re
import bn_extract as b

for p in range(129, 790):
    f = f"work/vision/v2_p{p:04d}.json"
    if not os.path.exists(f): continue
    d = json.load(open(f, encoding="utf-8"))
    
    tit = d.get("chapter_title") or ""
    ch = d.get("chapter_number")
    paras = d.get("paragraphs", [])
    sa = d.get("sanskrit_verse_numbers") or []
    
    # Check if this page is a genuine chapter start (contains verse 1)
    has_v1 = False
    for num in sa:
        if str(num).strip() in ("1", "১"):
            has_v1 = True
            break
    if not has_v1:
        for para in paras[:2]:
            if any(k in para for k in ["॥ ১", "॥ ১–", "॥ ১-", "। ১ ।", "। ১ ।।", "॥ ১ ॥"]):
                has_v1 = True
                break
    
    if not has_v1:
        if ch is not None or tit:
            d["chapter_number"] = None
            d["chapter_title"] = None
            json.dump(d, open(f, "w", encoding="utf-8"), indent=2, ensure_ascii=False)
    else:
        tit_num = b.parse_chapter_num(tit)
        ch_num = b.parse_chapter_num(ch)
        final_ch = tit_num if tit_num else ch_num
        d["chapter_number"] = final_ch
        json.dump(d, open(f, "w", encoding="utf-8"), indent=2, ensure_ascii=False)
        print(f"p{p:04d}: Ch {final_ch} -> {tit[:35]}")

print("Sanitization complete.")
