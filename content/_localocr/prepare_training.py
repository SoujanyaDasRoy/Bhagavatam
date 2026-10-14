#!/usr/bin/env python3
"""P1/P2: from work/lines/manifest.jsonl make the training and evaluation sets for Tesseract fine-tuning.

    py content/_localocr/prepare_training.py --min-sim 0.9          select, normalise, split, write box files
    py content/_localocr/prepare_training.py --lstmf --workers 6    then make the .lstmf files (slow: one Tesseract call per line)

Selection (all must hold): transcript text exists, similarity to the stock reading >= --min-sim, no impossible spelling
(odia_text.SEVERE), every character is known to the base model (after the substitutions below), plausible size.
Substitutions, because the base model has no such characters and the project text uses a plain hyphen anyway:
    em / en dash -> '-'   curly single quotes -> "'"   curly double quotes -> '"'   ZWNJ / ZWJ removed
Split by page (never inside a chapter): train = v1:88-410 (Skandha 1 chapter 3 to Skandha 3), eval = v1:411-578 (Skandha 4).
Output (ignored by git): work/train/{train,eval}/<id>.{png,gt.txt,box} and work/train/{train,eval}.list
"""
import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import unicodedata
from concurrent.futures import ProcessPoolExecutor
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "_odia"))
sys.stdout.reconfigure(encoding="utf-8")
import odia_text as T  # noqa: E402

WORK = HERE / "work"
BN = os.environ.get("LOCALOCR_LANG") == "bn"          # Bengali: lines_bn -> train_bn, ben base model, no Odia wa/ba rule
LINES = WORK / ("lines_bn" if BN else "lines")
OUT = WORK / ("train_bn" if BN else "train")
TESS = r"C:\Program Files\Tesseract-OCR\tesseract.exe"
SUBST = {"—": "-", "–": "-", "‘": "'", "’": "'", "“": '"', "”": '"', "\u200c": "", "\u200d": ""}
TRAIN_MAX_PAGE = 410


def base_chars():
    code = "ben" if BN else "ori"
    f = WORK / "tessdata_best" / f"{code}.lstm-unicharset"
    if not f.exists():
        subprocess.run([r"C:\Program Files\Tesseract-OCR\combine_tessdata.exe", "-u", str(WORK / "tessdata_best" / f"{code}.traineddata"),
                        str(WORK / "tessdata_best" / f"{code}.")], check=True, capture_output=True)
    out = set()
    for i, line in enumerate(f.read_text(encoding="utf-8").split("\n")):
        if i and line:
            out.add(line.split(" ")[0])
    return out


def clean_text(s):
    s = unicodedata.normalize("NFC", s)
    for a, b in SUBST.items():
        s = s.replace(a, b)
    # Odia ba-phala and wa-phala are printed alike; the transcript has wa (U+0B71) 799 times and ba 37 times after "ଦ୍" and 1002 to 47 after "ସ୍".
    # One spelling for the label, or the model is trained on noise it cannot resolve from the image.
    # Only after d, s, t, sh, h, j, dh, ss are these the wa-phala cases. After m, r, l, n (sambandha, sarva, parvata, gandharva)
    # the sign is a real "ba": changing it there corrupted the first training labels (found by auditing the output).
    if not BN:
        s = re.sub("(?<=[\u0b26\u0b38\u0b24\u0b36\u0b39\u0b1c\u0b27\u0b37]\u0b4d)\u0b2c", "\u0b71", s)
    return " ".join(s.split())


def line_box(text, w, h):
    """The box file tesstrain uses for a whole-line image: every grapheme gets the full image box, then a tab line."""
    import regex  # grapheme clusters
    out = [f"{g} 0 0 {w} {h} 0" for g in regex.findall(r"\X", text.replace(" ", " ")) if g.strip() or g == " "]
    out.append(f"\t 0 0 {w} {h} 0")
    return "\n".join(out) + "\n"


def select(min_sim):
    known = base_chars()
    rows = [json.loads(x) for x in (LINES / "manifest.jsonl").read_text(encoding="utf-8").split("\n") if x]
    keep, why = [], {}
    for r in rows:
        def drop(k):
            why[k] = why.get(k, 0) + 1
        if not r["gt"]:
            drop("no text"); continue
        if r["sim"] < min_sim:
            drop("low similarity"); continue
        t = clean_text(r["gt"])
        if BN:
            bad = bool(re.search(r"[A-Za-z0-9ऀ-ॣ०-ॿ଀-୿]", t)) or "্্" in t   # Latin, digits, other scripts, double virama
        else:
            bad = any(T.severe(w) for w in t.split())
        if bad:
            drop("impossible spelling"); continue
        if any(c not in known and c != " " for c in t):
            drop("character unknown to base model"); continue
        if not (6 <= len(t) <= 120) or r["h"] < 20 or r["w"] < 100:
            drop("size"); continue
        keep.append({**r, "text": t})
    return keep, why, len(rows)


def write_sets(keep):
    import regex  # noqa: F401  (fail early if missing)
    from PIL import Image  # noqa: F401
    for sub in ("train", "eval"):
        d = OUT / sub
        if d.exists():
            shutil.rmtree(d)
        d.mkdir(parents=True)
    counts = {"train": 0, "eval": 0}
    for r in keep:
        sub = ("train" if (r["vol"] == 1 or r["page"] <= 700) else "eval") if BN else ("train" if r["page"] <= TRAIN_MAX_PAGE else "eval")
        d = OUT / sub
        shutil.copy(LINES / f"{r['id']}.png", d / f"{r['id']}.png")
        (d / f"{r['id']}.gt.txt").write_text(r["text"] + "\n", encoding="utf-8")
        (d / f"{r['id']}.box").write_text(line_box(r["text"], r["w"], r["h"]), encoding="utf-8")
        counts[sub] += 1
    return counts


def _lstmf(path):
    p = Path(path)
    if (p.with_suffix(".lstmf")).exists():
        return 0
    env = dict(os.environ, TESSDATA_PREFIX=str(WORK / "tessdata_best"), OMP_THREAD_LIMIT="1")
    r = subprocess.run([TESS, str(p), str(p.with_suffix("")), "--psm", "13", "-l", "ori", "lstm.train"], capture_output=True, env=env)
    return 0 if p.with_suffix(".lstmf").exists() else 1


def make_lstmf(workers):
    for sub in ("train", "eval"):
        d = OUT / sub
        pngs = sorted(d.glob("*.png"))
        with ProcessPoolExecutor(workers) as ex:
            failed = sum(ex.map(_lstmf, [str(p) for p in pngs], chunksize=50))
        ok = sorted(d.glob("*.lstmf"))
        (OUT / f"{sub}.list").write_text("\n".join(str(p) for p in ok) + "\n", encoding="utf-8")
        print(f"{sub}: {len(ok)} .lstmf files ({failed} failed) -> {OUT / (sub + '.list')}")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--min-sim", type=float, default=0.9)
    ap.add_argument("--lstmf", action="store_true")
    ap.add_argument("--workers", type=int, default=6)
    a = ap.parse_args()
    if a.lstmf:
        make_lstmf(a.workers)
        return
    keep, why, total = select(a.min_sim)
    counts = write_sets(keep)
    print(f"{total} lines in the manifest -> {len(keep)} selected; dropped: {why}")
    print(f"train {counts['train']} (pages 88-{TRAIN_MAX_PAGE}), eval {counts['eval']} (pages {TRAIN_MAX_PAGE + 1}-578)")


if __name__ == "__main__":
    main()
