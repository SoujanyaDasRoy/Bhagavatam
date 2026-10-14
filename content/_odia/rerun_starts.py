import sys, json, os
from pathlib import Path
from multiprocessing import Pool
sys.path.insert(0, '.')
import odia_ocr as o
vol = int(sys.argv[1]); lo = int(sys.argv[2]) if len(sys.argv) > 2 else 1; hi = int(sys.argv[3]) if len(sys.argv) > 3 else 9999
W = Path(os.environ.get("ODIA_WORK", "work")) / "vision"
def cands():
    prev = None; out = []
    for f in sorted(W.glob(f"v{vol}_p*.json")):
        d = json.loads(f.read_text(encoding="utf-8")); p = d["_page"]["page"]
        if not (lo <= p <= hi): prev = d; continue
        nums = d.get("sanskrit_verse_numbers", [])
        hit = d.get("colophon") or d.get("chapter_title")
        fn = " ".join(d["footnotes"]) if isinstance(d.get("footnotes"), list) else ""
        if fn.strip()[:3] in ("ଇତି","କତି","ଇଜି") or "ତି ଶ" in fn[:20]: hit = True
        if prev is not None:
            pn = prev.get("sanskrit_verse_numbers", [])
            if nums and pn and nums[0] <= 4 and max(pn) >= 6: hit = True
            if d.get("running_head_chapter") != prev.get("running_head_chapter"): hit = True
        if not nums: hit = True
        if hit: out.append(p)
        prev = d
    return out
def go(p):
    r, raw = o.extract_page(vol, p); o.save_page(r, raw); return p
if __name__ == "__main__":
    c = cands(); print(len(c), "pages"); 
    with Pool(2) as pool:
        for i, p in enumerate(pool.imap_unordered(go, c)):
            if i % 20 == 0: print(i, p, flush=True)
