import json, os

fn1 = 'work/vision/v1_p0701.json'
d1 = json.load(open(fn1, encoding='utf-8'))
d1['paragraphs_before_chapter_heading'] = 1
json.dump(d1, open(fn1, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)

fn2 = 'work/vision/v1_p0753.json'
d2 = json.load(open(fn2, encoding='utf-8'))
d2['paragraphs_before_chapter_heading'] = 2
json.dump(d2, open(fn2, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)

print("Fixed paragraphs_before_chapter_heading for 701 and 753.")
