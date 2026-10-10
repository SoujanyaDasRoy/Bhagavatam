#!/usr/bin/env python3
"""Decide the loose-key rule by the real words of the book.

Rule A is the key in prototype.py (the code). Rule B is what the keys in tests/loose_pairs.json imply:
all 'h' dropped, 'ks' merged to 's', and a 'v'/'b' between two 'a' dropped. This script applies both to every word of the book
and lists, by frequency, the words that B merges and A keeps apart, so a person can see whether they are the same word or not.

    py content/_search/compare_rules.py            prints a report
    py content/_search/compare_rules.py --pairs    also checks that rule B reproduces every key of loose_pairs.json
"""
import collections
import json
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.stdout.reconfigure(encoding="utf-8")
import prototype as p  # noqa: E402


def rule_b_from_a(key_a: str) -> str:
    """Rule B applied on top of A's key: drop every h, merge ks to s, drop a b between two a, collapse repeats."""
    k = key_a.replace("h", "")
    k = k.replace("ks", "s")
    k = re.sub(r"(?<=a)b(?=a)", "", k)
    k = re.sub(r"(.)\1+", r"\1", k)
    return k


def key_a(word: str) -> str:
    return p.loose_from_indic(word) if re.search(r"[ऀ-ॿঀ-৿଀-୿]", word) else p.loose_from_roman(word)


def check_pairs() -> None:
    d = json.loads((HERE / "tests" / "loose_pairs.json").read_text(encoding="utf-8"))
    bad = [(x["input"], x["key"], rule_b_from_a(key_a(x["input"]))) for x in d["pairs"] if rule_b_from_a(key_a(x["input"])) != x["key"]]
    print(f"rule B reproduces {len(d['pairs']) - len(bad)} of {len(d['pairs'])} keys in loose_pairs.json")
    for b in bad[:10]:
        print("   ", b)


def main() -> None:
    from sqlite3 import connect
    db = connect(p.ROOT / "content" / "content.db")
    freq = collections.Counter()
    for sa, iast, hi, en, bn in db.execute('select sa, iast, hi, en, "bn" from verse'):
        for lang, text in (("hi", hi), ("bn", bn), ("sa", sa)):
            for w in p.WORD[lang].findall((text or "").replace("।", " ").replace("॥", " ")):
                freq[p.nfc(w)] += 1
        for text in (iast, en):
            for w in p.WORD["iast"].findall(text or ""):
                freq[w.lower()] += 1
    words = [w for w in freq if len(key_a(w)) >= 3]
    by_a, by_b = collections.defaultdict(set), collections.defaultdict(set)
    for w in words:
        ka = key_a(w)
        by_a[ka].add(w)
        by_b[rule_b_from_a(ka)].add(w)
    print(f"distinct words {len(words):,} | keys under A {len(by_a):,} | keys under B {len(by_b):,}")
    worse = collections.Counter(min(len(v), 6) for v in by_a.values())
    worse_b = collections.Counter(min(len(v), 6) for v in by_b.values())
    print("words per key, A:", dict(sorted(worse.items())))
    print("words per key, B:", dict(sorted(worse_b.items())))

    # groups that B joins and A keeps apart: list the A keys that become one B key
    merged = []
    for kb, ws in by_b.items():
        a_keys = {key_a(w) for w in ws}
        if len(a_keys) > 1:
            weight = sum(freq[w] for w in ws)
            merged.append((weight, kb, sorted(a_keys), sorted(ws, key=lambda w: -freq[w])[:5]))
    merged.sort(reverse=True)
    print(f"\nB key groups that join two or more A keys: {len(merged):,}")
    print("the 40 most frequent, with the words in them (same word, or different words?):")
    for weight, kb, aks, ws in merged[:40]:
        print(f"  {kb:10} <- A keys {aks[:4]}  e.g. {ws[:4]}  (count {weight})")
    if "--pairs" in sys.argv:
        print()
        check_pairs()


if __name__ == "__main__":
    main()
