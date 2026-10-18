#!/usr/bin/env python3
"""Add hand-written entries for frequent book words that Wiktionary lacks: content/_dict/curated/{or,hi,bn,sa}.tsv.

    py content/_dict/add_curated.py [path/to/dictionary.db]      (default: app/src/main/assets/dictionary.db)

Format: word <TAB> part of speech <TAB> English gloss. The entries are written for this app (names, common function words and
verb forms, all inflected forms Wiktionary does not list) and are NOT reviewed by a native reader: they are stored with their own
source row "curated" so the credits and the review can find them. Safe to run again (the curated source is replaced each time).
"""
import sqlite3
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
sys.path.insert(0, str(HERE))
from parse_wiktionary import norm_unicode  # noqa: E402

sys.stdout.reconfigure(encoding="utf-8")


def to_deva(w):
    out = []
    for ch in w:
        c = ord(ch)
        out.append(chr(c - 0x80) if 0x0980 <= c <= 0x09FF else chr(c - 0x200) if 0x0B00 <= c <= 0x0B7F else ch)
    return "".join(out)


def main():
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "app/src/main/assets/dictionary.db"
    db = sqlite3.connect(str(path))
    row = db.execute("select id from source where code='curated'").fetchone()
    if row:
        sid = row[0]
        ids = [r[0] for r in db.execute("select id from entry where source_id=?", (sid,))]
        for i in ids:
            db.execute("delete from sense where entry_id=?", (i,))
            db.execute("delete from form where entry_id=?", (i,))
        db.execute("delete from entry where source_id=?", (sid,))
    else:
        sid = db.execute("select max(id) from source").fetchone()[0] + 1
        db.execute("insert into source(id, code, name, licence, url, attribution, retrieved) values (?,?,?,?,?,?,?)",
                   (sid, "curated", "Bhagavatam app (written for this app, unreviewed)", "CC0 1.0", "",
                    "Short glosses written for this app; not reviewed by a native reader.", "2026-10-14"))
    nid = db.execute("select max(id) from entry").fetchone()[0] + 1
    n = 0
    for lang in ("or", "hi", "bn", "sa"):
        f = HERE / "curated" / f"{lang}.tsv"
        if not f.exists():
            continue
        for line in f.read_text(encoding="utf-8").splitlines():
            parts = line.split("\t")
            if len(parts) < 3 or not parts[0].strip():
                continue
            word, pos, gloss = parts[0].strip(), parts[1].strip(), parts[2].strip()
            norm = norm_unicode(word)
            db.execute("insert into entry(id, lang, headword, norm, skey, pos, ipa, etymology, freq, source_id) values (?,?,?,?,?,?,?,?,?,?)",
                       (nid, lang, word, norm, to_deva(norm).lower(), pos, None, None, 1000, sid))
            db.execute("insert into sense(entry_id, idx, gloss_lang, gloss, example) values (?,?,?,?,?)", (nid, 0, "en", gloss, None))
            nid += 1
            n += 1
    db.commit()
    db.close()
    print(f"{n} curated entries added to {path}")


main()
