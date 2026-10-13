#!/usr/bin/env python3
"""P1: cut the translation column of trusted pages into line images and pair each with its text.

    py content/_localocr/make_lines.py --pages 88-120            a small try-out
    py content/_localocr/make_lines.py --all --workers 5         all trusted pages (v1:88-578)

For every page: layout from odia_ocr.analyse (column rule, zones); each 'col' zone right of the rule is read once by stock
Tesseract (ori, tessdata_best) only to get line boxes and a rough hypothesis; the line crops are paired with the trusted
transcript by monotone fuzzy alignment of the hypothesis against the transcript words. Output (ignored by git):
    work/lines/v1_p0088_03.png     the line image (grey)
    work/lines/v1_p0088_03.gt.txt  its text from the transcript (single line)
    work/lines/manifest.jsonl      {id, vol, page, sim, hyp, gt, w, h}
`sim` is the character similarity between the stock reading and the transcript: the clean set (sim >= --min-sim) is what is
trained on; the rest is kept in the manifest for the bootstrap rounds.
"""
import argparse
import difflib
import json
import os
import re
import sys
from concurrent.futures import ProcessPoolExecutor
from pathlib import Path

HERE = Path(__file__).resolve().parent
ODIA = HERE.parent / "_odia"
WORK = HERE / "work"
LINES = WORK / "lines"
os.environ.setdefault("TESSDATA_PREFIX", str(WORK / "tessdata_best"))
os.environ.setdefault("ODIA_PDF_1", str(Path.home() / "Downloads" / "Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-1.pdf"))
os.environ.setdefault("ODIA_PDF_2", str(Path.home() / "Downloads" / "Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-2.pdf"))
sys.path.insert(0, str(ODIA))
sys.stdout.reconfigure(encoding="utf-8")

TESS_EXE = r"C:\Program Files\Tesseract-OCR\tesseract.exe"
PAD = 5


def _imports():
    import cv2  # noqa
    import numpy as np  # noqa
    import pytesseract
    pytesseract.pytesseract.tesseract_cmd = TESS_EXE
    import odia_ocr as o
    import odia_text as T
    return cv2, np, pytesseract, o, T


