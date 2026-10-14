#!/usr/bin/env python3
"""P2: fine-tune the Tesseract Odia (or Bengali) LSTM on the prepared lines, then evaluate against the stock model.

    py content/_localocr/run_training.py train --iterations 20000          (resumes from the last checkpoint)
    py content/_localocr/run_training.py export                            checkpoint -> work/model/odia_ft.traineddata
    py content/_localocr/run_training.py eval                              character error of stock vs fine-tuned on the held-out lines
Everything lives under work/ (ignored by git). Needs the .lstmf files from prepare_training.py --lstmf.
"""
import argparse
import os
import re
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
WORK = HERE / "work"
BEST = WORK / "tessdata_best"
TRAIN = WORK / "train"
MODEL = WORK / "model"
TD = r"C:\Program Files\Tesseract-OCR"
sys.stdout.reconfigure(encoding="utf-8")


def run(cmd, log=None):
    print(" ".join(str(c) for c in cmd), flush=True)
    env = dict(os.environ, TESSDATA_PREFIX=str(BEST))
    with open(log, "a", encoding="utf-8") if log else open(os.devnull, "w") as fh:
        p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace", env=env)
        for line in p.stdout:
            fh.write(line)
            if re.search(r"At iteration|Finished|Error|error|Eval Char|BCER|Best|Fail|Can't", line):
                print(line.rstrip(), flush=True)
        return p.wait()


def train(a):
    MODEL.mkdir(parents=True, exist_ok=True)
    cmd = [f"{TD}\\lstmtraining.exe", "--model_output", str(MODEL / "ori_ft"), "--traineddata", str(BEST / "ori.traineddata"),
           "--train_listfile", str(TRAIN / "train.list"), "--eval_listfile", str(TRAIN / "eval.list"),
           "--max_iterations", str(a.iterations), "--learning_rate", str(a.lr), "--debug_interval", "0"]
    ckpt = MODEL / "ori_ft_checkpoint"
    cmd += ["--continue_from", str(ckpt)] if ckpt.exists() else ["--continue_from", str(BEST / "ori.lstm")]
    sys.exit(run(cmd, MODEL / "train.log"))


def export(a):
    out = MODEL / "odia_ft.traineddata"
    r = run([f"{TD}\\lstmtraining.exe", "--stop_training", "--continue_from", str(MODEL / "ori_ft_checkpoint"),
             "--traineddata", str(BEST / "ori.traineddata"), "--model_output", str(out)])
    print("->", out, "exists" if out.exists() else "MISSING")
    sys.exit(r)


def evaluate(a):
    lst = TRAIN / "eval.list"
    res = {}
    for name, model in (("stock ori", BEST / "ori.traineddata"), ("fine-tuned", MODEL / "odia_ft.traineddata")):
        if not model.exists():
            print(name, "missing:", model)
            continue
        out = subprocess.run([f"{TD}\\lstmeval.exe", "--model", str(model), "--traineddata", str(model), "--eval_listfile", str(lst)],
                             capture_output=True, text=True, encoding="utf-8", errors="replace", env=dict(os.environ, TESSDATA_PREFIX=str(BEST)))
        txt = out.stdout + out.stderr
        m = re.findall(r"Eval Char error rate=([\d.]+), Word error rate=([\d.]+)", txt)
        res[name] = m[-1] if m else None
        print(f"{name:12} {'character error %s%%, word error %s%%' % res[name] if res[name] else 'no result: ' + txt[-300:]}")
    return res


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sp = ap.add_subparsers(dest="cmd", required=True)
    t = sp.add_parser("train"); t.add_argument("--iterations", type=int, default=20000); t.add_argument("--lr", type=float, default=0.0001)
    sp.add_parser("export"); sp.add_parser("eval")
    a = ap.parse_args()
    {"train": train, "export": export, "eval": evaluate}[a.cmd](a)


if __name__ == "__main__":
    main()
