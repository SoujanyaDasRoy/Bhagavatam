#!/usr/bin/env python3
"""Accuracy control for the Odia transcription: two independent readings of every page, compared word by word.

Why: a single vision reading is wrong on roughly 1 word in 50 or worse and nobody can see which words. Two readings by
different models (or the same model at a different setting) only agree on a word when both read it the same way, so the
disagreements are exactly where the errors are. Tesseract (already run on every page) is the tie-breaker. What nobody can
agree on goes to a person, one word at a time, with the page image next to it. Finally a random audit of the words that
WERE agreed measures the error that is left, with a confidence interval.

    py odia_verify.py compare --skandha 5-12                 A=work_v/vision  B=work_v/vision_b  T=work_tess/vision -> work_v/consensus
    py odia_verify.py compare --a DIR --b DIR --t DIR --out DIR [--lexicon FILE]
    py odia_verify.py report  [--dir work_v/consensus]       -> work_v/verify_report.md, work_v/review_queue.csv
    py odia_verify.py lexicon                                -> work_v/lexicon.txt (words of the checked pages + Odia Wiktionary)
    py odia_verify.py audit-sample --n 200 --seed 7          -> work_v/audit/items.json (for odia_review.py)
    py odia_verify.py audit-score work_v/audit/results.json  -> error rate of the agreed words, 95% interval
    py odia_verify.py apply work_v/review/decisions.json     -> writes the person's answers into the consensus pages
    py odia_verify.py check-lines                            -> the page JSON files as the assembler will see them (hygiene only)

Environment: ODIA_VWORK (default work_v), ODIA_TESS (default work_tess/vision: tar xzf page_json.tgz -C work_tess).
Outputs hold book text: they live in work_v/ and are never committed.
"""
import argparse
import csv
import json
import math
import os
import random
import re
import sys
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import odia_text as T  # noqa: E402

sys.stdout.reconfigure(encoding="utf-8")
WORK = Path(os.environ.get("ODIA_VWORK", HERE / "work_v"))
TESS = Path(os.environ.get("ODIA_TESS", HERE / "work_tess" / "vision"))
PAGE_RE = re.compile(r"v(\d)_p(\d{4})\.json$")

# How much to trust a word, best first. "AB" = both independent readings agree.
CONF = ("AB", "AT", "BT", "lex", "dispute", "single")


# ---------------------------------------------------------------- the decision for one word
def decide(a, b, t, lex=None):
    """Choose between three readings of one word position (None = that reader had no word here).

    Returns (chosen_core_or_None, confidence, needs_review). Cores are compared, never punctuation.
    2 of 3 wins; a lone disagreement is settled by the lexicon only when exactly one candidate is a known word, and is
    still sent to review: a known word can be the wrong known word."""
    ca, cb, ct = (T.core(x) if x is not None else None for x in (a, b, t))
    if ca is not None and ca == cb:
        return a, "AB", False
    if ca is not None and ca == ct:
        return a, "AT", False
    if cb is not None and cb == ct:
        return b, "BT", False
    cands = [x for x in (a, b) if x is not None]
    if not cands:
        return (t, "single", True) if t is not None else (None, "single", False)
    if len(cands) == 1:
        return cands[0], "single", True
    if lex is not None:
        known = [x for x in cands if T.core(x) in lex]
        if len(known) == 1:
            return known[0], "lex", True
    return a, "dispute", True


