#!/usr/bin/env python3
"""The Python side of the key parity test. Run:  py content/_search/tests/test_loose_pairs.py

Every (input, key) pair in loose_pairs.json must be what the code gives; every must_match pair must produce one key;
every must_not_match pair must produce two different keys. The Kotlin test (LooseKeyTest) reads the same file.
Exit code 0 = pass.
"""
import json
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))
sys.stdout.reconfigure(encoding="utf-8")
import prototype as p  # noqa: E402

FN = {"roman": p.loose_from_roman, "indic": p.loose_from_indic}
INDIC = re.compile(r"[ऀ-ॿঀ-৿଀-୿]")


def key(w: str) -> str:
    return FN["indic" if INDIC.search(w) else "roman"](w)


def main() -> int:
    d = json.loads((HERE / "loose_pairs.json").read_text(encoding="utf-8"))
    bad = []
    for x in d["pairs"]:
        got = FN[x["fn"]](x["input"])
        if got != x["key"]:
            bad.append(f"pair {x['input']!r}: file {x['key']!r}, code {got!r}")
    for a, b in d["must_match"]:
        if key(a) != key(b):
            bad.append(f"must_match {a!r} / {b!r}: {key(a)!r} != {key(b)!r}")
    for a, b in d["must_not_match"]:
        if key(a) == key(b):
            bad.append(f"must_not_match {a!r} / {b!r}: both {key(a)!r}")
    print(f"{len(d['pairs'])} pairs, {len(d['must_match'])} must-match, {len(d['must_not_match'])} must-not-match")
    for b in bad:
        print("FAIL", b)
    print("PASS" if not bad else f"{len(bad)} failures")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
