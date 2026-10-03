import os, sys, json, io, re
from pathlib import Path
import bn_extract as b

cfg = b.load_config()
expected = json.loads((b.DATA / "expected_verses.json").read_text(encoding="utf-8"))
fixes = json.loads((b.DATA / "numbering_fixes.json").read_text(encoding="utf-8"))

# Let's fix the few vision json metadata fields first
def fix_json_meta(p, ch, title=None):
    fn = f'work/vision/v2_p{p:04d}.json'
    if os.path.exists(fn):
        d = json.load(open(fn, encoding='utf-8'))
        d['chapter_number'] = ch
        if title:
            d['chapter_title'] = title
        json.dump(d, open(fn, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)

fix_json_meta(313, 23, 'যজ্ঞপত্নীগণের প্রতি কৃপা')
fix_json_meta(336, 27, 'শ্রীকৃষ্ণের অভিষেক')
fix_json_meta(344, 29, 'রাসলীলা আরম্ভ')
fix_json_meta(363, 31, 'গোপিকা-গীত')
fix_json_meta(613, 66, 'পৌণ্ড্র ও কাশীরাজ উদ্ধার')

# Run assemble
pages = b.load_pages(cfg)
chapters, unassigned = b.build_chapters(cfg, pages)

# Save markdown files
OUT = Path('out')
for ch in chapters:
    if ch.section == 'skandha-10':
        sdir = OUT / ch.section
        sdir.mkdir(parents=True, exist_ok=True)
        (sdir / f"chapter-{ch.number:02d}.md").write_text(b.chapter_md(cfg, ch, "extracted"), encoding="utf-8")

print("Assembly done. Checking Skandha 10 chapters:")
DIG = str.maketrans('০১২৩৪৫৬৭৮৯', '0123456789')
BN = '০১২৩৪৫৬৭৮৯'
def bn(n): return ''.join(BN[int(c)] if c.isdigit() else c for c in str(n))

def apply_fixes(text, section, chapter, fixes):
    for f in fixes:
        if f['section'] != section or f['chapter'] != chapter: continue
        head = '### শ্লোক ' + bn(f['printed'])
        head_norm = re.sub(r'[–—\s]', '-', head)
        seen, out = 0, []
        for line in text.split('\n'):
            line_norm = re.sub(r'[–—\s]', '-', line.strip())
            if line_norm == head_norm:
                seen += 1
                if seen == f['occurrence']:
                    out.append('### শ্লোক ' + bn(f['is']))
                    continue
            out.append(line)
        text = '\n'.join(out)
    return text

def units(text):
    res = []
    for blk in re.split(r'^### শ্লোক ', text, flags=re.M)[1:]:
        head = blk.splitlines()[0]
        n = [int(x.translate(DIG)) for x in re.findall(r'[০-৯]+', head)]
        if n: res.append((n[0], n[-1], head.strip(), blk))
    return res

missing_map = {}
for ch in range(1, 91):
    p = OUT / 'skandha-10' / f'chapter-{ch:02d}.md'
    if not p.exists():
        print(f"Ch {ch} missing!")
        continue
    text = apply_fixes(p.read_text(encoding='utf-8'), 'skandha-10', ch, fixes)
    us = units(text)
    seen = set()
    for a, b, _, _ in us: seen.update(range(a, b + 1))
    exp = set(expected['verses'].get(f'10.{ch}', []))
    missing = sorted(exp - seen)
    extra = sorted(seen - exp)
    if missing or extra:
        missing_map[ch] = (missing, extra, us)
        print(f"Ch {ch:02d}: missing {missing}, extra {extra}")

