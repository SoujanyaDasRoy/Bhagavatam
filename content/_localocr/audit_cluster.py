#!/usr/bin/env python3
"""Page-clustered random-word audit: N random pages, K random words on each, so one page image answers K checks.
    py content/_localocr/audit_cluster.py --dir work/final/vision --out work/audit_a --pages 60 --words 5 --seed 11
Words with [?], markers and words shorter than 3 letters are not sampled. Writes items.json (with context) and the page images.
Judgements are written to results.json as [{vol, page, word, ok}] and scored with Wilson 95% intervals (audit_score.py)."""
import argparse
import json
import os
import random
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ap = argparse.ArgumentParser()
ap.add_argument("--dir", required=True)
ap.add_argument("--out", required=True)
ap.add_argument("--pages", type=int, default=60)
ap.add_argument("--words", type=int, default=5)
ap.add_argument("--seed", type=int, default=11)
a = ap.parse_args()
out = Path(a.out).resolve()
out.mkdir(parents=True, exist_ok=True)
os.environ["ODIA_VWORK"] = str(out)
os.environ.setdefault("ODIA_PDF_1", str(Path.home() / "Downloads" / "Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-1.pdf"))
os.environ.setdefault("ODIA_PDF_2", str(Path.home() / "Downloads" / "Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-2.pdf"))
sys.path.insert(0, str(HERE.parent / "_odia"))
sys.stdout.reconfigure(encoding="utf-8")
import odia_text as T  # noqa: E402
import odia_vision as ov  # noqa: E402

rng = random.Random(a.seed)
by_page = {}
for f in sorted(Path(a.dir).glob("v*_p*.json")):
    j = json.loads(f.read_text(encoding="utf-8"))
    v, p = int(f.name[1]), int(f.name[4:8])
    pool = []
    for pi, para in enumerate(j.get("paragraphs", [])):
        toks = para.split(" ")
        for ti, tok in enumerate(toks):
            if len(T.core(tok)) >= 3 and not T.is_marker(tok) and "[?]" not in tok:
                pool.append((pi, ti, tok, " ".join(toks[max(0, ti - 4):ti] + [">>" + tok + "<<"] + toks[ti + 1:ti + 4])))
    if len(pool) >= a.words:
        by_page[(v, p)] = pool
pages = sorted(rng.sample(sorted(by_page), min(a.pages, len(by_page))))
items = []
for v, p in pages:
    for pi, ti, tok, ctx in sorted(rng.sample(by_page[(v, p)], a.words)):
        items.append({"vol": v, "page": p, "para": pi, "idx": ti, "word": tok, "ctx": ctx})
(out / "items.json").write_text(json.dumps(items, ensure_ascii=False, indent=1), encoding="utf-8")
for v, p in pages:
    ov.page_image(v, p)
print(len(items), "words on", len(pages), "pages ->", out)
