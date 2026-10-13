#!/usr/bin/env python3
"""Reading a line with two recognisers and a word list, deciding by likelihood instead of by a rule.

Candidates for a line: model A's reading, model B's reading, and for every word that is not in the word list, the nearby words
that are (one edit away). Each candidate is scored by how well it explains the IMAGE (the summed CTC log-likelihood under both
models) minus a penalty for every word not in the word list. The word list is a prior, never a verdict: a rare real word stays
if the image supports it strongly enough. Tuned on held-out lines (see tune()).

    from ensemble import Reader
    r = Reader(["crnn.pt", "crnn_b.pt"], lexicon_counter)
    r.read_lines([grey_array, ...])          -> [text, ...]
"""
import collections
import itertools
import re
import sys
from pathlib import Path

import numpy as np
import torch
import torch.nn.functional as F

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE.parent / "_odia"))
import crnn  # noqa: E402
import odia_text as T  # noqa: E402

PUNCT_EDGE = re.compile(r"^([\"'(\[]*)(.*?)([\"')\]!?;:,.।॥-]*)$")


def deletes(w):
    return {w[:i] + w[i + 1:] for i in range(len(w))}


class Lexicon:
    def __init__(self, counts):
        self.cnt = counts
        self.idx = collections.defaultdict(set)
        for w in counts:
            self.idx[w].add(w)
            for d in deletes(w):
                self.idx[d].add(w)

    def known(self, core):
        return core in self.cnt

    def neighbours(self, core, min_count=2):
        """Words one insertion, deletion or substitution away (SymSpell delete index)."""
        out = set(self.idx.get(core, ()))
        for d in deletes(core):
            out |= self.idx.get(d, set())
        return [w for w in out if w != core and abs(len(w) - len(core)) <= 1 and self.cnt[w] >= min_count]


def split_word(tok):
    m = PUNCT_EDGE.match(tok)
    return (m.group(1), m.group(2), m.group(3)) if m else ("", tok, "")


class Reader:
    def __init__(self, model_files, lexicon=None, penalty=3.0, max_variants=6):
        self.models = []
        for f in model_files:
            ck = torch.load(crnn.MODEL / f, map_location=crnn.DEV)
            m = crnn.CRNN(len(ck["chars"]) + 1).to(crnn.DEV)
            m.load_state_dict(ck["state"])
            m.eval()
            self.models.append((m, ck["chars"]))
        self.chars = self.models[0][1]
        assert all(c == self.chars for _, c in self.models), "models must share one character set"
        self.index = {c: i + 1 for i, c in enumerate(self.chars)}
        self.lex = lexicon
        self.penalty = penalty
        self.max_variants = max_variants

    def _logprobs(self, arrays):
        x, _ = crnn.batchify([crnn.prep_image(a) for a in arrays])
        out = []
        with torch.no_grad():
            for m, _ in self.models:
                out.append(F.log_softmax(m(x.to(crnn.DEV)).float(), -1))      # B, T, C
        return out, x.shape[-1] // 4

    def _ctc(self, lp, text):
        """Negative log-likelihood of `text` under one model's log-probs for one line (T, C)."""
        ids = [self.index[c] for c in text if c in self.index]
        if not ids:
            return 1e9
        y = torch.tensor(ids, dtype=torch.long)
        t = lp.shape[0]
        loss = F.ctc_loss(lp.unsqueeze(1), y.unsqueeze(0), torch.tensor([t]), torch.tensor([len(ids)]),
                          blank=0, reduction="sum", zero_infinity=True)
        return float(loss)

    def _prior(self, text):
        if self.lex is None:
            return 0.0
        n = 0
        for tok in text.split():
            core = split_word(tok)[1]
            if len(core) >= 3 and not T.is_marker(tok) and not self.lex.known(core):
                n += 1
        return self.penalty * n

    def _variants(self, text):
        """The text itself plus, for each unknown word, versions with one nearby known word swapped in."""
        if self.lex is None:
            return [text]
        toks = text.split()
        alts = []
        for i, tok in enumerate(toks):
            pre, core, post = split_word(tok)
            if len(core) >= 4 and not self.lex.known(core):
                for w in sorted(self.lex.neighbours(core), key=lambda w: -self.lex.cnt[w])[:3]:
                    alts.append((i, pre + w + post))
        alts = alts[: self.max_variants]
        out = [text]
        for i, new in alts:
            t = toks[:]
            t[i] = new
            out.append(" ".join(t))
        return out

    def read_lines(self, arrays, bs=32):
        res = []
        for s in range(0, len(arrays), bs):
            chunk = arrays[s:s + bs]
            lps, _ = self._logprobs(chunk)
            greedy = [crnn.decode(lp, self.chars) for lp in lps]
            for b in range(len(chunk)):
                cands = []
                for g in greedy:
                    cands += self._variants(g[b])
                cands = list(dict.fromkeys(cands))
                best, best_s = cands[0], -1e18
                for c in cands:
                    ll = -sum(self._ctc(lp[b], c) for lp in lps) / len(lps)
                    sc = ll - self._prior(c)
                    if sc > best_s:
                        best, best_s = c, sc
                res.append(best)
        return res