def combine_page(a_paras, b_paras, t_paras, lex=None):
    """Word-level consensus of one page. Reading A is the skeleton; B and T are aligned onto it.

    Returns (paragraphs, flags, stats). flags: dicts with para, idx, a, b, t, chosen, why."""
    a_tok = [[tok for tok in T.tokens(p)] for p in a_paras]
    flat_a = [(pi, ti) for pi, toks in enumerate(a_tok) for ti in range(len(toks))]
    a_list = [a_tok[pi][ti] for pi, ti in flat_a]
    b_list = [w for p in b_paras for w in T.tokens(p)] if b_paras is not None else None
    t_list = [w for p in t_paras for w in T.tokens(p)] if t_paras is not None else None
    amap_b, amap_t = {}, {}
    inserted_b, inserted_t = [], []
    if b_list is not None:
        for i, j in T.align(a_list, b_list):
            if i is not None and j is not None:
                amap_b[i] = j
            elif i is None:
                inserted_b.append(j)
    if t_list is not None:
        for i, j in T.align(a_list, t_list):
            if i is not None and j is not None:
                amap_t[i] = j
            elif i is None:
                inserted_t.append(j)

    out = [list(toks) for toks in a_tok]
    flags, st = [], Counter()
    for n, (pi, ti) in enumerate(flat_a):
        a = a_list[n]
        if T.is_marker(a):                       # verse marker: kept as read by A, checked by the structural checks
            st["marker"] += 1
            continue
        b = b_list[amap_b[n]] if (b_list is not None and n in amap_b) else None
        t = t_list[amap_t[n]] if (t_list is not None and n in amap_t) else None
        if b_list is None:
            b = a                                   # no second reading: A agrees with itself, so only T and hygiene speak
        chosen, conf, review = decide(a, b, t, lex)
        issues = T.severe(chosen) if chosen else []
        if b_list is None and conf == "AB":
            conf = "single"
            review = review or bool(issues)
        st["words"] += 1
        st[conf] += 1
        if issues:
            st["hygiene"] += 1
            review = True
        if review:
            why = ("hygiene:" + ",".join(issues)) if issues and conf in ("AB", "AT", "BT") else conf
            flags.append({"para": pi, "idx": ti, "a": a, "b": b if b_list is not None else None, "t": t,
                          "chosen": chosen, "why": why})
            if chosen and not chosen.endswith("[?]"):
                chosen = chosen + "[?]"
        if chosen is not None:
            out[pi][ti] = chosen
    # words one reading has and A does not: a possible omission by A
    for j in inserted_b:
        w = b_list[j]
        if not T.is_marker(w):
            flags.append({"para": None, "idx": None, "a": None, "b": w, "t": None, "chosen": None, "why": "omission?"})
            st["omission"] += 1
    return [" ".join(toks) for toks in out], flags, st


def page_meta_mismatches(a, b):
    """Structure fields that two readings of the same page must agree on."""
    out = []
    for k in ("running_head_chapter", "chapter_number", "paragraphs_before_chapter_heading"):
        if a.get(k) != b.get(k):
            out.append(f"{k}: {a.get(k)!r} vs {b.get(k)!r}")
    if sorted(a.get("sanskrit_verse_numbers") or []) != sorted(b.get("sanskrit_verse_numbers") or []):
        out.append(f"sanskrit_verse_numbers: {a.get('sanskrit_verse_numbers')} vs {b.get('sanskrit_verse_numbers')}")
    if bool(a.get("colophon")) != bool(b.get("colophon")):
        out.append("colophon present in only one reading")
    na, nb = len(a.get("paragraphs") or []), len(b.get("paragraphs") or [])
    if na != nb:
        out.append(f"paragraph count: {na} vs {nb}")
    return out


def page_score(st, flags, meta):
    """0..1, 1 = nothing to look at. Used to order the review queue, not to accept or reject a page."""
    w = max(1, st["words"])
    risk = (st["dispute"] + st["single"] + st["hygiene"]) / w + 0.5 * st["lex"] / w + 0.02 * st["omission"] + 0.05 * len(meta)
    return round(max(0.0, 1.0 - 4 * risk), 3)


# ---------------------------------------------------------------- lexicon
def load_lexicon(path=None):
    p = Path(path) if path else WORK / "lexicon.txt"
    if not p.exists():
        return None
    return {w for w in p.read_text(encoding="utf-8").split("\n") if w}


def cmd_lexicon(a):
    """Words of pages that are already trusted (Skandha 1 to 4 vision track) plus the Odia Wiktionary forms."""
    words = Counter()
    src = Path(a.trusted) if a.trusted else HERE / "vision_transcripts"
    for f in sorted(src.glob("v*_p*.json")):
        d = json.loads(f.read_text(encoding="utf-8"))
        for p in d.get("paragraphs", []):
            for w in T.tokens(p):
                c = T.core(w)
                if c and not T.is_marker(w) and not T.severe(w):
                    words[c] += 1
    n_book = len(words)
    db = HERE.parent.parent / "app" / "src" / "main" / "assets" / "dictionary.db"
    n_dict = 0
    if db.exists():
        import sqlite3
        con = sqlite3.connect(str(db))
        for (h,) in con.execute("select headword from entry where lang='or'"):
            c = T.core(T.norm(h))
            if c and c not in words:
                words[c] += 0
                n_dict += 1
        try:
            for (f,) in con.execute("select f.form_norm from form f join entry e on e.id=f.entry_id where e.lang='or'"):
                c = T.core(T.norm(f))
                if c and c not in words:
                    words[c] += 0
                    n_dict += 1
        except Exception:  # noqa: the form table layout is not part of this tool's contract
            pass
        con.close()
    out = WORK / "lexicon.txt"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("\n".join(sorted(words)) + "\n", encoding="utf-8")
    print(f"lexicon: {len(words):,} words ({n_book:,} from checked pages, {n_dict:,} added from the dictionary) -> {out}")


