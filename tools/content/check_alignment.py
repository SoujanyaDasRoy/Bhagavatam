"""Checks that the Bengali and Odia text sits under the right verse, using the Hindi text as the yardstick.

    py tools/content/check_alignment.py

Read-only. Three checks on content/content.db:
  1. copies   a verse whose first 120 characters are almost the same as another verse of the same chapter
  2. shared   the same text under verses of two different chapters
  3. length   a verse whose length is far from the Hindi translation of the same verse (ratio outside 0.4 .. 3.5; joint verses skipped)
A verse flagged here is not always wrong (joint verses and long commentary exist), but every real misplacement found so far
(shifted numbers, a page's paragraph copied under several verses, a neighbour's text in front) shows up in one of these three.
"""
import collections
import difflib
import os
import re
import sqlite3
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.stdout.reconfigure(encoding="utf-8")


def norm(t):
    return re.sub(r"\s+", "", t or "")


def main():
    db = sqlite3.connect(os.path.join(ROOT, "content", "content.db"))
    for col, name in (("bn", "Bengali"), ('"or"', "Odia")):
        rows = db.execute(f'SELECT skandha, chapter, num, {col}, hi FROM verse WHERE coalesce({col},"")<>""').fetchall()
        by = collections.defaultdict(list)
        shared = collections.defaultdict(list)
        for s, c, n, t, h in rows:
            if len(t) >= 60:
                by[(s, c)].append((n, t))
            if len(norm(t)) >= 80:
                shared[norm(t)].append((s, c, n))
        copies = []
        for k, v in by.items():
            for i in range(len(v)):
                for j in range(i + 1, len(v)):
                    a, b = v[i][1][:120], v[j][1][:120]
                    m = difflib.SequenceMatcher(None, a, b)
                    if m.quick_ratio() > 0.9 and m.ratio() > 0.85:
                        copies.append((k, v[i][0], v[j][0]))
        across = [v for v in shared.values() if len({(a, b) for a, b, _ in v}) > 1]
        length = []
        for s, c, n, t, h in rows:
            if h and len(h) >= 40 and not 0.4 <= len(t) / len(h) <= 3.5:
                length.append((s, c, n, round(len(t) / len(h), 1)))
        print(f"== {name}: {len(copies)} near-copies in a chapter, {len(across)} texts shared by two chapters, {len(length)} verses far from the Hindi length")
        for x in copies[:30]:
            print("  copy  ", x)
        for x in across[:30]:
            print("  shared", x)
        print("  length (first 30):", length[:30])


if __name__ == "__main__":
    main()
