#!/usr/bin/env python3
"""Checks on the built search.db. Run after build_index.py:  py content/_search/tests/test_search_db.py
Exit code 0 = pass. (Rebuild determinism is checked by building twice and comparing bytes: see HANDOFF.md.)
"""
import sqlite3
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))
sys.stdout.reconfigure(encoding="utf-8")
import build_index as bi  # noqa: E402
from search_engine import Index, SEARCH_DB, parse_reference  # noqa: E402

fails = []


def check(ok, msg):
    print(("ok   " if ok else "FAIL ") + msg)
    if not ok:
        fails.append(msg)


# posting codec
for ids in ([], [1], [5, 6, 7], [1, 128, 129, 16383, 16384, 100000], list(range(1, 14581, 7))):
    check(bi.decode_posting(bi.encode_posting(ids)) == sorted(set(ids)), f"posting round trip, {len(ids)} ids")

check(SEARCH_DB.exists(), "search.db exists")
size = SEARCH_DB.stat().st_size
check(size <= 8 * 1024 * 1024, f"size {size / 1e6:.2f} MB is at most 8 MB")

db = sqlite3.connect(str(SEARCH_DB))
meta = dict(db.execute("select key, value from meta"))
cdb = sqlite3.connect(str(bi.CONTENT_DB))
cv = dict(cdb.execute("select key, value from meta"))["content_version"]
check(meta.get("content_version") == cv, f"index content_version {meta.get('content_version')} equals content.db {cv}")
check(db.execute("select count(*) from chapter_start").fetchone()[0] == cdb.execute("select count(*) from chapter").fetchone()[0], "one chapter_start row per chapter")
check(db.execute("select count(*) from sqlite_master where sql like '%fts5%' or sql like '%VIRTUAL%'").fetchone()[0] == 0, "no FTS5 / virtual tables")

ix = Index()
check(len(ix.verses("Krishna")) >= 1000, "Krishna has 1000+ verses")
check(ix.verses("कृष्ण") == ix.verses("Krishna") == ix.verses("কৃষ্ণ"), "one name, one result set in three scripts")
check(ix.verses("Nrisimha") == ix.verses("नृसिंह") == ix.verses("Narasimha"), "alias group: Nrisimha = नृसिंह = Narasimha")
check(len(ix.verses("a")) == 0, "one letter is not a search")
check(ix.lookup("Arjna")[1] == "arjun" and len(ix.lookup("Arjna")[0]) >= 100, "typo Arjna is corrected to the key of Arjuna")
check(ix.lookup("Krshna")[1] is None and len(ix.lookup("Krshna")[0]) >= 1000, "typed alias Krshna resolves by the alias list, not as a correction")
check(ix.lookup("Krishna")[1] is None, "a correct word is not 'corrected'")
check((8, 2) in ix.chapters(ix.verses("crocodile elephant", "chapter"), "chapter", "crocodile elephant"), "crocodile elephant reaches chapter 8.2")
check((10, 29) in ix.story_chapters("Rasa Lila"), "story Rasa Lila links to 10.29 (from Episodes.kt)")
check(parse_reference("13.1.1") is None and parse_reference("10 29 1") == [10, 29, 1], "reference parsing")
check(ix.chapter_of(1) == (0, 1), "verse 1 is in chapter 0.1")

print("\nPASS" if not fails else f"\n{len(fails)} failures")
sys.exit(1 if fails else 0)
