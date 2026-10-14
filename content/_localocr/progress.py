#!/usr/bin/env python3
"""Progress of the local Odia pipeline per skandha: py content/_localocr/progress.py"""
import collections
import json
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
W = HERE / "work"
ODIA = HERE.parent / "_odia"
sp = json.loads((ODIA / "data" / "skandha_pages.json").read_text(encoding="utf-8"))
chap = json.loads((ODIA / "data" / "expected_verses.json").read_text(encoding="utf-8"))["verses"]
cps = collections.Counter(int(k.split(".")[0]) for k in chap)
final = {int(p.parent.name.split("-")[1]): 0 for p in (W / "final" / "out").glob("skandha-*/chapter-*.md")} if (W / "final" / "out").exists() else {}
fin = collections.Counter(int(p.parent.name.split("-")[1]) for p in (W / "final" / "out").glob("skandha-*/chapter-*.md")) if (W / "final" / "out").exists() else {}

print(time.strftime("%Y-%m-%d %H:%M:%S"))
print(f"{'skandha':>7} {'pages':>6} {'read A':>7} {'read B':>7} {'chapters':>9} {'chapter files':>14}  state")
tn = ta = tb = tc = tf = 0
for k in range(6, 13):
    pages = [(v, p) for v, a, b in sp[str(k)] for p in range(a, b + 1)]
    a = sum((W / "pages_a" / "vision" / f"v{v}_p{p:04d}.json").exists() for v, p in pages)
    b = sum((W / "pages_b" / "vision" / f"v{v}_p{p:04d}.json").exists() for v, p in pages)
    state = "done" if fin.get(k) else ("both read" if b == len(pages) else "A read" if a == len(pages) else "reading" if a else "waiting")
    print(f"{k:>7} {len(pages):>6} {a:>7} {b:>7} {cps[k]:>9} {fin.get(k, 0):>14}  {state}")
    tn += len(pages); ta += a; tb += b; tc += cps[k]; tf += fin.get(k, 0)
print(f"{'total':>7} {tn:>6} {ta:>7} {tb:>7} {tc:>9} {tf:>14}")
steps = [("model A reading", ta / tn), ("model B reading", tb / tn)]
for name, f in steps:
    print(f"  {name}: {100 * f:.0f}%")
st = W / "pipeline_status.json"
if st.exists():
    print("driver status file:", json.loads(st.read_text(encoding="utf-8")).get("step"))
