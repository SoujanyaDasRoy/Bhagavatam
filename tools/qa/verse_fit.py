#!/usr/bin/env python3
"""Does each Bengali/Odia verse say what the Hindi of the same verse says? Finds verses whose text belongs to a neighbour.

    py tools/qa/verse_fit.py [or|bn]           (default or)  -> docs/audits/VERSE_FIT_<lang>.md
    py tools/qa/verse_fit.py bn --fix          also propose, for each flagged chapter, where every block belongs (in order, at most
                                               three verses away, a block may join the one before it) and write the proposals to
                                               content/_build/verse_moves.proposed.json with a side-by-side sheet for review
    py tools/qa/verse_fit.py bn --apply        apply the REVIEWED content/_build/verse_moves.json to both content.db copies
                                               (add_odia.py / add_bengali.py apply it on every rebuild too)

Score of a translated verse against a Hindi verse = share of its words that the Hindi verse uses: the word itself in the other
script (tatsama words), or the Hindi word it is aligned to in content/_dict/work/align_<lang>_hi.pickle (book_gloss.py).
A verse is listed when the Hindi of a neighbouring verse (n-1 or n+1) fits clearly better than its own (the text was put
one slot off during assembly). Grouped verses (one block for several shlokas, *_from pointing at it, or Hindi printed with
another verse, hi_from) are judged against the Hindi of the whole group.
"""
import collections
import json
import pickle
import re
import sqlite3
import sys
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
sys.stdout.reconfigure(encoding="utf-8")
LANG = next((a for a in sys.argv[1:] if not a.startswith("--")), "or")
PAT = {"or": r"[଀-୿]+", "bn": r"[ঀ-৿]+"}[LANG]
OFF = {"or": 0x200, "bn": 0x80}[LANG]
DBS = (ROOT / "content/content.db", ROOT / "app/src/main/assets/content.db")
REACH = 3          # a block may move at most this many verses
JOIN_COST = 0.05   # small cost for joining a block to the one before it


def words(t, pat):
    t = unicodedata.normalize("NFC", (t or "").replace("‌", "").replace("‍", ""))
    return [w for w in re.findall(pat, t) if len(w) >= 2]


def deva(w):
    return "".join(chr(ord(ch) - OFF) if 0x0980 <= ord(ch) <= 0x0B7F else ch for ch in w).replace("व", "ब")


