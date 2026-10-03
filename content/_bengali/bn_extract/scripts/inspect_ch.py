import os, sys, json, io, re

TOOL = '.'
OUT = os.path.join(TOOL, 'out')
DIG = str.maketrans('০১২৩৪৫৬৭৮৯', '0123456789')
BN = '০১২৩৪৫৬৭৮৯'

def bn(n):
    return ''.join(BN[int(c)] if c.isdigit() else c for c in str(n))

def nums(s):
    return [int(x.translate(DIG)) for x in re.findall(r'[০-৯]+', s)]

expected = json.load(open(os.path.join(TOOL, 'data', 'expected_verses.json'), encoding='utf-8'))['verses']

# Let's inspect chapters 23, 27, 29, 31, 34 first
for ch in [23, 27, 29, 31, 34]:
    p = os.path.join(OUT, 'skandha-10', f'chapter-{ch:02d}.md')
    if os.path.exists(p):
        lines = open(p, encoding='utf-8').read().splitlines()
        print(f'=== CHAPTER {ch} HEADINGS ===')
        for l in lines:
            if l.startswith('### '):
                print('  ', l)
