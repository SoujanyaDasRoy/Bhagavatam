import json, os, re
from pathlib import Path
import bn_extract as b

cfg = b.load_config()
expected = json.loads((b.DATA / "expected_verses.json").read_text(encoding="utf-8"))
fixes = json.loads((b.DATA / "numbering_fixes.json").read_text(encoding="utf-8"))
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

print("Detailed scan of missing verses across all Skandha 10 chapters:")

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
    if not missing and not extra: continue
    
    print(f"\n--- SKANDHA 10 CHAPTER {ch} ---")
    print(f"Missing: {missing}")
    print(f"Extra:   {extra}")
    print("All Headings in order:")
    h_list = [h for _, _, h, _ in us]
    print("  " + ", ".join(h_list))
