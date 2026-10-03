import json, re, os

expected = json.load(open("data/expected_verses.json", encoding="utf-8"))["verses"]
fixes = json.load(open("data/numbering_fixes.json", encoding="utf-8")) if os.path.exists("data/numbering_fixes.json") else []

DIG = str.maketrans('০১২৩৪৫৬৭৮৯', '0123456789')
def nums(s):
    return [int(x.translate(DIG)) for x in re.findall(r'[০-৯]+', s)]

for ch in range(1, 91):
    f = f"out/skandha-10/chapter-{ch:02d}.md"
    if not os.path.exists(f): continue
    txt = open(f, encoding="utf-8").read()
    exp = expected.get(f"10.{ch}", [])
    
    parts = re.split(r"^### শ্লোক ", txt, flags=re.M)[1:]
    units = []
    seen = set()
    for blk in parts:
        head = blk.splitlines()[0]
        n = nums(head)
        if n:
            units.append((n[0], n[-1], head))
            for x in range(n[0], n[-1]+1):
                seen.add(x)
    
    missing = sorted(set(exp) - seen)
    if missing:
        print(f"Ch {ch:02d} (missing {len(missing)}: {missing}):")
        # print units around missing
        for u in units:
            print(f"   unit {u[2]}")
