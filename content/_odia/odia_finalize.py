#!/usr/bin/env python3
"""Collect the pages that go into the book: trusted pages (Skandha 1 to 4 vision track) plus the checked consensus pages.

    py odia_finalize.py --consensus work_v/consensus --out work_final                    keep [?] marks (assembly flags them)
    py odia_finalize.py --consensus work_v/consensus --out work_final --strip --max-unresolved 0
                                                       remove the [?] marks; refuses while any page still has unresolved words
    py odia_finalize.py ... --fill-from-tess work_tess/vision      pages with no consensus yet: take the Tesseract page (draft)

Then assemble and look at the result:
    $env:ODIA_WORK="work_final"; $env:ODIA_OUT="work_final/out"; $env:ODIA_REVIEW="work_final/review"
    $env:ODIA_METHOD="Vision model (two independent readings), word consensus, human review (odia_verify.py)"
    py odia_assemble.py
The assembler checks every chapter against the Hindi verse list (expected_verses.json); read work_final/review/report.md.
"""
import argparse
import json
import os
import re
import sys
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import odia_verify as V  # noqa: E402

sys.stdout.reconfigure(encoding="utf-8")
UNSURE = re.compile(r"\[\?+\]")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--consensus", default=str(V.WORK / "consensus"))
    ap.add_argument("--trusted", default=str(HERE / "vision_transcripts"))
    ap.add_argument("--out", default=str(HERE / "work_final"))
    ap.add_argument("--strip", action="store_true", help="remove [?] marks (only when nothing is left unresolved)")
    ap.add_argument("--max-unresolved", type=int, default=0, help="pages with more unresolved words than this stop --strip")
    ap.add_argument("--fill-from-tess", help="directory of Tesseract page JSON used for pages with no consensus (draft quality)")
    a = ap.parse_args()

    out = Path(a.out) / "vision"
    out.mkdir(parents=True, exist_ok=True)
    trusted, cons = V.page_ids(a.trusted), V.page_ids(a.consensus)
    pages = {k: ("trusted", f) for k, f in trusted.items()}
    pages.update({k: ("consensus", f) for k, f in cons.items()})          # a checked page replaces a trusted one
    if a.fill_from_tess:
        for k, f in V.page_ids(a.fill_from_tess).items():
            pages.setdefault(k, ("tesseract", f))

    src_count, unresolved_pages = Counter(), []
    for (v, p), (src, f) in sorted(pages.items()):
        j = json.loads(f.read_text(encoding="utf-8"))
        n_unsure = sum(len(UNSURE.findall(x)) for x in j.get("paragraphs", []))
        src_count[src] += 1
        if src == "consensus" and n_unsure:
            unresolved_pages.append((n_unsure, v, p))
        j["_final"] = {"source": src, "unresolved": n_unsure}
        pages[(v, p)] = (src, j)
    bad = [x for x in unresolved_pages if x[0] > a.max_unresolved]
    if a.strip and bad:
        print(f"refusing to strip: {len(bad)} page(s) have more than {a.max_unresolved} unresolved word(s) (worst first):")
        for n, v, p in sorted(bad, reverse=True)[:20]:
            print(f"  v{v}:{p}  {n}")
        print("resolve them with odia_review.py queue + odia_verify.py apply, then run this again.")
        return 1
    for (v, p), (src, j) in pages.items():
        if a.strip:
            j["paragraphs"] = [re.sub(r"\s+", " ", UNSURE.sub("", x)).strip() for x in j["paragraphs"]]
        (out / f"v{v}_p{p:04d}.json").write_text(json.dumps(j, ensure_ascii=False, indent=1), encoding="utf-8")
    left = sum(n for n, _, _ in unresolved_pages)
    print(f"{len(pages)} pages -> {out}  (trusted {src_count['trusted']}, consensus {src_count['consensus']}, tesseract {src_count['tesseract']})")
    print(f"unresolved words on consensus pages: {left}" + ("  ([?] marks removed)" if a.strip else "  ([?] marks kept)"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
