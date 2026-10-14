#!/usr/bin/env python3
"""prep: render reading pieces for pages.   merge: apply my transcription file to page JSON."""
import sys, json, re, os
from pathlib import Path
import numpy as np, cv2
sys.path.insert(0, str(Path(__file__).parent))
import odia_ocr as o
H = Path(__file__).parent
OUT = H / "vt"; VIS = H / "work_v" / "vision"; SRC = H / "work" / "fixed"

def cut_points(prof, total, maxh):
    n = max(1, int(np.ceil(total / maxh))); cuts = [0]
    for i in range(1, n):
        t = int(total * i / n); w = 160
        lo, hi = max(cuts[-1] + 200, t - w), min(total - 100, t + w)
        seg = prof[lo:hi]
        c = lo + int(np.argmin(seg)) if len(seg) else t
        cuts.append(c)
    cuts.append(total); return cuts

FORCE_FULL=False
def prep(vol, page):
    g = o.render(vol, page); lay = o.analyse(g)
    d = json.loads((SRC / f"v{vol}_p{page:04d}.json").read_text(encoding="utf-8"))
    hint = d.get("sanskrit_verse_numbers", [])
    if lay is None:
        print(f"v{vol} p{page}: blank"); return []
    gc = lay["gclean"]; z = lay["zones"]; gx = lay["split"]
    top, bot = z[0][0] - 8, z[-1][1] + 8
    _c0 = (gx + 6) if gx is not None else lay['x0']
    _r = np.where((gc[:, _c0:lay['x1']] < 128).sum(1) > 3)[0]
    if len(_r) and _r.max() + 8 > bot: bot = int(_r.max()) + 8
    x0, x1 = max(0, lay["x0"] - 8), min(gc.shape[1], lay["x1"] + 8)
    cols = [i for i, q in enumerate(z) if q[2] == "col"]
    fulls = [q for q in z if q[2] == "full"]
    pieces = []
    simple = gx is not None and bool(cols) and not d.get('chapter_title') and not d.get('colophon') and not FORCE_FULL
    imgs = []
    if simple:
        yend = z[cols[-1]][1] + 8
        if z[-1][2] == 'col': yend = bot
        _rules = [q for q in z if q[2] == 'full' and q[1]-q[0] <= 30 and q[0] > 0.5*gc.shape[0] and q[0] > z[cols[0]][0]]
        if _rules: yend = min(yend, _rules[0][0] - 4)
        sub = gc[top:yend, gx + 6:x1]
        cuts = cut_points((sub < 128).sum(1), sub.shape[0], 1950)
        for a, b in zip(cuts[:-1], cuts[1:]):
            imgs.append(cv2.resize(sub[a:b], None, fx=0.72, fy=0.72, interpolation=cv2.INTER_AREA))
        if yend < bot:
            fn = gc[yend:bot, x0:x1]
            imgs.append(cv2.resize(fn, None, fx=0.75, fy=0.75, interpolation=cv2.INTER_AREA))
    else:
        sub = gc[top:bot, x0:x1]
        cuts = cut_points((sub < 128).sum(1), sub.shape[0], 1250)
        for a, b in zip(cuts[:-1], cuts[1:]):
            imgs.append(cv2.resize(sub[a:b], None, fx=0.74, fy=0.74, interpolation=cv2.INTER_AREA))
    OUT.mkdir(exist_ok=True)
    for k, im in enumerate(imgs):
        p = OUT / f"v{vol}_p{page:04d}_{k}.png"; cv2.imwrite(str(p), im); pieces.append(str(p))
    print(f"v{vol} p{page} {'RIGHT-COLUMN' if simple else 'FULL-WIDTH'} hint={hint} head={d.get('running_head_chapter')} :: " + " ".join(pieces))
    return pieces

def merge(txt):
    VIS.mkdir(parents=True, exist_ok=True)
    cur = None; recs = {}
    for line in txt.splitlines():
        if line.startswith("@@"):
            m = re.match(r"@@\s*v(\d+)\s*p(\d+)", line); cur = (int(m[1]), int(m[2])); recs[cur] = {"P": [], "F": []}; continue
        if cur is None or not line.strip(): continue
        k, _, v = line.partition(":"); v = v.strip(); k = k.strip()
        if k == "P": recs[cur]["P"].append(v)
        elif k == "F": recs[cur]["F"].append(v)
        else: recs[cur][k] = v
    for (vol, page), r in recs.items():
        f = VIS / f"v{vol}_p{page:04d}.json"
        d = json.loads((f if f.exists() else SRC / f"v{vol}_p{page:04d}.json").read_text(encoding="utf-8"))
        d["paragraphs"] = r["P"]; d["footnotes"] = r["F"]
        d["chapter_title"] = r.get("T", "")
        d["colophon"] = r.get("C") or None
        b = int(r.get("B", 0) or 0)
        if d["chapter_title"] or r.get("H"):
            d["chapter_marker_text"] = "boxed heading"; d["paragraphs_before_chapter_heading"] = b
        else:
            d["chapter_marker_text"] = None; d["paragraphs_before_chapter_heading"] = 0
        if "K" in r: d["first_paragraph_continues_previous_page"] = r["K"] == "1"
        if "RH" in r and r["RH"].isdigit(): d["running_head_chapter"] = int(r["RH"])
        d["_page"]["model"] = "claude-vision"; d["verified_vision"] = True
        f.write_text(json.dumps(d, ensure_ascii=False, indent=1), encoding="utf-8")
    print("merged", len(recs), "pages")

if __name__ == "__main__":
    if sys.argv[1] == "prep":
        vol = int(sys.argv[2]); a, _, b = sys.argv[3].partition("-")
        FORCE_FULL = len(sys.argv) > 4 and sys.argv[4] == 'full'
        for p in range(int(a), int(b or a) + 1): prep(vol, p)
    elif sys.argv[1] == "merge":
        merge(open(sys.argv[2], encoding="utf-8").read())
