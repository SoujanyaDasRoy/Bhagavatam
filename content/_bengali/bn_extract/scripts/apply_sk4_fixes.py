import json, os, re
from pathlib import Path

FIXES_PATH = Path('data/numbering_fixes.json')
fixes = json.load(open(FIXES_PATH, encoding='utf-8'))

sk4_fixes = [
    # 4.4
    {"section": "skandha-04", "chapter": 4, "printed": 2, "occurrence": 1, "is": "1-2", "why": "Gita Press combined opening verses 1 and 2."},
    {"section": "skandha-04", "chapter": 4, "printed": 9, "occurrence": 1, "is": 8, "why": "Gita Press printed 9 twice; first occurrence is verse 8."},
    # 4.5
    {"section": "skandha-04", "chapter": 5, "printed": 18, "occurrence": 1, "is": "18-19", "why": "Gita Press combined verses 18 and 19."},
    # 4.7
    {"section": "skandha-04", "chapter": 7, "printed": 2, "occurrence": 1, "is": "1-2", "why": "Gita Press combined opening verses 1 and 2."},
    # 4.9
    {"section": "skandha-04", "chapter": 9, "printed": 42, "occurrence": 1, "is": 41, "why": "Gita Press printed 42 twice; first occurrence is verse 41."},
    # 4.12
    {"section": "skandha-04", "chapter": 12, "printed": "15-16", "occurrence": 1, "is": "15-17", "why": "Gita Press combined verses 15 through 17."},
    # 4.13
    {"section": "skandha-04", "chapter": 13, "printed": 2, "occurrence": 1, "is": "2-3", "why": "Gita Press combined verses 2 and 3."},
    {"section": "skandha-04", "chapter": 13, "printed": "44-45", "occurrence": 1, "is": "44-46", "why": "Gita Press combined verses 44 through 46."},
    # 4.14
    {"section": "skandha-04", "chapter": 14, "printed": 1, "occurrence": 1, "is": "1-2", "why": "Gita Press combined opening verses 1 and 2."},
    # 4.16
    {"section": "skandha-04", "chapter": 16, "printed": 9, "occurrence": 1, "is": 8, "why": "Gita Press printed 9 twice; first occurrence is verse 8."},
    # 4.17
    {"section": "skandha-04", "chapter": 17, "printed": 3, "occurrence": 1, "is": "1-3", "why": "Gita Press combined opening verses 1 through 3."},
    {"section": "skandha-04", "chapter": 17, "printed": 4, "occurrence": 1, "is": "4-5", "why": "Gita Press combined verses 4 and 5."},
    # 4.19
    {"section": "skandha-04", "chapter": 19, "printed": 3, "occurrence": 1, "is": "3-4", "why": "Gita Press combined verses 3 and 4."},
    {"section": "skandha-04", "chapter": 19, "printed": 33, "occurrence": 1, "is": "31-33", "why": "Gita Press combined verses 31 through 33."},
    # 4.21
    {"section": "skandha-04", "chapter": 21, "printed": 49, "occurrence": 1, "is": "49-50", "why": "Gita Press combined verses 49 and 50."},
    # 4.22
    {"section": "skandha-04", "chapter": 22, "printed": 3, "occurrence": 1, "is": "3-4", "why": "Gita Press combined verses 3 and 4."},
    # 4.24
    {"section": "skandha-04", "chapter": 24, "printed": 22, "occurrence": 1, "is": "22-23", "why": "Gita Press combined verses 22 and 23."},
    # 4.29
    {"section": "skandha-04", "chapter": 29, "printed": 2, "occurrence": 1, "is": "1-2", "why": "Gita Press combined opening verses 1 and 2."},
    {"section": "skandha-04", "chapter": 29, "printed": 65, "occurrence": 1, "is": "65-67", "why": "Gita Press combined verses 65 through 67."},
]

existing_keys = {(f['section'], f['chapter'], str(f['printed']), f['occurrence']) for f in fixes}
for nf in sk4_fixes:
    key = (nf['section'], nf['chapter'], str(nf['printed']), nf['occurrence'])
    if key not in existing_keys:
        fixes.append(nf)
        existing_keys.add(key)

FIXES_PATH.write_text(json.dumps(fixes, ensure_ascii=False, indent=2), encoding='utf-8')
print(f"Updated fixes. Total: {len(fixes)}")
