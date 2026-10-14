#!/usr/bin/env python3
"""Read the printed contents (ସୂଚୀପତ୍ର) of the two Odia volumes into chapter titles, with the two-model agreement rule.

    py content/_localocr/read_contents.py --out content/_localocr/work/toc

Every contents page has two columns of entries "N. title ........ page". Each column is cut out, stock Tesseract only finds the
line boxes, and models A and B (crnn.pt, crnn_b.pt) read every line. Lines are joined into entries by the leading number.
A title is ACCEPTED only when A and B give the same text after punctuation and spacing are folded; the other entries are listed
for a person and are not used by the app. Output: toc.json  {"S.C": {"title": ..., "a": ..., "b": ..., "page": ...}}  and toc_report.md.
"""
import argparse
import io
import json
import os
import re
import sys
import unicodedata
from pathlib import Path

HERE = Path(__file__).resolve().parent
WORK = HERE / "work"
os.environ.setdefault("TESSDATA_PREFIX", str(WORK / "tessdata_best"))
sys.path.insert(0, str(HERE))
sys.stdout.reconfigure(encoding="utf-8")

import cv2  # noqa: E402
import fitz  # noqa: E402
import numpy as np  # noqa: E402
import pytesseract  # noqa: E402
import torch  # noqa: E402

pytesseract.pytesseract.tesseract_cmd = r"C:\Program Files\Tesseract-OCR\tesseract.exe"
import crnn  # noqa: E402

PDFS = {1: Path.home() / "Downloads" / "Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-1.pdf",
        2: Path.home() / "Downloads" / "Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-2.pdf"}
OD = "୦୧୨୩୪୫୬୭୮୯"
ENTRY = re.compile(r"^\s*([" + OD + r"]{1,3})\s*[.,।]\s*(.*)$")
HEADER = re.compile(r"ସ୍କନ୍ଧ|ମାହାତ୍ମ୍ୟ")
DOTS = re.compile(r"[.,;:'`ʼ·\-_~]{3,}.*$")


def odia_int(s):
    return int("".join(str(OD.index(c)) for c in s if c in OD)) if s and all(c in OD for c in s) else None


def load(name):
    ck = torch.load(crnn.MODEL / name, map_location=crnn.DEV)
    m = crnn.CRNN(len(ck["chars"]) + 1).to(crnn.DEV)
    m.load_state_dict(ck["state"])
    m.eval()
    return m, ck["chars"]


def page_image(vol, pno):
    d = fitz.open(str(PDFS[vol]))
    pix = d[pno - 1].get_pixmap(dpi=200)
    arr = np.frombuffer(pix.samples, dtype=np.uint8).reshape(pix.h, pix.w, pix.n)
    return cv2.cvtColor(arr, cv2.COLOR_RGB2GRAY if pix.n == 3 else cv2.COLOR_RGBA2GRAY)


def column_lines(g):
    """Line crops of one column, top to bottom."""
    d = pytesseract.image_to_data(g, lang="ori", config="--oem 1 --psm 6", output_type=pytesseract.Output.DICT)
    lines = {}
    for i, t in enumerate(d["text"]):
        if not t.strip():
            continue
        k = (d["block_num"][i], d["par_num"][i], d["line_num"][i])
        x0, y0, w, h = d["left"][i], d["top"][i], d["width"][i], d["height"][i]
        b = lines.setdefault(k, [x0, y0, x0 + w, y0 + h])
        b[0], b[1], b[2], b[3] = min(b[0], x0), min(b[1], y0), max(b[2], x0 + w), max(b[3], y0 + h)
    boxes = sorted(lines.values(), key=lambda b: b[1])
    pad = 5
    return [(g[max(0, y0 - pad):y1 + pad, max(0, x0 - pad):x1 + pad], x0) for x0, y0, x1, y1 in boxes if y1 - y0 >= 12 and x1 - x0 >= 40]


