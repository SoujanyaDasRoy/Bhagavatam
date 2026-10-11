#!/usr/bin/env python3
"""Odia Bhagavatam page extractor: PDF page -> layout analysis -> Tesseract (ori) -> page JSON.

Page JSON uses the same schema as the Bengali pipeline (work/vision/vN_pNNNN.json) so that the
assembler can be a straight adaptation of bn_extract.py.
"""
import argparse, json, os, re, subprocess, sys, tempfile
from pathlib import Path
import cv2
import numpy as np
import pytesseract

HERE = Path(__file__).resolve().parent
CFG = json.loads((HERE / "config.json").read_text(encoding="utf-8"))
WORK = Path(os.environ.get("ODIA_WORK", HERE / "work"))
TESSDATA = os.environ.get("TESSDATA_PREFIX", str(HERE / "tessdata"))
os.environ["TESSDATA_PREFIX"] = TESSDATA
os.environ["OMP_THREAD_LIMIT"] = "1"
DPI = 300
OD_DIGITS = "୦୧୨୩୪୫୬୭୮୯"


def pdf_path(vol):
    return os.environ.get(f"ODIA_PDF_{vol}") or CFG["volumes"][str(vol)]["path"]


def render(vol, page):
    import shutil
    if not shutil.which("pdftoppm"):
        import fitz  # PyMuPDF fallback (e.g. on Windows without poppler)
        doc = fitz.open(pdf_path(vol))
        pix = doc[page - 1].get_pixmap(dpi=DPI, colorspace=fitz.csGRAY)
        return np.frombuffer(pix.samples, dtype=np.uint8).reshape(pix.height, pix.width).copy()
    with tempfile.TemporaryDirectory() as d:
        subprocess.run(["pdftoppm", "-r", str(DPI), "-gray", "-f", str(page), "-l", str(page), "-png", pdf_path(vol), d + "/p"], check=True)
        f = next(Path(d).glob("p-*.png"))
        return cv2.imread(str(f), cv2.IMREAD_GRAYSCALE)


def groups(profile, thresh, merge_gap):
    """Runs of indexes where profile > thresh, merging runs separated by <= merge_gap."""
    on = profile > thresh
    runs, start = [], None
    for i, v in enumerate(on):
        if v and start is None:
            start = i
        elif not v and start is not None:
            runs.append([start, i - 1]); start = None
    if start is not None:
        runs.append([start, len(on) - 1])
    merged = []
    for r in runs:
        if merged and r[0] - merged[-1][1] <= merge_gap:
            merged[-1][1] = r[1]
        else:
            merged.append(r)
    return merged


