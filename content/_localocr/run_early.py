#!/usr/bin/env python3
"""Read the Mahatmya and Skandha 1 to 5 (volume 1, pages 21-697) with the local readers, so they can be checked the same way as 6 to 12.

    py content/_localocr/run_early.py             restartable; waits until run_settle.py and every other recogniser job has finished
    py content/_localocr/run_early.py --status

Steps: read with A, read with B (work/early/pages_a, pages_b), then word consensus of A, B and stock Tesseract
(work/early/verify/consensus, verify_report.md). The old Skandha 1 to 4 transcripts are NOT touched; compare them after.
Note: the models trained on lines of these pages (train split v1:88-410), so agreement here is less independent than for 6 to 12.
"""
import json
import os
import subprocess
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
WORK = HERE / "work"
EARLY = WORK / "early"
ODIA = HERE.parent / "_odia"
PY = sys.executable
FIRST, LAST = 21, 697
LOG = EARLY / "early.log"
sys.stdout.reconfigure(encoding="utf-8")


def log(msg):
    line = f"{time.strftime('%Y-%m-%d %H:%M:%S')}  {msg}"
    print(line, flush=True)
    with open(LOG, "a", encoding="utf-8") as fh:
        fh.write(line + "\n")


def busy():
    r = subprocess.run(["powershell", "-NoProfile", "-Command",
                        "Get-CimInstance Win32_Process | Where-Object { $_.Name -match 'python' -and $_.CommandLine -match 'crnn\\.py|read_pages\\.py|run_settle\\.py|run_pipeline\\.py' -and $_.ProcessId -ne %d } | Measure-Object | Select-Object -ExpandProperty Count" % os.getpid()],
                       capture_output=True, text=True)
    try:
        return int(r.stdout.strip() or 0)
    except ValueError:
        return 0


def have(tag):
    d = EARLY / f"pages_{tag}" / "vision"
    return sum(1 for p in range(FIRST, LAST + 1) if (d / f"v1_p{p:04d}.json").exists())


def run(cmd, logfile, env=None):
    log("RUN " + " ".join(str(c) for c in cmd))
    with open(logfile, "a", encoding="utf-8") as fh:
        p = subprocess.run([str(c) for c in cmd], stdout=fh, stderr=subprocess.STDOUT, env=dict(os.environ, **(env or {})), cwd=str(HERE.parent.parent))
    log(f"  exit {p.returncode}")
    return p.returncode


def main():
    EARLY.mkdir(parents=True, exist_ok=True)
    total = LAST - FIRST + 1
    if "--status" in sys.argv:
        print(json.dumps({"pages": total, "read_by_A": have("a"), "read_by_B": have("b"),
                          "consensus": len(list((EARLY / "verify" / "consensus").glob("v*_p*.json")))}, indent=1))
        return 0
    while busy():
        log("another recogniser job is running; waiting 60 s")
        time.sleep(60)
    for tag, model in (("a", "crnn.pt"), ("b", "crnn_b.pt")):
        if have(tag) < total:
            run([PY, HERE / "read_pages.py", "--vol", "1", "--pages", f"{FIRST}-{LAST}", "--model", model, "--out", EARLY / f"pages_{tag}"],
                EARLY / f"read_{tag}.log")
        if have(tag) < total:
            log(f"reader {tag.upper()} stopped at {have(tag)}/{total}; run again")
            return 2
    ver = EARLY / "verify"
    run([PY, ODIA / "odia_verify.py", "compare", "--a", EARLY / "pages_a" / "vision", "--b", EARLY / "pages_b" / "vision",
         "--t", WORK / "tess_pages" / "work" / "vision", "--out", ver / "consensus", "--lexicon", WORK / "verify" / "lexicon.txt"],
        EARLY / "compare.log", {"ODIA_VWORK": str(ver)})
    run([PY, ODIA / "odia_verify.py", "report", "--dir", ver / "consensus"], EARLY / "compare.log", {"ODIA_VWORK": str(ver)})
    log("DONE")
    return 0


if __name__ == "__main__":
    sys.exit(main())
