import json, os, re
import bn_extract as b

cfg = b.load_config()
expected = json.load(open("data/expected_verses.json", encoding="utf-8"))["verses"]
pages = b.load_pages(cfg)

# Filter for skandha 10 (pages 129 to 789)
s10_pages = [p for p in pages if p["_vol"] == "2" and 129 <= p["_pg"] <= 789]
print(f"Total Skandha 10 pages: {len(s10_pages)}")

chs, unassigned = b.build_chapters(cfg, s10_pages)
print(f"Chapters built: {len(chs)}")

DIG = str.maketrans('০১২৩৪৫৬৭৮৯', '0123456789')
def nums(s):
    return [int(x.translate(DIG)) for x in re.findall(r'[০-৯]+', s)]

for c in chs:
    seen = set()
    for u in c.units:
        if u["a"] is not None and u["b"] is not None:
            for x in range(u["a"], u["b"]+1):
                seen.add(x)
    exp = expected.get(f"10.{c.number}", [])
    missing = sorted(set(exp) - seen)
    print(f"Ch {c.number:02d}: {len(c.units)} units, {len(seen)} verses seen / {len(exp)} expected. Missing: {len(missing)}")