# ---------------------------------------------------------------- compare
def page_ids(d):
    out = {}
    for f in Path(d).glob("v*_p*.json"):
        m = PAGE_RE.search(f.name)
        if m:
            out[(int(m.group(1)), int(m.group(2)))] = f
    return out


def cmd_compare(a):
    dir_a = Path(a.a) if a.a else WORK / "vision"
    dir_b = Path(a.b) if a.b else WORK / "vision_b"
    dir_t = Path(a.t) if a.t else TESS
    out_dir = Path(a.out) if a.out else WORK / "consensus"
    pa, pb, pt = page_ids(dir_a), page_ids(dir_b), page_ids(dir_t)
    want = set(pa)
    if a.skandha:
        sys.path.insert(0, str(HERE))
        import odia_vision as ov
        spans = ov.skandha_spans(a.skandha)
        want = {(v, p) for (v, p) in pa if any(vv == v and x <= p <= y for vv, x, y in spans)}
    lex = load_lexicon(a.lexicon)
    print(f"A={dir_a} ({len(pa)} pages)  B={dir_b} ({len(pb)})  T={dir_t} ({len(pt)})  lexicon={'yes, %d words' % len(lex) if lex else 'no'}")
    out_dir.mkdir(parents=True, exist_ok=True)
    n = 0
    tot = Counter()
    for key in sorted(want):
        da = json.loads(pa[key].read_text(encoding="utf-8"))
        db_ = json.loads(pb[key].read_text(encoding="utf-8")) if key in pb else None
        dt = json.loads(pt[key].read_text(encoding="utf-8")) if key in pt else None
        paras, flags, st = combine_page(da.get("paragraphs", []), db_["paragraphs"] if db_ else None,
                                        dt["paragraphs"] if dt else None, lex)
        meta = page_meta_mismatches(da, db_) if db_ else []
        res = dict(da)
        res["paragraphs"] = paras
        res["_verify"] = {"readings": "A" + ("B" if db_ else "") + ("T" if dt else ""), "stats": dict(st), "flags": flags,
                          "meta_mismatch": meta, "score": page_score(st, flags, meta)}
        (out_dir / pa[key].name).write_text(json.dumps(res, ensure_ascii=False, indent=1), encoding="utf-8")
        tot.update(st)
        n += 1
    print(f"{n} pages -> {out_dir}")
    print_totals(tot)


def print_totals(tot):
    w = max(1, tot["words"])
    print(f"words {tot['words']:,}: agreed by two readings {100 * tot['AB'] / w:.1f}%, settled 2 of 3 {100 * (tot['AT'] + tot['BT']) / w:.1f}%, "
          f"lexicon-settled {100 * tot['lex'] / w:.1f}%, disputed {100 * tot['dispute'] / w:.1f}%, one reading only {100 * tot['single'] / w:.1f}%, "
          f"hygiene {100 * tot['hygiene'] / w:.2f}%, possible omissions {tot['omission']}")


