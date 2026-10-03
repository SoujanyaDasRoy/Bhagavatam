import glob, json, os

for p in range(250, 350):
    fn = f'work/vision/v2_p{p:04d}.json'
    if os.path.exists(fn):
        d = json.load(open(fn, encoding='utf-8'))
        ch_raw = d.get('chapter_number')
        title = d.get('chapter_title')
        sa = d.get('sanskrit_verse_numbers')
        before = d.get('paragraphs_before_chapter_heading')
        paras = d.get('paragraphs', [])
        # check if contains অধ্যায়
        sample = paras[0][:50] if paras else 'EMPTY'
        if ch_raw or title or before:
            print(f'Page {p}: ch_raw={ch_raw}, title={title[:30] if title else None}, before={before}, sa={sa[:5] if sa else []}')
