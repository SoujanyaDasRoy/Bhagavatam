#!/usr/bin/env python3
"""Bengali check by cross-fitting: every line is read by a model that never saw that page, then compared with the stored text.

    py content/_localocr/bn_check.py             restartable; waits for the GPU, then folds -> train 2 models -> read -> report
    py content/_localocr/bn_check.py --status

Pages are split into two folds (blocks of 25 pages, alternating, per volume). Model F0 trains on fold-1 lines and reads fold 0,
model F1 the other way round, so a stored-text error cannot be copied back by a model that memorised it.
For every line with a stored text (work/lines_bn/manifest.jsonl): reader R, stored text G, stock Tesseract H.
    R = G                  fine
    R != G and R = H       the stored text is probably wrong  (candidates.csv, listed first)
    R != G and G = H       the reader is probably wrong       (not listed)
    all three differ       unclear                            (listed after)
This FINDS errors; it does not fix the text. Nothing here touches content/bn, content.db or git. Output: work/bn_check/.
"""
import csv
import json
import os
import re
import subprocess
import sys
import time
import unicodedata
from pathlib import Path

HERE = Path(__file__).resolve().parent
WORK = HERE / "work"
OUT = WORK / "bn_check"
LINES = WORK / "lines_bn"
PY = sys.executable
BLOCK = 25
EPOCHS = 20
sys.stdout.reconfigure(encoding="utf-8")
SUBST = {"—": "-", "–": "-", "‘": "'", "’": "'", "“": '"', "”": '"', "‌": "", "‍": ""}


def log(msg):
    OUT.mkdir(parents=True, exist_ok=True)
    line = f"{time.strftime('%Y-%m-%d %H:%M:%S')}  {msg}"
    print(line, flush=True)
    with open(OUT / "bn_check.log", "a", encoding="utf-8") as fh:
        fh.write(line + "\n")


def norm(s):
    s = unicodedata.normalize("NFC", s or "")
    for a, b in SUBST.items():
        s = s.replace(a, b)
    return " ".join(s.split())


