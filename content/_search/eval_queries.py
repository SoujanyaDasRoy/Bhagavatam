#!/usr/bin/env python3
"""Run content/_search/gold_queries.json against a search engine and report what passes.

    python content/_search/eval_queries.py                 uses the real search.db (search_engine.Index, built by build_index.py)
    python content/_search/eval_queries.py --engine prototype   the old in-memory loose-key prototype (no aliases, related words or typos)

Output per query: PASS, FAIL (a required query broke: fix it), or TARGET (a target query that does not pass yet: expected).
Exit code 1 if any required query fails. A target that starts passing is reported as 'TARGET now passes: promote it to required'.
"""
import argparse
import collections
import json
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.stdout.reconfigure(encoding="utf-8")
import prototype as p  # noqa: E402

REF = re.compile(r"^(\d+)[.:/\-\s]+(\d+)(?:[.:/\-\s]+(\d+))?$")
INDIC = re.compile(r"[ऀ-ॿঀ-৿଀-୿]")


def parse_reference(q: str):
    m = REF.match(q.strip())
    if not m:
        return None
    sk = int(m.group(1))
    if not 0 <= sk <= 12:
        return None
    return [sk, int(m.group(2)), int(m.group(3)) if m.group(3) else None]


class Prototype:
    """The prototype's loose-key index: every word of the query must be present (any order); Roman and Indic queries meet."""

    def __init__(self):
        self.rows, _, _ = p.build()
        self.meta = {r[0]: (r[1], r[2], r[3]) for r in self.rows}
        self.loose, _ = p.build_loose()

    def verses(self, query: str, scope: str = "verse"):
        words = [w for w in re.findall(r"[^\s]+", query.strip())]
        if len(query.strip()) < 2 or not words:
            return set()
        sets = []
        for w in words:
            k = p.loose_from_indic(w) if INDIC.search(w) else p.loose_from_roman(w)
            hit = self.loose.get(k, set())
            if scope == "chapter":
                hit = {(self.meta[v][0], self.meta[v][1]) for v in hit}
            sets.append(hit)
        return set.intersection(*sets)

    def chapters(self, hit, scope, query=""):
        return set(hit) if scope == "chapter" else {(self.meta[v][0], self.meta[v][1]) for v in hit}


def run(engine) -> int:
    gold = json.loads((HERE / "gold_queries.json").read_text(encoding="utf-8"))["queries"]
    bad = 0
    for g in gold:
        scope = g.get("scope", "verse")
        problems = []
        if g.get("kind") == "reference":
            got = parse_reference(g["query"])
            if got != g["expect"]:
                problems.append(f"parsed as {got}, expected {g['expect']}")
            label = g["query"]
        elif "same_results" in g:
            sets = [frozenset(engine.verses(q)) for q in g["same_results"]]
            if len(set(sets)) != 1:
                sizes = {q: len(s) for q, s in zip(g["same_results"], sets)}
                problems.append(f"different result sets: {sizes}")
            label = " = ".join(g["same_results"])
        else:
            hit = engine.verses(g["query"], scope)
            n = len(hit)
            if "min_verses" in g and n < g["min_verses"]:
                problems.append(f"{n} results, expected at least {g['min_verses']}")
            if "max_verses" in g and n > g["max_verses"]:
                problems.append(f"{n} results, expected at most {g['max_verses']}")
            if "chapters_include" in g:
                have = {f"{a}.{b}" for a, b in engine.chapters(hit, scope, g["query"])}
                missing = [c for c in g["chapters_include"] if c not in have]
                if missing:
                    problems.append(f"chapters {missing} not in the results")
            label = f"{g['query']}  ({n} {'chapters' if scope == 'chapter' else 'verses'})"
        target = g["status"] == "target"
        if not problems:
            tag = "TARGET now passes: promote it to required" if target else "PASS"
        else:
            tag = "TARGET" if target else "FAIL"
            bad += 0 if target else 1
        print(f"{tag:8} {g['id']:24} {label}" + ("" if not problems else "  <- " + "; ".join(problems)))
    print(f"\n{bad} required queries failed")
    return 1 if bad else 0


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--engine", choices=["prototype", "index"], default="index")
    a = ap.parse_args()
    if a.engine == "index":
        from search_engine import Index, SEARCH_DB   # the real content/_search/search.db (build it with build_index.py)
        if not SEARCH_DB.exists():
            sys.exit("search.db does not exist: run  py content/_search/build_index.py  first (or use --engine prototype).")
        sys.exit(run(Index()))
    sys.exit(run(Prototype()))


if __name__ == "__main__":
    main()
