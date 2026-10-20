#!/usr/bin/env python3
"""Hand-written meanings for the words no source could explain (the work queue, batch by batch).

    py content/_dict/glossing.py status
    py content/_dict/glossing.py next [--size 400] [--merge work/ans_NNN.txt]
        answer lines: N <TAB> first two letters of the word <TAB> meaning   (a wrong pair is refused and asked again)
    py content/_dict/glossing.py load [path/to/dictionary.db]

The queue is the words in content/_dict/work/no_gloss_{sa,hi,bn,or,en}.tsv (written by book_gloss.py) that are not yet
in content/_dict/curated/glossed/*.tsv, most frequent first. Words that differ only by script or by the b/v, y/j letters
of the Bengali and Odia spelling form one group; one meaning is written for the group's most frequent word and goes to all.

  next      prints the next batch as "N<TAB>word" and saves it to work/batch_NNN.txt
            (--merge reads the answers of the batch before: lines "N<TAB>meaning", saves them to curated/glossed/NNN.tsv)
  load      adds every meaning in curated/glossed/*.tsv to dictionary.db as entries of the 'curated' source (CC0), for every
            word of the group in its own language, then bumps nothing else: run add_book_words.py and book_gloss.py afterwards

A meaning of "?" says the word is a misprint of the page reading that nothing can be made of; it is listed in curated/glossed/misread.txt
and counted apart. Meanings are short English phrases. They are written for this app and are not reviewed by a native reader.
"""
import collections
import re
import sqlite3
import sys
import unicodedata
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
sys.path.insert(0, str(HERE))
sys.stdout.reconfigure(encoding="utf-8")
import coverage_real as cr  # noqa: E402

WORK = HERE / "work"
OUT = HERE / "curated" / "glossed"
LANGS = ("sa", "hi", "bn", "or", "en")
MATRA = re.compile(r"[ा-्ँ-ःা-্ঁ-ঃା-୍ଁ-ଃ]")


def fold(w):
    """One key for the spellings of a word across scripts: Devanagari, with b=v, the nukta letters and ya-variants merged."""
    k = unicodedata.normalize("NFC", cr.to_deva(w)).lower()
    for a, b in (("ब", "व"), ("ड़", "ड"), ("ढ़", "ढ"), ("य़", "य"), ("ँ", "ं"), ("ः", ""), ("ऽ", "")):
        k = k.replace(a, b)
    return k


def broken(w):
    """A string no reader would write: starts with a sign, two vowel signs in a row, a virama before a vowel sign."""
    if not cr.INDIC.search(w):
        return len(w) < 3 or not re.search(r"[aeiouāīū]", w)
    if MATRA.match(w[0]):
        return True
    return bool(re.search(r"[ा-ौা-ৌା-ୌ]{2}|[्্୍][ा-ौা-ৌା-ୌ]", w))


def done():
    """fold key -> meaning for everything already written (including misread, as '?')."""
    got = {}
    for f in sorted(OUT.glob("[0-9]*.tsv")):
        for line in f.read_text(encoding="utf-8").splitlines():
            p = line.split("\t")
            if len(p) >= 3:
                got[fold(p[1])] = p[2]
    return got


def queue():
    have = done()
    groups = collections.OrderedDict()
    for L in LANGS:
        p = WORK / f"no_gloss_{L}.tsv"
        if not p.exists():
            continue
        for line in p.read_text(encoding="utf-8").splitlines():
            if not line:
                continue
            w, n = line.split("\t")
            k = fold(w) if L != "en" else w
            g = groups.setdefault(k, {"n": 0, "rep": (0, w, L), "members": []})
            g["n"] += int(n)
            g["members"].append((L, w))
            if int(n) > g["rep"][0]:
                g["rep"] = (int(n), w, L)
    todo = [(k, g) for k, g in groups.items() if k not in have]
    todo.sort(key=lambda kg: (-kg[1]["n"], kg[0]))
    return todo, have, groups


def next_batch_no():
    nums = [int(f.stem) for f in OUT.glob("[0-9]*.tsv")] + [int(f.stem.split("_")[1]) for f in WORK.glob("batch_*.txt")]
    return max(nums, default=0) + 1


