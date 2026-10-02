"""
Local OCR (Tesseract, no API key) -> work/vision/vN_pNNNN.json, the same record the rest of the tool reads.

  python ocr_local.py check
  python ocr_local.py layout  --vol 1 --pages 356-361        # no OCR: shows what the layout step finds on each page (columns, headings, footnotes)
  python ocr_local.py run     --vol 1 --pages 362-402 --after-chapter 25
  python bn_extract.py assemble

What it does per page: render the page image (same renderer as bn_extract.py) -> find the footnote rule and the gap between the two
columns -> full-width bands (chapter heading, closing line) are read at full width, the right column (the Bengali translation) is read
as paragraphs, the left column is only used to pick up the Sanskrit verse numbers, the strip under the footnote rule is the footnotes.
Pages must be run in order and without gaps: chapter numbers are counted up from --after-chapter (kept in work/local_state.json).

Honest limits: Tesseract's Bengali reading is much worse than a person's or a vision model's. Words it is unsure of get " [?]".
The cross-check in bn_extract.py compares against bundled OCR from the same kind of engine, so its agreement scores will look better than
the text deserves. Treat every chapter as unreviewed until you have looked at review\\index.html next to the page images.
"""
import argparse
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path

import numpy as np
from PIL import Image

import bn_extract as b

EXE_FALLBACKS = [r"C:\Program Files\Tesseract-OCR\tesseract.exe", r"C:\Program Files (x86)\Tesseract-OCR\tesseract.exe"]
BN = str.maketrans("০১২৩৪৫৬৭৮৯०१२३४५६७८९", "01234567890123456789")
UP = 2            # upscale factor before OCR
MARK_BELOW = 55   # word confidence under this gets [?]
STATE = b.WORK / "local_state.json"


def tesseract() -> str:
    exe = shutil.which("tesseract") or next((p for p in EXE_FALLBACKS if Path(p).exists()), None)
    if not exe:
        sys.exit("Tesseract is not installed. See LOCAL_OCR.md step 1.")
    return exe


def has_lang(exe: str, lang: str) -> bool:
    out = subprocess.run([exe, "--list-langs"], capture_output=True, text=True, encoding="utf-8").stdout
    return lang in out.split()


# ---------- layout (pure image analysis, no OCR) ----------
def runs(mask: np.ndarray, min_len: int, join_gap: int):
    """True-runs of a 1-D mask, merging runs closer than join_gap, dropping those shorter than min_len."""
    idx = np.flatnonzero(mask)
    if idx.size == 0:
        return []
    out, start, prev = [], idx[0], idx[0]
    for i in idx[1:]:
        if i - prev > join_gap:
            out.append((start, prev))
            start = i
        prev = i
    out.append((start, prev))
    return [(a, z) for a, z in out if z - a + 1 >= min_len]


def longest_run(row: np.ndarray) -> int:
    best = cur = 0
    for v in row:
        cur = cur + 1 if v else 0
        best = max(best, cur)
    return best


def analyse(img: Image.Image) -> dict:
    g = np.array(img.convert("L"))
    h, w = g.shape
    dark = g < 170
    # footnote rule: a row with one long unbroken dark run (text rows never have one) in the lower part of the page
    foot = h
    for y in range(int(h * 0.62), int(h * 0.985)):
        if dark[y].sum() > w * 0.45 and longest_run(dark[y]) > w * 0.45:
            foot = y
            break
    top = int(h * 0.075)                       # below the running header
    body = dark[top:foot]
    # gap between the columns: the widest ink-free stretch of columns in the middle fifth of the page
    cols = body.sum(axis=0)
    lo, hi = int(w * 0.40), int(w * 0.60)
    free = cols[lo:hi] <= max(2, 0.004 * body.shape[0])
    best, cur, start, bs, be = 0, 0, 0, lo, lo
    for i, f in enumerate(free):
        if f:
            if cur == 0:
                start = i
            cur += 1
            if cur > best:
                best, bs, be = cur, start, i
        else:
            cur = 0
    gap = lo + (bs + be) // 2
    if best < 4:                               # no clean gap: a page that is mostly a full-width block
        gap = w // 2
    # full-width bands: rows with ink inside the narrow gap strip
    strip = body[:, max(0, gap - 3): gap + 4].any(axis=1)
    bands = [(int(a) + top, int(z) + top) for a, z in runs(strip, min_len=int(h * 0.010), join_gap=int(h * 0.008))]
    return dict(w=w, h=h, top=top, foot=foot, gap=gap, bands=bands, gapwidth=best)


