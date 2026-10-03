import os, sys, json, io, re

import bn_extract as b

cfg = b.load_config()
expected = json.loads((b.DATA / "expected_verses.json").read_text(encoding="utf-8"))
pages = b.load_pages(cfg)

# Let's test building chapters with enhanced chapter detection
chapters, unassigned = b.build_chapters(cfg, pages)

sk10_chs = [ch for ch in chapters if ch.section == 'skandha-10']
print(f'Total skandha-10 chapters built: {len(sk10_chs)}')
for ch in sk10_chs:
    problems = b.check_chapter(ch, expected)
    if problems:
        print(f'Ch {ch.number:02d}: {problems}')
