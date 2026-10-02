"""
Moves checked Bengali chapters from the extraction tool's output into content/bn, where add_bengali.py reads them.

  python promote_bengali.py                promote every chapter that is complete (all verse numbers present)
  python promote_bengali.py --report       only report, copy nothing
  python promote_bengali.py --force        also replace chapters that are already in content/bn

A chapter is complete when, after the numbering fixes in bn_extract/data/numbering_fixes.json, every verse number of the
Hindi edition has a Bengali translation unit. Incomplete chapters are listed and left out, so the app never shows half a chapter
as if it were whole. The text is copied as extracted (flags and all); only a printed verse number that is demonstrably wrong
is relabelled, and the file says so. Flagged verses are listed in content/bn/REVIEW.md.
"""
import io
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
TOOL = os.path.join(ROOT, '_bengali', 'bn_extract')
OUT = os.path.join(TOOL, 'out')
DST = os.path.join(ROOT, 'bn')
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
    """Relabel unit headings whose printed number is wrong. Returns (text, notes)."""
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
    """(first number, last number, flags) for every unit heading, in order."""
    res = []
    parts = re.split(r'^### শ্লোক ', text, flags=re.M)[1:]
    for blk in parts:
        head = blk.splitlines()[0]
        n = nums(head)
        if not n:
            continue
        flags = re.search(r'FLAGS: ([^>]*?)\s*-->', blk)
        score = re.search(r'ocr-agreement: ([0-9.]+)', blk)
        res.append((n[0], n[-1], flags.group(1) if flags else '', float(score.group(1)) if score else None, blk))
    return res


def main():
    report_only = '--report' in sys.argv
    expected = json.load(open(os.path.join(TOOL, 'data', 'expected_verses.json'), encoding='utf-8'))['verses']
    fixes = load_fixes()
    promoted, left, review = [], [], []
    for section in sorted(os.listdir(OUT)):
        sdir = os.path.join(OUT, section)
        if not os.path.isdir(sdir) or section == 'mahatmya-skanda':
            continue
        sk = 0 if section == 'mahatmya' else int(section.split('-')[1])
        for fn in sorted(os.listdir(sdir)):
            m = re.match(r'chapter-(\d+)\.md$', fn)
            if not m:
                continue
            ch = int(m.group(1))
            target = os.path.join(DST, f'skandha-{sk:02d}', f'chapter-{ch:02d}.md')
            if os.path.exists(target) and '--force' not in sys.argv:
                continue   # a chapter already in the app is never replaced silently (use --force to redo one)
            text = io.open(os.path.join(sdir, fn), encoding='utf-8').read()
            text, notes = apply_fixes(text, section, ch, fixes)
            us = units(text)
            seen = set()
            for a, b, *_ in us:
                seen.update(range(a, b + 1))
            exp = set(expected.get(f'{sk}.{ch}', []))
            missing = sorted(exp - seen)
            extra = sorted(seen - exp)
            if not exp or missing:
                left.append((sk, ch, f'{len(missing)} verse number(s) missing' if exp else 'no Hindi reference'))
                continue
            promoted.append((sk, ch, len(us), len(extra)))
            for a, b, flags, score, blk in us:
                real = [x for x in flags.split(';') if x.strip() and not x.strip().startswith('continued')]
                if real:
                    review.append((sk, ch, a if a == b else f'{a}-{b}', score, '; '.join(x.strip() for x in real)))
            for n in notes:
                review.append((sk, ch, 'numbering', None, n))
            if not report_only:
                d = os.path.join(DST, f'skandha-{sk:02d}')
                os.makedirs(d, exist_ok=True)
                io.open(os.path.join(d, f'chapter-{ch:02d}.md'), 'w', encoding='utf-8', newline='\n').write(text)
    print('promoted', len(promoted), 'chapters;', 'left out', len(left))
    for sk, ch, why in left[:40]:
        print('  left out', f'{sk}.{ch}', why)
    if not report_only:
        old = os.path.join(DST, 'REVIEW.md')
        lines = ['# Bengali: verses to check against the page images', '',
                 'Every verse the extraction checker flagged in chapters now in the app (OCR agreement below 0.45, uncertain readings, structure problems), and every numbering correction.',
                 'Chapters from the earlier extraction (Skandha 1 to 3.11) are listed in `../_bengali/bengali_flagged_verses.csv`.', '',
                 '| Skandha.Chapter | Verse | OCR agreement | What was flagged |', '|---|---|---|---|']
        for sk, ch, v, score, why in sorted(review, key=lambda r: (r[0], r[1])):
            lines.append(f'| {sk}.{ch} | {v} | {"" if score is None else f"{score:.2f}"} | {why} |')
        io.open(old, 'w', encoding='utf-8', newline='\n').write('\n'.join(lines) + '\n')
        print('review items:', len(review), '->', old)


if __name__ == '__main__':
    main()
