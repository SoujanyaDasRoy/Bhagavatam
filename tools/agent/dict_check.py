#!/usr/bin/env python3
"""Check a dictionary.db against the schema and rules in docs/superpowers/plans/2026-10-09-dictionary-db.md.

    python tools/agent/dict_check.py                          checks app/src/main/assets/dictionary.db
    python tools/agent/dict_check.py path/to/dictionary.db --max-mb 30
    python tools/agent/dict_check.py --selftest               builds a tiny database in memory and checks the checker

Exit code 0 = every check passed, 1 = something failed. Failures are printed one per line starting with FAIL.
What it cannot judge: whether a meaning is right. Look at a sample by hand (the last section prints some).
"""
import argparse
import re
import sqlite3
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
DEFAULT = ROOT / "app" / "src" / "main" / "assets" / "dictionary.db"
LANGS = ("en", "hi", "bn", "or")
SCRIPT = {"hi": r"[ऀ-ॿ]", "bn": r"[ঀ-৿]", "or": r"[଀-୿]", "en": r"[A-Za-z]"}
# Words that must be found in every database that claims to cover that language (the most common words of the book).
MUST_FIND = {"en": ["lord", "king", "god"], "hi": ["धर्म", "भगवान", "राजा"], "bn": ["ধর্ম", "ভগবান", "রাজা"], "or": ["ଧର୍ମ", "ଭଗବାନ", "ରାଜା"]}
SCHEMA = {
    "meta": {"key", "value"},
    "source": {"id", "code", "name", "licence", "url", "attribution", "retrieved"},
    "entry": {"id", "lang", "headword", "norm", "skey", "pos", "ipa", "etymology", "freq", "source_id"},
    "sense": {"id", "entry_id", "idx", "gloss_lang", "gloss", "example"},
    "form": {"form_norm", "entry_id", "kind"},
}
LICENCE_OK = re.compile(r"CC[ -]BY|CC0|CC-BY|Public Domain|public domain|Creative Commons", re.I)


def check(path: Path, max_mb: float) -> int:
    fails = []

    def fail(msg):
        fails.append(msg)
        print("FAIL", msg)

    if not path.exists():
        print(f"FAIL {path} does not exist")
        return 1
    mb = path.stat().st_size / 1048576
    print(f"file: {path.name}, {mb:.1f} MB (budget {max_mb:g} MB)")
    if mb > max_mb:
        fail(f"database is {mb:.1f} MB, over the {max_mb:g} MB budget (see plan, size budget)")
    db = sqlite3.connect(path)
    have = {r[0] for r in db.execute("select name from sqlite_master where type in ('table','view')")}
    for t, cols in SCHEMA.items():
        if t not in have:
            fail(f"table {t} is missing")
            continue
        got = {r[1] for r in db.execute(f"pragma table_info({t})")}
        if not cols <= got:
            fail(f"table {t} lacks columns {sorted(cols - got)}")
    if fails:
        return 1
    meta = dict(db.execute("select key, value from meta").fetchall())
    for k in ("dictionary_version", "built", "schema"):
        if k not in meta:
            fail(f"meta has no '{k}'")

    print("== sources and licences")
    src = db.execute("select id, code, name, licence, attribution from source").fetchall()
    if not src:
        fail("no rows in source: every entry must point at a source with a licence")
    for i, code, name, lic, attr in src:
        n = db.execute("select count(*) from entry where source_id=?", (i,)).fetchone()[0]
        print(f"  {code}: {name} | {lic} | {n:,} entries")
        if not lic or not LICENCE_OK.search(lic):
            fail(f"source {code} has no usable licence ('{lic}')")
        if not attr:
            fail(f"source {code} has no attribution text (CC BY-SA requires credit)")
    bad = db.execute("select count(*) from entry where source_id not in (select id from source)").fetchone()[0]
    if bad:
        fail(f"{bad} entries point at a source that does not exist")

    print("== entries per language")
    for lang in LANGS:
        n = db.execute("select count(*) from entry where lang=?", (lang,)).fetchone()[0]
        s = db.execute("select count(*) from sense s join entry e on e.id=s.entry_id where e.lang=?", (lang,)).fetchone()[0]
        print(f"  {lang}: {n:,} entries, {s:,} senses")
        if n == 0:
            print(f"  (note) no {lang} entries; fine only if this build is not meant to cover {lang}")
            continue
        wrong = [h for (h,) in db.execute("select headword from entry where lang=? limit 5000", (lang,)) if not re.search(SCRIPT[lang], h)]
        if wrong:
            fail(f"{lang}: {len(wrong)} of the first 5000 headwords have no {lang} letters, e.g. {wrong[:3]}")
        for w in MUST_FIND[lang]:
            r = db.execute("select 1 from entry where lang=? and norm=? union select 1 from form f join entry e on e.id=f.entry_id where e.lang=? and f.form_norm=?", (lang, w, lang, w)).fetchone()
            if not r:
                fail(f"{lang}: the very common word '{w}' is not in the dictionary")

    print("== structure")
    q = {
        "entries with no sense": "select count(*) from entry e where not exists (select 1 from sense s where s.entry_id=e.id)",
        "senses with empty gloss": "select count(*) from sense where gloss is null or trim(gloss)=''",
        "senses pointing at no entry": "select count(*) from sense where entry_id not in (select id from entry)",
        "forms pointing at no entry": "select count(*) from form where entry_id not in (select id from entry)",
        "wiki markup left in glosses ([[ {{ <ref)": "select count(*) from sense where gloss like '%[[%' or gloss like '%{{%' or gloss like '%<ref%' or gloss like '%&nbsp;%'",
        "em or en dashes in glosses": "select count(*) from sense where gloss like '%' || char(8212) || '%' or gloss like '%' || char(8211) || '%'",
    }
    for label, sql in q.items():
        n = db.execute(sql).fetchone()[0]
        print(f"  {label}: {n}")
        if n:
            fail(f"{label}: {n}")
    fts = "entry_fts" in have
    print(f"  prefix search table: {'present' if fts else 'missing'}")
    if not fts:
        fail("entry_fts (FTS5 prefix search over headwords) is missing")

    print("== sample for a human to read (does each meaning fit the word?)")
    for lang in LANGS:
        for h, g in db.execute("select e.headword, s.gloss from entry e join sense s on s.entry_id=e.id where e.lang=? and s.idx=0 order by e.freq desc limit 4", (lang,)):
            print(f"  {lang}  {h}  ->  {g[:90]}")
    return 1 if fails else 0


