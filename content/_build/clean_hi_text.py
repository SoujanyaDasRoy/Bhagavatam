#!/usr/bin/env python3
"""Remove a printed verse number left at the very end of a Hindi translation (found in 1.2.28 to 1.2.33).
    py content/_build/clean_hi_text.py content/content.db
Idempotent; the same pass is made on app/src/main/assets/content.db (CLAUDE.md invariant 5)."""
import re
import sqlite3
import sys

END = re.compile(r"\s*[॥।]+\s*[०-९0-9]+(?:\s*[-–]\s*[०-९0-9]+)*\s*[॥।]+\s*$")

def run(path):
    db = sqlite3.connect(path)
    n = 0
    for rid, hi in db.execute("SELECT rowid, hi FROM verse WHERE hi IS NOT NULL AND hi <> ''").fetchall():
        new = END.sub("", hi)
        if new != hi:
            if new and new[-1] not in "।?!\"”’)":
                new += "।"
            db.execute("UPDATE verse SET hi=? WHERE rowid=?", (new, rid))
            n += 1
    db.commit()
    db.close()
    return n

if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    print("Hindi verses cleaned:", run(sys.argv[1]))
