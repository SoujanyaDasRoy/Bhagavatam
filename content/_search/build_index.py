#!/usr/bin/env python3
"""Build content/_search/search.db from content/content.db and aliases.json.

    py content/_search/build_index.py            build, verify the alias file against the book, print sizes
    py content/_search/build_index.py --check    only verify aliases.json (no database written)

Design (docs/superpowers/plans/2026-10-09-search.md section 3.2, size-reduced after the first draft came out at 52 MB):
  loose(key, df, forms, post)   one row per loose phonetic key; post = delta-coded varint list of verse ids (rowids of content.db)
  exact(form, post)             posting lists only for the exact word forms named in the "related" word groups
  grp / grp_by_term             alias groups (loose keys) and related-word groups (exact forms), both directions
  chapter_start(idx, ...)       first verse id of every chapter, so a verse id maps to its chapter by bisection
  title_key(key, post)          loose keys of the chapter-title words -> chapter indexes
  meta                          search_version, content_version (the app refuses an index built from another content.db)
Plain tables only (no FTS5), WITHOUT ROWID, no timestamps: building twice gives identical bytes.
Stories are NOT stored here: the app already has them in Episodes.kt and matches them with the same loose key.
"""
import collections
import hashlib
import json
import os
import re
import sqlite3
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
sys.path.insert(0, str(HERE))
sys.stdout.reconfigure(encoding="utf-8")
import prototype as p  # noqa: E402

CONTENT_DB = ROOT / "content" / "content.db"
SEARCH_DB = HERE / "search.db"
ALIASES = HERE / "aliases.json"
SEARCH_VERSION = "2"
MIN_KEY = 3          # loose keys shorter than this are not indexed (same as the prototype)
FORMS_MIN_DF = 8     # keys seen in fewer verses get no display forms (typo suggestions only offer common words)
INDIC = re.compile(r"[ऀ-ॿঀ-৿଀-୿]")
SCRIPT_OF = (("deva", re.compile(r"[ऀ-ॿ]")), ("bn", re.compile(r"[ঀ-৿]")), ("or", re.compile(r"[଀-୿]")))


# ---------------------------------------------------------------- posting lists (varint, delta coded)
def encode_posting(ids) -> bytes:
    out, prev = bytearray(), 0
    for v in sorted(set(ids)):
        n = v - prev
        prev = v
        while n >= 0x80:
            out.append((n & 0x7F) | 0x80)
            n >>= 7
        out.append(n)
    return bytes(out)


def decode_posting(blob: bytes) -> list:
    out, acc, val, shift = [], 0, 0, 0
    for b in blob:
        val |= (b & 0x7F) << shift
        if b & 0x80:
            shift += 7
        else:
            acc += val | 0  # the last byte of a number has no continuation bit
            out.append(acc)
            val, shift = 0, 0
    return out


# ---------------------------------------------------------------- words
def key_of(word: str) -> str:
    return p.loose_from_indic(word) if INDIC.search(word) else p.loose_from_roman(word)


def exact_of(word: str) -> str:
    """The exact form used for related words: NFC without joiners; lower case for Roman text."""
    w = p.nfc(word)
    return w if INDIC.search(w) else w.lower()


def script_of(word: str) -> str:
    for name, rx in SCRIPT_OF:
        if rx.search(word):
            return name
    return "roman"


def verse_words(sa, iast, hi, en, bn):
    """Yield every word of a verse, the same words the prototype indexes."""
    for lang, text in (("hi", hi), ("bn", bn), ("sa", sa)):
        for w in p.WORD[lang].findall((text or "").replace("।", " ").replace("॥", " ")):
            yield w
    for text in (iast, en):
        for w in p.WORD["iast"].findall(text or ""):
            yield w


def load_groups():
    """aliases.json -> list of dicts: id, kind ('alias' by loose key | 'related' by exact form), forms, typed."""
    d = json.loads(ALIASES.read_text(encoding="utf-8"))
    groups = []
    for kind, field in (("alias", "aliases"), ("related", "related")):
        for g in d.get(field, []):
            groups.append({"id": g["id"], "kind": kind, "forms": g["forms"], "typed": g.get("typed", [])})
    return groups


# ---------------------------------------------------------------- build
def scan(want_exact: set):
    cdb = sqlite3.connect(str(CONTENT_DB))
    meta = dict(cdb.execute("select key, value from meta").fetchall())
    rows = cdb.execute("select rowid, skandha, chapter, num, sa, iast, hi, en, bn from verse order by rowid").fetchall()
    titles = cdb.execute("select skandha, chapter, title_en, title_hi, title_bn from chapter order by skandha, chapter").fetchall()
    cdb.close()

    # verse ids must run in book order, or "first verse of the chapter" bisection is wrong
    firsts, seen = [], set()
    for vid, sk, ch, *_ in rows:
        if (sk, ch) not in seen:
            seen.add((sk, ch))
            firsts.append((sk, ch, vid))
    if [(s, c) for s, c, _ in firsts] != sorted((s, c) for s, c, _ in firsts):
        raise SystemExit("verses are not stored in (skandha, chapter) order")

    loose = collections.defaultdict(set)
    forms = collections.defaultdict(collections.Counter)
    exact = collections.defaultdict(set)
    for vid, sk, ch, num, sa, iast, hi, en, bn in rows:
        for w in verse_words(sa, iast, hi, en, bn):
            ex = exact_of(w)
            k = key_of(w)
            if len(k) >= MIN_KEY:
                loose[k].add(vid)
                forms[k][ex] += 1
            if ex in want_exact:
                exact[ex].add(vid)
    return meta, rows, firsts, titles, loose, forms, exact