def analyse(g):
    """Layout: header/footer, column split x, body zones [(y0,y1,'col'|'full')]."""
    H, W = g.shape
    _, braw = cv2.threshold(g, 0, 1, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)
    braw[:, :int(W * 0.04)] = 0; braw[:, int(W * 0.96):] = 0; braw[:int(H * 0.015), :] = 0; braw[int(H * 0.985):, :] = 0
    # long vertical strokes (page frame lines) are removed everywhere
    closed = cv2.morphologyEx(braw, cv2.MORPH_CLOSE, cv2.getStructuringElement(cv2.MORPH_RECT, (1, 25)))
    vm0 = cv2.morphologyEx(braw, cv2.MORPH_OPEN, cv2.getStructuringElement(cv2.MORPH_RECT, (1, 90)))
    vm = cv2.dilate(vm0, np.ones((1, 7), np.uint8))
    b = (braw & (1 - vm)).astype(np.uint8)
    rg = [r for r in groups(b.sum(1), 2, 14) if (r[1] - r[0]) > 12]
    if not rg:
        return None
    x_ink = np.where(b.sum(0) > 0)[0]
    x0, x1 = int(x_ink.min()), int(x_ink.max())
    header = footer = None
    if rg[0][1] < H * 0.10:
        header = tuple(rg[0]); rg = rg[1:]
    if rg and rg[-1][0] > H * 0.90 and (rg[-1][1] - rg[-1][0]) < H * 0.05:
        footer = tuple(rg[-1]); rg = rg[:-1]
    out = {"H": H, "W": W, "header": header, "footer": footer, "zones": [], "split": None, "x0": x0, "x1": x1, "b": b, "gclean": g, "braw": braw}
    if not rg:
        return out
    by0, by1 = rg[0][0], rg[-1][1]
    n = by1 - by0 + 1
    body_raw = braw[by0:by1 + 1]
    clo, chi = int(W * 0.44), int(W * 0.56)
    split = None; rule_cols = []
    vcov = vm0[by0:by1 + 1].mean(0)
    cand = [x for x in range(clo, chi) if vcov[x] >= 0.12]
    if cand:
        split = int(np.mean(cand))
        rule_cols = list(range(min(cand) - 3, max(cand) + 4))
    body = b[by0:by1 + 1].copy()
    if rule_cols:
        body[:, rule_cols] = 0
    cf = body.mean(0)
    if split is None:
        for th in (0.03, 0.08, 0.15, 0.25):
            zr = groups((cf[clo:chi] <= th).astype(int), 0, 1)
            zr = [(clo + p, clo + q) for p, q in zr if q - p >= 14]
            if zr:
                z = max(zr, key=lambda z: (z[1] - z[0]) - abs((z[0] + z[1]) / 2 - W / 2) / 10.0)
                split = (z[0] + z[1]) // 2
                break
    if split is None:
        out["zones"] = [(by0, by1, "full")]
        out["gclean"] = g
        return out
    gclean = g.copy()
    gclean[vm > 0] = 255
    if rule_cols:
        gclean[:, rule_cols] = 255
    out["gclean"] = gclean
    out["split"] = split
    lines = [l for l in groups(body.sum(1), 1, 3) if (l[1] - l[0]) >= 4]
    zones = []
    for (la, lc) in lines:
        near = body[la:lc + 1, split - 10:split + 11].sum()
        t = "full" if near >= 4 else "col"
        if zones and zones[-1][2] == t and (by0 + la) - zones[-1][1] <= 90:
            zones[-1] = (zones[-1][0], by0 + lc, t)
        else:
            zones.append((by0 + la, by0 + lc, t))
    full = sum(z[1] - z[0] for z in zones if z[2] == "full")
    if full / max(1, n) > 0.80:
        zones = [(by0, by1, "full")]; out["split"] = None
    out["zones"] = zones
    return out


def ocr_lines(img, psm=6, conf_cut=30):
    """OCR an image, return list of dict(text, left, top, conf_words) per text line."""
    d = pytesseract.image_to_data(img, lang="ori", config=f"--oem 1 --psm {psm}", output_type=pytesseract.Output.DICT)
    lines = {}
    for i, t in enumerate(d["text"]):
        if not t.strip():
            continue
        key = (d["block_num"][i], d["par_num"][i], d["line_num"][i])
        ln = lines.setdefault(key, {"words": [], "left": d["left"][i], "top": d["top"][i]})
        ln["left"] = min(ln["left"], d["left"][i])
        c = float(d["conf"][i])
        ln["words"].append((t, c))
    out = []
    for key in sorted(lines, key=lambda k: lines[k]["top"]):
        ln = lines[key]
        out.append({"left": ln["left"], "top": ln["top"], "words": ln["words"]})
    return out


def line_text(ln, mark=True, cut=22):
    ws = []
    for t, c in ln["words"]:
        if mark and c < cut and re.search(r"[\u0B00-\u0B7F]", t) and len(re.sub(r"[॥।|!]", "", t)) > 1:
            m = re.match(r"^(.*?)([॥।|!]+[’'”\"]?)?$", t)
            core, tail = m.group(1), (m.group(2) or "")
            ws.append(core + " [?]" + tail)
        else:
            ws.append(t)
    return " ".join(ws)


def paragraphs_from(lines, indent=28):
    if not lines:
        return []
    edge = np.percentile([l["left"] for l in lines], 20)
    paras, cur = [], []
    for ln in lines:
        txt = line_text(ln)
        if cur and ln["left"] > edge + indent:
            paras.append(" ".join(cur)); cur = []
        cur.append(txt)
    if cur:
        paras.append(" ".join(cur))
    return paras


def first_line_indented(lines, indent=28):
    if not lines:
        return False
    edge = np.percentile([l["left"] for l in lines], 20)
    return not (lines[0]["left"] > edge + indent)


CO_RE = r"^\s*(?:ଇତି|କତି|ଇଜି|ଇତ|କତ)\s"
DIG = r"[୦-୯]"


def digits_to_int(s):
    t = "".join(str(OD_DIGITS.index(c)) for c in s if c in OD_DIGITS)
    return int(t) if t else None


def sanskrit_numbers(lines):
    nums = []
    for ln in lines:
        txt = " ".join(w for w, _ in ln["words"])
        for m in re.finditer(r"[॥।|]+\s*((?:" + DIG + r"\s*){1,3})\s*[॥।|]*\s*$", txt):
            n = digits_to_int(m.group(1))
            if n:
                nums.append(n)
    return nums


