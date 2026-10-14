#!/usr/bin/env python3
"""Step 1 after the main run: a third local reader (model C) casts the deciding vote on the words A and B disagreed on.

    py content/_localocr/run_settle.py             do every step not finished yet (restartable, like run_pipeline.py)
    py content/_localocr/run_settle.py --status

Steps: 1 train model C (seed 3)  2 read the pages that still have open words with C  3 vote  4 apply  5 report  6 assemble again.
A flagged word is settled only when at least two of the four readings (A, B, C, stock Tesseract) agree on it and no other word
has as many votes. Everything else stays in the review queue. Settled words are recorded as "model" decisions in the consensus
pages, so the audit can sample them as their own group. Nothing touches git, content.db or content/or.
"""
import collections
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
WORK = HERE / "work"
ODIA = HERE.parent / "_odia"
# SETTLE_ROOT=content/_localocr/work/early settles the Mahatmya + Skandha 1-5 pages the same way (model C is reused, not retrained)
ROOT = Path(os.environ.get("SETTLE_ROOT", WORK))
EARLY = ROOT != WORK
VERIFY = ROOT / "verify"
PAGES_C = ROOT / "pages_c" / "vision"
LIST = ROOT / "settle_pages.txt"
LOG = ROOT / "settle.log"
PY = sys.executable
sys.path.insert(0, str(ODIA))
sys.stdout.reconfigure(encoding="utf-8")


def log(msg):
    line = f"{time.strftime('%Y-%m-%d %H:%M:%S')}  {msg}"
    print(line, flush=True)
    with open(LOG, "a", encoding="utf-8") as fh:
        fh.write(line + "\n")


def run(cmd, logfile=None, env=None):
    log("RUN " + " ".join(str(c) for c in cmd)[:200])
    e = dict(os.environ, **(env or {}))
    with open(logfile, "a", encoding="utf-8") if logfile else open(os.devnull, "w") as fh:
        p = subprocess.run([str(c) for c in cmd], stdout=fh, stderr=subprocess.STDOUT, env=e, cwd=str(HERE.parent.parent))
    log(f"  exit {p.returncode}")
    return p.returncode


def epochs_done():
    f = WORK / "crnn_c_train.log"
    if not f.exists():
        return 0
    m = re.findall(r"^epoch (\d+)/", f.read_text(encoding="utf-8", errors="replace"), re.M)
    return int(m[-1]) if m else 0


def open_pages():
    """Pages of the consensus that still have an unresolved flagged word, written to settle_pages.txt."""
    names = []
    for f in sorted((VERIFY / "consensus").glob("v*_p*.json")):
        j = json.loads(f.read_text(encoding="utf-8"))
        if any(x.get("para") is not None and not x.get("resolved") for x in j.get("_verify", {}).get("flags", [])):
            names.append(f.stem)
    LIST.write_text("\n".join(names) + "\n", encoding="utf-8")
    return names


def vote():
    import odia_text as T
    decisions, stats = [], collections.Counter()
    for name in LIST.read_text().split():
        v, p = int(name[1]), int(name[4:8])
        cons_f = VERIFY / "consensus" / f"{name}.json"
        a_f = ROOT / "pages_a" / "vision" / f"{name}.json"
        c_f = PAGES_C / f"{name}.json"
        if not (cons_f.exists() and a_f.exists() and c_f.exists()):
            stats["page without C"] += 1
            continue
        cons = json.loads(cons_f.read_text(encoding="utf-8"))
        a = json.loads(a_f.read_text(encoding="utf-8"))
        c = json.loads(c_f.read_text(encoding="utf-8"))
        a_flat = [(pi, ti, w) for pi, para in enumerate(a.get("paragraphs", [])) for ti, w in enumerate(T.tokens(para))]
        c_flat = [w for para in c.get("paragraphs", []) for w in T.tokens(para)]
        at = {(pi, ti): n for n, (pi, ti, _) in enumerate(a_flat)}
        amap = {i: j for i, j in T.align([w for _, _, w in a_flat], c_flat) if i is not None and j is not None}
        for fl in cons.get("_verify", {}).get("flags", []):
            if fl.get("para") is None or fl.get("resolved"):
                continue
            kind = str(fl["why"]).split(":")[0]
            if kind == "hygiene":
                stats["hygiene (left for a person)"] += 1
                continue
            n = at.get((fl["para"], fl["idx"]))
            cw = c_flat[amap[n]] if n is not None and n in amap else None
            cand = [x for x in (fl.get("a"), fl.get("b"), cw, fl.get("t")) if x]
            votes = collections.Counter(T.core(x) for x in cand if T.core(x))
            if not votes:
                stats["no reading"] += 1
                continue
            ranked = votes.most_common()
            top, k = ranked[0]
            if k >= 2 and (len(ranked) == 1 or ranked[1][1] < k):
                form = next(x for x in cand if T.core(x) == top)
                decisions.append({"vol": v, "page": p, "para": fl["para"], "idx": fl["idx"], "text": form, "votes": k, "was": kind})
                stats[f"settled ({kind})"] += 1
            else:
                stats[f"still open ({kind})"] += 1
    out = ROOT / "settle_decisions.json"
    out.write_text(json.dumps(decisions, ensure_ascii=False, indent=1), encoding="utf-8")
    log(f"vote: {len(decisions)} settled; {dict(stats)}")
    return decisions


