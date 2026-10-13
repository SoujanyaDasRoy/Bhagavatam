#!/usr/bin/env python3
"""A small line recogniser (CNN + BiLSTM + CTC) trained on this book's own lines. Replaces the Tesseract fine-tune, whose Windows
trainer cannot load more than one training file.

    py content/_localocr/crnn.py train --epochs 30          trains on work/train/train, checks on work/train/eval, keeps the best
    py content/_localocr/crnn.py eval                       character and word error on all held-out lines
    py content/_localocr/crnn.py read IMG [IMG ...]         recognise line images
Model: work/model/crnn.pt (ignored by git). Labels are the .gt.txt files prepare_training.py wrote; characters outside the set are
impossible by construction (they were filtered out).
"""
import argparse
import glob
import json
import math
import random
import sys
import time
from pathlib import Path

import cv2
import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F

HERE = Path(__file__).resolve().parent
WORK = HERE / "work"
TRAIN = WORK / "train"
MODEL = WORK / "model"
H = 64
MAX_W = 1400
sys.stdout.reconfigure(encoding="utf-8")
DEV = torch.device("cuda" if torch.cuda.is_available() else "cpu")


# ---------------------------------------------------------------- data
def load_lines(sub):
    items = []
    for gt in sorted(glob.glob(str(TRAIN / sub / "*.gt.txt"))):
        p = Path(gt)
        img = p.with_name(p.name[:-7] + ".png")
        if img.exists():
            items.append((img, p.read_text(encoding="utf-8").strip()))
    return items


def prep_image(path_or_array):
    g = cv2.imread(str(path_or_array), cv2.IMREAD_GRAYSCALE) if not isinstance(path_or_array, np.ndarray) else path_or_array
    h, w = g.shape
    nw = min(MAX_W, max(32, int(round(w * H / h))))
    g = cv2.resize(g, (nw, H), interpolation=cv2.INTER_AREA)
    return g


def augment(g, rng):
    h, w = g.shape
    if rng.random() < 0.8:
        ang = rng.uniform(-1.2, 1.2)
        sc = rng.uniform(0.94, 1.06)
        sh = rng.uniform(-0.05, 0.05)
        M = np.array([[sc, sh, rng.uniform(-6, 6)], [0, sc, rng.uniform(-3, 3)]], dtype=np.float32)
        R = cv2.getRotationMatrix2D((w / 2, h / 2), ang, 1.0)
        g = cv2.warpAffine(g, M, (w, h), borderValue=255)
        g = cv2.warpAffine(g, R, (w, h), borderValue=255)
    if rng.random() < 0.5:
        k = rng.choice([3, 5])
        g = cv2.GaussianBlur(g, (k, k), 0)
    if rng.random() < 0.5:
        g = np.clip(g.astype(np.float32) * rng.uniform(0.8, 1.1) + rng.uniform(-20, 20), 0, 255).astype(np.uint8)
    if rng.random() < 0.3:
        g = np.clip(g.astype(np.float32) + np.random.normal(0, rng.uniform(2, 8), g.shape), 0, 255).astype(np.uint8)
    return g


