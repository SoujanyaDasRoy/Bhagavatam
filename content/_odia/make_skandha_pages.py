#!/usr/bin/env python3
"""Write data/skandha_pages.json: for every skandha, the PDF page ranges [[vol, first, last], ...] it occupies.

Source: the source_pages lines in the assembled chapter files (content/or/skandha-NN/chapter-NN.md), so the ranges are
what the first (Tesseract) assembly found. Each range is extended by MARGIN pages at the end so a closing colophon or a
footnote that spilled onto the next page is not cut off (clamped to the volume length; ranges of neighbouring skandhas
overlap by a page at the border, which is fine: a page is only read once).

    py make_skandha_pages.py            (run from content/_odia)
The output holds page numbers only, no book text, and is committed.
"""
import glob
import json
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
OR = HERE.parent / "or"
CFG = json.loads((HERE / "config.json").read_text(encoding="utf-8"))
MARGIN = 1


def main() -> int:
    spans = {}
    for f in sorted(glob.glob(str(OR / "skandha-*" / "chapter-*.md"))):
        t = Path(f).read_text(encoding="utf-8")
        sk = int(re.search(r"^skandha:\s*(\d+)", t, re.M).group(1))
        line = re.search(r"^source_pages:.*$", t, re.M)
        for v, p in re.findall(r'"v(\d):(\d+)"', line.group(0) if line else ""):
            lo, hi = spans.setdefault(sk, {}).setdefault(int(v), [int(p), int(p)])
            spans[sk][int(v)] = [min(lo, int(p)), max(hi, int(p))]
    out = {}
    for sk in sorted(spans):
        out[str(sk)] = [[v, lo, min(hi + MARGIN, CFG["volumes"][str(v)]["pages"])] for v, (lo, hi) in sorted(spans[sk].items())]
    (HERE / "data" / "skandha_pages.json").write_text(json.dumps(out, indent=1), encoding="utf-8")
    total = sum(y - x + 1 for k in range(5, 13) for _, x, y in out[str(k)])
    print(f"skandhas {sorted(map(int, out))}; skandha 5 to 12 cover {total} pages")
    for k in range(5, 13):
        print(f"  skandha {k}: {out[str(k)]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