def selftest() -> int:
    d = Path(tempfile.mkdtemp()) / "dictionary.db"
    db = sqlite3.connect(d)
    db.executescript(
        """
        create table meta(key text primary key, value text);
        create table source(id integer primary key, code text, name text, licence text, url text, attribution text, retrieved text);
        create table entry(id integer primary key, lang text, headword text, norm text, skey text, pos text, ipa text, etymology text, freq integer, source_id integer);
        create table sense(id integer primary key, entry_id integer, idx integer, gloss_lang text, gloss text, example text);
        create table form(form_norm text, entry_id integer, kind text);
        create virtual table entry_fts using fts5(headword, content='entry', content_rowid='id');
        insert into meta values('dictionary_version','1'),('built','2026-10-09'),('schema','1');
        insert into source values(1,'enwikt','English Wiktionary','CC BY-SA 4.0','https://en.wiktionary.org','Text from Wiktionary, CC BY-SA 4.0','2026-10-09');
        """
    )
    n = 0
    for lang, words in MUST_FIND.items():
        for w in words:
            n += 1
            db.execute("insert into entry values(?,?,?,?,?,?,?,?,?,?)", (n, lang, w, w, w, "noun", None, None, 10, 1))
            db.execute("insert into sense values(?,?,?,?,?,?)", (n, n, 0, "en", f"meaning of {w}", None))
    db.execute("insert into entry_fts(rowid, headword) select id, headword from entry")
    db.commit()
    db.close()
    print("-- selftest 1: a sound database must pass")
    ok = check(d, 30) == 0
    db = sqlite3.connect(d)
    db.execute("update sense set gloss='see [[this]]' where id=1")
    db.execute("update source set licence=''")
    db.commit()
    db.close()
    print("-- selftest 2: a database with markup and no licence must fail")
    bad = check(d, 30) == 1
    print("selftest:", "OK" if ok and bad else "BROKEN")
    return 0 if ok and bad else 1


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("db", nargs="?", default=str(DEFAULT))
    ap.add_argument("--max-mb", type=float, default=30)
    ap.add_argument("--selftest", action="store_true")
    a = ap.parse_args()
    sys.exit(selftest() if a.selftest else check(Path(a.db), a.max_mb))
