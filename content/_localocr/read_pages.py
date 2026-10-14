#!/usr/bin/env python3
"""P3: read pages locally with the trained line recogniser, writing the same page JSON as odia_vision.py.

    py content/_localocr/read_pages.py --skandha 6                      pages of Skandha 6 -> work/pages_a/vision
    py content/_localocr/read_pages.py --pages 696-700 --model crnn_b.pt --out work/pages_b
Layout, headings, colophon, footnotes and verse numbers come from the existing odia_ocr.extract_page; only the text of every
line is read by the recogniser instead of Tesseract (stock Tesseract is still used to find the line boxes).
Output feeds odia_verify.py compare (A=this, B=a second model, T=stock Tesseract) and odia_assemble.py unchanged.
"""
import argparse
import json
import os
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
ODIA = HERE.parent / "_odia"
WORK = HERE / "work"
os.environ.setdefault("TESSDATA_PREFIX", str(WORK / "tessdata_best"))
os.environ.setdefault("ODIA_PDF_1", str(Path.home() / "Downloads" / "Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-1.pdf"))
os.environ.setdefault("ODIA_PDF_2", str(Path.home() / "Downloads" / "Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-2.pdf"))
sys.path.insert(0, str(ODIA))
sys.path.insert(0, str(HERE))
sys.stdout.reconfigure(encoding="utf-8")

TESS_EXE = r"C:\Program Files\Tesseract-OCR\tesseract.exe"


def trusted_lexicon():
    """Word counts of the transcribed pages that are already trusted (Skandha 1 to 4), for the likelihood rescoring."""
    import collections
    import odia_text as T
    from ensemble import Lexicon
    cnt = collections.Counter()
    for f in (ODIA / "vision_transcripts").glob("v*_p*.json"):
        for para in json.loads(f.read_text(encoding="utf-8")).get("paragraphs", []):
            for w in T.tokens(para):
                c = T.core(w)
                if c and not T.is_marker(w):
                    cnt[c] += 1
    return Lexicon(cnt)


def install_recogniser(model_files, penalty=0.0):
    import pytesseract
    pytesseract.pytesseract.tesseract_cmd = TESS_EXE
    import odia_ocr as o
    from ensemble import Reader
    reader = Reader(model_files, trusted_lexicon() if penalty > 0 else None, penalty=penalty)
    pad = 5

    def ocr_lines(img, psm=6, conf_cut=30):
        d = pytesseract.image_to_data(img, lang="ori", config=f"--oem 1 --psm {psm}", output_type=pytesseract.Output.DICT)
        boxes = {}
        for i, t in enumerate(d["text"]):
            if not t.strip():
                continue
            key = (d["block_num"][i], d["par_num"][i], d["line_num"][i])
            x0, y0 = d["left"][i], d["top"][i]
            b = boxes.setdefault(key, [x0, y0, x0 + d["width"][i], y0 + d["height"][i]])
            b[0], b[1] = min(b[0], x0), min(b[1], y0)
            b[2], b[3] = max(b[2], x0 + d["width"][i]), max(b[3], y0 + d["height"][i])
        order = sorted(boxes.values(), key=lambda b: (b[1], b[0]))
        crops = []
        for b in order:
            c = img[max(0, b[1] - pad):b[3] + pad, max(0, b[0] - pad):b[2] + pad]
            if c.shape[0] < 8 or c.shape[1] < 12:
                c = img[:1, :1]
            crops.append(c)
        texts = reader.read_lines(crops) if crops else []
        out = []
        for b, t in zip(order, texts):
            words = [(w, 95.0) for w in t.split()]
            if words:
                out.append({"left": b[0], "top": b[1], "words": words})
        return out

    o.ocr_lines = ocr_lines
    return o


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--skandha")
    ap.add_argument("--pages", help="e.g. 696-700 (needs --vol if not skandha)")
    ap.add_argument("--vol", type=int, default=1)
    ap.add_argument("--model", default="crnn.pt", help="one model file, or two separated by a comma (their likelihoods are added)")
    ap.add_argument("--prior", type=float, default=0.0, help="penalty per word not in the trusted word list (0 = no word list)")
    ap.add_argument("--out", default=str(WORK / "pages_a"))
    ap.add_argument("--force", action="store_true")
    ap.add_argument("--list", help="text file with one page per line, like v1_p0696")
    a = ap.parse_args()
    jobs = []
    if a.skandha:
        import odia_vision as ov
        spans = ov.skandha_spans(a.skandha)
        for v, x, y in spans:
            lo, hi = x, y
            if a.pages:
                plo, _, phi = a.pages.partition("-")
                lo, hi = max(x, int(plo)), min(y, int(phi or plo))
            jobs += [(v, p) for p in range(lo, hi + 1)]
    elif a.list:
        jobs = [(int(x[1]), int(x[4:8])) for x in Path(a.list).read_text().split() if x.startswith("v")]
    elif a.pages:
        plo, _, phi = a.pages.partition("-")
        jobs = [(a.vol, p) for p in range(int(plo), int(phi or plo) + 1)]
    else:
        ap.error("--skandha or --pages")
    jobs = list(dict.fromkeys(jobs))
    out = Path(a.out) / "vision"
    out.mkdir(parents=True, exist_ok=True)
    o = install_recogniser(a.model.split(","), a.prior)
    t0 = time.time()
    done = 0
    for v, p in jobs:
        f = out / f"v{v}_p{p:04d}.json"
        if f.exists() and not a.force:
            continue
        try:
            rec, _raw = o.extract_page(v, p)
        except Exception as e:  # noqa
            print(f"v{v} p{p}: FAILED {e}", flush=True)
            continue
        rec["_page"]["model"] = f"crnn:{a.model}" + (f"+prior{a.prior:g}" if a.prior else "")
        f.write_text(json.dumps(rec, ensure_ascii=False, indent=1), encoding="utf-8")
        done += 1
        if done % 10 == 0:
            print(f"{done}/{len(jobs)} pages, {time.time() - t0:.0f}s", flush=True)
    print(f"{done} page(s) -> {out} in {time.time() - t0:.0f}s")


if __name__ == "__main__":
    main()
