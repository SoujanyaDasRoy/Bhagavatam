import json, glob, os

for p in range(168, 790):
    f = f"work/vision/v2_p{p:04d}.json"
    if os.path.exists(f):
        d = json.load(open(f, encoding="utf-8"))
        ch = d.get("chapter_number")
        sk = d.get("skandha_heading")
        tit = d.get("chapter_title")
        paras = len(d.get("paragraphs", []))
        if ch is not None:
            print(f"p{p:04d}: ch={ch} | sk={sk} | tit={tit} | paras={paras}")