def segments(lay: dict):
    """Alternating column blocks and full-width bands, top to bottom: ('col', y0, y1) / ('full', y0, y1)."""
    segs, y = [], lay["top"]
    for a, z in lay["bands"]:
        if a - y > lay["h"] * 0.012:
            segs.append(("col", y, a - 1))
        segs.append(("full", a, z))
        y = z + 1
    if lay["foot"] - y > lay["h"] * 0.012:
        segs.append(("col", y, lay["foot"] - 1))
    return segs


# ---------- OCR ----------
def ocr_tsv(exe, im: Image.Image, lang: str, psm: int):
    """Lines of an image: dicts with text, left, top, right, bottom, low (words under MARK_BELOW)."""
    tmp = b.WORK / "_ocr_tmp.png"
    im.save(tmp)
    r = subprocess.run([exe, str(tmp), "stdout", "-l", lang, "--psm", str(psm), "tsv"], capture_output=True, text=True, encoding="utf-8")
    lines = {}
    for row in r.stdout.splitlines()[1:]:
        p = row.split("\t")
        if len(p) < 12 or p[0] != "5" or not p[11].strip():
            continue
        key = (p[2], p[3], p[4])
        left, top, wd, ht, conf, txt = int(p[6]), int(p[7]), int(p[8]), int(p[9]), float(p[10]), p[11].strip()
        L = lines.setdefault(key, dict(words=[], left=10 ** 9, top=10 ** 9, right=0, bottom=0))
        L["words"].append((txt, conf))
        L["left"], L["top"] = min(L["left"], left), min(L["top"], top)
        L["right"], L["bottom"] = max(L["right"], left + wd), max(L["bottom"], top + ht)
    out = []
    for L in sorted(lines.values(), key=lambda v: v["top"]):
        txt = " ".join((t + " [?]") if (MARK and c < MARK_BELOW) else t for t, c in L["words"])
        out.append(dict(text=txt, left=L["left"], top=L["top"], right=L["right"], bottom=L["bottom"]))
    return out


def crop(img, x0, y0, x1, y1):
    c = img.crop((x0, y0, x1, y1))
    return c.resize((c.width * UP, c.height * UP), Image.LANCZOS)


def paragraphs(lines, indent_px):
    """Group lines into paragraphs; a line indented past the block's left edge starts a new one."""
    if not lines:
        return [], False
    left = min(l["left"] for l in lines)
    paras, cont = [], not (lines[0]["left"] - left > indent_px)
    for i, l in enumerate(lines):
        new = (l["left"] - left > indent_px) or i == 0
        if new and not (i == 0 and cont):
            paras.append(l["text"])
        elif i == 0:
            paras.append(l["text"])
        else:
            paras[-1] += " " + l["text"]
    return paras, cont


FOOT = re.compile(r"^\s*[\(\[]?\s*[১-৯1-9][\)\]]")


def process(exe, cfg, vol, pg, state, lang):
    img = Image.open(b.prepared_image(cfg, vol, pg)).convert("RGB")
    lay = analyse(img)
    w, h = lay["w"], lay["h"]
    rec = dict(title=None, chapter=None, before=0, colophon=None)
    items, col_paras_before = [], 0
    first_cont = False
    colo, heading_y = [], None
    seen_col = False
    for kind, y0, y1 in segments(lay):
        if kind == "col":
            lines = ocr_tsv(exe, crop(img, lay["gap"] + 8, y0, w, y1 + 1), lang, 6)
            paras, cont = paragraphs(lines, int(w * 0.012) * UP)
            if not seen_col:
                first_cont, seen_col = cont, True
            items.append(("col", paras))
        else:
            lines = ocr_tsv(exe, crop(img, 0, y0, w, y1 + 1), lang, 6)
            text = " ".join(l["text"] for l in lines)
            if re.search(r"[ঀ-৿]", text) is None:
                continue
            if "সমাপ্ত" in text or "ইতি" in text.split()[:2] or "শ্রীমন্মহর্ষি" in text:
                colo.append(text)
            elif "অধ্যায়" in text and heading_y is None:
                heading_y = len(items)
                rec["title"] = lines[-1]["text"] if len(lines) >= 3 else None
                items.append(("head", []))
    if heading_y is not None:
        sk = state["key"]
        state["last"] = state.get("last", 0) + 1
        rec["chapter"] = state["last"]
        rec["before"] = sum(len(p) for k, p in items[:heading_y] if k == "col")
    paras = [t for k, p in items if k == "col" for t in p]
    if colo:
        rec["colophon"] = " ".join(colo)
    # Sanskrit verse numbers from the left column
    left_lines = ocr_tsv(exe, crop(img, 0, lay["top"], lay["gap"] - 4, lay["foot"]), lang + "+hin" if has_lang(exe, "hin") else lang, 6)
    sv = []
    for m in re.finditer(r"॥\s*([০-৯0-9०-९]{1,3})", " ".join(l["text"] for l in left_lines)):
        n = int(m.group(1).translate(BN))
        if n not in sv:
            sv.append(n)
    # footnotes
    notes = []
    if lay["foot"] < h - 10:
        for l in ocr_tsv(exe, crop(img, 0, lay["foot"] + 3, w, h), lang, 6):
            if FOOT.match(l["text"]) or not notes:
                notes.append(l["text"])
            else:
                notes[-1] += " " + l["text"]
    return {
        "_page": {"vol": vol, "page": pg, "model": "tesseract"},
        "skandha_heading": None,
        "chapter_number": rec["chapter"],
        "chapter_title": rec["title"],
        "paragraphs_before_chapter_heading": rec["before"],
        "colophon": rec["colophon"],
        "sanskrit_verse_numbers": sv,
        "paragraphs": paras,
        "first_paragraph_continues_previous_page": bool(first_cont and heading_y is None),
        "footnotes": notes,
        "page_label": None,
        "layout_note": "local Tesseract OCR, unreviewed",
    }


