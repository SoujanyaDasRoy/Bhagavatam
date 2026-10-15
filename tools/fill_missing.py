"""One command that works on the missing Bengali and Odia verses.

    python tools/fill_missing.py              Bengali and Odia
    python tools/fill_missing.py --bn         Bengali only
    python tools/fill_missing.py --or         Odia only
    python tools/fill_missing.py --dry        only print what would run

Bengali: finds the chapters that still have empty verses, re-reads their PDF pages with the Gemini vision extractor
(needs GEMINI_API_KEY in the environment), assembles, promotes the complete chapters, loads them into both content.db copies.
Odia: finishes the restartable local-reader steps (run_settle.py), then loads content/or into both content.db copies.
Then the search index version is checked and the README tables are rebuilt. Stops at the first step that fails.
"""
import json
import os
import re
import sqlite3
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PY = sys.executable
DBS = [os.path.join(ROOT, "content", "content.db"), os.path.join(ROOT, "app", "src", "main", "assets", "content.db")]
DRY = "--dry" in sys.argv


def run(cmd, cwd=ROOT):
    print("\n$", " ".join(cmd), f"   (in {os.path.relpath(cwd, ROOT) or '.'})", flush=True)
    if not DRY and subprocess.call(cmd, cwd=cwd) != 0:
        sys.exit("step failed, stopped")


def missing_chapters(col, frm):
    db = sqlite3.connect(DBS[0])
    return db.execute(f'''SELECT DISTINCT skandha, chapter FROM verse
                          WHERE coalesce("{col}",'')='' AND {frm} IS NULL ORDER BY 1,2''').fetchall()


def bengali_pages(chapters):
    """volume -> sorted PDF pages of the chapters, from source_pages in content/bn (a chapter's pages plus one either side)."""
    pages = {}
    for s, c in chapters:
        p = os.path.join(ROOT, "content", "bn", f"skandha-{s:02d}", f"chapter-{c:02d}.md")
        if not os.path.exists(p):
            print("no Bengali file for", s, c, "- its pages cannot be found, skipped")
            continue
        m = re.search(r"source_pages:\s*\[(.*?)\]", open(p, encoding="utf-8").read())
        for v, n in re.findall(r"v(\d):(\d+)", m.group(1) if m else ""):
            n = int(n)
            pages.setdefault(v, set()).update({n - 1, n, n + 1})
    return {v: sorted(x) for v, x in pages.items()}


def spans(nums):
    out, s, p = [], None, None
    for n in nums:
        if s is None:
            s = p = n
        elif n == p + 1:
            p = n
        else:
            out.append(f"{s}-{p}"); s = p = n
    out.append(f"{s}-{p}")
    return out


def bengali():
    ch = missing_chapters("bn", "bn_from")
    print(f"Bengali: {len(ch)} chapters with empty verses")
    bx = os.path.join(ROOT, "content", "_bengali", "bn_extract")
    for vol, nums in bengali_pages(ch).items():
        for sp in spans(nums):
            run([PY, "vision_extract.py", "run", "--vol", vol, "--pages", sp, "--workers", "2", "--force"], bx)
    run([PY, "bn_extract.py", "assemble"], bx)
    b = os.path.join(ROOT, "content", "_build")
    run([PY, "promote_bengali.py", "--force"], b)
    for db in DBS:
        run([PY, "add_bengali.py", db], b)


def odia():
    print(f"Odia: {len(missing_chapters('or', 'or_from'))} chapters with empty verses")
    run([PY, os.path.join("content", "_localocr", "run_settle.py")])
    ver = sqlite3.connect(DBS[0]).execute("SELECT value FROM meta WHERE key='content_version'").fetchone()[0]
    for db in DBS:
        run([PY, os.path.join("content", "_build", "add_odia.py"), db, str(int(ver) + 1)])


def snapshot():
    """Copy of every translation column before anything runs, so a re-read can never make a verse worse."""
    db = sqlite3.connect(DBS[0])
    return {(s, c, n): (b, bf, o, of) for s, c, n, b, bf, o, of in
            db.execute('SELECT skandha, chapter, num, bn, bn_from, "or", or_from FROM verse')}


def protect(before, version):
    """Put the earlier text back where the new run left a verse empty; keep new text where it filled a gap. Both copies, then search.db."""
    if DRY:
        return
    for path in DBS:
        db = sqlite3.connect(path)
        back = 0
        for s, c, n, b, bf, o, of in db.execute('SELECT skandha, chapter, num, bn, bn_from, "or", or_from FROM verse').fetchall():
            ob, obf, oo, oof = before.get((s, c, n), (None,) * 4)
            if not (b or "").strip() and bf is None and ((ob or "").strip() or obf is not None):
                db.execute("UPDATE verse SET bn=?, bn_from=? WHERE skandha=? AND chapter=? AND num=?", (ob, obf, s, c, n)); back += 1
            if not (o or "").strip() and of is None and ((oo or "").strip() or oof is not None):
                db.execute('UPDATE verse SET "or"=?, or_from=? WHERE skandha=? AND chapter=? AND num=?', (oo, oof, s, c, n)); back += 1
        db.execute("UPDATE meta SET value=? WHERE key='content_version'", (str(version),))
        db.commit(); db.close()
        print(f"{os.path.relpath(path, ROOT)}: {back} verses put back from the earlier text")
    run([PY, os.path.join("content", "_search", "build_index.py")])
    import shutil
    shutil.copy(os.path.join(ROOT, "content", "_search", "search.db"), os.path.join(ROOT, "app", "src", "main", "assets", "search.db"))


def main():
    before = None if DRY else snapshot()
    ver0 = 0 if DRY else int(sqlite3.connect(DBS[0]).execute("SELECT value FROM meta WHERE key='content_version'").fetchone()[0])
    want_bn, want_or = "--or" not in sys.argv, "--bn" not in sys.argv
    if want_bn:
        bengali()
    if want_or:
        odia()
    if before is not None:
        protect(before, ver0 + 1)
        import shutil
        shutil.copy(DBS[0], DBS[1])
    if not DRY:
        a, b = (open(p, "rb").read() for p in DBS)
        print("\ncontent.db copies identical:", a == b)
    run([PY, os.path.join("tools", "missing_report.py"), "--readme"])
    print("\nDone. search.db was rebuilt and copied; build the APK to ship the new text.")


if __name__ == "__main__":
    main()
