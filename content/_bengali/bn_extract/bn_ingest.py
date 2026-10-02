"""
Page transcriptions without an API key.

The extraction tool (bn_extract.py) has one step that needs a model: reading a page image and returning a JSON record.
When a person or an assistant reads the rendered page instead, this script takes their transcription in a compact text
format and writes exactly the JSON that the tool expects (work/vision/vN_pNNNN.json), so `assemble` works unchanged.

Commands
  python bn_ingest.py render A B [--vol N]     prepare the page images for pages A to B (prints their paths)
  python bn_ingest.py ingest FILE              read a batch file (format below), write one JSON per page, run checks
  python bn_ingest.py todo A B [--vol N]       list pages in A..B that have no transcription yet

Batch file format: one block per page, in any order, UTF-8.

  @@ 1 30                      volume and page
  chapter: 2                   only when a chapter heading starts on this page
  title: ...                   the printed chapter title (with chapter:)
  skandha: তৃতীয় স্কন্ধ        only when a Skandha heading is printed on this page
  before: 0                    paragraphs above a chapter heading that belong to the previous chapter
  colophon: ...                closing line, exactly as printed
  sv: 1 2 3                    verse numbers printed under the Sanskrit shlokas on this page
  label: 1577                  printed page number
  cont: 1                      the first paragraph continues from the previous page
  note: ...                    anything unusual about the page
  fn: (১) ...                  a footnote (repeat the line for more)
  P: ...                       one printed paragraph of Bengali translation (repeat, in reading order, one line each)

Nothing is corrected here. The checks only report: verse markers found, Latin letters, Sanskrit-only text, empty pages.
"""
import json
import re
import sys
from pathlib import Path

import bn_extract as b

BN_DIGITS = str.maketrans('০১২৩৪৫৬৭৮৯', '0123456789')
MARKER = re.compile(r'॥\s*([০-৯]+(?:\s*[-–—]\s*[০-৯]+)*)\s*॥')


def parse_batch(text: str):
    pages = []
    cur = None
    for raw in text.splitlines():
        line = raw.rstrip()
        if line.startswith('@@'):
            m = re.match(r'@@\s*(\d+)\s+(\d+)\s*$', line)
            if not m:
                raise SystemExit(f'bad page header: {line!r}')
            cur = {'vol': m.group(1), 'page': int(m.group(2)), 'paragraphs': [], 'footnotes': [], 'sv': []}
            pages.append(cur)
            continue
        if cur is None or not line.strip():
            continue
        if line.startswith('P:'):
            cur['paragraphs'].append(line[2:].strip())
            continue
        m = re.match(r'(chapter|title|skandha|before|colophon|sv|label|cont|note|fn):\s*(.*)$', line)
        if not m:
            raise SystemExit(f'v{cur["vol"]} p{cur["page"]}: line not understood: {line[:60]!r}')
        k, v = m.group(1), m.group(2).strip()
        if k == 'fn':
            cur['footnotes'].append(v)
        elif k == 'sv':
            cur['sv'] = [int(x) for x in re.findall(r'\d+', v)]
        else:
            cur[k] = v
    return pages


def to_record(p: dict) -> dict:
    return {
        '_page': {'vol': p['vol'], 'page': p['page'], 'model': 'claude-agent'},
        'skandha_heading': p.get('skandha') or None,
        'chapter_number': int(p['chapter']) if p.get('chapter') else None,
        'chapter_title': p.get('title') or None,
        'paragraphs_before_chapter_heading': int(p.get('before') or 0),
        'colophon': p.get('colophon') or None,
        'sanskrit_verse_numbers': p['sv'],
        'paragraphs': p['paragraphs'],
        'first_paragraph_continues_previous_page': p.get('cont') in ('1', 'yes', 'true'),
        'footnotes': p['footnotes'],
        'page_label': p.get('label') or None,
        'layout_note': p.get('note') or None,
    }


def check(p: dict) -> list[str]:
    out = []
    body = ' '.join(p['paragraphs'])
    if not p['paragraphs']:
        out.append('no paragraphs')
        return out
    marks = MARKER.findall(body)
    if re.search(r'[A-Za-z]', re.sub(r'\[\?+\]', '', body)):
        out.append('Latin letters in the text')
    if re.search(r'[ऀ-ॣ०-ॿ]', body):   # the danda and double danda (U+0964, U+0965) are ordinary Bengali punctuation
        out.append('Devanagari in the text (Sanskrit copied in?)')
    if not marks and not p.get('cont') in ('1', 'yes', 'true') and len(body) > 600:
        out.append('long page with no verse marker')
    if '[?' in body:
        out.append(f'{body.count("[?")} uncertain reading(s)')
    nums = []
    for m in marks:
        parts = [int(x.translate(BN_DIGITS)) for x in re.findall(r'[০-৯]+', m)]
        nums.extend(range(parts[0], parts[-1] + 1) if len(parts) > 1 else parts)   # "২৮-৩০" covers 28, 29 and 30
    sv = set(p['sv'])
    if sv and nums:
        missing = sorted(sv - set(nums))
        # a verse can be translated in a unit that ends on the next page, so this is only a hint
        if missing and len(missing) <= 3:
            out.append(f'Sanskrit verse numbers not seen as translation markers on this page: {missing}')
    return out


def cmd_ingest(path: str) -> None:
    pages = parse_batch(Path(path).read_text(encoding='utf-8'))
    vis = b.WORK / 'vision'
    vis.mkdir(parents=True, exist_ok=True)
    total = 0
    for p in pages:
        rec = to_record(p)
        (vis / f'{b.page_key(p["vol"], p["page"])}.json').write_text(json.dumps(rec, ensure_ascii=False, indent=1), encoding='utf-8')
        marks = len(MARKER.findall(' '.join(p['paragraphs'])))
        total += marks
        issues = check(p)
        print(f'v{p["vol"]} p{p["page"]}: {len(p["paragraphs"])} paragraph(s), {marks} marker(s)' + (('  <-- ' + '; '.join(issues)) if issues else ''))
    print(f'{len(pages)} page(s) written, {total} verse marker(s)')


def cmd_render(a: int, bpage: int, vol: str) -> None:
    cfg = b.load_config()
    for pg in range(a, bpage + 1):
        print(b.prepared_image(cfg, vol, pg))


def cmd_todo(a: int, bpage: int, vol: str) -> None:
    todo = [pg for pg in range(a, bpage + 1) if not (b.WORK / 'vision' / f'{b.page_key(vol, pg)}.json').exists()]
    print(f'{len(todo)} of {bpage - a + 1} page(s) without a transcription:', todo[:80], '...' if len(todo) > 80 else '')


if __name__ == '__main__':
    args = sys.argv[1:]
    vol = '1'
    if '--vol' in args:
        i = args.index('--vol')
        vol = args[i + 1]
        del args[i:i + 2]
    if not args:
        sys.exit(__doc__)
    if args[0] == 'ingest':
        cmd_ingest(args[1])
    elif args[0] == 'render':
        cmd_render(int(args[1]), int(args[2]), vol)
    elif args[0] == 'todo':
        cmd_todo(int(args[1]), int(args[2]), vol)
    else:
        sys.exit(__doc__)