def cmd_next(size, merge):
    OUT.mkdir(parents=True, exist_ok=True)
    if merge:
        prev = int(re.search(r"(\d+)", Path(merge).stem).group(1))
        batch = {}
        for line in (WORK / f"batch_{prev:03d}.txt").read_text(encoding="utf-8").splitlines():
            n, w = line.split("\t")
            batch[n] = w
        rows, bad = [], []
        for line in Path(merge).read_text(encoding="utf-8").splitlines():
            parts = line.split("	")
            if len(parts) >= 3:      # N <TAB> first two letters of the word <TAB> meaning
                n, key, gloss = parts[0], parts[1], "	".join(parts[2:])
            elif len(parts) == 2:    # N <TAB> meaning (no check possible)
                n, key, gloss = parts[0], None, parts[1]
            else:
                m = re.match(r"^(\d+)\s+(.*)$", line)
                if not m:
                    continue
                n, key, gloss = m.group(1), None, m.group(2)
            if n not in batch:
                bad.append(n)
            elif key is not None and not batch[n].startswith(key):
                bad.append(n)        # the answer was written for another word: it is asked again
            elif gloss.strip():
                rows.append((n, batch[n], gloss.strip().replace("—", " - ").replace("–", " - ")))
        missing = [n for n in batch if n not in {r[0] for r in rows}]
        (OUT / f"{prev:03d}.tsv").write_text("\n".join(f"{n}\t{w}\t{g}" for n, w, g in rows) + "\n", encoding="utf-8")
        print(f"batch {prev:03d}: {len(rows)} meanings saved; unanswered {len(missing)}; unknown ids {len(bad)}")
        if missing:
            (WORK / f"redo_{prev:03d}.txt").write_text("\n".join(f"{n}\t{batch[n]}" for n in missing), encoding="utf-8")
            print(f"  unanswered words saved to work/redo_{prev:03d}.txt (they come up again in the next batch)")
    todo, have, groups = queue()
    # the unanswered words of a batch are simply still in the queue
    todo = [(k, g) for k, g in todo if not broken(g["rep"][1])] if True else todo
    batch_no = next_batch_no()
    lines = []
    for i, (k, g) in enumerate(todo[:size], 1):
        lines.append(f"{i}\t{g['rep'][1]}")
    (WORK / f"batch_{batch_no:03d}.txt").write_text("\n".join(lines), encoding="utf-8")
    print(f"--- batch {batch_no:03d}: {len(lines)} words ({len(todo):,} groups left, {sum(g['n'] for _, g in todo):,} tokens) ---")
    print("\n".join(lines))


def cmd_status():
    todo, have, groups = queue()
    real = [(k, g) for k, g in todo if not broken(g["rep"][1])]
    junk = [(k, g) for k, g in todo if broken(g["rep"][1])]
    miss = sum(1 for v in have.values() if v == "?")
    print(f"written: {len(have) - miss:,} meanings for groups, {miss:,} marked misread")
    print(f"left to write: {len(real):,} groups ({sum(len(g['members']) for _, g in real):,} words, {sum(g['n'] for _, g in real):,} tokens)")
    print(f"left that look like misreads (not shown for writing): {len(junk):,} groups ({sum(len(g['members']) for _, g in junk):,} words)")
    by = collections.Counter(L for _, g in real for L, _ in g["members"])
    print("words left by language:", dict(by))


def cmd_load(path):
    d = sqlite3.connect(path)
    sid = d.execute("select id from source where code='curated'").fetchone()[0]
    d.execute("delete from sense where entry_id in (select id from entry where source_id=? and pos='glossed')", (sid,))
    d.execute("delete from entry where source_id=? and pos='glossed'", (sid,))
    todo, have, groups = queue()
    # members of every group, from the full word lists (written groups are no longer in the queue, so rebuild them)
    members = collections.defaultdict(list)
    for L in LANGS:
        p = WORK / f"no_gloss_{L}.tsv"
        if p.exists():
            for line in p.read_text(encoding="utf-8").splitlines():
                if line:
                    w = line.split("\t")[0]
                    members[fold(w) if L != "en" else w].append((L, w))
    # words written earlier may have left the lists since (they got entries): keep their own spelling too
    own = {}
    for f in sorted(OUT.glob("[0-9]*.tsv")):
        for line in f.read_text(encoding="utf-8").splitlines():
            p = line.split("\t")
            if len(p) >= 3:
                own[fold(p[1])] = (p[1], p[2])
    nid = d.execute("select max(id) from entry").fetchone()[0] + 1
    n = 0
    misread = []
    sys.path.insert(0, str(HERE))
    from parse_wiktionary import norm_unicode
    for k, (word, gloss) in own.items():
        if gloss == "?":
            misread.append(word)
            continue
        seen = set()
        mem = members.get(k, [])
        lang0 = "en" if not cr.INDIC.search(word) else None
        for L, w in mem + ([(None, word)] if not mem else []):
            lang = L or ("en" if not cr.INDIC.search(w) else ("bn" if re.search(r"[ঀ-৿]", w) else "or" if re.search(r"[଀-୿]", w) else "sa"))
            if (lang, w) in seen:
                continue
            seen.add((lang, w))
            norm = norm_unicode(w)
            d.execute("insert into entry(id, lang, headword, norm, skey, pos, ipa, etymology, freq, source_id) values (?,?,?,?,?,?,?,?,?,?)",
                      (nid, lang, w, norm, cr.to_deva(norm).lower(), "glossed", None, None, 1, sid))
            d.execute("insert into sense(entry_id, idx, gloss_lang, gloss, example) values (?,?,?,?,?)", (nid, 0, "en", gloss, None))
            nid += 1
            n += 1
    d.commit()
    (OUT / "misread.txt").write_text("\n".join(sorted(misread)), encoding="utf-8")
    print(f"{n} entries added from {len(own) - len(misread):,} written groups; {len(misread)} marked misread")


if __name__ == "__main__":
    c = sys.argv[1] if len(sys.argv) > 1 else "status"
    if c == "next":
        size = int(sys.argv[sys.argv.index("--size") + 1]) if "--size" in sys.argv else 400
        merge = sys.argv[sys.argv.index("--merge") + 1] if "--merge" in sys.argv else None
        cmd_next(size, merge)
    elif c == "load":
        cmd_load(sys.argv[2] if len(sys.argv) > 2 else str(HERE / "dictionary.db"))
    else:
        cmd_status()