def zone_lines(pytesseract, img):
    """Stock Tesseract on a zone image -> [{box:(x0,y0,x1,y1), text}] in reading order."""
    d = pytesseract.image_to_data(img, lang="ori", config="--oem 1 --psm 6", output_type=pytesseract.Output.DICT)
    lines = {}
    for i, t in enumerate(d["text"]):
        if not t.strip():
            continue
        key = (d["block_num"][i], d["par_num"][i], d["line_num"][i])
        x0, y0 = d["left"][i], d["top"][i]
        x1, y1 = x0 + d["width"][i], y0 + d["height"][i]
        ln = lines.setdefault(key, {"box": [x0, y0, x1, y1], "words": []})
        b = ln["box"]
        ln["box"] = [min(b[0], x0), min(b[1], y0), max(b[2], x1), max(b[3], y1)]
        ln["words"].append(t)
    out = [{"box": l["box"], "text": " ".join(l["words"])} for l in lines.values()]
    out.sort(key=lambda l: (l["box"][1] // 12, l["box"][0]))
    return out


def sim(a, b):
    return difflib.SequenceMatcher(a=a, b=b, autojunk=False).ratio() if a or b else 1.0


def align_lines(hyps, key_words, T, min_cover=0.5):
    """Global alignment: the whole page reading against the whole page transcript (character blocks), then each line takes the
    transcript words its characters fall on. A bad line cannot derail the lines after it. Returns ([(gt or None, sim)], 0)."""
    kc = [T.core(w) for w in key_words]
    kstr, kpos = "", []                       # kpos[w] = (start, end) of word w in kstr
    for c in kc:
        kpos.append((len(kstr), len(kstr) + len(c)))
        kstr += c + " "
    hl = [" ".join(T.core(w) for w in h.split()) for h in hyps]
    hstr, hr = "", []
    for t in hl:
        hr.append((len(hstr), len(hstr) + len(t)))
        hstr += t + " "
    sm = difflib.SequenceMatcher(a=hstr, b=kstr, autojunk=False)
    blocks = [bl for bl in sm.get_matching_blocks() if bl.size >= 2]
    import bisect
    starts = [pos[0] for pos in kpos]
    out = []
    for (la, lb), text in zip(hr, hl):
        covered, kmin, kmax = 0, None, None
        for bl in blocks:
            lo, hi = max(la, bl.a), min(lb, bl.a + bl.size)
            if lo < hi:
                covered += hi - lo
                k0, k1 = bl.b + (lo - bl.a), bl.b + (hi - bl.a)
                kmin = k0 if kmin is None else min(kmin, k0)
                kmax = k1 if kmax is None else max(kmax, k1)
        if not text or covered < min_cover * max(1, len(text)) or kmin is None:
            out.append((None, covered / max(1, len(text))))
            continue
        w0 = max(0, bisect.bisect_right(starts, kmin) - 1)
        w1 = max(0, bisect.bisect_left(starts, kmax) - 1)
        w1 = max(w1, w0)
        # punctuation-only words ("॥", "!") have no core, so the character match never reaches them: take the ones the stock reading
        # shows at the line's edges (the 50-line hand check found "॥ ୪୭" for "॥ ୪୭ ॥" and "ମହାବାହୁ" for "ମହାବାହୁ !")
        raw = hyps[len(out)].strip()
        while w1 + 1 < len(key_words) and kc[w1 + 1] == "" and key_words[w1 + 1] and any(ch in raw[-6:] for ch in key_words[w1 + 1]):
            w1 += 1
        while w0 > 0 and kc[w0 - 1] == "" and key_words[w0 - 1] and any(ch in raw[:6] for ch in key_words[w0 - 1]):
            w0 -= 1
        gt = " ".join(key_words[w0:w1 + 1])
        out.append((gt, sim(text, " ".join(kc[w0:w1 + 1]))))
    return out, 0


def page_lines(vol, page):
    cv2, np, pytesseract, o, T = _imports()
    f = ODIA / "vision_transcripts" / f"v{vol}_p{page:04d}.json"
    if not f.exists():
        return []
    key = json.loads(f.read_text(encoding="utf-8"))
    import unicodedata
    # raw words, exactly as the transcript has them (quotes, dashes, spaced verse markers): the line text must be what is printed
    key_words = [w for para in key.get("paragraphs", []) for w in unicodedata.normalize("NFC", para).split()]
    g = o.render(vol, page)
    lay = o.analyse(g)
    if lay is None or lay.get("split") is None:
        return []
    gc, x1, split = lay["gclean"], lay["x1"], lay["split"]
    recs = []
    for zi, (y0, y1, kind) in enumerate(lay["zones"]):
        if kind != "col":
            continue
        ya, yb = max(0, y0 - 6), min(gc.shape[0], y1 + 6)
        xa, xb = split + 6, min(gc.shape[1], x1 + 8)
        zone = gc[ya:yb, xa:xb]
        for ln in zone_lines(pytesseract, zone):
            bx = ln["box"]
            crop = zone[max(0, bx[1] - PAD):bx[3] + PAD, max(0, bx[0] - PAD):bx[2] + PAD]
            if crop.shape[0] < 12 or crop.shape[1] < 20:
                continue
            recs.append({"img": crop, "hyp": ln["text"], "y": ya + bx[1]})
    recs.sort(key=lambda r: r["y"])
    LINES.mkdir(parents=True, exist_ok=True)
    gts, _ = align_lines([r["hyp"] for r in recs], key_words, T)
    out = []
    for k, (r, (gt, s)) in enumerate(zip(recs, gts)):
        lid = f"v{vol}_p{page:04d}_{k:02d}"
        cv2.imwrite(str(LINES / f"{lid}.png"), r["img"])
        if gt:
            (LINES / f"{lid}.gt.txt").write_text(gt + "\n", encoding="utf-8")
        out.append({"id": lid, "vol": vol, "page": page, "sim": round(s, 3), "hyp": r["hyp"], "gt": gt,
                    "h": int(r["img"].shape[0]), "w": int(r["img"].shape[1])})
    return out


def _job(a):
    try:
        return page_lines(*a)
    except Exception as e:  # noqa
        return [{"error": f"v{a[0]} p{a[1]}: {e}"}]


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--pages", help="e.g. 88-120 (volume 1)")
    ap.add_argument("--all", action="store_true")
    ap.add_argument("--workers", type=int, default=4)
    a = ap.parse_args()
    if a.all:
        lo, hi = 88, 578
    elif a.pages:
        lo, _, hi = a.pages.partition("-")
        lo, hi = int(lo), int(hi or lo)
    else:
        ap.error("--pages or --all")
    jobs = [(1, p) for p in range(lo, hi + 1)]
    man = LINES / "manifest.jsonl"
    LINES.mkdir(parents=True, exist_ok=True)
    n = bad = 0
    with ProcessPoolExecutor(a.workers) as ex, open(man, "a", encoding="utf-8") as fh:
        for res in ex.map(_job, jobs):
            for r in res:
                if "error" in r:
                    print("ERROR", r["error"], flush=True)
                    continue
                fh.write(json.dumps(r, ensure_ascii=False) + "\n")
                n += 1
                bad += r["gt"] is None
    print(f"{n} lines written, {bad} without a matching transcript text -> {man}")


if __name__ == "__main__":
    main()
