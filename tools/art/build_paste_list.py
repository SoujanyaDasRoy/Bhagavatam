"""Merge content/art/scenes/s*.txt into scenes.csv and paste_list.md.

Line format: ch_S_C | title | scene | light | figures | mood
Run: py tools/art/build_paste_list.py
"""
import csv
import glob
import os
import re

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "content", "art")
rows, seen = [], set()
for path in sorted(glob.glob(os.path.join(ROOT, "scenes", "s*.txt"))):
    for n, line in enumerate(open(path, encoding="utf-8"), 1):
        line = line.strip()
        if not line:
            continue
        p = [x.strip() for x in line.split(" | ")]
        assert len(p) == 6, f"{path}:{n}: {len(p)} fields"
        assert re.fullmatch(r"ch_\d+_\d+", p[0]), f"{path}:{n}: bad id {p[0]}"
        assert p[0] not in seen, f"{path}:{n}: duplicate {p[0]}"
        assert "—" not in line and "–" not in line, f"{path}:{n}: dash"
        seen.add(p[0])
        rows.append(p)

with open(os.path.join(ROOT, "scenes.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["id", "title", "scene", "light", "figures", "mood"])
    w.writerows(rows)

out, skandha = ["# Paste list\n", "One message per chapter. Tick when the picture is made and installed.\n"], None
for r in rows:
    s = r[0].split("_")[1]
    if s != skandha:
        skandha = s
        out.append(f"\n## Skandha {s}\n")
    out.append(f"- [ ] {r[0]}. SCENE: {r[2]} TIME AND LIGHT: {r[3]}. FIGURES: {r[4]}. MOOD: {r[5]}.")
open(os.path.join(ROOT, "paste_list.md"), "w", encoding="utf-8").write("\n".join(out) + "\n")
print(len(rows), "chapters")
