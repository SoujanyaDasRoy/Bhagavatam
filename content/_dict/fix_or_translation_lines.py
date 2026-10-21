#!/usr/bin/env python3
"""Odia Wiktionary pages hold translation lines such as "ବଙ୍ଗାଳୀ: গড়ানে" and "ହିନ୍ଦୀ: ..." that were stored as Odia meanings.

    py -I content/_dict/fix_or_translation_lines.py content/_dict/dictionary.db

Run it after build_dictionary.py (and again after any rebuild from the dumps). An Odia entry must not show Bengali or Hindi text as its meaning:
the line is removed; an entry left with no sense at all is removed too (its forms with it), so the app falls back to the book's own meaning.
"""
import sqlite3
import sys

PREFIX = {"ବଙ୍ଗାଳୀ": "bn", "ହିନ୍ଦୀ": "hi"}
db = sqlite3.connect(sys.argv[1])
dropped = 0
emptied = set()
for pre, lang in PREFIX.items():
    rows = db.execute(
        "SELECT s.id, s.entry_id, s.gloss FROM sense s JOIN entry e ON e.id = s.entry_id "
        "WHERE e.lang = 'or' AND s.gloss LIKE ? || ':%'", (pre,)).fetchall()
    for sid, eid, gloss in rows:
        others = db.execute(
            "SELECT COUNT(*) FROM sense WHERE entry_id = ? AND id != ? AND gloss NOT LIKE 'ବଙ୍ଗାଳୀ:%' AND gloss NOT LIKE 'ହିନ୍ଦୀ:%'",
            (eid, sid)).fetchone()[0]
        if others:
            db.execute("DELETE FROM sense WHERE id = ?", (sid,))
            dropped += 1
        else:
            db.execute("DELETE FROM sense WHERE id = ?", (sid,))
            dropped += 1
            emptied.add(eid)
for eid in emptied:
    db.execute("DELETE FROM form WHERE entry_id = ?", (eid,))
    db.execute("DELETE FROM entry WHERE id = ?", (eid,))
db.commit()
print(f"Odia translation lines: {dropped} removed, {len(emptied)} entries left empty and removed")
