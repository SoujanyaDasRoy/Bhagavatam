"""
Adds the Gita Press Bengali translation to content.db.

Source: content/bn/skandha-NN/chapter-NN.md (skandha-00 is the Mahatmya). Only chapters whose extraction has been checked are placed there.
Two file formats are read:
  - the earlier extraction ("#### শ্লোক ১২", translation under a label), used for Skandha 1 to 3.11;
  - the extraction tool's output ("### শ্লোক ১২" or a range "### শ্লোক ২৮–৩০", an HTML comment, then the text).
The source files are never changed. For the database copy:
  - the printed verse marker ("॥ १२ ॥") is turned into a sentence end, because the app shows verse numbers on their own;
  - lines of one paragraph are joined, paragraphs are kept (the same cleaning the Hindi text gets);
  - a Bengali unit that covers several verses puts the translation on the first verse and the others point back to it (bn_from),
    exactly as English does (en_from);
  - the Hindi edition groups some verses into one row (the Mahatmya has rows like 28-30), while the Bengali print numbers them one by one:
    Bengali units are mapped onto database rows by the verse numbers they cover.
Adds: verse.bn, verse.bn_from, chapter.title_bn. Safe to run again.

Usage: python add_bengali.py [path-to-content.db] [content_version]   (default: ../content.db)
"""
import glob
import json
import os
import re
import sqlite3
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
DIGITS = str.maketrans('০১২৩৪৫৬৭৮৯', '0123456789')
# "॥ १२ ॥" and, for joint verses, "॥ ७-८ ॥" or "॥ ४३-४४-४५ ॥"
MARKER = re.compile(r'\s*॥\s*[০-৯0-9]+(?:\s*[-–—]\s*[০-৯0-9]+)*\s*॥\s*')
# A translation that is only a bracketed note: "[শ্লোক ১২-১৫ একান্বয়ে অনুবাদিত]" or "[২৭ নং শ্লোকের সাথে অন্বয় ও অনুবাদ দ্রষ্টব্য]".
NOTE = re.compile(r'^\[[^\]]*\]$')
LABEL = '**গীতা প্রেস বঙ্গানুবাদ:**'


def translation_of(block):
    """The lines after the translation label, up to the next divider or heading (the colophon follows the last verse)."""
    lines = block.splitlines()
    start = next((i for i, l in enumerate(lines) if l.strip().startswith(LABEL)), None)
    if start is None:
        return ''
    out = []
    for l in lines[start + 1:]:
        if l.strip() == '---' or l.lstrip().startswith('#'):
            break
        out.append(l)
    return '\n'.join(out).strip()


def nums(s):
    return [int(x.translate(DIGITS)) for x in re.findall(r'[০-৯0-9]+', s)]


def clean_translation(body):
    """Join wrapped lines, keep paragraphs, and turn printed verse markers into sentence ends."""
    paras = [re.sub(r'\s*\n\s*', ' ', p).strip() for p in re.split(r'\n\s*\n', body.strip())]
    text = '\n\n'.join(p for p in paras if p)
    # A closing quote printed inside the marker ("॥' ৪২ ॥"): keep the quote, end the sentence before it.
    text = re.sub(r"॥\s*(['’\"”]+)\s*[০-৯0-9]+(?:\s*[-–—]\s*[০-৯0-9]+)*\s*॥", lambda m: '।' + m.group(1) + ' ', text)

    # Replace from the back so earlier offsets stay valid.
    for m in reversed(list(MARKER.finditer(text))):
        before = text[:m.start()].rstrip()
        rest = text[m.end():]
        end = '' if before.endswith(('।', '?', '!')) else '।'
        text = before + end + (' ' + rest.lstrip() if rest.strip() else '')
    text = text.strip()
    if text and text[-1] not in '।?!"”’)':
        text += '।'
    return text


def front_title(t):
    fm = re.match(r'---\n(.*?)\n---', t, re.S)
    if not fm:
        return ''
    m = re.search(r'^title:\s*"?(.*?)"?\s*$', fm.group(1), re.M)
    return m.group(1).strip() if m else ''


def parse_units(t):
    """Extraction-tool format. Returns (title, units): (first number, last number, text) in reading order."""
    units = []
    for blk in re.split(r'^### শ্লোক ', t, flags=re.M)[1:]:
        lines = blk.splitlines()
        n = nums(lines[0])
        if not n:
            continue
        body = []
        for l in lines[1:]:
            if l.strip() == '---' or l.lstrip().startswith('###'):
                break
            if l.lstrip().startswith('<!--'):
                continue
            body.append(l)
        units.append((n[0], n[-1], clean_translation('\n'.join(body))))
    return front_title(t), units