def verify_groups(groups, loose, exact) -> list:
    """Every 'forms' entry must occur in the book. Returns a list of problems (empty = all fine)."""
    problems = []
    for g in groups:
        ok = 0
        for f in g["forms"]:
            if g["kind"] == "alias":
                df = len(loose.get(key_of(f), ()))
            else:
                df = len(exact.get(exact_of(f), ()))
            if df == 0:
                problems.append(f"group {g['id']} ({g['kind']}): form {f!r} does not occur in the book")
            else:
                ok += 1
        if ok < 2:
            problems.append(f"group {g['id']}: needs at least 2 forms that occur in the book, has {ok}")
    return problems


def build():
    t0 = time.time()
    groups = load_groups()
    want_exact = {exact_of(f) for g in groups if g["kind"] == "related" for f in g["forms"]}
    meta, rows, firsts, titles, loose, forms, exact = scan(want_exact)
    print(f"read {len(rows):,} verses, {len(firsts)} chapters; content_version {meta.get('content_version')}")
    problems = verify_groups(groups, loose, exact)
    for pr in problems:
        print("ALIAS PROBLEM:", pr)
    if problems:
        return 1
    print(f"aliases.json: {len(groups)} groups, every listed form occurs in the book")
    if "--check" in sys.argv:
        return 0

    title_post = collections.defaultdict(set)
    for idx, (sk, ch, ten, thi, tbn) in enumerate(titles):
        for t in (ten, thi, tbn):
            for w in re.findall(r"[^\W\d_]+", t or ""):
                k = key_of(w)
                if len(k) >= MIN_KEY:
                    title_post[k].add(idx)

    if SEARCH_DB.exists():
        os.remove(SEARCH_DB)
    db = sqlite3.connect(str(SEARCH_DB))
    db.execute("pragma journal_mode=OFF")
    db.execute("pragma page_size=4096")
    db.executescript(
        """
        CREATE TABLE meta(key TEXT PRIMARY KEY, value TEXT) WITHOUT ROWID;
        CREATE TABLE loose(key TEXT PRIMARY KEY, df INTEGER NOT NULL, forms TEXT, post BLOB NOT NULL) WITHOUT ROWID;
        CREATE TABLE exact(form TEXT PRIMARY KEY, post BLOB NOT NULL) WITHOUT ROWID;
        CREATE TABLE grp(g INTEGER NOT NULL, kind TEXT NOT NULL, term TEXT NOT NULL, PRIMARY KEY(g, kind, term)) WITHOUT ROWID;
        CREATE TABLE grp_by_term(term TEXT NOT NULL, kind TEXT NOT NULL, g INTEGER NOT NULL, PRIMARY KEY(term, kind, g)) WITHOUT ROWID;
        CREATE TABLE chapter_start(idx INTEGER PRIMARY KEY, skandha INTEGER NOT NULL, chapter INTEGER NOT NULL, first_id INTEGER NOT NULL);
        CREATE TABLE title_key(key TEXT PRIMARY KEY, post BLOB NOT NULL) WITHOUT ROWID;
        """
    )
    db.executemany("INSERT INTO meta VALUES (?,?)", [("search_version", SEARCH_VERSION), ("content_version", str(meta.get("content_version", "")))])

    # loose index; display forms (best Roman, Devanagari, Bengali, Odia spelling) for keys common enough to suggest
    out = []
    for k in sorted(loose):
        vids = loose[k]
        disp = ""
        if len(vids) >= FORMS_MIN_DF:
            best = {}
            for f, c in sorted(forms[k].items(), key=lambda kv: (-kv[1], kv[0])):
                best.setdefault(script_of(f), f)
            disp = "|".join(best[s] for s in ("roman", "deva", "bn", "or") if s in best)
        out.append((k, len(vids), disp or None, encode_posting(vids)))
    db.executemany("INSERT INTO loose VALUES (?,?,?,?)", out)
    db.executemany("INSERT INTO exact VALUES (?,?)", [(f, encode_posting(v)) for f, v in sorted(exact.items())])

    # groups: aliases by loose key (forms and the spellings people type), related words by exact form
    for gi, g in enumerate(groups, 1):
        members = set()
        for f in g["forms"] + g["typed"]:
            members.add(("k", key_of(f)) if g["kind"] == "alias" else ("f", exact_of(f)))
        for kind, term in sorted(members):
            db.execute("INSERT OR IGNORE INTO grp VALUES (?,?,?)", (gi, kind, term))
            db.execute("INSERT OR IGNORE INTO grp_by_term VALUES (?,?,?)", (term, kind, gi))

    db.executemany("INSERT INTO chapter_start VALUES (?,?,?,?)", [(i, s, c, v) for i, (s, c, v) in enumerate(firsts)])
    db.executemany("INSERT INTO title_key VALUES (?,?)", [(k, encode_posting(v)) for k, v in sorted(title_post.items())])
    db.commit()
    db.execute("VACUUM")
    db.close()

    size = SEARCH_DB.stat().st_size
    print(f"\nsearch.db: {size / 1e6:.2f} MB, built in {time.time() - t0:.1f}s, sha256 {hashlib.sha256(SEARCH_DB.read_bytes()).hexdigest()[:16]}")
    con = sqlite3.connect(str(SEARCH_DB))
    for t in ("loose", "exact", "grp", "grp_by_term", "chapter_start", "title_key", "meta"):
        print(f"  {t}: {con.execute(f'select count(*) from {t}').fetchone()[0]:,} rows")
    con.close()
    return 0


if __name__ == "__main__":
    sys.exit(build())
