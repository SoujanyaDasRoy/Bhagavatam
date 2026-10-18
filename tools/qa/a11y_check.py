#!/usr/bin/env python3
"""Accessibility check on uiautomator dumps (what Android's Accessibility Scanner looks at first).

    py tools/qa/a11y_check.py <folder with *.xml dumps> [--density 2.625]

For every tappable node (clickable or long-clickable):
  - no label: no text and no content-desc on the node or anything inside it (TalkBack says only "button")
  - small target: narrower or lower than 48dp (the project rule, CLAUDE.md section 5.3), unless it is a text link inside a line
Each finding is counted once per screen kind (home, top, end) and resource or label, so repeats across languages do not inflate it.
"""
import collections
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

sys.stdout.reconfigure(encoding="utf-8")
BOUNDS = re.compile(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]")


def label_of(n):
    for x in n.iter():
        t = (x.get("text") or "").strip() or (x.get("content-desc") or "").strip()
        if t:
            return t
    return ""


def main():
    folder = Path(sys.argv[1])
    dens = float(sys.argv[sys.argv.index("--density") + 1]) if "--density" in sys.argv else 2.625
    min_px = 48 * dens - 1
    unlabelled, small = collections.Counter(), collections.Counter()
    where = collections.defaultdict(set)
    for f in sorted(folder.glob("*.xml")):
        kind = f.stem.split("_")[-1]
        try:
            root = ET.parse(f).getroot()
        except ET.ParseError:
            continue
        for n in root.iter("node"):
            if n.get("clickable") != "true" and n.get("long-clickable") != "true":
                continue
            m = BOUNDS.match(n.get("bounds", ""))
            if not m:
                continue
            x1, y1, x2, y2 = map(int, m.groups())
            w, h = x2 - x1, y2 - y1
            lab = label_of(n)
            key = (kind, n.get("class", "").split(".")[-1], n.get("resource-id", ""), lab[:40] if lab else f"@{x1},{y1} {w}x{h}")
            if not lab:
                unlabelled[key] += 1
                where[key].add(f.stem)
            if (w < min_px or h < min_px) and w > 0 and h > 0:
                small[key + (f"{w / dens:.0f}x{h / dens:.0f}dp",)] += 1
                where[key].add(f.stem)
    print(f"dumps: {len(list(folder.glob('*.xml')))}")
    print(f"\nTappable with no label ({len(unlabelled)} kinds):")
    for k, c in unlabelled.most_common():
        print(f"  {c:>3}x  {k[0]:<5} {k[1]:<12} {k[2]} {k[3]}   e.g. {sorted(where[k])[0]}")
    print(f"\nSmaller than 48dp ({len(small)} kinds):")
    for k, c in small.most_common(40):
        print(f"  {c:>3}x  {k[0]:<5} {k[1]:<12} {k[3][:40]:<40} {k[4]}")


if __name__ == "__main__":
    main()
