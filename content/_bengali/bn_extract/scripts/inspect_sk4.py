import json, os, re
from pathlib import Path

OUT = Path('out/skandha-04')
expected = json.load(open('data/expected_verses.json', encoding='utf-8'))['verses']

DIG = str.maketrans('০১২৩৪৫৬৭৮৯', '0123456789')
BN = '০১২৩৪৫৬৭৮৯'

def units(text):
    res = []
    for blk in re.split(r'^### শ্লোক ', text, flags=re.M)[1:]:
        head = blk.splitlines()[0]
        n = [int(x.translate(DIG)) for x in re.findall(r'[০-৯]+', head)]
        if n: res.append((n[0], n[-1], head.strip(), blk.strip()))
    return res

missing_chapters = [4, 5, 7, 9, 12, 13, 14, 16, 17, 19, 21, 22, 24, 29]

for ch in missing_chapters:
    p = OUT / f'chapter-{ch:02d}.md'
    text = p.read_text(encoding='utf-8')
    us = units(text)
    seen = set()
    for a, b_val, _, _ in us: seen.update(range(a, b_val + 1))
    exp = set(expected.get(f'4.{ch}', []))
    missing = sorted(exp - seen)
    print(f"\n=================== CHAPTER 4.{ch} (Missing: {missing}) ===================")
    for idx, (a, b_val, head, blk) in enumerate(us):
        # check if near missing
        if any(abs(a - m) <= 2 or abs(b_val - m) <= 2 for m in missing):
            lines = blk.splitlines()
            body = lines[1] if len(lines) > 1 else lines[0]
            tail = lines[-1] if len(lines) > 2 else ''
            print(f"  [{idx}] Head: '{head}' ({a}-{b_val}) | {body[:60]}... | {tail[-40:]}")