def classify_band(text):
    t = text.replace("\n", " ")
    kind = None
    if re.search(r"ସ୍କନ୍ଧ", t) and not re.search(r"ଅଧ୍ୟାୟ|ଧ୍ୟାୟ", t.replace("ସ୍କନ୍ଧ", "")):
        kind = "skandha"
    if re.search(r"^\s*ଅଥ\b.*ଧ୍ୟାୟ", t) or re.search(r"ଅଥ\s+\S*ଧ୍ୟାୟଃ", t) or ("ଧ୍ୟାୟଃ" in t and len(t) < 400 and not t.strip().startswith("ଇତି")):
        kind = "chapter"
    if t.strip().startswith("ଇତି") or "ଇତି ଶ୍ରୀମଦ୍ଭାଗବତେ" in t:
        kind = "colophon"
    return kind


SK_WORDS = [("ପ୍ରଥମ", 1), ("ଦ୍ଵିତୀୟ", 2), ("ଦ୍ବିତୀୟ", 2), ("ତୃତୀୟ", 3), ("ଚତୁର୍ଥ", 4), ("ପଞ୍ଚମ", 5), ("ଷଷ୍ଠ", 6), ("ସପ୍ତମ", 7), ("ଅଷ୍ଟମ", 8), ("ନବମ", 9), ("ଦଶମ", 10), ("ଏକାଦଶ", 11), ("ଦ୍ଵାଦଶ", 12), ("ଦ୍ବାଦଶ", 12)]


def skandha_from(text):
    if "ସ୍କନ୍ଧ" not in text:
        return None
    pre = text.split("ସ୍କନ୍ଧ")[0]
    for w, n in SK_WORDS:
        if w in pre:
            return n
    return None


def header_info(g, lay):
    if not lay or not lay["header"]:
        return {}
    y0, y1 = lay["header"]
    crop = g[max(0, y0 - 6):y1 + 8, :]
    t = pytesseract.image_to_string(crop, lang="ori", config="--oem 1 --psm 7").strip()
    m = re.search(r"ଅଧ୍ୟାୟ\s*((?:" + DIG + r"\s*){1,3})", t)
    return {"text": t, "chapter": digits_to_int(m.group(1)) if m else None}


