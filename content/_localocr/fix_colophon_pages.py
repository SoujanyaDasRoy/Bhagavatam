#!/usr/bin/env python3
"""Pages where a chapter ends and the next begins, but the reader did not mark the closing line ("iti shrimad bhagavate ...").

    py content/_localocr/fix_colophon_pages.py <pages dir>   e.g. content/_localocr/work/final/vision

Such a page has the verse numbers falling back to 1 inside it (..., 54, 55, 1, 2) and the closing line sitting among the
paragraphs as short, garbled ([?]-marked) paragraphs with no verse marker. Without help the assembler gives the whole page to the
new chapter, so the end of the previous chapter is lost to it. Here the closing-line paragraphs are lifted out into `colophon`
and `paragraphs_before_chapter_heading` is set to the number of paragraphs before them. Only clear cases are changed (all of:
no colophon yet, 0 paragraphs before the heading, a fall in the verse numbers, 1-3 short paragraphs of closing-line words that
follow a verse marker). Idempotent. Prints what it changed.
"""
import json
import re
import sys
from pathlib import Path

sys.stdout.reconfigure(encoding="utf-8")
LINE_WORDS = re.compile(r"ସ୍କନ୍|ସଂହିତା|ମହାପୁର|ପାରମହଂସ|ଅଧ୍ୟାୟ|ଇତି ଶ୍ରୀ|ସ୍କନ୍ଧ|ଶ୍ରୀମଦ୍ଭାଗ")
MARKER_END = re.compile(r"॥\s*[୦-୯]+(?:\s*[-–,]\s*[୦-୯]+)*\s*॥\s*$")


def fell(nums):
    return any(a >= 5 and b <= 3 and b < a for a, b in zip(nums, nums[1:]))


def closing_like(p):
    return len(p) <= 450 and not MARKER_END.search(p.strip()) and (p.count("[?]") >= 2 or bool(LINE_WORDS.search(p)))


def main(d):
    changed = 0
    for f in sorted(Path(d).glob("v*_p*.json")):
        j = json.loads(f.read_text(encoding="utf-8"))
        if j.get("colophon") or int(j.get("paragraphs_before_chapter_heading") or 0) > 0 or not fell(j.get("sanskrit_verse_numbers") or []):
            continue
        paras = j.get("paragraphs", [])
        run = [i for i, p in enumerate(paras) if closing_like(p)]
        if not run or len(run) > 3 or run != list(range(run[0], run[-1] + 1)) or run[0] == 0:
            continue
        if not MARKER_END.search(paras[run[0] - 1].strip()) or run[-1] == len(paras) - 1 and False:
            continue
        j["colophon"] = " ".join(paras[i] for i in run)
        j["paragraphs"] = paras[:run[0]] + paras[run[-1] + 1:]
        j["paragraphs_before_chapter_heading"] = run[0]
        j["chapter_marker_text"] = "boxed heading (found from the fall in verse numbers)"   # the assembler splits a page at a heading only if this says so
        j["_colophon_lifted"] = True
        f.write_text(json.dumps(j, ensure_ascii=False, indent=1), encoding="utf-8")
        changed += 1
        print(f"{f.stem}: closing line = paragraphs {run}, {run[0]} before the heading")
    print(f"{changed} page(s) changed")


if __name__ == "__main__":
    main(sys.argv[1])
