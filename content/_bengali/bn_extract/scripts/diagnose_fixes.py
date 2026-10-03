import os, sys, json, io, re
from pathlib import Path
import bn_extract as b

cfg = b.load_config()
expected = json.loads((b.DATA / "expected_verses.json").read_text(encoding="utf-8"))
OUT = Path('out')

DIG = str.maketrans('০১২৩৪৫৬৭৮৯', '0123456789')
BN = '০১২৩৪৫৬৭৮৯'
def bn(n): return ''.join(BN[int(c)] if c.isdigit() else c for c in str(n))

def units(text):
    res = []
    for blk in re.split(r'^### শ্লোক ', text, flags=re.M)[1:]:
        head = blk.splitlines()[0]
        n = [int(x.translate(DIG)) for x in re.findall(r'[০-৯]+', head)]
        if n: res.append((n[0], n[-1], head.strip(), blk.strip()))
    return res

results = []
for ch in range(1, 91):
    p = OUT / 'skandha-10' / f'chapter-{ch:02d}.md'
    if not p.exists(): continue
    text = p.read_text(encoding='utf-8')
    us = units(text)
    seen = set()
    for a, b_val, _, _ in us: seen.update(range(a, b_val + 1))
    exp = set(expected['verses'].get(f'10.{ch}', []))
    missing = sorted(exp - seen)
    extra = sorted(seen - exp)
    if missing or extra:
        print(f"\n==================== CHAPTER {ch} (Missing {missing}, Extra {extra}) ====================")
        for idx, (a, b_val, head, blk) in enumerate(us):
            # Print unit if it's near missing or extra
            is_near = False
            for m in missing:
                if abs(a - m) <= 2 or abs(b_val - m) <= 2: is_near = True
            for e in extra:
                if a == e or b_val == e: is_near = True
            if is_near:
                lines = blk.splitlines()
                preview = lines[1] if len(lines) > 1 else lines[0]
                tail = lines[-1] if len(lines) > 2 else ''
                print(f"  Unit [{idx}]: head='{head}' (parsed {a}-{b_val})")
                print(f"     Preview: {preview[:70]}...")
                print(f"     Tail:    {tail[-70:]}")