def main():
    if "--apply" in sys.argv:
        sys.path.insert(0, str(ROOT / "content/_build"))
        from verse_moves import apply_moves
        for p in DBS:
            d = sqlite3.connect(p)
            print(f"{p}: {apply_moves(d, LANG)} verses given their text")
            d.commit()
        return

    table, _ = pickle.loads((ROOT / f"content/_dict/work/align_{LANG}_hi.pickle").read_bytes())
    link = {w: {g for g, _ in c.most_common(3)} for w, c in table.items()}
    c = sqlite3.connect(DBS[0])
    col, frm = ('"or"', "or_from") if LANG == "or" else ("bn", "bn_from")
    rows = c.execute(f"select skandha, chapter, num, num_end, hi, {col}, {frm}, hi_from, en from verse order by skandha, chapter, num").fetchall()
    by_ch = collections.defaultdict(list)
    for r in rows:
        by_ch[(r[0], r[1])].append(r)

    def hindi_set(text):
        hs = words(text, r"[ऀ-ॿ]+")
        return set(hs), {h[:4].replace("व", "ब") for h in hs}

    def fit(tw, hset):
        hw, hk = hset
        if not tw:
            return 0.0
        return sum(1 for w in tw if (link.get(w, set()) & hw) or deva(w)[:4] in hk) / len(tw)

    def text_words(t):
        return words(re.sub(r"\S*\[\?\]", " ", t or ""), PAT)

    hindi_of = {}
    for key, vs in by_ch.items():
        # Hindi of each verse: its own, or the Hindi it is printed with (hi_from); a block that others point to is judged
        # against the Hindi of the whole group
        own_hi = {v[2]: v[4] or "" for v in vs}
        group_hi = {v[2]: own_hi[v[2]] or own_hi.get(v[7] or -1, "") for v in vs}
        for v in vs:
            if v[6] and v[6] in group_hi:
                group_hi[v[6]] += " " + (v[4] or own_hi.get(v[7] or -1, ""))
        hindi_of[key] = {k: hindi_set(t) for k, t in group_hi.items()}

    shifted, scores = [], []
    for (s, ch), vs in by_ch.items():
        H = hindi_of[(s, ch)]
        for i, v in enumerate(vs):
            tw = text_words(v[5])
            if len(tw) < 6:
                continue
            own = fit(tw, H[v[2]])
            best = max(((fit(tw, H[vs[j][2]]), vs[j][2]) for j in (i - 1, i + 1) if 0 <= j < len(vs)), default=(0.0, None))
            scores.append(own)
            if best[0] >= 0.45 and best[0] - own >= 0.2:
                shifted.append((f"{s}.{ch}.{v[2]}", own, best[0], best[1]))

    out = [f"# Verse fit, {LANG} ({len(scores)} verses of 6+ words checked, median fit {sorted(scores)[len(scores) // 2]:.2f})", "",
           "Made by tools/qa/verse_fit.py: share of a verse's words that the Hindi of the same verse uses, against its neighbours.", "",
           f"## Text that belongs to a neighbouring verse ({len(shifted)})"]
    out += [f"- {r}: own {o:.2f}, verse {n} {b:.2f}" for r, o, b, n in shifted]

    if "--fix" in sys.argv:
        proposals, sheet = {}, [f"# Proposed verse moves, {LANG}", "",
                                "For review: each line is the block's opening words, the verse it is on now and the verse proposed,",
                                "with the English and Hindi of that verse. Copy the chapters that are right into content/_build/verse_moves.json.", ""]
        for key in sorted({tuple(map(int, r.split(".")[:2])) for r, *_ in shifted}):
            vs, H = by_ch[key], hindi_of[key]
            slots = [v[2] for v in vs]
            texts = [(slots.index(v[2]), v[5]) for v in vs if (v[5] or "").strip()]
            tw = [text_words(t) for _, t in texts]
            m, n = len(texts), len(slots)
            NEG = -1e9

            # only the stretch around the flagged verses may change; every other block stays where it is
            flagged = [int(r.split(".")[2]) for r, *_ in shifted if tuple(map(int, r.split(".")[:2])) == key]
            # the stretch grows from each flagged verse while the next verse is empty, joined, or also fits a neighbour better
            pos = {v[2]: k for k, v in enumerate(vs)}

            def leans(k):
                v = vs[k]
                tw0 = text_words(v[5])
                if len(tw0) < 6:
                    return True
                own = fit(tw0, H[v[2]])
                return any(fit(tw0, H[vs[j][2]]) > own + 0.05 for j in (k - 1, k + 1) if 0 <= j < len(vs))

            region = set()
            for f0 in flagged:
                k = pos[f0]
                lo = hi_ = k
                while lo > 0 and leans(lo - 1):
                    lo -= 1
                while hi_ < len(vs) - 1 and leans(hi_ + 1):
                    hi_ += 1
                region |= {vs[x][2] for x in range(max(0, lo - 1), min(len(vs), hi_ + 2))}

            def f(i, j):
                cur = texts[i][0]
                if slots[cur] not in region:
                    return fit(tw[i], H[slots[j]]) if j == cur else NEG
                return fit(tw[i], H[slots[j]]) if abs(j - cur) <= REACH and slots[j] in region else NEG

            # dp[i][j]: best total with blocks 0..i placed and block i on slot j; a block goes to a later slot than the one
            # before it, or joins it on the same slot
            dp = [[NEG] * n for _ in range(m)]
            back = [[None] * n for _ in range(m)]
            for j in range(n):
                dp[0][j] = f(0, j)
            for i in range(1, m):
                best_before, arg = NEG, None
                for j in range(n):
                    join = dp[i - 1][j] + f(i, j) - JOIN_COST
                    step = best_before + f(i, j)
                    dp[i][j], back[i][j] = (join, j) if join > step else (step, arg)
                    if dp[i - 1][j] > best_before:
                        best_before, arg = dp[i - 1][j], j
            j = max(range(n), key=lambda x: dp[m - 1][x])
            place = [0] * m
            for i in range(m - 1, -1, -1):
                place[i] = j
                if i:
                    j = back[i][j]
            now = sum(fit(tw[i], H[slots[texts[i][0]]]) for i in range(m))
            new = sum(fit(tw[i], H[slots[place[i]]]) for i in range(m))
            if new - now < 0.4 or all(place[i] == texts[i][0] for i in range(m)):
                continue
            plan = collections.defaultdict(list)
            for i in range(m):
                plan[slots[place[i]]].append(slots[texts[i][0]])
            # keep only verses whose contents change
            changed = {k: v for k, v in plan.items() if v != [k]}
            emptied = {slots[texts[i][0]] for i in range(m)} - set(plan)
            if not changed and not emptied:
                continue
            ck = f"{key[0]}.{key[1]}"
            proposals[ck] = {str(k): v for k, v in sorted(changed.items())}
            sheet.append(f"## {ck}  (fit {now:.1f} -> {new:.1f}; emptied: {sorted(emptied) or 'none'})")
            for k, src in sorted(changed.items()):
                row = next(v for v in vs if v[2] == k)
                for a in src:
                    t = next(v for v in vs if v[2] == a)[5] or ""
                    sheet.append(f"- {a} -> {k}: {t.replace(chr(10), ' ')[:90]}")
                sheet.append(f"    EN {k}: {(row[8] or '')[:90]}")
                sheet.append(f"    HI {k}: {(row[4] or '')[:70]}")
            sheet.append("")
        pf = ROOT / "content/_build/verse_moves.proposed.json"
        allp = json.loads(pf.read_text(encoding="utf-8")) if pf.exists() else {}
        allp[LANG] = proposals
        pf.write_text(json.dumps(allp, ensure_ascii=False, indent=1), encoding="utf-8")
        sp = ROOT / f"content/_build/verse_moves_review_{LANG}.md"
        sp.write_text("\n".join(sheet) + "\n", encoding="utf-8")
        print(f"{LANG}: proposals for {len(proposals)} chapters -> {pf}, review sheet {sp}")

    p = ROOT / f"docs/audits/VERSE_FIT_{LANG}.md"
    p.write_text("\n".join(out) + "\n", encoding="utf-8")
    print(f"{LANG}: checked {len(scores)}; shifted {len(shifted)} -> {p}")


if __name__ == "__main__":
    main()
