import json, os

for p in [606, 626, 636]:
    fn = f'work/vision/v1_p{p:04d}.json'
    d = json.load(open(fn, encoding='utf-8'))
    d['paragraphs_before_chapter_heading'] = 1
    json.dump(d, open(fn, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)

print("Fixed paragraphs_before_chapter_heading for 606, 626, 636.")