def extract_page(vol, page, keep_images=False):
    g = render(vol, page)
    lay = analyse(g)
    rec = {"_page": {"vol": str(vol), "page": page, "model": "tesseract-ori"}, "paragraphs": [], "sanskrit_verse_numbers": [],
           "footnotes": [], "skandha_heading": None, "chapter_number": None, "chapter_title": "", "colophon": None,
           "paragraphs_before_chapter_heading": 0, "first_paragraph_continues_previous_page": False}
    raw = {}
    if lay is None:
        rec["layout_note"] = "blank page"
        return rec, raw
    hdr = header_info(g, lay)
    rec["running_head"] = hdr.get("text", "")
    rec["running_head_chapter"] = hdr.get("chapter")
    rec["running_head_skandha"] = skandha_from(hdr.get("text", ""))
    gx = lay["split"]
    seen_heading = False
    first_para = True
    zones_l = lay["zones"]
    # short 'col' zones sandwiched between full zones are really full-width lines (2nd colophon line); merge close full zones
    zz = [list(z) for z in zones_l]
    for i in range(1, len(zz) - 1):
        if zz[i][2] == "col" and zz[i][1] - zz[i][0] < 150 and zz[i - 1][2] == "full" and zz[i + 1][2] == "full":
            zz[i][2] = "full"
    mz = []
    for z in zz:
        if mz and z[2] == "full" and mz[-1][2] == "full" and z[0] - mz[-1][1] < 60:
            mz[-1][1] = z[1]
        else:
            mz.append(z)
    zones_l = [tuple(z) for z in mz]
    big = [i for i, z in enumerate(zones_l) if z[2] == "col" and z[1] - z[0] >= 350]
    last_big = max([i for i, z in enumerate(zones_l) if z[2] == "col"], default=None)
    _ = big
    body_top = zones_l[0][0] if zones_l else 0
    body_h = (zones_l[-1][1] - body_top) if zones_l else 1
    for zi, (y0, y1, kind) in enumerate(zones_l):
        pad = 6
        ya, yb = max(0, y0 - pad), min(lay["H"], y1 + pad)
        if kind == "full":
            braw = lay["braw"]
            zone = braw[ya:yb]
            hl = cv2.morphologyEx(zone, cv2.MORPH_OPEN, cv2.getStructuringElement(cv2.MORPH_RECT, (int(lay["W"] * 0.25), 1)))
            hrows = groups(hl.sum(1), 0, 3)
            hrows = [(a_, c_) for a_, c_ in hrows]
            box = None
            for i in range(len(hrows)):
                for j in range(i + 1, len(hrows)):
                    dist = hrows[j][0] - hrows[i][1]
                    if 30 <= dist <= 260:
                        box = (ya + hrows[i][0], ya + hrows[j][1]); break
                if box: break
            gcl = lay["gclean"]
            if box and gx is not None:
                below = gcl[box[1] + 6:yb, lay["x0"] - 6: lay["x1"] + 6]
                tl = ocr_lines(below) if below.shape[0] > 25 else []
                title = " ".join(" ".join(line_text(l, mark=False) for l in tl).split())
                if box[0] - ya > 40:
                    ab = gcl[ya:box[0] - 4, lay["x0"] - 6: lay["x1"] + 6]
                    al = ocr_lines(ab)
                    at = "\n".join(line_text(l, mark=False) for l in al)
                    if re.match(r"^\s*ଇତି\s", at):
                        rec["colophon"] = " ".join(at.split())
                        raw.setdefault("bands", []).append({"kind": "colophon", "text": at, "y": [y0, box[0]]})
                rec["chapter_marker_text"] = "boxed heading"
                rec["chapter_title"] = title
                rec["paragraphs_before_chapter_heading"] = len(rec["paragraphs"])
                raw.setdefault("bands", []).append({"kind": "chapter", "text": title, "y": [y0, y1]})
                continue
            crop = gcl[ya:yb, lay["x0"] - 6: lay["x1"] + 6]
            lines = ocr_lines(crop)
            text = "\n".join(line_text(l, mark=False) for l in lines)
            ci = next((k for k, l in enumerate(lines) if re.match(CO_RE, line_text(l, mark=False))), None)
            if ci is not None:
                _ct = " ".join(line_text(l, mark=False) for l in lines[ci:ci + 2])
                if not any(w in _ct for w in ("ଭାଗବ", "ଭାରଗ", "ଗବତ", "ମହାପୁ", "ମହାଫୁ", "ମହାଦୁ", "ମହାଣୁ", "ସଂହିତ", "ପାରମ", "ସ୍କନ୍ଧ", "ଷ୍କନ୍ଧ", "ଯୋପାଖ୍ୟ", "ଧ୍ୟାୟ")):
                    ci = None
            hi = next((k for k, l in enumerate(lines) if re.search(r"ଧ୍\S{0,2}[ୟଯ]ାୟ\s*[ଃ:]", line_text(l, mark=False)) and re.match(r"^\W*\S{0,3}\s*ଅ", line_text(l, mark=False))), None)
            if gx is not None and ((ci is not None and ((y1 - y0) >= 330 or len(lines) > ci + 2)) or (hi is not None and last_big is not None and zi < last_big and sum(len(re.findall(r"[\u0B15-\u0B39]", line_text(l, mark=False))) >= 6 for l in lines[hi + 1:]) >= 1)):
                start = ci + 2 if ci is not None else (hi + 1 if hi is not None else 0)
                if ci is not None:
                    rec["colophon"] = re.sub(CO_RE, "ଇତି ", " ".join(" ".join(line_text(l, mark=False) for l in lines[ci:ci + 2]).split()) + " ", count=1).strip()
                else:
                    start = hi + 1
                rest = [l for l in lines[start:] if len(re.findall(r"[\u0B15-\u0B39]", line_text(l, mark=False))) >= 6]
                rt = [line_text(l, mark=False) for l in rest]
                title = " ".join(rt[-1].split()) if rt else ""
                if rest or hi is not None:
                    rec["chapter_marker_text"] = "boxed heading (text)"
                    rec["chapter_title"] = title
                    rec["paragraphs_before_chapter_heading"] = len(rec["paragraphs"])
                raw.setdefault("bands", []).append({"kind": "colophon+heading", "text": text, "y": [y0, y1]})
                continue
            if gx is not None and re.match(r"^\s*ଇତି\s", text) and (y1 - y0) < 330:
                rec["colophon"] = " ".join(text.split())
                raw.setdefault("bands", []).append({"kind": "colophon", "text": text, "y": [y0, y1]})
                continue
            if gx is not None and last_big is not None and zi > last_big and (y0 - body_top) > 0.45 * body_h:
                fcrop = gcl[ya:min(lay["H"], zones_l[-1][1] + 8), lay["x0"] - 6: lay["x1"] + 6]
                fl = ocr_lines(fcrop)
                fparas = paragraphs_from(fl, indent=40)
                rec["footnotes"] = fparas
                raw.setdefault("bands", []).append({"kind": "footnote", "text": "\n".join(line_text(l, mark=False) for l in fl), "y": [y0, zones_l[-1][1]]})
                break
            if gx is not None:
                kind = "col"      # not a real band: read it per column
            else:
                hj = next((k for k, l in enumerate(lines[:4]) if re.search(r"ଧ୍\S{0,2}[ୟଯ]ାୟ\s*[ଃ:]", line_text(l, mark=False)) and re.match(r"^\W*\S{0,3}\s*ଅ", line_text(l, mark=False))), None)
                if hj is not None and not rec["chapter_title"] and len(lines) > hj + 2:
                    cand = [l for l in lines[hj + 1:hj + 4] if len(re.findall(r"[\u0B15-\u0B39]", line_text(l, mark=False))) >= 6]
                    if cand:
                        rec["chapter_title"] = " ".join(line_text(cand[0], mark=False).split())
                        rec["chapter_marker_text"] = "boxed heading (text)"
                        rec["paragraphs_before_chapter_heading"] = 0
                        lines = [l for l in lines[hj + 1:] if l is not cand[0]]
                paras = paragraphs_from(lines)
                if paras:
                    if first_para:
                        rec["first_paragraph_continues_previous_page"] = not first_line_indented(lines)
                        first_para = False
                    rec["paragraphs"] += paras
                continue
        if kind == "col":
            gcl = lay["gclean"]
            left = gcl[ya:yb, lay["x0"] - 6: gx - 8]
            right = gcl[ya:yb, gx + 8: lay["x1"] + 6]
            ll = ocr_lines(left)
            rl = ocr_lines(right)
            raw.setdefault("left", []).append("\n".join(line_text(l, mark=False) for l in ll))
            raw.setdefault("right", []).append("\n".join(line_text(l, mark=False) for l in rl))
            rec["sanskrit_verse_numbers"] += sanskrit_numbers(ll)
            paras = paragraphs_from(rl)
            if paras:
                if first_para:
                    rec["first_paragraph_continues_previous_page"] = not first_line_indented(rl)
                    first_para = False
                rec["paragraphs"] += paras
    return rec, raw