# ---------------------------------------------------------------- report and queue
def cmd_report(a):
    d = Path(a.dir) if a.dir else WORK / "consensus"
    pages = []
    for key, f in sorted(page_ids(d).items()):
        j = json.loads(f.read_text(encoding="utf-8"))
        pages.append((key, j.get("_verify", {}), j))
    if not pages:
        sys.exit(f"no pages in {d}")
    queue = []
    per_chapter = {}
    for (v, p), vf, j in pages:
        for fl in vf.get("flags", []):
            if fl.get("resolved"):
                continue
            queue.append({"vol": v, "page": p, "para": fl["para"], "idx": fl["idx"], "why": fl["why"], "a": fl["a"], "b": fl["b"],
                          "t": fl["t"], "chosen": fl["chosen"], "page_score": vf.get("score")})
        for m in vf.get("meta_mismatch", []):
            queue.append({"vol": v, "page": p, "para": None, "idx": None, "why": "page:" + m, "a": "", "b": "", "t": "", "chosen": "",
                          "page_score": vf.get("score")})
    priority = {"hygiene": 0, "dispute": 1, "omission?": 2, "single": 3, "lex": 4}
    queue.sort(key=lambda r: (priority.get(str(r["why"]).split(":")[0], 9), r["page_score"] if r["page_score"] is not None else 1, r["vol"], r["page"]))
    out_csv = WORK / "review_queue.csv"
    with open(out_csv, "w", encoding="utf-8", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=list(queue[0].keys()) if queue else ["vol", "page"])
        w.writeheader()
        w.writerows(queue)
    tot = Counter()
    for _, vf, _ in pages:
        tot.update(vf.get("stats", {}))
    worst = sorted(pages, key=lambda x: x[1].get("score", 1))[:15]
    lines = ["# Odia verification report", "", f"Pages: {len(pages)}   source: `{d}`", "",
             "| measure | value |", "|---|---|"]
    w_ = max(1, tot["words"])
    for label, key in (("words", "words"), ("agreed by both readings (AB)", "AB"), ("settled by Tesseract (AT or BT)", None),
                       ("settled by lexicon only (needs review)", "lex"), ("disputed (needs review)", "dispute"),
                       ("one reading only (needs review)", "single"), ("hygiene problems (needs review)", "hygiene"),
                       ("possible omissions", "omission")):
        val = tot["AT"] + tot["BT"] if key is None else tot[key]
        lines.append(f"| {label} | {val:,} ({100 * val / w_:.2f}%) |" if key != "words" else f"| words | {val:,} |")
    lines += ["", f"Review queue: {len(queue):,} items in `{out_csv.name}` (hygiene, disputes, omissions first).", "",
              "## Pages to look at first (lowest score)", "", "| page | score | disputes | single | hygiene | meta mismatches |", "|---|---|---|---|---|---|"]
    for (v, p), vf, _ in worst:
        s = vf.get("stats", {})
        lines.append(f"| v{v}:{p} | {vf.get('score')} | {s.get('dispute', 0)} | {s.get('single', 0)} | {s.get('hygiene', 0)} | {len(vf.get('meta_mismatch', []))} |")
    lines += ["", "A page with score 1.0 has nothing flagged: it is still only as good as two readings that agree. The error that is",
              "left in agreed words is measured by `audit-sample` and `audit-score`, not assumed.", ""]
    (WORK / "verify_report.md").write_text("\n".join(lines), encoding="utf-8")
    print("\n".join(lines[:22]))
    print(f"-> {WORK / 'verify_report.md'}, {out_csv}")


# ---------------------------------------------------------------- audit of the agreed words
def wilson(k, n, z=1.96):
    if n == 0:
        return 0.0, 1.0
    p = k / n
    d = 1 + z * z / n
    c = p + z * z / (2 * n)
    r = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n))
    return max(0.0, (c - r) / d), min(1.0, (c + r) / d)


def cmd_audit_sample(a):
    d = Path(a.dir) if a.dir else WORK / "consensus"
    rng = random.Random(a.seed)
    pool = []
    for (v, p), f in sorted(page_ids(d).items()):
        j = json.loads(f.read_text(encoding="utf-8"))
        vf = j.get("_verify", {})
        flagged = {(x["para"], x["idx"]) for x in vf.get("flags", [])}
        if a.stratum == "arbitrated":
            for pi, ti in vf.get("arbitrated", []):
                toks = j["paragraphs"][pi].split(" ")
                if ti < len(toks) and T.core(toks[ti]):
                    pool.append((v, p, pi, ti, toks[ti]))
            continue
        for pi, para in enumerate(j.get("paragraphs", [])):
            for ti, tok in enumerate(para.split(" ")):
                c = T.core(tok)
                if c and not T.is_marker(tok) and (pi, ti) not in flagged and len(c) >= 2:
                    pool.append((v, p, pi, ti, tok))
    if not pool:
        sys.exit("no agreed words to sample")
    items = rng.sample(pool, min(a.n, len(pool)))
    items.sort()
    out = WORK / "audit" / ("items.json" if a.stratum == "agreed" else "items_arbitrated.json")
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps([{"vol": v, "page": p, "para": pi, "idx": ti, "word": w} for v, p, pi, ti, w in items], ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(items)} of {len(pool):,} agreed words (seed {a.seed}) -> {out}. Open it with: py odia_review.py audit")


