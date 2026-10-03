import json, os
from pathlib import Path

FIXES_PATH = Path('data/numbering_fixes.json')
fixes = json.load(open(FIXES_PATH, encoding='utf-8'))

sk5_fixes = [
    # 5.3
    {"section": "skandha-05", "chapter": 3, "printed": 14, "occurrence": 1, "is": 13, "why": "Gita Press printed 14 twice; first occurrence is verse 13."},
    {"section": "skandha-05", "chapter": 3, "printed": 18, "occurrence": 1, "is": "18-19", "why": "Gita Press combined verses 18 and 19."},
    # 5.6
    {"section": "skandha-05", "chapter": 6, "printed": 4, "occurrence": 1, "is": "4-5", "why": "Gita Press combined verses 4 and 5."},
    # 5.10
    {"section": "skandha-05", "chapter": 10, "printed": 1, "occurrence": 1, "is": "1-2", "why": "Gita Press combined opening verses 1 and 2."},
    {"section": "skandha-05", "chapter": 10, "printed": 10, "occurrence": 1, "is": 9, "why": "Gita Press printed 10 twice; first occurrence is verse 9."},
    {"section": "skandha-05", "chapter": 10, "printed": 22, "occurrence": 1, "is": "22-23", "why": "Gita Press combined verses 22 and 23."},
    # 5.14
    {"section": "skandha-05", "chapter": 14, "printed": 44, "occurrence": 1, "is": "44-45", "why": "Gita Press combined verses 44 and 45."},
    # 5.17
    {"section": "skandha-05", "chapter": 17, "printed": 10, "occurrence": 1, "is": "10-11", "why": "Gita Press combined verses 10 and 11."},
    # 5.22
    {"section": "skandha-05", "chapter": 22, "printed": 16, "occurrence": 1, "is": "16-17", "why": "Gita Press combined verses 16 and 17."},
]

existing_keys = {(f['section'], f['chapter'], str(f['printed']), f['occurrence']) for f in fixes}
for nf in sk5_fixes:
    key = (nf['section'], nf['chapter'], str(nf['printed']), nf['occurrence'])
    if key not in existing_keys:
        fixes.append(nf)
        existing_keys.add(key)

FIXES_PATH.write_text(json.dumps(fixes, ensure_ascii=False, indent=2), encoding='utf-8')
print(f"Updated fixes with Skandha 5. Total: {len(fixes)}")