def batchify(arrs):
    w = max(a.shape[1] for a in arrs)
    w = int(math.ceil(w / 8) * 8)
    x = np.full((len(arrs), 1, H, w), 255, dtype=np.uint8)
    for i, a in enumerate(arrs):
        x[i, 0, :, :a.shape[1]] = a
    t = torch.from_numpy(x).float().div_(255.0).sub_(0.5).div_(0.5)
    return t, torch.tensor([a.shape[1] // 4 for a in arrs])


# ---------------------------------------------------------------- model
def block(i, o, pool):
    layers = [nn.Conv2d(i, o, 3, padding=1, bias=False), nn.BatchNorm2d(o), nn.ReLU(inplace=True)]
    if pool:
        layers.append(nn.MaxPool2d(pool))
    return layers


class CRNN(nn.Module):
    def __init__(self, n_classes):
        super().__init__()
        self.cnn = nn.Sequential(
            *block(1, 32, (2, 2)), *block(32, 64, (2, 2)),
            *block(64, 128, None), *block(128, 128, (2, 1)),
            *block(128, 256, None), *block(256, 256, (2, 1)),
            *block(256, 256, None))
        self.rnn = nn.LSTM(256 * 4, 256, num_layers=2, bidirectional=True, batch_first=True, dropout=0.2)
        self.out = nn.Linear(512, n_classes)

    def forward(self, x):
        f = self.cnn(x)                                  # B, 256, H/16=4, W/4
        b, c, h, w = f.shape
        f = f.permute(0, 3, 1, 2).reshape(b, w, c * h)    # B, T, C*H
        y, _ = self.rnn(f)
        return self.out(y)                               # B, T, classes


# ---------------------------------------------------------------- text
def levenshtein(a, b):
    if a == b:
        return 0
    prev = list(range(len(b) + 1))
    for i, ca in enumerate(a, 1):
        cur = [i]
        for j, cb in enumerate(b, 1):
            cur.append(min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (ca != cb)))
        prev = cur
    return prev[-1]


def decode(logits, chars):
    best = logits.argmax(-1).cpu().numpy()
    out = []
    for row in best:
        s, last = [], 0
        for k in row:
            if k != last and k != 0:
                s.append(chars[k - 1])
            last = k
        out.append("".join(s))
    return out


def evaluate(model, items, chars, limit=None, bs=32):
    model.eval()
    sub = items[:limit] if limit else items
    ce = cn = we = wn = 0
    with torch.no_grad():
        for i in range(0, len(sub), bs):
            chunk = sub[i:i + bs]
            x, _ = batchify([prep_image(p) for p, _ in chunk])
            pred = decode(model(x.to(DEV)).float(), chars)
            for (_, t), p in zip(chunk, pred):
                ce += levenshtein(t, p); cn += len(t)
                tw, pw = t.split(), p.split()
                we += levenshtein(tw, pw); wn += len(tw)
    return ce / max(1, cn), we / max(1, wn)


# ---------------------------------------------------------------- commands
def cmd_train(a):
    MODEL.mkdir(parents=True, exist_ok=True)
    torch.manual_seed(a.seed); np.random.seed(a.seed)
    tr, ev = load_lines("train"), load_lines("eval")
    chars = sorted({c for _, t in tr + ev for c in t})
    idx = {c: i + 1 for i, c in enumerate(chars)}
    print(f"train {len(tr)}, eval {len(ev)}, {len(chars)} characters, device {DEV}", flush=True)
    imgs = [prep_image(p) for p, _ in tr]
    labels = [[idx[c] for c in t] for _, t in tr]
    model = CRNN(len(chars) + 1).to(DEV)
    print("parameters:", sum(p.numel() for p in model.parameters()), flush=True)
    opt = torch.optim.AdamW(model.parameters(), lr=1e-3, weight_decay=1e-4)
    steps_per_epoch = math.ceil(len(tr) / a.batch)
    sched = torch.optim.lr_scheduler.OneCycleLR(opt, max_lr=1.5e-3, total_steps=a.epochs * steps_per_epoch, pct_start=0.15)
    ctc = nn.CTCLoss(blank=0, zero_infinity=True)
    scaler = torch.amp.GradScaler("cuda", enabled=DEV.type == "cuda")
    rng = random.Random(a.seed)
    best = 9.9
    for ep in range(1, a.epochs + 1):
        model.train()
        order = list(range(len(tr)))
        rng.shuffle(order)
        # similar widths together keeps padding small
        chunks = [order[i:i + a.batch * 16] for i in range(0, len(order), a.batch * 16)]
        batches = []
        for ch in chunks:
            ch.sort(key=lambda k: imgs[k].shape[1])
            batches += [ch[i:i + a.batch] for i in range(0, len(ch), a.batch)]
        rng.shuffle(batches)
        t0, tot = time.time(), 0.0
        for bi in batches:
            x, tl = batchify([augment(imgs[k], rng) for k in bi])
            y = torch.tensor([c for k in bi for c in labels[k]], dtype=torch.long)
            yl = torch.tensor([len(labels[k]) for k in bi], dtype=torch.long)
            opt.zero_grad(set_to_none=True)
            with torch.autocast(device_type=DEV.type, dtype=torch.float16, enabled=DEV.type == "cuda"):
                lg = model(x.to(DEV))
            lp = F.log_softmax(lg.float(), -1).permute(1, 0, 2)
            loss = ctc(lp, y, torch.full((len(bi),), lp.shape[0], dtype=torch.long), yl)
            scaler.scale(loss).backward()
            scaler.unscale_(opt)
            nn.utils.clip_grad_norm_(model.parameters(), 5.0)
            scaler.step(opt); scaler.update(); sched.step()
            tot += loss.item()
        cer, wer = evaluate(model, ev, chars, limit=600)
        flag = ""
        if cer < best:
            best = cer
            torch.save({"state": model.state_dict(), "chars": chars}, MODEL / a.name)
            flag = "  saved"
        print(f"epoch {ep}/{a.epochs}  loss {tot / len(batches):.3f}  held-out(600 lines) char error {100 * cer:.2f}%  word error {100 * wer:.2f}%  {time.time() - t0:.0f}s{flag}", flush=True)


def load_model():
    ck = torch.load(MODEL / "crnn.pt", map_location=DEV)
    m = CRNN(len(ck["chars"]) + 1).to(DEV)
    m.load_state_dict(ck["state"])
    m.eval()
    return m, ck["chars"]


def cmd_eval(a):
    m, chars = load_model()
    ev = load_lines("eval")
    cer, wer = evaluate(m, ev, chars)
    print(f"held-out lines: {len(ev)}  character error {100 * cer:.2f}%  word error {100 * wer:.2f}%")


def read_images(m, chars, arrays, bs=32):
    out = []
    with torch.no_grad():
        for i in range(0, len(arrays), bs):
            x, _ = batchify([prep_image(g) for g in arrays[i:i + bs]])
            out += decode(m(x.to(DEV)).float(), chars)
    return out


def cmd_read(a):
    m, chars = load_model()
    for p in a.images:
        print(p, "|", read_images(m, chars, [cv2.imread(p, cv2.IMREAD_GRAYSCALE)])[0])


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sp = ap.add_subparsers(dest="cmd", required=True)
    t = sp.add_parser("train"); t.add_argument("--epochs", type=int, default=30); t.add_argument("--batch", type=int, default=24)
    t.add_argument("--seed", type=int, default=1); t.add_argument("--name", default="crnn.pt", help="checkpoint file in work/model")
    sp.add_parser("eval")
    r = sp.add_parser("read"); r.add_argument("images", nargs="+")
    a = ap.parse_args()
    {"train": cmd_train, "eval": cmd_eval, "read": cmd_read}[a.cmd](a)


if __name__ == "__main__":
    main()
