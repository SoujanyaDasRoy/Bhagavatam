import json, os
from pathlib import Path

# Fix page 753
fn = 'work/vision/v1_p0753.json'
d = json.load(open(fn, encoding='utf-8'))
d['paragraphs_before_chapter_heading'] = 1
json.dump(d, open(fn, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)

FIXES_PATH = Path('data/numbering_fixes.json')
fixes = json.load(open(FIXES_PATH, encoding='utf-8'))

sk6_fixes = [
    # 6.4
    {"section": "skandha-06", "chapter": 4, "printed": 25, "occurrence": 1, "is": 24, "why": "Gita Press printed 25 twice; first occurrence is verse 24."},
    # 6.6
    {"section": "skandha-06", "chapter": 6, "printed": "30-31", "occurrence": 1, "is": "29-31", "why": "Gita Press combined verses 29 through 31."},
    # 6.7
    {"section": "skandha-06", "chapter": 7, "printed": 32, "occurrence": 1, "is": "32-33", "why": "Gita Press combined verses 32 and 33."},
    # 6.8
    {"section": "skandha-06", "chapter": 8, "printed": 7, "occurrence": 1, "is": "7-9", "why": "Gita Press combined verses 7 through 9."},
    # 6.9
    {"section": "skandha-06", "chapter": 9, "printed": 32, "occurrence": 1, "is": 31, "why": "Gita Press printed 32 twice; first occurrence is verse 31."},
    # 6.11
    {"section": "skandha-06", "chapter": 11, "printed": 5, "occurrence": 2, "is": 6, "why": "Gita Press printed 5 twice; second occurrence is verse 6."},
    # 6.14
    {"section": "skandha-06", "chapter": 14, "printed": 26, "occurrence": 1, "is": 25, "why": "Gita Press printed 26 twice; first occurrence is verse 25."},
    {"section": "skandha-06", "chapter": 14, "printed": 48, "occurrence": 1, "is": "48-49", "why": "Gita Press combined verses 48 and 49."},
    # 6.15
    {"section": "skandha-06", "chapter": 15, "printed": 7, "occurrence": 2, "is": 8, "why": "Gita Press printed 7 twice; second occurrence is verse 8."},
    # 6.16
    {"section": "skandha-06", "chapter": 16, "printed": 25, "occurrence": 1, "is": 24, "why": "Gita Press printed 25 twice; first occurrence is verse 24."},
    {"section": "skandha-06", "chapter": 16, "printed": 63, "occurrence": 1, "is": 62, "why": "Gita Press printed 63 twice; first occurrence is verse 62."},
    # 6.17
    {"section": "skandha-06", "chapter": 17, "printed": "2-3", "occurrence": 1, "is": "2-4", "why": "Gita Press combined verses 2 through 4."},
]

existing_keys = {(f['section'], f['chapter'], str(f['printed']), f['occurrence']) for f in fixes}
for nf in sk6_fixes:
    key = (nf['section'], nf['chapter'], str(nf['printed']), nf['occurrence'])
    if key not in existing_keys:
        fixes.append(nf)
        existing_keys.add(key)

FIXES_PATH.write_text(json.dumps(fixes, ensure_ascii=False, indent=2), encoding='utf-8')
print(f"Updated fixes with Skandha 6. Total: {len(fixes)}")
