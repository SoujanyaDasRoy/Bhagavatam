"""Independent check of the verses filled by fill_odia_gaps.py and fill_gaps.py.

    py tools/content/verify_gaps.py

The fill scripts used a vision model. This check uses a different reader: stock Tesseract (ben / ori, tessdata_best) reads the same
pages, and every filled verse is scored by how many of its words appear (allowing small spelling differences) in that page text.
  score >= 0.60  supported    the page really contains these words
  score <  0.60  CHECK        possible slip, wrong verse, or a page Tesseract cannot read: a person looks at these first
The scores are in content/_odia/work_v/gaps_verify.csv. Nothing is written to the databases.
"""
import csv
import difflib
import json
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
W = ROOT / "content" / "_odia" / "work_v"
TESS = r"C:\Program Files\Tesseract-OCR\tesseract.exe"
os.environ.setdefault("TESSDATA_PREFIX", str(ROOT / "content" / "_localocr" / "work" / "tessdata_best"))
sys.path.insert(0, str(ROOT / "tools"))
sys.path.insert(0, str(ROOT / "content" / "_odia"))
sys.stdout.reconfigure(encoding="utf-8")
import fill_gaps as fg  # noqa: E402

TESS_LANG = {"bn": "ben", "or": "ori"}
_ocr = {}


def ocr(lang, vol, page):
    k = (lang, vol, page)
    if k not in _ocr:
        img = fg.image(lang, vol, page)
        with tempfile.TemporaryDirectory() as d:
            subprocess.run([TESS, str(img), str(Path(d) / "o"), "-l", TESS_LANG[lang], "--oem", "1", "--psm", "6"],
                           check=True, capture_output=True)
            _ocr[k] = (Path(d) / "o.txt").read_text(encoding="utf-8")
    return _ocr[k]


def words(t):
    return [w for w in re.findall(r"[ঀ-৿଀-୿]+", t) if len(w) >= 3]


def score(text, page_text):
    pool = list(dict.fromkeys(words(page_text)))
    mine = words(text)
    if not mine or not pool:
        return 0.0
    hit = sum(1 for w in mine if w in pool or difflib.get_close_matches(w, pool, 1, 0.78))
    return hit / len(mine)


def filled():
    out = {"bn": {}, "or": {}}
    for f in ("gaps_filled.json", "gaps2_filled.json"):
        p = W / f
        if not p.exists():
            continue
        d = json.loads(p.read_text(encoding="utf-8"))
        if f == "gaps_filled.json":
            d = {"or": d}
        for lang, g in d.items():
            out[lang].update(g)
    return out


def main():
    rows = []
    for lang, g in filled().items():
        for key, text in g.items():
            s, c, n = map(int, key.split("."))
            path = fg.chapter_file(lang, s, c)
            vol, pages, srcs = fg.pages_of(path, fg.LANGS[lang]["digits"])
            before = max((pg for v, pg in pages.items() if v < n), default=srcs[0])
            after = min((pg for v, pg in pages.items() if v > n), default=srcs[-1] + 1)
            best = 0.0
            for pg in range(before, after + 1):
                try:
                    best = max(best, score(text, ocr(lang, vol, pg)))
                except Exception as e:
                    print("  ocr failed", lang, vol, pg, str(e)[:60])
            rows.append((lang, key, round(best, 2), "supported" if best >= 0.60 else "CHECK", text[:60]))
            print(f"{lang} {key:>9} {best:.2f} {'supported' if best >= 0.6 else 'CHECK'}", flush=True)
    with open(W / "gaps_verify.csv", "w", encoding="utf-8", newline="") as f:
        csv.writer(f).writerows([("lang", "verse", "score", "result", "start")] + rows)
    for lang in ("bn", "or"):
        r = [x for x in rows if x[0] == lang]
        print(f"{lang}: {sum(1 for x in r if x[3] == 'supported')} supported, {sum(1 for x in r if x[3] == 'CHECK')} to check, of {len(r)}")


if __name__ == "__main__":
    main()
