import json, glob, os

expected = json.load(open("data/expected_verses.json", encoding="utf-8"))

for ch_num in range(1, 91):
    f = f"out/skandha-10/chapter-{ch_num:02d}.md"
    if not os.path.exists(f):
        print(f"Ch {ch_num:02d}: MISSING FILE")
        continue
    txt = open(f, encoding="utf-8").read()
    units = [l for l in txt.splitlines() if l.startswith("#### Verse ") or l.startswith("#### Verses ")]
    exp = expected.get("verses", {}).get(f"10.{ch_num}", [])
    
    # parse verses found
    found = set()
    for u in units:
        v_part = u.replace("#### Verse ", "").replace("#### Verses ", "").strip()
        if "-" in v_part:
            a, b = v_part.split("-")
            for x in range(int(a), int(b)+1):
                found.add(x)
        elif v_part.isdigit():
            found.add(int(v_part))
    
    missing = sorted(set(exp) - found)
    if missing:
        print(f"Ch {ch_num:02d}: expected {len(exp)}, found {len(found)}, missing {len(missing)}: {missing[:10]}...")
