import json

def check_pages(start, end):
    for p in range(start, end+1):
        f = f"work/vision/v2_p{p:04d}.json"
        d = json.load(open(f, encoding="utf-8"))
        print(f"p{p:04d}: ch={d.get('chapter_number')} | tit={d.get('chapter_title')} | paras={len(d.get('paragraphs', []))} | sa={d.get('sanskrit_verse_numbers')}")

print("--- Ch 31 (p368-p370) ---")
check_pages(368, 370)

print("\n--- Ch 27 (p336-p340) ---")
check_pages(336, 340)

print("\n--- Ch 29 (p344-p354) ---")
check_pages(344, 354)
