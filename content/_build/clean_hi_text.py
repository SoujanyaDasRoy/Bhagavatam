#!/usr/bin/env python3
"""Write the printed verse number at the end of a Hindi translation one way: "॥ N ॥" with no-break spaces (found in 1.2.28 to 1.2.33).
The number is KEPT; only its spacing is made consistent. Idempotent.
    py content/_build/clean_hi_text.py content/content.db
The same pass is made on app/src/main/assets/content.db (CLAUDE.md invariant 5)."""
import re
import sqlite3
import sys

NB = " "
END = re.compile(r"[\s ]*[॥।]+[\s ]*([०-९0-9]+)[\s ]*[॥।]+[\s ]*$")


def run(path):
    db = sqlite3.connect(path)
    n = 0
    for rid, hi in db.execute("SELECT rowid, hi FROM verse WHERE hi IS NOT NULL AND hi <> ''").fetchall():
        m = END.search(hi)
        if not m:
            continue
        body = hi[:m.start()].rstrip()
        new = body + NB + "॥" + NB + m.group(1) + NB + "॥"
        if new != hi:
            db.execute("UPDATE verse SET hi=? WHERE rowid=?", (new, rid))
            n += 1
    db.commit()
    db.close()
    return n


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    print("Hindi verses tidied:", run(sys.argv[1]))