def cmd_audit_score(a):
    res = json.loads(Path(a.results).read_text(encoding="utf-8"))
    judged = [r for r in res if r.get("ok") in (True, False)]
    wrong = sum(1 for r in judged if r["ok"] is False)
    n = len(judged)
    lo, hi = wilson(wrong, n)
    print(f"audited {n} agreed words: {wrong} wrong = {100 * wrong / max(1, n):.2f}% (95% interval {100 * lo:.2f}% to {100 * hi:.2f}%)")
    print(f"=> estimated word accuracy of the agreed words: {100 * (1 - wrong / max(1, n)):.2f}% (at least {100 * (1 - hi):.2f}% with 95% confidence)")
    if n < 150:
        print("note: fewer than 150 words audited; the interval is wide. Audit more before relying on the number.")


# ---------------------------------------------------------------- apply a person's decisions
def cmd_apply(a):
    dec = json.loads(Path(a.decisions).read_text(encoding="utf-8"))
    d = Path(a.dir) if a.dir else WORK / "consensus"
    by_page = {}
    for r in dec:
        by_page.setdefault((int(r["vol"]), int(r["page"])), []).append(r)
    changed = 0
    for (v, p), rows in by_page.items():
        f = d / f"v{v}_p{p:04d}.json"
        if not f.exists():
            print("missing", f.name)
            continue
        j = json.loads(f.read_text(encoding="utf-8"))
        paras = [x.split(" ") for x in j["paragraphs"]]
        settled = set()
        for r in rows:
            if r.get("para") is None or r.get("text") is None:
                continue
            pi, ti = int(r["para"]), int(r["idx"])
            if pi < len(paras) and ti < len(paras[pi]):
                settled.add((pi, ti))
                old = re.sub(r"\[\?+\]$", "", paras[pi][ti])      # the [?] mark goes; real punctuation after the word stays
                trail = re.search(r"[“”‘’\"'()\]!?;:,.\-–—~|।॥*#]+$", old)
                paras[pi][ti] = r["text"] + (trail.group(0) if trail and not r["text"].endswith(trail.group(0)) else "")
                changed += 1
        j["paragraphs"] = [" ".join(x) for x in paras]
        vf = j.setdefault("_verify", {})
        for fl in vf.get("flags", []):
            if (fl.get("para"), fl.get("idx")) in settled:
                fl["resolved"] = a.source
        key = "arbitrated" if a.source == "model" else "human_decisions"
        vf[key] = sorted(set(map(tuple, vf.get(key, []))) | settled) if a.source == "model" else vf.get(key, 0) + len(settled)
        f.write_text(json.dumps(j, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{changed} word(s) changed on {len(by_page)} page(s)")


# ---------------------------------------------------------------- one-reading hygiene check
def cmd_check_lines(a):
    d = Path(a.dir) if a.dir else WORK / "vision"
    tot, bad = Counter(), []
    for (v, p), f in sorted(page_ids(d).items()):
        j = json.loads(f.read_text(encoding="utf-8"))
        n = 0
        for para in j.get("paragraphs", []):
            for tok in T.tokens(para):
                tot["words"] += 1
                iss = T.hygiene(tok)
                for i in iss:
                    tot[i] += 1
                n += bool(iss)
        if n:
            bad.append((n, v, p))
    print(dict(tot))
    for n, v, p in sorted(bad, reverse=True)[:15]:
        print(f"  v{v}:{p}: {n} word(s) with issues")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sp = ap.add_subparsers(dest="cmd", required=True)
    c = sp.add_parser("compare"); c.add_argument("--a"); c.add_argument("--b"); c.add_argument("--t"); c.add_argument("--out")
    c.add_argument("--skandha"); c.add_argument("--lexicon")
    r = sp.add_parser("report"); r.add_argument("--dir")
    l = sp.add_parser("lexicon"); l.add_argument("--trusted")
    s = sp.add_parser("audit-sample"); s.add_argument("--n", type=int, default=200); s.add_argument("--seed", type=int, default=7); s.add_argument("--dir")
    s.add_argument("--stratum", choices=["agreed", "arbitrated"], default="agreed", help="agreed = words both readings gave; arbitrated = words a model settled")
    sc = sp.add_parser("audit-score"); sc.add_argument("results")
    ap_ = sp.add_parser("apply"); ap_.add_argument("decisions"); ap_.add_argument("--dir")
    ap_.add_argument("--source", choices=["human", "model"], default="human", help="who made these decisions")
    ck = sp.add_parser("check-lines"); ck.add_argument("--dir")
    a = ap.parse_args()
    {"compare": cmd_compare, "report": cmd_report, "lexicon": cmd_lexicon, "audit-sample": cmd_audit_sample,
     "audit-score": cmd_audit_score, "apply": cmd_apply, "check-lines": cmd_check_lines}[a.cmd](a)


if __name__ == "__main__":
    main()
