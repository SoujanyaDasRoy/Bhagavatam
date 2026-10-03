import json, os, re
from pathlib import Path

FIXES_PATH = Path('data/numbering_fixes.json')
fixes = json.loads(FIXES_PATH.read_text(encoding='utf-8'))

# List of new fixes for Skandha 10:
new_fixes = [
    # Ch 02
    {"section": "skandha-10", "chapter": 2, "printed": 16, "occurrence": 1, "is": "16-17", "why": "Gita Press combined verses 16 and 17."},
    {"section": "skandha-10", "chapter": 2, "printed": 40, "occurrence": 1, "is": "40-41", "why": "Gita Press combined verses 40 and 41."},
    # Ch 05
    {"section": "skandha-10", "chapter": 5, "printed": 27, "occurrence": 1, "is": "27-28", "why": "Gita Press combined verses 27 and 28."},
    # Ch 08
    {"section": "skandha-10", "chapter": 8, "printed": 5, "occurrence": 1, "is": "5-6", "why": "Gita Press combined verses 5 and 6."},
    # Ch 10
    {"section": "skandha-10", "chapter": 10, "printed": 24, "occurrence": 1, "is": "24-25", "why": "Gita Press combined verses 24 and 25."},
    # Ch 11
    {"section": "skandha-10", "chapter": 11, "printed": 49, "occurrence": 1, "is": "49-50", "why": "Gita Press combined verses 49 and 50."},
    {"section": "skandha-10", "chapter": 11, "printed": 58, "occurrence": 1, "is": "58-59", "why": "Gita Press combined verses 58 and 59."},
    # Ch 13
    {"section": "skandha-10", "chapter": 13, "printed": 1, "occurrence": 1, "is": "1-2", "why": "Gita Press combined opening verses 1 and 2."},
    # Ch 15
    {"section": "skandha-10", "chapter": 15, "printed": 448, "occurrence": 1, "is": 48, "why": "Gita Press typo 448 for verse 48."},
    # Ch 16
    {"section": "skandha-10", "chapter": 16, "printed": 450, "occurrence": 1, "is": 45, "why": "Gita Press typo 450 for verse 45."},
    {"section": "skandha-10", "chapter": 16, "printed": 64, "occurrence": 1, "is": "64-65", "why": "Gita Press combined verses 64 and 65."},
    # Ch 24
    {"section": "skandha-10", "chapter": 24, "printed": 13, "occurrence": 1, "is": "13-14", "why": "Gita Press combined verses 13 and 14."},
    # Ch 29
    {"section": "skandha-10", "chapter": 29, "printed": 3, "occurrence": 1, "is": "1-3", "why": "Gita Press combined opening verses 1 through 3."},
    {"section": "skandha-10", "chapter": 29, "printed": 23, "occurrence": 1, "is": "23-24", "why": "Gita Press combined verses 23 and 24."},
    # Ch 31
    {"section": "skandha-10", "chapter": 31, "printed": 2, "occurrence": 1, "is": "2-3", "why": "Gita Press combined verses 2 and 3."},
    # Ch 39
    {"section": "skandha-10", "chapter": 39, "printed": 8, "occurrence": 1, "is": 7, "why": "Gita Press printed 8 twice; first occurrence is verse 7."},
    # Ch 41
    {"section": "skandha-10", "chapter": 41, "printed": 41, "occurrence": 1, "is": 40, "why": "Gita Press printed 41 twice; first occurrence is verse 40."},
    # Ch 43
    {"section": "skandha-10", "chapter": 43, "printed": 17, "occurrence": 1, "is": "17-18", "why": "Gita Press combined verses 17 and 18."},
    # Ch 44
    {"section": "skandha-10", "chapter": 44, "printed": 10, "occurrence": 1, "is": 9, "why": "Gita Press printed 10 twice; first occurrence is verse 9."},
    # Ch 45
    {"section": "skandha-10", "chapter": 45, "printed": 4, "occurrence": 1, "is": 3, "why": "Gita Press printed 4 twice; first occurrence is verse 3."},
    # Ch 46
    {"section": "skandha-10", "chapter": 46, "printed": 6, "occurrence": 1, "is": 5, "why": "Gita Press printed 6 twice; first occurrence is verse 5."},
    # Ch 47
    {"section": "skandha-10", "chapter": 47, "printed": 46, "occurrence": 2, "is": 48, "why": "Gita Press printed 46 twice; second occurrence is verse 48."},
    {"section": "skandha-10", "chapter": 47, "printed": 51, "occurrence": 1, "is": "51-52", "why": "Gita Press combined verses 51 and 52."},
    # Ch 48
    {"section": "skandha-10", "chapter": 48, "printed": 23, "occurrence": 1, "is": "23-24", "why": "Gita Press combined verses 23 and 24."},
    # Ch 49
    {"section": "skandha-10", "chapter": 49, "printed": 12, "occurrence": 1, "is": "12-13", "why": "Gita Press combined verses 12 and 13."},
    # Ch 50
    {"section": "skandha-10", "chapter": 50, "printed": 4, "occurrence": 1, "is": "4-5", "why": "Gita Press combined verses 4 and 5."},
    # Ch 51
    {"section": "skandha-10", "chapter": 51, "printed": 6, "occurrence": 1, "is": "6-7", "why": "Gita Press combined verses 6 and 7."},
    {"section": "skandha-10", "chapter": 51, "printed": 47, "occurrence": 1, "is": "47-48", "why": "Gita Press combined verses 47 and 48."},
    # Ch 53
    {"section": "skandha-10", "chapter": 53, "printed": 52, "occurrence": 1, "is": "52-53", "why": "Gita Press combined verses 52 and 53."},
    # Ch 54
    {"section": "skandha-10", "chapter": 54, "printed": 46, "occurrence": 1, "is": "46-47", "why": "Gita Press combined verses 46 and 47."},
    # Ch 55
    {"section": "skandha-10", "chapter": 55, "printed": 6, "occurrence": 1, "is": "6-7", "why": "Gita Press combined verses 6 and 7."},
    # Ch 58
    {"section": "skandha-10", "chapter": 58, "printed": 30, "occurrence": 1, "is": "30-31", "why": "Gita Press combined verses 30 and 31."},
    # Ch 59
    {"section": "skandha-10", "chapter": 59, "printed": 4, "occurrence": 1, "is": "4-5", "why": "Gita Press combined verses 4 and 5."},
    {"section": "skandha-10", "chapter": 59, "printed": 32, "occurrence": 1, "is": "32-33", "why": "Gita Press combined verses 32 and 33."},
    # Ch 62
    {"section": "skandha-10", "chapter": 62, "printed": 3, "occurrence": 1, "is": "3-4", "why": "Gita Press combined verses 3 and 4."},
    {"section": "skandha-10", "chapter": 62, "printed": 21, "occurrence": 1, "is": "21-22", "why": "Gita Press combined verses 21 and 22."},
    # Ch 63
    {"section": "skandha-10", "chapter": 63, "printed": 22, "occurrence": 1, "is": "22-23", "why": "Gita Press combined verses 22 and 23."},
    {"section": "skandha-10", "chapter": 63, "printed": 28, "occurrence": 1, "is": "28-29", "why": "Gita Press combined verses 28 and 29."},
    # Ch 66
    {"section": "skandha-10", "chapter": 66, "printed": 7, "occurrence": 1, "is": "7-8", "why": "Gita Press combined verses 7 and 8."},
    {"section": "skandha-10", "chapter": 66, "printed": 15, "occurrence": 1, "is": "15-16", "why": "Gita Press combined verses 15 and 16."},
    {"section": "skandha-10", "chapter": 66, "printed": 39, "occurrence": 1, "is": "39-40", "why": "Gita Press combined verses 39 and 40."},
    # Ch 67
    {"section": "skandha-10", "chapter": 67, "printed": 6, "occurrence": 1, "is": "6-7", "why": "Gita Press combined verses 6 and 7."},
    # Ch 69
    {"section": "skandha-10", "chapter": 69, "printed": 25, "occurrence": 1, "is": "25-26", "why": "Gita Press combined verses 25 and 26."},
    {"section": "skandha-10", "chapter": 69, "printed": 32, "occurrence": 1, "is": "32-33", "why": "Gita Press combined verses 32 and 33."},
    # Ch 71
    {"section": "skandha-10", "chapter": 71, "printed": 38, "occurrence": 1, "is": "38-39", "why": "Gita Press combined verses 38 and 39."},
    # Ch 73
    {"section": "skandha-10", "chapter": 73, "printed": 13, "occurrence": 1, "is": "13-14", "why": "Gita Press combined verses 13 and 14."},
    {"section": "skandha-10", "chapter": 73, "printed": 20, "occurrence": 1, "is": "20-21", "why": "Gita Press combined verses 20 and 21."},
    # Ch 74
    {"section": "skandha-10", "chapter": 74, "printed": 41, "occurrence": 1, "is": "41-42", "why": "Gita Press combined verses 41 and 42."},
    # Ch 75
    {"section": "skandha-10", "chapter": 75, "printed": 8, "occurrence": 1, "is": "8-9", "why": "Gita Press combined verses 8 and 9."},
    # Ch 79
    {"section": "skandha-10", "chapter": 79, "printed": 7, "occurrence": 1, "is": "7-8", "why": "Gita Press combined verses 7 and 8."},
    {"section": "skandha-10", "chapter": 79, "printed": 24, "occurrence": 1, "is": "24-25", "why": "Gita Press combined verses 24 and 25."},
    # Ch 80
    {"section": "skandha-10", "chapter": 80, "printed": 3, "occurrence": 1, "is": "3-4", "why": "Gita Press combined verses 3 and 4."},
    {"section": "skandha-10", "chapter": 80, "printed": 27, "occurrence": 1, "is": "27-28", "why": "Gita Press combined verses 27 and 28."},
    # Ch 81
    {"section": "skandha-10", "chapter": 81, "printed": "5-7", "occurrence": 1, "is": "5-8", "why": "Gita Press combined verses 5 through 8."},
    # Ch 82
    {"section": "skandha-10", "chapter": 82, "printed": 36, "occurrence": 1, "is": "36-37", "why": "Gita Press combined verses 36 and 37."},
    {"section": "skandha-10", "chapter": 82, "printed": 40, "occurrence": 1, "is": "40-41", "why": "Gita Press combined verses 40 and 41."},
    {"section": "skandha-10", "chapter": 82, "printed": 46, "occurrence": 2, "is": 48, "why": "Gita Press printed 46 twice; second occurrence is verse 48."},
    # Ch 83
    {"section": "skandha-10", "chapter": 83, "printed": 2, "occurrence": 1, "is": "2-3", "why": "Gita Press combined verses 2 and 3."},
    {"section": "skandha-10", "chapter": 83, "printed": 17, "occurrence": 1, "is": "17-18", "why": "Gita Press combined verses 17 and 18."},
    # Ch 84
    {"section": "skandha-10", "chapter": 84, "printed": 2, "occurrence": 1, "is": "1-3", "why": "Gita Press combined opening verses 1 through 3."},
    {"section": "skandha-10", "chapter": 84, "printed": 23, "occurrence": 1, "is": "23-24", "why": "Gita Press combined verses 23 and 24."},
    {"section": "skandha-10", "chapter": 84, "printed": 53, "occurrence": 1, "is": "53-54", "why": "Gita Press combined verses 53 and 54."},
    # Ch 85
    {"section": "skandha-10", "chapter": 85, "printed": 6, "occurrence": 1, "is": 5, "why": "Gita Press printed 6 twice; first occurrence is verse 5."},
    # Ch 86
    {"section": "skandha-10", "chapter": 86, "printed": 21, "occurrence": 1, "is": "21-22", "why": "Gita Press combined verses 21 and 22."},
    {"section": "skandha-10", "chapter": 86, "printed": 37, "occurrence": 1, "is": "37-38", "why": "Gita Press combined verses 37 and 38."},
    {"section": "skandha-10", "chapter": 86, "printed": 44, "occurrence": 1, "is": "44-45", "why": "Gita Press combined verses 44 and 45."},
    {"section": "skandha-10", "chapter": 86, "printed": 58, "occurrence": 1, "is": "58-59", "why": "Gita Press combined verses 58 and 59."},
    # Ch 87
    {"section": "skandha-10", "chapter": 87, "printed": 13, "occurrence": 1, "is": "13-14", "why": "Gita Press combined verses 13 and 14."},
    {"section": "skandha-10", "chapter": 87, "printed": 26, "occurrence": 1, "is": "26-27", "why": "Gita Press combined verses 26 and 27."},
]

# Append non-duplicate fixes
existing_keys = {(f['section'], f['chapter'], str(f['printed']), f['occurrence']) for f in fixes}
for nf in new_fixes:
    key = (nf['section'], nf['chapter'], str(nf['printed']), nf['occurrence'])
    if key not in existing_keys:
        fixes.append(nf)
        existing_keys.add(key)

FIXES_PATH.write_text(json.dumps(fixes, ensure_ascii=False, indent=2), encoding='utf-8')
print(f"Updated numbering_fixes.json. Total fixes: {len(fixes)}")
