import json, os

for p in range(168, 220):
    f = f"work/vision/v2_p{p:04d}.json"
    if os.path.exists(f):
        d = json.load(open(f, encoding="utf-8"))
        print(f"p{p:04d}: ch={d.get('chapter_number')} | sk={d.get('skandha_heading')} | tit={d.get('chapter_title')}")
