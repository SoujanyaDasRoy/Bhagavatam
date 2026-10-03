import glob, json, os, re

DIG = str.maketrans('০১২৩৪৫৬৭৮৯', '0123456789')
def to_int(x):
    if isinstance(x, int):
        return x
    s = str(x).translate(DIG)
    s = re.sub(r'\D', '', s)
    return int(s) if s else None

pages = []
for p in range(129, 790):
    fn = f'work/vision/v2_p{p:04d}.json'
    if os.path.exists(fn):
        d = json.load(open(fn, encoding='utf-8'))
        d['_page_num'] = p
        pages.append(d)

print(f'Total pages loaded: {len(pages)}')

# Find all chapter start candidates
starts = []
for pg in pages:
    p = pg['_page_num']
    sa = [to_int(x) for x in pg.get('sanskrit_verse_numbers', []) if to_int(x) is not None]
    ch_num = pg.get('chapter_number')
    title = pg.get('chapter_title')
    before = pg.get('paragraphs_before_chapter_heading')
    colophon = pg.get('colophon')
    
    # Check if page has verse 1 in sa or in paragraphs
    has_v1_sa = 1 in sa
    has_v1_text = False
    for para in pg.get('paragraphs', []):
        if re.search(r'॥\s*[১1]\s*॥|॥\s*[১1]-[০-৯0-9]+\s*॥', para):
            has_v1_text = True
            break
            
    if has_v1_sa or has_v1_text or title or before:
        print(f"P{p:04d}: ch={ch_num} (type {type(ch_num).__name__}), title={repr(title)[:35]}, before={before}, sa={sa[:6]}, has_v1_text={has_v1_text}")
