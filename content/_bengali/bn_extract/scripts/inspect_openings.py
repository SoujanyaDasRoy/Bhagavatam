import json

def show(p_list):
    for p in p_list:
        f = f"work/vision/v2_p{p:04d}.json"
        d = json.load(open(f, encoding="utf-8"))
        print(f"p{p:04d}: sa={d.get('sanskrit_verse_numbers')} | paras={len(d.get('paragraphs', []))}")
        for i, para in enumerate(d.get('paragraphs', [])):
            print(f"   para[{i}]: {para[:70]}...")

print("=== Ch 23 start ===")
show([313, 314, 315])

print("\n=== Ch 29 start ===")
show([343, 344])

print("\n=== Ch 31 start ===")
show([366, 367, 368])

print("\n=== Ch 34 start ===")
show([388, 389, 390])
