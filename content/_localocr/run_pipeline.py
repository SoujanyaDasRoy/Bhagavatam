#!/usr/bin/env python3
"""Unattended, restartable Odia pipeline for Skandha 7 to 12 (and 6 again with the second model).

    py content/_localocr/run_pipeline.py            run every step that is not finished yet, then stop
    py content/_localocr/run_pipeline.py --status   print where it is (also in work/pipeline_status.json)

Steps, in order, each one skipping work that exists already (so it can be stopped and started any number of times, by anyone):
  1 wait    wait until no other crnn.py / read_pages.py job is running (the GPU has 4 GB: one job at a time)
  2 train_b train model B (second seed) if work/model/crnn_b.pt is missing or training was cut short (log has no epoch 24)
  3 read_a  read skandha 6-12 with model A       -> work/pages_a/vision
  4 read_b  read skandha 6-12 with model B       -> work/pages_b/vision
  5 compare word-level consensus of A, B and stock Tesseract -> work/verify/consensus, work/verify/verify_report.md, review queue
  6 final   collect pages and assemble chapters  -> work/final/out/skandha-NN/chapter-NN.md  (+ work/final/review/report.md)
Log: work/pipeline.log.  Nothing here touches git, content.db or content/or.
"""
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
PY = sys.executable
LOG = WORK / "pipeline.log"
STATUS = WORK / "pipeline_status.json"
PAGES = {"A": WORK / "pages_a" / "vision", "B": WORK / "pages_b" / "vision"}
SK_FIRST, SK_LAST = 6, 12
sys.stdout.reconfigure(encoding="utf-8")


def log(msg):
    line = f"{time.strftime('%Y-%m-%d %H:%M:%S')}  {msg}"
    print(line, flush=True)
    with open(LOG, "a", encoding="utf-8") as fh:
        fh.write(line + "\n")


def spans():
    sp = json.loads((ODIA / "data" / "skandha_pages.json").read_text(encoding="utf-8"))
    out = []
    for k in range(SK_FIRST, SK_LAST + 1):
        for v, a, b in sp[str(k)]:
            out += [(v, p) for p in range(a, b + 1)]
    return list(dict.fromkeys(out))


def count(dirpath):
    want = spans()
    have = sum(1 for v, p in want if (dirpath / f"v{v}_p{p:04d}.json").exists())
    return have, len(want)


def other_jobs():
    r = subprocess.run(["powershell", "-NoProfile", "-Command",
                        "Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'python' -and $_.CommandLine -match 'crnn\\.py|read_pages\\.py' -and $_.ProcessId -ne %d } | Measure-Object | Select-Object -ExpandProperty Count" % os.getpid()],
                       capture_output=True, text=True)
    try:
        return int(r.stdout.strip() or 0)
    except ValueError:
        return 0


def status(step, extra=""):
    a, n = count(PAGES["A"])
    b, _ = count(PAGES["B"])
    d = {"step": step, "time": time.strftime("%Y-%m-%d %H:%M:%S"), "pages_total": n, "read_by_A": a, "read_by_B": b,
         "model_B_epochs": last_epoch(), "note": extra}
    STATUS.write_text(json.dumps(d, indent=1), encoding="utf-8")
    return d


def last_epoch():
    f = WORK / "crnn_b_train.log"
    if not f.exists():
        return 0
    m = re.findall(r"^epoch (\d+)/", f.read_text(encoding="utf-8", errors="replace"), re.M)
    return int(m[-1]) if m else 0


def run(cmd, logfile=None, env=None):
    log("RUN " + " ".join(str(c) for c in cmd))
    e = dict(os.environ, **(env or {}))
    with open(logfile, "a", encoding="utf-8") if logfile else open(os.devnull, "w") as fh:
        p = subprocess.run([str(c) for c in cmd], stdout=fh, stderr=subprocess.STDOUT, env=e, cwd=str(HERE.parent.parent))
    log(f"  exit {p.returncode}")
    return p.returncode


def main():
    WORK.mkdir(exist_ok=True)
    if "--status" in sys.argv:
        print(json.dumps(status("status"), indent=1))
        return 0

    status("wait")
    while other_jobs():
        log("another recogniser job is running; waiting 60 s")
        time.sleep(60)

    # 2 model B
    if last_epoch() < 24 or not (WORK / "model" / "crnn_b.pt").exists():
        status("train_b")
        run([PY, HERE / "crnn.py", "train", "--epochs", "24", "--seed", "2", "--name", "crnn_b.pt"], WORK / "crnn_b_train.log")

    # 3, 4 reading
    for tag, model in (("A", "crnn.pt"), ("B", "crnn_b.pt")):
        done, total = count(PAGES[tag])
        if done < total:
            status("read_" + tag.lower(), f"{done}/{total}")
            run([PY, HERE / "read_pages.py", "--skandha", f"{SK_FIRST}-{SK_LAST}", "--model", model, "--out", PAGES[tag].parent],
                WORK / f"read_{tag.lower()}.log")
        done, total = count(PAGES[tag])
        log(f"reader {tag}: {done}/{total} pages")
        if done < total:
            status("incomplete", f"reader {tag} stopped at {done}/{total}; run again")
            return 2

    # 5 compare
    status("compare")
    ver = WORK / "verify"
    env = {"ODIA_VWORK": str(ver)}
    run([PY, ODIA / "odia_verify.py", "lexicon"], WORK / "compare.log", env)
    run([PY, ODIA / "odia_verify.py", "compare", "--a", PAGES["A"], "--b", PAGES["B"], "--t", WORK / "tess_pages" / "work" / "vision",
         "--out", ver / "consensus", "--lexicon", ver / "lexicon.txt"], WORK / "compare.log", env)
    run([PY, ODIA / "odia_verify.py", "report", "--dir", ver / "consensus"], WORK / "compare.log", env)

    # 6 assemble (consensus pages; [?] marks stay where the readings disagreed)
    status("final")
    fin = WORK / "final"
    run([PY, ODIA / "odia_finalize.py", "--consensus", ver / "consensus", "--trusted", WORK / "no_trusted", "--out", fin], WORK / "final.log", env)
    run([PY, ODIA / "odia_assemble.py", "--starts", ODIA / "data" / "starts_s5_12_dense.json"], WORK / "final.log",
        {"ODIA_WORK": str(fin), "ODIA_OUT": str(fin / "out"), "ODIA_REVIEW": str(fin / "review"),
         "ODIA_METHOD": "Local CNN-BiLSTM-CTC recogniser, two models + Tesseract word consensus (odia_verify.py), unreviewed"})
    status("done", "chapters in work/final/out, report in work/final/review/report.md, disputes in work/verify")
    log("DONE")
    return 0


if __name__ == "__main__":
    sys.exit(main())