def parse_blocks(t):
    """Earlier-extraction format. Returns (title, blocks): first verse number -> dict(text, frm)."""
    blocks = {}   # first verse number -> dict(text, frm)
    groups = {}   # verse number -> carrier of a joint heading
    for blk in re.split(r'^#### ', t, flags=re.M)[1:]:
        head = blk.splitlines()[0]
        n = nums(head.split('(')[0])
        if not n:
            continue
        first = n[0]
        body = translation_of(blk)
        frm = None
        if NOTE.match(body):
            body_nums = nums(body)
            frm = body_nums[0] if body_nums else None
            body = ''
        if len(n) > 1:
            for other in n[1:]:
                groups.setdefault(other, first)
        if first in blocks:
            continue   # a repeated heading: keep the first
        blocks[first] = dict(text=clean_translation(body) if body else '', frm=frm)
    for num, b in blocks.items():
        if not b['text'] and b['frm'] is None and num in groups:
            b['frm'] = groups[num]
    return front_title(t), blocks


def blocks_from_units(units, rows):
    """Map Bengali units onto database rows. rows: list of (num, num_end). Returns (blocks, unmatched unit starts)."""
    blocks = {}
    covered = set()
    for num, end in rows:
        own = [u for u in units if num <= u[0] <= end and u[2]]
        if own:
            blocks[num] = dict(text=' '.join(u[2] for u in own), frm=None)
            covered.update(u[0] for u in own)
            continue
        # no unit starts in this row: a unit that began earlier may run over it ("৪-৫" when the database has 4 and 5 apart)
        prev = [u for u in units if u[0] < num <= u[1]]
        if prev:
            first = prev[0][0]
            carrier = next((n for n, e in rows if n <= first <= e), first)
            blocks[num] = dict(text='', frm=carrier)
    unmatched = [u[0] for u in units if u[0] not in covered and not any(n <= u[0] <= e for n, e in rows)]
    return blocks, unmatched


def apply(db_path, src_root=None, version=None):
    src_root = src_root or os.path.join(ROOT, 'bn')
    db = sqlite3.connect(db_path)
    cols = {r[1] for r in db.execute('PRAGMA table_info(verse)')}
    if 'bn' not in cols:
        db.execute('ALTER TABLE verse ADD COLUMN bn TEXT')
    if 'bn_from' not in cols:
        db.execute('ALTER TABLE verse ADD COLUMN bn_from INTEGER')
    if 'title_bn' not in {r[1] for r in db.execute('PRAGMA table_info(chapter)')}:
        db.execute('ALTER TABLE chapter ADD COLUMN title_bn TEXT')
    db.execute('UPDATE verse SET bn = NULL, bn_from = NULL')
    db.execute('UPDATE chapter SET title_bn = NULL')
    stats = dict(chapters=0, verses_with_text=0, verses_pointing=0, verses_without=0, bengali_blocks_unmatched=0)
    issues = []
    for path in sorted(glob.glob(os.path.join(src_root, 'skandha-*', 'chapter-*.md'))):
        m = re.search(r'skandha-(\d+)[/\\]chapter-(\d+)\.md$', path)
        s, c = int(m.group(1)), int(m.group(2))
        t = open(path, encoding='utf-8').read()
        rows = [(r[0], r[1]) for r in db.execute('SELECT num, num_end FROM verse WHERE skandha=? AND chapter=? ORDER BY num', (s, c))]
        if not rows:
            issues.append((s, c, 'chapter not in database'))
            continue
        if '\n### শ্লোক ' in t and '#### শ্লোক ' not in t:
            title, units = parse_units(t)
            blocks, unmatched = blocks_from_units(units, rows)
        else:
            title, blocks = parse_blocks(t)
            unmatched = [n for n in blocks if n not in {r[0] for r in rows}]
        for num, _end in rows:
            b = blocks.get(num)
            if b is None:
                stats['verses_without'] += 1
                issues.append((s, c, 'no bengali block for verse', num))
                continue
            if b['text']:
                db.execute('UPDATE verse SET bn=?, bn_from=NULL WHERE skandha=? AND chapter=? AND num=?', (b['text'], s, c, num))
                stats['verses_with_text'] += 1
            elif b['frm'] is not None:
                db.execute('UPDATE verse SET bn=\'\', bn_from=? WHERE skandha=? AND chapter=? AND num=?', (b['frm'], s, c, num))
                stats['verses_pointing'] += 1
            else:
                stats['verses_without'] += 1
                issues.append((s, c, 'empty bengali translation', num))
        for num in unmatched:
            stats['bengali_blocks_unmatched'] += 1
            issues.append((s, c, 'bengali block with no verse in the database', num))
        if title:
            db.execute('UPDATE chapter SET title_bn=? WHERE skandha=? AND chapter=?', (title, s, c))
        stats['chapters'] += 1
    if version is not None:
        db.execute("UPDATE meta SET value=? WHERE key='content_version'", (str(version),))
    db.execute("UPDATE meta SET value=? WHERE key='languages'", ('sa,hi,en,bn',))
    db.execute("INSERT OR REPLACE INTO meta VALUES('bengali_stats', ?)", (json.dumps(stats),))
    db.commit()
    db.close()
    json.dump(issues, open(os.path.join(HERE, 'bengali_issues.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
    return stats, issues


if __name__ == '__main__':
    path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, 'content.db')
    stats, issues = apply(path, version=int(sys.argv[2]) if len(sys.argv) > 2 else None)
    print(stats, 'issues', len(issues))
    for i in issues[:20]:
        print('  ', i)
