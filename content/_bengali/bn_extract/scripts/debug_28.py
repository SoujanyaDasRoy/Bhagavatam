import json, glob, os

for p in range(340, 360):
    f = f"work/vision/v2_p{p:04d}.json"
    if os.path.exists(f):
        d = json.load(open(f, encoding="utf-8"))
        print(f"p{p:04d}: ch={d.get('chapter_number')} | tit={d.get('chapter_title')} | hd={d.get('skandha_heading')}")