def busy():
    r = subprocess.run(["powershell", "-NoProfile", "-Command",
                        "Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'python' -and $_.CommandLine -match 'crnn\\.py|read_pages\\.py|bn_check\\.py|run_early\\.py|run_pipeline\\.py' -and $_.ProcessId -ne %d } | Measure-Object | Select-Object -ExpandProperty Count" % os.getpid()],
                       capture_output=True, text=True)
    try:
        return int(r.stdout.strip() or 0)
    except ValueError:
        return 0


def main():
    WORK.mkdir(exist_ok=True)
    if "--status" in sys.argv:
        names = LIST.read_text().split() if LIST.exists() else []
        done = len(list(PAGES_C.glob("v*_p*.json"))) if PAGES_C.exists() else 0
        print(json.dumps({"model_C_epochs": epochs_done(), "pages_to_read": len(names), "read_by_C": done}, indent=1))
        return 0
    while busy():
        log("another recogniser job is running; waiting 60 s")
        time.sleep(60)
    # 1 train
    if epochs_done() < 24 or not (WORK / "model" / "crnn_c.pt").exists():
        run([PY, HERE / "crnn.py", "train", "--epochs", "24", "--seed", "3", "--name", "crnn_c.pt"], WORK / "crnn_c_train.log")
    # 2 read
    names = LIST.read_text().split() if LIST.exists() else open_pages()
    todo = [n for n in names if not (PAGES_C / f"{n}.json").exists()]
    if todo:
        run([PY, HERE / "read_pages.py", "--list", LIST, "--model", "crnn_c.pt", "--out", PAGES_C.parent], WORK / "read_c.log")
    left = [n for n in names if not (PAGES_C / f"{n}.json").exists()]
    if left:
        log(f"model C has not read {len(left)} pages; run again")
        return 2
    # 3, 4 vote and apply
    decisions = vote()
    env = {"ODIA_VWORK": str(VERIFY)}
    if decisions:
        run([PY, ODIA / "odia_verify.py", "apply", ROOT / "settle_decisions.json", "--source", "model", "--dir", VERIFY / "consensus"], WORK / "settle_apply.log", env)
    # 5 report
    run([PY, ODIA / "odia_verify.py", "report", "--dir", VERIFY / "consensus"], WORK / "settle_apply.log", env)
    # 6 assemble again from the updated consensus
    if EARLY:
        log("DONE (consensus updated; assemble the early chapters after the verse starts for Skandha 1-5 are settled)")
        return 0
    fin = WORK / "final"
    run([PY, ODIA / "odia_finalize.py", "--consensus", VERIFY / "consensus", "--trusted", WORK / "no_trusted", "--out", fin], WORK / "final.log", env)
    run([PY, ODIA / "odia_assemble.py", "--starts", ODIA / "data" / "starts_s5_12_dense.json"], WORK / "final.log",
        {"ODIA_WORK": str(fin), "ODIA_OUT": str(fin / "out"), "ODIA_REVIEW": str(fin / "review"),
         "ODIA_METHOD": "Local CNN-BiLSTM-CTC recognisers (3 models) + Tesseract word vote (odia_verify.py), unreviewed"})
    log("DONE")
    return 0


if __name__ == "__main__":
    sys.exit(main())
