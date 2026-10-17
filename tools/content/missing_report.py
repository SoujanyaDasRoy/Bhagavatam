"""Reports the verses that still have no Bengali or Odia translation and why.

Read-only for the databases and the chapter files. Run from the project root:

    python tools/content/missing_report.py            # print the report
    python tools/content/missing_report.py --readme   # also rewrite the tables between the README markers

For every verse with an empty translation it looks in the chapter's source markdown (content/bn, content/or) for a printed
group that covers it ("### শ্লোক ২৮-৩০", "॥ ৪-৫ ॥", "[শ্লোক ১২-১৫ একান্বয়ে অনুবাদিত]"):
  grouped      the print translates it together with the verses of a range, so the text sits on the first verse
  not found    no group covers it: the verse needs a re-read of its page or is not in the print
"""
import os
import re
import sqlite3
import sys
from itertools import groupby

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DB = os.path.join(ROOT, "content", "content.db")
DIG = str.maketrans("০১২৩৪৫৬৭৮৯୦୧୨୩୪୫୬୭୮୯", "01234567890123456789")
RANGE = re.compile(r"([0-9০-৯୦-୯]{1,3})\s*[-–—]\s*([0-9০-৯୦-୯]{1,3})(?:\s*[-–—]\s*([0-9০-৯୦-୯]{1,3}))?")
LANGS = (("bn", "bn_from", "Bengali", "bn", "বাংলা"), ("or", "or_from", "Odia (beta)", "or", "ଓଡ଼ିଆ"))
START, END = "<!-- missing-verses:start -->", "<!-- missing-verses:end -->"


def chapter_file(folder, s, c):
    names = [f"skandha-{s:02d}"] if s else ["mahatmya", "skandha-00"]
    for n in names:
        p = os.path.join(ROOT, "content", folder, n, f"chapter-{c:02d}.md")
        if os.path.exists(p):
            return p
    return None


def groups_in(path):
    """(first, last) of every printed group in the chapter file: headings, verse markers and 'translated together' notes."""
    if not path:
        return []
    out = []
    for line in open(path, encoding="utf-8"):
        if not (line.lstrip().startswith(("###", "####", "[")) or "॥" in line):
            continue
        for m in RANGE.finditer(line):
            nums = [int(x.translate(DIG)) for x in m.groups() if x]
            if 1 < len(nums) and nums[0] < nums[-1] <= nums[0] + 60:
                out.append((nums[0], nums[-1]))
    return out


def ranges(nums):
    out, s, p = [], None, None
    for n in sorted(nums):
        if s is None:
            s = p = n
        elif n == p + 1:
            p = n
        else:
            out.append((s, p)); s = p = n
    out.append((s, p))
    return ", ".join(str(a) if a == b else f"{a}-{b}" for a, b in out)


def report(db, col, frm, folder):
    rows = db.execute(f'''SELECT skandha, chapter, num, num_end FROM verse
                          WHERE coalesce("{col}",'')='' AND {frm} IS NULL ORDER BY 1,2,3''').fetchall()
    result = []
    for (s, c), g in groupby(rows, key=lambda r: (r[0], r[1])):
        nums = [n for r in list(g) for n in range(r[2], r[3] + 1)]
        grp = groups_in(chapter_file(folder, s, c))
        grouped, loose = {}, []
        for n in nums:
            hit = next((g2 for g2 in grp if g2[0] <= n <= g2[1]), None)
            (grouped.setdefault(hit, []) if hit else loose).append(n)
        result.append((s, c, nums, grouped, loose))
    return result


def table(name, res):
    """Four plain columns. Joint-group details are added only when some missing verse sits inside a printed group."""
    total = sum(len(r[2]) for r in res)
    n_grp = sum(len(v) for r in res for v in r[3].values())
    head = f"**{name}: {total} verses without a translation.**"
    if not total:
        return head
    if n_grp:
        head += f" {n_grp} of them are printed inside a joint group (see the last column)."
    cols = "| Skandha | Chapter | Missing verses | Count |" + (" Joint group |" if n_grp else "")
    lines = [head, "", cols, "|---|---|---|---|" + ("---|" if n_grp else "")]
    for s, c, nums, grouped, loose in res:
        row = f"| {'Mahatmya' if s == 0 else s} | {c} | {ranges(nums)} | {len(nums)} |"
        if n_grp:
            row += " " + ("; ".join(f"{a}-{b}" for a, b in sorted(grouped)) or "-") + " |"
        lines.append(row)
    return chr(10).join(lines)


def main():
    db = sqlite3.connect(DB)
    parts = []
    for col, frm, name, folder, _ in LANGS:
        res = report(db, col, frm, folder)
        parts.append(table(name, res))
    body = "\n\n".join(parts)
    print(body)
    if "--readme" in sys.argv:
        p = os.path.join(ROOT, "README.md")
        t = open(p, encoding="utf-8", newline="").read()
        nl = "\r\n" if "\r\n" in t else "\n"
        t = t.replace("\r\n", "\n")
        block = f"{START}\n{body}\n{END}"
        if START in t:
            t = re.sub(re.escape(START) + r".*?" + re.escape(END), lambda m: block, t, flags=re.S)
        else:
            sys.exit("README has no missing-verses markers")
        open(p, "w", encoding="utf-8", newline="").write(t.replace("\n", nl))
        print("\nREADME updated")


if __name__ == "__main__":
    main()