def fold_of(vol, page):
    return ((page // BLOCK) + vol) % 2


def busy():
    r = subprocess.run(["powershell", "-NoProfile", "-Command",
                        "Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'python' -and $_.CommandLine -match 'crnn\\.py|read_pages\\.py|run_settle\\.py|run_pipeline\\.py|run_early\\.py' -and $_.ProcessId -ne %d } | Measure-Object | Select-Object -ExpandProperty Count" % os.getpid()],
                       capture_output=True, text=True)
    try:
        return int(r.stdout.strip() or 0)
    except ValueError:
        return 0


def rows():
    return [json.loads(x) for x in (LINES / "manifest.jsonl").read_text(encoding="utf-8").split("\n") if x.strip()]


def make_folds():
    """work/train_bn_f{0,1}: train = selected lines of the OTHER fold, eval = 600 selected lines of its own fold (checkpoint choice only)."""
    sel = {}
    for sub in ("train", "eval"):
        for gt in (WORK / "train_bn" / sub).glob("*.gt.txt"):
            sel[gt.name[:-7]] = (WORK / "train_bn" / sub / gt.name, WORK / "train_bn" / sub / (gt.name[:-7] + ".png"))
    for f in (0, 1):
        d = WORK / f"train_bn_f{f}"
        if (d / "ready").exists():
            continue
        for sub in ("train", "eval"):
            (d / sub).mkdir(parents=True, exist_ok=True)
        n_eval = 0
        for lid, (g, p) in sorted(sel.items()):
            m = re.match(r"v(\d)_p(\d+)_", lid)
            ff = fold_of(int(m.group(1)), int(m.group(2)))
            if ff == f:
                if n_eval >= 600 or hash(lid) % 7:
                    continue
                sub = "eval"
                n_eval += 1
            else:
                sub = "train"
            for src in (g, p):
                dst = d / sub / src.name
                if not dst.exists():
                    os.link(src, dst)
        (d / "ready").write_text("ok")
        log(f"fold {f}: train {len(list((d / 'train').glob('*.gt.txt')))}, eval {len(list((d / 'eval').glob('*.gt.txt')))}")


def epochs_done(f):
    log_f = OUT / f"train_f{f}.log"
    if not log_f.exists():
        return 0
    m = re.findall(r"^epoch (\d+)/", log_f.read_text(encoding="utf-8", errors="replace"), re.M)
    return int(m[-1]) if m else 0


def train(f):
    if epochs_done(f) >= EPOCHS and (WORK / "model" / f"crnn_bn_f{f}.pt").exists():
        return
    env = dict(os.environ, LOCALOCR_LANG="bn", LOCALOCR_TRAINDIR=f"train_bn_f{f}")
    log(f"train fold-{1 - f} lines -> model f{f}")
    with open(OUT / f"train_f{f}.log", "a", encoding="utf-8") as fh:
        subprocess.run([PY, str(HERE / "crnn.py"), "train", "--epochs", str(EPOCHS), "--seed", str(10 + f), "--name", f"crnn_bn_f{f}.pt"],
                       stdout=fh, stderr=subprocess.STDOUT, env=env, cwd=str(HERE.parent.parent))


def read_all():
    if (OUT / "reads.jsonl").exists():
        return
    sys.argv = [sys.argv[0]]
    os.environ["LOCALOCR_LANG"] = "bn"
    sys.path.insert(0, str(HERE))
    import cv2
    import torch
    import crnn
    models = {}
    for f in (0, 1):
        ck = torch.load(WORK / "model" / f"crnn_bn_f{f}.pt", map_location=crnn.DEV)
        m = crnn.CRNN(len(ck["chars"]) + 1).to(crnn.DEV)
        m.load_state_dict(ck["state"])
        m.eval()
        models[f] = (m, ck["chars"])
    todo = {0: [], 1: []}
    for r in rows():
        if r.get("gt") and (LINES / f"{r['id']}.png").exists():
            todo[fold_of(r["vol"], r["page"])].append(r)
    with open(OUT / "reads.jsonl.part", "w", encoding="utf-8") as out:
        for f, rs in todo.items():
            m, chars = models[f]
            log(f"reading {len(rs)} lines of fold {f}")
            for i in range(0, len(rs), 256):
                chunk = rs[i:i + 256]
                imgs = [cv2.imread(str(LINES / f"{r['id']}.png"), cv2.IMREAD_GRAYSCALE) for r in chunk]
                for r, t in zip(chunk, crnn.read_images(m, chars, imgs)):
                    out.write(json.dumps({"id": r["id"], "vol": r["vol"], "page": r["page"], "gt": r["gt"], "hyp": r.get("hyp", ""), "read": t},
                                         ensure_ascii=False) + "\n")
    (OUT / "reads.jsonl.part").replace(OUT / "reads.jsonl")


def words_diff(a, b):
    import difflib
    wa, wb = a.split(), b.split()
    sm = difflib.SequenceMatcher(a=wa, b=wb, autojunk=False)
    return [(" ".join(wa[i1:i2]), " ".join(wb[j1:j2])) for tag, i1, i2, j1, j2 in sm.get_opcodes() if tag != "equal"]


def report():
    cls = {"same": 0, "stored_wrong": [], "unclear": [], "reader_wrong": 0}
    for x in (json.loads(l) for l in (OUT / "reads.jsonl").read_text(encoding="utf-8").split("\n") if l.strip()):
        g, r, h = norm(x["gt"]), norm(x["read"]), norm(x["hyp"])
        if g == r:
            cls["same"] += 1
        elif r == h:
            cls["stored_wrong"].append((x, g, r))
        elif g == h:
            cls["reader_wrong"] += 1
        else:
            cls["unclear"].append((x, g, r))
    n = cls["same"] + len(cls["stored_wrong"]) + len(cls["unclear"]) + cls["reader_wrong"]
    with open(OUT / "candidates.csv", "w", encoding="utf-8-sig", newline="") as fh:
        w = csv.writer(fh)
        w.writerow(["kind", "vol", "page", "line", "stored", "reader", "word changes"])
        for kind, items in (("stored probably wrong", cls["stored_wrong"]), ("unclear", cls["unclear"])):
            for x, g, r in sorted(items, key=lambda t: (t[0]["vol"], t[0]["page"], t[0]["id"])):
                w.writerow([kind, x["vol"], x["page"], x["id"], g, r, " | ".join(f"{a} > {b}" for a, b in words_diff(g, r))])
    md = [f"# Bengali cross-fit check", "",
          f"- lines compared: {n}", f"- reader = stored text: {cls['same']} ({100 * cls['same'] / max(1, n):.1f}%)",
          f"- stored text probably wrong (reader = stock Tesseract, both differ from stored): {len(cls['stored_wrong'])}",
          f"- unclear (all three differ): {len(cls['unclear'])}", f"- reader probably wrong (stored = Tesseract): {cls['reader_wrong']}", "",
          "Details: candidates.csv (open in a spreadsheet). A candidate is a place to look at the page, not a correction."]
    (OUT / "report.md").write_text("\n".join(md), encoding="utf-8")
    log("report: " + "; ".join(md[2:7]))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    if "--status" in sys.argv:
        print(json.dumps({"fold0_epochs": epochs_done(0), "fold1_epochs": epochs_done(1), "of": EPOCHS,
                          "read": (OUT / "reads.jsonl").exists(), "report": (OUT / "report.md").exists()}, indent=1))
        return 0
    while busy():
        log("another recogniser job is running; waiting 60 s")
        time.sleep(60)
    make_folds()
    train(0)
    train(1)
    if epochs_done(0) < EPOCHS or epochs_done(1) < EPOCHS:
        log("training did not finish; run again")
        return 2
    read_all()
    report()
    log("DONE")
    return 0


if __name__ == "__main__":
    sys.exit(main())
