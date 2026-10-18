#!/usr/bin/env python3
"""Put each Odia chapter title on the right chapter.

    py content/_build/align_odia_titles.py            report only
    py content/_build/align_odia_titles.py --apply    also write content/or/titles.json and chapter.title_or in both content.db copies

read_contents.py reads the printed contents (ସୂଚୀପତ୍ର) entry by entry and counts chapters as it goes; a missed line or a title that
runs onto a second line moves the count by one, and every title after it lands one chapter off (2026-10-18: 51 titles in the app
were on a neighbouring chapter). Here the entries of each Skandha, in printed order, are aligned to the chapters in order
(Needleman-Wunsch, monotone, skips allowed), scored by how many words the Odia title shares with the Hindi title of the chapter
(the Odia translation follows the Hindi, so titles share most of their Sanskrit words). A title is kept only when
  - both reading models agree on it (or differ only by stray marks at the end, which are cut), and
  - it shares enough words with the Hindi title of the chapter it is aligned to (Dice >= MIN_SIM).
Every other chapter keeps no Odia title and the app shows the Hindi one.
"""
import json
import re
import sqlite3
import sys
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
REPORT = ROOT / "content/_localocr/work/toc/toc_report.md"
OUT = ROOT / "content/or/titles.json"
DBS = [ROOT / "content/content.db", ROOT / "app/src/main/assets/content.db"]
MIN_SIM = 0.25
sys.stdout.reconfigure(encoding="utf-8")

ODIA_WORD = re.compile(r"^[଀-୿‌‍-]+$")
TRAIL = re.compile(r"[\s!?,;:'`.]+$")


def heads(t):
    """Three-letter starts of the words, Odia moved into Devanagari, b and v as one letter."""
    t = "".join(chr(ord(ch) - 0x200) if 0x0B00 <= ord(ch) <= 0x0B7F else ch for ch in unicodedata.normalize("NFC", t or ""))
    t = t.replace("व", "ब").replace("य़", "य")
    return {w[:3] for w in re.findall(r"[ऀ-ॿ]+", t) if len(w) >= 3}


def sim(a, b):
    ha, hb = heads(a), heads(b)
    return 2 * len(ha & hb) / (len(ha) + len(hb)) if ha and hb else 0.0


def agreed(a, b, voted=None):
    """The reading both models give, or None. Punctuation stuck to a word does not count as a difference, a difference made
    only of stray marks at the end is cut off, and read_contents.py's own three-reader vote (toc.json) is taken when it has one."""
    if voted:
        return TRAIL.sub("", voted)
    ta = [w.strip("!?,;:'`.॥") for w in TRAIL.sub("", a).split()]
    tb = [w.strip("!?,;:'`.॥") for w in TRAIL.sub("", b).split()]
    ta, tb = [w for w in ta if w], [w for w in tb if w]
    if ta == tb:
        return " ".join(ta) or None
    k = 0
    while k < min(len(ta), len(tb)) and ta[k] == tb[k]:
        k += 1

    def junk(tok):
        return len(tok) <= 2 or not ODIA_WORD.match(tok) or re.search(r"[୦-୯]", tok)

    if k >= 2 and all(junk(t) for t in ta[k:]) and all(junk(t) for t in tb[k:]):
        return " ".join(ta[:k])
    return None


def entries():
    """Per Skandha, the printed entries in order: (number the reader gave, agreed title or None)."""
    out = {}
    voted = json.loads((REPORT.parent / "toc.json").read_text(encoding="utf-8"))
    for line in REPORT.read_text(encoding="utf-8").splitlines():
        p = line.split("\t")
        if len(p) < 5 or not re.match(r"^\d+\.\d+$", p[0]):
            continue
        s, c = map(int, p[0].split("."))
        vote = voted.get(p[0]) if p[1].strip() == "OK" else None
        out.setdefault(s, []).append((c, agreed(p[2].strip(), p[4].strip(), vote)))
    return out


def align(ent, hindi):
    """Monotone alignment of entries to chapters 1..n maximising the summed likeness; returns {chapter: title}."""
    n, m = len(hindi), len(ent)
    score = [[0.0] * (n + 1) for _ in range(m + 1)]
    back = [[None] * (n + 1) for _ in range(m + 1)]
    for i in range(m + 1):
        for j in range(n + 1):
            if i == 0 and j == 0:
                continue
            best, how = -1.0, None
            if i and j:
                num, title = ent[i - 1]
                s = (sim(title, hindi[j - 1]) if title else 0.0) + (0.05 if num == j else 0.0)
                if score[i - 1][j - 1] + s > best:
                    best, how = score[i - 1][j - 1] + s, "pair"
            if i and score[i - 1][j] > best:
                best, how = score[i - 1][j], "skip_entry"
            if j and score[i][j - 1] > best:
                best, how = score[i][j - 1], "skip_chapter"
            score[i][j], back[i][j] = best, how
    got, i, j = {}, m, n
    while i or j:
        how = back[i][j]
        if how == "pair":
            title = ent[i - 1][1]
            if title and sim(title, hindi[j - 1]) >= MIN_SIM:
                got[j] = title
            i, j = i - 1, j - 1
        elif how == "skip_entry":
            i -= 1
        else:
            j -= 1
    return got


def main():
    db = sqlite3.connect(DBS[0])
    hindi, current = {}, {}
    for s, c, hi, ot in db.execute("select skandha, chapter, title_hi, title_or from chapter order by skandha, chapter"):
        hindi.setdefault(s, []).append(hi or "")
        current[f"{s}.{c}"] = ot
    titles = {}
    for s, ent in sorted(entries().items()):
        for c, t in align(ent, hindi[s]).items():
            titles[f"{s}.{c}"] = unicodedata.normalize("NFC", t)
    moved = [k for k, t in titles.items() if current.get(k) and current[k] != t]
    dropped = [k for k, t in current.items() if t and k not in titles]
    print(f"titles kept: {len(titles)} of {len(current)}; changed: {len(moved)}; dropped (no clear match): {len(dropped)};"
          f" new: {len([k for k in titles if not current.get(k)])}")
    for k in sorted(moved + dropped, key=lambda x: tuple(map(int, x.split("."))))[:400]:
        print(f"  {k}: {current.get(k)!r} -> {titles.get(k)!r}")
    if "--apply" in sys.argv:
        OUT.write_text(json.dumps(titles, ensure_ascii=False, indent=1), encoding="utf-8")
        for p in DBS:
            d = sqlite3.connect(p)
            d.execute("update chapter set title_or = null")
            d.executemany("update chapter set title_or = ? where skandha = ? and chapter = ?",
                          [(t, *map(int, k.split("."))) for k, t in titles.items()])
            d.commit()
        print("written:", OUT, "and", ", ".join(str(p) for p in DBS))


if __name__ == "__main__":
    main()