def save_page(rec, raw):
    vol, page = rec["_page"]["vol"], rec["_page"]["page"]
    key = f"v{vol}_p{page:04d}"
    (WORK / "vision").mkdir(parents=True, exist_ok=True)
    (WORK / "ocr_raw").mkdir(parents=True, exist_ok=True)
    (WORK / "vision" / f"{key}.json").write_text(json.dumps(rec, ensure_ascii=False, indent=1), encoding="utf-8")
    (WORK / "ocr_raw" / f"{key}.json").write_text(json.dumps(raw, ensure_ascii=False, indent=1), encoding="utf-8")
    return key


def _job(args):
    vol, page, force = args
    key = f"v{vol}_p{page:04d}"
    if not force and (WORK / "vision" / f"{key}.json").exists():
        return key, "skip"
    try:
        rec, raw = extract_page(vol, page)
        save_page(rec, raw)
        return key, "ok"
    except Exception as e:  # keep going; log
        (WORK).mkdir(parents=True, exist_ok=True)
        with open(WORK / "errors.log", "a", encoding="utf-8") as f:
            f.write(f"{key}: {e!r}\n")
        return key, "error"


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    r = sub.add_parser("run"); r.add_argument("--vol", required=True); r.add_argument("--pages", required=True)
    r.add_argument("--workers", type=int, default=2); r.add_argument("--force", action="store_true")
    a = ap.parse_args()
    lo, _, hi = a.pages.partition("-"); lo, hi = int(lo), int(hi or lo)
    jobs = [(a.vol, p, a.force) for p in range(lo, hi + 1)]
    from multiprocessing import Pool
    done = 0
    with Pool(a.workers) as pool:
        for key, st in pool.imap(_job, jobs):
            done += 1
            if done % 10 == 0 or st == "error":
                print(f"{done}/{len(jobs)} {key} {st}", flush=True)


if __name__ == "__main__":
    main()