def fold(s):
    s = unicodedata.normalize("NFC", s)
    s = re.sub(r"[\s.,;:'\"‘’“”()\-–—_!?।॥\u200c\u200d]+", "", s)
    return s


LEADER = re.compile(r"[.,;:'`ʼ·\-_~]{3,}")
START = re.compile(r"^\s*[" + OD + r"\-_~·ଠ]{1,3}\s*[.,)।;:]*\s*")


def classify(lines_a, x0s):
    """From model A's text and the box positions: per line, 'head' (Skandha heading), 'skip' (column heading) or 'start' / 'cont'."""
    left = min(x0s) if x0s else 0
    kinds = []
    for t, x in zip(lines_a, x0s):
        if HEADER.search(t) and len(t) < 70 and not LEADER.search(t) and not START.match(t):
            kinds.append("head")
        elif re.search(r"ଅଧ୍ୟା|ବିଷ|ପୃଷ", t) and len(t) < 40 and not LEADER.search(t):
            kinds.append("skip")
        elif x <= left + 38 or re.match(r"^\s*[" + OD + r"]{1,3}\s*[.,)।;:]", t):
            kinds.append("start")
        else:
            kinds.append("cont")
    return kinds


def entries(lines, kinds):
    out, cur = [], []
    for ln, k in zip(lines, kinds):
        if k == "skip":
            continue
        if k == "head":
            if cur:
                out.append(" ".join(cur)); cur = []
            out.append("#HEADER " + ln)
            continue
        if k == "start" and cur:
            out.append(" ".join(cur)); cur = []
        cur.append(ln)
    if cur:
        out.append(" ".join(cur))
    return out


def vote(a, b, t):
    """Word by word majority of A, B and stock when the three have the same number of words; else exact agreement of two."""
    if fold(a) == fold(b):
        return a
    wa, wb, wt = a.split(), b.split(), t.split()
    if len(wa) == len(wb) == len(wt) and wa:
        out = []
        for x, y, z in zip(wa, wb, wt):
            if fold(x) == fold(y) or fold(x) == fold(z):
                out.append(x)
            elif fold(y) == fold(z):
                out.append(y)
            else:
                return None
        return " ".join(out)
    if t and fold(t) == fold(a):
        return a
    if t and fold(t) == fold(b):
        return b
    return None