def section_of(cfg, vol, pg):
    for v, a, z, sk in cfg["skandha_pages"]:
        if v == vol and a <= pg <= z:
            return f"v{vol}-s{sk}"
    sys.exit(f"page {pg} of volume {vol} is outside every skandha_pages range in config.json")


def parse_pages(s, vol, cfg):
    if s:
        a, z = s.split("-")
        return int(a), int(z)
    sys.exit("--pages is required, e.g. --pages 362-402")


def main():
    global MARK
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("cmd", choices=["check", "layout", "run"])
    ap.add_argument("--vol", choices=["1", "2"])
    ap.add_argument("--pages")
    ap.add_argument("--after-chapter", type=int, help="chapter that is current when the first page starts (0 if the first page opens chapter 1)")
    ap.add_argument("--lang", default="ben")
    ap.add_argument("--no-mark", action="store_true", help="do not add [?] after low-confidence words")
    ap.add_argument("--force", action="store_true")
    a = ap.parse_args()
    MARK = not a.no_mark
    cfg = b.load_config()
    if a.cmd == "check":
        exe = tesseract()
        print("tesseract:", exe)
        print("ben language data:", "ok" if has_lang(exe, "ben") else "MISSING (see LOCAL_OCR.md)")
        print("hin language data:", "ok" if has_lang(exe, "hin") else "missing (optional, only helps the Sanskrit verse numbers)")
        return
    lo, hi = parse_pages(a.pages, a.vol, cfg)
    if a.cmd == "layout":
        for pg in range(lo, hi + 1):
            lay = analyse(Image.open(b.prepared_image(cfg, a.vol, pg)))
            print(f"p{pg}: size {lay['w']}x{lay['h']}  gap x={lay['gap']}  footnote rule y={lay['foot']}  full-width bands={len(lay['bands'])} {lay['bands']}")
        return
    exe = tesseract()
    if not has_lang(exe, a.lang):
        sys.exit(f"Tesseract language '{a.lang}' is missing. See LOCAL_OCR.md step 1.")
    state_all = json.loads(STATE.read_text(encoding="utf-8")) if STATE.exists() else {}
    vis = b.WORK / "vision"
    vis.mkdir(parents=True, exist_ok=True)
    for pg in range(lo, hi + 1):
        out = vis / f"{b.page_key(a.vol, pg)}.json"
        key = section_of(cfg, a.vol, pg)
        if out.exists() and not a.force:
            continue
        st = state_all.setdefault(key, {})
        if "last" not in st:
            if a.after_chapter is None:
                sys.exit(f"First page of {key}: give --after-chapter (0 if page {pg} opens chapter 1).")
            st["last"] = a.after_chapter
        st["key"] = key
        rec = process(exe, cfg, a.vol, pg, st, a.lang)
        out.write_text(json.dumps(rec, ensure_ascii=False, indent=1), encoding="utf-8")
        STATE.write_text(json.dumps(state_all, ensure_ascii=False, indent=1), encoding="utf-8")
        print(f"v{a.vol} p{pg}: {len(rec['paragraphs'])} paragraph(s)" + (f", chapter {rec['chapter_number']} starts" if rec["chapter_number"] else ""))


MARK = True

if __name__ == "__main__":
    main()
