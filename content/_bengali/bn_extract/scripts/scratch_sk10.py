import os, sys, json, io, re

TOOL = '.'
OUT = os.path.join(TOOL, 'out')
DIG = str.maketrans('০১২৩৪৫৬৭৮৯', '0123456789')
BN = '০১২৩৪৫৬৭৮৯'

def bn(n):
    return ''.join(BN[int(c)] if c.isdigit() else c for c in str(n))

def nums(s):
    return [int(x.translate(DIG)) for x in re.findall(r'[০-৯]+', s)]

def load_fixes():
    p = os.path.join(TOOL, 'data', 'numbering_fixes.json')
    return json.load(open(p, encoding='utf-8')) if os.path.exists(p) else []

def apply_fixes(text, section, chapter, fixes):
    notes = []
    for f in fixes:
        if f['section'] != section or f['chapter'] != chapter:
            continue
        head = '### শ্লোক ' + bn(f['printed'])
        head_norm = re.sub(r'[–—\s]', '-', head)
        seen = 0
        out = []
        for line in text.split('\n'):
            line_norm = re.sub(r'[–—\s]', '-', line.strip())
            if line_norm == head_norm:
                seen += 1
                if seen == f['occurrence']:
                    out.append('### শ্লোক ' + bn(f['is']))
                    out.append(f"<!-- numbering fix: printed {f['printed']}, is {f['is']}. {f['why']} -->")
                    notes.append(f"verse {f['is']}: {f['why']}")
                    continue
            out.append(line)
        text = '\n'.join(out)
    return text, notes

def units(text):
    res = []
    parts = re.split(r'^### শ্লোক ', text, flags=re.M)[1:]
    for blk in parts:
        head = blk.splitlines()[0]
        n = nums(head)
        if not n:
            continue
        res.append((n[0], n[-1], head.strip(), blk))
    return res

expected = json.load(open(os.path.join(TOOL, 'data', 'expected_verses.json'), encoding='utf-8'))['verses']
fixes = load_fixes()

section = 'skandha-10'
sdir = os.path.join(OUT, section)
for ch in range(1, 91):
    fn = f'chapter-{ch:02d}.md'
    p = os.path.join(sdir, fn)
    if not os.path.exists(p):
        print(f'Missing file {fn}')
        continue
    text = io.open(p, encoding='utf-8').read()
    text, _ = apply_fixes(text, section, ch, fixes)
    us = units(text)
    seen = set()
    for a, b, _, _ in us:
        seen.update(range(a, b + 1))
    exp = set(expected.get(f'10.{ch}', []))
    missing = sorted(exp - seen)
    extra = sorted(seen - exp)
    if missing:
        print(f'=== Ch {ch}: missing {missing}, extra {extra} ===')
        for m in missing:
            # find surrounding units
            surrounding = [h for a, b, h, _ in us if abs(a - m) <= 2 or abs(b - m) <= 2]
            print(f'   Target missing {m}, surrounding headings: {surrounding}')
