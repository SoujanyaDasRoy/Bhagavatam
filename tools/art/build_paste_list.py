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

# Self-contained prompts for the person making the pictures (works in any tool, no Project needed)
import re as _re
guide = open(os.path.join(ROOT, "PROMPT_GUIDE.md"), encoding="utf-8").read()
blocks = _re.findall(r"```\n(.*?)```", guide, _re.S)
setup = next(b for b in blocks if b.startswith("You paint chapter"))
style = setup.split("STYLE (always):")[1].split("CHARACTERS")[0].strip()
chars = setup.split("CHARACTERS (use exactly when named):")[1].strip()
hand = os.path.join(ROOT, "HANDOFF")
os.makedirs(hand, exist_ok=True)
with open(os.path.join(hand, "prompts_full.txt"), "w", encoding="utf-8") as f:
    for r in rows:
        if "DONE" in r[1]:
            continue
        f.write(f"===== {r[0]}  ({r[1]}) =====\n")
        f.write(f"Use the attached picture ch_0_1 only as the style reference, never copy its scene. Make ONE image, wide landscape 3:2.\n")
        f.write(f"SCENE: {r[2]} TIME AND LIGHT: {r[3]}. FIGURES: {r[4]}. MOOD: {r[5]}.\n")
        f.write(f"{style}\nCHARACTERS: {chars}\n\n")
with open(os.path.join(hand, "tracker.csv"), "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f); w.writerow(["id", "title", "made", "approved", "notes"])
    w.writerows([[r[0], r[1], "", "", ""] for r in rows if "DONE" not in r[1]])