def title_of(t):
    t = START.sub("", t, count=1)
    t = LEADER.split(t)[0]
    t = re.sub(r"\s*['’ʼ`]+\S{0,3}$", "", t)
    t = re.sub(r"\s+[" + OD + r"]{1,4}\s*$", "", t)
    return re.sub(r"\s+", " ", t).strip(" .,;:-)")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=str(WORK / "toc"))
    ap.add_argument("--pages", default="1-14")
    a = ap.parse_args()
    out = Path(a.out)
    out.mkdir(parents=True, exist_ok=True)
    A, B = load("crnn.pt"), load("crnn_b.pt")
    lo, hi = map(int, a.pages.split("-"))
    raw = {}
    for vol in (1, 2):
        ea, eb, et = [], [], []
        for pno in range(lo, hi + 1):
            g = page_image(vol, pno)
            h, w = g.shape
            page_cols = []
            cols = []
            for side in (g[:, : w // 2], g[:, w // 2:]):
                cx = column_lines(side)
                if not cx:
                    continue
                crops = [c for c, _ in cx]
                cols.append((crops, [x for _, x in cx], crnn.read_images(A[0], A[1], crops)))
            dots = sum(1 for _, _, ra in cols for l in ra if LEADER.search(l))
            if dots < 6:
                continue
            for crops, xs, ra in cols:
                kinds = classify(ra, xs)
                rb = crnn.read_images(B[0], B[1], crops)
                rt = [pytesseract.image_to_string(c, lang="ori", config="--oem 1 --psm 7").strip() for c in crops]
                ea.append(entries(ra, kinds)); eb.append(entries(rb, kinds)); et.append(entries(rt, kinds))
            print(f"vol {vol} page {pno}: contents page, {dots} leader lines", flush=True)
        raw[vol] = (ea, eb, et)
    per_vol = {1: [("0", 6)] + [(str(k), n) for k, n in zip(range(1, 9), (19, 10, 33, 31, 26, 19, 15, 24))],
               2: [(str(k), n) for k, n in zip(range(9, 13), (24, 90, 31, 13))]}
    def real(e):
        t = title_of(e)
        return len([c for c in t if "଀" <= c <= "୿"]) >= 10
    def seqs(vol):
        """Entries of A, B and stock, kept in step: an entry is dropped from all three when A's reading is not a title."""
        la = [e for col in raw[vol][0] for e in col if not e.startswith("#HEADER")]
        lb = [e for col in raw[vol][1] for e in col if not e.startswith("#HEADER")]
        lt = [e for col in raw[vol][2] for e in col if not e.startswith("#HEADER")]
        keep = [real(e) for e in la]
        pick = lambda L: [e for e, k in zip(L, keep) if k] if len(L) == len(la) else []
        return pick(la), pick(lb), pick(lt)
    FIRST_DIGITS = re.compile(r"^\s*([" + OD + r"ଠ]{1,4})")
    result, rep, acc = {}, ["# Odia contents read by two models + stock", ""], 0
    order = {1: ["0", "1", "2", "3", "4", "5", "6", "7", "8"], 2: ["9", "10", "11", "12"]}
    sizes = {"0": 6, "1": 19, "2": 10, "3": 33, "4": 31, "5": 26, "6": 19, "7": 15, "8": 24, "9": 24, "10": 90, "11": 31, "12": 13}
    for vol in (1, 2):
        sa, sb, st = seqs(vol)
        n = len(sa)
        print(f"vol {vol}: entries A {len(sa)} B {len(sb)} stock {len(st)}")
        if not (len(sa) == len(sb) and (len(st) == 0 or len(st) == n)):
            rep.append(f"vol {vol}: counts differ between models, skipped")
            continue
        sk_i, last = -1, 0
        for k in range(n):
            m = FIRST_DIGITS.match(sa[k])
            digs = m.group(1).replace("ଠ", "୦") if m else ""
            full = odia_int(digs) if digs and all(c in OD for c in digs) else None
            strip = odia_int(digs[:-1]) if len(digs) > 1 and digs[-1] in "୯୦" and all(c in OD for c in digs[:-1]) else None
            vals = [v for v in (full, strip) if v is not None]
            if sk_i == -1:
                if full == 1 or strip == 1:
                    sk_i, last = 0, 1
                else:
                    continue            # a heading line before the first entry
            elif last + 1 in vals:
                last += 1
            elif full == 1 and last >= 2:
                sk_i, last = sk_i + 1, 1
            elif strip == 1 and last >= 5 and sizes[order[vol][sk_i]] - last <= 3:
                sk_i, last = sk_i + 1, 1
            elif vals and last < max(vals) <= last + 3:
                last = max(vals)       # a skipped entry (its line was not found): follow the printed number
            else:
                last += 1
            if sk_i >= len(order[vol]):
                break
            sk = order[vol][sk_i]
            if last > sizes[sk]:
                continue
            ta, tb = title_of(sa[k]), title_of(sb[k])
            tt = title_of(st[k]) if st else ""
            fa, fb, ft = fold(ta), fold(tb), fold(tt)
            pick = vote(ta, tb, tt)
            key = f"{sk}.{last}"
            rep.append(f"{key}	{'OK ' if pick else '?? '}	{ta}	|	{tb}")
            if pick and key not in result:
                result[key] = pick
                acc += 1
    total = sum(sizes.values())
    json.dump(result, open(out / "toc.json", "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    (out / "toc_report.md").write_text(chr(10).join(rep), encoding="utf-8")
    print("accepted", acc, "of", total)


if __name__ == "__main__":
    main()
