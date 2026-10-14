#!/usr/bin/env python3
"""Random-word audit of recogniser output against the page image (the measurement that does not depend on any transcript).
    py content/_localocr/audit_pages.py --dir work/pages_a/vision --out work/audit_s6 --n 50 --seed 4
Writes items.json and renders the page images; batch listing: py batch_list.py (set W) or read items.json."""
import argparse, json, os, random, sys
from pathlib import Path
HERE = Path(__file__).resolve().parent
ap = argparse.ArgumentParser(); ap.add_argument("--dir", required=True); ap.add_argument("--out", required=True)
ap.add_argument("--n", type=int, default=50); ap.add_argument("--seed", type=int, default=4); ap.add_argument("--vol", type=int, default=1)
a = ap.parse_args()
out = Path(a.out).resolve(); out.mkdir(parents=True, exist_ok=True)
os.environ["ODIA_VWORK"] = str(out)
os.environ.setdefault("ODIA_PDF_1", str(Path.home() / "Downloads" / "Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-1.pdf"))
os.environ.setdefault("ODIA_PDF_2", str(Path.home() / "Downloads" / "Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-2.pdf"))
sys.path.insert(0, str(HERE.parent / "_odia")); sys.stdout.reconfigure(encoding="utf-8")
import odia_text as T, odia_vision as ov
pool = []
for f in sorted(Path(a.dir).glob("v*_p*.json")):
    j = json.loads(f.read_text(encoding="utf-8")); v, p = int(f.name[1]), int(f.name[4:8])
    for pi, para in enumerate(j.get("paragraphs", [])):
        for ti, tok in enumerate(para.split(" ")):
            c = T.core(tok)
            if len(c) >= 3 and not T.is_marker(tok) and not tok.startswith("[") :
                pool.append((v, p, pi, ti, tok, para))
random.seed(a.seed); pick = sorted(random.sample(pool, min(a.n, len(pool))))
items = [{"vol": v, "page": p, "para": pi, "idx": ti, "word": tok, "ctx": " ".join(para.split(" ")[max(0, ti-5):ti] + [">>" + tok + "<<"] + para.split(" ")[ti+1:ti+4])} for v, p, pi, ti, tok, para in pick]
(out / "items.json").write_text(json.dumps(items, ensure_ascii=False, indent=1), encoding="utf-8")
for v, p in sorted({(i["vol"], i["page"]) for i in items}):
    ov.page_image(v, p)
print(len(items), "words on", len({(i["vol"], i["page"]) for i in items}), "pages ->", out)
for n, i in enumerate(items): print(f"#{n} v{i['vol']}_p{i['page']:04d}: {i['ctx']}")
